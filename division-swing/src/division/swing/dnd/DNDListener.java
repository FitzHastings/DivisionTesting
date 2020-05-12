package division.swing.dnd;

import java.awt.Point;
import java.util.List;
import mapping.MappingObject;

public interface DNDListener {
  public void dragOver(Point point, List<MappingObject> objects, Class<? extends MappingObject> interfaceClass);
  public void drop(Point point, List<MappingObject> objects, Class<? extends MappingObject> interfaceClass);
}
