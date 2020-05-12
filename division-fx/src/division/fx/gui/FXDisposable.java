package division.fx.gui;

import java.util.List;
import javafx.collections.FXCollections;

public interface FXDisposable {
  
  public default void fireDispose() {
    List<FXDisposable> list = disposeList();
    while(!list.isEmpty()) {
      FXDisposable d = list.remove(0);
      if(d != null)
        d.dispose();
    }
  }
  
  public default void dispose() {
    System.out.println("DISPOSE "+getClass().getSimpleName());
    fireDispose();
    finaly();
  }
  
  public default List<FXDisposable> disposeList() {
    return FXCollections.observableArrayList();
  }
  
  public void finaly();
}