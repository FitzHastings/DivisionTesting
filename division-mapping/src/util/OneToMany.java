package util;

import java.lang.reflect.Field;

public class OneToMany extends DBRelation {
  private DBColumn column;
  private String   orderBy;
  private String   consrtaintName;
  
  public OneToMany(Class objectClass, Class TargetClass, Field field) {
    super(objectClass, TargetClass, field);
  }

  public String getOrderBy() {
    return orderBy;
  }

  public void setOrderBy(String orderBy) {
    this.orderBy = orderBy;
  }
  
  @Override
  public void configure() {
    if(getObjectTable() != null && getTargetTable() != null){
      DBColumn idColumn = getObjectTable().getIdColumn();
      if(idColumn != null){
        column = new DBColumn(getObjectClass(), Integer.class, this.getName()+"_"+getObjectTable().getName()+"_"+idColumn.getName());

        consrtaintName = getTargetTable().getName()+"_"+column.getName()+"_"+getObjectTable().getName()+"_"+getObjectTable().getIdColumn().getName();

        alterTable();
      }
    }
  }

  private void alterTable() {
    String createColumn = "ALTER TABLE "+getTargetTable().getName()+" ADD COLUMN "+column;
    String createForeignKey = "ALTER TABLE "+getTargetTable().getName()+" ADD CONSTRAINT "+consrtaintName+" FOREIGN KEY("+column.getName()+") REFERENCES "+
            getObjectTable().getName()+"("+getObjectTable().getIdColumn().getName()+") " +
            "ON UPDATE "+getOn_update().toString().replaceAll("_", " ")+" ON DELETE "+getOn_delete().toString().replaceAll("_", " ");
    String dropForeignKey = "ALTER TABLE "+getTargetTable().getName()+" DROP CONSTRAINT IF EXISTS "+consrtaintName;

    if(!DBColumn.isExist(getTargetTable().getName(), column.getName()))
      execute(createColumn);
    execute(dropForeignKey);
    execute(createForeignKey);
  }
  
  @Override
  public String getObjectColumnName() {
    return null;
  }

  @Override
  public String getTargetColumnName() {
    return getColumn().getName();
  }
  
  public DBColumn getColumn() {
    return column;
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
}