package division.fx.editor;

import javafx.collections.ObservableList;

@Deprecated
public interface Storable {
  public ObservableList storeControls();
  public default void store() {
    store(storeFileName());
  }
  
  public default void store(String fileName) {
    GLoader.store(fileName == null ? storeFileName() : fileName, storeControls());
  }
  
  public default String storeFileName() {
    return getClass().getName().replaceAll("\\$", "");
  }
  
  public default void load() {
    load(storeFileName());
  }
  
  public default void load(String fileName) {
    GLoader.load(fileName == null ? storeFileName() : fileName, storeControls());
  }
}