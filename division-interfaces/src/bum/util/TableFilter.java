package bum.util;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TableFilter extends Remote {
  public enum EqualType {IN,NOT_IN}
  
  public void open() throws RemoteException;
  public void close() throws RemoteException;
  
  public Condition AND(Class targetClass, 
          EqualType type, Object[] objects) throws RemoteException;
  
  public Condition AND(String methodName, 
          EqualType type, Object[] objects) throws RemoteException;
  
  public Condition OR(Class targetClass, 
          EqualType type, Object[] objects) throws RemoteException;
  
  public Condition OR(String methodName, 
          EqualType type, Object[] objects) throws RemoteException;
  
  public void clear() throws RemoteException;
  
  public Object[] getObjects() throws RemoteException;
  
  public boolean isSatisfy(Object object) throws Exception;
  
  public void setFilterToObject(Object object) throws RemoteException;
}
