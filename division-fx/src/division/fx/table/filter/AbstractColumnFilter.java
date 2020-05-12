package division.fx.table.filter;

import division.fx.PropertyMap;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;

public abstract class AbstractColumnFilter<S extends PropertyMap> implements ColumnFilter<S> {
  private TableFilter<S> tableFilter;
  private String property;
  private final BooleanProperty active = new SimpleBooleanProperty(false);
  private final TreeMap<FilterListener, List> listeners = new TreeMap<>();

  public AbstractColumnFilter(String property) {
    this.property = property;
    active.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> tableFilter.startFilter());
  }
  
  @Override
  public String getProperty() {
    return property;
  }

  @Override
  public void setProperty(String property) {
    this.property = property;
  }

  @Override
  public BooleanProperty activeProperty() {
    return active;
  }
  
  @Override
  public TableFilter<S> getTableFilter() {
    return tableFilter;
  }

  @Override
  public void setTableFilter(TableFilter<S> tableFilter) {
    this.tableFilter = tableFilter;
    this.tableFilter.disposeList().add(this);
  }
  
  @Override
  public void removeFilterListener(FilterListener handler) {
    if(listeners.containsKey(handler))
      listeners.get(handler).stream().forEach(l -> {
        removeListener(l);
      });
  }
  
  protected void putListener(FilterListener handler, Object Listener) {
    if(!listeners.containsKey(handler))
      listeners.put(handler, new ArrayList());
    listeners.get(handler).add(Listener);
  }
  
  public void removeAllListeners() {
    listeners.clear();
  }

  protected abstract void removeListener(Object listener);

  @Override
  public void finaly() {
    while(!listeners.isEmpty())
      listeners.remove(listeners.firstKey()).clear();
    removeAllListeners();
    tableFilter = null;
  }
}