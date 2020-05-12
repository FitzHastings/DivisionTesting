package division.swing.TreeTable;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class TreeTableModelAdapter extends AbstractTableModel {
  private final JTree tree;
  private final TreeTableModel treeTableModel;

  public TreeTableModelAdapter(TreeTableModel treeTableModel, JTree tree) {
    this.tree = tree;
    this.treeTableModel = treeTableModel;

    tree.addTreeExpansionListener(new TreeExpansionListener() {
      @Override
      public void treeExpanded(TreeExpansionEvent event) {
        fireTableDataChanged(); 
      }
      
      @Override
      public void treeCollapsed(TreeExpansionEvent event) {
        fireTableDataChanged(); 
      }
    });

    treeTableModel.addTreeModelListener(new TreeModelListener() {
      @Override
      public void treeNodesChanged(TreeModelEvent e) {
        delayedFireTableDataChanged();
      }

      @Override
      public void treeNodesInserted(TreeModelEvent e) {
        delayedFireTableDataChanged();
      }

      @Override
      public void treeNodesRemoved(TreeModelEvent e) {
        delayedFireTableDataChanged();
      }

      @Override
      public void treeStructureChanged(TreeModelEvent e) {
        delayedFireTableDataChanged();
      }
    });
  }

  @Override
  public int getColumnCount() {
    return treeTableModel.getColumnCount();
  }

  @Override
  public String getColumnName(int column) {
    return treeTableModel.getColumnName(column);
  }

  @Override
  public Class getColumnClass(int column) {
    return treeTableModel.getColumnClass(column);
  }

  @Override
  public int getRowCount() {
    return tree.getRowCount();
  }

  protected TreeNode nodeForRow(int row) {
    TreePath treePath = tree.getPathForRow(row);
    if(treePath == null)
        return null;
    return (TreeNode)treePath.getLastPathComponent();         
  }

  @Override
  public Object getValueAt(int row, int column) {
    return treeTableModel.getValueAt(nodeForRow(row), column);
  }

  @Override
  public boolean isCellEditable(int row, int column) {
    return treeTableModel.isCellEditable(nodeForRow(row), column); 
  }

  @Override
  public void setValueAt(Object value, int row, int column) {
    treeTableModel.setValueAt(value, nodeForRow(row), column);
  }

  protected void delayedFireTableDataChanged() {
    SwingUtilities.invokeLater(() ->  fireTableDataChanged());
  }
}

