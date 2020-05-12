package bum.interfaces;

import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.List;
import mapping.MappingObject;

public interface CreatedDocument extends MappingObject {
  public enum ExportType{FOREXPORT,EXPORTED}
  public enum SendType{FORSEND,SENT}
  
  public ExportType getExportType() throws RemoteException;
  public void setExportType(ExportType exportType) throws RemoteException;

  public SendType getSendType() throws RemoteException;
  public void setSendType(SendType sendType) throws RemoteException;
  
  public Timestamp getStornoDate() throws RemoteException;
  public void setStornoDate(Timestamp stornoDate) throws RemoteException;
  
  public ProductDocument.ActionType getActionType() throws RemoteException;
  public void setActionType(ProductDocument.ActionType actionType) throws RemoteException;
  
  public Integer getIntNumber() throws RemoteException;
  public void setIntNumber(Integer intNumber) throws RemoteException;

  public String getXml() throws RemoteException;
  public void setXml(String xml) throws RemoteException;

  public CompanyPartition getCustomerCompanyPartition() throws RemoteException;
  public void setCustomerCompanyPartition(CompanyPartition customerCompanyPartition) throws RemoteException;

  public CompanyPartition getSellerCompanyPartition() throws RemoteException;
  public void setSellerCompanyPartition(CompanyPartition sellerCompanyPartition) throws RemoteException;

  public List<Deal> getDeals() throws RemoteException;
  public void setDeals(List<Deal> deals) throws RemoteException;
  
  public List<DealPosition> getDealPositions() throws RemoteException;
  public void setDealPositions(List<DealPosition> dealPositions) throws RemoteException;
  
  public Document getDocument() throws RemoteException;
  public void setDocument(Document document) throws RemoteException;

  public String getNumber() throws RemoteException;
  public void setNumber(String number) throws RemoteException;
  
  public Payment getPayment() throws RemoteException;
  public void setPayment(Payment payment) throws RemoteException;

  public int getSellerAccountID() throws  RemoteException;
  public void setSellerAccountID(int sellerAccountID) throws  RemoteException;
}