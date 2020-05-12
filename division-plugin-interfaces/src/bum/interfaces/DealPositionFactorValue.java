package bum.interfaces;

import java.rmi.RemoteException;
import java.sql.SQLException;
import mapping.MappingObject;

public interface DealPositionFactorValue extends MappingObject {
  public Boolean isActive() throws RemoteException;
  public void setActive(Boolean active) throws RemoteException;
  
  public DealPosition getDealPosition() throws RemoteException;
  public void setDealPosition(DealPosition dealPosition) throws RemoteException;

  public Factor getFactor() throws RemoteException;
  public void setFactor(Factor factor) throws RemoteException;
  
  public Double getCost() throws RemoteException;
  public Double getCostQuery() throws RemoteException, SQLException;
}
