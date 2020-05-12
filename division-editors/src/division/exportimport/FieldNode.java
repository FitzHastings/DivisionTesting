package division.exportimport;

import division.swing.tree.Node;
import division.swing.tree.Tree;
import java.util.Map;
import java.util.Objects;
import mapping.MappingObject;

public class FieldNode extends Node {
  private Map<String, Object> data;

  public FieldNode(Tree tree, Map<String, Object> data) {
    super(data.get("DESCRIPTION").equals("")?data.get("NAME").toString():data.get("DESCRIPTION").toString(), tree);
    this.data = data;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public String getGetMethod() {
    return data.containsKey("GETMETHOD")?data.get("GETMETHOD").toString():null;
  }

  public String getSetMethod() {
    return data.containsKey("SETMETHOD")?data.get("SETMETHOD").toString():null;
  }

  public String getFieldName() {
    return data.get("NAME").toString();
  }

  public String getFieldDescription() {
    return data.get("DESCRIPTION").equals("")?data.get("NAME").toString():data.get("DESCRIPTION").toString();
  }

  public Class<? extends MappingObject> getObjectClass() {
    return (Class)data.get("CLASS");
  }

  public String getObjectClassDescription() {
    return data.containsKey("CLASSDESCRIPTION")?data.get("CLASSDESCRIPTION").toString():null;
  }

  public Class getFieldType() {
    return (Class)data.get("TYPE");
  }
  
  public Class getParameterizedType() {
    return (Class)data.get("PARAMETERIZEDTYPE");
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final FieldNode other = (FieldNode) obj;
    if (!Objects.equals(this.data, other.data)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 97 * hash + Objects.hashCode(this.data);
    return hash;
  }
}