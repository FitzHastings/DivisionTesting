package bum.interfaces;

import java.rmi.RemoteException;
import java.time.LocalDateTime;
import mapping.MappingObject;

public interface Comment extends MappingObject {
  public String getObjectClass() throws RemoteException;
  public void setObjectClass(String objectClass) throws RemoteException;

  public String getAuthor() throws RemoteException;
  public void setAuthor(String author) throws RemoteException;

  public String getText() throws RemoteException;
  public void setText(String text) throws RemoteException;
  
  public LocalDateTime getControlDateTime() throws RemoteException;
  public void setControlDateTime(LocalDateTime controlDateTime) throws RemoteException;
  
  public Integer getObjectId() throws RemoteException;
  public void setObjectId(Integer objectId) throws RemoteException;
}