package division.swing.table.filter;

import division.swing.DivisionTextField;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.RowFilter.Entry;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import division.swing.table.groupheader.ColumnGroupHeader;
import division.swing.table.groupheader.DefaultColumnGroupHeaderRenderer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class TableFilter extends MouseAdapter implements TableModelListener {
  private final JTable              table;
  private JTableHeader        tableHeader;
  private int                 currentColumn = -1;
  private Point               currentPoint;

  private final JMenuItem removeAllFilters = new JMenuItem("Отменить все фильтры");
  private final JMenuItem removeFilter = new JMenuItem("Отменить фильтр по данной колонке");
  
  private final DefaultColumnGroupHeaderRenderer columnGroupHeaderRenderer = new DefaultColumnGroupHeaderRenderer();
  private final HeaderRenderer renderer = new HeaderRenderer();
  
  private final ConcurrentHashMap<Integer, FilterComponent> filterComponents = new ConcurrentHashMap<>();
  
  public static RowFilter<Object,Object> EmptyFilter = new RowFilter<Object,Object>() { 
    @Override
    public boolean include(Entry<? extends Object, ? extends Object> entry) {
      return true;
    }
  };

  public TableFilter(JTable table) {
    this.table = table;
    setTableHeader(table.getTableHeader());
    initEvents();
  }

  public void setTableHeader(JTableHeader tableHeader) {
    this.tableHeader = tableHeader;
    this.tableHeader.addMouseListener(this);
    this.tableHeader.setDefaultRenderer(renderer);
    for(int i=0;i<table.getColumnCount();i++)
      table.getColumnModel().getColumn(i).setHeaderRenderer(renderer);
    tableHeader.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        currentPoint = e.getPoint();
        currentColumn = TableFilter.this.tableHeader.columnAtPoint(currentPoint);
        int modelColumn = table.convertColumnIndexToModel(currentColumn);
        TableRowSorter sorter = (TableRowSorter)table.getRowSorter();
        if(modelColumn >= 0 && sorter != null)
          sorter.setSortable(modelColumn, !isFilterButtonContains(currentPoint, modelColumn));
      }
    });
  }
  
  public void revalidate() {
    this.tableHeader.setDefaultRenderer(renderer);
    for(int i=0;i<table.getColumnCount();i++)
      table.getColumnModel().getColumn(i).setHeaderRenderer(renderer);
  }

  public JTable getTable() {
    return table;
  }

  public JTableHeader getTableHeader() {
    return tableHeader;
  }
    
  @Override
  public void tableChanged(TableModelEvent e) {
    filter();
  }
  
  public FilterComponent getFilterComponent(int column) {
    return filterComponents.get(column);
  }

  public void addListFilter(int column) {
    filterComponents.put(column, new FilterList(this, column, column));
  }
  
  public void addListFilter(int column, int columnItems) {
    filterComponents.put(column, new FilterList(this, column, columnItems));
  }

  public void addTextFilter(int column) {
    filterComponents.put(column, new FilterTextField(this, DivisionTextField.Type.ALL, column));
  }

  public void addDateFilter(int column) {
    filterComponents.put(column, new FilterCalendar(this, column));
  }

  public void addNumberFilter(int column) {
    filterComponents.put(column, new FilterTextField(this, DivisionTextField.Type.FLOAT, column));
  }

  public void addFilter(int column, FilterComponent filterComponent) {
    filterComponents.put(column, filterComponent);
  }
    
  public FilterButton getFilterButton(int column) {
    return filterComponents.containsKey(column)?filterComponents.get(column).getFilterButton():null;
  }
  
  public ObjectProperty<ArrayList<RowFilter<Object,Object>>> filterProperty = new SimpleObjectProperty(new ArrayList<RowFilter<Object,Object>>());

  public void filter() {
    TableRowSorter sorter = (TableRowSorter)table.getRowSorter();
    if(sorter != null) {
      ArrayList<RowFilter<Object,Object>> filters = new ArrayList<>();

      for(int column:filterComponents.keySet())
        if(filterComponents.get(column).isFilter())
          filters.add(filterComponents.get(column).getFilter());

      if(filters.isEmpty())
        filters.add(EmptyFilter);

      sorter.setRowFilter(RowFilter.andFilter(filters));
      filterProperty.setValue(filters);
      tableHeader.repaint();

      int row = table.getSelectedRow();
      if(row >= 0)
        table.scrollRectToVisible(table.getCellRect(row, 0, true));
    }
  }
  
  public boolean isFilterButtonContains(Point point, int modelColumn) {
    TableRowSorter sorter = (TableRowSorter)table.getRowSorter();
    FilterButton component = getFilterButton(modelColumn);
    Rectangle hr = tableHeader.getHeaderRect(modelColumn);
    if(component != null && hr.contains(point)) {
      Rectangle cr = component.getBounds();
      cr.x += hr.x;
      cr.y = hr.height-cr.height-cr.y;
      SwingUtilities.convertPoint(tableHeader, point, component);
      return cr.contains(point);
    }
    return sorter.isSortable(modelColumn);
  }
    
  private void initEvents() {
    table.getModel().addTableModelListener(this);

    removeAllFilters.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        removeAllFilters();
      }
    });

    removeFilter.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        removeFilter(currentColumn);
      }
    });
  }
    
    public void removeFilter(int column) {
      if(filterComponents.containsKey(column))
        filterComponents.get(column).clearFilter();
      filter();
    }
    
    public void removeAllFilters() {
      for(Integer column:filterComponents.keySet())
        removeFilter(column);
    }

  @Override
  public void mouseClicked(MouseEvent me) {
    currentPoint = me.getPoint();
    currentColumn = tableHeader.columnAtPoint(currentPoint);
    if(me.getModifiers() != MouseEvent.META_MASK) {
      if(me.getClickCount() == 2) {
        removeFilter(currentColumn);
      }else if(me.getClickCount() == 1) {
        FilterButton component = getFilterButton(table.convertColumnIndexToModel(currentColumn));
        if(component != null) {
          Rectangle hr = tableHeader.getHeaderRect(currentColumn);
          Rectangle cr = component.getBounds();
          cr.x += hr.x;
          cr.y = hr.height-cr.height-cr.y;
          SwingUtilities.convertPoint(tableHeader, currentPoint, component);
          if(cr.contains(currentPoint))
            component.fire(me);
        }
      }
    }else {
      JPopupMenu pop = new JPopupMenu();
      pop.add(removeFilter);
      pop.add(removeAllFilters);
      pop.show(tableHeader,me.getX(),me.getY());
    }
  }

  class HeaderRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      JComponent component;
      
      if(tableHeader instanceof ColumnGroupHeader)
        component = (JComponent) columnGroupHeaderRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      else component = (JComponent)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      
      
      JPanel panel = new JPanel(new GridBagLayout());
      //panel.setBorder(component.getBorder());
      panel.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
      component.setBorder(BorderFactory.createEmptyBorder());
      component.setBackground(panel.getBackground());
      column = table.convertColumnIndexToModel(column);
      
      SortArrow sortArrow = null;
      /*TableRowSorter sorter = (TableRowSorter) table.getRowSorter();
      if(sorter != null && sorter.isSortable(column)) {
        for(RowSorter.SortKey sortKey:(java.util.List<RowSorter.SortKey>) sorter.getSortKeys()) {
          if(sortKey.getColumn() == column) {
            sortArrow = new SortArrow(sortKey.getSortOrder());
          }
        }
      }*/
      
      if(getFilterButton(column) != null) {
        panel.add(getFilterButton(column), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        panel.add(component, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
        if(sortArrow != null)
          panel.add(sortArrow, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
      }else {
        panel.add(component, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
        if(sortArrow != null)
          panel.add(sortArrow, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
      }
      return panel;
    }
  }
  
  class SortArrow extends JPanel {
    private SortOrder order;

    public SortArrow(SortOrder order) {
      this.order = order;
      setMinimumSize(new Dimension(8, 10));
      setMaximumSize(new Dimension(8, 10));
      setPreferredSize(new Dimension(8, 10));
    }
    
    @Override
    public void paint(Graphics g) {
      super.paint(g);
      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
      g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.setColor(getTable().getTableHeader().getBackground().darker());
      switch(order) {
        case ASCENDING:
          g2.fillPolygon(new int[]{0, getWidth(), getWidth()/2}, new int[]{getHeight(), getHeight(), 0}, 3);
          break;
        case DESCENDING:
          g2.fillPolygon(new int[]{0, getWidth()/2, getWidth()}, new int[]{0, getHeight(), 0}, 3);
          break;
        case UNSORTED: break;
      }
    }
  }
}