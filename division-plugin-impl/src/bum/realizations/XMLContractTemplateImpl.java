package bum.realizations;

import bum.annotations.Column;
import bum.annotations.ManyToMany;
import bum.annotations.ManyToOne;
import bum.annotations.Table;
import bum.interfaces.*;
import java.rmi.RemoteException;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Table
public class XMLContractTemplateImpl extends XMLTemplateImpl implements XMLContractTemplate {
  @ManyToOne
  private Company companyOwner;

  @ManyToOne
  private SellerNickName sellerNickname;

  @ManyToOne
  private CompanyNickname customerNickname;

  @Column(defaultValue="true")
  private Boolean contractAccounting = true;
  
  @ManyToMany
  private List<Service> processes = new ArrayList<>();

  /*@OneToMany(mappedBy="template")
  private List<Contract> contracts = new ArrayList<>();*/

  @Column
  private Period duration;

  public XMLContractTemplateImpl() throws RemoteException {
    super();
    setObjectClassName(ContractImpl.class.getName());
  }

  @Override
  public Period getDuration() throws RemoteException {
    return duration;
  }

  @Override
  public void setDuration(Period duration) throws RemoteException {
    this.duration = duration;
  }

  @Override
  public Company getCompanyOwner() throws RemoteException {
    return companyOwner;
  }

  @Override
  public void setCompanyOwner(Company companyOwner) throws RemoteException {
    this.companyOwner = companyOwner;
  }

  @Override
  public List<Service> getProcesses() throws RemoteException {
    return processes;
  }

  @Override
  public void setProcesses(List<Service> processes) throws RemoteException {
    this.processes = processes;
  }

  /*@Override
  public List<Contract> getContracts() throws RemoteException {
    return contracts;
  }

  @Override
  public void setContracts(List<Contract> contracts) throws RemoteException {
    this.contracts = contracts;
  }*/

  @Override
  public CompanyNickname getCustomerNickname() throws RemoteException {
    return customerNickname;
  }

  @Override
  public void setCustomerNickname(CompanyNickname customerNickname) throws RemoteException {
    this.customerNickname = customerNickname;
  }

  @Override
  public SellerNickName getSellerNickname() throws RemoteException {
    return sellerNickname;
  }

  @Override
  public void setSellerNickname(SellerNickName sellerNickname) throws RemoteException {
    this.sellerNickname = sellerNickname;
  }

  @Override
  public Boolean isContractAccounting() throws RemoteException {
    return contractAccounting;
  }

  @Override
  public void setContractAccounting(Boolean contractAccounting) throws RemoteException {
    this.contractAccounting = contractAccounting;
  }
}