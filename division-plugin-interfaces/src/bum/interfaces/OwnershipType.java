package bum.interfaces;

import java.rmi.RemoteException;
import mapping.MappingObject;

public interface OwnershipType extends MappingObject {
  public String getTranscript() throws RemoteException;
  public void setTranscript(String transcript) throws RemoteException;
}