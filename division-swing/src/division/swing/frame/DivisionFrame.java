package division.swing.frame;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class DivisionFrame extends JFrame {
  private JLabel titleLabel = new JLabel();
  private JPanel container;

  public DivisionFrame(boolean defaultWindow) {
    //setUndecorated(true);
    //setDefaultLookAndFeelDecorated(defaultWindow);
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        try {
          updateShape();
        }catch(Exception ex) {
          ex.printStackTrace();
        }
      }
    });

    if(isDefaultLookAndFeelDecorated()) {
      addHeader();
      addMouseMotionListener(new MouseMotionListener() {
        @Override
        public void mouseDragged(MouseEvent e) {
          int x = getLocationOnScreen().x,
                  y = getLocationOnScreen().y,
                  width = e.getX(),
                  height = e.getY();
          setBounds(x, y, width, height);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
          int cursor = Cursor.DEFAULT_CURSOR;
          Rectangle SW = new Rectangle(0, getHeight()-10, 10, 10);
          Rectangle SE = new Rectangle(getWidth()-10, getHeight()-10, 10, 10);
          Rectangle NW = new Rectangle(0, 0, 10, 10);
          Rectangle NE = new Rectangle(getWidth()-10, 0, 10, 10);
          if(SW.contains(e.getPoint()))
            cursor = Cursor.SW_RESIZE_CURSOR;
          if(SE.contains(e.getPoint()))
            cursor = Cursor.SE_RESIZE_CURSOR;
          if(NW.contains(e.getPoint()))
            cursor = Cursor.NW_RESIZE_CURSOR;
          if(NE.contains(e.getPoint()))
            cursor = Cursor.NE_RESIZE_CURSOR;
          setCursor(Cursor.getPredefinedCursor(cursor));
        }
      });
    }
  }

  protected void updateShape() throws Exception {
    //if(getShape() != null)
      //throw new Exception("Переопределите метод updateShape");
  }

  private void addHeader() {
    container = new JPanel();
    titleLabel = new JLabel(getTitle());
    titleLabel.setForeground(Color.white);

    VisualDragHandle dragHandle = new VisualDragHandle(this);
    dragHandle.setBackground(Color.GRAY);
    dragHandle.setLayout(new GridBagLayout());
    
    dragHandle.add(titleLabel,            new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
    dragHandle.add(getIconifyComponent(), new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
    dragHandle.add(getCloseComponent(),   new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));

    super.getContentPane().setLayout(new GridBagLayout());
    super.getContentPane().add(dragHandle, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    super.getContentPane().add(container, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(2, 4, 4, 4), 0, 0));
  }

  public JComponent getCloseComponent() {
    final JLabel closeLabel = new JLabel("<html><b> x </b></html>");
    closeLabel.setMinimumSize(new Dimension(20, 20));
    closeLabel.setBorder(BorderFactory.createLineBorder(Color.GREEN));
    closeLabel.setForeground(Color.green);
    closeLabel.addMouseMotionListener(new MouseAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        closeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      }
    });
    closeLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        WindowEvent wev = new WindowEvent(DivisionFrame.this, WindowEvent.WINDOW_CLOSING);
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
        setVisible(false);
      }
    });
    return closeLabel;
  }

  public JComponent getIconifyComponent() {
    final JLabel iconLabel = new JLabel("<html>(<b>свернуть</b>)</html>");
    iconLabel.setForeground(Color.green);
    iconLabel.addMouseMotionListener(new MouseAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        iconLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      }
    });
    iconLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        setState(Frame.ICONIFIED);
      }
    });
    return iconLabel;
  }

  @Override
  public void setTitle(String title) {
    super.setTitle(title);
    titleLabel.setText(title);
  }

  @Override
  public Component add(Component comp) {
    if(container != null)
      return container.add(comp);
    return super.add(comp);
  }

  @Override
  public void add(Component comp, Object constraints) {
    if(container != null)
      container.add(comp, constraints);
    else super.add(comp, constraints);
  }

  @Override
  public Component add(String name, Component comp) {
    if(container != null)
      return container.add(name, comp);
    return super.add(name, comp);
  }

  @Override
  public Component add(Component comp, int index) {
    if(container != null)
      return container.add(comp, index);
    return super.add(comp, index);
  }

  @Override
  public void setLayout(LayoutManager manager) {
    if(container != null)
      container.setLayout(manager);
    else super.setLayout(manager);
  }

  @Override
  public void remove(Component comp) {
    if(container != null)
      container.remove(comp);
    else super.remove(comp);
  }

  @Override
  public void remove(int index) {
    if(container != null)
       container.remove(index);
    else super.remove(index);
  }
}