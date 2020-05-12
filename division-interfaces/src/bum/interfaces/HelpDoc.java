package bum.interfaces;

import java.rmi.RemoteException;
import mapping.MappingObject;

public interface HelpDoc extends MappingObject {
  public String getDoc() throws RemoteException;
  public void setDoc(String doc) throws RemoteException;
}