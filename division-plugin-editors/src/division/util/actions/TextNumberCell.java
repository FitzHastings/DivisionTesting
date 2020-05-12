package division.util.actions;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class TextNumberCell<T> extends CustomEditableCell<T, String>{
  private TextField textField = new TextField();

  public TextNumberCell(String propertyName) {
    super(propertyName);
    textField.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(!newValue)
        commitEdit(textField.getText());
    });

    textField.setOnKeyPressed((KeyEvent event) -> {
      if(event.getCode() == KeyCode.ENTER)
        commitEdit(textField.getText());
      else if(event.getCode() == KeyCode.ESCAPE)
        cancelEdit();
    });
  }
  
  @Override
  public void startEdit() {
    super.startEdit();
    if(isEditable()) {
      textField.setText(getItem());
      setGraphic(textField);
      setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
      textField.requestFocus();
      textField.selectAll();
    }
  }

  @Override
  public void cancelEdit() {
    super.cancelEdit();
    setText(getItem());
    setContentDisplay(ContentDisplay.TEXT_ONLY);
  }

  @Override
  protected void updateItem(String item, boolean empty) {
    super.updateItem(item, empty);
    if(!empty)
      setText(item);
  }

  @Override
  public void commitEdit(String newValue) {
    super.commitEdit(newValue);
    setValue(newValue);
    setContentDisplay(ContentDisplay.TEXT_ONLY);
  }
}