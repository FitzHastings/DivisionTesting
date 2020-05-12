package bum.interfaces;

import java.rmi.RemoteException;
import mapping.MappingObject;

public interface StateUnitPeople extends MappingObject {
  public StateUnit getStateUnit() throws RemoteException;
  public void setStateUnit(StateUnit stateUnit) throws RemoteException;
  
  public People getPeople() throws RemoteException;
  public void setPeople(People people) throws RemoteException;

  public Double getCount() throws RemoteException;
  public void setCount(Double count) throws RemoteException;
}
