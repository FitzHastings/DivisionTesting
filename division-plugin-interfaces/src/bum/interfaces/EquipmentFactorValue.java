package bum.interfaces;

import java.rmi.RemoteException;
import mapping.MappingObject;

public interface EquipmentFactorValue extends MappingObject {
  public Factor getFactor() throws RemoteException;
  public void setFactor(Factor factor) throws RemoteException;

  public Equipment getEquipment() throws RemoteException;
  public void setEquipment(Equipment equipment) throws RemoteException;
  
  public boolean checkValue(String value) throws RemoteException;
}