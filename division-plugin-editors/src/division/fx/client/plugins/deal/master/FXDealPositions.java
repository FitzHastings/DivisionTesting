package division.fx.client.plugins.deal.master;

import bum.interfaces.Equipment;
import division.fx.PropertyMap;
import division.fx.controller.store.EquipmentFactorTable;
import division.fx.editor.FXTableEditor;
import division.fx.gui.FXStorable;
import division.fx.table.Column;
import division.fx.table.FXDivisionTable;
import division.fx.table.filter.ListFilter;
import division.fx.table.filter.TextFilter;
import java.util.Arrays;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.util.converter.BigDecimalStringConverter;
import mapping.MappingObject;

public class FXDealPositions extends BorderPane implements FXStorable {
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
  
  private final SplitPane factorSplit = new SplitPane(equipmentTable.getTable(), equipmentFactorTable.getTable());
  
  private final ObservableList<Node> stories = FXCollections.observableArrayList();

  public FXDealPositions() {
    setCenter(factorSplit);
    
    stories.addAll(equipmentTable, factorSplit);
    
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
    
    equipmentTable.setSelectionMode(SelectionMode.MULTIPLE);
    equipmentTable.getTable().setHorizontScrollBarPolicyAlways();
    
    equipmentTable.addInitDataListener(e -> equipmentFactorTable.initHeader());
  }

  public EquipmentFactorTable getEquipmentFactorTable() {
    return equipmentFactorTable;
  }

  public FXTableEditor getEquipmentTable() {
    return equipmentTable;
  }
  
  @Override
  public List<Node> storeControls() {
    return stories;
  }
}