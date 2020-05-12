package bum.realizations;

import bum.annotations.Column;
import bum.annotations.ManyToOne;
import bum.interfaces.DependentProcess;
import bum.interfaces.ProductDocument;
import bum.interfaces.ProductDocument.ActionType;
import bum.interfaces.Service;
import bum.interfaces.XMLContractTemplate;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

public class DependentProcessImpl extends MappingObjectImpl implements DependentProcess {
  //@ManyToOne(viewFields = {"process", "contract", "customerPartition"}, viewNames = {"subprocess", "contract", "customerPartition"})
  //private ContractProcess tempProcess;
  
  @ManyToOne
  private XMLContractTemplate contractTemplate;
  
  @ManyToOne(viewFields={"name"},viewNames={"process-name"})
  private Service process;
  
  @ManyToOne(viewFields={"name"},viewNames={"sub-process-name"})
  private Service subProcess;

  @Column(defaultValue="СТАРТ", nullable=false)
  private ProductDocument.ActionType actionType;
  
  @Column(defaultValue="0 дней", nullable=false)
  private String delay;

  public DependentProcessImpl() throws RemoteException {
    super();
  }

  @Override
  public ActionType getActionType() throws RemoteException {
    return actionType;
  }

  @Override
  public void setActionType(ActionType actionType) throws RemoteException {
    this.actionType = actionType;
  }

  @Override
  public String getDelay() throws RemoteException {
    return delay;
  }

  @Override
  public void setDelay(String delay) throws RemoteException {
    this.delay = delay;
  }

  @Override
  public XMLContractTemplate getContractTemplate() throws RemoteException {
    return contractTemplate;
  }

  @Override
  public void setContractTemplate(XMLContractTemplate contractTemplate) throws RemoteException {
    this.contractTemplate = contractTemplate;
  }

  @Override
  public Service getProcess() throws RemoteException {
    return process;
  }

  @Override
  public void setProcess(Service process) throws RemoteException {
    this.process = process;
  }
  
  @Override
  public Service getSubProcess() throws RemoteException {
    return subProcess;
  }

  @Override
  public void setSubProcess(Service subProcess) throws RemoteException {
    this.subProcess = subProcess;
  }

  /*@Override
  public ContractProcess getTempProcess() throws RemoteException {
    return tempProcess;
  }

  @Override
  public void setTempProcess(ContractProcess tempProcess) throws RemoteException {
    this.tempProcess = tempProcess;
  }*/
}