package bum.interfaces;

import java.rmi.RemoteException;
import mapping.MappingObject;

public interface LKCompany extends MappingObject {
  public enum Command {
    SINEUP,
    GETYEARS,
    SHOWDOCS,
    PREVIEWDOCUMENT,
    PREVIEWACT,
    DOWNLOADDOCUMENT,
    DOWNLOADZIP,
    GETCONTRACTS,
    GETPASSWORD,
    LOGOUT
  }
  
  public Company getCompany() throws RemoteException;
  public void setCompany(Company company) throws RemoteException;
  
  public Command getCommand() throws RemoteException;
  public void setCommand(Command command) throws RemoteException;
  
  public String getSession() throws RemoteException;
  public void setSession(String session) throws RemoteException;
}