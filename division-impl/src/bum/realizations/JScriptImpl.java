package bum.realizations;

import bum.annotations.Column;
import bum.interfaces.JScript;
import java.rmi.RemoteException;

public class JScriptImpl extends JSModulImpl implements JScript {
  @Column(defaultValue = "false",nullable = false)
  private Boolean autoRun;

  @Column(description="Класс источника события")
  private String sourceClass;

  @Column(description="Описания класса источника")
  private String classDescription;

  public JScriptImpl() throws RemoteException {
  }

  @Override
  public Boolean isAutoRun() throws RemoteException {
    return autoRun;
  }

  @Override
  public void setAutoRun(Boolean autoRun) throws RemoteException {
    this.autoRun = autoRun;
  }

  @Override
  public String getSourceClass() throws RemoteException {
    return sourceClass;
  }

  @Override
  public void setSourceClass(String sourceClass) throws RemoteException {
    this.sourceClass = sourceClass;
  }

  @Override
  public String getClassDescription() throws RemoteException {
    return classDescription;
  }

  @Override
  public void setClassDescription(String classDescription) throws RemoteException {
    this.classDescription = classDescription;
  }
}