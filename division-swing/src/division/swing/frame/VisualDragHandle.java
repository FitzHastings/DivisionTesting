package division.swing.frame;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;

public class VisualDragHandle extends JPanel {
  private Point draggingAnchor = null;

  public VisualDragHandle(final Window target) {
    super();
    this.addMouseMotionListener(new MouseAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        draggingAnchor = new Point(e.getX() + getX(), e.getY() + getY());
      }

      @Override
      public void mouseDragged(MouseEvent e) {
        target.setLocation(e.getLocationOnScreen().x - draggingAnchor.x, e.getLocationOnScreen().y - draggingAnchor.y);
      }
    });
  }
}