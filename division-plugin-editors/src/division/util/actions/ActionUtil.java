package division.util.actions;

// CURRENT

import TextFieldLabel.TextFieldLabel;
import bum.editors.util.ObjectLoader;
import bum.interfaces.*;
import bum.interfaces.ProductDocument.ActionType;
import division.fx.PropertyMap;
import division.fx.dialog.FXD;
import division.fx.editor.GLoader;
import division.fx.gui.FXGLoader;
import division.fx.table.Column;
import division.fx.table.FXDivisionTable;
import division.fx.table.FXDivisionTableCell;
import division.fx.util.MsgTrash;
import division.util.ScriptUtil;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import mapping.Archive;
import mapping.MappingObject;
import org.apache.commons.lang3.ArrayUtils;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class ActionUtil{
  
  public static void runAction(ActionType actionType, PropertyMap data) throws Exception {
    Executors.newSingleThreadExecutor().submit(() -> {
      try {
        Map<String,Object> scriptData = data.getSimpleMap();
        scriptData.put("actionType", actionType);
        String scriptName = null;
        switch(actionType) {
          case СТАРТ   : scriptName = "StartDeals";break;
          case ОТГРУЗКА: scriptName = "DispatchDeals";break;
          case ОПЛАТА  : scriptName = "PayDeals";break;
        }
        ScriptUtil.runGroovyClass(scriptName, scriptData);
      }catch(Exception ex) {
        MsgTrash.out(ex);
      }
    });
  }
  
  public static void startDeals(Parent parent, List<Integer> deals) {
    Platform.runLater(() -> {
      //Получить не стартонувшие позиции из сделок
      List<PropertyMap> dealPositions = ObjectLoader.getList(DBFilter.create(DealPosition.class)
              .AND_IN("deal", deals.toArray(new Integer[0]))
              .AND_EQUAL("dispatchId", null)
              .AND_EQUAL("startId", null),
              "id",
              // Другие позиции в которых присутствует объект из данной позиции
              "positions=query:(select array_agg(a.id) from [DealPosition] a where a.[!DealPosition(equipment)]=[DealPosition(equipment)] and a.id != [DealPosition(id)])",
              "equipment",
              "deal",
              "product",
              "startDate",
              "startId",
              "group_name",
              "identity_name",
              "identity_value",
              "amount", // количество в позиции
              "кол.:=:amount", // количество введённое оператором
              "customProductCost", // цена за единицу
              "cost=query:customProductCost*amount", // стоимость позиции
              "выбрать=query:(select [DealPosition(zakaz)] = false)",
              "storeType:=:target-store-type",
              "storeObjectType:=:target-store-object-type",
              // Флаг перемещаемости
              "move=query:SELECT [Service(moveStorePosition)] FROM [Service] WHERE [Service(id)]=(SELECT [Product(service)] FROM [Product] WHERE [Product(id)]=[DealPosition(product)])",
              "sourceStore",
              "targetStore",
              "target-store-name",
              "customer_partition_id",
              "zakaz",
              "equipment-store",
              "controll=query:(select [Store(controllOut)] from [Store] where [Store(id)]=[DealPosition(equipment-store)])").stream()
              .map(dp -> dp
                      .setValue("targetStore", PropertyMap.create().setValue("id", dp.getInteger("targetStore")).setValue("name", dp.getString("target-store-name")))
                      .remove("target-store-name").setValue("выбрать", dp.isNull("выбрать") ? true : dp.is("выбрать"))).collect(Collectors.toList());
      if(dealPositions.isEmpty()) // если таких нет
        new Alert(Alert.AlertType.WARNING, "Данн"+(deals.size()==1?"ая":"ые")+" сделк"+(deals.size()==1?"а":"и")+" полностью старнону"+(deals.size()==1?"ла":"ли"), ButtonType.OK).show();
      else {
        //if(deals.size() == 1) { // Если старт только одной сделки, то нужно выбрать какие позиции и сколько стартовать
        
        
        TextFieldLabel<String> requestNumber = new TextFieldLabel<>("Заявка №");
        
        if(!(dealPositions.size() == 1 && dealPositions.get(0).isNull("equipment"))) {
          
          FXDivisionTable<PropertyMap> table = createTableForSelectDealPositions(dealPositions);
          table.getColumn("выбрать").setVisible(deals.size() == 1);
          VBox.setVgrow(table, Priority.ALWAYS);
          FXD fxd = FXD.create("Старт", parent, table, FXD.ButtonType.OK);
          fxd.getButtonPanel().getChildren().add(0, requestNumber);
          fxd.setOnShowing(e -> FXGLoader.load("select-deal-positions", table));
          fxd.setOnHiding(e -> FXGLoader.store("select-deal-positions", table));
          fxd.setOnCloseRequest(e -> dealPositions.stream().forEach(dp -> dp.setValue("выбрать",false)));
          if(fxd.showDialog() == FXD.ButtonType.CANCEL)
            dealPositions.stream().forEach(dp -> dp.setValue("выбрать",false));
        //}
        
        }
        
        // фильтруем только выбранные для старта позиции
        List<PropertyMap> dps = dealPositions.stream().filter(dp -> dp.is("выбрать")).collect(Collectors.toList());
        if(!dps.isEmpty()) { // стартуем позиции
          // готовим создателя документов
          DocumentCreator documentCreator = new DocumentCreator();
          // инициализируем создателя документов
          documentCreator.init(ProductDocument.ActionType.СТАРТ, PropertyMap.getArrayFromList(dps, "id", Integer.class));
          // настраиваем документы
          if(documentCreator.customUpdateDocuments(parent, deals.size() > 1)) { // настройка документов прошла успешно
            RemoteSession session = null;
            try {
              session = ObjectLoader.createSession(false);
              
              if(requestNumber.valueProperty().getValue() != null && !"".equals(requestNumber.valueProperty().getValue())) {
                PropertyMap rst = PropertyMap.create()
                        .setValue("tmp",true)
                        .setValue("number",requestNumber.valueProperty().getValue())
                        .setValue("applicant",session.getList(DBFilter.create(Deal.class).AND_EQUAL("id", deals.get(0)), "customerCompany").get(0).get("customerCompany"))
                        .setValue("reason","Автоматическая заявка");
                rst.setValue("equipments", dps.stream().map(d -> d.getInteger("equipment")).collect(Collectors.toSet()).toArray(new Integer[0]));
                rst.setValue("id", session.createObject(Request.class, rst.getSimpleMap()));
                session.executeUpdate(DealPosition.class, new String[]{"request"}, new Object[]{rst.getInteger("id")}, dps.stream().map(d -> d.getInteger("id")).collect(Collectors.toList()).toArray(new Integer[0]));
              }//else session.executeUpdate(DealPosition.class, new String[]{"request"}, new Object[]{null}, dps.stream().map(d -> d.getInteger("id")).collect(Collectors.toList()).toArray(new Integer[0]));
              
              for(PropertyMap position:dps) { // цикл по стартуемым позициям
                if(!position.isNull("equipment")) { //объект присутствует, то есть это не безобъектная сделка

                  // Инициализируем объект
                  position.setValue("equipment", PropertyMap.copy(session.object(Equipment.class, position.getInteger("equipment"))).remove("dealPositions","groupFactorValues"));
                  
                  // если количество в позиции больше количества в объекте, то это ошибка
                  if(position.getBigDecimal("amount").compareTo(position.getMap("equipment").getBigDecimal("amount")) > 0)
                    throw new Exception("Колличество в позиции больше колличества в объекте - такого не должно случаться");
                  else { // количество в позиции меньше или равно количеству в объекте
                    
                    // если количество выбранных для старта объектов больше количества в позиции, то это ошибка
                    if(position.getBigDecimal("кол.").compareTo(position.getBigDecimal("amount")) > 0)
                      throw new Exception("Недопустимое колличество стартующих объектов");
                    else { // количество выбранных для старта объектов меньше или равно количеству в позиции

                      PropertyMap startPosition = position.copy(); // стартуемая позиция, копируем её
                      
                      // Если колличество стартуемых объектом меньше общего колличества объектов в позиции, то нужно разделить позиции
                      if(startPosition.getBigDecimal("кол.").compareTo(position.getBigDecimal("amount")) < 0) {
                        // разбить позицию поставщика на две в соответствии с выбранным колличеством стартуемых объектов.
                        startPosition = splitDealPosition(session, position, position.getBigDecimal("amount").subtract(position.getBigDecimal("кол.")), position.getBigDecimal("кол."));
                        documentCreator.replaceDealPosition(position.getInteger("id"), startPosition.getInteger("id"));
                      }
                      
                      /*if(startPosition.is("move")) { // Нужно переместить объект
                        
                        // Если количество стартуемых объектов меньше количества самих объектов, то нужно разделить объекты
                        if(startPosition.getBigDecimal("amount").compareTo(startPosition.getMap("equipment").getBigDecimal("amount")) < 0) {
                          // Разбиваем объект на два в соответствии с позициями
                          startPosition.setValue("equipment", splitEquipment(session, position, startPosition));
                        }

                        // архивировать объект
                        session.toArchive(Equipment.class, position.getMap("equipment").getInteger("id"));
                        
                        session.saveObject(DealPosition.class, startPosition.getSimpleMap("id","targetStore"));
                        
                        // Перемещаем объект на склад покупателя
                        session.saveObject(Equipment.class, startPosition.getMap("equipment").setValue("store", startPosition.getValue("targetStore")).setValue("zakaz", false).getSimpleMap("id", "store","zakaz"));
                      }else { // нет перемещений
                      }*/
                      
                      position.copyFrom(startPosition);
                    }
                  }
                }else { // объект отсутствует
                }
              }
              // Сформировать документы и стартонуть позиции
              Map<Integer,List> createdDocuments = documentCreator.start(session);
              
              PropertyMap.fromSimple(session.getList(DBFilter.create(CreatedDocument.class).AND_IN("id", ((Set)createdDocuments.values().stream().flatMap(l -> l.stream()).collect(Collectors.toSet())).toArray(new Integer[0])), "id","name","date"))
                      .stream().forEach(d -> System.out.println(d.toJson()));
              
              Long startId = (Long) session.executeQuery("SELECT MAX([DealPosition(startId)])+1 FROM [DealPosition]").get(0).get(0);
              
              
              List<String> querys = FXCollections.observableArrayList("UPDATE [DealPosition] SET [DealPosition(startId)]=?,[DealPosition(startDate)]=CURRENT_TIMESTAMP WHERE [DealPosition(id)]=ANY(?)");
              List<Object[]> params = FXCollections.observableArrayList();
              params.add(new Object[]{startId == null ? 1 : startId,PropertyMap.getArrayFromList(dps, "id", Integer.class)});
              
              dps.stream().forEach(dp -> {
                List<Integer> docs = createdDocuments.get(dp.getInteger("id"));
                querys.add("UPDATE [DealPosition] SET [DealPosition(startId)]="+(startId == null ? 1 : startId)+", [DealPosition(startDate)]=CURRENT_TIMESTAMP "
                        //+ "[DealPosition(actionDispatchDate)]="+(docs == null || docs.isEmpty() ? "CURRENT_DATE " : "(CASE WHEN (SELECT MAX([CreatedDocument(date)]) FROM [CreatedDocument] WHERE id=ANY(?)) ISNULL THEN CURRENT_DATE ELSE (SELECT MAX([CreatedDocument(date)]) FROM [CreatedDocument] WHERE id=ANY(?)) END) ")
                        + "WHERE [DealPosition(id)]="+dp.getInteger("id"));
                /*if(docs != null && !docs.isEmpty())
                  params.add(new Object[]{docs.toArray(new Integer[0]), docs.toArray(new Integer[0])});
                else */params.add(new Object[0]);
              });
              
              Object[][] pp = null;
              for(Object[] p:params)
                pp = ArrayUtils.add(pp, p);
              
              session.executeUpdate(querys.toArray(new String[0]), pp);
              session.addEvent(DealPosition.class, "UPDATE", PropertyMap.getArrayFromList(dps, "id", Integer.class));
              session.addEvent(Deal.class, "UPDATE", deals.toArray(new Integer[0]));
              
              ObjectLoader.commitSession(session);
            }catch (Exception ex) {
              ObjectLoader.rollBackSession(session);
              MsgTrash.out(parent, ex);
            }
          }
        }
      }
    });
    
    
    
    
    
    
    /*ObservableList<PropertyMap> dealPositions = ObjectLoader.getList(DBFilter.create(DealPosition.class).AND_IN("deal", deals.toArray(new Integer[0])).AND_EQUAL("startDate", null), "id","deal","amount");
    if(dealPositions.isEmpty())
      new Alert(Alert.AlertType.WARNING, "Данн"+(deals.size()==1?"ая":"ые")+" сделк"+(deals.size()==1?"а":"и")+" полностью стартонул"+(deals.size()==1?"а":"и"), ButtonType.OK).showAndWait();
    else {
      Platform.runLater(() -> {
        DocumentCreator documentCreator = new DocumentCreator();
        documentCreator.init(ProductDocument.ActionType.СТАРТ, PropertyMap.getListFromList(dealPositions, "id", Integer.TYPE));
        if(documentCreator.customUpdateDocuments(deals.size() > 1)) {
          RemoteSession session = null;
          try {
            session = ObjectLoader.createSession(false);
            session.executeUpdate("UPDATE [DealPosition] SET "
                    + "[DealPosition(startId)]=(SELECT MAX([DealPosition(startId)])+1 FROM [DealPosition]), "
                    + "[DealPosition(startDate)]=CURRENT_TIMESTAMP "
                    + "WHERE [DealPosition(id)]=ANY(?)", new Object[]{PropertyMap.getListFromList(dealPositions, "id", Integer.TYPE).toArray(new Integer[0])});
            documentCreator.start(session);
            session.addEvent(DealPosition.class, "UPDATE", PropertyMap.getListFromList(dealPositions, "id", Integer.TYPE).toArray(new Integer[0]));
            session.addEvent(Deal.class, "UPDATE", deals.toArray(new Integer[0]));
            ObjectLoader.commitSession(session);
          } catch (Exception ex) {
            ObjectLoader.rollBackSession(session);
            MsgTrash.out(ex);
          }
        }
      });
    }*/
  }
  
  public static void dispatchDeals(Parent parent, List<Integer> deals) {
    Platform.runLater(() -> {
      //Получить неотгруженные и стартонувшие позиции из сделок
      List<PropertyMap> dealPositions = ObjectLoader.getList(DBFilter.create(DealPosition.class)
              .AND_IN("deal", deals.toArray(new Integer[0]))
              .AND_EQUAL("dispatchId", null)
              .AND_NOT_EQUAL("startId", null),
              "id",
              // Другие позиции в которых присутствует объект из данной позиции
              "positions=query:(select array_agg(a.id) from [DealPosition] a where a.[!DealPosition(equipment)]=[DealPosition(equipment)] and a.id != [DealPosition(id)])",
              "equipment",
              "deal",
              "product",
              "startDate",
              "startId",
              "group_name",
              "service_name",
              "identity_name",
              "identity_value",
              "amount", // количество в позиции
              "кол.:=:amount", // количество введённое оператором
              "customProductCost", // цена за единицу
              "cost=query:customProductCost*amount", // стоимость позиции
              "выбрать=query:(case when (select [DealPosition(zakaz)]) isnull then true else (select [DealPosition(zakaz)] = false) end)",
              "storeType:=:target-store-type",
              "storeObjectType:=:target-store-object-type",
              // Флаг перемещаемости
              "move=query:SELECT [Service(moveStorePosition)] FROM [Service] WHERE [Service(id)]=(SELECT [Product(service)] FROM [Product] WHERE [Product(id)]=[DealPosition(product)])",
              "sourceStore",
              "targetStore",
              "target-store-name",
              "customer_partition_id",
              "zakaz",
              "equipment-store",
              "controll=query:(select [Store(controllOut)] from [Store] where [Store(id)]=[DealPosition(equipment-store)])").stream()
              .map(dp -> dp.setValue("targetStore", PropertyMap.create().setValue("id", dp.getInteger("targetStore")).setValue("name", dp.getString("target-store-name"))).remove("target-store-name")).collect(Collectors.toList());
      
      if(dealPositions.isEmpty()) // если таких нет
        new Alert(Alert.AlertType.WARNING, "Данн"+(deals.size()==1?"ая":"ые")+" сделк"+(deals.size()==1?"а":"и")+" полностью отгруже"+(deals.size()==1?"а":"ы"), ButtonType.OK).show();
      else {
        
        List<PropertyMap> zakazList = dealPositions.stream().filter(dp -> !dp.is("выбрать")).collect(Collectors.toList());
        
        if(!zakazList.isEmpty()) {
          if(zakazList.size() == dealPositions.size()) {
            FXD.showWait("Внимание...", parent, "Отгрузка заказов невозможна", FXD.ButtonType.OK);
            return;
          }else {
            String msg = String.join("\n", zakazList.stream().map(dp -> "  -"+dp.getString("service_name")+" "+dp.getString("group_name")).toArray(String[]::new));
            if(FXD.showWait("Внимание...", parent, "Отгрузка следующих заказов невозможна:\n\n"+msg+"\n\nПродолжить отгрузку остальных позиций?", FXD.ButtonType.YES, FXD.ButtonType.NO).orElseGet(() -> FXD.ButtonType.NO) == FXD.ButtonType.NO)
              return;
          }
        }
        
        
        dealPositions.stream().anyMatch(d -> d.isNull("equipment"));
        
        //if(dealPositions.size() > 1 && !dealPositions.stream().anyMatch(d -> d.isNull("equipment"))) {
        //if(deals.size() == 1) { // Если отгрузка только одной сделки, то нужно выбрать какие позиции и сколько отгружать
          FXDivisionTable<PropertyMap> table = createTableForSelectDealPositions(dealPositions);
          VBox.setVgrow(table, Priority.ALWAYS);
          
          FXD fxd = FXD.create("Отгрузка", parent, table, FXD.ButtonType.OK);
          fxd.setOnShowing(e -> FXGLoader.load("select-deal-positions", table));
          fxd.setOnHiding(e -> FXGLoader.store("select-deal-positions", table));
          fxd.setOnCloseRequest(e -> dealPositions.stream().forEach(dp -> dp.setValue("выбрать",false)));
          if(fxd.showDialog() == FXD.ButtonType.CANCEL)
            dealPositions.stream().forEach(dp -> dp.setValue("выбрать",false));
        //}
        //}
        
        // фильтруем только выбранные для отгрузки позиции
        List<PropertyMap> dps = dealPositions.stream().filter(dp -> dp.getValue("выбрать", true)).collect(Collectors.toList());
        if(!dps.isEmpty()) { // отгружаем позиции
          // готовим создателя документов
          DocumentCreator documentCreator = new DocumentCreator();
          // инициализируем создателя документов
          documentCreator.init(ProductDocument.ActionType.ОТГРУЗКА, PropertyMap.getListFromList(dps, "id", Integer.TYPE).toArray(new Integer[0]));
          // настраиваем документы
          if(documentCreator.customUpdateDocuments(parent, deals.size() > 1)) { // настройка документов прошла успешно
            RemoteSession session = null;
            try {
              session = ObjectLoader.createSession(false);
              for(PropertyMap position:dps) { // цикл по отгружаемым позициям
                
                if(!position.isNull("equipment")) { //объект присутствует, то есть это не безобъектная сделка

                  // Инициализируем объект
                  position.setValue("equipment", PropertyMap.copy(session.object(Equipment.class, position.getInteger("equipment"))).remove("dealPositions","groupFactorValues"));
                  
                  // если количество в позиции больше количества в объекте, то это ошибка
                  if(position.getBigDecimal("amount").compareTo(position.getMap("equipment").getBigDecimal("amount")) > 0)
                    throw new Exception("Колличество в позиции больше колличества в объекте - такого не должно случаться");
                  else { // количество в позиции меньше или равно количеству в объекте
                    
                    // если количество выбранных для отгрузки объектов больше количества в позиции, то это ошибка
                    if(position.getBigDecimal("кол.").compareTo(position.getBigDecimal("amount")) > 0)
                      throw new Exception("Недопустимое колличество отгружаемых объектов");
                    else { // количество выбранных для отгрузки объектов меньше или равно количеству в позиции

                      PropertyMap dispatchPosition = position.copy(); // отгружаемая позиция, копируем её
                      
                      // Если колличество отгружаемых объектом меньше общего колличества объектов в позиции, то нужно разделить позиции
                      if(dispatchPosition.getBigDecimal("кол.").compareTo(position.getBigDecimal("amount")) < 0) {
                        // разбить позицию поставщика на две в соответствии с выбранным колличеством отгружаемых объектов.
                        dispatchPosition = splitDealPosition(session, position, position.getBigDecimal("amount").subtract(position.getBigDecimal("кол.")), position.getBigDecimal("кол."));
                        documentCreator.replaceDealPosition(position.getInteger("id"), dispatchPosition.getInteger("id"));
                      }
                      
                      if(dispatchPosition.is("move")) { // Нужно переместить объект
                        
                        // Если количество отгружаемых объектов меньше количества самих объектов, то нужно разделить объекты
                        if(dispatchPosition.getBigDecimal("amount").compareTo(dispatchPosition.getMap("equipment").getBigDecimal("amount")) < 0) {
                          // Разбиваем объект на два в соответствии с позициями
                          dispatchPosition.setValue("equipment", splitEquipment(session, position, dispatchPosition));
                        }

                        // архивировать объект
                        session.toArchive(Equipment.class, position.getMap("equipment").getInteger("id"));
                        
                        session.saveObject(DealPosition.class, dispatchPosition.getSimpleMap("id","targetStore"));
                        
                        // Перемещаем объект на склад покупателя
                        session.saveObject(Equipment.class, dispatchPosition.getMap("equipment").setValue("store", dispatchPosition.getValue("targetStore")).setValue("zakaz", false).getSimpleMap("id", "store","zakaz"));
                      }else { // нет перемещений
                      }
                      
                      position.copyFrom(dispatchPosition);
                    }
                  }
                }else { // объект отсутствует
                  // если количество выбранных для отгрузки объектов больше количества в позиции, то это ошибка
                  if(position.getBigDecimal("кол.").compareTo(position.getBigDecimal("amount")) > 0)
                    throw new Exception("Недопустимое колличество отгружаемых объектов");
                  
                  // количество выбранных для отгрузки объектов меньше или равно количеству в позиции
                  
                  PropertyMap dispatchPosition = position.copy(); // отгружаемая позиция, копируем её
                      
                  // Если колличество отгружаемых объектом меньше общего колличества объектов в позиции, то нужно разделить позиции
                  if(dispatchPosition.getBigDecimal("кол.").compareTo(position.getBigDecimal("amount")) < 0) {
                    // разбить позицию поставщика на две в соответствии с выбранным колличеством отгружаемых объектов.
                    dispatchPosition = splitDealPosition(session, position, position.getBigDecimal("amount").subtract(position.getBigDecimal("кол.")), position.getBigDecimal("кол."));
                    documentCreator.replaceDealPosition(position.getInteger("id"), dispatchPosition.getInteger("id"));
                  }
                  position.copyFrom(dispatchPosition);
                }
              }
              // Сформировать документы и отгрузить позиции
              Map<Integer,List> createdDocuments = documentCreator.start(session);
              
              PropertyMap.fromSimple(session.getList(DBFilter.create(CreatedDocument.class).AND_IN("id", ((Set)createdDocuments.values().stream().flatMap(l -> l.stream()).collect(Collectors.toSet())).toArray(new Integer[0])), "id","name","date"))
                      .stream().forEach(d -> System.out.println(d.toJson()));
              
              Long dispatchId = (Long) session.executeQuery("SELECT MAX([DealPosition(dispatchId)])+1 FROM [DealPosition]").get(0).get(0);
              List<String> querys = FXCollections.observableArrayList("UPDATE [DealPosition] SET [DealPosition(dispatchId)]=?,[DealPosition(dispatchDate)]=CURRENT_TIMESTAMP WHERE [DealPosition(id)]=ANY(?)");
              List<Object[]> params = FXCollections.observableArrayList();
              params.add(new Object[]{dispatchId == null ? 1 : dispatchId,PropertyMap.getListFromList(dps, "id", Integer.TYPE).toArray(new Integer[0])});
              
              dps.stream().forEach(dp -> {
                List<Integer> docs = createdDocuments.get(dp.getInteger("id"));
                querys.add("UPDATE [DealPosition] SET [DealPosition(dispatchId)]="+(dispatchId == null ? 1 : dispatchId)+", [DealPosition(dispatchDate)]=CURRENT_TIMESTAMP, "
                        + "[DealPosition(actionDispatchDate)]="+(docs == null || docs.isEmpty() ? "CURRENT_DATE " : "(CASE WHEN (SELECT MAX([CreatedDocument(date)]) FROM [CreatedDocument] WHERE id=ANY(?)) ISNULL OR (SELECT MAX([CreatedDocument(date)]) FROM [CreatedDocument] WHERE id=ANY(?)) < CURRENT_DATE THEN CURRENT_DATE ELSE (SELECT MAX([CreatedDocument(date)]) FROM [CreatedDocument] WHERE id=ANY(?)) END) ")
                        + "WHERE [DealPosition(id)]="+dp.getInteger("id"));
                if(docs != null && !docs.isEmpty())
                  params.add(new Object[]{docs.toArray(new Integer[0]), docs.toArray(new Integer[0]), docs.toArray(new Integer[0])});
                else params.add(new Object[0]);
              });
              
              /*createdDocuments.forEach((dp,docs) -> {
                querys.add("UPDATE [DealPosition] SET [DealPosition(actionDispatchDate)]="
                        + "(CASE WHEN (SELECT MAX([CreatedDocument(date)]) FROM [CreatedDocument] WHERE id=ANY(?)) ISNULL THEN CURRENT_DATE ELSE (SELECT MAX([CreatedDocument(date)]) FROM [CreatedDocument] WHERE id=ANY(?)) END) "
                        + "WHERE [DealPosition(id)]=?");
                params.add(new Object[]{docs.toArray(new Integer[0]), docs.toArray(new Integer[0]), dp});
              });*/
              
              Object[][] pp = null;
              for(Object[] p:params)
                pp = ArrayUtils.add(pp, p);
              
              session.executeUpdate(querys.toArray(new String[0]), pp);
              session.addEvent(DealPosition.class, "UPDATE", PropertyMap.getListFromList(dps, "id", Integer.TYPE).toArray(new Integer[0]));
              session.addEvent(Deal.class, "UPDATE", deals.toArray(new Integer[0]));
              
              ObjectLoader.commitSession(session);
            }catch (Exception ex) {
              ObjectLoader.rollBackSession(session);
              MsgTrash.out(parent, ex);
            }
          }
        }
      }
    });
  }
  
  /**
   * 
   * @param parentPosition
   * @param parentAmount
   * @param childAmount
   * @return child DealPosition
   */
  private static PropertyMap splitDealPosition(RemoteSession session, PropertyMap parentPosition, BigDecimal parentAmount, BigDecimal childAmount) throws Exception {
    PropertyMap childPosition = parentPosition.copy().setValue("amount", childAmount);
    childPosition.setValue("id", session.createObject(DealPosition.class, childPosition.getSimpleMap("amount","equipment","deal","product","customProductCost","startDate","startId","sourceStore","targetStore")));
    session.saveObject(DealPosition.class, parentPosition.setValue("amount", parentAmount).getSimpleMap("id","amount"));
    copyDocuments(session, parentPosition, childPosition);
    session.addEvent(DealPosition.class, "CREATE", childPosition.getInteger("id"));
    session.addEvent(DealPosition.class, "UPDATE", parentPosition.getInteger("id"));
    return childPosition;
  }
  
  private static PropertyMap splitEquipment(RemoteSession session, PropertyMap parentPosition, PropertyMap childPosition) throws Exception {
    // Разбиваем объект на два в соответствии с позициями
    PropertyMap childEquipment = parentPosition.getMap("equipment").copy()
                                  .setValue("parent", parentPosition.getMap("equipment").getInteger("id"))
                                  .setValue("amount", childPosition.getBigDecimal("amount"));
    // устанавливаем количество родительского объекта за вычетом количества дочернего
    parentPosition.getMap("equipment").setValue("amount", parentPosition.getMap("equipment").getBigDecimal("amount").subtract(childEquipment.getBigDecimal("amount")));
    
    // сохраняем дочерний объект
    childEquipment.setValue("id", session.createObject(Equipment.class, childEquipment.getSimpleMapWithoutKeys("id","modificationDate","lastUserId")));
    
    // сохраняем родительский объект
    session.saveObject(Equipment.class, parentPosition.getMap("equipment").getSimpleMap("id","amount"));

    // сохранить дочерний объект в дочерней позиции
    session.saveObject(DealPosition.class, childPosition.setValue("equipment", childEquipment).getSimpleMap("id","equipment"));

    // Копируем реквизиты
    for(Map efv:session.getList(DBFilter.create(EquipmentFactorValue.class).AND_EQUAL("equipment", parentPosition.getMap("equipment").getInteger("id")), "factor", "name"))
      session.createObject(EquipmentFactorValue.class, PropertyMap.copy(efv).setValue("equipment", childEquipment.getInteger("id")).getSimpleMap());
    
    // Теперь необходимо разбить другие позиции, в которых учавствует старый объект
    
    for(PropertyMap other:session.getList(DBFilter.create(DealPosition.class).AND_IN("id", parentPosition.getValue("positions", Integer[].class)), 
            "id","amount","equipment","deal","product","customProductCost","startDate","startId","sourceStore","targetStore")
            // если количество в позиции получилось больше количества объекта
            .stream().map(p -> PropertyMap.copy(p)).filter(p -> p.getBigDecimal("amount").compareTo(parentPosition.getMap("equipment").getBigDecimal("amount")) > 0).collect(Collectors.toList())) {
      
      PropertyMap child = splitDealPosition(
              session, 
              other, 
              parentPosition.getMap("equipment").getBigDecimal("amount"), 
              other.getBigDecimal("amount").subtract(parentPosition.getMap("equipment").getBigDecimal("amount"))).setValue("equipment", childEquipment);
      session.saveObject(DealPosition.class, child.getSimpleMap("id","amount","equipment"));
    }
    return childEquipment;
  }
  
  public static Integer[] getDocuments(Integer[] dealPositions, Integer payment, RemoteSession session, ActionType... actionTypes) throws Exception {
    List<PropertyMap> docs = new ArrayList<>();
    
    final ArrayList<Integer> ids = new ArrayList<>();
    
    if(payment != null)
      session.executeQuery("SELECT id FROM [CreatedDocument] WHERE [CreatedDocument(actionType)]=? AND [CreatedDocument(payment)]=? AND "
              + "tmp=false AND type='CURRENT'", new Object[]{ActionType.ОПЛАТА, payment}).forEach(d -> ids.add((Integer) d.get(0)));
    
    if(dealPositions != null && dealPositions.length > 0)
      session.executeQuery("SELECT id FROM [CreatedDocument] WHERE [CreatedDocument(actionType)]=ANY(?) AND "
              + "id IN (SELECT [CreatedDocument(dealPositions):object] FROM [CreatedDocument(dealPositions):table] WHERE [CreatedDocument(dealPositions):target]=ANY(?))",
              new Object[]{actionTypes, dealPositions}).forEach(d -> ids.add((Integer) d.get(0)));
    
    return ids.toArray(new Integer[0]);
  }
  
  private static void copyDocuments(RemoteSession session, PropertyMap parentPosition, PropertyMap childPosition) throws Exception {
    // получить документы от родительской позиции и прописать их же для новой позиции
    List<Integer> documents = new ArrayList<>();
    for(Integer docId:getDocuments(new Integer[]{parentPosition.getInteger("id")}, null, session, ActionType.ОТГРУЗКА, ActionType.СТАРТ)) {
      List<List> l = session.executeQuery("SELECT [CreatedDocument(dealPositions):object] FROM [CreatedDocument(dealPositions):table] "
              + "WHERE [CreatedDocument(dealPositions):target]="+childPosition.getInteger("id")+" AND [CreatedDocument(dealPositions):object]="+docId);
      if(l.isEmpty()) {
        session.executeUpdate("INSERT INTO [CreatedDocument(dealPositions):table] ("
                + "[CreatedDocument(dealPositions):object],"
                + "[CreatedDocument(dealPositions):target]"
                + ") VALUES ("+docId+","+childPosition.getInteger("id")+")");
      }
      documents.add(docId);
    }
    
    for(PropertyMap doc:PropertyMap.createList(session.getList(DBFilter.create(CreatedDocument.class).AND_IN("id", documents.toArray(new Integer[0])), "id","document_name","document_script","document-scriptLanguage","dealPositions"))) {
      DocumentCreator.runDocumentScript(
              doc.getInteger("id"), 
              doc.getString("name"), 
              doc.getString("document_script"), 
              doc.getString("document-scriptLanguage"), 
              doc.getValue("dealPositions", Integer[].class), 
              session, 
              null);
    }
  }
  
  public static void removeDispatch(Integer... positions) throws Exception {
    Platform.runLater(() -> {
      RemoteSession session = null;
      try {
        session = ObjectLoader.createSession(false);
        removeDispatch(session, positions);
        ObjectLoader.commitSession(session);
      } catch (Exception ex) {
        ObjectLoader.rollBackSession(session);
        MsgTrash.out(ex);
      }
    });
  }
  
  public static void removeDispatch(RemoteSession s, Integer... positions) throws Exception {
    String msg = null;
    Integer[] deals = new Integer[0];
    for(Map map:s.getList(DBFilter.create(DealPosition.class).AND_IN("id", positions), 
            "id",
            "deal",
            "equipment",
            "sourceStore",
            "targetStore",
            "equipment-store-company-partition-id=query:(select [Store(companyPartition)] from [Store] where id=[DealPosition(equipment-store)])",
            "customer_partition_id")) {
      PropertyMap dp = PropertyMap.copy(map);
      if(!ArrayUtils.contains(deals, dp.getInteger("deal")))
        deals = ArrayUtils.add(deals, dp.getInteger("deal"));
      if(!dp.isNull("equipment")) { // есть объект
        if(dp.isNotNull("targetStore") && !dp.getInteger("sourceStore").equals(dp.getInteger("targetStore"))) { // объект был перемещён
          
          // Если объекта нет на складе покупателя, то отмена отгрузки невозможна
          if(dp.getInteger("equipment-store-company-partition-id").intValue() != dp.getInteger("customer_partition_id").intValue()) {
            positions = ArrayUtils.removeElement(positions, dp.getInteger("id"));
            msg = (positions.length > 1 ? "Одна из позиций " : "Позиция ")+"не может быть отгружена, так как объект уже перепродан.";
          }else {
            // Получаем данные об объекте
            dp.setValue("equipment", PropertyMap.copy(s.object(Equipment.class, dp.getInteger("equipment"))));
            
            boolean zakaz = false;
            
            if(!dp.getMap("equipment").isNull("parent")) { // если было деление
              // ищем родителя
              PropertyMap parent = PropertyMap.copy(s.object(Equipment.class, dp.getMap("equipment").getInteger("parent")));
              if(parent.getInteger("store").intValue() == dp.getInteger("sourceStore").intValue()) { // пока родитель находится у продавца
                zakaz = parent.is("zakaz");
              }else {
                //ищем родителя в архиве
                ObservableList<PropertyMap> list = PropertyMap.createList(s.getArchive(Equipment.class, dp.getMap("equipment").getInteger("parent")));
                list = list.filtered(m -> m.getValue("object", Map.class).get("store").equals(dp.getInteger("sourceStore")));
                if(!list.isEmpty()) {
                  PropertyMap archive = PropertyMap.copy(list.get(0).getValue("object", Map.class));
                  zakaz = archive.is("zakaz");
                }
              }
            }else  {
              Map arch = s.getLastArchive(Equipment.class, dp.getMap("equipment").getInteger("id"));
              if(arch != null) {
                PropertyMap archive = PropertyMap.copy((Map)arch.get("object"));
                zakaz = archive.is("zakaz");
                
                //удалить последний архивный объект
                s.removeObjects(Archive.class, (Integer)arch.get("id"));
              }
            }
            
            //вернуть признак заказа
            s.executeUpdate(Equipment.class, "zakaz", zakaz, dp.getMap("equipment").getInteger("id"));

            // Вернуть объект на склад продавца
            s.executeUpdate(Equipment.class, "store", dp.getInteger("sourceStore"), dp.getMap("equipment").getInteger("id"));
          }
        }else { // объект не был перемещён
        }
      }else { // объекта нет
      }
    }
    if(msg != null) {
      if(positions.length == 0)
        throw new Exception(msg);
      else new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).show();
    }
    s.toTypeObjects(CreatedDocument.class, getDocuments(positions, null, s, ActionType.ОТГРУЗКА), MappingObject.Type.ARCHIVE);
    s.executeUpdate(DealPosition.class, new String[]{"dispatchId","dispatchDate","actionDispatchDate"}, new Object[]{null,null,null}, positions);
    s.addEvent(Deal.class, "UPDATE", deals);
  }
  
  private static FXDivisionTable<PropertyMap> createTableForSelectDealPositions(List<PropertyMap> dealPositions) {
    FXDivisionTable<PropertyMap> table = new FXDivisionTable(true,
            Column.create("Объект")
                    .addColumn("Наименование", "group_name")
                    .addColumn(" ---- ", "identity_name")
                    .addColumn("----", "identity_value"), 
            Column.create("Стоимость")
                    .addColumn("Цена за ед.", "customProductCost")
                    .addColumn("Колличество", "amount")
                    .addColumn("Итого", "cost"), 
            Column.create("Отобрать")
                    .addColumn("кол.",true,true)
                    .addColumn("выбрать"));
    
    CheckBox check = new CheckBox();
    
    if(dealPositions.stream().filter(t -> !t.is("выбрать")).collect(Collectors.toList()).isEmpty())
      check.setSelected(true);
    
    table.getItems().addListener((ListChangeListener.Change<? extends PropertyMap> c) -> {
      PropertyMap.addListCangeListener(table.getItems(), "выбрать", (ChangeListener) (ObservableValue observable, Object oldValue, Object newValue) -> {
        check.setDisable(true);
        check.setSelected(table.getItems().filtered(t -> !t.is("выбрать")).isEmpty());
        check.setDisable(false);
      });
    });
    
    check.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(!check.isDisable())
        table.getItems().stream().forEach(t -> t.setValue("выбрать", newValue));
    });
    
    table.getColumn("выбрать").setGraphic(check);
    
    if(dealPositions.get(0).is("move")) {
      table.getColumns().add(Column.create("доставить в:", "targetStore",true,true));
      
      ObservableList<PropertyMap> stories = ObjectLoader.getList(DBFilter.create(Store.class)
              .AND_EQUAL("companyPartition", dealPositions.get(0).getValue("customer_partition_id"))
              .AND_EQUAL("storeType", dealPositions.get(0).getValue("storeType")) 
              .AND_EQUAL("objectType", dealPositions.get(0).getValue("storeObjectType")), 
              "id","name");
      
      table.getColumn("доставить в:").setCellFactory((Object param) -> new FXDivisionTableCell(stories.toArray()));
      
      table.setRowFactory((TableView<PropertyMap> param) -> {
        TableRow<PropertyMap> row = new TableRow<>();
        row.itemProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> 
          row.setDisable(newValue != null && newValue.is("zakaz") && (newValue.getInteger("sourceStore").intValue() != newValue.getInteger("equipment-store").intValue() || newValue.is("controll"))));
        return row;
      });
    }
    
    //dealPositions.stream().filter(dp -> dp.is("move")).forEach(dp -> dp.setValue("target-store", PropertyMap.create().setValue("id", dp.getValue("targetStore")).setValue("name", dp.getValue("target-store-name"))));
    table.getItems().setAll(dealPositions);
    
    table.setEditable(true);
    
    dealPositions.stream().forEach(dp -> {

      dp.get("выбрать").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
        if((boolean)newValue && dp.getBigDecimal("кол.").compareTo(BigDecimal.ZERO) == 0)
          dp.setValue("кол.", dp.getBigDecimal("amount"));
        if(!(boolean)newValue && dp.getBigDecimal("кол.").compareTo(BigDecimal.ZERO) != 0)
          dp.setValue("кол.", BigDecimal.ZERO);
      });

      dp.get("кол.").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
        if(newValue == null)
          dp.setValue("кол.", BigDecimal.ZERO);
        else {
          if(((BigDecimal)newValue).compareTo(BigDecimal.ZERO) > 0) {
            if(((BigDecimal)newValue).compareTo(dp.getBigDecimal("amount")) > 0)
              dp.setValue("кол.", dp.getValue("amount"));
            if(!dp.is("выбрать"))
              dp.setValue("выбрать", true);
          }else if(dp.is("выбрать"))
            dp.setValue("выбрать", false);
        }
      });
    });
    return table;
  }
}