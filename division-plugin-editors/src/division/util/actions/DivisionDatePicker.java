package division.util.actions;

import java.time.LocalDate;
import javafx.scene.control.DatePicker;

public class DivisionDatePicker extends DatePicker {

  public DivisionDatePicker() {
  }

  public DivisionDatePicker(LocalDate localDate) {
    super(localDate);
  }
}