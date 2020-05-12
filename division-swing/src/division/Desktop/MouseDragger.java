package division.Desktop;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;

public class MouseDragger extends MouseAdapter {
  private Point startPoint;
  private Component draggedObject;

  private MouseDragger(Component component) {
    draggedObject = component;
  }

  @Override
  public void mousePressed(MouseEvent e) {
    if(e.getModifiers() != MouseEvent.META_MASK)
      startPoint = e.getPoint();
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    if(e.getModifiers() != MouseEvent.META_MASK) {
      Point location = SwingUtilities.convertPoint(draggedObject, e.getPoint(), draggedObject.getParent());

      location.x -= startPoint.x;
      location.y -= startPoint.y;
      
      if(draggedObject.getParent().contains(location))
        draggedObject.setLocation(location);
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if(e.getModifiers() != MouseEvent.META_MASK)
      startPoint = null;
  }

  public static void makeDraggable(Component component) {
    MouseDragger dragger = new MouseDragger(component);
    component.addMouseListener(dragger);
    component.addMouseMotionListener(dragger);
  }
}