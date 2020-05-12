package division.editors.tables;

import bum.editors.EditorGui;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Company;
import bum.interfaces.Deal;
import bum.interfaces.DealPosition;
import bum.interfaces.DealPositionFactorValue;
import division.editors.objects.company.nCompanyEditor;
import division.scale.ObjectPeriod;
import division.scale.ObjectPeriodScaleListener;
import division.scale.PeriodScale;
import division.swing.*;
import division.swing.guimessanger.Messanger;
import division.swing.table.CellColorController;
import division.swing.table.CellEditableController;
import division.swing.table.SplitTable;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Date;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.table.TableCellEditor;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class DealObjectTableEditor extends EditorGui {
  private boolean     active     = true;
  private SplitTable  splitTable = new SplitTable(3);
  private PeriodScale scale      = new PeriodScale(10, 20, 3);
  
  private boolean          processCheck = true;
  private DivisionComboBox processComboBox = new DivisionComboBox();
  private JLabel           processLabel    = new JLabel();
  
  private DBFilter filter        = DBFilter.create(DealPosition.class);
  private DBFilter processFilter = filter.AND_FILTER();
  private DBFilter agentFilter   = filter.AND_FILTER();
  private DBFilter datesFilter   = filter.AND_FILTER();
  
  private Integer[] agents;
  private Integer[] cfcs;
  
  private String[]  staticColumns = new String[]{"contractId","processId","equipmentId","groupId","Объект","Идентификатор","Количество"};
  private Integer[] objectFactors  = new Integer[0];
  private Integer[] productFactors = new Integer[0];
  private Integer[] groups         = new Integer[0];
  private Integer[] equipments     = new Integer[0];
  
  /**
   * key   - factorId
   * value - list of groups
   */
  private ConcurrentHashMap<Integer,CopyOnWriteArrayList<Integer>> groupsForEditProductFactor = new ConcurrentHashMap<>();
  
  private ExecutorService pool = Executors.newSingleThreadExecutor();
  private DataTask dataTask;

  public DealObjectTableEditor() {
    super(null, null);
    filter.AND_EQUAL("type", "CURRENT");
    setAutoLoad(true);
    setAutoStore(true);
    initComponents();
    initEvents();
  }

  public Integer[] getCfcs() {
    return cfcs;
  }

  public void setCfcs(Integer[] cfcs) {
    this.cfcs = cfcs;
  }

  public void setAgents(Integer[] objects) {
    this.agents = objects;
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public void setActive(boolean active) {
    this.active = active;
  }

  @Override
  public Boolean okButtonAction() {
    return true;
  }

  private void initComponents() {
    splitTable.setTable(1, scale);
    ((DivisionScrollPane)splitTable.getScroll(0)).setRowHeader(true);
    splitTable.getTable(0).setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    splitTable.getTable(2).setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    splitTable.setColumns(new String[]{"contrId","Контрагент"}, 2);
    splitTable.setSortable(false);
    
    processLabel.setFont(new Font("Dialog", Font.BOLD, 20));
    
    splitTable.getScroll(0).setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    splitTable.getScroll(1).setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    splitTable.getScroll(2).setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    
    getTopPanel().setLayout(new GridBagLayout());
    getTopPanel().add(processComboBox, new GridBagConstraints(0, 0, 1, 1, 0.3, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
    getTopPanel().add(processLabel,    new GridBagConstraints(1, 0, 1, 1, 0.7, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    
    getRootPanel().setLayout(new GridBagLayout());
    getRootPanel().add(splitTable, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 5, 0, 5), 0, 0));
    
    addComponentToStore(splitTable.getTable(2),"table2");
    for(int i=0;i<splitTable.getSplits().length;i++)
      addComponentToStore(splitTable.getSplits()[i],"split"+i);
    
    splitTable.getTable(0).setCellEditableController(new CellEditableController() {
      @Override
      public boolean isCellEditable(JTable table, int modelRow, int modelColumn) {
        if(modelColumn >= staticColumns.length+objectFactors.length) {
          CopyOnWriteArrayList<Integer> groupsList = groupsForEditProductFactor.get(productFactors[modelColumn-objectFactors.length-staticColumns.length]);
          if(groupsList.contains((Integer)table.getValueAt(modelRow, 3)))
            return true;
        }
        return false;
      }
    });
    
    splitTable.getTable(0).setCellColorController(new CellColorController() {
      @Override
      public Color getCellColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        if(modelColumn >= staticColumns.length+objectFactors.length) {
          CopyOnWriteArrayList<Integer> groupsList = groupsForEditProductFactor.get(productFactors[modelColumn-objectFactors.length-staticColumns.length]);
          if(!groupsList.contains((Integer)table.getValueAt(modelRow, 3)))
            return new Color(Color.LIGHT_GRAY.getRed(), Color.LIGHT_GRAY.getGreen(), Color.LIGHT_GRAY.getBlue(), isSelect?150:50);
        }else return new Color(Color.LIGHT_GRAY.getRed(), Color.LIGHT_GRAY.getGreen(), Color.LIGHT_GRAY.getBlue(), isSelect?150:50);
        return null;
      }

      @Override
      public Color getCellTextColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        return null;
      }
    });
  }

  private void initEvents() {
    scale.addObjectPeriodScaleListener(new ObjectPeriodScaleListener() {
      @Override
      public void objectPeriodDoubleClicked(ObjectPeriod period) {
      }

      @Override
      public void objectPeriodsSelected(java.util.List<ObjectPeriod> periods) {
        periodsSelected();
      }

      @Override
      public void dayDoubleClicked(int rowIndex, Date day) {
      }

      @Override
      public void daysSelected(TreeMap<Integer, java.util.List<Date>> days) {
        periodsSelected();
      }

      @Override
      public void dayWidthChanged(int dayWidth) {
      }
    });
    
    processComboBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if(e.getStateChange() == ItemEvent.SELECTED && processCheck) {
          initData();
          DivisionItem item = (DivisionItem) processComboBox.getSelectedItem();
          processLabel.setText(item==null?"Выберите процесс":item.getName());
        }
      }
    });
    
    splitTable.getTable(2).addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 2) {
          int row = splitTable.getTable(2).rowAtPoint(e.getPoint());
          if(row != -1) {
            try {
              Integer id = (Integer) splitTable.getTable(2).getValueAt(row, 0);
              Company company = (Company) ObjectLoader.getObject(Company.class, id);
              nCompanyEditor editor = new nCompanyEditor();
              editor.setAutoLoad(true);
              editor.setAutoStore(true);
              editor.setEditorObject(company);
              editor.createDialog().setVisible(true);
            }catch(Exception ex) {
              Messanger.showErrorMessage(ex);
            }
          }
        }
      }
    });
  }
  
  private void periodsSelected() {
    Integer[] deals = scale.getSelectedIds();
    clearDealPositionFactorValues();
    if(deals.length > 0) {
      try {
        java.util.List<java.util.List> data = ObjectLoader.executeQuery("SELECT "
                + "[DealPositionFactorValue(id)],"
                + "[DealPositionFactorValue(name)],"
                + "[DealPositionFactorValue(equipment_id)],"
                + "[DealPositionFactorValue(factor)] "
                + "FROM [DealPositionFactorValue] WHERE [DealPositionFactorValue(dealPosition)] IN "
                + "(SELECT [DealPosition(id)] FROM [DealPosition] WHERE [DealPosition(deal)]=ANY(?)) "
                + "ORDER BY [DealPositionFactorValue(equipment_id)]", true, new Object[]{deals});
        
        int[] idexToRemove = new int[0];
        for(int i=data.size()-1;i>=0;i--) {
          int length = idexToRemove.length;
          java.util.List row = data.get(i);
          for(int j=data.size()-1;j>=0;j--) {
            if(j != i) {
              java.util.List d = data.get(j);
              if(!row.get(1).equals(d.get(1)) && row.get(2).equals(d.get(2)) && row.get(3).equals(d.get(3))) {
                if(!ArrayUtils.contains(idexToRemove, j))
                  idexToRemove = (int[]) ArrayUtils.add(idexToRemove, j);
              }
            }
          }
          if(idexToRemove.length > length && !ArrayUtils.contains(idexToRemove, i))
            idexToRemove = (int[]) ArrayUtils.add(idexToRemove, i);
        }
        
        if(idexToRemove.length > 0) {
          Arrays.sort(idexToRemove);
          for(int i=idexToRemove.length-1;i>=0;i--)
            data.remove(idexToRemove[i]);
        }

        for(java.util.List d:data) {
          DivisionTableModel model = splitTable.getTable(0).getTableModel();
          for(int i=0;i<model.getRowCount();i++) {
            if(model.getValueAt(i, 2).equals(d.get(2))) {
              int column = staticColumns.length+objectFactors.length+ArrayUtils.indexOf(productFactors, (Integer)d.get(3));
              if(model.getValueAt(i, column) instanceof JComboBox) {
                ((JComboBox)model.getValueAt(i, column)).setEnabled(false);
                ((JComboBox)model.getValueAt(i, column)).setSelectedItem(d.get(1));
                ((JComboBox)model.getValueAt(i, column)).setEnabled(true);
              }else model.setValueAt(d.get(1), i, column);
            }
          }
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
    splitTable.repaint();
  }
  
  private void clearDealPositionFactorValues() {
    DivisionTableModel model = splitTable.getTable(0).getTableModel();
    for(int j=0;j<model.getRowCount();j++) {
      for(int i=0;i<productFactors.length;i++) {
        int column = i+staticColumns.length+objectFactors.length;
        if(model.getValueAt(j, column) instanceof JComboBox) {
          ((JComboBox)model.getValueAt(j, column)).setEnabled(false);
          ((JComboBox)model.getValueAt(j, column)).setSelectedIndex(-1);
          ((JComboBox)model.getValueAt(j, column)).setEnabled(true);
        }else model.setValueAt(null, i, column);
      }
    }
  }
  
  private Color getColor(String status) {
    Color c = Color.LIGHT_GRAY;
    switch(status) {
      case "Старт": c = Color.YELLOW;break;
      case "Проект": c = Color.LIGHT_GRAY;break;
      case "Финиш": c = Color.GREEN;break;
      case "Принято": c = Color.RED;break;
      case "Расчёт": c = Color.BLUE;break;
      case "Неопределённость": c = Color.BLACK;break;
    }
    return c;
  }
  
  private void changeProductFactor(Integer equipmentId, Integer factorId, String value) {
    RemoteSession session = null;
    try {
      Integer[] deals = scale.getSelectedIds();
      session = ObjectLoader.createSession();
      java.util.List<java.util.List> data = session.executeQuery("SELECT "
              + "[DealPositionFactorValue(id)],"
              + "[DealPositionFactorValue(deal_id)] "
              + "FROM [DealPositionFactorValue] "
              + "WHERE [DealPositionFactorValue(deal_id)]=ANY(?) AND [DealPositionFactorValue(equipment_id)]=? AND "
              + "[DealPositionFactorValue(factor)]=?", new Object[]{deals,equipmentId,factorId});
      Integer[] dealPositionFactorValues = new Integer[0];
      for(java.util.List d:data) {
        dealPositionFactorValues = (Integer[]) ArrayUtils.add(dealPositionFactorValues, (Integer) d.get(0));
        deals = (Integer[]) ArrayUtils.removeElement(deals, (Integer) d.get(1));
      }
      
      for(Integer deal:deals) {
        session.executeUpdate("INSERT INTO [DealPositionFactorValue] "
                + "([DealPositionFactorValue(factor)],[DealPositionFactorValue(dealPosition)]) VALUES "
                + "(?,(SELECT [DealPosition(id)] FROM [DealPosition] WHERE [DealPosition(deal)]=? AND "
                + "[DealPosition(equipment)]=? AND [DealPosition(tmp)]=false AND [DealPosition(type)]='CURRENT'))", 
                new Object[]{factorId,deal,equipmentId});
        dealPositionFactorValues = (Integer[]) ArrayUtils.add(dealPositionFactorValues, 
                (Integer) session.executeQuery("SELECT MAX([DealPositionFactorValue(id)]) FROM [DealPositionFactorValue]").get(0).get(0));
      }
      
      if(dealPositionFactorValues.length > 0) {
        session.executeUpdate("UPDATE [DealPositionFactorValue] SET [DealPositionFactorValue(name)]=? WHERE [DealPositionFactorValue(id)]=ANY(?)",
                new Object[]{value,dealPositionFactorValues});
        session.addEvent(DealPositionFactorValue.class, "UPDATE", dealPositionFactorValues);
      }
      
      session.commit();
    }catch(Exception ex) {
      ObjectLoader.rollBackSession(session);
      Messanger.showErrorMessage(ex);
    }
  }
  
  @Override
  public void initData() {
    splitTable.clear();
    objectFactors  = new Integer[0];
    productFactors = new Integer[0];
    groups         = new Integer[0];
    equipments     = new Integer[0];
    groupsForEditProductFactor.clear();
    
    if(dataTask != null) {
      dataTask.setShutDoun(true);
      dataTask = null;
    }
    if(isActive()) {
      pool.submit(dataTask = new DataTask());
    }
  }

  @Override
  public void initTargets() {
  }
  
  class DataTask implements Runnable {
    private boolean shutDoun = false;

    public boolean isShutDoun() {
      return shutDoun;
    }

    public void setShutDoun(boolean shutDoun) {
      this.shutDoun = shutDoun;
    }
    
    public int rowAtEquipmentId(Vector<Vector> ldata, Integer equipmentId) {
      //Vector<Vector> ldata = splitTable.getTable(0).getTableModel().getDataVector();
      for(int i=0;i<ldata.size();i++)
        if(ldata.get(i).get(2).equals(equipmentId))
          return i;
      return -1;
    }
    
    public DivisionComboBox createCombobox(final Integer equipmentId, final Integer factorId, String values) {
      final DivisionComboBox comboBox = new DivisionComboBox();
      comboBox.addItems(values.split(";"));
      comboBox.setSelectedIndex(-1);
      comboBox.addItemListener((ItemEvent e) -> {
        if(e.getStateChange() == ItemEvent.SELECTED && comboBox.isEnabled()) {
          changeProductFactor(equipmentId, factorId, (String)e.getItem());
          TableCellEditor editor = splitTable.getTable(0).getCellEditor();
          if(editor != null)
            editor.stopCellEditing();
        }
      });
      return comboBox;
    }
    
    @Override
    public void run() {
      setCursor(new Cursor(Cursor.WAIT_CURSOR));
      try {
        if(isShutDoun())
          return;
        splitTable.setColumns(staticColumns, 0);
        splitTable.getTable(2).setColumnWidthZero(new int[]{0});
        splitTable.getTable(0).setColumnWidthZero(new int[]{0,1,2,3});
        Vector<Vector> ldata = splitTable.getTable(0).getTableModel().getDataVector();
        Vector<Vector> cdata = splitTable.getTable(1).getTableModel().getDataVector();
        Vector<Vector> rdata = splitTable.getTable(2).getTableModel().getDataVector();
        if(isShutDoun())
          return;
        
        if(isShutDoun())
          return;
        
        processCheck = false;
        DivisionItem selectedItem = (DivisionItem) processComboBox.getSelectedItem();
        processComboBox.clear();
        if(isShutDoun())
          return;
        
        DBFilter agentFilter_ = DBFilter.create(Deal.class);
        if(agents.length > 0 && cfcs.length > 0) {
          agentFilter_.AND_IN("seller_id", agents);
          agentFilter_.AND_IN("seller_cfc_id", cfcs);
          agentFilter_.OR_IN("customer_id", agents);
          agentFilter_.AND_IN("customer_cfc_id", cfcs);
        }else if(agents.length > 0 && cfcs.length == 0) {
          agentFilter_.AND_IN("seller_id", agents);
          agentFilter_.OR_IN("customer_id", agents);
        }else if(agents.length == 0 && cfcs.length > 0) {
          agentFilter_.AND_IN("seller_cfc_id", cfcs);
          agentFilter_.OR_IN("customer_cfc_id", cfcs);
        }
        
        if(isShutDoun())
          return;
        java.util.List<java.util.List> data = ObjectLoader.getData(agentFilter_, new String[]{"service","service_name"});
        for(java.util.List d:data) {
          DivisionItem item = new DivisionItem((Integer)d.get(0), (String)d.get(1), "Service");
          if(!ArrayUtils.contains(processComboBox.getItems(), item))
            processComboBox.addItem(item);
        }
        if(selectedItem != null) {
          processComboBox.setSelectedItem(selectedItem);
        }else {
          if(processComboBox.getItemCount() > 0) {
            processComboBox.setSelectedIndex(0);
            selectedItem = (DivisionItem) processComboBox.getSelectedItem();
          }else {
            processComboBox.setSelectedIndex(-1);
            setShutDoun(true);
          }
        }
        processCheck = true;
        
        if(isShutDoun())
          return;
        
        processFilter.clear();
        processFilter.AND_EQUAL("process_id", selectedItem.getId());
        
        datesFilter.clear();
        datesFilter.AND_DATE_BETWEEN("deal_start_date", scale.getStartDate(), scale.getEndDate());
        datesFilter.AND_DATE_BETWEEN("deal_end_date", scale.getStartDate(), scale.getEndDate());
        
        agentFilter.clear();
        
        if(agents.length > 0 && cfcs.length > 0) {
          agentFilter.AND_IN("seller_id", agents);
          agentFilter.AND_IN("seller_cfc_id", cfcs);
          agentFilter.OR_IN("customer_id", agents);
          agentFilter.AND_IN("customer_cfc_id", cfcs);
        }else if(agents.length > 0 && cfcs.length == 0) {
          agentFilter.AND_IN("seller_id", agents);
          agentFilter.OR_IN("customer_id", agents);
        }else if(agents.length == 0 && cfcs.length > 0) {
          agentFilter.AND_IN("seller_cfc_id", cfcs);
          agentFilter.OR_IN("customer_cfc_id", cfcs);
        }
        
        String[] fields = new String[]{
          /*0*/"id",
          /*1*/"process_id",
          /*2*/"deal_start_date",
          /*3*/"deal_end_date",
          /*4*/"equipment",
          /*5*/"identity_id",
          /*6*/"identity_name",
          /*7*/"identity_value",
          /*8*/"equipment_amount",
          /*9*///"cfc_name",
          /*9*/"query:getCompanyPartitionName([DealPosition(customer_partition_id)])",
          /*10*/"query:getstatus([DealPosition(deal)])",
          /*11*/"contract_id",
          /*12*/"customer_partition_id",
          /*13*/"seller_partition_id",
          /*14*/"seller_id",
          /*15*/"deal",
          /*16*/"group_name",
          /*17*/"group_id",
          /*18*/"query:getCompanyPartitionName([DealPosition(seller_partition_id)])",
          /*19*/"customer_id"
        };
        
        if(isShutDoun())
          return;
        data = ObjectLoader.getData(filter, fields, true);
        
        Integer[] sellers = new Integer[0];
        Integer contractId = null;
        Integer processId = null;
        Integer dealId = null;
        Integer equipmentId = null;
        for(java.util.List d:data) {
          if(isShutDoun())
            return;
          
          if(!ArrayUtils.contains(sellers, d.get(14)))
            sellers = (Integer[]) ArrayUtils.add(sellers, d.get(14));
          
          if(!ArrayUtils.contains(groups, d.get(17)))
            groups = (Integer[]) ArrayUtils.add(groups, d.get(17));
          
          equipmentId = (Integer) d.get(4);
          
          if(!ArrayUtils.contains(equipments, equipmentId))
            equipments = (Integer[]) ArrayUtils.add(equipments, equipmentId);
          
          int row = rowAtEquipmentId(ldata, equipmentId);
          if(row == -1) {
            processId                   = (Integer) d.get(1);
            String  identityValue       = (String)  d.get(7);
            Integer amount              = (Integer) d.get(8);
            Integer sellerId            = (Integer) d.get(14);
            Integer customerId          = (Integer) d.get(19);
            String customerName         = (String)  d.get(9);
            String sellerName           = (String)  d.get(18);
            contractId                  = (Integer) d.get(11);
            //Заполняем левую таблицу
            ldata.add(new Vector(Arrays.asList(new Object[]{contractId,processId,equipmentId,d.get(17),d.get(16),identityValue,amount})));
            //Заполняем строку шкалы
            Vector cd = new Vector();
            cd.setSize(splitTable.getTable(1).getColumnCount());
            cdata.add(cd);
            //Заполняем контрагента
            rdata.add(agents.length==0||ArrayUtils.contains(agents, sellerId)?new Vector(Arrays.asList(new Object[]{customerId,customerName})):new Vector(Arrays.asList(new Object[]{sellerId,sellerName})));
          }
          
          dealId = (Integer) d.get(15);
          //Вставляем период
          scale.addPeriod(
                  row != -1?row:(splitTable.getTable(0).getRowCount()-1), 
                  dealId, 
                  (java.sql.Date)d.get(2), 
                  (java.sql.Date)d.get(3), 
                  getColor((String) d.get(10)), 
                  "");
        }
        
        if(isShutDoun())
          return;
        //Получаю значения реквизитов
        data = ObjectLoader.executeQuery("SELECT "
                + "[EquipmentFactorValue(id)],"
                + "[EquipmentFactorValue(equipment_id)],"
                + "[EquipmentFactorValue(name)],"
                + "[EquipmentFactorValue(factor)],"
                + "[EquipmentFactorValue(factor_name)] "
                + "FROM [EquipmentFactorValue] "
                + "WHERE [EquipmentFactorValue(equipment_id)]=ANY(?) AND "
                + "[EquipmentFactorValue(factor)] NOT IN (SELECT [Group(identificator)] FROM [Group] WHERE [Group(id)]=ANY(?))", true, new Object[]{equipments,groups});
        for(java.util.List d:data) {
          if(isShutDoun())
            return;
          if(!ArrayUtils.contains(objectFactors, d.get(3))) {
            objectFactors = (Integer[]) ArrayUtils.add(objectFactors, d.get(3));
            splitTable.getTable(0).getTableModel().addColumn(d.get(4));
          }
          int row    = rowAtEquipmentId(ldata, (Integer)d.get(1));
          int fIndex = ArrayUtils.indexOf(objectFactors, d.get(3));
          if(row != -1 && fIndex != -1) {
            ldata.get(row).set(fIndex+staticColumns.length, d.get(2));
          }
        }
        
        if(isShutDoun())
          return;
        //Получаю реквизиты всех продуктов, связанных с текущими объектами, выделенным процессом и текущим прайс-листом выделенных предприятий
        data = ObjectLoader.executeQuery("SELECT "
                + "[Product(group)],"
                + "[Product(factors):target],"
                + "[Factor(name)],"
                + "[Factor(listValues)] "
                + "FROM [Product(factors):table],[Product],[Factor] "
                + "WHERE [Factor(id)]=[Product(factors):target] AND "
                + "[Product(id)]=[Product(factors):object] AND "
                + "[Product(factors):object] IN (SELECT [Product(id)] FROM [Product] "
                + "WHERE [Product(priceList)]=(SELECT [PriceList(id)] FROM [PriceList] "
                + "WHERE [PriceList(tmp)]=false AND [PriceList(type)]='CURRENT' AND [PriceList(company)]=ANY(?)) AND "
                + "[Product(tmp)]=false AND [Product(type)]='CURRENT' AND "
                + "[Product(group)]=ANY(?) AND [Product(service)]=?)", true, new Object[]{sellers,groups,selectedItem.getId()});
        
        for(java.util.List d:data) {
          if(isShutDoun())
            return;
          if(!ArrayUtils.contains(productFactors, d.get(1))) {
            productFactors = (Integer[]) ArrayUtils.add(productFactors, d.get(1));
            splitTable.getTable(0).getTableModel().addColumn(d.get(2));
          }
          
          CopyOnWriteArrayList<Integer> groupsList = groupsForEditProductFactor.get((Integer)d.get(1));
          if(groupsList == null) {
            groupsList = new CopyOnWriteArrayList<>();
            groupsForEditProductFactor.put((Integer)d.get(1), groupsList);
          }
          if(!groupsList.contains((Integer)d.get(0)))
            groupsList.add((Integer)d.get(0));
          
          if(d.get(3) != null && !d.get(3).equals("")) {
            //Вставляем комбобоксы
            for(Vector row:ldata) {
              if(row.get(3).equals(d.get(0))) {
                row.set(staticColumns.length+objectFactors.length+ArrayUtils.indexOf(productFactors, d.get(1)), 
                        createCombobox((Integer)row.get(2),(Integer)d.get(1),(String)d.get(3)));
              }
            }
          }
        }
        
        splitTable.getTable(0).setColumnWidthZero(new int[]{0,1,2,3});
        splitTable.getTable(2).setColumnWidthZero(new int[]{0});
        
        if(isShutDoun())
          return;
        SwingUtilities.invokeLater(() -> {
          if(isShutDoun())
            return;
          splitTable.getTable(0).getTableModel().fireTableDataChanged();
          splitTable.getTable(1).getTableModel().fireTableDataChanged();
          splitTable.getTable(2).getTableModel().fireTableDataChanged();
          scale.scrollToCurrentDate();
        });
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }finally {
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
    }
  }
}