package division.swing.table.groupheader;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class ColumnGroupHeaderUI extends BasicTableHeaderUI {
  private static final long MAX_WIDTH = Integer.MAX_VALUE; //0x7fffffffL

  public ColumnGroupHeaderUI() {
  }

  @Override
  public final void paint(final Graphics graphics, final JComponent component) {
    if(super.header.getColumnModel() == null)
      return;

    final Rectangle rectangle = graphics.getClipBounds();
    int columnIndex = 0;
    final Dimension dimension = super.header.getSize();
    final Rectangle rectangle1 = new Rectangle(0, 0, dimension.width, dimension.height);
    final Hashtable hashtable = new Hashtable();
    int margin = super.header.getColumnModel().getColumnMargin();
    for(final Enumeration enumeration = super.header.getColumnModel().getColumns(); enumeration.hasMoreElements();) {
      rectangle1.height = dimension.height;
      rectangle1.y = 0;
      final TableColumn tablecolumn = (TableColumn) enumeration.nextElement();
      final Enumeration enumeration1 = ((ColumnGroupHeader) super.header).getColumnGroups(tablecolumn);
      if(enumeration1 != null) {
        int height = 0;
        while (enumeration1.hasMoreElements()) {
          final ColumnGroup columngroup = (ColumnGroup) enumeration1.nextElement();
          Rectangle rectangle2 = (Rectangle) hashtable.get(columngroup);
          if(rectangle2 == null) {
              rectangle2 = new Rectangle(rectangle1);
              final Dimension dimension1 = columngroup.getSize(super.header.getTable());
              rectangle2.width = dimension1.width;
              //rectangle2.width -= margin;
              rectangle2.height = dimension1.height;
              hashtable.put(columngroup, rectangle2);
          }
          paintCell(graphics, rectangle2, columngroup);
          height += rectangle2.height;
          rectangle1.height = dimension.height - height;
          rectangle1.y = height;
        }
      }
      rectangle1.width = tablecolumn.getWidth();// - margin;
      if(rectangle1.intersects(rectangle)) {
        paintCell(graphics, rectangle1, columnIndex);
      }
      rectangle1.x += tablecolumn.getWidth();
      columnIndex++;
    }
  }

  private void paintCell(final Graphics graphics, final Rectangle rect, final int columnIndex) {
    final TableColumn tableColumn = super.header.getColumnModel().getColumn(columnIndex);
    Object obj = tableColumn.getHeaderRenderer();
    if(obj == null)
      obj = new DefaultColumnGroupHeaderRenderer();

    final TableCellRenderer renderer = (TableCellRenderer) obj;
    final JTable table = super.header.getTable();
    final Object name = tableColumn.getHeaderValue();
    final Component component = renderer.getTableCellRendererComponent(table, name, false, false, -1, columnIndex);
    super.rendererPane.add(component);
    super.rendererPane.paintComponent(graphics, component, super.header, rect.x, rect.y, rect.width, rect.height, true);
  }

  private void paintCell(final Graphics graphics, final Rectangle rect, final ColumnGroup columnGroup) {
    final TableCellRenderer renderer = columnGroup.getHeaderRenderer();
    final JTable table = super.header.getTable();
    final Object name = columnGroup.getHeaderValue();
    final Component component = renderer.getTableCellRendererComponent(table, name, false, false, -1, -1);
    super.rendererPane.add(component);
    super.rendererPane.paintComponent(graphics, component, super.header, rect.x, rect.y, rect.width, rect.height, true);
  }

  @Override
  public final Dimension getPreferredSize(final JComponent component) {
    long width = 0L;
    for (final Enumeration enumeration = super.header.getColumnModel().getColumns(); enumeration.hasMoreElements();) {
        final TableColumn tablecolumn = (TableColumn) enumeration.nextElement();
        width += tablecolumn.getPreferredWidth();
    }
    final TableColumnModel tableColumnModel = super.header.getColumnModel();
    final long size = width + tableColumnModel.getColumnMargin() * tableColumnModel.getColumnCount();
    return new Dimension((int) (size > MAX_WIDTH ? MAX_WIDTH : size), getHeaderHeight());
  }

  private int getHeaderHeight() {
        int headerHeight = 0;
        TableColumnModel columnModel = super.header.getColumnModel();
        for (int column = 0; column < columnModel.getColumnCount(); column++) {
            TableColumn aColumn = columnModel.getColumn(column);
            TableCellRenderer renderer = aColumn.getHeaderRenderer();
            if (renderer == null) {
                renderer = super.header.getDefaultRenderer();
            }

            Component comp = renderer.getTableCellRendererComponent(super.header.getTable(), aColumn.getHeaderValue(), false,
                    false, -1, column);
            int cHeight = comp.getPreferredSize().height;
            Enumeration en = ((ColumnGroupHeader)super.header).getColumnGroups(aColumn);
            if(en != null) {
              while(en.hasMoreElements()) {
                cHeight += ((ColumnGroup)en.nextElement()).getSize(super.header.getTable()).height;
              }
            }
            headerHeight = Math.max(headerHeight, cHeight);
        }
        return headerHeight;
    }
}
