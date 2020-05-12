package bum.interfaces;

import java.rmi.RemoteException;
import mapping.MappingObject;

public interface ProductFactorRule extends MappingObject {
  public Product getProduct() throws RemoteException;
  public void setProduct(Product product) throws RemoteException;

  public Factor getFactor() throws RemoteException;
  public void setFactor(Factor factor) throws RemoteException;

  public String getCondition() throws RemoteException;
  public void setCondition(String condition) throws RemoteException;
  
  public String getParameter() throws RemoteException;
  public void setParameter(String parameter) throws RemoteException;
  
  public String getOperator() throws RemoteException;
  public void setOperator(String operator) throws RemoteException;
  
  public Double getMeaning() throws RemoteException;
  public void setMeaning(Double meaning) throws RemoteException;
}