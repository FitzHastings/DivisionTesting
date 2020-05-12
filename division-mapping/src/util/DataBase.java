package util;

import bum.pool.DBController;
import conf.P;
import division.util.Utility;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Properties;
import javax.jms.JMSException;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import mapping.MappingObject;
import net.sf.json.JSONObject;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;
import static util.Element.logger;

public class DataBase {
  private static final Hashtable<Class,DBTable> tables = new Hashtable<>();
  private static final Hashtable<Class,Class> classes = new Hashtable<>();
  private static Properties properties = null;
  
  private static Properties getProperties() {
    try {
      if(properties == null) {
        properties = new Properties();
        P.list("types", JSONObject.class).stream().forEach(t -> {
          properties.put(t.getString("name"), t.getString("value"));
        });
      }
    }catch(Exception ex) {
      Logger.getLogger(DataBase.class).error(ex);
    }
    return properties;
  }

  public static String getProperty(String key) {
    return getProperties().getProperty(key);
  }

  public static Collection<DBTable> getTables() {
    return tables.values();
  }
  
  public static DBTable get(MappingObject object) {
    Class[] interfaces = object.getClass().getInterfaces();
    for(int i=interfaces.length-1;i>=0;i--)
      if(tables.get(interfaces[i]) != null)
        return tables.get(interfaces[i]);
    return null;
  }
  
  public static DBTable get(Class objectClass) {
    if(tables.containsKey(objectClass))
      return tables.get(objectClass);
    else
      return tables.get(classes.get(objectClass));
  }

  public static DBTable get(String className) throws ClassNotFoundException {
    return get(Class.forName(className));
  }
  
  public static void put(Class annotatedClass) {
    put(annotatedClass,annotatedClass);
  }
  
  public static void put(Class realClass, Class interfaceClass) {
    if(!tables.containsKey(interfaceClass)) {
      tables.put(interfaceClass, new DBTable(realClass, interfaceClass));
      classes.put(realClass, interfaceClass);
      System.out.println("PUT: "+interfaceClass+" - "+realClass);
    }
  }
  
  public static DBTable getTable(String tableName) {
    for(DBTable table:tables.values())
      if(table.getName().toLowerCase().intern() == tableName.toLowerCase().intern())
        return table;
    return null;
  }

  public static Hashtable<Class, Class> getClasses() {
    return classes;
  }
  
  public static void reIndex() {
    Logger.getLogger(DataBase.class).info("Переиндексация...");
    tables.values().stream().forEach((table) -> table.configureIndexes());
  }
  
  public static void reProcedures() {
    Logger.getLogger(DataBase.class).info("Конфигурирование хранимых процедур...");
    tables.values().stream().forEach((table) -> table.configureProcedures());
  }
  
  public static void reTriggers() throws ClassNotFoundException {
    Logger.getLogger(DataBase.class).info("Конфигурирование триггерных функций...");
    tables.values().stream().forEach((table) -> table.configureTriggerFunctions());

    Logger.getLogger(DataBase.class).info("Конфигурирование триггеров...");
    for(DBTable table:tables.values())
      table.configureTriggers();
  }
  
  public static void reViewFields() {
    Logger.getLogger(DataBase.class).info("Конфигурирование viewers...");
    tables.values().stream().forEach((table) -> table.configureView());
  }
  
  public static void reUnicumFields() {
    Logger.getLogger(DataBase.class).info("Конфигурирование уникальных полей...");
    tables.values().stream().forEach((table) -> table.configureUnicumFields());
  }
  
  public static void reTables() {
    Logger.getLogger(DataBase.class).info("Конфигурирование таблиц...");
    tables.values().stream().forEach((table) -> {
      Logger.getLogger(DataBase.class).debug("конфигурирую таблицу "+table.getInterfacesClass());
      table.configure();
    });
  }
  
  public static void reFieldsOfRelations() {
    Logger.getLogger(DataBase.class).info("Конфигурирование связей таблиц...");
    tables.values().stream().forEach((table) -> {
      for(DBRelation relation:table.getRelations()) {
        Logger.getLogger(DataBase.class).debug("конфигурирую связь таблицы "+table.getInterfacesClass()+" с "+relation.getTargetClass());
        relation.configure();
      }
    });
  }
  
  public static void removeOldColumns() {
    Logger.getLogger(DataBase.class).info("Удаляю старые колонки...");
    tables.values().stream().forEach((table) -> {
      Logger.getLogger(DataBase.class).debug("Удаляю старые колонки из"+table.getInterfacesClass());
      table.removeOldColumns();
    });
  }
  
  public static void removeConstraints() {
    Logger.getLogger(DataBase.class).info("Удаляю старые уникумы...");
    tables.values().stream().forEach((table) -> {
      Logger.getLogger(DataBase.class).debug("Удаляю старые уникумы из"+table.getInterfacesClass());
      table.removeConstraints();
    });
  }
  
  public static void analizedb() {
    Logger.getLogger(DataBase.class).info("ANALYZE DB START");
    Element.execute("ANALYZE");
    Logger.getLogger(DataBase.class).info("ANALYZE DB END");
  }
  
  public static void vacuumdb() {
    Logger.getLogger(DataBase.class).info("VACUUM DB START");
    Element.execute("VACUUM", true);
    Logger.getLogger(DataBase.class).info("VACUUM DB END");
  }

  public static void configure() {
    try {
      Logger.getLogger(DataBase.class).info("Создаю функцию формирования идентификторов");
      Element.execute("CREATE OR REPLACE FUNCTION getNextID(VARCHAR,VARCHAR) RETURNS INTEGER AS '\n"
              + "DECLARE\n"
              + "  ID INTEGER;\n"
              + "BEGIN\n"
              + "  EXECUTE ''(SELECT MAX(''||$1||'')+1 FROM ''||$2||'')'' INTO ID;\n"
              + "  IF ID IS NULL THEN ID := 1; END IF;\n"
              + "  RETURN ID;\n"
              + "END'\n"
              + "LANGUAGE plpgsql\n");
      
      Logger.getLogger(DataBase.class).info("Создаю функцию даты последней модификации");
      Element.execute("CREATE OR REPLACE FUNCTION updateLastModification() RETURNS TRIGGER AS '\n"
              + "DECLARE\n"
              + "BEGIN\n"
              + "  NEW.modificationDate = CURRENT_TIMESTAMP;\n"
              + "  RETURN NEW;\n"
              + "END'\n"
              + "LANGUAGE plpgsql\n");

      analizedb();
      removeConstraints();
      reTables();
      reFieldsOfRelations();
      removeOldColumns();
      reUnicumFields();
      reIndex();
      reViewFields();
      reProcedures();
      reTriggers();
    }catch(Exception ex) {
      Logger.getLogger(DataBase.class).info(ex);
    }
  }
  
  public static void updateDataBase() {
    File update = new File("update");
    if(update.exists() && update.isDirectory()) {
      Logger.getLogger(DataBase.class).info("START UPDATE");
      Connection conn = null;
      PreparedStatement st = null;
      try {
        conn = DBController.getConnection();
        conn.setAutoCommit(false);
        
        for(File f:update.listFiles((File dir, String name) -> name.endsWith(".sql"))) {
          String str = Utility.getStringFromFile(f.getAbsolutePath(),true);
          /*FileInputStream inFile = new FileInputStream(f);
          byte[] str = new byte[inFile.available()];
          inFile.read(str);
          inFile.close();
          inFile = null;*/
          
          logger.info("START SCRIPT "+f.getName());
          st = conn.prepareStatement(DBTable.replaceString(str));
          logger.debug(st);
          st.execute();
          logger.info(st);
          logger.info("FINISH SCRIPT "+f.getName());
        }
        conn.commit();
        Logger.getLogger(DataBase.class).info("END UPDATE");
        //for(File f:update.listFiles((File dir, String name) -> name.endsWith(".sql"))) {f.renameTo(new File(f.getAbsolutePath()+"-old"));}
      }catch(Exception ex) {
        if(conn != null) {
          try {conn.rollback();}
          catch(Exception e) {logger.error("", e);}
        }
        logger.error(ex.getMessage(), ex);
      }finally {
        try{conn.close();}
        catch(Exception t) {logger.error("", t);}
        try{st.close();}
        catch(Exception t) {logger.error("", t);}
      }
    }
  }
  
  
  public static TopicConnection createConnection() {
    try {
      String url = ActiveMQConnection.DEFAULT_BROKER_URL;
      if(System.getProperty("messanger.protocol") != null && 
              !System.getProperty("messanger.protocol").equals("") &&
              System.getProperty("messanger.host") != null && 
              !System.getProperty("messanger.host").equals("") &&
              System.getProperty("messanger.port") != null &&
              !System.getProperty("messanger.port").equals(""))
        url = System.getProperty("messanger.protocol")+"://"
                +System.getProperty("messanger.host")+":"
                +System.getProperty("messanger.port");
        
      TopicConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
      TopicConnection connection = connectionFactory.createTopicConnection();
      connection.start();
      return connection;
    }catch(JMSException ex) {
      Logger.getLogger(DataBase.class).error(ex);
      return createConnection();
    }
  }
}