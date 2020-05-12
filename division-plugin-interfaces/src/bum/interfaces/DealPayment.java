package bum.interfaces;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import mapping.MappingObject;

public interface DealPayment extends MappingObject {
  public Deal getDeal() throws RemoteException;
  public void setDeal(Deal deal) throws RemoteException;

  public Payment getPayment() throws RemoteException;
  public void setPayment(Payment payment) throws RemoteException;

  public BigDecimal getAmount() throws RemoteException;
  public void setAmount(BigDecimal amount) throws RemoteException;
}