package division.xml;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.TreeMap;

public class Node implements Cloneable, Serializable {
  private String name;
  private String value;
  private TreeMap<String,String> attributes = new TreeMap<>();
  private ArrayList<Node> nodes = new ArrayList<>();
  protected Node parent = null;

  public Node() {
    this("","");
  }

  public Node(String name) {
    this(name,"");
  }
  
  public Node(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public Node getParent() {
    return parent;
  }

  public void clear() {
    nodes.clear();
    attributes.clear();
  }
  
  public boolean isRoot() {
    return getParent() == null;
  }

  public boolean isEmpty() {
    return nodes.isEmpty();
  }

  public int getNodesCount() {
    return getNodes().size();
  }
  
  public void addNode(Node node) {
    node.parent = this;
    nodes.add(node);
  }
  
  public void inserNode(Node node, int index) {
    node.parent = this;
    nodes.add(index, node);
  }
  
  public void removeNode(int index) {
    nodes.remove(index).parent = null;
  }
  
  public void removeNode(Node node) {
    nodes.remove(node);
    node.parent = this;
  }
  
  public Node getNode(int index) {
    return nodes.get(index);
  }

  public Node getNode(String nodeName) {
    for(int i=0;i<nodes.size();i++)
      if(nodes.get(i).getName().equals(nodeName))
        return nodes.get(i);
    return null;
  }

  public ArrayList<Node> getNodes() {
    return nodes;
  }

  public String getAttribute(String attributeName) {
    return attributes.get(attributeName);
  }
  
  public void setAttribute(String attributeName, String attributeValue) {
    attributes.put(attributeName, attributeValue);
  }

  public TreeMap<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(TreeMap<String, String> attributes) {
    this.attributes = attributes;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }
  
  public boolean getBoolean() {
    return Boolean.getBoolean(value);
  }
  
  public int getInt() {
    return Integer.valueOf(value);
  }
  
  public double getDouble() {
    return Double.valueOf(value);
  }
  
  public float getFloat() {
    return Float.valueOf(value);
  }
  
  public long getLong() {
    return Long.valueOf(value);
  }
  
  public BigDecimal getBigDecimal() {
    return BigDecimal.valueOf(getDouble());
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Node other = (Node) obj;
    if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
      return false;
    }
    if ((this.value == null) ? (other.value != null) : !this.value.equals(other.value)) {
      return false;
    }
    if (this.attributes != other.attributes && (this.attributes == null || !this.attributes.equals(other.attributes))) {
      return false;
    }
    if (this.nodes != other.nodes && (this.nodes == null || !this.nodes.equals(other.nodes))) {
      return false;
    }
    return true;
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  @Override
  public String toString() {
    String str = "";
    Node p = this;
    while((p = p.getParent()) != null) {
      str += " ";
    }
    str += getName()+"\n";
    for(Node n:getNodes())
      str += n.toString();
    return str;
  }
}