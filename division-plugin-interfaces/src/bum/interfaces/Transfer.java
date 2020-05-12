package bum.interfaces;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import mapping.MappingObject;

public interface Transfer extends MappingObject {
  public Integer getTransaction() throws RemoteException;
  public void setTransaction(Integer transaction) throws RemoteException;
  
  public Store getSource() throws RemoteException;
  public void setSource(Store source) throws RemoteException;
  
  public Store getTarget() throws RemoteException;
  public void setTarget(Store source) throws RemoteException;
  
  public Equipment getEquipment() throws RemoteException;
  public void setEquipment(Equipment equipment) throws RemoteException;
  
  public BigDecimal getMoneyAmount() throws RemoteException;
  public void getMoneyAmount(BigDecimal moneyAmount) throws RemoteException;
}