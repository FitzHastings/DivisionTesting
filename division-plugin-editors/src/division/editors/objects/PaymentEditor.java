package division.editors.objects;

import division.swing.guimessanger.Messanger;
import bum.editors.EditorGui;
import bum.editors.EditorListener;
import bum.editors.TableEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.CompanyPartition;
import bum.interfaces.Contract;
import bum.interfaces.Payment;
import division.editors.contract.ContractEditor;
import division.editors.objects.company.SellerCustomerPanel;
import division.swing.actions.DivisionEvent;
import division.swing.*;
import division.swing.dnd.DNDPanel;
import division.swing.table.CellColorController;
import division.swing.table.CellEditableController;
import division.util.Fprinter;
import division.util.Utility;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.List;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import mapping.MappingObject;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;

public class PaymentEditor extends EditorGui {
  private final DivisionTable dealTable = new DivisionTable();
  private final DivisionScrollPane dealScroll = new DivisionScrollPane(dealTable);
  
  private Integer[]            deals                     = null;
  private Integer              paymentId                 = null;
  private Payment              payment                   = null;
  private final SellerCustomerPanel  sellerPanel               = new SellerCustomerPanel("Получатель",false,true,true,true,false,false);
  private final SellerCustomerPanel  customerPanel             = new SellerCustomerPanel("Плательщик",false,true,true,true,false,false);
  private final DNDPanel             payPanel                  = new DNDPanel("Платёж", new GridBagLayout());
  private final DivisionComboBox     payDocument               = new DivisionComboBox(new String[]{"Платёжное поручение","Наличный расчёт"});
  private final JTextField           numberText                = new JTextField();
  private final DivisionCalendarComboBox operDate                  = new DivisionCalendarComboBox();
  private final DivisionTextField        amountText                = new DivisionTextField(DivisionTextField.Type.FLOAT);
  private final LinkLabel            notDistribAmountLabel     = new LinkLabel();
  private boolean              checked                   = true;
  
  private final JButton printReceipt = new JButton("Пробить чек");
  private String  FRName = null;
  
  private final DivisionSplitPane split = new DivisionSplitPane();
  private final TableEditor contractTableEditor = new TableEditor(
          new String[]{"id","Номер","Наименование","Основание"},
          new String[]{"id","number","templatename"},
          Contract.class,
          ContractEditor.class,
          MappingObject.Type.CURRENT) {
            @Override
            protected void insertData(List<List> data, int startIndex) {
              for(List d:data)
                d.add(false);
              super.insertData(data, startIndex);
            }
          };
  
  public PaymentEditor() {
    this(null, null);
  }
  
  public PaymentEditor(Integer[] deals) {
    this(null, deals);
  }

  public PaymentEditor(Payment payment) {
    this(payment, null);
  }
  
  private PaymentEditor(Payment payment, Integer[] deals) {
    super(null, null);
    this.payment = payment;
    this.deals   = deals;
    initComponents();
    initEvents();
  }
  
  private void initComponents() {
    addSubEditorToStore(contractTableEditor);
    addComponentToStore(split);
    
    dealTable.setColumns(
      "id",
      "наименование",
      "начало",
      "окончание",
      "Цена",
      "Задолжность",
      "Платёж", // Платёж именно с этого платежа.
      "Зачёт",
      "Количество оплат с этого платежа",
      "Общее количество оплат");
    dealTable.setColumnWidthZero(new int[]{0,8,9});
    
    JLabel label         = new JLabel("Нераспределённые средства");
    
    getRootPanel().setLayout(new GridBagLayout());
    getRootPanel().add(sellerPanel,           new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 0), 0, 0));
    getRootPanel().add(customerPanel,         new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 0), 0, 0));
    
    getRootPanel().add(payPanel,              new GridBagConstraints(0, 1, 2, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 0), 0, 0));
    
    getRootPanel().add(split,                 new GridBagConstraints(0, 2, 2, 1, 1.0, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 0), 0, 0));
    
    JPanel panel = new JPanel(new GridBagLayout());
    panel.add(label,                 new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
    panel.add(notDistribAmountLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
    
    getRootPanel().add(panel,                 new GridBagConstraints(0, 3, 2, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
    
    getButtonsPanel().add(printReceipt, 0);
    
    contractTableEditor.getTable().setSortable(false);
    contractTableEditor.getTable().setColumnEditable(3, true);
    contractTableEditor.setAdministration(false);
    contractTableEditor.getScroll().setRowHeader(false);
    contractTableEditor.getTable().setFocusable(false);
    contractTableEditor.getTable().setSelectionBackground(contractTableEditor.getTable().getBackground());
    split.add(contractTableEditor.getRootPanel(), JSplitPane.LEFT);
    split.add(dealScroll, JSplitPane.RIGHT);
    
    //contractPanel.getBorder().setLinkBorder(false);
    //contractPanel.add(contractComboBox, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 0), 0, 0));
    
    JLabel summ = new JLabel("руб.");
    summ.setFont(new Font("Dialog",Font.BOLD,30));
    
    label.setFont(new Font("Dialog", Font.BOLD, 24));
    notDistribAmountLabel.setFont(new Font("Dialog", Font.BOLD, 24));
    
    payPanel.getBorder().setLinkBorder(false);
    payPanel.add(payDocument,         new GridBagConstraints(0, 0, 4, 1, 0.5, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 0), 0, 0));
    payPanel.add(new JLabel("№"),     new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
    payPanel.add(numberText,          new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 0), 0, 0));
    payPanel.add(new JLabel("дата операции"), new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
    payPanel.add(operDate,                new GridBagConstraints(3, 1, 1, 1, 0.5, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 0), 0, 0));
    payPanel.add(amountText,              new GridBagConstraints(4, 0, 1, 2, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 0), 0, 0));
    payPanel.add(summ,                new GridBagConstraints(5, 0, 1, 2, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));

    amountText.setFont(new Font("Dialog",Font.BOLD,40));
    amountText.setColumns(5);
    
    dealTable.setCellEditableController(new CellEditableController() {
      @Override
      public boolean isCellEditable(JTable table, int modelRow, int modelColumn) {
        if(modelColumn != 7)
          return false;
        
        BigDecimal amount = new BigDecimal(amountText.getText().equals("")?"0.0":amountText.getText());
        BigDecimal notDistrib       = getNotDistribAmount();
        BigDecimal cost             = (BigDecimal)  table.getModel().getValueAt(modelRow, 4);
        BigDecimal debt             = (BigDecimal)  table.getModel().getValueAt(modelRow, 5);
        BigDecimal dealPayment      = (BigDecimal)  table.getModel().getValueAt(modelRow, 6);
        int    paymentCount     = (int)     table.getModel().getValueAt(modelRow, 8);
        int    allPaymentCount  = (int)     table.getModel().getValueAt(modelRow, 9);
        
        if(amount.compareTo(new BigDecimal(0.0)) == 0) {
          return cost.compareTo(new BigDecimal(0.0)) == 0 && (allPaymentCount == paymentCount || paymentCount > 0);
        }else {
          if(dealPayment.compareTo(new BigDecimal(0.0)) > 0 || debt.compareTo(new BigDecimal(0.0)) > 0 && notDistrib.compareTo(new BigDecimal(0.0)) > 0 || cost.compareTo(new BigDecimal(0.0)) == 0 && debt.compareTo(new BigDecimal(0.0)) == 0 && allPaymentCount-paymentCount == 0) // или наш платёж или есть задолжность и есть нераспределённые средства.
            return true;
          else return false;
        }
      }
    });
    
    dealTable.setCellColorController(new CellColorController() {
      @Override
      public Color getCellColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        BigDecimal cost             = (BigDecimal)  table.getModel().getValueAt(modelRow, 4);
        BigDecimal debt             = (BigDecimal)  table.getModel().getValueAt(modelRow, 5);
        BigDecimal dealPayment      = (BigDecimal)  table.getModel().getValueAt(modelRow, 6);
        int    paymentCount     = (int)     table.getModel().getValueAt(modelRow, 8);
        int    allPaymentCount  = (int)     table.getModel().getValueAt(modelRow, 9);
        
        if(debt.compareTo(new BigDecimal(0.0)) == 0.0 && dealPayment.compareTo(new BigDecimal(0.0)) == 0.0 && paymentCount == 0 && allPaymentCount > 0)
          return Color.LIGHT_GRAY;
        else if(cost.compareTo(debt) != 0 && debt.compareTo(new BigDecimal(0.0)) != 0.0)
          return Color.RED;
        else return null;
      }

      @Override
      public Color getCellTextColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        return null;
      }
    });
  }
  
  private void initEvents() {
    printReceipt.addActionListener((ActionEvent e) -> {
      RemoteSession session = null;
      try {
        session = ObjectLoader.createSession();
        if(FRName != null && saveAction(session)) {
          if(paymentId == null && payment != null)
            paymentId = payment.getId();
          
          Double amount = amountText.getText().equals("")?0.0:Double.valueOf(amountText.getText());
          if(amount > 0 && paymentId != null) {
            List<List> data = session.executeQuery("SELECT "
                    + "[Payment(amount)], "
                    + "(SELECT [Company(defaultNds)] FROM [Company] WHERE [Company(id)]="
                    + "(SELECT [CompanyPartition(company)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[Payment(sellerCompanyPartition)])) "
                    + "FROM [Payment] WHERE [Payment(id)]="+paymentId);
            
            if(!data.isEmpty()) {
              BigDecimal cost = (BigDecimal) data.get(0).get(0);
              BigDecimal nds  = (BigDecimal) data.get(0).get(1);
              nds = cost.multiply(nds).divide(nds.add(new BigDecimal(100)), RoundingMode.HALF_UP);
              
              Fprinter.open(FRName);
              Fprinter.printString(FRName, "", 1);
              Fprinter.printString(FRName, "Услуги ЦТО = "+Utility.doubleToString(cost,2),1);
              Fprinter.printString(FRName, "", 1);
              
              String str = "Без НДС";
              if(nds.compareTo(new BigDecimal(0.0)) > 0)
                str = "В том числе НДС "+Utility.doubleToString(nds, 2);
              
              Fprinter.sale(FRName, "Услуги ЦТО", 1, cost);
              Fprinter.closeReceipt(FRName, cost, str);
              Fprinter.close(FRName);
              
              Fprinter.open(FRName);
              Integer receiptNumber = Fprinter.getCurrentReceiptNumber(FRName);
              Fprinter.close(FRName);
              
              session.executeUpdate("UPDATE [Payment] SET [Payment(receiptNumber)]=? WHERE id=?", new Object[]{receiptNumber, paymentId});
              session.addEvent(Payment.class, "UPDATE", paymentId);
              
              printReceipt.setText("Чек № "+receiptNumber);
              setEnabled(false);
            }
          }
        }
        session.commit();
      }catch(Exception t) {
        ObjectLoader.rollBackSession(session);
        Messanger.showErrorMessage(t);
      }
    });
    
    sellerPanel.addDivisionListener((DivisionEvent divisionEvent) -> {
      initContractTable();
      setFrName();
    });
    
    customerPanel.addDivisionListener((DivisionEvent divisionEvent) -> {
      initContractTable();
    });
    
    amountText.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        notDistribAmountLabel.setText(String.valueOf(getNotDistribAmount()));
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        notDistribAmountLabel.setText(String.valueOf(getNotDistribAmount()));
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        notDistribAmountLabel.setText(String.valueOf(getNotDistribAmount()));
      }
    });
    
    dealTable.getTableModel().addTableModelListener((TableModelEvent e) -> {
      if(checked) {
        final int row = e.getLastRow();
        int column = e.getColumn();
        
        if(e.getType() == TableModelEvent.UPDATE && column == 7) {
          SwingUtilities.invokeLater(() -> {
            TableModel model = dealTable.getModel();
            BigDecimal  notDistrib    = getNotDistribAmount();
            boolean value             = (boolean) model.getValueAt(row, 7);
            BigDecimal  debt          = (BigDecimal)  model.getValueAt(row, 5);
            BigDecimal  dealPayment   = (BigDecimal)  model.getValueAt(row, 6);
            if(value) {
              //Полное погашение
              if(debt.compareTo(notDistrib) <= 0) {
                model.setValueAt(new BigDecimal(0.0), row, 5); // долг == 0
                model.setValueAt(debt, row, 6);
              }
              //Частичное погашение
              if(debt.compareTo(notDistrib) > 0) {
                model.setValueAt(debt.subtract(notDistrib), row, 5); // долг == debt-notDistrib
                model.setValueAt(notDistrib, row, 6);
              }
            }else {
              model.setValueAt(debt.add(dealPayment), row, 5); // долг == debt+dealPayment
              model.setValueAt(new BigDecimal(0.0), row, 6);
            }
            ((DefaultTableModel)model).fireTableDataChanged();
            notDistribAmountLabel.setText(String.valueOf(getNotDistribAmount()));
            //checkContractComboBoxEnabled();
            checkAmountTextEnabled();
          });
        }
      }
    });
    
    payDocument.addItemListener((ItemEvent e) -> {
      numberText.setEnabled(payDocument.getSelectedIndex() == 0);
      numberText.setEditable(payDocument.getSelectedIndex() == 0);
      operDate.setEnabled(payDocument.getSelectedIndex() == 0);
      printReceipt.setVisible(payDocument.getSelectedIndex() == 1);
    });
    
    contractTableEditor.getTable().getTableModel().addTableModelListener((TableModelEvent e) -> {
      if(e.getColumn() == 3) {
        if(!(Boolean)contractTableEditor.getTable().getValueAt(e.getLastRow(), 3)) {
          for(int i=0;i<dealTable.getTableModel().getRowCount();i++) {
            dealTable.getTableModel().setValueAt(false, i, 7);
            //d.set(7, false);
            //dealTable.getTableModel().fireTableCellUpdated(row, 7);
          }
        }
        initDealTable();
      }
    });
  }
  
  private void checkAmountTextEnabled() {
    if(printReceipt.isVisible() && !printReceipt.getText().equals("Пробить чек")) {
      amountText.setEnabled(false);
    }else {
      amountText.setEnabled(true);
      //TableModel model = dealTable.getModel();
      for(List d:(Vector<Vector>) dealTable.getTableModel().getDataVector()) {
        if((boolean) d.get(7) && ((int) d.get(8) > 0 || (int) d.get(8) == 0 && (int) d.get(9) == 0)) {
          amountText.setEnabled(false);
          break;
        }
      }
    }
  }
  
  private BigDecimal getNotDistribAmount() {
    BigDecimal notDistribAmount = new BigDecimal(amountText.getText().equals("")?"0.0":amountText.getText());
    for(Object d:dealTable.getTableModel().getDataVector())
      notDistribAmount = notDistribAmount.subtract((BigDecimal)((Vector)d).get(6));
    return notDistribAmount;
  }
  
  public String commit() {
    String msg = "";
    if(sellerPanel.getCompany() == null)
      msg += " - не указан получатель\n";
    if(customerPanel.getCompany() == null)
      msg += " - не указан плательщик\n";

    Double am = 0.0;
    if(amountText.getText() != null && !amountText.getText().equals(""))
      am = Double.valueOf(amountText.getText());

    if(payDocument.getSelectedIndex() == 0 && numberText.getText().equals(""))
      msg += " - укажите номер платёжного поручения\n";
    
    Integer[] contracts = getContracts();
    if(contracts.length == 0)
      msg += "С плательщиком отсутствует договор, платёж невозможен";
    return msg;
  }

  @Override
  public void initData() {
    if(deals != null && deals.length > 0)
      initFromDeals();
    else initFromPayment();
    checkAmountTextEnabled();
  }
  
  private void initFromDeals() {
    waitCursor();
    try {
      List<List> data = ObjectLoader.executeQuery("SELECT "
              + "[Deal(sellerCompanyPartition)],"
              + "[Deal(customerCompanyPartition)],"
              + "getDealNeedPay([Deal(id)]) "
              + "FROM [Deal] WHERE [Deal(id)]=ANY(?)", true, new Object[]{deals});

      BigDecimal amount = new BigDecimal(0.0);
      for(List d:data)
        amount = amount.add((BigDecimal)d.get(2));

      sellerPanel.setPartition((Integer) data.get(0).get(0));
      customerPanel.setPartition((Integer) data.get(0).get(1));
      amountText.setText(String.valueOf(amount));
      
      sellerPanel.setCompanyChoosable(false);
      sellerPanel.setPartitionChoosable(false);
      
      customerPanel.setCompanyChoosable(false);
      customerPanel.setPartitionChoosable(false);
      
      printReceipt.setVisible(payDocument.getSelectedIndex() == 1);
      
      Date date = (Date) System.getProperties().get("last.payment.date");
      if(date != null)
        operDate.setDateInCalendar(date);

    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }finally {
      defaultCursor();
    }
  }
  
  private void initFromPayment() {
    waitCursor();
    try {
      if(payment != null) {
        CompanyPartition sellerPartition   = payment.getSellerCompanyPartition();
        CompanyPartition customerPartition = payment.getCustomerCompanyPartition();

        BigDecimal amount = payment.getAmount();
        String number = payment.getPaymentDocumentNumber();
        java.sql.Date date = payment.getOperationDate();

        if(sellerPartition != null)
          sellerPanel.setPartition(sellerPartition.getId());
        if(customerPartition != null)
          customerPanel.setPartition(customerPartition.getId());
        
        if(amount != null)
          amountText.setText(String.valueOf(amount));
        if(number == null)
          payDocument.setSelectedIndex(1);
        else {
          numberText.setText(number);
          printReceipt.setVisible(false);
        }

        if(date != null)
          operDate.setDateInCalendar(date);
        
        if(!payment.isTmp()) {
          payDocument.setEnabled(false);

          sellerPanel.setCompanyChoosable(false);
          sellerPanel.setPartitionChoosable(false);

          customerPanel.setCompanyChoosable(false);
          customerPanel.setPartitionChoosable(false);
        }
        
        Integer receiptNumber = payment.getReceiptNumber();
        if(receiptNumber != null) {
          printReceipt.setText("Чек № "+receiptNumber);
          printReceipt.setEnabled(false);
        }
      }else {
        payDocument.setSelectedIndex(0);
        printReceipt.setVisible(false);
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }finally {
      defaultCursor();
    }
  }
  
  private void setFrName() {
    division.xml.Document document = division.xml.Document.load("conf"+File.separator+"machinery_configuration.xml");
    if(document != null) {
      System.out.println("sellerPanel.getPartition() = "+sellerPanel.getPartition());
      division.xml.Node frNode = document.getRootNode().getNode("FR_"+sellerPanel.getPartition());
      if(frNode != null)
        FRName = frNode.getAttribute("FiscalPrinterName");
    }
    printReceipt.setEnabled(FRName != null);
  }
  
  private void initContractTable() {
    contractTableEditor.getClientFilter().clear();
    try {
      Integer seller   = sellerPanel.getCompany();
      Integer customer = customerPanel.getCompany();
      if(seller != null && customer != null) {
        contractTableEditor.setActive(true);
        contractTableEditor.setEnabled(true);
        contractTableEditor.getClientFilter().AND_EQUAL("sellerCompany",   seller);
        contractTableEditor.getClientFilter().AND_EQUAL("customerCompany", customer);
        contractTableEditor.addEditorListener(new EditorListener() {
          @Override
          public void initDataComplited(EditorGui editor) {
            if(deals != null) {
              for(List d:ObjectLoader.executeQuery("SELECT [Deal(contract)] FROM [Deal] WHERE [Deal(id)]=ANY(?)", new Object[]{deals})) {
                int row = contractTableEditor.getRowObject((Integer)d.get(0));
                if(row >= 0)
                  contractTableEditor.getTable().setValueAt(true, row, 3);
              }
            }else {
              if(payment != null) {
                try {
                  List<List> data = ObjectLoader.executeQuery("SELECT [DealPayment(contract_id)] FROM [DealPayment] WHERE [DealPayment(payment)]="+payment.getId(), true);
                  data.addAll(ObjectLoader.executeQuery("SELECT [Payment(contracts):target] FROM [Payment(contracts):table] WHERE [Payment(contracts):object]="+payment.getId(), true));
                  for(List d:data) {
                    int row = contractTableEditor.getRowObject((Integer)d.get(0));
                    if(row >= 0)
                      contractTableEditor.getTable().setValueAt(true, row, 3);
                  }
                }catch(Exception ex) {
                  Messanger.showErrorMessage(ex);
                }
              }
            }
            if(getContracts().length == 0 && contractTableEditor.getAllIds().length == 1)
              contractTableEditor.getTable().setValueAt(true, 0, 3);
          }
        });
        contractTableEditor.initData();
      }else {
        contractTableEditor.setActive(false);
        contractTableEditor.setEnabled(false);
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  private Integer[] getContracts() {
    Integer[] contracts = new Integer[0];
    for(Vector d:(Vector<Vector>)contractTableEditor.getTable().getTableModel().getDataVector())
      if((Boolean)d.get(3))
        contracts = (Integer[]) ArrayUtils.add(contracts, d.get(0));
    return contracts;
  }
  
  private void initDealTable() {
    Integer[] contracts = getContracts();
    dealTable.getTableModel().clear();
    if(contracts.length > 0) {
      try {
        String dealWhere = "[Deal(contract)]=ANY(?)";
        if(deals != null)
          dealWhere += " AND [Deal(id)]=ANY(?)";

        if(dealWhere != null) {
          Vector<Vector> dataVector = dealTable.getTableModel().getDataVector();
          //deals = new Integer[0];
          List<List> data = ObjectLoader.executeQuery("SELECT "
                  + "[Deal(id)],"
                  + "[Deal(service_name)],"
                  + "[Deal(dealStartDate)],"
                  + "[Deal(dealEndDate)],"
                  + "[Deal(cost)],"
                  + "[Deal(needPay)],"
                  + "getPaymentCount([Deal(id)]) AS paymentCount,"
                  + "getContractName([Deal(contract)]),"
                  + "[Deal(contract)],"
                  + "getStartPercent([Deal(id)]) "
                  + "FROM [Deal] WHERE "+dealWhere+" ORDER BY [Deal(dealStartDate)], paymentCount DESC", true, 
                  deals==null?new Object[]{contracts}:new Object[]{contracts,deals});
          if(!data.isEmpty()) {
            for(List d:data) {
              d.add(6, deals!=null?(BigDecimal)d.get(5):new BigDecimal(0.0));
              d.add(7, deals!=null);
              d.add(8, 0);
              if(deals!=null)
                d.set(5, new BigDecimal(0.0));
              dataVector.add(new Vector(d));
            }
            
            if(payment != null && !payment.isTmp()) {
              data = ObjectLoader.executeQuery("SELECT [DealPayment(deal)],[DealPayment(amount)] FROM [DealPayment] WHERE [DealPayment(payment)]="+payment.getId(), true);
              for(List d:data) {
                for(Vector row:dataVector) {
                  if(row.get(0).equals(d.get(0))) {
                    row.set(6,d.get(1));
                    row.set(7,true);
                    row.set(8,1);
                  }
                }
              }
            }
          }
        }

        checked = false;
        dealTable.getTableModel().fireTableDataChanged();
        checked = true;
        notDistribAmountLabel.setText(String.valueOf(getNotDistribAmount()));
        //checkContractComboBoxEnabled();
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }
  
  /*private void checkContractComboBoxEnabled() {
    contractComboBox.setEnabled(true);
    if(contractComboBox.getItemCount() > 0)
      contractComboBox.setSelectedIndex(0);
    Vector<Vector> dataVector = dealTable.getTableModel().getDataVector();
    for(Vector d:dataVector) {
      if((boolean)d.get(7)) {
        contractComboBox.setSelectedIndex(-1);
        contractComboBox.setEnabled(false);
        break;
      }
    }
  }*/
  
  private boolean saveAction() {
    RemoteSession session = null;
    try {
      session = ObjectLoader.createSession();
      boolean is = saveAction(session);
      session.commit();
      return is;
    }catch (Exception e) {
      ObjectLoader.rollBackSession(session);
      Messanger.showErrorMessage(e);
      return false;
    }
  }
  
  private boolean saveAction(RemoteSession session) throws Exception {
    String msg = commit();
    if(msg != null && !msg.equals("")) {
      Messanger.alert(msg, JOptionPane.ERROR_MESSAGE);
      return false;
    }else {
      final List<Integer> payDeals     = new ArrayList<>();
      final List<BigDecimal>  amounts  = new ArrayList<>();
      final List<Integer> updatePay    = new ArrayList<>();
      final List<BigDecimal>  updateAmount = new ArrayList<>();
      final List<Integer> unpay        = new ArrayList<>();
      for(Vector d:(Vector<Vector>)dealTable.getTableModel().getDataVector()) {
        if((int)d.get(8) == 0 && (boolean)d.get(7)) {
          //Создаём платёж
          payDeals.add((Integer)d.get(0));
          amounts.add((BigDecimal)d.get(6));
        }else if((int)d.get(8) == 1 && !(boolean)d.get(7)) {
          //Удаляем платёж
          unpay.add((Integer)d.get(0));
        }else if((int)d.get(8) == 1 && (boolean)d.get(7)) {
          //Изменяем платёж
          updatePay.add((Integer)d.get(0));
          updateAmount.add((BigDecimal)d.get(6));
        }
      }

      final LocalProcessing processing = new LocalProcessing();
      //processing.setModal(true);
      processing.setSubProgressVisible(true);
      processing.setSubTextVisible(true);
      processing.setMinMax(0, 3);
      /*processing.submit(new Runnable() {
        @Override
        public void run() {*/
            boolean replaceDocument = false;
            boolean tmp          = payment==null||payment.isTmp();
            final boolean isNewPayment = tmp;
            if(payment != null) {
              Double amount = amountText.getText().equals("")?0.0:Double.valueOf(amountText.getText());
              replaceDocument = !tmp && (!amount.equals(payment.getAmount()) || (payDocument.getSelectedIndex()==0 && (!numberText.equals(payment.getPaymentDocumentNumber()) || operDate.getDate().getTime() != payment.getOperationDate().getTime())));
            }

            paymentId = payment != null?payment.getId():null;
            if(payment == null) {
              session.executeUpdate("INSERT INTO [Payment]([!Payment(amount)],[!Payment(operationDate)],[!Payment(paymentDocumentNumber)],"
                      + "[!Payment(sellerCompanyPartition)],[!Payment(customerCompanyPartition)]) VALUES (?,?,?,?,?)", new Object[] {
                        amountText.getText().equals("")?0.0:Double.valueOf(amountText.getText()),
                        payDocument.getSelectedIndex()==1?null:(numberText.getText().equals("")?null:new java.sql.Date(operDate.getDate().getTime())),
                        payDocument.getSelectedIndex()==1?null:(numberText.getText().equals("")?null:numberText.getText()),
                        sellerPanel.getPartition(),
                        customerPanel.getPartition()});
              paymentId = (Integer) session.executeQuery("SELECT MAX(id) FROM [Payment]").get(0).get(0);
              if(operDate.getDate() != null)
                System.getProperties().put("last.payment.date", operDate.getDate());
            }else {
              session.executeUpdate("UPDATE [Payment] SET [!Payment(amount)]=?, [!Payment(operationDate)]=?, [!Payment(paymentDocumentNumber)]=?, "
                      + "[!Payment(sellerCompanyPartition)]=?, [!Payment(customerCompanyPartition)]=?, tmp=false WHERE id=?", new Object[] {
                        amountText.getText().equals("")?0.0:Double.valueOf(amountText.getText()),
                        payDocument.getSelectedIndex()==1?null:(numberText.getText().equals("")?null:new java.sql.Date(operDate.getDate().getTime())),
                        payDocument.getSelectedIndex()==1?null:(numberText.getText().equals("")?null:numberText.getText()),
                        sellerPanel.getPartition(),
                        customerPanel.getPartition(),
                        paymentId});
            }
            
            session.executeUpdate("DELETE FROM [!Payment(contracts):table] WHERE [!Payment(contracts):object]="+paymentId);
            for(Integer contractId:getContracts())
              session.executeUpdate("INSERT INTO [!Payment(contracts):table]([!Payment(contracts):object],[!Payment(contracts):target]) "
                      + "VALUES("+paymentId+","+contractId+")");

            //ActionUtil.pay(isNewPayment, replaceDocument, paymentId, payDeals, amounts, updatePay, updateAmount, unpay, session, processing);
        /*}
      });*/
      //EditorController.getInstance().dispose(this);
      return true;
    }
  }

  @Override
  public Boolean okButtonAction() {
    boolean is = saveAction();
    if(is)
      dispose();
    return is;
  }

  @Override
  public void initTargets() {
  }
}