package division.fx.controller.payment;

import division.fx.table.Pojo;
import java.math.BigDecimal;
import java.time.LocalDate;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class DealRow extends Pojo {
  private final StringProperty             contractNumber = new SimpleStringProperty();
  private final StringProperty             processName    = new SimpleStringProperty();
  private final ObjectProperty<LocalDate>  startDate      = new SimpleObjectProperty<>();;
  private final ObjectProperty<LocalDate>  endDate        = new SimpleObjectProperty<>();
  private final ObjectProperty<BigDecimal> cost           = new SimpleObjectProperty<>();
  private final ObjectProperty<BigDecimal> debt           = new SimpleObjectProperty<>();
  
  private final ObjectProperty<BigDecimal> pay            = new SimpleObjectProperty<>();
  private final ObjectProperty<BigDecimal> paymentPay     = new SimpleObjectProperty<>();
  
  private final BooleanProperty            check          = new SimpleBooleanProperty();
  
  private final IntegerProperty payCount     = new SimpleIntegerProperty();
  private final IntegerProperty paymentCount = new SimpleIntegerProperty();

  public DealRow(
          Integer id, 
          String name, 
          String contractNumber, 
          String processName, 
          LocalDate startDate, 
          LocalDate endDate, 
          BigDecimal cost, 
          BigDecimal pay, 
          BigDecimal paymentPay, 
          Integer    payCount) {
    
    super(id,name);
    this.contractNumber.set(contractNumber);
    this.processName.set(processName);
    this.startDate.set(startDate);
    this.endDate.set(endDate);
    this.cost.set(cost);
    
    this.pay.set(pay);
    this.payCount.set(payCount);
    
    setPaymentPay(paymentPay);
    
    check.set(paymentPay.compareTo(BigDecimal.ZERO) > 0 || (payCount > 0 && getDebt().compareTo(BigDecimal.ZERO) == 0));
  }
  
  public Integer getPaymentCount() {
    return paymentCount.get();
  }
  
  public void setPaymentCount(Integer paymentCount) {
    this.paymentCount.set(paymentCount);
  }
  
  public IntegerProperty paymentCountProperty() {
    return paymentCount;
  }
  
  public Integer getPayCount() {
    return payCount.get();
  }
  
  public void setPayCount(Integer payCount) {
    this.payCount.set(payCount);
  }
  
  public IntegerProperty payCountProperty() {
    return payCount;
  }
  
  public boolean isEditable() {
    return getDebt().compareTo(BigDecimal.ZERO) > 0 || getPaymentCount() != 0 || getCost().compareTo(BigDecimal.ZERO) == 0;
  }

  public boolean isDisable() {
    return !isEditable();
  }

  public String getContractNumber() {
    return contractNumber.get();
  }

  public void setContractNumber(String contractNumber) {
    this.contractNumber.set(contractNumber);
  }

  public StringProperty contractNumberProperty() {
    return contractNumber;
  }

  public String getProcessName() {
    return processName.get();
  }

  public void setProcessName(String processName) {
    this.processName.set(processName);
  }

  public StringProperty processNameProperty() {
    return processName;
  }
  
  

  public LocalDate getStartDate() {
    return startDate.get();
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate.set(startDate);
  }

  public ObjectProperty startDateProperty() {
    return startDate;
  }
  
  

  public LocalDate getEndDate() {
    return endDate.get();
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate.set(endDate);
  }

  public ObjectProperty endDateProperty() {
    return endDate;
  }
  
  

  public BigDecimal getCost() {
    return cost.get();
  }

  public void setCost(BigDecimal cost) {
    this.cost.set(cost);
  }

  public ObjectProperty costProperty() {
    return cost;
  }
  
  

  public BigDecimal getDebt() {
    return debt.get();
  }
  
  public void setDebt(BigDecimal debt) {
    this.debt.set(debt);
  }
  
  public ObjectProperty debtProperty() {
    return debt;
  }
  
  
  
  public BigDecimal getPay() {
    return pay.get();
  }
  
  public void setPay(BigDecimal pay) {
    this.pay.set(pay);
    setDebt(getCost().subtract(getPay()));
  }
  
  public ObjectProperty payProperty() {
    return pay;
  }
  
  
  
  public BigDecimal getPaymentPay() {
    return paymentPay.get();
  }
  
  public void setPaymentPay(BigDecimal paymentPay) {
    setPay(pay.get().add(paymentPay.subtract(getPaymentPay()==null?BigDecimal.ZERO:getPaymentPay())));
    setPaymentCount(paymentPay == null || paymentPay.compareTo(BigDecimal.ZERO) == 0 ? 0 : 1);
    this.paymentPay.set(paymentPay);
  }

  public ObjectProperty<BigDecimal> paymentPayProperty() {
    return paymentPay;
  }
  
  

  public boolean getCheck() {
    return check.get();
  }

  public void setCheck(boolean check) {
    this.check.set(check);
  }

  public BooleanProperty checkProperty() {
    return check;
  }

  @Override
  public String toString() {
    return getId()+" "+getCheck();
  }
}