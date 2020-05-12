package bum.interfaces;

import java.rmi.RemoteException;
import mapping.MappingObject;

public interface DispatchDealPosition extends MappingObject {
  public Double getCost() throws RemoteException;
  public void setCost(Double cost) throws RemoteException;

  public DealPosition getDealPosition() throws RemoteException;
  public void setDealPosition(DealPosition dealPosition) throws RemoteException;

  public Integer getDispatchAmount() throws RemoteException;
  public void setDispatchAmount(Integer dispatchAmount) throws RemoteException;
}