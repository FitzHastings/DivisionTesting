package division.fx.client.plugins;

import division.fx.table.FXMap;
import javafx.beans.value.*;

public class PaymentPojo extends FXMap {
  /*private ObjectProperty<BigDecimal> amount    = new SimpleObjectProperty();
  private ObjectProperty<BigDecimal> debet    = new SimpleObjectProperty();
  private ObjectProperty<BigDecimal> credit   = new SimpleObjectProperty();
  
  private ObjectProperty<Date>       date     = new SimpleObjectProperty();
  
  private StringProperty             document = new SimpleStringProperty();
  
  private StringProperty             customer = new SimpleStringProperty();
  private StringProperty             store    = new SimpleStringProperty();
  
  
  private BooleanProperty toBank       = new SimpleBooleanProperty();
  private BooleanProperty toClientBank = new SimpleBooleanProperty();
  private BooleanProperty finish       = new SimpleBooleanProperty();*/

  public PaymentPojo() {
    
    get("Ok").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
      if((Boolean)newValue) {
        setValue("?", false);
        setValue("$", false);
      }
    });
    
    get("$").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
      if((Boolean)newValue) {
        setValue("?", false);
        setValue("Ok", false);
      }
    });
    
    get("?").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
      if((Boolean)newValue) {
        setValue("$", false);
        setValue("Ok", false);
      }
    });
    
    /*finish.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(newValue) {
        toBank.set(false);
        toClientBank.set(false);
      }
    });
    
    toBank.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(newValue) {
        finish.set(false);
        toClientBank.set(false);
      }
    });
    
    toClientBank.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(newValue) {
        finish.set(false);
        toBank.set(false);
      }
    });*/
  }

  /*public ObjectProperty<Date> dateProperty() {
    return date;
  }

  public StringProperty documentProperty() {
    return document;
  }

  public StringProperty customerProperty() {
    return customer;
  }
  
  public StringProperty storeProperty() {
    return store;
  }

  public ObjectProperty<BigDecimal> amountProperty() {
    return amount;
  }

  public ObjectProperty<BigDecimal> debetProperty() {
    return debet;
  }
  
  public ObjectProperty<BigDecimal> creditProperty() {
    return credit;
  }

  public BooleanProperty toBankProperty() {
    return toBank;
  }

  public BooleanProperty toClientBankProperty() {
    return toClientBank;
  }

  public BooleanProperty finishProperty() {
    return finish;
  }*/
}