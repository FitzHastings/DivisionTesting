package division.fx.table;

import division.fx.PropertyMap;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.util.StringConverter;

public class FXDivisionTableCell<S,T> extends TableCell<S,T> implements FXDivisionCell<S,T> {
  private Node component;
  private final ObjectProperty<DivisionCellEditor> cellEditorProperty = new SimpleObjectProperty<>();
  private final ObservableList<Object> values = FXCollections.observableArrayList();
  private final ObjectProperty<StringConverter> customTextConverterProperty = new SimpleObjectProperty<>();
  
  public FXDivisionTableCell() {
    this(null, null, new Object[0]);
  }
  
  public FXDivisionTableCell(StringConverter customTextConverter) {
    this(customTextConverter, new Object[0]);
  }
  
  public FXDivisionTableCell(Object... values) {
    this(null, values);
  }
  
  public FXDivisionTableCell(List values) {
    this(null, values.toArray());
  }
  
  public FXDivisionTableCell(DivisionCellEditor cellEditor) {
    this(cellEditor, null, new Object[0]);
  }

  public FXDivisionTableCell(StringConverter customTextConverter, Object... values) {
    this(null, customTextConverter, values);
  }
  
  private FXDivisionTableCell(DivisionCellEditor cellEditor, StringConverter customTextConverter, Object... vals) {
    if(customTextConverter != null)
      customTextConverterProperty.setValue(customTextConverter);
    if(vals != null)
      values.addAll(vals);
    if(cellEditor != null)
      cellEditorProperty.setValue(cellEditor);
    if(cellEditor != null)
      cellEditor.setCell(this);
  }

  @Override
  public void startEdit() {
    if(getTableView() instanceof FXDivisionTable)
      ((FXDivisionTable)getTableView()).findableProperty().setValue(false);
    super.startEdit();
    edit();
  }

  @Override
  public void cancelEdit() {
    if(getTableView() instanceof FXDivisionTable)
      ((FXDivisionTable)getTableView()).findableProperty().setValue(true);
    super.cancelEdit();
    cancel();
  }

  @Override
  public void commitEdit(T newValue) {
    commit(newValue);
    if(getTableView() instanceof FXDivisionTable)
      ((FXDivisionTable)getTableView()).findableProperty().setValue(true);
    super.commitEdit(newValue);
  }

  @Override
  protected void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);
    update(item, empty, getStyleClass());
  }
  
  @Override
  public StringConverter getCustomTextConverter() {
    return customTextConverterProperty.getValue();
  }

  @Override
  public Node getComponent() {
    return component;
  }

  @Override
  public ObservableList getValues() {
    return values;
  }
  
  @Override
  public PropertyMap getRowItem() {
    return getTableRow() == null ? null : (PropertyMap)getTableRow().getItem();
  }

  @Override
  public DivisionCellEditor getCellEditor() {
    return cellEditorProperty.getValue();
  }

  @Override
  public void setComponent(Node component) {
    this.component = component;
  }

  @Override
  public FXColumn getColumn() {
    return (FXColumn)getTableColumn();
  }

  public ObjectProperty<StringConverter> customTextConverterProperty() {
    return customTextConverterProperty;
  }

  public ObjectProperty<DivisionCellEditor> cellEditorProperty() {
    return cellEditorProperty;
  }
}