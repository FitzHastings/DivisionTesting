package bum.realizations;

import bum.annotations.Column;
import bum.interfaces.HelpDoc;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

public class HelpDocImpl extends MappingObjectImpl implements HelpDoc {
  @Column(length=-1)
  private String doc;
  
  public HelpDocImpl() throws RemoteException {
    super();
  }
  
  @Override
  public String getDoc() throws RemoteException {
    return doc;
  }
  
  @Override
  public void setDoc(String doc) throws RemoteException {
    this.doc = doc;
  }
}