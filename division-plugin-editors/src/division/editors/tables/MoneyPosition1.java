package division.editors.tables;

import bum.editors.EditorGui;
import bum.editors.util.ObjectLoader;
import bum.interfaces.CFC;
import bum.interfaces.Deal;
import bum.interfaces.Service;
import bum.interfaces.Worker;
import division.fx.PropertyMap;
import division.swing.CalendarChangeListener;
import division.swing.DistanceList;
import division.swing.guimessanger.Messanger;
import division.swing.TreeTable.SwingNode;
import division.swing.TreeTable.SwingObject;
import division.swing.TreeTable.TreeTable;
import division.swing.TreeTable.TreeTableModel;
import division.swing.DivisionCalendarComboBox;
import division.util.Utility;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import util.filter.local.DBFilter;

public class MoneyPosition1 extends EditorGui {
  private ExecutorService initPool = Executors.newSingleThreadExecutor();
  private ExecutorService ostatokPool = Executors.newSingleThreadExecutor();
  private Calendar c = Calendar.getInstance();
  private DivisionCalendarComboBox start = new DivisionCalendarComboBox();
  private DivisionCalendarComboBox end   = new DivisionCalendarComboBox(new Date());
  private JButton go = new JButton("получить");
  private JButton graf = new JButton("тест");
  
  private MoneyPositionNode root   = new MoneyPositionNode();
  private TreeTableModel    model  = new TreeTableModel(root);
  private TreeTable         table;
  private JScrollPane       scroll;
  
  private InitDataTask initDataTask = null;
  private OstatokDataTask ostatokDataTask = null;
  
  private Integer cfc;
  private Integer[] companys = new Integer[0];
  
  private DBFilter filter       = DBFilter.create(Deal.class);
  private DBFilter sideFilter   = filter.AND_FILTER();
  private DBFilter dateFilter   = filter.AND_FILTER();
  private DBFilter statusFilter = filter.AND_FILTER();
  
  private JLabel cfcName = new JLabel();
  private JLabel ostatok = new JLabel("Входящий остаток: ");
  private JLabel profit  = new JLabel("Профит: ");
  
  private NumberFormat df = NumberFormat.getInstance(); // создали форматер
  
  private DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Component com = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      TreePath path = MoneyPosition1.this.table.getTree().getPathForRow(row);
      if(path != null) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
        if(node instanceof SwingObject) {
          if(((SwingObject)node).getFont() != null)
            com.setFont(((SwingObject)node).getFont());
        }
      }
      return com;
    }
  };

  public MoneyPosition1() {
    super(null, null);
    initComponents();
    initEvents();
    
    setTitle("Денежная позиция");
    
    try {
      Integer workerId = ObjectLoader.getClient().getWorkerId();
      java.util.List<java.util.List> data = ObjectLoader.getData(Worker.class, new Integer[]{workerId}, new String[]{"cfc"});
      if(!data.isEmpty())
        cfc = (Integer) data.get(0).get(0);
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }


  @Override
  public void setActive(boolean active) {
    super.setActive(active);
    if(!isActive()) {
      if(initDataTask != null) {
        initDataTask.setShutdoun(true);
        initDataTask = null;
      }
      if(ostatokDataTask != null) {
        ostatokDataTask.setShutdown(true);
        ostatokDataTask = null;
      }
    }
  }

  public Integer getCfc() {
    return cfc;
  }

  public void setCfc(Integer cfc) {
    this.cfc = cfc;
  }

  public Integer[] getCompanys() {
    return companys;
  }

  public void setCompanys(Integer[] companys) {
    this.companys = companys;
  }

  private void initComponents() {
    getStatusBar().add(ostatok);
    getStatusBar().add(profit);
    
    df.setMaximumFractionDigits(2);
    df.setMinimumFractionDigits(2);
    
    c.setTime(new Date());
    c.set(Calendar.DAY_OF_MONTH, 1);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    start.setDateInCalendar(c.getTime());
    
    root.setFont(new Font("Dialog", Font.BOLD, 11));
    model.addColumn("Доход", "getDohod", "setDohod", String.class);
    model.addColumn("Расход", "getRashod", "setRashod", String.class);
    model.addColumn("Отгружено", "getLastDispatchDate", "setLastDispatchDate", Timestamp.class);
    model.addColumn("Оплачено", "getLastPayDate", "setLastPayDate", Timestamp.class);
    model.addColumn("ЦФУ", "getCFCName", "setCFCName", String.class);
    model.addColumn("Контрагент", "getCompanyName", "setCompanyName", String.class);
    table = new TreeTable(model);
    scroll = new JScrollPane(table);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    table.getColumnModel().getColumn(1).setCellRenderer(renderer);
    table.getColumnModel().getColumn(2).setCellRenderer(renderer);
    
    addComponentToStore(table);
    
    cfcName.setFont(new Font("Dialog", Font.BOLD, 16));
    getTopPanel().setLayout(new FlowLayout(FlowLayout.TRAILING, 10, 5));
    getTopPanel().add(cfcName);
    getTopPanel().add(new JLabel("с"));
    getTopPanel().add(start);
    getTopPanel().add(new JLabel("по"));
    getTopPanel().add(end);
    getTopPanel().add(go);
    getTopPanel().add(graf);
    
    getRootPanel().setLayout(new GridBagLayout());
    getRootPanel().add(scroll,  new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
  }
  
  public void profitLine(final LineChart chart, final String name, final LocalDate fstart, final LocalDate end, final int step) {
    XYChart.Series profit = new XYChart.Series<>(name, FXCollections.observableArrayList());
    chart.getData().add(profit);
    
    ExecutorService p = Executors.newCachedThreadPool();
    p.submit(() -> {
      LocalDate start = fstart;
      while(start.isBefore(end) || start.isEqual(end)) {
        BigDecimal profitY;
        switch(step) {
          case Calendar.MONTH:
            profitY = calcProfit(cfc, companys, start.withDayOfMonth(1), start.withDayOfMonth(start.lengthOfMonth())).setScale(2, RoundingMode.HALF_UP);
            final String monthLabel = start.withDayOfMonth(start.lengthOfMonth()).getMonth().getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())+" "+start.withDayOfMonth(start.lengthOfMonth()).format(DateTimeFormatter.ofPattern("yyyy"));
            Platform.runLater(() -> profit.getData().add(new XYChart.Data(monthLabel, profitY)));
            start = start.plusMonths(1);
            break;
          case Calendar.YEAR:
            profitY = calcProfit(cfc, companys, start.withDayOfYear(1), start.withDayOfYear(start.lengthOfYear())).setScale(2, RoundingMode.HALF_UP);
            final String yearLabel = start.withDayOfMonth(start.lengthOfYear()).format(DateTimeFormatter.ofPattern("yyyy"));
            Platform.runLater(() -> profit.getData().add(new XYChart.Data(yearLabel, profitY)));
            start = start.plusYears(1);
            break;
          case Calendar.DAY_OF_MONTH:
            profitY = calcProfit(cfc, companys, start, start.plusDays(1)).setScale(2, RoundingMode.HALF_UP);
            final String dateLabel = start.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            Platform.runLater(() -> profit.getData().add(new XYChart.Data(dateLabel, profitY)));
            start = start.plusDays(1);
            break;
        }
      }
    });
  }
  
  public void plusLine(final LineChart chart, String name, final LocalDate fstart, final LocalDate end, final int step) {
    final ArrayList<Integer> processes = new ArrayList();
    processes.add(-1);
    for(TreePath p:table.getTree().getSelectionModel().getSelectionPaths())
      processes.add(((MoneyPositionNode)p.getLastPathComponent()).getProcessId());
    
    if(processes.size() > 2)
      processes.add(1, 0);
    
    for(Integer process:processes) {
      String lineName = name;
      
      if(process == -1)
        lineName += " (полный)";
      if(process == 0)
        lineName += " ("+Utility.join(PropertyMap.getListFromList(ObjectLoader.getList(Service.class, processes.subList(2, processes.size()).toArray(new Integer[0]), "name"),"name", String.class).toArray(new String[0]), ", ")+")";
      if(process > 0)
        lineName += " ("+ObjectLoader.getMap(Service.class, process, "name").getString("name")+")";
      
      XYChart.Series data = new XYChart.Series<>(lineName, FXCollections.observableArrayList());
      chart.getData().add(data);

      ExecutorService p = Executors.newCachedThreadPool();
      p.submit(() -> {
        LocalDate start = fstart;
        while(start.isBefore(end) || start.isEqual(end)) {
          BigDecimal Y;
          switch(step) {
            case Calendar.MONTH:
              Y   = calcPlus(cfc, companys, start.withDayOfMonth(1), start.withDayOfMonth(start.lengthOfMonth()), process == -1 ? null : process == 0 ? processes.subList(2, processes.size()).toArray(new Integer[0]) : new Integer[]{process}).setScale(2, RoundingMode.HALF_UP);
              final String monthLabel = start.withDayOfMonth(start.lengthOfMonth()).getMonth().getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())+" "+start.withDayOfMonth(start.lengthOfMonth()).format(DateTimeFormatter.ofPattern("yyyy"));
              Platform.runLater(() -> data.getData().add(new XYChart.Data(monthLabel, Y)));
              start = start.plusMonths(1);
              break;
            case Calendar.YEAR:
              Y = calcPlus(cfc, companys, start.withDayOfYear(1), start.withDayOfYear(start.lengthOfYear()), process == -1 ? null : process == 0 ? processes.subList(2, processes.size()).toArray(new Integer[0]) : new Integer[]{process}).setScale(2, RoundingMode.HALF_UP);
              final String yearLabel = start.withDayOfMonth(start.lengthOfYear()).format(DateTimeFormatter.ofPattern("yyyy"));
              Platform.runLater(() -> data.getData().add(new XYChart.Data(yearLabel, Y)));
              start = start.plusYears(1);
              break;
            case Calendar.DAY_OF_MONTH:
              Y = calcPlus(cfc, companys, start, start.plusDays(1), process == -1 ? null : process == 0 ? processes.subList(2, processes.size()).toArray(new Integer[0]) : new Integer[]{process}).setScale(2, RoundingMode.HALF_UP);
              final String dateLabel = start.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
              Platform.runLater(() -> data.getData().add(new XYChart.Data(dateLabel, Y)));
              start = start.plusDays(1);
              break;
          }
        }
      });
    }
  }
  
  public void minusLine(final LineChart chart, final String name, final LocalDate fstart, final LocalDate end, final int step) {
    XYChart.Series data = new XYChart.Series<>(name, FXCollections.observableArrayList());
    chart.getData().add(data);
    
    ExecutorService p = Executors.newCachedThreadPool();
    p.submit(() -> {
      LocalDate start = fstart;
      while(start.isBefore(end) || start.isEqual(end)) {
        BigDecimal Y;
        switch(step) {
          case Calendar.MONTH:
            Y   = calcMinus(cfc, companys, start.withDayOfMonth(1), start.withDayOfMonth(start.lengthOfMonth())).setScale(2, RoundingMode.HALF_UP);
            final String monthLabel = start.withDayOfMonth(start.lengthOfMonth()).getMonth().getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())+" "+start.withDayOfMonth(start.lengthOfMonth()).format(DateTimeFormatter.ofPattern("yyyy"));
            Platform.runLater(() -> data.getData().add(new XYChart.Data(monthLabel, Y)));
            start = start.plusMonths(1);
            break;
          case Calendar.YEAR:
            Y = calcMinus(cfc, companys, start.withDayOfYear(1), start.withDayOfYear(start.lengthOfYear())).setScale(2, RoundingMode.HALF_UP);
            final String yearLabel = start.withDayOfMonth(start.lengthOfYear()).format(DateTimeFormatter.ofPattern("yyyy"));
            Platform.runLater(() -> data.getData().add(new XYChart.Data(yearLabel, Y)));
            start = start.plusYears(1);
            break;
          case Calendar.DAY_OF_MONTH:
            Y = calcMinus(cfc, companys, start, start.plusDays(1)).setScale(2, RoundingMode.HALF_UP);
            final String dateLabel = start.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            Platform.runLater(() -> data.getData().add(new XYChart.Data(dateLabel, Y)));
            start = start.plusDays(1);
            break;
        }
      }
    });
  }
  
  private void initEvents() {
    start.addCalendarChangeListener(new CalendarChangeListener() {
      @Override
      public void CalendarChangeDate(DistanceList dates) {
      }

      @Override
      public void CalendarSelectedDate(DistanceList dates) {
        if(start.getDate().after(end.getDate()))
          end.setDateInCalendar(start.getDate());
      }
    });
    
    go.addActionListener((ActionEvent e) -> {
      initData();
    });
    
    graf.addActionListener((ActionEvent e) -> {
      Platform.runLater(() -> {
        NumberAxis yAxis = new NumberAxis();
        yAxis.setAutoRanging(true);
        yAxis.setTickLabelFormatter(new StringConverter<Number>() {
          @Override
          public String toString(Number object) {
            return df.format(object)+" р.";
          }

          @Override
          public Number fromString(String string) {
            try {
              return df.parse(string);
            } catch (ParseException ex) {
              ex.printStackTrace();
              return null;
            }
          }
        });
        
        LineChart chart = new LineChart(new CategoryAxis(), yAxis);
        VBox.setVgrow(chart, Priority.ALWAYS);
        VBox vBox = new VBox(chart);
        Stage primaryStage = new Stage();
        primaryStage.setScene(new Scene(vBox, 800, 600));
        primaryStage.show();
        profitLine(chart, "профит", LocalDate.now().minusMonths(24), LocalDate.now(), Calendar.MONTH);
        plusLine(chart, "доход", LocalDate.now().minusMonths(24), LocalDate.now(), Calendar.MONTH);
        minusLine(chart, "расход", LocalDate.now().minusMonths(24), LocalDate.now(), Calendar.MONTH);
        
        chart.lookupAll(".chart-legend-item").stream().forEach(n -> {
          javafx.scene.control.Label l = ((javafx.scene.control.Label)n);
          l.setCursor(javafx.scene.Cursor.HAND);
          l.setOnMouseClicked(me -> {
            chart.getData().stream().filter(cc -> ((Series)cc).getName().equals(l.getText())).forEach(cc -> {
              ((Series)cc).getNode().setVisible(!((Series)cc).getNode().isVisible());
              l.setOpacity(((Series)cc).getNode().isVisible() ? 1 : 0.5);
            });
          });
        });
        
        chart.getData().stream().forEach(cc -> {
          Tooltip.install(((Series)cc).getNode(), new Tooltip(((Series)cc).getName()));
          
          ((Series)cc).getData().addListener((ListChangeListener.Change c1) -> {
            ((Series)cc).getData().stream().forEach(d -> {
              ((Data)d).getNode().visibleProperty().bind(((Series)cc).getNode().visibleProperty());
              Tooltip.install(((Data)d).getNode(), new Tooltip(((Series)cc).getName()+" за "+((Data)d).getXValue()+" ("+df.format(((Data)d).getYValue())+" р.)"));
            });
          });
        });
      });
    });
    
    
    table.getTree().addTreeWillExpandListener(new TreeWillExpandListener() {
      @Override
      public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        MoneyPositionNode node = (MoneyPositionNode)event.getPath().getLastPathComponent();
        if(!node.isRoot()) {
          node.setDohod("");
          node.setRashod("");
        }
      }

      @Override
      public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
        try {
          MoneyPositionNode node = (MoneyPositionNode)event.getPath().getLastPathComponent();
          if(!node.isRoot()) {
            for(int i=0;i<node.getChildCount();i++) {
              Double nodeDohod  = node.getDohod().equals("")?0d:df.parse(node.getDohod()).doubleValue();
              Double childDohod = df.parse(((MoneyPositionNode)node.getChildAt(i)).getDohod().equals("")?"0":((MoneyPositionNode)node.getChildAt(i)).getDohod()).doubleValue();
              Double dohod = nodeDohod + childDohod;

              double rashod = (node.getRashod().equals("")?0d:df.parse(node.getRashod()).doubleValue()) + (((MoneyPositionNode)node.getChildAt(i)).getRashod().equals("")?0d:df.parse(((MoneyPositionNode)node.getChildAt(i)).getRashod()).doubleValue());
              node.setDohod(dohod==0?"":df.format(dohod));
              node.setRashod(rashod==0?"":df.format(rashod));
            }
          }
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
  }
  
  @Override
  public void clear() {
    ostatok.setText("Входящий остаток......");
    profit.setText("Профит.....");
    root.removeAllChildren();
    root.setDohod("");
    root.setRashod("");
    model.reload();
  }
  
  @Override
  public void initData() {
    if(isActive()) {
      clear();
      if(initDataTask != null) {
        initDataTask.setShutdoun(true);
        initDataTask = null;
      }
      initPool.submit(initDataTask = new InitDataTask());
      if(ostatokDataTask != null) {
        ostatokDataTask.setShutdown(true);
        ostatokDataTask = null;
      }
      ostatokPool.submit(ostatokDataTask = new OstatokDataTask());
    }
  }

  @Override
  public Boolean okButtonAction() {
    return true;
  }
  
  private BigDecimal calcPlus(Integer cfc, Integer[] companys, LocalDate start, LocalDate end, Integer[] processes) {
    ObservableList params = FXCollections.observableArrayList(cfc, Utility.convertToTimestamp(start), Utility.convertToTimestamp(LocalDateTime.of(end, LocalTime.MAX)), Utility.convertToTimestamp(LocalDateTime.of(end, LocalTime.MAX)));
    if(processes != null && processes.length > 0)
      params.add(0, processes);
    BigDecimal d = (BigDecimal)ObjectLoader.executeQuery("SELECT NULLTOZERO((SELECT SUM([Deal(costWihoutNds)]) FROM [Deal] WHERE "
            + (processes != null && processes.length > 0 ? "[Deal(service)]=ANY(?) AND " : "")
            + "[Deal(sellerCfc)]=? AND "
            + "(SELECT COUNT(id) FROM [DealPosition] WHERE [DealPosition(deal)] = [Deal(id)] AND [DealPosition(dispatchId)] IS NULL AND tmp=false) = 0 AND"
            + "(SELECT SUM([DealPosition(customProductCost)] * [DealPosition(amount)]) FROM [DealPosition] WHERE [DealPosition(deal)] = [Deal(id)]) = (SELECT SUM([DealPayment(amount)]) FROM [DealPayment] WHERE [DealPayment(deal)] = [Deal(id)] AND tmp = false AND type='CURRENT') AND "
            + "[Deal(positionDate)] BETWEEN ? AND ? AND [Deal(dealEndDate)] <= ?))", 
            params.toArray()).get(0).get(0);
    return d;
  }
  
  private BigDecimal calcMinus(Integer cfc, Integer[] companys, LocalDate start, LocalDate end) {
    BigDecimal d = (BigDecimal)ObjectLoader.executeQuery("SELECT NULLTOZERO((SELECT SUM([Deal(costWihoutNds)]) FROM [Deal] WHERE "
            + "[Deal(customerCfc)]=? AND "
            + "(SELECT COUNT(id) FROM [DealPosition] WHERE [DealPosition(deal)] = [Deal(id)] AND [DealPosition(dispatchId)] IS NULL AND tmp=false) = 0 AND"
            + "(SELECT SUM([DealPosition(customProductCost)] * [DealPosition(amount)]) FROM [DealPosition] WHERE [DealPosition(deal)] = [Deal(id)]) = (SELECT SUM([DealPayment(amount)]) FROM [DealPayment] WHERE [DealPayment(deal)] = [Deal(id)] AND tmp = false AND type='CURRENT') AND "
            + "[Deal(positionDate)] BETWEEN ? AND ? AND [Deal(dealEndDate)] <= ?))", 
            cfc, Utility.convertToTimestamp(start), Utility.convertToTimestamp(LocalDateTime.of(end, LocalTime.MAX)), Utility.convertToTimestamp(LocalDateTime.of(end, LocalTime.MAX))).get(0).get(0);
    return d;
  }
  
  private BigDecimal calcProfit(Integer cfc, Integer[] companys, LocalDate start, LocalDate end) {
    BigDecimal d = (BigDecimal)ObjectLoader.executeQuery("SELECT NULLTOZERO((SELECT SUM([Deal(costWihoutNds)]) FROM [Deal] WHERE "
            + "[Deal(sellerCfc)]=? AND "
            + "(SELECT COUNT(id) FROM [DealPosition] WHERE [DealPosition(deal)] = [Deal(id)] AND [DealPosition(dispatchId)] IS NULL AND tmp=false) = 0 AND"
            + "(SELECT SUM([DealPosition(customProductCost)] * [DealPosition(amount)]) FROM [DealPosition] WHERE [DealPosition(deal)] = [Deal(id)]) = (SELECT SUM([DealPayment(amount)]) FROM [DealPayment] WHERE [DealPayment(deal)] = [Deal(id)] AND tmp = false AND type='CURRENT') AND "
            + "[Deal(positionDate)] BETWEEN ? AND ? AND [Deal(dealEndDate)] <= ?)) - "
            + "NULLTOZERO((SELECT SUM([Deal(costWihoutNds)]) FROM [Deal] WHERE "
            + "[Deal(customerCfc)]=? AND "
            + "(SELECT COUNT(id) FROM [DealPosition] WHERE [DealPosition(deal)] = [Deal(id)] AND [DealPosition(dispatchId)] IS NULL AND tmp=false) = 0 AND"
            + "(SELECT SUM([DealPosition(customProductCost)] * [DealPosition(amount)]) FROM [DealPosition] WHERE [DealPosition(deal)] = [Deal(id)]) = (SELECT SUM([DealPayment(amount)]) FROM [DealPayment] WHERE [DealPayment(deal)] = [Deal(id)] AND tmp = false AND type='CURRENT') AND "
            + "[Deal(positionDate)] BETWEEN ? AND ? AND [Deal(dealEndDate)] <= ?))", 
            cfc, Utility.convertToTimestamp(start), Utility.convertToTimestamp(LocalDateTime.of(end, LocalTime.MAX)), Utility.convertToTimestamp(LocalDateTime.of(end, LocalTime.MAX)),
            cfc, Utility.convertToTimestamp(start), Utility.convertToTimestamp(LocalDateTime.of(end, LocalTime.MAX)), Utility.convertToTimestamp(LocalDateTime.of(end, LocalTime.MAX))).get(0).get(0);
    return d;
  }

  @Override
  public void initTargets() {
  }
  
  class InitDataTask implements Runnable {
    private boolean shutdoun = false;

    public boolean isShutdoun() {
      return shutdoun;
    }

    public void setShutdoun(boolean shutdoun) {
      this.shutdoun = shutdoun;
    }
    
    @Override
    public void run() {
      setCursor(new Cursor(Cursor.WAIT_CURSOR));
      try {
        if(isShutdoun() || cfc == null)
          return;
        
        java.util.List<java.util.List> cfcData = ObjectLoader.getData(CFC.class, new Integer[]{cfc}, new String[]{"name"});
        if(!cfcData.isEmpty())
          cfcName.setText((String) cfcData.get(0).get(0));
        
        if(isShutdoun())
          return;
        c.setTime(end.getDate());
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
                
        statusFilter.clear().AND_EQUAL("dispatchPercent", BigDecimal.valueOf(100)).AND_EQUAL("paymentPercent", BigDecimal.valueOf(100));
        dateFilter.clear().AND_TIMESTAMP_BETWEEN("positionDate", new Timestamp(start.getDate().getTime()), new Timestamp(c.getTimeInMillis()));

        sideFilter.clear().AND_EQUAL("sellerCfc", cfc);
        if(companys != null && companys.length > 0)
          sideFilter.AND_IN("sellerCompany", companys);
        sideFilter.OR_EQUAL("customerCfc", cfc);
        if(companys != null && companys.length > 0)
          sideFilter.AND_IN("customerCompany", companys);
        
        if(isShutdoun())
          return;
        final java.util.List<java.util.List> data = ObjectLoader.getData(filter, new String[]{
          /*0*/ "id",
          /*1*/ "service",
          /*2*/ "service_name",
          /*3*/ "sellerCfc",
          /*4*/ "sellerCompany",
          /*5*/ "costWihoutNds",
          /*6*/ "lastDispatchDate",
          /*7*/ "lastPayDate",
          /*8*/ "positionDate",
          /*9*/ "seller",
          /*10*/"customer",
          /*11*/"seller_cfc_name",
          /*12*/"customer_cfc_name",
          /*13*/"customerCfc"
        }, new String[]{"finishDate"});
        
        if(isShutdoun())
          return;
        SwingUtilities.invokeLater(() -> {
          try {
            boolean seller;
            MoneyPositionNode parentNode = null;
            for(java.util.List d:data) {
              if(isShutdoun())
                return;
              seller = cfc.equals(d.get(3));
              if(seller && cfc.equals(d.get(13)))
                continue;
              parentNode = null;
              for(int i=0;i<root.getChildCount();i++) {
                if(((MoneyPositionNode)root.getChildAt(i)).getProcessId().equals(d.get(1)))
                  parentNode = (MoneyPositionNode)root.getChildAt(i);
              }
              if(parentNode == null)
                model.insertNodeInto(parentNode = new MoneyPositionNode((Integer) d.get(1), (String) d.get(2), "", ""), root, root.getChildCount());
              
              model.insertNodeInto(new MoneyPositionNode(
                      (Integer) d.get(0),
                      (Integer) d.get(1),
                      (String) d.get(2),
                      seller?df.format((BigDecimal)d.get(5)):"",
                      !seller?df.format((BigDecimal)d.get(5)):"",
                      (java.sql.Date) d.get(6),
                      (java.sql.Date) d.get(7),
                      (java.sql.Date) d.get(8),
                      seller?(String)d.get(12):(String)d.get(11),
                      seller?(String)d.get(10):(String)d.get(9)),
                      parentNode, parentNode.getChildCount());
              
              if(seller) {
                parentNode.setDohod(df.format((parentNode.getDohod().equals("")?new BigDecimal(0.0):new BigDecimal(df.parse(parentNode.getDohod()).doubleValue())).add((BigDecimal)d.get(5))));
                root.setDohod(df.format((root.getDohod().equals("")?new BigDecimal(0.0):new BigDecimal(df.parse(root.getDohod()).doubleValue())).add((BigDecimal)d.get(5))));
              }else {
                parentNode.setRashod(df.format((parentNode.getRashod().equals("")?new BigDecimal(0.0):new BigDecimal(df.parse(parentNode.getRashod()).doubleValue())).add((BigDecimal)d.get(5))));
                root.setRashod(df.format((root.getRashod().equals("")?new BigDecimal(0.0):new BigDecimal(df.parse(root.getRashod()).doubleValue())).add((BigDecimal)d.get(5))));
              }
              model.reload(parentNode);
            }
            model.reload();
            profit.setText("Профит: "+df.format(df.parse(root.getDohod().equals("")?"0":root.getDohod()).doubleValue() - df.parse(root.getRashod().equals("")?"0":root.getRashod()).doubleValue())+"р.");
          }catch(Exception ex) {
            Messanger.showErrorMessage(ex);
          }
        });
      }catch(Exception e) {
        setShutdoun(true);
        Messanger.showErrorMessage(e);
      }finally {
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
    }
  }
  
  class OstatokDataTask implements Runnable {
    private boolean shutdown = false;

    public boolean isShutdown() {
      return shutdown;
    }

    public void setShutdown(boolean shutdown) {
      this.shutdown = shutdown;
    }
    
    @Override
    public void run() {
      setCursor(new Cursor(Cursor.WAIT_CURSOR));
      try {
        if(isShutdown())
          return;
        try {
          java.util.List<java.util.List> data = ObjectLoader.executeQuery(""
                  + "SELECT NULLTOZERO((SELECT SUM([Deal(costWihoutNds)]) "
                  + "FROM [Deal] "
                  + "WHERE tmp=false AND "
                  + "[Deal(finishDate)]<? AND [Deal(dispatchPercent)]=100 AND [Deal(paymentPercent)]=100 AND "
                  + "[Deal(sellerCfc)]=?"
                  + (companys != null && companys.length>0?" AND [Deal(sellerCompany)]=ANY(?)":"")+")) - "
                  + "NULLTOZERO((SELECT SUM([Deal(costWihoutNds)]) "
                  + "FROM [Deal] "
                  + "WHERE tmp=false AND "
                  + "[Deal(finishDate)]<? AND [Deal(dispatchPercent)]=100 AND [Deal(paymentPercent)]=100 AND "
                  + "[Deal(customerCfc)]=?"
                  + (companys != null && companys.length>0?" AND [Deal(customerCompany)]=ANY(?)":"")+"))", true, 
                  companys != null && companys.length>0?
                  new Object[]{new java.sql.Date(start.getDate().getTime()), cfc, companys, new java.sql.Date(start.getDate().getTime()), cfc, companys}:
                  new Object[]{new java.sql.Date(start.getDate().getTime()), cfc, new java.sql.Date(start.getDate().getTime()), cfc});
          if(isShutdown())
            return;
          ostatok.setText("Входящий остаток: 0р");
          if(!data.isEmpty())
            ostatok.setText("Входящий остаток: "+df.format(data.get(0).get(0))+"р");
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
        
      }catch(Exception e) {
        setShutdown(true);
        Messanger.showErrorMessage(e);
      }finally {
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
    }
  }
  
  public class MoneyPositionNode extends SwingNode {
    private Integer   dealId;
    private Integer   processId;
    private String    dohod;
    private String    rashod;
    private java.sql.Date lastDispatchDate;
    private java.sql.Date lastPayDate;
    private String    companyName;
    private String    CFCName;

    public MoneyPositionNode() {
      super("Денежная позиция");
    }
    
    public MoneyPositionNode(Integer processId, String processName, String dohod, String rashod) {
      super(processName);
      this.processId   = processId;
      this.dohod       = dohod;
      this.rashod      = rashod;
    }
    
    public MoneyPositionNode(Integer dealId, Integer processId, String processName, String dohod, String rashod, java.sql.Date lastDispatchDate, java.sql.Date lastPayDate, java.sql.Date positionDate, String CFCName, String companyName) {
      super(Utility.format(positionDate));
      this.dealId      = dealId;
      this.processId   = processId;
      this.dohod       = dohod;
      this.rashod      = rashod;
      this.lastDispatchDate = lastDispatchDate;
      this.lastPayDate      = lastPayDate;
      this.CFCName          = CFCName;
      this.companyName      = companyName;
    }

    public String getCFCName() {
      return CFCName;
    }

    public void setCFCName(String CFCName) {
      this.CFCName = CFCName;
    }

    public String getCompanyName() {
      return companyName;
    }

    public void setCompanyName(String companyName) {
      this.companyName = companyName;
    }

    public String getDohod() {
      return dohod;
    }

    public void setDohod(String dohod) {
      this.dohod = dohod;
    }

    public java.sql.Date getLastDispatchDate() {
      return lastDispatchDate;
    }

    public void setLastDispatchDate(java.sql.Date lastDispatchDate) {
      this.lastDispatchDate = lastDispatchDate;
    }

    public java.sql.Date getLastPayDate() {
      return lastPayDate;
    }

    public void setLastPayDate(java.sql.Date lastPayDate) {
      this.lastPayDate = lastPayDate;
    }

    public String getRashod() {
      return rashod;
    }

    public void setRashod(String rashod) {
      this.rashod = rashod;
    }

    public Integer getDealId() {
      return dealId;
    }

    public Integer getProcessId() {
      return processId;
    }
  }
}