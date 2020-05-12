package division.swing;

import division.swing.list.RowHeaderList;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 * Отличается от родителя тем, что установлен цвет фона как фон элемента в 
 *    нём и PreferedSize 100x100
 */
public class DivisionScrollPane extends JScrollPane {
  private final ScrollMouseAdapter scrollMouseAdapter = new ScrollMouseAdapter();

  public DivisionScrollPane(JComponent component) {
    this();
    getViewport().setView(component);
    getViewport().setBackground(component.getBackground());
  }
  
  public DivisionScrollPane() {
    setPreferredSize(new Dimension(100,50));
    setMinimumSize(new Dimension(0,0));
    
    addMouseMotionListener(scrollMouseAdapter);
    addMouseListener(scrollMouseAdapter);
  }

  public boolean isRowHeader() {
    return getRowHeader() != null && getRowHeader().getView() != null;
  }

  public void setRowHeader(boolean rhv) {
    if(getViewport().getView() instanceof JTable && rhv) {
      RowHeaderList rowHeaderList = new RowHeaderList(this, (JTable) getViewport().getView());
      rowHeaderList.setBackground(getBackground());
    }else {
      setRowHeaderView(null);
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    getViewport().setBackground(enabled?Color.WHITE:Color.lightGray);
    super.setEnabled(enabled);
  }
  
  
  
  public void clickTopLeft(Point point) {
    if(getViewport().getView() instanceof JTable) {
      if(((JTable)getViewport().getView()).getSelectedRowCount() == ((JTable)getViewport().getView()).getRowCount())
        ((JTable)getViewport().getView()).clearSelection();
      else ((JTable)getViewport().getView()).setRowSelectionInterval(0, ((JTable)getViewport().getView()).getRowCount()-1);
    }
  }
  
  public void enterTopLeft(Point point) {
  }
  
  public void exitTopLeft(Point point) {
  }
  
  public void moveLeftTop(Point point) {
  }

  public void pressTopLeft(Point point) {
  }

  public void releasTopLeft(Point point) {
  }
  
  class ScrollMouseAdapter extends MouseAdapter {
    private boolean contains(Point point) {
      if(getRowHeader() != null && getColumnHeader() != null)
        return new Rectangle(0, 0, getRowHeader().getWidth(), getColumnHeader().getHeight()).contains(point);
      return false;
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
      if(contains(e.getPoint()))
        clickTopLeft(e.getPoint());
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
      if(contains(e.getPoint()))
        enterTopLeft(e.getPoint());
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
      if(contains(e.getPoint()))
        exitTopLeft(e.getPoint());
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      if(contains(e.getPoint()))
        moveLeftTop(e.getPoint());
    }

    @Override
    public void mousePressed(MouseEvent e) {
      if(contains(e.getPoint()))
        pressTopLeft(e.getPoint());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if(contains(e.getPoint()))
        releasTopLeft(e.getPoint());
    }
  }
}