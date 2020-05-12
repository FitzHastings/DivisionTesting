package division.plugin;

import bum.editors.DivisionDesktop;
import division.ClientMainListener;

public interface DivisionPlugin extends ClientMainListener {
  
  public default String getName() {
    return getClass().getSimpleName();
  }
  
  public DivisionDesktop getDesktop();
  public void setDesktop(DivisionDesktop desktop);
  
  public default void start() {
  }
}