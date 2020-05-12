package util;

import java.lang.reflect.Field;
import org.apache.log4j.Logger;

public abstract class DBRelation extends DBObject {
  public enum ActionType{CASCADE,RESTRICT,NO_ACTION,SET_NULL,SET_DEFAULT}
  
  private ActionType on_delete;
  private ActionType on_update;
  
  private Field field;
  
  private String mappedBy = null;
  
  public DBRelation(Class objectClass, Class TargetClass, Field field) {
    this.setObjectClass(objectClass);
    this.setTargetClass(TargetClass);
    this.field = field;

    try {
      setSetMethod("set"+field.getName().substring(0, 1).toUpperCase()+field.getName().substring(1));
      setGetMethod("get"+field.getName().substring(0, 1).toUpperCase()+field.getName().substring(1));
    }catch(Exception ex) {
      Logger.getRootLogger().error(ex);
    }
  }

  public ActionType getOn_delete() {
    return on_delete;
  }

  public void setOn_delete(ActionType on_delete) {
    this.on_delete = on_delete;
  }

  public ActionType getOn_update() {
    return on_update;
  }

  public void setOn_update(ActionType on_update) {
    this.on_update = on_update;
  }
  
  public Field getField() {
    return field;
  }
  
  public DBTable getTargetTable() {
    return DataBase.get(this.getTargetClass());
  }
  
  public String getRelationTableName() {
    return getTargetTable().getName();
  }

  public void setMappedBy(String mappedBy) {
    this.mappedBy = mappedBy;
  }

  public abstract String getObjectColumnName();

  public abstract String getTargetColumnName();

  //public abstract String getConditionForFilter(Object[] objects, EqualType type);
  
  //public abstract String getConditionForFilter(String sql, EqualType type);
  
  //public abstract String getCondition(Object object);
  
  //public abstract Object[] getValues(Object object, Connection connect);
  
  /*public boolean setValues(Object object, Object[] values, Connection connect) {
    removeValues(object,connect);
    return addValues(object,values,connect);
  }*/
  
  //public abstract boolean addValues(Object object, Object[] objects, Connection connect);
  
  //public abstract void removeValues(Object object, Object[] objects, Connection connect);
  
  //public abstract void removeValues(Object object, Connection connect);
}
