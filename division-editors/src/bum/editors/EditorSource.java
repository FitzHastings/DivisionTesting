package bum.editors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import mapping.MappingObject;

public interface EditorSource {
  public ExecutorService pool = Executors.newFixedThreadPool(10);
  
  static final ArrayList<EditorListener> staticTableEditorListeners = new ArrayList<>();
  
  public default boolean isActive() {
    return true;
  }
  
  public default void setActive(boolean a) {
  }
  
  public List<EditorListener> getEditorListeners();
  
  public default void addEditorListener(EditorListener liistener) {
    getEditorListeners().add(liistener);
  }
  
  public default void removeEditorListener(EditorListener liistener) {
    getEditorListeners().remove(liistener);
  }
  
  public default void fireChangeSelection(EditorGui editor, Integer[] ids) {
    if(isActive()) {
      getEditorListeners().stream().forEach(l -> l.changeSelection(editor, ids));
      staticTableEditorListeners.stream().forEach(l -> l.changeSelection(editor, ids));
    }
  }
  
  public default void fireChangeSelection(Class<? extends MappingObject> objectClass, Integer[] ids) {
    if(isActive()) {
      getEditorListeners().stream().forEach(l -> l.changeSelection(objectClass, ids));
      staticTableEditorListeners.stream().forEach(l -> l.changeSelection(objectClass, ids));
    }
  }
  
  public default void fireChangeSelection(EditorGui editor, Object... params) {
    if(isActive()) {
      getEditorListeners().stream().forEach(l -> l.changeSelection(editor, params));
      staticTableEditorListeners.stream().forEach(l -> l.changeSelection(editor, params));
    }
  }
  
  public default void fireSelectObjects(EditorGui editor, Integer[] ids) {
    if(isActive())
      getEditorListeners().stream().forEach(l -> l.selectObjects(editor, ids));
  }
  
  public default void fireOpenObjectEditor(EditorGui editor) {
    if(isActive())
      getEditorListeners().stream().forEach(l -> l.openObjectEditor(editor));
  }
  
  public default void fireInitDataComplited(EditorGui editor) {
    if(isActive())
      getEditorListeners().stream().forEach(l -> l.initDataComplited(editor));
  }
}