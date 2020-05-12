package division.swing.TreeTable;

import javax.swing.tree.DefaultMutableTreeNode;

public class TreeTableNode extends DefaultMutableTreeNode {
  private Integer id;

  public TreeTableNode(String name) {
    super(name);
  }

  public TreeTableNode(String name, Integer id) {
    this(name);
    this.id = id;
  }

  public Integer getId() {
    return id;
  }
}