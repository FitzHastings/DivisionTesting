package division.fx.controller.process;

import bum.interfaces.Service;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.editor.FXObjectEditor;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

public class FXProcess extends FXObjectEditor {
        private CheckBox objectCheckBox = new CheckBox("Применяется к объектам:");
  @FXML private TitleBorderPane   objectTitleBorder;
  @FXML private TextField         processName;
  @FXML private CheckBox          withChangeOwnerCheckBox;
  @FXML private RadioButton       sellerCheckBox;
  @FXML private RadioButton       customerCheckBox;
  
  @Override
  public void initData() {
    objectTitleBorder.setTitle(objectCheckBox);
    
    processName.setText(getObjectProperty().isNotNull("name") ? getObjectProperty().getString("name") : "");
    
    //Блокировать блок объектов если это группа процессов
    objectTitleBorder.getCenter().disableProperty().bind(Bindings.createBooleanBinding(() -> 
            !objectCheckBox.isSelected() || getObjectProperty().getValue("childs") != null && ((Integer[])getObjectProperty().getValue("childs")).length > 0, 
            objectCheckBox.selectedProperty(), getObjectProperty().get("childs")));
    objectTitleBorder.setDisable(getObjectProperty().getValue("childs") != null && ((Integer[])getObjectProperty().getValue("childs")).length > 0);
    
    titleProperty().bind(processName.textProperty());
    
    if(getObjectProperty().getValue("childs") == null || ((Integer[])getObjectProperty().getValue("childs")).length == 0) {
      sellerCheckBox.setSelected(getObjectProperty().is("tmp") || Service.Owner.SELLER == getObjectProperty().getValue("owner", Service.Owner.class));
      customerCheckBox.setSelected(!getObjectProperty().is("tmp") && Service.Owner.CUSTOMER == getObjectProperty().getValue("owner", Service.Owner.class));
      withChangeOwnerCheckBox.setSelected(getObjectProperty().getValue("moveStorePosition", false));
      
      objectCheckBox.setSelected(sellerCheckBox.isSelected() || customerCheckBox.isSelected());

      getObjectProperty().get("name").bind(processName.textProperty());
      getObjectProperty().get("moveStorePosition").bind(objectCheckBox.selectedProperty().and(withChangeOwnerCheckBox.selectedProperty().and(sellerCheckBox.selectedProperty())));
      
      getObjectProperty().get("owner").bind(Bindings.createObjectBinding(() -> !objectCheckBox.isSelected() ? null : sellerCheckBox.isSelected() ? Service.Owner.SELLER : Service.Owner.CUSTOMER, 
              sellerCheckBox.selectedProperty(), customerCheckBox.selectedProperty(), objectCheckBox.selectedProperty()));
      
      setMementoProperty(getObjectProperty().copy());
    }
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    withChangeOwnerCheckBox.disableProperty().bind(sellerCheckBox.selectedProperty().not());
    new ToggleGroup().getToggles().addAll(sellerCheckBox, customerCheckBox);
  }

  @Override
  public String validate() {
    return getObjectProperty().getValue("name", "").equals("") ? "Введите наименование" : null;
  }
  
  @Override
  public boolean save() {
    return super.save("id","name","owner","tmp","moveStorePosition","parent");
  }
}