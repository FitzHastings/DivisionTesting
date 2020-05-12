package bum.interfaces;

import java.rmi.RemoteException;
import mapping.MappingObject;

public interface CompanyPartitionDocument extends MappingObject {
  public enum Period{день,месяц,год}
  
  public Document.DocumentDateType getDateType() throws RemoteException;
  public void setDateType(Document.DocumentDateType dateType) throws RemoteException;
  
  public Boolean isGrabFreeNumber() throws RemoteException;
  public void setGrabFreeNumber(Boolean grabFreeNumber) throws RemoteException;
  
  public String getPrefixTypeFormat() throws RemoteException;
  public void setPrefixTypeFormat(String prefixTypeFormat) throws RemoteException;
  
  public String getSuffixTypeFormat() throws RemoteException;
  public void setSuffixTypeFormat(String suffixTypeFormat) throws RemoteException;
  
  public Document getDocument() throws RemoteException;
  public void setDocument(Document document) throws RemoteException;

  public CompanyPartition getPartition() throws RemoteException;
  public void setPartition(CompanyPartition partition) throws RemoteException;

  public Period getPeriodForZero() throws RemoteException;
  public void setPeriodForZero(Period periodForZero) throws RemoteException;

  public String getPrefix() throws RemoteException;
  public void setPrefix(String prefix) throws RemoteException;

  public String getPrefixSplit() throws RemoteException;
  public void setPrefixSplit(String prefixSplit) throws RemoteException;

  public Integer getStartNumber() throws RemoteException;
  public void setStartNumber(Integer startNumber) throws RemoteException;

  public String getSuffix() throws RemoteException;
  public void setSuffix(String suffix) throws RemoteException;

  public String getSuffixSplit() throws RemoteException;
  public void setSuffixSplit(String suffixSplit) throws RemoteException;
}