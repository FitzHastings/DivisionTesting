package bum.realizations;

import bum.annotations.Column;
import bum.annotations.Table;
import bum.interfaces.XMLTemplate;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

@Table(clientName="Шаблоны")
public class XMLTemplateImpl extends MappingObjectImpl implements XMLTemplate {
  
  @Column(length=-1)
  private String XML;
  
  @Column
  private String description;
  
  @Column
  private String objectClassName;
  
  @Column(name="group_temp")
  private Boolean group = false;

  public XMLTemplateImpl() throws RemoteException {
    super();
  }

  @Override
  public String getXML() throws RemoteException {
    return this.XML;
  }

  @Override
  public void setXML(String xml) throws RemoteException {
    this.XML = xml;
  }

  @Override
  public String getObjectClassName() throws RemoteException {
    return this.objectClassName;
  }

  @Override
  public void setObjectClassName(String objectClassName) throws RemoteException {
    this.objectClassName = objectClassName;
  }

  @Override
  public Boolean isGroup() throws RemoteException {
    return this.group;
  }

  @Override
  public void setGroup(Boolean group) throws RemoteException {
    this.group = group;
  }

  @Override
  public String getDescription() throws RemoteException {
    return this.description;
  }

  @Override
  public void setDescription(String description) throws RemoteException {
    this.description = description;
  }
}
