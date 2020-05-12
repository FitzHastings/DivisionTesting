package division.util.actions;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;

public class DateCell<T> extends CustomEditableCell<T, LocalDate> {
  private final DatePicker datePicker = new DatePicker();

  public DateCell(String datePropertyName) {
    super(datePropertyName);
    focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(!newValue)
        startEdit();
      else cancelEdit();
    });
    
    datePicker.valueProperty().addListener((ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) -> {
      commitEdit(newValue);
    });
  }

  @Override
  public void startEdit() {
    super.startEdit();
    if(isEditable()) {
      super.startEdit();
      datePicker.setValue(getItem());
      setGraphic(datePicker);
      setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
      datePicker.show();
    }
  }

  @Override
  protected void updateItem(LocalDate item, boolean empty) {
    super.updateItem(item, empty);
    if(!empty)
      setText(item == null ? "" : item.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
  }

  @Override
  public void cancelEdit() {
    super.cancelEdit();
    setText(getItem().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
    setGraphic(null);
    setContentDisplay(ContentDisplay.TEXT_ONLY);
  }

  @Override
  public void commitEdit(LocalDate newValue) {
    super.commitEdit(newValue);
    setValue(newValue);
    setGraphic(null);
    setContentDisplay(ContentDisplay.TEXT_ONLY);
  }
}