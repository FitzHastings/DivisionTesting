package division.fx.controller.unit;

import bum.interfaces.Unit;
import division.fx.DivisionTextField;
import division.fx.editor.FXObjectEditor;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;

public class FXUnit extends FXObjectEditor {
  @FXML
  private RadioButton intval;
  @FXML
  private RadioButton floatval;
  @FXML
  private DivisionTextField nameField;
  @FXML
  private TextArea descriptField;
  
  private ToggleGroup group;
  
  @Override
  public void initData() {
    nameField.setText(getObjectProperty().isNotNull("name") ? getObjectProperty().getString("name") : "");
    descriptField.setText(getObjectProperty().isNotNull("comments") ? getObjectProperty().getString("comments") : "");
    intval.setSelected(getObjectProperty().isNotNull("intval") ? getObjectProperty().is("intval") : true);
    floatval.setSelected(!intval.isSelected());
    
    getObjectProperty().get("name").bind(nameField.textProperty());
    getObjectProperty().get("comments").bind(descriptField.textProperty());
    getObjectProperty().get("intval").bind(intval.selectedProperty());
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    setObjectClass(Unit.class);
    group = new ToggleGroup();
    group.getToggles().addAll(intval, floatval);
  }
}