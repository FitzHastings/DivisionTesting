package division.fx.editor;

import bum.editors.util.ObjectLoader;
import bum.editors.util.api.Transaction;
import com.sun.javafx.scene.control.skin.VirtualFlow;
import division.fx.FXButton;
import division.fx.FXToolToggleButton;
import division.fx.PropertyMap;
import division.fx.tree.FXDivisionCheckTree;
import division.fx.tree.FXDivisionTree;
import division.fx.FXUtility;
import division.fx.PropertyMapTextField;
import division.fx.dialog.FXD;
import division.fx.tree.FXTree;
import division.fx.util.MsgTrash;
import division.util.DivisionTask;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import mapping.MappingObject;
import org.apache.commons.lang3.ArrayUtils;
import util.filter.local.DBFilter;

public class FXTreeEditor extends FXEditor<PropertyMap> {
  //private final FXToolToggleButton expandButton = new FXToolToggleButton("Открыть всё");
  
  private ObjectProperty<TreeView<PropertyMap>> treeProperty = new SimpleObjectProperty<>(new FXDivisionTree(new TreeItem(PropertyMap.create().setValue("name", "root"))));
  
  private EventHandler checkHandler;
  private BooleanProperty checkBoxTreeProperty = new SimpleBooleanProperty(true);
  private BooleanProperty checkBoxIndependentProperty  = new SimpleBooleanProperty(false);
  
  private FXToolToggleButton herachy = new FXToolToggleButton("herachy","herachy");
  
  private ObservableList<PropertyMap> nodeTypeData = FXCollections.observableArrayList();
  
  public FXTreeEditor(Class<? extends MappingObject> objectClass) {
    this(objectClass, null);
  }
  
  public FXTreeEditor(Class<? extends MappingObject> objectClass, Class<? extends FXObjectEditor> objectEditorClass) {
    this(objectClass, objectEditorClass, FXCollections.observableArrayList(), MappingObject.Type.CURRENT, Boolean.FALSE);
  }
  
  public FXTreeEditor(Class<? extends MappingObject> objectClass, Class<? extends FXObjectEditor> objectEditorClass, MappingObject.Type type) {
    this(objectClass, objectEditorClass, FXCollections.observableArrayList(), type, Boolean.FALSE);
  }
  
  public FXTreeEditor(Class<? extends MappingObject> objectClass, Class<? extends FXObjectEditor> objectEditorClass, String... otherFields) {
    this(objectClass, objectEditorClass, Arrays.asList(otherFields), MappingObject.Type.CURRENT, Boolean.FALSE);
  }
  
  public FXTreeEditor(Class<? extends MappingObject> objectClass, Class<? extends FXObjectEditor> objectEditorClass, List<String> otherFields, MappingObject.Type type) {
    this(objectClass, objectEditorClass, otherFields, type, Boolean.FALSE);
  }

  public FXTreeEditor(Class<? extends MappingObject> objectClass, Class<? extends FXObjectEditor> objectEditorClass, List<String> otherFields, MappingObject.Type type, Boolean tmp) {
    super(objectClass, objectEditorClass, otherFields, type, tmp);
    init();
  }
  
  public static ObservableList<TreeItem<PropertyMap>> getList(Class<? extends MappingObject> objectClass, Node parent, String title, Class<? extends FXObjectEditor> objectEditorClass, String... otherFields) {
    return getList(DBFilter.create(objectClass), parent, title, objectEditorClass, otherFields);
  }
  
  public static ObservableList<TreeItem<PropertyMap>> getList(DBFilter filter, Node parent, String title, Class<? extends FXObjectEditor> objectEditorClass, String... otherFields) {
    FXTreeEditor editor = new FXTreeEditor(filter.getTargetClass(), objectEditorClass, otherFields);
    editor.getClientFilter().AND_FILTER(filter);
    FXD fxd = FXD.create(title, parent, editor, FXD.ButtonType.OK);
    fxd.setOnShowing(e -> {
      editor.load(title);
      editor.initData();
    });
    fxd.setOnHiding(e -> editor.store(title));
    fxd.setOnCloseRequest(e -> editor.treeProperty.getValue().getSelectionModel().clearSelection());
    fxd.showDialog(e -> fxd.close());
    return editor.treeProperty.getValue().getSelectionModel().getSelectedItems();
  }
  
  public static TreeItem<PropertyMap> get(DBFilter filter, Node parent, String title, Class<? extends FXObjectEditor> objectEditorClass, boolean onlyLast, String... otherFields) {
    FXTreeEditor editor = new FXTreeEditor(filter.getTargetClass(), objectEditorClass, otherFields);
    editor.getTree().setShowRoot(true);
    editor.getClientFilter().AND_FILTER(filter);
    editor.setSelectionMode(SelectionMode.SINGLE);
    
    FXD fxd = FXD.create(title, parent, editor, FXD.ButtonType.OK);
    editor.getTree().setOnMouseClicked(e -> {
      if(e.getClickCount() == 2 && editor.getTree().getSelectionModel().getSelectedItem().getChildren().size() == 0) {
        fxd.fire(FXD.ButtonType.OK);
      }
    });
    
    fxd.setOnShowing(e -> {
      editor.load(title);
      editor.initData();
    });
    fxd.setOnHiding(e -> editor.store(title));
    fxd.setOnCloseRequest(e -> editor.getTree().getSelectionModel().clearSelection());
    fxd.showDialog(e -> {
      if(onlyLast && !editor.getTree().getSelectionModel().getSelectedItem().getChildren().isEmpty()) {
        MsgTrash.out(new Exception("Выберите конечный объект"));
      }else fxd.close();
    });
    return editor.getTree().getSelectionModel().getSelectedItem();
  }
  
  /*public static TreeItem<PropertyMap> getLast(DBFilter filter, Node parent, String title, Class<? extends FXObjectEditor> objectEditorClass, String... otherFields) {
    FXTreeEditor editor = new FXTreeEditor(filter.getTargetClass(), objectEditorClass, otherFields);
    editor.getClientFilter().AND_FILTER(filter);
    editor.setSelectionMode(SelectionMode.SINGLE);
    
    FXD fxd = FXD.create(title, parent, editor, FXD.ButtonType.OK);
    editor.getTree().setOnMouseClicked(e -> {
      if(e.getClickCount() == 2) {
        fxd.fire(FXD.ButtonType.OK);
      }
    });
    
    fxd.setOnShowing(e -> {
      editor.load(title);
      editor.initData();
    });
    fxd.setOnHiding(e -> editor.store(title));
    fxd.setOnCloseRequest(e -> editor.getTree().getSelectionModel().clearSelection());
    fxd.showDialog(e -> {
      if(!editor.getTree().getSelectionModel().getSelectedItem().getChildren().isEmpty()) {
        MsgTrash.out(new Exception("Выберите конечный объект"));
      }else fxd.close();
    });
    return editor.getTree().getSelectionModel().getSelectedItem();
  }*/
  
  public static FXTreeSelector createSelector(Node parent, String title, Class<? extends MappingObject> objectClass, Class<? extends FXObjectEditor> objectEditorClass, String... otherFields) {
    return createSelector(DBFilter.create(objectClass), parent, title, title+"...", objectEditorClass, true, otherFields);
  }
  
  public static FXTreeSelector createSelector(Node parent, String title, String promtText, Class<? extends MappingObject> objectClass, boolean onlyLast, Class<? extends FXObjectEditor> objectEditorClass, String... otherFields) {
    return createSelector(DBFilter.create(objectClass), parent, title, promtText, objectEditorClass, onlyLast, otherFields);
  }

  public static FXTreeSelector createSelector(
          DBFilter filter, 
          Node parent, 
          String title, 
          Class<? extends FXObjectEditor> objectEditorClass, 
          String... otherFields) {
    return createSelector(filter, parent, title, title+"...", objectEditorClass, true, otherFields);
  }
  
  public static FXTreeSelector createSelector(
          DBFilter filter, 
          Node parent, 
          String title, 
          String promtText,
          Class<? extends FXObjectEditor> objectEditorClass, 
          boolean onlyLastItem,
          String... otherFields) {
    FXTreeSelector selector = new FXTreeSelector(promtText,"name");
    selector.getButton().setOnAction(e -> {
      Platform.runLater(() -> {
        TreeItem<PropertyMap> item = get(filter, parent, title, objectEditorClass, onlyLastItem, otherFields);
        selector.getField().valueProperty().setValue(item == null ? null : item.getValue());
      });
    });
    return selector;
  }
  
  public BooleanProperty checkBoxTreeProperty() {
    return checkBoxTreeProperty;
  }
  public BooleanProperty checkBoxIndependentProperty() {
    return checkBoxIndependentProperty;
  }
  
  public static class FXTreeSelector extends HBox {
    private final PropertyMapTextField field;
    private final FXButton button = new FXButton("...");

    public FXTreeSelector(String promtText, String... keys) {
      field = new PropertyMapTextField(promtText, keys);
      getChildren().addAll(field,button);
      HBox.setHgrow(field, Priority.ALWAYS);
    }

    public PropertyMapTextField getField() {
      return field;
    }

    public FXButton getButton() {
      return button;
    }
    
    public ObjectProperty<PropertyMap> valueProperty() {
      return getField().valueProperty();
    }
  }

  @Override
  protected void editPropertyObjectBeforeCreateDialog(PropertyMap prop) {
    prop.setValue("parent", getTree().getSelectionModel().getSelectedItem() == null ? null : getTree().getSelectionModel().getSelectedItem().getValue().getInteger("id"));
  }
  
  @Override
  protected void removeAction() {
    if(!getTree().getSelectionModel().getSelectedItems().isEmpty()) {
      String msg = null;
      switch(getRemoveActionType()) {
        case DELETE:          msg = "Удалить безвозвратно?";break;
        case MARK_FOR_DELETE: msg = "Удалить выбранные объекты?";break;
        case MOVE_TO_ARCHIVE: msg = "Переместить выбранные объекты в архив?";break;
      }
      if(FXD.showWait("Удаление", this, msg, FXD.ButtonType.YES, FXD.ButtonType.NO).orElseGet(() -> FXD.ButtonType.NO) == FXD.ButtonType.YES) {
        switch(getRemoveActionType()) {
          case DELETE:
            ObjectLoader.removeObjects(getObjectClass(), getTree().getSelectionModel().getSelectedItems().stream().map(p -> p.getValue().getInteger("id")).collect(Collectors.toList()));
            break;
          case MARK_FOR_DELETE:
            ObjectLoader.toTmpObjects(getObjectClass(), getTree().getSelectionModel().getSelectedItems().stream().map(p -> p.getValue().getInteger("id")).collect(Collectors.toList()).toArray(new Integer[0]), true);
            break;
          case MOVE_TO_ARCHIVE:
            ObjectLoader.toTypeObjects(getObjectClass(), MappingObject.Type.ARCHIVE, getTree().getSelectionModel().getSelectedItems().stream().map(p -> p.getValue().getInteger("id")).collect(Collectors.toList()));
            break;
        }
      }
    }
  }
  
  @Override
  protected void editAction() {
    if(getTree().getSelectionModel().getSelectedItem() != null) {
      setCursor(Cursor.WAIT);
      try {
        FXObjectEditor editor = (FXObjectEditor)getObjectEditorClass().newInstance();
        editor.setObjectClass(getObjectClass());
        editor.setObjectProperty(Transaction.create().getMap(getObjectClass(), getTree().getSelectionModel().getSelectedItem().getValue().getInteger("id")));
        editPropertyObjectBeforeEditDialog(editor.getObjectProperty());
        editor.showAndWait(this);
      }catch(Exception ex) {
        MsgTrash.out(ex);
      }finally {
        setCursor(Cursor.DEFAULT);
      }
    }
  }
  
  private void reItem(TreeItem<PropertyMap> source, TreeItem<PropertyMap> target) {
    target.setExpanded(source.isExpanded());
    for(TreeItem<PropertyMap> item:source.getChildren()) {
      TreeItem<PropertyMap> node;
      if(checkBoxTreeProperty.getValue())
        node = new CheckBoxTreeItem<>(item.getValue());
      else node = new TreeItem<>(item.getValue());
      target.getChildren().add(node);
      reItem(item, node);
    }
  }
  
  public BooleanProperty checkBoxTreeHerachyProperty() {
    return herachy.selectedProperty();
  }
  
  public TreeItem<PropertyMap> getTreeItemAtPoint(double x, double y) {
    VirtualFlow vf = (VirtualFlow)getTree().getChildrenUnmodifiable().get(0);
    y -= vf.getBoundsInParent().getMinY();
    for(int i=0;i<vf.getCellCount();i++) {
      if(vf.getVisibleCell(i) != null && vf.getVisibleCell(i).getBoundsInParent().contains(x, y))
        return getTree().getTreeItem(i);
    }
    return null;
  }

  @Override
  public PropertyMap getObjectAtPoint(double x, double y) {
    TreeItem<PropertyMap> item = getTreeItemAtPoint(x, y);
    return item == null ? null : item.getValue();
  }

  
  
  private void init() {
    
    setOnDrop(e -> {
      PropertyMap item = getTreeItemAtPoint(e.getX(), e.getY()).getValue();
      ObservableList<PropertyMap> cfcData = PropertyMap.fromSimple((List<Map>)e.getDragboard().getContent(getDragFormat()));
      FXD.show((FXD.ButtonType type) -> {
        try {
          if(type == FXD.ButtonType.OK)
            ObjectLoader.executeUpdate(getObjectClass(), "parent", item.getInteger("id"), PropertyMap.getListFromList(cfcData, "id", Integer.TYPE).toArray(new Integer[0]));
        }catch(Exception ex) {
          MsgTrash.out(ex);
        }finally {
          return true;
        }
      }, "Внимание!!!", getControl(), "Переместить группу?", Modality.NONE, FXD.ButtonType.OK, FXD.ButtonType.CANCEL);
    });
    
    herachy.setSelected(true);
    
    //root.getValue().get("name").bind(titleProperty());
    //tree.setShowRoot(false);
    //tree.getStyleClass().add("dir-tree");
    
    /*ChangeListener<Boolean> listener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(!getTools().getItems().contains(herachy) && checkBoxTreeProperty.and(checkBoxIndependentProperty).getValue())
        getTools().getItems().add(herachy);
      if(getTools().getItems().contains(herachy) && !checkBoxTreeProperty.and(checkBoxIndependentProperty).getValue())
        getTools().getItems().remove(herachy);
    };*/
    
    treeProperty.addListener((ObservableValue<? extends TreeView<PropertyMap>> observable, TreeView<PropertyMap> oldtree, TreeView<PropertyMap> tree) -> {
      //tree.setShowRoot(false);
      tree.setCellFactory((TreeView<PropertyMap> param) -> {
        TreeCell cell;
        if(checkBoxTreeProperty.getValue()) {
          cell = new CheckBoxTreeCell() {
            @Override
            public void updateItem(Object item, boolean empty) {
              super.updateItem(item, empty);
              setText(null);
              if(item != null) {
                getDragOverObject().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
                  getStyleClass().remove("tree-item-drag-over");
                  if(Objects.equals(newValue, item))
                    getStyleClass().add("tree-item-drag-over");
                });
                setText(((PropertyMap)item).getString("name"));
                FXTreeEditor.this.updateItem((PropertyMap)item, this);
              }
            }
          };
        }else cell = new TreeCell() {
          @Override
          public void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            if(item != null) {
              getDragOverObject().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
                getStyleClass().remove("tree-item-drag-over");
                if(Objects.equals(newValue, item))
                  getStyleClass().add("tree-item-drag-over");
              });
              setText(((PropertyMap)item).getString("name"));
              FXTreeEditor.this.updateItem((PropertyMap)item, this);
            }
          }
        };
        return cell;
      });
      
      /*tree.setCellFactory((TreeView<PropertyMap> param) -> new TreeCell() {
        @Override
        protected void updateItem(Object item, boolean empty) {
          super.updateItem(item, empty);
          setText(null);
          if(item != null) {
            getDragOverObject().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
              getStyleClass().remove("tree-item-drag-over");
              if(Objects.equals(newValue, item))
                getStyleClass().add("tree-item-drag-over");
            });
            setText(((PropertyMap)item).getString("name"));
            FXTreeEditor.this.updateItem((PropertyMap)item, this);
          }
        }
      });*/
      
      /*tree.setOnMouseClicked((MouseEvent event) -> {
        if(event.getClickCount() == 2 && tree.getSelectionModel().getSelectedItem() != null && tree.getSelectionModel().getSelectedItem().getChildren().isEmpty()) {
          switch(getDoubleClickActionType()) {
            case SELECT:
              if(getDialog() != null) {
                getDialog().setResult(FXDialog.Type.OK);
                getDialog().close();
              }
              break;
            case EDIT:
              getEditAction().handle(null);
              break;
          }
        }
      });*/
      
      storeControls().add(tree);
      getRoot().getValue().get("name").bind(titleProperty());
      getRoot().setExpanded(true);
      getTree().setEditable(false);
      getTree().getStyleClass().add("dir-tree");
      setCenter(getTree());
      initDragAndDrop();
    });
    
    checkBoxTreeProperty.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      TreeView<PropertyMap> nTree = newValue ? new FXDivisionCheckTree<>(new FXCheckBoxTreeItem(getRoot().getValue())) : new FXDivisionTree<>(new TreeItem<>(getRoot().getValue()));
      reItem(getRoot(), nTree.getRoot());
      nTree.setShowRoot(getTree().isShowRoot());
      treeProperty.setValue(nTree);
    });
    
    //checkBoxTreeProperty.addListener(listener);
    //checkBoxIndependentProperty.addListener(listener);
    checkBoxTreeProperty.setValue(false);
    
    FXUtility.initCss(this);
  }
  
  protected void updateItem(PropertyMap item, TreeCell cell) {
  }
  
  public ObjectProperty<TreeView<PropertyMap>> treeProperty() {
    return treeProperty;
  }
  
  public TreeItem<PropertyMap> getRoot() {
    return treeProperty.getValue().getRoot();
  }
  
  @Override
  public Node getControl() {
    return getTree();
  }
  
  private void expand(TreeItem<PropertyMap> parent, Boolean newValue) {
    for(TreeItem<PropertyMap> node:parent.getChildren())
      expand(node, newValue);
    parent.setExpanded(newValue);
  }

  public TreeView<PropertyMap> getTree() {
    return treeProperty == null ? null : treeProperty.getValue();
  }
  
  private PropertyMap getNodeType(Integer id) {
    for(PropertyMap n:nodeTypeData)
      if(n.getValue("id").equals(id))
        return n;
    
    try {
      return create().copyFrom(ObjectLoader.getMap(getObjectClass(), id, ArrayUtils.addAll(getOtherFields().toArray(new String[0]), getFields())));
    }catch(Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }
  
  public TreeItem<PropertyMap> getNodeObject(Integer id) {
    return getNodeObject(id, getRoot());
  }
  
  public TreeItem<PropertyMap> getNodeObject(Integer id, TreeItem<PropertyMap> parent) {
    TreeItem<PropertyMap> node = null;
    if(id != null) {
      for(TreeItem<PropertyMap> n:parent.getChildren()) {
        try {
          if(n.getValue().containsKey("id") && id.equals(n.getValue().getValue("id")))
            node = n;
          else node = getNodeObject(id, n);
          if(node != null)
            break;
        }catch(NullPointerException ex) {
          ex.printStackTrace();
        }
      }
    }
    return node;
  }
  
  
  public void setCheckHandler(EventHandler<Event> checkHandler) {
    this.checkHandler = checkHandler;
  }
  
  public synchronized TreeItem<PropertyMap> insertObject(PropertyMap nodeType) {
    TreeItem<PropertyMap> node = getNodeObject((Integer)nodeType.getValue("id"), getRoot()); //поиск существующего объекта
    if(node != null)// элемент существует
      return null;
    // элемент отсутствует
    if(checkBoxTreeProperty.getValue()) {
      node = new FXCheckBoxTreeItem(nodeType);
    }else node = new TreeItem(nodeType); // создаём элемент
    
    // получаем родительский объект
    if(nodeType.getValue("parent") == null) { // родительского объекта нет, то добавляем новый элемент в верхний узел
      getRoot().getChildren().add(node);
    }else { // родительский объект существует
      // получаем элемент дерева соответствующий родительскому объекту
      TreeItem<PropertyMap> parentNode = getNodeObject((Integer)nodeType.getValue("parent"), getRoot());

      if(parentNode == null) { // родительский элемент отсутствует
        PropertyMap parentNodeType = getNodeType((Integer)nodeType.getValue("parent"));
        parentNode = insertObject(parentNodeType); // добавляем родительский объект в дерево (рекурсия)
      }
      // родительский элемент теперь существует
      // добавляем новый элемент в родительский
      parentNode.getChildren().add(node);
    }
    return node;
  }

  @Override
  protected void setData(ObservableList<PropertyMap> objects) {
    nodeTypeData.addAll(objects);
    for(PropertyMap nodeType : objects)
      insertObject(nodeType);
  }

  @Override
  public String[] getFields() {
    return new String[]{"id", "name", "parent"};
  }

  @Override
  public void clearData() {
    getRoot().getChildren().clear();
    nodeTypeData.clear();
  }
  
  public List<TreeItem<PropertyMap>> getCheckedItems() {
    return FXTree.ListItems(getTree()).stream().filter(it -> ((CheckBoxTreeItem)it).isSelected()).collect(Collectors.toList());
  }
  
  public Integer[] getCheckedIds() {
    return getCheckedObjects().stream().map(p -> p.getInteger("id")).collect(Collectors.toList()).toArray(new Integer[0]);
  }

  @Override
  public ReadOnlyObjectProperty<TreeItem<PropertyMap>> selectedItemProperty() {
    return getTree().getSelectionModel().selectedItemProperty();
  }
  
  public void setSelectionMode(SelectionMode mode) {
    getTree().getSelectionModel().setSelectionMode(mode);
  }
  
  @Override
  public Integer[] getSelectedIds() {
    if(checkBoxTreeProperty.getValue())
      return getCheckedIds();
    Integer[] ids = new Integer[0];
    for(TreeItem<PropertyMap> tp:getTree().getSelectionModel().getSelectedItems())
      ids = ArrayUtils.addAll(ids, getAllChildren(tp));
    return ids;
  }
  
  public Integer[] getAllChildren(TreeItem<PropertyMap> parent) {
    Integer[] ids = new Integer[0];
    if(parent != null) {
      ids = ArrayUtils.add(ids, parent.getValue().getInteger("id"));
      for(TreeItem<PropertyMap> node:parent.getChildren())
        ids = ArrayUtils.addAll(ids, getAllChildren(node));
    }
    return ids;
  }

  @Override
  public List<PropertyMap> getSelectedObjects() {
    ObservableList<PropertyMap> list = FXCollections.observableArrayList();
    getTree().getSelectionModel().getSelectedItems().stream().forEach(item -> list.add(item.getValue()));
    return list;
  }
  
  public List<PropertyMap> getCheckedObjects() {
    return getCheckedItems().stream().map(it -> it.getValue()).collect(Collectors.toList());
  }

  @Override
  public List<PropertyMap> getObjects(Node parent, String title, FXD.FXDHandler handler) {
    FXD dialog = new FXD(handler, title, parent, this, Modality.NONE, StageStyle.DECORATED, true, FXD.ButtonType.OK);
    dialog.setOnCloseRequest(e -> dispose());
    dialog.requestFocus();
    
    initData();
    load();
    
    return dialog.showDialog() == FXD.ButtonType.OK ? checkBoxTreeProperty.getValue() ? getCheckedObjects() : getSelectedObjects() : FXCollections.observableArrayList();
  }
  
  public List<PropertyMap> getSubObjects(TreeItem<PropertyMap> parent) {
    ObservableList<PropertyMap> list = FXCollections.observableArrayList();
    list.add(parent.getValue());
    parent.getChildren().stream().forEach(p -> list.addAll(getSubObjects(p)));
    return list;
  }

  @Override
  protected void updateData(ObservableList<PropertyMap> data) {
    data.stream().forEach(d -> {
      TreeItem<PropertyMap> node = getNodeObject(d.getInteger("id"), getRoot());
      if(node != null) {
        if(!Objects.equals(node.getValue().getInteger("parent"), d.getInteger("parent"))) {
          TreeItem<PropertyMap> parent = getNodeObject(d.getInteger("parent"), getRoot());
          parent = parent == null ? getRoot() : parent;
          node.getParent().getChildren().remove(node);
          parent.getChildren().add(node);
        }
        node.getValue().copyFrom(d);
      }else node = insertObject(getNodeType(d.getValue("id", Integer.TYPE)));
      transformItem(node.getValue());
    });
  }

  @Override
  protected void removeObjects(Integer[] ids) {
    ObservableList<Integer> selectedIndices = getTree().getSelectionModel().getSelectedIndices();
    for(Integer id:ids) {
      TreeItem<PropertyMap> ti = getNodeObject(id, getRoot());
      if(ti != null)
        ti.getParent().getChildren().remove(ti);
    }
    for(Integer index:selectedIndices) {
      if(index < getTree().getExpandedItemCount())
        getTree().getSelectionModel().select(index);
    }
  }
  
  private boolean is = true;
  
  public class FXCheckBoxTreeItem extends CheckBoxTreeItem<PropertyMap> {
    //private BooleanProperty activeProperty = new SimpleBooleanProperty(true);

    public FXCheckBoxTreeItem(PropertyMap value) {
      super(value);
      setIndependent(checkBoxIndependentProperty.getValue());
      selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
        //if(activeProperty.getValue()) {
          //if(checkBoxIndependentProperty.getValue() && checkBoxTreeHerachyProperty().getValue())
            //check(this);
          if(checkHandler != null) {
            DivisionTask.start(() -> {
              if(is) {
                is = false;
                try {
                  Thread.sleep(50);
                }catch(InterruptedException ex) {}
                System.out.println(this);
                checkHandler.handle(new Event(FXTreeEditor.this, null, EventType.ROOT));
                is = true;
              }
            });
          }
        //}
      });
    }

    /*public BooleanProperty activeProperty() {
      return activeProperty;
    }*/
    
    /*public void check(FXCheckBoxTreeItem item) {
      for(TreeItem it:item.getChildren()) {
        ((FXCheckBoxTreeItem)it).activeProperty().setValue(false);
        ((FXCheckBoxTreeItem)it).setSelected(item.isSelected());
        ((FXCheckBoxTreeItem)it).activeProperty().setValue(true);
        check((FXCheckBoxTreeItem)it);
      }
    }*/
  }
}