package bum.realizations;

import bum.annotations.Table;
import bum.interfaces.SellerNickName;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

@Table
public class SellerNickNameImpl extends MappingObjectImpl implements SellerNickName {
  public SellerNickNameImpl() throws RemoteException {
    super();
  }
}