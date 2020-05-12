package division.swing.table.filter;

import javax.swing.JComponent;
import javax.swing.RowFilter;

public interface FilterComponent {
  public RowFilter getFilter();
  public default void setFilter(Object... params) {
  }
  public FilterButton getFilterButton();
  public TableFilter getTableFilter();
  public boolean isFilter();
  public JComponent packComponent();
  public int getColumn();
  public void clearFilter();
  public void startFilter();
  
  public default boolean isOnlineFilter() {
    return true;
  }
}