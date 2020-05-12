package bum.interfaces;

import java.rmi.RemoteException;

public interface Department extends RMIDBNodeObject<Department> {
  public Company getCompany() throws RemoteException;
  public void setCompany(Company company) throws RemoteException;
}
