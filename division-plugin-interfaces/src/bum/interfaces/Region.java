package bum.interfaces;

import java.rmi.RemoteException;
import java.util.List;
import mapping.MappingObject;

public interface Region extends MappingObject {
  public String getTown() throws RemoteException;
  public void setTown(String town) throws RemoteException;

  public List<Bank> getBanks() throws RemoteException;
  public void setBanks(List<Bank> banks) throws RemoteException;
  
  public void addBanks(List<Bank> banks) throws RemoteException;
  public void removeBanks(List<Bank> banks) throws RemoteException;
  
  public String getRegionCode() throws RemoteException;
  public void setRegionCode(String regionCode) throws RemoteException;
}
