package util;

public abstract class DBObject extends Element {
  private Class objectClass;
  private Class targetClass;
  private String description;
  private String getMethod;
  private String setMethod;
  
  public DBObject() {
  }
  
  public Class getTargetClass() {
    return targetClass;
  }

  public void setTargetClass(Class targetClass) {
    this.targetClass = targetClass;
  }

  public Class getObjectClass() {
    return objectClass;
  }

  public void setObjectClass(Class objectClass) {
    this.objectClass = objectClass;
  }

  public DBTable getObjectTable() {
    return DataBase.get(this.getObjectClass());
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getGetMethod() {
    return getMethod;
  }

  public String getSetMethod() {
    return setMethod;
  }

  public void setGetMethod(String getMethod) {
    this.getMethod = getMethod;
  }

  public void setSetMethod(String setMethod) {
    this.setMethod = setMethod;
  }
}
