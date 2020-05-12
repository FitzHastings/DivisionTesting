package bum.interfaces;

import java.rmi.RemoteException;
import mapping.MappingObject;

public interface DealPrototype extends MappingObject {
  public String getDuration() throws RemoteException;
  public void setDuration(String duration) throws RemoteException;

  public Service getProcess() throws RemoteException;
  public void setProcess(Service process) throws RemoteException;

  public String getRecurrence() throws RemoteException;
  public void setRecurrence(String recurrence) throws RemoteException;

  public Contract getContract() throws RemoteException;
  public void setContract(Contract contract) throws RemoteException;
}