package division.fx.client.plugins.deal;

import bum.editors.util.ObjectLoader;
import bum.interfaces.Deal;
import bum.interfaces.DealPosition;
import bum.interfaces.EquipmentFactorValue;
import bum.interfaces.Group;
import bum.interfaces.Service;
import division.fx.controller.store.EquipmentFactorTable;
import division.fx.editor.FXTableEditor;
import division.fx.table.Column;
import division.fx.table.FXDivisionTable;
import division.fx.PropertyMap;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.gui.FXDisposable;
import division.fx.gui.FXStorable;
import division.fx.table.FXDivisionTableCell;
import division.fx.table.filter.TextFilter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.BorderPane;
import org.apache.commons.lang3.ArrayUtils;
import util.filter.local.DBFilter;

public class DealPositionTable extends BorderPane implements FXStorable, FXDisposable {
  private FXTableEditor positionTable = new FXTableEditor(DealPosition.class, null, 
                  FXCollections.observableArrayList(
                          "id",
                          "deal",
                          "equipment",
                          "iden-id:=:identity_id",
                          "identity_value",
                          "factors",
                          "factor_values",
                          "process",
                          "group_id",
                          "dispatchId",
                          "amount"
                  ),
          Column.create("собственник", "собственник=query:getCompanypartitionName((select [Store(companyPartition)] from [Store] where id=[DealPosition(equipment-store)]))"),
          Column.create("Источник",    "Источник=query:[DealPosition(sourceStore)]||'-'||getCompanypartitionName((select [Store(companyPartition)] from [Store] where id=[DealPosition(sourceStore)]))"),
          Column.create("Назначение",  "Назначение=query:[DealPosition(targetStore)]||'-'||getCompanypartitionName((select [Store(companyPartition)] from [Store] where id=[DealPosition(targetStore)]))"),
                  Column.create("Организация", "Организация=query:select name from [Company] where id=(select [Deal(customerCompany)] from [Deal] where id=[DealPosition(deal)])", new TextFilter("Организация")),
                  Column.create("Процесс", "service_name", new TreeFilter(Service.class, "process")),
                  Column.create("Наименование", "group_name", new TreeFilter(Group.class, "group_id")),
                  Column.create("Кол.", "amount"),
                  Column.create("Цена за ед.","customProductCost",true,true),
                  Column.create("Стоимость")) {
                    @Override
                    protected void transformItem(PropertyMap o) {
                      if(o.isNotNull("factor_values")) {
                        for(String factorValue:o.getArray("factor_values", String.class)) {
                          String[] fv = factorValue.split(":");
                          o.setValue("factor-"+fv[0], fv.length > 1 ? fv[1] : null);
                        }
                      }
                    }
                  };
  
  private EquipmentFactorTable equipmentFactorTable = new EquipmentFactorTable();
  private SplitPane split = new SplitPane(positionTable, equipmentFactorTable);
  private boolean is = true;
  
  private TitleBorderPane root = new TitleBorderPane(split, "Позиции сделок");

  public DealPositionTable() {
    setCenter(root);
    
    root.addToTitle(positionTable.getTools().getItems().stream().filter(n -> !(n instanceof Separator)).collect(Collectors.toList()).toArray(new Node[0]));
    positionTable.getTools().getItems().clear();

    ((TableColumn<PropertyMap,BigDecimal>)positionTable.getTable().getColumn("Стоимость")).setCellValueFactory((TableColumn.CellDataFeatures<PropertyMap, BigDecimal> param) -> 
            new SimpleObjectProperty(param.getValue().getBigDecimal("amount").multiply(param.getValue().getBigDecimal("customProductCost"))));
    
    positionTable.getTable().getColumns().stream().forEach(c -> {
      ((TableColumn)c).setCellFactory((Object param) -> new FXDivisionTableCell() {
        @Override
        protected void updateItem(Object item, boolean empty) {
          super.updateItem(item, empty);
          if(!empty && item != null && getRowItem() != null) {
            getStyleClass().remove("position-dispatch");
            if(!getRowItem().isNull("dispatchId"))
              getStyleClass().add("position-dispatch");
          }
        }
      });
    });
    
    setTop(positionTable.getTools());
    
    positionTable.editableInTableProperty.setValue(true);
    positionTable.setSelectionMode(SelectionMode.MULTIPLE);
    equipmentFactorTable.getTable().getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    equipmentFactorTable.getTable().getSelectionModel().setCellSelectionEnabled(true);
    
    positionTable.getTable().setHorizontScrollBarPolicyAlways();
    equipmentFactorTable.getTable().setHorizontScrollBarPolicyAlways();
    
    equipmentFactorTable.setRootColumnVisible(false);
    equipmentFactorTable.setIndentivicatorsVisible(true);
    equipmentFactorTable.setColumnVisible(false, "Наименование", "Кол.");
    
    FXDivisionTable.setGeneralItems(positionTable.getTable(), equipmentFactorTable.getTable());
    
    initEvents();
  }

  public FXTableEditor getPositionTable() {
    return positionTable;
  }

  public EquipmentFactorTable getEquipmentFactorTable() {
    return equipmentFactorTable;
  }
  
  private void initEvents() {
    positionTable.getTable().getSourceItems().addListener((ListChangeListener.Change<? extends PropertyMap> c) -> equipmentFactorTable.initHeader());
    
    positionTable.getTable().getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      if(!positionTable.getTable().isDisable()) {
        positionTable.getTable().setDisable(true);
        equipmentFactorTable.getTable().getSelectionModel().clearSelection();
        positionTable.getTable().getSelectionModel().getSelectedItems().stream().forEach(it -> equipmentFactorTable.getTable().getSelectionModel().select(it));
        positionTable.getTable().setDisable(false);
      }
    });
    
    equipmentFactorTable.getTable().getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      if(!positionTable.getTable().isDisable()) {
        positionTable.getTable().setDisable(true);
        positionTable.getTable().getSelectionModel().clearSelection();
        equipmentFactorTable.getTable().getSelectionModel().getSelectedItems().stream().forEach(it -> positionTable.getTable().getSelectionModel().select(it));
        positionTable.getTable().setDisable(false);
      }
    });
    
    positionTable.setRemoveAction(e -> {
      Integer[] ids = positionTable.getSelectedIds();
      if(ids.length > 0) {
        String msg = "Удалить безвозвратно?";
        Optional<ButtonType> o = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO).showAndWait();
        if(o != null && o.get() == ButtonType.YES) {
          ObjectLoader.removeObjects(DealPosition.class, ids);
          Integer[] deals = new Integer[0];
          for(List d:ObjectLoader.getData(DBFilter.create(Deal.class).AND_EQUAL("type", Deal.Type.CURRENT).AND_EQUAL("dealPositionCount", 0), "id"))
            deals = ArrayUtils.add(deals, (Integer)d.get(0));
          if(deals.length > 0 && ObjectLoader.removeObjects(Deal.class, deals) == deals.length)
            ObjectLoader.sendMessage(Deal.class, "REMOVE", deals);
        }
      }
    });
    
    equipmentFactorTable.factorChangeListeners.add(e -> {
      PropertyMap p = (PropertyMap)e.getSource();
      Integer factorId = Integer.valueOf(p.getValue("column", Column.class).getDatabaseColumnName().split("-")[1]);
      ObservableList<PropertyMap> efv = ObjectLoader.getList(DBFilter.create(EquipmentFactorValue.class)
              .AND_EQUAL("equipment", p.getMap("object").getInteger("equipment"))
              .AND_EQUAL("factor", factorId));
      if(!efv.isEmpty())
        ObjectLoader.removeObjects(EquipmentFactorValue.class, PropertyMap.getListFromList(efv, "id", Integer.TYPE));
      ObjectLoader.createObject(EquipmentFactorValue.class, 
              PropertyMap.create()
                      .setValue("name", p.getString("value"))
                      .setValue("equipment", p.getMap("object").getInteger("equipment"))
                      .setValue("factor", factorId));
    });
    
    equipmentFactorTable.getItems().addListener((ListChangeListener.Change<? extends PropertyMap> c) -> {
      while(c.next()) {
        if(c.wasAdded()) {
          c.getAddedSubList().stream().forEach((p) -> {
            for (String key : p.keySet()) {
              p.get(key).addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
                Integer id = (Integer) p.getValue("equipment");
                if(id != null) {
                  for(PropertyMap pm:equipmentFactorTable.getItems()) {
                    if(id.equals(pm.getValue("equipment"))) {
                      pm.setValue(key, newValue);
                    }
                  }
                }
              });
            }
          });
        }
      }
    });
  }
  
  public DBFilter getClientFilter() {
    return positionTable.getClientFilter();
  }
  
  public void initData() {
    equipmentFactorTable.clear();
    positionTable.initData();
    FXDivisionTable.bindScrollBars(Orientation.VERTICAL, positionTable.getTable(), equipmentFactorTable.getTable());
  }

  @Override
  public ObservableList storeControls() {
    return FXCollections.observableArrayList(positionTable, split);
  }

  @Override
  public List<FXDisposable> disposeList() {
    return FXCollections.observableArrayList(positionTable);
  }

  @Override
  public void finaly() {
    positionTable.initDataListeners().clear();
    
    positionTable.clearData();
    equipmentFactorTable.clear();
    
    //equipmentFactorTable = null;
    //positionTable = null;
  }
}