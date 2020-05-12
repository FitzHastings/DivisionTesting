package division.store;


import bum.editors.EditorGui;
import bum.editors.EditorListener;
import bum.editors.util.DivisionTarget;
import bum.editors.util.ObjectLoader;
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
import java.util.ArrayList;
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

public class StorePositionTableEditor extends EditorGui {
  private enum ColumnName{идентификатор,Депозитарий,Наименование,ед_изм,Наличие,Резерв,Доступно,Сред_за_ед_,кол_,выдел}
  
  private final JCheckBox groupCheck = new JCheckBox("Групировать по парамметрам", true);
  
  protected DivisionToolButton addToolButton = new DivisionToolButton(FileLoader.getIcon("Add16.gif"),"Добавить");
  
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
  
  private Integer ownerPartition;
  private Integer customerPartition;
  private boolean remainderControll;
  private Integer[] groups;
  private Integer[] invisibleEquipment;
  
  private final LinkedHashMap<String, String>        oldColumns       = new LinkedHashMap<>();
  private final LinkedHashMap<Integer, FactorColumn> oldFactorColumns = new LinkedHashMap<>();
  
  private final LinkedHashMap<String, String>        idColumns     = new LinkedHashMap<>();
  private final LinkedHashMap<String, String>        columns       = new LinkedHashMap<>();
  private final LinkedHashMap<Integer, FactorColumn> factorColumns = new LinkedHashMap<>();
  private final LinkedHashMap<Integer,Integer[]>     editableCells = new LinkedHashMap<>();
  
  private final ArrayList<EditorListener> tableEditorListeners = new ArrayList<>();
  
  private boolean onlyAccessible = false;
  private boolean visibleSelectCountColumn = false;
  private boolean onlyPurchase = false;
  
  private Group.ObjectType defaultStoreObjectType = null;
  private Store.StoreType  defaultStoreType       = null;
  
  
  private final TreeMap<Integer, BigDecimal> selectedPositions = new TreeMap<>();
  
  public StorePositionTableEditor(Integer companyPartition) {
    this(companyPartition, null);
  }
  
  public StorePositionTableEditor() {
    this(null, null);
  }
  
  public StorePositionTableEditor(Integer ownerPartition, Integer customerPartition) {
    super(null, null);
    initComponents();
    initTargets();
    initEvents();
    
    idColumns.put("id",           "[Equipment(id)]");
    idColumns.put("store-id",     "[Equipment(store)]");
    idColumns.put("group-id",     "[Equipment(group)]");
    idColumns.put("exist",        "[Equipment(amount)]>=0");
    idColumns.put("hashcode",     "[Equipment(hashcode)]");
    
    columns.put(ColumnName.идентификатор.toString(),                    "[Equipment(id)]");
    columns.put(ColumnName.Депозитарий.toString(),                      "[Equipment(object-type)]||' - '||[Equipment(store-type)]||' - '||[Equipment(store-name)]");
    
    columns.put(ColumnName.Наименование.toString(),                     "[Equipment(group_name)]");
    columns.put(ColumnName.ед_изм.toString().replaceAll("_", "."),      "[Equipment(group-unit)]");
    
    columns.put(ColumnName.Наличие.toString(),                          "[Equipment(amount)]");
    columns.put(ColumnName.Резерв.toString(),                           "[Equipment(reserve)]");
    columns.put(ColumnName.Доступно.toString(),                         "[Equipment(amount)] - [Equipment(reserve)]");
    //columns.put(ColumnName.Доступно.toString(),                         "CASE WHEN [Equipment(amount)] >= [Equipment(reserve)] THEN [Equipment(amount)] - [Equipment(reserve)] ELSE 0 END");
    //columns.put(ColumnName.Дефицит.toString(),                          "CASE WHEN [Equipment(amount)] >= [Equipment(reserve)] THEN 0 ELSE [Equipment(reserve)] - [Equipment(amount)] END");
    columns.put(ColumnName.Сред_за_ед_.toString().replaceAll("_", "."), "[Equipment(mean-price)]");
    
    columns.put(ColumnName.кол_.toString().replaceAll("_", "."),        "0.0::NUMERIC(10,2)");
    columns.put(ColumnName.выдел.toString(),                            "false");
    
    table.getTableModel().setColumnClassAsValue(true);
    
    setSellerPartition(ownerPartition);
    setCustomerPartition(customerPartition);
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
    getToolBar().add(addToolButton);
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
        //if((column == getColumnIndex(ColumnName.Доступно) || column == getColumnIndex(ColumnName.Наличие)) && !remainderControll)
          //value = "Неограничено";
        return table.getCellRenderer().getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
      }
    });
    
    table.setCellEditableController((JTable table1, int modelRow, int modelColumn) -> 
            modelColumn == getColumnIndex(ColumnName.кол_) || 
                    modelColumn == getColumnIndex(ColumnName.выдел) || 
                    ArrayUtils.contains(editableCells.get(modelRow), modelColumn));
    
    table.setCellColorController(new CellColorController() {
      @Override
      public Color getCellColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        Color b = getRootPanel().getBackground();
        
        if(modelColumn < columns.size()) {
          if(modelColumn == getColumnIndex(ColumnName.кол_) || modelColumn == getColumnIndex(ColumnName.выдел))
            return null;
          else return (isSelect?b.darker():b);
        }else return ArrayUtils.contains(editableCells.get(modelRow), modelColumn) || 
                factorColumns.values().toArray(new FactorColumn[0])[modelColumn-columns.size()].getFactorType() == FactorType.адрес
                ?null:(isSelect?b.darker():b);
      }

      /*@Override
      public Color getCellTextColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        if(!((StoreId)table.getModel().getValueAt(modelRow, 0)).isExist() && modelColumn != getColumnIndex(ColumnName.кол_))
          return isSelect?Color.RED.darker():Color.RED;
        return null;
      }*/
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
          Integer[] ids = identificators;
          Integer[] ids_ = new Integer[0];
          for (int i = table.getTableModel().getRowCount()-1; i>=0; i--) {
            for (Integer id1 : ids) {
              if (((StoreId)table.getTableModel().getValueAt(i, 0)).contains(id1)) {
                ids_ = (Integer[]) ArrayUtils.addAll(ids_, ((StoreId)table.getTableModel().getValueAt(i, 0)).getIds());
                table.getTableModel().removeRow(i);
                break;
              }
            }
          }
          ids = ids_.length==0?identificators:ids_;
          if (type.intern() == "UPDATE".intern() || type.intern() == "CREATE".intern()) {
            String factorQuery = "";
            if (!factorColumns.isEmpty()) {
              factorQuery = factorColumns.keySet().stream().map((java.lang.Integer id2) -> ","
                      + "(SELECT COUNT([Group(factors):object]) FROM [Group(factors):table] WHERE [Group(factors):object]=[Equipment(group)] AND [Group(factors):target]=" + id2 + ")," + "(SELECT name FROM [EquipmentFactorValue] " + "WHERE [EquipmentFactorValue(equipment)]=[Equipment(id)] AND [EquipmentFactorValue(factor)]=" + id2 + ")").reduce(factorQuery, String::concat);
            }
            try {
              List<List> data = ObjectLoader.executeQuery("SELECT [CompanyPartition(remainderControll)] FROM [CompanyPartition] WHERE id="+getOwnerPartition(), true);
              if(!data.isEmpty())
                remainderControll = (boolean) data.get(0).get(0);
              data = ObjectLoader.executeQuery(
                      "SELECT "
                              + Utility.join(idColumns.values().toArray(), ",")+","
                              + Utility.join(columns.values().toArray(), ",")
                              + factorQuery+" "
                              + " FROM [Equipment] "
                              + "WHERE id=ANY(?)", true, new Object[]{ids});
              
              if(groupCheck.isSelected()) {
                TreeMap<String, Vector> ndata = new TreeMap<>();
                for(int i=data.size()-1;i>=0;i--) {
                  Vector d = ndata.get((String) data.get(i).get(4));
                  if(d == null) {
                    d = new Vector(data.get(i).subList(idColumns.size(), data.get(i).size()));
                    d.set(0, new StoreId(
                            (Integer)data.get(i).get(0),
                            (Integer)data.get(i).get(1),
                            (Integer)data.get(i).get(2),
                            //(boolean)data.get(i).get(3),
                            (String) data.get(i).get(4),
                            null,
                            (BigDecimal)data.get(i).get(getColumnIndex(ColumnName.Резерв)+idColumns.size()),
                            (BigDecimal)data.get(i).get(getColumnIndex(ColumnName.Доступно)+idColumns.size())/*,
                            (BigDecimal)data.get(i).get(getColumnIndex(ColumnName.Наличие)+idColumns.size())*/));
                    ndata.put((String) data.get(i).get(4), d);
                  }else {
                    ((StoreId)d.get(0)).addId((Integer)data.get(i).get(0));
                    d.set(getColumnIndex(ColumnName.Резерв), ((BigDecimal)d.get(getColumnIndex(ColumnName.Резерв))).add((BigDecimal)data.get(i).get(idColumns.size()+getColumnIndex(ColumnName.Резерв))));
                    d.set(getColumnIndex(ColumnName.Доступно), ((BigDecimal)d.get(getColumnIndex(ColumnName.Доступно))).add((BigDecimal)data.get(i).get(idColumns.size()+getColumnIndex(ColumnName.Доступно))));
                  }
                }
                data.clear();
                data.addAll(ndata.values());
              }else {
                for(int i=data.size()-1;i>=0;i--) {
                  Vector d = new Vector(data.get(i).subList(idColumns.size(), data.get(i).size()));
                  d.set(0, new StoreId(
                          (Integer)data.get(i).get(0),
                          (Integer)data.get(i).get(1),
                          (Integer)data.get(i).get(2),
                          //(boolean)data.get(i).get(3),
                          (String)data.get(i).get(4),
                          null,
                          (BigDecimal)data.get(i).get(getColumnIndex(ColumnName.Резерв)+idColumns.size()),
                          (BigDecimal)data.get(i).get(getColumnIndex(ColumnName.Доступно)+idColumns.size())/*,
                          (BigDecimal)data.get(i).get(getColumnIndex(ColumnName.Наличие)+idColumns.size())*/));
                  data.set(i, d);
                }
              }
              
              if(!factorColumns.isEmpty()) {
                for(int row=0;row<data.size();row++) {
                  List d = data.get(row);
                  for(int i=d.size()-1;i>=columns.size();i=i-2) {
                    if(Integer.parseInt(d.get(i-1).toString()) > 0) {
                      int column = columns.size()+(i-columns.size())/2;
                      FactorColumn factorColumn = factorColumns.values().toArray(new FactorColumn[0])[column-columns.size()];
                      if(factorColumn.getListValues() != null && !factorColumn.getListValues().equals("")) {
                        DivisionComboBox combobox = createComboBox(
                                ArrayUtils.add(factorColumn.getListValues().split(";"), 0, "------"),
                                d.get(i));
                        d.set(i, combobox);
                      }
                      if(factorColumn.getFactorType() != FactorType.адрес) {
                        Integer[] cols = editableCells.get(row);
                        if(cols == null)
                          cols = new Integer[0];
                        editableCells.put(row, (Integer[])ArrayUtils.add(cols, column));
                      }
                    }
                    d.remove(i-1);
                  }
                }
              }
              
              if(!selectedPositions.isEmpty()) {
                for(List d:data) {
                  BigDecimal count = selectedPositions.get(((StoreId)d.get(0)).getIds()[0]);
                  if(count != null && count.compareTo(BigDecimal.ZERO) > 0)
                    d.set(getColumnIndex(ColumnName.кол_), count);
                }
              }
              
              table.getTableModel().getDataVector().addAll(data);
              table.getTableModel().fireTableDataChanged();
            }catch (Exception ex) {
              Messanger.showErrorMessage(ex);
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
  
  private void selectCount(int row, StoreId id, BigDecimal count) {
    BigDecimal oldCount = selectedPositions.get(id.getIds()[0]);
    if (remainderControll) {
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

  public Integer getOwnerPartition() {
    return ownerPartition;
  }

  public void setSellerPartition(Integer companyPartition) {
    this.ownerPartition = companyPartition;
  }

  public Integer getCustomerPartition() {
    return customerPartition;
  }

  public void setCustomerPartition(Integer customerPartition) {
    this.customerPartition = customerPartition;
  }

  public Integer[] getGroups() {
    return groups;
  }

  public void setGroups(Integer[] groups) {
    this.groups = groups;
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
    if(initDataTask != null) {
      initDataTask.setShutDown(true);
      initDataTask = null;
    }
    if(isActive() && getOwnerPartition() != null) {
      pool.submit(initDataTask = new InitDataTask());
    }
  }
  
  @Override
  public Boolean okButtonAction() {
    fireSelectObjects(this, getSelectedId());
    /*if(!selectedPositions.isEmpty()) {
      Integer[] updateIds = new Integer[0];
      String[] querys = new String[0];
      Object[][] params = new Object[0][];
      for(Integer id:selectedPositions.keySet()) {
        for(Vector d:(Vector<Vector>)table.getTableModel().getDataVector()) {
          if(!((StoreId)d.get(0)).isExist() && ((StoreId)d.get(0)).contains(id)) {
            updateIds = (Integer[]) ArrayUtils.add(updateIds, id);
            querys = (String[]) ArrayUtils.add(querys, "UPDATE [Equipment] SET [Equipment(amount)]=? WHERE id="+id);
            params = (Object[][]) ArrayUtils.add(params, new Object[]{((BigDecimal)d.get(getColumnIndex(ColumnName.Дефицит))).negate()});
          }
        }
      }
      if(querys.length > 0) {
        if(ObjectLoader.executeUpdate(querys, params).length == updateIds.length)
          ObjectLoader.sendMessage(Equipment.class, "UPDATE", updateIds);
      }
    }*/
    
    dispose();
    return true;
  }
  
  public Object[] getColumns() {
    return ArrayUtils.addAll(columns.keySet().toArray(), factorColumns.values().toArray());
  }
  
  private void setTableColumns() {
    header.clear();
    table.setColumns(getColumns());

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
      for(int i=0;i<factorColumns.size();i++)
        factorColumn.add(table.findTableColumn(i+columns.size()));
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
    private final boolean isGroupCheck;

    public InitDataTask() {
      isGroupCheck = groupCheck.isSelected();
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
      List<List> data = new Vector<>();
      try {
        String factorQuery = "";
        if(!factorColumns.isEmpty()) {
          factorQuery = factorColumns.keySet().stream().map((id) -> ","
                  + "(SELECT COUNT([Group(factors):object]) FROM [Group(factors):table] WHERE [Group(factors):object]=[Equipment(group)] AND [Group(factors):target]="+id+"),"
                  + "(SELECT name FROM [EquipmentFactorValue] "
                  + "WHERE [EquipmentFactorValue(equipment)]=[Equipment(id)] AND [EquipmentFactorValue(factor)]="+id+")").reduce(factorQuery, String::concat);
        }

        editableCells.clear();
        
        Object[] params = new Object[]{ownerPartition};
        if(invisibleEquipment != null && invisibleEquipment.length > 0)
          params = ArrayUtils.add(params, invisibleEquipment);
        if(groups != null && groups.length > 0)
          params = ArrayUtils.add(params, groups);
        
        data = ObjectLoader.executeQuery("SELECT id FROM [Store] WHERE [Store(companyPartition)]=? AND [Store(objectType)]=?", true, 
                new Object[]{getOwnerPartition(), Group.ObjectType.ВАЛЮТА});
        
        for(List d:data)
          ObjectLoader.executeQuery("SELECT getFreeMoneyId(?,?,?)", true, new Object[]{getOwnerPartition(), d.get(0), 2597});
        
        data = ObjectLoader.executeQuery("SELECT [CompanyPartition(remainderControll)] FROM [CompanyPartition] WHERE id="+getOwnerPartition(), true);
        if(!data.isEmpty())
          remainderControll = (boolean) data.get(0).get(0);
        
        data = ObjectLoader.executeQuery(
                "SELECT "
                        + Utility.join(idColumns.values().toArray(), ",")+","
                        + Utility.join(columns.values().toArray(), ",")
                        + factorQuery+" "
                        + " FROM [Equipment]"
                        + " WHERE [Equipment(companyPartition)]=?"
                        + (getDefaultStoreObjectType()!=null?" AND [Equipment(group-type)]='"+getDefaultStoreObjectType()+"'":"")
                        + (getDefaultStoreType()!=null?" AND [Equipment(store-type)]='"+getDefaultStoreType()+"'":"")
                        + (isOnlyPurchase()?" AND [Equipment(amount)] < 0":"")
                        + (isOnlyAccessible()?"AND ([Equipment(amount)]-[Equipment(reserve)]) != 0":"")
                        + (invisibleEquipment!=null&&invisibleEquipment.length>0?" AND [Equipment(id)]<>ALL(?)":"")
                        + (groups!=null&&groups.length>0?" AND [Equipment(group)]=ANY(?)":""), true, params);
        
        if(isGroupCheck) {
          TreeMap<String, Vector> ndata = new TreeMap<>();
          
          data.stream().forEach(d -> {
            String hashcode = ((boolean)d.get(3)?"+":"-")+(String) d.get(4);
            Vector nd = ndata.get(hashcode);
            if(nd == null) {
              nd = new Vector(d.subList(idColumns.size(), d.size()));
              nd.set(0, new StoreId(
                      (Integer)d.get(0), 
                      (Integer)d.get(1), 
                      (Integer)d.get(2), 
                      //(boolean)d.get(3), 
                      (String)d.get(4),
                      null,
                      (BigDecimal)d.get(getColumnIndex(ColumnName.Резерв)+idColumns.size()),
                      (BigDecimal)d.get(getColumnIndex(ColumnName.Доступно)+idColumns.size())/*,
                      (BigDecimal)d.get(getColumnIndex(ColumnName.Наличие)+idColumns.size())*/));
              ndata.put(hashcode, nd);
            }else {
              ((StoreId)nd.get(0)).addId((Integer)d.get(0));
              nd.set(getColumnIndex(ColumnName.Наличие),  ((BigDecimal)nd.get(getColumnIndex(ColumnName.Наличие))) .add((BigDecimal)d.get(idColumns.size()+getColumnIndex(ColumnName.Наличие))));
              nd.set(getColumnIndex(ColumnName.Резерв),   ((BigDecimal)nd.get(getColumnIndex(ColumnName.Резерв)))  .add((BigDecimal)d.get(idColumns.size()+getColumnIndex(ColumnName.Резерв))));
              nd.set(getColumnIndex(ColumnName.Доступно), ((BigDecimal)nd.get(getColumnIndex(ColumnName.Доступно))).add((BigDecimal)d.get(idColumns.size()+getColumnIndex(ColumnName.Доступно))));
            }
          });
          data.clear();
          data.addAll(ndata.values());
        }else {
          for(int i=data.size()-1;i>=0;i--) {
            Vector d = new Vector(data.get(i).subList(idColumns.size(), data.get(i).size()));
            d.set(0, new StoreId(
                        (Integer)data.get(i).get(0), 
                        (Integer)data.get(i).get(1), 
                        (Integer)data.get(i).get(2), 
                        //(boolean)data.get(i).get(3), 
                        (String)data.get(i).get(4),
                        null,
                        (BigDecimal)data.get(i).get(getColumnIndex(ColumnName.Резерв)+idColumns.size()),
                        (BigDecimal)data.get(i).get(getColumnIndex(ColumnName.Доступно)+idColumns.size())/*,
                        (BigDecimal)data.get(i).get(getColumnIndex(ColumnName.Наличие)+idColumns.size())*/));
            data.set(i, d);
          }
        }
        
        if(!factorColumns.isEmpty()) {
          for(int row=0;row<data.size();row++) {
            List d = data.get(row);
            for(int i=d.size()-1;i>=columns.size();i=i-2) {
              if(Integer.parseInt(d.get(i-1).toString()) > 0) {
                int column = columns.size()+(i-columns.size())/2;
                FactorColumn factorColumn = factorColumns.values().toArray(new FactorColumn[0])[column-columns.size()];
                if(factorColumn.getListValues() != null && !factorColumn.getListValues().equals("")) {
                  DivisionComboBox combobox = createComboBox(
                          ArrayUtils.add(factorColumn.getListValues().split(";"), 0, "------"), 
                          d.get(i));
                  d.set(i, combobox);
                }
                if(factorColumn.getFactorType() != FactorType.адрес) {
                  Integer[] cols = editableCells.get(row);
                  if(cols == null)
                    cols = new Integer[0];
                  editableCells.put(row, (Integer[])ArrayUtils.add(cols, column));
                }
              }
              d.remove(i-1);
            }
          }
        }
        
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }finally {
        final List<List> dataVector = data;
        SwingUtilities.invokeLater(() -> {
          clear();
          setTableColumns();
          
          if(!selectedPositions.isEmpty()) {
            dataVector.stream().forEach((d) -> {
              BigDecimal count = selectedPositions.get(((StoreId)d.get(0)).getIds()[0]);
              if (count != null && count.compareTo(BigDecimal.ZERO) > 0) {
                d.set(getColumnIndex(ColumnName.кол_), count);
              }
            });
          }
          table.getTableModel().getDataVector().addAll(dataVector);
          table.getTableModel().fireTableDataChanged();
        });
        defaultCursor();
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
        throw new Exception("Не создалось значение");
      }else
        session.commit();
    } catch (Exception ex) {
      ok = false;
      ObjectLoader.rollBackSession(session);
      Messanger.showErrorMessage(ex);
    }
    return ok;
  }
  
  private void addStorePosition() {
    if(getOwnerPartition() != null) {
      AddStorePositionDialog groupTableEditor = new AddStorePositionDialog(getOwnerPartition(), getCustomerPartition(), true);
      groupTableEditor.setGroups(groups);
      groupTableEditor.setAutoLoadAndStore(true);
      groupTableEditor.initData();
      groupTableEditor.createDialog(true).setVisible(true);
    }
  }
}