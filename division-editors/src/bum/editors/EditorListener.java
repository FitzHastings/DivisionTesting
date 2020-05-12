package bum.editors;

import mapping.MappingObject;

public interface EditorListener {
  
  public default void selectObjects(EditorGui editor, Integer[] objects) {
  }
  
  public default void openObjectEditor(EditorGui editor) {
  }
  
  public default void initDataComplited(EditorGui editor) {
  }
  
  public default void changeSelection(EditorGui editor, Integer[] ids) {
  }
  
  public default void changeSelection(Class<? extends MappingObject> objectClass, Integer[] ids) {
  }
  
  public default void changeSelection(EditorGui editor, Object... params) {
  }
}