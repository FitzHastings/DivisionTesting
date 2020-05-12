package bum.realizations;

import bum.annotations.Column;
import bum.annotations.ManyToOne;
import bum.annotations.Table;
import bum.interfaces.Deal;
import bum.interfaces.DealPayment;
import bum.interfaces.Payment;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

@Table
public class DealPaymentImpl extends MappingObjectImpl implements DealPayment {
  @ManyToOne(viewFields={"contract"},viewNames={"contract_id"})
  private Deal deal;

  @ManyToOne(viewFields={"order","amount"},viewNames={"order_id","payment_amount"})
  private Payment payment;

  @Column(defaultValue="0")
  private BigDecimal amount = new BigDecimal(0.0);

  public DealPaymentImpl() throws RemoteException {
  }

  @Override
  public Deal getDeal() throws RemoteException {
    return deal;
  }

  @Override
  public void setDeal(Deal deal) throws RemoteException {
    this.deal = deal;
  }

  @Override
  public Payment getPayment() throws RemoteException {
    return payment;
  }

  @Override
  public void setPayment(Payment payment) throws RemoteException {
    this.payment = payment;
  }

  @Override
  public BigDecimal getAmount() throws RemoteException {
    return amount;
  }

  @Override
  public void setAmount(BigDecimal amount) throws RemoteException {
    this.amount = amount;
  }
}