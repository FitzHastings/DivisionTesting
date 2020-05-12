package division.fx.editor;

import division.fx.table.Column;
import bum.editors.util.ObjectLoader;
import division.fx.table.FXDivisionTable;
import division.fx.PropertyMap;
import division.fx.dialog.FXD;
import division.fx.util.MsgTrash;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import mapping.MappingObject;
import mapping.MappingObject.Type;
import org.apache.commons.lang3.ArrayUtils;

public class FXTableEditor extends FXEditor<PropertyMap> {
  private final FXDivisionTable<PropertyMap> table = new FXDivisionTable();
  
  public BooleanProperty editableInTableProperty = new SimpleBooleanProperty(false);
  
  public FXTableEditor(Class<? extends MappingObject> objectClass, Class<? extends FXObjectEditor> objectEditorClass, Column<PropertyMap,Object>... columns) {
    this(objectClass, objectEditorClass, FXCollections.observableArrayList(), Type.CURRENT, Boolean.FALSE, columns);
  }
  
  public FXTableEditor(Class<? extends MappingObject> objectClass, Class<? extends FXObjectEditor> objectEditorClass, Type type, Column<PropertyMap,Object>... columns) {
    this(objectClass, objectEditorClass, FXCollections.observableArrayList(), type, Boolean.FALSE, columns);
  }
  
  public FXTableEditor(Class<? extends MappingObject> objectClass, Class<? extends FXObjectEditor> objectEditorClass, Type type, Boolean tmp, Column<PropertyMap,Object>... columns) {
    this(objectClass, objectEditorClass, FXCollections.observableArrayList(), type, tmp, columns);
  }
  
  public FXTableEditor(Class<? extends MappingObject> objectClass, Class<? extends FXObjectEditor> objectEditorClass, List<String> otherFields, Column<PropertyMap,Object>... columns) {
    this(objectClass, objectEditorClass, otherFields, Type.CURRENT, Boolean.FALSE, columns);
  }
  
  public FXTableEditor(Class<? extends MappingObject> objectClass, Class<? extends FXObjectEditor> objectEditorClass, List<String> otherFields, Type type, Column<PropertyMap,Object>... columns) {
    this(objectClass, objectEditorClass, otherFields, type, Boolean.FALSE, columns);
  }
  
  public FXTableEditor(Class<? extends MappingObject> objectClass, Class<? extends FXObjectEditor> objectEditorClass, List<String> otherFields, Type type, Boolean tmp, Column<PropertyMap,Object>... columns) {
    super(objectClass, objectEditorClass, otherFields, type, tmp);
    table.getColumns().setAll(columns);
    table.getStyleClass().add(getObjectClass().getSimpleName()+"-table");
    table.setId(getClass().getSimpleName()+"-"+getObjectClass().getSimpleName());
    storeControls().add(table);
    init();
  }

  @Override
  public PropertyMap getObjectAtPoint(double x, double y) {
    int index = getTable().getRowAtPoint(x, y);
    return index >= 0 ? getTable().getItems().get(index) : null;
  }

  private void init() {
    initDragAndDrop();
    table.setOnKeyPressed(e -> {
      if(e.getCode() == KeyCode.DELETE)
        getRemoveAction().handle(null);
    });
    
    table.editableProperty().bind(editableInTableProperty);
    table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    setCenter(table);
    storeControls().add(table);
    
    table.setOnScroll(e -> {
      if(e.isControlDown()) {
        double s = e.getDeltaY() > 0 ? 0.1 : -0.1;
        table.setScaleX(table.getScaleX()+s);
        table.setScaleY(table.getScaleY()+s);
      }
    });
    
    table.setOnMouseClicked((MouseEvent event) -> {
      if(event.getClickCount() == 2) {
        switch(getDoubleClickActionType()) {
          case SELECT:
            Window dialog = getScene().getWindow();
            if(dialog != null && dialog instanceof FXD) {
              ((FXD)dialog).fire(FXD.ButtonType.OK);
            }
            break;
          case EDIT:
            getEditAction().handle(null);
            break;
        }
      }
    });
    
    editableInTableProperty.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      for(TableColumn<PropertyMap,Object> column:table.getAllColumns(null, true)) {
        if(column.isEditable()) {
          column.setEditable(newValue);
          if(newValue) {
            column.setOnEditCommit((TableColumn.CellEditEvent<PropertyMap, Object> event) -> {
              PropertyMap r = table.getItems().get(event.getTablePosition().getRow());
              String dbFieldName  = ((Column)column).getDatabaseColumnName();
              String propertyName = ((Column)column).getColumnName();
              if(ObjectLoader.executeUpdate(getObjectClass(), dbFieldName, event.getNewValue(), (Integer)r.getValue("id")) != 1)
                event.consume();
            });
          }else column.removeEventHandler(TableColumn.editCommitEvent(), column.getOnEditCommit());
        }
      }
    });
  }

  @Override
  public boolean isStartDrag(MouseEvent e) {
    return getTable().getRowAtPoint(e.getX(), e.getY()) >= 0;
  }

  @Override
  public Node getControl() {
    return getTable();
  }

  @Override
  protected void addAction() {
    if(editableInTableProperty.getValue()) {
      setCursor(Cursor.WAIT);
      try {
        MappingObject object = ObjectLoader.getObject(getObjectClass(), ObjectLoader.createObject(getObjectClass(), create().setValue("name", "...").setValue("tmp", false).getSimpleMap(), true));
        if(!getRootFilter().isEmpty())
          ObjectLoader.createSession(true).toEstablishes(getRootFilter(), object);
        ObjectLoader.sendMessage(getObjectClass(), "CREATE", object.getId());
      }catch(Exception ex) {
        MsgTrash.out(ex);
      }finally {
        setCursor(Cursor.DEFAULT);
      }
    }else super.addAction();
  }

  public Column getColumn(String columnName) {
    return getColumn(table.getColumns(), columnName);
  }
  
  public Column getColumn(ObservableList columns, String columnName) {
    Column returnColumn = null;
    for(Object c:columns) {
      if(columnName.equals(((Column)c).getColumnName()))
        returnColumn = (Column)c;
      else returnColumn = getColumn(((Column)c).getColumns(), columnName);
      if(returnColumn != null)
        break;
    }
    return returnColumn;
  }
  
  @Override
  public String[] getFields() {
    String[] fields = new String[0];
    for(TableColumn<PropertyMap, ?> column:table.getColumns()) {
      if(column instanceof Column)
        fields = ArrayUtils.addAll(fields, getFields((Column<PropertyMap, Object>)column));
    }
    return fields;
  }
  
  private String[] getFields(Column<PropertyMap, Object> column) {
    String[] fields = new String[0];
    for(TableColumn c:column.getColumns())
      fields = ArrayUtils.addAll(fields, getFields((Column)c));
    if(fields.length == 0 && ((Column)column).getDatabaseColumnName() != null)
      fields = new String[]{((Column)column).getDatabaseColumnName()};
    return fields;
  }

  public FXDivisionTable<PropertyMap> getTable() {
    return table;
  }
  
  public void setSelectionMode(SelectionMode mode) {
    getTable().getSelectionModel().setSelectionMode(mode);
  }
  
  @Override
  public ReadOnlyObjectProperty<PropertyMap> selectedItemProperty() {
    return getTable().getSelectionModel().selectedItemProperty();
  }

  @Override
  public void clearData() {
    if(!table.getSourceItems().isEmpty())
      table.getSourceItems().clear();
  }
  
  public Column[] getLastColumns(Column column) {
    Column[] columns = new Column[0];
    ObservableList cols = column == null ? table.getColumns() : column.getColumns();
    if(cols.isEmpty())
      columns = new Column[]{column};
    else for(Object c:cols)
      columns = ArrayUtils.addAll(columns, getLastColumns((Column)c));
    return columns;
  }
  
  @Override
  protected void setData(ObservableList<PropertyMap> objects) {
    table.getSourceItems().addAll(objects);
  }
  
  @Override
  public Integer[] getSelectedIds() {
    return getTable().getSelectionModel().getSelectedItems().stream().map(d -> d.getInteger("id")).collect(Collectors.toList()).toArray(new Integer[0]);
  }

  @Override
  public ObservableList<PropertyMap> getSelectedObjects() {
    return table.getSelectionModel().getSelectedItems();
  }
  
  @Override
  protected void updateData(ObservableList<PropertyMap> data) {
    for(int i=data.size()-1;i>=0;i--) {
      PropertyMap updaterow = null;
      for(int j=table.getSourceItems().size()-1;j>=0;j--) {
        if(data.get(i).getValue("id").equals(table.getSourceItems().get(j).getValue("id"))) {
          updaterow = table.getSourceItems().get(j);
          break;
        }
      }
      if(updaterow == null)
        table.getSourceItems().add(updaterow = data.get(i));
      else updaterow.copyFrom(data.get(i));
      
      transformItem(updaterow);
    }
  }

  @Override
  protected void removeObjects(Integer[] ids) {
    ObservableList<Integer> selectedIndices = table.getSelectionModel().getSelectedIndices();
    for(int i=table.getSourceItems().size()-1;i>=0;i--)
      if(ArrayUtils.contains(ids, table.getSourceItems().get(i).getValue("id")))
        table.getSourceItems().remove(i);
    for(Integer index:selectedIndices) {
      if(index < table.getSourceItems().size())
        table.getSelectionModel().select(index);
    }
  }
}