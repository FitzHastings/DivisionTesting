package util;

import bum.pool.DBController;
import division.util.GzipUtil;
import division.util.Utility;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;

public class DBColumn extends DBObject implements Comparable {
  private int length;
  private boolean identify;
  private Field   field;
  private Object  defaultValue;
  private String  sqlType  = "";
  private boolean nullAble = true;
  private boolean unique   = false;
  private boolean index    = false;
  private boolean zip      = false;
  private boolean gzip     = false;
  
  private boolean view;
  
  public DBColumn(Class objectClass, Class targetClass, String name) {
    this.setName(name);
    
    this.setObjectClass(objectClass);
    this.setTargetClass(targetClass);
    
    this.length = 0;
    this.identify = false;
  }
  
  public DBColumn(Class objectClass, Field field, String name, int length, String sqlType, String defaultValue, boolean identify) {
    this.setName(name);
    this.sqlType = sqlType;
    this.setObjectClass(objectClass);
    this.setTargetClass(field.getType());
    
    this.length = length;
    this.field = field;
    
    try {
      String getName = (getTargetClass().equals(Boolean.class) || getTargetClass().equals(boolean.class) ? "is" : "get")+field.getName().substring(0, 1).toUpperCase()+field.getName().substring(1);
      setGetMethod(getObjectClass().getMethod(getName, new Class[0]).getName());
      setSetMethod(getObjectClass().getMethod("set"+
              field.getName().substring(0, 1).toUpperCase()+field.getName().substring(1), 
              new Class[]{getTargetClass()}).getName());
    }catch(NoSuchMethodException | SecurityException ex) {
      Logger.getRootLogger().error("", ex);
    }

    if(defaultValue == null) {
      try {
        Object o = objectClass.newInstance();
        Object value = this.getValue(o);
        if(value != null) {
          if(value instanceof Integer || value instanceof Double || value instanceof Float ||
                  value instanceof Number || value instanceof String || getField().getType().isEnum() ||
                  value instanceof java.sql.Date || value instanceof java.util.Date ||
                  value instanceof java.sql.Timestamp || getField().getType().isPrimitive())
            defaultValue = String.valueOf(value);
        }
      }catch(InstantiationException | IllegalAccessException | RemoteException ex) {
      }
    }
    
    this.defaultValue = defaultValue;
    this.identify = identify;
  }

  public boolean isZip() {
    return zip;
  }

  public void setZip(boolean zip) {
    this.zip = zip;
  }

  public boolean isGzip() {
    return gzip;
  }

  public void setGzip(boolean gzip) {
    this.gzip = gzip;
  }

  public boolean isIndex() {
    return index;
  }

  public void setIndex(boolean index) {
    this.index = index;
  }

  public boolean isUnique() {
    return unique;
  }

  public void setUnique(boolean unique) {
    this.unique = unique;
  }

  public boolean isNullAble() {
    return nullAble;
  }

  public void setNullAble(boolean nullAble) {
    this.nullAble = nullAble;
  }

  public boolean isView() {
    return view;
  }

  public void setView(boolean view) {
    this.view = view;
  }

  public Field getField() {
    return field;
  }
  
  @Override
  public void configure(){
    if(!isExist())
      create();
    else if(isAlter())
      alterType();
  }
  
  /*@Override
  public void configure(Connection conn) throws SQLException {
    if(!isExist(conn))
      create(conn);
    else if(isIdentify() && isAlter(conn))
      alterType(conn);
  }*/
  
  public Object getValue(Object object) throws RemoteException {
    try {
      return object.getClass().getMethod(getGetMethod(), new Class[0]).invoke(object, new Object[0]);
    }catch(NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
      Logger.getLogger(util.Element.class).error(ex);
      throw new RemoteException("Ошибка при получении значения: "+getGetMethod()+"\n",ex);
    }
  }
  
  public synchronized void setValue(Object object, Object param) throws RemoteException {
    try {
      object.getClass().getMethod(getSetMethod(), new Class[]{getTargetClass()}).invoke(object, new Object[]{cast(param)});
    }catch(SQLException | IOException | ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
      Logger.getLogger(util.Element.class).error(ex);
      throw new RemoteException("Ошибка при установке значения: "+getSetMethod()+":"+param+"\n",ex);
    }
  }
  
  public Object cast(Object param) throws IOException, SQLException, ClassNotFoundException {
    if(getField().getType().isEnum()) {
      for(Object o:getField().getType().getEnumConstants())
        if(o.toString().equals(param)) {
          param = o;
          break;
        }
    }

    if(getField().getType() == Period.class) {
      param = param == null ? Period.ZERO : Utility.convert(param.toString());
    }

    if(param != null && (field.getType() == Properties.class || field.getType() == Map.class) && param instanceof byte[]) {
      try {
        /*if(isZip())
          param = GzipUtil.getObjectFromZip((byte[])param);
        else if(isGzip())
          param = GzipUtil.getObjectFromGzip((byte[])param);
        else */param = GzipUtil.getObjectFromGzip((byte[])param);
      }catch(Exception ex) {
        System.out.println(ex.getMessage()+" - попробую без разархивации");
        try {
          param = GzipUtil.deserializable((byte[])param);
        }catch (Exception e) {
          System.out.println(e.getMessage());
        }
        //param = null;
        //throw new IOException(ex);
      }
      
      /*if(!(param instanceof Properties) && param instanceof Map) {
        Properties p = new Properties();
        for(Object k:((Map)param).keySet())
          p.put(k, ((Map)param).get(k));
        param = p;
      }*/
    }

    if(param != null && field.getType() == Color.class) {
      param = new Color((Integer)param);
    }

    //h2
    if(param instanceof Clob) {
      InputStream in = ((Clob)param).getAsciiStream();
      byte[] buff = new byte[in.available()];
      in.read(buff);
      param = new String(buff);
    }

    if(isIdentify())
      param = Integer.valueOf(String.valueOf(param));

    if(param != null && param instanceof Array)
      param = ((Array)param).getArray();
    
    if(param instanceof java.sql.Date && field.getType() == LocalDate.class)
      param = Utility.convert((java.sql.Date)param);
    
    if(param instanceof Timestamp && field.getType() == LocalDateTime.class)
      param = Utility.convertToDateTime((Timestamp)param);
      
    return param;
  }

  public boolean isIdentify() {
    return identify;
  }

  public Object getDefaultValue() {
    return defaultValue;
  }
  
  private String getSqlType() {
    String type = null;
    if(sqlType != null && !sqlType.equals("")) {
      type = sqlType+(isNullAble()?"":" NOT NULL");
      if(defaultValue != null) {
        String def = DataBase.getProperty(String.valueOf(defaultValue));
        String kav = "";
        if(getTargetClass() == String.class || getTargetClass().isEnum())
          kav = "'";
        type += " DEFAULT "+kav+(def==null?defaultValue:def)+kav;
      }
    }else {
      String key = getTargetClass().getSimpleName();
      if(getTargetClass().isEnum())
        key = "String";
      String lastStr = "";
      String kav = "";
      if(isIdentify())
        key = "ID";
      else if(getTargetClass() == String.class || getTargetClass().isEnum()) {
        kav = "'";
        if(length <= 0 || length > 255)
          key = "Text";
        else lastStr = "("+length+")";
      }
      type = DataBase.getProperty(key);
      if(type == null)
        type = key.toUpperCase();
      type += lastStr+(isNullAble()?"":" NOT NULL");
      if(defaultValue != null) {
        String def = DataBase.getProperty(String.valueOf(defaultValue));
        type += " DEFAULT "+kav+(def==null?defaultValue:def)+kav;
      }
    }
    return type;
  }
  
  @Override
  public String toString() {
    return getName()+" "+getSqlType()+(isUnique()?" UNIQUE":"")+(isIdentify()?" PRIMARY KEY DEFAULT getNextID('"+getName()+"','"+getObjectTable().getName()+"')":"");
  }
  
  public void alterType() {
    String type = getSqlType();
    type = type.indexOf(" ")>0?type.substring(0, type.indexOf(" ")):type;
    execute("ALTER TABLE "+getObjectTable().getName()+" ALTER COLUMN "+getName()+" TYPE "+type);
    if(isIdentify())
      execute("ALTER TABLE "+getObjectTable().getName()+" ALTER COLUMN "+getName()+" SET DEFAULT getNextID('"+getName()+"','"+getObjectTable().getName()+"')");
    else {
      execute("ALTER TABLE "+getObjectTable().getName()+" ALTER COLUMN "+getName()+" DROP DEFAULT");
      if(defaultValue != null) {
        String def = DataBase.getProperty(String.valueOf(defaultValue));
        String kav = "";
        if(getTargetClass() == String.class || getTargetClass().isEnum())
          kav = "'";
        execute("ALTER TABLE "+getObjectTable().getName()+" ALTER COLUMN "+getName()+" SET DEFAULT "+kav+(def==null?defaultValue:def)+kav);
      }
    }
    //if(getObjectTable().isHistory())
      //execute("ALTER TABLE h_"+getObjectTable().getName()+" ALTER COLUMN "+getName()+" TYPE "+getSqlType());
  }
  
  
  @Override
  public void create() {
    execute("ALTER TABLE "+getObjectTable().getName()+" ADD COLUMN "+this);
  }

  @Override
  public void drop() {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  
  public static boolean isExist(String tableName, String columnName) {
    boolean exist = false;
    Connection conn = null;
    try {
      conn = DBController.getConnection();
      DatabaseMetaData meta = conn.getMetaData();
      ResultSet rs = meta.getColumns(System.getProperty("DB-NAME"),
              "%", tableName, columnName);
      while(rs.next())
        exist = true;
      rs.close();
      conn.commit();
    }catch(SQLException ex) {
        try {
          if(conn != null)
            conn.rollback();
        }catch(SQLException e) {
          logger.error("", e);
        }
        logger.error("", ex);
      }
      finally {
        try {
          if(conn != null)
            conn.close();
        }catch(SQLException ex) {
          logger.error("", ex);
        }
      }
    return exist;
  }
  
  public boolean isAlter() {
    boolean exist = false;
    ResultSet rs = null;
    Connection conn = null;
    try {
      conn = DBController.getConnection();
      DatabaseMetaData meta = conn.getMetaData();
      rs = meta.getColumns(System.getProperty("DB-NAME"),
              "%", getObjectTable().getName(), getName());
      String type = getSqlType();
      int index = type.indexOf(" ");
      if(index > 0)
        type = type.substring(0, index);
      while(rs.next()) {
        String dbType = rs.getString("TYPE_NAME");
        if(dbType.equalsIgnoreCase("varchar") && rs.getString("COLUMN_SIZE") != null)
          dbType += "("+rs.getString("COLUMN_SIZE")+")";
        //System.out.println(dbType.toLowerCase()+" VS "+type.toLowerCase());
        exist = !dbType.equalsIgnoreCase(type);
      }
      conn.commit();
    }catch(SQLException ex) {
      if(conn != null)
        try {conn.rollback();}catch(SQLException e) {logger.error("", e);}
        logger.error("", ex);
      }
      finally {
        if(rs != null)
          try {rs.close();}catch(SQLException ex) {logger.error("", ex);}
        if(conn != null)
          try {conn.close();}catch(SQLException ex) {logger.error("", ex);}
      }
    return exist;
  }


  @Override
  public boolean isExist() {
    boolean exist = false;
    ResultSet rs = null;
    Connection conn = null;
    try {
      conn = DBController.getConnection();
      DatabaseMetaData meta = conn.getMetaData();
      rs = meta.getColumns(System.getProperty("DB-NAME"),
              "%", getObjectTable().getName(), getName());
      while(rs.next())
        exist = true;
      conn.commit();
    }catch(SQLException ex) {
      try {
        if(conn != null)
          conn.rollback();
      }catch(SQLException e) {
        logger.error("", e);
      }
      logger.error("", ex);
    }finally {
      if(rs != null)
        try {rs.close();}catch(SQLException ex){logger.error("", ex);}
      if(conn != null)
        try {conn.close();}catch(SQLException ex){logger.error("", ex);}
    }
    return exist;
  }

  @Override
  public int compareTo(Object o) {
    DBColumn column = (DBColumn)o;
    String key = column.getDescription();
    if(key.equals(""))
      key = column.getName();
    String thisKey = getDescription();
    if(thisKey.equals(""))
      thisKey = getName();
    return thisKey.compareTo(key);
  }
}