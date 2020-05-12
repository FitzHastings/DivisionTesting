package division.swing.table.filter;

import division.swing.CalendarChangeListener;
import division.swing.DistanceList;
import division.swing.DistanceTime;
import division.swing.bum_Calendar;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import javax.swing.*;

public class FilterCalendar extends JPanel implements FilterComponent, CalendarChangeListener, ItemListener {
  private TableFilter tableFilter;
  private int column;
  private FilterButton filterButton;
  
  private bum_Calendar filterDate = new bum_Calendar();
  private JCheckBox checkFilter = new JCheckBox("Использовать фильтр");

  public FilterCalendar(TableFilter tableFilter, int column) {
    super(new BorderLayout());
    this.tableFilter = tableFilter;
    this.column = column;
    this.filterButton = new FilterButton(this);
    
    filterDate.addToListener(this);
    checkFilter.addItemListener(this);
    checkFilter.setSelected(false);
    setEnabled(false);
    checkFilter.setFocusable(false);
    add(checkFilter,BorderLayout.NORTH);
    add(filterDate,BorderLayout.CENTER);
    setBorder(BorderFactory.createLineBorder(Color.BLACK));
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    filterDate.setEnabled(enabled);
  }

  @Override
  public void itemStateChanged(ItemEvent e) {
    setEnabled(e.getStateChange() == ItemEvent.SELECTED);
    if(isOnlineFilter())
      startFilter();
  }
  
  @Override
  public void CalendarChangeDate(DistanceList dates) {
    if(isOnlineFilter())
      startFilter();
  }

  @Override
  public void CalendarSelectedDate(DistanceList dates) {
    startFilter();
  }
  
  @Override
  public void setFilter(Object... params) {
    DistanceList distanceList = new DistanceList();
    Arrays.asList(params).stream().forEach(d -> distanceList.add((DistanceTime)d));
    filterDate.setDistances(distanceList);
    checkFilter.setSelected(true);
  }

  @Override
  public RowFilter getFilter() {
    if(isFilter())
      return new DatesFilter(DatesFilter.Type.EQUAL, getDates(), new int[]{column});
    return null;
  }

  @Override
  public FilterButton getFilterButton() {
    return filterButton;
  }

  @Override
  public TableFilter getTableFilter() {
    return tableFilter;
  }

  @Override
  public boolean isFilter() {
    return checkFilter.isSelected();
  }

  @Override
  public JComponent packComponent() {
    return this;
  }

  @Override
  public int getColumn() {
    return column;
  }
  
  public DistanceList getDates() {
    if(checkFilter.isSelected())
      return filterDate.getDates();
    return null;
  }

  @Override
  public void clearFilter() {
    checkFilter.setSelected(false);
  }

  @Override
  public void startFilter() {
    tableFilter.filter();
  }
}