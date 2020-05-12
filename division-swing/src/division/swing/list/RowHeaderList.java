package division.swing.list;

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class RowHeaderList extends JList implements MouseListener {
  private RowHeaderModel model;
  public RowHeaderList(final JScrollPane scroll, final JTable table) {
    model = new RowHeaderModel(this,table);
    setModel(model);
    setBackground(table.getBackground());
    setCellRenderer(new RowHeaderCellRenderer(this,table));
    scroll.setRowHeaderView(this);

    int h = 16;
    if(table.getRowCount() > 0)
      h = table.getCellRect(0, 0, true).height;
    else h = table.getRowHeight();
    setFixedCellHeight(h);
    setFixedCellWidth(((RowHeaderModel)getModel()).getMaxWidth());

    scroll.getRowHeader().addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        //scroll.getViewport().setViewPosition(scroll.getRowHeader().getViewPosition());
      }
    });

    scroll.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {
      @Override
      public void adjustmentValueChanged(AdjustmentEvent e) {
        /*if(table != null && table.getTableHeader() != null) {
          table.getTableHeader().resizeAndRepaint();
        }*/
      }
    });

    scroll.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
      @Override
      public void adjustmentValueChanged(AdjustmentEvent e) {
        /*if(table != null && table.getTableHeader() != null) {
          table.getTableHeader().resizeAndRepaint();
        }*/
      }
    });
    
    addMouseListener(this);
  }

  @Override
  public void repaint() {
    if(model != null)
      setFixedCellWidth(model.getMaxWidth());
    super.repaint();
  }

  @Override
  public void mouseClicked(MouseEvent e) {
  }

  @Override
  public void mousePressed(MouseEvent e) {
    if(e.getModifiers() == MouseEvent.META_MASK)
      setSelectedIndex(model.getList().locationToIndex(e.getPoint()));
  }

  @Override
  public void mouseReleased(MouseEvent e) {
  }

  @Override
  public void mouseEntered(MouseEvent e) {
  }

  @Override
  public void mouseExited(MouseEvent e) {
  }
}