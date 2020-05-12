package bum.interfaces;

import java.rmi.RemoteException;
import java.util.List;
import mapping.MappingObject;

public interface RMIDBNodeObject<T> extends MappingObject {
  public T getParent() throws RemoteException;
  public void setParent(T parent) throws RemoteException;
  public List<T> getChilds() throws RemoteException;
  public void setChilds(List<T> childs) throws RemoteException;
}
