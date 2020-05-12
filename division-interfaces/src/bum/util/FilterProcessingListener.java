package bum.util;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;
import mapping.MappingObject;

public interface FilterProcessingListener extends Remote {
  public void addObject(MappingObject object,int index) throws RemoteException;
  public void addData(Vector data,int index) throws RemoteException;
  public void setProgress(int min, int max) throws RemoteException;
  public Boolean isShutdown() throws RemoteException;
  public void setShutdown(Boolean shutdown) throws RemoteException;
  public void stop() throws RemoteException;
}
