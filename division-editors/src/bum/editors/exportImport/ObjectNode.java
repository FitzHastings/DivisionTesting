package bum.editors.exportImport;

import division.swing.tree.Node;
import division.swing.tree.Tree;
import java.util.Map;
import mapping.MappingObject;

public class ObjectNode extends Node {
  private Map<String, Object> data;

  public ObjectNode(Tree tree, Map<String, Object> data) {
    super(data.get("DESCRIPTION").equals("")?data.get("NAME").toString():data.get("DESCRIPTION").toString(), tree);
    this.data = data;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public String getGetMethod() {
    return data.get("GETMETHOD").toString();
  }

  public String getSetMethod() {
    return data.get("SETMETHOD").toString();
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
    return data.get("CLASSDESCRIPTION").toString();
  }

  public Class getFieldType() {
    return (Class)data.get("TYPE");
  }
}