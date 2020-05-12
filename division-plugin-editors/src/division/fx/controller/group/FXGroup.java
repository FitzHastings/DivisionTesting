package division.fx.controller.group;

import bum.interfaces.Factor;
import bum.interfaces.Unit;
import client.util.ObjectLoader;
import division.fx.ChoiseLabel.ChoiceLabel;
import division.fx.DivisionTextField;
import division.fx.FXUtility;
import division.fx.PropertyMap;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.controller.unit.FXUnit;
import division.fx.editor.FXObjectEditor;
import division.fx.editor.FXTableEditor;
import division.fx.table.Column;
import division.fx.util.MsgTrash;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class FXGroup extends FXObjectEditor {
  private final DivisionTextField<String> nameField = new DivisionTextField<>();
  private final FXTableEditor unitEditor = new FXTableEditor(Unit.class, FXUnit.class, Column.create("Наименование","name"), Column.create("Целое","intval"), Column.create("Комментарии","comments"));
  private final ChoiceLabel<PropertyMap> unitField = unitEditor.getChoiceLabel("Ед.измерения","", getRoot());
  private final FXTableEditor  factors   = new FXTableEditor(Factor.class, FXFactor.class, 
          Column.create("Наименование","name"), 
          Column.create("Тип","factorType"), 
          Column.create("Ед. измерения","unit"), 
          Column.create("Значения","listValues"), 
          Column.create("Уникальность","unique"));
  
  private final TitleBorderPane factorBox = new TitleBorderPane(new Label("Реквизиты"));
  private final VBox            root      = new VBox(5, nameField, unitField, factorBox);

  public FXGroup() {
    FXUtility.initMainCss(this);
    getRoot().setCenter(root);
    nameField.setPromptText("Наименование...");
    factorBox.setCenter(factors);
    VBox.setVgrow(factorBox, Priority.ALWAYS);
    storeControls().add(factors);
  }

  @Override
  public void initData() {
    nameField.setValue(getObjectProperty().getString("name"));
    
    try {
      if(getObjectProperty().isNotNull("unit")) {
        getObjectProperty().setValue("unit", ObjectLoader.getMap(Unit.class, getObjectProperty().getInteger("unit"), unitEditor.getFieldsAndOther()));
        unitField.valueProperty().setValue(getObjectProperty().getMap("unit"));
      }
    }catch(Exception ex) {
      MsgTrash.out(ex);
    }
    
    factors.getClientFilter().clear().AND_IN("groups", new Integer[]{getObjectProperty().getInteger("id")});
    factors.initData();
    
    getObjectProperty().get("unit").bind(unitField.valueProperty());
    getObjectProperty().get("name").bind(nameField.valueProperty());
    setMementoProperty(getObjectProperty().copy());
  }
}