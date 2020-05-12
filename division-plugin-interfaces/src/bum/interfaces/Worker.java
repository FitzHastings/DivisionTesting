package bum.interfaces;

import java.rmi.RemoteException;
import mapping.MappingObject;

public interface Worker extends MappingObject {
  public CFC getCfc() throws RemoteException;
  public void setCfc(CFC cfc) throws RemoteException;
  
  public People getPeople() throws RemoteException;
  public void setPeople(People people) throws RemoteException;
}
