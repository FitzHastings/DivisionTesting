package division.fx;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;
import org.apache.commons.lang3.ArrayUtils;

public class PropertyMapTextField extends TextField {
  private final ObjectProperty<PropertyMap> valueProperty = new SimpleObjectProperty<>();

  public PropertyMapTextField(String promtText, String... keys) {
    setEditable(false);
    setFocusTraversable(false);
    setFocused(false);
    setPromptText(promtText);
    
    valueProperty.addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      if(newValue == null)
        setText("");
      else {
        String[] vals = new String[0];
        for(String k:keys)
          vals = ArrayUtils.add(vals, String.valueOf(newValue.getValue(k)));
        setText(String.join(", ", vals));
      }
    });
  }
  
  public ObjectProperty<PropertyMap> valueProperty() {
    return valueProperty;
  }
}