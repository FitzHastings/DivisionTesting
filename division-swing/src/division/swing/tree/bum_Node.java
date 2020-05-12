package division.swing.tree;

//import bum.interfaces.RMIDBNodeObject;
import java.rmi.RemoteException;
import java.util.Objects;

public class bum_Node extends Node {
  //private RMIDBNodeObject object;
  private Integer id;
  
  public bum_Node(String name, Tree tree) {
    super(name, tree);
  }
  
  /*public bum_Node(RMIDBNodeObject object, Tree tree) throws RemoteException {
    this(object.getName(), tree);
    this.object = object;
    this.id = this.object.getId();
  }*/
  
  public bum_Node(String name, Integer id, Tree tree) {
    this(name, tree);
    this.id = id;
  }
  
  public Tree getTree() {
    return this.tree;
  }
  
  /*public RMIDBNodeObject getObject() {
    return this.object;
  }*/

  public Integer getId() {
    /*if(this.object != null && this.id == null) {
      try {
        this.id = this.object.getId();
      }catch(RemoteException ex) {
        ex.printStackTrace();
      }
    }*/
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if(!(obj instanceof bum_Node))
      return false;
    bum_Node other = (bum_Node)obj;
    /*if(this.object != null)
      return this.object.equals(other.getObject());*/
    if(this.id != other.getId())
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    //hash = 41 * hash + Objects.hashCode(this.object);
    hash = 41 * hash + Objects.hashCode(this.id);
    return hash;
  }
}
