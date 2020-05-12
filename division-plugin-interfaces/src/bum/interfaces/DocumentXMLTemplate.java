package bum.interfaces;

import java.rmi.RemoteException;

public interface DocumentXMLTemplate extends XMLTemplate {
  public CompanyPartition getCompanyPartition() throws RemoteException;
  public void setCompanyPartition(CompanyPartition companyPartition) throws RemoteException;
  
  public Document getDocument() throws RemoteException;
  public void setDocument(Document document) throws RemoteException;

  public Boolean isMain() throws RemoteException;
  public void setMain(Boolean main) throws RemoteException;
}