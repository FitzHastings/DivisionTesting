package util;

import bum.pool.DBController;
import division.fx.PropertyMap;
import division.util.GzipUtil;
import division.util.IDStore;
import division.util.Utility;
import java.awt.Color;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.jms.*;
import mapping.Archive;
import mapping.MappingObject;
import mapping.MappingObjectImpl;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import util.filter.local.DBExpretion;
import util.filter.local.DBExpretion.EqualDateType;
import util.filter.local.DBExpretion.EqualType;
import util.filter.local.DBExpretion.Logic;
import util.filter.local.DBFilter;
import util.proxy.Delegator;

public class Session extends UnicastRemoteObject implements RemoteSession {
  //public static ExecutorService pool = Executors.newFixedThreadPool(100);
  //public static TreeMap<Long,TreeMap<Long, SoftReference>> clientsObjectsCash = new TreeMap<>();
  
  private JdbcTemplate jdbcTemplate;
  
  private final Client client;
  private Connection connection;
  private final Vector<MessageProxy> events = new Vector<>();
  private final TreeMap<String, Savepoint> savePoints = new TreeMap<>();
  
  public int objectPort;
  public RMIClientSocketFactory clientSocketFactory;
  public RMIServerSocketFactory serverSocketFactory;
  
  public boolean autoCommit = false;
  
  private Long id;
  
  public Session() throws RemoteException {
    super();
    client = null;
  }
  
  public Session(boolean autoCommit) throws RemoteException {
    super();
    client = null;
    this.autoCommit = autoCommit;
  }

  public Session(int objectPort, RMIClientSocketFactory clientSocketFactory, RMIServerSocketFactory serverSocketFactory, Client client) throws RemoteException {
    this(objectPort, clientSocketFactory, serverSocketFactory, client, false);
  }
  
  public Session(int objectPort, RMIClientSocketFactory clientSocketFactory, RMIServerSocketFactory serverSocketFactory, Client client, boolean autoCommit) throws RemoteException {
    super(objectPort, clientSocketFactory, serverSocketFactory);
    this.objectPort = objectPort;
    this.clientSocketFactory = clientSocketFactory;
    this.serverSocketFactory = serverSocketFactory;
    this.client = client;
    this.autoCommit = autoCommit;
    this.id = IDStore.createID();
  }
  
  @Override
  public byte[] executeGzipQuery(String sqlQuerys[], Object[][] arrParams) throws RemoteException {
    try {
      return GzipUtil.gzip(executeQuery(sqlQuerys, arrParams));
    }catch(IOException ex) {
      throw new RemoteException(ex.getMessage());
    }
  }
  
  @Override
  public byte[] executeGzipQuery(String sqlQuery, Object... params) throws RemoteException {
    try {
      return GzipUtil.gzip(executeQuery(sqlQuery, params));
    }catch(IOException ex) {
      throw new RemoteException(ex.getMessage());
    }
  }
  
  @Override
  public byte[] executeGzipQuery(String sqlQuerys[]) throws RemoteException {
    try {
      return GzipUtil.gzip(executeQuery(sqlQuerys));
    }catch(IOException ex) {
      throw new RemoteException(ex.getMessage());
    }
  }
  
  @Override
  public byte[] executeGzipQuery(String sqlQuery) throws RemoteException {
    try {
      return GzipUtil.gzip(executeQuery(sqlQuery));
    }catch(IOException ex) {
      throw new RemoteException(ex.getMessage());
    }
  }
  
  @Override
  public List<List> executeQuery(String sqlQuery, Object[] params) throws RemoteException {
    List<List> resultSet = new Vector<>();
    ResultSet rs = null;
    PreparedStatement st = null;
    try {
      st = getConnection().prepareStatement(DBTable.replaceString(sqlQuery));
      setParams(st, params);
      Logger.getLogger(getClass()).debug(st);
      rs = st.executeQuery();
      int columnCount = rs.getMetaData().getColumnCount();
      while(rs.next()) {
        Vector row = new Vector();
        for(int i=1;i<=columnCount;i++) {
          Object result = rs.getObject(i);
          String type = rs.getMetaData().getColumnTypeName(i);
          /*if(result instanceof byte[]) {
            try {
              result = GzipUtil.deserializable((byte[]) result);
            }catch(Exception ex) {
              throw new RemoteException("etarggsd", ex);
            }
          }else */if(result instanceof java.sql.Array || rs.getMetaData().getColumnTypeName(i).startsWith("_")) {
            if(result == null) {
              if(rs.getMetaData().getColumnTypeName(i).startsWith("_int"))
                result = new Integer[0];
              if(rs.getMetaData().getColumnTypeName(i).startsWith("_text"))
                result = new String[0];
              if(rs.getMetaData().getColumnTypeName(i).startsWith("_bytea"))
                result = new byte[0];
            }else result = ((java.sql.Array)result).getArray();
          }
          row.add(result);
        }
        resultSet.add(row);
      }
    }catch(SQLException | IOException ex) {
      if(autoCommit) {
        rollback();
        close();
      }
      Logger.getLogger(getClass()).error("---"+st+"---"+"\n",ex);
      throw new RemoteException(ex.getMessage());
    }finally {
      params = null;
      try {rs.close();}catch(Exception e) {}
      try {st.close();}catch(Exception e) {}
      if(autoCommit) {
        commit();
        close();
      }
    }
    return resultSet;
  }
  
  public static void setParams(PreparedStatement st, Object[] params) throws IOException, SQLException {
    if(params != null) {
      for(int i=0;i<params.length;i++) {
        if(params[i] != null && params[i].getClass().isArray()) {
          if(params[i].getClass() == byte[].class) {
            st.setObject(i+1, params[i]);
          }else {
            String type = params[i].getClass().getSimpleName().toLowerCase();
            if(params[i].getClass() != Integer[].class) {
              if(params[i].getClass() == MappingObject[].class) {
                Integer[] ids = new Integer[0];
                for(Object o:(Object[])params[i])
                  ids = (Integer[]) ArrayUtils.add(ids, ((MappingObject)o).getId());
                params[i] = ids;
                type = params[i].getClass().getSimpleName().toLowerCase();
              }else type = "varchar[]";
            }
            st.setArray(i+1, st.getConnection().createArrayOf(type.substring(0, type.length()-2), (Object[]) params[i]));
          }
        }else {
          if(params[i] != null) {
            if(params[i] instanceof MappingObject) {
              params[i] = ((MappingObject)params[i]).getId();
            }else if(params[i] instanceof  Map) {
              params[i] = GzipUtil.gzip(params[i]);// GzipUtil.serializable(params[i]);
            }else if(params[i] instanceof Color) {
              params[i] = ((Color)params[i]).getRGB();
            }else if(params[i] instanceof LocalDateTime) {
              params[i] = Utility.convertToTimestamp((LocalDateTime)params[i]);
            }else if(params[i] instanceof LocalDate) {
              params[i] = Utility.convertToSqlDate((LocalDate)params[i]);
            }else if(params[i] instanceof java.util.Date && !(params[i] instanceof Timestamp)) {
              params[i] = new java.sql.Date(((java.util.Date)params[i]).getTime());
            }else if(params[i] instanceof Period) {
              params[i] = ((Period)params[i]).toString();
            }else if(params[i] != null) {
              if(params[i].getClass().isEnum()) {
                params[i] = params[i].toString();
              }
            }
          }
          st.setObject(i+1, params[i]);
        }
      }
    }
  }

  @Override
  public List<List> executeQuery(String sqlQuery) throws RemoteException {
    return executeQuery(sqlQuery, new Object[0]);
  }
  
  @Override
  public List<List>[] executeQuery(String... sqlQuery) throws RemoteException {
    List<List>[] resultSet = new Vector[sqlQuery.length];
    for(int i=0;i<sqlQuery.length;i++)
      resultSet[i] = executeQuery(sqlQuery[i]);
    return resultSet;
  }

  @Override
  public List<List>[] executeQuery(String[] sqlQuery, Object[][] params) throws RemoteException {
    List<List>[] resultSet = new Vector[sqlQuery.length];
    for(int i=0;i<sqlQuery.length;i++)
      resultSet[i] = executeQuery(sqlQuery[i],params[i]);
    return resultSet;
  }
  
  @Override
  public int executeUpdate(Class<? extends MappingObject> objectClass, String param, Object value, Integer id) throws RemoteException {
    return executeUpdate(objectClass, new String[]{param}, new Object[]{value}, new Integer[]{id});
  }
  
  @Override
  public int executeUpdate(Class<? extends MappingObject> objectClass, String[] params, Object[] values, Integer[] ids) throws RemoteException {
    if(params.length == 0 || params.length != values.length)
      return 0;
    String query = "UPDATE ["+objectClass.getSimpleName()+"] SET ";
    for(String param:params)
      query += "[!"+objectClass.getSimpleName()+"("+param+")]=?,";
    query = query.substring(0,query.length()-1);
    if(ids.length > 0)
      query += " WHERE id=ANY(?)";
    addEvent(objectClass, "UPDATE", ids);
    return executeUpdate(query, ArrayUtils.add(values, ids));
  }

  @Override
  public int executeUpdate(String sqlQuery, Object... params) throws RemoteException {
    PreparedStatement st = null;
    try {
      String query = DBTable.replaceString(sqlQuery);
      if(query.toLowerCase().startsWith("update")) {
        int index = query.toLowerCase().indexOf(" set ");
        query = query.substring(0, index+5)
                + "modificationDate=CURRENT_TIMESTAMP, "
                + "lastUserId="+(client!=null?client.getWorkerId():0)+", "
                + query.substring(index+5);
      }else if(query.toLowerCase().startsWith("insert")) {
        
        Pattern p = Pattern.compile("insert\\s+into[^\\(]+", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(query);
        if(m.find()) {
          String startQuery = m.group();
          p = Pattern.compile("(\\([^\\(]+\\)\\s*values\\s*)(.*)", Pattern.CASE_INSENSITIVE);
          m = p.matcher(query);
          if(m.find()) {
            startQuery += "(modificationDate, lastUserId, "+m.group(1).substring(1);
            query = m.group(2);
          }
          p = Pattern.compile("\\(.*\\)", Pattern.CASE_INSENSITIVE);
          m = p.matcher(query);
          if(m.find()) {
            query = "";
            char[] q = m.group().toCharArray();
            int g = 0;
            for(int i=0;i<q.length;i++) {
              if(q[i] == '(')
                g++;
              if(q[i] == ')')
                g--;
              if(g == 0 && !query.equals("")) {
                startQuery += query.substring(0, query.indexOf("(")+1)+"CURRENT_TIMESTAMP, "+(client!=null?client.getWorkerId():0)+", "+query.substring(query.indexOf("(")+1)+")";
                query = "";
              }else query += q[i];
            }
          }
          query = startQuery;
        }
      }
      
      st = getConnection().prepareStatement(query);
      setParams(st, params);
      Logger.getLogger(getClass()).debug(st);
      int result =  st.executeUpdate();
      if(result > 0 && autoCommit)
        commit();
      return result;
    }catch(SQLException | IOException ex) {
      rollback();
      close();
      Logger.getLogger(getClass()).error(st+"\n",ex);
      throw new RemoteException(ex.getMessage());
    }finally {
      params = null;
      try{st.close();}catch(Exception e) {}
      if(autoCommit)
        close();
    }
  }

  @Override
  public int executeUpdate(String sqlQuery) throws RemoteException {
    return executeUpdate(sqlQuery, new Object[0]);
  }

  @Override
  public int[] executeUpdate(String[] sqlQuery) throws RemoteException {
    int[] res = new int[sqlQuery.length];
    for(int i=0;i<sqlQuery.length;i++)
      res[i] = executeUpdate(sqlQuery[i]);
    return res;
  }

  @Override
  public int[] executeUpdate(String[] sqlQuery, Object[][] params) throws RemoteException {
    int[] res = new int[sqlQuery.length];
    for(int i=0;i<sqlQuery.length;i++)
      res[i] = executeUpdate(sqlQuery[i],params[i]);
    return res;
  }

  @Override
  public Client getClient() throws RemoteException {
    return client;
  }

  @Override
  public synchronized void clearEvents() throws RemoteException {
    events.clear();
  }
  
  /*@Override
  public void insertEvent(int index, javax.jms.Message event) throws RemoteException {
    if(!events.contains(event))
      events.insertElementAt(event, index);
  }*/
  
  /*@Override
  public void addEvent(javax.jms.Message event) throws RemoteException {
    events.add(0, event);
  }*/
  
  @Override
  public void addEvent(Class<? extends MappingObject> objectClass, String type, Integer... ids) throws RemoteException {
    MessageProxy messageProxy = new MessageProxy(objectClass, type, null, ids);
    if(!events.contains(messageProxy))
      events.add(0, messageProxy);
  }
  
  @Override
  public void addEvent(Class<? extends MappingObject> objectClass, String type, Map<String,Object> objectEventProperty, Integer... ids) throws RemoteException {
    MessageProxy messageProxy = new MessageProxy(objectClass, type, objectEventProperty, ids);
    if(!events.contains(messageProxy))
      events.add(0, messageProxy);
  }
  
  /*public javax.jms.Message createTopicMessage(Class<? extends MappingObject> objectClass, String type, Integer[] ids) throws RemoteException {
    try {
      javax.jms.Message message = topicSession.createObjectMessage(ids);
      message.setJMSType(type);
      message.setStringProperty("topic.name",  objectClass.toString());
      return message;
    }catch(JMSException ex) {
      throw new RemoteException(ex.getMessage());
    }
  }*/
  
  @Override
  public void toTypeObjects(Class<? extends MappingObject> objectClass, Integer[] ids, MappingObject.Type type) throws RemoteException {
    addEvent(objectClass, "UPDATE", ids);
    executeUpdate("UPDATE ["+objectClass.getName()+"] SET ["+objectClass.getName()+"(type)]=? WHERE id=ANY(?)", new Object[]{type, ids});
  }
  
  @Override
  public void toTmpObjects(Class<? extends MappingObject> objectClass, Integer[] ids, boolean tmp) throws RemoteException {
    addEvent(objectClass, "UPDATE", ids);
    executeUpdate("UPDATE ["+objectClass.getName()+"] SET ["+objectClass.getName()+"(tmp)]=? WHERE id=ANY(?)", new Object[]{tmp, ids});
  }
  
  @Override
  public boolean removeObject(MappingObject object) throws RemoteException {
    addEvent(DataBase.get(object).getInterfacesClass(), "REMOVE", new Integer[]{object.getId()});
    return executeUpdate("DELETE FROM "+DataBase.get(object).getName()+" WHERE id=?", new Object[]{object.getId()}) > 0;
  }
  
  @Override
  public int removeObjects(Class<? extends MappingObject> objectClass, Integer[] ids) throws RemoteException {
    return removeObjects(objectClass, null, ids);
  }
  
  @Override
  public int removeObjects(Class<? extends MappingObject> objectClass, Map objectEventProperty, Integer... ids) throws RemoteException {
    addEvent(objectClass, "REMOVE", objectEventProperty, ids);
    return executeUpdate("DELETE FROM "+DataBase.get(objectClass).getName()+" WHERE id=ANY(?)", new Object[]{ids});
  }
  
  @Override
  public int toArchive(Class<? extends MappingObject> objectClass, Integer objectid) throws RemoteException, IOException {
    return toArchive(objectClass, objectid, object(objectClass, objectid));
  }
  
  @Override
  public int toArchive(Class<? extends MappingObject> objectClass, Integer objectid, Map<String, Object> object) throws RemoteException, IOException {
    return createObject(Archive.class, PropertyMap.create().setValue("objectclass", objectClass.getName()).setValue("classid", objectid).setValue("object", object).getSimpleMap());
  }
  
  @Override
  public List<Map> getArchive(Class<? extends MappingObject> objectClass, Integer objectid) throws Exception {
    List<Map> list = getList(DBFilter.create(Archive.class).AND_EQUAL("objectclass", objectClass.getName()).AND_EQUAL("classid", objectid));
    list.sort((Map o1, Map o2) -> ((Timestamp)o1.get("date")).after((Timestamp)o2.get("date")) ? -1 : ((Timestamp)o1.get("date")).before((Timestamp)o2.get("date")) ? 1 : 0);
    return list;
  }
  
  @Override
  public Map getLastArchive(Class<? extends MappingObject> objectClass, Integer objectid) throws Exception {
    List<Map> list = getArchive(objectClass, objectid);
    if(list.isEmpty())
      return null;
    list.sort((Map o1, Map o2) -> ((Timestamp)o1.get("date")).after((Timestamp)o2.get("date")) ? -1 : ((Timestamp)o1.get("date")).before((Timestamp)o2.get("date")) ? 1 : 0);
    return list.get(0);
  }
  
  @Override
  public boolean saveObject(MappingObject object) throws RemoteException {
    String query = "";
    Object[] params = new Object[0];
    DBTable table = DataBase.get(object);
    Class objectClass = table.getInterfacesClass();
    Integer id = object.getId();
    query = (id==null?"INSERT INTO "+table.getName()+"(":("UPDATE "+table.getName()+" SET "));
    for(DBColumn column:table.getColumns()) {
      if(!column.isIdentify() && column.getField().getName().intern() != "modificationDate".intern() && column.getField().getName().intern() != "lastUserId".intern()) {
        Object value = column.getValue(object);
        if(!(value == null && !column.isNullAble())) {
          if(value == null && column.getDefaultValue() != null && !column.getDefaultValue().equals("null")) {
            if(column.getField().getType() == Integer.class)
              value = Integer.valueOf((String)column.getDefaultValue());
            if(column.getField().getType() == Double.class)
              value = Double.valueOf((String)column.getDefaultValue());
            if(column.getField().getType() == Float.class)
              value = Float.valueOf((String)column.getDefaultValue());
            if(column.getField().getType() == Long.class)
              value = Long.valueOf((String)column.getDefaultValue());
            if(column.getField().getType() == BigDecimal.class)
              value = BigDecimal.valueOf(Double.valueOf((String)column.getDefaultValue()));
          }
          query += id==null?column.getName()+",":(column.getName()+"=?,");
          params = ArrayUtils.add(params, value);
        }
      }
    }

    try {
      Delegator delegator = (Delegator) Proxy.getInvocationHandler(object);
      for(DBRelation relation:table.getRelations()) {
        if(relation instanceof ManyToOne) {
          DBColumn column = ((ManyToOne)relation).getColumn();
          Method method = objectClass.getMethod(relation.getGetMethod(), new Class[0]);
          MappingObject obj = (MappingObject) delegator.invokeWithoutProxy(method, new Object[0]);
          method = null;
          if(obj != null) {
            query += id==null?column.getName()+",":(column.getName()+"=?,");
            params = ArrayUtils.add(params, obj.getId());
          }
        }
      }
    }catch(Throwable ex) {
      throw new RemoteException(ex.getMessage());
    }

    query = query.substring(0,query.length()-1);
    if(id == null) {
      query += ") VALUES (";
      for(int i=0;i<params.length;i++)
        query += "?,";
      query = query.substring(0, query.length()-1)+")";
    }else query += " WHERE id="+id;

    boolean originalAutoCommit = this.autoCommit;
    this.autoCommit = false;

    int count = executeUpdate(query.intern(), params);
    if(id == null && count == 1) {
      List<List> data = executeQuery("SELECT MAX(id) FROM "+table.getName());
      object.setId((Integer) data.get(0).get(0));
    }
    reloadObject(object);
    addEvent(DataBase.get(object).getInterfacesClass(), "UPDATE", new Integer[]{object.getId()});

    if(originalAutoCommit)
      commit();
    
    this.autoCommit = originalAutoCommit;

    return count == 1;
  }
  
  @Override
  public boolean saveObject(Map<String, Object> map) throws RemoteException, IOException {
    return saveObject((Class<? extends MappingObject>)map.get("class"), (Integer)map.get("id"), map);
  }
  
  @Override
  public boolean saveObject(Class<? extends MappingObject> objectClass, Map<String, Object> map) throws RemoteException, IOException {
    return saveObject(objectClass, (Integer)map.get("id"), map);
  }
  
  @Override
  public boolean saveObject(Class<? extends MappingObject> objectClass, Integer id, Map<String, Object> map) throws RemoteException, IOException {
    return saveObject(objectClass, id, map, "UPDATE");
  }
  
  public boolean saveObject(Class<? extends MappingObject> objectClass, Integer id, Map<String, Object> map, String eventType) throws RemoteException, IOException {
    if(!map.isEmpty() && objectClass != null && id != null) {
      DBTable table = DataBase.get(objectClass);
      StringBuilder sb = new StringBuilder("UPDATE ["+objectClass.getName()+"] SET ");
      //String query = "UPDATE ["+objectClass.getName()+"] SET ";
      Object[] param = new Object[0];
      
      String[] subquerys = new String[0];
      Object[][] subparams = new Object[0][];
      
      for(String field:map.keySet()) {
        if(table.getColumn(field) != null) {
          sb.append("[!").append(objectClass.getName()).append("(").append(field).append(")]=?,");
          //query += "[!"+objectClass.getName()+"("+field+")]=?,";
          if(table.getColumn(field).isZip())
            param = ArrayUtils.add(param, GzipUtil.zip(map.get(field)));
          else if(table.getColumn(field).isGzip())
            param = ArrayUtils.add(param, GzipUtil.gzip(map.get(field)));
          else if((table.getColumn(field).getField().getType() == LocalDateTime.class || table.getColumn(field).getField().getType() == Timestamp.class) && "CURRENT_TIMESTAMP".equals(map.get(field))) {
            sb.replace(sb.length()-2, sb.length(), "CURRENT_TIMESTAMP,");
            //query = query.substring(0, query.length()-2)+"CURRENT_TIMESTAMP,";
          }else param = ArrayUtils.add(param, map.get(field));
        }else if(table.getRelation(field) != null) {
          DBRelation relation = table.getRelation(field);
          if(relation instanceof ManyToOne) {
            sb.append("[!").append(objectClass.getName()).append("(").append(field).append(")]=?,");
            //query += "[!"+objectClass.getName()+"("+field+")]=?,";
            if(map.get(field) == null || map.get(field) instanceof Integer)
              param = ArrayUtils.add(param, map.get(field));
            else if(map.get(field) instanceof Map)
              param = ArrayUtils.add(param, ((Map)map.get(field)).get("id"));
          }
          if(relation instanceof OneToMany) {
            OneToMany r = (OneToMany) relation;
            Integer[] ids = null;
            Object[] array = map.get(field) == null ? new Object[0] : map.get(field) instanceof List ? ((List)map.get(field)).toArray() : (Object[])map.get(field);
            for(Object object:array) {
              if(object instanceof Integer)
                ids = ArrayUtils.add(ids, (Integer)object);
              if(object instanceof Map && ((Map)object).containsKey("id"))
                ids = ArrayUtils.add(ids, (Integer)((Map)object).get("id"));
            }
            subquerys = ArrayUtils.add(subquerys, "UPDATE "+r.getTargetTable().getName()+" SET "+r.getTargetColumnName()+"=? WHERE "+r.getTargetColumnName()+"=?");
            subparams = ArrayUtils.add(subparams, new Object[]{null,id});
            if(ids != null && ids.length > 0) {
              subquerys = ArrayUtils.add(subquerys, "UPDATE "+r.getTargetTable().getName()+" SET "+r.getTargetColumnName()+"=? WHERE id=ANY(?)");
              subparams = ArrayUtils.add(subparams, new Object[]{id,ids});
            }
          }
          if(relation instanceof ManyToMany) {
            ManyToMany r = (ManyToMany) relation;
            Integer[] ids = null;
            Object[] array = map.get(field) == null ? new Object[0] : map.get(field) instanceof List ? ((List)map.get(field)).toArray() : (Object[])map.get(field);
            for(Object object:array) {
              if(object instanceof Integer)
                ids = ArrayUtils.add(ids, (Integer)object);
              if(object instanceof Map && ((Map)object).containsKey("id"))
                ids = ArrayUtils.add(ids, (Integer)((Map)object).get("id"));
            }
            subquerys = ArrayUtils.add(subquerys, "DELETE FROM "+r.getRelationTableName()+" WHERE "+r.getObjectColumnName()+"=?");
            subparams = ArrayUtils.add(subparams, new Object[]{id});
            if(ids != null && ids.length > 0) {
              String q = "INSERT INTO "+r.getRelationTableName()+"("+r.getObjectColumnName()+","+r.getTargetColumnName()+") VALUES ";
              Integer[] p = new Integer[0];
              for(Integer rid:ids) {
                q += "(?,?),";
                p = ArrayUtils.addAll(p, id,rid);
              }
              subquerys = ArrayUtils.add(subquerys, q.substring(0, q.length()-1));
              subparams = ArrayUtils.add(subparams, p);
            }
          }
        }
      }
      
      addEvent(objectClass, eventType, id);
      
      if(subquerys.length > 0)
        executeUpdate(subquerys, subparams);
      
      return executeUpdate(sb.replace(sb.length()-1, sb.length(), "").append(" WHERE [!").append(objectClass.getName()).append("(id)]=?").toString(), ArrayUtils.add(param, id)) == 1;
      //return executeUpdate(query.substring(0, query.length()-1)+" WHERE [!"+objectClass.getName()+"(id)]=?", ArrayUtils.add(param, id)) == 1;
    }return false;
  }
  
  @Override
  public Integer createObject(Class<? extends MappingObject> objectClass, Map<String,Object> map) throws RemoteException, IOException {
    return createObject(objectClass, map, null);
  }
  
  @Override
  public Integer createObject(Class<? extends MappingObject> objectClass, Map<String,Object> map, Map<String,Object> objectEventProperty) throws RemoteException, IOException {
    Integer id = null;
    if(!map.isEmpty() && objectClass != null) {
      DBTable table = DataBase.get(objectClass);
      
      String query = "INSERT INTO ["+objectClass.getName()+"] (";
      String values = " VALUES (";
      Object[] params = new Object[0];
      
      String[] subquerys = new String[0];
      Object[][] subparams = new Object[0][];
      
      for(String field:map.keySet()) {
        if(table.getColumn(field) != null) {
          
          query += "[!"+objectClass.getName()+"("+field+")],";
          if(table.getColumn(field).isZip()) {
            params = ArrayUtils.add(params, GzipUtil.zip(map.get(field)));
            values += "?,";
          }else if(table.getColumn(field).isGzip()) {
            params = ArrayUtils.add(params, GzipUtil.gzip(map.get(field)));
            values += "?,";
          }else if((table.getColumn(field).getField().getType() == LocalDateTime.class || table.getColumn(field).getField().getType() == Timestamp.class) && "CURRENT_TIMESTAMP".equals(map.get(field)))
            values += "CURRENT_TIMESTAMP,";
          else {
            params = ArrayUtils.add(params, map.get(field));
            values += "?,";
          }
        }else if(table.getRelation(field) != null) {
          DBRelation relation = table.getRelation(field);
          
          if(relation instanceof ManyToOne) {
            query += "[!"+objectClass.getName()+"("+field+")],";
            values += "?,";
            if(map.get(field) == null || map.get(field) instanceof Integer)
              params = ArrayUtils.add(params, map.get(field));
            else if(map.get(field) instanceof Map)
              params = ArrayUtils.add(params, ((Map)map.get(field)).get("id"));
          }
          
          if(relation instanceof OneToMany) {
            OneToMany r = (OneToMany) relation;
            Integer[] ids = null;
            Object[] array = map.get(field) == null ? new Object[0] : map.get(field) instanceof List ? ((List)map.get(field)).toArray() : (Object[])map.get(field);
            for(Object object:array) {
              if(object instanceof Integer)
                ids = ArrayUtils.add(ids, (Integer)object);
              if(object instanceof Map && ((Map)object).containsKey("id"))
                ids = ArrayUtils.add(ids, (Integer)((Map)object).get("id"));
            }
            subquerys = ArrayUtils.add(subquerys, "UPDATE "+r.getTargetTable().getName()+" SET "+r.getTargetColumnName()+"=? WHERE "+r.getTargetColumnName()+"=(SELECT MAX(id) FROM ["+objectClass.getName()+"])");
            subparams = ArrayUtils.add(subparams, new Object[]{null});
            if(ids != null && ids.length > 0) {
              subquerys = ArrayUtils.add(subquerys, "UPDATE "+r.getTargetTable().getName()+" SET "+r.getTargetColumnName()+"=(SELECT MAX(id) FROM ["+objectClass.getName()+"]) WHERE id=ANY(?)");
              subparams = ArrayUtils.add(subparams, new Object[]{ids});
            }
          }
          
          if(relation instanceof ManyToMany) {
            ManyToMany r = (ManyToMany) relation;
            Integer[] ids = null;
            Object[] array = map.get(field) == null ? new Object[0] : map.get(field) instanceof List ? ((List)map.get(field)).toArray() : (Object[])map.get(field);
            for(Object object:array) {
              if(object instanceof Integer)
                ids = ArrayUtils.add(ids, (Integer)object);
              if(object instanceof Map && ((Map)object).containsKey("id"))
                ids = ArrayUtils.add(ids, (Integer)((Map)object).get("id"));
            }
            subquerys = ArrayUtils.add(subquerys, "DELETE FROM "+r.getRelationTableName()+" WHERE "+r.getObjectColumnName()+"=(SELECT MAX(id) FROM ["+objectClass.getName()+"])");
            subparams = ArrayUtils.add(subparams, new Object[]{});
            if(ids != null && ids.length > 0) {
              String q = "INSERT INTO "+r.getRelationTableName()+"("+r.getObjectColumnName()+","+r.getTargetColumnName()+") VALUES ";
              Integer[] p = new Integer[0];
              for(Integer rid:ids) {
                q += "((SELECT MAX(id) FROM ["+objectClass.getName()+"]),?),";
                p = ArrayUtils.addAll(p, rid);
              }
              subquerys = ArrayUtils.add(subquerys, q.substring(0, q.length()-1));
              subparams = ArrayUtils.add(subparams, p);
            }
          }
        }
      }
      
      boolean originalAutoCommit = this.autoCommit;
      this.autoCommit = false;
      
      query = query.substring(0, query.length()-1)+")"+values.substring(0, values.length()-1)+")";
      
      executeUpdate(query,params);
      
      if(subquerys.length > 0)
        executeUpdate(subquerys, subparams);
      
      id = (Integer) executeQuery("SELECT MAX(id) FROM ["+objectClass.getName()+"]").get(0).get(0);
      map.put("id", id);
      
      addEvent(objectClass, "CREATE", objectEventProperty, id);
      
      //if(!saveObject(objectClass,id,map,"CREATE"))
        //id = null;
      
      if(originalAutoCommit)
        commit();
    
      this.autoCommit = originalAutoCommit;
      
    }
    return id;
  }
  
  /*@Override
  public Integer createObject(Class<? extends MappingObject> objectClass, Map<String,Object> map) throws RemoteException {
    String params = "";
    String values = "";

    for(String field:map.keySet()) {
      params += ",["+objectClass.getName()+"("+field+")]";
      values += ",?";
    }

    if(!params.equals("")) {
      params = params.substring(1);
      values = values.substring(1);
    }
    String query = "INSERT INTO ["+objectClass.getName()+"]("+params+") VALUES("+values+")";
    
    boolean originalAutoCommit = this.autoCommit;
    this.autoCommit = false;
    
    int count = executeUpdate(query, map.values().toArray());
    if(count == 0)
      return null;
    Vector<Vector> data = executeQuery("SELECT MAX(id) FROM ["+objectClass.getName()+"]");
    Integer id = (Integer) data.get(0).get(0);

    addEvent(objectClass, "CREATE", new Integer[]{id});
    
    if(originalAutoCommit)
      commit();
    
    this.autoCommit = originalAutoCommit;
    
    return id;
  }*/
  
  
  
  
  @Override
  public List<List> getData(Class<? extends MappingObject> objectClass, String[] fields) throws RemoteException {
    return getData(DBFilter.create(objectClass), fields);
  }
  
  @Override
  public List<List> getData(DBFilter filter, String[] fields) throws RemoteException {
    return getData(filter, fields, null);
  }
  
  @Override
  public List<List> getData(Class<? extends MappingObject> objectClass, String[] fields, String[] fieldsOrderBy) throws RemoteException {
    return getData(DBFilter.create(objectClass), fields, fieldsOrderBy);
  }
  
  @Override
  public List<List> getData(DBFilter filter, String[] fields, String[] fieldsOrderBy) throws RemoteException {
    String where = "";
    Object[] parmas = new Object[0];
    if(filter != null && !filter.isEmpty()) {
      where += "WHERE "+filter;
      parmas = filter.getValues();
    }
    return getData(filter.getTargetClass(), where, fields, fieldsOrderBy, parmas);
  }
  
  @Override
  public List<List> getData(Class<? extends MappingObject> objectClass, Integer[] ids, String[] fields) throws RemoteException {
    return getData(objectClass, ids, fields, null);
  }
  
  @Override
  public List<List> getData(Class<? extends MappingObject> objectClass, Integer[] ids, String[] fields, String[] fieldsOrderBy) throws RemoteException {
    String where = "WHERE id=ANY(?)";
    Object[] objectList = new Object[]{ids};
    return getData(objectClass, where, fields, fieldsOrderBy, objectList);
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  @Override
  public List<Map> getList(DBFilter filter, String... fields) throws Exception {
    DBTable table = DataBase.get(filter.getTargetClass());
    
    if(fields == null || fields.length == 0) {
      for(DBColumn column:table.getColumns())
        fields = ArrayUtils.add(fields, column.getField().getName());
      for(DBRelation relation:table.getRelations()) {
        fields = ArrayUtils.add(fields, relation.getField().getName());
        if(relation instanceof ManyToOne) {
          for(String name:((ManyToOne) relation).getViewNames())
            fields = ArrayUtils.add(fields, name);
        }
      }
    }
    
    String orderby = "";
    String groupby = "";
    
    for(int i=fields.length-1; i>=0; i--) {
      if(fields[i].startsWith("sort:")) {
        orderby = " ORDER BY "+String.join(", ", Arrays.stream(fields[i].substring(5).split(",")).map(f -> table.getColumn(f.trim()) == null && table.getRelation(f.trim()) == null ? f : ("["+filter.getTargetClass().getName()+"("+f.trim()+")]")).collect(Collectors.toList()));
        fields = ArrayUtils.remove(fields, i);
      }else if(fields[i].startsWith("group by:")) {
        groupby = " GROUP BY "+String.join(", ", Arrays.stream(fields[i].substring(9).split(",")).map(f -> table.getColumn(f.trim()) == null && table.getRelation(f.trim()) == null ? f : ("["+filter.getTargetClass().getName()+"("+f.trim()+")]")).collect(Collectors.toList()));
        fields = ArrayUtils.remove(fields, i);
      }
    }
    
    Entry<String[],String[]> key_fields = get_keys_fields(fields);
    String[] keys = key_fields.getKey();
    String[] vals = key_fields.getValue();
    
    
    List<List> data = executeQuery("SELECT "
            +get_selected_fields(filter.getTargetClass().getName(), key_fields.getValue())
            +" FROM ["+filter.getTargetClass().getName()+"]" 
            + (filter != null && !filter.isEmpty() ? " WHERE "+filter : "")+groupby+orderby,
            filter != null && !filter.isEmpty() ? filter.getValues() : new Object[0]);
    
    List<Map> list = new ArrayList();
    for(List row:data) {
      HashMap<String,Object> map = new HashMap<>();
      for(int i=0;i<row.size();i++) {
        DBColumn column = null;
        if(!key_fields.getValue()[i].startsWith("query:")) 
          column = table.getColumn(key_fields.getValue()[i]);
        map.put(key_fields.getKey()[i], column != null ? column.cast(row.get(i)) : row.get(i));
      }
      list.add(map);
    }
    return list;
  }
  
  private Map.Entry<String[],String[]> get_keys_fields(String... fields) {
    String[] keys = Arrays.copyOf(fields, fields.length);
    for(int i=0;i<fields.length;i++) {
      if(fields[i].startsWith("query:")) {
        keys[i] = "query-"+System.currentTimeMillis();
      }else if(fields[i].contains("=query:")) {
        keys[i]   = keys[i].substring(0, keys[i].indexOf("=query:"));
        fields[i] = fields[i].substring(fields[i].indexOf("query:"));
        if(!fields[i].substring(6).matches("^[а-яА-ЯёЁa-zA-Z0-9_-]+:.*"))
          fields[i] = fields[i].replace("query:", "query:"+keys[i].replaceAll("-", "_")+":");
      }else if(fields[i].contains(":=:")) {
        keys[i]   = keys[i].substring(0, keys[i].indexOf(":=:"));
        fields[i] = fields[i].substring(fields[i].indexOf(":=:")+3);
      }
    }
    return new AbstractMap.SimpleEntry<>(keys,fields);
  }
  
  private String get_selected_fields(String tablename, String... fields) {
    StringBuilder selectFieldsString = new StringBuilder();
    List<String> fieldAliases = new ArrayList<>();
    if(fields != null && fields.length > 0) {
      for(String field:fields) {
        if(field.startsWith("query:")) {
          int index = field.indexOf(":")+1;
          String as = "";
          if(field.substring(index).matches("^[а-яА-ЯёЁa-zA-Z0-9_-]+:.*")) {
            index += field.substring(index).indexOf(":")+1;
            as = " as "+field.substring(field.indexOf(":")+1, index-1).replaceAll("-", "_");
            fieldAliases.add(field.substring(field.indexOf(":")+1, index-1));
          }
          selectFieldsString.append(",(").append(field.substring(index)).append(")").append(as);
        }else selectFieldsString.append(",[").append(tablename).append("(").append(field).append(")]");
      }
      return selectFieldsString.substring(1);
    }else return "*";
  }
  
  
  
  //GZIP
  
  @Override
  public byte[] getGZipData(Class<? extends MappingObject> objectClass, String[] fields) throws RemoteException {
    return getGZipData(DBFilter.create(objectClass), fields);
  }
  
  @Override
  public byte[] getGZipData(DBFilter filter, String[] fields) throws RemoteException {
    return getGZipData(filter, fields, null);
  }
  
  @Override
  public byte[] getGZipData(Class<? extends MappingObject> objectClass, String[] fields, String[] fieldsOrderBy) throws RemoteException {
    return getGZipData(DBFilter.create(objectClass), fields, fieldsOrderBy);
  }
  
  @Override
  public byte[] getGZipData(DBFilter filter, String[] fields, String[] fieldsOrderBy) throws RemoteException {
    List<List> data = null;
    try {
      String where = "";
      Object[] parmas = new Object[0];
      if(filter != null && !filter.isEmpty()) {
        where += "WHERE "+filter;
        parmas = filter.getValues();
      }
      data = getData(filter.getTargetClass(), where, fields, fieldsOrderBy, parmas);
      return GzipUtil.gzip(data);
    }catch(IOException ex) {
      ex.printStackTrace();
      throw new RemoteException(ex.getMessage());
    }
  }
  
  @Override
  public byte[] getGZipData(DBFilter filter, String[] fields, String[] fieldsOrderBy, String[] fieldsGroupBy) throws RemoteException {
    List<List> data = null;
    try {
      String where = "";
      Object[] parmas = new Object[0];
      if(filter != null && !filter.isEmpty()) {
        where += "WHERE "+filter;
        parmas = filter.getValues();
      }
      data = getData(filter.getTargetClass(), where, fields, fieldsOrderBy, fieldsGroupBy, parmas);
      return GzipUtil.gzip(data);
    }catch(IOException ex) {
      ex.printStackTrace();
      throw new RemoteException(ex.getMessage());
    }
  }
  
  @Override
  public byte[] getGZipData(Class<? extends MappingObject> objectClass, Integer[] ids, String[] fields) throws RemoteException {
    return getGZipData(objectClass, ids, fields, null);
  }
  
  @Override
  public byte[] getGZipData(Class<? extends MappingObject> objectClass, Integer[] ids, String[] fields, String[] fieldsOrderBy) throws RemoteException {
    try {
      return GzipUtil.gzip(getData(objectClass, "WHERE id=ANY(?)", fields, fieldsOrderBy, new Object[]{ids}));
    }catch(IOException ex) {
      throw new RemoteException(ex.getMessage());
    }
  }
  
  private List<List> getData(Class<? extends MappingObject> objectClass, String where, String[] fields, String[] fieldsOrderBy, Object[] objectlist) throws RemoteException {
    return getData(objectClass, where, fields, fieldsOrderBy, null, objectlist);
  }
  
  private List<List> getData(Class<? extends MappingObject> objectClass, String where, String[] fields, String[] fieldsOrderBy, String[] fieldsGroupBy, Object[] objectlist) throws RemoteException {
    String selectFieldsString = "";
    List<String> fieldAliases = new ArrayList<>();
    
    final DBTable table = DataBase.get(objectClass);
    
    if(fields == null || fields.length == 0) {
      for(DBColumn column:table.getColumns())
        fields = ArrayUtils.add(fields, column.getField().getName());
      for(DBRelation relation:table.getRelations()) {
        fields = ArrayUtils.add(fields, relation.getField().getName());
        if(relation instanceof ManyToOne) {
          for(String name:((ManyToOne) relation).getViewNames())
            fields = ArrayUtils.add(fields, name);
        }
      }
    }
    
    if(fields != null && fields.length > 0) {
      for(String field:fields) {
        if(field.startsWith("query:")) {
          int index = field.indexOf(":")+1;
          String as = "";
          if(field.substring(index).matches("^[а-яА-ЯёЁa-zA-Z0-9_-]+:.*")) {
            index += field.substring(index).indexOf(":")+1;
            as = " as "+field.substring(field.indexOf(":")+1, index-1).replaceAll("-", "_");
            fieldAliases.add(field.substring(field.indexOf(":")+1, index-1));
          }
          selectFieldsString += ",("+field.substring(index)+")"+as;
        }else selectFieldsString += ",["+objectClass.getName()+"("+field+")]";
      }
      selectFieldsString = selectFieldsString.substring(1).intern();
    }else selectFieldsString = "*";

    String orderFieldsString = "";
    if(fieldsOrderBy != null && fieldsOrderBy.length > 0) {
      for(String field:fieldsOrderBy) {
        if(field != null) {
          String[] fs = field.trim().split(" ");
          if(fs.length > 0)
            orderFieldsString += fieldAliases.contains(fs[0]) ? ","+fs[0]+(fs.length > 1?" "+fs[fs.length-1]:"") : ",["+objectClass.getName()+"("+fs[0]+")]"+(fs.length > 1?" "+fs[fs.length-1]:"");
        }
      }
      if(!orderFieldsString.equals(""))
        orderFieldsString = " ORDER BY "+orderFieldsString.substring(1).intern();
    }
    
    String groupFieldsString = "";
    if(fieldsGroupBy != null && fieldsGroupBy.length > 0) {
      for(String field:fieldsGroupBy) {
        if(field != null) {
          String[] fs = field.trim().split(" ");
          if(fs.length > 0)
            groupFieldsString += fieldAliases.contains(fs[0]) ? ","+fs[0]+(fs.length > 1?" "+fs[fs.length-1]:"") : ",["+objectClass.getName()+"("+fs[0]+")]"+(fs.length > 1?" "+fs[fs.length-1]:"");
        }
      }
      if(!groupFieldsString.equals(""))
        groupFieldsString = " GROUP BY "+groupFieldsString.substring(1).intern();
    }

    List<List> data = executeQuery("SELECT "+selectFieldsString+" FROM ["+objectClass.getName()+"] " + where + groupFieldsString + orderFieldsString, objectlist);
    for(List row:data) {
      for(int i=0;i<row.size();i++) {
        if(!fields[i].startsWith("query:")) {
          try {
            DBColumn column = table.getColumn(fields[i]);
            if(column != null)
              row.set(i, column.cast(row.get(i)));
          }catch(Exception ex) {
            Logger.getLogger(Session.class).error(ex);
            ex.printStackTrace();
          }
        }
      }
    }
    return data;
  }
  
  
  
  
  
  
  
  @Override
  public MappingObject getObject(Class<? extends MappingObject> objectClass, Integer id) throws RemoteException {
    MappingObject[] objects = getObjects(new Integer[]{id}, DBFilter.create(objectClass), null);
    return objects!=null&&objects.length==1?objects[0]:null;
  }
  
  @Override
  public MappingObject[] getObjects(Class<? extends MappingObject> objectClass, Integer[] ids) throws RemoteException {
    return getObjects(ids, DBFilter.create(objectClass));
  }
  
  @Override
  public MappingObject[] getObjects(Integer[] ids, DBFilter filter) throws RemoteException {
    return getObjects(ids, filter, null);
  }
  
  @Override
  public MappingObject[] getObjects(Class<? extends MappingObject> objectClass) throws RemoteException {
    return getObjects(null, DBFilter.create(objectClass), null);
  }

  @Override
  public MappingObject[] getObjects(Class<? extends MappingObject> objectClass, String[] fieldsOrderBy) throws RemoteException {
    return getObjects(null, DBFilter.create(objectClass), fieldsOrderBy);
  }
  
  
  @Override
  public MappingObject[] getObjects(DBFilter filter) throws RemoteException {
    return getObjects(null, filter, null);
  }

  @Override
  public MappingObject[] getObjects(DBFilter filter, String[] fieldsOrderBy) throws RemoteException {
    return getObjects(null, filter, fieldsOrderBy);
  }
  
  @Override
  public MappingObject[] getObjects(Integer[] ids, DBFilter filter, String[] fieldsOrderBy) throws RemoteException {
    DBTable table = DataBase.get(filter.getTargetClass());
    MappingObject[] objects = new MappingObject[0];

    String selectFieldsString = "";
    for(DBColumn column:table.getColumns())
      selectFieldsString += ",["+filter.getTargetClass().getName()+"("+column.getField().getName()+")]";
    
    selectFieldsString = selectFieldsString.substring(1).intern();

    Object[] params = new Object[0];
    String sqlQuery = "SELECT "+selectFieldsString+" FROM ["+filter.getTargetClass().getName()+"]";

    if(ids != null && ids.length > 0 || filter != null && !filter.isEmpty()) {
      sqlQuery += " WHERE ";
      if(ids != null && ids.length > 0) {
        sqlQuery += "id=ANY(?)";
        params = ArrayUtils.add(params, ids);
      }
      if(filter != null && !filter.isEmpty()) {
        sqlQuery += (params.length>0?" AND ":"")+filter;
        params = ArrayUtils.addAll(params, filter.getValues());
      }
    }

    ResultSet rs = null;
    PreparedStatement st = null;
    try {
      st = getConnection().prepareStatement(DBTable.replaceString(sqlQuery));
      setParams(st, params);
      Logger.getLogger(getClass()).debug(st);
      rs = st.executeQuery();

      while(rs.next()) {
        MappingObject object = createEmptyObject(filter.getTargetClass());
        for(DBColumn column:table.getColumns()) {
          if(rs.getObject(column.getName()) instanceof Array)
            column.setValue(object, rs.getArray(column.getName()).getArray());
          else column.setValue(object, rs.getObject(column.getName()));
        }
        objects = (MappingObject[]) ArrayUtils.add(objects, object);
      }

    }catch(SQLException | IOException ex) {
      rollback();
      close();
      Logger.getLogger(getClass()).error(st+"\n",ex);
      throw new RemoteException(ex.getMessage());
    }finally {
      try {rs.close();}catch(Exception e) {}
      try {st.close();}catch(Exception e) {}
      if(autoCommit)
        close();
    }
    return objects;
  }
  
  
  @Override
  public Map object(Class<? extends MappingObject> objectClass, Integer id) throws RemoteException {
    Map[] objects = objects(new Integer[]{id}, DBFilter.create(objectClass), null);
    return objects!=null&&objects.length==1?objects[0]:null;
  }
  
  @Override
  public Map[] objects(Class<? extends MappingObject> objectClass, Integer[] ids) throws RemoteException {
    return objects(ids, DBFilter.create(objectClass));
  }
  
  @Override
  public Map[] objects(Integer[] ids, DBFilter filter) throws RemoteException {
    return objects(ids, filter, null);
  }
  
  @Override
  public Map[] objects(Class<? extends MappingObject> objectClass, Integer[] ids, String[] fieldsOrderBy) throws RemoteException {
    return objects(ids, DBFilter.create(objectClass), fieldsOrderBy);
  }
  
  @Override
  public Map[] objects(Class<? extends MappingObject> objectClass) throws RemoteException {
    return objects(new Integer[0], DBFilter.create(objectClass), null);
  }
  
  @Override
  public Map[] objects(Class<? extends MappingObject> objectClass, String[] fieldsOrderBy) throws RemoteException {
    return objects(new Integer[0], DBFilter.create(objectClass), fieldsOrderBy);
  }

  @Override
  public Map[] objects(DBFilter filter) throws RemoteException {
    return objects(new Integer[0], filter, null);
  }
  
  @Override
  public Map[] objects(DBFilter filter, String[] fieldsOrderBy) throws RemoteException {
    return objects(new Integer[0], filter, fieldsOrderBy);
  }
  
  @Override
  public Map[] objects(Integer[] ids, DBFilter filter, String[] fieldsOrderBy) throws RemoteException {
    DBTable table = DataBase.get(filter.getTargetClass());
    Map[] objects = new Map[0];
    
    if(ids != null || filter != null) {
      String selectFieldsString = "";
      for(DBColumn column:table.getColumns())
        selectFieldsString += ",["+filter.getTargetClass().getName()+"("+column.getField().getName()+")]";
      for(DBRelation relation:table.getRelations())
        selectFieldsString += ",["+filter.getTargetClass().getName()+"("+relation.getField().getName()+")] AS "+relation.getField().getName();

      selectFieldsString = selectFieldsString.substring(1).intern();

      Object[] params = new Object[0];
      String sqlQuery = "SELECT "+selectFieldsString+" FROM ["+filter.getTargetClass().getName()+"]";

      if(ids.length > 0 || !filter.isEmpty()) {
        sqlQuery += " WHERE ";
        if(ids != null && ids.length > 0) {
          sqlQuery += "id=ANY(?)";
          params = ArrayUtils.add(params, ids);
        }
        if(filter != null && !filter.isEmpty()) {
          sqlQuery += (params.length>0?" AND ":"")+filter;
          params = ArrayUtils.addAll(params, filter.getValues());
        }
      }

      ResultSet rs = null;
      PreparedStatement st = null;
      try {
        st = getConnection().prepareStatement(DBTable.replaceString(sqlQuery));
        setParams(st, params);
        Logger.getLogger(getClass()).debug(st);
        rs = st.executeQuery();

        while(rs.next()) {
          Map object = new TreeMap();
          for(DBColumn column:table.getColumns()) {
            /*if(rs.getObject(column.getName()) instanceof Array)
              object.put(column.getField().getName(), rs.getArray(column.getName()).getArray());
            else */object.put(column.getField().getName(), column.cast(rs.getObject(column.getName())));
          }
          for(DBRelation relation:table.getRelations()) {
            //Session session = new Session(true);
            if(rs.getObject(relation.getField().getName()) instanceof Array){
              object.put(relation.getField().getName(), rs.getArray(relation.getField().getName()).getArray());
              //object.put(relation.getField().getName(), session.objects(relation.getTargetClass(), (Integer[])rs.getArray(relation.getField().getName()).getArray()));
            }else {
              object.put(relation.getField().getName(), rs.getObject(relation.getField().getName()));
              //object.put(relation.getField().getName(), session.object(relation.getTargetClass(), rs.getInt(relation.getField().getName())));
            }
          }
          objects = (Map[]) ArrayUtils.add(objects, object);
        }

      }catch(Exception ex) {
        rollback();
        close();
        Logger.getLogger(getClass()).error(st+"\n",ex);
        throw new RemoteException(ex.getMessage());
      }finally {
        try {rs.close();}catch(Exception e) {}
        try {st.close();}catch(Exception e) {}
        if(autoCommit)
          close();
      }
    }
    return objects;
  }
  
  
  private void reloadObject(MappingObject object) throws RemoteException {
    DBTable table = DataBase.get(object);
    Class objectClass = table.getInterfacesClass();
    
    String selectFieldsString = "";
    for(DBColumn column:table.getColumns()) {
      selectFieldsString += ",["+objectClass.getName()+"("+column.getField().getName()+")]";
    }
    selectFieldsString = selectFieldsString.substring(1);
    ResultSet rs = null;
    PreparedStatement st = null;
    try {
      st = getConnection().prepareStatement(DBTable.replaceString("SELECT "+selectFieldsString+" "
              + "FROM ["+objectClass.getName()+"] WHERE id="+object.getId()));
      rs = st.executeQuery();
      while(rs.next()) {
        for(DBColumn column:table.getColumns()) {
          if(rs.getObject(column.getName()) instanceof Array)
            column.setValue(object, rs.getArray(column.getName()).getArray());
          else column.setValue(object, rs.getObject(column.getName()));
        }
      }
    }catch(RemoteException | SQLException ex) {
      rollback();
      close();
      Logger.getLogger(getClass()).error(st+"\n",ex);
      throw new RemoteException(ex.getMessage());
    }finally {
      try {rs.close();}catch(Exception e) {}
      try {st.close();}catch(Exception e) {}
      if(autoCommit)
        close();
    }
  }
  
  @Override
  public boolean isSatisfy(DBFilter filter, Integer id) throws RemoteException {
    return isSatisfy(filter, new Integer[]{id}).length == 1;
  }

  @Override
  public Integer[] isSatisfy(DBFilter filter, Integer[] ids) throws RemoteException {
    if(filter != null && !filter.isEmpty() && ids != null && ids.length > 0) {
      List<List> data = executeQuery("SELECT id FROM ["+filter.getTargetClass().getName()+"] WHERE id=ANY(?) AND ("+filter+")", 
              ArrayUtils.add(filter.getValues(), 0, ids));
      ids = new Integer[0];
      for(List d:data)
        ids = (Integer[]) ArrayUtils.add(ids, d.get(0));
      return ids;
    }
    return new Integer[0];
  }
  
  private String establish(DBTable table, DBFilter filter, MappingObject object, ArrayList<Object> values) throws RemoteException {
    String query = "";
    for(int i=0;i<filter.size();i++) {
      DBExpretion expretion = filter.get(i);
      if(expretion.getLogic() == Logic.OR)
        break;
      if(expretion instanceof DBFilter)
        query += establish(table, (DBFilter) expretion, object, values);
      if(!(expretion instanceof DBFilter) && expretion.getLogic() == Logic.AND && !expretion.isEmpty() && 
              (
              (expretion.getEqualType() != null && expretion.getEqualType() != EqualType.NOT_EQUAL && expretion.getEqualType() != EqualType.NOT_IN && expretion.getEqualType() != EqualType.NOT_LIKE && expretion.getEqualType() != EqualType.NOT_ILIKE) || 
              (expretion.getEqualDateType() != null && expretion.getEqualDateType() != EqualDateType.NOT_EQUAL)
              )) {
        DBColumn column = table.getColumn(expretion.getFieldName());
        if(column != null) {
          query += ", ["+filter.getTargetClass().getName()+"("+expretion.getFieldName()+")]=?";
          values.add(expretion.getValues()[0]);
          column.setValue(object, expretion.getValues()[0]);
        }else {
          DBRelation relation = table.getRelation(expretion.getFieldName());
          if(relation != null) {
            if(relation instanceof ManyToMany) {
              executeUpdate("DELETE FROM ["+filter.getTargetClass().getName()+"("+expretion.getFieldName()+"):table] "
                      + "WHERE ["+filter.getTargetClass().getName()+"("+expretion.getFieldName()+"):object]=?", new Object[]{object.getId()});
              for(Object param:expretion.getValues()) {
                if(param instanceof Integer) {
                  executeUpdate("INSERT INTO ["+filter.getTargetClass().getName()+"("+expretion.getFieldName()+"):table] "
                          + "(["+filter.getTargetClass().getName()+"("+expretion.getFieldName()+"):object],"
                          + "["+filter.getTargetClass().getName()+"("+expretion.getFieldName()+"):target]) VALUES (?,?)", new Object[]{object.getId(),param});
                }else if(param instanceof Integer[]) {
                  for(Integer id:(Integer[])param) {
                    executeUpdate("INSERT INTO ["+filter.getTargetClass().getName()+"("+expretion.getFieldName()+"):table] "
                            + "(["+filter.getTargetClass().getName()+"("+expretion.getFieldName()+"):object],"
                            + "["+filter.getTargetClass().getName()+"("+expretion.getFieldName()+"):target]) VALUES (?,?)", new Object[]{object.getId(),id});
                  }
                }
              }
            }else if(relation instanceof OneToMany) {
              executeUpdate("UPDATE ["+filter.getTargetClass().getName()+"("+expretion.getFieldName()+"):table] "
                      + "SET ["+filter.getTargetClass().getName()+"("+expretion.getFieldName()+"):target]=NULL "
                      + "WHERE ["+filter.getTargetClass().getName()+"("+expretion.getFieldName()+"):target]="+object.getId());
              executeUpdate("UPDATE ["+filter.getTargetClass().getName()+"("+expretion.getFieldName()+"):table] "
                      + "SET ["+filter.getTargetClass().getName()+"("+expretion.getFieldName()+"):target]=? "
                      + "WHERE id=ANY(?)",new Object[]{object.getId(),expretion.getValues()});
            }else {
              query += ", ["+filter.getTargetClass().getName()+"("+expretion.getFieldName()+")]=?";
              if(expretion.getEqualType() != null) {
                if(expretion.getEqualType() == EqualType.IN) {
                  values.add(((Object[])expretion.getValues()[0])[0]);
                }else values.add(expretion.getValues()[0]);
              }else if(expretion.getEqualDateType() != null)
                values.add(expretion.getValues()[0]);
              //if(i < filter.size()-1 && filter.get(i+1).getLogic() == Logic.OR)
                //break;
            }
          }
        }
      }
    }
    return query;
  }

  @Override
  public void toEstablishes(DBFilter filter, MappingObject object) throws RemoteException {
    if(filter != null && !filter.isEmpty()) {
      if(filter.getTargetClass().isInstance(object)) {
        //addObject(object);
        DBTable table = DataBase.get(filter.getTargetClass());
        if(table != null) {
          if(object.getId() == null)
            saveObject(object);
          ArrayList<Object> values = new ArrayList<>();
          String query = establish(table, filter, object, values);
          if(!query.equals("")) {
            query = "UPDATE ["+filter.getTargetClass().getName()+"] SET "+query.substring(2)+" WHERE id="+object.getId();
            executeUpdate(query, values.toArray());
            values.clear();
          }
        }
      } else throw new RemoteException("Объект не соответствует классу фильтра");
    }
  }
  
  public void close() {
    try {
      if(!isClosed())
        connection.close();
      connection = null;
    }catch(RemoteException | SQLException ex) {
      Logger.getLogger(getClass()).error(ex);
    }finally {
      savePoints.clear();
    }
  }
  
  @Override
  public boolean isClosed() throws RemoteException {
    try {
      return connection == null || connection.isClosed();
    }catch(SQLException ex) {
      Logger.getRootLogger().error(ex);
    }
    return true;
  }
  
  
  public Connection getConnection() throws SQLException {
    if(autoCommit) {
      close();
      connection = DBController.getConnection(true);
    }else if(connection == null)
        connection = DBController.getConnection(false);
    return connection;
  }
  
  /*@Override
  public void begin() throws RemoteException {
    if(!autoCommit)
      connection = DBController.getConnection();
  }*/
  
  @Override
  public void rollback() throws RemoteException {
    rollback(null);
  }
  
  @Override
  public void rollback(String savePointName) throws RemoteException {
    if(!isClosed()) {
      try {
        if(!autoCommit) {
          if(savePointName == null) {
            connection.rollback();
            Logger.getLogger(Session.class).debug("ROLLBACK");
          }else {
            connection.rollback(savePoints.get(savePointName));
            Logger.getLogger(Session.class).debug("ROLLBACK TO "+savePointName);
          }
        }
        close();
        clearEvents();
      }catch(SQLException ex) {
        Logger.getLogger(getClass()).error(ex);
      }finally {
        if(savePointName == null)
          events.clear();
      }
    }
  }
  
  @Override
  public void setSavePoint(String name) throws RemoteException {
    if(!isClosed()) {
      try {
        savePoints.put(name, connection.setSavepoint(name));
      }catch(SQLException ex) {
        Logger.getLogger(getClass()).error(ex);
      }
    }
  }
  
  @Override
  public void commit() throws RemoteException {
    try {
      //TODO: ActiveNQ  Клиент с модулями
      if(!isClosed()) {
        if(!autoCommit) {
          connection.commit();
          Logger.getLogger(getClass()).debug("COMMIT");
        }
        close();
        //Thread t = new Thread(() -> {
          TopicConnection topicConnection = null;
          TopicSession topicSession = null;
          try {
            if(!events.isEmpty()) {
              topicConnection = DataBase.createConnection();
              topicSession = topicConnection.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
              for(MessageProxy msg:events) {
                Message message = topicSession.createObjectMessage(msg.getIds());
                message.setJMSType(msg.getMessageType());
                if(msg.getObjectEventProperty() != null)
                  message.setObjectProperty("data", msg.getObjectEventProperty());
                message.setStringProperty("topic.name",  msg.getObjectClass().getName());
                
                Topic topic = topicSession.createTopic(message.getStringProperty("topic.name"));
                TopicPublisher publisher = topicSession.createPublisher(topic);
                publisher.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
                publisher.publish(message);
                publisher.close();
              }
            }
          }catch(JMSException ex) {
            Logger.getLogger(getClass()).error(ex);
          }finally {
            events.clear();
            try {
              if(topicSession != null)
                topicSession.close();
            } catch (JMSException e) {
              e.printStackTrace();
            }
            try {
              if(topicConnection != null)
                topicConnection.close();
            } catch (JMSException e) {
              e.printStackTrace();
            }
          }
          //Thread.interrupted();
        //});
        //t.setDaemon(true);
        //t.start();
      }
    }catch(SQLException ex) {
      Logger.getLogger(getClass()).error(ex);
    }
  }
  
 /* @Override
  public void addObject(MappingObject object) throws RemoteException {
    Delegator delegator = (Delegator) Proxy.getInvocationHandler(object);
    //Session ses = delegator.getSession();
    //delegator.setSession(this);
  }*/
  
  @Override
  public MappingObject createEmptyObject(Class<? extends MappingObject> objectClass) throws RemoteException {
    try {
      MappingObjectImpl impl = (MappingObjectImpl)DataBase.get(objectClass).getAnnotatedClass().newInstance();
      impl.setSessionFactory(createFactory());
      impl.setLoadId(IDStore.createID());
      
      MappingObject object = (MappingObject)Delegator.newInstance(objectPort, clientSocketFactory, serverSocketFactory, impl);

      /*if(client != null) {
        TreeMap<Long, SoftReference> objectsCash = clientsObjectsCash.get(client.getSessionId());
        if(objectsCash == null)
          clientsObjectsCash.put(client.getSessionId(), objectsCash = new TreeMap<>());
        objectsCash.put(object.getLoadId(), new SoftReference(object));
      }*/

      return object;
    }catch(InstantiationException | IllegalAccessException | IllegalArgumentException ex) {
      throw new RemoteException("Ошибка при создании обёртки объекта класса "+objectClass.getName()+"\n", ex);
    }
  }
  
  @Override
  public Map<String,String> getFiledColumns(Class<? extends MappingObject> objectClass) throws RemoteException {
    Map<String,String> fieldColumns = new TreeMap<>();
    for(DBColumn column:DataBase.get(objectClass).getColumns())
      fieldColumns.put(column.getField().getName(), column.getName());
    for(DBRelation relation:DataBase.get(objectClass).getRelations())
      if(relation != null && relation instanceof util.ManyToOne)
        fieldColumns.put(relation.getField().getName(),((util.ManyToOne)relation).getColumn().getName());
    return fieldColumns;
  }
  
  @Override
  public Map<String, String> getQueryColumns(Class<? extends MappingObject> objectClass) throws RemoteException {
    TreeMap<String,String> fieldColumns = new TreeMap<>();
    TreeMap<String,Map<String,Object>> qc = DataBase.get(objectClass).getQueryColumns();
    for(String fn:qc.keySet())
      fieldColumns.put(fn, qc.get(fn).get("query").toString().substring(0, qc.get(fn).get("query").toString().lastIndexOf("AS")-1));
    
    return fieldColumns;
  }
  
  public String toString(Object value) {
    if(true)
    if(value.getClass().isArray()) {
      if(value instanceof Object[]) {
        value = Arrays.toString((Object[])value);
      }else {
        if(value instanceof int[])
          value = Arrays.toString((int[])value);
        if(value instanceof double[])
          value = Arrays.toString((double[])value);
        if(value instanceof float[])
          value = Arrays.toString((float[])value);
        if(value instanceof byte[])
          value = Arrays.toString((byte[])value);
        if(value instanceof boolean[])
          value = Arrays.toString((boolean[])value);
        if(value instanceof char[])
          value = Arrays.toString((char[])value);
        if(value instanceof long[])
          value = Arrays.toString((long[])value);
        if(value instanceof short[])
          value = Arrays.toString((short[])value);
      }
    }else if(value instanceof Map) {
      Map map = (Map)value;
      value = "{";
      for(Object key:map.keySet())
        value += key+"="+toString(map.get(key))+",";
      value = value.toString().substring(0, value.toString().length()-1)+"}";
    }
    return value.toString();
  }
  
  @Override
  public JSONObject getJson(Class<? extends MappingObject> objectClass, Integer id) throws RemoteException {
    return getJson(getObject(objectClass, id));
  }
  
  @Override
  public JSONObject getJson(MappingObject object) throws RemoteException {
    if(object != null) {
      Delegator delegator = (Delegator) Proxy.getInvocationHandler(object);
      JSONObject json = new JSONObject();
      Integer id = object.getId();
      DBTable table = DataBase.get(object);
      Class objectClass = table.getInterfacesClass();
      String className = objectClass.getName();
      json.put("class", objectClass.getName());

      for(DBColumn column:table.getColumns()) {
        if(column.getField().getName().intern() != "modificationDate".intern() && 
                column.getField().getName().intern() != "lastUserName".intern() && 
                column.getField().getName().intern() != "lastUserId".intern()) {
          
          Object value = column.getValue(object);
          
          //if(value instanceof java.sql.Date)
            //value = new java.util.Date(((java.sql.Date)value).getTime());
          
          /*if(value.getClass().isArray()) {
            if(value instanceof Object[]) {
              value = Arrays.toString((Object[])value);
            }else {
              if(value instanceof int[])
                value = Arrays.toString((int[])value);
              if(value instanceof double[])
                value = Arrays.toString((double[])value);
              if(value instanceof float[])
                value = Arrays.toString((float[])value);
              if(value instanceof byte[])
                value = Arrays.toString((byte[])value);
              if(value instanceof boolean[])
                value = Arrays.toString((boolean[])value);
              if(value instanceof char[])
                value = Arrays.toString((char[])value);
              if(value instanceof long[])
                value = Arrays.toString((long[])value);
              if(value instanceof short[])
                value = Arrays.toString((short[])value);
            }
          }*/
          try {
            json.put(column.getField().getName(), value == null ? "" : toString(value));
          }catch(Exception ex) {
            System.out.println(ex.getMessage());
            System.out.println("Ошибка при установке значения для '"+column.getField().getName()+"'");
            throw new RemoteException("Ошибка при установке значения для '"+column.getField().getName()+"'");
          }
        }
      }
      
      try {
        for(DBRelation relation:table.getRelations()) {
          Method method = objectClass.getMethod(relation.getGetMethod(), new Class[0]);
          Field field = relation.getField();
          field.setAccessible(true);
          String fieldName = field.getName();
          Object value = delegator.invokeWithoutProxy(method, new Object[0]);
          if(relation instanceof ManyToOne) {
            if(value == null)  {
              List<List> data = executeQuery("SELECT ["+className+"("+fieldName+")] FROM ["+className+"] WHERE id=? ORDER BY ["+className+"("+fieldName+")]", new Object[]{id});
              if(!data.isEmpty())
                value = (Integer) data.get(0).get(0);
            } else value = ((MappingObject)value).getId();
          }else if(relation instanceof OneToMany || relation instanceof ManyToMany) {
            if(value == null || ((List)value).isEmpty()) {
              List<List> data = executeQuery("SELECT ["+className+"("+fieldName+"):target] FROM ["+className+"("+fieldName+"):table]  "
                      + "WHERE ["+className+"("+fieldName+"):object]=? ORDER BY ["+className+"("+fieldName+"):target]", new Object[]{id});
              value = new Integer[0];
              if(!data.isEmpty()) {
                for(List d:data)
                  value = (Integer[]) ArrayUtils.add((Integer[])value, d.get(0));
              }
            } else {
              Integer[] ids = new Integer[0];
              for(Object o:(List)value)
                ids = (Integer[]) ArrayUtils.add(ids, ((MappingObject)o).getId());
              Arrays.sort(ids);
              value = ids;
            }
          }
          json.put(fieldName, value);
        }
      }catch(Throwable ex) {
        throw new RemoteException("Ошибка \n", ex);
      }

      return json;
    } else throw new RemoteException("Объект null!");
  }
  
  public SessionFactory createFactory() {
    return new SessionFactory();
  }
  
  public class SessionFactory {
    public Session createSession() throws RemoteException {
      return createSession(false);
    }
    
    public Session createSession(boolean autoCommit) throws RemoteException {
      return new Session(objectPort, clientSocketFactory, serverSocketFactory, client, autoCommit);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Session other = (Session) obj;
    if (!Objects.equals(this.client, other.client)) {
      return false;
    }
    if (!Objects.equals(this.connection, other.connection)) {
      return false;
    }
    return true;
  }
  
  class MessageProxy {
    private Class objectClass;
    private String messageType;
    private Integer[] ids;
    private Map<String,Object> objectEventProperty;

    public MessageProxy(Class objectClass, String messageType, Map<String,Object> objectEventProperty, Integer[] ids) {
      this.objectClass = objectClass;
      this.messageType = messageType;
      this.ids = ids;
      this.objectEventProperty = objectEventProperty;
    }

    public Integer[] getIds() {
      return ids;
    }

    public String getMessageType() {
      return messageType;
    }

    public Class getObjectClass() {
      return objectClass;
    }

    public Map<String,Object> getObjectEventProperty() {
      return objectEventProperty;
    }

    @Override
    public int hashCode() {
      int hash = 7;
      hash = 11 * hash + Objects.hashCode(this.objectClass);
      hash = 11 * hash + Objects.hashCode(this.messageType);
      hash = 11 * hash + Arrays.deepHashCode(this.ids);
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final MessageProxy other = (MessageProxy) obj;
      if (!Objects.equals(this.objectClass, other.objectClass)) {
        return false;
      }
      if (!Objects.equals(this.messageType, other.messageType)) {
        return false;
      }
      if (!Arrays.deepEquals(this.ids, other.ids)) {
        return false;
      }
      return true;
    }
  }
}
// -Xrunhprof:heap=sites -verbose:gc