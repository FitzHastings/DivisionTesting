package division.fx.controller.store;

import bum.editors.util.ObjectLoader;
import bum.interfaces.Group;
import bum.interfaces.Store;
import division.fx.editor.FXEditor;
import division.fx.editor.FXObjectEditor;
import division.fx.editor.FXTreeEditor;
import division.fx.PropertyMap;
import division.fx.FXUtility;
import division.fx.dialog.FXD;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;

public class FXStore extends FXObjectEditor {
  @FXML private ComboBox storeType;
  @FXML private CheckBox creditControll;
  @FXML private ComboBox storeObjectType;
  @FXML private TextField storeName;
  @FXML private Label parentStore;
  @FXML private Label currencyLabel;
  
  @Override
  public String validate() {
    return getObjectProperty().getValue("name") == null || getObjectProperty().getValue("name").equals("") ? "Введите наименование": null;
  }
  
  @Override
  public void initData() {
    getObjectProperty().keySet().stream().forEach(key -> getObjectProperty().get(key).unbind());
    
    storeName.requestFocus();
    storeName.setText((String)getObjectProperty().getValue("name"));
    
    if(getObjectProperty().getValue("storeType") != null)
      storeType.getSelectionModel().select(getObjectProperty().getValue("storeType"));
    
    if(getObjectProperty().getValue("objectType") != null)
      storeObjectType.getSelectionModel().select(getObjectProperty().getValue("objectType"));
    
    if(getObjectProperty().getValue("currency") != null && getObjectProperty().getValue("currency") instanceof Integer)
      getObjectProperty().setValue("currency", ObjectLoader.getMap(Group.class, getObjectProperty().getInteger("currency"), "id","name"));
    
    if(getObjectProperty().isNotNull("parent") && getObjectProperty().getValue("parent") instanceof Integer)
      getObjectProperty().setValue("parent", ObjectLoader.getMap(Store.class, getObjectProperty().getInteger("id")));
    
    creditControll.setSelected(getObjectProperty().is("controllOut"));
    
    getObjectProperty().get("name").bind(storeName.textProperty());
    getObjectProperty().get("storeType").bind(storeType.getSelectionModel().selectedItemProperty());
    getObjectProperty().get("objectType").bind(storeObjectType.getSelectionModel().selectedItemProperty());
    getObjectProperty().get("controllOut").bind(creditControll.selectedProperty());
  }
  
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    FXUtility.initCss(this);
    
    storeType.getItems().addAll(Store.StoreType.НАЛИЧНЫЙ, Store.StoreType.БЕЗНАЛИЧНЫЙ);
    storeObjectType.getItems().addAll(Group.ObjectType.ТМЦ, Group.ObjectType.ВАЛЮТА);
    
    initData();
    
    parentStore.textProperty().bind(Bindings.createStringBinding(() -> getObjectProperty().isNull("parent") ? "Родительская группа..." : ("Родительская группа: "+getObjectProperty().getMap("parent").getString("name")), 
            getObjectProperty().get("parent")));
    //parentStore.setOnMouseClicked(e -> selectParent());
    
    currencyLabel.textProperty().bind(Bindings.createStringBinding(() -> getObjectProperty().getValue("currency") == null ? "Выберите валюту" : (String)((PropertyMap)getObjectProperty().getValue("currency")).getValue("name"), 
            getObjectProperty().get("currency")));
    currencyLabel.visibleProperty().bind(Bindings.createBooleanBinding(() -> getObjectProperty().getValue("objectType") != null && getObjectProperty().getValue("objectType").equals(Group.ObjectType.ВАЛЮТА), 
            getObjectProperty().get("objectType")));
    currencyLabel.setOnMouseClicked(e -> selectCurrency());
  }

  private void selectCurrency() {
    FXTreeEditor groupTable = new FXTreeEditor(Group.class);
    groupTable.getClientFilter().AND_EQUAL("groupType", Group.ObjectType.ВАЛЮТА);
    groupTable.setDoubleClickActionType(FXEditor.DoubleClickActionType.SELECT);
    groupTable.getTree().getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    
    List<PropertyMap> objects = groupTable.getObjects(getRoot(), "Выберите валюту", (FXD.ButtonType type) -> {
      if(type == FXD.ButtonType.OK) {
        if(groupTable.getTree().getSelectionModel().getSelectedItem() != null && ((TreeItem)groupTable.getTree().getSelectionModel().getSelectedItem()).getChildren().isEmpty())
          return true;
        else {
          new Alert(Alert.AlertType.WARNING, "Выберите валюту", ButtonType.CLOSE).showAndWait();
          return false;
        }
      }
      return true;
    });

    if(!objects.isEmpty())
      getObjectProperty().setValue("currency", objects.get(0));
  }
}