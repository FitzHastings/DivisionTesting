package bum.interfaces;

import java.rmi.RemoteException;
import mapping.MappingObject;

public interface JSModul extends MappingObject {
  public String getScriptLanguage() throws RemoteException;
  public void setScriptLanguage(String scriptLanguage) throws RemoteException;
  
  public void setScript(String script) throws RemoteException;
  public String getScript() throws RemoteException;
}