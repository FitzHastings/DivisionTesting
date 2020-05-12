package bum.interfaces;

import java.rmi.RemoteException;
import java.sql.Date;
import java.util.List;
import mapping.MappingObject;

public interface Contract extends MappingObject {
  public String getNumber() throws RemoteException;
  public void setNumber(String number) throws RemoteException;

  public String getGosContractId() throws RemoteException;
  public void setGosContractId(String gosContractId) throws RemoteException;
  
  public Integer getIntNumber() throws RemoteException;
  public void setIntNumber(Integer intNumber) throws RemoteException;

  public CompanyPartition getCustomerCompanyPartition() throws RemoteException;
  public void setCustomerCompanyPartition(CompanyPartition customerCompanyPartition) throws RemoteException;

  public CompanyPartition getSellerCompanyPartition() throws RemoteException;
  public void setSellerCompanyPartition(CompanyPartition sellerCompanyPartition) throws RemoteException;
  
  public XMLContractTemplate getTemplate() throws RemoteException;
  public void setTemplate(XMLContractTemplate template) throws RemoteException;
  
  public CFC getSellerCfc() throws RemoteException;
  public void setSellerCfc(CFC sellerCfc) throws RemoteException;
  
  public CFC getCustomerCfc() throws RemoteException;
  public void setCustomerCfc(CFC customerCfc) throws RemoteException;  
  
  public Company getSellerCompany() throws RemoteException;
  public void setSellerCompany(Company sellerCompany) throws RemoteException;
  
  public Company getCustomerCompany() throws RemoteException;
  public void setCustomerCompany(Company customerCompany) throws RemoteException;
  
  public Date getStartDate() throws RemoteException;
  public void setStartDate(Date startDate) throws RemoteException;
  
  public Date getEndDate() throws RemoteException;  
  public void setEndDate(Date endDate) throws RemoteException;

  public List<Deal> getDeals() throws RemoteException;
  public void setDeals(List<Deal> deals) throws RemoteException;
  
  public String getSellerReason() throws RemoteException; 
  public void setSellerReason(String sellerReason) throws RemoteException;
  
  public String getCustomerReason() throws RemoteException;
  public void setCustomerReason(String customerReason) throws RemoteException;
  
  public String getSellerPerson() throws RemoteException;
  public void setSellerPerson(String sellerPerson) throws RemoteException;
  
  public String getCustomerPerson() throws RemoteException;
  public void setCustomerPerson(String customerPerson) throws RemoteException;
}