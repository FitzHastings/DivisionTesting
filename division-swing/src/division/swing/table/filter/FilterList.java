package division.swing.table.filter;

import java.awt.Dimension;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.commons.lang3.ArrayUtils;

public class FilterList extends JList<String> implements FilterComponent, ListSelectionListener {
  private TableFilter tableFilter;
  private int column;
  private int columnItems;
  private FilterButton filterButton;
  private boolean active = true;

  public FilterList(TableFilter filter, int column) {
    this(filter, column, column);
  }

  public FilterList(TableFilter filter, int column, int columnItems) {
    super(new DefaultListModel());
    this.tableFilter = filter;
    this.column = column;
    this.columnItems = columnItems;
    filterButton = new FilterButton(this);
    addListSelectionListener(this);
    /*filterButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(getSelectedIndices().length == 1 && getSelectedIndex() == 0)
          clearFilter();
      }
    });*/
  }
  
  @Override
  public void valueChanged(ListSelectionEvent e) {
    if(active && e.getValueIsAdjusting()) {
      if(isOnlineFilter())
        startFilter();
      if(getSelectedIndex() == 0) {
        active = false;
        clearSelection();
        active = true;
      }
    }
  }
  
  public void reList() {
    DefaultListModel model = (DefaultListModel) getModel();
    String[] selectedValues = new String[0];
    for(int row:getSelectedIndices())
      selectedValues = (String[]) ArrayUtils.add(selectedValues, model.getElementAt(row));

    model.clear();
    model.addElement("Все");
    //boolean nullable = false;
    for(int i=0;i<tableFilter.getTable().getModel().getRowCount();i++) {
      Object value = tableFilter.getTable().getModel().getValueAt(i, columnItems) instanceof JComboBox?((JComboBox)tableFilter.getTable().getModel().getValueAt(i, columnItems)).getSelectedItem():tableFilter.getTable().getModel().getValueAt(i, columnItems);
      if(value != null && !model.contains(String.valueOf(value)))
        model.addElement(String.valueOf(value));
    }
    /*if(nullable) {
      model.addElement("Пустые");
      model.addElement("Непустые");
    }*/
    for(String val:selectedValues)
      addSelectionInterval(model.indexOf(val), model.indexOf(val));
  }
  
  @Override
  public RowFilter getFilter() {
    if(getSelectedIndices().length == 1 && getSelectedIndex() == 0)
      return null;

    return new RowFilter<DefaultListModel, Integer>() {
      @Override
      public String toString() {
        String d = "";
        for(int row:getSelectedIndices())
          d += getModel().getElementAt(row)+" или ";
        return "фильтр для колонки №"+column+" = "+(d.equals("")?"NULL":d.substring(0, d.length()-5));
      }

      @Override
      public boolean include(RowFilter.Entry<? extends DefaultListModel, ? extends Integer> entry) {
        if(getSelectedIndices().length == 0)
          return true;
        String[] values = new String[0];
        for(int row:getSelectedIndices())
          values = (String[]) ArrayUtils.add(values, getModel().getElementAt(row));

        if(ArrayUtils.contains(values, "Все"))
          return true;

        Object object = entry.getValue(columnItems);
        if(object == null)
          object = "";
        if(object instanceof JComboBox)
          object = ((JComboBox)object).getSelectedItem()==null?"":((JComboBox)object).getSelectedItem().toString();
        return ArrayUtils.contains(values, object);
      }
    };
  }

  @Override
  public int getColumn() {
    return column;
  }

  public int getColumnItems() {
    return columnItems;
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
    return getSelectedIndex() > 0;
  }

  @Override
  public JComponent packComponent() {
    reList();
    JScrollPane scroll = new JScrollPane(this);
    if(scroll.getPreferredSize().width < 100)
      scroll.setPreferredSize(new Dimension(100, 100));
    else scroll.setPreferredSize(new Dimension(scroll.getPreferredSize().width+10, scroll.getPreferredSize().height));
    return scroll;
  }

  @Override
  public void clearFilter() {
    clearSelection();
  }

  @Override
  public void startFilter() {
    tableFilter.filter();
  }
}