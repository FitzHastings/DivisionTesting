package util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ManyToOne extends DBRelation {
  private DBColumn column;
  private boolean saveNow = false;
  private String consrtaintName;
  
  private String[] viewFields;
  private String[] viewNames;
  
  private boolean nullAble;
  
  public ManyToOne(Class objectClass, Class TargetClass, Field field) {
    super(objectClass, TargetClass, field);
  }

  public boolean isSaveNow() {
    return saveNow;
  }

  public void setSaveNow(boolean saveNow) {
    this.saveNow = saveNow;
  }
  
  public DBColumn getColumn() {
    return this.column;
  }

  public boolean isNullAble() {
    return nullAble;
  }

  public void setNullAble(boolean nullAble) {
    this.nullAble = nullAble;
  }

  public String[] getViewFields() {
    return viewFields;
  }

  public void setViewFields(String[] viewFields) {
    this.viewFields = viewFields;
  }

  public String[] getViewNames() {
    return viewNames;
  }

  public void setViewNames(String[] viewNames) {
    this.viewNames = viewNames;
  }
  
  public String getViewFieldQuery(String fieldName) {
    if(fieldName.endsWith(getTargetTable().getIdColumn().getName()))
      return getObjectTable().getName()+"."+column.getName();
    String columnName = null;
    DBColumn col = getTargetTable().getColumn(fieldName);
    if(col != null)
      columnName = col.getName();
    else {
      DBRelation relation = getTargetTable().getRelation(fieldName);
      if(relation != null && relation instanceof ManyToOne) {
        columnName = relation.getObjectColumnName();
      }
    }

    if(columnName == null)
      return "";

    String sql = "SELECT "+columnName+
            " FROM "+getTargetTable().getName()+" WHERE "+
            getTargetTable().getIdColumn().getName()+"="+getObjectTable().getName()+"."+column.getName();
    return sql;
  }
  
  @Override
  public void configure() {
    if(getObjectTable() != null && getTargetTable() != null) {
      DBColumn idColumn = getTargetTable().getIdColumn();
      if(idColumn != null) {
        column = new DBColumn(getObjectClass(), Integer.class, this.getName()+"_"+getTargetTable().getName()+"_"+idColumn.getName());
        column.setNullAble(isNullAble());
        
        String getName = "get"+getField().getName().substring(0, 1).toUpperCase()+getField().getName().substring(1);
        String setName = "set"+getField().getName().substring(0, 1).toUpperCase()+getField().getName().substring(1);
        column.setGetMethod(getMethod(getObjectClass(), getName, new Class[0]).getName());
        column.setSetMethod(getMethod(getObjectClass(), setName, new Class[]{getTargetClass()}).getName());

        consrtaintName = getObjectTable().getName()+"_"+column.getName()+"_"+getTargetTable().getName()+"_"+getTargetTable().getIdColumn().getName();
        
        alterTable();
      }
    }
  }
  
  private Method getMethod(Class clazz, String name, Class[] classes) {
    Class parent = clazz;
    Method method = null;
    while(method == null) {
      try {
        method = parent.getMethod(name,classes);
      }catch(NoSuchMethodException | SecurityException ex) {
        if(classes.length > 0) {
          for(Class c:classes[0].getInterfaces()) {
            try {
              method = getMethod(parent,name,new Class[]{c});
            }catch(Exception x) {
              logger.error("", x);
            }
          }
        }
      }
      parent = parent.getSuperclass();
    }
    return method;
  }
  
  private void alterTable() {
    if(!column.isExist())
      execute("ALTER TABLE "+getObjectTable().getName()+" ADD COLUMN "+column);
    
    execute("ALTER TABLE "+getObjectTable().getName()+" DROP CONSTRAINT IF EXISTS "+consrtaintName);
    execute("ALTER TABLE "+getObjectTable().getName()+" ADD CONSTRAINT "+consrtaintName+" FOREIGN KEY("+column.getName()+") REFERENCES "+
            getTargetTable().getName()+"("+getTargetTable().getIdColumn().getName()+") " +
            "ON UPDATE "+getOn_update().toString().replaceAll("_", " ")+" ON DELETE "+getOn_delete().toString().replaceAll("_", " "));
  }

  @Override
  public void create() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void drop() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean isExist() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public String getObjectColumnName() {
    return getColumn().getName();
  }

  @Override
  public String getTargetColumnName() {
    return null;
  }
}