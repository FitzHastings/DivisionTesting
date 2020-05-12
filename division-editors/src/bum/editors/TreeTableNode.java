package bum.editors;

import javax.swing.tree.MutableTreeNode;
import mapping.MappingObject;

public interface TreeTableNode extends MutableTreeNode {
  public int getId();
  public String getName();
  public MappingObject getObject();
}