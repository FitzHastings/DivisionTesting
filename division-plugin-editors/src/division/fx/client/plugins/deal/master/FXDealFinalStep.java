package division.fx.client.plugins.deal.master;

import bum.interfaces.CFC;
import bum.interfaces.Company;
import bum.interfaces.CompanyPartition;
import bum.interfaces.Contract;
import bum.interfaces.ContractProcess;
import bum.interfaces.Deal;
import bum.interfaces.DealPosition;
import bum.interfaces.Equipment;
import bum.interfaces.Group;
import bum.interfaces.Product;
import bum.interfaces.Store;
import client.util.ObjectLoader;
import division.fx.DateLabel.DateLabel;
import division.fx.PropertyMap;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.controller.company.FXCompanySelector;
import division.fx.controller.store.EquipmentFactorTable;
import division.fx.dialog.FXD;
import division.fx.table.Column;
import division.fx.util.MsgTrash;
import division.util.IDStore;
import division.util.Utility;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.util.converter.BigDecimalStringConverter;
import javafx.util.converter.IntegerStringConverter;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class FXDealFinalStep extends Step {
  private final PropertyMap deal;
  
  private final DateLabel startDealDateLabel = new DateLabel("Начать с ", LocalDate.now());
  private final Label     endDealDateLabel   = new Label();
  
  private final Spinner<Integer>    durCount   = createSpinner();
  private final ChoiceBox<String>   durType    = new ChoiceBox<>(FXCollections.observableArrayList(Utility.getPeriodTypes(durCount.getValue())));
  private final TitleBorderPane     durBorder  = new TitleBorderPane(new VBox(5, new HBox(5, durCount, durType), endDealDateLabel), "Длительность");
  
  private final CheckBox            copyCheck     = new CheckBox("Запланировать");
  private final RadioButton         contractRadio = new RadioButton("До конца договора");
  private final RadioButton         countRadio    = new RadioButton("Циклов");
  private final Spinner<Integer>    countCopy     = new Spinner<>(1, Integer.MAX_VALUE, 1, 1);
  private final ToggleGroup         toggleGroup   = new ToggleGroup();
  private final TitleBorderPane     copyBorder    = new TitleBorderPane(new VBox(5, contractRadio, new HBox(5, countRadio, countCopy)), copyCheck);
  
  private final CheckBox            recCheck   = new CheckBox("Цикличность");
  private final Spinner<Integer>    recCount   = createSpinner();
  private final ChoiceBox<String>   recType    = new ChoiceBox<>(FXCollections.observableArrayList(Utility.getPeriodTypes(recCount.getValue())));
  private final TitleBorderPane     recBorder  = new TitleBorderPane(new VBox(5, new HBox(5, recCount, recType), copyBorder), recCheck);
  
  private final Label             processLabel  = new Label();
  
  private final FXCompanySelector sellerSelector   = new FXCompanySelector("Продавец", false, false, false, false, true, true);
  private final FXCompanySelector customerSelector = new FXCompanySelector("Покупатель", false, false, false, false, true, true);
  private final GridPane          companyBox       = new GridPane();
  
  private final EquipmentFactorTable positionTable  = new EquipmentFactorTable();
  private final TitleBorderPane      positionBorder = new TitleBorderPane(positionTable, "Позиции сделки");

  public FXDealFinalStep(String title, PropertyMap deal) {
    super(title);
    this.deal = deal;
    
    positionTable.getTable().setRowFactory((TableView<PropertyMap> param) -> {
      TableRow row = new TableRow() {
        @Override
        protected void updateItem(Object item, boolean empty) {
          getStyleClass().remove("zakaz-row");
          if(!empty && ((PropertyMap)item).isNotNull("equipment-id") && ((PropertyMap)item).getInteger("equipment-id") < 0)
            getStyleClass().add("zakaz-row");
          super.updateItem(item, empty);
        }
      };
      return row;
    });
    
    positionTable.setRootColumnVisible(false);
    positionTable.getTable().getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    positionTable.getTable().setEditable(false);
    positionTable.getTable().getSourceItems().addListener((ListChangeListener.Change<? extends PropertyMap> c) -> positionTable.initHeader());
    positionTable.getCustomColumns().addAll(Column.create("Цена за ед.", "cost", true, true), Column.create("Сумма", "sum"));
    positionTable.getTable().setEditable(true);
    
    positionTable.getTable().getSourceItems().addListener((ListChangeListener.Change<? extends PropertyMap> c) -> {
      positionTable.getTable().getSourceItems().forEach(p -> {
        p.get("sum").bind(Bindings.createObjectBinding(() -> 
                p.isNull("cost") ? null : p.getValue("cost", BigDecimal.ZERO).multiply(p.getBigDecimal("amount")).setScale(2, RoundingMode.HALF_UP), 
                p.get("cost")));
      });
    });
    
    HBox.setHgrow(positionBorder, Priority.ALWAYS);
    
    durCount.valueProperty().addListener((ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) -> setTypes(newValue, durType));
    
    recCount.valueProperty().addListener((ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) -> {
      if(!recCount.isDisable())
        setTypes(newValue, recType);
    });
    
    durType.getSelectionModel().selectFirst();
    recType.getSelectionModel().selectFirst();
    
    recType.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> validateDurRec(recType.isDisable() ? null : recType, oldValue, newValue));
    durType.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> validateDurRec(!recCheck.isSelected() || durType.isDisable() ? null : durType, oldValue, newValue));
    recCount.valueProperty().addListener((ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) -> validateDurRec(recCount.isDisable() ? null : recCount, oldValue, newValue));
    durCount.valueProperty().addListener((ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) -> validateDurRec(!recCheck.isSelected() ? null : durCount, oldValue, newValue));
    recCheck.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> validateDurRec(!newValue ? null : recCheck, oldValue, newValue));
    
    recBorder.getCenter().disableProperty().bind(recCheck.selectedProperty().not());
    copyBorder.getCenter().disableProperty().bind(copyCheck.selectedProperty().not().or(copyCheck.disableProperty()));
    
    toggleGroup.getToggles().addAll(contractRadio, countRadio);
    toggleGroup.selectToggle(contractRadio);
    recCheck.setSelected(false);
    copyCheck.setSelected(false);
    
    companyBox.addRow(0, sellerSelector, customerSelector);
    companyBox.getColumnConstraints().addAll(new ColumnConstraints(), new ColumnConstraints());
    companyBox.getColumnConstraints().forEach(c -> c.setPercentWidth(50));
    
    endDealDateLabel.textProperty().bind(Bindings.createStringBinding(() -> "окончание: "+Utility.format(startDealDateLabel.valueProperty().getValue().plus(Utility.convert(durCount.getValue()+" "+durType.getValue()))), durCount.valueProperty(), durType.valueProperty()));
    
    VBox durRecBox = new VBox(5, durBorder, recBorder);
    positionBorder.prefHeightProperty().bind(durRecBox.heightProperty());
    
    setCenter(new VBox(5, new BorderPane(null, null, startDealDateLabel, null, processLabel), companyBox, new HBox(durRecBox, positionBorder)));
  }
  
  private void setTypes(int couont, ChoiceBox typeNode) {
    typeNode.setDisable(true);
    int index = typeNode.getSelectionModel().getSelectedIndex();
    typeNode.getItems().setAll(FXCollections.observableArrayList(Utility.getPeriodTypes(couont)));
    typeNode.getSelectionModel().select(index);
    typeNode.setDisable(false);
  }
  
  private Spinner<Integer> createSpinner() {
    StringConverter<Integer> converter = new IntegerStringConverter();
    Spinner<Integer> count = new Spinner<>(1, Integer.MAX_VALUE, 1, 1);
    count.setEditable(true);
    count.getEditor().setAlignment(Pos.CENTER_RIGHT);
    count.getEditor().textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
      try {
        if(newValue != null && !newValue.equals("") && converter.fromString(newValue) == null)
          throw new Exception(newValue);
      }catch(Exception ex) {
        new Timeline(new KeyFrame(Duration.millis(300), e -> {
          new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(count.getEditor().opacityProperty(), 1))).play();
        }, new KeyValue(count.getEditor().opacityProperty(), 0))).play();
        count.getEditor().setText(oldValue);
      }
    });
    
    count.getEditor().focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(!newValue)
        count.getValueFactory().setValue(converter.fromString(count.getEditor().getText()));
    });
    
    return count;
  }

  @Override
  public boolean start() {
    if(!isInit()) {
      try {
        if(deal.isNotNull("sellerCompany"))
          sellerSelector.companyProperty().setValue(ObjectLoader.getMap(Company.class, deal.getInteger("sellerCompany"), "id", "name", "ownership","cfcs"));
        
        if(deal.isNotNull("sellerCompanyPartition"))
          sellerSelector.partitionProperty().setValue(ObjectLoader.getMap(CompanyPartition.class, deal.getInteger("sellerCompanyPartition"), "id", "name"));
        
        if(deal.isNotNull("sellerCfc"))
          sellerSelector.cfcProperty().setValue(ObjectLoader.getMap(CFC.class, deal.getInteger("sellerCfc"), "id", "name"));
        
        if(deal.isNotNull("customerCompany"))
          customerSelector.companyProperty().setValue(ObjectLoader.getMap(Company.class, deal.getInteger("customerCompany"), "id", "name", "ownership","cfcs"));
        
        if(deal.isNotNull("customerCompanyPartition"))
          customerSelector.partitionProperty().setValue(ObjectLoader.getMap(CompanyPartition.class, deal.getInteger("customerCompanyPartition"), "id", "name"));
        
        if(deal.isNotNull("customerCfc"))
          customerSelector.cfcProperty().setValue(ObjectLoader.getMap(CFC.class, deal.getInteger("customerCfc"), "id", "name"));
        
        if(deal.isNotNull("tempProcess")) {
          deal.setValue("tempProcess", ObjectLoader.getMap(ContractProcess.class, deal.getInteger("tempProcess")));
          if(!deal.getMap("tempProcess").getInteger("process").equals(deal.getMap("service").getInteger("id")))
            deal.remove("tempProcess");
        }
        
        deal.setValue("contract", ObjectLoader.getMap(Contract.class, deal.getInteger("contract")));
      }catch(Exception ex) {
        MsgTrash.out(ex);
      }
      
      deal.get("sellerCompany").bind(sellerSelector.companyProperty());
      deal.get("sellerCompanyPartition").bind(sellerSelector.partitionProperty());
      deal.get("sellerCfc").bind(sellerSelector.cfcProperty());
      
      deal.get("customerCompany").bind(customerSelector.companyProperty());
      deal.get("customerCompanyPartition").bind(customerSelector.partitionProperty());
      deal.get("customerCfc").bind(customerSelector.cfcProperty());
      
      if(deal.isNotNull("dealStartDate"))
        startDealDateLabel.valueProperty().setValue(deal.getLocalDate("dealStartDate"));
      
      deal.get("dealStartDate").bind(startDealDateLabel.valueProperty());
      
      deal.get("duration").bind(Bindings.createObjectBinding(() -> Utility.convert(durCount.getValue()+" "+durType.getValue()), durCount.valueProperty(), durType.valueProperty()));
      deal.get("recurrence").bind(Bindings.createObjectBinding(() -> Utility.convert(recCount.getValue()+" "+recType.getValue()), recCount.valueProperty(), recType.valueProperty()));
    }
    
    Period duration   = Period.ofDays(5);
    Period recurrence = null;
    
    if(!deal.getList("positions").isEmpty()) {
      try {
        List<PropertyMap> products = ObjectLoader.getList(DBFilter.create(Product.class)
                    .AND_EQUAL("company", deal.getMap("sellerCompany").getInteger("id"))
                    .AND_EQUAL("service", deal.getMap("service").getInteger("id"))
                    .AND_IN("group", deal.getList("positions").stream().map(p -> p.getInteger("group")).collect(Collectors.toSet()).toArray(new Integer[0]))
                    .AND_EQUAL("tmp", false).AND_EQUAL("type", Product.Type.CURRENT), 
                    "id","service","service_name","group","group_name","cost","nds","duration","recurrence");
        
        deal.getList("positions").stream().filter(position -> position.isNull("product")).forEach(position -> {
          products.stream().filter(product -> product.getInteger("group").equals(position.getInteger("group"))).forEach(product -> 
            position.setValue("product", product.getInteger("id")).copyFrom(product, "cost","nds","service","service_name"));
          
          if(position.isNull("cost"))
            position.setValue("cost", BigDecimal.ZERO);
          
          if(position.isNull("product"))
            position.setValue("product", IDStore.createIntegerID("product")*-1);
        });
        
        if(deal.isNull("duration") && deal.isNull("recurrence") && !deal.getList("positions").isEmpty()) {
          if(!products.isEmpty()) {
            duration   = products.get(0).getPeriod("duration");
            recurrence = products.get(0).getPeriod("recurrence");
            for(PropertyMap p:products) {
              if(!Objects.equals(duration, p.getPeriod("duration")))
                duration = Period.ofDays(5);
              if(!Objects.equals(recurrence, p.getPeriod("recurrence")))
                recurrence = null;
            }
          }
        }
      }catch(Exception ex) {
        MsgTrash.out(ex);
      }
    }
    
    durCount.getValueFactory().setValue(duration.getDays() > 0 ? duration.getDays() : duration.getMonths() > 0 ? duration.getMonths() : duration.getYears());
    durType.getSelectionModel().select(duration.getDays() > 0 ? 0 : duration.getMonths() > 0 ? 1 : 2);
    
    recCheck.setSelected(recurrence != null);
    if(recCheck.isSelected()) {
      recCount.getValueFactory().setValue(recurrence.getDays() > 0 ? recurrence.getDays() : recurrence.getMonths() > 0 ? recurrence.getMonths() : recurrence.getYears());
      recType.getSelectionModel().select(recurrence.getDays() > 0 ? 0 : recurrence.getMonths() > 0 ? 1 : 2);
      copyCheck.setSelected(true);
    }
    
    positionTable.setDisable(deal.getList("positions").isEmpty());
    processLabel.setText(deal.getMap("service").getString("name"));
    
    positionTable.getTable().getSourceItems().setAll(deal.getList("positions"));
    
    return super.start();
  }

  @Override
  public boolean end() {
    ObservableList<PropertyMap> list = deal.getList("positions").filtered(p -> p.getInteger("product") < 0);
    
    if(!list.isEmpty()) {
      RadioButton createInCatalog = new RadioButton("Создать продукты в каталоге");
      RadioButton createForOnce   = new RadioButton("Использовать только в этот раз");
      createInCatalog.setSelected(true);
      ToggleGroup group = new ToggleGroup();
      group.getToggles().addAll(createInCatalog, createForOnce);
      Label msg = new Label("Следующие продукты отсутствуют в каталоге: \n\n" 
              + String.join(";\n", list.stream().map(p -> "  -"+deal.getMap("service").getString("name")+" "+p.getString("group_name")).collect(Collectors.toSet()))
              +"\n\nВыберите один из вариантов дальнейших действий:");
      if(FXD.showWait("", getScene().getRoot(), new VBox(5, msg, createInCatalog, createForOnce), FXD.ButtonType.OK, FXD.ButtonType.CANCEL).orElseGet(() -> FXD.ButtonType.CANCEL) == FXD.ButtonType.OK) {
        RemoteSession session = null;
        try {
          session = ObjectLoader.createSession(false);
          Integer seller = deal.getMap("sellerCompany").getInteger("id");
          
          HashMap<Integer,PropertyMap> products = new HashMap<>();
          for(PropertyMap position:list) {
            PropertyMap product = products.get(position.getInteger("group"));
            if(product == null) {
              products.put(position.getInteger("group"), product = PropertyMap.create()
                            .copyFrom(position, "group")
                            .setValue("type", createInCatalog.isSelected() ? Product.Type.CURRENT : Product.Type.ARCHIVE)
                            .setValue("tmp", createForOnce.isSelected())
                            .setValue("service", deal.getMap("service").getInteger("id"))
                            .setValue("company", seller));
              product.setValue("id", session.createObject(Product.class, product.getSimpleMap()));
            }
            position.setValue("product", product);
          }
          ObjectLoader.commitSession(session);
        }catch(Exception ex) {
          ObjectLoader.rollBackSession(session);
          MsgTrash.out(ex);
          return false;
        }
      }else return false;
    }
    
    ObservableList<PropertyMap> deals = FXCollections.observableArrayList();
    
    Period    duration   = deal.getPeriod("duration");
    Period    recurrence = deal.getPeriod("recurrence");

    LocalDate endContract = Utility.convert(deal.getMap("contract").getSqlDate("endDate"));
    LocalDate start = deal.getLocalDate("dealStartDate");
    LocalDate end = duration.getDays() > 0 ? start.plus(duration) : duration.getMonths() > 0 ? 
            start.plusMonths(duration.getMonths()-1).withDayOfMonth(start.plusMonths(duration.getMonths()-1).lengthOfMonth()) : 
            start.plusYears(duration.getYears()-1).withMonth(12).withDayOfMonth(31);
    deal.setValue("dealEndDate", end);
    
    if(copyCheck.isSelected()) {
      int i = 0;
      while(contractRadio.isSelected() && (end.equals(endContract) || end.isBefore(endContract)) || countRadio.isSelected() && i < countCopy.getValue()) {
        deals.add(deal.copy().setValue("dealStartDate", start).setValue("dealEndDate", end));
        start = duration.getDays() > 0 ? start.plusDays(recurrence.getDays()+1) : duration.getMonths() > 0 ? start.plus(recurrence).withDayOfMonth(1) : start.plus(recurrence).withDayOfYear(1);
        end = duration.getDays() > 0 ? start.plus(duration) : duration.getMonths() > 0 ? 
              start.plusMonths(duration.getMonths()-1).withDayOfMonth(start.plusMonths(duration.getMonths()-1).lengthOfMonth()) : 
              start.plusYears(duration.getYears()-1).withMonth(12).withDayOfMonth(31);
        i++;
      }
    }else deals.add(deal);
    
    RemoteSession session = null;
    try {
      session = ObjectLoader.createSession(false);
      
      deal.setValue("tempProcess", getTempProcess(session, deals));
      
      for(PropertyMap d:deals) {
        d.setValue("tempProcess", deal.getMap("tempProcess"));
        d.setValue("id", session.createObject(Deal.class, d.getSimpleMap(
                "dealStartDate",
                "dealEndDate",
                "contract",
                "customerCompany",
                "sellerCompany",
                "customerCfc",
                "sellerCfc",
                "service",
                "tempProcess",
                "sellerCompanyPartition",
                "customerCompanyPartition",
                "duration",
                "recurrence")));
        
        for(PropertyMap dp:d.getList("positions")) {
          dp.setValue("deal", d.getInteger("id")).setValue("equipment", dp.getInteger("equipment-id")).setValue("customProductCost", dp.getValue("cost"));
          
          if(dp.isNotNull("equipment") && dp.getInteger("equipment") < 0) { // Заказ
            PropertyMap store = PropertyMap.create();
            PropertyMap group = ObjectLoader.getMap(Group.class, dp.getInteger("group"));
            List<PropertyMap> stories = ObjectLoader.getList(DBFilter.create(Store.class)
                    .AND_EQUAL("objectType", group.getValue("groupType"))
                    .AND_EQUAL("storeType", Store.StoreType.НАЛИЧНЫЙ)
                    .AND_EQUAL("companyPartition", d.getMap("sellerCompanyPartition").getInteger("id")));
            if(stories.isEmpty()) {
              store.setValue("id", session.createObject(Store.class, store
                      .setValue("companyPartition", d.getInteger("sellerCompanyPartition"))
                      .setValue("storeType", Store.StoreType.НАЛИЧНЫЙ).setValue("objectType", group.getValue("groupType"))
                      .setValue("name", "Основной.").getSimpleMap()));
            }else store = stories.get(0);
            PropertyMap equipment = PropertyMap.create().setValue("zakaz", true).setValue("amount", dp.getValue("amount")).setValue("group", dp.getInteger("group")).setValue("store", store.getInteger("id"));
            equipment.setValue("id", session.createObject(Equipment.class, equipment.getSimpleMap()));
            dp.setValue("equipment", equipment.getInteger("id"));
          }
          
          dp.setValue("id", session.createObject(DealPosition.class, dp.getSimpleMap("equipment", "amount", "deal", "product", "customProductCost")));
        }
      }
      
      ObjectLoader.commitSession(session);
    }catch(Exception ex) {
      ObjectLoader.rollBackSession(session);
      MsgTrash.out(ex);
    }
    return super.end();
  }
  
  private PropertyMap getTempProcess(RemoteSession session, List<PropertyMap> deals) throws Exception {
    List<PropertyMap> temps = session.getList(DBFilter.create(ContractProcess.class)
            .AND_EQUAL("process", deal.getMap("service").getInteger("id"))
            .AND_EQUAL("customerPartition", deal.getMap("customerCompanyPartition").getInteger("id"))
            .AND_EQUAL("contract", deal.getMap("contract").getInteger("id"))).stream().map(temp -> PropertyMap.copy(temp)).collect(Collectors.toList());
    
    for(int i=temps.size()-1;i>=0;i--) {
      DBFilter filter = DBFilter.create(Deal.class).AND_EQUAL("tempProcess", temps.get(i).getInteger("id")).AND_EQUAL("tmp", false).AND_EQUAL("type", Deal.Type.CURRENT);
      DBFilter datefilter = filter.AND_FILTER();
      deals.forEach(d -> datefilter.AND_DATE_BETWEEN("dealStartDate", Utility.convert(d.getLocalDate("dealStartDate")), Utility.convert(d.getLocalDate("dealEndDate")))
              .OR_DATE_BETWEEN("dealEndDate", Utility.convert(d.getLocalDate("dealStartDate")), Utility.convert(d.getLocalDate("dealEndDate"))));
      if(!session.getList(filter, "id").isEmpty())
        temps.remove(i);
    }
    
    PropertyMap tempProcess;
    
    if(temps.isEmpty()) {
      tempProcess = PropertyMap.create()
              .setValue("contract", deal.getMap("contract").getInteger("id"))
              .setValue("customerPartition", deal.getMap("customerCompanyPartition").getInteger("id"))
              .setValue("process", deal.getMap("service").getInteger("id"));
      tempProcess.setValue("id", session.createObject(ContractProcess.class, tempProcess.getSimpleMap()));
    }else tempProcess = deal.isNull("tempProcess") ? temps.get(0) : temps.stream().filter(t -> t.getInteger("id").equals(deal.getMap("tempProcess").getInteger("id"))).findFirst().orElseGet(() -> temps.get(0));
    
    return tempProcess;
  }

  private void validateDurRec(Node source, Object ov, Object nv) {
    if(source != null) {
      Period duration   = Utility.convert(durCount.getValue()+" "+durType.getSelectionModel().getSelectedItem());
      Period reccurence = Utility.convert(recCount.getValue()+" "+recType.getSelectionModel().getSelectedItem());

      int durDays = duration.getDays()+duration.getMonths()*30+duration.getYears()*365;
      int recDays = reccurence.getDays()+reccurence.getMonths()*30+reccurence.getYears()*365;

      if(durDays > recDays) {
        if(source instanceof CheckBox) {
          recCount.setDisable(true);
          recType.setDisable(true);
          recCount.getValueFactory().setValue(durCount.getValue());
          recType.getItems().setAll(durType.getItems());
          recType.getSelectionModel().select(durType.getSelectionModel().getSelectedIndex());
          recType.setDisable(false);
          recCount.setDisable(false);
        }else {
          FXD.showWait("Внимание", this, "Длительность не может быть больше цикличности", FXD.ButtonType.OK);
          source.setDisable(true);
          if(source instanceof ChoiceBox)
            ((ChoiceBox)source).setValue(ov);
          if(source instanceof Spinner) {
            ((Spinner)source).getValueFactory().setValue(ov);
            setTypes((int)ov, source.equals(durCount) ? durType : recType);
          }
          source.setDisable(false);
        }
      }
    }
  }
}