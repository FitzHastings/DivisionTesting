/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package bum.editors.actions;

import bum.util.FilterProcessingListener;

/**
 *
 * @author Antosha
 */
public interface FilterProcessingActionGenerator {
  /**
   * Добавляет слушателя событий
   * @param listener слушатель событий
   */
  public void addFilterProcessingListener(FilterProcessingListener listener);
  /**
   * Удаляет слушателя событий
   * @param listener слушатель событий
   */
  public void removeFilterProcessingListener(FilterProcessingListener listener);
  /**
   * Удаляет всех слушателей событий
   */
  public void removeAllFilterProcessingListenerr();
}