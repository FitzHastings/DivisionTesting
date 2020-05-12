package bum.interfaces;

import java.rmi.RemoteException;

public interface JScript extends JSModul {
  public Boolean isAutoRun() throws RemoteException;
  public void setAutoRun(Boolean autoRun) throws RemoteException;

  public String getSourceClass() throws RemoteException;
  public void setSourceClass(String sourceClass) throws RemoteException;

  public String getClassDescription() throws RemoteException;
  public void setClassDescription(String classDescription) throws RemoteException;
}