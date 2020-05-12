package bum.interfaces;

import java.rmi.RemoteException;
import java.util.List;
import mapping.MappingObject;

public interface StateUnit extends MappingObject {
  public List<StateUnitPeople> getStateUnitPeoples() throws RemoteException;
  public void setStateUnitPeoples(List<StateUnitPeople> stateUnitPeoples) throws RemoteException;
  
  public Department getDepartment() throws RemoteException;
  public void setDepartment(Department department) throws RemoteException;
  
  public Double getCount() throws RemoteException;
  public void setCount(Double count) throws RemoteException;
  
  public Company getCompany() throws RemoteException;
  public void setCompany(Company company) throws RemoteException;
}
