package division.editors.tables;

import bum.editors.EditorController;
import bum.editors.EditorGui;
import bum.editors.InitDataListener;
import bum.editors.TableEditor;
import bum.editors.actions.ActionMacrosUtil;
import bum.editors.util.DivisionTarget;
import bum.editors.util.ObjectLoader;
import bum.interfaces.*;
import bum.interfaces.Factor.FactorType;
import division.ClientMain;
import division.editors.objects.company.CompanyPartitionStoreTableEditor;
import division.fx.PropertyMap;
import division.scale.ObjectPeriod;
import division.scale.ObjectPeriodScaleListener;
import division.store.FactorColumn;
import division.swing.*;
import division.swing.guimessanger.Messanger;
import division.swing.table.CellColorController;
import division.swing.table.groupheader.ColumnGroup;
import division.swing.table.groupheader.ColumnGroupHeader;
import division.tables.dealPosition.DealPositionID;
import division.util.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelEvent;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class DealPositionTableEditor extends EditorGui implements ObjectPeriodScaleListener {
  private final LinkedHashMap<String, String>        columns              = new LinkedHashMap<>();
  private final LinkedHashMap<Integer, FactorColumn> factorColumns        = new LinkedHashMap<>();
  private final LinkedHashMap<Integer, FactorColumn> productFactorColumns = new LinkedHashMap<>();
  
  private final DivisionToolButton addToolButton       = new DivisionToolButton(FileLoader.getIcon("Add16.gif"),"Добавить");
  private final DivisionToolButton removeToolButton    = new DivisionToolButton(FileLoader.getIcon("Delete16.gif"),"Удалить");
  
  private final DivisionTable         table  = new DivisionTable();
  private final ColumnGroupHeader     header = new ColumnGroupHeader(table);
  private final DivisionScrollPane    scroll = new DivisionScrollPane(table) {
    private final Color buttonColor = Color.GRAY;
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
        initProductFactorsMenu();
        columnsMenu.show(scroll, getRowHeader().getWidth(), 0);
      }else super.clickTopLeft(point);
    }
  };
  
  private final LinkedHashMap<Integer, FactorColumn> oldFactorColumns        = new LinkedHashMap<>();
  private final LinkedHashMap<Integer, FactorColumn> oldproductFactorColumns = new LinkedHashMap<>();
  private final JPopupMenu     columnsMenu         = new JPopupMenu();
  private final JMenu          productFactors      = new JMenu("Реквизиты продукта");
  private final JPanel         productFactorsPanel = new JPanel(new GridBagLayout());
  private final JMenu          factors             = new JMenu("Реквизиты объекта");
  private final JPanel         factorsPanel        = new JPanel(new GridBagLayout());
  
  

  private Integer[] dealIds;
  
  private final ExecutorService pool = Executors.newFixedThreadPool(5);
  private InitDealPositionTask dealPositionTask;
  
  private final JLabel allCount = new JLabel("Общее количество: ");
  private final JLabel allCost  = new JLabel("Итого: ");
  
  private Date contractEndDate = null;
  
  private boolean onlyNotDispatch = false;
  private boolean visibleSelectCountColumn = false;
  
  private final TreeMap<Integer, BigDecimal> selectedPositions = new TreeMap<>();

  private void setAdministration(boolean b) {
    getToolBar().setVisible(b);
  }
  
  private enum ColumnName {
    id,
    продавца,
    покупателя,
    покупатель,
    Процесс,
    Наименование,
    Ед_изм,
    Зав_номер,
    Цена_за_ед,
    кол_,
    Итого,
    кол,
    выдел
  }
  
  public DealPositionTableEditor() {
    super(null, null);
    getRootPanel().setLayout(new GridBagLayout());
    initComponents();
    initTargets();
    initEvents();
    
    columns.put(ColumnName.id.toString(),                                   "id");
    columns.put(ColumnName.продавца.toString(),                             "source-store-name");
    columns.put(ColumnName.покупателя.toString(),                           "target-store-name");
    columns.put(ColumnName.покупатель.toString(),                            "query:select getcompanyname([DealPosition(customer_id)])");
    columns.put(ColumnName.Процесс.toString(),                              "service_name");
    columns.put(ColumnName.Наименование.toString(),                         "group_name");
    columns.put(ColumnName.Ед_изм.toString(),                               "group-unit");
    columns.put(ColumnName.Зав_номер.toString().replaceAll("_", "."),       "identity_value");
    columns.put(ColumnName.Цена_за_ед.toString().replaceAll("_", "."),      "customProductCost");
    columns.put(ColumnName.кол_.toString().replaceAll("_", "."),            "query:@[DealPosition(amount)]");
    columns.put(ColumnName.Итого.toString(),                                "query:[DealPosition(customProductCost)] * @[DealPosition(amount)]");
    columns.put(ColumnName.кол.toString(),                                  "query:0.0::NUMERIC(10,2)");
    columns.put(ColumnName.выдел.toString(),                                "query:false");
  }
  
  private void initComponents() {
    scroll.setRowHeader(true);
    
    getToolBar().add(addToolButton);
    getToolBar().add(removeToolButton);

    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    table.setTableHeader(header);
    table.getTableModel().setColumnClassAsValue(true);
    
    getStatusBar().add(allCount);
    getStatusBar().add(allCost);

    //getRootPanel().add(tool, new GridBagConstraints(0, 0, 3, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    getRootPanel().add(scroll, new GridBagConstraints(0, 1, 3, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    
    table.setCellEditableController((JTable table1, int modelRow, int modelColumn) -> 
            getID(modelRow).getDispatchId() == null && 
                    (modelColumn == getColumnIndex(ColumnName.кол)||
                    modelColumn == getColumnIndex(ColumnName.выдел) || 
                    modelColumn == getColumnIndex(ColumnName.Цена_за_ед) || 
                    modelColumn == getColumnIndex(ColumnName.покупателя) && getID(modelRow).isObjectMove()));
    
    table.setCellColorController(new CellColorController() {
      @Override
      public Color getCellColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        return isSelect?null:getID(modelRow).getDispatchId() == null && 
                (modelColumn == getColumnIndex(ColumnName.кол) || 
                modelColumn == getColumnIndex(ColumnName.выдел) || 
                modelColumn == getColumnIndex(ColumnName.Цена_за_ед) ||
                modelColumn == getColumnIndex(ColumnName.покупателя) && getID(modelRow).isObjectMove())
                ?Color.WHITE:scroll.getBackground();
      }
      
      @Override
      public Color getCellTextColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        if(getID(modelRow).getDispatchId() != null) {
          return Color.GRAY;
        }else return null;
      }
    });
    
    table.setCellFontController((JTable table1, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) -> {
      Font f = modelColumn == getColumnIndex(ColumnName.кол) || 
                    modelColumn == getColumnIndex(ColumnName.Цена_за_ед)
                    ?new Font("Dialog", Font.BOLD, 10):null;
      /*if(getID(modelRow).getDispatchId() != null) {
        f = f==null?table1.getFont():f;
        Map attr = f.getAttributes();
        attr.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        f = new Font(attr);
      }*/
      return f;
    });
  }
  
  private DealPositionID getID(int rowIndex) {
    return (DealPositionID)table.getModel().getValueAt(rowIndex, getColumnIndex(ColumnName.id));
  }

  @Override
  public void initTargets() {
    addTarget(new DivisionTarget(DealPosition.class) {
      @Override
      public void messageReceived(final String type, final Integer[] ids, PropertyMap objectEventProperty) {
        //System.out.println(type+" DealPosition ("+Utility.join(ids, ",")+") active = "+isActive());
        if(ids.length > 0 && isActive()) {
          //System.out.println("messageReceived "+isActive());
          SwingUtilities.invokeLater(() -> {
            for(int i=table.getModel().getRowCount()-1;i>=0;i--) {
              if(ArrayUtils.contains(ids, ((DealPositionID)table.getModel().getValueAt(i, 0)).getId()))
                table.getTableModel().removeRow(i);
            }
            if(type.intern() != "REMOVE")
              initData(ids);
          });
        }
      }
    });
  }

  private void initEvents() {
    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if(table.isEnabled()) {
          if(e.getModifiers() == MouseEvent.META_MASK) {
            //JMenuItem getData = new JMenuItem("Данные о последнем изменении");
            /*getData.addActionListener(a -> {
              PropertyMap p = ObjectLoader.getMap(DealPosition.class, getSelectedId()[0], "name=query:select surName||' '||name||' '||lastName from [People] where id=(select [Worker(people)] from worker where id=[DealPosition(lastUserId)])","modificationDate");
              JOptionPane.showMessageDialog(null, p.getValue("name")+": "+p.getValue("modificationDate"), "последнее изменение", JOptionPane.INFORMATION_MESSAGE);
            });*/
            JPopupMenu menu = new JPopupMenu();
            //menu.add(getData);
            
            Map<String,Object> values = new TreeMap<>();
            values.put("ids", getSelectedId());
            try {
              JMenuItem[] scriptItems = ActionMacrosUtil.createMacrosMenu(DealPosition.class, values);
              if(scriptItems.length > 0) {
                if(scriptItems.length > 0) {
                  for(JMenuItem item:scriptItems)
                    menu.add(item);
                }
              }
            }catch(Exception ex) {
              Logger.getLogger(TableEditor.class).warn(ex);
            }
            menu.show(table, e.getX(), e.getY());
          }
        }
      }
    });
    
    
    columnsMenu.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        oldFactorColumns.clear();
        oldFactorColumns.putAll(factorColumns);
        
        oldproductFactorColumns.clear();
        oldproductFactorColumns.putAll(productFactorColumns);
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        if(!oldFactorColumns.equals(factorColumns) || !oldproductFactorColumns.equals(productFactorColumns))
          initData();
        oldFactorColumns.clear();
        oldproductFactorColumns.clear();
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
      }
    });
    
    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        try {
          if(e.getModifiers() == MouseEvent.META_MASK) {
            JPopupMenu processPopMenu = new JPopupMenu();
            Integer[] dealPositions = getSelectedId();
            int column = table.columnAtPoint(e.getPoint());
            if(dealPositions.length > 1 && column >= 0) {
              if(table.convertColumnIndexToModel(column) == getColumnIndex(ColumnName.Цена_за_ед)) {
                JMenuItem setCost = new JMenuItem("Единая цена для выделенных позиций");
                processPopMenu.add(setCost);
                
                setCost.addActionListener((ActionEvent e1) -> {
                  DivisionTextField costField = new DivisionTextField(DivisionTextField.Type.FLOAT);
                  costField.grabFocus();
                  if(JOptionPane.showConfirmDialog(
                          null,
                          costField,
                          "Единая цена для выделенных позиций",
                          JOptionPane.OK_CANCEL_OPTION,
                          JOptionPane.QUESTION_MESSAGE) == 0) {
                    updateDealPositionCost(costField.getText().equals("")?BigDecimal.ZERO:new BigDecimal(costField.getText()));
                  }
                });
              }
            }
            
            if(processPopMenu.getComponentCount() > 0)
              processPopMenu.show(table, e.getX(), e.getY());
          }
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
    
    table.getTableModel().addTableModelListener((TableModelEvent e) -> {
      if(isActive() && e.getType() == TableModelEvent.UPDATE && e.getLastRow() >= 0 && e.getColumn() >= 0) {
        DealPositionID id1 = (DealPositionID)table.getTableModel().getValueAt(e.getLastRow(), getColumnIndex(ColumnName.id));
        setActive(false);
        Object value = table.getTableModel().getValueAt(e.getLastRow(), e.getColumn());
        if(e.getColumn() == getColumnIndex(ColumnName.Цена_за_ед)) {
          Object val = table.getTableModel().getValueAt(e.getLastRow(), getColumnIndex(ColumnName.Цена_за_ед));
          table.getEditor().cancelCellEditing();
          updateDealPositionCost(val==null||val.equals("")?BigDecimal.ZERO:new BigDecimal(val.toString()));
        }else if(e.getColumn() == getColumnIndex(ColumnName.кол)) {
          BigDecimal amount   = (BigDecimal) table.getTableModel().getValueAt(e.getLastRow(), getColumnIndex(ColumnName.кол_));
          BigDecimal count    = (BigDecimal) value;
          BigDecimal oldCount = selectedPositions.get(id1.getId());
          if(count.compareTo(BigDecimal.ZERO) > 0 && count.compareTo(amount) <= 0) {
            selectedPositions.put(id1.getId(), count);
          } else if (count.compareTo(BigDecimal.ZERO) == 0) {
            selectedPositions.remove(id1.getId());
          } else if (oldCount != null) {
            table.getTableModel().setValueAt(oldCount, e.getLastRow(), e.getColumn());
          } else {
            selectedPositions.remove(id1.getId());
            table.getTableModel().setValueAt(BigDecimal.ZERO, e.getLastRow(), e.getColumn());
          }
          
          table.getTableModel().setValueAt(((BigDecimal)table.getTableModel().getValueAt(e.getLastRow(), e.getColumn())).compareTo(BigDecimal.ZERO) > 0, e.getLastRow(), getColumnIndex(ColumnName.выдел));
        }else if(e.getColumn() == getColumnIndex(ColumnName.выдел)) {
          BigDecimal amount   = (BigDecimal) table.getTableModel().getValueAt(e.getLastRow(), getColumnIndex(ColumnName.кол_));
          if((Boolean)value) {
            table.getTableModel().setValueAt(amount, e.getLastRow(), getColumnIndex(ColumnName.кол));
            selectedPositions.put(id1.getId(), amount);
          }else {
            table.getTableModel().setValueAt(BigDecimal.ZERO, e.getLastRow(), getColumnIndex(ColumnName.кол));
            selectedPositions.remove(id1.getId());
          }
        }
        setActive(true);
      }
    });
    
    table.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_DELETE && isEditable()) {
          table.getEditor().cancelCellEditing();
          removeDealPositions();
        }
      }
    });
    
    addToolButton.addActionListener(e -> addButton());
    removeToolButton.addActionListener(e -> removeDealPositions());
  }
  
  public Integer[] getVisibleIds() {
    Integer[] ids = new Integer[0];
    for(int i=0;i<table.getRowCount();i++)
      ids = (Integer[]) ArrayUtils.add(ids, ((DealPositionID)table.getValueAt(i, 0)).getId());
    return ids;
  }
  
  private JCheckBox createFactorColumnCheckBox(Integer id, String name, FactorType factorType, String listValues, boolean productFactor) {
    final JCheckBox box = createMenuCheckBox(name);
    box.setSelected(factorColumns.containsKey(id) || productFactorColumns.containsKey(id));
    box.addItemListener(e -> {
      if(e.getStateChange() == ItemEvent.SELECTED)
        if(!productFactor)
          factorColumns.put(id, new FactorColumn(id, name, factorType, listValues, productFactor));
        else productFactorColumns.put(id, new FactorColumn(id, name, factorType, listValues, productFactor));
      else {
        factorColumns.remove(id);
        productFactorColumns.remove(id);
      }
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
    factors.removeAll();
    factorsPanel.removeAll();
    try {
      List<List> data = ObjectLoader.executeQuery("SELECT "
              + "[Factor(id)], "
              + "[Factor(name)] ,"
              + "[Factor(factorType)], "
              + "[Factor(listValues)] "
              + "FROM [Factor] "
              + "WHERE [Factor(productFactor)]=false AND "
              + "[Factor(id)] IN (SELECT DISTINCT [Factor(groups):object] FROM [Factor(groups):table] "
              + "WHERE [Factor(groups):target] IN (SELECT [Equipment(group)] FROM [Equipment] WHERE [Equipment(id)] IN "
              + "(SELECT [DealPosition(equipment)] FROM [DealPosition] WHERE [DealPosition(id)]=ANY(?))))", true, 
              new Object[]{getVisibleIds()});
      if(!data.isEmpty()) {
        columnsMenu.add(factors);
        for(int i=0;i<data.size();i++)
          factorsPanel.add(
                  createFactorColumnCheckBox((Integer) data.get(i).get(0), 
                          (String) data.get(i).get(1), 
                          FactorType.valueOf((String) data.get(i).get(2)), 
                          (String) data.get(i).get(3), false), 
                  new GridBagConstraints(0, i, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(1,2,1,2), 0, 0));
        factors.add(factorsPanel);
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  private void initProductFactorsMenu() {
    columnsMenu.remove(productFactors);
    productFactors.removeAll();
    productFactorsPanel.removeAll();
    try {
      List<List> data = ObjectLoader.executeQuery("SELECT "
              + "[Factor(id)], "
              + "[Factor(name)] ,"
              + "[Factor(factorType)], "
              + "[Factor(listValues)] "
              + "FROM [Factor] "
              + "WHERE [Factor(productFactor)]=true AND "
              + "[Factor(id)] IN (SELECT DISTINCT [Factor(products):object] FROM [Factor(products):table] "
              + "WHERE [Factor(products):target] IN (SELECT [DealPosition(product)] FROM [DealPosition] WHERE [DealPosition(id)]=ANY(?)))", true, 
              new Object[]{getVisibleIds()});
      if(!data.isEmpty()) {
        columnsMenu.add(productFactors);
        for(int i=0;i<data.size();i++)
          productFactorsPanel.add(
                  createFactorColumnCheckBox((Integer) data.get(i).get(0), 
                          (String) data.get(i).get(1), 
                          FactorType.valueOf((String) data.get(i).get(2)), 
                          (String) data.get(i).get(3), true), 
                  new GridBagConstraints(0, i, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(1,2,1,2), 0, 0));
        productFactors.add(productFactorsPanel);
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  public void setContractEndDate(Date contractEndDate) {
    this.contractEndDate = contractEndDate;
  }
  
  public int getRowEquipment(Integer id) {
    for(int i=0;i<table.getModel().getRowCount();i++)
      if(id.equals(table.getModel().getValueAt(i, 1)))
        return i;
    return -1;
  }
  
  public void addButton() {
  }
  
  public Integer[] getAllEquipmentIds() {
    Integer[] ids = new Integer[0];
    List<List> data = table.getTableModel().getDataVector();
    for(List row:data)
      ids = (Integer[])ArrayUtils.add(ids, ((DealPositionID)row.get(getColumnIndex(ColumnName.id))).getEquipmentId());
    return ids;
  }
  
  public Integer[] getAllIds() {
    Integer[] ids = new Integer[0];
    List<List> data = table.getTableModel().getDataVector();
    for(List row:data)
      ids = (Integer[])ArrayUtils.add(ids, ((DealPositionID)row.get(getColumnIndex(ColumnName.id))).getId());
    return ids;
  }
  
  public Integer[] getSelectedEquipmentId() {
    Integer[] ids = new Integer[0];
    for(int row:table.getSelectedRows())
      ids = (Integer[]) ArrayUtils.add(ids, ((DealPositionID)table.getValueAt(row, getColumnIndex(ColumnName.id))).getEquipmentId());
    return ids;
  }
  
  public Integer[] getSelectedId() {
    Integer[] ids = new Integer[0];
    for(int row:table.getSelectedRows())
      ids = (Integer[]) ArrayUtils.add(ids, ((DealPositionID)table.getValueAt(row, getColumnIndex(ColumnName.id))).getId());
    return ids;
  }
  
  @Override
  public void clear() {
    table.ResetFilters();
    table.getTableModel().clear();
    table.setColumns(new Object[0]);
  }
  
  @Override
  public void initData() {
    initData(null);
  }
  
  public void initData(Integer[] ids) {
    if(dealPositionTask != null)
      dealPositionTask.setShutDown(true);
    
    if(isActive()) {
      dealPositionTask = new InitDealPositionTask(ids);
      pool.submit(dealPositionTask);
    }
  }
  
  public void createDeals(
          Integer tempProcessId,
          Integer processId, 
          Integer contractId,
          Integer sellerId,
          Integer customerId,
          Integer sellerPartitionId,
          Integer customerPartitionId,
          Integer sellerCfcId,
          Integer customerCfcId,
          LocalDate dealStartDate) {
    EditorController.waitCursor(this.getGUI());
    try {
      if(contractId != null && 
              sellerId != null && 
              customerId != null && 
              sellerCfcId != null && 
              customerCfcId != null && 
              sellerPartitionId != null && 
              customerPartitionId != null &&
              processId != null &&
              tempProcessId != null) {
        
        Period  duration    = null;
        Period  recurrence  = null;

        HashMap<Integer, Map.Entry<Period,Period>> groups = new HashMap<>();
        List<List> data = ObjectLoader.getData(DBFilter.create(Product.class).AND_EQUAL("company", sellerId).AND_EQUAL("service", processId), 
                "duration","recurrence","group");
        if(data.isEmpty())
          data = ObjectLoader.getData(DBFilter.create(Product.class).AND_EQUAL("company", null).AND_EQUAL("globalProduct", null).AND_EQUAL("service", processId), 
                "duration","recurrence","group");
        if(!data.isEmpty()) {
          for(List d:data) {
            groups.put((Integer)d.get(2), new AbstractMap.SimpleEntry<>((Period)d.get(0), (Period)d.get(1)));
            //groupIds = (Integer[])ArrayUtils.add(groupIds, d.get(2));
          }
          TreeMap<Integer,BigDecimal> equipments = getStorePositions(sellerId, sellerPartitionId, customerPartitionId, processId, null);
          setCursor(new Cursor(Cursor.WAIT_CURSOR));

          if(!equipments.isEmpty()) {
            Integer[] groupIds = null;
            data = ObjectLoader.getData(DBFilter.create(Equipment.class).AND_IN("id", equipments.keySet().toArray(new Integer[0])), "group");
            for(List d:data)
              groupIds = (Integer[])ArrayUtils.add(groupIds, d.get(0));

            if(groupIds != null && groupIds.length > 0) {
              duration   = groups.containsKey(groupIds[0]) ? groups.get(groupIds[0]).getKey()   : null;
              recurrence = groups.containsKey(groupIds[0]) ? groups.get(groupIds[0]).getValue() : null;
              for(Integer g:groupIds) {
                Period d = groups.containsKey(g) ? groups.get(g).getKey()   : null;
                Period r = groups.containsKey(g) ? groups.get(g).getValue() : null;
                if((duration == null ? d != null : !duration.equals(d)) ||
                        (recurrence == null ? r != null : !recurrence.equals(r))) {
                  duration = null;
                  recurrence = null;
                  break;
                }
              }
            }

            if(duration == null && recurrence == null) {
              JOptionPane.showMessageDialog(null,
                      "Необходимо выбрать продукты с одинаковой\nдлительностью и цикличностью","Ошибка",JOptionPane.ERROR_MESSAGE);
            }else {
              TreeMap<String,Object> fields = new TreeMap();
              fields.put("contract",                 contractId);
              fields.put("sellerCompany",            sellerId);
              fields.put("sellerCompanyPartition",   sellerPartitionId);
              fields.put("sellerCfc",                sellerCfcId);
              fields.put("customerCompany",          customerId);
              fields.put("customerCompanyPartition", customerPartitionId);
              fields.put("customerCfc",              customerCfcId);
              fields.put("service",                  processId);
              fields.put("tempProcess",              tempProcessId);
              fields.put("dealStartDate",            dealStartDate);
              copyDeals(fields, equipments, duration, recurrence);
            }
          }
        }else Messanger.alert("Данный процесс отсутствует в каталоге", "Внимание", JOptionPane.WARNING_MESSAGE);
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }finally {
      EditorController.defaultCursor(this.getGUI());
    }
  }
  
  private void addDealPositionsToDeals(Integer[] deals, TreeMap<Integer, BigDecimal> equipments, RemoteSession session) throws Exception {
    boolean isMySession = session == null;
    try {
      if(isMySession)
        session = ObjectLoader.createSession();
      List<List> data = session.executeQuery("SELECT MAX(id) FROM [DealPosition]");
      Integer startId = (Integer) (data.isEmpty()?0:data.get(0).get(0));
      
      for(Integer dealId:deals) {
        //Получить все продукты в соответсвии с процессом сделки и группами экземпляров
        data = session.executeQuery("SELECT "
                /*0*/+ "id,"
                /*1*/+ "(SELECT [Product(id)] FROM [Product] WHERE [Product(group)]=[Equipment(group)] AND "
                     + "[Product(service)]=(SELECT [Deal(service)] FROM [Deal] WHERE [Deal(id)]=?) AND "
                     + "[Product(company)]=(SELECT [Deal(sellerCompany)] FROM [Deal] WHERE id=?) AND "
                     + "tmp=false AND "
                     + "type='CURRENT'), "
                /*2*/+ "(SELECT [Product(id)] FROM [Product] WHERE [Product(group)]=[Equipment(group)] AND "
                     + "[Product(service)]=(SELECT [Deal(service)] FROM [Deal] WHERE [Deal(id)]=?) AND "
                     + "[Product(company)] ISNULL AND [Product(globalProduct)] ISNULL), "
                /*3*/+ "[Equipment(store)], "
                /*4*/+ "(SELECT [Deal(object-owner)] FROM [Deal] WHERE [Deal(id)]="+dealId+"), "
                /*5*/+ "(SELECT [Deal(customerCompanyPartition)] FROM [Deal] WHERE [Deal(id)]="+dealId+"), "
                /*6*/+ "[Equipment(amount)],"
                /*7*/+ "[Equipment(store-type)], "
                /*8*/+ "[Equipment(object-type)] "
                + "FROM [Equipment] "
                + "WHERE id=ANY(?)",
                new Object[]{dealId,dealId,dealId,equipments.keySet().toArray(new Integer[0])});
        
        for(List d:data) {
          Integer product = (Integer)(d.get(1) != null ? d.remove(1) : d.remove(2));
          if(product != null) {
              session.executeUpdate("INSERT INTO [DealPosition] ("
                      + "[DealPosition(deal)],"
                      + "[DealPosition(equipment)],"
                      + "[DealPosition(product)],"
                      + "[DealPosition(amount)]"
                      + ") "
                      + "VALUES("+dealId+","+d.get(0)+","+product+","
                      + equipments.get((Integer) d.get(0))+")");
          }
        }
      }
      
      data = session.executeQuery("SELECT id FROM [DealPosition] WHERE id>"+startId);
      Integer[] dealPositions = new Integer[0];
      for(List d:data)
        dealPositions = (Integer[]) ArrayUtils.add(dealPositions, d.get(0));
      
      if(dealPositions.length > 0) {
        session.addEvent(DealPosition.class, "CREATE", dealPositions);
        session.addEvent(Deal.class, "UPDATE", deals);
      }
      if(isMySession)
        session.commit();
    }catch(Exception ex) {
      if(isMySession)
        ObjectLoader.rollBackSession(session);
      throw new Exception(ex);
    }
  }
  
  private void copyDeals(TreeMap<String,Object> fields, TreeMap<Integer, BigDecimal> equipments, Period duration, Period recurrence) {
    //Calendar c = Calendar.getInstance();
    LocalDate dealStartDate = (LocalDate) fields.get("dealStartDate");
    //String[] sr = recurrence==null?null:recurrence.split(" ");
    //String[] sd = duration.split(" ");
    //int rCount = sr==null?0:Integer.valueOf(sr[0]);
    //int dCount   = Integer.valueOf(sd[0]);
    
    int recCount = recurrence == null || recurrence.isZero() ? 1 : getCount(1);
    if(recCount == 0) {
      try {
        recCount = 0;
        if(contractEndDate == null) {
          List<List> data = ObjectLoader.getData(Contract.class, new Integer[]{(Integer)fields.get("contract")}, new String[]{"endDate"});
          if(!data.isEmpty() && data.get(0).get(0) != null)
            contractEndDate = new Date(((java.sql.Date)data.get(0).get(0)).getTime());
        }
        if(contractEndDate != null) {
          LocalDate start = dealStartDate;
          while(start.isBefore(Utility.convert(contractEndDate)) || start.isEqual(Utility.convert(contractEndDate))) {
            recCount++;
            
            if(recurrence.getDays() > 0)
              start = start.plusDays(recurrence.getDays());
            if(recurrence.getMonths() > 0)
              start = start.plusMonths(recurrence.getMonths()).withDayOfMonth(1);
            if(recurrence.getYears() > 0)
              start = start.plusYears(recurrence.getYears()).withMonth(1).withDayOfMonth(1);
          }
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
    
    RemoteSession session = null;
    try {
      session = ObjectLoader.createSession();
      for(int r=0;r<recCount;r++) {
        
        LocalDate dealEndDate = dealStartDate.plusDays(10);
        
        if(duration.getDays() > 0)
          dealEndDate = dealStartDate.plusDays(duration.getDays()-1);
        if(duration.getMonths() > 0)
          dealEndDate = dealStartDate.plusMonths(duration.getMonths()-1).withDayOfMonth(dealStartDate.plusMonths(duration.getMonths()-1).lengthOfMonth());
        if(duration.getYears() > 0)
          dealEndDate = dealStartDate.plusYears(duration.getYears()-1).withDayOfYear(dealStartDate.plusYears(duration.getYears()-1).getDayOfYear());
        
        //= dealStartDate.plusDays(duration.getDays()).plusMonths(duration.getMonths()).plusYears(duration.getYears());
        //c.setTime(dealStartDate);
        /*long days = 0;
        if(sd[1].startsWith("м")) {
          days = c.getActualMaximum(Calendar.DAY_OF_MONTH) - c.get(Calendar.DAY_OF_MONTH);
          for(int i=0;i<dCount-1;i++) {
            c.set(Calendar.DAY_OF_MONTH, 1);
            if(c.get(Calendar.MONTH)==11) {
              c.set(Calendar.YEAR, c.get(Calendar.YEAR)+1);
              c.set(Calendar.MONTH,0);
            }else  c.set(Calendar.MONTH, c.get(Calendar.MONTH)+1);
            days += c.getActualMaximum(Calendar.DAY_OF_MONTH);
          }
        }else if(sd[1].startsWith("д")) {
          days = dCount-1;
        }else if(sd[1].startsWith("л") || sd[1].startsWith("г")) {
          days = c.getActualMaximum(Calendar.DAY_OF_YEAR) - c.get(Calendar.DAY_OF_YEAR);
          for(int i=0;i<dCount-1;i++) {
            c.set(Calendar.YEAR, c.get(Calendar.YEAR)+1);
            days += c.getActualMaximum(Calendar.DAY_OF_MONTH);
          }
        }
        c.setTimeInMillis(dealStartDate.getTime()+(days*24*60*60*1000));
        
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);*/
        
        Integer newDealId = null;
        //Проверить не пересекается ли тело создаваемой сделки с телом уже существующей такой же
        Integer[] ids = getDeals((Integer)fields.get("contract"), (Integer)fields.get("tempProcess"), Utility.convert(dealStartDate), Utility.convert(dealEndDate,23,59,59,999), (Integer)fields.get("customerCompanyPartition"));
        if(ids.length > 0) {
          newDealId = ids[0];
        }else {
          fields.put("dealStartDate",            Utility.convertToSqlDate(dealStartDate));
          fields.put("dealEndDate",              Utility.convertToSqlDate(dealEndDate,23,59,59,999));
          newDealId = session.createObject(Deal.class, (Map)fields);
        }
        
        addDealPositionsToDeals(new Integer[]{newDealId}, equipments, session);
        if(recCount > 1) {
          if(recurrence.getDays() > 0)
            dealStartDate = dealStartDate.plusDays(recurrence.getDays());
          if(recurrence.getMonths() > 0)
            dealStartDate = dealStartDate.plusMonths(recurrence.getMonths()).withDayOfMonth(1);
          if(recurrence.getYears() > 0)
            dealStartDate = dealStartDate.plusYears(recurrence.getYears()).withMonth(1).withDayOfMonth(1);
        }
          //dealStartDate = dealStartDate.plusDays(recurrence.getDays()).plusMonths(recurrence.getMonths()).plusYears(recurrence.getYears());//getNextStart(dealStartDate, sr[1], rCount);
      }
      session.commit();
    }catch(Exception ex) {
      ObjectLoader.rollBackSession(session);
      Messanger.showErrorMessage(ex);
    }
  }
  
  public static Integer[] getDeals(Integer contractId, Integer tempProcessId, Date startDate, Date endDate, Integer customerPartition) {
    //Проверить не пересекается ли тело создаваемой сделки с телом уже существующей такой же
    Integer[] ids = new Integer[0];
    try {
      DBFilter filter = DBFilter.create(Deal.class);
      filter.AND_EQUAL("type", Deal.Type.CURRENT);
      filter.AND_EQUAL("contract", contractId);
      filter.AND_EQUAL("customerCompanyPartition", customerPartition);
      filter.AND_EQUAL("tempProcess",  tempProcessId);
      filter.AND_FILTER()
              .AND_DATE_BEFORE_OR_EQUAL("dealStartDate", startDate)
              .AND_DATE_AFTER_OR_EQUAL("dealEndDate", startDate)

              .OR_DATE_BEFORE_OR_EQUAL("dealStartDate", endDate)
              .AND_DATE_AFTER_OR_EQUAL("dealEndDate", endDate)

              .OR_DATE_BEFORE_OR_EQUAL("dealStartDate", startDate)
              .AND_DATE_AFTER_OR_EQUAL("dealEndDate", endDate)

              .OR_DATE_AFTER_OR_EQUAL("dealStartDate", startDate)
              .AND_DATE_BEFORE_OR_EQUAL("dealEndDate", endDate)

              .OR_DATE_EQUAL("dealStartDate", startDate)
              .OR_DATE_EQUAL("dealEndDate", startDate);

      List<List> deals = ObjectLoader.getData(filter, new String[]{"id"});
      for(List d:deals)
        ids = (Integer[]) ArrayUtils.add(ids, d.get(0));
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    return ids;
  }
  
  /*public static Date getNextStart(Date previosDate, Period rec) {
    Calendar c = Calendar.getInstance();
    c.setTime(previosDate);
    if(rec.getMonths() > 0) {
      c.set(Calendar.DAY_OF_MONTH, 1);
      for(int i=0;i<rCount;i++) {
        if(c.get(Calendar.MONTH)==11) {
          c.set(Calendar.YEAR, c.get(Calendar.YEAR)+1);
          c.set(Calendar.MONTH,0);
        }else  c.set(Calendar.MONTH, c.get(Calendar.MONTH)+1);
      }
    }else if(rType.startsWith("д")) {
      c.setTimeInMillis(c.getTimeInMillis()+((long)rCount*24*60*60*1000));
    }else if(rType.startsWith("л") || rType.startsWith("г")) {
      for(int i=0;i<rCount;i++) {
        c.set(c.get(Calendar.YEAR)+1, 0, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
      }
    }
    return c.getTime();
  }*/

  @Override
  public void closeDialog() {
    selectedPositions.clear();
    table.clearSelection();
    super.closeDialog();
  }
  
  public TreeMap<Integer, BigDecimal> get() {
    JDialog d = createDialog(ClientMain.getInstance());
    d.setModal(true);
    d.setVisible(true);
    return selectedPositions;
  }
  
  public void setAllSelected(boolean selected) {
    selectedPositions.clear();
    ((List<List>)table.getTableModel().getDataVector()).stream().forEach(d -> {
      if(selected)
        selectedPositions.put(((DealPositionID)d.get(getColumnIndex(ColumnName.id))).getId(), (BigDecimal)d.get(getColumnIndex(ColumnName.кол_)));
      d.set(getColumnIndex(ColumnName.кол), selected ? d.get(getColumnIndex(ColumnName.кол_)) : BigDecimal.ZERO);
    });
    table.getTableModel().fireTableDataChanged();
  }
  
  @Override
  public Boolean okButtonAction() {
    dispose();
    return true;
  }
  
  private int getColumnIndex(ColumnName columnName) {
    return ArrayUtils.indexOf(getTableColumns(), columnName.toString().replaceAll("_", "."));
  }
  
  public boolean isOnlyNotDispatch() {
    return onlyNotDispatch;
  }
  
  public void setOnlyNotDispatch(boolean onlyNotDispatch) {
    this.onlyNotDispatch = onlyNotDispatch;
  }
  
  public boolean isVisibleSelectCountColumn() {
    return visibleSelectCountColumn;
  }
  
  public void setVisibleSelectCountColumn(boolean visibleSelectCountColumn) {
    this.visibleSelectCountColumn = visibleSelectCountColumn;
  }
  
  protected void updateDealPositionCost(BigDecimal cost) {
    setCursor(new Cursor(Cursor.WAIT_CURSOR));
    try {
      Integer[] ids = getSelectedId();
      if(ids.length > 0) {
        ids = getDealPositions(ids); //Вот мы и получили все позиции, которые изменить.
        if(ids.length > 0) {
          RemoteSession session = null;
          try {
            session = ObjectLoader.createSession(false);
            session.executeUpdate(DealPosition.class, new String[]{"customProductCost"}, new Object[]{cost}, ids);
            Integer[] cids = new Integer[0];
            for(List d:session.executeQuery("SELECT [CreatedDocument(dealPositions):object] FROM [CreatedDocument(dealPositions):table] WHERE "
                    + "[CreatedDocument(dealPositions):target]=ANY(?)",new Object[]{ids}))
              cids = (Integer[]) ArrayUtils.add(cids, d.get(0));
            ActionUtil.replaceDocuments(cids, session);
            session.commit();
          }catch(Exception ex) {
            ObjectLoader.rollBackSession(session);
            Messanger.showErrorMessage(ex);
          }
        }
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }finally {
      setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }
  }
  
  private void removeDealPositions() {
    waitCursor();
    try {
      Integer[] ids = getSelectedId();
      if(ids.length > 0) {
        if(JOptionPane.showConfirmDialog(
                null,
                "Вы действительно хотите удалить выделенны"+(ids.length>1?"е":"й")+" объект"+(ids.length>1?"ы":"")+"?",
                "Отправка в архив",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == 0) {
          Integer[] dealPositionIds = getDealPositions(ids); //Вот мы и получили все позиции, которые необходимо удалить.
          if(dealPositionIds.length > 0)
            ActionUtil.removeDealPositions(dealPositionIds, null);
        }
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }finally {
      defaultCursor();
    }
  }
  
  protected void addDealPositions() {
    try {
      if(dealIds != null && dealIds.length > 0) {
        List<List> data = ObjectLoader.getData(Deal.class, dealIds, 
                new String[] {
                  /*0*/"service",
                  /*1*/"contract",
                  /*2*/"sellerCompany",
                  /*3*/"customerCompany",
                  /*4*/"sellerCompanyPartition",
                  /*5*/"customerCompanyPartition",
                  /*6*/"sellerCfc",
                  /*7*/"customerCfc",
                  /*8*/"dealStartDate",
                  /*9*/"contract_enddate",
                  /*10*/"tempProcess"
                }, new String[]{"dealStartDate"});
        List deal = data.get(0);
        for(List d:data) {
          if(!d.get(0).equals(deal.get(0)) || !d.get(1).equals(deal.get(1))) {
            Messanger.alert("Слишком разные сделки", JOptionPane.ERROR_MESSAGE);
            return;
          }
        }
        
        TreeMap<Integer,BigDecimal> equipments = getStorePositions(null, null, null, null, dealIds);
        if(!equipments.isEmpty()) {
          if(dealIds.length > 1)
            addDealPositionsToDeals(dealIds, equipments, null);
          else {
            java.sql.Date start   = (java.sql.Date) deal.get(8);
            java.sql.Date end     = (java.sql.Date) deal.get(9);
            Integer contractId    = (Integer) deal.get(1);
            Integer tempProcessId = (Integer) deal.get(10);
            data = ObjectLoader.getData(
                    DBFilter.create(Deal.class).
                    AND_EQUAL("type", Deal.Type.CURRENT).
                    AND_EQUAL("tempProcess", tempProcessId).
                    AND_EQUAL("contract", contractId).
                    AND_DATE_AFTER_OR_EQUAL("dealStartDate", start), 
                    new String[]{"id"}, new String[]{"dealStartDate"});
            if(!data.isEmpty()) {
              Integer[] deals = new Integer[0];
              int count = data.size()>1?getCount(data.size()):1;
              if(count == 0) {
                data = ObjectLoader.getData(
                        DBFilter.create(Deal.class).
                        AND_EQUAL("contract", contractId).
                        AND_EQUAL("type", Deal.Type.CURRENT).
                        AND_EQUAL("tempProcess", tempProcessId).
                        AND_DATE_AFTER_OR_EQUAL("dealStartDate", start).
                        AND_DATE_BEFORE_OR_EQUAL("dealEndDate", end), 
                        new String[]{"id"}, 
                        new String[]{"dealStartDate"});
                for(List d:data)
                  deals = (Integer[]) ArrayUtils.add(deals, d.get(0));
              }else {
                for(List d:data)
                  deals = (Integer[]) ArrayUtils.add(deals, d.get(0));
                
                deals = (Integer[]) ArrayUtils.subarray(deals, 0, count);
              }
              addDealPositionsToDeals(deals, equipments, null);
            }
          }
        }
      }
    }catch(Exception e) {
      Messanger.showErrorMessage(e);
    }
  }
  
  private Integer[] getDealPositions(Integer[] ids) throws Exception {
    Integer[] dealPositionIds = new Integer[0];
    TreeMap<Integer,List<List>> mapData = new TreeMap<>();
    List<List> data = ObjectLoader.getData(
            DBFilter.create(DealPosition.class).AND_IN("id", ids), 
            new String[] {
              /*0*/"id",
              /*1*/"deal",
              /*2*/"tempProcess_id",
              /*3*/"deal_start_date",
              /*4*/"process_id",
              /*5*/"product",
              /*6*/"equipment",
              /*7*/"contract_id",
              /*8*/"query:(SELECT [Contract(endDate)] FROM [Contract] WHERE [Contract(id)]=[DealPosition(contract_id)])",
              /*9*/"recurrence"});
    for(List d:data) {
      List<List> dealPositions = mapData.get(d.get(1));
      if(dealPositions == null) {
        dealPositions = new Vector<>();
        mapData.put((Integer)d.get(1), dealPositions);
      }
      dealPositions.add(d);
    }
    
    //Если удаляем позиции из нескольких сделок, то проверку на цикличность не делаем, а если из одной, то делаем!!!
    if(mapData.size() == 1) {
      data.clear();
      List<List> positions = mapData.get(mapData.firstKey());
      java.sql.Date start   = (java.sql.Date) positions.get(0).get(3);
      java.sql.Date end     = (java.sql.Date) positions.get(0).get(8);
      Integer contractId    = (Integer) positions.get(0).get(7);
      Integer tempProcessId = (Integer) positions.get(0).get(2);
      String recurrence     = (String)  positions.get(0).get(9);
      boolean isRecurrence = false;
      try {
        isRecurrence = recurrence != null && !recurrence.equals("") && !recurrence.startsWith("0");
        if(!isRecurrence)
          isRecurrence = !Period.parse(recurrence).isZero();
      } catch (Exception ex) {}
      if(contractId != null && tempProcessId != null && isRecurrence) {
        try {
          data = ObjectLoader.executeQuery("SELECT [Deal(id)] FROM [Deal] WHERE tmp=false AND type='CURRENT' AND "
                  + "[Deal(tempProcess)]="+tempProcessId+" AND [Deal(dealStartDate)] >= ? AND "
                  + "(SELECT array_agg(a) FROM unnest(ARRAY(SELECT [DealPosition(equipment)] FROM [DealPosition] WHERE [DealPosition(deal)]=[Deal(id)])) a WHERE "
                  + "a IN (SELECT [DealPosition(equipment)] FROM [DealPosition] WHERE [DealPosition(id)]=ANY(?))) NOTNULL "
                  + "ORDER BY [Deal(dealStartDate)]", true,
                  new Object[]{start,ids});
        }catch(Exception ex) {
          data.clear();
          mapData.clear();
          Messanger.showErrorMessage(ex);
        }
        
        if(!data.isEmpty()) {
          Integer[] deals = new Integer[0];
          int count = data.size()>1?getCount(data.size()):1;
          if(count >= 0) {
            Integer[] products   = new Integer[0];
            Integer[] equipments = new Integer[0];
            for(List prodition:positions) {
              products   = (Integer[]) ArrayUtils.add(products, prodition.get(5));
              equipments = (Integer[]) ArrayUtils.add(equipments, prodition.get(6));
            }
            if(count == 0) {
              try {
                data = ObjectLoader.executeQuery("SELECT [Deal(id)] FROM [Deal] WHERE tmp=false AND type='CURRENT' AND "
                        + "[Deal(tempProcess)]="+tempProcessId+" AND [Deal(dealStartDate)] >= ? AND [Deal(dealStartDate)]<=? AND "
                        
                        + "(SELECT array_agg(a) FROM unnest(ARRAY(SELECT [DealPosition(equipment)] FROM [DealPosition] WHERE [DealPosition(deal)]=[Deal(id)])) a WHERE "
                        + "a IN (SELECT [DealPosition(equipment)] FROM [DealPosition] WHERE [DealPosition(id)]=ANY(?))) NOTNULL "
                        
                        + "ORDER BY [Deal(dealStartDate)]", true,
                        new Object[]{start,end,ids});
              }catch(Exception ex) {
                data.clear();
                mapData.clear();
                Messanger.showErrorMessage(ex);
              }
              for(List d:data)
                deals = (Integer[]) ArrayUtils.add(deals, d.get(0));
            }else {
              for(List d:data)
                deals = (Integer[]) ArrayUtils.add(deals, d.get(0));

              deals = (Integer[]) ArrayUtils.subarray(deals, 0, count);
            }
            
            data = ObjectLoader.getData(DBFilter.create(DealPosition.class).AND_IN("deal", deals).AND_IN("product", products).AND_IN("equipment", equipments), 
                    new String[]{"id","deal","duration","recurrence","deal_start_date","process_id","product","equipment","contract_id"});
            for(List d:data) {
              List<List> dealPositions = mapData.get(d.get(1));
              if(dealPositions == null) {
                dealPositions = new Vector<>();
                mapData.put((Integer)d.get(1), dealPositions);
              }
              if(!dealPositions.contains(d))
                dealPositions.add(d);
            }
          }else mapData.clear();
        }
      }
    }
    
    if(!mapData.isEmpty())
      for(List<List> dps:mapData.values())
        for(List d:dps)
          if(!ArrayUtils.contains(dealPositionIds, d.get(0)))
            dealPositionIds = (Integer[]) ArrayUtils.add(dealPositionIds, d.get(0));
    
    return dealPositionIds;
  }
  
  //0 - до конца договора
  //-1 - отмена
  public static int getCount(int defaultCount) {
    int returnCount = -1;
    
    JPanel rootPanel   = new JPanel(new GridBagLayout());
    JPanel periodPanel = new JPanel(new GridBagLayout());
    
    JRadioButton allContractButton = new JRadioButton("На весь срок действия договора");
    JRadioButton recCountButton    = new JRadioButton();
    
    ButtonGroup group = new ButtonGroup();
    group.add(allContractButton);
    group.add(recCountButton);
    
    JSpinner count = new JSpinner(new SpinnerNumberModel(defaultCount, 1, Integer.MAX_VALUE, 1));
    count.setPreferredSize(new Dimension(50, count.getPreferredSize().height));
    allContractButton.setSelected(true);
    
    ((NumberEditor)count.getEditor()).getTextField().getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        recCountButton.setSelected(true);
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        recCountButton.setSelected(true);
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        recCountButton.setSelected(true);
      }
    });
    
    periodPanel.setBorder(BorderFactory.createTitledBorder("Запланировать"));
    periodPanel.add(allContractButton,    new GridBagConstraints(0, 0, 3, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    periodPanel.add(recCountButton,       new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    periodPanel.add(count,                new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    periodPanel.add(new JLabel("циклов"), new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

    rootPanel.add(periodPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    
    if(JOptionPane.showConfirmDialog(null, rootPanel, "Выберите вариант добавления", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == 0)
      returnCount = recCountButton.isSelected()?(Integer)count.getValue():0;
    return returnCount;
  }
  
  private TreeMap<Integer, BigDecimal> getStorePositions(Integer sellerId, Integer sellerPartitionId, 
          Integer customerPartitionId, Integer processId, Integer[] deals) throws Exception {
    
    TreeMap<Integer, BigDecimal> selectedStorePositions = new TreeMap<>();
    //Проверяем на то чтобы продавцы и покупатели в сделках были одни и те же
    if(deals != null && deals.length > 0) {
      for(List d:ObjectLoader.getData(Deal.class, deals, "sellerCompany", "sellerCompanyPartition", "customerCompanyPartition", "service")) {
        sellerId = (Integer) d.get(0);
        if(sellerPartitionId == null)
          sellerPartitionId = (Integer) d.get(1);
        else if(!sellerPartitionId.equals(d.get(1))) {
          //если не совпадают продавцы в выбранных сделках, то прекращаем выбор
          sellerPartitionId = null;
          break;
        }
        if(customerPartitionId == null)
          customerPartitionId = (Integer) d.get(2);
        else if(!customerPartitionId.equals(d.get(2))) {
          //если не совпадают покупатели в выбранных сделках, то прекращаем выбор
          customerPartitionId = null;
          break;
        }
        if(processId == null)
          processId = (Integer) d.get(3);
        else if(!processId.equals(d.get(3))) {
          //если не совпадают процессы в выбранных сделках, то прекращаем выбор
          processId = null;
          break;
        }
      }
    }
    
    if(sellerPartitionId != null && customerPartitionId != null && processId != null) {
      //получаем группы объектов из прайса
      Service.Owner owner = ObjectLoader.getMap(Service.class, processId, "owner").getValue("owner", Service.Owner.class);
      Integer equipmentsOwner = owner == Service.Owner.SELLER ? sellerPartitionId : customerPartitionId;
      
      List<PropertyMap> data = ObjectLoader.getList(DBFilter.create(Product.class)
              .AND_EQUAL("company", sellerId)
              .AND_EQUAL("service", processId)
              .AND_EQUAL("tmp", false)
              .AND_EQUAL("type", Product.Type.CURRENT), "group_id","service_owner");
      
      Integer[] comapnyGroupIds = PropertyMap.getArrayFromList(ObjectLoader.getList(DBFilter.create(Product.class)
              .AND_EQUAL("company", sellerId)
              .AND_EQUAL("service", processId)
              .AND_EQUAL("tmp", false)
              .AND_EQUAL("type", Product.Type.CURRENT), "group_id"), "group_id", Integer.class);
      
      
      /*for(PropertyMap d:data) {
        if(equipmentsOwner == null && d.isNotNull("service_owner"))
          equipmentsOwner = d.get(1).toString().equals(Service.Owner.CUSTOMER.toString()) ? customerPartitionId : sellerPartitionId;
        comapnyGroupIds.add((Integer) d.get(0));
      }*/
      
      /*data = ObjectLoader.getData(DBFilter.create(Product.class).AND_EQUAL("globalProduct", null).AND_EQUAL("company", null).AND_EQUAL("service", processId), new String[]{"group_id","service_owner"});
      
      final java.util.List<Integer> globalGroupIds        = new ArrayList<>();
      for(List d:data) {
        if(equipmentsOwner == null && d.get(1) != null)
          equipmentsOwner = d.get(1).toString().equals(Service.Owner.CUSTOMER.toString())?customerPartitionId:sellerPartitionId;
        globalGroupIds.add((Integer) d.get(0));
      }*/
      
      if(equipmentsOwner != null) {
        CompanyPartitionStoreTableEditor equipments = new CompanyPartitionStoreTableEditor(equipmentsOwner);
        JCheckBox boxGroup = new JCheckBox("Показывать только соответствующие каталогу предприятия", false);
        boxGroup.addItemListener(d -> {
          equipments.getStorePositionTable().setGroups(comapnyGroupIds);
          equipments.getStorePositionTable().initData();
        });
        equipments.getToolBar().add(boxGroup);
        
        String ownerName = String.join(" - ", ObjectLoader.getMap(CompanyPartition.class, equipmentsOwner, "company_name","name").getSimpleMap().values().toArray(new String[0]));

        equipments.setTitle(ownerName);
        equipments.getStorePositionTable().setGroupCheckable(false);
        equipments.setAutoLoad(true);
        equipments.setAutoStore(true);
        equipments.getStorePositionTable().setVisibleSelectCountColumn(true);
        equipments.getOkButton().setText("В сделку");
        equipments.getStorePositionTable().setGroups(comapnyGroupIds);
        equipments.initData();
        selectedStorePositions = equipments.get();
      }
    }
    return selectedStorePositions;
  }
  
  private Object[] getTableColumns() {
    return ArrayUtils.addAll(ArrayUtils.addAll(columns.keySet().toArray(), factorColumns.values().toArray()), productFactorColumns.values().toArray());
  }
  
  private void setTableColumns() {
    header.clear();
    
    table.setColumns(getTableColumns());
    
    ColumnGroup storeColumn = new ColumnGroup("Депозитарий");
    storeColumn.add(table.findTableColumn(getColumnIndex(ColumnName.продавца)));
    storeColumn.add(table.findTableColumn(getColumnIndex(ColumnName.покупателя)));
    header.addColumnGroup(storeColumn);
    
    ColumnGroup cost = new ColumnGroup("Стоимость");
    cost.add(table.findTableColumn(getColumnIndex(ColumnName.Цена_за_ед)));
    cost.add(table.findTableColumn(getColumnIndex(ColumnName.кол_)));
    cost.add(table.findTableColumn(getColumnIndex(ColumnName.Итого)));
    header.addColumnGroup(cost);
    
    ColumnGroup select = new ColumnGroup("Отобрать");
    select.add(table.findTableColumn(getColumnIndex(ColumnName.кол)));
    select.add(table.findTableColumn(getColumnIndex(ColumnName.выдел)));
    header.addColumnGroup(select);
    
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
            if(factorColumns.get(keys[i]).getListValues() != null && !factorColumns.get(keys[i]).getListValues().equals(""))
              table.getTableFilters().addListFilter(i+columns.size());
            else table.getTableFilters().addTextFilter(i+columns.size());
            break;
          case число : 
            table.getTableFilters().addNumberFilter(i+columns.size());
            break;
        }
      }
      header.addColumnGroup(factorColumn);
    }
    
    /*if(factorColumns.size() > 0) {
      ColumnGroup factorColumnGroup = new ColumnGroup("Реквизиты объекта");
      for(int i=0;i<factorColumns.size();i++)
        factorColumnGroup.add(table.findTableColumn(columns.size()+i));
      header.addColumnGroup(factorColumnGroup);
    }*/
    
    if(productFactorColumns.size() > 0) {
      ColumnGroup factorColumnGroup = new ColumnGroup("Реквизиты продукта");
      for(int i=0;i<productFactorColumns.size();i++)
        factorColumnGroup.add(table.findTableColumn(columns.size()+factorColumns.size()+i));
      header.addColumnGroup(factorColumnGroup);
    }

    int[] zeroColumns = new int[]{0};
    if(!visibleSelectCountColumn) {
      zeroColumns = ArrayUtils.add(zeroColumns, getColumnIndex(ColumnName.кол));
      zeroColumns = ArrayUtils.add(zeroColumns, getColumnIndex(ColumnName.выдел));
    }

    if(table.getTableFilters().getFilterComponent(getColumnIndex(ColumnName.Наименование)) == null)
      table.getTableFilters().addListFilter(getColumnIndex(ColumnName.Наименование));

    table.getTableFilters().revalidate();
    table.setColumnWidthZero(zeroColumns);
  }
  
  
  
  public void setDeals(Integer[] dealIds) {
    this.dealIds = dealIds;
  }
  
  @Override
  public void objectPeriodDoubleClicked(ObjectPeriod period) {
  }

  @Override
  public void objectPeriodsSelected(java.util.List<ObjectPeriod> periods) {
    this.dealIds = null;
    periods.stream().forEach(p -> dealIds = (Integer[]) ArrayUtils.add(dealIds, p.getId()));
    initData();
  }

  @Override
  public void dayDoubleClicked(int rowIndex, Date day) {
  }

  @Override
  public void daysSelected(TreeMap<Integer, java.util.List<Date>> days) {
    this.dealIds = null;
    clear();
  }

  @Override
  public void dayWidthChanged(int dayWidth) {
  }
  
  
  
  class InitDealPositionTask implements Runnable {
    private boolean shutDown = false;
    private Integer[] ids;

    public InitDealPositionTask(Integer[] ids) {
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
      List<List> data = new Vector();
      
      if(isActive() && dealIds.length > 0) {
        waitCursor();
        
        ObjectLoader.executeQuery("SELECT "
                + "[Group(identificator)], "
                + "[Group(iden_name)] ,"
                + "[Group(iden_factorType)], "
                + "[Group(iden_listValue)] "
                + "FROM [Group] "
                + "WHERE [Group(id)] IN (SELECT DISTINCT [Equipment(group)] FROM [Equipment] WHERE [Equipment(id)] IN "
                + "(SELECT [DealPosition(equipment)] FROM [DealPosition] WHERE [DealPosition(deal)]=ANY(?)))", 
                new Object[]{dealIds})
                .stream().filter(d -> d.get(0) != null).forEach (d -> factorColumns.put((Integer)d.get(0), 
                        new FactorColumn((Integer)d.get(0), (String)d.get(1), FactorType.valueOf((String)d.get(2)), (String)d.get(3), false)));
        
        DBFilter filter = DBFilter.create(DealPosition.class).AND_IN("deal", dealIds);
        if(isOnlyNotDispatch())
          filter.AND_EQUAL("dispatchId", null);
        if(ids != null && ids.length > 0)
          filter.AND_IN("id", ids);
        
        String[] fColumns = new String[0];
        for(Integer id:factorColumns.keySet())
          fColumns = (String[]) ArrayUtils.add(fColumns, "query:(SELECT [EquipmentFactorValue(name)] FROM [EquipmentFactorValue] "
                  + "WHERE [EquipmentFactorValue(equipment)]=[DealPosition(equipment)] AND [EquipmentFactorValue(factor)]="+id+")");
        
        String[] pfColumns = new String[0];
        for(Integer id:productFactorColumns.keySet())
          pfColumns = (String[]) ArrayUtils.add(pfColumns, "query:(SELECT [EquipmentFactorValue(name)] FROM [EquipmentFactorValue] "
                  + "WHERE [EquipmentFactorValue(equipment)]=[DealPosition(equipment)] AND [EquipmentFactorValue(factor)]="+id+")");
        
        try {
          data = ObjectLoader.getData(filter, 
                  (String[]) ArrayUtils.addAll(ArrayUtils.addAll(ArrayUtils.addAll(DealPositionID.getIdColumns(), columns.values().toArray()), fColumns), pfColumns),
                  "dispatchDate");
          data.stream().forEach(d -> {
            try {
              int size = DealPositionID.size();
              java.util.List list = d.subList(0, DealPositionID.size());
              d.set(DealPositionID.size(), new DealPositionID(d.subList(0, DealPositionID.size())));
              for(int i=0;i<DealPositionID.size();i++)
                d.remove(0);
              DivisionComboBox storeCombobox = ((DealPositionID)d.get(0)).createStoreCombobox();
              storeCombobox.addActionListener(e -> {
                if(table.getCellEditor() != null)
                  table.getCellEditor().stopCellEditing();
              });
              storeCombobox.addItemListener(e -> {
                try {
                  if(ObjectLoader.executeUpdate("UPDATE [DealPosition] SET [DealPosition(targetStore)]="+((DivisionItem)e.getItem()).getId()
                          +" WHERE id="+getSelectedId()[0]) > 0)
                    ObjectLoader.sendMessage(DealPosition.class, "UPDATE", getSelectedId()[0]);
                }catch(Exception ex) {
                  Messanger.showErrorMessage(ex);
                }
              });
              d.set(getColumnIndex(ColumnName.покупателя), storeCombobox);
            }catch(Exception ex) {
              Messanger.showErrorMessage(ex);
            }
          });
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
      
      final List<List> dataVector = data;
      
      SwingUtilities.invokeLater(() -> {
        if(ids == null || ids.length == 0) {
          clear();
          setTableColumns();
        }
        
        if(isActive() && !dataVector.isEmpty()) {
          if(!selectedPositions.isEmpty()) {
            dataVector.stream().forEach((d) -> {
              BigDecimal count = selectedPositions.get(((DealPositionID)d.get(0)).getId());
              if(count != null && count.compareTo(BigDecimal.ZERO) > 0) {
                d.set(getColumnIndex(ColumnName.кол_), count);
              }
            });
          }
          
          BigDecimal amout = BigDecimal.ZERO;
          for(List d:dataVector) {
            amout = amout.add((BigDecimal)d.get(getColumnIndex(ColumnName.кол_)));
          }
          
          allCount.setText("Общее количество: "+amout);
          
          table.getTableModel().getDataVector().addAll(dataVector);
          table.getTableModel().fireTableDataChanged();
          initDataListeners.stream().forEach((InitDataListener l) -> l.initDataComplited());
        }
      });
      defaultCursor();
    }
  }

  public DivisionTable getTable() {
    return table;
  }
  
  public ArrayList<InitDataListener> initDataListeners = new ArrayList<>();
}