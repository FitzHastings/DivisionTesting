package bum.editors.util;

import division.fx.PropertyMap;
import division.fx.editor.Disposable;
import division.fx.gui.FXDisposable;
import division.fx.util.MsgTrash;
import division.util.IDStore;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.TopicSubscriber;
import mapping.MappingObject;
import org.apache.commons.lang3.ArrayUtils;

public abstract class DivisionTarget implements FXDisposable {
  private Long id;
  private Long listenerId;
  private Integer objectId;
  private Class<? extends MappingObject> objectClass;
  private TopicSubscriber subscriber;
  private ObservableList<FXDisposable> disposeList = FXCollections.observableArrayList();
  
  private final MessageListener target = (javax.jms.Message message) -> {
    try {
      
      Map<String,Object> objectEventProperty = (Map<String,Object>) ((ObjectMessage)message).getObjectProperty("data");
      Integer[] ids = (Integer[]) ((ObjectMessage)message).getObject();
      if(objectId == null || ArrayUtils.contains(ids, objectId)) {
        //if(objectEventProperty != null)
          messageReceived(message.getJMSType(), ids, objectEventProperty == null ? null : PropertyMap.copy(objectEventProperty));
        //else messageReceived(message.getJMSType(), ids);
      }
    }catch(JMSException ex) {
      MsgTrash.out(ex);
    }
  };
  
  public DivisionTarget(MappingObject object) throws RemoteException {
    this(object.getInterface(), object.getId());
  }
  
  public DivisionTarget(Class<? extends MappingObject> objectClass) {
    this(objectClass, null);
  }

  public DivisionTarget(Class<? extends MappingObject> objectClass, Integer id) {
    this.objectId = id;
    this.objectClass = objectClass;
    this.id = IDStore.createID();
    setSubscriber();
  }

  public Class<? extends MappingObject> getObjectClass() {
    return objectClass;
  }

  public Long getId() {
    return id;
  }

  public Long getListenerId() {
    return listenerId;
  }

  public void setListenerId(Long listenerId) {
    this.listenerId = listenerId;
  }
  
  public void setSubscriber() {
    subscriber = ObjectLoader.addSubscriber(objectClass, target);
    System.out.println("INSTALL TARGET ["+id+"] "+objectClass.getName());
  }
  
  public void reload() {
    dispose();
    setSubscriber();
  }

  public TopicSubscriber getSubscriber() {
    return subscriber;
  }

  public Integer getObjectId() {
    return objectId;
  }

  public void messageReceived(String type, Integer[] ids, PropertyMap objectEventProperty) {
  }
  
  public static DivisionTarget create(FXDisposable disposable, MappingObject object, DivisionTargetListener listener) throws RemoteException {
    return create(disposable, object.getInterface(), object.getId(), listener);
  }
  
  public static DivisionTarget create(MappingObject object, DivisionTargetListener listener) throws RemoteException {
    return create(object.getInterface(), object.getId(), listener);
  }
  
  public static DivisionTarget create(FXDisposable disposable, Class<? extends MappingObject> objectClass, DivisionTargetListener listener) {
    return create(disposable, objectClass, null, listener);
  }
  
  public static DivisionTarget create(Class<? extends MappingObject> objectClass, DivisionTargetListener listener) {
    return create(objectClass, null, listener);
  }
  
  public static DivisionTarget create(Class<? extends MappingObject> objectClass, Integer id, DivisionTargetListener listener) {
    return create(null, objectClass, id, listener);
  }
  
  public static DivisionTarget create(FXDisposable disposable, Class<? extends MappingObject> objectClass, Integer id, DivisionTargetListener listener) {
    DivisionTarget target = new DivisionTarget(objectClass, id) {
      @Override
      public void messageReceived(String type, Integer[] ids, PropertyMap objectEventProperty) {
        listener.messageReceived(this, type, ids, objectEventProperty);
      }
    };
    if(disposable != null)
      disposable.disposeList().add(target);
    //target.setTargetListener(listener);
    return target;
  }
  
  @Override
  public void finaly() {
    if(getSubscriber() != null) {
      try {
        getSubscriber().close();
        System.out.println("DISPOSE TARGET ["+id+"] "+objectClass.getName());
      }catch(JMSException ex) {}
    }
  }
  
  @Override
  public List<FXDisposable> disposeList() {
    return disposeList;
  }
  
  public interface DivisionTargetListener {
    public void messageReceived(DivisionTarget target, String type, Integer[] ids, PropertyMap data);
  }
}