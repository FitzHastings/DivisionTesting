package bum.interfaces;

import java.rmi.RemoteException;
import mapping.MappingObject;

public interface ExportImport extends MappingObject {
  public String getImportData() throws RemoteException;
  public void setImportData(String importData) throws RemoteException;
  
  public String getExportData() throws RemoteException;
  public void setExportData(String exportData) throws RemoteException;
  
  public Boolean isScript() throws RemoteException;
  public void setScript(Boolean script) throws RemoteException;

  public String getExportScript() throws RemoteException;
  public void setExportScript(String exportScript) throws RemoteException;

  public String getImportScript() throws RemoteException;
  public void setImportScript(String importScript) throws RemoteException;

  public String getObjectClassName() throws RemoteException;
  public void setObjectClassName(String objectClassName) throws RemoteException;
}