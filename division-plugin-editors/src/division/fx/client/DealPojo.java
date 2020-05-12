package division.fx.client;

import division.fx.table.Pojo;
import java.util.Date;
import javafx.beans.property.*;

public class DealPojo extends Pojo {
  private final ObjectProperty<Date> dealStartDate = new SimpleObjectProperty<>();
  private final ObjectProperty<Date> dealEndDate   = new SimpleObjectProperty<>();

  public ObjectProperty<Date> dealStartDateProperty() {
    return dealStartDate;
  }

  public ObjectProperty<Date> dealEndDateProperty() {
    return dealEndDate;
  }
}