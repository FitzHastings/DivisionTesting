package bum.interfaces;

import java.rmi.RemoteException;
import mapping.MappingObject;

public interface XMLTemplate extends MappingObject {
  public String getXML() throws RemoteException;
  public void setXML(String xml) throws RemoteException;
  
  public String getDescription() throws RemoteException;
  public void setDescription(String description) throws RemoteException;

  public String getObjectClassName() throws RemoteException;
  public void setObjectClassName(String objectClassName) throws RemoteException;
  
  public Boolean isGroup() throws RemoteException;
  public void setGroup(Boolean group) throws RemoteException;
}
