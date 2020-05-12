package division.editors.tables;

import bum.editors.EditorController;
import bum.editors.EditorGui;
import bum.editors.actions.ActionMacrosUtil;
import bum.editors.util.DivisionTarget;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Company;
import bum.interfaces.Contract;
import bum.interfaces.Deal;
import division.ClientMain;
import division.ClientMainListener;
import division.editors.contract.ContractEditor;
import division.editors.objects.company.nCompanyEditor;
import division.fx.PropertyMap;
import division.scale.ObjectPeriod;
import division.scale.ObjectPeriodScaleListener;
import division.scale.PeriodScale;
import division.scale.UserComment;
import division.swing.DivisionSplitPane;
import division.swing.DivisionTable;
import division.swing.guimessanger.Messanger;
import division.swing.table.SplitTable;
import division.swing.table.filter.FilterList;
import division.swing.table.filter.FilterTextField;
import division.swing.table.filter.TableFilter;
import division.util.Hronometr;
import division.util.Utility;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.beans.value.ObservableValue;
import javax.swing.*;
import mapping.MappingObject;
import org.apache.commons.lang.ArrayUtils;
import util.filter.local.DBFilter;

public class CleverDealTableEditor extends EditorGui implements ClientMainListener {
  private DivisionSplitPane split = new DivisionSplitPane(JSplitPane.VERTICAL_SPLIT);
  private JTabbedPane   tabb  = new JTabbedPane();
  private SplitTable    splitTable = new SplitTable(3);
  
  private DivisionTable leftTable  = new DivisionTable();
  private DivisionTable rightTable = new DivisionTable();
  
  private PeriodScale   scale = new PeriodScale(15, 6, 5) {
    @Override
    protected String getToolTipText(int row, int column) {
      String toolTip = super.getToolTipText(row, column)
              +"<br/><b>"+splitTable.getTable(0).getValueAt(row, 5)
              +"<br/><b>"+splitTable.getTable(2).getValueAt(row, 1)+"</b>";
      return toolTip;
    }
  };
  
  private Integer[] cfcs   = new Integer[0];
  private Integer[] agents = new Integer[0];
  private DBFilter filter        = DBFilter.create(Deal.class);
  private DBFilter agentFilter   = filter.AND_FILTER();
  private DBFilter processFilter = filter.AND_FILTER();
  private DBFilter datesFilter   = filter.AND_FILTER();
  private DBFilter companyFilter = filter.AND_FILTER();
  private DBFilter statusFilter  = filter.AND_FILTER();
  
  private ExecutorService pool = Executors.newFixedThreadPool(6);
  private CleverDealTask cleverDealTask;
  
  private DealActionsPanel actionsPanel = new DealActionsPanel(scale);
  
  private DealCommentTableEditor dealCommentsTableEditor = new DealCommentTableEditor();
  
  private DealPositionTableEditor dealPositionTableEditor = new DealPositionTableEditor() {
    @Override
    public void addButton() {
      if(!scale.getSelectedPeriods().isEmpty())
        addDealPositions();
      else createDeal();
    }
  };
  
  private PercentCountPanel percentCountPanel = new PercentCountPanel();
  
  class PercentCountPanel extends JPanel {
    private double greenCost = 0;
    private double redCost   = 0;
    private double blueCost  = 0;
    private double yelowCost = 0;
    private double blackCost = 0;
    private double grayCost  = 0;
    
    public void setCostData(double greenCost, double redCost, double blueCost, double yelowCost, double blackCost, double grayCost) {
      this.greenCost = greenCost;
      this.redCost = redCost;
      this.blueCost = blueCost;
      this.yelowCost = yelowCost;
      this.blackCost = blackCost;
      this.grayCost = grayCost;
      PercentCountPanel.this.repaint();
    }

    @Override
    public void paint(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      
      double onePercentWidth = (double) getWidth()/100;
      double allCost = greenCost+redCost+blueCost+yelowCost+blackCost+grayCost;
      double onePercent = (double)(allCost/100);
      double greenPercent = (double)greenCost/onePercent;
      double redPercent   = (double)redCost/onePercent;
      double bluePercent  = (double)blueCost/onePercent;
      double yelowPercent = (double)yelowCost/onePercent;
      double blackPercent = (double)blackCost/onePercent;
      double grayPercent  = (double)grayCost/onePercent;
     
      int x = 0;
      g2.setColor(Color.GREEN);
      g2.fillRect(x, 0, (int)(greenPercent*onePercentWidth), getHeight());
      x += (int)(greenPercent*onePercentWidth);
      
      g2.setColor(Color.RED);
      g2.fillRect(x, 0, (int)(redPercent*onePercentWidth), getHeight());
      x += (int)(redPercent*onePercentWidth);
      
      g2.setColor(Color.BLUE);
      g2.fillRect(x, 0, (int)(bluePercent*onePercentWidth), getHeight());
      x += (int)(bluePercent*onePercentWidth);
      
      g2.setColor(Color.YELLOW);
      g2.fillRect(x, 0, (int)(yelowPercent*onePercentWidth), getHeight());
      x += (int)(yelowPercent*onePercentWidth);
      
      g2.setColor(Color.BLACK);
      g2.fillRect(x, 0, (int)(blackPercent*onePercentWidth), getHeight());
      x += (int)(blackPercent*onePercentWidth);
      
      g2.setColor(Color.LIGHT_GRAY);
      g2.fillRect(x, 0, (int)(grayPercent*onePercentWidth), getHeight());
      x += (int)(grayPercent*onePercentWidth);
      
      g2.setColor(Color.BLACK);
      g2.drawRect(0, 0, getWidth()-1, getHeight()-1);
    }
  }
  
  private JLabel selectedDealsCount = new JLabel("Выделенно: 0 (0р.)");
  private JLabel selectedGREENDeals = new JLabel("0 (0р.)");
  private JLabel selectedREDDeals   = new JLabel("0 (0р.)");
  private JLabel selectedBLUEDeals  = new JLabel("0 (0р.)");
  private JLabel selectedYELOWDeals = new JLabel("0 (0р.)");
  
  private JPanel colorPanel = new JPanel(new GridBagLayout());
  private ColorButton greenButton = new ColorButton("0%", Color.GREEN, "Сделка окончена (олачена и отгружена)");
  private ColorButton redButton   = new ColorButton("0%", Color.RED, "Сделка не оплачена, но отгружена");
  private ColorButton blueButton  = new ColorButton("0%", Color.BLUE, "Сделка оплачена, но не отгружена");
  private ColorButton blackButton = new ColorButton("0%", Color.BLACK, "Какой то косяк");
  private ColorButton yelowButton = new ColorButton("0%", Color.YELLOW.darker(), "Сделка только стартовала");
  private ColorButton grayButton  = new ColorButton("0%", Color.LIGHT_GRAY, "Проект сделки");
  
  class ColorButton extends JToggleButton {
    public ColorButton(String text, Color color, String toolTipText) {
      super(text);
      setToolTipText(toolTipText);
      setBackground(color);
    }

    public ColorButton(Color color) {
      this(null,color, null);
    }
    
    @Override
    public void paint(Graphics g) {
      
      ((Graphics2D)g).clearRect(0, 0, getWidth(), getHeight());
      ((Graphics2D)g).setStroke(new BasicStroke(1));
      
      ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
      ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      
      g.setColor(isSelected()?Color.BLACK:Color.LIGHT_GRAY);
      g.drawRect(0, 0, getWidth()-1, getHeight()-1);
      
      g.setColor(new Color(getBackground().getRed(),getBackground().getGreen(),getBackground().getBlue(),isSelected()?255:50));
      g.fillRect(2, 2, getWidth()-4, getHeight()-4);
      
      g.setFont(new Font("Dialog", Font.PLAIN, 8));
      int sw = g.getFontMetrics().stringWidth(getText());
      int x = (getWidth()-sw)/2;
      
      g.setColor(isSelected()?Color.WHITE:getBackground().darker());
      g.drawString(getText(), x, getHeight()-3);
    }
  }

  public CleverDealTableEditor() {
    super("Сделки", null, "Сделки");
    setAutoLoad(true);
    setAutoStore(true);
    initComponents();
    initTargets();
    initEvents();
    filter.AND_EQUAL("type", MappingObject.Type.CURRENT).AND_EQUAL("tmp", false);
    
    if(ObjectLoader.getClient().isPartner())
      this.cfcs = new Integer[]{ObjectLoader.getClient().getCfcId()};
    else cfcs = ClientMain.getCurrentCfc();
    agents = ClientMain.getCurrentCompanys();
  }
  
  private void createDeal() {
    try {
      int row = splitTable.getTable(0).getSelectedRow();
      if(row != -1) {
        Integer contractId = (Integer) splitTable.getTable(0).getTableModel().getValueAt(row, 0);
        Integer processId  = (Integer) splitTable.getTable(0).getTableModel().getValueAt(row, 1);
        Integer tempProcessId  = (Integer) splitTable.getTable(0).getTableModel().getValueAt(row, 6);
        Integer sellerPartitionId    = (Integer) splitTable.getTable(0).getTableModel().getValueAt(row, 3);
        Integer customerPartitionId  = (Integer) splitTable.getTable(0).getTableModel().getValueAt(row, 2);
        
        List<List> data = ObjectLoader.getData(Contract.class, new Integer[]{contractId}, 
                new String[]{"sellerCompany","customerCompany","sellerCfc","customerCfc"});
        if(!data.isEmpty()) {
          TreeMap<Integer,java.util.List<Date>> rowDates = scale.getSelectedDates();
          if(!rowDates.isEmpty()) {
            if(processId != null) {
              Date dealStartDate = rowDates.get((Integer)rowDates.keySet().toArray()[0]).get(0);
              dealPositionTableEditor.createDeals(tempProcessId,processId, contractId, (Integer) data.get(0).get(0), (Integer) data.get(0).get(1), 
                      sellerPartitionId, customerPartitionId, (Integer) data.get(0).get(2), (Integer) data.get(0).get(3), Utility.convert(dealStartDate));
            }
          }
        }
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  @Override
  public void setActive(boolean active) {
    super.setActive(active);
    if(!isActive() && cleverDealTask != null) {
      cleverDealTask.setShutDown(true);
      cleverDealTask = null;
    }
  }

  @Override
  public Boolean okButtonAction() {
    return true;
  }

  private void initComponents() {
    greenButton.setMinimumSize(new Dimension(30, 12));
    greenButton.setMaximumSize(new Dimension(30, 12));
    greenButton.setPreferredSize(new Dimension(30, 12));
    
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
    
    grayButton.setMinimumSize(new Dimension(30, 12));
    grayButton.setMaximumSize(new Dimension(30, 12));
    grayButton.setPreferredSize(new Dimension(30, 12));
    
    colorPanel.add(greenButton, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    colorPanel.add(redButton,   new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    colorPanel.add(blueButton,  new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    colorPanel.add(blackButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    colorPanel.add(yelowButton, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    colorPanel.add(grayButton,  new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    
    selectedGREENDeals.setFont(new Font("Arial", Font.BOLD, 10));
    selectedREDDeals.setFont(new Font("Arial", Font.BOLD, 10));
    selectedBLUEDeals.setFont(new Font("Arial", Font.BOLD, 10));
    selectedYELOWDeals.setFont(new Font("Arial", Font.BOLD, 10));
    
    selectedGREENDeals.setForeground(Color.GREEN.darker());
    selectedREDDeals.setForeground(Color.RED.darker());
    selectedBLUEDeals.setForeground(Color.BLUE.darker());
    selectedYELOWDeals.setForeground(Color.YELLOW.darker());
    
    splitTable.getStatusBar2().add(percentCountPanel);
    
    splitTable.getStatusBar().add(selectedDealsCount);
    splitTable.getStatusBar().add(selectedGREENDeals);
    splitTable.getStatusBar().add(selectedREDDeals);
    splitTable.getStatusBar().add(selectedBLUEDeals);
    splitTable.getStatusBar().add(selectedYELOWDeals);
    splitTable.getStatusBar().add(colorPanel);
    
    //leftTable.setFocusable(false);
    //rightTable.setFocusable(false);
    getRootPanel().setLayout(new GridBagLayout());
    splitTable.setTable(0, leftTable);
    splitTable.setTable(2, rightTable);
    splitTable.setTable(1, scale);
    
    splitTable.getScroll(1).getHorizontalScrollBar().setUnitIncrement(30);
    
    splitTable.setColumns(new String[]{"contractId","serviceId","customerPartitionId","sellerPartitionId","№ договора","Предмет","tempProcess"}, 0);
    splitTable.setColumns(new String[]{"id","Контрагент"}, 2);
    splitTable.getTable(0).setColumnWidthZero(0,1,2,3,6);
    splitTable.getTable(2).setColumnWidthZero(0);
    splitTable.setSortable(false);
    splitTable.getTable(0).setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    splitTable.getTable(2).setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    
    splitTable.getScroll(0).setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    splitTable.getScroll(1).setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    splitTable.getScroll(2).setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    
    splitTable.getTable(0).getTableFilters().addFilter(5, new ProcessFilterList(splitTable.getTable(0).getTableFilters(), 5));
    splitTable.getTable(2).getTableFilters().addFilter(1, new CompanyFilterText(splitTable.getTable(0).getTableFilters(), 1));
    
    /*DivisionSlider slider = new DivisionSlider();
    slider.getModel().setMin(-48);
    slider.getModel().setMax(48);
    slider.getModel().addController("start", 0 - scale.getPreviosMonthPeriod());
    slider.getModel().addController("end", scale.getNextMonthPeriod());
    
    getRootPanel().add(slider, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    */getRootPanel().add(split, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 5, 0, 5), 0, 0));
    
    dealPositionTableEditor.setVisibleOkButton(false);
    
    tabb.add(actionsPanel, "События");
    tabb.add(dealPositionTableEditor.getGUI(), "Позиции");
    tabb.add(dealCommentsTableEditor, "Комментарии");
    initTabb();
    
    split.add(splitTable,       JSplitPane.TOP);
    split.add(tabb,       JSplitPane.BOTTOM);
    
    for(int i=0;i<splitTable.getTables().length;i++)
      if(i != 1)
        addComponentToStore(splitTable.getTable(i),"table"+i);
    for(int i=0;i<splitTable.getSplits().length;i++)
      addComponentToStore(splitTable.getSplits()[i],"split"+i);
    addComponentToStore(split, "mainSplit");
    getComponentsToStore().addAll(dealPositionTableEditor.getComponentsToStore());
    getComponentsToStore().addAll(actionsPanel.getComponentsToStore());
    getComponentsToStore().addAll(dealCommentsTableEditor.getComponentsToStore());
    
    addComponentToStore(scale);
  }
  
  public Deal[] getSelectedDeals() {
    Integer[] ids = getSelectedDealIds();
    if(ids.length > 0)
      return Arrays.asList(ObjectLoader.getObjects(Deal.class, ids)).toArray(new Deal[0]);
    return new Deal[0];
  }
  
  public Integer[] getSelectedDealIds() {
    return scale.getSelectedIds();
  }
  
  private void initTabb() {
    /*actionsPanel.setActive(false);
    dealPositionTableEditor.setActive(false);
    dealCommentsTableEditor.setActive(false);
    switch(tabb.getSelectedIndex()) {
      case 0:
        actionsPanel.setActive(true);
        actionsPanel.initData();
        break;
      case 1:
        dealPositionTableEditor.setActive(true);
        dealPositionTableEditor.initData();
        break;
      case 2:
        dealCommentsTableEditor.setActive(true);
        dealCommentsTableEditor.initData();
        break;
    }*/
  }
  
  @Override
  public void changedCFC(Integer[] ids) {
    
    if(ObjectLoader.getClient().isPartner()) {
      this.cfcs = new Integer[]{ObjectLoader.getClient().getCfcId()};
      changedCompany(new Integer[0]);
    }else {    
      if(cfcs != null)
        Arrays.sort(cfcs);
      if(ids != null)
        Arrays.sort(ids);

      if(cfcs == null && ids != null || cfcs != null && ids == null || !Arrays.equals(cfcs, ids) || ids.length == 0 && cfcs.length == 0) {
        this.cfcs = ids;
        changedCompany(new Integer[0]);
      }
    }
  }

  @Override
  public void changedCompany(Integer[] ids) {
    if(agents != null)
      Arrays.sort(agents);
    if(ids != null)
      Arrays.sort(ids);
    
    if(agents == null && ids != null || agents != null && ids == null || !Arrays.equals(agents, ids) || ids.length == 0 && agents.length == 0) {
      this.agents = ids;
      initData();
    }
  }

  @Override
  public void initTargets() {
    addTarget(new DivisionTarget(Deal.class) {
      @Override
      public void messageReceived(final String type, final Integer[] ids_, PropertyMap objectEventProperty) {
        SwingUtilities.invokeLater(() -> {
          if(isActive()) {
            if (type.equals("REMOVE")) {
              scale.removePeriods(ids_);
            } else if(type.equals("UPDATE") || type.equals("CREATE")) {
              try {
                Integer[] ids = ObjectLoader.isSatisfy(filter, ids_);
                
                if(type.equals("UPDATE")) {
                  Integer[] removeIds = ids.length==0?ids_:new Integer[0];
                  if(removeIds.length == 0)
                    for(int i=0;i<ids_.length;i++)
                      if(!ArrayUtils.contains(ids, ids_[i]))
                        ArrayUtils.add(removeIds, ids_[i]);
                  if(removeIds.length > 0)
                    scale.removePeriods(ids_);
                }
                
                List<List> data = ObjectLoader.getData(Deal.class, ids, new String[]{
                  "id",                   //0
                  "dealStartDate",        //1
                  "dealEndDate",          //2
                  "contract_id",          //3
                  "service_id",           //4
                  "service_name",         //5
                  "customer",             //6
                  "customerpartition_id", //7
                  "seller",               //8
                  "sellerpartition_id",   //9
                  "seller_id",            //10
                  "customer_id",          //11
                  "contract_number",      //12
                  "cost",                 //13
                  "tempProcess"           //14
                }, new String[]{"contract_id","service_id"});
                
                data.stream().forEach(d -> {
                  Integer dealId = (Integer) d.get(0);
                  ObjectPeriod period = scale.getPeriod(dealId);
                  if(period == null) {
                    int row = rowAtTempProcess((Integer) d.get(14), (Integer) d.get(7));
                    if(row == -1) {
                      Integer contractId  = (Integer) d.get(3);
                      Integer serviceId   = (Integer) d.get(4);
                      Integer customerPartitionId = (Integer) d.get(7);
                      Integer sellerPartitionId   = (Integer) d.get(11);
                      Integer sellerId   = (Integer) d.get(10);
                      splitTable.addRow(new Object[]{contractId,serviceId,customerPartitionId,sellerPartitionId,d.get(12),d.get(5),d.get(14)});
                      splitTable.getTable(2).getTableModel().setValueAt(ArrayUtils.contains(agents, sellerId)?d.get(6):d.get(8),splitTable.getTable(2).getRowCount()-1,0);
                      row = splitTable.getTable(1).getRowCount()-1;
                    }
                    scale.addPeriod(row, dealId, (java.sql.Date)d.get(1), (java.sql.Date)d.get(2), Color.LIGHT_GRAY, "");
                  }else {
                    period.setStartDate(((java.sql.Date)d.get(1)).getTime());
                    period.setEndDate(((java.sql.Date)d.get(2)).getTime());
                    period.setColor(Color.LIGHT_GRAY);
                    scale.repaint();
                  }
                });
                setStatuses(ids);
                setComments(ids);
              }catch(Exception ex) {
                Messanger.showErrorMessage(ex);
              }
            }
          }
        });
      }
    });
  }
  
  private JLabel dealPosFilterLabel = new JLabel("Включён фильтр по реквизитам");
  
  public void initEvents() {
    ClientMain.addClientMainListener(this);
    
    /*dealPositionTableEditor.initDataListeners.add((InitDataListener) () -> {
      actionsPanel.dealPositionFiter.setValue(dealPositionTableEditor.getVisibleIds());
      actionsPanel.initData();
    });*/
    
    dealPosFilterLabel.setForeground(Color.blue);
    actionsPanel.getTools().add(new JToolBar.Separator());
    actionsPanel.getTools().add(dealPosFilterLabel);
    dealPosFilterLabel.setVisible(false);
    
    dealPositionTableEditor.getTable().getTableFilters().filterProperty.addListener((ObservableValue<? extends ArrayList<RowFilter<Object, Object>>> ob, ArrayList<RowFilter<Object, Object>> ol, ArrayList<RowFilter<Object, Object>> nw) -> {
      actionsPanel.dealPositionFiter.setValue(dealPositionTableEditor.getVisibleIds());
      dealPosFilterLabel.setVisible(!(nw.size() == 1 && nw.get(0).equals(TableFilter.EmptyFilter)));
      if(actionsPanel.dealPositionFiter.getValue().length == 0)
        actionsPanel.clear();
      else actionsPanel.initData();
    });
    
    tabb.addChangeListener(e -> initTabb());
    
    greenButton.addItemListener(e -> setFilterStatus());
    redButton  .addItemListener(e -> setFilterStatus());
    blueButton .addItemListener(e -> setFilterStatus());
    blackButton.addItemListener(e -> setFilterStatus());
    yelowButton.addItemListener(e -> setFilterStatus());
    grayButton .addItemListener(e -> setFilterStatus());
    
    scale.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        try {
          if(e.getModifiers() == MouseEvent.META_MASK) {
            Integer[] dealIds = getSelectedDealIds();
            if(dealIds.length > 0) {
              JPopupMenu processPopMenu = new JPopupMenu();
              Map<String,Object> map = new TreeMap<>();
              map.put("ids", dealIds);
              for(JMenuItem item:ActionMacrosUtil.createMacrosMenu(Deal.class, map))
                processPopMenu.add(item);
              processPopMenu.show(scale, e.getX(), e.getY());
            }
          }
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
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
      public void objectPeriodsSelected(java.util.List<ObjectPeriod> periods) {
        periodsSelected();
        Integer[] ids = new Integer[0];
        for(ObjectPeriod p:periods)
          ids = (Integer[])ArrayUtils.add(ids, p.getId());
        fireChangeSelection(Deal.class, ids);
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
    
    /*splitTable.getTable(0).getSorter().addRowSorterListener(new RowSorterListener() {
      @Override
      public void sorterChanged(final RowSorterEvent e) {
        filteringTables();
      }
    });*/
    
    splitTable.getTable(0).addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 2 && e.getModifiers() != MouseEvent.META_MASK) {
          int row = splitTable.getTable(0).rowAtPoint(e.getPoint());
          if(row >= 0) {
            try {
              Integer id = (Integer) splitTable.getTable(0).getValueAt(row, 0);
              Contract contract = (Contract) ObjectLoader.getObject(Contract.class, id);
              if(contract != null) {
                ContractEditor editor = new ContractEditor();
                editor.setAutoLoad(true);
                editor.setAutoStore(true);
                editor.setEditorObject(contract);
                EditorController.getDeskTop().add(editor).getInternalDialog().setVisible(true);
                //editor.createDialog(CleverDealTableEditor.this,false).setVisible(true);
              }
            }catch(Exception ex) {
              Messanger.showErrorMessage(ex);
            }
          }
        }else {
          if(scale.getSelectedColumnCount() != scale.getColumnCount())
            splitTable.getTable(0).fireTableSelectionChange();
        }
      }
    });
    
    splitTable.getTable(2).addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 2 && e.getModifiers() != MouseEvent.META_MASK) {
          int row = splitTable.getTable(2).rowAtPoint(e.getPoint());
          if(row >= 0) {
            try {
              Object o = splitTable.getTable(2).getValueAt(row, 0);
              Integer id = (Integer) splitTable.getTable(2).getTableModel().getValueAt(row, 0);
              Company company = (Company) ObjectLoader.getObject(Company.class, id);
              if(company != null) {
                nCompanyEditor editor = new nCompanyEditor();
                editor.setAutoLoad(true);
                editor.setAutoStore(true);
                editor.setEditorObject(company);
                EditorController.getDeskTop().add(editor).getInternalDialog().setVisible(true);
                //editor.createDialog(CleverDealTableEditor.this,false).setVisible(true);
              }
            }catch(Exception ex) {
              Messanger.showErrorMessage(ex);
            }
          }
        }else {
          if(scale.getSelectedColumnCount() != scale.getColumnCount())
            splitTable.getTable(2).fireTableSelectionChange();
        }
      }
    });
  }
  
  private void setFilterStatus() {
    statusFilter.clear();
    
    String[] values = new String[0];
    if(greenButton.isSelected())
      values = (String[]) ArrayUtils.add(values, "Финиш");
    if(redButton.isSelected())
      values = (String[]) ArrayUtils.add(values, "Принято");
    if(blueButton.isSelected())
      values = (String[]) ArrayUtils.add(values, "Расчёт");
    if(blackButton.isSelected())
      values = (String[]) ArrayUtils.add(values, "Неопределённость");
    if(yelowButton.isSelected())
      values = (String[]) ArrayUtils.add(values, "Старт");
    if(grayButton.isSelected())
      values = (String[]) ArrayUtils.add(values, "Проект");
    
    if(values.length > 0)
      statusFilter.AND_IN("status", values);
    initData();
  }
  
  private void periodsSelected() {
    Integer[] dealIds = scale.getSelectedIds();
    
    greenButton.setText("0%");
    redButton.setText("0%");
    blueButton.setText("0%");
    blackButton.setText("0%");
    yelowButton.setText("0%");
    grayButton.setText("0%");
    
    selectedGREENDeals.setText("0 (0р.)");
    selectedREDDeals.setText("0 (0р.)");
    selectedBLUEDeals.setText("0 (0р.)");
    selectedYELOWDeals.setText("0 (0р.)");
    selectedDealsCount.setText("Выделенно: "+dealIds.length+" (0р.)");
    
    percentCountPanel.setCostData(0, 0, 0, 0, 0, 0);
    
    getGUI().repaint();
    
    if(dealIds.length > 0) {
      try {
        List<List> data = ObjectLoader.executeQuery("SELECT [Deal(status)], [Deal(needPay)], [Deal(cost)] FROM [Deal] WHERE [Deal(id)]=ANY(?)",true, new Object[]{dealIds});
        if(!data.isEmpty()) {
          int yelowCount = 0;
          int greenCount = 0;
          int redCount = 0;
          int blueCount = 0;
          int blackCount = 0;
          int grayCount = 0;
          
          BigDecimal yelowCost = new BigDecimal(0.0);
          BigDecimal greenCost = new BigDecimal(0.0);
          BigDecimal redCost   = new BigDecimal(0.0);
          BigDecimal blueCost  = new BigDecimal(0.0);
          BigDecimal blackCost = new BigDecimal(0.0);
          BigDecimal grayCost  = new BigDecimal(0.0);
          
          BigDecimal cost = new BigDecimal(0.0);
          for(List d:data) {
            cost = cost.add((BigDecimal)d.get(2));
            if(d.get(0).equals("Принято")) {
              redCount++;
              redCost = redCost.add((BigDecimal)d.get(1));
            }else if(d.get(0).equals("Расчёт")) {
              blueCount++;
              blueCost = blueCost.add((BigDecimal)d.get(2));
            }else if(d.get(0).equals("Финиш")) {
              greenCount++;
              greenCost = greenCost.add((BigDecimal)d.get(2));
            }else if(d.get(0).equals("Старт")) {
              yelowCount++;
              yelowCost = yelowCost.add((BigDecimal)d.get(2));
            }else if(d.get(0).equals("Неопределённость")) {
              blackCount++;
              blackCost = blackCost.add((BigDecimal)d.get(2));
            }else {
              grayCount++;
              grayCost = grayCost.add((BigDecimal)d.get(2));
            }
          }
          selectedGREENDeals.setText(greenCount+" ("+greenCost+"р.)");
          selectedREDDeals.setText(redCount+" ("+redCost+"р.)");
          selectedBLUEDeals.setText(blueCount+" ("+blueCost+"р.)");
          selectedYELOWDeals.setText(yelowCount+" ("+yelowCost+"р.)");
          selectedDealsCount.setText("Выделенно: "+dealIds.length+" ("+Utility.doubleToString(cost.doubleValue(), 2)+"р.)");
          
          percentCountPanel.setCostData(greenCost.doubleValue(), redCost.doubleValue(), blueCost.doubleValue(), yelowCost.doubleValue(), blackCost.doubleValue(), grayCost.doubleValue());
          
          double onePercent = cost.doubleValue()/100;
          
          greenButton.setText(Math.round(greenCost.doubleValue()/onePercent)+"%");
          redButton.setText(Math.round(redCost.doubleValue()/onePercent)+"%");
          blueButton.setText(Math.round(blueCost.doubleValue()/onePercent)+"%");
          blackButton.setText(Math.round(blackCost.doubleValue()/onePercent)+"%");
          yelowButton.setText(Math.round(yelowCost.doubleValue()/onePercent)+"%");
          grayButton.setText(Math.round(grayCost.doubleValue()/onePercent)+"%");
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }
  
  private void filteringTables() {
    splitTable.getTable(1).getTableModel().showAll();
    splitTable.getTable(2).getTableModel().showAll();

    int tableRowCount = splitTable.getTable(0).getRowCount();
    int modelRowCount = splitTable.getTable(0).getTableModel().getDataVector().size();

    if(tableRowCount == modelRowCount) {
      splitTable.getTable(2).getTableModel().showAll();
      splitTable.getTable(1).getTableModel().showAll();
    }else {
      int[] showRows = new int[0];
      for(int i=0;i<tableRowCount;i++) {
        Object tableRow = splitTable.getTable(0).getTableModel().getDataVector().get(splitTable.getTable(0).convertRowIndexToModel(i));
        for(int j=modelRowCount-1;j>=0;j--) {
          Object modelRow = splitTable.getTable(0).getTableModel().getDataVector().get(j);
          if(modelRow.equals(tableRow)) {
            showRows = ArrayUtils.add(showRows, j);
          }
        }
      }
      for(int j=modelRowCount-1;j>=0;j--) {
        if(!ArrayUtils.contains(showRows, j)) {
          splitTable.getTable(2).getTableModel().hideRow(j);
          splitTable.getTable(1).getTableModel().hideRow(j);
        }
      }
    }
  }
  
  @Override
  public void initData() {
    if(isActive()) {
      actionsPanel.clear();
      greenButton.setText("0%");
      redButton.setText("0%");
      blueButton.setText("0%");
      blackButton.setText("0%");
      yelowButton.setText("0%");
      grayButton.setText("0%");

      percentCountPanel.setCostData(0, 0, 0, 0, 0, 0);

      selectedDealsCount.setText("Выделенно: 0 (0р.)");
      selectedGREENDeals.setText("0 (0р.)");
      selectedBLUEDeals.setText("0 (0р.)");
      selectedREDDeals.setText("0 (0р.)");
      selectedYELOWDeals.setText("0 (0р.)");

      getGUI().repaint();
      
      Hronometr.start("Переключение");
      if(cleverDealTask != null) {
        cleverDealTask.setShutDown(true);
        cleverDealTask = null;
      }
      pool.submit(cleverDealTask = new CleverDealTask());
      Hronometr.stop("Переключение");
    }
  }
  
  public int rowAtTempProcess(Integer tempProcessId, Integer customerPartitionId) {
    for(int i=0;i<splitTable.getTable(0).getRowCount();i++)
      if(tempProcessId != null && tempProcessId.equals(splitTable.getTable(0).getTableModel().getValueAt(i, 6)) &&
              customerPartitionId != null && customerPartitionId.equals(splitTable.getTable(0).getTableModel().getValueAt(i, 2)))
        return i;
    return -1;
  }
  
  class CleverDealTask implements Runnable {
    private boolean shutDown = false;

    public boolean isShutDown() {
      return shutDown;
    }

    public void setShutDown(boolean shutDown) {
      this.shutDown = shutDown;
    }

    @Override
    public void run() {
      setCursor(new Cursor(Cursor.WAIT_CURSOR));
      try {
        if(!isShutDown()) {
          
          datesFilter.clear();
          datesFilter.AND_DATE_BETWEEN("dealStartDate", scale.getStartDate(), scale.getEndDate());
          datesFilter.OR_DATE_BETWEEN("dealEndDate", scale.getStartDate(), scale.getEndDate());
          
          agentFilter.clear();
          
          if(agents.length > 0 && cfcs.length > 0) {
            agentFilter.AND_IN("sellerCompany", agents);
            agentFilter.AND_IN("sellerCfc", cfcs);
            agentFilter.OR_IN("customerCompany", agents);
            agentFilter.AND_IN("customerCfc", cfcs);
          }else if(agents.length > 0 && cfcs.length == 0) {
            agentFilter.AND_IN("sellerCompany", agents);
            agentFilter.OR_IN("customerCompany", agents);
          }else if(agents.length == 0 && cfcs.length > 0) {
            agentFilter.AND_IN("sellerCfc", cfcs);
            agentFilter.OR_IN("customerCfc", cfcs);
          }
          
          System.out.println("Получение сделок...");
          Hronometr.start("Получение сделок");
          if(isShutDown())
            return;
          List<List> data = ObjectLoader.getData(
                  filter, 
                  new String[]{
                    /*0*/"id",
                    /*1*/"dealStartDate",
                    /*2*/"dealEndDate",
                    /*3*/"contract",
                    /*4*/"service",
                    /*5*/"service_name",
                    /*6*/"query:(SELECT (SELECT name FROM [OwnershipType] WHERE id=[Company(ownershipType)])||' '||name||' ('||(SELECT name FROM [CompanyPartition] WHERE id=[Deal(customerCompanyPartition)])||')' FROM [Company] WHERE [Company(id)]=[Deal(customerCompany)])",
                    /*7*/"customerpartition_id",
                    /*8*/"query:(SELECT (SELECT name FROM [OwnershipType] WHERE id=[Company(ownershipType)])||' '||name||' ('||(SELECT name FROM [CompanyPartition] WHERE id=[Deal(sellerCompanyPartition)])||')' FROM [Company] WHERE [Company(id)]=[Deal(sellerCompany)])",
                    /*9*/"sellerCompanyPartition",
                    /*10*/"sellerCompany",
                    /*11*/"customerCompany",
                    /*12*/"contract_number",
                    /*14*/"tempProcess"
                  }, 
                  new String[]{"customerpartition_id","contract_id","service_id"});
          Hronometr.stop("Получение сделок");
          if(isShutDown())
            return;
          
          TreeMap<String,Integer> rows = new TreeMap<>();

          final List<List> leftDataList   = new Vector<>();
          final List<List> centerDataList = new Vector<>();
          final List<List> rightDataList  = new Vector<>();
          final ArrayList<ObjectPeriod> periods = new ArrayList<>();
          final ArrayList<Integer> deals = new ArrayList<>();
          
          Hronometr.start("цикл");
          for(List d:data) {
            if(isShutDown())
              return;

            Integer dealId              = (Integer)       d.get(0);
            java.sql.Date startDate     = (java.sql.Date) d.get(1);
            java.sql.Date endDate       = (java.sql.Date) d.get(2);
            Integer contractId          = (Integer)       d.get(3);
            Integer serviceId           = (Integer)       d.get(4);
            String processName          = (String)        d.get(5);
            String customerName         = (String)        d.get(6);
            Integer customerPartitionId = (Integer)       d.get(7);
            String sellerName           = (String)        d.get(8);
            Integer sellerPartitionId   = (Integer)       d.get(9);
            Integer sellerId            = (Integer)       d.get(10);
            Integer customerId          = (Integer)       d.get(11);
            String contractNumber       = (String)        d.get(12);
            Integer tempProcessId       = (Integer)       d.get(13);

            String key = tempProcessId+"_"+customerPartitionId;
            Integer row = rows.get(key);

            deals.add(dealId);
            if(row == null) {
              leftDataList.add(new Vector(Arrays.asList(new Object[]{contractId,serviceId,customerPartitionId,sellerPartitionId,contractNumber,processName,tempProcessId})));

              Vector cd = new Vector();
              cd.setSize(splitTable.getTable(1).getColumnCount());
              centerDataList.add(cd);

              Vector rd = new Vector();
              rd.setSize(splitTable.getTable(2).getColumnCount());
              rd.setElementAt(agents.length==0||ArrayUtils.contains(agents, sellerId)?customerId:sellerId,0);
              rd.setElementAt(agents.length==0||ArrayUtils.contains(agents, sellerId)?customerName:sellerName,1);

              rightDataList.add(rd);

              rows.put(key, leftDataList.size()-1);
            }
            periods.add(scale.createPeriod(
                    row != null?row:(leftDataList.size()-1), 
                    dealId, 
                    startDate, 
                    endDate, 
                    Color.LIGHT_GRAY, 
                    ""));
          }
          
          Hronometr.stop("цикл");
          
          SwingUtilities.invokeLater(() -> {
            scale.clear();
            splitTable.clear();
            Hronometr.start("Отображение шкалы");
            scale.scrollToCurrentDate();
            splitTable.getTable(0).getTableModel().getDataVector().addAll(leftDataList);
            splitTable.getTable(0).getTableModel().fireTableDataChanged();
            splitTable.getTable(1).getTableModel().getDataVector().addAll(centerDataList);
            splitTable.getTable(1).getTableModel().fireTableDataChanged();
            splitTable.getTable(2).getTableModel().getDataVector().addAll(rightDataList);
            splitTable.getTable(2).getTableModel().fireTableDataChanged();
            scale.addPeriods(periods);
            
            if(isShutDown())
              return;
            
            setStatuses(deals.toArray(new Integer[0]));
            
            if(isShutDown())
              return;
            
            setComments(deals.toArray(new Integer[0]));
            
            Hronometr.stop("Отображение шкалы");
          });
        }
      }catch(Exception ex) {
        System.out.println("agents = "+agents);
        System.out.println("сасы   = "+cfcs);
        Messanger.showErrorMessage(ex);
      }finally {
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
    }
  }
  
  private void setComments(final Integer[] dealIds) {
    pool.submit(() -> {
      setCursor(new Cursor(Cursor.WAIT_CURSOR));
      Hronometr.start("Получение комментариев сделок");
      String[] querys = new String[2];
      Object[][] arrParams = new Object[2][1];
      querys[0] = "SELECT id,[Deal(service_name)]||' ('||[Deal(cost)]||'р.)' FROM [Deal] WHERE id=ANY(?)";
      arrParams[0][0] = dealIds;
      querys[1] = "SELECT "
              + "[DealComment(deal)],"
              + "[DealComment(date)],"
              + "[DealComment(comment)],"
              //+ "(SELECT [DealCommentSubject(color)] FROM [DealCommentSubject] WHERE [DealCommentSubject(id)]=[DealComment(subject)]), "
              + "[DealComment(stopTime)] "
              + "FROM [DealComment] "
              + "WHERE [DealComment(deal)]=ANY(?) AND tmp=false AND type='CURRENT'";
      arrParams[1][0] = dealIds;
      List<List<List>> datas = ObjectLoader.executeQuery(querys, arrParams);
      Hronometr.stop("Получение комментариев сделок");
      
      Hronometr.start("Расставляю комментарии");
      TreeMap<Integer, String> comments = new TreeMap<>();
      TreeMap<Integer, TreeMap<Long,UserComment>> dealDateUserComments     = new TreeMap<>();
      TreeMap<Integer, TreeMap<Long,UserComment>> dealStopDateUserComments = new TreeMap<>();
      datas.get(0).stream().forEach(d -> comments.put((Integer) d.get(0), (String) d.get(1)));
      datas.get(1).stream().forEach(d -> {
        TreeMap<Long,UserComment> dateUserComments     = dealDateUserComments.get((Integer) d.get(0));
        TreeMap<Long,UserComment> stopDateUserComments = dealStopDateUserComments.get((Integer) d.get(0));
        
        if(dateUserComments == null)
          dealDateUserComments.put((Integer) d.get(0), dateUserComments = new TreeMap<>());
        
        if(stopDateUserComments == null)
          dealStopDateUserComments.put((Integer) d.get(0), stopDateUserComments = new TreeMap<>());
        
        UserComment userComment = new UserComment(
                (Timestamp) d.get(1),
                new Timestamp(((java.sql.Date)d.get(3)).getTime()),
                (String) d.get(2));
        
        dateUserComments.put(((Timestamp) d.get(1)).getTime(), userComment);
        stopDateUserComments.put(((java.sql.Date)d.get(3)).getTime(), userComment);
      });
      
      scale.getPeriods().stream().forEach((list) -> {
        list.stream().forEach(period -> {
          if(comments.containsKey(period.getId()))
            period.setComment(comments.get(period.getId()));
          if(dealDateUserComments.containsKey(period.getId())) {
            period.setDateUserComments(dealDateUserComments.get(period.getId()));
            period.setStopDateUserComments(dealStopDateUserComments.get(period.getId()));
          }
        });
      });
      Hronometr.stop("Расставляю комментарии");
      scale.repaint();
      setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    });
  }
  
  /*private Color getColor(int count, int startCount, int dispatchCount, int paymentCount, BigDecimal paymentAmount, BigDecimal cost) {
    Color color = Color.BLACK;
    
    BigDecimal bCount         = new BigDecimal(count);
    BigDecimal bStartCount    = new BigDecimal(startCount);
    BigDecimal bDispatchCount = new BigDecimal(dispatchCount);
    
    BigDecimal startPercent    = cost.compareTo(BigDecimal.ZERO)==0?BigDecimal.ZERO:bStartCount.divide(bCount, 2, RoundingMode.HALF_UP);
    BigDecimal dispatchPercent = cost.compareTo(BigDecimal.ZERO)==0?BigDecimal.ZERO:bDispatchCount.divide(bCount, 2, RoundingMode.HALF_UP);
    BigDecimal paymentPercent  = cost.compareTo(BigDecimal.ZERO)==1&&paymentAmount.compareTo(BigDecimal.ZERO)==1?paymentAmount.divide(cost, MathContext.DECIMAL128):(cost.compareTo(BigDecimal.ZERO)==0&&paymentAmount.compareTo(BigDecimal.ZERO)==0&&paymentCount>0?BigDecimal.ONE:BigDecimal.ZERO);

    if(startPercent.compareTo(BigDecimal.ONE) == 0 && dispatchPercent.compareTo(BigDecimal.ONE) == 0 && paymentPercent.compareTo(BigDecimal.ONE) == 0)
      color = Color.GREEN;
    if(startPercent.compareTo(BigDecimal.ONE) == 0 && dispatchPercent.compareTo(BigDecimal.ONE) == 0 && paymentPercent.compareTo(BigDecimal.ZERO) == 0)
      color = Color.RED;
    if(startPercent.compareTo(BigDecimal.ONE) == 0 && dispatchPercent.compareTo(BigDecimal.ZERO) == 0 && paymentPercent.compareTo(BigDecimal.ONE) == 0)
      color = Color.BLUE;
    if(startPercent.compareTo(BigDecimal.ONE) == 0 && dispatchPercent.compareTo(BigDecimal.ZERO) == 0 && paymentPercent.compareTo(BigDecimal.ZERO) == 0)
      color = Color.YELLOW;
    if(startPercent.compareTo(BigDecimal.ZERO) == 0 && dispatchPercent.compareTo(BigDecimal.ZERO) == 0 && paymentPercent.compareTo(BigDecimal.ZERO) == 0)
      color = Color.LIGHT_GRAY;
    
    return color;
  }*/
  
  private void setStatuses(final Integer[] dealIds) {
    pool.submit(() -> {
      setCursor(new Cursor(Cursor.WAIT_CURSOR));
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
        scale.repaint();
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      });
    });
  }
  
  class CompanyFilterText extends FilterTextField {
    private final ExecutorService p = Executors.newFixedThreadPool(10);
    
    public CompanyFilterText(TableFilter filter, int column) {
      super(filter, Type.ALL, column);
    }

    @Override
    public void startFilter() {
      companyFilter.clear();
      final String text = getText();
      if(!"".equals(text)) {
        p.submit(() -> {
          try {
            Thread.sleep(1000);
          }catch (InterruptedException ex) {}
          if(CompanyFilterText.this.getText().equals(text)) {
            companyFilter.AND_ILIKE("customer", "%"+text+"%");
            initData();
          }
        });
      }else initData();
    }
    
    @Override
    public RowFilter getFilter() {
      return TableFilter.EmptyFilter;
    }
  }
  
  class ProcessFilterList extends FilterList {
    private final ExecutorService p = Executors.newFixedThreadPool(10);
    
    public ProcessFilterList(TableFilter filter, int column) {
      super(filter, column);
    }

    @Override
    public void reList() {
      try {
        DefaultListModel model = (DefaultListModel) getModel();
        String[] selectedValues = new String[0];
        for(int row:getSelectedIndices())
          selectedValues = (String[]) ArrayUtils.add(selectedValues, model.getElementAt(row));
        
        model.clear();
        model.addElement("Все");
        processFilter.clear();
        List<List> data = ObjectLoader.getData(filter, new String[]{"service_name"}, new String[]{"service_name"});
        data.stream().filter((d) -> (!model.contains(d.get(0)))).forEach((d) -> {
          model.addElement(d.get(0));
        });
        
        for(String val:selectedValues)
          addSelectionInterval(model.indexOf(val), model.indexOf(val));
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }

    @Override
    public void startFilter() {
      processFilter.clear();
      final Object[] objects = getSelectedValuesList().toArray();
      if(getSelectedIndex() > 0) {
        p.submit(() -> {
          try {
            Thread.sleep(1000);
          }catch (InterruptedException ex) {}
          if(Arrays.equals(objects, getSelectedValuesList().toArray())) {
            processFilter.AND_IN("service_name", objects);
            initData();
          }
        });
      }else initData();
    }
    
    @Override
    public void clearFilter() {
      super.clearFilter();
      startFilter();
    }

    @Override
    public RowFilter getFilter() {
      return TableFilter.EmptyFilter;
    }
  }

  @Override
  public void dispose() {
    actionsPanel.dispose();
    dealPositionTableEditor.dispose();
    dealCommentsTableEditor.dispose();
    super.dispose();
  }
}