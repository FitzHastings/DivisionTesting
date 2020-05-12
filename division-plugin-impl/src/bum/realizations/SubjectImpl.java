package bum.realizations;

import bum.annotations.Table;
import bum.interfaces.Subject;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

@Table
public class SubjectImpl extends MappingObjectImpl implements Subject {

  public SubjectImpl() throws RemoteException {
    super();
  }
}