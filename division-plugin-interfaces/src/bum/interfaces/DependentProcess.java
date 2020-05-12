package bum.interfaces;

import java.rmi.RemoteException;
import mapping.MappingObject;

public interface DependentProcess extends MappingObject {
  public ProductDocument.ActionType getActionType() throws RemoteException;
  public void setActionType(ProductDocument.ActionType actionType) throws RemoteException;

  public String getDelay() throws RemoteException;
  public void setDelay(String delay) throws RemoteException;

  public Service getProcess() throws RemoteException;
  public void setProcess(Service Process) throws RemoteException;
  
  public Service getSubProcess() throws RemoteException;
  public void setSubProcess(Service subProcess) throws RemoteException;
  
  public XMLContractTemplate getContractTemplate() throws RemoteException;
  public void setContractTemplate(XMLContractTemplate contractTemplate) throws RemoteException;

  //public ContractProcess getTempProcess() throws RemoteException;
  //public void setTempProcess(ContractProcess tempProcess) throws RemoteException;
}