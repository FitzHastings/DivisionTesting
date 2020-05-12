package division.util.actions;

import javafx.beans.value.WritableValue;
import javafx.scene.control.TableCell;

public class CustomEditableCell<T,S> extends TableCell<T, S> {
  private final String propertyName;

  public CustomEditableCell(String propertyName) {
    this.propertyName = propertyName;
  }

  @Override
  public void startEdit() {
    setEditable(getItem() != null && isCustomEditable((T) getTableRow().getItem()));
    if(isEditable())
      super.startEdit();
  }
  
  public boolean isCustomEditable(T row) {
    return true;
  }
  
  public void setValue(Object value) {
    try {
      ((WritableValue)getTableRow().getItem().getClass().getMethod(propertyName+"Property").invoke(getTableRow().getItem())).setValue(value);
    }catch(Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}