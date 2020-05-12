package bum.editors.util;

import division.util.JMSUtil;
import bum.interfaces.Server;
import conf.P;
import division.fx.PropertyMap;
import division.swing.guimessanger.Messanger;
import division.util.GzipUtil;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javax.jms.*;
import mapping.MappingObject;
import net.sf.json.JSONObject;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import util.Client;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class ObjectLoader {
  private static List<RemoteSession> sessions = new ArrayList<>();
  
  private static ExecutorService pool = Executors.newCachedThreadPool();
  
  private static Server server;
  private static Client client;
  
  public static void setparams(String pkey, JSONObject config) {
    for(Object key:config.keySet()) {
      String prop = pkey+(pkey.equals("") ? "" : ".")+key;
      Object val = config.get(key);
      if(val instanceof JSONObject)
        setparams(prop, (JSONObject)val);
      else {
        System.getProperties().put(prop, val);
        System.out.println(prop+" = "+val+" "+val.getClass().getSimpleName());
      }
    }
  }

  public static void initPeopleMessanger(MessageListener listener) {
    try {
      JMSUtil.createQueue(client.getPeopleId().toString(), listener);

      addSubscriber("people-messanger", listener);

      javax.jms.Message m = new ActiveMQMessage();
      m.setJMSType("ONLINE");
      m.setIntProperty("people", client.getPeopleId());
      m.setStringProperty("topic.name", "people-messanger");
      sendMessage(m);

      m = new ActiveMQMessage();
      m.setJMSType("ANYBODY-IS-ONLINE");
      m.setStringProperty("topic.name", "people-messanger");
      sendMessage(m);
    }catch(Exception ex) {
      Logger.getRootLogger().error(ex);
      initPeopleMessanger(listener);
    }
  }

  private static void showConfig() {
  }
  
  private ObjectLoader() {
  }

  public static void clear() {
    client = null;
    server = null;
  }

  public static void connect() {
    initServer();
  }
  
  public static long sendMessageToPeople(Integer peopleId, String text) {
    try {
      ActiveMQMessage m = new ActiveMQMessage();
      //javax.jms.Message m = queueSession.createTextMessage(text);
      m.setJMSType("TEXT-MESSAGE");
      m.setIntProperty("people", getClient().getPeopleId());
      m.setStringProperty("TEXT", text);
      m.setJMSTimestamp(System.currentTimeMillis());
      JMSUtil.sendQueneMessage(peopleId.toString(), m);
      return m.getJMSTimestamp();
    }catch(Exception ex) {
      Logger.getRootLogger().error(ex);
      sendMessageToPeople(peopleId, text);
    }
    return 0;
  }
  
  public static void sendMessage(Class<? extends MappingObject> objectClass, String type, Integer id) {
    sendMessage(objectClass, type, new Integer[]{id});
  }
  
  public static void sendMessage(Class<? extends MappingObject> objectClass, String type, Integer[] ids) {
    JMSUtil.sendTopicMessage(objectClass.getName(), type, ids);
  }
  
  public static void sendMessage(javax.jms.Message message) {
    JMSUtil.sendTopicMessage(message);
  }
  
  public static void removeSubscriber(String name) {
    JMSUtil.removeTopicSubscriber(name);
  }
  
  public static void removeSubscriber(TopicSubscriber subscriber) {
    try {
      removeSubscriber(subscriber.getTopic().getTopicName());
    }catch(JMSException ex) {
      Logger.getRootLogger().error(ex);
    }
  }
  
  public static TopicSubscriber addSubscriber(Class<? extends MappingObject> objectClass, MessageListener target) {
    return addSubscriber(objectClass.getName(), target);
  }
  
  public static TopicSubscriber addSubscriber(MappingObject object, MessageListener target) throws RemoteException {
    return addSubscriber(object.getInterface().getName(), target);
  }
  
  public static TopicSubscriber addSubscriber(String source, MessageListener target) {
    return JMSUtil.addTopicSubscriber(source, target);
  }

  private static void initServer(/*JSONObject configServer*/) {
    if(server == null) {
      try {
        if((!P.contains("server.name") || !P.contains("server.host") || !P.contains("server.port") || !P.contains("server.timeout") || !P.contains("server.connection-count")) && 
                new Alert(Alert.AlertType.CONFIRMATION, "Отсутствуют настройки сервера. Перейти к настройкам подключения?", ButtonType.YES, ButtonType.NO).showAndWait().get() == ButtonType.YES)
          showConfig();
        String name            = P.String("server.name");
        String host            = P.String("server.host");
        int    port            = P.Integer("server.port");
        int    timeout         = P.Integer("server.timeout");
        int    connectionCount = P.Integer("server.connection-count");
        while(connectionCount > 0) {
          try {
            server = (Server) LocateRegistry.getRegistry(host, port).lookup(name);
            Logger.getRootLogger().info("CONNECTED TO RMI "+name+" "+host+":"+port);
            return;
          }catch(RemoteException | NotBoundException ex) {
            Logger.getRootLogger().error("CAN NOT CONNECTED TO RMI "+name+" "+host+":"+port, ex);
            Thread.sleep(timeout*1000);
          }finally {
            connectionCount--;
          }
          Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Соединение с сервером "+host+":"+port+"/"+name+" отсутствует. Перейти к настройкам подключения?", ButtonType.YES, ButtonType.NO);
          if(a.showAndWait().get() == ButtonType.YES)
            showConfig();
        }
      }catch(Exception ex) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Неправильные настройки сервера. Перейти к настройкам подключения?", ButtonType.YES, ButtonType.NO);
        if(a.showAndWait().get() == ButtonType.YES)
          showConfig();
      }
    }
  }
  
  public static void offLine() {
    try {
      JMSUtil.sendTopicMessage("client-system-command-topic", "offline", client);
    }catch(Exception ex){}
  }
  
  public static void registrateClient(Client client) throws RemoteException {
    ObjectLoader.client = server.registrateClient(client);
    JMSUtil.addTopicSubscriber("server-system-command-topic", (Message msg) -> {
      try {
        if(msg.getJMSType().equals("online-test"))
          JMSUtil.sendTopicMessage("client-system-command-topic", msg.getJMSType(), ObjectLoader.client);
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    });
  }
  
  public static void dispose() {
    sessions.stream().forEach(s -> {
      try{s.rollback();}catch(Exception ex){}
    });
    sessions.clear();
    offLine();
    JMSUtil.dispose();
    System.exit(0);
  }

  public static Server getServer() {
    return server;
  }
  
  public static Map<String, Map<String, Object>> getFieldsInfo(Class clazz) throws RemoteException {
    return getServer().getFieldsInfo(clazz);
  }
  
  public static String getClientName(Class<? extends MappingObject> clazz) throws RemoteException {
    return getServer().getClientName(clazz);
  }
  
  public static RemoteSession createSession() {
    return createSession(false);
  }
  
  public static RemoteSession createSession(boolean autocommit) {
    try {
      return createSession(autocommit, false);
    }catch (Exception ex) {Messanger.showErrorMessage(ex);}
    return null;
  }
  
  public static RemoteSession createSession(boolean autocommit, boolean isThrown) throws Exception {
    try {
      RemoteSession session = server.createSession(client, autocommit);
      if(!autocommit)
        sessions.add(session);
      return session;
    }catch(Exception ex) {
      if(isThrown)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return null;
  }
  
  public static MappingObject createEmptyObject(Class<? extends MappingObject> objectClass) {
    try {
      return createEmptyObject(objectClass, false);
    }catch(Exception ex) {Messanger.showErrorMessage(ex);}
    return null;
  }
  
  public static MappingObject createEmptyObject(Class<? extends MappingObject> objectClass, boolean isThrown) throws Exception {
    try {
      return createSession(true).createEmptyObject(objectClass);
    }catch(Exception ex) {
      if(isThrown)
        throw new Exception(ex);
    }
    return null;
  }
  
  public static void rollBackSession(RemoteSession session) {
    if(session != null) {
      try {
        if(!session.isClosed()) {
          session.rollback();
          System.out.println("ROLLBACK");
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }
  
  public static void commitSession(RemoteSession session) {
    if(session != null) {
      try {
        if(!session.isClosed())
          session.commit();
      }catch(Exception ex) {
        rollBackSession(session);
        Messanger.showErrorMessage(ex);
      }
    }
  }
  
  public static boolean isSatisfy(DBFilter filter, MappingObject object) {
    try {return isSatisfy(filter, object.getId(), false);} catch (Exception ex) {}
    return false;
  }
  
  public static boolean isSatisfy(DBFilter filter, MappingObject object, boolean isThrownException) throws Exception {
    boolean isSatisfy = false;
    try {
      isSatisfy = createSession(true).isSatisfy(filter, object.getId());
    } catch (Exception ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return isSatisfy;
  }
  
  public static boolean isSatisfy(DBFilter filter, Integer id) {
    try {return isSatisfy(filter, id, false);} catch (Exception ex) {}
    return false;
  }
  
  public static boolean isSatisfy(DBFilter filter, Integer id, boolean isThrownException) throws Exception {
    boolean isSatisfy = false;
    try {
      isSatisfy = createSession(true).isSatisfy(filter, id);
    } catch (Exception ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return isSatisfy;
  }
  
  public static Integer[] isSatisfy(DBFilter filter, Integer[] ids) {
    try {return isSatisfy(filter, ids, false);} catch (Exception ex) {}
    return new Integer[0];
  }
  
  public static Integer[] isSatisfy(DBFilter filter, Integer[] ids, boolean isThrownException) throws Exception {
    Integer[] isSatisfy = new Integer[0];
    try {
      isSatisfy = createSession(true).isSatisfy(filter, ids);
    } catch (Exception ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return isSatisfy;
  }
  
  public static List<List> executeQuery(String query) {
    try {return executeQuery(query, false, new Object[0]);}catch(Exception ex) {}
    return new Vector<>();
  }
  
  public static List<List> executeQuery(String query, boolean isThrownException) {
    try {return executeQuery(query, isThrownException, new Object[0]);}catch(Exception ex) {}
    return new Vector<>();
  }
  
  public static List<List> executeQuery(String query, Object... params) {
    try {return executeQuery(query, false, params);}catch (Exception ex) {}
    return new Vector<>();
  }
  
  public static List<List> executeQuery(String query, boolean isThrownException, Object... params) throws Exception {
    List<List> data = new Vector<>();
    try {
      data = (List<List>) GzipUtil.getObjectFromGzip(createSession(true).executeGzipQuery(query, params));
    }catch (Exception ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return data;
  }
  
  public static List<List<List>> executeQuery(String[] querys, Object[][] arrParams) {
    try {return executeQuery(querys, arrParams, false);}catch (Exception ex) {}
    return new ArrayList<>();
  }
  
  public static List<List<List>> executeQuery(String[] querys, Object[][] arrParams, boolean isThrownException) throws Exception {
    List<List<List>> data = new ArrayList<>();
    try {
      data = Arrays.asList((List<List>[]) GzipUtil.getObjectFromGzip(createSession(true).executeGzipQuery(querys, arrParams)));
    }catch (Exception ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return data;
  }
  
  public static int executeUpdate(String query) {
    return executeUpdate(query, new Object[0]);
  }
  
  public static int executeUpdate(String query, boolean isThrownException) throws Exception {
    return executeUpdate(query, isThrownException, new Object[0]);
  }
  
  public static int executeUpdate(String query, Object... params) {
    try {return executeUpdate(query, false, params);} catch (Exception ex) {}
    return 0;
  }
  
  public static int executeUpdate(String query, boolean isThrownException, Object[] params) throws Exception {
    int returnInt = 0;
    try {
      returnInt = createSession(true).executeUpdate(query, params);
    } catch (Exception ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return returnInt;
  }
  
  public static int executeUpdate(Class<? extends MappingObject> objectClass, String param, Object value, Integer id) {
    return executeUpdate(objectClass, new String[]{param}, new Object[]{value}, new Integer[]{id});
  }
  
  public static int executeUpdate(Class<? extends MappingObject> objectClass, String param, Object value, Integer[] ids) {
    return executeUpdate(objectClass, new String[]{param}, new Object[]{value}, ids);
  }
  
  public static int executeUpdate(Class<? extends MappingObject> objectClass, String param, Object value, ObservableList<PropertyMap> ps) {
    List<Integer> ids = new ArrayList<>();
    ps.stream().forEach(p -> ids.add(p.getValue("id", Integer.TYPE)));
    return executeUpdate(objectClass, new String[]{param}, new Object[]{value}, ids.toArray(new Integer[0]));
  }
  
  public static int executeUpdate(Class<? extends MappingObject> objectClass, List params, List values, Integer id) {
    return executeUpdate(objectClass, params, values, new Integer[]{id});
  }
  
  public static int executeUpdate(Class<? extends MappingObject> objectClass, List params, List values, Integer[] ids) {
    return executeUpdate(objectClass, (String[])params.toArray(new String[0]), values.toArray(), ids);
  }
  
  public static int executeUpdate(Class<? extends MappingObject> objectClass, String[] params, Object[] values, Integer id) {
    return executeUpdate(objectClass, params, values, new Integer[]{id});
  }
  
  public static int executeUpdate(Class<? extends MappingObject> objectClass, String[] params, Object[] values, Integer[] ids) {
    try {return executeUpdate(objectClass, params, values, ids, false);} catch (Exception ex) {}
    return 0;
  }
  
  public static int executeUpdate(Class<? extends MappingObject> objectClass, List params, List values, Integer id, boolean isThrownException) throws Exception {
    return executeUpdate(objectClass, (String[])params.toArray(new String[0]), values.toArray(), new Integer[]{id}, isThrownException);
  }
  
  public static int executeUpdate(Class<? extends MappingObject> objectClass, List params, List values, Integer[] ids, boolean isThrownException) throws Exception {
    return executeUpdate(objectClass, (String[])params.toArray(new String[0]), values.toArray(), ids, isThrownException);
  }
  
  public static int executeUpdate(Class<? extends MappingObject> objectClass, String[] params, Object[] values, Integer id, boolean isThrownException) throws Exception {
    return executeUpdate(objectClass, params, values, new Integer[]{id}, isThrownException);
  }
  
  public static int executeUpdate(Class<? extends MappingObject> objectClass, String[] params, Object[] values, Integer[] ids, boolean isThrownException) throws Exception {
    int returnInt = 0;
    try {
      returnInt = createSession(true).executeUpdate(objectClass, params, values, ids);
    } catch (Exception ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return returnInt;
  }
  
  public static int[] executeUpdate(String[] query, Object[][] params) {
    int[] returnInt = new int[0];
    try {
      returnInt = executeUpdate(query, params, false);
    }catch(Exception ex){}
    return returnInt;
  }
  
  public static int[] executeUpdate(String[] query, Object[][] params, boolean isThrownException) throws Exception {
    int[] returnInt;
    try {
      returnInt = createSession(true).executeUpdate(query, params);
    } catch (Exception ex) {
      returnInt = new int[0];
      if(isThrownException)
        throw new Exception(ex);
    }
    return returnInt;
  }
  
  public static int update(Class<? extends MappingObject> objectClass, PropertyMap... objects) throws Exception {
    int r = 0;
    for(PropertyMap object:objects)
      r += update(objectClass, object);
    return r;
  }
  
  public static int update(Class<? extends MappingObject> objectClass, PropertyMap object) throws Exception {
    return update(createSession(true), objectClass, object);
  }
  
  public static int update(RemoteSession session, Class<? extends MappingObject> objectClass, PropertyMap object) throws Exception {
    return session.executeUpdate(objectClass, object.getSimpleMapWithoutKeys("id").keySet().toArray(new String[0]), object.getSimpleMapWithoutKeys("id").values().toArray(), new Integer[]{object.getValue("id", Integer.TYPE)});
  }
  
  public static int removeObjects(Class<? extends MappingObject> objectClass, Collection<Integer> ids) {
    return removeObjects(objectClass, ids.toArray(new Integer[0]));
  }
  
  public static int removeObjects(Class<? extends MappingObject> objectClass, Integer... ids) {
    try {return removeObjects(objectClass, false, ids);} catch (Exception ex) {}
    return 0;
  }
  
  public static int removeObjects(Class<? extends MappingObject> objectClass, PropertyMap objectEventProperty, Integer... ids) {
    try {return removeObjects(objectClass, false, objectEventProperty, ids);} catch (Exception ex) {}
    return 0;
  }
  
  public static int removeObjects(Class<? extends MappingObject> objectClass, boolean isThrownException, Collection<Integer> ids) throws Exception {
    return removeObjects(objectClass, isThrownException, ids.toArray(new Integer[0]));
  }
  
  public static int removeObjects(Class<? extends MappingObject> objectClass, boolean isThrownException, Integer... ids) throws Exception {
    return removeObjects(objectClass, isThrownException, null, ids);
  }
  
  public static int removeObjects(Class<? extends MappingObject> objectClass, boolean isThrownException, PropertyMap objectEventProperty, Integer... ids) throws Exception {
    int returnInt = 0;
    try {
      if(ids.length > 0)
        returnInt = createSession(true).removeObjects(objectClass, objectEventProperty == null ? new HashMap() : objectEventProperty.getSimpleMap(), ids);
    } catch (Exception ex) {
      returnInt = 0;
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return returnInt;
  }
  
  public static void toTypeObjects(Class<? extends MappingObject> objectClass, MappingObject.Type type, List<PropertyMap> ids) {
    List<Integer> ids_ = new ArrayList<>();
    ids.stream().forEach(i -> ids_.add(i.getValue("id", Integer.class)));
    toTypeObjects(objectClass, type, ids_);
  }
  
  public static void toTypeObjects(Class<? extends MappingObject> objectClass, MappingObject.Type type, Collection<Integer> ids) {
    toTypeObjects(objectClass, type, ids.toArray(new Integer[0]));
  }
  
  public static void toTypeObjects(Class<? extends MappingObject> objectClass, MappingObject.Type type, Integer... ids) {
    try{toTypeObjects(objectClass, false, type, ids);}catch(Exception ex) {}
  }
  
  public static void toTypeObjects(Class<? extends MappingObject> objectClass, boolean isThrown, MappingObject.Type type, List<PropertyMap> ids) throws Exception {
    List<Integer> ids_ = new ArrayList<>();
    ids.stream().forEach(i -> ids_.add(i.getValue("id", Integer.class)));
    toTypeObjects(objectClass, isThrown, type, ids_);
  }
  
  public static void toTypeObjects(Class<? extends MappingObject> objectClass, boolean isThrown, MappingObject.Type type, Collection<Integer> ids) throws Exception {
    toTypeObjects(objectClass, isThrown, type, ids.toArray(new Integer[0]));
  }
  
  public static void toTypeObjects(Class<? extends MappingObject> objectClass, boolean isThrown, MappingObject.Type type, Integer... ids) throws Exception {
    try {
      if(ids.length > 0)
        createSession(true).toTypeObjects(objectClass, ids, type);
    }catch(Exception ex) {
      if(isThrown)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
  }
  
  public static void toTmpObjects(Class<? extends MappingObject> objectClass, Integer[] ids, boolean tmp) {
    try{toTmpObjects(objectClass, false, ids, tmp);}catch(Exception ex) {}
  }
  
  public static void toTmpObjects(Class<? extends MappingObject> objectClass, boolean isThrown, Integer[] ids, boolean tmp) throws Exception {
    try {
      createSession(true).toTmpObjects(objectClass, ids, tmp);
    }catch(Exception ex) {
      if(isThrown)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
  }
  
  
  
  
  public static List<List> getData(Class<? extends MappingObject> objectClass, String... fields) {
    return getData(DBFilter.create(objectClass), fields);
  }
  
  public static List<List> getData(DBFilter filter, String... fields) {
    String[] order = new String[0];
    String[] group = new String[0];
    
    for(int i=fields.length-1;i>=0;i--) {
      if(fields[i].toLowerCase().startsWith("sort:")) {
        order  = ArrayUtils.addAll(order, fields[i].substring(5).split(","));
        fields = ArrayUtils.remove(fields, i);
      }
    }
    
    for(int i=fields.length-1;i>=0;i--) {
      if(fields[i].toLowerCase().startsWith("group by:")) {
        group  = ArrayUtils.addAll(group, fields[i].substring(9).split(","));
        fields = ArrayUtils.remove(fields, i);
      }
    }
    
    return getData(filter, fields, order, group);
  }
  
  public static List<List> getData(DBFilter filter, boolean isThrownException, String... fields) throws Exception {
    return getData(filter, fields, isThrownException, (String)null);
  }
  
  public static List<List> getData(DBFilter filter, String[] fields, String[] orderby, String[] groupby) {
    try {return getData(filter, fields, false, orderby, groupby);} catch (Exception ex) {}
    return null;
  }
  
  public static List<List> getData(DBFilter filter, String[] fields, String... orderby) {
    try {
      return getData(filter, fields, false, orderby);
    }catch (Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    return null;
  }
  
  public static List<List> getData(DBFilter filter, String[] fields, boolean isThrownException, String... orderby) throws Exception {
    List<List> data = null;
    RemoteSession session = null;
    try {
      data = (List<List>) GzipUtil.getObjectFromGzip(createSession(true).getGZipData(filter, fields, orderby));
    } catch (Exception ex) {
      ex.printStackTrace();
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return data;
  }
  
  public static List<List> getData(DBFilter filter, String[] fields, boolean isThrownException, String[] orderby, String[] groupby) throws Exception {
    List<List> data = null;
    try {
      data = (List<List>) GzipUtil.getObjectFromGzip(createSession(true).getGZipData(filter, fields, orderby, groupby));
    } catch (Exception ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return data;
  }
  
  public static List<List> getData(RemoteSession session, DBFilter filter, String[] fields, boolean isThrownException, String[] orderby, String[] groupby) throws Exception {
    List<List> data = null;
    try {
      data = (List<List>) GzipUtil.getObjectFromGzip(session.getGZipData(filter, fields, orderby, groupby));
    } catch (Exception ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return data;
  }
  
  public static Object getData(Class<? extends MappingObject> objectClass, Integer id, String field) {
    Object[] o = getSingleData(objectClass, id, field);
    return o == null ? null : o[0];
  }
  
  public static Object[] getSingleData(Class<? extends MappingObject> objectClass, Integer id, String... fields) {
    List<List> data = getData(objectClass, new Integer[]{id}, fields, null);
    return data.isEmpty() || data == null ? null : data.get(0).toArray();
  }
  
  public static List<List> getData(Class<? extends MappingObject> objectClass, Integer id, String... fields) {
    return getData(objectClass, new Integer[]{id}, fields, null);
  }
  
  public static ObservableList<PropertyMap> getList(Class<? extends MappingObject> objectClass, Collection<Integer> ids, String... fields) {
    ObservableList<PropertyMap> list = FXCollections.observableArrayList();
    fillList(DBFilter.create(objectClass).AND_IN("id", ids.toArray(new Integer[0])), list, fields);
    return list;
  }
  
  public static ObservableList<PropertyMap> getList(Class<? extends MappingObject> objectClass, Integer[] ids, String... fields) {
    ObservableList<PropertyMap> list = FXCollections.observableArrayList();
    fillList(DBFilter.create(objectClass).AND_IN("id", ids), list, fields);
    return list;
  }
  
  public static ObservableList<PropertyMap> getList(Class<? extends MappingObject> objectClass, String... fields) {
    ObservableList<PropertyMap> list = FXCollections.observableArrayList();
    fillList(DBFilter.create(objectClass).AND_EQUAL("type", MappingObject.Type.CURRENT).AND_EQUAL("tmp", false), list, fields);
    return list;
  }
  
  public static ObservableList<PropertyMap> getList(DBFilter filter, List<String> fields) {
    return getList(filter, fields.toArray(new String[0]));
  }
  
  public static ObservableList<PropertyMap> getList(DBFilter filter, String... fields) {
    ObservableList<PropertyMap> list = FXCollections.observableArrayList();
    fillList(filter, list, fields);
    return list;
  }
  
  /*public static void fillList(DBFilter filter, ObservableList<PropertyMap> list, String... fields) {
    fillList(filter, list, fields);
  }*/
  
  public static void fillList(DBFilter filter, List<PropertyMap> list, String... fields) {
    list.clear();
    
    //if(fields.length == 0) {
      try {
        createSession(true).getList(filter, fields).stream().forEach(m -> list.add(PropertyMap.copy(m)));
      }catch(Exception ex) {
        ex.printStackTrace();
      }
    /*}else {
      String[] keys = Arrays.copyOf(fields, fields.length);
      for(int i=0;i<fields.length;i++) {
        if(fields[i].toLowerCase().startsWith("sort:") || fields[i].toLowerCase().startsWith("group by:")) {
          keys = ArrayUtils.removeElement(keys, fields[i]);
        }else {
          if(fields[i].contains("=query:")) {
            keys[i]   = keys[i].substring(0, keys[i].indexOf("=query:"));
            fields[i] = fields[i].substring(fields[i].indexOf("query:"));
            if(!fields[i].substring(6).matches("^[а-яА-ЯёЁa-zA-Z0-9_-]+:.*"))
              fields[i] = fields[i].replace("query:", "query:"+keys[i].replaceAll("-", "_")+":");
          }else if(fields[i].contains(":=:")) {
            keys[i]   = keys[i].substring(0, keys[i].indexOf(":=:"));
            fields[i] = fields[i].substring(fields[i].indexOf(":=:")+3);
          }
        }
      }

      for(List d:getData(filter, fields)) {
        PropertyMap map = PropertyMap.create();
        for(int i=0;i<keys.length;i++)
          map.setValue(keys[i], d.get(i));
        list.add(map);
      }
    }*/
  }
  
  public static PropertyMap getMap(Class<? extends MappingObject> objectClass, Integer id, String... fields) {
    return getList(DBFilter.create(objectClass).AND_EQUAL("id", id), fields).stream().findFirst().orElseGet(() -> null);
  }
  
  /*public static void fillMap(Class<? extends MappingObject> objectClass, PropertyMap map, Integer id, String... fields) {
    if(fields.length == 0) {
      try {
        if(id != null)
          map.copyFrom(createSession(true).object(objectClass, id));
      }catch(Exception ex){}
    }else {
      String[] keys = Arrays.copyOf(fields, fields.length);
      for(int i=0;i<fields.length;i++) {
        if(fields[i].contains("=query:")) {
          keys[i]   = keys[i].substring(0, keys[i].indexOf("=query:"));
          fields[i] = fields[i].substring(fields[i].indexOf("query:"));
          if(!fields[i].substring(6).matches("^[а-яА-ЯёЁa-zA-Z0-9_-]+:.*"))
            fields[i] = fields[i].replace("query:", "query:"+keys[i].replaceAll("-", "_")+":");
        }else if(fields[i].contains(":=:")) {
          keys[i]   = keys[i].substring(0, keys[i].indexOf(":=:"));
          fields[i] = fields[i].substring(fields[i].indexOf(":=:")+3);
        }
      }
      getData(objectClass, new Integer[]{id}, fields, null).stream().forEach(d -> {
        for(int i=0;i<keys.length;i++)
          map.setValue(keys[i], d.get(i));
      });
    }
  }*/
  
  
  
  
  
  
  public static Object getData(Class<? extends MappingObject> objectClass, boolean isThrownException, Integer id, String field) throws Exception {
    Object[] o = getSingleData(objectClass, isThrownException, id, field);
    return o == null ? null : o[0];
  }
  
  public static Object[] getSingleData(Class<? extends MappingObject> objectClass, boolean isThrownException, Integer id, String... fields) throws Exception {
    List<List> data = getData(objectClass, new Integer[]{id}, fields, null, isThrownException);
    return data.isEmpty() ? null : data.get(0).toArray();
  }
  
  public static List<List> getData(Class<? extends MappingObject> objectClass, boolean isThrownException, Integer id, String... fields) throws Exception {
    return getData(objectClass, new Integer[]{id}, fields, null, isThrownException);
  }
  
  public static List<List> getData(Class<? extends MappingObject> objectClass, Integer[] ids, String... fields) {
    return getData(objectClass, ids, fields, null);
  }
  
  public static List<List> getData(Class<? extends MappingObject> objectClass, boolean isThrownException, Integer[] ids, String... fields) throws Exception {
    return getData(objectClass, ids, fields, null, isThrownException);
  }
  
  public static List<List> getData(Class<? extends MappingObject> objectClass, Integer[] ids, String[] fields, String[] orderby) {
    try {return getData(objectClass, ids, fields, orderby, false);} catch (Exception ex) {}
    return null;
  }
  
  public static List<List> getData(Class<? extends MappingObject> objectClass, Integer[] ids, String[] fields, String[] orderby, boolean isThrownException) throws Exception {
    List<List> data = null;
    try {
      data = createSession(true).getData(objectClass, ids, fields, orderby);
    } catch (Exception ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return data;
  }
  
  public static List<List> getData(String sql) {
    return getData(sql, new Object[0]);
  }
  
  public static List<List> getData(String sql, Object[] params) {
    try {return getData(sql, params, false);} catch (Exception ex) {}
    return null;
  }
  
  public static List<List> getData(String sql, Object[] params, boolean isThrownException) throws Exception {
    List<List> data = null;
    try {
      data = (List<List>) GzipUtil.getObjectFromGzip(createSession(true).executeGzipQuery(sql, params));
    } catch (Exception ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return data;
  }
  
  public static Integer getMaxId(Class<? extends MappingObject> objectClass, boolean isThrownException) throws Exception {
    Integer maxId = 0;
    maxId = (Integer) executeQuery("SELECT MAX(id) FROM ["+objectClass.getSimpleName()+"]", isThrownException, new Object[0]).get(0).get(0);
    return maxId;
  }
  
  public static boolean saveObject(Class<? extends MappingObject> objectClass, Integer id, Map<String, Object> map) {
    try {return saveObject(objectClass, id, map, false);}catch(Exception ex) {}
    return false;
  }
  
  public static boolean saveObject(Class<? extends MappingObject> objectClass, Integer id, boolean isThrownException, Map.Entry<String, Object>... params) throws Exception {
    Map<String, Object> map = new TreeMap<>();
    for(Map.Entry<String, Object> param:params)
      map.put(param.getKey(), param.getValue());
    return saveObject(objectClass, id, map, isThrownException);
  }
  
  public static boolean saveObject(Class<? extends MappingObject> objectClass, Integer id, Map.Entry<String, Object>... params) {
    try {return saveObject(objectClass, id, false, params);}catch(Exception ex) {}
    return false;
  }
  
  public static boolean saveObject(Class<? extends MappingObject> objectClass, PropertyMap map) throws Exception {
    return saveObject(objectClass, map, false);
  }
  
  public static boolean saveObject(Class<? extends MappingObject> objectClass, PropertyMap map, boolean isThrownException) throws Exception {
    return saveObject(objectClass, map.getValue("id", Integer.TYPE), map, isThrownException);
  }
  
  public static boolean saveObject(Class<? extends MappingObject> objectClass, Integer id, PropertyMap map, boolean isThrownException) throws Exception {
    return saveObject(objectClass, id, map.getSimpleMapWithoutKeys("modificationDate", "lastUserId"), isThrownException);
  }
  
  private static Map<String, Object> clearLastuseridModificationDate(Map<String, Object> map) {
    map.remove("lastUserId");
    map.remove("modificationDate");
    map.forEach((k,v) -> {
      if(v instanceof Map)
        clearLastuseridModificationDate((Map<String, Object>)v);
      if(v instanceof Object[]) {
        for(Object o:(Object[])v)
          if(o instanceof Map)
            clearLastuseridModificationDate((Map<String, Object>)o);
      }
    });
    return map;
  }
  
  public static boolean saveObject(Class<? extends MappingObject> objectClass, Integer id, Map<String, Object> map, boolean isThrownException) throws Exception {
    boolean is = false;
    try {
      is = createSession(true).saveObject(objectClass, id, clearLastuseridModificationDate(map));
    } catch (Exception ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return is;
  }
  
  public static boolean saveObject(MappingObject object) {
    try {return saveObject(object, false);}catch(Exception ex) {}
    return false;
  }
  
  public static boolean saveObject(MappingObject object, boolean isThrownException) throws Exception {
    boolean is = false;
    try {
      is = createSession(true, isThrownException).saveObject(object);
    } catch (Exception ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return is;
  }
  
  public static MappingObject createObject(Class<? extends MappingObject> objectClass) {
    try {return createObject(objectClass, false);}catch(Exception ex) {}
    return null;
  }
  
  public static MappingObject createObject(Class<? extends MappingObject> objectClass, boolean isThrownException) throws Exception {
    MappingObject object = null;
    try {
      object = createSession(true).createEmptyObject(objectClass);
    } catch (Exception ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return object;
  }
  
  public static Integer createObject(Class<? extends MappingObject> objectClass, Map<String, Object> map) {
    try {return createObject(objectClass, map, false);}catch(Exception ex) {}
    return null;
  }
  
  public static Integer createObject(Class<? extends MappingObject> objectClass, PropertyMap map, boolean isThrownException) throws Exception {
    try {
      return createObject(objectClass, map.getSimpleMapWithoutKeys("modificationDate", "lastUserId", "id"), isThrownException);
    }
    catch(Exception ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return null;
  }
  
  public static Integer createObject(Class<? extends MappingObject> objectClass, PropertyMap map) {
    try {return createObject(objectClass, map, false);}catch(Exception ex) {}
    return null;
  }
  
  public static Integer createObject(Class<? extends MappingObject> objectClass, PropertyMap map, PropertyMap objectEventProperty) {
    Integer id = null;
    try {
      id = createSession(true).createObject(objectClass, map.getSimpleMapWithoutKeys("modificationDate", "lastUserId", "id"), objectEventProperty.getSimpleMap());
    } catch (Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    return id;
  }
  
  public static Integer createObject(Class<? extends MappingObject> objectClass, String json) {
    try {
      Map m = new HashMap<>();
      JSONObject js = JSONObject.fromObject(json);
      for(Object key:js.keySet())
        m.put(key, js.get(key));
      return createObject(objectClass, m, false);
    }catch(Exception ex) {}
    return null;
  }
  
  
  
  public static Integer createObject(Class<? extends MappingObject> objectClass, Map<String, Object> map, boolean isThrownException) throws Exception {
    Integer id = null;
    try {
      id = createSession(true).createObject(objectClass, map);
    } catch (Exception ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return id;
  }
  
  public static <T> T getObjectFieldValue(Class<T> cl, Class<? extends MappingObject> objectClass, Integer id, String field) throws Exception {
    return cl.cast(fillObject(objectClass, id, new String[]{field}, new Object())[0]);
  }
  
  public static Object[] fillObject(Class<? extends MappingObject> objectClass, Integer id, String[] fields, Object... values) {
    try {values = fillObject(objectClass, id, fields, false, values);} catch (Exception ex) {}
    return values;
  }
  
  public static Object[] fillObject(Class<? extends MappingObject> objectClass, Integer id, String[] fields, boolean isThrownException, Object... values) throws Exception {
    try {
      List<List> data = createSession(true).getData(objectClass, new Integer[]{id}, fields);
      if(!data.isEmpty())
        for(int i=0;i<data.get(0).size();i++)
          values[i] = data.get(0).get(i);
    } catch (Exception ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return values;
  }
  
  public static String getMD5Hash(Class<? extends MappingObject> objectClass, Integer id) throws RemoteException {
    return getServer().getMD5Hash(objectClass, id);
  }
  
  public static String getMD5Hash(MappingObject object) throws RemoteException {
    return getServer().getMD5Hash(object);
  }
  
  public static JSONObject getJSON(MappingObject object) {
    try {return getJSON(object, false);} catch (Exception ex) {}
    return null;
  }
  
  public static JSONObject getJSON(MappingObject object, boolean isThrownException) throws Exception {
    JSONObject json = null;
    try {
      json = createSession(true).getJson(object);
    } catch (RemoteException | IllegalArgumentException | IllegalAccessException ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return json;
  }
  
  public static JSONObject getJSON(Class<? extends MappingObject> objectClass, Integer id)  {
    try {return getJSON(objectClass, id, false);} catch (Exception ex) {}
    return null;
  }
  
  public static JSONObject getJSON(Class<? extends MappingObject> objectClass, Integer id, boolean isThrownException) throws Exception {
    JSONObject json = null;
    try {
      json = createSession(true).getJson(objectClass, id);
    } catch (Exception ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return json;
  }
  
  public static MappingObject getObject(Class<? extends MappingObject> objectClass, Integer id) {
    try {return getObject(objectClass, id, false);} catch (Exception ex) {}
    return null;
  }
  
  public static MappingObject getObject(Class<? extends MappingObject> objectClass, Integer id, boolean isThrownException) throws Exception {
    MappingObject object = null;
    try {
      object = (MappingObject) createSession(true).getObject(objectClass, id);
    } catch (Exception ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return object;
  }
  
  public static MappingObject[] getObjects(Class<? extends MappingObject> objectClass) {
    try {return getObjects(null, DBFilter.create(objectClass), null, false);}catch(Exception ex) {}
    return null;
  }
  
  public static MappingObject[] getObjects(DBFilter filter) {
    return getObjects(filter, null);
  }
  
  public static MappingObject[] getObjects(Class<? extends MappingObject> objectClass, Integer[] ids) {
    return getObjects(ids, DBFilter.create(objectClass), null);
  }
  
  public static MappingObject[] getObjects(DBFilter filter, String[] orderby) {
    return getObjects(null, filter, orderby);
  }
  
  public static MappingObject[] getObjects(Class<? extends MappingObject> objectClass, Integer[] ids, String[] orderby) {
    return getObjects(ids, DBFilter.create(objectClass), orderby);
  }
  
  public static MappingObject[] getObjects(Integer[] ids, DBFilter filter, String[] orderby) {
    try {return getObjects(ids, filter, orderby, false);} catch (Exception ex) {}
    return null;
  }
  
  public static MappingObject[] getObjects(Integer[] ids, DBFilter filter, String[] orderby, boolean isThrownException) throws Exception {
    MappingObject[] objects = null;
    try {
      MappingObject[] values = createSession(true).getObjects(ids, filter, orderby);
      objects = Arrays.copyOf(values, values.length, MappingObject[].class);
    } catch (Exception ex) {
      if(isThrownException)
        throw new Exception(ex);
      else Messanger.showErrorMessage(ex);
    }
    return objects;
  }

  public static Client getClient() {
    return client;
  }
  
  public static Integer[] getChilds(Class<? extends MappingObject> classname, Integer... ids) {
    for(List d:ObjectLoader.getData(DBFilter.create(classname).AND_IN("id", ids), "childs"))
      ids = ArrayUtils.addAll(ids, getChilds(classname, (Integer[])d.get(0)));
    return ids;
  }
}