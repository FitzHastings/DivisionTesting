package division.swing;

import java.rmi.RemoteException;
import java.util.Objects;
import mapping.MappingObject;

public class DivisionItem {
  protected MappingObject object;
  protected String name;
  protected Integer id;
  protected String className;
  protected Object data;

  public DivisionItem() {
  }
  
  public DivisionItem(Integer id, String name, String className) {
    this(id, name, className, null);
  }
  
  public DivisionItem(Integer id, String name, String className, Object data) {
    this.id = id;
    this.name = name;
    this.className = className;
    this.data = data;
  }
  
  public DivisionItem(MappingObject object) throws RemoteException {
    setObject(object);
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setObject(MappingObject object) throws RemoteException {
    this.object = object;
    name      = this.object.getName();
    id        = this.object.getId();
    className = this.object.getRealClassName();
  }

  public Object getData() {
    return data;
  }

  public Integer getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public MappingObject getObject() {
    return object;
  }

  public String getClassName() {
    return className;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final DivisionItem other = (DivisionItem) obj;
    if (!Objects.equals(this.name, other.name)) {
      return false;
    }
    if (!Objects.equals(this.id, other.id)) {
      return false;
    }
    if (!Objects.equals(this.className, other.className)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    return hash;
  }

  

  @Override
  public String toString() {
    return getName();
  }
}