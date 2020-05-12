package bum.interfaces;

import java.rmi.RemoteException;
import java.util.List;
import mapping.MappingObject;

public interface Factor extends MappingObject {
  public enum FactorType{число, текст, дата, время, дата_время, адрес}
  
  public FactorType getFactorType() throws RemoteException;
  public void setFactorType(FactorType factorType) throws RemoteException;
  
  public Unit getUnit() throws RemoteException;
  public void setUnit(Unit unit) throws RemoteException;
  
  public Boolean isProductFactor() throws RemoteException;
  public void setProductFactor(Boolean productFactor) throws RemoteException;
  
  public List<Group> getGroups() throws RemoteException;
  public void setGroups(List<Group> groups) throws RemoteException;

  public List<Product> getProducts() throws RemoteException;
  public void setProducts(List<Product> products) throws RemoteException;

  public List<Service> getProcesses() throws RemoteException;
  public void setProcesses(List<Service> processes) throws RemoteException;
  
  public Boolean isUnique() throws RemoteException;
  public void setUnique(Boolean unique) throws RemoteException;
  
  public String getListValues() throws RemoteException;
  public void setListValues(String listValues) throws RemoteException;  
}
