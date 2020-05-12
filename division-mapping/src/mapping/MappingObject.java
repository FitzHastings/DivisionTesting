package mapping;

import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.Properties;
import net.sf.json.JSONObject;

public interface MappingObject extends/* Remote,*/ Cloneable {
  public Boolean isEditable();
  public void setEditable(Boolean editable);
  
  public String getComments();
  public void setComments(String comments);
  
  public Long getLoadId();
  
  public boolean isUpdate(JSONObject jsonMemento) throws RemoteException;

  public enum Type{PROJECT,CURRENT,ARCHIVE}
  public enum RemoveAction{DELETE,MOVE_TO_ARCHIVE,MARK_FOR_DELETE}
  
  public Class getInterface() throws RemoteException;
  
  public Properties getParams() throws RemoteException;
  public void setParams(Properties params) throws RemoteException;
  
  public Integer getId() throws RemoteException;
  public void setId(Integer id) throws RemoteException;
  
  public String getName() throws RemoteException;
  public void setName(String name) throws RemoteException;
  
  public Type getType() throws RemoteException;
  public void setType(Type type) throws RemoteException;
  
  public Timestamp getDate() throws RemoteException;
  public void setDate(Timestamp date) throws RemoteException;
  
  public Timestamp getModificationDate() throws RemoteException;
  public void setModificationDate(Timestamp modificationDate) throws RemoteException;

  public String getLastUserName() throws RemoteException;
  public void setLastUserName(String lastUserName) throws RemoteException;
  
  public Integer getLastUserId() throws RemoteException;
  public void setLastUserId(Integer lastUserId) throws RemoteException;
  
  public Boolean isTmp() throws RemoteException;
  public void setTmp(Boolean tmp) throws RemoteException;
 
  public void setFixTime(Timestamp fixTime) throws RemoteException;
  public Timestamp getFixTime() throws RemoteException;
  public boolean isFixTime() throws RemoteException;
  public String getRealClassName() throws RemoteException;
}
