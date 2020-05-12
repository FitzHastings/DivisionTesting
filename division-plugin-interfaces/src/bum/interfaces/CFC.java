package bum.interfaces;

import java.rmi.RemoteException;
import java.util.List;

public interface CFC extends RMIDBNodeObject<CFC> {
  public List<Company> getCompanys() throws RemoteException;
  public void setCompanys(List companys) throws RemoteException;
  public void addCompanys(List companys) throws RemoteException;
  public void removeCompanys(List companys) throws RemoteException;

  public List<Worker> getUsers() throws RemoteException;
  public void setUsers(List<Worker> users) throws RemoteException;
  public void addUsers(List<Worker> users) throws RemoteException;
  public void removeUsers(List<Worker> users) throws RemoteException;
}