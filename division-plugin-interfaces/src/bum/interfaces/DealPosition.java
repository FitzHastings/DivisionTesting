package bum.interfaces;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.List;
import mapping.MappingObject;

public interface DealPosition extends MappingObject {
  public Request getRequest();
  public void setRequest(Request request);
  
  public Product getProduct() throws RemoteException;
  public void setProduct(Product product) throws RemoteException;
  
  public Equipment getEquipment() throws RemoteException;
  public void setEquipment(Equipment equipment) throws RemoteException;
  
  public DealPosition getEquipmentSourceDealPosition() throws RemoteException;
  public void setEquipmentSourceDealPosition(DealPosition equipmentSourceDealPosition) throws RemoteException;
  
  public Deal getDeal() throws RemoteException;
  public void setDeal(Deal deal) throws RemoteException;
  
  public Store getTargetStore() throws RemoteException;
  public void setTargetStore(Store targetStore) throws RemoteException;
  
  public Timestamp getActionDispatchDate() throws RemoteException;
  public void setActionDispatchDate(Timestamp actionDispatchDate) throws RemoteException;
  
  public BigDecimal getAmount() throws RemoteException;
  public void setAmount(BigDecimal amount) throws RemoteException;
  
  public Store getSourceStore() throws RemoteException;
  public void setSourceStore(Store sourceStore) throws RemoteException;
  
  public BigDecimal getCustomProductCost() throws RemoteException;
  public void setCustomProductCost(BigDecimal customProductCost) throws RemoteException;
  
  public Timestamp getStartDate() throws RemoteException;
  public void setStartDate(Timestamp startDate) throws RemoteException;

  public Long getStartId() throws RemoteException;
  public void setStartId(Long startId) throws RemoteException;
  
  public Long getDispatchId() throws RemoteException;
  public void setDispatchId(Long dispatchId) throws RemoteException;
  
  public Timestamp getDispatchDate() throws RemoteException;
  public void setDispatchDate(Timestamp dispatchDate) throws RemoteException;
  
  public List<CreatedDocument> getDocuments() throws RemoteException;
  public void setDocuments(List<CreatedDocument> documents) throws RemoteException;
}
