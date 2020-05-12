package division.swing.table.groupheader;

import java.util.Enumeration;
import java.util.Vector;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

public class ColumnGroupHeader extends JTableHeader {
  private Vector columns;
  
  public void clear() {
    if(columns != null)
      columns.clear();
  }
  
  public ColumnGroupHeader(JTable table) {
    super(table.getColumnModel());
    setTable(table);
    columns = null;
    setUI(new ColumnGroupHeaderUI());
  }
  
  public void addColumnGroup(final ColumnGroup columnGroup) {
    if(columns == null) {
      columns = new Vector();
      setReorderingAllowed(false);
    }
    columns.addElement(columnGroup);
  }
  
  public ColumnGroup getColumnGroup(int column) {
    TableColumn tc = getColumnModel().getColumn(column);
    for(Object o:columns) {
      Enumeration em = ((ColumnGroup)o).getColumns();
      while(em.hasMoreElements())
        if(em.nextElement().equals(tc))
          return (ColumnGroup)o;
    }
    return null;
  }
  
  final Enumeration getColumnGroups(final TableColumn tableColumn) {
    if (columns != null) {
      for (final Enumeration enumeration = columns.elements(); enumeration.hasMoreElements();) {
        final ColumnGroup columngroup = (ColumnGroup) enumeration.nextElement();
        final Vector vector = columngroup.getColumnGroups(tableColumn, new Vector());
        if (vector != null) {
          return vector.elements();
        }
      }
    }
    return null;
  }
}