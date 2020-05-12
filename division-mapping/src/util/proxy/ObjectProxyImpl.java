package util.proxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import mapping.MappingObject;
import mapping.MappingObjectImpl;
import org.apache.commons.lang3.ArrayUtils;
import util.*;

public class ObjectProxyImpl extends UnicastRemoteObject implements ObjectProxy {
  private MappingObject object;
    
  public ObjectProxyImpl(
          int objectPort, 
          RMIClientSocketFactory clientSocketFactory, 
          RMIServerSocketFactory serverSocketFactory, 
          MappingObject object) throws RemoteException {
    super(objectPort, clientSocketFactory, serverSocketFactory);
    this.object  = object;
  }

  
  private Field getField(String fieldName) {
    Field field = null;
    Class clazz = DataBase.get(object).getAnnotatedClass();
    while(clazz != Object.class) {
      try {
        field = clazz.getDeclaredField(fieldName);
      }catch(NoSuchFieldException | SecurityException ex){}
      clazz = clazz.getSuperclass();
    }
    if(field != null)
      field.setAccessible(true);
    return field;
  }
  
  private Method getMethod(String methodName, Class<?>[] parameterTypes) {
    Method method = null;
    Class clazz = DataBase.get(object).getAnnotatedClass();
    while(clazz != Object.class) {
      try {
        method = clazz.getDeclaredMethod(methodName, parameterTypes);
      }catch(NoSuchMethodException ex){}
      clazz = clazz.getSuperclass();
    }
    return method;
  }
    
  @Override
  public Object invoke(
          //RemoteSession session,
          Object proxy, 
          String methodName, 
          Class<?> returnType, 
          Class<?>[] parameterTypes, 
          Object[] args, 
          boolean nonProxy) throws Exception {
    /*if(methodName.equals("isUpdate")) {
      try {
        JSONObject json = session.getJson((MappingObject) proxy);
        return !json.toString().equals(args[0].toString());
      }catch(RemoteException | IllegalArgumentException | IllegalAccessException ex) {
        throw new RemoteException(ex.getMessage());
      }
    }*/
      
    Method method = getMethod(methodName, parameterTypes);
    if(!nonProxy) {
      DBTable table = DataBase.get(object);
      String className = table.getAnnotatedClass().getName();

      String fieldName = methodName.substring(3);
      fieldName = fieldName.substring(0, 1).toLowerCase()+fieldName.substring(1);

      for(DBRelation relation:table.getRelations()) {
        if(methodName.equals(relation.getSetMethod())) {
          //SET
          Field field = getField(fieldName);
          if(field != null) {
            try {
              if(relation instanceof ManyToOne) {
                method.invoke(object, args);
              }else {
                Session session = ((MappingObjectImpl)object).createSession(true);
                if(object.getId() == null)
                  session.saveObject((MappingObject) proxy);
                
                Integer[] ids = new Integer[0];
                for(Object obj:(List)args[0])
                  ids = (Integer[]) ArrayUtils.add(ids, ((MappingObject)obj).getId());
                if(relation instanceof OneToMany) {
                  session.executeUpdate("UPDATE ["+className+"("+fieldName+"):table] SET ["+className+"("+fieldName+"):object]=NULL "
                          + "WHERE ["+className+"("+fieldName+"):object]=?", new Object[]{object.getId()});
                  if(ids.length > 0)
                    session.executeUpdate("UPDATE ["+className+"("+fieldName+"):table] SET ["+className+"("+fieldName+"):object]=? "
                            + "WHERE ["+className+"("+fieldName+"):target]=ANY(?)", new Object[]{object.getId(),ids});
                }else if(relation instanceof ManyToMany) {
                  session.executeUpdate("DELETE FROM ["+className+"("+fieldName+"):table] "
                          + "WHERE ["+className+"("+fieldName+"):object]=?", new Object[]{object.getId()});
                  if(ids.length > 0) {
                    for(Integer id:ids) {
                      session.executeUpdate("INSERT INTO ["+className+"("+fieldName+"):table]"
                              + "(["+className+"("+fieldName+"):object],["+className+"("+fieldName+"):target]) "
                              + "VALUES(?,?)", new Object[]{object.getId(),id});
                    }
                  }
                }
              }
              //session.commit();
            }catch(Exception ex) {
              //session.rollback();
              throw new RemoteException(ex.getMessage());
            }
          }
          break;
        } else if(methodName.equals(relation.getGetMethod())) {
          //GET
          Field field = getField(fieldName);
          if(field != null) {
            try {
              Session session = ((MappingObjectImpl)object).createSession(true);
              if(relation instanceof ManyToOne) {
                List<List> data = session.executeQuery("SELECT ["+className+"("+fieldName+")] FROM ["+className+"] "
                        + "WHERE id=? ORDER BY ["+className+"("+fieldName+")]", new Object[]{object.getId()});
                if(!data.isEmpty()) {
                  MappingObject value = session.getObject(relation.getTargetClass(), (Integer) data.get(0).get(0));
                  field.set(object, value);
                }
              }else if(relation instanceof OneToMany || relation instanceof ManyToMany) {
                List<List> data = session.executeQuery("SELECT ["+className+"("+fieldName+"):target] FROM ["+className+"("+fieldName+"):table]  "
                        + "WHERE ["+className+"("+fieldName+"):object]=? ORDER BY ["+className+"("+fieldName+"):target]", new Object[]{object.getId()});
                if(!data.isEmpty()) {
                  Integer[] ids = new Integer[0];
                  for(List d:data)
                    ids = (Integer[]) ArrayUtils.add(ids, d.get(0));
                  MappingObject[] values = session.getObjects(relation.getTargetClass(), ids);
                  field.set(object, Arrays.asList(values));
                }
              }
            }catch(RemoteException | IllegalArgumentException | IllegalAccessException ex) {
              throw new RemoteException(ex.getMessage());
            }
          }
          break;
        }
      }
    }
    
    Object returnObject = method.invoke(object, args);
    method = null;
    return returnObject;
  }
}