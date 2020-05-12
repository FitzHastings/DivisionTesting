package bum.realizations;

import bum.annotations.Column;
import bum.annotations.Table;
import bum.interfaces.OwnershipType;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

@Table
public class OwnershipTypeImpl extends MappingObjectImpl implements OwnershipType {
  @Column
  private String transcript;

  public OwnershipTypeImpl() throws RemoteException {
  }

  @Override
  public String getTranscript() throws RemoteException {
    return transcript;
  }

  @Override
  public void setTranscript(String transcript) throws RemoteException {
    this.transcript = transcript;
  }
}