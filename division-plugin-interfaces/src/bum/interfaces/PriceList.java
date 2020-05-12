package bum.interfaces;

import java.rmi.RemoteException;
import java.util.List;
import mapping.MappingObject;

public interface PriceList extends MappingObject {
  
  public List<Product> getProducts() throws RemoteException;
  public void setProducts(List<Product> products) throws RemoteException;

  public Company getCompany() throws RemoteException;
  public void setCompany(Company company) throws RemoteException;
}
