package bum.interfaces;

import java.rmi.RemoteException;
import mapping.MappingObject;

public interface MarkerNode extends MappingObject {
  public Integer getBlue() throws RemoteException;
  public void setBlue(Integer blue) throws RemoteException;

  public Integer getGreen() throws RemoteException;
  public void setGreen(Integer green) throws RemoteException;

  public String getObjectClass() throws RemoteException;
  public void setObjectClass(String objectClass) throws RemoteException;

  public Integer[] getObjectsId() throws RemoteException;
  public void setObjectsId(Integer[] objectsId) throws RemoteException;

  public Integer getRed() throws RemoteException;
  public void setRed(Integer red) throws RemoteException;

  public String getText() throws RemoteException;
  public void setText(String text) throws RemoteException;
}