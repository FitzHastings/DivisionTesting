package util.filter.local;

import division.fx.PropertyMap;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.lang3.ArrayUtils;

public class DBFilter extends DBExpretion implements Serializable {
  private CopyOnWriteArrayList<DBExpretion> expretions = new CopyOnWriteArrayList<>();
  private Class targetClass;
  
  private DBFilter(Class targetClass) {
    this(null, null, targetClass);
  }
  
  private DBFilter(String targetClass) throws ClassNotFoundException {
    super(null, null, null, null, new Object[0]);
    try {
      this.targetClass = Class.forName(targetClass);
    }catch (ClassNotFoundException ex) {
      this.targetClass = Class.forName("bum.interfaces."+targetClass);
    }
  }

  private DBFilter(DBFilter parent, Logic logic, Class targetClass) {
    super(parent, logic, null, null, new Object[0]);
    this.targetClass = targetClass;
  }
  
  
  
  public static DBFilter create(Class targetClass) {
    return new DBFilter(targetClass);
  }
  
  public static DBFilter create(String targetClass) throws ClassNotFoundException {
    return new DBFilter(targetClass);
  }
  
  @Override
  public int size() {
    return expretions.size();
  }
  
  public DBExpretion get(int index) {
    return expretions.get(index);
  }
  
  public DBFilter AND_FILTER() {
    return AND_FILTER(new DBFilter(this, Logic.AND, getTargetClass()));
  }
  
  public DBFilter OR_FILTER() {
    return OR_FILTER(new DBFilter(this, Logic.OR, getTargetClass()));
  }
  
  public DBFilter AND_FILTER(DBFilter filter) {
    filter.setParent(this);
    filter.setLogic(Logic.AND);
    expretions.add(filter);
    return filter;
  }
  
  public DBFilter OR_FILTER(DBFilter filter) {
    filter.setLogic(Logic.OR);
    expretions.add(filter);
    return filter;
  }

  public Class getTargetClass() {
    return targetClass;
  }

  public DBFilter clear() {
    expretions.clear();
    return this;
  }
  
  
  
  public DBFilter AND_LIKE(String fieldName,Object value) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualType.LIKE, fieldName, new Object[]{value}));
    return this;
  }
  
  public DBFilter AND_ILIKE(String fieldName,Object value) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualType.ILIKE, fieldName, new Object[]{value}));
    return this;
  }
  
  public DBFilter OR_LIKE(String fieldName,Object value) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualType.LIKE, fieldName, new Object[]{value}));
    return this;
  }
  
  public DBFilter OR_ILIKE(String fieldName,Object value) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualType.ILIKE, fieldName, new Object[]{value}));
    return this;
  }
  
  
  
  public DBFilter AND_NOT_LIKE(String fieldName,Object value) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualType.NOT_LIKE, fieldName, new Object[]{value}));
    return this;
  }
  
  public DBFilter AND_NOT_ILIKE(String fieldName,Object value) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualType.NOT_ILIKE, fieldName, new Object[]{value}));
    return this;
  }
  
  public DBFilter OR_NOT_LIKE(String fieldName,Object value) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualType.NOT_LIKE, fieldName, new Object[]{value}));
    return this;
  }
  
  public DBFilter OR_NOT_ILIKE(String fieldName,Object value) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualType.NOT_ILIKE, fieldName, new Object[]{value}));
    return this;
  }
  
  

  public DBFilter AND_EQUAL(String fieldName,Object value) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualType.EQUAL, fieldName, new Object[]{value}));
    return this;
  }

  public DBFilter OR_EQUAL(String fieldName,Object value) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualType.EQUAL, fieldName, new Object[]{value}));
    return this;
  }

  public DBFilter AND_NOT_EQUAL(String fieldName,Object value) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualType.NOT_EQUAL, fieldName, new Object[]{value}));
    return this;
  }

  public DBFilter OR_NOT_EQUAL(String fieldName,Object value) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualType.NOT_EQUAL, fieldName, new Object[]{value}));
    return this;
  }
  
  
  
  public DBFilter AND_IN(String fieldName, Collection<PropertyMap> values) {
    return AND_IN(fieldName, PropertyMap.getSetFromList(values, "id", Integer.class).toArray(new Integer[0]));
  }
  
  public DBFilter AND_IN(String fieldName, Object[] values) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualType.IN, fieldName, new Object[]{values}));
    return this;
  }

  public DBFilter OR_IN(String fieldName, Object[] values) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualType.IN, fieldName, new Object[]{values}));
    return this;
  }

  public DBFilter AND_NOT_IN(String fieldName, Object[] values) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualType.NOT_IN, fieldName, new Object[]{values}));
    return this;
  }

  public DBFilter OR_NOT_IN(String fieldName, Object[] values) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualType.NOT_IN, fieldName, new Object[]{values}));
    return this;
  }
  
  

  public DBFilter AND_DATE_AFTER(String fieldName, Date date) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.AFTER, fieldName, new java.sql.Date[]{new java.sql.Date(date.getTime())}));
    return this;
  }
  
  public DBFilter AND_DATE_AFTER_OR_EQUAL(String fieldName, Date date) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.AFTER_OR_EQUAL, fieldName, new java.sql.Date[]{new java.sql.Date(date.getTime())}));
    return this;
  }

  public DBFilter AND_DATE_BEFORE(String fieldName, Date date) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.BEFORE, fieldName, new java.sql.Date[]{new java.sql.Date(date.getTime())}));
    return this;
  }
  
  public DBFilter AND_DATE_BEFORE_OR_EQUAL(String fieldName, Date date) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.BEFORE_OR_EQUAL, fieldName, new java.sql.Date[]{new java.sql.Date(date.getTime())}));
    return this;
  }

  public DBFilter AND_DATE_BETWEEN(String fieldName, Date startDate, Date endDate) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.BETWEEN, fieldName, new java.sql.Date[]{new java.sql.Date(startDate.getTime()),new java.sql.Date(endDate.getTime())}));
    return this;
  }

  public DBFilter AND_DATE_EQUAL(String fieldName, Date date) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.EQUAL, fieldName, new java.sql.Date[]{new java.sql.Date(date.getTime())}));
    return this;
  }

  public DBFilter AND_DATE_NOT_EQUAL(String fieldName, Date date) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.NOT_EQUAL, fieldName, new java.sql.Date[]{new java.sql.Date(date.getTime())}));
    return this;
  }

  public DBFilter OR_DATE_AFTER(String fieldName, Date date) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.AFTER, fieldName, new java.sql.Date[]{new java.sql.Date(date.getTime())}));
    return this;
  }
  
  public DBFilter OR_DATE_AFTER_OR_EQUAL(String fieldName, Date date) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.AFTER_OR_EQUAL, fieldName, new java.sql.Date[]{new java.sql.Date(date.getTime())}));
    return this;
  }

  public DBFilter OR_DATE_BEFORE(String fieldName, Date date) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.BEFORE, fieldName, new java.sql.Date[]{new java.sql.Date(date.getTime())}));
    return this;
  }
  
  public DBFilter OR_DATE_BEFORE_OR_EQUAL(String fieldName, Date date) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.BEFORE_OR_EQUAL, fieldName, new java.sql.Date[]{new java.sql.Date(date.getTime())}));
    return this;
  }

  public DBFilter OR_DATE_BETWEEN(String fieldName, Date startDate, Date endDate) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.BETWEEN, fieldName, new java.sql.Date[]{new java.sql.Date(startDate.getTime()),new java.sql.Date(endDate.getTime())}));
    return this;
  }

  public DBFilter OR_DATE_EQUAL(String fieldName, Date date) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.EQUAL, fieldName, new java.sql.Date[]{new java.sql.Date(date.getTime())}));
    return this;
  }

  public DBFilter OR_DATE_NOT_EQUAL(String fieldName, Date date) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.NOT_EQUAL, fieldName, new java.sql.Date[]{new java.sql.Date(date.getTime())}));
    return this;
  }


  public DBFilter AND_DATE_AFTER(String fieldName, java.sql.Date date) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.AFTER, fieldName, new java.sql.Date[]{date}));
    return this;
  }
  
  public DBFilter AND_DATE_AFTER_OR_EQUAL(String fieldName, java.sql.Date date) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.AFTER_OR_EQUAL, fieldName, new java.sql.Date[]{date}));
    return this;
  }

  public DBFilter AND_DATE_BEFORE(String fieldName, java.sql.Date date) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.BEFORE, fieldName, new java.sql.Date[]{date}));
    return this;
  }
  
  public DBFilter AND_DATE_BEFORE_OR_EQUAL(String fieldName, java.sql.Date date) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.BEFORE_OR_EQUAL, fieldName, new java.sql.Date[]{date}));
    return this;
  }

  public DBFilter AND_DATE_BETWEEN(String fieldName, java.sql.Date startDate, java.sql.Date endDate) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.BETWEEN, fieldName, new java.sql.Date[]{startDate,endDate}));
    return this;
  }

  public DBFilter AND_DATE_EQUAL(String fieldName, java.sql.Date date) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.EQUAL, fieldName, new java.sql.Date[]{date}));
    return this;
  }

  public DBFilter AND_DATE_NOT_EQUAL(String fieldName, java.sql.Date date) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.NOT_EQUAL, fieldName, new java.sql.Date[]{date}));
    return this;
  }


  public DBFilter OR_DATE_AFTER(String fieldName, java.sql.Date date) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.AFTER, fieldName, new java.sql.Date[]{date}));
    return this;
  }
  
  public DBFilter OR_DATE_AFTER_OR_EQUAL(String fieldName, java.sql.Date date) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.AFTER_OR_EQUAL, fieldName, new java.sql.Date[]{date}));
    return this;
  }

  public DBFilter OR_DATE_BEFORE(String fieldName, java.sql.Date date) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.BEFORE, fieldName, new java.sql.Date[]{date}));
    return this;
  }
  
  public DBFilter OR_DATE_BEFORE_OR_EQUAL(String fieldName, java.sql.Date date) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.BEFORE_OR_EQUAL, fieldName, new java.sql.Date[]{date}));
    return this;
  }

  public DBFilter OR_DATE_BETWEEN(String fieldName, java.sql.Date startDate, java.sql.Date endDate) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.BETWEEN, fieldName, new java.sql.Date[]{startDate,endDate}));
    return this;
  }

  public DBFilter OR_DATE_EQUAL(String fieldName, java.sql.Date date) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.EQUAL, fieldName, new java.sql.Date[]{date}));
    return this;
  }

  public DBFilter OR_DATE_NOT_EQUAL(String fieldName, java.sql.Date date) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.NOT_EQUAL, fieldName, new java.sql.Date[]{date}));
    return this;
  }


  public DBFilter AND_TIMESTAMP_AFTER(String fieldName, Timestamp date) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.AFTER, fieldName, new Timestamp[]{date}));
    return this;
  }
  
  public DBFilter AND_TIMESTAMP_AFTER_OR_EQUAL(String fieldName, Timestamp date) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.AFTER_OR_EQUAL, fieldName, new Timestamp[]{date}));
    return this;
  }

  public DBFilter AND_TIMESTAMP_BEFORE(String fieldName, Timestamp date) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.BEFORE, fieldName, new Timestamp[]{date}));
    return this;
  }
  
  public DBFilter AND_TIMESTAMP_BEFORE_OR_EQUAL(String fieldName, Timestamp date) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.BEFORE_OR_EQUAL, fieldName, new Timestamp[]{date}));
    return this;
  }

  public DBFilter AND_TIMESTAMP_BETWEEN(String fieldName, Timestamp startDate, Timestamp endDate) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.BETWEEN, fieldName, new Timestamp[]{startDate,endDate}));
    return this;
  }

  public DBFilter AND_TIMESTAMP_EQUAL(String fieldName, Timestamp date) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.EQUAL, fieldName, new Timestamp[]{date}));
    return this;
  }

  public DBFilter AND_TIMESTAMP_NOT_EQUAL(String fieldName, Timestamp date) {
    expretions.add(new DBExpretion(this, Logic.AND, EqualDateType.NOT_EQUAL, fieldName, new Timestamp[]{date}));
    return this;
  }


  public DBFilter OR_TIMESTAMP_AFTER(String fieldName, Timestamp date) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.AFTER, fieldName, new Timestamp[]{date}));
    return this;
  }
  
  public DBFilter OR_TIMESTAMP_AFTER_OR_EQUAL(String fieldName, Timestamp date) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.AFTER_OR_EQUAL, fieldName, new Timestamp[]{date}));
    return this;
  }

  public DBFilter OR_TIMESTAMP_BEFORE(String fieldName, Timestamp date) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.BEFORE, fieldName, new Timestamp[]{date}));
    return this;
  }
  
  public DBFilter OR_TIMESTAMP_BEFORE_OR_EQUAL(String fieldName, Timestamp date) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.BEFORE_OR_EQUAL, fieldName, new Timestamp[]{date}));
    return this;
  }

  public DBFilter OR_TIMESTAMP_BETWEEN(String fieldName, Timestamp startDate, Timestamp endDate) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.BETWEEN, fieldName, new Timestamp[]{startDate,endDate}));
    return this;
  }

  public DBFilter OR_TIMESTAMP_EQUAL(String fieldName, Timestamp date) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.EQUAL, fieldName, new Timestamp[]{date}));
    return this;
  }

  public DBFilter OR_TIMESTAMP_NOT_EQUAL(String fieldName, Timestamp date) {
    expretions.add(new DBExpretion(this, Logic.OR, EqualDateType.NOT_EQUAL, fieldName, new Timestamp[]{date}));
    return this;
  }

  @Override
  public Object[] getValues() {
    Object[] values = new Object[0];
    for(DBExpretion expretion:expretions) {
      if(!expretion.isEmpty()) {
        if(
                !((expretion.getEqualType() != null && 
                (expretion.getEqualType() == EqualType.EQUAL || expretion.getEqualType() == EqualType.NOT_EQUAL) ||
                expretion.getEqualDateType() != null && 
                (expretion.getEqualDateType() == EqualDateType.EQUAL || expretion.getEqualDateType() == EqualDateType.NOT_EQUAL)) &&
                expretion.getValues().length == 1 && expretion.getValues()[0] == null))
        values = ArrayUtils.addAll(values, expretion.getValues());
      }
    }
    return values;
  }

  public void toEstablishes(PropertyMap p) {
    expretions.stream().forEach(ex -> {
      if(ex.getLogic() != Logic.OR) {
        if(ex instanceof DBFilter)
          ((DBFilter)ex).toEstablishes(p);
        else if(!ex.isEmpty() &&
                ((ex.getEqualType() != null && ex.getEqualType() != EqualType.NOT_EQUAL && ex.getEqualType() != EqualType.NOT_IN && ex.getEqualType() != EqualType.NOT_LIKE && ex.getEqualType() != EqualType.NOT_ILIKE) || 
                (ex.getEqualDateType() != null && ex.getEqualDateType() != EqualDateType.NOT_EQUAL))) {
          p.setValue(ex.getFieldName(), ex.getValues()[0]);
        }
      }
    });
  }

  @Override
  public String toString() {
    String string = "";
    if(!isEmpty()) {
      for(int i=0;i<expretions.size();i++) {
        DBExpretion exp = expretions.get(i);
        if(!exp.isEmpty()) {
          string += (i>0&&!string.equals("")?" "+expretions.get(i).getLogic()+" ":"")+expretions.get(i);
        }
      }
      if(getParent() != null && size() > 1)
        string = "("+string+")";
    }
    return string;
  }
  
  @Override
  public boolean isEmpty() {
    boolean empty = true;
    for(DBExpretion expretion:expretions) {
      if(!expretion.isEmpty()) {
        empty = false;
        break;
      }
    }
    return empty;
  }
}