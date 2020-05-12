package division.swing;

import division.swing.table.CellColorController;
import division.swing.table.CellEditableController;
import division.swing.table.CellFontController;
import division.swing.table.filter.TableFilter;
import division.util.FileLoader;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import org.apache.commons.lang3.ArrayUtils;

public class DivisionTable extends JTable {
  /**
   * Модель
   */
  private DivisionTableModel model = new DivisionTableModel();
  /**
   * сортировщик
   */
  @SuppressWarnings("unchecked")
  //private TableRowSorter sorter = new TableRowSorter(model);
  /**
   * Пустой фильтр
   */
  private RowFilter<Object,Object> EmptyFilter = new RowFilter<Object,Object>() {
    @Override
    public boolean include(RowFilter.Entry<? extends Object, ? extends Object> entry) {
      return true;
    }
  };
  /**
   * номера редактируемых колонок
   */
  private ArrayList<Integer> editingColumns = new ArrayList<>();
  private final ArrayList<TableSelectionListener> listeners = new ArrayList<>();
  private int[] lastSelection = new int[0];
  private TableFilter tableFilters = new TableFilter(this);
  private final DivisionTableRenderer renderer = new DivisionTableRenderer();
  private final DivisionTableEditor editor = new DivisionTableEditor(this);
  private boolean editable = true;

  private final DivisionToolButton previos = new DivisionToolButton(FileLoader.getIcon("arrow4_left.GIF"));
  private final DivisionToolButton next    = new DivisionToolButton(FileLoader.getIcon("arrow4_right.GIF"));
  private final JTextField findText  = new JTextField();
  private final JPopupMenu pop       = new JPopupMenu("Поиск");
  private Integer[] findTextColumns = new Integer[0];
  private Integer generalColumnIndex = -1;

  private boolean findable = true;
  

  private CellEditableController cellEditableController;

  public DivisionTable() {
    this(null);
  }

  @SuppressWarnings("unchecked")
  public DivisionTable(CellColorController cellColorController) {
    this(cellColorController, true);
  }
  
  public DivisionTable(CellColorController cellColorController, boolean sortable) {
    renderer.setCellColorController(cellColorController);
    
    this.setDefaultRenderer(Object.class, renderer);
    this.setDefaultEditor(Object.class, editor);

    this.setDefaultRenderer(java.sql.Date.class, renderer);
    this.setDefaultEditor(java.sql.Date.class, editor);

    this.setDefaultRenderer(java.sql.Timestamp.class, renderer);
    this.setDefaultEditor(java.sql.Timestamp.class, editor);
    
    this.setDefaultRenderer(Double.class, renderer);
    this.setDefaultEditor(Double.class, editor);

    this.setDefaultRenderer(Float.class, renderer);
    this.setDefaultEditor(Float.class, editor);
    
    this.setDefaultRenderer(BigDecimal.class, renderer);
    this.setDefaultEditor(BigDecimal.class, editor);

    this.setDefaultRenderer(Integer.class, renderer);
    this.setDefaultEditor(Integer.class, editor);

    this.setDefaultRenderer(Boolean.class, renderer);
    this.setDefaultEditor(Boolean.class, editor);

    this.setDefaultRenderer(JComponent.class, renderer);
    this.setDefaultEditor(JComponent.class, editor);

    this.setDefaultRenderer(String.class, renderer);
    this.setDefaultEditor(String.class, editor);

    setModel(model);
    initEvents();

    previos.setFocusable(false);
    next.setFocusable(false);
    JPanel panel = new JPanel(new GridBagLayout());
    panel.add(findText, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
    panel.add(previos,  new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 2), 0, 0));
    panel.add(next,     new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
    pop.add(panel);
    findText.setColumns(20);
    setAllColumnSearching(true);
    setSelectionBackground(new Color(100,100,255,100));
    setSelectionForeground(Color.DARK_GRAY);
    setSortable(sortable);
  }

  @Override
  public void setTableHeader(JTableHeader tableHeader) {
    super.setTableHeader(tableHeader);
    if(tableFilters != null && tableHeader != null)
      tableFilters.setTableHeader(tableHeader);
  }
  
  public void clear() {
    TableCellEditor ed = getCellEditor();
    if(ed != null)
      ed.stopCellEditing();
    model.clear();
  }
  
  public CellFontController getCellFontController() {
    return renderer.getCellFontController();
  }

  public void setCellFontController(CellFontController cellFontController) {
    renderer.setCellFontController(cellFontController);
  }
  
  public void setCellColorController(CellColorController cellColorController) {
    renderer.setCellColorController(cellColorController);
  }

  public CellEditableController getCellEditableController() {
    return cellEditableController;
  }

  public void setCellEditableController(CellEditableController cellEditableController) {
    this.cellEditableController = cellEditableController;
  }

  public CellColorController getCellColorController() {
    return renderer.getCellColorController();
  }

  public boolean isFindable() {
          return findable;
  }

  public void setFindable(boolean findable) {
          this.findable = findable;
  }

  private void findPreviosText(int startRow) {
    Object name;
    String text = findText.getText().toLowerCase();
    int column = generalColumnIndex!=-1?generalColumnIndex:getSelectedColumn();
    for(int i=(startRow<0?0:startRow-1);i>=0;i--) {
      name = getValueAt(i, column);
      if(name != null && name.toString().toLowerCase().indexOf(text) != -1) {
        setRowSelectionInterval(i, i);
        scrollRectToVisible(getCellRect(i, column, false));
        break;
      }
    }
    findText.grabFocus();
  }
  
  private void findNextText(int startRow) {
    Object name;
    String text = findText.getText().toLowerCase();
    int column = generalColumnIndex!=-1?generalColumnIndex:getSelectedColumn();
    for(int i=startRow<0?0:startRow;i<getRowCount();i++) {
      name = getValueAt(i, column);
      if(name != null && name.toString().toLowerCase().indexOf(text) != -1) {
        setRowSelectionInterval(i, i);
        scrollRectToVisible(getCellRect(i, column, false));
        break;
      }
    }
    findText.grabFocus();
  }

  public void setColumnSearching(int[] columnIndexes, boolean isSearching) {
    for(int columnIndex:columnIndexes) {
      if(isSearching) {
        if(!ArrayUtils.contains(findTextColumns,columnIndex))
          findTextColumns = (Integer[])ArrayUtils.add(findTextColumns, columnIndex);
      }else findTextColumns = (Integer[])ArrayUtils.removeElement(findTextColumns, columnIndex);
    }
  }

  public void setAllColumnSearching(boolean isSearching) {
    int[] indexes = new int[getColumnCount()];
    for(int i=0;i<getColumnCount();i++)
      indexes[i] = i;
    setColumnSearching(indexes, isSearching);
  }

  public void setColumnForAllFinders(int columnIndex) {
    generalColumnIndex = columnIndex;
  }

  public void removeColumnForAllFinders() {
    generalColumnIndex = -1;
  }

  public DivisionTableEditor getEditor() {
    return editor;
  }
  
  public DivisionTableRenderer getCellRenderer() {
    return renderer;
  }
  
  public division.swing.table.filter.TableFilter getTableFilters() {
    return tableFilters;
  }

  public void setTableFilters(division.swing.table.filter.TableFilter tableFilters) {
    this.tableFilters = tableFilters;
  }
  
  public void setSortable(boolean is) {
    if(!is) 
      setRowSorter(null);
    else {
      setRowSorter(new TableRowSorter<>(getModel()));
      getSorter().setMaxSortKeys(100);
    }
  }
  
  public DivisionTableModel getTableModel() {
    return model;
  }
  
  public void setColumnWidthZero(int... columns) {
    for(int i:columns) {
      findTableColumn(i).setMinWidth(0);
      findTableColumn(i).setMaxWidth(0);
      findTableColumn(i).setPreferredWidth(0);
      findTableColumn(i).setWidth(0);
    }
  }
  
  /**
   * Возвращает сортировщик
   */
  public TableRowSorter getSorter() {
    return (TableRowSorter)getRowSorter();
  }

  /**
   * Устанавливает сортировщик
   */
  /*public void setSorter(javax.swing.table.TableRowSorter val) {
    setRowSorter(val);
  }*/

  /**
   * устанавливает редактиркемость колонки
   * @param columns
   * @param isEditable
   */
  public void setColumnEditable(int[] columns, boolean isEditable) {
    for(int column:columns)
      setColumnEditable(column, isEditable);
  }
  
  public void setColumnEditable(int column, boolean isEditable) {
    if(isEditable) {
      if(!editingColumns.contains(column))
        editingColumns.add(column);
    }else editingColumns.remove(column);
  }

  /**
   * устанавливает колонки таблицы
   * @param columns
   */
  public void setColumns(ArrayList columns) {
    setColumns(columns.toArray());
  }
  
  /**
   * устанавливает колонки таблицы
   * @param columns
   */
  public void setColumns(Object... columns) {
    model.setDataVector(new Object[0][0], columns);
  }
  
  @Override
  public boolean isCellEditable(int row, int column) {
    if(!isEditable())
      return false;
    
    if(getCellEditableController() != null)
      return getCellEditableController().isCellEditable(this, convertRowIndexToModel(row), convertColumnIndexToModel(column));
    
    return editingColumns.contains(column);
  }

  /**
   * возвращает TableColumn по индексу
   */
  public TableColumn findTableColumn(int columnModelIndex) {
    //return getColumnModel().getColumn(columnModelIndex);
    Enumeration en = getColumnModel().getColumns();
    while(en.hasMoreElements()) {
      TableColumn col = (TableColumn)en.nextElement();
      if(col.getModelIndex() == columnModelIndex){return col;}
    }
    return null;
  }

  public RowFilter<Object,Object> getEmptyFilter() {
    return EmptyFilter;
  }
  
  public void setEmptyFilter(RowFilter<Object,Object> val) {
    EmptyFilter = val;
  }
  
  public void setEditable(boolean editable) {
    this.editable = editable;
  }

  public boolean isEditable() {
    return editable;
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if(isEnabled())
      setBackground(Color.WHITE);
    else setBackground(Color.lightGray);
  }

  public ArrayList<Integer> getEditingColumns() {
    return editingColumns;
  }
  
  public void setEditingColumns(ArrayList<Integer> val) {
    editingColumns = val;
  }

  public void addTableSelectionListener(TableSelectionListener tableSelectionListener) {
    listeners.add(tableSelectionListener);
  }

  public void removeTableSelectionListener(TableSelectionListener tableSelectionListener) {
    listeners.remove(tableSelectionListener);
  }

  public void fireTableSelectionChange() {
    for(TableSelectionListener tableSelectionListener:listeners)
      tableSelectionListener.TableSelectionChanged(lastSelection,getSelectedRows());
    lastSelection = Arrays.copyOf(getSelectedRows(), getSelectedRowCount());
  }
  
  @SuppressWarnings("unchecked")
  public void ResetFilters() {
    //getTableFilters().removeAllFilters();
    getSorter().setRowFilter(EmptyFilter);
  }
  
  public void removeAllRows() {
    try {
      ResetFilters();
      if(getSorter().getViewRowCount() > 0) {
        if(getCellEditor() != null)getCellEditor().stopCellEditing();
        for(int i=getSorter().getViewRowCount()-1;i>=0;i--)
          model.removeRow(i);
      }
    }catch(Exception e){e.printStackTrace();}
  }

  private void initEvents() {
    /*addMouseWheelListener(new MouseWheelListener() {
      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
        grabFocus();
        try {
          getCellEditor().stopCellEditing();
        }catch(Exception ex) {
        }
        int index = getSelectedRow()+e.getWheelRotation();
        if(getSelectedRow() == -1)
          index = (e.getWheelRotation() == 1?getSelectedRow():getRowCount())+e.getWheelRotation();
        if(getSelectedRow() == -1 || (index >= 0 && index <= getRowCount()-1))
          setRowSelectionInterval(index,index);
        scrollRectToVisible(getCellRect(getSelectedRow(), 0 , true));
      }
    });*/
    
    addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        if(e.getModifiers() == MouseEvent.META_MASK && rowAtPoint(e.getPoint()) != -1) {
          if(getSelectedRows().length != 1)
            addRowSelectionInterval(rowAtPoint(e.getPoint()),rowAtPoint(e.getPoint()));
          else DivisionTable.this.setRowSelectionInterval(rowAtPoint(e.getPoint()),rowAtPoint(e.getPoint()));
        }
      }
    });

    addKeyListener(new KeyAdapter() {
      @Override
      public void keyTyped(KeyEvent e) {
        if(isFindable()) {
          if(Character.isLetter(e.getKeyChar()) || Character.isDigit(e.getKeyChar())) {
            if(!pop.isVisible()) {
              setColumnSelectionAllowed(true);
              pop.show(getParent(), (getParent().getWidth()/2)-100, (getParent().getHeight()/2)-10);
              pop.setLocation((int)(getParent().getLocationOnScreen().getX() + (getParent().getWidth() / 2) - (findText.getWidth() / 2)),(int)(getParent().getLocationOnScreen().getY() + (getParent().getHeight() / 2) - 10));
              findText.setText("");
              findText.setText(String.valueOf(e.getKeyChar()));
              findText.grabFocus();
            }
            findText.grabFocus();
          }
        }
      }
    });

    next.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        findNextText(getSelectedRow()+1);
      }
    });
    
    previos.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        findPreviosText(getSelectedRow());
      }
    });
    
    findText.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_F3)
          findNextText(getSelectedRow()+1);
        else if(e.getKeyCode() == KeyEvent.VK_F2)
          findPreviosText(getSelectedRow());
      }
    });

    findText.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        findNextText(0);
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        findText.grabFocus();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        findText.grabFocus();
      }
    });

    pop.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        setColumnSelectionAllowed(true);
        if(generalColumnIndex != -1)
          setColumnSelectionInterval(generalColumnIndex,generalColumnIndex);
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        setColumnSelectionAllowed(false);
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
        setColumnSelectionAllowed(false);
        setRowSelectionInterval(getSelectedRow(), getSelectedRow());
      }
    });
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    super.valueChanged(e);
    if(!e.getValueIsAdjusting())
      fireTableSelectionChange();
  }
}