package client.util;

import division.util.JMSUtil;
import bum.interfaces.Server;
import division.fx.PropertyMap;
import division.util.GzipUtil;
import division.util.Utility;
import java.io.File;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.collections.ObservableList;
import javax.jms.*;
import mapping.MappingObject;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.log4j.Logger;
import util.Client;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class ObjectLoader {
  private static List<RemoteSession> sessions = new ArrayList<>();
  
  private static Server server;
  private static Client client;
  
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

  public static void clear() {
    sessions.stream().forEach(s -> rollBackSession(s));
    sessions.clear();
    client = null;
    server = null;
  }

  public static void connect() {
    PropertyMap s = PropertyMap.fromJson(Utility.getStringFromFile("conf"+File.separator+"conf.json")).getMap("server");
    
    int connectionCount = s.getInteger("connection-count");
    while(server == null && connectionCount > 0) {
      try {
        Logger.getRootLogger().info("CONNECTED TO RMI "+s.getString("name")+" "+s.getString("host")+":"+s.getInteger("port"));
        server = (Server) LocateRegistry.getRegistry(s.getString("host"), s.getInteger("port")).lookup(s.getString("name"));
      }catch(RemoteException | NotBoundException ex) {
        Logger.getRootLogger().error("CAN NOT CONNECTED TO RMI "+s.getString("name")+" "+s.getString("host")+":"+s.getInteger("port"), ex);
        try {
          Thread.sleep(s.getInteger("timeout")*1000);
        }catch(Exception e) {Logger.getRootLogger().error(e);}
      }finally {
        connectionCount--;
      }
    }
  }
  
  public static long sendMessageToPeople(Integer peopleId, String text) {
    try {
      ActiveMQMessage m = new ActiveMQMessage();
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
  
  public static Server getServer() {
    if(server == null)
      connect();
    return server;
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
  
  public static void removeSubscriber(TopicSubscriber subscriber) throws JMSException {
    removeSubscriber(subscriber.getTopic().getTopicName());
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
  
  public static void offLine() {
    JMSUtil.sendTopicMessage("client-system-command-topic", "offline", client);
  }
  
  public static void registrateClient(Client client) throws Exception {
    ObjectLoader.client = getServer().registrateClient(client);
    
    JMSUtil.addTopicSubscriber("server-system-command-topic", (Message msg) -> {
      try {
        JMSUtil.sendTopicMessage("client-system-command-topic", msg.getJMSType(), ObjectLoader.client);
      }catch (JMSException ex) {
        Logger.getLogger(ObjectLoader.class).error(ex.getMessage(), ex);
      }
    });
  }
  
  public static void dispose() {
    sessions.stream().forEach(s -> rollBackSession(s));
    sessions.clear();
    offLine();
    JMSUtil.dispose();
    System.exit(0);
  }

  public static Map<String, Map<String, Object>> getFieldsInfo(Class clazz) throws RemoteException {
    return getServer().getFieldsInfo(clazz);
  }
  
  public static String getClientName(Class<? extends MappingObject> clazz) throws RemoteException {
    return getServer().getClientName(clazz);
  }
  
  public static RemoteSession createSession() throws Exception {
    return createSession(false);
  }

  public static RemoteSession createSession(boolean autocommit) throws Exception {
    RemoteSession session = getServer().createSession(client, autocommit);
    if(!autocommit)
      sessions.add(session);
    return session;
  }
  
  public static void rollBackSession(RemoteSession session) {
    if(session != null) {
      try {
        if(!session.isClosed()) {
          session.rollback();
          System.out.println("ROLLBACK");
        }
      }catch(Exception ex) {
        Logger.getLogger(ObjectLoader.class).error(ex.getMessage(), ex);
      }
    }
  }
  
  public static void commitSession(RemoteSession session) throws Exception {
    if(session != null && !session.isClosed())
      session.commit();
  }
  
  public static boolean isSatisfy(DBFilter filter, MappingObject object) throws Exception {
    return createSession(true).isSatisfy(filter, object.getId());
  }
  
  public static boolean isSatisfy(DBFilter filter, Integer id) throws Exception {
    return createSession(true).isSatisfy(filter, id);
  }
  
  public static Integer[] isSatisfy(DBFilter filter, Integer[] ids) throws Exception {
    return createSession(true).isSatisfy(filter, ids);
  }
  
  public static List<List> executeQuery(String query, Object... params) throws Exception {
    return (List<List>) GzipUtil.getObjectFromGzip(createSession(true).executeGzipQuery(query, params));
  }
  
  public static List<List<List>> executeQuery(String[] querys, Object[][] arrParams) throws Exception {
    return Arrays.asList((List<List>[]) GzipUtil.getObjectFromGzip(createSession(true).executeGzipQuery(querys, arrParams)));
  }
  
  public static int executeUpdate(String query, Object... params) throws Exception {
    return createSession(true).executeUpdate(query, params);
  }

  public static int executeUpdate(Class<? extends MappingObject> objectClass, String param, Object value, Integer... ids) throws Exception {
    return executeUpdate(objectClass, new String[]{param}, new Object[]{value}, ids);
  }
  
  public static int executeUpdate(Class<? extends MappingObject> objectClass, String param, Object value, ObservableList<PropertyMap> ps) throws Exception {
    return executeUpdate(objectClass, new String[]{param}, new Object[]{value}, PropertyMap.getArrayFromList(ps, "id", Integer.TYPE));
  }
  
  public static int executeUpdate(Class<? extends MappingObject> objectClass, List params, List values, Integer... ids) throws Exception {
    return executeUpdate(objectClass, (String[])params.toArray(new String[0]), values.toArray(), ids);
  }
  
  public static int executeUpdate(Class<? extends MappingObject> objectClass, String[] params, Object[] values, Integer... ids) throws Exception {
    return createSession(true).executeUpdate(objectClass, params, values, ids);
  }
  
  public static int[] executeUpdate(String[] query, Object[][] params) throws Exception {
    return createSession(true).executeUpdate(query, params);
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
  
  public static int removeObjects(Class<? extends MappingObject> objectClass, Collection<Integer> ids) throws Exception {
    return removeObjects(objectClass, ids.toArray(new Integer[0]));
  }
  
  public static int removeObjects(Class<? extends MappingObject> objectClass, Integer... ids) throws Exception {
    return createSession(true).removeObjects(objectClass, ids);
  }
  
  public static int removeObjects(Class<? extends MappingObject> objectClass, PropertyMap objectEventProperty, Integer... ids) throws Exception {
    return createSession(true).removeObjects(objectClass, objectEventProperty == null ? new HashMap() : objectEventProperty.getSimpleMap(), ids);
  }

  public static void toTypeObjects(Class<? extends MappingObject> objectClass, MappingObject.Type type, Collection<Integer> ids) throws Exception {
    toTypeObjects(objectClass, type, ids.toArray(new Integer[0]));
  }
  
  public static void toTypeObjects(Class<? extends MappingObject> objectClass, MappingObject.Type type, Integer... ids) throws Exception {
    createSession(true).toTypeObjects(objectClass, ids, type);
  }
  
  public static void toTmpObjects(Class<? extends MappingObject> objectClass, boolean tmp, Integer... ids) throws Exception {
    createSession(true).toTmpObjects(objectClass, ids, tmp);
  }
  
  public static PropertyMap getMap(Class<? extends MappingObject> objectClass, Integer id, String... fields) throws Exception {
    ObservableList<PropertyMap> list = getList(objectClass, new Integer[]{id}, fields);
    return list.isEmpty() ? null : list.get(0);
  }
  
  public static ObservableList<PropertyMap> getList(Class<? extends MappingObject> objectClass, Collection<Integer> ids, String... fields) throws Exception {
    return PropertyMap.fromSimple(createSession(true).getList(DBFilter.create(objectClass).AND_IN("id", ids.toArray(new Integer[0])), fields));
  }
  
  public static ObservableList<PropertyMap> getList(Class<? extends MappingObject> objectClass, Integer[] ids, String... fields) throws Exception {
    return PropertyMap.fromSimple(createSession(true).getList(DBFilter.create(objectClass).AND_IN("id", ids), fields));
  }
  
  public static ObservableList<PropertyMap> getList(Class<? extends MappingObject> objectClass, String... fields) throws Exception {
    return PropertyMap.fromSimple(createSession(true).getList(DBFilter.create(objectClass).AND_EQUAL("type", MappingObject.Type.CURRENT).AND_EQUAL("tmp", false), fields));
  }
  
  public static ObservableList<PropertyMap> getList(DBFilter filter, String... fields) throws Exception {
    return PropertyMap.fromSimple(createSession(true).getList(filter, fields));
  }
  
  public static boolean saveObject(Class<? extends MappingObject> objectClass, PropertyMap map) throws Exception {
    return saveObject(objectClass, map.getValue("id", Integer.TYPE), map.getSimpleMap());
  }
  
  public static boolean saveObject(Class<? extends MappingObject> objectClass, Integer id, Map<String, Object> map) throws Exception {
    return createSession(true).saveObject(objectClass, id, map);
  }

  public static Integer createObject(Class<? extends MappingObject> objectClass, PropertyMap map) throws Exception {
    return createObject(objectClass, map, PropertyMap.create());
  }
  
  public static Integer createObject(Class<? extends MappingObject> objectClass, Map map) throws Exception {
    return createSession(true).createObject(objectClass, map);
  }

  public static Integer createObject(Class<? extends MappingObject> objectClass, PropertyMap map, PropertyMap objectEventProperty) throws Exception {
    return createSession(true).createObject(objectClass, map.getSimpleMapWithoutKeys("modificationDate", "lastUserId", "id"), objectEventProperty.getSimpleMap());
  }
  
  public static String getMD5Hash(Class<? extends MappingObject> objectClass, Integer id) throws RemoteException {
    return getServer().getMD5Hash(objectClass, id);
  }
  
  public static String getMD5Hash(MappingObject object) throws RemoteException {
    return getServer().getMD5Hash(object);
  }
  
  public static Client getClient() {
    return client;
  }
}