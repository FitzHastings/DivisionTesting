package division.swing.actions;

import java.util.LinkedList;
import java.util.TreeMap;

public class DivisionListenerList extends TreeMap<String, LinkedList<DivisionListener>> {
  private boolean fireEventActive = true;

  public boolean isFireEventActive() {
    return fireEventActive;
  }

  public void setFireEventActive(boolean fireEventActive) {
    this.fireEventActive = fireEventActive;
  }

  public void addAll(String command, LinkedList<DivisionListener> divisionListeners) {
    LinkedList<DivisionListener> list = get(command);
    if(list == null) {
      list = new LinkedList<DivisionListener>();
      put(command, list);
    }
    list.addAll(divisionListeners);
  }

  public void add(String command, DivisionListener divisionListener) {
    LinkedList<DivisionListener> list = get(command);
    if(list == null) {
      list = new LinkedList<DivisionListener>();
      put(command, list);
    }
    list.add(divisionListener);
  }

  public void fire(DivisionEvent divisionEvent) {
    if(isFireEventActive()) {
      LinkedList<DivisionListener> list = get(divisionEvent.getCommand());
      if(list != null)
        for(DivisionListener divisionListener:list)
          divisionListener.divisionActionPerformed(divisionEvent);
    }
  }
}