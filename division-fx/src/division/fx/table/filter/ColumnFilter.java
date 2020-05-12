package division.fx.table.filter;

import division.fx.PropertyMap;
import division.fx.gui.FXDisposable;
import java.util.function.Predicate;
import javafx.beans.property.BooleanProperty;
import javafx.scene.Node;

public interface ColumnFilter<S extends PropertyMap> extends FXDisposable {
  public void initFilter();
  public Node getContent();
  public Predicate<S> getPredicate();
  public void addFilterListener(FilterListener handler);
  public void removeFilterListener(FilterListener handler);
  public TableFilter<S> getTableFilter();
  public void setTableFilter(TableFilter<S> tableFilter);
  public String getProperty();
  public void setProperty(String property);
  
  public BooleanProperty activeProperty();
  
  public default boolean isActive() {
    return activeProperty().getValue();
  }
}