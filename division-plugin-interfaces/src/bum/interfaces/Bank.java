package bum.interfaces;

import java.rmi.RemoteException;
import mapping.MappingObject;

public interface Bank extends MappingObject {
  public String getAddress() throws RemoteException;
  public void setAddress(String address) throws RemoteException;
  
  public String getBik() throws RemoteException;
  public void setBik(String bik) throws RemoteException;
  
  public String getCorrAccount() throws RemoteException;
  public void setCorrAccount(String corrAccount) throws RemoteException;
  
  public Region getRegion() throws RemoteException;
  public void setRegion(Region region) throws RemoteException;
}