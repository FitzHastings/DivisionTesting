package mapping;

import bum.annotations.Column;
import bum.annotations.Id;
import java.lang.reflect.InvocationHandler;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import util.DataBase;
import util.Session;
import util.Session.SessionFactory;

public class MappingObjectImpl /*extends UnicastRemoteObject*/ implements MappingObject {
  private Long loadId;
  /**
   * Идентификатор объекта
   */
  @Id
  private Integer       id;
  /**
   * Наименование
   */
  @Column(description="Наименование",index=true,length=-1)
  private String        name;
  
  @Column(nullable=false,description="Тип {проектый,текущий,архивный}",index=true,defaultValue="CURRENT")
  private MappingObject.Type type = MappingObject.Type.CURRENT;
  /**
   * Дата и время создания объекта
   */
  @Column(nullable=false,defaultValue="CURRENT_TIMESTAMP",description="Дата создания",index=true)
  private Timestamp     date;
  /**
   * Дата и время последнего изменения объекта
   */
  @Column(nullable=false,defaultValue="CURRENT_TIMESTAMP",description="Дата последней модификации")
  private Timestamp     modificationDate;
  /**
   * Имя пользователя, который произвёл последнее изменение.
   */
  @Column
  private String lastUserName;
  /**
   * Идентификатор пользователя, который произвёл последнее изменение.
   */
  @Column
  private Integer lastUserId;
  /**
   * Показатель временности, т.е.
   * если значение true, то объкт временный и
   * удалиться при перезапуске сервера
   */
  @Column(index = true, defaultValue = "false", nullable = false)
  private Boolean       tmp     = false;
  /**
   * Дата и время фиксации объекта, это означает что
   * все объекты на которые он ссылается должны быть
   * восстановленны из таблицы истории с modificationDate
   * более ранней чем fixTime, но самой ближней к fixTime.
   */
  @Column
  private Timestamp fixTime = null;
  
  @Column
  private Properties params = new Properties();
  
  private SessionFactory sessionFactory;
  
  @Column(description="Комментарии", index=true, length=-1)
  private String comments;
  
  @Column
  private Boolean editable = true;
  
  public MappingObjectImpl() throws RemoteException {
    super();
  }

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }
  
  public Session createSession() throws RemoteException {
    return createSession(false);
  }
  
  public Session createSession(boolean autoCommit) throws RemoteException {
    return sessionFactory==null?null:sessionFactory.createSession(autoCommit);
  }

  @Override
  public Boolean isEditable() {
    return editable;
  }

  @Override
  public void setEditable(Boolean editable) {
    this.editable = editable;
  }

  @Override
  public String getComments() {
    return comments;
  }

  @Override
  public void setComments(String comments) {
    this.comments = comments;
  }

  @Override
  public Long getLoadId() {
    return loadId;
  }

  public void setLoadId(Long loadId) {
    this.loadId = loadId;
  }
  
  @Override
  public Class getInterface() throws RemoteException {
    return DataBase.getClasses().get(getClass());
  }
  
  @Override
  public Properties getParams() throws RemoteException {
    return params;
  }

  @Override
  public void setParams(Properties params) throws RemoteException {
    this.params = params;
  }
  
  @Override
  public MappingObject.Type getType() throws RemoteException {
    return type;
  }

  @Override
  public void setType(MappingObject.Type type) throws RemoteException {
    this.type = type;
  }

  @Override
  public boolean isFixTime() throws RemoteException {
    java.sql.Date dNow = new java.sql.Date(new Date().getTime());
    return fixTime == null?false:fixTime.equals(dNow)||fixTime.before(dNow);
  }
  
  @Override
  public Timestamp getFixTime() throws RemoteException {
    return this.fixTime;
  }

  @Override
  public void setFixTime(Timestamp fixTime) throws RemoteException {
    this.fixTime = fixTime;
  }
  
  @Override
  public Timestamp getModificationDate() throws RemoteException {
    return modificationDate;
  }

  @Override
  public void setModificationDate(Timestamp modificationDate) throws RemoteException {
    this.modificationDate = modificationDate;
  }

  @Override
  public String getLastUserName() throws RemoteException {
    return lastUserName;
  }

  @Override
  public void setLastUserName(String lastUserName) throws RemoteException {
    this.lastUserName = lastUserName;
  }

  @Override
  public Integer getLastUserId() throws RemoteException {
    return lastUserId;
  }

  @Override
  public void setLastUserId(Integer lastUserId) throws RemoteException {
    this.lastUserId = lastUserId;
  }
  
  @Override
  public Integer getId() throws RemoteException {
    return this.id;
  }
  
  @Override
  public void setId(Integer id) throws RemoteException {
    this.id = id;
  }
  
  @Override
  public String getName() throws RemoteException {
    return this.name;
  }
  
  @Override
  public void setName(String name) throws RemoteException {
    this.name = name;
  }
  
  @Override
  public Timestamp getDate() throws RemoteException {
    return this.date;
  }
  
  @Override
  public void setDate(Timestamp date) throws RemoteException {
    this.date = date;
  }
  
  @Override
  public Boolean isTmp() throws RemoteException {
    return this.tmp;
  }
  
  @Override
  public void setTmp(Boolean tmp) throws RemoteException {
    this.tmp = tmp;
  }
  
  protected boolean beforeCreateInTransaction() throws RemoteException {
    return true;
  }

  protected boolean afterCreateInTransaction() throws RemoteException {
    return true;
  }

  protected boolean beforeSaveInTransaction() throws RemoteException {
    return true;
  }

  protected boolean afterSaveInTransaction() throws RemoteException {
    return true;
  }

  protected boolean beforeRemoveInTransaction() throws RemoteException {
    return true;
  }

  protected boolean afterRemoveInTransaction() throws RemoteException {
    return true;
  }
  
  public void beforeCreate() throws RemoteException {
  }

  public void afterCreate() throws RemoteException {
  }

  protected void beforeSave() throws RemoteException {
  }

  protected void afterSave() throws RemoteException {
  }

  protected void beforeRemove() throws RemoteException {
  }

  protected void afterRemove() throws RemoteException {
  }
  
  @Override
  public boolean equals(Object obj) {
    if(obj instanceof InvocationHandler)
      return obj.equals(this);
    if(obj == null || !(obj instanceof MappingObject))
      return false;
    MappingObject other = (MappingObject)obj;
    try {
      if(!this.getRealClassName().equals(other.getRealClassName()))
        return false;

      if(this.getId() == null || other.getId() == null)
        return false;

      if(this.getId().intValue() != other.getId().intValue())
        return false;

    }catch(RemoteException ex) {
      Logger.getRootLogger().error("", ex);
      return false;
    }
    return true;
  }
  
  @Override
  public String getRealClassName() throws RemoteException {
    return getClass().getName();
  }

  @Override
  public boolean isUpdate(JSONObject jsonMemento) {
    return false;
  }
}