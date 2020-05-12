package division.Desktop;

import division.util.FileLoader;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;

public class DesktopLabel extends JLabel implements MouseListener, MouseMotionListener {
  private final List<ActionListener> actionListeners = new ArrayList<>();
  
  public DesktopLabel(String name, Icon image, String text, ActionListener actionListener) {
    super();
    setName(name);
    
    if(image != null)
      setIcon(image);
    setText(text);
    
    setFont(new Font("Verdana", Font.BOLD, 16));
    
    int width = getText()==null||getText().equals("")?0:(getFontMetrics(getFont()).stringWidth(text)+10);
    setSize(getIcon().getIconWidth()+(width<100?width:100), getIcon().getIconHeight());
    
    addMouseListener(this);
    addActionListener(actionListener);
    MouseDragger.makeDraggable(this);
  }
  
  public void load() {
    try {
      Properties prop = FileLoader.loadProperties("conf"+File.separator+"DesktopLabels");
      String point = prop.getProperty(getName());
      setLocation(Integer.parseInt(point.split(",")[0]), Integer.parseInt(point.split(",")[1]));
    }catch(Exception ex) {}
  }
  
  public void store() {
    try {
      Properties prop = FileLoader.loadProperties("conf"+File.separator+"DesktopLabels");
      prop.setProperty(getName(), getLocation().x+","+getLocation().y);
      FileLoader.storeProperties("conf"+File.separator+"DesktopLabels", prop);
    }catch(Exception ex) {}
  }
  
  public void addActionListener(ActionListener listener) {
    actionListeners.add(listener);
  }
  
  public void removeActionListener(ActionListener listener) {
    actionListeners.remove(listener);
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if(e.getModifiers() != MouseEvent.META_MASK && e.getClickCount() == 2)
      fireActionParformed();
  }

  @Override
  public void mousePressed(MouseEvent e) {
    setBorder(BorderFactory.createLineBorder(Color.BLACK));
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    setBorder(BorderFactory.createLineBorder(getParent().getBackground()));
  }

  @Override
  public void mouseEntered(MouseEvent e) {
  }

  @Override
  public void mouseExited(MouseEvent e) {
  }

  private void fireActionParformed() {
    for(ActionListener listener:actionListeners)
      listener.actionPerformed(new ActionEvent(this, 0, "click"));
  }

  @Override
  public void mouseDragged(MouseEvent e) {
  }

  @Override
  public void mouseMoved(MouseEvent e) {
  }
}