package bum.interfaces;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.sql.Date;
import java.util.List;
import mapping.MappingObject;

public interface Payment extends MappingObject {
  public enum State{ПОДГОТОВКА,ОТПРАВЛЕНО,ИСПОЛНЕНО}
  
  public State getState();
  public void setState(State state);
  
  public Store getSellerStore() throws RemoteException;
  public void setSellerStore(Store sellerStore) throws RemoteException;

  public Store getCustomerStore() throws RemoteException;
  public void setCustomerStore(Store customerStore) throws RemoteException;
  
  public Integer getReceiptNumber() throws RemoteException;
  public void setReceiptNumber(Integer receiptNumber) throws RemoteException;
  
  public List<Contract> getContracts() throws RemoteException;
  public void setContracts(List<Contract> contracts) throws RemoteException;
  
  //public CreatedDocument getOrder() throws RemoteException;
  //public void setOrder(CreatedDocument order) throws RemoteException;
  
  public List<DealPayment> getDealPayments() throws RemoteException;
  public void setDealPayments(List<DealPayment> dealPayments) throws RemoteException;

  public CompanyPartition getCustomerCompanyPartition() throws RemoteException;
  public void setCustomerCompanyPartition(CompanyPartition customerCompanyPartition) throws RemoteException;

  public CompanyPartition getSellerCompanyPartition() throws RemoteException;
  public void setSellerCompanyPartition(CompanyPartition sellerCompanyPartition) throws RemoteException;

  public Date getOperationDate() throws RemoteException;
  public void setOperationDate(Date operationDate) throws RemoteException;
  
  public String getPaymentDocumentNumber() throws RemoteException;
  public void setPaymentDocumentNumber(String paymentDocumentNumber) throws RemoteException;

  public BigDecimal getAmount() throws RemoteException;
  public void setAmount(BigDecimal amount) throws RemoteException;
}
