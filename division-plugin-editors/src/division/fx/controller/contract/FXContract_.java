package division.fx.controller.contract;

import bum.editors.util.ObjectLoader;
import bum.interfaces.CFC;
import bum.interfaces.Company;
import bum.interfaces.CompanyPartition;
import bum.interfaces.Contract;
import bum.interfaces.Service;
import bum.interfaces.XMLContractTemplate;
import division.fx.DateLabel.DateLabel;
import division.fx.FXUtility;
import division.fx.PropertyMap;
import division.fx.editor.FXObjectEditor;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.client.plugins.deal.test.FXDealTable;
import division.fx.controller.company.FXCompanySelector;
import division.fx.controller.process.FXProcess;
import division.fx.dialog.FXD;
import division.fx.editor.FXTreeEditor;
import division.fx.gui.FXGLoader;
import division.fx.scale.test.ScalePeriod;
import division.fx.scale.test.ScaleRow;
import division.fx.table.Column;
import division.fx.util.MsgTrash;
import division.util.IDStore;
import division.util.Utility;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import util.filter.local.DBFilter;

public class FXContract_ extends FXObjectEditor {
  private final Label             sellerLabel          = new Label();
  private final Label             customerLabel        = new Label();
  private final Label             contractLabel        = new Label();
  private final GridPane          contractCrid         = new GridPane();
  
  private final Button            changeCompany        = new Button("<>");
  private final FXCompanySelector sellerSelector       = new FXCompanySelector("Продавец");
  private final FXCompanySelector customerSelector     = new FXCompanySelector("Покупатель");
  
  private final DateLabel         startDate            = new DateLabel("Начало",LocalDate.now());
  private final DateLabel         endDate              = new DateLabel("Окончание",LocalDate.now());
  //private final CheckBox          endDateCheck         = new CheckBox();
  
  private final FXDealTable           dealTable                     = new FXDealTable();
  //private final DealPositionTable     dealPositionTable             = new DealPositionTable();
  //private final FXDealActionDocuments actionDocuments               = new FXDealActionDocuments();
  //private final SplitPane             deals_positions_actions_split = new SplitPane(dealTable, dealPositionTable, actionDocuments);
  
  /*private final FXButton processAddButton = new FXButton("Добавить процесс", h -> createProcess(), "add-tool-button");
  private final FXButton processDelButton = new FXButton("Удалить процесс", h -> removeProcess(), "remove-tool-button");
  private final ToolBar  processTool      = new ToolBar(processAddButton, processDelButton);*/
  
  /*private final FXButton dealAddButton = new FXButton("Добавить сделку", h -> createDeal(), "add-tool-button");
  private final FXButton dealDelButton = new FXButton("Удалить сделку", h -> removeDeal(), "remove-tool-button");
  private final ToolBar  dealTool      = new ToolBar(dealAddButton, dealDelButton);*/
  
  /*private final FXButton dealPositionAddButton = new FXButton("Добавить ппозицию сделки", h -> createDealPosition(), "add-tool-button");
  private final FXButton dealPositionDelButton = new FXButton("Удалить позицию сделки", h -> removeDealPosition(), "remove-tool-button");
  private final ToolBar  dealPositionTool      = new ToolBar(dealPositionAddButton, dealPositionDelButton);*/
  
  private final GridPane          sellerCustomerGrid   = new GridPane();
  private final HBox              dateBox              = new HBox(5, new Label("Длительность договора: ") ,startDate, endDate);
  private final VBox              topBox               = new VBox(5, sellerCustomerGrid, dateBox);
  private final SplitPane         rootSplit            = new SplitPane(topBox, dealTable.getContent());
  
  private final ContractTemplateScene templateScene = new ContractTemplateScene();
  
  public FXContract_() {
    super(Contract.class);
    FXUtility.initMainCss(this);
    
    VBox.setVgrow(rootSplit, Priority.ALWAYS);
    HBox.setHgrow(sellerSelector, Priority.ALWAYS);
    HBox.setHgrow(customerSelector, Priority.ALWAYS);
    
    rootSplit.setOrientation(Orientation.VERTICAL);
    
    sellerCustomerGrid.addRow(0, sellerSelector, changeCompany, customerSelector);
    sellerCustomerGrid.getColumnConstraints().addAll(new ColumnConstraints(), new ColumnConstraints(), new ColumnConstraints());
    GridPane.setHgrow(customerSelector, Priority.ALWAYS);
    GridPane.setHgrow(sellerSelector, Priority.ALWAYS);
    sellerCustomerGrid.setAlignment(Pos.CENTER);
    dateBox.setAlignment(Pos.CENTER_LEFT);
    
    ((TitleBorderPane)dealTable.getContent()).getItems().add(0, new Label("План-график"));
    
    sellerSelector.getCfcSelector().setMinWidth(300);
    sellerSelector.getCfcSelector().setMaxWidth(300);
    sellerSelector.getPartitionSelector().setMinWidth(300);
    sellerSelector.getPartitionSelector().setMaxWidth(300);
    
    customerSelector.getCfcSelector().setMinWidth(300);
    customerSelector.getCfcSelector().setMaxWidth(300);
    customerSelector.getPartitionSelector().setMinWidth(300);
    customerSelector.getPartitionSelector().setMaxWidth(300);
    
    sellerSelector.getCfcSelector().promtTextProperty().setValue("Задайте группу...");
    sellerSelector.getPartitionSelector().promtTextProperty().setValue("Выберите подразделение...");
    
    customerSelector.getCfcSelector().promtTextProperty().setValue("Задайте группу...");
    customerSelector.getPartitionSelector().promtTextProperty().setValue("Выберите подразделение...");
    
    storeControls().addAll(rootSplit, dealTable, templateScene);
    
    //dealTable.setTop(new HBox(processTool, dealTool));
    //dealPositionTable.setTop(dealPositionTool);
    //processTool.minWidthProperty().bind(dealTable.getLeftTable().widthProperty());
    //HBox.setHgrow(dealTool, Priority.ALWAYS);
    
    changeCompany.setOnAction(e -> {
      PropertyMap sellerCompany   = sellerSelector.companyProperty().getValue();
      PropertyMap sellerPartition = sellerSelector.partitionProperty().getValue();
      PropertyMap sellerCfc       = sellerSelector.cfcProperty().getValue();
      String      sellerInFace    = sellerSelector.inFaceProperty().getValue();
      String      sellerReason    = sellerSelector.reasonProperty().getValue();
      
      PropertyMap customerCompany   = customerSelector.companyProperty().getValue();
      PropertyMap customerPartition = customerSelector.partitionProperty().getValue();
      PropertyMap customerCfc       = customerSelector.cfcProperty().getValue();
      String      customerInFace    = customerSelector.inFaceProperty().getValue();
      String      customerReason    = customerSelector.reasonProperty().getValue();
      
      sellerSelector.companyProperty().setValue(customerCompany);
      sellerSelector.partitionProperty().setValue(customerPartition);
      sellerSelector.cfcProperty().setValue(customerCfc);
      sellerSelector.inFaceProperty().setValue(customerInFace);
      sellerSelector.reasonProperty().setValue(customerReason);
      
      customerSelector.companyProperty().setValue(sellerCompany);
      customerSelector.partitionProperty().setValue(sellerPartition);
      customerSelector.cfcProperty().setValue(sellerCfc);
      customerSelector.inFaceProperty().setValue(sellerInFace);
      customerSelector.reasonProperty().setValue(sellerReason);
    });
    
    sellerLabel.setOpacity(0);
    customerLabel.setOpacity(0);
    
    sellerLabel.textProperty().bind(((Label)sellerSelector.getTitlePane().getTitle()).textProperty());
    customerLabel.textProperty().bind(((Label)customerSelector.getTitlePane().getTitle()).textProperty());
    
    rootSplit.getDividers().get(0).positionProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
      new Timeline(new KeyFrame(Duration.millis(500), new KeyValue(customerLabel.opacityProperty(), newValue.doubleValue() < 0.03 ? 1 : 0), new KeyValue(sellerLabel.opacityProperty(), newValue.doubleValue() < 0.03 ? 1 : 0))).play();
    });
    
    initEvents();
  }
  
  private void initEvents() {
    customerSelector.companyProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
    });
  }

  @Override
  public void initData() {
    getObjectProperty().unbindAll();
    
    startDate.valueProperty().setValue(getObjectProperty().isNull("startDate") ? LocalDate.now() : Utility.convert(getObjectProperty().getDate("startDate")));
    endDate.valueProperty().setValue(getObjectProperty().isNull("endDate") ? LocalDate.now() : Utility.convert(getObjectProperty().getDate("endDate")));
    
    getObjectProperty().get("endDate").bind(endDate.valueProperty());
    getObjectProperty().get("startDate").bind(startDate.valueProperty());
    
    if(getObjectProperty().isNotNull("sellerCompany"))
      getObjectProperty().setValue("sellerCompany",   
              ObjectLoader.getMap(Company.class, 
                      getObjectProperty().getValue("sellerCompany") instanceof Integer ? getObjectProperty().getInteger("sellerCompany") : getObjectProperty().getArray("sellerCompany", Integer.class)[0]));
    
    if(getObjectProperty().isNotNull("customerCompany"))
      getObjectProperty().setValue("customerCompany", 
              ObjectLoader.getMap(Company.class, 
                      getObjectProperty().getValue("customerCompany") instanceof Integer ? getObjectProperty().getInteger("customerCompany") : getObjectProperty().getArray("customerCompany", Integer.class)[0]));
    
    if(getObjectProperty().isNotNull("sellerCfc"))
      getObjectProperty().setValue("sellerCfc",   
              ObjectLoader.getMap(CFC.class, 
                      getObjectProperty().getValue("sellerCfc") instanceof Integer ? getObjectProperty().getInteger("sellerCfc") : getObjectProperty().getArray("sellerCfc", Integer.class)[0]));

    if(getObjectProperty().isNotNull("customerCfc"))
      getObjectProperty().setValue("customerCfc", 
              ObjectLoader.getMap(CFC.class, 
                      getObjectProperty().getValue("customerCfc") instanceof Integer ? getObjectProperty().getInteger("customerCfc") : getObjectProperty().getArray("customerCfc", Integer.class)[0]));
    
    if(getObjectProperty().isNotNull("sellerCompanyPartition"))
      getObjectProperty().setValue("sellerCompanyPartition",   ObjectLoader.getMap(CompanyPartition.class, getObjectProperty().getInteger("sellerCompanyPartition")));
    
    if(getObjectProperty().isNotNull("customerCompanyPartition"))
      getObjectProperty().setValue("customerCompanyPartition", ObjectLoader.getMap(CompanyPartition.class, getObjectProperty().getInteger("customerCompanyPartition")));

    if(getObjectProperty().isNotNull("template") && getObjectProperty().getValue("template") instanceof Integer)
      getObjectProperty().setValue("template", ObjectLoader.getMap(XMLContractTemplate.class, getObjectProperty().getInteger("template"), 
              "id", 
              "name", 
              "seller-nick-name=query:select name from [SellerNickName] where id=[XMLContractTemplate(sellerNickname)]",
              "customer-nick-name=query:select name from [CompanyNickname] where id=[XMLContractTemplate(customerNickname)]"));

    if(getObjectProperty().isNotNull("sellerCompany"))
      sellerSelector.companyProperty().setValue(getObjectProperty().getMap("sellerCompany"));
    
    if(getObjectProperty().isNotNull("sellerCompanyPartition"))
      sellerSelector.partitionProperty().setValue(getObjectProperty().getMap("sellerCompanyPartition"));
    
    if(getObjectProperty().isNotNull("sellerCfc"))
      sellerSelector.cfcProperty().setValue(getObjectProperty().getMap("sellerCfc"));
    
    if(getObjectProperty().isNotNull("sellerPerson"))
      sellerSelector.inFaceProperty().setValue(getObjectProperty().getString("sellerPerson"));
    
    if(getObjectProperty().isNotNull("sellerReason"))
      sellerSelector.reasonProperty().setValue(getObjectProperty().getString("sellerReason"));
    
    
    if(getObjectProperty().isNotNull("customerCompany"))
      customerSelector.companyProperty().setValue(getObjectProperty().getMap("customerCompany"));
    
    if(getObjectProperty().isNotNull("customerCompanyPartition"))
      customerSelector.partitionProperty().setValue(getObjectProperty().getMap("customerCompanyPartition"));
    
    if(getObjectProperty().isNotNull("customerCfc"))
      customerSelector.cfcProperty().setValue(getObjectProperty().getMap("customerCfc"));
    
    if(getObjectProperty().isNotNull("customerPerson"))
      customerSelector.inFaceProperty().setValue(getObjectProperty().getString("customerPerson"));
    
    if(getObjectProperty().isNotNull("customerReason"))
      customerSelector.reasonProperty().setValue(getObjectProperty().getString("customerReason"));

    contractLabel.textProperty().bind(Bindings.createStringBinding(() -> 
            getObjectProperty().isNull("template") ? "" : getObjectProperty().getMap("template").getString("name")+(getObjectProperty().isNull("number") ? "" : (" № "+getObjectProperty().getString("number"))), 
            getObjectProperty().get("template"), 
            getObjectProperty().get("number")));
    
    sellerSelector.titleProperty().bind(Bindings.createStringBinding(() -> getObjectProperty().isNotNull("template") && getObjectProperty().getValue("template") instanceof PropertyMap ? getObjectProperty().getMap("template").getString("seller-nick-name") : "Продавец", getObjectProperty().get("template")));
    customerSelector.titleProperty().bind(Bindings.createStringBinding(() -> getObjectProperty().isNotNull("template") && getObjectProperty().getValue("template") instanceof PropertyMap ? getObjectProperty().getMap("template").getString("customer-nick-name") : "Покупатель", getObjectProperty().get("template")));
    
    getObjectProperty().get("sellerCompany").bind(sellerSelector.companyProperty());
    getObjectProperty().get("customerCompany").bind(customerSelector.companyProperty());
    getObjectProperty().get("sellerCfc").bind(sellerSelector.cfcProperty());
    getObjectProperty().get("customerCfc").bind(customerSelector.cfcProperty());
    getObjectProperty().get("sellerCompanyPartition").bind(sellerSelector.partitionProperty());
    getObjectProperty().get("customerCompanyPartition").bind(customerSelector.partitionProperty());
    getObjectProperty().get("customerCompanyPartition").bind(customerSelector.partitionProperty());
    getObjectProperty().get("sellerPerson").bind(sellerSelector.inFaceProperty());
    getObjectProperty().get("customerPerson").bind(customerSelector.inFaceProperty());
    getObjectProperty().get("sellerReason").bind(sellerSelector.reasonProperty());
    getObjectProperty().get("customerReason").bind(customerSelector.reasonProperty());
    
    getObjectProperty().bind("template", templateScene.valueProperty());
    
    dealTable.getContent().disableProperty().bind(Bindings.createBooleanBinding(() -> getObjectProperty().isNull("sellerCompany") || getObjectProperty().isNull("customerCompany"), getObjectProperty().get("sellerCompany","customerCompany")));
    
    setMementoProperty(getObjectProperty().copy());
  }

  @Override
  public void beforeShow() {
    setScene();
  }
  
  public void setScene() {
    if(getObjectProperty().isNull("id")) { // Сцена первая
      getRoot().setCenter(templateScene.getRoot());
      getRoot().setTop(topBox);
      templateScene.initData();
      if(getDialog() != null && ((FXD)getDialog()).getButton(FXD.ButtonType.OK) != null)
        ((FXD)getDialog()).getButton(FXD.ButtonType.OK).setText("Далее");
      
      sellerCustomerGrid.getColumnConstraints().get(0).setPercentWidth(47.5);
      sellerCustomerGrid.getColumnConstraints().get(1).setPercentWidth(5);
      sellerCustomerGrid.getColumnConstraints().get(1).setHalignment(HPos.CENTER);
      sellerCustomerGrid.getColumnConstraints().get(2).setPercentWidth(47.5);
    }else { // Сцена вторая
      if(getDialog() != null && ((FXD)getDialog()).getButton("Далее") != null)
        ((FXD)getDialog()).getButton("Далее").setText("Ок");
      
      contractCrid.getColumnConstraints().addAll(new ColumnConstraints(),new ColumnConstraints(),new ColumnConstraints());
      contractCrid.addRow(0, sellerLabel, customerLabel, contractLabel);
      
      sellerLabel.setPadding(new Insets(5));
      sellerLabel.setPadding(new Insets(5));

      contractCrid.getColumnConstraints().get(0).setPercentWidth(20);
      contractCrid.getColumnConstraints().get(1).setPercentWidth(20);
      contractCrid.getColumnConstraints().get(2).setPercentWidth(60);
      contractCrid.getColumnConstraints().get(2).setHalignment(HPos.RIGHT);
      
      sellerSelector.companyDisableProperty().setValue(true);
      customerSelector.companyDisableProperty().setValue(true);
      
      sellerCustomerGrid.getChildren().remove(changeCompany);
      sellerCustomerGrid.getColumnConstraints().get(0).setPercentWidth(50);
      sellerCustomerGrid.getColumnConstraints().get(1).setPercentWidth(0);
      sellerCustomerGrid.getColumnConstraints().get(2).setPercentWidth(50);
      
      VBox box = new VBox(5, contractCrid, rootSplit);
      box.setAlignment(Pos.CENTER_RIGHT);
      getRoot().setCenter(box);
      
      initDealTable();
      
      if(!getObjectProperty().is("tmp")) // Попали сразу на вторую сцену
        dealTable.initData();
      else { // Вторая сцена нового договора
        getObjectProperty().setValue("number", ObjectLoader.getData(Contract.class, getObjectProperty().getInteger("id"), "number"));
        getObjectProperty().setValue("tmp", false);
        
        //getObjectProperty().setValue("endDate", getObjectProperty().getLocalDate("startDate").pl)
        
        ObjectLoader.getList(Service.class, getObjectProperty().getMap("template").getArray("processes", Integer.class), "id", "owner", "name").stream().forEach(p -> {
          try {
            ScaleRow<ScalePeriod> row = (ScaleRow<ScalePeriod>) dealTable.getScale().createRow()
                    .setValue("service", p.getInteger("id"))
                    .setValue("object-owner", p.getValue("owner"))
                    .setValue("service_name", p.getString("name"))
                    .setValue("contract", getObjectProperty().getInteger("id"))
                    .setValue("tempProcess", -1*IDStore.createID())
                    .setValue("temp-customer-name", getObjectProperty().getMap("customerCompanyPartition"));

            dealTable.getLeftTable().getSourceItems().add(row);
          }catch(Exception ex) {
            MsgTrash.out(ex);
          }
        });
      }
      
      //initDealPositionTable();
    }
  }

  @Override
  public void dispose() {
    super.dispose();
  }
  
  private void initDealTable() {
    //dealTable.showContractDoubleClickProperty().setValue(false);
    //dealTable.showCompanyDoubleClickProperty().setValue(false);
    dealTable.changeContract(new Integer[]{getObjectProperty().getInteger("id")}, true);
    dealTable.getFields().addAll(
            "object-owner",
            "customer-partition-name=query:select name from [CompanyPartition] where id = [Deal(tempprocess_customer_partition)]",
            "customerCfc",
            "sellerCfc");

    dealTable.getLeftTable().setEditable(true);
    dealTable.getRightTable().setEditable(true);
    
    dealTable.getLeftTable().getColumns().clear();
    dealTable.getRightTable().getColumns().clear();
    
    dealTable.getLeftTable().getColumns().addAll(
            Column.create("Процесс", "service_name"),
            Column.create("От Продавца", "seller-partition-name", true, true, 
                    ObjectLoader.getList(DBFilter.create(CompanyPartition.class)
                            .AND_EQUAL("company", getObjectProperty().getMap("sellerCompany").getInteger("id")), "id", "name").toArray(new PropertyMap[0])),
            Column.create("Группа продавца", true, true, 
                    ObjectLoader.getList(CFC.class, getObjectProperty().getMap("sellerCompany").getArray("cfcs", Integer.class), "id", "name").toArray(new PropertyMap[0])));
    
    dealTable.getRightTable().getColumns().addAll(Column.create("От Покупателя", "customer-partition-name", true, true, 
                    ObjectLoader.getList(DBFilter.create(CompanyPartition.class)
                            .AND_EQUAL("company", getObjectProperty().getMap("customerCompany").getInteger("id")), "id", "name").toArray(new PropertyMap[0])),
            Column.create("Группа покупателя", true, true, 
                    ObjectLoader.getList(CFC.class, getObjectProperty().getMap("customerCompany").getArray("cfcs", Integer.class), "id", "name").toArray(new PropertyMap[0])));
    
    dealTable.getLeftTable().getSourceItems().addListener((ListChangeListener.Change<? extends ScaleRow> c) -> {
      dealTable.getLeftTable().getSourceItems().stream().forEach(row -> {
        setCFCValue(row, "Группа продавца", "sellerCfc", row.getPeriods().stream().map(d -> (PropertyMap)d).collect(Collectors.toList()));
        setCFCValue(row, "Группа покупателя", "customerCfc", row.getPeriods().stream().map(d -> (PropertyMap)d).collect(Collectors.toList()));
      });
    });
    
    dealTable.getScale().selectedPeriodProperty().addListener((ObservableValue<? extends List<ScalePeriod>> observable, List<ScalePeriod> oldValue, List<ScalePeriod> newValue) -> {
      dealTable.getLeftTable().getSourceItems().stream().forEach(row -> {
        setCFCValue(row, "Группа продавца",   "sellerCfc",   dealTable.getScale().getSelectedPeriods(row).stream().map(p -> (PropertyMap)p).collect(Collectors.toList()));
        setCFCValue(row, "Группа покупателя", "customerCfc", dealTable.getScale().getSelectedPeriods(row).stream().map(p -> (PropertyMap)p).collect(Collectors.toList()));
      });
    });
    
    /*dealTable.selectedHandlers.add(e -> {
      dealTable.getLeftTable().getSourceItems().stream().forEach(row -> {
        setCFCValue(row, "Группа продавца", "sellerCfc", (List<PropertyMap>)row.getPeriods().stream().filter(deal -> ((DealPeriod)deal).selectedProperty().getValue()).collect(Collectors.toList()));
        setCFCValue(row, "Группа покупателя", "customerCfc", (List<PropertyMap>)row.getPeriods().stream().filter(deal -> ((DealPeriod)deal).selectedProperty().getValue()).collect(Collectors.toList()));
      });
    });*/
    
    dealTable.getLeftTable().getColumn("Группа продавца").setOnEditStart(e -> {
      if(dealTable.getLeftTable().getSelectionModel().getSelectedItem().isNull("Группа продавца"))
        e.consume();
    });
    
    onEditCommit("Группа продавца", "sellerCfc");
    onEditCommit("Группа покупателя", "customerCfc");
    
    FXGLoader.load(storeFileName(), dealTable);
  }
  
  private void onEditCommit(String columnName, String cfcPropertyName) {
    dealTable.getColumn(columnName).setOnEditCommit(e -> {
      ScaleRow<ScalePeriod> row = (ScaleRow) ((TableColumn.CellEditEvent)e).getRowValue();
      Object cfc = row.getValue(columnName);
      List<ScalePeriod> deals = row.getPeriods().stream().filter(d -> d.isVisible()).collect(Collectors.toList());
      deals = deals.isEmpty() ? row.getPeriods() : deals;
      deals.stream().forEach(d -> d.setValue(cfcPropertyName, (PropertyMap)cfc));
      setCFCValue(row, columnName, cfcPropertyName, deals.stream().map(d -> (PropertyMap)d).collect(Collectors.toList()));
    });
  }
  
  private void setCFCValue(ScaleRow row, String columnName, String cfcPropertyName, List<PropertyMap> deals) {
    deals = deals.isEmpty() ? row.getPeriods() : deals;
    PropertyMap firstDeal = (PropertyMap) deals.stream().findFirst().orElseGet(() -> PropertyMap.create());
    Integer firsDealtId = firstDeal.isPropertyMap(cfcPropertyName) ? firstDeal.getMap(cfcPropertyName).getInteger("id") : firstDeal.getInteger(cfcPropertyName);
    PropertyMap otherDeal = (PropertyMap) deals.stream()
            .filter(deal -> !firsDealtId.equals(((PropertyMap)deal).isPropertyMap(cfcPropertyName) ? ((PropertyMap)deal).getMap(cfcPropertyName).getInteger("id") : ((PropertyMap)deal).getInteger(cfcPropertyName)))
            .findFirst().orElseGet(() -> null);
    if(otherDeal == null) {// других групп нет
      if(firstDeal == null) {// вообще групп нет
        row.setValue(columnName, null);
      }else {
        if(!(firstDeal.getValue(cfcPropertyName) instanceof PropertyMap))
          firstDeal.setValue(cfcPropertyName, ObjectLoader.getMap(CFC.class, firstDeal.getInteger(cfcPropertyName), "id","name"));
        row.setValue(columnName, firstDeal.getMap(cfcPropertyName));
      }
    }else row.setValue(columnName, "-------");
  }

  /*private void initDealPositionTable() {
    dealTable.getDateScale().selectedPeriodsHandlers.add(e -> {
      dealPositionTable.getClientFilter().clear().AND_IN("deal", dealTable.getSelectedDeals());
      dealPositionTable.initData();
    });
    
    //dealPositionTable.getPositionTable().addInitDataListener(e -> initActionTable());

    //dealPositionTable.getPositionTable().getTable().getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> initActionTable());
  }*/
  
  /*private void initActionTable() {
    if(dealPositionTable.getPositionTable().getTable().getSelectionModel().getSelectedItems().isEmpty())
      actionDocuments.dealPositionsProperty().setValue(PropertyMap.copyList(dealPositionTable.getPositionTable().getTable().getItems()));
    else actionDocuments.dealPositionsProperty().setValue(PropertyMap.copyList(dealPositionTable.getPositionTable().getTable().getSelectionModel().getSelectedItems()));
  }*/

  private void createProcess() {
    FXTreeEditor processes = new FXTreeEditor(Service.class, FXProcess.class, "childs", "owner");
    //processes.checkBoxTreeProperty().setValue(true);
    processes.titleProperty().setValue("Процессы");
    processes.setSelectionMode(SelectionMode.MULTIPLE);
    
    processes.getObjects(this.getRoot(), processes.titleProperty().getValue()).stream().filter(p -> p.getArray("childs", Integer.class).length == 0).forEach(p -> {
      try {
        ScaleRow<ScalePeriod> row = (ScaleRow<ScalePeriod>) dealTable.getScale().createRow()
                .setValue("service", p.getInteger("id"))
                .setValue("object-owner", p.getValue("owner"))
                .setValue("service_name", p.getString("name"))
                .setValue("contract", getObjectProperty().getInteger("id"))
                .setValue("tempProcess", -1*IDStore.createID())
                .setValue("temp-customer-name", getObjectProperty().getMap("customerCompanyPartition"));

        dealTable.getLeftTable().getSourceItems().add(row);
      }catch(Exception ex) {
        MsgTrash.out(ex);
      }
    });
  }

  private void removeProcess() {
    dealTable.getLeftTable().getSourceItems().removeAll(dealTable.getLeftTable().getSelectionModel().getSelectedItems());
  }

  private void createDeal() {
    /*LocalDate   startDate   = dealTable.getSelectedDay();
    PropertyMap process     = ObjectLoader.getMap(Service.class, dealTable.getLeftTable().getSelectionModel().getSelectedItem().getInteger("service"));
    //Integer   tempProcess = dealTable.getLeftTable().getSelectionModel().getSelectedItem().getInteger("tempProcess");
    PropertyMap partition = dealTable.getLeftTable().getSelectionModel().getSelectedItem().getValue("object-owner", Service.Owner.class) == Service.Owner.CUSTOMER ? getObjectProperty().getMap("customerCompanyPartition") : getObjectProperty().getMap("sellerCompanyPartition");
    
    FXCompanyStoreTable storeTable = new FXCompanyStoreTable();
    storeTable.setTop(new HBox(5, new DateLabel(process.getString("name"), startDate == null ? LocalDate.now() : startDate)));
    storeTable.processProperty().setValue(process);
    storeTable.companyPartitionProperty().setValue(partition);
    
    storeTable.get("Выбор объектов", getRoot());*/
    
    
  }

  private void removeDeal() {
  }

  private void createDealPosition() {
  }

  private void removeDealPosition() {
  }

  @Override
  public String validate() {
    String msg = "";
    if(getObjectProperty().isNull("sellerCompany"))
      msg += "  -агент\n";
    if(getObjectProperty().isNull("customerCompany"))
      msg += "  -контрагент\n";
    if(getObjectProperty().isNull("template"))
      msg += "  -тип договора\n";
    return msg.equals("") ? null : ("Заполните следующие поля:\n"+msg);
  }

  @Override
  public boolean save() {
    if(getObjectProperty().isNull("id")) { // первая сцена
      if(super.save(true))
        setScene();
      return false;
    }else return super.save();
  }
}