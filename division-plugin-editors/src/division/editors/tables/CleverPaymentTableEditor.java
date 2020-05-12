package division.editors.tables;

import bum.editors.EditorController;
import bum.editors.EditorGui;
import bum.editors.util.DivisionTarget;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Company;
import bum.interfaces.CreatedDocument;
import bum.interfaces.Document;
import bum.interfaces.Payment;
import bum.interfaces.ProductDocument.ActionType;
import bum.interfaces.Store.StoreType;
import division.editors.objects.company.nCompanyEditor;
import division.fx.PropertyMap;
import division.fx.controller.payment.FXPayment_1;
import division.fx.controller.payment.PaymentController;
import division.swing.DistanceList;
import division.swing.DistanceTime;
import division.swing.DivisionScrollPane;
import division.swing.DivisionTable;
import division.swing.DivisionTableRenderer;
import division.swing.DivisionToolButton;
import division.swing.guimessanger.Messanger;
import division.swing.table.filter.FilterCalendar;
import division.swing.table.filter.TableFilter;
import division.swing.table.groupheader.ColumnGroup;
import division.swing.table.groupheader.ColumnGroupHeader;
import division.swing.table.span.*;
import division.util.Hronometr;
import division.util.DocumentUtil;
import division.util.FileLoader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.*;
import mapping.MappingObject;
import org.apache.commons.lang.ArrayUtils;
import util.filter.local.DBFilter;

public class CleverPaymentTableEditor extends EditorGui {
  private ExecutorService pool = Executors.newFixedThreadPool(5);
  private InitDataTask initDataTask;
  
  private JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
  
  private DivisionTable topTable = new DivisionTable();
  private DivisionScrollPane topScroll = new DivisionScrollPane(topTable);
  
  private AttributiveCellTableModel ml;
  private MultiSpanCellTable bottomTable;
  private DivisionScrollPane bottomScroll;
  
  private DBFilter paymentFilter = DBFilter.create(Payment.class);
  private DBFilter companyFilter    = paymentFilter.AND_FILTER();
  private DBFilter dateFilter    = paymentFilter.AND_FILTER();
  
  private final ColumnGroupHeader header = new ColumnGroupHeader(topTable);
  
  private Integer[] companys = new Integer[0];
  
  protected DivisionToolButton editToolButton    = new DivisionToolButton(FileLoader.getIcon("Edit16.gif"),"Редактировать");
  protected DivisionToolButton removeToolButton  = new DivisionToolButton(FileLoader.getIcon("Delete16.gif"),"Удалить");
  
  private final String[] fields = new String[] {
    /*0*/"id",
    /*1*/"seller-store-type",
    /*2*/"amount",
    /*3*/"notDistribAmount",
    /*4*/"query:getCompanyName([Payment(sellerCompany)])",
    /*5*/"query:getCompanyName([Payment(customerCompany)])",
    /*6*/"sellerCompany",
    /*7*/"customerCompany",
    /*8*/"operationDate"
  };
  
  private final String[] paymentColumns = new String[] {
    /*0*/"id",
    /*1*/"Дата операции",
    /*2*/"дебет",
    /*3*/"кредит",
    /*4*/"не распределено",
    /*5*/"дебет",
    /*6*/"кредит",
    /*7*/"не распределно",
    /*8*/"id",
    /*9*/"Корреспондент"
  };
  
  public CleverPaymentTableEditor() {
    super("Платежи", null);
    initComponents();
    initEvents();
    
    getToolBar().add(editToolButton);
    getToolBar().add(removeToolButton);
  }
  
  private void initComponents() {
    String[] documentColumns = new String[0];
    java.util.List<java.util.List> data = ObjectLoader.getData(
            DBFilter.create(Document.class).AND_EQUAL("system", true).AND_EQUAL("actionType", ActionType.ОПЛАТА).AND_EQUAL("tmp", false).AND_EQUAL("type", Document.Type.CURRENT), 
            new String[]{"id","name"});
    for(java.util.List d:data) {
      documentColumns = (String[]) ArrayUtils.add(documentColumns, String.valueOf(d.get(0)));
      documentColumns = (String[]) ArrayUtils.add(documentColumns, d.get(1));
    }
    
    topTable.setColumns(ArrayUtils.addAll(paymentColumns, documentColumns));
   
    OperationDateFilter operationDateFilter = new OperationDateFilter(topTable.getTableFilters(), 1);
    topTable.getTableFilters().addFilter(1,operationDateFilter);

    Calendar c = Calendar.getInstance();
    c.set(c.get(Calendar.YEAR), 0, 1, 0, 0, 0);
    c.set(Calendar.MILLISECOND, 0);
    operationDateFilter.setFilter(new DistanceTime(c.getTimeInMillis(), System.currentTimeMillis()));
    
    topTable.getTableFilters().addTextFilter(paymentColumns.length-1);
    
    ColumnGroup bank = new ColumnGroup("Банк");
    bank.add(topTable.findTableColumn(2));
    bank.add(topTable.findTableColumn(3));
    bank.add(topTable.findTableColumn(4));
    header.addColumnGroup(bank);
    
    ColumnGroup cash = new ColumnGroup("Касса");
    cash.add(topTable.findTableColumn(5));
    cash.add(topTable.findTableColumn(6));
    cash.add(topTable.findTableColumn(7));
    header.addColumnGroup(cash);
    
    if(documentColumns.length > 0) {
      for(int i=paymentColumns.length;i<paymentColumns.length+documentColumns.length;i+=2) {
        topTable.getTableFilters().addTextFilter(i+1);
        topTable.setColumnWidthZero(i);
      }
    }
    
    header.revalidate();
    topTable.setTableHeader(header);
    topTable.setColumnWidthZero(0,8);
    topTable.getTableFilters().addTextFilter(7);
    
    
    
    
    
    ml = new AttributiveCellTableModel(4,paymentColumns.length+documentColumns.length);
    bottomTable = new MultiSpanCellTable(ml) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }

      @Override
      public void valueChanged(ListSelectionEvent e) {
        CellFont     cellFont   = (CellFont)    ml.getCellAttribute();
        ColoredCell  cellColor  = (ColoredCell) ml.getCellAttribute();
        cellFont.setFont(new Font("Dialog", Font.PLAIN, 10), new int[]{0,1,2,3}, new int[]{1,1,1,1});
        cellColor.setBackground(bottomTable.getBackground(), new int[]{0,1,2,3}, new int[]{1,1,1,1});
        for(int row:bottomTable.getSelectedRows()) {
          cellFont.setFont(new Font("Dialog", Font.BOLD, 10), row, 1);
          cellColor.setBackground(Color.LIGHT_GRAY, row, 1);
        }
        bottomTable.repaint();
      }
    };
    bottomScroll = new DivisionScrollPane(bottomTable);
    
    topScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    bottomScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    
    
    bottomTable.getColumnModel().getColumn(0).setMinWidth(0);
    bottomTable.getColumnModel().getColumn(0).setPreferredWidth(0);
    bottomTable.getColumnModel().getColumn(0).setWidth(0);
    bottomTable.getColumnModel().getColumn(0).setMaxWidth(0);
    
    bottomTable.getColumnModel().getColumn(8).setMinWidth(0);
    bottomTable.getColumnModel().getColumn(8).setPreferredWidth(0);
    bottomTable.getColumnModel().getColumn(8).setWidth(0);
    bottomTable.getColumnModel().getColumn(8).setMaxWidth(0);
    
    if(documentColumns.length > 0) {
      for(int i=paymentColumns.length;i<paymentColumns.length+documentColumns.length;i+=2) {
        bottomTable.getColumnModel().getColumn(i).setMinWidth(0);
        bottomTable.getColumnModel().getColumn(i).setPreferredWidth(0);
        bottomTable.getColumnModel().getColumn(i).setWidth(0);
        bottomTable.getColumnModel().getColumn(i).setMaxWidth(0);
      }
    }
    
    
    
    DivisionTableRenderer renderer = new DivisionTableRenderer();
    for(int i=0;i<bottomTable.getColumnCount();i++)
      bottomTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
    
    
    CellSpan  cellAtt   = (CellSpan)ml.getCellAttribute();
    CellFont  cellFont  = (CellFont)ml.getCellAttribute();
    CellAlign cellAlign = (CellAlign)ml.getCellAttribute();
    
    cellAtt.combine(new int[]{1},new int[]{2,3});
    cellAtt.combine(new int[]{2},new int[]{2,3});
    cellAtt.combine(new int[]{3},new int[]{2,3});
    
    cellAtt.combine(new int[]{1},new int[]{5,6});
    cellAtt.combine(new int[]{2},new int[]{5,6});
    cellAtt.combine(new int[]{3},new int[]{5,6});
    
    if(documentColumns.length > 0) {
      int[] columns = new int[0];
      for(int i=paymentColumns.length-2;i<paymentColumns.length+documentColumns.length;i++)
        columns = ArrayUtils.add(columns, i);

      cellAtt.combine(new int[]{0},columns);
      cellAtt.combine(new int[]{1},columns);
      cellAtt.combine(new int[]{2},columns);
      cellAtt.combine(new int[]{3},columns);
    }
    
    ml.setValueAt("ОБОРОТЫ ЗА ПЕРИОД", 0, 1);
    ml.setValueAt("САЛЬДО ЗА ПЕРИОД",  1, 1);
    ml.setValueAt("ВХОДЯЩИЙ ОСТАТОК",  2, 1);
    ml.setValueAt("ИСХОДЯЦИЙ ОСТАТОК", 3, 1);
    ml.setValueAt("СУММА", 0, paymentColumns.length-2);
    
    cellFont.setFont(new Font("Dialog", Font.PLAIN, 10), new int[]{0,1,2,3}, new int[]{1,1,1,1});
    cellFont.setFont(new Font("Dialog", Font.BOLD, 12), 1, paymentColumns.length-2);
    cellFont.setFont(new Font("Dialog", Font.BOLD, 12), 2, paymentColumns.length-2);
    cellFont.setFont(new Font("Dialog", Font.BOLD, 12), 3, paymentColumns.length-2);
    
    cellAlign.setAlign(SwingConstants.CENTER, 1, 2);
    cellAlign.setAlign(SwingConstants.CENTER, 2, 2);
    cellAlign.setAlign(SwingConstants.CENTER, 3, 2);
    
    cellAlign.setAlign(SwingConstants.CENTER, 1, 5);
    cellAlign.setAlign(SwingConstants.CENTER, 2, 5);
    cellAlign.setAlign(SwingConstants.CENTER, 3, 5);
    
    cellAlign.setAlign(SwingConstants.CENTER, 0, paymentColumns.length-2);
    cellAlign.setAlign(SwingConstants.CENTER, 1, paymentColumns.length-2);
    cellAlign.setAlign(SwingConstants.CENTER, 2, paymentColumns.length-2);
    cellAlign.setAlign(SwingConstants.CENTER, 3, paymentColumns.length-2);
    
    bottomTable.setTableHeader(null);
    bottomTable.revalidate();
    bottomTable.repaint();
     
    
    
    split.setDividerSize(2);
    
    addComponentToStore(topTable,"table");
    addComponentToStore(split,"split");
    
    split.add(topScroll,    JSplitPane.TOP);
    split.add(bottomScroll, JSplitPane.BOTTOM);
    
    getRootPanel().add(split, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
  }
  
  private void initEvents() {
    new DivisionTarget(Payment.class) {
      @Override
      public void messageReceived(String type, Integer[] ids, PropertyMap objectEventProperty) {
        switch(type) {
          case "REMOVE":
            for(int i=topTable.getTableModel().getColumnCount()-1;i>=0;i--)
              if(ArrayUtils.contains(ids, topTable.getTableModel().getValueAt(i, 0)))
                topTable.getTableModel().removeRow(i);
            fillBottomTable();
            break;
          case "UPDATE":
            break;
        }
      }
    };
    
    topTable.getRowSorter().addRowSorterListener((RowSorterEvent e) -> {
      fillBottomTable();
    });
    
    editToolButton.addActionListener((ActionEvent e) -> {
      int row    = topTable.getSelectedRow();
      if(row >= 0) {
        Integer paymentId = (Integer) topTable.getTableModel().getValueAt(row, 0);
        //new PaymentController(paymentId).show();
        Platform.runLater(() -> {
          try {
            FXPayment_1 payment = new FXPayment_1();
            payment.setObjectProperty(ObjectLoader.getMap(Payment.class, paymentId));
            payment.showAndWait();
          }catch(Exception ex) {
            Messanger.showErrorMessage(ex);
          }
        });
      }
    });
    
    removeToolButton.addActionListener((ActionEvent e) -> {
      int[] rows    = topTable.getSelectedRows();
      if(rows.length > 0) {
        if(JOptionPane.showConfirmDialog(
          getRootPanel(),
          "<html>Вы уверены в том что хотите <b>удалить</b> выделенны"+(rows.length>1?"е":"й")+" платеж"+(rows.length>1?"и":"")+"?</html>",
          "Подтверждение удаления",
          JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE) == 0) {
          Integer[] ids = null;
          for(int row:rows)
            ids = (Integer[]) ArrayUtils.add(ids, (Integer) topTable.getTableModel().getValueAt(row, 0));
          try {
            ObjectLoader.createSession(true).removeObjects(Payment.class, ids);
          }catch(Exception ex) {
            Messanger.showErrorMessage(ex);
          }
        }
      }
    });
    
    topTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 2 && e.getModifiers() != MouseEvent.META_MASK) {
          int column = topTable.columnAtPoint(e.getPoint());
          int row    = topTable.rowAtPoint(e.getPoint());
          if(row >= 0) {
            row = topTable.convertRowIndexToModel(row);
            if(column > paymentColumns.length-1) {
              Integer documentId = (Integer) topTable.getTableModel().getValueAt(row, column-1);
              if(documentId != null) {
                DocumentUtil.preview(documentId);
              }
            }else if(column < paymentColumns.length-2) {
              waitCursor();
              try {
                Integer paymentId = (Integer) topTable.getTableModel().getValueAt(row, 0);
                new PaymentController(paymentId).show();
                /*Payment payment = (Payment) ObjectLoader.getObject(Payment.class, paymentId);
                nPaymentEditor editor = new nPaymentEditor();
                editor.setAutoLoad(true);
                editor.setAutoStore(true);
                editor.setEditorObject(payment);
                editor.showEditorDialog(CleverPaymentTableEditor.this.getGUI());*/
              }catch(Exception ex) {
                Messanger.showErrorMessage(ex);
              }finally {
                defaultCursor();
              }
            }else {
              waitCursor();
              try {
                Integer companyId = (Integer) topTable.getTableModel().getValueAt(row, paymentColumns.length-2);
                Company company = (Company) ObjectLoader.getObject(Company.class, companyId);
                nCompanyEditor editor = new nCompanyEditor();
                editor.setAutoLoad(true);
                editor.setAutoStore(true);
                editor.setEditorObject(company);
                EditorController.getDeskTop().add(editor);
                if(editor.getInternalDialog() != null)
                  editor.getInternalDialog().setVisible(true);
              }catch(Exception ex) {
                Messanger.showErrorMessage(ex);
              }finally {
                defaultCursor();
              }
            }
          }
        }
      }
    });
    
    topTable.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
      @Override
      public void columnAdded(TableColumnModelEvent e) {
      }

      @Override
      public void columnRemoved(TableColumnModelEvent e) {
      }

      @Override
      public void columnMoved(TableColumnModelEvent e) {
      }

      @Override
      public void columnMarginChanged(ChangeEvent e) {
        for(int i=0;i<topTable.getColumnCount();i++) {
          bottomTable.getColumnModel().getColumn(i).setPreferredWidth(topTable.getColumnModel().getColumn(i).getPreferredWidth());
        }
      }

      @Override
      public void columnSelectionChanged(ListSelectionEvent e) {
      }
    });
  }
  
  @Override
  public void initData() {
    if(isActive()) {
      Hronometr.start("Переключение");
      if(initDataTask != null) {
        initDataTask.setShutDown(true);
        initDataTask = null;
      }
      clear();
      try {
        pool.submit(initDataTask = new InitDataTask());
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
      Hronometr.stop("Переключение");
    }
  }

  @Override
  public void clearData() {
    topTable.getTableModel().clear();
    topTable.getTableModel().fireTableDataChanged();
  }
  
  @Override
  public Boolean okButtonAction() {
    return true;
  }

  public Integer[] getCompanys() {
    return companys;
  }

  public void setCompanys(Integer[] companys) {
    this.companys = companys;
  }
  
  private void setDocuments() {
    if(topTable.getColumnCount() > 0) {
      Vector<Vector> data = topTable.getTableModel().getDataVector();
      final java.util.List<Integer> payments = new ArrayList<>();
      data.stream().forEach(d -> payments.add((Integer)d.get(0)));
      for(int i=paymentColumns.length;i<=topTable.getColumnCount()-2;i+=2) {
        final int index = i;
        pool.submit(() -> {
          try {
            Integer id = Integer.valueOf(topTable.findTableColumn(index).getIdentifier().toString());
            Hronometr.start("SET DOCUMENT "+id);
            java.util.List<java.util.List> documents = ObjectLoader.getData(
                    DBFilter.create(CreatedDocument.class).AND_EQUAL("document", id).AND_IN("payment", payments.toArray(new Integer[0])).AND_EQUAL("type", MappingObject.Type.CURRENT).AND_EQUAL("tmp", false), 
                    new String[]{"payment","id","number"},
                    true);
            data.stream().forEach(p -> {
              documents.stream().forEach(d -> {
                if(d.get(0).equals(p.get(0))) {
                  p.set(index,   d.get(1));
                  p.set(index+1, d.get(2));
                }
              });
            });
            Hronometr.stop("SET DOCUMENT "+id);
            SwingUtilities.invokeLater(() -> topTable.getTableModel().fireTableDataChanged());
          }catch(Exception ex) {
            Messanger.showErrorMessage(ex);
          }
        });
      }
    }
  }

  @Override
  public void initTargets() {
  }
  
  class InitDataTask implements Runnable{
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
        
        if(isShutDown())
          return;
        
        if(companys.length > 0) {
          companyFilter.clear().AND_IN("sellerCompany", companys).OR_IN("customerCompany", companys);
          
          Hronometr.start("PAYMENT");
          java.util.List<java.util.List> data = ObjectLoader.getData(
                  paymentFilter.AND_EQUAL("type", MappingObject.Type.CURRENT).AND_EQUAL("tmp", false), 
                  fields,
                  true,
                  "operationDate DESC");
          Hronometr.stop("PAYMENT");

          if(isShutDown())
            return;

          final Vector<Vector> dataVector = new Vector<>();
          for(java.util.List d:data) {
            if(isShutDown())
              return;

            Integer    id            = (Integer)    d.get(0);
            StoreType  storeType     = StoreType.valueOf((String)d.get(1));
            BigDecimal amount        = (BigDecimal) d.get(2);
            BigDecimal notDistrib    = (BigDecimal) d.get(3);
            String     sellerName    = (String)     d.get(4);
            String     customerName  = (String)     d.get(5);
            Integer    seller        = (Integer)    d.get(6);
            Integer    customer      = (Integer)    d.get(7);
            Date       operationDate = (Date)       d.get(8);
            
            if(!seller.equals(customer)) {
              boolean isSeller   = ArrayUtils.contains(companys, seller);
              boolean isCustomer = ArrayUtils.contains(companys, customer);

              BigDecimal 
                    bankDebet      = null, 
                    bankCredit     = null, 
                    bankNotDistrib = null, 
                    cashDebet      = null, 
                    cashCredit     = null, 
                    cashNotDistrib = null;

              if(isSeller) {
                if(storeType == StoreType.БЕЗНАЛИЧНЫЙ) { // БЕЗНАЛ
                  bankNotDistrib = notDistrib.compareTo(BigDecimal.ZERO)==0 ? null : notDistrib;
                  bankDebet      = amount;
                }else { // НАЛ
                  cashNotDistrib = notDistrib.compareTo(BigDecimal.ZERO)==0 ? null : notDistrib;
                  cashDebet      = amount;
                }

                Vector row = new Vector(Arrays.asList(new Object[] {
                  id,
                  operationDate,

                  bankDebet,
                  bankCredit,
                  bankNotDistrib,

                  cashDebet,
                  cashCredit,
                  cashNotDistrib,

                  customer,
                  customerName
                }));

                row.setSize(topTable.getColumnCount());
                dataVector.add(row);
              }

              bankDebet = bankNotDistrib = cashDebet = cashNotDistrib = null;

              if(isCustomer) {
                if(storeType == StoreType.БЕЗНАЛИЧНЫЙ) { // БЕЗНАЛ
                  bankNotDistrib = notDistrib.compareTo(BigDecimal.ZERO)==0 ? null : notDistrib;
                  bankCredit     = amount;
                }else { // НАЛ
                  cashNotDistrib = notDistrib.compareTo(BigDecimal.ZERO)==0 ? null : notDistrib;
                  cashCredit     = amount;
                }

                Vector row = new Vector(Arrays.asList(new Object[] {
                  id,
                  operationDate,

                  bankDebet,
                  bankCredit,
                  bankNotDistrib,

                  cashDebet,
                  cashCredit,
                  cashNotDistrib,

                  customer,
                  customerName
                }));

                row.setSize(topTable.getColumnCount());
                dataVector.add(row);
              }
            }
          }

          SwingUtilities.invokeLater(() -> {
            if(isShutDown())
              return;
            topTable.getTableModel().getDataVector().addAll(dataVector);
            topTable.getTableModel().fireTableDataChanged();
            setDocuments();
            fillBottomTable();
          });
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }finally {
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
    }
  }
  
  private void fillBottomTable() {
    ml.setValueAt(null, 0, 2);
    ml.setValueAt(null, 0, 3);
    ml.setValueAt(null, 0, 4);

    ml.setValueAt(null, 0, 5);
    ml.setValueAt(null, 0, 6);
    ml.setValueAt(null, 0, 7);
    
    ml.setValueAt(null, 1, 2);
    ml.setValueAt(null, 1, 5);
    
    ml.setValueAt(null, 1, paymentColumns.length-2);
    
    ml.setValueAt(null, 2, 2);
    ml.setValueAt(null, 2, 5);

    ml.setValueAt(null, 3, 2);
    ml.setValueAt(null, 3, 5);

    ml.setValueAt(null, 2, paymentColumns.length-2);
    ml.setValueAt(null, 3, paymentColumns.length-2);
    
    BigDecimal sumBankDebet      = new BigDecimal(0);
    BigDecimal sumBankCredit     = new BigDecimal(0);
    BigDecimal sumBankNotDistrib = new BigDecimal(0);
    
    BigDecimal sumCashDebet      = new BigDecimal(0);
    BigDecimal sumCashCredit     = new BigDecimal(0);
    BigDecimal sumCashNotDistrib = new BigDecimal(0);
    
    for(int i=0;i<topTable.getRowCount();i++) {

      BigDecimal value = (BigDecimal) topTable.getValueAt(i, 2);
      if(value != null)
        sumBankDebet = sumBankDebet.add(value);

      value = (BigDecimal) topTable.getValueAt(i, 3);
      if(value != null)
        sumBankCredit = sumBankCredit.add(value);
      
      value = (BigDecimal) topTable.getValueAt(i, 4);
      if(value != null)
        sumBankNotDistrib = sumBankNotDistrib.add(value);

      value = (BigDecimal) topTable.getValueAt(i, 5);
      if(value != null)
        sumCashDebet = sumCashDebet.add(value);

      value = (BigDecimal) topTable.getValueAt(i, 6);
      if(value != null)
        sumCashCredit = sumCashCredit.add(value);
      
      value = (BigDecimal) topTable.getValueAt(i, 7);
      if(value != null)
        sumCashNotDistrib = sumCashNotDistrib.add(value);
    }
    
    ml.setValueAt(sumBankDebet.compareTo(BigDecimal.ZERO)==0?null:sumBankDebet,   0, 2);
    ml.setValueAt(sumBankCredit.compareTo(BigDecimal.ZERO)==0?null:sumBankCredit, 0, 3);
    ml.setValueAt(sumBankNotDistrib.compareTo(BigDecimal.ZERO)==0?null:sumBankNotDistrib, 0, 4);

    ml.setValueAt(sumCashDebet.compareTo(BigDecimal.ZERO)==0?null:sumCashDebet,   0, 5);
    ml.setValueAt(sumCashCredit.compareTo(BigDecimal.ZERO)==0?null:sumCashCredit, 0, 6);
    ml.setValueAt(sumCashNotDistrib.compareTo(BigDecimal.ZERO)==0?null:sumCashNotDistrib, 0, 6);

    BigDecimal saldoBank = sumBankDebet.subtract(sumBankCredit);
    BigDecimal saldoCash = sumCashDebet.subtract(sumCashCredit);
    BigDecimal sumSaldo  = saldoBank.add(saldoCash);

    ml.setValueAt(saldoBank.compareTo(BigDecimal.ZERO)==0?null:saldoBank, 1, 2);
    ml.setValueAt(saldoCash.compareTo(BigDecimal.ZERO)==0?null:saldoCash, 1, 5);
    ml.setValueAt(sumSaldo.compareTo(BigDecimal.ZERO)==0?null:sumSaldo,   1, paymentColumns.length-2);
    
    pool.submit(() -> {
      DistanceList dates = null;
      if(((FilterCalendar)topTable.getTableFilters().getFilterComponent(1)).isFilter())
        dates = ((FilterCalendar)topTable.getTableFilters().getFilterComponent(1)).getDates();
      java.sql.Date startDate = dates==null||dates.isEmpty()?null:new Date(dates.get(0).getStart());
      
      //Входящий остаток по банку
      BigDecimal inOstBank = BigDecimal.ZERO;
      //Входящий остаток по кассе
      BigDecimal inOstCash = BigDecimal.ZERO;
      if(startDate != null) {
        java.util.List<java.util.List> data = ObjectLoader.executeQuery(""
                + "SELECT NULLTOZERO((SELECT SUM([Payment(amount)]) "
                + "FROM [Payment] "
                + "WHERE [Payment(sellerCompany)]=ANY(?) AND [Payment(customerCompany)]<>ALL(?) AND "
                + "tmp=false AND type='CURRENT' AND [Payment(seller-store-type)]='БЕЗНАЛИЧНЫЙ'"
                + " AND [Payment(operationDate)] < ?"
                + ")) - "

                + "NULLTOZERO((SELECT SUM([Payment(amount)]) "
                + "FROM [Payment] "
                + "WHERE [Payment(customerCompany)]=ANY(?) AND [Payment(sellerCompany)]<>ALL(?) AND "
                + "tmp=false AND type='CURRENT' AND [Payment(seller-store-type)]='БЕЗНАЛИЧНЫЙ'"
                + " AND [Payment(operationDate)] < ?"
                + "))",
                new Object[]{companys, companys, startDate, companys, companys, startDate});
        if(!data.isEmpty())
          inOstBank = (BigDecimal) data.get(0).get(0);
        
        data = ObjectLoader.executeQuery(""
                + "SELECT NULLTOZERO((SELECT SUM([Payment(amount)]) "
                + "FROM [Payment] "
                + "WHERE [Payment(sellerCompany)]=ANY(?) AND [Payment(customerCompany)]<>ALL(?) AND "
                + "tmp=false AND type='CURRENT' AND [Payment(seller-store-type)]='НАЛИЧНЫЙ'"
                + " AND [Payment(operationDate)] < ?"
                + ")) - "

                + "NULLTOZERO((SELECT SUM([Payment(amount)]) "
                + "FROM [Payment] "
                + "WHERE [Payment(customerCompany)]=ANY(?) AND [Payment(sellerCompany)]<>ALL(?) AND "
                + "tmp=false AND type='CURRENT' AND [Payment(seller-store-type)]='НАЛИЧНЫЙ'"
                + " AND [Payment(operationDate)] < ?"
                + "))",
                new Object[]{companys, companys, new Timestamp(startDate.getTime()), companys, companys, new Timestamp(startDate.getTime())});
        if(!data.isEmpty())
          inOstCash = (BigDecimal) data.get(0).get(0);
        
        final BigDecimal inOstBank_ = inOstBank;
        final BigDecimal inOstCash_ = inOstCash;
        final BigDecimal saldoBank_ = saldoBank;
        final BigDecimal saldoCash_ = saldoCash;
        
        SwingUtilities.invokeLater(() -> {
          ml.setValueAt(inOstBank_.compareTo(BigDecimal.ZERO)==0?null:inOstBank_, 2, 2);
          ml.setValueAt(inOstCash_.compareTo(BigDecimal.ZERO)==0?null:inOstCash_, 2, 5);

          //Исходящий остаток по банку
          BigDecimal outOstBank = inOstBank_.add(saldoBank_);
          ml.setValueAt(outOstBank.compareTo(BigDecimal.ZERO)==0?null:outOstBank, 3, 2);

          //Исходящий остаток по кассе
          BigDecimal outOstCash = inOstCash_.add(saldoCash_);
          ml.setValueAt(outOstCash.compareTo(BigDecimal.ZERO)==0?null:outOstCash, 3, 5);

          BigDecimal sumInOst  = inOstBank_.add(inOstCash_);
          BigDecimal sumOutOst = outOstBank.add(outOstCash);

          ml.setValueAt(sumInOst.compareTo(BigDecimal.ZERO)==0?null:sumInOst,   2, paymentColumns.length-2);
          ml.setValueAt(sumOutOst.compareTo(BigDecimal.ZERO)==0?null:sumOutOst, 3, paymentColumns.length-2);
        });
      }
    });
  }
  
  class OperationDateFilter extends FilterCalendar {
    public OperationDateFilter(TableFilter tableFilter, int column) {
      super(tableFilter, column);
    }

    @Override
    public boolean isOnlineFilter() {
      return false;
    }

    @Override
    public void setFilter(Object... params) {
      super.setFilter(params); //To change body of generated methods, choose Tools | Templates.
      if(isFilter())
        getDates().stream().forEach(d -> dateFilter.OR_DATE_AFTER_OR_EQUAL("operationDate", d.getStartDate()).AND_DATE_BEFORE_OR_EQUAL("operationDate", d.getEndDate()));
    }

    @Override
    public void startFilter() {
      dateFilter.clear();
      if(isFilter())
        getDates().stream().forEach(d -> dateFilter.OR_DATE_AFTER_OR_EQUAL("operationDate", d.getStartDate()).AND_DATE_BEFORE_OR_EQUAL("operationDate", d.getEndDate()));
      initData();
    }

    @Override
    public void clearFilter() {
      super.clearFilter();
      dateFilter.clear();
      initData();
    }
  }
}