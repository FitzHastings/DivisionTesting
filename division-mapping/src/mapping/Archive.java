package mapping;

import java.rmi.RemoteException;
import java.util.Map;

public interface Archive extends MappingObject {
  public String getObjectclass() throws RemoteException;
  public void setObjectclass(String objectclass) throws RemoteException;

  public Integer getClassid() throws RemoteException;
  public void setClassid(Integer classid) throws RemoteException;

  public Map<String, Object> getObject() throws RemoteException;
  public void setObject(Map<String, Object> object) throws RemoteException;
}