package division.fx.editor;

import bum.editors.util.DivisionTarget;
import bum.editors.util.ObjectLoader;
import bum.editors.util.api.Transaction;
import division.fx.ChoiseLabel.ChoiceLabel;
import division.fx.FXToolButton;
import division.fx.PropertyMap;
import division.fx.dialog.FXD;
import division.fx.gui.FXDisposable;
import division.fx.gui.FXStorable;
import division.fx.util.MsgTrash;
import division.util.DNDUtil;
import division.util.DivisionTask;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import mapping.MappingObject;
import org.apache.commons.lang3.ArrayUtils;
import util.filter.local.DBFilter;

public abstract class FXEditor<S extends PropertyMap> extends BorderPane implements FXStorable, FXDisposable {
  private final StringProperty title = new SimpleStringProperty("root");
  private final ObservableList storeControls = FXCollections.observableArrayList(getControl());
  
  private MappingObject.RemoveAction removeActionType = MappingObject.RemoveAction.MARK_FOR_DELETE;
  
  private final FXToolButton  addToolButton    = new FXToolButton("Создать",       "add-button");
  private final FXToolButton  editToolButton   = new FXToolButton("Редактировать", "edit-button");
  private final FXToolButton  removeToolButton = new FXToolButton("Удалить",       "remove-button");
  private final FXToolButton  questionButton   = new FXToolButton("Справка",       "question-button");
  
  private final MenuItem addMenuItem    = new MenuItem("Создать");
  private final MenuItem editMenuItem   = new MenuItem("Редактировать");
  private final MenuItem removeMenuItem = new MenuItem("Удалить");
  
  private final ToolBar tools = new ToolBar(addToolButton, editToolButton, removeToolButton, new Separator()/*, questionButton*/);
  private final Class<? extends MappingObject> objectClass;
  private final Class<? extends FXObjectEditor> objectEditorClass;
  private InitDataTask initDataTask;
  
  private DBFilter rootFilter;
  private DBFilter tmpFilter;
  private DBFilter typeFilter;
  private DBFilter clientFilter;
  
  private final BooleanProperty activeProperty = new SimpleBooleanProperty(true);
  
  private final ObservableList<FXDisposable> disposablelist = FXCollections.observableArrayList();
  
  private final ObjectProperty<PropertyMap> selectedItem = new SimpleObjectProperty();
  
  private final ObservableList<EventHandler<Event>> initDataListener = FXCollections.observableArrayList();
  
  private EventHandler<ActionEvent> addAction  = e -> {if(isAddable())addAction();};
  private EventHandler<ActionEvent> editAction = e -> {if(isEditable())editAction();};
  private EventHandler<ActionEvent> delAction  = e -> {if(isRemovable())removeAction();};
  
  public enum DoubleClickActionType {EDIT,SELECT}
  private DoubleClickActionType doubleClickActionType = DoubleClickActionType.EDIT;
  
  private List<String> otherFields   = FXCollections.observableArrayList();
  
  private final HashMap<DataFormat, EventHandler<DragEvent>> dragHandlers = new HashMap<>();
  private final ObjectProperty<S> dragOverObject = new SimpleObjectProperty<>();

  public FXEditor(Class<? extends MappingObject> objectClass, Class<? extends FXObjectEditor> objectEditorClass, List<String> otherFields, MappingObject.Type type, Boolean tmp) {
    this.objectClass = objectClass;
    this.objectEditorClass = objectEditorClass;
    this.otherFields = new ArrayList<>(otherFields);
    if(!this.otherFields.contains("id"))
      this.otherFields.add("id");
    init();
    if(type != null)
      typeFilter.AND_EQUAL("type", type);
    if(tmp != null)
      tmpFilter.AND_EQUAL("tmp", tmp);
  }
  
  public BooleanProperty activeProperty() {
    return activeProperty;
  }
  
  public boolean isActive() {
    return activeProperty().getValue();
  }

  public Collection<DataFormat> getAcceptFormats() {
    return dragHandlers.keySet();
  }
  
  public DataFormat getDragFormat() {
    return DNDUtil.getDragFormat(getObjectClass());
  }
  
  public void initDragAndDrop() {
    getControl().setOnDragDetected(e -> startDrag(e));
    getControl().setOnDragDropped(e -> {
      boolean transferDone = false;
      for(DataFormat format:dragHandlers.keySet()) {
        if(e.getDragboard().hasContent(format)) {
          transferDone = true;
          dragHandlers.get(format).handle(e);
        }
      }
      e.setDropCompleted(transferDone);
    });
    /*getControl().setOnDragOver(e -> {
      if(!getDataFormatAccess(e).isEmpty())
        e.acceptTransferModes(TransferMode.ANY);
      else e.acceptTransferModes(TransferMode.NONE);
    });*/
    
    getControl().setOnDragOver(e -> {
      dragOverObject.setValue(getObjectAtPoint(e.getX(), e.getY()));
      if(dragOverObject.getValue() != null && !getDataFormatAccess(e).isEmpty())
        e.acceptTransferModes(TransferMode.ANY);
      else e.acceptTransferModes(TransferMode.NONE);
    });
    getControl().setOnDragExited(e -> dragOverObject.setValue(null));
  }
  
  private void startDrag(MouseEvent e) {
    if(isStartDrag(e)) {
      Collection<S> objects = getSelectedObjects();
      Collection<Map> data = Arrays.asList(PropertyMap.toSimple(objects).toArray(new Map[0]));

      ClipboardContent content = new ClipboardContent();
      content.put(getDragFormat(), data);
      content.putString(getDragString(objects));
      
      Dragboard db = getControl().startDragAndDrop(TransferMode.COPY);
      db.setContent(content);
      
      db.setDragView(getControl().snapshot(null, null));
      e.consume();
    }
  }
  
  public void setOnDrop(EventHandler<DragEvent> e) {
    setOnDrop(getObjectClass(), e);
  }
  
  public void setOnDrop(Class dragClass, EventHandler<DragEvent> e) {
    setOnDrop(DNDUtil.getDragFormat(dragClass), e);
  }
  
  public abstract S getObjectAtPoint(double x, double y);
    
  public void setOnDrop(DataFormat format, EventHandler<DragEvent> e) {
    dragHandlers.put(format, e);
  }
  
  public void removeDrop(DataFormat format) {
    dragHandlers.remove(format);
  }
  
  public ObjectProperty<S> getDragOverObject() {
    return dragOverObject;
  }
  
  public Collection<DataFormat> getDataFormatAccess(DragEvent e) {
    Set<DataFormat> types = e.getDragboard().getContentTypes();
    types.retainAll(getAcceptFormats());
    return types;
  }
  
  public boolean isStartDrag(MouseEvent e) {
    return true;
  }
  
  public String getDragString(Collection<S> objects) {
    String str = "";
    for(S p:objects)
      str += "\n"+p.getString("name");
    return objects.isEmpty() ? str : str.substring(2);
  }
  
  public abstract Node getControl();
  
  public StringProperty titleProperty() {
    return title;
  }

  public Button getAddToolButton() {
    return addToolButton;
  }

  public Button getEditToolButton() {
    return editToolButton;
  }

  public Button getRemoveToolButton() {
    return removeToolButton;
  }

  public MenuItem getAddMenuItem() {
    return addMenuItem;
  }

  public MenuItem getEditMenuItem() {
    return editMenuItem;
  }

  public MenuItem getRemoveMenuItem() {
    return removeMenuItem;
  }
  
  @Override
  public List<Node> storeControls() {
    return storeControls;
  }

  public List<String> getOtherFields() {
    return otherFields;
  }
  
  public DoubleClickActionType getDoubleClickActionType() {
    return doubleClickActionType;
  }

  public void setDoubleClickActionType(DoubleClickActionType doubleClickActionType) {
    this.doubleClickActionType = doubleClickActionType;
  }

  public MappingObject.RemoveAction getRemoveActionType() {
    return removeActionType;
  }

  public void setRemoveActionType(MappingObject.RemoveAction removeActionType) {
    this.removeActionType = removeActionType;
  }

  public ToolBar getTools() {
    return tools;
  }
  
  public void setAdminFunctions(boolean admin) {
    tools.setVisible(admin);
    setTop(admin ? tools : null);
  }
  
  public boolean isAdminFunctions() {
    return tools.isVisible();
  }
  
  public void setEditable(boolean editable) {
    if(editable && !tools.getItems().contains(editToolButton))
      tools.getItems().add(isAddable() ? 1 : 0, editToolButton);
    if(!editable)
      tools.getItems().remove(editToolButton);
  }
  
  public boolean isEditable() {
    return tools.getItems().contains(editToolButton);
  }
  
  public void setAddable(boolean editable) {
    if(editable && !tools.getItems().contains(addToolButton))
      tools.getItems().add(isAddable() ? 1 : 0, addToolButton);
    if(!editable)
      tools.getItems().remove(addToolButton);
  }
  
  public boolean isAddable() {
    return tools.getItems().contains(addToolButton);
  }
  
  public void setRemovable(boolean editable) {
    if(editable && !tools.getItems().contains(removeToolButton))
      tools.getItems().add(isAddable() ? 1 : 0, removeToolButton);
    if(!editable)
      tools.getItems().remove(removeToolButton);
  }
  
  public boolean isRemovable() {
    return tools.getItems().contains(removeToolButton);
  }
  
  private void init() {
    setId(getClass().getSimpleName()+"_"+getObjectClass().getSimpleName());
    
    setTop(tools);
    
    getStyleClass().add("FXEditor");
    
    addMenuItem.getStyleClass().add("addMenuItem");
    editMenuItem.getStyleClass().add("editMenuItem");
    removeMenuItem.getStyleClass().add("removeMenuItem");
    
    setAddable(getObjectEditorClass() != null);
    setEditable(getObjectEditorClass() != null);
    
    addMenuItem.setDisable(getObjectEditorClass() == null);
    editMenuItem.setDisable(getObjectEditorClass() == null);
    
    
    rootFilter   = DBFilter.create(objectClass);
    tmpFilter    = rootFilter.AND_FILTER();
    typeFilter   = rootFilter.AND_FILTER();
    clientFilter = rootFilter.AND_FILTER();
    
    DivisionTarget.create(this, objectClass, (DivisionTarget target, String type, Integer[] ids, PropertyMap objectEventProperty) -> {
      Platform.runLater(() -> {
        switch(type) {
          case "CREATE":
            createObjects(ids);
            break;
          case "UPDATE":
            updateObjects(ids);
            break;
          case "REMOVE":
            removeObjects(ids);
            break;
        }
      });
    });
    
    addToolButton.setOnAction(getAddAction());
    editToolButton.setOnAction(getEditAction());
    removeToolButton.setOnAction(getRemoveAction());
  }
  
  protected void createObjects(Integer[] ids) {
    Integer[] createdIds = ObjectLoader.isSatisfy(rootFilter, ids);
    if(createdIds.length > 0)
      initData(createdIds);
  }

  protected void updateObjects(Integer[] ids) {
    Integer[] updateIds = ObjectLoader.isSatisfy(rootFilter, ids);
    
    if(updateIds.length > 0)
      initData(updateIds);
    
    for(Integer id:updateIds)
      ids = ArrayUtils.removeElement(ids, id);
    if(ids.length > 0)
      removeObjects(ids);
  }

  protected void removeObjects(Integer[] ids) {
  }

  public EventHandler<ActionEvent> getAddAction() {
    return addAction;
  }

  public void setAddAction(EventHandler<ActionEvent> addAction) {
    addToolButton.removeEventHandler(ActionEvent.ACTION, this.addAction);
    this.addAction = addAction;
    addToolButton.setOnAction(this.addAction);
  }

  public EventHandler<ActionEvent> getEditAction() {
    return editAction;
  }

  public void setEditAction(EventHandler<ActionEvent> editAction) {
    editToolButton.removeEventHandler(ActionEvent.ACTION, this.editAction);
    this.editAction = editAction;
    editToolButton.setOnAction(this.editAction);
  }

  public EventHandler<ActionEvent> getRemoveAction() {
    return delAction;
  }

  public void setRemoveAction(EventHandler<ActionEvent> delAction) {
    removeToolButton.removeEventHandler(ActionEvent.ACTION, this.delAction);
    this.delAction = delAction;
    removeToolButton.setOnAction(this.delAction);
  }

  /*@Override
  public String getId(Node node) {
    return Storable.super.getId(node)+"-"+getObjectClass().getSimpleName();
  }*/
  
  protected void addAction() {
    setCursor(Cursor.WAIT);
    try {
      FXObjectEditor editor = (FXObjectEditor)getObjectEditorClass().newInstance();
      editor.setObjectClass(getObjectClass());
      
      PropertyMap object = PropertyMap.create().setValue("tmp", true);
      rootFilter.toEstablishes(object);
      
      editor.setObjectProperty(object);
      editPropertyObjectBeforeCreateDialog(editor.getObjectProperty());
      
      editor.showAndWait(this);
    }catch(Exception ex) {
      MsgTrash.out(ex);
    }finally {
      setCursor(Cursor.DEFAULT);
    }
  }
  
  protected void editAction() {
    Integer[] ids = getSelectedIds();
    if(ids.length > 0) {
      setCursor(Cursor.WAIT);
      try {
        FXObjectEditor editor = (FXObjectEditor)getObjectEditorClass().newInstance();
        editor.setObjectClass(getObjectClass());
        editor.setObjectProperty(Transaction.create().getMap(getObjectClass(), ids[0]));
        editPropertyObjectBeforeEditDialog(editor.getObjectProperty());
        editor.showAndWait(this);
      }catch(Exception ex) {
        MsgTrash.out(ex);
      }finally {
        setCursor(Cursor.DEFAULT);
      }
    }
  }
  
  protected void editPropertyObjectBeforeCreateDialog(PropertyMap prop) {
    prop.setValue("tmp", true);
  }
  
  protected void editPropertyObjectBeforeEditDialog(PropertyMap prop) {
  }
  
  public abstract List<S> getSelectedObjects();
  public abstract Integer[] getSelectedIds();
  
  protected void removeAction() {
    Integer[] ids = getSelectedIds();
    if(ids.length > 0) {
      String msg = null;
      switch(getRemoveActionType()) {
        case DELETE:          msg = "Удалить безвозвратно?";break;
        case MARK_FOR_DELETE: msg = "Удалить выбранные объекты?";break;
        case MOVE_TO_ARCHIVE: msg = "Переместить выбранные объекты в архив?";break;
      }
      if(FXD.showWait("Удаление", this, msg, FXD.ButtonType.YES, FXD.ButtonType.NO).orElseGet(() -> FXD.ButtonType.NO) == FXD.ButtonType.YES) {
        switch(getRemoveActionType()) {
          case DELETE:
            ObjectLoader.removeObjects(objectClass, ids);
            break;
          case MARK_FOR_DELETE:
            ObjectLoader.toTmpObjects(objectClass, ids, true);
            break;
          case MOVE_TO_ARCHIVE:
            ObjectLoader.toTypeObjects(objectClass, MappingObject.Type.ARCHIVE, ids);
            break;
        }
      }
    }
  }
  
  public DBFilter getClientFilter() {
    return clientFilter;
  }
  
  public Class<? extends MappingObject> getObjectClass() {
    return objectClass;
  }

  public Class<? extends FXObjectEditor> getObjectEditorClass() {
    return objectEditorClass;
  }

  public void initData() {
    initData(null);
  }
  
  public void initData(Integer... updateIds) {
    if(updateIds == null || updateIds.length == 0) {
      InitDataTask.stop(initDataTask);
      clearData();
    }
    if(isActive())
      InitDataTask.start(initDataTask = new InitDataTask(updateIds));
  }

  protected DBFilter getRootFilter() {
    return rootFilter;
  }

  @Override
  public List<FXDisposable> disposeList() {
    return disposablelist;
  }

  @Override
  public void finaly() {
    store();
    InitDataTask.stop(initDataTask);
    clearData();
  }
  
  class InitDataTask extends DivisionTask {
    private Integer[] updateIds;

    public InitDataTask(Integer[] updateIds) {
      super("table -> " + getObjectClass().getSimpleName(), FXEditor.this);
      this.updateIds = updateIds == null ? new Integer[0] : updateIds;
    }
    
    @Override
    public void task() throws DivisionTaskException {
      ObservableList<PropertyMap> its = FXCollections.observableArrayList();
      if(updateIds.length == 0) {
        ObjectLoader.fillList(rootFilter, its, ArrayUtils.addAll(getOtherFields().toArray(new String[0]), getFields()));
        checkShutdoun();
        Platform.runLater(() -> {
          its.stream().forEach(item -> transformItem(item));
          setData(its);
          fireInitData();
        });
      }else {
        checkShutdoun();
        Platform.runLater(() -> updateData(ObjectLoader.getList(objectClass, updateIds, getFieldsAndOther())));
      }
    }
  }
  
  public String[] getFieldsAndOther() {
    return ArrayUtils.addAll(getOtherFields().toArray(new String[0]), getFields());
  }
  
  public void fireInitData() {
    for(EventHandler<Event> listener:initDataListener)
      listener.handle(new Event(this, this, EventType.ROOT));
  }
  
  public void addInitDataListener(EventHandler<Event> listener) {
    initDataListener.add(listener);
  }
  
  public void removeInitDataListener(EventHandler<Event> listener) {
    initDataListener.remove(listener);
  }
  
  public ObservableList<EventHandler<Event>> initDataListeners() {
    return initDataListener;
  }
  
  public abstract ReadOnlyObjectProperty selectedItemProperty();
  
  protected abstract void updateData(ObservableList<PropertyMap> data);
  protected abstract void setData(ObservableList<PropertyMap> objects);
  protected void transformItem(PropertyMap item) {};
  
  public abstract String[] getFields();
  public abstract void clearData();
  
  public PropertyMap create() {
    return PropertyMap.create();
  }

  @Override
  public String storeFileName() {
    return FXStorable.super.storeFileName()+"-"+getObjectClass().getSimpleName();
  }
  
  public List<S> getObjects(Node parent, String title) {
    return getObjects(parent, title, null);
  }
  
  public List<S> getObjects(Node parent, String title, FXD.FXDHandler handler) {
    FXD dialog = new FXD(handler, title, parent, this, Modality.NONE, StageStyle.DECORATED, true, FXD.ButtonType.OK);
    dialog.setOnCloseRequest(e -> dispose());
    dialog.requestFocus();
    
    initData();
    load();
    
    List<S> data = FXCollections.observableArrayList();
    
    if(dialog.showDialog() == FXD.ButtonType.OK){
      data = PropertyMap.copyList(getSelectedObjects());
      dispose();
    }
    return data;
  }
  
  public ChoiceLabel<PropertyMap> getChoiceLabel(String name, String promt, Node parent) {
    final ChoiceLabel<PropertyMap> field = new ChoiceLabel<>(name);
    field.promtTextProperty().setValue(promt);
    field.startListener().add(e -> {
      try {
        field.itemsProperty().getValue().setAll(
          PropertyMap.create().setValue("name", "Редактировать список").setValue("check", false).setValue("action", (EventHandler<ActionEvent>) (ActionEvent event) -> getObjects(parent, name)),
          PropertyMap.create().setValue("name", "создать и выбрать").setValue("check", false).setValue("action", (EventHandler<ActionEvent>) (ActionEvent event) -> {
            try {
              FXObjectEditor editor = getObjectEditorClass().newInstance();
              PropertyMap val = PropertyMap.create();
              editor.setObjectProperty(val);
              if(editor.showAndWait(parent))
                field.valueProperty().setValue(val);
            }catch(Exception ex) {
              MsgTrash.out(ex);
            }
          }));
        field.itemsProperty().getValue().addAll(ObjectLoader.getList(getObjectClass(),getFieldsAndOther()));
      } catch (Exception ex) {
        MsgTrash.out(ex);
      }
    });
    return field;
  }
}
