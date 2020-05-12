package util;

import java.lang.reflect.Field;

public class ManyToMany extends DBRelation {
  private String relationTableName;
  private String objectColumnName;
  private String targetColumnName;
  private String orderBy;
  
  private String objectConsrtaintName;
  private String targetConsrtaintName;

  public ManyToMany(Class objectClass, Class targetClass, Field field) {
    super(objectClass, targetClass, field);
  }

  public String getOrderBy() {
    return orderBy;
  }

  public void setOrderBy(String orderBy) {
    this.orderBy = orderBy;
  }

  @Override
  public String getObjectColumnName() {
    return objectColumnName;
  }

  @Override
  public String getRelationTableName() {
    return relationTableName;
  }

  @Override
  public String getTargetColumnName() {
    return targetColumnName;
  }
  
  @Override
  public void configure() {
    if(getObjectTable() != null && getTargetTable() != null) {
      objectColumnName = getObjectTable().getName()+"_"+
              getObjectTable().getIdColumn().getName();
      targetColumnName = getTargetTable().getName()+"_"+
              getTargetTable().getIdColumn().getName();
      
      if(DBTable.isExist(this.getName()+"_"+getObjectTable().getName()+"_"+getTargetTable().getName()))
        relationTableName = this.getName()+"_"+getObjectTable().getName()+"_"+getTargetTable().getName();
      else if(DBTable.isExist(this.getName()+"_"+getTargetTable().getName()+"_"+getObjectTable().getName()))
        relationTableName = this.getName()+"_"+getTargetTable().getName()+"_"+getObjectTable().getName();
      if(relationTableName == null) {
        relationTableName = this.getName()+"_"+getObjectTable().getName()+"_"+getTargetTable().getName();
        execute("CREATE TABLE "+relationTableName+"("
                + objectColumnName+" INTEGER NOT NULL, "
                + targetColumnName+" INTEGER NOT NULL, "
                + "modificationDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "lastUserId INTEGER, "
                + "UNIQUE("+objectColumnName+","+targetColumnName+")"+
            ")");
      }
      
      alterTable();
    }
  }
  
  private void alterTable() {
    objectConsrtaintName = getObjectTable().getName()+"_"+objectColumnName+"_"+getTargetTable().getName()+"_"+getTargetTable().getIdColumn().getName();
    targetConsrtaintName = getTargetTable().getName()+"_"+targetColumnName+"_"+getObjectTable().getName()+"_"+getObjectTable().getIdColumn().getName();
    
    
    String createObjectForeignKey = "ALTER TABLE "+relationTableName+" ADD CONSTRAINT "+objectConsrtaintName+" FOREIGN KEY("+objectColumnName+") REFERENCES "+
            getObjectTable().getName()+"("+getObjectTable().getIdColumn().getName()+") " +
            "ON UPDATE "+getOn_update().toString().replaceAll("_", " ")+" ON DELETE "+getOn_delete().toString().replaceAll("_", " ");
    
    String createTargetForeignKey = "ALTER TABLE "+relationTableName+" ADD CONSTRAINT "+targetConsrtaintName+" FOREIGN KEY("+targetColumnName+") REFERENCES "+
            getTargetTable().getName()+"("+getTargetTable().getIdColumn().getName()+") " +
            "ON UPDATE "+getOn_update().toString().replaceAll("_", " ")+" ON DELETE "+getOn_delete().toString().replaceAll("_", " ");
    
    String dropObjectForeignKey = "ALTER TABLE "+getObjectTable().getName()+" DROP CONSTRAINT IF EXISTS "+objectConsrtaintName;
    String dropTargetForeignKey = "ALTER TABLE "+getObjectTable().getName()+" DROP CONSTRAINT IF EXISTS "+targetConsrtaintName;

    execute(dropObjectForeignKey);
    execute(dropTargetForeignKey);
    execute(createObjectForeignKey);
    execute(createTargetForeignKey);
    
    if(!DBColumn.isExist(relationTableName, "modificationDate"))
      execute("ALTER TABLE "+relationTableName+" ADD COLUMN modificationDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
    if(!DBColumn.isExist(relationTableName, "lastUserId"))
      execute("ALTER TABLE "+relationTableName+" ADD COLUMN lastUserId INTEGER");
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