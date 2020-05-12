package bum.realizations;

import bum.annotations.Column;
import bum.interfaces.ExportImport;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

public class ExportImportImpl extends MappingObjectImpl implements ExportImport {
  @Column(unique=true)
  private String objectClassName;
  
  @Column(length=-1)
  private String exportData;
  
  @Column(length=-1)
  private String importData;
  
  @Column(length=1000)
  private String exportScript;
  
  @Column(length=1000)
  private String importScript;
  
  @Column(defaultValue="false")
  private Boolean script = false;
  
  public ExportImportImpl() throws RemoteException {
  }

  @Override
  public String getImportData() throws RemoteException {
    return importData;
  }

  @Override
  public void setImportData(String importData) throws RemoteException {
    this.importData = importData;
  }

  @Override
  public String getExportData() throws RemoteException {
    return exportData;
  }

  @Override
  public void setExportData(String exportData) throws RemoteException {
    this.exportData = exportData;
  }

  @Override
  public Boolean isScript() throws RemoteException {
    return script;
  }

  @Override
  public void setScript(Boolean script) throws RemoteException {
    this.script = script;
  }

  @Override
  public String getExportScript() throws RemoteException {
    return exportScript;
  }

  @Override
  public void setExportScript(String exportScript) throws RemoteException {
    this.exportScript = exportScript;
  }

  @Override
  public String getImportScript() throws RemoteException {
    return importScript;
  }

  @Override
  public void setImportScript(String importScript) throws RemoteException {
    this.importScript = importScript;
  }

  @Override
  public String getObjectClassName() throws RemoteException {
    return objectClassName;
  }

  @Override
  public void setObjectClassName(String objectClassName) throws RemoteException {
    this.objectClassName = objectClassName;
  }
}