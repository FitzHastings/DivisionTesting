package division.fx.table;

import division.fx.PropertyMap;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class FXMap implements PropertyMap {
  private final Map<String, Property> map = new TreeMap();
  private final ObservableList<String> equalKeys = FXCollections.observableArrayList();
  private final ObservableList<String> notEqualKeys = FXCollections.observableArrayList();
  private final ObjectProperty<LocalDateTime> lastupdate = new SimpleObjectProperty<>(LocalDateTime.now());

  @Override
  public ObservableList<String> equalKeys() {
    return equalKeys;
  }
  
  @Override
  public Map<String, Property> getMap() {
    return map;
  }

  @Override
  public ObservableList<String> notEqualKeys() {
    return notEqualKeys;
  }

  @Override
  public ObjectProperty<LocalDateTime> lastupdate() {
    return lastupdate;
  }
  
  @Override
  public BooleanProperty printDifferenceProperty() {
    return null;
  }
}