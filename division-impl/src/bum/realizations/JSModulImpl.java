package bum.realizations;

import bum.annotations.Column;
import bum.interfaces.JSModul;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

public class JSModulImpl extends MappingObjectImpl implements JSModul {
  @Column(length=1000)
  private String script;
  
  @Column
  private String scriptLanguage;

  public JSModulImpl() throws RemoteException {
  }
  
  @Override
  public String getScriptLanguage() throws RemoteException {
    return scriptLanguage;
  }
  
  @Override
  public void setScriptLanguage(String scriptLanguage) throws RemoteException {
    this.scriptLanguage = scriptLanguage;
  }

  @Override
  public void setScript(String script) throws RemoteException {
    this.script = script;
  }

  @Override
  public String getScript() throws RemoteException {
    return this.script;
  }
}