package util.proxy;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public abstract class DelegatorInterfaceImpl extends UnicastRemoteObject implements DelegatorInterface {
  public DelegatorInterfaceImpl() throws RemoteException {
  }
}