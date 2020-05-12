package division.fx.controller.store;

import bum.editors.util.ObjectLoader;
import bum.interfaces.DealPosition;
import bum.interfaces.Equipment;
import bum.interfaces.EquipmentFactorValue;
import bum.interfaces.Group;
import bum.interfaces.Store;
import division.fx.FXButton;
import division.fx.FXUtility;
import division.fx.PropertyMap;
import division.fx.dialog.FXD;
import division.fx.editor.FXTreeEditor;
import division.fx.editor.Storable;
import division.fx.table.FXDivisionTable;
import division.fx.tree.FXDivisionTreeTable;
import division.fx.tree.FXDivisionTreeTableCell;
import division.fx.util.MsgTrash;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import mapping.MappingObject;
import util.filter.local.DBFilter;

public class FXCompanyStore extends BorderPane implements Storable {
  public enum ItemType {ROOT,STORE,GROUP,EQUIPMENT}
  
  private final DBFilter storeFilter = DBFilter.create(Store.class);
  private final FXTreeEditor.FXTreeSelector stories = FXTreeEditor.createSelector(storeFilter, this, "Выберите склад", null, "objectType", "storeType", "controllOut");
  
  private final FXButton           createStoreButton     = new FXButton(e -> createStore()     , "tool-button");
  private final FXButton           editGroupButton       = new FXButton(e -> editGroup()       , "tool-button");
  private final FXButton           createEquipmentButton = new FXButton(e -> createEquipment() , "tool-button");
  private final FXButton           removeButton          = new FXButton(e -> remove()          , "tool-button");
  private final ToolBar tools = new ToolBar(createStoreButton, editGroupButton, createEquipmentButton, removeButton, stories);
  
  private final FXDivisionTreeTable<PropertyMap> storeTree = new FXDivisionTreeTable<>(new TreeItem<PropertyMap>(PropertyMap.create().setValue("name", "Депозитарии").setValue("itemtype", ItemType.ROOT)));
  private final EquipmentFactorTable equipmentFactorTable = new EquipmentFactorTable();
  private final SplitPane treeSplit = new SplitPane(storeTree, equipmentFactorTable);
  
  private final TreeTableColumn<PropertyMap, String>     objectColumn    = new TreeTableColumn<>("Активы");
  private final TreeTableColumn<PropertyMap, String>     unitColumn      = new TreeTableColumn<>("Ед.изм.");
  private final TreeTableColumn<PropertyMap, BigDecimal> amountColumn    = new TreeTableColumn<>("Наличие");
  private final TreeTableColumn<PropertyMap, BigDecimal> blockColumn     = new TreeTableColumn<>("Заблокировано");
  private final TreeTableColumn<PropertyMap, BigDecimal> freeColumn      = new TreeTableColumn<>("Доступно");
  private final TreeTableColumn<PropertyMap, Long>       partitionColumn = new TreeTableColumn<>("Партия");
  
  private final ObservableList<PropertyMap> groupList = FXCollections.observableArrayList();
  
  private Collection<PropertyMap> mementoList;
  private boolean selectable = true;
  private final ObjectProperty<PropertyMap> companyPartitionProperty = new SimpleObjectProperty<>();

  public FXCompanyStore() {
    FXUtility.initMainCss(this);
    initialize();
    initEvents();
  }
  
  public ObjectProperty<PropertyMap> companyPartitionProperty() {
    return companyPartitionProperty;
  }
  
  public ObjectProperty<PropertyMap> storeProperty() {
    return stories.valueProperty();
  }
  //4345097833
  
  private void initData() {
    storeTree.getRoot().getChildren().clear();
    if(companyPartitionProperty().getValue() != null) {
      try {
        storeFilter.clear().AND_EQUAL("companyPartition", companyPartitionProperty().getValue().getValue("id"));
        
        if(storeProperty().getValue() == null) {
          List<PropertyMap> s = ObjectLoader.getList(DBFilter.create(Store.class).AND_EQUAL("companyPartition", companyPartitionProperty().getValue().getValue("id")));
          if(!s.isEmpty())
            storeProperty().setValue(s.stream().filter(st -> st.isNotNull("main") && st.is("main")).findFirst().orElseGet(() -> s.get(0)));
        }else {
            groupList.setAll(ObjectLoader.getList(DBFilter.create(Group.class)
                    .AND_EQUAL("tmp", false)
                    .AND_EQUAL("type", Group.Type.CURRENT)
                    .AND_EQUAL("groupType", storeProperty().getValue().getValue("objectType")),
                    "id","name","parent","iden_id","iden_name","groupType","unit")
                    .stream().map(s -> s.setValue("itemType", ItemType.GROUP)).collect(Collectors.toList()));

            ObjectLoader.getList(DBFilter.create(Equipment.class).AND_EQUAL("store", storeProperty().getValue().getInteger("id")).AND_EQUAL("tmp", false).AND_EQUAL("type", Equipment.Type.CURRENT),
                    "id",
                    "factors",
                    "name:=:group_name",
                    "group",
                    "unit:=:group-unit",
                    "store",
                    "amount",
                    "identity_value:=:identity_value_name",
                    "block-amount:=:reserve",
                    "type",
                    "tmp",
                    "values=query:(select array_agg('factor-'||[EquipmentFactorValue(factor)]||'='||name) from [EquipmentFactorValue] where [EquipmentFactorValue(equipment)]=[Equipment(id)])",
                    "partition=query:(SELECT [DealPosition(deal)] FROM [DealPosition] WHERE id=[Equipment(sourceDealPosition)])")
                    .stream().map(s -> {
                      if(s.getArray("values", String.class).length > 0) {
                        Arrays.stream(s.getArray("values", String.class)).filter(a -> a!= null).forEach(a -> {
                          if(a.split("=").length == 2)
                            s.setValue(a.split("=")[0], a.split("=")[1]);
                        });
                      }
                      return s.setValue("itemType", ItemType.EQUIPMENT);
                    }).collect(Collectors.toList()).stream().forEach(eq -> insertObject(eq));
        }
        mementoList = PropertyMap.copyList(getCurrentList());
      } catch (Exception ex) {
        MsgTrash.out(ex);
      }
    }
  }
  
  private Collection<PropertyMap> getCurrentList() {
    return storeTree.listItems("itemType", ItemType.EQUIPMENT).stream().map(t -> t.getValue()).collect(Collectors.toList());
  }

  private void initialize() {
    equipmentFactorTable.setRootColumnVisible(false);
    createStoreButton.setDisable(true);
    editGroupButton.setDisable(true);
    
    equipmentFactorTable.getTable().getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    storeTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    equipmentFactorTable.getTable().setEditable(true);
    
    setCenter(new VBox(5, tools, treeSplit));
    VBox.setVgrow(treeSplit, Priority.ALWAYS);
    storeTree.setShowRoot(false);
    storeTree.getColumns().addAll(objectColumn, amountColumn, blockColumn, freeColumn, unitColumn, partitionColumn);
    
    storeTree.setRowFactory((TreeTableView<PropertyMap> param) -> {
      return new TreeTableRow<PropertyMap>() {
        @Override
        protected void updateItem(PropertyMap item, boolean empty) {
          super.updateItem(item, empty);
          getStyleClass().remove("equipment-row");
          if(!empty && item != null && item.getValue("itemType", ItemType.class) == ItemType.EQUIPMENT)
            getStyleClass().add("equipment-row");
        }
      };
    });
    
    
    
    storeTree.getColumns().stream().forEach(c -> {
      ((TreeTableColumn<PropertyMap,Object>)c).setCellFactory((TreeTableColumn<PropertyMap, Object> param) -> {
        return new FXDivisionTreeTableCell<PropertyMap, Object>() {
          @Override
          protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().removeAll("equipment-tmp", "equipment-archive","free-column","new-equipment");
            
            if(!empty && getRowItem() != null) {
              if(getRowItem().getValue("id") instanceof String)
                getStyleClass().add("new-equipment");
              
              if(getRowItem().getValue("tmp",false))
                getStyleClass().add("equipment-tmp");
              
              if(getRowItem().getValue("type",Equipment.Type.CURRENT) == MappingObject.Type.ARCHIVE)
                getStyleClass().add("equipment-archive");
              
              if(getTableColumn().getText().equals("Доступно"))
                getStyleClass().add("free-column");
            }
          }
        };
      });
    });
    
    equipmentFactorTable.getTable().getColumns().addListener((ListChangeListener.Change<? extends TableColumn<PropertyMap, ?>> c) -> {
      equipmentFactorTable.getTable().getColumns().stream().forEach(column -> {
        if(column instanceof EquipmentFactorTable.FactorColumn) {
          ((TableColumn<PropertyMap,Object>)column).setCellFactory((TableColumn<PropertyMap, Object> param) -> new EquipmentFactorTable.FactorCell((EquipmentFactorTable.FactorColumn) column) {
            @Override
            protected void updateItem(Object item, boolean empty) {
              super.updateItem(item, empty);
              getStyleClass().removeAll("equipment-tmp", "equipment-archive","free-column","new-equipment");

              if(!empty && getRowItem() != null) {
                if(getRowItem().getValue("id") instanceof String)
                  getStyleClass().add("new-equipment");

                if(getRowItem().getValue("tmp",false))
                  getStyleClass().add("equipment-tmp");

                if(getRowItem().getValue("type",Equipment.Type.CURRENT) == MappingObject.Type.ARCHIVE)
                  getStyleClass().add("equipment-archive");
              }
            }
          });
        }
      });
    });
    
    objectColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<PropertyMap, String> param) -> {
      PropertyMap node = param.getValue().getValue();
      if(node.getValue("itemType", ItemType.class) == ItemType.EQUIPMENT)
        if(node.isNotNull("identity_value") && param.getValue().getParent().getValue().isNotNull("iden_name") && node.getBigDecimal("amount").compareTo(BigDecimal.ONE) == 0)
          return new SimpleStringProperty(param.getValue().getParent().getValue().getString("iden_name")+" ("+node.getString("identity_value")+")");
      return node.get("name");
    });
    
    amountColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<PropertyMap, BigDecimal> p) -> {
      BigDecimal amount = BigDecimal.ZERO;
      for(TreeItem<PropertyMap> item:storeTree.listItems(p.getValue(), "itemType", ItemType.EQUIPMENT))
        if(!item.getValue().getValue("tmp",false) && item.getValue().getValue("type", Equipment.Type.CURRENT) != MappingObject.Type.ARCHIVE)
          amount = amount.add(item.getValue().getValue("amount", BigDecimal.ZERO));
      return new SimpleObjectProperty<>(amount);
    });
    
    blockColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<PropertyMap, BigDecimal> p) -> {
      BigDecimal blockAmount = BigDecimal.ZERO;
      for(TreeItem<PropertyMap> item:storeTree.listItems(p.getValue(), "itemType", ItemType.EQUIPMENT))
        if(!item.getValue().getValue("tmp",false) && item.getValue().getValue("type", Equipment.Type.CURRENT) != MappingObject.Type.ARCHIVE)
          blockAmount = blockAmount.add(item.getValue().getValue("block-amount", BigDecimal.ZERO));
      return new SimpleObjectProperty<>(blockAmount);
    });
    
    freeColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<PropertyMap, BigDecimal> p) -> {
      BigDecimal freeAmount = BigDecimal.ZERO;
      for(TreeItem<PropertyMap> item:storeTree.listItems(p.getValue(), "itemType", ItemType.EQUIPMENT))
        if(!item.getValue().getValue("tmp",false) && item.getValue().getValue("type", Equipment.Type.CURRENT) != MappingObject.Type.ARCHIVE)
          freeAmount = freeAmount.add(item.getValue().getValue("amount", BigDecimal.ZERO).subtract(item.getValue().getValue("block-amount", BigDecimal.ZERO)));
      return new SimpleObjectProperty<>(freeAmount);
    });
    
    partitionColumn.setCellValueFactory(p -> p.getValue().getValue().get("partition"));
    
    unitColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<PropertyMap, String> param) -> param.getValue().getValue().get("unit"));
    
    equipmentFactorTable.getTable().setHorizontScrollBarPolicyAlways();
    storeTree.setHorizontScrollBarPolicyAlways();
  }

  private void initEvents() {
    storeProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      if(oldValue != null && isUpdate() && FXD.showWait("Сохранение", this, "Сохранить изменения?", FXD.ButtonType.YES, FXD.ButtonType.NO).orElseGet(() -> FXD.ButtonType.NO) == FXD.ButtonType.YES)
        savePartitionStore();
      initData();
    });
    
    companyPartitionProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      if(oldValue != null && isUpdate() && FXD.showWait("Сохранение", this, "Сохранить изменения?", FXD.ButtonType.YES, FXD.ButtonType.NO).orElseGet(() -> FXD.ButtonType.NO) == FXD.ButtonType.YES)
        savePartitionStore();
      initData();
    });
    
    storeTree.getSelectionModel().getSelectedIndices().addListener((ListChangeListener.Change<? extends Integer> c) -> {
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
        storeTree.getSelectionModel().clearSelection();
        c.getList().stream().forEach(i -> storeTree.getSelectionModel().select(i));
        selectable = true;
      }
    });
    
    storeTree.expandedItemCountProperty().addListener(e -> reloadEquipmentTable());
    //storeTree.setOnSort(e -> reloadEquipmentTable());
  }
  
  private void reloadEquipmentTable() {
    Platform.runLater(() -> {
      List<PropertyMap> items = new ArrayList<>();
      for(int i=0;i<storeTree.getExpandedItemCount();i++) {
        if(storeTree.getTreeItem(i).getValue().getValue("itemType", ItemType.class) == ItemType.EQUIPMENT)
          items.add(storeTree.getTreeItem(i).getValue());
        else items.add(PropertyMap.create().setValue("amount", null));
      }
      equipmentFactorTable.getItems().setAll(items);
      equipmentFactorTable.initHeader();
      equipmentFactorTable.nameColumnVisibleProperty().setValue(false);
      equipmentFactorTable.amountColumnVisibleProperty().setValue(false);
      
      FXDivisionTable.bindScrollBars(Orientation.VERTICAL, storeTree, equipmentFactorTable.getTable());
      if(FXDivisionTable.getScrollBar(storeTree, Orientation.VERTICAL) != null) {
        FXDivisionTable.getScrollBar(storeTree, Orientation.VERTICAL).setPrefWidth(0);
        FXDivisionTable.getScrollBar(storeTree, Orientation.VERTICAL).setMinWidth(0);
        FXDivisionTable.getScrollBar(storeTree, Orientation.VERTICAL).setMaxWidth(0);
      }
    });
  }
  
  private TreeItem<PropertyMap> insertObject(PropertyMap object) {
    // Ищем такую ноду
    TreeItem<PropertyMap> node = storeTree.getNode(object.getSimpleMap("id","itemType"));
    if(node == null) { // нет такой ноды, то сначала определим родительскую ноду
      TreeItem<PropertyMap> parentTree = null;
      PropertyMap parent = null;
      switch(object.getValue("itemType", ItemType.class)) {
        case GROUP: // вставляем группу
          if(object.getValue("parent") == null) // если нет родителя, то родетелем будет root
            parentTree = storeTree.getRoot();
          else {// найдём папу в дереве 
            parent = groupList.stream().filter(g -> g.getValue("id").equals(object.getValue("parent"))).findFirst().orElseGet(() -> null);
            if(parent != null)
              parent = parent.copy();
          }
          break;
        case EQUIPMENT: // вставляем экземпляр, а папа у него всегда группа
          parent = groupList.stream().filter(g -> g.getValue("id").equals(object.getValue("group"))).findFirst().orElseGet(() -> null);
          if(parent != null)
            parent = parent.copy();
          break;
      }
      
      if(parent != null) {
        //parentTree = storeTree.getNode(parent.getSimpleMap("id","itemType","store"));
        parentTree = storeTree.getNode(parent.getSimpleMap("id","itemType"));
        while(parentTree == null)
          parentTree = insertObject(parent);
      }
      
      if(parentTree != null) {
        parentTree.getChildren().add(node = new TreeItem<>(object));
        node.setExpanded(true);
      }
    }
    return node;
  }

  private void remove() {
    HashSet<TreeItem<PropertyMap>> objectsToRemove  = new HashSet<>();
    List<Integer> objectsToArchive = new ArrayList<>();
    
    storeTree.getSelectionModel().getSelectedItems().stream().forEach(it -> storeTree.listItems(it, "itemType", ItemType.EQUIPMENT).stream().forEach(o -> objectsToRemove.add(o)));
    
    if(!objectsToRemove.isEmpty() && FXD.showWait("Удаление", this, "Удалить?", FXD.ButtonType.YES, FXD.ButtonType.CANCEL).orElseGet(() -> FXD.ButtonType.CANCEL) == FXD.ButtonType.YES) {
      List<PropertyMap> objToCheck = objectsToRemove.stream().filter(ti -> ti.getValue().getValue("id") instanceof Integer).map(ti -> ti.getValue()).collect(Collectors.toList());
      List<PropertyMap> dealPositions = objToCheck.isEmpty() ? new ArrayList<>() : ObjectLoader.getList(DBFilter.create(DealPosition.class).AND_IN("equipment", objToCheck), "id","deal","equipment");
      if(!dealPositions.isEmpty()) {
        if(FXD.showWait("Удаление", this, "Некоторые объекты присутствуют в сделках, продолжить удаление?", FXD.ButtonType.YES, FXD.ButtonType.NO).orElseGet(() -> FXD.ButtonType.NO) == FXD.ButtonType.YES)
          objectsToArchive.addAll(PropertyMap.getListFromList(dealPositions, "equipment", Integer.class));
        else objectsToRemove.clear();
      }
      
      objectsToRemove.stream().forEach(ti -> {
        if(ti.getValue().getValue("id") instanceof String) {
          TreeItem<PropertyMap> node = ti;
          TreeItem<PropertyMap> parent = node.getParent();
          parent.getChildren().remove(ti);
          while(parent.getChildren().isEmpty()) {
            node = parent;
            parent = parent.getParent();
            parent.getChildren().remove(node);
          }
        }else {
          if(objectsToArchive.contains(ti.getValue().getInteger("id")))
            ti.getValue().setValue("type", Equipment.Type.ARCHIVE);
          else ti.getValue().setValue("tmp", true);
        }
        storeTree.refresh();
        equipmentFactorTable.getTable().refresh();
      });
    }
  }

  private void createEquipment() {
    PropertyMap store = stories.getField().valueProperty().getValue();
    try {
      FXEquipmentCreator.getFromStore(this, store.getInteger("id")).stream().forEach(e -> insertObject(e.setValue("itemType", ItemType.EQUIPMENT)));
    } catch (Exception ex) {
      MsgTrash.out(ex);
    }
  }

  private void editGroup() {
  }

  private void createStore() {
  }
  
  @Override
  public ObservableList storeControls() {
    return FXCollections.observableArrayList(treeSplit, storeTree);
  }
  
  public boolean isUpdate() {
    return !PropertyMap.equals(mementoList, getCurrentList(), "id","name","tmp","type","r=factor-\\d+");
  }
  
  public void savePartitionStore() {
    
    Collection<PropertyMap> currentList = getCurrentList();
    
    List<Integer> itemsToRemove  = currentList.stream().filter(e -> e.isNotNull("tmp") && e.is("tmp")).map(e -> e.getInteger("id")).collect(Collectors.toList());
    List<Integer> itemsToArchive = currentList.stream().filter(e -> e.isNotNull("type") && e.getValue("type", Equipment.Type.class) == MappingObject.Type.ARCHIVE).map(e -> e.getInteger("id")).collect(Collectors.toList());
    
    if(!itemsToRemove.isEmpty())
      ObjectLoader.toTmpObjects(Equipment.class, itemsToRemove.toArray(new Integer[0]), true);
    
    if(!itemsToArchive.isEmpty())
      ObjectLoader.toTypeObjects(Equipment.class, Equipment.Type.ARCHIVE, itemsToArchive);
    
    List<PropertyMap> itemsToCreate = currentList.stream().filter(t -> t.getValue("id") instanceof String && t.getString("id").startsWith("n-")).collect(Collectors.toList());
    
    if(!itemsToCreate.isEmpty()) {
      itemsToCreate.stream().filter(p -> p.getValue("itemType", ItemType.class) == ItemType.STORE).forEach(s -> 
              s.setValue("id", ObjectLoader.createObject(Store.class, s.getSimpleMap("name","parent","objectType","storeType","controllIn","controllOut","currency","companyPartition"))));
      
      itemsToCreate.stream().filter(p -> p.getValue("itemType", ItemType.class) == ItemType.EQUIPMENT).forEach(e -> {
        
        e.setValue("id", ObjectLoader.createObject(Equipment.class, e.getSimpleMap("group","amount","store")));
        
        e.keySet().stream().filter(k -> k.matches("factor-\\d+")).forEach(k -> {
            ObjectLoader.createObject(EquipmentFactorValue.class, 
                    PropertyMap.create()
                            .setValue("equipment", e.getInteger("id"))
                            .setValue("factor", Integer.valueOf(k.substring(7)))
                            .setValue("name", e.getString(k)));
          });
      });
    }
    
    
    
    for(PropertyMap me:mementoList) {
      for(PropertyMap ce:currentList) {
        if(me.getValue("id").equals(ce.getValue("id")) && !PropertyMap.equals(me, ce, "r=factor-\\d+")) {
          ObjectLoader.removeObjects(EquipmentFactorValue.class, PropertyMap.getArrayFromList(ObjectLoader.getList(DBFilter.create(EquipmentFactorValue.class).AND_EQUAL("equipment", me.getInteger("id")), "id"), "id", Integer.class));
          ce.keySet().stream().filter(k -> k.matches("factor-\\d+")).forEach(k -> {
            ObjectLoader.createObject(EquipmentFactorValue.class, 
                    PropertyMap.create()
                            .setValue("equipment", ce.getInteger("id"))
                            .setValue("factor", Integer.valueOf(k.substring(7)))
                            .setValue("name", ce.getString(k)));
          });
        }
      }
    }
    
    mementoList = PropertyMap.copyList(getCurrentList());
  }
  
  public PropertyMap getSelectedStore() {
    return stories.getField().valueProperty().getValue();
  }
  
  public FXDivisionTreeTable<PropertyMap> getTreetable() {
    return storeTree;
  }
}