package division.swing.frame;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.geom.RoundRectangle2D;

public class RoundRectangleFrame extends DivisionFrame {
  private int arcw = 20;
  private int arch = 20;

  private Color borderColor = Color.BLACK;
  private int   borderWidth = 1;


  public RoundRectangleFrame(boolean defaultWindow) throws HeadlessException {
    super(defaultWindow);
  }

  @Override
  protected void updateShape() {
    //setShape(new RoundRectangle2D.Double(0d, 0d, getWidth(), getHeight(), getArcw(), getArch()));
  }

  public int getArch() {
    return arch;
  }

  public void setArch(int arch) {
    this.arch = arch;
  }

  public int getArcw() {
    return arcw;
  }

  public void setArcw(int arcw) {
    this.arcw = arcw;
  }

  public Color getBorderColor() {
    return borderColor;
  }

  public void setBorderColor(Color borderColor) {
    this.borderColor = borderColor;
  }

  public int getBorderWidth() {
    return borderWidth;
  }

  public void setBorderWidth(int borderWidth) {
    this.borderWidth = borderWidth;
  }

  @Override
  public void paint(Graphics g) {
    super.paint(g);
    ((Graphics2D)g).setStroke(new BasicStroke(getBorderWidth()));
    ((Graphics2D)g).setColor(getBorderColor());
    ((Graphics2D)g).draw(new RoundRectangle2D.Double(1d, 1d, getWidth()-2, getHeight()-2, getArcw(), getArch()));
  }
}