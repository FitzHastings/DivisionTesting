package division.fx.controller.company;

import division.fx.editor.FXObjectEditor;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class FXOwnerShipType extends FXObjectEditor {
  @FXML private TextArea transcript;
  @FXML private TextField name;

  @Override
  public String validate() {
    return getObjectProperty().getValue("name") == null || getObjectProperty().getValue("name").equals("") ? "Введите наименование": null;
  }

  @Override
  public void initData() {
    name.setText((String)getObjectProperty().getValue("name"));
    transcript.setText((String)getObjectProperty().getValue("transcript"));
    getObjectProperty().get("name").bind(name.textProperty());
    getObjectProperty().get("transcript").bind(transcript.textProperty());
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    initData();
  }
}