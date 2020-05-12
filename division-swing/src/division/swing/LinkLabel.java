package division.swing;

import division.swing.actions.LinkBorderActionEvent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;

public class LinkLabel extends JLabel {
  private final List<ActionListener> listeners = new ArrayList<>();
  private Color linkColor  = Color.BLUE;
  private Color hoverColor = Color.RED;
  private boolean linkBorder = true;
  private boolean selected   = false;

  public LinkLabel() {
    init();
  }

  public boolean isSelected() {
    return selected;
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
    if(isSelected()) {
      setBorder(BorderFactory.createLineBorder(linkColor));
    }else setBorder(BorderFactory.createEmptyBorder());
  }

  public LinkLabel(Icon image, int horizontalAlignment) {
    super(image, horizontalAlignment);
    init();
  }

  public LinkLabel(String text) {
    super(text);
    init();
  }

  public LinkLabel(String text, Icon icon, int horizontalAlignment) {
    super(text, icon, horizontalAlignment);
    init();
  }

  public boolean isLinkBorder() {
    return linkBorder;
  }

  public void setLinkBorder(boolean isLinkBorder) {
    this.linkBorder = isLinkBorder;
    if(!isLinkBorder())
      setCursor(Cursor.getDefaultCursor());
    else setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    repaint();
  }

  /*@Override
  public void setText(String text) {
    super.setText("<html><a href=#>"+text+"</a></html>");
  }*/

  public Color getHoverColor() {
    return hoverColor;
  }

  public void setHoverColor(Color hoverColor) {
    this.hoverColor = hoverColor;
  }

  public Color getLinkColor() {
    return linkColor;
  }


  public void setLinkColor(Color linkColor) {
    this.linkColor = linkColor;
    setForeground(linkColor);
  }

  public void addActionListener(ActionListener listener) {
    if(!listeners.contains(listener))
      listeners.add(listener);
  }

  public void removeActionListener(ActionListener listener) {
    listeners.remove(listener);
  }

  private void fireActionListeners(MouseEvent me) {
    if(isEnabled())
      for(ActionListener listener:listeners)
        listener.actionPerformed(new LinkBorderActionEvent(this, me, this.getBounds()));
  }

  private void init() {
    setForeground(getLinkColor());
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        if(isLinkBorder() && e.getClickCount() == 1 && e.getModifiers() != MouseEvent.META_MASK)
          fireActionListeners(e);
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        if(isLinkBorder())
          setForeground(getHoverColor());
      }

      @Override
      public void mouseExited(MouseEvent e) {
        if(isLinkBorder())
          setForeground(getLinkColor());
      }
    });
  }
}