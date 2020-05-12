package division.fx;

import division.util.Utility;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import javafx.util.StringConverter;

public class DivisionTextField<T> extends TextField {
  private StringConverter converter;
  private ObjectProperty<T> value = new SimpleObjectProperty();

  public DivisionTextField() {
    this(null);
  }
  
  public DivisionTextField(StringConverter converter) {
    this(converter, (T)null);
  }

  public DivisionTextField(StringConverter converter, T value) {
    setConverter(converter == null ? Utility.getConverter(value) : converter);
    
    if(value != null)
      setValue(value);
    
    textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
      try {
        if(newValue != null && !newValue.equals("") && this.converter.fromString(newValue) == null)
          throw new Exception(newValue);
      }catch(Exception ex) {
        new Timeline(new KeyFrame(Duration.millis(300), e -> {
          new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(opacityProperty(), 1))).play();
        }, new KeyValue(opacityProperty(), 0))).play();
        setText(oldValue);
      }
    });
    
    this.value.bind(Bindings.createObjectBinding(() -> getValue(), textProperty()));
  }

  public void setConverter(StringConverter converter) {
    this.converter = converter != null ? converter : new StringConverter<Object>() {
      @Override
      public String toString(Object object) {
        return object == null ? "" : String.valueOf(object);
      }

      @Override
      public Object fromString(String string) {
        return string;
      }
    };
  }

  public StringConverter getConverter() {
    return converter;
  }
  
  public void setValue(T object) {
    textProperty().setValue(converter.toString(object));
  }
  
  public T getValue() {
    return (T) converter.fromString(getText());
  }

  public ObjectProperty<T> valueProperty() {
    return value;
  }
}
