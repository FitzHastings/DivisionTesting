package division.editors.contract;

import bum.editors.util.ObjectLoader;
import bum.interfaces.CFC;
import bum.interfaces.Company;
import bum.interfaces.CompanyPartition;
import bum.interfaces.Contract;
import bum.interfaces.ContractProcess;
import bum.interfaces.Deal;
import bum.interfaces.Product;
import bum.interfaces.Service;
import bum.interfaces.XMLContractTemplate;
import division.fx.FXToolButton;
import division.fx.FXUtility;
import division.fx.controller.company.CompanySelector;
import division.fx.editor.FXObjectEditor;
import division.fx.table.Column;
import division.fx.PropertyMap;
import division.fx.client.plugins.deal.test.FXDealTable;
import division.fx.controller.process.FXProcess;
import division.fx.editor.FXEditor;
import division.fx.editor.FXTableEditor;
import division.fx.editor.FXTreeEditor;
import division.fx.scale.test.ScalePeriod;
import division.fx.scale.test.ScaleRow;
import division.fx.tree.FXDivisionTree;
import division.fx.util.MsgTrash;
import division.util.DivisionMap;
import division.util.Utility;
import java.net.URL;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.apache.commons.lang3.ArrayUtils;
import util.filter.local.DBFilter;

public class FXContract extends FXObjectEditor {
  @FXML private Label contractLabel;
  @FXML private Button changeCompany;
  @FXML private CompanySelector seller;
  @FXML private CompanySelector customer;
  @FXML private DatePicker      startDate;
  @FXML private DatePicker      endDate;
  @FXML private CheckBox        endDateCheck;
  @FXML private Tab planGrafic;
  @FXML private SplitPane rootSplit;
  private final FXToolButton addProcess    = new FXToolButton(e -> addProcess(), "", "add-button");
  private final FXToolButton removeProcess = new FXToolButton(e -> removeProcess(), "", "remove-button");
  
  private final FXDealTable deals = new FXDealTable();
  
  private final Column col = Column.create("Исполнитель", true, true);
  
  //private final PropertyMap redodata = null;
  private final DivisionMap<Integer,PropertyMap> cfccash = new DivisionMap<>();

  public FXContract() {
    FXUtility.initMainCss(this);
    setObjectClass(Contract.class);
  }

  @Override
  public void initData() {
    try {
      deals.changeContract(new Integer[]{getObjectProperty().getValue("id", Integer.TYPE)}, true);
      
      getObjectProperty().setValue("template", ObjectLoader.getMap(XMLContractTemplate.class, getObjectProperty().getValue("template", Integer.TYPE)));

      getObjectProperty().setValue("sellerCompany", ObjectLoader.getMap(Company.class, 
              getObjectProperty().getValue("sellerCompany") instanceof Integer[] ? getObjectProperty().getValue("sellerCompany", Integer[].class)[0] :
                      getObjectProperty().getValue("sellerCompany", Integer.TYPE)));

      getObjectProperty().setValue("sellerCfc", ObjectLoader.getMap(CFC.class, 
              getObjectProperty().getValue("sellerCfc") instanceof Integer[] ? getObjectProperty().getValue("sellerCfc", Integer[].class)[0] : 
                      getObjectProperty().getValue("sellerCfc", Integer.TYPE)));

      getObjectProperty().setValue("sellerCompanyPartition", ObjectLoader.getMap(CompanyPartition.class, getObjectProperty().getValue("sellerCompanyPartition", Integer.TYPE)));

      getObjectProperty().setValue("customerCompany", ObjectLoader.getMap(Company.class, getObjectProperty().getValue("customerCompany", Integer.TYPE)));

      getObjectProperty().setValue("customerCfc", ObjectLoader.getMap(CFC.class, getObjectProperty().getValue("customerCfc", Integer.TYPE)));

      getObjectProperty().setValue("customerCompanyPartition", ObjectLoader.getMap(CompanyPartition.class, getObjectProperty().getValue("customerCompanyPartition", Integer.TYPE)));
      
      getObjectProperty().remove("deals");
      //getObjectProperty().setValue("deals", ObjectLoader.getList(Deal.class, getObjectProperty().getValue("deals", Integer[].class)));
      
      seller.reasonProperty().setValue(getObjectProperty().getValue("sellerReason", String.class));
      seller.fioProperty().setValue(getObjectProperty().getValue("sellerPerson", String.class));
      seller.companyProperty().setValue(getObjectProperty().getMap("sellerCompany"));
      
      if(!getObjectProperty().getMap("sellerCfc").isNullOrEmpty("id"))
        seller.cfcProperty().setValue(getObjectProperty().getMap("sellerCfc"));
      
      if(!getObjectProperty().getMap("sellerCompanyPartition").isNullOrEmpty("id"))
        seller.partitionProperty().setValue(getObjectProperty().getMap("sellerCompanyPartition"));
      
      getObjectProperty().get("sellerCompany").bind(seller.companyProperty());
      getObjectProperty().get("sellerCompanyPartition").bind(seller.partitionProperty());
      getObjectProperty().get("sellerCfc").bind(seller.cfcProperty());
      getObjectProperty().get("sellerPerson").bind(seller.fioProperty());
      getObjectProperty().get("sellerReason").bind(seller.reasonProperty());
      
      customer.reasonProperty().setValue(getObjectProperty().getValue("customerReason", String.class));
      customer.fioProperty().setValue(getObjectProperty().getValue("customerPerson", String.class));
      customer.companyProperty().setValue(getObjectProperty().getMap("customerCompany"));
      
      if(!getObjectProperty().getMap("customerCfc").isNullOrEmpty("id"))
        customer.cfcProperty().setValue(getObjectProperty().getMap("customerCfc"));
      
      if(!getObjectProperty().getMap("customerCompanyPartition").isNullOrEmpty("id"))
        customer.partitionProperty().setValue(getObjectProperty().getMap("customerCompanyPartition"));
      
      getObjectProperty().get("customerCompany").bind(customer.companyProperty());
      getObjectProperty().get("customerCompanyPartition").bind(customer.partitionProperty());
      getObjectProperty().get("customerCfc").bind(customer.cfcProperty());
      getObjectProperty().get("customerPerson").bind(customer.fioProperty());
      getObjectProperty().get("customerReason").bind(customer.reasonProperty());
      
      
      contractLabel.textProperty().bind(Bindings.createStringBinding(() -> {
        return (getObjectProperty().getMap("template").isNullOrEmpty("id") ? "Выберите тип договора..." : getObjectProperty().getMap("template").getValue("name"))
              + (getObjectProperty().isNullOrEmpty("number") ? "" : (" № "+getObjectProperty().getValue("number")));
      }, getObjectProperty().get("template"), getObjectProperty().getMap("template").get("name"), getObjectProperty().get("number")));
      
      endDateCheck.setSelected(!getObjectProperty().isNullOrEmpty("endDate"));
      endDate.disableProperty().bind(endDateCheck.selectedProperty().not());
      
      startDate.setValue(Utility.convert(getObjectProperty().getValue("startDate", new Date(System.currentTimeMillis()))));
      endDate.setValue(Utility.convert(getObjectProperty().getValue("endDate", new Date(System.currentTimeMillis()))));
      
      getObjectProperty().get("startDate").bind(startDate.valueProperty());
      getObjectProperty().get("endDate").bind(Bindings.createObjectBinding(() -> endDateCheck.isSelected() ? endDate.getValue() : null, endDate.valueProperty(), endDateCheck.selectedProperty()));
      
      /*Function<PropertyMap,Boolean> isCompanyDisable = (PropertyMap t) -> !ObjectLoader.getList(DBFilter.create(Deal.class)
              .AND_EQUAL("contract", getObjectProperty().getInteger("id"))
              .AND_EQUAL("tmp", false)
              .AND_EQUAL("type", Deal.Type.CURRENT), "id").isEmpty();
      
      seller.getCompanyLabel().disableProperty().bind(Bindings.createBooleanBinding(() -> isCompanyDisable.apply(getObjectProperty()), deals.getDealTable().getDateScale().datePeriods));
      customer.getCompanyLabel().disableProperty().bind(Bindings.createBooleanBinding(() -> isCompanyDisable.apply(getObjectProperty()), deals.getDealTable().getDateScale().datePeriods));*/
      
      contractLabel.disableProperty().bind(customer.getCompanyLabel().disableProperty());
      
      changeCompany.disableProperty().bind(((ObjectProperty<PropertyMap>)getObjectProperty().get("id")).isNotNull());
      
      planGrafic.disableProperty().bind(Bindings.createBooleanBinding(() -> 
                getObjectProperty().isNullOrEmpty("customerCompanyPartition") || getObjectProperty().getMap("customerCompanyPartition").isNullOrEmpty("id") ||
                getObjectProperty().isNullOrEmpty("sellerCompany") || getObjectProperty().getMap("sellerCompany").isNullOrEmpty("id") ||
                getObjectProperty().isNullOrEmpty("template") || getObjectProperty().getMap("template").isNullOrEmpty("id"), 
              getObjectProperty().get("customerCompanyPartition"), getObjectProperty().get("sellerCompany"), getObjectProperty().get("template")));
      
      Map<String,Object> m = getObjectProperty().getSimpleMap(true);
      setMementoProperty(getObjectProperty().copy());
    }catch (Exception ex) {
      MsgTrash.out(ex);
    }
    
    /*Executors.newSingleThreadExecutor().submit(() -> {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        Logger.getLogger(FXContract.class.getName()).log(Level.SEVERE, null, ex);
      }
      Platform.runLater(() -> deals.getDealTable().getDateScale().scrollToDate(LocalDate.now()));
    });*/
  }
  
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    storeControls().addAll(rootSplit, deals);
    planGrafic.setContent(deals.getContent());
    
    /*deals.getDealTable().getLeftTool().getItems().addAll(addProcess, removeProcess);
    deals.getDealTable().getLeftTable().getColumn("№").setVisible(false);
    deals.getDealTable().getRightTable().getColumn("Наименование").setVisible(false);
    deals.getDealTable().getRightTable().getColumns().add(Column.create("От покупателя", "customerpartitionname"));
    deals.getDealTable().getFields().add("sellerCfc");
    deals.getDealTable().getLeftTable().setEditable(true);
    deals.getDealTable().getLeftTable().getItems().addListener((ListChangeListener.Change<? extends DateRow> c) -> fill_cfc());
    deals.getDealTable().getDateScale().selectedPeriods.addListener((ListChangeListener.Change<? extends DealPeriod> c) -> fill_cfc());
    deals.getDealTable().getLeftTable().getColumns().add(col);
    deals.setPreviosMonth(60);
    deals.setNextMax(60);
    deals.setNextMonth(24);*/
    
    initData();
    initEvents();
  }
  
  private void fill_cfc() {
    deals.getItems().forEach(row -> set_cfc_to_row(row, ((ScaleRow<ScalePeriod>)row).getPeriods()));
    get_row_periods(deals.getScale().selectedPeriodProperty().getValue()).forEach((row,list) -> set_cfc_to_row(row, list));
  }
  
  private HashMap<ScaleRow<ScalePeriod>,ObservableList<ScalePeriod>> get_row_periods(List<? extends ScalePeriod> periods) {
    HashMap<ScaleRow<ScalePeriod>,ObservableList<ScalePeriod>> data = new DivisionMap<>();
    periods.forEach(p -> {
      ScaleRow<ScalePeriod> row = deals.getScale().rowAtPeriod(p);
      if(row != null) {
        if(!data.containsKey(row))
          data.put(row, FXCollections.observableArrayList());
        data.get(row).add(p);
      }
    });
    return data;
  }
  
  private void set_cfc_to_row(ScaleRow<ScalePeriod> row, List<? extends ScalePeriod> list) {
    row.setValue("Исполнитель", 
            !list.isEmpty() && list.stream().filter(p -> !list.get(0).equals(p) && !list.get(0).getInteger("sellerCfc").equals(p.getInteger("sellerCfc"))).count() == 0 ? 
                    cfccash.containsKey(list.get(0).getInteger("sellerCfc")) ? cfccash.get(list.get(0).getInteger("sellerCfc")):
                            cfccash.put(list.get(0).getInteger("sellerCfc"), ObjectLoader.getMap(CFC.class, list.get(0).getInteger("sellerCfc"))) : 
                    null);
  }
  
  private void selectTemplate() {
    FXTableEditor contractTypeTable = new FXTableEditor(XMLContractTemplate.class, null, FXCollections.observableArrayList("id","processes"), Column.create("Наименование", "name"));
    contractTypeTable.setDoubleClickActionType(FXEditor.DoubleClickActionType.SELECT);
    List<PropertyMap> list = contractTypeTable.getObjects(seller, "Выберите тип договора");
    if(list != null && !list.isEmpty())
      getObjectProperty().setValue("template", list.get(0));
  }
  
  private ScaleRow createContractProcess(PropertyMap partition, PropertyMap process) {
    try {
      ScaleRow row = (ScaleRow) deals.getScale().createRow()
              .setValue("customerPartition",     partition.getValue("id"))
              .setValue("customerpartitionname", partition.getValue("name"))
              .bind("contract",                  getObjectProperty().get("id"))
              .setValue("process",               process.getValue("id"))
              .setValue("service_name",          process.getValue("name"));
              
      row.bind("customerCompanyPartition", row.get("customerPartition"))
              .setValue("tempProcess", ObjectLoader.createObject(ContractProcess.class, row, true));
      //row.setValue("tempProcess", row.getValue("id"));
      return row;
    }catch(Exception ex) {MsgTrash.out(ex);}
    return null;
  }
  
  private void addProcess() {
    ChoiceBox<PropertyMap> partitions = new ChoiceBox<>();
    HBox hbox = new HBox(new Label("Обособленное подразделение: "), partitions);
    hbox.setAlignment(Pos.CENTER_RIGHT);
    hbox.setPadding(new Insets(5));

    ObjectLoader.fillList(DBFilter.create(CompanyPartition.class)
            .AND_EQUAL("company", getObjectProperty().getMap("customerCompany").getValue("id", Integer.TYPE))
            .AND_EQUAL("type", CompanyPartition.Type.CURRENT)
            .AND_EQUAL("tmp", false), partitions.getItems(), "id", "name", "mainPartition");
    
    ScaleRow item = deals.getLeftTable().getSelectionModel().getSelectedItem();
    if(item != null)
      partitions.getItems().stream().filter(p -> p.getValue("id").equals(item.getValue("sellerCompanyPartition"))).forEach(p -> partitions.getSelectionModel().select(p));
    else partitions.getItems().stream().filter(p -> p.getInteger("id").equals(getObjectProperty().getMap("customerCompanyPartition").getInteger("id"))).forEach(p -> partitions.getSelectionModel().select(p));
    //else partitions.getItems().stream().filter(p -> p.getValue("mainPartition", Boolean.TYPE)).forEach(p -> partitions.getSelectionModel().select(p));
    if(!partitions.getItems().isEmpty() && partitions.getSelectionModel().getSelectedItem() == null)
      partitions.getItems().stream().filter(p -> p.getValue("mainPartition", Boolean.TYPE)).forEach(p -> partitions.getSelectionModel().select(p));
      
    if(!partitions.getItems().isEmpty() && partitions.getSelectionModel().getSelectedItem() == null)
      partitions.getSelectionModel().select(0);

    Integer[] ids = new Integer[0];
    for(List d:ObjectLoader.getData(DBFilter.create(Product.class).AND_EQUAL("company", getObjectProperty().getMap("sellerCompany").getValue("id", Integer.TYPE)), "service"))
      ids = ArrayUtils.add(ids, (Integer)d.get(0));

    FXTreeEditor processes = new FXTreeEditor(Service.class, FXProcess.class);
    processes.getClientFilter().AND_IN("id", ids);
    processes.setBottom(hbox);
    processes.initData();

    processes.getStylesheets().addAll(((Parent)((BorderPane)getRoot()).getCenter()).getStylesheets());
    
    processes.setDoubleClickActionType(FXEditor.DoubleClickActionType.SELECT);
    
    List<PropertyMap> objects = processes.getObjects(getRoot(), "Выберите процесс");
    if(objects != null && !objects.isEmpty()) {
      processes.getTree().getSelectionModel().getSelectedItems().stream().forEach(it -> {
        ((FXDivisionTree<PropertyMap>)processes.getTree()).listItems(it).stream().filter(p -> p.getChildren().isEmpty()).forEach(p -> 
          deals.getLeftTable().getSourceItems().add(createContractProcess(partitions.getSelectionModel().getSelectedItem(), p.getValue())));
      });
    }
  }
  
  private void removeProcess() {
    ObservableList<PropertyMap> dlist;
    Integer[] contractProcesses;
    if((contractProcesses = deals.getSelectedContractProcess()).length > 0 && 
            new Alert(Alert.AlertType.CONFIRMATION, "Удалить выделленые строки"+
                    (!(dlist = ObjectLoader.getList(DBFilter.create(Deal.class).AND_IN("tempProcess", deals.getSelectedContractProcess()), "id")).isEmpty() ? " и сделки в них" : "")+"?"
                    , ButtonType.YES, ButtonType.CANCEL).showAndWait().get() == ButtonType.YES) {
      try {
        List<Integer> dl = new ArrayList();
        dlist.stream().forEach(d -> dl.add(d.getValue("id", Integer.TYPE)));
        ObjectLoader.removeObjects(Deal.class, true, dl.toArray(new Integer[0]));
        ObjectLoader.removeObjects(ContractProcess.class, true, contractProcesses);
        deals.getLeftTable().getSourceItems().removeAll(deals.getLeftTable().getSelectionModel().getSelectedItems());
      }catch(Exception ex) {
        MsgTrash.out(ex);
      }
    }
  }

  @Override
  public void dispose() {
    deals.dispose();
    super.dispose();
  }
  
  private void validateContractData(boolean removetemplate) {
    if(!getObjectProperty().isNullOrEmpty("sellerCompanyPartition") && !getObjectProperty().getMap("sellerCompanyPartition").isNullOrEmpty("id") &&
            !getObjectProperty().isNullOrEmpty("customerCompanyPartition") && !getObjectProperty().getMap("customerCompanyPartition").isNullOrEmpty("id") &&
            !getObjectProperty().isNullOrEmpty("template") && !getObjectProperty().getMap("template").isNullOrEmpty("id")) {
      
      if(removetemplate) {
        try {
          List<Integer> ids = new ArrayList<>();
          deals.getLeftTable().getSourceItems().stream().forEach(cp -> ids.add(cp.getValue("tempProcess", Integer.TYPE)));
          System.out.println("ids = "+ids);
          if(!ids.isEmpty()) {
            ObjectLoader.removeObjects(ContractProcess.class, true, ids);
            deals.getLeftTable().getSourceItems().clear();
          }
        }catch(Exception ex) {
          MsgTrash.out(ex);
        }
      }
      
      save(getObjectProperty().isNullOrEmpty("id"));
      getObjectProperty().setValue("number", ObjectLoader.getSingleData(Contract.class, getObjectProperty().getValue("id", Integer.TYPE), "number")[0]);
      
      if(deals.getLeftTable().getSourceItems().isEmpty() || removetemplate) {
        ObjectLoader.getList(Service.class, getObjectProperty().getMap("template").getValue("processes", Integer[].class), "id", "name").stream().forEach(p -> 
                deals.getLeftTable().getSourceItems().add(createContractProcess(getObjectProperty().getMap("customerCompanyPartition"), p)));
        deals.changeContract(new Integer[]{getObjectProperty().getValue("id", Integer.TYPE)}, true);
      }
    }
  }

  private void initEvents() {
    col.setEditable(true);
    col.choisevalues().setAll(getObjectProperty().getMap("sellerCompany").isNull("cfcs") ? FXCollections.observableArrayList() : ObjectLoader.getList(CFC.class, getObjectProperty().getMap("sellerCompany").getValue("cfcs", Integer[].class)));
    
    /*col.setOnEditCommit(e -> {
      ScaleRow<ScalePeriod> row = deals.getLeftTable().getSelectionModel().getSelectedItem();
      List<ScalePeriod> list = ((ObservableList<ScalePeriod>)row.getPeriods()).filtered(p -> p.selectedProperty().getValue());
      list = list.isEmpty() ? 
              new Alert(Alert.AlertType.CONFIRMATION, "Изменить исполнителя для всех сделок в линейке?", ButtonType.YES, ButtonType.CANCEL).showAndWait().get() == ButtonType.YES ? 
                (ObservableList<DealPeriod>)row.getPeriods() : 
                list : 
              list;
      RemoteSession session = ObjectLoader.createSession();
      fill_cfc();
    });*/
    
    
    getObjectProperty().get("template", PropertyMap.class).addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      //if(oldValue == null || newValue != null &&  oldValue.getInteger("id").intValue() != newValue.getInteger("id").intValue())
      if(!Objects.equals(oldValue, newValue))
        validateContractData(true);
    });
    getObjectProperty().get("sellerCompanyPartition", PropertyMap.class).addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      //if(oldValue == null || newValue != null &&  oldValue.getInteger("id").intValue() != newValue.getInteger("id").intValue())
      if(!Objects.equals(oldValue, newValue))
        validateContractData(false);
    });
    getObjectProperty().get("customerCompany", PropertyMap.class).addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      //if(oldValue == null || newValue != null &&  oldValue.getInteger("id").intValue() != newValue.getInteger("id").intValue())
      if(!Objects.equals(oldValue, newValue))
        validateContractData(true);
    });
    getObjectProperty().get("customerCompanyPartition", PropertyMap.class).addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      //if(oldValue == null || newValue != null && oldValue.getInteger("id").intValue() != newValue.getInteger("id").intValue())
      if(!Objects.equals(oldValue, newValue))
        validateContractData(false);
    });
    
    changeCompany.setOnAction(e -> {
      PropertyMap sellerCompany   = seller.companyProperty().getValue();
      PropertyMap sellerCfc       = seller.cfcProperty().getValue();
      PropertyMap sellerPartition = seller.partitionProperty().getValue();
      String sellerfio            = seller.fioProperty().getValue();
      String sellerReason         = seller.reasonProperty().getValue();
      
      seller.reasonProperty().setValue(getObjectProperty().getValue("customerReason", String.class));
      seller.fioProperty().setValue(getObjectProperty().getValue("customerPerson", String.class));
      seller.companyProperty().setValue(getObjectProperty().getMap("customerCompany"));
      seller.cfcProperty().setValue(getObjectProperty().getMap("customerCfc"));
      seller.partitionProperty().setValue(getObjectProperty().getMap("customerCompanyPartition"));
      
      customer.reasonProperty().setValue(sellerReason);
      customer.fioProperty().setValue(sellerfio);
      customer.companyProperty().setValue(sellerCompany);
      customer.cfcProperty().setValue(sellerCfc);
      customer.partitionProperty().setValue(sellerPartition);
    });
    
    //deals.showContractDoubleClickProperty().setValue(false);
    //deals.showCompanyDoubleClickProperty().setValue(false);
    
    contractLabel.setOnMouseClicked(e -> selectTemplate());
  }
}