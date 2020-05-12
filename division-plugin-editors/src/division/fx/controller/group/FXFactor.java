package division.fx.controller.group;

import TextFieldLabel.TextFieldLabel;
import bum.interfaces.Factor;
import bum.interfaces.Unit;
import client.util.ObjectLoader;
import division.fx.ChoiseLabel.ChoiceLabel;
import division.fx.DivisionTextField;
import division.fx.FXToolButton;
import division.fx.FXUtility;
import division.fx.PropertyMap;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.controller.unit.FXUnit;
import division.fx.editor.FXObjectEditor;
import division.fx.editor.FXTableEditor;
import division.fx.table.Column;
import division.fx.table.FXDivisionTable;
import division.fx.util.MsgTrash;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class FXFactor extends FXObjectEditor {
  private final DivisionTextField<String> nameField = new DivisionTextField<>();
  private final FXTableEditor            unitEditor = new FXTableEditor(Unit.class, FXUnit.class, Column.create("Наименование","name"), Column.create("Целое","intval"), Column.create("Комментарии","comments"));
  
  private final CheckBox valuesCheckBox = new CheckBox("Список значений");
  private final TitleBorderPane                valuesBox   = new TitleBorderPane(valuesCheckBox);
  private final FXToolButton                   addValue    = new FXToolButton(e -> addValue(), "Добавить значение", "add-button");
  private final FXToolButton                   delValue    = new FXToolButton(e -> delValue(), "Удалить значение", "remove-button");
  private final ToolBar                        tools       = new ToolBar(addValue,delValue);
  private final FXDivisionTable<PropertyMap>   valuesTable = new FXDivisionTable(Column.create("Значение"));
  private final ChoiceLabel<PropertyMap>       unitField   = unitEditor.getChoiceLabel("Ед.измерения","", getRoot());
  private final ChoiceLabel<Factor.FactorType> type        = new ChoiceLabel<>("Тип значения");
  
  private final VBox root = new VBox(5, nameField, unitField, type, valuesBox);

  public FXFactor() {
    FXUtility.initMainCss(this);
    getRoot().setCenter(root);
    type.itemsProperty().getValue().setAll(Factor.FactorType.values());
  }

  @Override
  public void initData() {
    valuesBox.setCenter(new BorderPane(valuesTable, tools, null, null, null));
    
    valuesTable.getColumn("Значение").setEditable(true);
    valuesTable.setEditable(true);
    
    storeControls().add(valuesTable);
    
    HBox.setHgrow(valuesBox, Priority.ALWAYS);
    
    nameField.setPromptText("Наименование...");
    nameField.setValue(getObjectProperty().getString("name"));
    
    valuesBox.getCenter().disableProperty().bind(valuesCheckBox.selectedProperty().not());
    valuesCheckBox.setSelected(!getObjectProperty().isNullOrEmpty("listValues"));
    
    valuesTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    
    try {
      if(getObjectProperty().isNotNull("unit")) {
        getObjectProperty().setValue("unit", ObjectLoader.getMap(Unit.class, getObjectProperty().getInteger("unit"), unitEditor.getFieldsAndOther()));
        unitField.valueProperty().setValue(getObjectProperty().getMap("unit"));
      }
    }catch(Exception ex) {
      MsgTrash.out(ex);
    }
    
    if(getObjectProperty().isNotNull("factorType"))
      type.valueProperty().setValue(getObjectProperty().getValue("factorType", Factor.FactorType.class));
    
    valuesTable.getSourceItems().addListener((ListChangeListener.Change<? extends PropertyMap> c) -> {
      getObjectProperty().get("listValues").setValue(!valuesCheckBox.isSelected() ? null : valuesCheckBox.isSelected() && !valuesTable.getSourceItems().isEmpty() ?
              String.join(";", valuesTable.getSourceItems().stream().filter(r -> !r.isNullOrEmpty("Значение")).map(r -> r.getString("Значение")).collect(Collectors.toList())) : null);
      
      valuesTable.getSourceItems().stream().map(r -> r.get("Значение")).forEach(z -> {
        z.addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
          getObjectProperty().get("listValues").setValue(!valuesCheckBox.isSelected() ? null : valuesCheckBox.isSelected() && !valuesTable.getSourceItems().isEmpty() ?
                  String.join(";", valuesTable.getSourceItems().stream().filter(r -> !r.isNullOrEmpty("Значение")).map(r -> r.getString("Значение")).collect(Collectors.toList())) : null);
        });
      });
    });
    
    valuesCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> 
            getObjectProperty().get("listValues").setValue(!valuesCheckBox.isSelected() ? null : valuesCheckBox.isSelected() && !valuesTable.getSourceItems().isEmpty() ?
                    String.join(";", valuesTable.getSourceItems().stream().filter(r -> !r.isNullOrEmpty("Значение")).map(r -> r.getString("Значение")).collect(Collectors.toList())) : null));
    
    if(!getObjectProperty().isNullOrEmpty("listValues"))
      valuesTable.getSourceItems().setAll(Arrays.stream(getObjectProperty().getString("listValues").split(";")).map(v -> PropertyMap.create().setValue("Значение", v)).collect(Collectors.toList()));
    
    getObjectProperty().get("factorType").bind(type.valueProperty());
    getObjectProperty().get("unit").bind(unitField.valueProperty());
    getObjectProperty().get("name").bind(nameField.valueProperty());
    
    delValue.disableProperty().bind(valuesTable.getSelectionModel().selectedItemProperty().isNull());
  }

  @Override
  public boolean isUpdate() {
    return super.isUpdate();
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
  }

  private void addValue() {
    valuesTable.getSourceItems().add(PropertyMap.create());
  }

  private void delValue() {
    valuesTable.getSourceItems().removeAll(valuesTable.getSelectionModel().getSelectedItems());
  }
}