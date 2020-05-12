package division.fx.controller.store;

import bum.editors.util.ObjectLoader;
import bum.interfaces.CompanyPartition;
import bum.interfaces.Equipment;
import bum.interfaces.Group;
import bum.interfaces.Product;
import bum.interfaces.Store;
import division.fx.FXUtility;
import division.fx.PropertyMap;
import division.fx.client.plugins.deal.TreeFilter;
import division.fx.dialog.FXD;
import division.fx.editor.FXTableEditor;
import division.fx.editor.FXTreeEditor;
import division.fx.editor.Storable;
import division.fx.table.Column;
import division.fx.table.FXDivisionTable;
import division.fx.table.filter.ListFilter;
import division.fx.table.filter.TextFilter;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.layout.BorderPane;
import javafx.util.converter.BigDecimalStringConverter;
import mapping.MappingObject.Type;
import util.filter.local.DBFilter;

public class FXCompanyStoreTable extends BorderPane implements Storable {
  private final ObjectProperty<PropertyMap> companyPartitionProperty = new SimpleObjectProperty<>();
  private final ObjectProperty<PropertyMap> processProperty          = new SimpleObjectProperty<>();
  
  private boolean selectable = true;
  
  private FXTreeEditor  groupTree;
  private FXTableEditor equipmentTable;
  private EquipmentFactorTable equipmentFactorTable;
  private SplitPane treeSplit;
  
  private ObservableList<PropertyMap> productGroups = FXCollections.observableArrayList();

  public FXCompanyStoreTable() {
    initialize();
    initEvents();
    FXUtility.initMainCss(this);
  }
  
  public ObjectProperty<PropertyMap> companyPartitionProperty() {
    return companyPartitionProperty;
  }
  
  public ObjectProperty<PropertyMap> processProperty() {
    return processProperty;
  }

  @Override
  public ObservableList storeControls() {
    return FXCollections.observableArrayList(treeSplit, equipmentTable);
  }

  public boolean isUpdate() {
    return false;
  }

  public void savePartitionStore() {
  }

  private void initialize() {
    groupTree = new FXTreeEditor(Group.class);
    //groupTree.getTree().setShowRoot(false);
    groupTree.titleProperty().setValue("Объекты");
    groupTree.setSelectionMode(SelectionMode.MULTIPLE);
    
    groupTree.getTree().setCellFactory((TreeView<PropertyMap> param) -> {
      TreeCell<PropertyMap> cell = new TextFieldTreeCell<PropertyMap>() {
        @Override
        public void updateItem(PropertyMap item, boolean empty) {
          /*getStyleClass().remove("product-group");
          if(item != null && !productGroups.filtered(pg -> pg.getInteger("group").equals(item.getInteger("id"))).isEmpty()) {
            getStyleClass().add("product-group");
            System.out.println("!!!!!!!!!!! - "+item.getString("name"));
          }*/
          super.updateItem(item, empty);
        }
      };
      return cell;
    });
    
    equipmentFactorTable = new EquipmentFactorTable();
    equipmentTable = new FXTableEditor(Equipment.class, null, 
            FXCollections.observableArrayList(
                    "id",
                    "factors",
                    "unit:=:group-unit",
                    "identity_value:=:identity_value_name",
                    "values=query:(select array_agg('factor-'||[EquipmentFactorValue(factor)]||'='||name) from [EquipmentFactorValue] where [EquipmentFactorValue(equipment)]=[Equipment(id)])",
                    "partition=query:(SELECT [DealPosition(deal)] FROM [DealPosition] WHERE id=[Equipment(sourceDealPosition)])",
                    "store",
                    "group"), 
            Type.CURRENT, false, 
            Column.create("Наименование"  , "group_name", new ListFilter("group_name")),
            Column.create("Склад"         , "store-name"),
            Column.create("Количество"    , "amount"    , new TextFilter("amount", new BigDecimalStringConverter())),
            Column.create("Заблокировано" , "reserve"   , new TextFilter("reserve", new BigDecimalStringConverter())),
            Column.create("Доступно"      , new TextFilter("Доступно", new BigDecimalStringConverter())),
            Column.create("Цена"          , new TextFilter("Цена"    , new BigDecimalStringConverter()))) {
              @Override
              protected void transformItem(PropertyMap s) {
                if(s.getArray("values", String.class).length > 0) {
                  Arrays.stream(s.getArray("values", String.class)).filter(a -> a!= null).forEach(a -> {
                    if(a.split("=").length == 2)
                      s.setValue(a.split("=")[0], a.split("=")[1]);
                  });
                }
              }
            };
    
    equipmentTable.getColumn("Цена").visibleProperty().bind(processProperty().isNotNull());
    
    ((TableColumn<PropertyMap,BigDecimal>)equipmentTable.getColumn("Доступно")).setCellValueFactory((TableColumn.CellDataFeatures<PropertyMap, BigDecimal> param) -> new SimpleObjectProperty<>(param.getValue().getBigDecimal("amount").subtract(param.getValue().getBigDecimal("reserve"))));
    
    equipmentFactorTable.setColumnVisible(false, "Наименование","Кол.");
    
    equipmentFactorTable.setRootColumnVisible(false);
    equipmentFactorTable.getTable().getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    equipmentTable.setSelectionMode(SelectionMode.MULTIPLE);
    equipmentFactorTable.getTable().setEditable(true);
    equipmentFactorTable.getTable().setHorizontScrollBarPolicyAlways();
    equipmentTable.getTable().setHorizontScrollBarPolicyAlways();
    
    
    FXDivisionTable.setGeneralItems(equipmentTable.getTable(), equipmentFactorTable.getTable());
    
    equipmentFactorTable.getItems().addListener((ListChangeListener.Change<? extends PropertyMap> c) -> {
      FXDivisionTable.bindScrollBars(Orientation.VERTICAL, equipmentTable.getTable(), equipmentFactorTable.getTable());
      if(FXDivisionTable.getScrollBar(equipmentTable.getTable(), Orientation.VERTICAL) != null) {
        FXDivisionTable.getScrollBar(equipmentTable.getTable(), Orientation.VERTICAL).setPrefWidth(0);
        FXDivisionTable.getScrollBar(equipmentTable.getTable(), Orientation.VERTICAL).setMinWidth(0);
        FXDivisionTable.getScrollBar(equipmentTable.getTable(), Orientation.VERTICAL).setMaxWidth(0);
      }
    });
    
    treeSplit = new SplitPane(groupTree, equipmentTable.getTable(), equipmentFactorTable);
    setCenter(treeSplit);
    
    groupTree.initData();
  }
  
  private void initData() {
    equipmentTable.clearData();
    equipmentTable.getClientFilter().clear();
    if(companyPartitionProperty().getValue() != null && groupTree.getSelectedIds().length > 0)
      equipmentTable.getClientFilter().AND_IN("group", groupTree.getSelectedIds()).AND_IN("store", ObjectLoader.getList(DBFilter.create(Store.class).AND_EQUAL("companyPartition", companyPartitionProperty().getValue().getInteger("id")), "id"));
    equipmentTable.initData();
  }

  private void initEvents() {
    equipmentTable.activeProperty().bind(companyPartitionProperty.isNotNull().and(groupTree.selectedItemProperty().isNotNull()));
    
    companyPartitionProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      productGroups.clear();
      equipmentTable.getTable().addFilter(equipmentTable.getColumn("Склад"), new TreeFilter(DBFilter.create(Store.class).AND_EQUAL("companyPartition", newValue.getInteger("id")), "store"));
      
      if(processProperty().getValue() != null && newValue != null) {
        equipmentTable.getColumn("Цена").setDatabaseColumnName("cost=query:select cost from product where [Product(group)]=[Equipment(group)] and tmp=false and type='CURRENT' and [Product(service)]="+processProperty().getValue().getInteger("id")
                +" and [Product(company)]=(select [CompanyPartition(company)] from [CompanyPartition] where id="+companyPartitionProperty().getValue().getInteger("id")+")");
        
        productGroups = ObjectLoader.getList(DBFilter.create(Product.class).AND_EQUAL("service", processProperty().getValue().getInteger("id"))
                .AND_EQUAL("company", ObjectLoader.getMap(CompanyPartition.class, companyPartitionProperty().getValue().getInteger("id"), "company").getInteger("company")), "group");
      }
      initData();
    });
    
    processProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      equipmentTable.getColumn("Цена").setDatabaseColumnName(null);
      productGroups.clear();
      
      if(newValue != null && companyPartitionProperty().getValue() != null) {
        equipmentTable.getColumn("Цена").setDatabaseColumnName("cost=query:select cost from product where [Product(group)]=[Equipment(group)] and tmp=false and type='CURRENT' and [Product(service)]="+processProperty().getValue().getInteger("id")
                +" and [Product(company)]=(select [CompanyPartition(company)] from [CompanyPartition] where id="+companyPartitionProperty().getValue().getInteger("id")+")");
        
        productGroups = ObjectLoader.getList(DBFilter.create(Product.class).AND_EQUAL("service", processProperty().getValue().getInteger("id"))
                .AND_EQUAL("company", ObjectLoader.getMap(CompanyPartition.class, companyPartitionProperty().getValue().getInteger("id"), "company").getInteger("company")), "group");
      }
      equipmentTable.initData();
    });
    
    groupTree.selectedItemProperty().addListener((ObservableValue<? extends TreeItem<PropertyMap>> observable, TreeItem<PropertyMap> oldValue, TreeItem<PropertyMap> newValue) -> initData());
    
    equipmentTable.addInitDataListener(e -> equipmentFactorTable.initHeader());
    
    equipmentTable.getTable().getSelectionModel().getSelectedIndices().addListener((ListChangeListener.Change<? extends Integer> c) -> {
      if(selectable) {
        selectable = false;
        equipmentFactorTable.getTable().getSelectionModel().clearSelection();
        c.getList().stream().forEach(i -> equipmentFactorTable.getTable().getSelectionModel().select(i));
        selectable = true;
      }
    });
    
    equipmentFactorTable.getTable().getSelectionModel().getSelectedIndices().addListener((ListChangeListener.Change<? extends Integer> c) -> {
      if(selectable) {
        selectable = false;
        equipmentTable.getTable().getSelectionModel().clearSelection();
        c.getList().stream().forEach(i -> equipmentTable.getTable().getSelectionModel().select(i));
        selectable = true;
      }
    });
  }
  
  public List<PropertyMap> get(String title, Node parent) {
    FXD fxd = FXD.create(title,parent,this,FXD.ButtonType.OK);
    fxd.setOnShowing(e -> load(title));
    fxd.setOnHiding(e -> store(title));
    return fxd.showDialog() == FXD.ButtonType.OK ? equipmentTable.getSelectedObjects() : FXCollections.observableArrayList();
  }
}