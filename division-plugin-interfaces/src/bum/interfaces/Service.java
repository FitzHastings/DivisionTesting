package bum.interfaces;

import java.rmi.RemoteException;
import java.util.List;

public interface Service extends RMIDBNodeObject<Service> {
  public enum Owner{SELLER,CUSTOMER}
  
  public List<XMLContractTemplate> getContractTemplates() throws RemoteException;
  public void setContractTemplates(List<XMLContractTemplate> contractTemplates) throws RemoteException;
  
  public Boolean isMoveStorePosition() throws RemoteException;
  public void setMoveStorePosition(Boolean moveStorePosition) throws RemoteException;
  
  public List<Product> getProducts() throws RemoteException;
  public void setProducts(List<Product> products) throws RemoteException;
  
  public Owner getOwner() throws RemoteException;
  public void setOwner(Owner owner) throws RemoteException;
}
