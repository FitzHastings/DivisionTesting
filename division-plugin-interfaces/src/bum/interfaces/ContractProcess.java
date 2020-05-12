package bum.interfaces;

import java.rmi.RemoteException;
import mapping.MappingObject;

public interface ContractProcess extends MappingObject {
  public Contract getContract() throws RemoteException;
  public void setContract(Contract contract) throws RemoteException;
  
  public Service getProcess() throws RemoteException;
  public void setProcess(Service process) throws RemoteException;
  
  public CompanyPartition getCustomerPartition() throws RemoteException;
  public void setCustomerPartition(CompanyPartition customerPartition) throws RemoteException;

  /*public XMLContractTemplate getTemplate() throws RemoteException;
  public void setTemplate(XMLContractTemplate template) throws RemoteException;*/
}