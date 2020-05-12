package division.fx.controller.payment;

import division.fx.table.Pojo;
import java.time.LocalDate;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DocumentRow extends Pojo {
  private final StringProperty          number = new SimpleStringProperty();
  private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>(LocalDate.now());
  private final boolean numberEditable;
  private final boolean dateEditable;
  
  public DocumentRow(Integer id, String name, String number, LocalDate date) {
    this(id, name, number, date, true, true);
  }

  public DocumentRow(Integer id, String name, String number, LocalDate date, boolean numberEditable, boolean dateEditable) {
    super(id, name);
    this.number.set(number);
    this.date.set(date);
    this.numberEditable = numberEditable;
    this.dateEditable = dateEditable;
  }

  public StringProperty numberProperty() {
    return number;
  }

  public ObjectProperty<LocalDate> dateProperty() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date.set(date);
  }

  public LocalDate getDate() {
    return date.get();
  }

  public void setNumber(String number) {
    this.number.set(number);
  }

  public String getNumber() {
    return number.get();
  }

  public boolean isNumberEditable() {
    return numberEditable;
  }

  public boolean isDateEditable() {
    return dateEditable;
  }
}