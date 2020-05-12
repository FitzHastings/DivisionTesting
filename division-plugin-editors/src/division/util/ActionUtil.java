package division.util;

import bum.editors.EditorController;
import bum.editors.util.ObjectLoader;
import bum.interfaces.*;
import bum.interfaces.ProductDocument.ActionType;
import division.fx.PropertyMap;
import division.fx.dialog.FXD;
import division.fx.dialog.FXDialog;
import division.fx.editor.GLoader;
import division.fx.table.Column;
import division.fx.table.FXDivisionTable;
import division.fx.util.MsgTrash;
import division.swing.guimessanger.Messanger;
import documents.FOP;
import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class ActionUtil{
  public static Entry entry(Object key, Object value) {
    return new AbstractMap.SimpleEntry(key, value);
  }
  
  public static void runAction(ActionType actionType, Entry<String,Object>... param) throws Exception {
    Executors.newSingleThreadExecutor().submit(() -> {
      try {
        Map<String,Object> scriptData = new TreeMap<>();
        for(Entry<String,Object> en:param)
          scriptData.put(en.getKey(), en.getValue());
        scriptData.put("actionType", actionType);
        String scriptName = null;
        switch(actionType) {
          case СТАРТ   : scriptName = "StartDeals";break;
          case ОТГРУЗКА: scriptName = "DispatchDeals";break;
          case ОПЛАТА  : scriptName = "PayDeals";break;
        }
        ScriptUtil.runGroovyClass(scriptName, scriptData);
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    });
  }
  
  private static Date getEndDate(Date dealStartDate, String type, long count) {
    Calendar c = Calendar.getInstance();
    c.setTime(dealStartDate);
    long days = 0;
    if(type.startsWith("м")) {
      days = c.getActualMaximum(Calendar.DAY_OF_MONTH) - c.get(Calendar.DAY_OF_MONTH);
      for(int i=0;i<count-1;i++) {
        c.set(Calendar.DAY_OF_MONTH, 1);
        if(c.get(Calendar.MONTH)==11) {
          c.set(Calendar.YEAR, c.get(Calendar.YEAR)+1);
          c.set(Calendar.MONTH,0);
        }else  c.set(Calendar.MONTH, c.get(Calendar.MONTH)+1);
        days += c.getActualMaximum(Calendar.DAY_OF_MONTH);
      }
    }else if(type.startsWith("д")) {
      days = count;
    }else if(type.startsWith("л") || type.startsWith("г")) {
      days = c.getActualMaximum(Calendar.DAY_OF_YEAR) - c.get(Calendar.DAY_OF_YEAR);
      for(int i=0;i<count-1;i++) {
        c.set(Calendar.YEAR, c.get(Calendar.YEAR)+1);
        days += c.getActualMaximum(Calendar.DAY_OF_MONTH);
      }
    }
    c.setTimeInMillis(dealStartDate.getTime()+(days*24*60*60*1000));

    return new Date(c.getTimeInMillis());
  }
  
  private static Date getNextStart(Date previosDate, String rType, int rCount) {
    Calendar c = Calendar.getInstance();
    c.setTime(previosDate);
    if(rType.startsWith("м")) {
      c.set(Calendar.DAY_OF_MONTH, 1);
      for(int i=0;i<rCount;i++) {
        if(c.get(Calendar.MONTH)==11) {
          c.set(Calendar.YEAR, c.get(Calendar.YEAR)+1);
          c.set(Calendar.MONTH,0);
        }else  c.set(Calendar.MONTH, c.get(Calendar.MONTH)+1);
      }
    }else if(rType.startsWith("д")) {
      c.setTimeInMillis(c.getTimeInMillis()+((long)rCount*24*60*60*1000));
    }else if(rType.startsWith("л") || rType.startsWith("г")) {
      for(int i=0;i<rCount;i++) {
        c.set(c.get(Calendar.YEAR)+1, 0, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
      }
    }
    return c.getTime();
  }

  /*public static void startDocumentsScript(
          ProductDocument.ActionType actionType, 
          Date date,
          Integer paymentId, 
          //Integer[] dealIds, 
          Integer[] dealPositions, 
          LocalProcessing processing, 
          RemoteSession session,
          StoreType storeType,
          ObjectType storeObjectType,
          Map<Integer, Entry<String, Date>> documentNumbers) throws Exception {
    
    //Получаем документы, которые относятся к данному событию и данным продуктам.
    Boolean moneyCash = null, moneyCashLess = null, tmcCash = null, tmcCashLess = null;
    if(storeType != null && storeObjectType != null) {
      moneyCash     = storeType == StoreType.НАЛИЧНЫЙ    && storeObjectType == ObjectType.ВАЛЮТА;
      moneyCashLess = storeType == StoreType.БЕЗНАЛИЧНЫЙ && storeObjectType == ObjectType.ВАЛЮТА;
      tmcCash       = storeType == StoreType.НАЛИЧНЫЙ    && storeObjectType == ObjectType.ТМЦ;
      tmcCashLess   = storeType == StoreType.БЕЗНАЛИЧНЫЙ && storeObjectType == ObjectType.ТМЦ;
    }
    
    Vector<Vector> documents = session.executeQuery("SELECT "
            /*0*+ "[Document(id)],"
            /*1*+ "[Document(script)],"
            /*2*+ "[Document(name)],"
            /*3*+ "[Document(system)],"
            /*4*+ "[Document(sellerDocumentSource)],"
            /*5*+ "[Document(customerDocumentSource)], "
            /*6*+ "[Document(sellerObjectSource)], "
            /*7*+ "[Document(customerObjectSource)] "
            + "FROM [Document] "
            + "WHERE [Document(tmp)]=false AND [Document(type)]='CURRENT' AND "
            + "([Document(system)]=true AND [Document(actionType)]='"+actionType+"' "
            + "AND ([Document(sellerObjectSource)]=true OR [Document(customerObjectSource)]=true) "
            + "AND ([Document(sellerDocumentSource)]=true OR [Document(customerDocumentSource)]=true)"
            + (moneyCash     != null?" AND [Document(moneyCash)]="    +moneyCash:"")
            + (moneyCashLess != null?" AND [Document(moneyCashLess)]="+moneyCashLess:"")
            + (tmcCash       != null?" AND [Document(tmcCash)]="      +tmcCash:"")
            + (tmcCashLess   != null?" AND [Document(tmcCashLess)]="  +tmcCashLess:"")
            
            + " OR "
            
            + "[Document(id)] IN "
            + "(SELECT [ProductDocument(document)] FROM [ProductDocument] WHERE tmp=false AND type='CURRENT' AND "
            + "[ProductDocument(actionType)]='"+actionType+"' AND [ProductDocument(product)] IN "
            + "(SELECT [DealPosition(product)] FROM [DealPosition] WHERE id=ANY(?))))", new Object[]{dealPositions});
    
    if(!documents.isEmpty()) {
      String[] moduls = new String[0];
      Vector<Vector> data = session.getData(JSModul.class, DBFilter.create(JSModul.class), new String[]{"script"});
      for(Vector d:data)
        moduls = (String[]) ArrayUtils.add(moduls, d.get(0));

      for(Vector doc:documents) {
        String script = (String) doc.get(1);
        if(script != null && !script.equals("")) {
          Integer documentId      = (Integer)doc.get(0);
          String  documentName    = (String) doc.get(2);
          // Для данного типа документа выбираем список позиций исходя из тех продуктов,
          // к которым привязан этот тип.
          String orderby = "";
          if(actionType == ProductDocument.ActionType.СТАРТ)
            orderby = " ORDER BY [DealPosition(deal_start_date)]";
          else orderby = " ORDER BY [DealPosition(deal_end_date)]";
          
          data = session.executeQuery("SELECT "
                  + "[DealPosition(contract_id)], "
                  + "[DealPosition(deal)],"
                  + "[DealPosition(id)] "
                  + "FROM [DealPosition] "
                  + "WHERE [DealPosition(id)]=ANY(?)"
                  + (!(boolean)doc.get(3)?(" AND [DealPosition(product)] IN "
                  + "(SELECT [ProductDocument(product)] FROM [ProductDocument] WHERE [ProductDocument(document)]="+doc.get(0)
                  +" AND [ProductDocument(actionType)]='"+actionType+"')"):(doc.get(6).equals(doc.get(7))?"":(" AND (SELECT [Service(owner)] FROM [Service] WHERE [Service(id)]=(SELECT [Product(service)] FROM [Product] WHERE [Product(id)]=[DealPosition(product)]))='"+((boolean)doc.get(6)?"SELLER":"CUSTOMER")+"'")))
                  + orderby, new Object[]{dealPositions});
          
          //if(!data.isEmpty()) {
            String documentNumber = null;
            Date   documentDate   = null;
            
            if(documentNumbers != null && documentNumbers.containsKey(documentId)) {
              documentNumber = documentNumbers.get(documentId).getKey();
              documentDate   = documentNumbers.get(documentId).getValue();
            }
            
            Integer[] deals = new Integer[0];
            Integer[] dealPositionIds = new Integer[0];
            //Упаковываем позиции соответственно договорам и сделкам
            TreeMap<Integer, TreeMap<Integer, List<Integer>>> contractDealDealPosition = new TreeMap<>();
            for(Vector d:data) {
              TreeMap<Integer, List<Integer>> dealDealPosition = contractDealDealPosition.get(d.get(0));
              if(dealDealPosition == null) {
                dealDealPosition = new TreeMap<>();
                contractDealDealPosition.put((Integer) d.get(0), dealDealPosition);
              }

              List<Integer> listDealPositions = dealDealPosition.get(d.get(1));
              if(listDealPositions == null) {
                listDealPositions = new ArrayList<>();
                dealDealPosition.put((Integer) d.get(1), listDealPositions);
              }

              if(!ArrayUtils.contains(deals, d.get(1)))
                deals = (Integer[]) ArrayUtils.add(deals, d.get(1));
              
              if(!ArrayUtils.contains(dealPositionIds, d.get(2)))
                dealPositionIds = (Integer[]) ArrayUtils.add(dealPositionIds, d.get(2));

              listDealPositions.add((Integer) d.get(2));
            }

            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName("JavaScript");
            Invocable invocable = (Invocable)engine;
            
            for(String modul:moduls) {
              try {
                engine.eval(modul);
              }catch(Exception ex) {throw new Exception("ошибка в модуле", new Exception(modul, ex));}
            }
            try {
              engine.eval(script);
            }catch(Exception ex) {throw new Exception("ошибка в скрипте", new Exception(script, ex));}
            
            engine.put("paymentId", paymentId);
            
            GroovyShell shell = new GroovyShell();
            shell.parse("").invokeMethod("", "");
            
            boolean isContinue = true;
            try {
              isContinue = (boolean)invocable.invokeFunction("beforeSplitDocument", documentId, dealPositionIds, session);
            }catch(NoSuchMethodException ex){}
            
            if(isContinue) {
              boolean isGroup = false;
              if(deals.length > 1)
                isGroup = JOptionPane.showConfirmDialog(null,"Объединять документы по периодам \""+documentName+"\"?","!!!",
                        JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE) == 0;

              if(isGroup) {
                if(processing != null) {
                  processing.setSuibMinMax(0, 2);
                  processing.setSubValue(1);
                }
                for(TreeMap<Integer,List<Integer>> dealDealPositions:contractDealDealPosition.values()) {
                  List<Integer> list = new ArrayList<>();
                  for(List<Integer> l:dealDealPositions.values()) {
                    list.addAll(l);
                  }

                  createDocument(list.toArray(new Integer[0]), actionType, documentNumber,
                          documentDate==null?(date==null?getDocumentDate(list.toArray(new Integer[0]), actionType, session):new Timestamp(date.getTime())):new Timestamp(documentDate.getTime()), 
                          documentId, paymentId, invocable, processing, session);
                }
                if(processing != null)
                  processing.setSubValue(2);
              }else {
                if(!contractDealDealPosition.isEmpty()) {
                  for(TreeMap<Integer,List<Integer>> dealDealPositions:contractDealDealPosition.values()) {
                    if(processing != null)
                      processing.setSuibMinMax(0, dealDealPositions.size());
                    for(List<Integer> list:dealDealPositions.values()) {
                      createDocument(list.toArray(new Integer[0]), actionType, documentNumber, 
                              documentDate==null?(date==null?getDocumentDate(list.toArray(new Integer[0]), actionType, session):new Timestamp(date.getTime())):new Timestamp(documentDate.getTime()), 
                              documentId, paymentId, invocable, processing, session);
                      if(processing != null)
                        processing.setSubValue(processing.getSubValue()+1);
                    }
                  }
                }else if((boolean)doc.get(3)) {
                  createDocument(new Integer[0], actionType, documentNumber, 
                          documentDate==null?(date==null?null:new Timestamp(date.getTime())):new Timestamp(documentDate.getTime()), 
                          documentId, paymentId, invocable, processing, session);
                }
              }
            }
          //}
        }
      }
    }
  }*/
  
  
  
  
  
  
  
  
  
  public static void startDeals(List<Integer> deals) {
    ObservableList<PropertyMap> dealPositions = ObjectLoader.getList(DBFilter.create(DealPosition.class).AND_IN("deal", deals.toArray(new Integer[0])).AND_EQUAL("startDate", null), "id","deal","amount");
    if(dealPositions.isEmpty())
      new Alert(Alert.AlertType.WARNING, "Данн"+(deals.size()==1?"ая":"ые")+" сделк"+(deals.size()==1?"а":"и")+" полностью стартонул"+(deals.size()==1?"а":"и"), ButtonType.OK).showAndWait();
    else {
      Platform.runLater(() -> {
        DocumentCreator documentCreator = new DocumentCreator();
        documentCreator.init(ProductDocument.ActionType.СТАРТ, PropertyMap.getListFromList(dealPositions, "id", Integer.TYPE).toArray(new Integer[0]));
        if(documentCreator.customUpdateDocuments(deals.size() > 1)) {
          RemoteSession session = null;
          try {
            session = ObjectLoader.createSession(false);
            session.executeUpdate("UPDATE [DealPosition] SET "
                    + "[DealPosition(startId)]=(SELECT MAX([DealPosition(startId)])+1 FROM [DealPosition]), "
                    + "[DealPosition(startDate)]=CURRENT_TIMESTAMP "
                    + "WHERE [DealPosition(id)]=ANY(?)", new Object[]{PropertyMap.getListFromList(dealPositions, "id", Integer.TYPE).toArray(new Integer[0])});
            documentCreator.start(session, null, null);
            session.addEvent(DealPosition.class, "UPDATE", PropertyMap.getListFromList(dealPositions, "id", Integer.TYPE).toArray(new Integer[0]));
            session.addEvent(Deal.class, "UPDATE", deals.toArray(new Integer[0]));
            ObjectLoader.commitSession(session);
          } catch (Exception ex) {
            ObjectLoader.rollBackSession(session);
            MsgTrash.out(ex);
          }
        }
      });
    }
  }
  
  public static void dispatchDeals(List<Integer> deals) {
    //Получить неотгруженные и стартонувшие позиции из сделок
    ObservableList<PropertyMap> dealPositions = ObjectLoader.getList(DBFilter.create(DealPosition.class).AND_IN("deal", deals.toArray(new Integer[0])).AND_EQUAL("dispatchDate", null).AND_NOT_EQUAL("startDate", null),
        "id",
        "equipment",
        "sourceStore",
        "source-store-controll-out",
        "targetStore",
        "deal",
        "product",
        "startDate",
        "startId",
        "group_name",
        "identity_name",
        "identity_value",
        "amount",
        "кол.:=:amount",
        "customProductCost",
        "cost=query:customProductCost*amount",
        "выбрать=query:true",
        "storeType:=:target-store-type",
        "storeObjectType:=:target-store-object-type",
        "move=query:SELECT [Service(moveStorePosition)] FROM [Service] WHERE [Service(id)]=(SELECT [Product(service)] FROM [Product] WHERE [Product(id)]=[DealPosition(product)])");
    if(dealPositions.isEmpty())
      new Alert(Alert.AlertType.WARNING, "Данн"+(deals.size()==1?"ая":"ые")+" сделк"+(deals.size()==1?"а":"и")+" полностью отгруже"+(deals.size()==1?"а":"ы"), ButtonType.OK).showAndWait();
    else {
      Platform.runLater(() -> {
        if(deals.size() == 1) { // Если отгрузка только одной сделки, то нужно выбрать какие позиции и сколько отгружать
          FXDivisionTable<PropertyMap> table = createTableForSelectDealPositions(dealPositions);
          
          FXD fxd = FXD.create("", table, FXD.ButtonType.OK, FXD.ButtonType.CANCEL);
          fxd.setOnShowing(e -> GLoader.load("select-deal-positions", table));
          fxd.setOnHiding(e -> GLoader.store("select-deal-positions", table));
          if(Optional.ofNullable(fxd.showDialog()).orElseGet(() -> FXD.ButtonType.CANCEL) == FXD.ButtonType.CANCEL)
            dealPositions.stream().forEach(dp -> dp.setValue("выбрать",false));
        }
        
        dealPositions.stream().forEach(dp -> System.out.println(dp.getSimpleMap()));
        
        ObservableList<PropertyMap> dps = PropertyMap.copyList(dealPositions.filtered(dp -> dp.getValue("выбрать", true)));
        dealPositions.clear();
        if(!dps.isEmpty()) {
          DocumentCreator documentCreator = new DocumentCreator();
          documentCreator.init(ProductDocument.ActionType.ОТГРУЗКА, PropertyMap.getListFromList(dps, "id", Integer.TYPE).toArray(new Integer[0]));
          if(documentCreator.customUpdateDocuments(deals.size() > 1)) {
            RemoteSession session = null;
            try {
              session = ObjectLoader.createSession(false);
              for(PropertyMap dp:dps) {
                if(dp.getBigDecimal("кол.").compareTo(dp.getBigDecimal("amount")) < 0) {
                  PropertyMap dpnew = dp.copy().setValue("amount", dp.getValue("кол."));
                  session.executeUpdate("INSERT INTO [DealPosition] ("
                          + "[DealPosition(amount)],"
                          + "[DealPosition(equipment)],"
                          + "[DealPosition(deal)],"
                          + "[DealPosition(product)],"
                          + "[DealPosition(customProductCost)],"
                          + "[DealPosition(startDate)],"
                          + "[DealPosition(startId)]) "
                          + "VALUES ("+dp.getBigDecimal("кол.")+","+dp.getInteger("equipment")+","+dp.getInteger("deal")+","+dp.getInteger("product")+","+dp.getBigDecimal("customProductCost")+",'"+dp.getTimestamp("startDate")+"',"+dp.getLong("startId")+")");
                  dpnew.setValue("id", session.executeQuery("SELECT MAX(id) FROM [DealPosition]").get(0).get(0));
                  session.executeUpdate("UPDATE [DealPosition] SET [DealPosition(amount)]="+dp.getBigDecimal("amount").subtract(dp.getBigDecimal("кол."))+" WHERE id="+dp.getInteger("id"));
                  
                  for(List doc:session.executeQuery("SELECT [CreatedDocument(dealPositions):object] FROM [CreatedDocument(dealPositions):table] WHERE [CreatedDocument(dealPositions):target]="+dp.getInteger("id"))) {
                    session.executeUpdate("INSERT INTO [CreatedDocument(dealPositions):table] ("
                            + "[CreatedDocument(dealPositions):object],"
                            + "[CreatedDocument(dealPositions):target]"
                            + ") VALUES ("+doc.get(0)+","+dpnew.getInteger("id")+")");
                  }
                  session.addEvent(DealPosition.class, "CREATE", dpnew.getInteger("id"));
                  session.addEvent(DealPosition.class, "UPDATE", dp.getInteger("id"));
                  dealPositions.add(dpnew);
                }else dealPositions.add(dp);
              }
              
              Map<Integer,List> createdDocuments = documentCreator.start(session);
              
              Long dispatchId = (Long) session.executeQuery("SELECT MAX([DealPosition(dispatchId)])+1 FROM [DealPosition]").get(0).get(0);
              List<String> querys = FXCollections.observableArrayList();
              List<Object[]> params = FXCollections.observableArrayList();
              
              for(PropertyMap dp:dealPositions) {
                List<Integer> docs = createdDocuments.get(dp);
                querys.add("UPDATE [DealPosition] SET [DealPosition(dispatchId)]="+dispatchId+", [DealPosition(dispatchDate)]=CURRENT_TIMESTAMP, "
                        + "[DealPosition(actionDispatchDate)]="+(docs == null || docs.isEmpty() ? "CURRENT_DATE" : "(CASE WHEN (SELECT MAX([CreatedDocument(date)]) FROM [CreatedDocument] WHERE id=ANY(?)) ISNULL THEN CURRENT_DATE ELSE (SELECT MAX([CreatedDocument(date)]) FROM [CreatedDocument] WHERE id=ANY(?)) END) ")
                        + "WHERE [DealPosition(id)]="+dp.getInteger("id"));
                params.add(new Object[]{docs.toArray(new Integer[0]), docs.toArray(new Integer[0])});
                
                if(dp.isNotNull("equipment") && dp.is("move") && dp.is("source-store-controll-out")) {
                  querys.add("UPDATE [Equipment] SET "
                          + "[Equipment(store)]="+dp.getInteger("targetStore")+","
                          + " "
                          + "WHERE [Equipment(id)]="+dp.getInteger("equipment"));
                }
              }
              
              /*createdDocuments.forEach((dp,docs) -> {
                querys.add("UPDATE [DealPosition] SET "
                        + "[DealPosition(dispatchId)]=?, "
                        + "[DealPosition(dispatchDate)]=CURRENT_TIMESTAMP, "
                        + "[DealPosition(actionDispatchDate)]="
                        + "(CASE WHEN (SELECT MAX([CreatedDocument(date)]) FROM [CreatedDocument] WHERE id=ANY(?)) ISNULL THEN CURRENT_DATE ELSE (SELECT MAX([CreatedDocument(date)]) FROM [CreatedDocument] WHERE id=ANY(?)) END) "
                        + "WHERE [DealPosition(id)]=?");
                params.add(new Object[]{dispatchId, docs.toArray(new Integer[0]), docs.toArray(new Integer[0]), dp});
              });*/
              
              Object[][] pp = null;
              for(Object[] p:params)
                pp = org.apache.commons.lang3.ArrayUtils.add(pp, p);
              
              session.executeUpdate(querys.toArray(new String[0]), pp);
              session.addEvent(DealPosition.class, "UPDATE", PropertyMap.getListFromList(dealPositions, "id", Integer.TYPE).toArray(new Integer[0]));
              session.addEvent(Deal.class, "UPDATE", deals.toArray(new Integer[0]));
              
              ObjectLoader.commitSession(session);
            } catch (Exception ex) {
              ObjectLoader.rollBackSession(session);
              MsgTrash.out(ex);
            }
          }
        }
      });
    }
  }
  
  private static FXDivisionTable<PropertyMap> createTableForSelectDealPositions(ObservableList<PropertyMap> dealPositions) {
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
    table.setItems(dealPositions);

    dealPositions.stream().forEach(dp -> {

      dp.get("выбрать").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
        if((boolean)newValue && dp.getValue("кол.", BigDecimal.class).compareTo(BigDecimal.ZERO) == 0)
          dp.setValue("кол.", dp.getValue("amount"));
        if(!(boolean)newValue && dp.getValue("кол.", BigDecimal.class).compareTo(BigDecimal.ZERO) != 0)
          dp.setValue("кол.", BigDecimal.ZERO);
      });

      dp.get("кол.").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
        if(newValue == null)
          dp.setValue("кол.", BigDecimal.ZERO);
        else {
          if(((BigDecimal)newValue).compareTo(BigDecimal.ZERO) > 0) {
            if(((BigDecimal)newValue).compareTo(dp.getValue("amount", BigDecimal.class)) > 0)
              dp.setValue("кол.", dp.getValue("amount"));
            if(!dp.getValue("выбрать", boolean.class))
              dp.setValue("выбрать", true);
          }else if(dp.getValue("выбрать", boolean.class))
            dp.setValue("выбрать", false);
        }
      });
    });
    return table;
  }
  
  
  
  
  
  
  
  
  
  
  
  public static String generateXML(String[] moduls, Integer documentId, Integer templateId, Integer createdDocumentId, Map<String, Object> params) throws Exception {
    String xmlTemplate = null;
    List<List> data;
    if(templateId == null) {
      data = ObjectLoader.getData(DBFilter.create(DocumentXMLTemplate.class).AND_EQUAL("document", documentId), true, new String[]{"XML","main"});
    }else {
      data = ObjectLoader.getData(DocumentXMLTemplate.class, true, new Integer[]{templateId}, new String[]{"XML","main"});
    }
    if(data.isEmpty())
      return null;
    for(List d:data)
      if((Boolean)d.get(1))
        xmlTemplate = (String)d.get(0);
    if(xmlTemplate == null)
      xmlTemplate = (String)data.get(0).get(0);
    
    return generateXML(moduls, documentId, xmlTemplate, createdDocumentId, params);
  }
  
  public static String generateXML(String[] moduls, Integer documentId, String xmlTemplate, Integer createdDocumentId, Map<String, Object> params) throws Exception {
    List<List> data;
    if(xmlTemplate == null) {
      data = ObjectLoader.getData(DBFilter.create(DocumentXMLTemplate.class).AND_EQUAL("document", documentId), true, new String[]{"XML","main"});
      if(data.isEmpty())
        return null;
      for(List d:data)
        if((Boolean)d.get(1))
          xmlTemplate = (String)d.get(0);
      if(xmlTemplate == null)
        xmlTemplate = (String)data.get(0).get(0);
    }
    
    params.put("id", createdDocumentId);
    return FOP.get_XML_From_XMLTemplate(xmlTemplate, Arrays.asList(moduls), params);
  }
  
  public static List<List> getDocumentsForDealPositions(Integer[] dealPositions, RemoteSession session) throws Exception {
    return session.executeQuery("SELECT "
            + "[CreatedDocument(dealPositions):object], "
            + "[CreatedDocument(dealPositions):target],"
            + "(SELECT [CreatedDocument(exportType)] FROM [CreatedDocument] WHERE id=[CreatedDocument(dealPositions):object]), "
            + "(SELECT [CreatedDocument(sendType)] FROM [CreatedDocument] WHERE id=[CreatedDocument(dealPositions):object]), "
            + "(SELECT [CreatedDocument(document)] FROM [CreatedDocument] WHERE id=[CreatedDocument(dealPositions):object]), "
            + "(SELECT [CreatedDocument(name)] FROM [CreatedDocument] WHERE id=[CreatedDocument(dealPositions):object]), "
            + "(SELECT [CreatedDocument(number)] FROM [CreatedDocument] WHERE id=[CreatedDocument(dealPositions):object]) "
            + "FROM [CreatedDocument(dealPositions):table] WHERE "
            + "(SELECT tmp FROM [CreatedDocument] WHERE id=[CreatedDocument(dealPositions):object])=false AND "
            + "(SELECT type FROM [CreatedDocument] WHERE id=[CreatedDocument(dealPositions):object])='CURRENT' AND "
            + "(SELECT [CreatedDocument(stornoDate)] FROM [CreatedDocument] WHERE id=[CreatedDocument(dealPositions):object]) ISNULL AND "
            + "[CreatedDocument(dealPositions):object] IN "
            + "(SELECT DISTINCT [CreatedDocument(dealPositions):object] FROM [CreatedDocument(dealPositions):table] "
            + "WHERE [CreatedDocument(dealPositions):target]=ANY(?))", new Object[]{dealPositions});
  }
  
  private static Integer[] getDealsFromDealPositions(Integer[] dealPositions, RemoteSession session) throws Exception {
    Integer[] deals = new Integer[0];
    List<List> data = session.executeQuery("SELECT DISTINCT [DealPosition(deal)] FROM [DealPosition] WHERE id=ANY(?)",new Object[]{dealPositions});
    for(List d:data)
      deals = (Integer[]) ArrayUtils.add(deals, d.get(0));
    return deals;
  }
  
  /**
   * При удалении позиций правим документы
   * @param removePositions
   * @param session
   * @throws Exception 
   */
  public static void removeDealPositions(
          Integer[] removePositions, 
          RemoteSession session) throws Exception {
    boolean isMySession = session == null;
    EditorController.waitCursor();
    try {
      if(isMySession)
        session = ObjectLoader.createSession();
      //Удаляем отгрузки
      removeDispatch(removePositions, session);
      //Получаем сделки, в которых удаляются позиции
      Integer[] updateDeals = getDealsFromDealPositions(removePositions, session);
      //Удаляем позиции
      session.executeUpdate("DELETE FROM [DealPosition] WHERE id=ANY(?)", new Object[]{removePositions});
      //Возможно что быстрее это делать отдельно, но пока так.
      //Удаляем сделки у которых нет позиций
      Integer[] removeDeals = new Integer[0];
      List<List> dealData = session.executeQuery("SELECT id FROM [Deal] "
              + "WHERE (SELECT COUNT([DealPosition(id)]) FROM [DealPosition] WHERE [DealPosition(deal)]=[Deal(id)])=0");
      for(List d:dealData) {
        removeDeals = (Integer[]) ArrayUtils.add(removeDeals, d.get(0));
        updateDeals = (Integer[]) ArrayUtils.removeElement(updateDeals, d.get(0));
      }
      if(removeDeals.length > 0) {
        session.executeUpdate("DELETE FROM [Deal] WHERE id=ANY(?)", new Object[]{removeDeals});
        session.addEvent(Deal.class, "REMOVE", removeDeals);
      }
      if(updateDeals.length > 0)
        session.addEvent(Deal.class, "UPDATE", updateDeals);
      
      session.addEvent(DealPosition.class, "REMOVE", removePositions);
      //Перезаписать документы
      List<Integer> dps = new ArrayList<>();
      session.executeQuery("SELECT id FROM [DealPosition] WHERE [DealPosition(deal)]=ANY(?)", new Object[]{updateDeals}).stream().forEach(d -> dps.add((Integer)d.get(0)));
      replaceDocuments(getDocuments(dps.toArray(new Integer[0]), null, session, ActionType.СТАРТ, ActionType.ОТГРУЗКА, ActionType.ОПЛАТА), session);
      if(isMySession)
        session.commit();
    }catch(Exception ex) {
      if(isMySession)
        ObjectLoader.rollBackSession(session);
      throw new Exception(ex);
    }finally {
      EditorController.defaultCursor();
    }
  }
  
  public static void replaceDocuments(Integer[] createdDocuments, RemoteSession session) throws Exception {
    Map<String, Object> param = new TreeMap<>();
    param.put("createdDocuments",  createdDocuments);
    param.put("session",    session);
    ScriptUtil.runGroovyClass("ReplaceDocuments", param);
  }
  
  private static void removeAction(ActionType actionType, Integer[] dealPositions, RemoteSession session) throws Exception {
    Integer[] createdDocuments = getDocuments(dealPositions, null, session, actionType);
    if(createdDocuments.length > 0) {
      session.executeUpdate("DELETE FROM [CreatedDocument(dealPositions):table] WHERE [CreatedDocument(dealPositions):target]=ANY(?) AND "
              + "[CreatedDocument(dealPositions):object]=ANY(?)", new Object[]{dealPositions, createdDocuments});
      
      session.executeUpdate("DELETE FROM [CreatedDocument] WHERE id=ANY(?) AND (SELECT COUNT([CreatedDocument(dealPositions):target]) FROM [CreatedDocument(dealPositions):table] "
              + "WHERE [CreatedDocument(dealPositions):object]=[CreatedDocument(id)])=0", new Object[]{createdDocuments});
      List<Integer> replaceDocuments = new ArrayList<>();
      session.executeQuery("SELECT id FROM [CreatedDocument] WHERE id=ANY(?)", new Object[]{createdDocuments}).stream()
              .forEach(d -> replaceDocuments.add((Integer)d.get(0)));
      if(!replaceDocuments.isEmpty())
        ActionUtil.replaceDocuments(replaceDocuments.toArray(new Integer[0]), session);
    }
  }
  
  public static void removeStart(Integer[] dealPositions, RemoteSession session) throws Exception {
    session.executeUpdate("UPDATE [DealPosition] SET [DealPosition(startDate)]=NULL, [DealPosition(startId)]=NULL "
            + "WHERE [DealPosition(id)]=ANY(?)", new Object[]{dealPositions});
    removeAction(ActionType.СТАРТ, dealPositions, session);
  }
  
  public static void removeDispatch(Integer[] dealPositions, RemoteSession session) throws Exception {
    session.executeUpdate("UPDATE [DealPosition] SET [DealPosition(dispatchDate)]=NULL, [DealPosition(dispatchId)]=NULL, "
            + "[DealPosition(actionDispatchDate)]=NULL WHERE [DealPosition(id)]=ANY(?)", new Object[]{dealPositions});
    removeAction(ActionType.ОТГРУЗКА, dealPositions, session);
    
    Map<String, Object> param = new TreeMap<>();
    param.put("dealPositions",    dealPositions);
    param.put("session",  session);
    ScriptUtil.runGroovyClass("removeDispatch", param);
  }
  
  public static void rePayDeals(Integer paymentId, Map<Integer,BigDecimal> dealAmounts, RemoteSession session) throws Exception {
    // Удалить старые оплаты
    String[] querys = new String[]{"DELETE FROM [DealPayment] WHERE [DealPayment(payment)]="+paymentId};
    // Создать оплаты
    for(Integer deal:dealAmounts.keySet())
      querys = (String[]) ArrayUtils.add(querys, "INSERT INTO [DealPayment]([DealPayment(deal)], [DealPayment(payment)], [DealPayment(amount)]) VALUES("+deal +","+paymentId+","+dealAmounts.get(deal)+")");
    session.executeUpdate(querys);
    session.addEvent(Deal.class, "UPDATE", dealAmounts.keySet().toArray(new Integer[0]));
    replaceDocuments(getDocuments(null, paymentId, session, ActionType.ОПЛАТА), session);
  }
  
  public static Integer[] getDocuments(Integer[] dealPositions, Integer payment, RemoteSession session, ActionType... actionTypes) throws Exception {
    final ArrayList<Integer> ids = new ArrayList<>();
    for(ActionType actionType:actionTypes) {
      if(payment != null)
        session.executeQuery("SELECT id FROM [CreatedDocument] WHERE [CreatedDocument(actionType)]=? AND [CreatedDocument(payment)]=? AND "
                + "tmp=false AND type='CURRENT'", new Object[]{ActionType.ОПЛАТА, payment}).forEach(d -> ids.add((Integer) d.get(0)));
      if(dealPositions != null && dealPositions.length > 0)
        session.executeQuery("SELECT id FROM [CreatedDocument] WHERE [CreatedDocument(actionType)]=? AND "
                + "id IN (SELECT [CreatedDocument(dealPositions):object] FROM [CreatedDocument(dealPositions):table] WHERE [CreatedDocument(dealPositions):target]=ANY(?))",
                new Object[]{actionType, dealPositions}).forEach(d -> ids.add((Integer) d.get(0)));
    }
    return ids.toArray(new Integer[0]);
  }
  
  public static void removePayment(List<Integer> payments, RemoteSession session) throws Exception {
    Map<String, Object> param = new TreeMap<>();
    param.put("payments", payments);
    param.put("session",  session);
    ScriptUtil.runGroovyClass("removePayment", param);
  }
}