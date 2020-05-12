package TextFieldLabel;

import division.fx.DivisionTextField;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.util.StringConverter;

public class NodeLabel<T> extends HBox {
  private final Label name  = new Label();
  private final Label value = new Label();
  private final StringProperty    nameTextProperty = new SimpleStringProperty("Наименование");
  private final StringProperty    promtTextProperty = new SimpleStringProperty("Выберите заначение...");
  private final ObjectProperty<T> valueProperty     = new SimpleObjectProperty<>();
  
  private DivisionTextField field = new DivisionTextField();
  
  public NodeLabel(String name) {
    this(name, null, null);
  }
  
  public NodeLabel(String nameText, T valueObject, StringConverter<T> converter) {
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
    getChildren().addAll(name, value);
    
    name.textProperty().bind(Bindings.createStringBinding(() -> nameTextProperty.getValue()+(valueProperty.getValue() == null || valueProperty.getValue().equals("") ? "..." : ":"), nameTextProperty(), valueProperty()));
    
    value.textProperty().bind(Bindings.createStringBinding(() -> {
      return valueProperty.getValue() == null || valueProperty.getValue().equals("") ? promtTextProperty.getValue() : valueProperty.getValue().toString();
    }, valueProperty, promtTextProperty));
    
    Tooltip toolTip = new Tooltip();
    toolTip.textProperty().bind(value.textProperty());
    Tooltip.install(this, toolTip);
    
    
    field.prefWidthProperty().bind(name.widthProperty().add(value.widthProperty()));
    
    field.setValue(valueObject);
    field.setOnAction(ev -> getChildren().setAll(name, value));
    
    valueProperty().bind(field.valueProperty());

    field.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(!newValue)
        getChildren().setAll(name, value);
    });
    
    value.setOnMouseClicked(e -> {
      getChildren().setAll(field);
      field.selectAll();
      field.requestFocus();
    });
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
}
