package bum.interfaces;

import java.rmi.RemoteException;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import mapping.MappingObject;

public interface CompanyPartition extends MappingObject {
  public List<DocumentXMLTemplate> getDefaultTemplates();
  public void setDefaultTemplates(List<DocumentXMLTemplate> defaultTemplates);
  
  public Boolean isMainnumberingcontract() throws RemoteException;
  public void setMainnumberingcontract(Boolean mainnumberingcontract) throws RemoteException;
  
  public Boolean isMainnumbering() throws RemoteException;
  public void setMainnumbering(Boolean mainnumbering) throws RemoteException;
  
  public Period getPeriodForZero() throws RemoteException;
  public void setPeriodForZero(Period periodForZero) throws RemoteException;

  public Boolean isGrabFreeNumber() throws RemoteException;
  public void setGrabFreeNumber(Boolean grabFreeNumber) throws RemoteException;
  
  public LocalDate getDocStopDate();
  public void setDocStopDate(LocalDate docStopDate);
  
  public String getPrefix() throws RemoteException;
  public void setPrefix(String prefix) throws RemoteException;

  public String getPrefixSplit() throws RemoteException;
  public void setPrefixSplit(String prefixSplit) throws RemoteException;

  public String getPrefixTypeFormat() throws RemoteException;
  public void setPrefixTypeFormat(String prefixTypeFormat) throws RemoteException;

  public Integer getStartNumber() throws RemoteException;
  public void setStartNumber(Integer startNumber) throws RemoteException;

  public String getSuffix() throws RemoteException;
  public void setSuffix(String suffix) throws RemoteException;

  public String getSuffixSplit() throws RemoteException;
  public void setSuffixSplit(String suffixSplit) throws RemoteException;

  public String getSuffixTypeFormat() throws RemoteException;
  public void setSuffixTypeFormat(String suffixTypeFormat) throws RemoteException;
  
  public String getContactFio() throws RemoteException;
  public void setContactFio(String contactFio) throws RemoteException;
  
  public String getEmail() throws RemoteException;
  public void setEmail(String email) throws RemoteException;
  
  public String getTelefon() throws RemoteException;
  public void setTelefon(String telefon) throws RemoteException;
  
  public String getCompanysExportFileName() throws RemoteException;
  public void setCompanysExportFileName(String companysExportFileName) throws RemoteException;

  public String getCompanysExportPath() throws RemoteException;
  public void setCompanysExportPath(String companysExportPath) throws RemoteException;

  public String getContractsExportFileName() throws RemoteException;
  public void setContractsExportFileName(String contractsExportFileName) throws RemoteException;
  
  public String getContractsExportPath() throws RemoteException;
  public void setContractsExportPath(String contractsExportPath) throws RemoteException;

  public String getDocumentsExportPath() throws RemoteException;
  public void setDocumentsExportPath(String documentsExportPath) throws RemoteException;

  public Boolean isMainPartition() throws RemoteException;
  public void setMainPartition(Boolean mainPartition) throws RemoteException;

  public Company getCompany() throws RemoteException;
  public void setCompany(Company company) throws RemoteException;
  
  //public Account getCurrentAccount() throws RemoteException;
  public List<Account> getAccounts() throws RemoteException;
  public void setAccounts(List<Account> accounts) throws RemoteException;

  public void setKpp(String kpp) throws RemoteException;
  public String getKpp() throws RemoteException;

  public String getUrAddres() throws RemoteException;
  public void setUrAddres(String urAddres) throws RemoteException;

  public void setAddres(String addres) throws RemoteException;
  public String getAddres() throws RemoteException;

  public void setPostAddres(String postAddres) throws RemoteException;
  public String getPostAddres() throws RemoteException;

  public String getContactInfo() throws RemoteException;
  public void setContactInfo(String contactInfo) throws RemoteException;
}