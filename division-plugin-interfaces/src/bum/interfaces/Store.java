package bum.interfaces;

import bum.interfaces.Group.ObjectType;
import java.rmi.RemoteException;

public interface Store extends RMIDBNodeObject<Store> {
  public enum StoreType {НАЛИЧНЫЙ, БЕЗНАЛИЧНЫЙ}
  
  public ObjectType getObjectType() throws RemoteException;
  public void setObjectType(ObjectType objectType) throws RemoteException;
  
  public StoreType getStoreType() throws RemoteException;
  public void setStoreType(StoreType storeType) throws RemoteException;
  
  public Boolean isControllIn() throws RemoteException;
  public void setControllIn(Boolean controllIn) throws RemoteException;

  public Boolean isControllOut() throws RemoteException;
  public void setControllOut(Boolean controllOut) throws RemoteException;
  
  public Boolean isMain() throws RemoteException;
  public void setMain(Boolean main) throws RemoteException;
  
  public CompanyPartition getCompanyPartition() throws RemoteException;
  public void setCompanyPartition(CompanyPartition companyPartition) throws RemoteException;
  
  public Group getCurrency() throws RemoteException;
  public void setCurrency(Group currency) throws RemoteException;
}