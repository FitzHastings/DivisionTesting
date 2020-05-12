package bum.editors.actions;

import bum.editors.EditorListener;
import bum.editors.MainObjectEditor;

/**
 *
 * @author Antosha
 */
public interface TableEditorActionGenerator {
  /**
   * Извещает слушателей о выборе объектов
   */
  public void fireSelectObjects();
  /**
   * Извещает слушателей об открытии редактора
   */
  public void fireOpenObjectEditor(MainObjectEditor editor);
  /**
   * Добавляет слушателя событий
   * @param listener слушатель событий
   */
  public void addDBTableEditorListener(EditorListener listener);
  /**
   * Удаляет слушателя событий
   * @param listener слушатель событий
   */
  public void removeDBTableEditorListener(EditorListener listener);
  /**
   * Удаляет всех слушателей событий
   */
  public void removeAllDBTableEditorListener();
}