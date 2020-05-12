package bum.realizations;

import bum.annotations.*;
import bum.interfaces.Account;
import bum.interfaces.Bank;
import bum.interfaces.CFC;
import bum.interfaces.CompanyPartition;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import mapping.MappingObjectImpl;

@Table(clientName="Расчётные счета")
public class AccountImpl extends MappingObjectImpl implements Account {
  @Column(description="Номер расчётного счёта")
  private String number;

  @Column(description="Основной",defaultValue="false")
  private Boolean current;
  
  @ManyToOne(description="Владелец (подразделение предприятия)")
  private CompanyPartition companyPartition;
  
  @ManyToOne(viewFields = {"name","corrAccount","bik","address"}, viewNames = {"bank_name","bank_corrAccount","bank_bik","bank_address"})
  private Bank bank;
  
  @ManyToMany(mappedBy="accounts")
  private List<CFC> cfcs = new ArrayList<>();

  public AccountImpl() throws RemoteException {
  }
  
  @Override
  public Bank getBank() throws Exception {
   return bank;
  }
  
  @Override
  public void setBank(Bank bank) throws RemoteException {
    this.bank = bank;
  }

  @Override
  public List<CFC> getCfcs() throws RemoteException {
    return cfcs;
  }

  @Override
  public void setCfcs(List<CFC> cfcs) throws RemoteException {
    this.cfcs = cfcs;
  }

  @Override
  public String getNumber() throws RemoteException {
    return this.number;
  }

  @Override
  public void setNumber(String number) throws RemoteException {
    this.number = number;
  }

  @Override
  public Boolean isCurrent() throws RemoteException {
    return this.current;
  }

  @Override
  public void setCurrent(Boolean current) throws RemoteException {
    this.current = current;
  }

  @Override
  public CompanyPartition getCompanyPartition() throws RemoteException {
    return this.companyPartition;
  }

  @Override
  public void setCompanyPartition(CompanyPartition companyPartition) throws RemoteException {
    this.companyPartition = companyPartition;
  }
}