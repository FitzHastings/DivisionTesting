package division.fx.client.plugins.product;

import bum.editors.util.ObjectLoader;
import bum.interfaces.Equipment;
import bum.interfaces.Store;
import division.fx.DivisionTextField;
import division.fx.FXButton;
import division.fx.FXUtility;
import division.fx.PropertyMap;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.client.plugins.deal.master.FXDealPositions;
import division.fx.controller.store.EquipmentFactorTable;
import division.fx.editor.FXTableEditor;
import division.fx.gui.FXStorable;
import division.fx.table.Column;
import division.fx.table.FXDivisionTable;
import division.fx.table.filter.ListFilter;
import division.fx.table.filter.TextFilter;
import division.util.IDStore;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.converter.BigDecimalStringConverter;
import mapping.MappingObject;
import util.filter.local.DBFilter;

public class FXStoreForProducts extends BorderPane implements FXStorable {
  private final ObjectProperty<PropertyMap> groupProperty     = new SimpleObjectProperty<>();
  private final ObjectProperty<PropertyMap> partitionProperty = new SimpleObjectProperty<>();
  
  private boolean selectable = true;
  private final EquipmentFactorTable equipmentFactorTable = new EquipmentFactorTable();
  private final FXTableEditor equipmentTable = new FXTableEditor(Equipment.class, null, 
          FXCollections.observableArrayList(
                  "id",
                  "group_name",
                  "factors",
                  "unit:=:group-unit",
                  "identity_value:=:identity_value_name",
                  "values=query:(select array_agg('factor-'||[EquipmentFactorValue(factor)]||'='||name) from [EquipmentFactorValue] where [EquipmentFactorValue(equipment)]=[Equipment(id)])",
                  "partition=query:(SELECT [DealPosition(deal)] FROM [DealPosition] WHERE id=[Equipment(sourceDealPosition)])",
                  "store",
                  "reserve",
                  "group"), 
          MappingObject.Type.CURRENT, false, 
          Column.create("Подразделение" , "partition-name=query:select name from [CompanyPartition] where id=[Equipment(owner-id)]", new ListFilter("partition-name")),
          Column.create("Хранилище"     , "store-name", new ListFilter("store-name")),
          Column.create("Количество"    , "amount"    , new TextFilter("amount", new BigDecimalStringConverter())),
          Column.create("Доступно"      , "free=query:[Equipment(amount)]-[Equipment(reserve)]", new TextFilter("free", new BigDecimalStringConverter()))) {
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
  
  private final SplitPane                         factorSplit      = new SplitPane(equipmentTable.getTable(), equipmentFactorTable.getTable());
  
  
  private final Label                             groupLabel       = new Label();
  private final DivisionTextField<BigDecimal>     countField       = new DivisionTextField(new BigDecimalStringConverter());
  private final FXButton                          addToDealButton  = new FXButton(e -> addPosition(), "add-position-button");
  private final FXButton                          removeDealButton = new FXButton(e -> removePosition(), "remove-position-button");
  private final EquipmentFactorTable              positionTable    = new EquipmentFactorTable();
  
  private final TitleBorderPane                   storePane    = new TitleBorderPane(factorSplit, "Активы");
  private final TitleBorderPane                   positionPane = new TitleBorderPane(positionTable, groupLabel,countField,addToDealButton,removeDealButton);
  
  private final SplitPane                         basketSplit      = new SplitPane(storePane, positionPane);
  
  
  public FXStoreForProducts() {
    FXUtility.initMainCss(this);
    setCenter(basketSplit);
    
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
    
    countField.disableProperty().bind(groupProperty.isNull());
    
    removeDealButton.disableProperty().bind(Bindings.createBooleanBinding(() -> positionTable.getTable().getSelectionModel().selectedItemProperty().getValue() == null, positionTable.getTable().getSelectionModel().selectedItemProperty()));
    
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
    
    FXDivisionTable.setGeneralItems(equipmentTable.getTable(), equipmentFactorTable.getTable());
    
    equipmentFactorTable.getItems().addListener((ListChangeListener.Change<? extends PropertyMap> c) -> {
      FXDivisionTable.bindScrollBars(Orientation.VERTICAL, equipmentTable.getTable(), equipmentFactorTable.getTable());
      if(FXDivisionTable.getScrollBar(equipmentTable.getTable(), Orientation.VERTICAL) != null) {
        FXDivisionTable.getScrollBar(equipmentTable.getTable(), Orientation.VERTICAL).setPrefWidth(0);
        FXDivisionTable.getScrollBar(equipmentTable.getTable(), Orientation.VERTICAL).setMinWidth(0);
        FXDivisionTable.getScrollBar(equipmentTable.getTable(), Orientation.VERTICAL).setMaxWidth(0);
      }
    });
    
    equipmentFactorTable.setRootColumnVisible(false);
    equipmentFactorTable.getTable().getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    equipmentFactorTable.getTable().setEditable(false);
    equipmentFactorTable.getTable().setHorizontScrollBarPolicyAlways();
    equipmentFactorTable.amountColumnVisibleProperty().setValue(false);
    equipmentFactorTable.nameColumnVisibleProperty().setValue(false);
    
    positionTable.setRootColumnVisible(false);
    positionTable.getTable().getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    positionTable.getTable().setEditable(false);
    
    equipmentTable.setSelectionMode(SelectionMode.MULTIPLE);
    equipmentTable.getTable().setHorizontScrollBarPolicyAlways();
    
    equipmentTable.addInitDataListener(e -> {
      equipmentFactorTable.initHeader();
      recountEquipments();
    });
    
    basketSplit.setOrientation(Orientation.VERTICAL);
    VBox.setVgrow(positionTable, Priority.ALWAYS);
    //addToDealBox.setAlignment(Pos.CENTER_RIGHT);
    
    removeDealButton.disableProperty().bind(positionTable.getTable().getSelectionModel().selectedItemProperty().isNull());
    
    groupLabel.textProperty().bind(Bindings.createStringBinding(() -> groupProperty().getValue() == null ? "" : groupProperty.getValue().getString("name"), groupProperty));
    
    groupProperty.addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      countField.setValue(null);
      initData();
    });
  }
  
  public ObjectProperty<ObservableList<PropertyMap>> positionsProperty() {
    return positionTable.getTable().itemsProperty();
  }
  
  public void clearPosition() {
    positionTable.clear();
  }
  
  private void recountEquipments() {
    positionTable.getTable().getSourceItems().filtered(p -> p.isNotNull("equipment-id")).forEach(p -> {
      equipmentTable.getTable().getSourceItems().stream().filter(e -> e.getInteger("id").equals(p.getInteger("equipment-id"))).forEach(e -> {
        e.setValue("amount", e.getBigDecimal("amount").subtract(p.getBigDecimal("amount")));
      });
    });
    
    equipmentTable.getTable().getSourceItems().removeAll(equipmentTable.getTable().getSourceItems().filtered(e -> e.getBigDecimal("amount").compareTo(BigDecimal.ZERO) == 0));
  }
  
  private void addPosition() {
    if(groupProperty().getValue() != null) {
      //if(productProperty().getValue().isNotNull("group")) {
        
        // Ничего не выделено
          // Добавляем позицию с колличеством из поля
        if(equipmentTable.selectedItemProperty().getValue() == null)
          positionTable.getTable().getSourceItems().add(
                  PropertyMap.create()
                          .setValue("id", IDStore.createIntegerID("position")*-1)
                          .setValue("group", groupProperty.getValue().getInteger("id"))
                          .setValue("group_name", groupProperty.getValue().getString("name"))
                          .setValue("equipment-id", IDStore.createIntegerID("zakaz")*-1)
                          .setValue("amount", countField.getValue() == null || countField.getValue().compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ONE : countField.getValue()));
        else {
          // Выделено много объектов
            // Добавляем позиции с тем количеством которое у них есть
          ObservableList<PropertyMap> eq = equipmentTable.getTable().getSelectionModel().getSelectedItems();
          if(eq.size() > 1) {
            eq.forEach(equipment -> {
              positionTable.getTable().getSourceItems().add(
                      PropertyMap.create().copyFrom(equipment)
                              .setValue("id", IDStore.createIntegerID("position")*-1)
                              .setValue("group", groupProperty.getValue().getInteger("id"))
                              .setValue("equipment-id", equipment.getInteger("id")));
            });
            equipmentTable.getTable().getSourceItems().removeAll(eq);
          }else {
            // Выделен один объект
            BigDecimal positionCount = countField.getValue() == null || countField.getValue().compareTo(BigDecimal.ZERO) <= 0 ? eq.get(0).getBigDecimal("amount") : 
                    (countField.getValue().compareTo(eq.get(0).getBigDecimal("amount")) < 0 && eq.get(0).isNotNull("identity_value") ? eq.get(0).getBigDecimal("amount") : countField.getValue());
            
            BigDecimal zakazCount = eq.get(0).getBigDecimal("amount").compareTo(positionCount) < 0 ? positionCount.subtract(eq.get(0).getBigDecimal("amount")) : BigDecimal.ZERO;
            positionCount = zakazCount.compareTo(BigDecimal.ZERO) > 0 ? positionCount.subtract(zakazCount) : positionCount;
            
            if(positionCount.compareTo(BigDecimal.ZERO) > 0) {
              positionTable.getTable().getSourceItems().add(
                      PropertyMap.create()
                              .copyFrom(eq.get(0))
                              .setValue("id", IDStore.createIntegerID("position")*-1)
                              .setValue("equipment-id", eq.get(0).getInteger("id"))
                              .setValue("group", groupProperty.getValue().getInteger("id"))
                              .setValue("amount", positionCount));
              eq.get(0).setValue("amount", eq.get(0).getBigDecimal("amount").subtract(positionCount));
              if(eq.get(0).getBigDecimal("amount").compareTo(BigDecimal.ZERO) == 0)
                equipmentTable.getTable().getSourceItems().remove(eq.get(0));
            }
            
            if(zakazCount.compareTo(BigDecimal.ZERO) > 0)
              positionTable.getTable().getSourceItems().add(
                      PropertyMap.create()
                          .setValue("id", IDStore.createIntegerID("position")*-1)
                          .setValue("equipment-id", IDStore.createIntegerID("zakaz")*-1)
                          .setValue("group", groupProperty.getValue().getInteger("id"))
                          .setValue("group_name", groupProperty.getValue().getString("name"))
                          .setValue("amount", zakazCount));
            
          }
        }
      /*}else {
        positionTable.getTable().getSourceItems().add(
                productProperty().getValue().copy()
                .setValue("id", IDStore.createIntegerID("position")*-1)
                .setValue("product-id", productProperty().getValue().getInteger("id"))
                .setValue("amount", countField.getValue() == null || countField.getValue().compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ONE : countField.getValue()));
      }*/
      positionTable.initHeader();
    }
  }
  
  private void removePosition() {
    if(positionTable.getTable().getSelectionModel().getSelectedItem() != null) {
      ObservableList<PropertyMap> eq = PropertyMap.copyList(positionTable.getTable().getSelectionModel().getSelectedItems());
      eq.stream().forEach(p -> {
        if(p.isNotNull("equipment-id") && p.getInteger("equipment-id") >= 0) {
          PropertyMap equipment = equipmentTable.getTable().getSourceItems().stream().filter(e -> e.getInteger("id").equals(p.getInteger("equipment-id"))).findFirst().orElseGet(() -> null);
          if(equipment != null)
            equipment.setValue("amount", equipment.getBigDecimal("amount").add(p.getBigDecimal("amount")));
          else equipmentTable.getTable().getSourceItems().add(p.copy().setValue("id", p.getInteger("equipment-id")).remove("equipment-id","product-id"));
        }
        positionTable.getTable().getSourceItems().remove(p);
      });
      positionTable.initHeader();
    }
  }
  
  private DBFilter storeFilter    = equipmentTable.getClientFilter().AND_FILTER();
  
  public void initData() {
    equipmentTable.clearData();
    equipmentTable.activeProperty().setValue(groupProperty().getValue() != null && partitionProperty.getValue() != null);
    storeFilter.clear();
    if(equipmentTable.isActive()) {
      storeFilter.AND_IN("store", 
              ObjectLoader.getList(DBFilter.create(Store.class).AND_EQUAL("companyPartition", partitionProperty.getValue().getInteger("id")), "id"))
              .AND_EQUAL("group", groupProperty().getValue().getInteger("id"));

    }
    equipmentTable.initData();
  }
  
  @Override
  public List<Node> storeControls() {
    basketSplit.setId("basketSplit");
    factorSplit.setId("factorSplit");
    positionTable.setId("positionTable");
    return Arrays.asList(factorSplit, equipmentTable.getTable(), equipmentFactorTable.getTable(), basketSplit, positionTable);
  }
  
  public ObjectProperty<PropertyMap> groupProperty() {
    return groupProperty;
  }
  
  public ObjectProperty<PropertyMap> partitionProperty() {
    return partitionProperty;
  }
}