package division.fx.table.filter;

import division.fx.DivisionTextField;
import division.fx.PropertyMap;
import division.util.DivisionTask;
import java.util.function.Predicate;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class TextFilter extends AbstractColumnFilter {
  private final VBox content = new VBox(5);
  private final DivisionTextField textField;
  
  private final ToggleGroup group        = new ToggleGroup();
  private final RadioButton equal        = new RadioButton("равно");
  private final RadioButton notEqual     = new RadioButton("не равно");
  private final RadioButton substring    = new RadioButton("включает подстроку");
  private final RadioButton notSubstring = new RadioButton("исключает подстроку");
  private final RadioButton startWith    = new RadioButton("совпадение с начала строки");
  private final RadioButton endWith      = new RadioButton("совпадение с конца строки");
  private final CheckBox    caseable     = new CheckBox("Учитывать регистр");
  
  public TextFilter() {
    this(null);
  }
  
  public TextFilter(String property) {
    this(property, null);
  }

  public TextFilter(String property, StringConverter converter) {
    super(property);
    textField = new DivisionTextField(converter);
    
    group.getToggles().addAll(equal, notEqual, substring, notSubstring, startWith, endWith);
    content.getChildren().addAll(textField, equal, notEqual, substring, notSubstring, startWith, endWith, caseable);
    
    substring.setSelected(true);
    content.setPadding(new Insets(10));
    
    activeProperty().bind(textField.textProperty().isNotEqualTo(""));
  }
  
  @Override
  public void removeListener(Object listener) {
    textField.textProperty().removeListener((ChangeListener)listener);
    equal.selectedProperty().removeListener((ChangeListener)listener);
    notEqual.selectedProperty().removeListener((ChangeListener)listener);
    substring.selectedProperty().removeListener((ChangeListener)listener);
    notSubstring.selectedProperty().removeListener((ChangeListener)listener);
    startWith.selectedProperty().removeListener((ChangeListener)listener);
    endWith.selectedProperty().removeListener((ChangeListener)listener);
    caseable.selectedProperty().removeListener((ChangeListener)listener);
  }
  
  @Override
  public void addFilterListener(FilterListener handler) {
    ChangeListener listener = (ChangeListener) (ObservableValue observable, Object oldValue, Object newValue) -> {
      DivisionTask.start(() -> {
        try {
          Thread.sleep(50);
        }catch(InterruptedException ex) {}
        handler.handle(new Event(TextFilter.this, null, EventType.ROOT));
      });
    };
    putListener(handler, listener);
    textField.textProperty().addListener(listener);
    equal.selectedProperty().addListener(listener);
    notEqual.selectedProperty().addListener(listener);
    substring.selectedProperty().addListener(listener);
    notSubstring.selectedProperty().addListener(listener);
    startWith.selectedProperty().addListener(listener);
    endWith.selectedProperty().addListener(listener);
    caseable.selectedProperty().addListener(listener);
  }
  
  @Override
  public Predicate<PropertyMap> getPredicate() {
    return (PropertyMap t) -> {
      String object = t.getValue(getProperty()) == null ? "" : String.valueOf(t.getValue(getProperty()));
      String value = textField.getText();
      if(value.equals(""))
        return true;
      else {
        if(!caseable.isSelected()) {
          value = value.toLowerCase();
          object = object.toLowerCase();
        }
        if(equal.isSelected())
          return value.equals(object);
        if(notEqual.isSelected())
          return !value.equals(object);
        if(substring.isSelected())
          return object.contains(value);
        if(notSubstring.isSelected())
          return !object.contains(value);
        if(startWith.isSelected())
          return object.startsWith(value);
        if(endWith.isSelected())
          return object.endsWith(value);
        return false;
      }
    };
  }
  
  @Override
  public Node getContent() {
    return content;
  }

  @Override
  public void initFilter() {
  }
}