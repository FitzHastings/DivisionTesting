package bum.interfaces;

import java.rmi.RemoteException;
import java.time.Period;
import java.util.List;

public interface XMLContractTemplate extends XMLTemplate {
  public Period getDuration() throws RemoteException;
  public void setDuration(Period duration) throws RemoteException;

  public Company getCompanyOwner() throws RemoteException;
  public void setCompanyOwner(Company companyOwner) throws RemoteException;

  public CompanyNickname getCustomerNickname() throws RemoteException;
  public void setCustomerNickname(CompanyNickname customerNickname) throws RemoteException;

  public SellerNickName getSellerNickname() throws RemoteException;
  public void setSellerNickname(SellerNickName sellerNickname) throws RemoteException;

  public Boolean isContractAccounting() throws RemoteException;
  public void setContractAccounting(Boolean contractAccounting) throws RemoteException;
  
  public List<Service> getProcesses() throws RemoteException;
  public void setProcesses(List<Service> processes) throws RemoteException;

  /*public List<Contract> getContracts() throws RemoteException;
  public void setContracts(List<Contract> contracts) throws RemoteException;*/
}
