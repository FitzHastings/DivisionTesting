package division.store;


import bum.editors.EditorGui;
import bum.editors.util.DivisionTarget;
import bum.editors.util.ObjectLoader;
import bum.interfaces.DealPosition;
import bum.interfaces.Equipment;
import bum.interfaces.Factor.FactorType;
import bum.interfaces.Group;
import bum.interfaces.Store;
import division.fx.PropertyMap;
import division.swing.DivisionComboBox;
import division.swing.DivisionScrollPane;
import division.swing.DivisionTable;
import division.swing.DivisionTableRenderer;
import division.swing.DivisionToolButton;
import division.swing.guimessanger.Messanger;
import division.swing.table.CellColorController;
import division.swing.table.groupheader.ColumnGroup;
import division.swing.table.groupheader.ColumnGroupHeader;
import division.util.FileLoader;
import division.util.Utility;
import division.util.yandex.YandexMapDialog;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelEvent;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class nStorePositionTableEditor extends EditorGui {
  private enum ColumnName{идентификатор,Депозитарий,Наименование,ед_изм,Наличие,Резерв,Доступно,Сред_за_ед_,кол_,выдел}
  
  private final JCheckBox groupCheck = new JCheckBox("Групировать по парамметрам", true);
  
  protected DivisionToolButton addToolButton = new DivisionToolButton(FileLoader.getIcon("Add16.gif"),"Добавить");
  protected DivisionToolButton delToolButton = new DivisionToolButton(FileLoader.getIcon("Delete16.gif"),"Удалить");
  
  private final ExecutorService pool = Executors.newSingleThreadExecutor();
  private InitDataTask initDataTask  = null;
  
  private final DivisionTable      table  = new DivisionTable();
  private final DivisionScrollPane scroll = new DivisionScrollPane(table) {
    private Color buttonColor = Color.GRAY;
    private Color borderColor = Color.LIGHT_GRAY;
    
    @Override
    public void paint(Graphics g) {
      super.paint(g);
      
      Graphics2D g2d = (Graphics2D) g;
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
      g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      
      g2d.setColor(buttonColor);
      g2d.fillPolygon(
              new int[]{4, getRowHeader().getWidth()-4, 4}, 
              new int[]{getColumnHeader().getHeight()/4+2, getColumnHeader().getHeight()/2, getColumnHeader().getHeight()*3/4-2}, 3);
      
      g2d.setColor(borderColor);
      g2d.setStroke(new BasicStroke(2.0f));
      g2d.drawPolygon(
              new int[]{4, getRowHeader().getWidth()-4, 4}, 
              new int[]{getColumnHeader().getHeight()/4+2, getColumnHeader().getHeight()/2, getColumnHeader().getHeight()*3/4-2}, 3);
    }
    
    public boolean containsTopLeft(Point point) {
      return new Polygon(
              new int[]{4, getRowHeader().getWidth()-4, 4}, 
              new int[]{getColumnHeader().getHeight()/4+2, getColumnHeader().getHeight()/2, getColumnHeader().getHeight()*3/4-2}, 3).contains(point);
    }

    @Override
    public void pressTopLeft(Point point) {
      if(containsTopLeft(point)) {
        borderColor = Color.GRAY.darker();
        repaint();
      }
    }

    @Override
    public void releasTopLeft(Point point) {
      if(containsTopLeft(point)) {
        borderColor = Color.LIGHT_GRAY;
        repaint();
      }
    }

    @Override
    public void clickTopLeft(Point point) {
      if(containsTopLeft(point)) {
        initFactorsMenu();
        initDispatchParamsMenu();
        columnsMenu.show(scroll, getRowHeader().getWidth(), 0);
      }else super.clickTopLeft(point);
    }
  };
  private final ColumnGroupHeader  header           = new ColumnGroupHeader(table);
  private final JPopupMenu         selectMenu       = new JPopupMenu();
  private final JMenuItem          selectAll        = new JMenuItem("Выделить всё");
  private final JMenuItem          deselectAll      = new JMenuItem("Убрать выделение");
  
  private final JPopupMenu     columnsMenu         = new JPopupMenu();
  private final JMenu          dispatchParams      = new JMenu("Параметры оприходования");
  private final JPanel         dispatchParamsPanel = new JPanel(new GridBagLayout());
  private final JMenu          factors             = new JMenu("Реквизиты");
  private final JPanel         factorsPanel        = new JPanel(new GridBagLayout());
  
  private Integer[] groups;
  private Integer[] stories;
  private Boolean controllIn;
  private Boolean controllOut;
  private Integer[] invisibleEquipment;
  
  private final LinkedHashMap<String, String>        oldColumns       = new LinkedHashMap<>();
  private final LinkedHashMap<Integer, FactorColumn> oldFactorColumns = new LinkedHashMap<>();
  
  private final LinkedHashMap<String, String>        idColumns     = new LinkedHashMap<>();
  private final LinkedHashMap<String, String>        columns       = new LinkedHashMap<>();
  private final LinkedHashMap<Integer, FactorColumn> factorColumns = new LinkedHashMap<>();
  
  private boolean onlyAccessible = false;
  private boolean visibleSelectCountColumn = false;
  private boolean onlyPurchase = false;
  
  private Group.ObjectType defaultStoreObjectType = null;
  private Store.StoreType  defaultStoreType       = null;
  
  
  private final TreeMap<Integer, BigDecimal> selectedPositions = new TreeMap<>();
  
  private DivisionTarget storуTarget = null;
  
  public nStorePositionTableEditor() {
    this(null);
  }
  
  public nStorePositionTableEditor(Integer[] stories) {
    super(null, null);
    this.stories = stories;
    
    initComponents();
    initTargets();
    initEvents();
    
    idColumns.put("id",           "[Equipment(id)]");
    idColumns.put("store-id",     "[Equipment(store)]");
    idColumns.put("group-id",     "[Equipment(group)]");
    idColumns.put("hashcode",     "[Equipment(hashcode)]");
    idColumns.put("factors",      "(SELECT ARRAY(SELECT [Group(factors):target] FROM [Group(factors):table] WHERE [Group(factors):object]=[Equipment(group)]))");
    
    columns.put(ColumnName.идентификатор.toString(),                    "[Equipment(id)]");
    columns.put(ColumnName.Депозитарий.toString(),                      "[Equipment(store-name)]");
    
    columns.put(ColumnName.Наименование.toString(),                     "[Equipment(group_name)]");
    columns.put(ColumnName.ед_изм.toString().replaceAll("_", "."),      "[Equipment(group-unit)]");
    
    columns.put(ColumnName.Наличие.toString(),                          "[Equipment(amount)]");
    columns.put(ColumnName.Резерв.toString(),                           "[Equipment(reserve)]");
    columns.put(ColumnName.Доступно.toString(),                         "[Equipment(amount)] - [Equipment(reserve)]");
    columns.put(ColumnName.Сред_за_ед_.toString().replaceAll("_", "."), "[Equipment(mean-price)]");
    
    columns.put(ColumnName.кол_.toString().replaceAll("_", "."),        "0.0::NUMERIC(10,2)");
    columns.put(ColumnName.выдел.toString(),                            "false");
    
    table.getTableModel().setColumnClassAsValue(true);
  }

  public Integer[] getGroups() {
    return groups;
  }

  public void setGroups(Integer[] groups) {
    this.groups = groups;
  }

  @Override
  public void changeSelection(EditorGui editor, Integer[] ids) {
    this.stories = ids;
    if(stories.length == 1) {
      if(storуTarget != null)
        removeTarget(storуTarget);
      addTarget(storуTarget = new DivisionTarget(Store.class, stories[0]) {
        @Override
        public void messageReceived(String type, Integer[] ids, PropertyMap objectEventProperty) {
          if(type.equals("UPDATE")) {
            controllIn = controllOut = null;
            Object[] d = ObjectLoader.getSingleData(Store.class, stories[0], "controllIn", "controllOut");
            if(d != null) {
              controllIn  = (Boolean) d[0];
              controllOut = (Boolean) d[1];
            }
            addToolButton.setEnabled(stories.length == 1 && controllIn != null && !controllIn);
          }
        }
      });
    }
    initData();
  }
  
  public void setAddFunction(boolean is) {
    addToolButton.setVisible(is);
  }
  
  public void setGroupCheckable(boolean checkable) {
    groupCheck.setVisible(checkable);
    setActive(false);
    groupCheck.setSelected(checkable);
    setActive(true);
  }
  
  public boolean isGroupCheckable() {
    return groupCheck.isVisible();
  }
  
  private int getColumnIndex(ColumnName columnName) {
    return ArrayUtils.indexOf(getColumns(), columnName.toString().replaceAll("_", "."));
  }
  
  private void initComponents() {
    addToolButton.setEnabled(false);
    
    getToolBar().add(addToolButton);
    getToolBar().add(delToolButton);
    getToolBar().addSeparator();
    getToolBar().add(groupCheck);
    
    selectMenu.add(selectAll);
    selectMenu.add(deselectAll);
    
    columnsMenu.add(dispatchParams);
    columnsMenu.add(factors);
    
    JCheckBox count   = createColumnCheckBox("Кол.",        "[Equipment(amount)]");
    JCheckBox oneCost = createColumnCheckBox("Цена за ед.", "NULLTOZERO([Equipment(cost)])");
    JCheckBox sum     = createColumnCheckBox("Всего",       "[Equipment(amount)]*NULLTOZERO([Equipment(cost)])");
    dispatchParamsPanel.add(count,   new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(1,2,1,2), 0, 0));
    dispatchParamsPanel.add(oneCost, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(1,2,1,2), 0, 0));
    dispatchParamsPanel.add(sum,     new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(1,2,1,2), 0, 0));
    
    dispatchParams.add(dispatchParamsPanel);
    factors.add(factorsPanel);
    
    table.setTableHeader(header);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    
    getRootPanel().setLayout(new BorderLayout());
    getRootPanel().add(scroll, BorderLayout.CENTER);
    scroll.setRowHeader(true);
    
    table.setDefaultRenderer(BigDecimal.class, new DivisionTableRenderer() {
      @Override
      public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return table.getCellRenderer().getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
      }
    });
    
    table.setCellEditableController((JTable table1, int modelRow, int modelColumn) -> 
                modelColumn == getColumnIndex(ColumnName.кол_) || 
                    modelColumn == getColumnIndex(ColumnName.выдел) || 
                    (modelColumn-columns.size() >= 0 && 
                            ((StoreId)table.getValueAt(modelRow, 0)).containsFactor((Integer) factorColumns.keySet().toArray()[modelColumn-columns.size()])));
    
    table.setCellColorController(new CellColorController() {
      @Override
      public Color getCellColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        Color b = getRootPanel().getBackground();
        
        if(modelColumn < columns.size()) {
          if(modelColumn == getColumnIndex(ColumnName.кол_) || modelColumn == getColumnIndex(ColumnName.выдел))
            return null;
          else return (isSelect?b.darker():b);
        }else return 
                (modelColumn-columns.size() >= 0 && 
                            ((StoreId)table.getModel().getValueAt(modelRow, 0)).containsFactor((Integer) factorColumns.keySet().toArray()[modelColumn-columns.size()])) 
                ? null:(isSelect?b.darker():b);
      }

      @Override
      public Color getCellTextColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        return null;
      }
    });
    
    table.setCellFontController((JTable table1, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) -> {
      if (modelColumn == getColumnIndex(ColumnName.Резерв) ||
              modelColumn == getColumnIndex(ColumnName.кол_) || 
              modelColumn == getColumnIndex(ColumnName.Доступно) || 
              modelColumn == getColumnIndex(ColumnName.Наличие)) {
        return new Font("Dialog", Font.BOLD, 11);
      }
      return null;
    });
  }

  @Override
  public void initTargets() {
    addTarget(new DivisionTarget(Equipment.class) {
      @Override
      public void messageReceived(final String type, final Integer[] identificators, PropertyMap objectEventProperty) {
        SwingUtilities.invokeLater(() -> {
          if(type.equals("REMOVE"))
            actionRemovePositions(identificators);
          else {
            Integer[] ids = ObjectLoader.isSatisfy(DBFilter.create(Equipment.class).AND_IN("store", stories)
                    .AND_EQUAL("type", Equipment.Type.CURRENT).AND_EQUAL("tmp", false), identificators);
            if(type.equals("CREATE")) {
              actionCreatePositions(ids);
            }else { // type == UPDATE
              Integer[] removeIds = new Integer[0];
              for(Integer id:identificators)
                if(!ArrayUtils.contains(ids, id))
                  removeIds = (Integer[]) ArrayUtils.add(removeIds, id);
              actionRemovePositions(removeIds);
              actionUpdatePositions(ids);
            }
          }
        });
      }
    });
  }
  
  private void initEvents() {
    addToolButton.addActionListener((ActionEvent e) -> {
      addStorePosition();
    });
    
    delToolButton.addActionListener((ActionEvent e) -> {
      delStorePosition();
    });
    
    columnsMenu.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        oldColumns.clear();
        oldColumns.putAll(columns);
        oldFactorColumns.clear();
        oldFactorColumns.putAll(factorColumns);
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        if(!oldColumns.equals(columns) || !oldFactorColumns.equals(factorColumns))
          initData();
        oldColumns.clear();
        oldFactorColumns.clear();
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
      }
    });
    
    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        int row = table.rowAtPoint(e.getPoint());
        int col = table.columnAtPoint(e.getPoint());
        if(e.getClickCount() == 2 && row >= 0 && col >= 0) {
          row = table.convertRowIndexToModel(table.rowAtPoint(e.getPoint()));
          col = table.convertColumnIndexToModel(table.columnAtPoint(e.getPoint()));
          if(row >= 0 && col >= columns.size()) {
            FactorColumn factorColumn = factorColumns.values().toArray(new FactorColumn[0])[col-columns.size()];
            if(factorColumn.getFactorType() == FactorType.адрес) {
              Object value = table.getTableModel().getValueAt(row, col);
              JSONObject address;
              if(value != null && !value.equals("") && value.toString().startsWith("json")) {
                address = YandexMapDialog.getAddress(JSONObject.fromObject(value.toString().substring(4)));
              }else address = YandexMapDialog.getAddress(value==null?"":value.toString());
              if(address != null && 
                      setFactorValue((StoreId) table.getTableModel().getValueAt(row, 0), factorColumn.getId(), "json"+address.toString()))
                table.getTableModel().setValueAt("json"+address.toString(), row, col);
            }
          }
        }
      }
    });
    
    table.getTableModel().addTableModelListener((TableModelEvent e) -> {
      if (isActive() && e.getType() == TableModelEvent.UPDATE && e.getLastRow() >= 0 && e.getColumn() >= 0) {
        StoreId id = (StoreId)table.getTableModel().getValueAt(e.getLastRow(), 0);
        setActive(false);
        Object value = table.getTableModel().getValueAt(e.getLastRow(), e.getColumn());
        if (e.getColumn() >= columns.size()) {
          if(value instanceof DivisionComboBox)
            value = ((DivisionComboBox)value).getSelectedIndex()>0?((DivisionComboBox)value).getSelectedItem():null;
          FactorColumn factorColumn = factorColumns.values().toArray(new FactorColumn[0])[e.getColumn()-columns.size()];
          setFactorValue(id, factorColumn.getId(), value);
        } else if (e.getColumn() == getColumnIndex(ColumnName.кол_)) {
          BigDecimal count    = (BigDecimal) value;
          selectCount(e.getLastRow(), id, count);
          table.getTableModel().setValueAt(((BigDecimal)table.getTableModel().getValueAt(e.getLastRow(), e.getColumn())).compareTo(BigDecimal.ZERO) > 0, e.getLastRow(), getColumnIndex(ColumnName.выдел));
        }else if(e.getColumn() == getColumnIndex(ColumnName.выдел)) {
          BigDecimal dostupno = (BigDecimal) table.getTableModel().getValueAt(e.getLastRow(), getColumnIndex(ColumnName.Доступно));
          if((Boolean)value)
            selectCount(e.getLastRow(), id, dostupno);
          else selectCount(e.getLastRow(), id, BigDecimal.ZERO);
        }
        setActive(true);
      }
    });
    
    groupCheck.addItemListener(e -> {initData();});
  }
  
  private void actionRemovePositions(Integer[] ids) {
    SwingUtilities.invokeLater(() -> {
      final Vector<Vector> dataVector = table.getTableModel().getDataVector();
      for(int i=dataVector.size()-1;i>=0;i--) {
        StoreId storeId = (StoreId) dataVector.get(i).get(0);
        for(Integer id:ids) {
          if(storeId.contains(id)) {
            if(storeId.getIds().length == 1)
              dataVector.remove(i);
            else storeId.removeId(id);
            break;
          }
        }
      }
      table.getTableModel().fireTableDataChanged();
    });
    /*final Vector<Vector> removedRows = new Vector<>();
    Arrays.stream(ids).forEach(id -> {
      dataVector.stream().filter(row -> ((StoreId) row.get(0)).contains(id)).forEach(row -> {
        if(((StoreId) row.get(0)).getIds().length == 1)
          removedRows.add(row);
        else ((StoreId) row.get(0)).removeId(id);
      });
    });
    removedRows.stream().forEach(row -> dataVector.removeElement(row));*/
  }
  
  private void actionCreatePositions(Integer[] ids) {
    initData(ids);
  }
  
  private void actionUpdatePositions(Integer[] ids) {
    initData(ids);
  }
  
  private void selectCount(int row, StoreId id, BigDecimal count) {
    BigDecimal oldCount = selectedPositions.get(id.getIds()[0]);
    if(controllOut) {
      BigDecimal reserve  = (BigDecimal) table.getTableModel().getValueAt(row, getColumnIndex(ColumnName.Резерв));
      BigDecimal dostupno = (BigDecimal) table.getTableModel().getValueAt(row, getColumnIndex(ColumnName.Доступно));
      if(count.compareTo(BigDecimal.ZERO) > 0 && count.compareTo(dostupno) <= 0) {
        // Введён НЕ 0
        if (oldCount == null || oldCount.compareTo(count) != 0) {
          // Старого значения нет или оно не равно новому
          if(count.compareTo(dostupno) > 0)
            count = dostupno;
          table.getTableModel().setValueAt(reserve.add(count), row, getColumnIndex(ColumnName.Резерв));
          table.getTableModel().setValueAt(dostupno.subtract(count), row, getColumnIndex(ColumnName.Доступно));
          table.getTableModel().setValueAt(count, row, getColumnIndex(ColumnName.кол_));
          selectedPositions.put(id.getIds()[0], count);
        }
      }else {
        if(count.compareTo(BigDecimal.ZERO) != 0 && oldCount != null) {
          count = oldCount;
        } else {
          count = BigDecimal.ZERO;
          table.getTableModel().setValueAt(id.getReserve(),  row, getColumnIndex(ColumnName.Резерв));
          table.getTableModel().setValueAt(id.getDostupno(), row, getColumnIndex(ColumnName.Доступно));
          selectedPositions.remove(id.getIds()[0]);
        }
        table.getTableModel().setValueAt(count, row, getColumnIndex(ColumnName.кол_));
      }
    }else {
      if (count.compareTo(BigDecimal.ZERO) <= 0) {
        if (count.compareTo(BigDecimal.ZERO) != 0 && oldCount != null)
          count = oldCount;
        else {
          count = BigDecimal.ZERO;
          selectedPositions.remove(id.getIds()[0]);
        }
      }else selectedPositions.put(id.getIds()[0], count);
      table.getTableModel().setValueAt(count, row, getColumnIndex(ColumnName.кол_));
      table.getTableModel().setValueAt(id.getReserve().add(count), row, getColumnIndex(ColumnName.Резерв));
    }
  }

  public Group.ObjectType getDefaultStoreObjectType() {
    return defaultStoreObjectType;
  }

  public void setDefaultStoreObjectType(Group.ObjectType defaultStoreObjectType) {
    this.defaultStoreObjectType = defaultStoreObjectType;
  }

  public Store.StoreType getDefaultStoreType() {
    return defaultStoreType;
  }

  public void setDefaultStoreType(Store.StoreType defaultStoreType) {
    this.defaultStoreType = defaultStoreType;
  }

  public DivisionTable getTable() {
    return table;
  }

  public TreeMap<Integer, BigDecimal> getSelectedPositions() {
    return selectedPositions;
  }
  
  private JCheckBox createFactorColumnCheckBox(Integer id, String name, FactorType factorType, String listValues, boolean productFactor) {
    final JCheckBox box = createMenuCheckBox(name);
    box.setSelected(factorColumns.containsKey(id));
    box.addItemListener(e -> {
      if(e.getStateChange() == ItemEvent.SELECTED)
        factorColumns.put(id, new FactorColumn(id, name, factorType, listValues, productFactor));
      else factorColumns.remove(id);
    });
    return box;
  }
  
  private JCheckBox createColumnCheckBox(final String name, final String columnName) {
    final JCheckBox box = createMenuCheckBox(name);
    box.setSelected(columns.containsKey(name));
    box.addItemListener(e -> {
      if(e.getStateChange() == ItemEvent.SELECTED)
        columns.put(name, columnName);
      else columns.remove(name);
    });
    return box;
  }
  
  private JCheckBox createMenuCheckBox(final String name) {
    final JCheckBox box = new JCheckBox(name);
    box.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        box.grabFocus();
      }

      @Override
      public void mouseExited(MouseEvent e) {
        ((JComponent)box.getParent()).grabFocus();
      }
    });
    return box;
  }
  
  private void initFactorsMenu() {
    columnsMenu.remove(factors);
    factorsPanel.removeAll();
    List<List> data = ObjectLoader.executeQuery("SELECT "
            + "[Factor(id)], "
            + "[Factor(name)] ,"
            + "[Factor(factorType)], "
            + "[Factor(listValues)] "
            + "FROM [Factor] "
            + "WHERE [Factor(productFactor)]=false AND "
            + "[Factor(id)] IN (SELECT DISTINCT [Factor(groups):object] FROM [Factor(groups):table] "
            + "WHERE [Factor(groups):target] IN (SELECT [Equipment(group)] FROM [Equipment] WHERE [Equipment(id)]=ANY(?)))", 
            new Object[]{getVisibleIds()});
    
    if(!data.isEmpty())
      columnsMenu.add(factors);
    for(int i=0;i<data.size();i++)
      factorsPanel.add(
              createFactorColumnCheckBox((Integer) data.get(i).get(0), 
                      (String) data.get(i).get(1), 
                      FactorType.valueOf((String) data.get(i).get(2)), 
                      (String) data.get(i).get(3), false), 
              new GridBagConstraints(0, i, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(1,2,1,2), 0, 0));
  }

  private void initDispatchParamsMenu() {
  }
  
  public Integer[] getVisibleIds() {
    Integer[] ids = new Integer[0];
    for(int i=0;i<table.getRowCount();i++)
      ids = (Integer[]) ArrayUtils.addAll(ids, ((StoreId)table.getValueAt(i, 0)).getIds());
    return ids;
  }
  
  public Integer[] getAllIds() {
    Integer[] ids = new Integer[0];
    for(Vector d:(Vector<Vector>) table.getTableModel().getDataVector())
      ids = (Integer[]) ArrayUtils.addAll(ids, ((StoreId)d.get(0)).getIds());
    return ids;
  }

  public Integer[] getInvisibleEquipment() {
    return invisibleEquipment;
  }

  public void setInvisibleEquipment(Integer[] invisibleEquipment) {
    this.invisibleEquipment = invisibleEquipment;
  }

  public boolean isOnlyAccessible() {
    return onlyAccessible;
  }

  public void setOnlyAccessible(boolean onlyAccessible) {
    this.onlyAccessible = onlyAccessible;
  }

  public boolean isOnlyPurchase() {
    return onlyPurchase;
  }

  public void setOnlyPurchase(boolean onlyPurchase) {
    this.onlyPurchase = onlyPurchase;
  }

  public boolean isVisibleSelectCountColumn() {
    return visibleSelectCountColumn;
  }

  public void setVisibleSelectCountColumn(boolean visibleSelectCountColumn) {
    this.visibleSelectCountColumn = visibleSelectCountColumn;
  }
  
  public Integer[] getSelectedId() {
    Integer[] ids = new Integer[0];
    int[] rows = table.getSelectedRows();
    for(int i=0;i<rows.length;i++)
      ids = (Integer[]) ArrayUtils.addAll(ids, ((StoreId)table.getValueAt(rows[i], 0)).getIds());
    return ids;
  }

  @Override
  public void closeDialog() {
    selectedPositions.clear();
    table.clearSelection();
    super.closeDialog();
  }
  
  public TreeMap<Integer, BigDecimal> get() {
    JDialog d = createDialog();
    d.setModal(true);
    d.setVisible(true);
    return selectedPositions;
  }

  @Override
  public void clearData() {
    table.getTableModel().clear();
  }

  @Override
  public void initData() {
    initData(null);
  }
  
  public void initData(Integer[] ids) {
    if(initDataTask != null) {
      initDataTask.setShutDown(true);
      initDataTask = null;
    }
    if(isActive() && stories != null) {
      pool.submit(initDataTask = new InitDataTask(ids));
    }
  }
  
  @Override
  public Boolean okButtonAction() {
    fireSelectObjects(this, getSelectedId());
    dispose();
    return true;
  }
  
  public Object[] getColumns() {
    return ArrayUtils.addAll(columns.keySet().toArray(), factorColumns.values().toArray());
  }
  
  private void setTableColumns() {
    header.clear();
    table.setColumns(getColumns());
    table.ResetFilters();

    ColumnGroup objectColumn = new ColumnGroup("Объект");
    objectColumn.add(table.findTableColumn(getColumnIndex(ColumnName.Наименование)));
    objectColumn.add(table.findTableColumn(getColumnIndex(ColumnName.ед_изм)));
    header.addColumnGroup(objectColumn);

    ColumnGroup currentStateColumn = new ColumnGroup("Текущее состояние");
    currentStateColumn.add(table.findTableColumn(getColumnIndex(ColumnName.Наличие)));
    currentStateColumn.add(table.findTableColumn(getColumnIndex(ColumnName.Резерв)));
    currentStateColumn.add(table.findTableColumn(getColumnIndex(ColumnName.Доступно)));
    currentStateColumn.add(table.findTableColumn(getColumnIndex(ColumnName.Сред_за_ед_)));
    header.addColumnGroup(currentStateColumn);
    
    ColumnGroup select = new ColumnGroup("Отобрать");
    select.add(table.findTableColumn(getColumnIndex(ColumnName.кол_)));
    select.add(table.findTableColumn(getColumnIndex(ColumnName.выдел)));
    header.addColumnGroup(select);

    if(columns.size() > 10) {
      ColumnGroup dispatchColumn = new ColumnGroup("Параметры оприходования");
      for(int i=10;i<columns.size();i++)
        dispatchColumn.add(table.findTableColumn(i));
      header.addColumnGroup(dispatchColumn);
    }

    int[] zeroColumns = new int[]{0};
    if(!visibleSelectCountColumn) {
      zeroColumns = ArrayUtils.add(zeroColumns, getColumnIndex(ColumnName.кол_));
      zeroColumns = ArrayUtils.add(zeroColumns, getColumnIndex(ColumnName.выдел));
    }

    if(!factorColumns.isEmpty()) {
      ColumnGroup factorColumn = new ColumnGroup("Реквизиты");
      Integer[] keys = factorColumns.keySet().toArray(new Integer[0]);
      for(int i=0;i<keys.length;i++) {
        factorColumn.add(table.findTableColumn(i+columns.size()));
        switch(factorColumns.get(keys[i]).getFactorType()) {
          case адрес : 
            table.getTableFilters().addTextFilter(i+columns.size());
            break;
          case дата : 
            table.getTableFilters().addDateFilter(i+columns.size());
            break;
          case текст : 
            table.getTableFilters().addTextFilter(i+columns.size());
            break;
          case число : 
            table.getTableFilters().addNumberFilter(i+columns.size());
            break;
        }
      }
      header.addColumnGroup(factorColumn);
    }

    header.revalidate();

    if(table.getTableFilters().getFilterComponent(getColumnIndex(ColumnName.Депозитарий)) == null)
      table.getTableFilters().addListFilter(getColumnIndex(ColumnName.Депозитарий));
    if(table.getTableFilters().getFilterComponent(getColumnIndex(ColumnName.Наименование)) == null)
      table.getTableFilters().addListFilter(getColumnIndex(ColumnName.Наименование));

    table.getTableFilters().revalidate();

    table.setColumnWidthZero(zeroColumns);
  }
  
  class InitDataTask implements Runnable {
    private boolean shutDown = false;
    private Integer[] ids;

    public InitDataTask(Integer[] ids) {
      this.ids = ids;
    }

    public boolean isShutDown() {
      return shutDown;
    }

    public void setShutDown(boolean shutDown) {
      this.shutDown = shutDown;
    }
    
    @Override
    public void run() {
      waitCursor();
      final List<List> data = new Vector<>();
      try {
        ObjectLoader.executeQuery("SELECT "
                + "[Group(identificator)], "
                + "[Group(iden_name)] ,"
                + "[Group(iden_factorType)], "
                + "[Group(iden_listValue)] "
                + "FROM [Group] "
                + "WHERE [Group(id)] "
                +(groups != null ? "=ANY(?)" : (ids != null ? "IN (SELECT DISTINCT [Equipment(group)] FROM [Equipment] WHERE id=ANY(?))" : 
                        " IN (SELECT DISTINCT [Equipment(group)] FROM [Equipment] WHERE [Equipment(store)]=ANY(?))")), true,
                groups != null ? new Object[]{groups} : ids != null ? new Object[]{ids} : new Object[]{stories})
                .stream().filter(d -> d.get(0) != null).forEach (d -> {
                  factorColumns.put((Integer)d.get(0), 
                        new FactorColumn((Integer)d.get(0), (String)d.get(1), FactorType.valueOf((String)d.get(2)), (String)d.get(3), false));
                });
        
        String factorQuery = "";
        if(!factorColumns.isEmpty()) {
          factorQuery = factorColumns.keySet().stream().map((id) -> ","
                  + "(SELECT name FROM [EquipmentFactorValue] "
                  + "WHERE [EquipmentFactorValue(equipment)]=[Equipment(id)] AND [EquipmentFactorValue(factor)]="+id+")").reduce(factorQuery, String::concat);
        }
        
        //Получаем контроль прихода и расхода
        controllIn = controllOut = null;
        if(stories.length == 1) {
          data.addAll(ObjectLoader.executeQuery("SELECT "
                  + "[Store(storeType)], "
                  + "[Store(objectType)],"
                  + "[Store(controllIn)],"
                  + "[Store(controllOut)]"
                  + " FROM [Store] WHERE id=ANY(?)", true, new Object[]{stories}));
          if(!data.isEmpty()) {
            controllIn  = (boolean) data.get(0).get(2);
            controllOut = (boolean) data.get(0).get(3);
          }
          data.clear();
        }
        
        Object[] param = new Object[0];
        if(ids != null)
          param = ArrayUtils.add(param, ids);
        else param = ArrayUtils.add(param, stories);
        if(groups != null)
          param = ArrayUtils.add(param, groups);
        
        data.addAll(ObjectLoader.executeQuery("SELECT "
                + Utility.join(idColumns.values().toArray(), ",")+","
                + Utility.join(columns.values().toArray(), ",")
                + factorQuery+" "
                + " FROM [Equipment]"
                + " WHERE "
                + (ids == null ? "[Equipment(store)]=ANY(?)" : "[Equipment(id)]=ANY(?)")
                + (groups != null ? " AND [Equipment(group)]=ANY(?)" : "")
                + " ORDER BY [Equipment(store)], id", true, 
                param));
        
      }catch (Exception ex) {
        Messanger.showErrorMessage(ex);
      }finally {
        SwingUtilities.invokeLater(() -> {
          addToolButton.setEnabled(stories.length == 1 && controllIn != null && !controllIn);
          if(ids == null) {
            clear();
            setTableColumns();
          }

          Vector<Vector> dataVector = table.getTableModel().getDataVector();
          for(List d:data) {
            Vector updateRow = null;
            if(groupCheck.isSelected()) {
              //Поиск по хэшкоду
              for(Vector row:dataVector) {
                StoreId storeId = (StoreId) row.get(0);
                if(storeId.contains((String) d.get(3))) {
                  updateRow = row;
                  break;
                }
              }
            }else if(ids != null) {
              //Поиск по id
              for(Vector row:dataVector) {
                StoreId storeId = (StoreId) row.get(0);
                if(storeId.contains((Integer) d.get(0))) {
                  updateRow = row;
                  break;
                }
              }
            }
            if(updateRow == null) {// не нашли
              Vector nrow = new Vector(d.subList(idColumns.size(), d.size()));
              nrow.set(0, new StoreId(
                      (Integer)    d.get(0),
                      (Integer)    d.get(1),
                      (Integer)    d.get(2),
                      (String)     d.get(3),
                      (Integer[])  d.get(4),
                      (BigDecimal) d.get(idColumns.size()+getColumnIndex(ColumnName.Резерв)),
                      (BigDecimal) d.get(idColumns.size()+getColumnIndex(ColumnName.Доступно))
              ));
              dataVector.add(nrow);
            }else {// нашли
              if(groupCheck.isSelected()) {// включена группировка по парамметрам
                ((StoreId)updateRow.get(0)).addId((Integer)d.get(0));
                updateRow.set(getColumnIndex(ColumnName.Наличие),  ((BigDecimal)updateRow.get(getColumnIndex(ColumnName.Наличие))).add((BigDecimal)d.get(idColumns.size()+getColumnIndex(ColumnName.Наличие))));
                updateRow.set(getColumnIndex(ColumnName.Резерв),   ((BigDecimal)updateRow.get(getColumnIndex(ColumnName.Резерв))).add((BigDecimal)d.get(idColumns.size()+getColumnIndex(ColumnName.Резерв))));
                updateRow.set(getColumnIndex(ColumnName.Доступно), ((BigDecimal)updateRow.get(getColumnIndex(ColumnName.Доступно))).add((BigDecimal)d.get(idColumns.size()+getColumnIndex(ColumnName.Доступно))));
              }else if(ids != null) {
                updateRow = new Vector(d);
              }
            }
          }

          if(!factorColumns.isEmpty()) {
            dataVector.stream().forEach(d -> {
              for(int i=0;i<factorColumns.size();i++) {
                FactorColumn factorColumn = (FactorColumn)factorColumns.values().toArray()[i];
                if(factorColumn.getListValues() != null && !factorColumn.getListValues().equals("")) {
                  d.set(i+columns.size(), createComboBox(factorColumn.getListValues().split(";"), d.get(i+columns.size())));
                }
              }
            });
          }

          table.getTableModel().fireTableDataChanged();
          defaultCursor();
        });
      }
    }
  }
  
  private DivisionComboBox createComboBox(Object[] items, Object selectedItem) {
    final DivisionComboBox combobox = new DivisionComboBox(items);
    if(selectedItem != null)
      combobox.setSelectedItem(selectedItem);
    combobox.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        if(table.getCellEditor() != null)
          table.getCellEditor().stopCellEditing();
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
      }
    });
    return combobox;
  }
  
  private boolean setFactorValue(StoreId id, Integer factorId, Object value) {
    boolean ok = true;
    RemoteSession session = null;
    try {
      session = ObjectLoader.createSession();
      session.executeUpdate("DELETE FROM [EquipmentFactorValue] WHERE [EquipmentFactorValue(equipment)]=ANY(?) AND [EquipmentFactorValue(factor)]=?",
              new Object[]{id.getIds(), factorId});
      boolean error = false;
      if(value != null) {
        for(Integer equipmentId:id.getIds()) {
          if(session.executeUpdate("INSERT INTO [EquipmentFactorValue] ([EquipmentFactorValue(equipment)], [EquipmentFactorValue(factor)], [EquipmentFactorValue(name)]) VALUES (?,?,?)", 
                  new Object[]{equipmentId, factorId, value}) != 1) {
            error = true;
            break;
          }
        }
      }
      if(error) {
        ObjectLoader.rollBackSession(session);
        throw new Exception();
      }else
        ObjectLoader.commitSession(session);
    } catch (Exception ex) {
      ok = false;
      ObjectLoader.rollBackSession(session);
      Messanger.showErrorMessage(ex);
    }
    return ok;
  }
  
  private void addStorePosition() {
    if(stories.length == 1 && controllIn != null && !controllIn) {
      nAddStorePositionDialog groupTableEditor = new nAddStorePositionDialog(stories[0], false);
      groupTableEditor.setAutoLoadAndStore(true);
      groupTableEditor.initData();
      groupTableEditor.createDialog(true).setVisible(true);
    }
  }
  
  private void delStorePosition() {
    Integer[] ids = getSelectedId();
    if(ids.length > 0 && controllOut != null && !controllOut) {
      try {
        if(ObjectLoader.getData(DBFilter.create(DealPosition.class).AND_IN("equipment", ids), true, "id").isEmpty()) {
          if(JOptionPane.showConfirmDialog(null, "Удалить выбранные объекты?", "Удаление...", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == 0)
            ObjectLoader.removeObjects(Equipment.class, true, ids);
        }else Messanger.alert("Данны"+(ids.length==1?"й":"е")+" объект"+(ids.length==1?"":"ы")+" учавствует в сделках", JOptionPane.ERROR_MESSAGE);
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }
}