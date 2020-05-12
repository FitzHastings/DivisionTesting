package bum.interfaces;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.List;
import mapping.MappingObject;

public interface Equipment extends MappingObject {
  public List<Request> getRequests() throws RemoteException;
  public void setRequests(List<Request> requests) throws RemoteException;
  
  public Equipment getParent() throws RemoteException;
  public void setParent(Equipment parent) throws RemoteException;
  
  public Boolean isZakaz() throws RemoteException;
  public void setZakaz(Boolean zakaz) throws RemoteException;
  
  public List<DealPosition> getDealPositions() throws RemoteException;
  public void setDealPositions(List<DealPosition> dealPositions) throws RemoteException;

  public List<EquipmentFactorValue> getGroupFactorValues() throws RemoteException;
  public void setGroupFactorValues(List<EquipmentFactorValue> groupFactorValues) throws RemoteException;
  
  //public CompanyPartition getCompanyPartition() throws RemoteException;
  //public void setCompanyPartition(CompanyPartition companyPartition) throws RemoteException;
  
  public void setGroup(Group group) throws RemoteException;
  public Group getGroup() throws RemoteException;
  
  public Store getStore() throws RemoteException;
  public void setStore(Store store) throws RemoteException;
  
  public DealPosition getSourceDealPosition() throws RemoteException;
  public void setSourceDealPosition(DealPosition sourceDealPosition) throws RemoteException;
  
  public BigDecimal getAmount() throws RemoteException;
  public void setAmount(BigDecimal amount) throws RemoteException;

  public String getPartition() throws RemoteException;
  public void setPartition(String partition) throws RemoteException;
  
  public boolean validateIdentityValue(String identityValue) throws RemoteException;
}
