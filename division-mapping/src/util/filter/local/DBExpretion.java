package util.filter.local;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;
import util.*;

public class DBExpretion implements Serializable {
  public enum Logic {AND,OR}
  public enum EqualType {EQUAL,NOT_EQUAL,IN,NOT_IN,LIKE,NOT_LIKE,ILIKE,NOT_ILIKE}
  public enum EqualDateType {EQUAL,NOT_EQUAL,BEFORE,AFTER,BEFORE_OR_EQUAL,AFTER_OR_EQUAL,BETWEEN}
  
  private DBFilter parent;
  private Logic logic;
  private EqualType equalType;
  private EqualDateType equalDateType;
  private String fieldName;
  private Object[] values;

  public DBExpretion(DBFilter parent, Logic logic, EqualType equalType, String fieldName, Object[] values) {
    this.parent = parent;
    this.logic = logic;
    this.equalType = equalType;
    this.fieldName = fieldName;
    this.values = values;
  }

  public DBExpretion(DBFilter parent, Logic logic, EqualDateType equalDateType, String fieldName, Date[] values) {
    this.parent = parent;
    this.logic = logic;
    this.equalDateType = equalDateType;
    this.fieldName = fieldName;
    this.values = values;
  }

  public DBExpretion(DBFilter parent, Logic logic, EqualDateType equalDateType, String fieldName, Timestamp[] values) {
    this.parent = parent;
    this.logic = logic;
    this.equalDateType = equalDateType;
    this.fieldName = fieldName;
    this.values = values;
  }

  public EqualDateType getEqualDateType() {
    return equalDateType;
  }

  public EqualType getEqualType() {
    return equalType;
  }

  public String getFieldName() {
    return fieldName;
  }

  public Logic getLogic() {
    return logic;
  }

  public DBFilter getParent() {
    return parent;
  }

  public Object[] getValues() {
    return values;
  }

  public void setParent(DBFilter parent) {
    this.parent = parent;
  }

  public void setLogic(Logic logic) {
    this.logic = logic;
  }

  public void setEqualType(EqualType equalType) {
    this.equalType = equalType;
  }

  public void setEqualDateType(EqualDateType equalDateType) {
    this.equalDateType = equalDateType;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public void setValues(Object[] values) {
    this.values = values;
  }
  
  public boolean isEmpty() {
    return getValues().length == 0;
  }
  
  public int size() {
    return getValues().length;
  }
  
  @Override
  public String toString() {
    String string = "";
    DBTable table = DataBase.get(parent.getTargetClass());
    DBRelation relation = table.getRelation(fieldName);
    if(relation != null && (relation instanceof ManyToMany || relation instanceof OneToMany)) {
      if(getEqualType() == EqualType.IN) {
        string += "["+parent.getTargetClass().getName()+"(id)] IN (SELECT ["+parent.getTargetClass().getName()+"("+fieldName+"):object] "
                + "FROM ["+parent.getTargetClass().getName()+"("+fieldName+"):table] "
                + "WHERE ["+parent.getTargetClass().getName()+"("+fieldName+"):target]=ANY(?))";
      }else if(getEqualType() == EqualType.NOT_IN) {
        string += "["+parent.getTargetClass().getName()+"(id)] NOT IN (SELECT ["+parent.getTargetClass().getName()+"("+fieldName+"):object] "
                + "FROM ["+parent.getTargetClass().getName()+"("+fieldName+"):table] "
                + "WHERE ["+parent.getTargetClass().getName()+"("+fieldName+"):target]=ANY(?))";
      }
    }else {
      if(!isEmpty()) {
        string += "["+parent.getTargetClass().getName()+"("+fieldName+")]";
        if(getEqualType() != null) {
          switch(getEqualType()) {
            case EQUAL:
              string += values[0]==null?" ISNULL":"=?";
              break;
            case NOT_EQUAL:
              string += values[0]==null?" NOTNULL":"!=?";
              break;
            case IN:
              string += "=ANY(?)";
              break;
            case NOT_IN:
              string += "<>ALL(?)";
              break;
            default:
              string += " "+getEqualType().toString().replaceAll("_", " ")+" ?";
              break;
          }
        }else if(getEqualDateType() != null) {
          switch(getEqualDateType()) {
            case BEFORE:
              string += "<?";
              break;
            case AFTER:
              string += ">?";
              break;
            case BEFORE_OR_EQUAL:
              string += "<=?";
              break;
            case AFTER_OR_EQUAL:
              string += ">=?";
              break;
            case EQUAL:
              string += values[0]==null?" ISNULL":"=?";
              break;
            case NOT_EQUAL:
              string += values[0]==null?" NOTNULL":"!=?";
              break;
            case BETWEEN:
              string += " BETWEEN ? AND ?";
              break;
          }
        }
      }
    }
    return string;
  }
}