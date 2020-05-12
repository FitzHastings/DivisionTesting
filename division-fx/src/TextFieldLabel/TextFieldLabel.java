package TextFieldLabel;

import division.fx.DivisionTextField;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.util.StringConverter;

public class TextFieldLabel<T> extends HBox {
  private final Label name  = new Label();
  private final Label value = new Label();
  private final StringProperty    nameTextProperty = new SimpleStringProperty("Наименование");
  private final StringProperty    promtTextProperty = new SimpleStringProperty("Выберите заначение...");
  private final ObjectProperty<T> valueProperty     = new SimpleObjectProperty<>();
  private final List<EventHandler> commitListener = new ArrayList<>();
  
  private BooleanProperty editProperty = new SimpleBooleanProperty(false);
  
  private DivisionTextField<T> field = new DivisionTextField();

  public TextFieldLabel() {
    this("Наименование", null, null);
  }
  
  public TextFieldLabel(String name) {
    this(name, null, null);
  }
  
  public TextFieldLabel(String nameText, T valueObject, StringConverter<T> converter) {
    field.setConverter(converter);
    
    getStyleClass().add("text-label-box");
    name.getStyleClass().add("text-label-name");
    value.getStyleClass().add("text-label-value");
    field.getStyleClass().add("text-label-field");
    
    name.minWidthProperty().bind(Bindings.createDoubleBinding(() -> {
      Text t = new Text(name.getText());
      t.setFont(name.getFont());
      return t.getLayoutBounds().getWidth()+10;
    }, name.textProperty()));
    
    nameTextProperty().setValue(nameText);
    
    if(!nameVisibleProperty().getValue())
      getChildren().remove(name);
    
    name.textProperty().bind(Bindings.createStringBinding(() -> nameTextProperty.getValue()+(valueProperty.getValue() == null || valueProperty.getValue().equals("") ? "..." : ":"), nameTextProperty(), valueProperty()));
    
    value.textProperty().bind(Bindings.createStringBinding(() -> {
      return valueProperty.getValue() == null || valueProperty.getValue().equals("") ? promtTextProperty.getValue() : valueProperty.getValue().toString();
    }, valueProperty, promtTextProperty));
    
    Tooltip toolTip = new Tooltip();
    toolTip.textProperty().bind(value.textProperty());
    Tooltip.install(this, toolTip);
    
    
    field.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> {
      Text t = new Text(field.getText());
      t.fontProperty().bind(field.fontProperty());
      return t.getLayoutBounds().getWidth()+50;
    }, field.textProperty()));
    
    nameVisibleProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(!newValue)
        getChildren().remove(name);
    });
    
    field.setValue(valueObject);
    
    //commit();
    getChildren().setAll(name, value);
    valueProperty().setValue((T) field.getValue());
    //commitListener().stream().forEach(e -> e.handle(new ActionEvent(this, null)));
    
    value.setOnMouseClicked(e -> startEdit());
    field.setOnAction(ev -> commit());
    field.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(!newValue)
        commit();
    });
    
    editProperty.bind(Bindings.createBooleanBinding(() -> getChildren().contains(field), getChildren()));
  }
  
  public ReadOnlyBooleanProperty editProperty() {
    return editProperty;
  }
  
  public List<EventHandler> commitListener() {
    return commitListener;
  }

  public DivisionTextField<T> getField() {
    return field;
  }
  
  public BooleanProperty nameVisibleProperty() {
    return name.visibleProperty();
  }
  
  public void setConverter(StringConverter converter) {
    field.setConverter(converter);
  }
  
  public StringProperty nameTextProperty() {
    return nameTextProperty;
  }

  public StringProperty promtTextProperty() {
    return promtTextProperty;
  }

  public ObjectProperty<T> valueProperty() {
    return valueProperty;
  }
  
  public void commit() {
    //new Exception().printStackTrace();
    if(editProperty().getValue()) {
      getChildren().setAll(name, value);
      if(!nameVisibleProperty().getValue())
        getChildren().remove(name);
      valueProperty().setValue((T) field.getValue());
      commitListener().stream().forEach(e -> e.handle(new ActionEvent(this, null)));
    }
  }
  
  public void cancelEdit() {
    getChildren().setAll(name, value);
    if(!nameVisibleProperty().getValue())
      getChildren().remove(name);
  }

  public void startEdit() {
    getChildren().setAll(name,field);
    field.setValue(valueProperty().getValue());
    field.selectAll();
    field.requestFocus();
  }
}
