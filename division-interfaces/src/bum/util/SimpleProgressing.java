package bum.util;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SimpleProgressing extends Remote {
  public void setMinMax(int min, int max) throws RemoteException;
  public void setValue(int value) throws RemoteException;
  public Integer getValue() throws RemoteException;
  
  public void addData(Object data) throws RemoteException;
  public void start() throws RemoteException;
  public void finish() throws RemoteException;
  public void exit() throws RemoteException;

  public void setTitle(String title) throws RemoteException;
  public String getTitle() throws RemoteException;

  public void setText(String text) throws RemoteException;
  public String getText() throws RemoteException;

  public int getMaximum() throws RemoteException;
  
  public boolean isRun() throws RemoteException;
  public void shutdown() throws RemoteException;
}