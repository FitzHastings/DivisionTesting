package bum.interfaces;

import java.rmi.RemoteException;
import java.util.List;
import mapping.MappingObject;

public interface Account extends MappingObject {
  public String getNumber() throws RemoteException;
  public void setNumber(String number) throws RemoteException;
  
  public Boolean isCurrent() throws RemoteException;
  public void setCurrent(Boolean current) throws RemoteException;
  
  public CompanyPartition getCompanyPartition() throws RemoteException;
  public void setCompanyPartition(CompanyPartition companyPartition) throws RemoteException;
  
  public void setBank(Bank bank) throws RemoteException;
  public Bank getBank() throws Exception;
  
  public List<CFC> getCfcs() throws RemoteException;
  public void setCfcs(List<CFC> cfcs) throws RemoteException;
}
