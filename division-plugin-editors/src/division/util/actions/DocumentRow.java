package division.util.actions;

import java.time.LocalDate;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DocumentRow {
  private final IntegerProperty             id         = new SimpleIntegerProperty();
  private final StringProperty              name       = new SimpleStringProperty();
  private final BooleanProperty             split      = new SimpleBooleanProperty();
  //private final BooleanProperty             startDeal  = new SimpleBooleanProperty();
  //private final BooleanProperty             endDeal    = new SimpleBooleanProperty();
  private final ObjectProperty<Object>      customDate = new SimpleObjectProperty<>();
  private final StringProperty              number     = new SimpleStringProperty();

  public DocumentRow(Integer id, String name, boolean split, /*boolean startDeal, boolean endDeal, */Object customDate, String number) {
    this.id.set(id);
    this.name.set(name);
    this.split.set(split);
    this.customDate.set(customDate);
    
    /*startDealProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(newValue)
        setEndDeal(false);
      setCustomDate(newValue ? null : LocalDate.now());
    });
    
    endDealProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(newValue)
        setStartDeal(false);
      setCustomDate(newValue ? null : LocalDate.now());
    });*/
    
    //this.startDeal.set(startDeal);
    //this.endDeal.set(endDeal);
    this.number.set(number);
  }

  public Integer getId() {
    return id.get();
  }

  public void setId(Integer id) {
    this.id.set(id);
  }

  public String getName() {
    return name.get();
  }

  public void setName(String name) {
    this.name.set(name);
  }

  public boolean getSplit() {
    return split.get();
  }

  public void setSplit(boolean split) {
    this.split.set(split);
  }

  public BooleanProperty splitProperty() {
    return split;
  }

  /*public boolean getStartDeal() {
    return startDeal.get();
  }

  public void setStartDeal(boolean startDeal) {
    this.startDeal.set(startDeal);
  }

  public BooleanProperty startDealProperty() {
    return startDeal;
  }

  public boolean getEndDeal() {
    return endDeal.get();
  }

  public void setEndDeal(boolean endDeal) {
    this.endDeal.set(endDeal);
  }

  public BooleanProperty endDealProperty() {
    return endDeal;
  }*/

  public Object getCustomDate() {
    return customDate.get();
  }

  public void setCustomDate(Object customDate) {
    this.customDate.set(customDate);
  }

  public ObjectProperty customDateProperty() {
    return customDate;
  }

  public String getNumber() {
    return number.get();
  }

  public void setNumber(String number) {
    this.number.set(number);
  }

  public StringProperty numberProperty() {
    return number;
  }
}