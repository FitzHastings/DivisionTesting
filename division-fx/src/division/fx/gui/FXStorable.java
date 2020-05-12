package division.fx.gui;

import java.util.List;
import javafx.scene.Node;

public interface FXStorable {
  
  public List<Node> storeControls();
  
  public default void store() {
    store(storeFileName());
  }
  
  public default void store(String fileName) {
    FXGLoader.store(fileName == null ? storeFileName() : fileName, storeControls());
  }
  
  public default String storeFileName() {
    return getClass().getName().replaceAll("\\$", "");
  }
  
  public default void load() {
    load(storeFileName());
  }
  
  public default void load(String fileName) {
    FXGLoader.load(fileName == null ? storeFileName() : fileName, storeControls());
  }
}