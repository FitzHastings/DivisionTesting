package division.swing.list;

import java.util.Vector;
import javax.swing.AbstractListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class RowHeaderModel extends AbstractListModel {
  private Vector<Object> headers = new Vector<Object>();
  private JTable table;
  private JList list;

  public RowHeaderModel(final JList list, JTable table) {
    this.list = list;
    this.table = table;
    table.getModel().addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(TableModelEvent e) {
        /*int frow = e.getFirstRow();
        if(e.getType() == TableModelEvent.INSERT)
          insertColumn(frow,""+(++frow));
        if(e.getType() == TableModelEvent.DELETE)
          removeColumn(frow);
        if(e.getType() == TableModelEvent.UPDATE)*/
          list.repaint();
      }
    });

    if(table.getRowSorter() != null) {
      table.getRowSorter().addRowSorterListener(new RowSorterListener() {
        @Override
        public void sorterChanged(RowSorterEvent e) {
          list.repaint();
        }
      });
    }
  }

  public JList getList() {
    return list;
  }

  public JTable getTable() {
    return table;
  }

  public int getMaxWidth() {
    int w = 0;
    if(headers.isEmpty()) {
      if(table.getTableHeader() != null)
        w = table.getTableHeader().getFontMetrics(table.getTableHeader().getFont()).stringWidth(table.getRowCount()+"");
      else w = table.getFontMetrics(table.getFont()).stringWidth(table.getRowCount()+"");
    }
    for(Object h:headers) {
      int hw;
      if(table.getTableHeader() != null) {
        if(h instanceof String)
          hw = table.getTableHeader().getFontMetrics(table.getTableHeader().getFont()).stringWidth(h.toString());
        else hw = ((JComponent)h).getPreferredSize().width;
      }else {
        if(h instanceof String)
          hw = table.getFontMetrics(table.getFont()).stringWidth(h.toString());
        else hw = ((JComponent)h).getPreferredSize().width;
      }
      if(hw > w)
        w = hw;
    }
    return w+20;
  }

  @Override
  public int getSize() {
    return headers.isEmpty()||headers.size()<table.getRowCount()?table.getRowCount():headers.size();
  }

  @Override
  public Object getElementAt(int index) {
    return (headers.isEmpty() || headers.size() <= index || headers.get(index)==null)?""+(index+1):headers.get(index);
  }

  public void addColumn(Object columnName) {
    headers.add(columnName);
    list.repaint();
  }

  public void insertColumn(int index, String s) {
    headers.insertElementAt(s, index);
    for(int i=index+1;i<headers.size();i++)
      headers.setElementAt(headers.get(i), i);
    list.repaint();
  }

  public void removeColumn(int index) {
    headers.remove(index);
    for(int i=index;i<headers.size();i++)
      if(headers.get(i) instanceof String)
        headers.setElementAt(""+(Integer.valueOf(headers.get(i).toString())-1), i);
    list.repaint();
  }

  public void clear() {
    headers.clear();
    list.repaint();
  }
}