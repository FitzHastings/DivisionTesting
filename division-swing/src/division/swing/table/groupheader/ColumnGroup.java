package division.swing.table.groupheader;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public final class ColumnGroup {
  private TableCellRenderer renderer = new DefaultColumnGroupHeaderRenderer();
  private final Vector columns;
  private final String name;

  public ColumnGroup(final String theName) {
    columns = new Vector();
    name = theName;
  }

  public void add(final TableColumn tableColumn) {
    if (tableColumn != null) {
      columns.addElement(tableColumn);
    }
  }

  public void add(final ColumnGroup columnGroup) {
    if (columnGroup != null) {
      columns.addElement(columnGroup);
    }
  }

  public Enumeration getColumns() {
    return columns.elements();
  }


  Vector getColumnGroups(final TableColumn tableColumn, final Vector vector) {
    vector.addElement(this);
    if (columns.contains(tableColumn))
      return vector;
    for (final Enumeration enumeration = columns.elements(); enumeration.hasMoreElements();) {
      final Object obj = enumeration.nextElement();
      if (obj instanceof ColumnGroup) {
        final Vector vector1 = ((ColumnGroup) obj).getColumnGroups(tableColumn, (Vector) vector.clone());
        if (vector1 != null) {
          return vector1;
        }
      }
    }
    return null;
  }

  public void setHeaderRenderer(TableCellRenderer renderer) {
    this.renderer = renderer;
  }

  public TableCellRenderer getHeaderRenderer() {
    return renderer;
  }

  public Object getHeaderValue() {
    return name;
  }

  public Dimension getSize(final JTable table) {
    final Component component = getHeaderRenderer().getTableCellRendererComponent(table, getHeaderValue(), false, false, -1, -1);
    final int height = component.getPreferredSize().height;
    int width = 0;
    for (final Enumeration enumeration = columns.elements(); enumeration.hasMoreElements();) {
      final Object obj = enumeration.nextElement();
      if (obj instanceof TableColumn) {
        final TableColumn tablecolumn = (TableColumn) obj;
        width += tablecolumn.getWidth();
      } else {
        width += ((ColumnGroup) obj).getSize(table).width;
      }
    }
    return new Dimension(width, height);
  }
}