package division.editors.tables;

import bum.editors.EditorGui;
import bum.editors.util.ObjectLoader;
import bum.interfaces.DealPosition;
import division.scale.PeriodScale;
import division.swing.guimessanger.Messanger;
import division.swing.table.SplitTable;
import java.awt.*;
import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import util.filter.local.DBFilter;

public class DealPositionScale extends EditorGui {
  private boolean active = true;
  private SplitTable    splitTable = new SplitTable(3);
  private PeriodScale   scale = new PeriodScale(10, 30, 5);
  
  private ExecutorService pool = Executors.newSingleThreadExecutor();
  private DealPositionScaleTask dealPositionScaleTask;

  public DealPositionScale() {
    super(null, null);
    setAutoLoad(true);
    setAutoStore(true);
    initComponents();
    initEvents();
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
    splitTable.setColumns(new String[]{"dealPositionId","equipmentId","contractId","dealId","serviceId","customerPartitionId","sellerPartitionId","Предмет","Группа","Объект"}, 0);
    splitTable.setColumns(new String[]{"id","Контрагент"}, 2);
    
    splitTable.setTable(1, scale);
    splitTable.getTable(0).setColumnWidthZero(new int[]{0,1,2,3,4,5,6});
    splitTable.getTable(2).setColumnWidthZero(new int[]{0});
    splitTable.setSortable(false);
    splitTable.getTable(0).setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    splitTable.getTable(2).setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    
    splitTable.getScroll(0).setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    splitTable.getScroll(1).setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    splitTable.getScroll(2).setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    
    for(int i=0;i<splitTable.getTables().length;i++)
      if(i != 1)
        addComponentToStore(splitTable.getTable(i),"table"+i);
    for(int i=0;i<splitTable.getSplits().length;i++)
      addComponentToStore(splitTable.getSplits()[i],"split"+i);
    
    addComponentToStore(scale);
    
    getRootPanel().setLayout(new GridBagLayout());
    getRootPanel().add(splitTable, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 5, 0, 5), 0, 0));
  }

  private void initEvents() {
  }
  
  @Override
  public void initData() {
    splitTable.clear();
    if(dealPositionScaleTask != null) {
      dealPositionScaleTask.setShutDoun(true);
      dealPositionScaleTask = null;
    }
    if(isActive())
      pool.submit(dealPositionScaleTask = new DealPositionScaleTask());
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

  @Override
  public void initTargets() {
  }
  
  class DealPositionScaleTask implements Runnable {
    private boolean shutDoun = false;
    
    public DealPositionScaleTask() {
    }
    
    public boolean isShutDoun() {
      return shutDoun;
    }

    public void setShutDoun(boolean shutDoun) {
      this.shutDoun = shutDoun;
    }
    
    @Override
    public void run() {
      setCursor(new Cursor(Cursor.WAIT_CURSOR));
      try {
        final java.util.List<java.util.List> data = ObjectLoader.getData(
                DBFilter.create(DealPosition.class),
                new String[]{
                  /*0*/"id",
                  /*1*/"deal",
                  /*2*/"group_name",
                  /*3*/"identity_value",
                  /*4*/"process_id",
                  /*5*/"equipment",
                  /*6*/"contract_id",
                  /*7*/"deal_start_date",
                  /*8*/"deal_end_date",
                  /*9*/"customer_partition_id",
                  /*10*/"seller_partition_id",
                  /*11*/"getCompanyPartitionName([DealPosition(customer_partition_id)])",
                  /*12*/"service_name",
                  /*13*/"status",
                  /*14*/"cost"
                }, new String[]{"process_id","customer_partition_id"});
        /*splitTable.setColumns(new String[]{
         * "dealPositionId",
         * "equipmentId"
         * "contractId",
         * "dealId",
         * "serviceId",
         * "customerPartitionId",
         * "sellerPartitionId",
         * "Предмет",
         * "Группа",
         * "Объект"}, 0);
        splitTable.setColumns(new String[]{"id","Контрагент"}, 2);*/
        SwingUtilities.invokeLater(() -> {
          Vector<Vector> ldata = splitTable.getTable(0).getTableModel().getDataVector();
          Vector<Vector> cdata = splitTable.getTable(1).getTableModel().getDataVector();
          Vector<Vector> rdata = splitTable.getTable(2).getTableModel().getDataVector();
          Integer id1;
          Integer dealId;
          Integer process_id;
          Integer equipment_id;
          Integer contract_id;
          Integer customer_partition_id;
          Integer seller_partition_id;
          for(java.util.List d : data) {
            id1 = (Integer) d.get(0);
            dealId                = (Integer) d.get(1);
            process_id            = (Integer) d.get(4);
            equipment_id          = (Integer) d.get(5);
            contract_id           = (Integer) d.get(6);
            customer_partition_id = (Integer) d.get(9);
            seller_partition_id   = (Integer) d.get(10);
            int row = getRow(ldata, contract_id, process_id, equipment_id);
            if (row < 0) {
              ldata.add(new Vector(Arrays.asList(new Object[]{id1, equipment_id, contract_id, dealId, process_id, customer_partition_id, seller_partition_id, d.get(12), d.get(2), d.get(3)})));
              Vector cd = new Vector();
              cd.setSize(splitTable.getTable(1).getColumnCount());
              cdata.add(cd);
              rdata.add(new Vector(Arrays.asList(new Object[]{customer_partition_id,d.get(11)})));
            }
            scale.addPeriod(row != -1?row:(splitTable.getTable(1).getRowCount()-1), id1, (java.sql.Date)d.get(7), (java.sql.Date)d.get(8), getColor((String)d.get(13)), "Стоимость "+d.get(14)+"р.");
          }
          splitTable.getTable(0).getTableModel().fireTableDataChanged();
          splitTable.getTable(1).getTableModel().fireTableDataChanged();
          splitTable.getTable(2).getTableModel().fireTableDataChanged();
          scale.scrollToCurrentDate();
        });
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }finally {
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
    }
    
    private int getRow(Vector<Vector> ldata, Integer contractId, Integer process_id, Integer equipmentId) {
      for(int i=0;i<ldata.size();i++) {
        if(ldata.get(i).get(2).equals(contractId) && ldata.get(i).get(4).equals(process_id) && ldata.get(i).get(1).equals(equipmentId))
          return i;
      }
      return -1;
    }
  }
}
