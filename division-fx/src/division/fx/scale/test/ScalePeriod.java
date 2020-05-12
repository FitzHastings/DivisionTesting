package division.fx.scale.test;

import java.time.LocalDate;
import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ScalePeriod extends AbstractScaleObject implements Comparable<ScalePeriod> {
  private final ObjectProperty<LocalDate>   startDateProperty    = new SimpleObjectProperty(LocalDate.now());
  private final ObjectProperty<LocalDate>   endDateProperty      = new SimpleObjectProperty(LocalDate.now());

  public ScalePeriod() {
    getStyleClass().add("scale-period");
  }

  public ObjectProperty<LocalDate> startDateProperty() {
    return startDateProperty;
  }

  public ObjectProperty<LocalDate> endDateProperty() {
    return endDateProperty;
  }
  
  @Override
  public boolean equals(Object obj) {
    return obj != null && 
            getClass() == obj.getClass() && 
            Objects.equals(startDateProperty.getValue(), ((ScalePeriod)obj).startDateProperty.getValue()) && 
            Objects.equals(endDateProperty.getValue(), ((ScalePeriod)obj).endDateProperty.getValue()) && 
            super.equals(obj);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 19 * hash + Objects.hashCode(this.startDateProperty);
    hash = 19 * hash + Objects.hashCode(this.endDateProperty);
    return hash;
  }

  @Override
  public int compareTo(ScalePeriod o) {
    return equals(o) ? 0 : this.startDateProperty.getValue().isAfter(o.startDateProperty.getValue()) ? -1 : 1;
  }
}