package bum.realizations;

import bum.annotations.Table;
import bum.interfaces.CompanyNickname;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

@Table
public class CompanyNicknameImpl extends MappingObjectImpl implements CompanyNickname {
  public CompanyNicknameImpl() throws RemoteException {
    super();
  }
}