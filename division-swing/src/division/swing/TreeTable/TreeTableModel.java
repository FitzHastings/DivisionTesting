package division.swing.TreeTable;

import java.util.ArrayList;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
 
public class TreeTableModel extends DefaultTreeModel {
  private int treeColumnIndex = 0;
  private final ArrayList<String>  columns = new ArrayList<>();
  private final ArrayList<String>  getMethods = new ArrayList<>();
  private final ArrayList<String>  setMethods = new ArrayList<>();
  private final ArrayList<Class>   setMethodParams = new ArrayList<>();
  private final ArrayList<Integer> columnEditable = new ArrayList<>();
  
  public TreeTableModel(TreeNode root) {
    super(root);
    columns.add("");
  }
  
  public void setColumnName(String columnName, int column) {
    columns.remove(column);
    columns.add(column,columnName);
  }
  
  public void removeColumn(int column) {
    if(column != treeColumnIndex) {
      if(column < treeColumnIndex)treeColumnIndex--;
      columns.remove(column);
      getMethods.remove(column);
      setMethods.remove(column);
      setMethodParams.remove(column);
    }
  }
  
  public void insertColumn(String columnName, int column, String getMetodName, String setMethodName, Class paramClass) {
    if(column <= treeColumnIndex)treeColumnIndex++;
    columns.add(column, columnName);
    getMethods.add(column, getMetodName);
    setMethods.add(column, setMethodName);
    setMethodParams.add(column, paramClass);
  }
  
  public void addColumn(String columnName, String getMetodName, String setMethodName, Class paramClass) {
    columns.add(columnName);
    getMethods.add(getMetodName);
    setMethods.add(setMethodName);
    setMethodParams.add(paramClass);
  }
  
  public int getColumnCount() {
    return columns.size();
  }

  public String getColumnName(int column) {
    return columns.get(column);
  }
  
  public Object getValueAt(TreeNode node, int column) {
    try {
      if(node != null) {
        if(column == treeColumnIndex)
          return ((DefaultMutableTreeNode)node).getUserObject();
        else
          return node.getClass().getMethod(getMethods.get(column-1), new Class[0]).invoke(node, new Object[0]);
      }
    }catch(Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }

  public void setValueAt(Object aValue, TreeNode node, int column) {
    try {
      if(column == treeColumnIndex)
        ((DefaultMutableTreeNode)node).setUserObject(aValue);
      else {
        if(node != null)
          node.getClass().getMethod(setMethods.get(column-1), new Class[]{setMethodParams.get(column-1)}).invoke(node, new Object[]{setMethodParams.get(column-1).cast(aValue)});
      }
    }catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public Class getColumnClass(int column) {
    if(column == treeColumnIndex)
      return TreeTableModel.class;
    else return setMethodParams.get(column-1);
  }

  public void setcolumnEditable(int column, boolean editable) {
    if(editable)
      columnEditable.add(column);
    else columnEditable.remove(column);
  }
 
  public boolean isCellEditable(TreeNode node, int column) {
    if(column == treeColumnIndex)
      return getColumnClass(column) == TreeTableModel.class;
    else return columnEditable.contains(column);
  }
}
