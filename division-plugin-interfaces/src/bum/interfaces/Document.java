package bum.interfaces;

import java.rmi.RemoteException;
import java.time.Period;
import java.util.List;
import mapping.MappingObject;

public interface Document extends MappingObject {
  public enum DocumentDateType{произвольная_дата,начало_выбранных_сделок,конец_выбранных_сделок}
  
  public boolean isActionConfirm() throws RemoteException;
  public void setActionConfirm(boolean actionConfirm) throws RemoteException;
  
  public String getPrefix() throws RemoteException;
  public void setPrefix(String prefix) throws RemoteException;

  public String getPrefixTypeFormat() throws RemoteException;
  public void setPrefixTypeFormat(String prefixTypeFormat) throws RemoteException;

  public String getPrefixSplit() throws RemoteException;
  public void setPrefixSplit(String prefixSplit) throws RemoteException;

  public String getSuffixSplit() throws RemoteException;
  public void setSuffixSplit(String suffixSplit) throws RemoteException;

  public String getSuffixTypeFormat() throws RemoteException;
  public void setSuffixTypeFormat(String suffixTypeFormat) throws RemoteException;

  public String getSuffix() throws RemoteException;
  public void setSuffix(String suffix) throws RemoteException;

  public Period getPeriodForZero() throws RemoteException;
  public void setPeriodForZero(Period periodForZero) throws RemoteException;

  public Integer getStartNumber() throws RemoteException;
  public void setStartNumber(Integer startNumber) throws RemoteException;

  public Boolean isGrabFreeNumber() throws RemoteException;
  public void setGrabFreeNumber(Boolean grabFreeNumber);
  
  public String getScriptLanguage() throws RemoteException;
  public void setScriptLanguage(String scriptLanguage) throws RemoteException;
  
  public Boolean isSystem() throws RemoteException;
  public void setSystem(Boolean system) throws RemoteException;
  
  public Service.Owner getDocumentSource() throws RemoteException;
  public void setDocumentSource(Service.Owner documentSource) throws RemoteException;
  
  public Boolean isNdsPayer() throws RemoteException;
  public void setNdsPayer(Boolean ndsPayer) throws RemoteException;
  
  public Boolean isMovable() throws RemoteException;
  public void setMovable(Boolean movable) throws RemoteException;
  
  public ProductDocument.ActionType getActionType() throws RemoteException;
  public void setActionType(ProductDocument.ActionType actionType) throws RemoteException;
  
  public Boolean isMoneyCash() throws RemoteException;
  public void setMoneyCash(Boolean moneyCash) throws RemoteException;

  public Boolean isMoneyCashLess() throws RemoteException;
  public void setMoneyCashLess(Boolean moneyCashLess) throws RemoteException;

  public Boolean isTmcCash() throws RemoteException;
  public void setTmcCash(Boolean tmcCash) throws RemoteException;

  public Boolean isTmcCashLess() throws RemoteException;
  public void setTmcCashLess(Boolean tmcCashLess) throws RemoteException;
  
  public String getDescription() throws RemoteException;
  public void setDescription(String description) throws RemoteException;

  public List<DocumentXMLTemplate> getTemplates() throws RemoteException;
  public void setTemplates(List<DocumentXMLTemplate> templates) throws RemoteException;

  public void setScript(String script) throws RemoteException;
  public String getScript() throws RemoteException;
}