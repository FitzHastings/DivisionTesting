package bum.realizations;

import bum.annotations.Column;
import bum.annotations.ManyToOne;
import bum.annotations.Table;
import bum.interfaces.Bank;
import bum.interfaces.Region;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

@Table(history=true,clientName="Банки")
public class BankImpl extends MappingObjectImpl implements Bank {
  @Column(description="Адрес")
  private String address;
  
  @Column(description="БИК")
  private String bik;
  
  @Column(description="Корреспондентский счёт")
  private String corrAccount;
  
  @ManyToOne(description="Номер региона",viewFields={"town"},viewNames={"town"})
  private Region region;
  
  public BankImpl() throws RemoteException {
  }

  @Override
  public String getAddress() throws RemoteException {
    return this.address;
  }

  @Override
  public void setAddress(String address) throws RemoteException {
    this.address = address;
  }

  @Override
  public String getBik() throws RemoteException {
    return this.bik;
  }

  @Override
  public void setBik(String bik) throws RemoteException {
    this.bik = bik;
  }

  @Override
  public String getCorrAccount() throws RemoteException {
    return this.corrAccount;
  }

  @Override
  public void setCorrAccount(String corrAccount) throws RemoteException {
    this.corrAccount = corrAccount;
  }

  @Override
  public Region getRegion() throws RemoteException {
    return region;
  }

  @Override
  public void setRegion(Region region) throws RemoteException {
    this.region = region;
  }
}