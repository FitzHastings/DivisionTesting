package bum.realizations;

import bum.annotations.Column;
import bum.annotations.ManyToOne;
import bum.interfaces.Company;
import bum.interfaces.LKCompany;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

public class LKCompanyImpl extends MappingObjectImpl implements LKCompany {
  @ManyToOne Company company;
  @Column Command command;
  @Column String session;
  
  public LKCompanyImpl() throws RemoteException {
  }

  @Override
  public Company getCompany() throws RemoteException {
    return this.company;
  }

  @Override
  public void setCompany(Company company) throws RemoteException {
    this.company = company;
  }
  
  @Override
  public Command getCommand() throws RemoteException {
    return this.command;
  }
  
  @Override
  public void setCommand(Command command) throws RemoteException {
    this.command = command;
  }

  @Override
  public String getSession() throws RemoteException {
    return session;
  }

  @Override
  public void setSession(String session) throws RemoteException {
    this.session = session;
  }
}