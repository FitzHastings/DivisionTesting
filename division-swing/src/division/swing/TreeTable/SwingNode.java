package division.swing.TreeTable;

import java.awt.Color;
import java.awt.Font;
import javax.swing.tree.DefaultMutableTreeNode;

public class SwingNode extends DefaultMutableTreeNode implements SwingObject {
  private Font font;
  private Color foreground;
  private Color background;

  public SwingNode(Object userObject, boolean allowsChildren) {
    super(userObject, allowsChildren);
  }

  public SwingNode(Object userObject) {
    super(userObject);
  }

  public SwingNode() {
    super();
  }

  public void setBackground(Color background) {
    this.background = background;
  }

  public void setFont(Font font) {
    this.font = font;
  }

  public void setForeground(Color foreground) {
    this.foreground = foreground;
  }

  @Override
  public Font getFont() {
    return font;
  }

  @Override
  public Color getForeground() {
    return foreground;
  }

  @Override
  public Color getBackground() {
    return background;
  }
}