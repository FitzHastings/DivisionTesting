package bum.editors.actions;

import bum.editors.InitDataListener;

/**
 *
 * @author Antosha
 */
public interface InitDataActionGenerator {
  /**
   * Извещает слушателей об окончании загрузки объектов
   */
  public void fireInitDataComplited();
  /**
   * Добавляет слушателя событий
   * @param initDataListener слушатель событий
   */
  public void addInitDataListener(InitDataListener initDataListener);
  /**
   * Удаляет слушателя событий
   * @param initDataListener слушатель событий
   */
  public void removeInitDataListener(InitDataListener initDataListener);
}