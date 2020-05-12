package bum.editors;

import division.swing.guimessanger.Messanger;
import bum.editors.actions.ActionMacrosUtil;
import bum.editors.util.IDColorController;
import bum.editors.util.ObjectLoader;
import bum.interfaces.MarkerNode;
import division.swing.*;
import division.util.FileLoader;
import division.xml.Node;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.rmi.RemoteException;
import java.util.*;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.ListSelectionModel;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.TableModelEvent;
import mapping.MappingObject;
import mapping.MappingObject.Type;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import util.RemoteSession;

public class TableEditor extends DBTableEditor {
  private final IDColorController  colorController = new IDColorController();
  private final DivisionToolButton print           = new DivisionToolButton(FileLoader.getIcon("Print16.gif"),"печать");
  private final DivisionTable      table           = new DivisionTable(colorController);
  private int            dndRow      = -1;
  private Object[]       tableColumns;
  
  private Integer[] autoEditingColumns = new Integer[0];

  double rectHeight;
  int    rowHeight;
  //Количество строк, которые видно на экране
  int    visibleRowCount;
  //Количество подгружаемых строк
  int    toLoadRowCount;
  //Количество строк от конца, за которое нужно старовать загрузку новых
  int    beforeStartRowCount;
  //Общее количество строк в таблице
  int    rowCount;
  int    startPixel;

  protected DivisionToolButton showHideMarkers   = new DivisionToolButton();
  protected DivisionToolButton changeMarkers     = new DivisionToolButton(FileLoader.getIcon("user.gif"),"одын");

  private ActionListener printAction = (ActionEvent e) -> {
    print();
  };

   public TableEditor(
          String[] tableColumns,
          String[] dataFields,
          Class objectClass,
          Class objectEditorClass,
          String title,
          ImageIcon icon,
          MappingObject.Type type) {
     this(tableColumns, dataFields, objectClass, objectEditorClass, title, icon, type, false);
   }

  /**
   * Конструктор
   * @param tableColumns названия колонок таблицы (первая должна быть всегда id)
   * @param objectClass класс объектов, которые будут отображаться в таблице
   * @param objectEditorClass класс редактора объекта
   * @param title заголовок окна таблицы
   * @param icon иконка окна таблицы
   * @param type тип отображаемых объектов CURRENT, PROJECT или ARCHIVE
   */
  public TableEditor(
          String[] tableColumns,
          String[] dataFields,
          Class objectClass,
          Class objectEditorClass,
          String title,
          ImageIcon icon,
          MappingObject.Type type,
          boolean markerVisible) {
    super(objectClass, objectEditorClass, title, icon, type);

    this.tableColumns = tableColumns;
    this.setDataFields(dataFields);
    initTableEvents(); 
    initTableComponents();
    table.setEditable(true);
    table.getTableHeader().setReorderingAllowed(false);
    addComponentToStore(table,"table");
    setSelectionBackground();
    //table.getTableFilters().addNumberFilter(0);
    DragSource dragSource = DragSource.getDefaultDragSource();
    dragSource.createDefaultDragGestureRecognizer(table,DnDConstants.ACTION_COPY_OR_MOVE,this);
    DropTarget dropTarget = new DropTarget(table, DnDConstants.ACTION_COPY_OR_MOVE,this,true,null);
  }

  public TableEditor(String[] tableColumns,Class objectClass,Class objectEditorClass,String title,ImageIcon icon,MappingObject.Type type) {
    this(tableColumns,new String[0],objectClass,objectEditorClass,title,icon,type);
  }
  /**
   * Конструктор
   * @param tableColumns названия колонок таблицы (первая должна быть всегда id)
   * @param objectClass класс объектов, которые будут отображаться в таблице
   * @param objectEditorClass класс редактора объекта
   * @param title заголовок окна таблицы
   * @param type тип отображаемых объектов CURRENT, PROJECT или ARCHIVE
   */
  public TableEditor(String[] tableColumns, Class objectClass, Class objectEditorClass, String title, MappingObject.Type type) {
    this(tableColumns, objectClass, objectEditorClass, title, null, type);
  }

  public TableEditor(String[] tableColumns, String[] dataFields, Class objectClass, Class objectEditorClass, String title, MappingObject.Type type) {
    this(tableColumns, dataFields, objectClass, objectEditorClass, title, null, type);
  }
  /**
   * Конструктор (тип объектов CURRENT)
   * @param tableColumns названия колонок таблицы (первая должна быть всегда id)
   * @param objectClass класс объектов, которые будут отображаться в таблице
   * @param objectEditorClass класс редактора объекта
   * @param title заголовок окна таблицы
   */
  public TableEditor(String[] tableColumns, Class objectClass, Class objectEditorClass, String title) {
    this(tableColumns, objectClass, objectEditorClass, title, MappingObject.Type.CURRENT);
  }

  public TableEditor(String[] tableColumns, String[] dataFields, Class objectClass, Class objectEditorClass, String title) {
    this(tableColumns, dataFields, objectClass, objectEditorClass, title, MappingObject.Type.CURRENT);
  }
  /**
   * Конструктор
   * @param tableColumns названия колонок таблицы (первая должна быть всегда id)
   * @param objectClass класс объектов, которые будут отображаться в таблице
   * @param objectEditorClass класс редактора объекта
   * @param type тип отображаемых объектов CURRENT, PROJECT или ARCHIVE
   */
  public TableEditor(String[] tableColumns, Class objectClass, Class objectEditorClass, MappingObject.Type type) {
    this(tableColumns, objectClass, objectEditorClass, "", type);
  }

  public TableEditor(String[] tableColumns, String[] dataFields, Class objectClass, Class objectEditorClass, MappingObject.Type type) {
    this(tableColumns, dataFields, objectClass, objectEditorClass, "", type);
  }
  /**
   * Конструктор (тип объектов CURRENT)
   * @param tableColumns названия колонок таблицы (первая должна быть всегда id)
   * @param objectClass класс объектов, которые будут отображаться в таблице
   * @param objectEditorClass класс редактора объекта
   */
  public TableEditor(String[] tableColumns, Class objectClass, Class objectEditorClass) {
    this(tableColumns, objectClass, objectEditorClass, "");
  }

  public TableEditor(String[] tableColumns,String[] dataFields, Class objectClass, Class objectEditorClass) {
    this(tableColumns, dataFields, objectClass, objectEditorClass, "");
  }
  
  public void addAutoEditingColumn(Integer columnIndex) {
    if(!ArrayUtils.contains(autoEditingColumns, columnIndex))
      autoEditingColumns = (Integer[]) ArrayUtils.add(autoEditingColumns, columnIndex);
  }
  
  public void addAutoEditingColumns(Integer... columnsIndexes) {
    for(Integer columnIndex:columnsIndexes)
      addAutoEditingColumn(columnIndex);
  }
  
  public void clearAutoEditingColumns() {
    autoEditingColumns = new Integer[0];
  }

  public Integer[] getAutoEditingColumns() {
    return autoEditingColumns;
  }

  public IDColorController getColorController() {
    return colorController;
  }

  public void setPrintFunction(boolean isPrint) {
    print.setVisible(isPrint);
  }

  @Override
  public void setType(Type type) {
    super.setType(type);
    setSelectionBackground();
  }

  private void setSelectionBackground() {
    if(table != null)
      if(getType() == MappingObject.Type.ARCHIVE || getType() == MappingObject.Type.PROJECT)
        table.setSelectionBackground(Color.LIGHT_GRAY);
  }

  public Integer[] getAllIds() {
    Integer[] ids = new Integer[0];
    Vector<Vector> data = table.getTableModel().getDataVector();
    for(Vector row:data) {
      if(row.get(0) instanceof Integer)
        ids = (Integer[])ArrayUtils.add(ids, row.get(0));
      else if(row.get(0) instanceof Integer[])
        ids = (Integer[])ArrayUtils.addAll(ids, (Integer[])row.get(0));
    }
    return ids;
  }

  @Override
  protected boolean dragOver(Point point, List objects, Class interfaceClass) {
    /*int row = table.rowAtPoint(point);
    if(row != -1) {
      if(dndRow != -1 && row != dndRow)
        colorController.removeDropRowColor();
      
      dndRow = row;
      colorController.setDropRowColor(dndRow, Color.LIGHT_GRAY);
      table.repaint();
      return true;
    }*/
    return false;
  }

  //HashMap<Integer, Color> colors = new HashMap<Integer, Color>();

  @Override
  public void dragEnter(DropTargetDragEvent dtde) {
    //colors = (HashMap<Integer, Color>)table.getTableModel().getRowColor().clone();
  }

  @Override
  public void dragExit(DropTargetEvent dte) {
    /*colorController.removeDropRowColor();
    dndRow = -1;
    table.repaint();*/
  }

  @Override
  protected void drop(Point point, List objects, Class interfaceClass) {
    /*colorController.removeDropRowColor();
    dndRow = -1;
    table.repaint();

    if(interfaceClass == Node.class || interfaceClass == MarkerNode.class) {
      Integer[] rIds = new Integer[0];
      Object id = table.getValueAt(table.rowAtPoint(point), 0);
      if(id instanceof Integer[])
        rIds = (Integer[])ArrayUtils.addAll(rIds, (Integer[])id);
      else if(id instanceof Integer)
        rIds = (Integer[])ArrayUtils.add(rIds, (Integer)id);

      Integer[] ids = getSelectedId();
      if(ids.length == 0)
        ids = rIds;
      else {
        for(int i=0;i<rIds.length;i++)
          if(ArrayUtils.indexOf(ids, rIds[i]) == -1 && i == rIds.length-1)
            ids = rIds;
          else break;
      }

      if(ids.length > 0) {

        if(marker.isMarker(ids)) {
          if(JOptionPane.showConfirmDialog(
                  null,
                  "Некоторые объекты уже маркированны,\nснять с них маркер и продолжить?",
                  "Внимание",
                  JOptionPane.YES_NO_CANCEL_OPTION,
                  JOptionPane.QUESTION_MESSAGE) == 0)
            marker.removeMarker(ids);
          else return;
        }

        if(marker.isLocal()) {
          ((Node)objects.get(1)).getNode("ID").setValue(((Node)objects.get(1)).getNode("ID").getValue()+","+Marker.getStringIds(ids));
          ((Document)objects.get(0)).save();
        }else {
          try {
            ((MarkerNode)objects.get(1)).setObjectsId((Integer[]) ArrayUtils.addAll(((MarkerNode)objects.get(1)).getObjectsId(), ids));
            ((MarkerNode)objects.get(1)).saveObject();
          }catch(RemoteException ex) {
            Messanger.showErrorMessage(ex);
          }
        }
        if(marker.isMarkersVisible())
          marker.setMarkersVisible(true);
      }
    }*/
  }

  @Override
  public void setSingleSelection(boolean singleSelection) {
    if(singleSelection)
      getTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    else getTable().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
  }
  
  @Override
  public boolean isSingleSelection() {
    return getTable().getSelectionModel().getSelectionMode() == ListSelectionModel.SINGLE_SELECTION;
  }

  @Override
  public void clearData() {
    getTable().getTableModel().getDataVector().clear();
    getTable().getTableModel().fireTableDataChanged();
  }
  /**
   * Возвращает таблицу отображения
   * @return таблица отображения
   */
  public DivisionTable getTable() {
    return this.table;
  }

  @Override
  public Integer[] getSelectedId() {
    int[] rows = table.getSelectedRows();
    Integer[] ids = new Integer[0];
    for(int row:rows) {
      Object o = table.getValueAt(row,0);
      if(o instanceof Integer)
        ids = (Integer[])ArrayUtils.add(ids, o);
      if(o instanceof Integer[])
        ids = (Integer[])ArrayUtils.addAll(ids, (Integer[])o);
    }
    return ids;
  }

  @Override
  public void checkMenu() {
    if(this.isEnabled()) {
      if(table.getSelectedRow() == -1) {
        editToolButton.setEnabled(false);
        removeToolButton.setEnabled(false);
        editPopMenuItem.setEnabled(false);
        removePopMenuItem.setEnabled(false);
      }else {
        editToolButton.setEnabled(true);
        removeToolButton.setEnabled(true);
        editPopMenuItem.setEnabled(true);
        removePopMenuItem.setEnabled(true);
      }
    }
  }

  @Override
  public int getObjectsCount() {
    return table.getRowCount();
    //return table.getTableModel().getDataVector().size();
  }

  public int getFilteredObjectsCount() {
    return table.getRowCount();
  }

  @Override
  public int getSelectedObjectsCount() {
    return table.getSelectedRowCount();
  }

  @Override
  public MappingObject[] getSelectedObjects() {
    getGUI().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    try {
      return ObjectLoader.getObjects(getObjectClass(), getSelectedId());
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }finally {
      getGUI().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
    return new MappingObject[0];
  }

  /**
   * Возвращает строку таблицы соответствующую объекту
   * @param object объект
   * @return номер строки
   * @throws RemoteException
   */
  public int getRowObject(MappingObject object) throws RemoteException {
    Integer id = object.getId();
    return getRowObject(id);
  }

  /**
   * Возвращает объект из таблицы
   * @param row номер строки в таблице
   * @return объект
   */
  public MappingObject[] getRowObject(int row) {
    MappingObject[] objects = null;
    try {
      if(row != -1 && row < table.getRowCount()) {
        if(this.table.getValueAt(row, 0) instanceof Integer[])
          objects = ObjectLoader.getObjects(getObjectClass(), (Integer[])this.table.getValueAt(row, 0));
        else {
          Integer id = Integer.valueOf(this.table.getValueAt(row, 0).toString());
          if(id != null) {
            objects = new MappingObject[1];
            objects[0] = ObjectLoader.getObject(getObjectClass(), id);
          }
        }
      }
    }catch(Exception ex){Messanger.showErrorMessage(ex);}
    return objects;
  }
  /**
   * Возвращает номер строки в модели таблицы
   * @param id
   * @return номер строки в модели таблицы
   * @throws RemoteException
   */
  public int getRowObject(Integer id) {
    Vector<Vector> data = table.getTableModel().getDataVector();
    for(int i=0;i<data.size();i++) {
      Object object = data.get(i).get(0);
      if(object instanceof Integer && object.equals(id))
        return i;
      if(object instanceof Integer[] && ArrayUtils.contains((Integer[])object, id))
        return i;
    }
    return -1;
  }

  @Override
  protected void eventUpdate(Integer[] ids) {
    try {
      Integer[] createdIds = new Integer[0];
      Integer[] removeIds = (Integer[])ArrayUtils.clone(ids);
      TreeMap<Integer,Integer> updateIds = new TreeMap<>();
      int modelRow;
      for(Integer id:isFilteredObject(ids)) {
        removeIds = (Integer[])ArrayUtils.removeElement(removeIds, id);
        modelRow = getRowObject(id);
        if(modelRow != -1) {
          updateIds.put(id,modelRow);
        }else createdIds = (Integer[])ArrayUtils.add(createdIds, id);
        createtedObjects.remove(id);
      }
      if(!updateIds.isEmpty())
        updateObjects(updateIds);
      if(createdIds.length > 0)
        createObject(createdIds);
      if(removeIds.length > 0)
        removeObjects(removeIds);
    }catch(Exception ex){Messanger.showErrorMessage(ex);}
  }

  @Override
  protected int insertObject(MappingObject object,int row) throws RemoteException {
    //table.getTableModel().insertRow(row<0?0:row, object.getRow());
    return row;
  }

  @Override
  protected void createObject(MappingObject object) throws RemoteException {
    insertObject(object,0);
    if(createtedObjects.contains(object.getId())) {
      int row = this.getRowObject(object);
      if(row != -1) {
        if(waitingSelect) {
          table.setRowSelectionInterval(row, row);
          table.scrollRectToVisible(table.getCellRect(row, 1, true));
          waitingSelect = false;
        }else table.addRowSelectionInterval(row, row);
      }
      createtedObjects.remove(object.getId());
    }
  }

  protected void createObject(Integer[] ids) throws Exception {
    if(ids.length > 0) {
      List<List> data = ObjectLoader.getData(getObjectClass(), ids, getDataFields(),getSortFields());
      insertData(data);
      for(Integer id:ids) {
        if(createtedObjects.contains(id)) {
          int modelRow = getRowObject(id);
          if(modelRow != -1) {
            if(waitingSelect) {
              int tableRow = table.convertRowIndexToView(modelRow);
              table.setRowSelectionInterval(tableRow, tableRow);
              table.scrollRectToVisible(table.getCellRect(tableRow, 1, true));
              waitingSelect = false;
            }else table.addRowSelectionInterval(modelRow, modelRow);
          }
          createtedObjects.remove(id);
        }
      }
    }
  }
  
  protected Object transformData(int modelRow, int modelColumn, Object object) {
    return object;
  }

  protected void updateObjects(TreeMap<Integer,Integer> updateIds) throws Exception {
    setActive(false);
    if(!updateIds.isEmpty()) {
      String[] fields = getDataFields();
      fields = (String[])ArrayUtils.add(fields, 0, "id");
      int modelRow;
      Vector<Vector> dataVector = table.getTableModel().getDataVector();
      List<List> data = ObjectLoader.getData(getObjectClass(), updateIds.keySet().toArray(new Integer[0]), fields, getSortFields());
      for(List d:data) {
        modelRow = updateIds.get(d.get(0));
        d.remove(0);
        for(int i=0;i<d.size();i++)
          dataVector.get(modelRow).set(i, transformData(modelRow, i, d.get(i)));
      }
      table.repaint();
    }
    setActive(true);
  }

  @Override
  public void removeObjects(Integer[] ids) {
    if(ids.length > 0) {
      
      int[] rows = table.getSelectedRows();
      
      Vector<Vector> data = table.getTableModel().getDataVector();
      int size = data.size();
      for(int i=data.size()-1;i>=0;i--) {
        Object object = data.get(i).get(0);
        if(object instanceof Integer && ArrayUtils.contains(ids, object))
          data.remove(i);
        if(object instanceof Integer[]) {
          for(int j=0;j<ids.length;j++) {
            if(ArrayUtils.contains((Integer[])object, ids[j])) {
              object = ArrayUtils.removeElement((Integer[])object, ids[j]);
              ((Vector)data.get(i)).set(0, object);
            }
          }
          if(((Integer[])object).length == 0)
            data.remove(i);
        }
      }
      
      table.repaint();
      if(size == table.getTableModel().getDataVector().size())
        for(int row:rows)
          if(row<table.getRowCount())
            table.addRowSelectionInterval(row, row);
    }
  }

  @Override
  public void addButtonAction() {
    //RemoteSession session = null;
    try {
      //session = ObjectLoader.createSession();
      waitingSelect = true;
      MappingObject object = (MappingObject) ObjectLoader.createEmptyObject(getObjectClass());
      object.setTmp(true);
      if(getTableFilter() != null && !getTableFilter().isEmpty())
        ObjectLoader.createSession(true).toEstablishes(getTableFilter(), object);
      else ObjectLoader.createSession(true).saveObject(object);
      createtedObjects.add(object.getId());
      postAddButton(object);
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  public void print() {
    try {
      table.print();
    }catch(PrinterException ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  protected void initTableEvents() {
    table.getSorter().addRowSorterListener((RowSorterEvent e) -> {
      setCountLabel();
    });

    table.getTableModel().addTableModelListener((TableModelEvent e) -> {
      tableModelDataChanged(e);
      setCountLabel();
    });

    unSelectMenuItem.addActionListener((ActionEvent e) -> {
      table.clearSelection();
    });

    table.addTableSelectionListener((int[] oldSelection, int[] newSelection) -> {
      checkMenu();
      fireChangeSelection(this, getSelectedId());
    });
    
    table.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_DELETE && isEditable()) {
          table.getEditor().cancelCellEditing();
          getRemoveAction().actionPerformed(new ActionEvent(table, 1, "remove"));
        }
      }
    });

    table.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        table.setToolTipText(null);
        int row = table.rowAtPoint(e.getPoint());
        int column = table.columnAtPoint(e.getPoint());
        if(row != -1 && column != -1) {
          String s = getString(row,column);
          if(s != null && !s.equals(""))
            table.setToolTipText(s);
        }
      }
    });

    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if(table.isEnabled()) {
          if(e.getModifiers() == MouseEvent.META_MASK) {
            if(isPopMenuActive()) {
              Map<String,Object> values = new TreeMap<>();
              values.put("ids", getSelectedId());
              try {
                JMenuItem[] scriptItems = ActionMacrosUtil.createMacrosMenu(getObjectClass(), values);
                scriptMenu.removeAll();
                scriptMenu.setEnabled(scriptItems.length > 0);
                if(scriptItems.length > 0) {
                  for(JMenuItem item:scriptItems)
                    scriptMenu.add(item);
                  popMenu.add(scriptMenu);
                }
              }catch(Exception ex) {
                Logger.getLogger(TableEditor.class).warn(ex);
              }
              popMenu.show(table, e.getX(), e.getY());
            }
          }else if(e.getClickCount() == 2) {
            if(isDoubleClickSelectable()) {
              fireSelectObjects(TableEditor.this, getSelectedId());
              dispose();
            }else if(isEditFunction())
              getEditAction().actionPerformed(new ActionEvent(getEditButton(), 0, "edit"));
          }
        }
      }
    });

    print.addActionListener(getPrintAction());

    getScroll().getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
      @Override
      public void adjustmentValueChanged(AdjustmentEvent e) {
        switch(e.getAdjustmentType()) {
          case AdjustmentEvent.TRACK:
            if(e.getAdjustable().getValue() >= startPixel)
              nextLoad();
            reCalculate();
            break;
        }
      }
    });
  }
  
  public String getString(int row, int column) {
    String str = "<html>";
    if(getType() == MappingObject.Type.ARCHIVE)
      str += "<b>АРХИВ</b><br/>";
    if(getType() == MappingObject.Type.PROJECT)
      str += "<b>ПРОЕКТ</b><br/>";
    for(int i=0;i<table.getColumnCount();i++) {
      if(table.findTableColumn(i).getWidth() > 0 && !(table.getValueAt(row, i) instanceof JComponent || table.getValueAt(row, i) instanceof Boolean)) {
        str += (i==column?"<b>":"")+
                table.getColumnName(i)+
                ": "+
                (table.getValueAt(row, i)==null?"":table.getValueAt(row, i))+
                (i==column?"</b>":"")+
                (i==table.getColumnCount()-1?"</html>":"<br/>");
      }
    }
    return str;
  }

  private void reCalculate() {
    rectHeight          = table.getVisibleRect().getHeight();
    rowHeight           = table.getRowHeight();
    visibleRowCount     = (int)rectHeight/rowHeight;
    toLoadRowCount      = visibleRowCount*3;
    beforeStartRowCount = toLoadRowCount;
    rowCount            = table.getRowCount();
    startPixel          = (rowCount-beforeStartRowCount)*rowHeight;
  }

  private void nextLoad() {
  }
  
  @Override
  protected void insertData(List<List> data) {
    synchronized(table.getTableModel().getDataVector()) {
      insertData(data, table.getTableModel().getDataVector().size());
    }
  }

  protected void insertData(List<List> data, int startIndex) {
    DivisionTableModel model = table.getTableModel();
    
    for(int modelRow=0;modelRow<data.size();modelRow++)
      for(int modelColumn=0;modelColumn<data.get(modelRow).size();modelColumn++)
        data.get(modelRow).set(modelColumn, transformData(startIndex+modelRow, modelColumn, data.get(modelRow).get(modelColumn)));
      
    model.getDataVector().addAll(startIndex, data);
    try {
      model.fireTableDataChanged();
    }catch(Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  protected void initTableComponents() {
    table.setColumns(tableColumns);
    table.moveColumn(table.getColumnModel().getColumnIndex("id"),0);
    table.findTableColumn(0).setMinWidth(0);
    table.findTableColumn(0).setMaxWidth(0);
    this.scroll.setViewportView(table);
    this.scroll.getViewport().setBackground(table.getBackground());
    this.scroll.setRowHeader(true);

    getToolBar().addSeparator();
    //getToolBar().add(multilineButton);
    getToolBar().add(print);
    print.setToolTipText("Печать таблицы");
  }

  /**
   * Задаёт действие на кнопку "печать"
   * @param addAction действие
   */
  public void setPrintAction(ActionListener printAction) {
    print.removeActionListener(this.printAction);
    this.printAction = printAction;
    print.addActionListener(this.printAction);
  }

  public ActionListener getPrintAction() {
    return printAction;
  }

  @Override
  public void setDefaultSelected() {
    if(table.getRowCount() > 0)
      table.setRowSelectionInterval(0, 0);
  }

  @Override
  public void clearSelection() {
    table.clearSelection();
  }


  @Override
  public void addSelectedObjects(Integer[] ids) {
    for(Integer id:ids) {
      int index = getRowObject(id);
      if(index >= 0) {
        index = table.convertRowIndexToView(index);
        table.addRowSelectionInterval(index, index);
      }
    }
    if(ids.length > 0)
      fireChangeSelection(this, ids);
  }

  @Override
  protected Hashtable<Class,Integer> getDropSupportedClasses() {
    Hashtable<Class,Integer> hash = new Hashtable<>();
    hash.put(Node.class,DnDConstants.ACTION_COPY);
    hash.put(MarkerNode.class,DnDConstants.ACTION_COPY);
    return hash;
  }

  @Override
  public Boolean okButtonAction() {
    if(getTable().getCellEditor() != null)
      getTable().getCellEditor().stopCellEditing();
    return super.okButtonAction();
  }
  
  public Object filterValue(Object value) {
    return value;
  }
  
  private void tableModelDataChanged(TableModelEvent e) {
    int column = e.getColumn();
    int row = e.getLastRow();
    if(e.getType() == TableModelEvent.UPDATE && row >= 0 && column >= 0 && ArrayUtils.contains(autoEditingColumns, column)) {
      Object value = filterValue(table.getTableModel().getValueAt(row, column));
      Object ids = table.getTableModel().getValueAt(row, 0);
      RemoteSession session = null;
      try {
        session = ObjectLoader.createSession(true);
        session.addEvent(getObjectClass(), "UPDATE", (ids instanceof Integer?new Integer[]{(Integer)ids}:(Integer[])ids));
        session.executeUpdate("UPDATE ["+getObjectClass().getSimpleName()+"] "
                + "SET ["+getObjectClass().getSimpleName()+"("+getDataFields()[column]+")]=? "
                + "WHERE id="+(ids instanceof Integer?"?":"ANY(?)"), value, ids);
      } catch (Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }
}