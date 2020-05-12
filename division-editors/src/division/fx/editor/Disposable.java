package division.fx.editor;

import java.util.List;

@Deprecated
public interface Disposable {
  
  public default void fireDispose() {
    List<Disposable> list = disposeList();
    while(!list.isEmpty())
      list.remove(0).dispose();
  }
  
  public default void dispose() {
    System.out.println("DISPOSE "+getClass().getSimpleName());
    fireDispose();
    finaly();
  }
  
  public List<Disposable> disposeList();
  
  public void finaly();
}