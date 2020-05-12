package bum.realizations;

import bum.interfaces.ExportImport;
import bum.interfaces.Server;
import bum.util.EmailMessage;
import division.util.EmailUtil;
import division.util.GzipUtil;
import division.util.IDStore;
import division.util.JMSUtil;
import division.util.Utility;
import division.xml.Document;
import division.xml.Node;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Store;
import mapping.MappingObject;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.mail.ByteArrayDataSource;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.log4j.Logger;
import util.*;
import util.filter.local.DBFilter;

public class ServerImpl extends UnicastRemoteObject implements Server {
  private ExecutorService pool = Executors.newFixedThreadPool(20);
  public int objectPort;
  public RMIClientSocketFactory clientSocketFactory;
  public RMIServerSocketFactory serverSocketFactory;
  
  private List<Client>  clients  = new CopyOnWriteArrayList<>();
  
  public ServerImpl() throws RemoteException {
    super();
  }
    
  public ServerImpl(int objectPort, RMIClientSocketFactory clientSocketFactory, RMIServerSocketFactory serverSocketFactory) throws RemoteException {
    super(objectPort, clientSocketFactory, serverSocketFactory);
    this.objectPort = objectPort;
    this.clientSocketFactory = clientSocketFactory;
    this.serverSocketFactory = serverSocketFactory;
  }

  @Override
  public Class[] getClasses() throws ClassNotFoundException, RemoteException {
    return DataBase.getClasses().values().toArray(new Class[0]);
  }
  
  @Override
  public RemoteSession createSession(Client clientServer, boolean autoCommit) throws RemoteException {
    return new Session(objectPort, clientSocketFactory, serverSocketFactory, clientServer, autoCommit);
  }
  
  @Override
  public String getClientName(Class<? extends MappingObject> objectClass) throws RemoteException {
    return DataBase.get(objectClass).getClientName();
  }
  
  @Override
  public Map<String, Map<String,Object>> getFieldsInfo(Class<? extends MappingObject> objectClass) throws RemoteException {
    LinkedHashMap<String, Map<String,Object>> tm = new LinkedHashMap<>();
    DBTable db = DataBase.get(objectClass);
    DBColumn[] columns = db.getColumns();
    for(DBColumn column:columns) {
      String key = column.getField().getName();
      Map<String,Object> value = new TreeMap<>();
      value.put("CLASS",             objectClass);
      value.put("CLASSDESCRIPTION",  getClientName(objectClass));
      value.put("NAME",              column.getField().getName());
      value.put("DBNAME",            column.getName());
      value.put("TYPE",              column.getField().getType());
      value.put("PARAMETERIZEDTYPE", column.getField().getType());
      value.put("DESCRIPTION",       column.getDescription());
      value.put("DEFAULTVALUE",      column.getDefaultValue());
      value.put("SETMETHOD",         column.getSetMethod());
      value.put("GETMETHOD",         column.getGetMethod());
      tm.put(key, value);
    }

    for(DBRelation relation:db.getRelations()) {
      String key = relation.getField().getName();
      Map<String,Object> value = new TreeMap<>();
      value.put("CLASS",             objectClass);
      value.put("CLASSDESCRIPTION",  getClientName(objectClass));
      value.put("NAME",              relation.getField().getName());
      value.put("DBNAME",            relation.getName());
      value.put("TYPE",              relation.getField().getType());
      value.put("PARAMETERIZEDTYPE", relation.getTargetClass());
      value.put("DESCRIPTION",       relation.getDescription());
      value.put("SETMETHOD",         relation.getSetMethod());
      value.put("GETMETHOD",         relation.getGetMethod());
      tm.put(key, value);
    }
    
    tm.putAll(db.getQueryColumns());
    return tm;
  }

  @Override
  public Client registrateClient(Client client) throws RemoteException {
    client.setSessionId(IDStore.createID());
    clients.add(client);
    return client;
  }

  @Override
  public Integer[] getRegistrateWorkers() throws RemoteException {
    Integer[] workers = new Integer[0];
    /*for(int i=clientServers.size()-1;i>=0;i--) {
      try {
        workers = (Integer[]) ArrayUtils.add(workers, clientServers.get(i).getWorkerId());
      }catch (Exception e) {
        clientServers.remove(i);
      }
    }*/
    return workers;
  }

  @Override
  public Integer[] getRegistratePeoples() throws RemoteException {
    Integer[] peoples = new Integer[0];
    /*for(int i=clientServers.size()-1;i>=0;i--) {
      try {
        peoples = (Integer[]) ArrayUtils.add(peoples, clientServers.get(i).getPeopleId());
      }catch (Exception e) {
        clientServers.remove(i);
      }
    }*/
    return peoples;
  }

  @Override
  public Client[] getRegistrateClients() throws RemoteException {
    Client[] clients = new Client[0];
    /*for(int i=clientServers.size()-1;i>=0;i--) {
      try {
        clientServers.get(i).getPeopleId();
        clients = (Client[]) ArrayUtils.add(clients, clientServers.get(i));
      }catch (Exception e) {
        clientServers.remove(i);
      }
    }*/
    return clients;
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  /*@Override
  public void sendMessage(String topicName, javax.jms.Message message) throws RemoteException {
    TopicConnection topicConnection = null;
    TopicSession topicSession = null;
    try {
      topicConnection = DataBase.createConnection();
      topicSession = topicConnection.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
      Topic topic = topicSession.createTopic(topicName);
      TopicPublisher publisher = topicSession.createPublisher(topic);
      publisher.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      publisher.publish(message);
      publisher.close();
    }catch(JMSException ex) {
      Logger.getRootLogger().error(ex);
      throw new RemoteException(topicName, ex);
    }finally {
      try {
        if(topicSession != null)
          topicSession.close();
      } catch (JMSException e) {}
      try {
        if(topicConnection != null)
          topicConnection.close();
      } catch (JMSException e) {}
    }
  }
  
  @Override
  public void sendMessage(String topicName, Serializable message) throws RemoteException {
    TopicConnection topicConnection = null;
    TopicSession topicSession = null;
    try {
      topicConnection = DataBase.createConnection();
      topicSession = topicConnection.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
      Topic topic = topicSession.createTopic(topicName);
      TopicPublisher publisher = topicSession.createPublisher(topic);
      publisher.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      publisher.publish(topicSession.createObjectMessage(message));
      publisher.close();
    }catch(JMSException ex) {
      Logger.getRootLogger().error(ex);
      throw new RemoteException(topicName, ex);
    }finally {
      try {
        if(topicSession != null)
          topicSession.close();
      } catch (JMSException e) {}
      try {
        if(topicConnection != null)
          topicConnection.close();
      } catch (JMSException e) {}
    }
  }*/
  
  @Override
  public void getExportData(Class<? extends MappingObject> className, Integer[] ids, Integer limit, String topicName) throws RemoteException {
    Session session = null;
    try {
      session = new Session(objectPort, clientSocketFactory, serverSocketFactory, null, false);
      List data = session.getData(DBFilter.create(ExportImport.class).AND_EQUAL("objectClassName", className.getName()), new String[]{"exportData"});
      session.commit();
      DBTable table = DataBase.get(className);
      if(!data.isEmpty()) {
        final String xml = (String)((List)data.get(0)).get(0);
        if(xml != null && !xml.equals("")) {
          Document doc = Document.loadFromString(xml);
          if(doc != null) {
            for(int j=0;j<ids.length;j+=limit) {
              Integer[] ids_ = ArrayUtils.subarray(ids, j, j+limit > ids.length ? ids.length : j+limit);
              pool.submit(() -> {
                Document document = new Document();
                RemoteSession sess = null;
                try {
                  sess = createSession(null,false);
                  for(int i=0;i<ids_.length;i++) {
                    try {
                      Node exportNode = new Node(doc.getRootNode().getName());
                      document.getRootNode().addNode(exportNode);
                      fillNode(sess, table, exportNode, doc.getRootNode(), ids_[i]);
                    }catch(Exception ex) {
                      Logger.getRootLogger().error(ex);
                    }
                  }
                  sess.commit();
                  JMSUtil.sendTopicMessage(topicName, document);
                  //sendMessage(topicName, document);
                }catch(Exception ex) {
                  ex.printStackTrace();
                  try {sess.rollback();}catch(Exception e){}
                }
              });
            }
          }
        }
      }
    }catch(Exception ex) {
      session.rollback();
      Logger.getRootLogger().error(ex);
      throw new RemoteException(ex.getMessage());
    }
  }
  
  @Override
  public Document getExportData(Class<? extends MappingObject> className, Integer[] ids) throws RemoteException {
    Document document = new Document();
    Session session = null;
    try {
      session = new Session(objectPort, clientSocketFactory, serverSocketFactory, null, false);
      List data = session.getData(DBFilter.create(ExportImport.class).AND_EQUAL("objectClassName", className.getName()), new String[]{"exportData"});
      session.commit();
      DBTable table = DataBase.get(className);
      if(!data.isEmpty()) {
        final String xml = (String)((Vector)data.get(0)).get(0);
        if(xml != null && !xml.equals("")) {
          Document doc = Document.loadFromString(xml);
          if(doc != null) {
            RemoteSession sess = null;
            try {
              sess = createSession(null,false);
              for(int i=0;i<ids.length;i++) {
                try {
                  Node exportNode = new Node(doc.getRootNode().getName());
                  document.getRootNode().addNode(exportNode);
                  fillNode(sess, table, exportNode, doc.getRootNode(), ids[i]);
                }catch(Exception ex) {
                  Logger.getRootLogger().error(ex);
                }
              }
              sess.commit();
            }catch(Exception ex) {
              try {sess.rollback();}catch(Exception e){}
            }
          }
        }
      }
    }catch(Exception ex) {
      session.rollback();
      Logger.getRootLogger().error(ex);
      throw new RemoteException(ex.getMessage());
    }
    return document;
  }
 
   private synchronized void fillNode(RemoteSession session, DBTable tab, Node exportNode, Node node, Integer objectId) throws RemoteException, SQLException {
    DBTable t = tab;
    Class objectclass = tab.getInterfacesClass();
    String fields = ",id";
    String expression = "";
    DBRelation relation = null;
    if(!node.isRoot()) {
       Node data = node.getParent();

       String[] keys;
       for(keys = new String[]{node.getName()}; !data.isRoot(); data = data.getParent()) {
          if(!data.getName().equals("item")) {
             keys = (String[])((String[])ArrayUtils.add(keys, 0, data.getName()));
          }
       }

       for(int i$ = 0; i$ < keys.length; ++i$) {
          relation = t.getRelation(keys[i$]);
          t = relation.getTargetTable();
          objectclass = t.getInterfacesClass();
          if(i$ == keys.length - 1 && relation instanceof ManyToOne) {
             expression = " WHERE [" + objectclass.getSimpleName() + "(id)]=" + "(SELECT " + relation.getObjectColumnName() + " FROM " + relation.getObjectTable().getName() + " WHERE id=" + objectId + ")";
          }

          if(relation instanceof OneToMany) {
             expression = " WHERE " + relation.getTargetColumnName() + "=" + objectId;
          }

          if(relation instanceof ManyToMany) {
             expression = " WHERE [" + objectclass.getSimpleName() + "(id)] IN (SELECT " + relation.getTargetColumnName() + " FROM " + relation.getRelationTableName() + " WHERE " + relation.getObjectColumnName() + "=" + objectId + ")";
          }
       }
    } else {
       expression = expression + " WHERE [" + objectclass.getSimpleName() + "(id)]=" + objectId;
    }

    String var21;
    for(Iterator var19 = node.getAttributes().keySet().iterator(); var19.hasNext(); fields = fields + ",[" + objectclass.getSimpleName() + "(" + var21 + ")]") {
       var21 = (String)var19.next();
    }

    fields = fields.substring(1);
    List var18 = session.executeQuery("SELECT " + fields + " FROM [" + objectclass.getSimpleName() + "]" + expression);
    if(!var18.isEmpty()) {
       Object[] var22 = node.getAttributes().keySet().toArray();
       Vector d;
       Node ch;
       Iterator var20;
       if(relation != null && !(relation instanceof ManyToOne)) {
          var20 = var18.iterator();

          while(var20.hasNext()) {
             d = (Vector)var20.next();
             Node var23 = new Node("item");
             exportNode.addNode(var23);

             for(int var24 = 0; var24 < var22.length; ++var24) {
                var23.setAttribute((String)var22[var24], String.valueOf(d.get(var24 + 1) == null?"":d.get(var24 + 1)));
             }

             Iterator var26 = node.getNodes().iterator();

             while(var26.hasNext()) {
                ch = (Node)var26.next();
                Node subNode = new Node(ch.getName());
                var23.addNode(subNode);
                fillNode(session, tab, subNode, ch, (Integer)d.get(0));
             }
          }
       } else {
          var20 = var18.iterator();

          while(var20.hasNext()) {
             d = (Vector)var20.next();

             for(int i$1 = 0; i$1 < var22.length; ++i$1) {
                if(d.get(i$1 + 1) != null && d.get(i$1 + 1).getClass().isArray()) {
                   fillArray(exportNode, (Object[])((Object[])d.get(i$1 + 1)));
                } else {
                   exportNode.setAttribute((String)var22[i$1], String.valueOf(d.get(i$1 + 1) == null?"":d.get(i$1 + 1)));
                }
             }

             Iterator var25 = node.getNodes().iterator();

             while(var25.hasNext()) {
                Node child = (Node)var25.next();
                ch = new Node(child.getName());
                exportNode.addNode(ch);
                fillNode(session, tab, ch, child, (Integer)d.get(0));
             }
          }
       }
    }
   }

   private void fillArray(Node exportNode, Object[] array) {
      Node arrayNode = new Node("ARRAY");
      exportNode.addNode(arrayNode);
      Object[] arr$ = array;
      int len$ = array.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         Object obj = arr$[i$];
         Node arrayElement = new Node("ELEMENT");
         arrayNode.addNode(arrayElement);
         if(obj != null && obj.getClass().isArray()) {
            fillArray(arrayElement, (Object[])((Object[])obj));
         } else {
            arrayElement.setAttribute("value", String.valueOf(obj == null?"":obj));
         }
      }
   }
   
  @Override
  public String sendEmail(
          String smtpHost, 
          Integer smtpPort, 
          String smtpUser, 
          String smtpPassword, 
          EmailUtil.Addres[] to, 
          EmailUtil.Addres from, 
          String subject, 
          String message,
          String charset,
          EmailUtil.Attachment... attachments) throws RemoteException, EmailException, IOException {
    return EmailUtil.sendEmail(smtpHost, smtpPort, smtpUser, smtpPassword, to, from, subject, message, charset, attachments);
  }
   
  @Override
  public void sendEmail(final EmailMessage emailMessage) throws RemoteException {
    pool.submit(() -> {
      try {
        MultiPartEmail simpleEmail = new MultiPartEmail();
        simpleEmail.setHostName(System.getProperty("smtp.host"));
        simpleEmail.setSmtpPort(Integer.valueOf(System.getProperty("smtp.port")));
        if(System.getProperty("smtp.user") != null && !System.getProperty("smtp.user").equals(""))
          simpleEmail.setAuthentication(System.getProperty("smtp.user"), System.getProperty("smtp.password"));
        
        String[][] to = emailMessage.getTo();
        for(String[] t:to) {
          if(t[0] != null && !t[0].equals("")) {
            if(t[1] != null && t[2] != null)
              simpleEmail.addTo(t[0], t[1], t[2]);
          }
        }
        simpleEmail.setFrom(emailMessage.getFromEmail(), emailMessage.getFromName(), emailMessage.getFromCharset());
        simpleEmail.setSubject(emailMessage.getSubject());
        simpleEmail.setMsg(emailMessage.getMessage());
        
        simpleEmail.setCharset(emailMessage.getCharset());
        
        TreeMap<String,byte[]> attachments = emailMessage.getAttachments();
        for(String key:attachments.keySet()) {
          String[] list = key.split("\n\t");
          simpleEmail.attach(new ByteArrayDataSource(attachments.get(key), list[0]), list.length>1?list[1]:"", list.length>2?list[2]:"");
        }
        
        simpleEmail.send();
      }catch(EmailException | IOException ex) {
        Logger.getRootLogger().error(ex);
      }
    });
  }
  
  @Override
  public Map<Integer, String> getEmailMessages(String protocol, String host, int port, String email, String login, String password) throws RemoteException {
    Store store = null;
    Folder folder = null;
    try {
      Properties props = System.getProperties();
      props.put("mail.pop3.host", host);

      javax.mail.Session session = javax.mail.Session.getDefaultInstance(props, new DefaultAuthenticator(login, password));
      store = session.getStore(protocol);
      store.connect();
      
      folder = store.getFolder("INBOX");
      folder.open(Folder.READ_ONLY);
      Message[] messages = folder.getMessages();
      
      TreeMap<Integer, String> m = new TreeMap<>();
      for(Message message:messages)
        m.put(message.getMessageNumber(), Utility.join(message.getFrom(), ", "));
      return m;
    }catch(Exception ex) {
      Logger.getRootLogger().error(ex);
      throw new RemoteException(ex.getMessage());
    }finally {
      if(folder != null) {
        try {
          folder.close(false);
        }catch(Exception ex){}
      }
      if(store != null) {
        try {
          store.close();
        }catch(Exception ex){}
      }
    }
  }
  
  @Override
  public void getFile(String fName, String topicName) throws RemoteException {
    pool.submit(() -> {
      try {
        String updatePath = "clients"+File.separator+"division";
        JMSUtil.sendTopicMessage(topicName, GzipUtil.gzip(Utility.getBytesFromFile(updatePath+File.separator+fName)));
        //sendMessage(topicName, GzipUtil.gzip(Utility.getBytesFromFile(updatePath+File.separator+Utility.validatePath(fName))));
      }catch(Exception ex) {
        Logger.getRootLogger().error(ex);
        try {
          JMSUtil.sendTopicMessage(topicName, ex.getMessage());
        }catch(Exception e) {
          Logger.getRootLogger().error(e);
        }
      }
    });
  }
  
  @Override
  public byte[] getFile(String fName) throws RemoteException {
    try {
      String updatePath = "clients"+File.separator+"division";
      return GzipUtil.gzip(Utility.getBytesFromFile(updatePath+File.separator+fName));
    }catch(Exception ex) {
      Logger.getRootLogger().error(ex);
      throw new RemoteException(ex.getMessage(), ex);
    }
  }
  
  @Override
  public Document getUpdatePaths() throws RemoteException {
    String updatePath = "clients"+File.separator+"division";
    return Document.load(updatePath+File.separator+"update.xml");
  }
  
  @Override
  public Map<String, String> getUpdates(Map<String, String> clientFiles) throws RemoteException {
    try {
      Map<String, String> updates = new TreeMap<>();
      String updatePath = "clients"+File.separator+"division";
      Map<String, String> serverFles = new TreeMap<>();
      for(Node n:Document.load(updatePath+File.separator+"update.xml").getNodes("file"))
        serverFles.putAll(Utility.getMD5Files(updatePath, updatePath+File.separator+n.getAttribute("name")));
      
      for(String fileName:clientFiles.keySet().toArray(new String[0])) {
        String md5 = clientFiles.get(fileName);
        clientFiles.remove(fileName);
        clientFiles.put(fileName, md5);
      }
      
      for(String fileName:serverFles.keySet()) {
        if(!clientFiles.containsKey(fileName))
          updates.put(fileName, "create");
        else if(!clientFiles.get(fileName).equals(serverFles.get(fileName)))
          updates.put(fileName, "update");
      }

      clientFiles.keySet().removeAll(serverFles.keySet());
      for(String fileName:clientFiles.keySet())
        updates.put(fileName, "remove");
      
      return updates;
    }catch(Exception ex) {
      Logger.getRootLogger().error(ex);
      throw new RemoteException(ex.getMessage(), ex);
    }
  }
  
  @Override
  public String getMD5Hash(Class<? extends MappingObject> className, Integer id) throws RemoteException {
    String hash = "";
    RemoteSession session = null;
    try {
      hash = Utility.getMD5(createSession(null,true).getJson(className, id));
    }catch(Exception ex) {
      Logger.getRootLogger().error(ex);
      throw new RemoteException(ex.getMessage(), ex);
    }
    return hash;
  }
  
  @Override
  public String getMD5Hash(MappingObject object) throws RemoteException {
    String hash = "";
    RemoteSession session = null;
    try {
      hash = Utility.getMD5(createSession(null,true).getJson(object));
    }catch(Exception ex) {
      Logger.getRootLogger().error(ex);
      throw new RemoteException(ex.getMessage(), ex);
    }
    return hash;
  }
}