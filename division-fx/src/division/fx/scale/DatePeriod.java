package division.fx.scale;

import division.fx.PropertyMap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import javafx.beans.property.*;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.*;
import javafx.scene.paint.*;

public class DatePeriod extends Pane implements Comparable<DatePeriod>, PropertyMap {
  public final ObjectProperty<LocalDate> startDate = new SimpleObjectProperty();
  public final ObjectProperty<LocalDate> endDate   = new SimpleObjectProperty();
  
  private final BooleanProperty selected    = new SimpleBooleanProperty(false);
  private Pane toolTipPane = null;
  
  private Runnable finalize;
  private ObservableList<String> equalKeys = FXCollections.observableArrayList();
  private ObservableList<String> notEqualKeys = FXCollections.observableArrayList();
  
  private ObjectProperty<LocalDateTime> lastupdate = new SimpleObjectProperty<>(LocalDateTime.now());
  
  @Override
  public ObservableList<String> equalKeys() {
    return equalKeys;
  }
  
  @Override
  public ObservableList<String> notEqualKeys() {
    return notEqualKeys;
  }
  
  public DatePeriod() {
    this(LocalDate.now(), LocalDate.now());
  }

  public DatePeriod(LocalDate start, LocalDate end) {
    startDate.set(start);
    endDate.set(end);
    
    getStyleClass().add("date-period");
    selected.addListener((ob, ol, nw) -> {
      if(nw)
        getStyleClass().add("selected");
      else getStyleClass().remove("selected");
    });
  }
  
  public void setColor(Paint color) {
    setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
  }

  public ObjectProperty<LocalDate> startDateProperty() {
    return startDate;
  }

  public ObjectProperty<LocalDate> endDateProperty() {
    return endDate;
  }

  public BooleanProperty selectedProperty() {
    return selected;
  }

  public Pane getToolTipPane() {
    return toolTipPane;
  }

  public void setToolTipPane(Pane toolTipPane) {
    this.toolTipPane = toolTipPane;
    this.toolTipPane.getStyleClass().add("toolTipPane");
  }

  public Runnable getFinalizeTask() {
    return finalize;
  }

  public void setFinalizeTask(Runnable finalize) {
    this.finalize = finalize;
  }
  
  public void dispose() {
    unbindAll();
    selected.unbind();
    startDate.unbind();
    endDate.unbind();
    if(finalize != null)
      finalize.run();
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 59 * hash + Objects.hashCode(this.startDate);
    hash = 59 * hash + Objects.hashCode(this.endDate);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final DatePeriod other = (DatePeriod) obj;
    if (!Objects.equals(this.startDate, other.startDate)) {
      return false;
    }
    if (!Objects.equals(this.endDate, other.endDate)) {
      return false;
    }
    return true;
  }

  @Override
  public int compareTo(DatePeriod o) {
    if(equals(o))
      return 0;
    else return this.startDate.getValue().isAfter(o.startDate.getValue()) ? -1 : 1;
  }
  
  private final Map<String, Property> map = new TreeMap();

  
  @Override
  public Map<String, Property> getMap() {
    return map;
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