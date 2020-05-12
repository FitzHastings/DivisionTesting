package division.editors.contract;

import division.swing.guimessanger.Messanger;
import bum.editors.EditorController;
import bum.editors.EditorGui;
import bum.editors.actions.ActionMacrosUtil;
import bum.editors.util.DivisionTarget;
import bum.editors.util.ObjectLoader;
import bum.interfaces.*;
import division.editors.tables.DealActionsPanel;
import division.editors.tables.DealCommentTableEditor;
import division.editors.tables.DealPositionTableEditor;
import static division.editors.tables.DealPositionTableEditor.getDeals;
import division.editors.tables.ServiceTableEditor;
import division.fx.PropertyMap;
import division.scale.*;
import division.swing.*;
import division.swing.table.SplitTable;
import division.util.FileLoader;
import division.util.Hronometr;
import division.util.Utility;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import mapping.MappingObject;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class PlanGraphic extends JPanel {
  private JMenuItem copy  = new JMenuItem("Копировать");
  private JMenuItem paste = new JMenuItem("Вставить");
  
  private ExecutorService pool = Executors.newSingleThreadExecutor();
  
  private DivisionTarget dealDivisionTarget;
  
  private JToolBar           tool                  = new JToolBar();
  private DivisionToolButton addToolButton         = new DivisionToolButton(FileLoader.getIcon("Add16.gif"),"Добавить");
  //private DivisionToolButton editToolButton        = new DivisionToolButton(FileLoader.getIcon("Edit16.gif"),"Редактировать");
  private DivisionToolButton removeToolButton      = new DivisionToolButton(FileLoader.getIcon("Delete16.gif"),"Удалить");
  
  private JLabel selectedDealsCount = new JLabel("Выделенных сделок: 0 на сумму: 0");
  
  private SplitTable     splitTable            = new SplitTable(3);
  private DivisionTable  leftTable             = new DivisionTable() {
    @Override
    public void paint(Graphics g) {
      super.paint(g);
      Integer id;
      for(int i=0;i<getRowCount();i++) {
        id = (Integer) getValueAt(i, 3);
        Integer[] rows = new Integer[0];
        for(int j=0;j<getRowCount();j++) {
          if(i != j && id != null && id.equals(getValueAt(j, 3))) {
            rows = (Integer[]) ArrayUtils.add(rows, j);
          }
        }
        if(rows.length > 0) {
          Rectangle rect = getCellRect(i, 1, true);
          rect.height += rect.height*rows.length;
          Graphics2D g2 = (Graphics2D) g;
          g2.setColor(Color.BLACK);
          g2.setStroke(new BasicStroke(1));
          g2.drawLine(0, rect.y, getWidth(), rect.y);
          g2.drawLine(0, rect.y+rect.height, getWidth(), rect.y+rect.height);
          i = rows[rows.length-1];
        }
      }
    }
  };
  
  private DivisionTable      rightTable             = new DivisionTable() {
    @Override
    public void paint(Graphics g) {
      super.paint(g);
      Integer id;
      for(int i=0;i<leftTable.getRowCount();i++) {
        id = (Integer) leftTable.getValueAt(i, 3);
        Integer[] rows = new Integer[0];
        for(int j=0;j<leftTable.getRowCount();j++) {
          if(i != j && id != null && id.equals(leftTable.getValueAt(j, 3))) {
            rows = (Integer[]) ArrayUtils.add(rows, j);
          }
        }
        if(rows.length > 0) {
          Rectangle rect = getCellRect(i, 1, true);
          rect.height += rect.height*rows.length;
          Graphics2D g2 = (Graphics2D) g;
          g2.setColor(Color.BLACK);
          g2.setStroke(new BasicStroke(1));
          g2.drawLine(0, rect.y, getWidth(), rect.y);
          g2.drawLine(0, rect.y+rect.height, getWidth(), rect.y+rect.height);
          i = rows[rows.length-1];
        }
      }
    }
  };
  
  private PeriodScale    scale                 = new PeriodScale(40, 15, 5) {
  //private PeriodScale    scale                 = new PeriodScale(12, 24, 5) {
    @Override
    protected String getToolTipText(int row, int column) {
      String toolTip = super.getToolTipText(row, column)+"<br/><b>"+splitTable.getTable(0).getValueAt(row, 1)+"</b>";
      return toolTip;
    }

    @Override
    public void paint(Graphics g) {
      super.paint(g);
      Integer id;
      for(int i=0;i<leftTable.getRowCount();i++) {
        id = (Integer) leftTable.getValueAt(i, 3);
        Integer[] rows = new Integer[0];
        for(int j=0;j<leftTable.getRowCount();j++) {
          if(i != j && id != null && id.equals(leftTable.getValueAt(j, 3))) {
            rows = (Integer[]) ArrayUtils.add(rows, j);
          }
        }
        if(rows.length > 0) {
          Rectangle rect = getCellRect(i, 1, true);
          rect.height += rect.height*rows.length;
          Graphics2D g2 = (Graphics2D) g;
          g2.setColor(Color.BLACK);
          g2.setStroke(new BasicStroke(1));
          g2.drawLine(0, rect.y, getWidth(), rect.y);
          g2.drawLine(0, rect.y+rect.height, getWidth(), rect.y+rect.height);
          i = rows[rows.length-1];
        }
      }
    }
  };
  
  private final DBFilter rootFilter         = DBFilter.create(Deal.class);
  private final DBFilter planGraphicFilter  = rootFilter.AND_FILTER();
  private final DBFilter planGraphicDateFilter  = planGraphicFilter.AND_FILTER();
  private final DBFilter statusFilter       = rootFilter.AND_FILTER();

  private final DivisionSplitPane split = new DivisionSplitPane(JSplitPane.VERTICAL_SPLIT);
  private final JTabbedPane   tabb  = new JTabbedPane();
  private final DealCommentTableEditor dealCommentsTableEditor = new DealCommentTableEditor();
  private final DealActionsPanel actionsPanel = new DealActionsPanel(scale);
  private final DealPositionTableEditor dealPositionTableEditor = new DealPositionTableEditor() {
    @Override
    public void addButton() {
      //SwingUtilities.invokeLater(() -> {
        if(!scale.getSelectedPeriods().isEmpty())
          addDealPositions();
        else createDeal();
      //});
    }
  };
  
  
  
  private final JPanel colorPanel = new JPanel(new GridBagLayout());
  private final ColorButton redButton   = new ColorButton();
  private final ColorButton blueButton  = new ColorButton();
  private final ColorButton blackButton = new ColorButton();
  private final ColorButton yelowButton = new ColorButton();
  
  class ColorButton extends JToggleButton {
    public ColorButton() {
      setSelected(false);
    }
    
    @Override
    public void paint(Graphics g) {
      
      ((Graphics2D)g).setStroke(new BasicStroke(1));
      
      g.setColor(isSelected()?Color.BLACK:Color.LIGHT_GRAY);
      g.drawRect(0, 0, getWidth()-1, getHeight()-1);
      
      g.setColor(isSelected()?getBackground().darker():getBackground().brighter());
      g.fillRect(2, 2, getWidth()-4, getHeight()-4);
    }
  }
  
  private final JPanel               durationContract = new JPanel(new GridBagLayout());
  private final JPanel               startPanel       = new JPanel(new GridBagLayout());
  private final DivisionCalendarComboBox startDate        = new DivisionCalendarComboBox();
  private final JPanel               durationPanel    = new JPanel(new GridBagLayout());
  private final JRadioButton         durButton        = new JRadioButton();
  private final JSpinner             durationCount    = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
  private final DivisionComboBox     durationType     = new DivisionComboBox();
  private final JPanel               endPanel         = new JPanel(new GridBagLayout());
  private final JRadioButton         endButton        = new JRadioButton();
  private final DivisionCalendarComboBox endDate          = new DivisionCalendarComboBox();
  private final ButtonGroup          groupButton      = new ButtonGroup();
  
  private final JPanel dealPositionPanel = new JPanel(new GridBagLayout());
  private final JLabel processName       = new JLabel();
  private final JPanel planGraphicPanel  = new JPanel(new GridBagLayout());
  
  private Integer contract = null;
  
  private Rectangle scaleVisibleRect = null;

  public PlanGraphic() {
    super(new GridBagLayout());
    initComponents();
    initEvents();
  }

  public Integer getContract() {
    return contract;
  }

  public void setContract(Integer contract) {
    this.contract = contract;
  }

  public Integer getCustomerPartition() {
    int row = splitTable.getTable(0).getSelectedRow();
    if(row >= 0)
      return getCustomerPartition(row);
    else {
      Integer id = null;
      if(splitTable.getTable(0).getRowCount() > 0) {
        id = getCustomerPartition(0);
        for(int i=1;i<splitTable.getTable(0).getRowCount();i++) {
          if(!id.equals(getCustomerPartition(i))) {
            id = null;
            break;
          }
        }
      }
      return id;
    }
  }

  public List getStoreComponents() {
    ArrayList components = new ArrayList();
    components.addAll(dealCommentsTableEditor.getComponentsToStore());
    split.setName("split1");
    components.add(split);
    components.addAll(actionsPanel.getComponentsToStore());
    components.addAll(dealPositionTableEditor.getComponentsToStore());
    for(int i=0;i<splitTable.getSplits().length;i++)
      components.add(splitTable.getSplits()[i]);
    for(int i=0;i<splitTable.getTables().length;i++)
      if(i != 1)
        components.add(splitTable.getTable(i));
    scale.setName("scale_fore_plangraphic");
    components.add(scale);
    return components;
  }
  
  public Date getStartDate() {
    return startDate.getDate();
  }
  
  public Date getEndDate() {
    return endDate.getDate();
  }
  
  private void setEndDate() {
    Calendar c = Calendar.getInstance();
    c.setTime(startDate.getDate());
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    int count = (Integer)durationCount.getValue();
    long time = (long)count*24*60*60*1000;
    switch(durationType.getSelectedIndex()) {
      case 0:
        endDate.setDateInCalendar(new Date(c.getTimeInMillis()+time));
        break;
      case 1:
        int y = (int)((count+c.get(Calendar.MONTH))/12);
        c.roll(Calendar.YEAR, y);
        c.roll(Calendar.MONTH, count);
        endDate.setDateInCalendar(c.getTime());
        break;
      case 2:
        endDate.setDateInCalendar(new Date(c.getTimeInMillis()+time*365));
        break;
    }
    dealPositionTableEditor.setContractEndDate(endDate.getDate());
    setDurationOnPlanGraphic();
  }
  
  private void setDurationOnPlanGraphic() {
    removeColorLabel("Начало действия договора");
    removeColorLabel("Окончание действия договора");
    addColorLabel("Начало действия договора",startDate.getDate(true), Color.GREEN);
    addColorLabel("Окончание действия договора",endDate.getDate(true), Color.RED);
  }
  
  private void setDurationActive() {
    durationCount.setEnabled(durButton.isSelected());
    durationType.setEnabled(durButton.isSelected());
    endDate.setEnabled(!durButton.isSelected());
    if(!durButton.isSelected())
      durationCount.setValue(0);
    setEndDate();
  }
  
  private void initComponents() {
    scale.setName("in-plan");
    scale.setDragEnabled(true);
    
    actionsPanel.getCreatedDocumentTableEditor().getTable().setColumnWidthZero(new int[]{5,6});
    
    redButton.setBackground(Color.RED);
    blueButton.setBackground(Color.BLUE);
    blackButton.setBackground(Color.DARK_GRAY);
    yelowButton.setBackground(Color.YELLOW);
    
    redButton.setMinimumSize(new Dimension(30, 12));
    redButton.setMaximumSize(new Dimension(30, 12));
    redButton.setPreferredSize(new Dimension(30, 12));
    
    blueButton.setMinimumSize(new Dimension(30, 12));
    blueButton.setMaximumSize(new Dimension(30, 12));
    blueButton.setPreferredSize(new Dimension(30, 12));
    
    blackButton.setMinimumSize(new Dimension(30, 12));
    blackButton.setMaximumSize(new Dimension(30, 12));
    blackButton.setPreferredSize(new Dimension(30, 12));
    
    yelowButton.setMinimumSize(new Dimension(30, 12));
    yelowButton.setMaximumSize(new Dimension(30, 12));
    yelowButton.setPreferredSize(new Dimension(30, 12));
    
    colorPanel.add(redButton,   new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    colorPanel.add(blueButton,  new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    colorPanel.add(blackButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    colorPanel.add(yelowButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    
    
    splitTable.getStatusBar().add(selectedDealsCount);
    splitTable.getStatusBar().add(colorPanel);
    
    splitTable.setTable(0, leftTable);
    splitTable.setTable(2, rightTable);
    
    for(int i=0;i<splitTable.getSplits().length;i++)
      splitTable.getSplits()[i].setName("splitFroTableSplit"+i);
    
    for(int i=0;i<splitTable.getTables().length;i++)
      if(i != 1)
        splitTable.getTable(i).setName("table"+i);
    
    splitTable.getTable(0).setFocusable(false);
    splitTable.getTable(2).setFocusable(false);
    //scale.setFocusable(false);
    splitTable.getTable(0).setColumns("id","Процесс","tempProcess","customerPartition","Подразделение");
    splitTable.setSortable(false);
    splitTable.getTable(0).setColumnWidthZero(0,2,3);
    splitTable.setTable(1, scale);
    splitTable.getTable(2).setColumns("ЦФУ");
    splitTable.getTable(2).setCellEditableController((JTable table, int modelRow, int modelColumn) -> splitTable.getTable(2).getValueAt(modelRow, modelColumn) != null);
    
    splitTable.getTable(0).setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    splitTable.getTable(2).setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    
    splitTable.getScroll(0).setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    splitTable.getScroll(1).setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    splitTable.getScroll(2).setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    
    splitTable.getScroll(1).getHorizontalScrollBar().setUnitIncrement(30);
    
    groupButton.add(durButton);
    groupButton.add(endButton);
    
    startPanel.setBorder(BorderFactory.createTitledBorder("Дата начала"));
    startPanel.add(startDate, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 2, 5), 0, 0));
    durationPanel.setBorder(BorderFactory.createTitledBorder("Длительность"));
    durationPanel.add(durButton,     new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(2, 5, 2, 5), 0, 0));
    durationPanel.add(durationCount, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(2, 5, 2, 5), 0, 0));
    durationPanel.add(durationType,  new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 2, 5), 0, 0));
    endPanel.setBorder(BorderFactory.createTitledBorder("Дата окончания"));
    endPanel.add(endButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(2, 5, 2, 5), 0, 0));
    endPanel.add(endDate,   new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 2, 5), 0, 0));
    durationContract.add(startPanel,    new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 2, 5), 0, 0));
    durationContract.add(durationPanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 2, 5), 0, 0));
    durationContract.add(endPanel,      new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 2, 5), 0, 0));
    
    processName.setFont(new Font("Dialog", Font.BOLD, 14));
    
    dealPositionPanel.add(processName, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    
    JPanel topPanel    = new JPanel(new GridBagLayout());
    JPanel bottomPanel = new JPanel(new GridBagLayout());
    bottomPanel.add(dealPositionPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    bottomPanel.add(tabb,              new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    
    tool.setFloatable(false);
    tool.add(addToolButton);
    //tool.add(editToolButton);
    tool.add(removeToolButton);
    tool.addSeparator();

    topPanel.add(tool,       new GridBagConstraints(0, 0, 3, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    topPanel.add(splitTable, new GridBagConstraints(0, 1, 3, 1, 1.0, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 5, 0, 5), 0, 0));
    
    split.add(topPanel, JSplitPane.TOP);
    split.add(bottomPanel, JSplitPane.BOTTOM);
    
    dealPositionTableEditor.setVisibleOkButton(false);
    
    tabb.add("Позиции", dealPositionTableEditor.getGUI());
    tabb.add("События", actionsPanel);
    tabb.add("Коментарии", dealCommentsTableEditor);
    
    /*partitionComboBox.setFont(new Font("Dialog",Font.BOLD,16));
    partitionComboBox.setPreferredSize(new Dimension(300, 25));
    partitionComboBox.setMinimumSize(new Dimension(300, 25));
    partitionComboBox.setFocusable(false);*/
    
    //planGraphicPanel.add(partitionComboBox, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    planGraphicPanel.add(split,             new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    planGraphicPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY),
            "План-график",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,new Font("Dialog",Font.BOLD,16)));
    
    add(durationContract, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    add(planGraphicPanel,             new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 5, 0, 5), 0, 0));
    
    
    initTabb();
  }
  
  private void initEvents() {
    redButton.addItemListener(e -> setFilterStatus());
    
    blueButton.addItemListener(e -> setFilterStatus());
    
    blackButton.addItemListener(e -> setFilterStatus());
    
    yelowButton.addItemListener(e -> setFilterStatus());
    
    /*editToolButton.addActionListener((ActionEvent e) -> {
      Integer[] tempProcesses = getSelectedTempProcess();
      if(tempProcesses.length > 0) {
        try {
          TempProcessEditor editor = new TempProcessEditor();
          editor.setAutoLoad(true);
          editor.setAutoStore(true);
          editor.setEditorObject(ObjectLoader.getObject(ContractProcess.class, tempProcesses[0]));
          editor.createDialog(true).setVisible(true);
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });*/
    
    addToolButton.addActionListener((ActionEvent e) -> {
      EditorController.waitCursor();
      try {
        List<List> data = ObjectLoader.executeQuery("SELECT "
                + "[Contract(sellerCompany)],"
                + "[Contract(customerCompany)],"
                + "[Contract(sellerCompanyPartition)] "
                + "FROM [Contract] "
                + "WHERE [Contract(id)]="+getContract());
        if(!data.isEmpty()) {
          Integer seller   = (Integer) data.get(0).get(0);
          Integer customer = (Integer) data.get(0).get(1);
          if(seller != null) {
            data = ObjectLoader.getData(DBFilter.create(Product.class).AND_EQUAL("company", seller).AND_EQUAL("type", Product.Type.CURRENT), "service");
            if(!data.isEmpty()) {
              Integer[] processes = new Integer[0];
              for(List d:data)
                processes = (Integer[]) ArrayUtils.add(processes, d.get(0));

              DivisionComboBox customerPartitionComboBox = new DivisionComboBox();
              data = ObjectLoader.getData(DBFilter.create(CompanyPartition.class).AND_EQUAL("company", customer).AND_EQUAL("tmp", false).AND_EQUAL("type", MappingObject.Type.CURRENT),
                      new String[]{"id","name"});
              for(List d:data)
                customerPartitionComboBox.addItem(new DivisionItem((Integer) d.get(0), (String) d.get(1), "CompanyPartition"));

              customerPartitionComboBox.setSelectedIndex(-1);
              customerPartitionComboBox.setPreferredSize(new Dimension(200, customerPartitionComboBox.getPreferredSize().height));

              final ServiceTableEditor serviceTableEditor = new ServiceTableEditor();
              serviceTableEditor.setVisibleOkButton(false);
              serviceTableEditor.getButtonsPanel().add(customerPartitionComboBox);
              serviceTableEditor.getClientFilter().AND_IN("id", processes);
              serviceTableEditor.setDoubleClickSelectable(true);
              serviceTableEditor.setSelectingOnlyLastNode(true);
              serviceTableEditor.setAutoLoad(true);
              serviceTableEditor.setAutoStore(true);
              serviceTableEditor.setAdministration(false);
              serviceTableEditor.initData();

              customerPartitionComboBox.addItemListener((ItemEvent e1) -> {
                if (e1.getStateChange() == ItemEvent.SELECTED) {
                  serviceTableEditor.okButtonAction();
                }
              });

              processes = serviceTableEditor.get();
              if(processes.length > 0) {
                Integer customerPartition = ((DivisionItem)customerPartitionComboBox.getSelectedItem()).getId();
                for(Integer processId:processes)
                  ObjectLoader.executeUpdate("INSERT INTO [ContractProcess]([ContractProcess(process)],[ContractProcess(contract)],[ContractProcess(customerPartition)]) "
                          + "VALUES("+processId+","+contract+","+customerPartition+")");
                scaleVisibleRect = scale.getVisibleRect();
                initData();
              }
            }else Messanger.alert("Каталог продуктов пуст", "Внимание", JOptionPane.WARNING_MESSAGE);
          }else Messanger.alert("Выберите стороны договора", "Внимание", JOptionPane.WARNING_MESSAGE);
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }finally {
        EditorController.defaultCursor();
      }
    });
    
    try {
      dealDivisionTarget = new DivisionTarget(Deal.class) {
        @Override
        public void messageReceived(final String type, Integer[] ids, PropertyMap objectEventProperty) {
          if(type.equals("REMOVE")) {
            scale.removePeriods(ids);
          }else if(type.equals("UPDATE")) {
            try {
              //scale.removePeriods(ids);
              ids = ObjectLoader.isSatisfy(rootFilter, ids);
              if(ids.length > 0) {
                ObjectPeriod[] periods = new ObjectPeriod[0];
                List<List> data = ObjectLoader.getData(DBFilter.create(Deal.class).AND_IN("id", ids), new String[] {"id","service","dealStartDate","dealEndDate","tempProcess"});
                for(List d:data) {
                  ObjectPeriod period = scale.getPeriod((Integer)d.get(0));
                  if(period != null) {
                    period.setStartDate(((java.sql.Date)d.get(2)).getTime());
                    period.setEndDate(((java.sql.Date)d.get(3)).getTime());
                  }else {
                    Integer row = getRow((Integer)d.get(4));
                    if(row != null && row != -1) {
                      Date start = new Date(((java.sql.Date)d.get(2)).getTime());
                      Date end = new Date(((java.sql.Date)d.get(3)).getTime());
                      periods = (ObjectPeriod[]) ArrayUtils.add(periods, scale.createPeriod(row, (Integer)d.get(0), start, end, Color.LIGHT_GRAY, ""));
                    }
                  }
                }
                if(periods.length > 0)
                  scale.addPeriods(periods);
                setStatuses(ids);
                setComments(ids);
              }
            }catch(Exception ex) {
              Messanger.showErrorMessage(ex);
            }
          }else if(type.equals("CREATE")) {
            try {
              ids = ObjectLoader.isSatisfy(rootFilter, ids);
              if(ids.length > 0) {
                ObjectPeriod[] periods = new ObjectPeriod[0];
                List<List> data = ObjectLoader.getData(DBFilter.create(Deal.class).AND_IN("id", ids), 
                        new String[] {"id","service","dealStartDate","dealEndDate","tempProcess","service_name","customerCompanyPartition","customerpartitionname"});
                for(List d:data) {
                  Integer row = getRow((Integer)d.get(4));
                  
                  if(row == null || row == -1) {
                    splitTable.getTable(0).getTableModel().addRow(new Object[]{d.get(1),d.get(5),d.get(4),d.get(6),d.get(7)});
                    scale.getTableModel().addRow(new Object[scale.getColumnCount()]);
                    splitTable.getTable(2).getTableModel().addRow(new Object[1]);
                    row = getRow((Integer)d.get(4));
                  }
                  
                  if(row != null && row != -1) {
                    Date start = new Date(((java.sql.Date)d.get(2)).getTime());
                    Date end = new Date(((java.sql.Date)d.get(3)).getTime());
                    periods = (ObjectPeriod[]) ArrayUtils.add(periods, scale.createPeriod(row, (Integer)d.get(0), start, end, Color.LIGHT_GRAY, ""));
                  }
                }
                scale.addPeriods(periods);
                setStatuses(ids);
                setComments(ids);
              }
            }catch(Exception ex) {
              Messanger.showErrorMessage(ex);
            }
          }
        }
      };
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    
    removeToolButton.addActionListener((ActionEvent e) -> {
      try {
        Integer[] ids = getSelectedTempProcess();
        if(ids.length > 0) {
          if(JOptionPane.showConfirmDialog(
                  null,
                  "Вы действительно хотите удалить выделенны"+(ids.length>1?"е":"й")+" процесс"+(ids.length>1?"ы":"")+"?",
                  "Удаление на всегда",
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.QUESTION_MESSAGE) == 0) {

            List<List> data = ObjectLoader.getData(DBFilter.create(Deal.class).AND_EQUAL("contract", contract).AND_EQUAL("type", Deal.Type.CURRENT).AND_IN("tempProcess", ids), true, new String[]{"id"});
            Integer[] deals = new Integer[0];
            for(List d:data)
              deals = (Integer[]) ArrayUtils.add(deals, d.get(0));
            
            if(deals.length > 0) {
              data = ObjectLoader.getData(DBFilter.create(DealPosition.class).AND_IN("deal", deals).AND_NOT_EQUAL("dispatchDate", null), true, new String[]{"tempProcess_id","deal"});
              Integer[] dispatchProcesses = new Integer[0];
              for(List d:data)
                if(!ArrayUtils.contains(dispatchProcesses, d.get(0)))
                  dispatchProcesses = (Integer[]) ArrayUtils.add(dispatchProcesses, d.get(0));
              if(dispatchProcesses.length > 0) {
                if(dispatchProcesses.length == ids.length) {
                  Messanger.alert("Позиции отгружены, их удаление невозможно!", "Внимание!", JOptionPane.WARNING_MESSAGE);
                  ids = null;
                }else if(JOptionPane.showConfirmDialog(
                        null,
                        "Некоторые позиции уже отгружены и их удаление невозможно\n"
                                + "Удалить неотгруженные?",
                        "Внимание!", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == 0) {
                  for(List d:data)
                    ids   = (Integer[])ArrayUtils.removeElement(ids, d.get(0));
                }else ids = null;
              }
            }
            if(ids != null && ids.length > 0) {
              data = ObjectLoader.getData(DBFilter.create(Deal.class).AND_EQUAL("contract", contract).AND_EQUAL("type", Deal.Type.CURRENT).AND_IN("tempProcess", ids), true, new String[]{"id"});
              deals = new Integer[0];
              for(List d:data)
                deals = (Integer[]) ArrayUtils.add(deals, d.get(0));
              if(deals.length > 0)
                ObjectLoader.removeObjects(Deal.class, true, deals);
              if(ids.length > 0)
                ObjectLoader.removeObjects(ContractProcess.class, true, ids);
              scaleVisibleRect = scale.getVisibleRect();
              initData();
            }
          }
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    });
    
    //JOptionPane.showConfirmDialog(null, "Введите новый номер договора:", "Изменение номера договора", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)
    
    paste.addActionListener((ActionEvent e) -> {
      try {
        TreeMap<Integer, List<Date>> dates = scale.getSelectedDates();
        if (!dates.isEmpty()) {
          JSONObject json = (JSONObject) System.getProperties().get("buffer:"+Deal.class.getName());
          if (json != null) {
            Integer processId = json.getInt("service");
            Integer dealId = json.getInt("id");
            Integer[] dealPositions = new Integer[0];
            for(Object dp:json.getJSONArray("dealPositions"))
              dealPositions = (Integer[]) ArrayUtils.add(dealPositions, dp);
            Integer contract1 = json.getInt("contract");
            Integer sellerCompanyPartition = json.getInt("sellerCompanyPartition");
            Integer customerCompanyPartition = json.getInt("customerCompanyPartition");
            Integer sellerCfc = json.getInt("sellerCfc");
            Integer customerCfc = json.getInt("customerCfc");
            if (getContract().intValue() != json.getInt("contract")) {
              List<List> data = ObjectLoader.getData(Contract.class, true,
                      new Integer[]{getContract()},
                      new String[]{"sellerCompanyPartition","customerCompanyPartition", "sellerCfc", "customerCfc", "sellerCompany", "customerCompany"});
              if (data.isEmpty()) {
                Messanger.alert("Невозможно вставить сделку", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
              } else {
                if (!data.get(0).get(4).equals(json.getInt("sellerCompany")) || !data.get(0).get(5).equals(json.getInt("customerCompany"))) {
                  Messanger.alert("Невозможно вставить сделку:\nсделка имеет другого продавца и(или) покупателя", "Ошибка", JOptionPane.ERROR_MESSAGE);
                  return;
                } else {
                  String msg = "";
                  if(!data.get(0).get(0).equals(json.getInt("sellerCompanyPartition")))
                    msg += "  -сменить подразделение продавца\n";
                  if(!data.get(0).get(1).equals(json.getInt("customerCompanyPartition")))
                    msg += "  -сменить подразделение покупателя\n";
                  if(!data.get(0).get(2).equals(json.getInt("sellerCfc")))
                    msg += "  -сменить группу продавца\n";
                  if(!data.get(0).get(3).equals(json.getInt("customerCfc")))
                    msg += "  -сменить группу покупателя\n";
                  if(!msg.equals(""))
                    msg = "Для вставки сделки, её необходимо изменить:\n"+msg+"Продолжить?";
                  if (msg.equals("") || JOptionPane.showConfirmDialog(null, msg, "Внимание...", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)==0) {
                    contract1 = getContract();
                    sellerCompanyPartition = (Integer)data.get(0).get(0);
                    customerCompanyPartition = (Integer)data.get(0).get(1);
                    sellerCfc = (Integer)data.get(0).get(2);
                    customerCfc = (Integer)data.get(0).get(3);
                  } else {
                    return;
                  }
                }
              }
            }
            TreeMap<String,Object> fields = new TreeMap();
            fields.put("contract", contract1);
            fields.put("sellerCompany",            json.getInt("sellerCompany"));
            fields.put("sellerCompanyPartition",   sellerCompanyPartition);
            fields.put("sellerCfc",                sellerCfc);
            fields.put("customerCompany",          json.getInt("customerCompany"));
            fields.put("customerCompanyPartition", customerCompanyPartition);
            fields.put("customerCfc",              customerCfc);
            fields.put("service",                  json.getInt("service"));
            try {
              LocalDate dealStartDate = null;
              Integer tempProcess = null;
              for(Integer row:dates.keySet()) {
                if(getProcess(row).equals(processId)) {
                  tempProcess = getTempProcess(row);
                  dealStartDate = Utility.convert(dates.get(row).get(0));
                  break;
                }
              }
              if(tempProcess != null) {
                List<List> data = ObjectLoader.executeQuery("SELECT [DealPosition(duration)],[DealPosition(recurrence)] FROM [DealPosition] "
                        + "WHERE id=ANY(?)", true, new Object[]{dealPositions});

                fields.put("tempProcess",   tempProcess);
                fields.put("dealStartDate", Utility.convert(dealStartDate));

                Period duration   = Utility.convert((String)data.get(0).get(0));
                Period recurrence =  Utility.convert((String)data.get(0).get(1));

                //String[] sr = recurrence==null?null:recurrence.toString().split(" ");
                //String[] sd = duration.toString().split(" ");
                //int rCount = sr==null?0:Integer.valueOf(sr[0]);
                //int dCount   = Integer.valueOf(sd[0]);
                int recCount = recurrence == null || recurrence.isZero() ? 1 : DealPositionTableEditor.getCount(1);
                if(recCount == 0) {
                  Date contractEndDate = null;
                  data = ObjectLoader.getData(Contract.class, true, new Integer[]{(Integer)fields.get("contract")}, new String[]{"endDate"});
                  if(!data.isEmpty() && data.get(0).get(0) != null)
                    contractEndDate = new Date(((java.sql.Date)data.get(0).get(0)).getTime());
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
                      //start = start.plusDays(recurrence.getDays()).plusMonths(recurrence.getMonths()).plusYears(recurrence.getYears());//DealPositionTableEditor.getNextStart(start, sr[1], rCount);
                    }
                  }
                }
                //Calendar c = Calendar.getInstance();
                RemoteSession session = null;
                try {
                  session = ObjectLoader.createSession(false);
                  for(int r=0;r<recCount;r++) {
                    LocalDate dealEndDate = null;
        
                    if(duration.getDays() > 0)
                      dealEndDate = dealStartDate.plusDays(duration.getDays()-1);
                    if(duration.getMonths() > 0)
                      dealEndDate = dealStartDate.plusMonths(duration.getMonths()-1).withDayOfMonth(dealStartDate.plusMonths(duration.getMonths()-1).lengthOfMonth());
                    if(duration.getYears() > 0)
                      dealEndDate = dealStartDate.plusYears(duration.getYears()-1).withDayOfYear(dealStartDate.plusYears(duration.getYears()-1).getDayOfYear());
                    
                    //LocalDate dealEndDate = dealStartDate.plusDays(duration.getDays()).plusMonths(duration.getMonths()).plusYears(duration.getYears());
                    /*c.setTime(dealStartDate);
                    long days = 0;
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
                    if(ids.length == 0) {
                      fields.put("dealStartDate",            Utility.convertToSqlDate(dealStartDate));
                      fields.put("dealEndDate",              Utility.convertToSqlDate(dealEndDate,23,59,59,999));

                      newDealId = session.createObject(Deal.class, (Map)fields);

                      data = session.executeQuery("SELECT "
                              + "[DealPosition(product)],"
                              + "[DealPosition(equipment)],"
                              + "[DealPosition(customProductCost)], "
                              + "[DealPosition(amount)] "
                              + "FROM [DealPosition] WHERE id=ANY(?)", new Object[]{dealPositions});
                      for(List d:data) {
                        session.executeUpdate("INSERT INTO [DealPosition] ("
                                + "[DealPosition(deal)],"
                                + "[DealPosition(product)],"
                                + "[DealPosition(equipment)],"
                                + "[DealPosition(customProductCost)],"
                                + "[DealPosition(amount)]"
                                + ") VALUES ("+newDealId+","+d.get(0)+","+d.get(1)+","+d.get(2)+","+d.get(3)+")");
                      }
                    }

                    if(recCount > 1)
                      dealStartDate = dealStartDate.plusDays(recurrence.getDays()).plusMonths(recurrence.getMonths()).plusYears(recurrence.getYears());//getNextStart(dealStartDate, sr[1], rCount);
                  }
                  ObjectLoader.commitSession(session);
                }catch(Exception ex) {
                  ObjectLoader.rollBackSession(session);
                  Messanger.showErrorMessage(ex);
                }
              }
            } catch (Exception ex) {
              Messanger.showErrorMessage(ex);
            }
          }
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    });
    
    copy.addActionListener((ActionEvent e) -> {
      Integer[] dealIds = scale.getSelectedIds();// getSelectedDealIds();
      if(dealIds.length > 0) {
        try {
          JSONObject json = ObjectLoader.getJSON(Deal.class, dealIds[0]);
          System.getProperties().put("buffer:"+Deal.class.getName(), json);
        } catch (Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
    
    scale.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        try {
          if(e.getModifiers() == MouseEvent.META_MASK) {
            JPopupMenu processPopMenu = new JPopupMenu();
            processPopMenu.add(copy);
            processPopMenu.add(paste);
            paste.setEnabled(!scale.getSelectedDates().isEmpty() && System.getProperties().containsKey("buffer:"+Deal.class.getName()));
            copy.setEnabled(false);
            Integer[] dealIds = scale.getSelectedIds();
            if(dealIds.length > 0) {
              copy.setEnabled(true);
              processPopMenu.addSeparator();
              TreeMap<String,Object> map = new TreeMap();
              map.put("ids", dealIds);
              for(JMenuItem item:ActionMacrosUtil.createMacrosMenu(Deal.class, map))
                processPopMenu.add(item);
            }
            processPopMenu.show(scale, e.getX(), e.getY());
          }
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
    
    scale.setPeriodDragger(new PeriodDragger() {
      @Override
      public void moved(ObjectPeriod period, int oldRow, long oldStartDate, long oldEndDate) {
        if(JOptionPane.showConfirmDialog(null, "Передвинуть сделку?", "Перенос работ", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == 0) {
          try {
            if(ObjectLoader.executeUpdate("UPDATE [Deal] SET "
                    + "[Deal(service)]=?, "
                    + "[Deal(tempProcess)]=?, "
                    + "[Deal(dealStartDate)]=?, "
                    + "[Deal(dealEndDate)]=? "
                    + "WHERE [Deal(id)]=?", true,
                    new Object[]{
                      getProcess(period.getRowIndex()), 
                      getTempProcess(period.getRowIndex()), 
                      new Timestamp(period.getStartDate()), 
                      new Timestamp(period.getEndDate()), period.getId()}) == 1)
              ObjectLoader.sendMessage(Deal.class, "UPDATE", period.getId());
          } catch (Exception ex) {
            Messanger.showErrorMessage(ex);
          }
        }else {
          period.setRowIndex(oldRow);
          period.setStartDate(oldStartDate);
          period.setEndDate(oldEndDate);
          period.recalculate();
          scale.repaint();
        }
      }

      @Override
      public boolean isMove(ObjectPeriod period, int newRow, long newStartDate, long newEndDate) {
        if(getProcess(newRow).equals(getProcess(period.getRowIndex())) && isEnabled())
          return true;
        return false;
      }
    });
    
    scale.addObjectPeriodScaleListener(dealPositionTableEditor);
    scale.addObjectPeriodScaleListener(dealCommentsTableEditor);
    scale.addObjectPeriodScaleListener(actionsPanel);
    
    scale.addObjectPeriodScaleListener(new ObjectPeriodScaleListener() {
      @Override
      public void objectPeriodDoubleClicked(ObjectPeriod period) {
      }

      @Override
      public void objectPeriodsSelected(List<ObjectPeriod> periods) {
        periodsSelected();
      }

      @Override
      public void dayDoubleClicked(int rowIndex, Date day) {
      }

      @Override
      public void daysSelected(TreeMap<Integer, List<Date>> days) {
        periodsSelected();
      }

      @Override
      public void dayWidthChanged(int dayWidth) {
      }
    });

    startDate.addCalendarChangeListener(new CalendarChangeListener() {
      @Override
      public void CalendarChangeDate(DistanceList dates) {
        setEndDate();
        saveDates();
      }

      @Override
      public void CalendarSelectedDate(DistanceList dates) {
      }
    });

    endDate.addCalendarChangeListener(new CalendarChangeListener() {
      @Override
      public void CalendarChangeDate(DistanceList dates) {
        setDurationOnPlanGraphic();
        saveDates();
      }

      @Override
      public void CalendarSelectedDate(DistanceList dates) {
      }
    });

    durButton.addItemListener((ItemEvent e) -> {
      setDurationActive();
    });

    endButton.addItemListener((ItemEvent e) -> {
      if(e.getStateChange() == ItemEvent.SELECTED) {
        setEndDate();
        saveDates();
      }
    });

    durationType.addItemListener((ItemEvent e) -> {
      if(e.getStateChange() == ItemEvent.SELECTED) {
        setEndDate();
        saveDates();
      }
    });

    durationCount.addChangeListener((ChangeEvent e) -> {
      int index = durationType.getSelectedIndex();
      Object[] typeItems = new Object[]{"дней","месяцев","лет"};
      int value = (Integer)durationCount.getValue();
      if(value == 0) {
        typeItems = new Object[0];
        index = -1;
      }else if(value != 11 && value%10 == 1)
        typeItems = new Object[]{"день","месяц","год"};
      else if((value > 21 || value < 10) && value%10 < 5 && value%10 > 0)
        typeItems = new Object[]{"дня","месяца","года"};
      durationType.clear();
      durationType.addItems(typeItems);
      index = index>=0?index:0;
      if(typeItems.length > 0)
        durationType.setSelectedIndex(index);
      setEndDate();
    });
    
    splitTable.getTable(2).addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 1 && scale.getSelectedColumnCount() != scale.getColumnCount()) {
          splitTable.getTable(2).fireTableSelectionChange();
        }
      }
    });
    
    splitTable.getTable(0).addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 1 && scale.getSelectedColumnCount() != scale.getColumnCount()) {
          splitTable.getTable(2).fireTableSelectionChange();
        }
      }
    });
    
    tabb.addChangeListener(e -> initTabb());
  }
  
  private void initTabb() {
    dealPositionTableEditor.setActive(false);
    actionsPanel.setActive(false);
    dealCommentsTableEditor.setActive(false);
    switch(tabb.getSelectedIndex()) {
      case 0:
        dealPositionTableEditor.setActive(true);
        dealPositionTableEditor.initData();
        break;
      case 1:
        actionsPanel.setActive(true);
        actionsPanel.initData();
        break;
      case 2:
        dealCommentsTableEditor.setActive(true);
        dealCommentsTableEditor.initData();
        break;
    }
  }
  
  
  private boolean savableDates = true;
  private void saveDates() {
    try {
      if(savableDates)
        ObjectLoader.executeUpdate("UPDATE [Contract] SET [Contract(startDate)]=?, [Contract(endDate)]=? WHERE [Contract(id)]=?",
                new Object[]{new java.sql.Date(startDate.getDate().getTime()), new java.sql.Date(endDate.getDate().getTime()), getContract()});
    }catch(Exception e) {
      Messanger.showErrorMessage(e);
    }
  }
  
  private void setFilterStatus() {
    statusFilter.clear();
    
    String[] values = new String[0];
    
    if(redButton.isSelected())
      values = (String[]) ArrayUtils.add(values, "Принято");
    if(blueButton.isSelected())
      values = (String[]) ArrayUtils.add(values, "Расчёт");
    if(blackButton.isSelected())
      values = (String[]) ArrayUtils.add(values, "Неопределённость");
    if(yelowButton.isSelected())
      values = (String[]) ArrayUtils.add(values, "Старт");
    
    if(values.length > 0)
      statusFilter.AND_IN("status", values);
    initData();
  }
  
  private void periodsSelected() {
    final Integer[] dealIds = scale.getSelectedIds();
    
    //actionsPanel.setDealIds(dealIds);
    //actionsPanel.initData();
    
    for(int i=0;i<splitTable.getTable(2).getRowCount();i++)
      splitTable.getTable(2).setValueAt(null, i, 0);
    if(dealIds.length > 0) {
      try {
        final TreeMap<Integer, List<Integer>> rowDeals = new TreeMap<>();
        for(Integer id:dealIds) {
          int row = scale.rowAtId(id);
          if(row >= 0) {
            List<Integer> deals = rowDeals.get(row);
            if(deals == null) {
              deals = new ArrayList<>();
              rowDeals.put(row, deals);
            }
            if(!deals.contains(id))
              deals.add(id);
          }
        }
        
        List<List> data = ObjectLoader.executeQuery("SELECT [CFC(id)],[CFC(name)] FROM [CFC] WHERE tmp=false AND type='CURRENT' AND [CFC(id)] IN (SELECT [Company(cfcs):target] FROM [Company(cfcs):table] WHERE [Company(cfcs):object]=(SELECT [Contract(sellerCompany)] FROM [Contract] WHERE [Contract(id)]="+getContract()+"))");
        for(final Integer row:rowDeals.keySet()) {
          DivisionComboBox comboBox = new DivisionComboBox();
          for(List d:data) {
            DivisionItem item = new DivisionItem((Integer) d.get(0), (String) d.get(1), "CFC");
            if(!ArrayUtils.contains(comboBox.getItems(), item))
              comboBox.addItem(item);
          }
          
          comboBox.addItemListener((ItemEvent e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
              try {
                ObjectLoader.executeUpdate("UPDATE [Deal] SET [Deal(sellerCfc)]="+((DivisionItem)e.getItem()).getId()+" WHERE [Deal(id)]=ANY(?)", true, new Object[]{rowDeals.get(row).toArray(new Integer[0])});
              } catch (Exception ex) {
                Messanger.showErrorMessage(ex);
              } finally {
                TableCellEditor editor = splitTable.getTable(2).getCellEditor();
                if(editor != null)
                  editor.cancelCellEditing();
              }
            }
          });
          
          splitTable.getTable(2).setValueAt(comboBox, row, 0);
          
          List<List> selectedData = ObjectLoader.executeQuery("SELECT DISTINCT [Deal(sellerCfc)], [Deal(seller_cfc_name)] FROM [Deal] WHERE [Deal(id)]=ANY(?)", true, new Object[]{rowDeals.get(row).toArray(new Integer[0])});
          if(selectedData.size() == 1) {
            DivisionItem item = new DivisionItem((Integer) selectedData.get(0).get(0), (String) selectedData.get(0).get(1), "CFC");
            if(!ArrayUtils.contains(comboBox.getItems(), item))
              comboBox.addItem(item);
            comboBox.setSelectedItem(item);
          }
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
    
    selectedDealsCount.setText("Выделенных сделок: "+dealIds.length+" на сумму: 0");
    if(dealIds.length > 0) {
      try {
        List<List> data = ObjectLoader.executeQuery("SELECT NULLTOZERO(getDealsCost(?))", true, new Object[]{dealIds});
        selectedDealsCount.setText("Выделенных сделок: "+dealIds.length+" на сумму: "+Utility.doubleToString(((BigDecimal)data.get(0).get(0)).doubleValue(),2));
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }
  
  private Integer getProcess(Integer row) {
    return (Integer) splitTable.getTable(0).getValueAt(row, 0);
  }
  
  private Integer getTempProcess(Integer row) {
    return (Integer) splitTable.getTable(0).getValueAt(row, 2);
  }
  
  private Integer getCustomerPartition(Integer row) {
    return (Integer) splitTable.getTable(0).getValueAt(row, 3);
  }
  
  private Integer[] getSelectedProcess() {
    Integer[] ids = new Integer[0];
    int[] rows = splitTable.getTable(0).getSelectedRows();
    for(int row:rows)
      ids = (Integer[]) ArrayUtils.add(ids, splitTable.getTable(0).getValueAt(row, 0));
    return ids;
  }
  
  private Integer[] getSelectedTempProcess() {
    Integer[] ids = new Integer[0];
    int[] rows = splitTable.getTable(0).getSelectedRows();
    for(int row:rows)
      ids = (Integer[]) ArrayUtils.add(ids, splitTable.getTable(0).getValueAt(row, 2));
    return ids;
  }
  
  /*private int getRow(Integer processId) {
    for(int i=0;i<splitTable.getTable(0).getRowCount();i++)
      if(processId.equals(splitTable.getTable(0).getValueAt(i, 0)))
        return i;
    return -1;
  }*/
  
  private int getRow(Integer tempProcessId) {
    for(int i=0;i<splitTable.getTable(0).getRowCount();i++)
      if(tempProcessId != null && tempProcessId.equals(splitTable.getTable(0).getValueAt(i, 2)))
        return i;
    return -1;
  }

  public void clear(int index) {
    scale.clear(index);
  }

  public void clear() {
    splitTable.clear();
    //processRow.clear();
    //((RowHeaderModel)((RowHeaderList)splitTable.getScroll(0).getRowHeader().getView()).getModel()).clear();
    //scale.clear();
  }

  public void addColorLabel(String name, Date date, Color color) {
    scale.addColorLabel(date, color, name);
  }

  public void removeColorLabel(Date date) {
    scale.removeColorLabel(date);
  }

  public void removeColorLabel(String name) {
    scale.removeColorLabel(name);
  }

  public Deal[] getSelectedDeals() {
    Integer[] ids = scale.getSelectedIds();
    if(ids.length > 0) {
      try {
        return Arrays.asList(ObjectLoader.getObjects(Deal.class, ids)).toArray(new Deal[0]);
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
    return new Deal[0];
  }
  
  public void initData() {
    clear();
    
    if(contract != null) {
      try {
        Integer contractId = contract;
        
        PropertyMap contractProp = ObjectLoader.getMap(Contract.class, contractId, 
                "startDate",
                "endDate",
                "duration=query:(SELECT [XMLContractTemplate(duration)] FROM [XMLContractTemplate] WHERE [XMLContractTemplate(id)]=[Contract(template)])",
                "tmp",
                "type");
        
        /*final List<List> contractData = ObjectLoader.executeQuery("SELECT "
                + "[Contract(startDate)],"
                + "[Contract(endDate)],"
                + "(SELECT [XMLContractTemplate(duration)] FROM [XMLContractTemplate] WHERE [XMLContractTemplate(id)]=[Contract(template)]),"
                + "tmp, "
                + "type "
                + "FROM [Contract] WHERE [Contract(id)]="+contractId, true);*/
        
        List<List> companyPartitionData = ObjectLoader.executeQuery("SELECT id FROM [CompanyPartition] WHERE [CompanyPartition(company)]="
                + "(SELECT [Contract(customerCompany)] FROM [Contract] WHERE [Contract(id)]="+contractId+") AND "
                + "(SELECT COUNT(id) FROM [Deal] WHERE [Deal(contract)]="+contractId+" AND [Deal(customerCompanyPartition)]=[CompanyPartition(id)] AND type='CURRENT' AND tmp=false)=0", true);
        Integer[] zeroCompanyPartitions = new Integer[0];
        for(List d:companyPartitionData)
          zeroCompanyPartitions = (Integer[]) ArrayUtils.add(zeroCompanyPartitions, d.get(0));
        
        for(List d:ObjectLoader.executeQuery("SELECT [ContractProcess(process)],[ContractProcess(process_name)],[ContractProcess(id)],[ContractProcess(customerPartition)],"
                + "(SELECT name FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[ContractProcess(customerPartition)]) FROM [ContractProcess] WHERE "
                + "[ContractProcess(contract)]="+contractId+" AND [ContractProcess(customerPartition)] <> ALL(?) ORDER BY "
                + "[ContractProcess(customerPartition)],[ContractProcess(date)], [ContractProcess(process_name)]", true, new Object[]{zeroCompanyPartitions})) {
          splitTable.getTable(0).getTableModel().addRow(d.toArray());
          scale.getTableModel().addRow(new Object[scale.getColumnCount()]);
          splitTable.getTable(2).getTableModel().addRow(new Object[1]);
        }
        
        for(List d:ObjectLoader.executeQuery("SELECT "
                + "[ContractProcess(process)],"
                + "[ContractProcess(process_name)],"
                + "[ContractProcess(id)],"
                + "[ContractProcess(customerPartition)],"
                + "(SELECT name FROM [CompanyPartition] "
                + "WHERE [CompanyPartition(id)]=[ContractProcess(customerPartition)]) FROM [ContractProcess] WHERE "
                + "[ContractProcess(contract)]="+contractId+" AND [ContractProcess(customerPartition)]=ANY(?) ORDER BY "
                + "[ContractProcess(customerPartition)],[ContractProcess(date)], [ContractProcess(process_name)]", true, new Object[]{zeroCompanyPartitions})) {
          splitTable.getTable(0).getTableModel().addRow(d.toArray());
          scale.getTableModel().addRow(new Object[scale.getColumnCount()]);
          splitTable.getTable(2).getTableModel().addRow(new Object[1]);
        }
        
        planGraphicFilter.clear();
        planGraphicFilter.AND_EQUAL("contract", contractId).AND_EQUAL("type", contractProp.getValue("type"));
        planGraphicDateFilter.AND_DATE_BETWEEN("dealStartDate", scale.getStartDate(), scale.getEndDate())
                .OR_DATE_BETWEEN("dealEndDate", scale.getStartDate(), scale.getEndDate());
        
        final List<List> data = ObjectLoader.getData(rootFilter, true, "id","service","dealStartDate","dealEndDate","tempProcess","customerCompanyPartition");
        
        SwingUtilities.invokeLater(() -> {
          Integer[] dealIds = new Integer[0];
          ObjectPeriod[] periods = new ObjectPeriod[0];
          for(List d:data) {
            Integer row = getRow((Integer)d.get(4));
            if(row != -1) {
              dealIds = (Integer[]) ArrayUtils.add(dealIds, (Integer)d.get(0));
              java.sql.Date start = (java.sql.Date)d.get(2);
              java.sql.Date end   = (java.sql.Date)d.get(3);
              periods = (ObjectPeriod[]) ArrayUtils.add(periods, scale.createPeriod(
                      row,
                      (Integer)d.get(0),
                      start,
                      end,
                      Color.LIGHT_GRAY,
                      ""));
            }
          }
          
          scale.addPeriods(periods);
          
          setStatuses(dealIds);
          setComments(dealIds);
          
          savableDates = false;
          if(contractProp.is("tmp") && !contractProp.isNull("duration")) {
            String duration = Utility.format(contractProp.getValue("duration", Period.class));
            durationCount.setValue(Integer.valueOf(duration.split(" ")[0]));
            durationType.setSelectedItem(duration.split(" ")[1]);
          }else {
            startDate.setDateInCalendar(contractProp.getSqlDate("startDate"));
            endDate.setDateInCalendar(contractProp.getSqlDate("endDate"));
          }
          
          endButton.setSelected(true);
          durButton.setSelected(false);
          setDurationActive();
          savableDates = true;
          
          if(scaleVisibleRect == null)
            scale.scrollToCurrentDate();
          else {
            scale.scrollRectToVisible(scaleVisibleRect);
            scaleVisibleRect = null;
          }
        });
      }catch(Exception e) {
        Messanger.showErrorMessage(e);
      }
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    startDate.setEnabled(enabled);
    endDate.setEnabled(enabled);
    durationCount.setEnabled(enabled);
    durationType.setEnabled(enabled);
    endButton.setEnabled(enabled);
    durButton.setEnabled(enabled);
    actionsPanel.setEnabled(enabled);
    splitTable.getTable(2).setEditable(enabled);
    
    EditorGui.setComponentEnable(dealPositionTableEditor.getToolBar(), enabled);
    EditorGui.setComponentEnable(tool, enabled);
    super.setEnabled(enabled);
  }
  
  private void setComments(final Integer[] dealIds) {
    pool.submit(() -> {
      setCursor(new Cursor(Cursor.WAIT_CURSOR));
      try {
        Hronometr.start("Получение комментариев сделок");
        List<List> commentData = ObjectLoader.executeQuery("SELECT id,[Deal(service_name)]||' ('||[Deal(cost)]||'р.)' FROM [Deal] WHERE id=ANY(?)", true, new Object[]{dealIds});
        List<List> userCommentData = ObjectLoader.executeQuery("SELECT "
                + "[DealComment(deal)],"
                + "[DealComment(date)],"
                + "[DealComment(comment)],"
                + "(SELECT [DealCommentSubject(color)] FROM [DealCommentSubject] WHERE [DealCommentSubject(id)]=[DealComment(subject)]), "
                + "[DealComment(stopTime)] "
                + "FROM [DealComment] "
                + "WHERE tmp=false AND type='CURRENT' AND [DealComment(deal)]=ANY(?)", true, new Object[]{dealIds});
        Hronometr.stop("Получение комментариев сделок");
        
        Hronometr.start("Расставляю комментарии");
        TreeMap<Integer, String> comments = new TreeMap<>();
        TreeMap<Integer, TreeMap<Long,UserComment>> dealDateUserComments     = new TreeMap<>();
        TreeMap<Integer, TreeMap<Long,UserComment>> dealStopDateUserComments = new TreeMap<>();
        for(List d:commentData)
          comments.put((Integer) d.get(0), (String) d.get(1));
        for(List d:userCommentData) {
          TreeMap<Long,UserComment> dateUserComments     = dealDateUserComments.get((Integer) d.get(0));
          TreeMap<Long,UserComment> stopDateUserComments = dealStopDateUserComments.get((Integer) d.get(0));
          
          if(dateUserComments == null)
            dealDateUserComments.put((Integer) d.get(0), dateUserComments = new TreeMap<>());
          
          if(stopDateUserComments == null)
            dealStopDateUserComments.put((Integer) d.get(0), stopDateUserComments = new TreeMap<>());
          
          UserComment userComment = new UserComment(
                  (Timestamp) d.get(1),
                  new Timestamp(((java.sql.Date)d.get(4)).getTime()),
                  (String) d.get(2));
          
          dateUserComments.put(((Timestamp) d.get(1)).getTime(), userComment);
          stopDateUserComments.put(((java.sql.Date)d.get(4)).getTime(), userComment);
        }
        
        for(CopyOnWriteArrayList<ObjectPeriod> list:scale.getPeriods()) {
          for(ObjectPeriod period:list) {
            if(comments.containsKey(period.getId()))
              period.setComment(comments.get(period.getId()));
            if(dealDateUserComments.containsKey(period.getId())) {
              period.setDateUserComments(dealDateUserComments.get(period.getId()));
              period.setStopDateUserComments(dealStopDateUserComments.get(period.getId()));
            }
          }
        }
        Hronometr.stop("Расставляю комментарии");
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }finally {
        scale.repaint();
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
    });
  }
  
  private void setStatuses(final Integer[] dealIds) {
    pool.submit(() -> {
      setCursor(new Cursor(Cursor.WAIT_CURSOR));
      try {
        Hronometr.start("Получение статусов");
        final List<List> statusData = ObjectLoader.executeQuery("SELECT "
              /*0*/+ "[Deal(id)],"
              //+ "[Deal(dealPositionCount)],"
              /*1*/+ "[Deal(startCount)],"
              /*2*/+ "[Deal(dispatchCount)],"
              /*3*/+ "[Deal(dispatchAmount)],"
              /*4*/+ "[Deal(paymentCount)],"
              /*5*/+ "[Deal(paymentAmount)],"
              /*6*/+ "[Deal(cost)] "
              + "FROM deal WHERE [Deal(id)]=ANY(?)", new Object[]{dealIds});
        Hronometr.stop("Получение статусов");
        
        SwingUtilities.invokeLater(() -> {
          Hronometr.start("Раскраска");
          TreeMap<Integer,Color> map = new TreeMap<>();
          statusData.stream().forEach(d ->
                  map.put((Integer) d.get(0),
                          Deal.getColor((Integer) d.get(1), (Integer) d.get(2), (BigDecimal) d.get(3), (Integer) d.get(4), (BigDecimal) d.get(5), (BigDecimal) d.get(6)))
          );
          scale.setColor(map);
          Hronometr.stop("Раскраска");
        });
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }finally{
        scale.repaint();
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
    });
  }
  
  private void createDeal() {
    EditorController.waitCursor(this);
    try {
      List<List> data = ObjectLoader.executeQuery("SELECT "
              + "[Contract(sellerCompany)],"
              + "[Contract(customerCompany)],"
              + "[Contract(sellerCompanyPartition)],"
              + "[Contract(sellerCfc)],"
              + "[Contract(customerCfc)] "
              + "FROM [Contract] "
              + "WHERE [Contract(id)]="+getContract());
      if(!data.isEmpty()) {
        Integer seller          = (Integer) data.get(0).get(0);
        Integer customer        = (Integer) data.get(0).get(1);
        Integer sellerPartition = (Integer) data.get(0).get(2);
        Integer sellerCfc       = (Integer) data.get(0).get(3);
        Integer customerCfc     = (Integer) data.get(0).get(4);
      
        if(splitTable.getTable(0).getSelectedRowCount() == 1 &&
                contract != null && 
                seller != null && 
                customer != null && 
                sellerCfc != null && 
                customerCfc != null && 
                sellerPartition != null) {
          //Получить процесс
          Integer processId = getSelectedProcess()[0];
          Integer tempProcessId = getSelectedTempProcess()[0];

          TreeMap<Integer,List<Date>> rowDates = scale.getSelectedDates();

          if(tempProcessId != null && processId != null) {
            Date dealStartDate = scale.getSelectedColumnCount() != 1?scale.getDate(scale.getCurrentColumn()):rowDates.get((Integer)rowDates.keySet().toArray()[0]).get(0);
            if(dealStartDate != null) {
              dealPositionTableEditor.createDeals(tempProcessId, processId, contract, seller, customer, 
                      sellerPartition, getCustomerPartition(), sellerCfc, customerCfc, Utility.convert(dealStartDate));
            }
          }
        }else Messanger.alert("Выберите "+(seller==null?"стороны договора":(seller==null?"продавца":"покупателя")), "Внимание", JOptionPane.WARNING_MESSAGE);
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }finally {
      EditorController.defaultCursor(this);
    }
  }
  
  public void setDurationContractVisible(boolean visible) {
    durationContract.setVisible(visible);
  }
  
  public void dispose() {
    dealDivisionTarget.dispose();
    actionsPanel.dispose();
    dealCommentsTableEditor.dispose();
  }
}