package bum.realizations;

import bum.annotations.Column;
import bum.annotations.OneToMany;
import bum.annotations.Table;
import bum.interfaces.Bank;
import bum.interfaces.Region;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import mapping.MappingObjectImpl;

@Table
public class RegionImpl extends MappingObjectImpl implements Region {
  @Column
  private String regionCode;

  @Column
  private String town;
  
  @OneToMany(mappedBy="region")
  private List<Bank> banks = new ArrayList<Bank>();
  
  public RegionImpl() throws RemoteException {
    super();
  }

  @Override
  public String getTown() throws RemoteException {
    return town;
  }

  @Override
  public void setTown(String town) throws RemoteException {
    this.town = town;
  }
  
  @Override
  public List<Bank> getBanks() throws RemoteException {
    return banks;
  }
  
  @Override
  public void setBanks(List<Bank> banks) throws RemoteException {
    this.banks = banks;
  }

  @Override
  public void addBanks(List<Bank> banks) throws RemoteException {
    this.banks.addAll(banks);
  }

  @Override
  public void removeBanks(List<Bank> banks) throws RemoteException {
    this.banks.removeAll(banks);
  }

  @Override
  public String getRegionCode() throws RemoteException {
    return this.regionCode;
  }

  @Override
  public void setRegionCode(String regionCode) throws RemoteException {
    this.regionCode = regionCode;
  }
}