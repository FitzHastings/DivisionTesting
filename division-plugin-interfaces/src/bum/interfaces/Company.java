package bum.interfaces;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.List;
import mapping.MappingObject;

public interface Company extends MappingObject {
  public byte[] getLogo() throws RemoteException;
  public void setLogo(byte[] logo) throws RemoteException;
  
  public byte[] getStamp() throws RemoteException;
  public void setStamp(byte[] stamp) throws RemoteException;

  public byte[] getChifSignature() throws RemoteException;
  public void setChifSignature(byte[] chifSignature) throws RemoteException;

  public byte[] getBookkeeperSignature() throws RemoteException;
  public void setBookkeeperSignature(byte[] bookkeeperSignature) throws RemoteException;
  
  public Boolean isNdsPayer() throws RemoteException;
  public void setNdsPayer(Boolean ndsPayer) throws RemoteException;
  
  public String getShotName() throws RemoteException;
  public void setShotName(String shotName) throws RemoteException;
  
  public BigDecimal getDefaultNds() throws RemoteException;
  public void setDefaultNds(BigDecimal defaultNds) throws RemoteException;
  
  public String getBookkeeper() throws RemoteException;
  public void setBookkeeper(String bookkeeper) throws RemoteException;

  public String getChiefName() throws RemoteException;
  public void setChiefName(String chiefName) throws RemoteException;

  public Place getChiefPlace() throws RemoteException;
  public void setChiefPlace(Place chiefPlace) throws RemoteException;
  
  public Place getSecondPerson() throws RemoteException;
  public void setSecondPerson(Place secondPerson) throws RemoteException;

  public String getOgrn() throws RemoteException;
  public void setOgrn(String ogrn) throws RemoteException;

  public String getOkved() throws RemoteException;
  public void setOkved(String okved) throws RemoteException;

  //public CompanyPartition getMainCompanyPartition() throws RemoteException;
  public List<CompanyPartition> getCompanyPartitions() throws RemoteException;
  public void setCompanyPartitions(List<CompanyPartition> companyPartitions) throws RemoteException;

  public OwnershipType getOwnershipType() throws RemoteException;
  public void setOwnershipType(OwnershipType ownershipType) throws RemoteException;
  
  public String getBusinessReason() throws RemoteException;
  public void setBusinessReason(String businessReason) throws RemoteException;
  
  public String getBusinessPerson() throws RemoteException;
  public void setBusinessPerson(String businessPerson) throws RemoteException;
  
  public String getInn() throws RemoteException;
  public void setInn(String inn) throws RemoteException;
  
  public List<CFC> getCfcs() throws RemoteException;
  public void setCfcs(List<CFC> cfcs) throws RemoteException;
  public void addCfcs(List<CFC> cfcs) throws RemoteException;
  public void removeCfcs(List<CFC> cfcs) throws RemoteException;
}