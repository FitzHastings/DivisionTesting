package division.fx.tree;

import division.fx.PropertyMap;
import division.fx.table.DivisionCellEditor;
import division.fx.table.FXColumn;
import division.fx.table.FXDivisionCell;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.util.StringConverter;

public class FXDivisionTreeTableCell<S,T> extends TreeTableCell<S,T> implements FXDivisionCell<S,T> {
  private Node component;
  private final ObjectProperty<DivisionCellEditor> cellEditorProperty = new SimpleObjectProperty<>();
  private final ObservableList<Object> values = FXCollections.observableArrayList();
  private final ObjectProperty<StringConverter> customTextConverterProperty = new SimpleObjectProperty<>();

  public FXDivisionTreeTableCell(StringConverter customTextConverter) {
    this(customTextConverter, null);
  }
  
  public FXDivisionTreeTableCell(DivisionCellEditor cellEditor) {
    this(null, cellEditor);
  }
  
  public FXDivisionTreeTableCell(String... values) {
    this(null, null, values);
  }
  
  private FXDivisionTreeTableCell(StringConverter customTextConverter, DivisionCellEditor cellEditor, String... values) {
    init();
    customTextConverterProperty().setValue(customTextConverter);
    cellEditorProperty().setValue(cellEditor);
    this.values.setAll(values);
  }
  
  private void init() {
    cellEditorProperty().addListener((ObservableValue<? extends DivisionCellEditor> observable, DivisionCellEditor oldValue, DivisionCellEditor newValue) -> {
      if(newValue != null)
        newValue.setCell(FXDivisionTreeTableCell.this);
    });
  }

  @Override
  public void startEdit() {
    if(getTreeTableView() instanceof FXDivisionTreeTable)
      ((FXDivisionTreeTable)getTreeTableView()).findableProperty().setValue(false);
    super.startEdit();
    edit();
  }

  @Override
  public void cancelEdit() {
    if(getTreeTableView() instanceof FXDivisionTreeTable)
      ((FXDivisionTreeTable)getTreeTableView()).findableProperty().setValue(true);
    super.cancelEdit();
    cancel();
  }
  
  @Override
  public void commitEdit(T newValue) {
    commit(newValue);
    if(getTreeTableView() instanceof FXDivisionTreeTable)
      ((FXDivisionTreeTable)getTreeTableView()).findableProperty().setValue(true);
    //if(newValue!= null)
      //super.commitEdit(newValue);
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
    return (PropertyMap)getTreeTableRow().getItem();
  }

  @Override
  public DivisionCellEditor getCellEditor() {
    return cellEditorProperty.getValue();
  }

  @Override
  public void setComponent(Node component) {
    this.component = component;
  }
  
  public ObjectProperty<DivisionCellEditor> cellEditorProperty() {
    return cellEditorProperty;
  }
  
  public ObjectProperty<StringConverter> customTextConverterProperty() {
    return customTextConverterProperty;
  }

  @Override
  public FXColumn getColumn() {
    return (FXColumn)getTableColumn();
  }
}