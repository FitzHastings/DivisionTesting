package bum.realizations;

import bum.annotations.ManyToOne;
import bum.annotations.QueryColumn;
import bum.annotations.Table;
import bum.interfaces.*;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

@Table(
        name = "tempprocess",
        queryColumns={
          @QueryColumn(name="dealCount",query="SELECT COUNT(id) FROM [Deal] WHERE [Deal(tempProcess)]=[TempProcess(id)]")
        })
public class ContractProcessImpl extends MappingObjectImpl implements ContractProcess {
  @ManyToOne(viewFields = {"number"}, viewNames = {"contract-number"})
  private Contract contract;
  
  @ManyToOne(viewFields = {"name"}, viewNames = {"customer-partition-name"})
  private CompanyPartition customerPartition;
  
  @ManyToOne(viewFields={"name","name"}, viewNames={"process-name","process_name"})
  private Service process;

  public ContractProcessImpl() throws RemoteException {
    super();
  }

  @Override
  public CompanyPartition getCustomerPartition() throws RemoteException {
    return customerPartition;
  }

  @Override
  public void setCustomerPartition(CompanyPartition customerPartition) throws RemoteException {
    this.customerPartition = customerPartition;
  }

  @Override
  public Contract getContract() throws RemoteException {
    return contract;
  }

  @Override
  public void setContract(Contract contract) throws RemoteException {
    this.contract = contract;
  }

  @Override
  public Service getProcess() throws RemoteException {
    return process;
  }

  @Override
  public void setProcess(Service process) throws RemoteException {
    this.process = process;
  }

  /*@Override
  public XMLContractTemplate getTemplate() throws RemoteException {
    return template;
  }

  @Override
  public void setTemplate(XMLContractTemplate template) throws RemoteException {
    this.template = template;
  }*/
}