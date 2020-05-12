package bum.util;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Condition extends Remote {
  
  public void remove() throws RemoteException;
}
