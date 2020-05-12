package division.swing.table.filter;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class FilterButton extends JButton implements ActionListener {
  private Point center;
  private int[] x,y;
  private int b,t;
  private JPopupMenu menu = new JPopupMenu();
  private FilterComponent component;
  
  private AWTEvent event;

  public FilterButton(FilterComponent component) {
    this.component = component;
    setMinimumSize(new Dimension(component.getTableFilter().getTable().getRowHeight(),component.getTableFilter().getTable().getRowHeight()));
    setPreferredSize(new Dimension(component.getTableFilter().getTable().getRowHeight(),component.getTableFilter().getTable().getRowHeight()));
    addActionListener(this);
    menu.setBorder(BorderFactory.createEmptyBorder());
  }
  
  @Override
  public void actionPerformed(ActionEvent e) {
    if(menu.isVisible())
      menu.setVisible(false);
    else {
      int sxx = 0;
      JComponent comp = component.packComponent();
      int column = component.getColumn();
      column = component.getTableFilter().getTable().convertColumnIndexToView(column);
      Rectangle rec = component.getTableFilter().getTableHeader().getHeaderRect(column);
      if(rec.x != 0 && rec.width != 0 || (event == null || !(event instanceof MouseEvent))) {
        sxx = rec.x+getBounds().x+getBounds().width;
      }else {
        int sx  = ((MouseEvent)event).getXOnScreen();
        int shx = component.getTableFilter().getTableHeader().getLocationOnScreen().x;
        int cx  = ((MouseEvent)event).getX();
        int cbx = getLocation().x;
        int w   = getBounds().width;
        sxx = sx-shx-cx+cbx+w;
        
        if(comp.getPreferredSize().width <= 0) {
          Dimension d = ((Component)event.getSource()).getSize();
          comp.setPreferredSize(new Dimension(d.width-w-2-cbx, comp.getPreferredSize().height<=0?d.height-1:comp.getPreferredSize().height));
        }
      }
      
      menu.removeAll();
      menu.add(comp);
      menu.setPreferredSize(comp.getPreferredSize());
      menu.addPopupMenuListener(new PopupMenuListener() {
        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
          if(!component.isOnlineFilter())
            component.startFilter();
        }
      });
      menu.show(component.getTableFilter().getTableHeader(), sxx, rec.height-getBounds().height-getBounds().y);
      comp.grabFocus();
    }
    repaint();
  }

  public void fire(AWTEvent event) {
    this.event = event;
    fireActionPerformed(new ActionEvent(this, hashCode(), "action"));
  }

  @Override
  public void paint(Graphics g) {
    g.setColor(Color.LIGHT_GRAY);
    //g.setColor(component.isFilter()?Color.RED:Color.LIGHT_GRAY);
    g.drawRoundRect(0, 0, getBounds().width-1, getBounds().height-1, 4, 4);
    b = (getWidth()/20==0?2:getWidth()/20)-1;
    t = getWidth()/3==0?3:getWidth()/3;
    center = new Point(getWidth()/2,getHeight()/2+1);
    x = new int[]{center.x-t,center.x+t,center.x+b,center.x+b,center.x-b,center.x-b};
    y = new int[]{center.y-center.y/2,center.y-center.y/2,center.y,center.y+center.y/2,center.y+center.y/2,center.y};
    //g.setColor(component.isFilter()?new Color(16, 184, 8):Color.GRAY);
    g.setColor(component.isFilter()?Color.RED:Color.GRAY);
    g.fillPolygon(x,y,6);
  }
}