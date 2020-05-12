package bum.realizations;

import bum.annotations.Column;
import bum.annotations.ManyToOne;
import bum.annotations.Table;
import bum.interfaces.Contract;
import bum.interfaces.DealPrototype;
import bum.interfaces.Service;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

@Table
public class DealPrototypeImpl extends MappingObjectImpl implements DealPrototype {
  @ManyToOne(viewFields={"name","id"}, viewNames={"process_name","process_id"})
  private Service process;
  @Column
  private String duration;
  @Column
  private String recurrence;
  @ManyToOne
  private Contract contract;

  public DealPrototypeImpl() throws RemoteException {
    super();
  }

  @Override
  public String getDuration() throws RemoteException {
    return duration;
  }

  @Override
  public void setDuration(String duration) throws RemoteException {
    this.duration = duration;
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
  public String getRecurrence() throws RemoteException {
    return recurrence;
  }

  @Override
  public void setRecurrence(String recurrence) throws RemoteException {
    this.recurrence = recurrence;
  }

  @Override
  public Contract getContract() throws RemoteException {
    return contract;
  }

  @Override
  public void setContract(Contract contract) throws RemoteException {
    this.contract = contract;
  }
}