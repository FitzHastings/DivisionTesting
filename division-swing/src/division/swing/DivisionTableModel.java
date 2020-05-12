package division.swing;

import java.util.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

/**
 * модель таблицы
 */
public class DivisionTableModel extends DefaultTableModel {
  private final HashMap<Integer, Class> columnClass  = new HashMap<>();
  private boolean columnClassAsValue = false;
  private List<MYTableModelListener> listeners = new ArrayList<>();
  
  private final HashMap<Integer,Object> hideRows = new HashMap<>();
  
  public DivisionTableModel() {
    addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(TableModelEvent e) {
        for(MYTableModelListener listener:listeners)
          listener.tableChanged(e);
      }
    });
  }
  
  public void addMYTableModelListener(MYTableModelListener listener) {
    if(!listeners.contains(listener))
      listeners.add(listener);
  }
  
  public void removeMYTableModelListener(MYTableModelListener listener) {
    listeners.remove(listener);
  }
  
  public void fireMoveRow(int startRow, int endRow, int toRow) {
    for(MYTableModelListener listener:listeners)
      listener.moveRow(startRow, endRow, toRow);
  }

  @Override
  public void moveRow(int start, int end, int to) {
    super.moveRow(start, end, to);
    fireMoveRow(start, end, to);
  }

  public boolean isColumnClassAsValue() {
    return columnClassAsValue;
  }

  public void setColumnClassAsValue(boolean columnClassAsValue) {
    this.columnClassAsValue = columnClassAsValue;
  }

  public void setColumnClass(int column, Class clazz) {
    columnClass.put(column, clazz);
  }
  
  public void hideRow(int index) {
    if(!hideRows.containsKey(index)) {
      for(MYTableModelListener listener:listeners)
        listener.hideRow(index);
      hideRows.put(index, getDataVector().remove(index));
      fireTableDataChanged();
    }
  }
  
  public void clear() {
    hideRows.clear();
    getDataVector().clear();
    fireTableDataChanged();
  }
  
  public void showAll() {
    for(Integer index:hideRows.keySet())
      getDataVector().insertElementAt(hideRows.get(index), index);
    hideRows.clear();
    for(MYTableModelListener listener:listeners)
      listener.showAll();
    fireTableDataChanged();
  }

  /**
   * возвращает класс содержимого колонки
   * @param column индекс колонки
   * @return 
   */
  @Override
  public Class getColumnClass(int column) {
    Class clazz = Object.class;
    if(getRowCount() > 0 && isColumnClassAsValue()) {
      Object val = null;
      int row = 0;
      while(val == null && row < getRowCount())
        val = getValueAt(row++,column);
      if(val != null)
        clazz = val.getClass();
    }else if(columnClass.containsKey(column))
      clazz = columnClass.get(column);
    return clazz;
  }

  public void removeColumn(int index) {
    this.columnIdentifiers.remove(index);
    for(Object row:dataVector)
      ((Vector)row).remove(index);
  }
  
  @Override
  public void insertRow(int row, Object[] rowData) {
    insertRow(row,new Vector(Arrays.asList(rowData)));
  }

  public interface  MYTableModelListener extends TableModelListener {
    public void moveRow(int startRow, int endRow, int toRow);
    public void hideRow(int index);
    public void showAll();
  }
}
