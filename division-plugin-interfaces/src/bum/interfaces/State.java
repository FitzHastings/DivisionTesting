package bum.interfaces;

import java.awt.Color;
import java.rmi.RemoteException;
import mapping.MappingObject;

public interface State extends MappingObject {
  public Color getColor() throws RemoteException;
  public void setColor(Color color) throws RemoteException;

  public Integer getStateNumber() throws RemoteException;
  public void setStateNumber(Integer stateNumber) throws RemoteException;

  public String getNickForContractCustomer() throws RemoteException;
  public void setNickForContractCustomer(String nickForContractCustomer) throws RemoteException;

  public String getNickForContractSeller() throws RemoteException;
  public void setNickForContractSeller(String nickForContractSeller) throws RemoteException;

  public String getNickForDealCustomer() throws RemoteException;
  public void setNickForDealCustomer(String nickForDealCustomer) throws RemoteException;

  public String getNickForDealSeller() throws RemoteException;
  public void setNickForDealSeller(String nickForDealSeller) throws RemoteException;
}