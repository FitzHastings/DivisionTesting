package bum.realizations;

import bum.interfaces.Party;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

public class PartyImpl extends MappingObjectImpl implements Party {
  public PartyImpl() throws RemoteException {
    super();
  }
}