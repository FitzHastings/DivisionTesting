package division.border;

import division.swing.actions.LinkBorderActionEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

public class LinkBorder extends TitledBorder implements MouseMotionListener, MouseListener {
  private Component comp;
  private final List<ActionListener> alisteners = new ArrayList<>();
  private int stroke = 1;
  private Color borderColor            = Color.GRAY;
  private Color activelinkTitleColor   = Color.RED;
  private Color deactiveLinkTitleColor = Color.BLUE;
  private Color notLinkColor           = Color.DARK_GRAY;
  private boolean linkBorder = true;
  private Rectangle titleRec;
  
  public LinkBorder(String title) {
    super(title);
    this.setTitleColor(Color.BLUE);
  }

  public Component getComponent() {
    return comp;
  }

  @Override
  public void setTitle(String title) {
    super.setTitle(title);
    repaint();
  }

  public void repaint() {
    if(comp != null)
      comp.repaint();
  }

  public Color getNotLinkColor() {
    return notLinkColor;
  }

  public void setNotLinkColor(Color notLinkColor) {
    this.notLinkColor = notLinkColor;
    repaint();
  }

  public Color getActivelinkTitleColor() {
    return activelinkTitleColor;
  }

  public void setActivelinkTitleColor(Color activelinkTitleColor) {
    this.activelinkTitleColor = activelinkTitleColor;
    repaint();
  }

  public Color getDeactiveLinkTitleColor() {
    return deactiveLinkTitleColor;
  }

  public void setDeactiveLinkTitleColor(Color deactiveLinkTitleColor) {
    this.deactiveLinkTitleColor = deactiveLinkTitleColor;
    repaint();
  }

  public boolean isLinkBorder() {
    return linkBorder;
  }

  public void setLinkBorder(boolean isLinkBorder) {
    this.linkBorder = isLinkBorder;
    repaint();
  }

  public int getStroke() {
    return stroke;
  }

  public void setStroke(int stroke) {
    this.stroke = stroke;
    repaint();
  }

  public Color getBorderColor() {
    return borderColor;
  }

  public void setBorderColor(Color borderColor) {
    this.borderColor = borderColor;
    repaint();
  }
  
  public void addActionListener(ActionListener listener) {
    if(!alisteners.contains(listener))
      alisteners.add(listener);
  }
  
  public void removeActionListener(ActionListener listener) {
    alisteners.remove(listener);
  }
  
  private void fireActionListeners(MouseEvent me, Rectangle titleBounds) {
    for(ActionListener listener:alisteners)
      listener.actionPerformed(new LinkBorderActionEvent(this, me, titleBounds));
  }

  @Override
  public void mouseDragged(MouseEvent e) {
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    if(isLinkBorder() && comp.isEnabled()) {
      if(titleRec.contains(e.getPoint())) {
        comp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setTitleColor(getActivelinkTitleColor());
      }else {
        comp.setCursor(Cursor.getDefaultCursor());
        setTitleColor(getDeactiveLinkTitleColor());
      }
      repaint();
    }
  }

  @Override
  public void mouseClicked(MouseEvent e) {
  }

  @Override
  public void mousePressed(MouseEvent e) {
    if(isLinkBorder())
      if(titleRec.contains(e.getPoint()))
        fireActionListeners(e, titleRec);
  }

  @Override
  public void mouseReleased(MouseEvent e) {
  }

  @Override
  public void mouseEntered(MouseEvent e) {
  }

  @Override
  public void mouseExited(MouseEvent e) {
    if(isLinkBorder() && comp.isEnabled()) {
      comp.setCursor(Cursor.getDefaultCursor());
      setTitleColor(getDeactiveLinkTitleColor());
      repaint();
    }
  }
  
  @Override
  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    Graphics2D g2d = (Graphics2D)g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    
    Font f = getTitleFont();
    if(f == null)
      f = UIManager.getFont("TitledBorder.font");
    g2d.setFont(f);
    FontMetrics fm = g2d.getFontMetrics(f);
    Rectangle2D stringBounds = fm.getStringBounds(getTitle(), g2d);
    
    g2d.setStroke(new BasicStroke(getStroke()));
    
    x = x+getStroke();
    y = y+getStroke();
    width = width-getStroke();
    height = height-getStroke();
    
    int diff = (int)stringBounds.getHeight()/2;
    y = y + diff;
    height = height - 3-diff;
    width = width - 3;
    
    //g2d.setStroke(new BasicStroke(2));
    g2d.setColor(getBorderColor());
    
    g2d.drawLine(x+20, y-diff, x+20, y+diff);
    
    
    g2d.drawLine(x+5, y, 20, y);
    g2d.drawArc(x, y, 10, 10, 90, 90);
    
    g2d.drawLine(x, y+5, x, y+height-5);
    g2d.drawArc(x, y+height-10, 10, 10, 180, 90);
    
    g2d.drawLine(x+5, y+height, x+width-5, y+height);
    g2d.drawArc(x+width-10, y+height-10, 10, 10, 270, 90);
    
    g2d.drawLine(x+width, y+height-5, x+width, y+5);
    g2d.drawArc(x+width-10, y, 10, 10, 360, 90);
    
    g2d.drawLine(x+width-5, y, x+(int)stringBounds.getWidth()+30, y);
    
    //g2d.fillOval(x+(int)stringBounds.getWidth()+30, y-2, 5, 5);
    g2d.drawLine(x+(int)stringBounds.getWidth()+30, y-diff, x+(int)stringBounds.getWidth()+30, y+diff);
    
    g2d.setStroke(new BasicStroke(1));
    
    if(isLinkBorder()) {
      g2d.setColor(getTitleColor());
      g2d.drawLine(x+25, y+diff+2, x+(int)stringBounds.getWidth()+25, y+diff+2);
    }else g2d.setColor(getNotLinkColor());
    
    g2d.drawString(getTitle(), x+25, y+diff-2);
   
    titleRec = new Rectangle(x+25, y-diff, (int)stringBounds.getWidth(), (int)stringBounds.getHeight());
    if(comp == null) {
      comp = c;
      comp.addMouseMotionListener(this);
      comp.addMouseListener(this);
    }
  }
}

/**
 * Проклятой ревности отвратная похлёбка
 * Как кислота сжирает изнутри
 * И жрёт причмокивая громко
 * Всю душу, сердце и мозги
 * Зубами редкими кусая
 * Смеётся и прихлёбывает морс
 * Души остатки запивая
 * Не морща свой поганый нос
 * Боюсь пропасть в её желудке
 * Сгореть от ревности в конец
 * Хоть иногда со мною в будке
 * Скажи что бобик молодец
 */

 /**
 * Что силы есть терплю другого
 * И корчусь и испытываю боль
 * Но выдержать смогу довольно долго
 * Упавшую на рану соль
 * Не знаю что судьба мне напророчит
 * Своей костлявою рукой
 * Но не хочу тебя порочить
 * Перед другим и пред тобой
 */