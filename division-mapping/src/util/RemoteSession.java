package util;

import division.util.GzipUtil;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import mapping.MappingObject;
import net.sf.json.JSONObject;
import util.filter.local.DBFilter;

public interface RemoteSession extends Remote {
  
  public List<List> executeQuery(String sqlQuery) throws RemoteException;
  public List<List>[] executeQuery(String... sqlQuery) throws RemoteException;
  public List<List> executeQuery(String sqlQuery, Object... params) throws RemoteException;
  
  public byte[] executeGzipQuery(String sqlQuerys[], Object[]... arrParams) throws RemoteException;
  public byte[] executeGzipQuery(String sqlQuery, Object... params) throws RemoteException;
  public byte[] executeGzipQuery(String... sqlQuerys) throws RemoteException;
  public byte[] executeGzipQuery(String sqlQuery) throws RemoteException;
  
  public List<List>[] executeQuery(String[] sqlQuery, Object[]... params) throws RemoteException;
  
  public int executeUpdate(Class<? extends MappingObject> objectClass, String param, Object value, Integer id) throws RemoteException;
  public int executeUpdate(Class<? extends MappingObject> objectClass, String[] params, Object[] values, Integer[] ids) throws RemoteException;
  public int executeUpdate(String sqlQuery, Object... params) throws RemoteException;
  
  public int executeUpdate(String sqlQuery) throws RemoteException;
  public int[] executeUpdate(String... sqlQuery) throws RemoteException;
  public int[] executeUpdate(String[] sqlQuery, Object[]... params) throws RemoteException;
  
  public boolean saveObject(MappingObject object) throws RemoteException, IOException;
  public boolean saveObject(Map<String, Object> map) throws RemoteException, IOException;
  public boolean saveObject(Class<? extends MappingObject> objectClass, Map<String, Object> map) throws RemoteException, IOException;
  public boolean saveObject(Class<? extends MappingObject> objectClass, Integer id, Map<String, Object> map) throws RemoteException, IOException;
  public boolean removeObject(MappingObject object) throws RemoteException;
  public int removeObjects(Class<? extends MappingObject> objectClass, Integer... ids) throws RemoteException;
  public int removeObjects(Class<? extends MappingObject> objectClass, Map objectEventProperty, Integer... ids) throws RemoteException;
  
  public int toArchive(Class<? extends MappingObject> objectClass, Integer objectid) throws RemoteException, IOException;
  public int toArchive(Class<? extends MappingObject> objectClass, Integer objectid, Map<String, Object> object) throws RemoteException, IOException;
  public List<Map> getArchive(Class<? extends MappingObject> objectClass, Integer objectid) throws Exception;
  public Map getLastArchive(Class<? extends MappingObject> objectClass, Integer objectid) throws Exception;
  
  public void toTypeObjects(Class<? extends MappingObject> objectClass, Integer[] ids, MappingObject.Type type) throws RemoteException;
  public void toTmpObjects(Class<? extends MappingObject> objectClass, Integer[] ids, boolean tmp) throws RemoteException;
  
  public List<List> getData(DBFilter filter, String... fields) throws RemoteException;
  public List<List> getData(DBFilter filter, String[] fields, String... fieldsOrderBy) throws RemoteException;
  
  public List<List> getData(Class<? extends MappingObject> objectClass, String... fields) throws RemoteException;
  public List<List> getData(Class<? extends MappingObject> objectClass, String[] fields, String... fieldsOrderBy) throws RemoteException;
  
  public List<List> getData(Class<? extends MappingObject> objectClass, Integer[] ids, String... fields) throws RemoteException;
  public List<List> getData(Class<? extends MappingObject> objectClass, Integer[] ids, String[] fields, String... fieldsOrderBy) throws RemoteException;
  
  
  public byte[] getGZipData(DBFilter filter, String... fields) throws RemoteException;
  public byte[] getGZipData(DBFilter filter, String[] fields, String... fieldsOrderBy) throws RemoteException;
  public byte[] getGZipData(DBFilter filter, String[] fields, String[] fieldsOrderBy, String... fieldsGroupBy) throws RemoteException;
  
  public byte[] getGZipData(Class<? extends MappingObject> objectClass, String... fields) throws RemoteException;
  public byte[] getGZipData(Class<? extends MappingObject> objectClass, String[] fields, String... fieldsOrderBy) throws RemoteException;
  public byte[] getGZipData(Class<? extends MappingObject> objectClass, Integer[] ids, String... fields) throws RemoteException;
  public byte[] getGZipData(Class<? extends MappingObject> objectClass, Integer[] ids, String[] fields, String... fieldsOrderBy) throws RemoteException;
  
  
  
  public default byte[] getGZipList(DBFilter filter, String... fields) throws Exception {
    return GzipUtil.gzip(getList(filter, fields));
  }
  
  public default byte[] getZipList(DBFilter filter, String... fields) throws Exception {
    return GzipUtil.zip(getList(filter, fields));
  }
  
  public List<Map> getList(DBFilter filter, String... fields) throws Exception;
  
  
  

  public MappingObject getObject(Class<? extends MappingObject> objectClass, Integer id) throws RemoteException;
  public MappingObject[] getObjects(Class<? extends MappingObject> objectClass) throws RemoteException;
  public MappingObject[] getObjects(DBFilter filter) throws RemoteException;
  public MappingObject[] getObjects(DBFilter filter, String[] fieldsOrderBy) throws RemoteException;
  public MappingObject[] getObjects(Class<? extends MappingObject> objectClass, String[] fieldsOrderBy) throws RemoteException;
  public MappingObject[] getObjects(Class<? extends MappingObject> objectClass, Integer[] ids) throws RemoteException;
  public MappingObject[] getObjects(Integer[] ids, DBFilter filter) throws RemoteException;
  public MappingObject[] getObjects(Integer[] ids, DBFilter filter, String[] fieldsOrderBy) throws RemoteException;
  
  
  
  public Map object(Class<? extends MappingObject> objectClass, Integer id) throws RemoteException;
  public Map[] objects(Class<? extends MappingObject> objectClass, Integer[] ids) throws RemoteException;
  public Map[] objects(Integer[] ids, DBFilter filter) throws RemoteException;
  public Map[] objects(Class<? extends MappingObject> objectClass, Integer[] ids, String[] fieldsOrderBy) throws RemoteException;
  public Map[] objects(Class<? extends MappingObject> objectClass) throws RemoteException;
  public Map[] objects(Class<? extends MappingObject> objectClass, String[] fieldsOrderBy) throws RemoteException;
  public Map[] objects(DBFilter filter) throws RemoteException;
  public Map[] objects(DBFilter filter, String[] fieldsOrderBy) throws RemoteException;
  public Map[] objects(Integer[] ids, DBFilter filter, String[] fieldsOrderBy) throws RemoteException;
  
  
  
  
  
  
  
  public boolean isSatisfy(DBFilter filter, Integer id) throws Exception;
  public Integer[] isSatisfy(DBFilter filter,Integer[] ids) throws Exception;
  public void toEstablishes(DBFilter filter, MappingObject object) throws Exception;
  
  //public void begin() throws RemoteException;
  public void rollback() throws RemoteException;
  public void rollback(String savePointName) throws RemoteException;
  public void setSavePoint(String name) throws RemoteException;
  public void commit() throws RemoteException;
  public boolean isClosed() throws RemoteException;
  
  public Client getClient() throws RemoteException;
  
  public void clearEvents() throws RemoteException;
  //public void insertEvent(int index, javax.jms.Message event) throws RemoteException;
  //public void addEvent(javax.jms.Message event) throws RemoteException;
  
  public void addEvent(Class<? extends MappingObject> objectClass, String type, Integer... ids) throws RemoteException;
  public void addEvent(Class<? extends MappingObject> objectClass, String type, Map<String,Object> objectEventProperty, Integer... ids) throws RemoteException;
  
  public Integer createObject(Class<? extends MappingObject> objectClass, Map<String,Object> map) throws RemoteException, IOException;
  public Integer createObject(Class<? extends MappingObject> objectClass, Map<String,Object> map, Map<String,Object> objectEventProperty) throws RemoteException, IOException;
  
  public MappingObject createEmptyObject(Class<? extends MappingObject> objectClass) throws RemoteException;
  
  public Map<String,String> getFiledColumns(Class<? extends MappingObject> objectClass) throws RemoteException;
  public Map<String, String> getQueryColumns(Class<? extends MappingObject> objectClass) throws RemoteException;
  
  public JSONObject getJson(MappingObject object) throws RemoteException, IllegalArgumentException, IllegalAccessException;
  public JSONObject getJson(Class<? extends MappingObject> objectClass, Integer id) throws RemoteException, IllegalArgumentException, IllegalAccessException;
}
