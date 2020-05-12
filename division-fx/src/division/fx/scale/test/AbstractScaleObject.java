package division.fx.scale.test;

import division.fx.PropertyMap;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

public abstract class AbstractScaleObject extends Pane implements PropertyMap {
  //private final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);
  
  private final ObservableList<String> notEqualKeys = FXCollections.observableArrayList();
  private final ObservableList<String> equalKeys = FXCollections.observableArrayList();
  private final Map<String, Property> map = new TreeMap<>();
  private final BooleanProperty printDifferenceProperty = new SimpleBooleanProperty(false);
  private final ObjectProperty<LocalDateTime> lastUpdate = new SimpleObjectProperty<>(LocalDateTime.now());

  public AbstractScaleObject() {
    addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
      System.out.println("FOCUSED ADD"+getClass().getSimpleName());
      pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true);
    });
    addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
      System.out.println("FOCUSED EXT"+getClass().getSimpleName());
      pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), false);
    });
  }
  
  @Override
  public ObjectProperty<LocalDateTime> lastupdate() {
    return lastUpdate;
  }

  @Override
  public BooleanProperty printDifferenceProperty() {
    return printDifferenceProperty;
  }

  @Override
  public Map<String, Property> getMap() {
    return map;
  }

  @Override
  public ObservableList<String> equalKeys() {
    return equalKeys;
  }

  @Override
  public ObservableList<String> notEqualKeys() {
    return notEqualKeys;
  }
}