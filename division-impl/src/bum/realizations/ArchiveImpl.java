package bum.realizations;

import bum.annotations.Column;
import mapping.Archive;
import java.rmi.RemoteException;
import java.util.Map;
import mapping.MappingObjectImpl;

public class ArchiveImpl extends MappingObjectImpl implements Archive {
  @Column
  private String objectclass;
  
  @Column
  private Integer classid;
  
  @Column(gzip = true)
  private Map<String,Object> object;
  
  public ArchiveImpl() throws RemoteException {
    super();
  }

  @Override
  public String getObjectclass() throws RemoteException {
    return objectclass;
  }

  @Override
  public void setObjectclass(String objectclass) throws RemoteException {
    this.objectclass = objectclass;
  }

  @Override
  public Integer getClassid() throws RemoteException {
    return classid;
  }

  @Override
  public void setClassid(Integer classid) throws RemoteException {
    this.classid = classid;
  }

  @Override
  public Map<String, Object> getObject() throws RemoteException {
    return object;
  }

  @Override
  public void setObject(Map<String, Object> object) throws RemoteException {
    this.object = object;
  }
}