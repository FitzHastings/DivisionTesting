package bum.realizations;

import bum.annotations.Table;
import bum.interfaces.Place;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

@Table
public class PlaceImpl extends MappingObjectImpl implements Place {
  public PlaceImpl() throws RemoteException {
    super();
  }
}