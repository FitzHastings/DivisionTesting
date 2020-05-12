package division.fx.controller.process;

import bum.editors.util.ObjectLoader;
import bum.interfaces.Factor;
import bum.interfaces.Product;
import bum.interfaces.Service;
import division.fx.DivisionTextField;
import division.fx.PropertyMap;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.editor.FXObjectEditor;
import division.fx.editor.FXTableEditor;
import division.fx.table.Column;
import division.fx.table.FXDivisionTable;
import division.util.Utility;
import java.net.URL;
import java.time.Period;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.converter.IntegerStringConverter;

public class FXProcessProduct extends FXObjectEditor {
  @FXML private TitleBorderPane   objectTitleBorder;
  @FXML private DivisionTextField duration;
  @FXML private DivisionTextField reccurence;
  @FXML private VBox              periodPanel;
  @FXML private TextField         processName;
  @FXML private CheckBox          withChangeOwnerCheckBox;
  @FXML private RadioButton       sellerCheckBox;
  @FXML private RadioButton       customerCheckBox;
  @FXML private RadioButton       myselfCheckbox;
  @FXML private ChoiceBox<String> durationType;
  @FXML private ChoiceBox<String> reccurenceType;
  @FXML private TitleBorderPane   factorPanel;
  
  private final Button  addFactor    = new Button();
  private final Button  editFactor   = new Button();
  private final Button  removeFactor = new Button();
  
  private final ToolBar tools        = new ToolBar(addFactor, editFactor, removeFactor);
  private final FXDivisionTable<PropertyMap> processFactors = new FXDivisionTable(
          Column.create("Наименование", "name"),
          Column.create("Тип", "factorType"),
          Column.create("Ед.изм.", "unit"));
  
   private final FXTableEditor     factors = new FXTableEditor(Factor.class, null, FXCollections.observableArrayList("id"), 
          Column.create("Наименование", "name"),
          Column.create("Тип", "factorType"),
          Column.create("Ед.изм.", "unit"));

  @Override
  public void initData() {
    getObjectProperty().setValue("factors", 
            getObjectProperty().getValue("factors") == null ? FXCollections.observableArrayList() : 
                    ObjectLoader.getList(Factor.class, getObjectProperty().getValue("factors", Integer[].class), 
                            "id","name","factorType","unit"));
    setMementoProperty(getObjectProperty().copy());
    processFactors.itemsProperty().bind(Bindings.createObjectBinding(() -> getObjectProperty().getList("factors", PropertyMap.class), getObjectProperty().getList("factors", PropertyMap.class)));
    
    processName.setText((String)getObjectProperty().getValue("name"));
    objectTitleBorder.setDisable(getObjectProperty().getValue("childs") != null && ((Integer[])getObjectProperty().getValue("childs")).length > 0);
    
    titleProperty().bind(Bindings.createStringBinding(() -> 
              (getObjectProperty().getValue("childs") != null && ((Integer[])getObjectProperty().getValue("childs")).length > 0 ? "РАЗДЕЛ: " : 
                      myselfCheckbox.isSelected() ? "ПРОДУКТ: " : "ПРОЦЕСС: ")+processName.getText(), myselfCheckbox.selectedProperty(), processName.textProperty()));
    
    if(getObjectProperty().getValue("childs") == null || ((Integer[])getObjectProperty().getValue("childs")).length == 0) {
      sellerCheckBox.setSelected(Service.Owner.SELLER.toString().equals(getObjectProperty().getValue("owner")));
      customerCheckBox.setSelected(Service.Owner.CUSTOMER.toString().equals(getObjectProperty().getValue("owner")));
      withChangeOwnerCheckBox.setSelected(getObjectProperty().getValue("moveStorePosition", false));
      myselfCheckbox.setSelected(getObjectProperty().getValue("product", false));

      if(getObjectProperty().getValue("duration") != null) {
        Period dur = Utility.convert((String)getObjectProperty().getValue("duration"));
        duration.setValue(dur.getDays() > 0 ? dur.getDays() : dur.getMonths() > 0 ? dur.getMonths() : dur.getYears() > 0 ? dur.getMonths() : 0);
        durationType.getSelectionModel().select(dur.getDays() > 0 ? 0 : dur.getMonths() > 0 ? 1 : dur.getYears() > 0 ? 2 : -1);
      }

      if(getObjectProperty().getValue("recurrence") != null) {
        Period rec = Utility.convert((String)getObjectProperty().getValue("recurrence"));
        reccurence.setValue(rec.getDays() > 0 ? rec.getDays() : rec.getMonths() > 0 ? rec.getMonths() : rec.getYears() > 0 ? rec.getMonths() : 0);
        reccurenceType.getSelectionModel().select(rec.getDays() > 0 ? 0 : rec.getMonths() > 0 ? 1 : rec.getYears() > 0 ? 2 : -1);
      }

      getObjectProperty().get("name").bind(processName.textProperty());

      getObjectProperty().get("owner").bind(Bindings.createStringBinding(() -> sellerCheckBox.isSelected() ? "SELLER" : "CUSTOMER", 
              sellerCheckBox.selectedProperty(), customerCheckBox.selectedProperty()));

      getObjectProperty().get("product").bind(myselfCheckbox.selectedProperty());

      getObjectProperty().get("moveStorePosition").bind(withChangeOwnerCheckBox.selectedProperty().and(sellerCheckBox.selectedProperty()));

      getObjectProperty().get("duration").bind(Bindings.createStringBinding(() -> 
              periodPanel.isDisable() || duration.getValue() == null ? null : Utility.convert(duration.getValue()+" "+durationType.getSelectionModel().getSelectedItem()).toString(), 
      duration.disableProperty(), duration.textProperty(), durationType.getSelectionModel().selectedItemProperty()));

      getObjectProperty().get("recurrence").bind(Bindings.createStringBinding(() -> 
              periodPanel.isDisable() || reccurence.getValue() == null ? null : Utility.convert(reccurence.getValue()+" "+reccurenceType.getSelectionModel().getSelectedItem()).toString(), 
      reccurence.disableProperty(), reccurence.textProperty(), reccurenceType.getSelectionModel().selectedItemProperty()));

      if(!sellerCheckBox.isSelected() && !customerCheckBox.isSelected() && !myselfCheckbox.isSelected())
        sellerCheckBox.setSelected(true);
    }
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    factorPanel.disableProperty().bind(myselfCheckbox.selectedProperty().not());
    periodPanel.disableProperty().bind(myselfCheckbox.selectedProperty().not());
    withChangeOwnerCheckBox.disableProperty().bind(sellerCheckBox.selectedProperty().not());
    
    factorPanel.setCenter(new BorderPane(processFactors, tools, null, null, null));
    storeControls().add(processFactors);
    
    factors.setSelectionMode(SelectionMode.MULTIPLE);
    processFactors.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    
    new ToggleGroup().getToggles().addAll(sellerCheckBox, customerCheckBox, myselfCheckbox);
    
    addFactor.getStyleClass().add("addToolButton");
    editFactor.getStyleClass().add("editToolButton");
    removeFactor.getStyleClass().add("removeToolButton");
    
    addFactor.setOnAction(e -> {
      List<PropertyMap> fs = factors.getObjects(getRoot(), "Выберите реквизиты для добавления в процесс");
      if(fs != null)
        getObjectProperty().getList("factors", PropertyMap.class).addAll(fs);
    });
    
    removeFactor.setOnAction(e -> {
      if(!processFactors.getSelectionModel().getSelectedItems().isEmpty())
        getObjectProperty().getList("factors", PropertyMap.class).removeAll(processFactors.getSelectionModel().getSelectedItems());
    });
    
    duration.setConverter(new IntegerStringConverter());
    reccurence.setConverter(new IntegerStringConverter());
    
    duration.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
      int index = durationType.getSelectionModel().getSelectedIndex();
      if(duration.getValue() != null && !duration.getValue().equals(0))
        durationType.getItems().setAll(getType((Integer)duration.getValue()));
      else durationType.getItems().clear();
      durationType.getSelectionModel().select(index == -1 ? 0 : index);
    });
    
    reccurence.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
      int index = reccurenceType.getSelectionModel().getSelectedIndex();
      if(reccurence.getValue() != null && !reccurence.getValue().equals(0))
        reccurenceType.getItems().setAll(getType((Integer)reccurence.getValue()));
      else reccurenceType.getItems().clear();
      reccurenceType.getSelectionModel().select(index == -1 ? 0 : index);
    });
    
    initData();
  }

  @Override
  public boolean save() {
    boolean is = super.save();
    if(getObjectProperty().getValue("product", false) && !getMementoProperty().getValue("product", false)) {
      Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Добавить процесс-продукт в каталог?", ButtonType.YES, ButtonType.NO);
      if(a.showAndWait().get() == ButtonType.YES) {
        ObjectLoader.createObject(Product.class, PropertyMap.create()
                .setValue("service", getObjectProperty().getValue("id"))
                .setValue("recurrence", getObjectProperty().getValue("recurrence"))
                .setValue("duration", getObjectProperty().getValue("duration"))
                .setValue("factors", getObjectProperty().getValue("factors")));
      }
    }//else ObjectLoader.sa
    return is;
  }
  
  private ObservableList<String> getType(Integer count) {
    if(count == null) return FXCollections.observableArrayList();
    if(count%10 == 0 || count%10 >= 5 && count%10 <= 9 || count%100 >= 11 && count%100 <= 14)
      return FXCollections.observableArrayList("дней","месяцев","лет");
    else if(count%10 == 1) return FXCollections.observableArrayList("день","месяц","год");
    else return FXCollections.observableArrayList("дня","месяца","года");
  }
}