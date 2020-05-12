package bum.realizations;

import bum.annotations.Column;
import bum.interfaces.Unit;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

public class UnitImpl extends MappingObjectImpl implements Unit {
  
  @Column(defaultValue = "true")
  private Boolean intval = true;

  public UnitImpl() throws RemoteException {
    super();
  }

  @Override
  public Boolean isIntval() {
    return intval;
  }

  @Override
  public void setIntval(Boolean intval) {
    this.intval = intval;
  }
}