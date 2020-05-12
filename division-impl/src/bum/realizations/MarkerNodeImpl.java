package bum.realizations;

import bum.annotations.Column;
import bum.interfaces.MarkerNode;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

public class MarkerNodeImpl extends MappingObjectImpl implements MarkerNode {
  @Column
  private Integer red;
  @Column
  private Integer green;
  @Column
  private Integer blue;
  @Column
  private Integer[] objectsId;
  @Column
  private String objectClass;
  @Column(length=-1)
  private String text;

  public MarkerNodeImpl() throws RemoteException {
  }

  @Override
  public Integer getBlue() throws RemoteException {
    return blue;
  }

  @Override
  public void setBlue(Integer blue) throws RemoteException {
    this.blue = blue;
  }

  @Override
  public Integer getGreen() throws RemoteException {
    return green;
  }

  @Override
  public void setGreen(Integer green) throws RemoteException {
    this.green = green;
  }

  @Override
  public String getObjectClass() throws RemoteException {
    return objectClass;
  }

  @Override
  public void setObjectClass(String objectClass) throws RemoteException {
    this.objectClass = objectClass;
  }

  @Override
  public Integer[] getObjectsId() throws RemoteException {
    return objectsId;
  }

  @Override
  public void setObjectsId(Integer[] objectsId) throws RemoteException {
    this.objectsId = objectsId;
  }

  @Override
  public Integer getRed() throws RemoteException {
    return red;
  }

  @Override
  public void setRed(Integer red) throws RemoteException {
    this.red = red;
  }

  @Override
  public String getText() throws RemoteException {
    return text;
  }

  @Override
  public void setText(String text) throws RemoteException {
    this.text = text;
  }
}