package division.editors.objects;

import bum.editors.MainObjectEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.CompanyPartition;
import bum.interfaces.Group.ObjectType;
import bum.interfaces.Payment;
import bum.interfaces.ProductDocument.ActionType;
import bum.interfaces.Service.Owner;
import bum.interfaces.Store;
import bum.interfaces.Store.StoreType;
import division.editors.objects.company.SellerCustomerPanel;
import division.swing.DivisionScrollPane;
import division.swing.DivisionTable;
import division.swing.DivisionTextField;
import division.swing.LinkLabel;
import division.swing.actions.DivisionEvent;
import division.swing.guimessanger.Messanger;
import division.swing.table.CellColorController;
import division.util.ActionUtil;
import division.util.Fprinter;
import division.util.Utility;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import org.apache.commons.lang.ArrayUtils;
import util.filter.local.DBFilter;

public class nPaymentEditor extends MainObjectEditor {
  
  private final SellerCustomerPanel sellerPanel       = new SellerCustomerPanel("Получатель", false, true, false, false, false, false);
  private final LinkLabel           sellerStoreName   = new LinkLabel("Выбор депозитария...");
  
  private final SellerCustomerPanel customerPanel     = new SellerCustomerPanel("Плательщик", false, true, false, false, false, false);
  private final LinkLabel           customerStoreName = new LinkLabel("Выбор депозитария...");
  
  //private final DivisionTable       documentTable     = new DivisionTable();
  //private final DivisionScrollPane  tableScroll       = new DivisionScrollPane(documentTable);
  
  private final JLabel              amountLabel       = new JLabel("Сумма платежа");
  private final DivisionTextField   amountField       = new DivisionTextField(DivisionTextField.Type.FLOAT);
  private final JLabel              notDistribLabel   = new JLabel();
  
  private final JCheckBox           allDeal           = new JCheckBox("Показывать все сделки", false);
  private final DivisionTable       dealTable         = new DivisionTable();
  private final DivisionScrollPane  dealScroll        = new DivisionScrollPane(dealTable);
  
  private Integer sellerStoreId;
  private Integer customerStoreId;
  
  private Map<Integer, BigDecimal> startDealAmounts = null;
  
  private final JButton printReceipt = new JButton("Пробить чек");
  private String  FRName = null;
  
  private final LinkedHashMap<String, String> columns = new LinkedHashMap<>();
  private enum ColumnName{
    id,
    Договор,
    Номер,
    Процесс,
    начало,
    окончание,
    цена,
    долг,
    оплат,
    сумма_оплат,
    оплат_с_этого_платежа,
    платёж,
    зачёт
  }
  
  public nPaymentEditor() {
    initComponents();
    initEvents();
  }
  
  private int getColumnIndex(ColumnName columnName) {
    return ArrayUtils.indexOf(columns.keySet().toArray(), columnName.toString());
  }
  
  private void initComponents() {
    /*0*/ columns.put(ColumnName.id.toString(),                    "[Deal(id)]");
    /*1*/ columns.put(ColumnName.Договор.toString(),               "(SELECT [Contract(templatename)] FROM [Contract] WHERE [Contract(id)]=[Deal(contract)])");
    /*2*/ columns.put(ColumnName.Номер.toString(),                 "[Deal(contract_number)]");
    /*3*/ columns.put(ColumnName.Процесс.toString(),               "[Deal(service_name)]");
    /*4*/ columns.put(ColumnName.начало.toString(),                "[Deal(dealStartDate)]");
    /*5*/ columns.put(ColumnName.окончание.toString(),             "[Deal(dealEndDate)]");
    /*6*/ columns.put(ColumnName.цена.toString(),                  "[Deal(cost)]");
    /*7*/ columns.put(ColumnName.долг.toString(),                  "[Deal(needPay)]");
    /*8*/ columns.put(ColumnName.оплат.toString(),                 "[Deal(paymentCount)]");
    /*9*/ columns.put(ColumnName.сумма_оплат.toString(),           "[Deal(paymentAmount)]");
    /*10*/columns.put(ColumnName.оплат_с_этого_платежа.toString(), "getPaymentCountFromPayment([Deal(id)], ?)");
    /*11*/columns.put(ColumnName.платёж.toString(),                "getPaymentAmountFromPayment([Deal(id)], ?)");
    /*12*/columns.put(ColumnName.зачёт.toString(),                 "false");
    dealTable.setColumns(columns.keySet().toArray());
    dealTable.setColumnWidthZero(getColumnIndex(ColumnName.id),getColumnIndex(ColumnName.оплат),
            getColumnIndex(ColumnName.сумма_оплат),getColumnIndex(ColumnName.оплат_с_этого_платежа));
    
    addComponentToStore(dealTable, "dealTableForPaymentEditor");
    
    dealTable.setCellColorController(new CellColorController() {
      @Override
      public Color getCellColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        return null;
      }

      @Override
      public Color getCellTextColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        //Оплачена полностью не этим платежом
        if(
                (Integer)table.getModel().getValueAt(modelRow, getColumnIndex(ColumnName.оплат_с_этого_платежа)) == 0 && //Этот платёж не учавствует
                (Integer)table.getModel().getValueAt(modelRow, getColumnIndex(ColumnName.оплат)) > 0 && //Оплачено другим платежом
                (
                  ((BigDecimal)table.getModel().getValueAt(modelRow, getColumnIndex(ColumnName.цена))).compareTo(BigDecimal.ZERO) == 0 || //цена = 0
                  
                  ((BigDecimal)table.getModel().getValueAt(modelRow, getColumnIndex(ColumnName.долг))).compareTo(BigDecimal.ZERO) == 0 //долг = 0
                )
          )
          return Color.LIGHT_GRAY;
        
        //Оплачена не полностью
        if(
                ((BigDecimal)table.getModel().getValueAt(modelRow, getColumnIndex(ColumnName.долг))).compareTo(BigDecimal.ZERO) != 0 &&
                ((BigDecimal)table.getModel().getValueAt(modelRow, getColumnIndex(ColumnName.долг))).compareTo((BigDecimal)table.getModel().getValueAt(modelRow, getColumnIndex(ColumnName.цена))) != 0)
          return Color.RED;
        
        return null;
      }
    });
    
    dealTable.setCellEditableController((JTable table, int modelRow, int modelColumn) -> {
      /*if(modelColumn == getColumnIndex(ColumnName.зачёт)) {
        if(
                (Integer)table.getModel().getValueAt(modelRow, getColumnIndex(ColumnName.оплат_с_этого_платежа)) > 0 || // Этот платёж не учавствует
                (Integer)table.getModel().getValueAt(modelRow, getColumnIndex(ColumnName.оплат))  == 0) { // НЕ оплачено другим платежом
          return 
                  (boolean)table.getModel().getValueAt(modelRow, getColumnIndex(ColumnName.зачёт)) ||
                  getBalance().compareTo(BigDecimal.ZERO) > 0 || 
                  ((BigDecimal)table.getModel().getValueAt(modelRow, getColumnIndex(ColumnName.цена))).compareTo(BigDecimal.ZERO) == 0;
        }
      }
      return false;*/
      
      return  modelColumn == getColumnIndex(ColumnName.зачёт) && (
                (boolean)table.getModel().getValueAt(modelRow, getColumnIndex(ColumnName.зачёт)) ||

                getBalance().compareTo(BigDecimal.ZERO) > 0 &&
                (
                  ((BigDecimal)table.getModel().getValueAt(modelRow, getColumnIndex(ColumnName.цена))).compareTo(BigDecimal.ZERO) == 0 ||
                  ((BigDecimal)table.getModel().getValueAt(modelRow, getColumnIndex(ColumnName.долг))).compareTo(BigDecimal.ZERO) > 0
                )
              );
      
    });
    
    dealTable.setCellFontController((JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) -> {
      Font font = table.getFont();
      if(
              !(table.getModel().getValueAt(modelRow, getColumnIndex(ColumnName.зачёт)) instanceof JCheckBox) &&
              (boolean)table.getModel().getValueAt(modelRow, getColumnIndex(ColumnName.зачёт))) {
        return new Font(font.getFontName(), Font.BOLD, font.getSize());
      }
      return font;
    });
    
    /*addComponentToStore(documentTable);
    documentTable.setColumns("document-id","createdDocument-id","start-number","Документ","№","дата");
    documentTable.setColumnWidthZero(0,1,2);
    
    documentTable.setCellEditableController((JTable table, int modelRow, int modelColumn) -> 
            modelColumn > 1 && (Integer)table.getModel().getValueAt(modelRow, 2) == 0);
    
    documentTable.setCellColorController(new CellColorController() {
      @Override
      public Color getCellColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        Color color = documentTable.getTableHeader().getBackground();
        return modelColumn > 1 && (Integer)table.getModel().getValueAt(modelRow, 2) == 0 ? null : isSelect ? color.darker() : color;
      }

      @Override
      public Color getCellTextColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        return null;
      }
    });
    
    documentTable.setRowHeight(25);
    documentTable.setFont(new Font("Dialog", Font.BOLD, 14));*/
    
    notDistribLabel.setForeground(Color.red);
    notDistribLabel.setFont(new Font("Dialog", Font.BOLD, 20));
    amountLabel.setFont(new Font("Dialog", Font.BOLD, 24));
    amountField.setFont(new Font("Dialog", Font.BOLD, 40));
    amountField.setColumns(5);
    amountField.setMaxLengthAfterPoint(2);
    
    sellerPanel.setCompanyChoosable(false);
    customerPanel.setCompanyChoosable(false);
    
    sellerStoreName.setFont(new Font("Dialog", Font.BOLD, 16));
    customerStoreName.setFont(new Font("Dialog", Font.BOLD, 16));
    
    sellerPanel.add(sellerStoreName,     new GridBagConstraints(0, 4, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
    customerPanel.add(customerStoreName, new GridBagConstraints(0, 4, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
    
    getRootPanel().add(sellerPanel,   new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    getRootPanel().add(customerPanel, new GridBagConstraints(1, 0, 1, 1, 0.5, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    
    //getRootPanel().add(tableScroll, new GridBagConstraints(0, 1, 1, 2, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(3,3,3,3), 0, 0));
    getRootPanel().add(amountLabel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(3,3,3,3), 0, 0));
    getRootPanel().add(amountField, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3,3,3,3), 0, 0));
    
    getRootPanel().add(allDeal,     new GridBagConstraints(0, 2, 2, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.WEST, new Insets(3,3,3,3), 0, 0));
    getRootPanel().add(dealScroll,  new GridBagConstraints(0, 3, 2, 1, 1.0, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(3,3,3,3), 0, 0));
    
    
    getButtonsPanel().setLayout(new GridBagLayout());
    getButtonsPanel().add(notDistribLabel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.SOUTHWEST, new Insets(3,3,3,3), 0, 0));
    getButtonsPanel().add(printReceipt,    new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.SOUTHEAST, new Insets(3,3,3,3), 0, 0));
    getButtonsPanel().add(getOkButton(),   new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.SOUTHEAST, new Insets(3,3,3,3), 0, 0));
  }
  
  private void rePaymentDeals() {
    BigDecimal amount = getAmount();
    TreeMap<Integer, BigDecimal> dealPayments = getDealPayments();
    
    for(Vector d:(Vector<Vector>)dealTable.getTableModel().getDataVector()) {
      Integer id = (Integer) d.get(0);
      if(dealPayments.containsKey(id)) {
        BigDecimal dolg = (BigDecimal)d.get(getColumnIndex(ColumnName.долг));
        BigDecimal pay = BigDecimal.ZERO;
        if(amount.compareTo(dolg) > 0)
          pay = ((BigDecimal)d.get(getColumnIndex(ColumnName.цена))).subtract(dolg).add(dolg);
        else pay = amount;

        d.set(getColumnIndex(ColumnName.долг), dolg.add(dealPayments.get(id)).subtract(pay));
        d.set(getColumnIndex(ColumnName.платёж), pay);

        amount = amount.subtract(pay);
      }
    }
    dealTable.getTableModel().fireTableDataChanged();
  }

  private void initEvents() {
    dealTable.getTableModel().addTableModelListener(new TableModelListener() {
      private boolean active = true;
      @Override
      public void tableChanged(TableModelEvent e) {
        if(active && e.getType() == TableModelEvent.UPDATE && e.getColumn() == getColumnIndex(ColumnName.зачёт)) {
          active = false;
          TableModel model = dealTable.getTableModel();
          int row = e.getLastRow();
          boolean value = (boolean) model.getValueAt(row, getColumnIndex(ColumnName.зачёт));
          BigDecimal dealPaymentAmount;
          if(value) {
            dealPaymentAmount = (BigDecimal)model.getValueAt(row, getColumnIndex(ColumnName.долг));
            BigDecimal balance = getBalance();
            if(balance.compareTo(dealPaymentAmount) < 0)
              dealPaymentAmount = balance;
            
            model.setValueAt(dealPaymentAmount, row, getColumnIndex(ColumnName.платёж));
            model.setValueAt(((BigDecimal)model.getValueAt(row, getColumnIndex(ColumnName.долг))).subtract(dealPaymentAmount), row, getColumnIndex(ColumnName.долг));
          }else {
            dealPaymentAmount = (BigDecimal)model.getValueAt(row, getColumnIndex(ColumnName.платёж));
            model.setValueAt(BigDecimal.ZERO, row, getColumnIndex(ColumnName.платёж));
            model.setValueAt(((BigDecimal)model.getValueAt(row, getColumnIndex(ColumnName.долг))).add(dealPaymentAmount), row, getColumnIndex(ColumnName.долг));
          }
          active = true;
          setNotDistrib();
        }
        dealTable.repaint();
      }
    });
    
    sellerStoreName.addActionListener((ActionEvent e) -> {
      createDepositMenu(Owner.SELLER).show((LinkLabel)e.getSource(), 0, ((LinkLabel)e.getSource()).getHeight());
    });
    
    customerStoreName.addActionListener((ActionEvent e) -> {
      createDepositMenu(Owner.CUSTOMER).show((LinkLabel)e.getSource(), 0, ((LinkLabel)e.getSource()).getHeight());
    });
    
    allDeal.addItemListener((ItemEvent e) -> {
      TreeMap<Integer, BigDecimal> dealPayments = getDealPayments();
      initDealTable();
      for(Vector d:(Vector<Vector>)dealTable.getTableModel().getDataVector()) {
        if(!(d.get(12) instanceof JCheckBox) && (boolean)d.get(12)) {
          BigDecimal dealPaymentAmount = (BigDecimal) d.get(11);
          d.set(11, BigDecimal.ZERO);
          d.set(7, ((BigDecimal)d.get(7)).add(dealPaymentAmount));
          d.set(12, false);
        }
      }
      setDealPayments(dealPayments);
    });
    
    amountField.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        rePaymentDeals();
        setNotDistrib();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        rePaymentDeals();
        setNotDistrib();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        rePaymentDeals();
        setNotDistrib();
      }
    });
    
    /*documentTable.getTableModel().addTableModelListener((TableModelEvent e) -> {
      if(e.getType() == TableModelEvent.UPDATE && e.getColumn() == 5) {
        for(int i=0;i<documentTable.getTableModel().getRowCount();i++) {
          if(i != e.getLastRow()) {
            documentTable.getTableModel().setValueAt(documentTable.getTableModel().getValueAt(e.getLastRow(), 5), i, 5);
          }
        }
      }
    });*/
    
    printReceipt.addActionListener((ActionEvent e) -> {
      try {
        if(FRName != null && saveAction()) {
          Payment payment = (Payment) getEditorObject();
          Integer paymentId = payment.getId();
          if(paymentId == null && payment != null)
            paymentId = payment.getId();
          
          Double amount = amountField.getText().equals("")?0.0:Double.valueOf(amountField.getText());
          if(amount > 0 && paymentId != null) {
            List<List> data = ObjectLoader.executeQuery("SELECT "
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
              
              if(ObjectLoader.executeUpdate("UPDATE [Payment] SET [Payment(receiptNumber)]=? WHERE id=?", true, new Object[]{receiptNumber, paymentId}) == 1)
                ObjectLoader.sendMessage(Payment.class, "UPDATE", paymentId);
              
              printReceipt.setText("Чек № "+receiptNumber);
              setEnabled(false);
            }
          }
        }
      }catch(Exception t) {
        Messanger.showErrorMessage(t);
      }
    });
    
    sellerPanel.addDivisionListener((DivisionEvent divisionEvent) -> {
      setFrName();
    });
  }
  
  public void setDealPayments(TreeMap<Integer, BigDecimal> dealPayments) {
    if(!dealPayments.isEmpty()) {
      for(Vector d:(Vector<Vector>)dealTable.getTableModel().getDataVector()) {
        if(dealPayments.containsKey(d.get(0))) {
          d.set(getColumnIndex(ColumnName.зачёт), true);
          d.set(getColumnIndex(ColumnName.долг), ((BigDecimal)d.get(getColumnIndex(ColumnName.долг))).subtract(dealPayments.get(d.get(0))));
          d.set(getColumnIndex(ColumnName.платёж), dealPayments.get(d.get(0)));
        }
      }
    }
    dealTable.getTableModel().fireTableDataChanged();
    setNotDistrib();
  }
  
  private void setFrName() {
    division.xml.Document document = division.xml.Document.load("conf"+File.separator+"machinery_configuration.xml");
    if(document != null) {
      division.xml.Node frNode = document.getRootNode().getNode("FR_"+sellerPanel.getPartition());
      if(frNode != null)
        FRName = frNode.getAttribute("FiscalPrinterName");
    }
    printReceipt.setEnabled(FRName != null);
  }
  
  public void setAmount(BigDecimal amount) {
    amountField.setText(amount.toString());
  }
  
  private BigDecimal getAmount() {
    return BigDecimal.valueOf(Double.valueOf(amountField.getText().equals("")?"0":amountField.getText()));
  }
  
  private BigDecimal getBalance() {
    BigDecimal balance = getAmount();
    for(Vector d:(Vector<Vector>)dealTable.getTableModel().getDataVector())
      if((boolean)d.get(getColumnIndex(ColumnName.зачёт)))
        balance = balance.subtract((BigDecimal) d.get(getColumnIndex(ColumnName.платёж)));
    return balance;
  }
  
  private TreeMap<Integer, BigDecimal> getDealPayments() {
    TreeMap<Integer, BigDecimal> dealPayments = new TreeMap<>();
    for(Vector d:(Vector<Vector>)dealTable.getTableModel().getDataVector())
      if((boolean)d.get(getColumnIndex(ColumnName.зачёт)))
        dealPayments.put((Integer) d.get(getColumnIndex(ColumnName.id)), (BigDecimal) d.get(getColumnIndex(ColumnName.платёж)));
    return dealPayments;
  }
  
  private void initDealTable() {
    dealTable.getTableModel().clear();
    try {
      Integer id = getEditorObject().getId();
      List<List> data = ObjectLoader.executeQuery(
                "SELECT "+Utility.join(columns.values().toArray(), ",")+" "
              + "FROM [Deal] "
              + "WHERE [Deal(sellerCompanyPartition)]="+sellerPanel.getPartition()+" AND [Deal(customerCompanyPartition)]="+customerPanel.getPartition()
              + (allDeal.isSelected()?"":" AND ("
                      + "getPaymentCountFromPayment([Deal(id)], "+getEditorObject().getId()+") > 0 OR "
                      + "[Deal(cost)] = 0 AND [Deal(needPay)] = 0 OR "
                      + "[Deal(cost)] > 0 AND [Deal(needPay)] > 0)")
              + " ORDER BY [Deal(contract)], [Deal(service)], [Deal(dealStartDate)] DESC", true, new Object[]{id,id});
      
      data.stream().forEach(d -> 
        d.set(getColumnIndex(ColumnName.зачёт), 
                          (Integer)d.get(getColumnIndex(ColumnName.оплат_с_этого_платежа)) != 0));
      
      dealTable.getTableModel().getDataVector().addAll(data);
      dealTable.getTableModel().fireTableDataChanged();
      
      //dealTable.getTableModel().getDataVector().stream().forEach(d -> System.out.println(d));
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }finally {
      setNotDistrib();
    }
  }
  
  private void setNotDistrib() {
    BigDecimal balance = getBalance();
    if(balance.compareTo(BigDecimal.ZERO) == 0)
      notDistribLabel.setText("");
    else if(balance.compareTo(BigDecimal.ZERO) < 0)
      notDistribLabel.setText("Недостаточно средств: "+Utility.doubleToString(balance.multiply(new BigDecimal(-1)), 2));
    else notDistribLabel.setText("Нераспределённые средства: "+Utility.doubleToString(balance, 2));
  }
  
  private Integer[] getDeals() {
    return new Integer[0];
  }
  
  private List<List> getDocuments(Integer[] deals, Owner documentSource, Integer documentSourcePartition, 
          ActionType actionType, Boolean movable, Integer moneyStore, Integer tmcStore) {
    Object[] params = new Object[]{documentSourcePartition, documentSourcePartition, documentSource, actionType};
    String query = "SELECT "
            + "[Document(id)],"
            + "NULL, "
            
            + "CASE WHEN (SELECT [CompanyPartitionDocument(startNumber)] FROM [CompanyPartitionDocument] "
            + "WHERE tmp=false AND type='CURRENT' AND [CompanyPartitionDocument(document)]=[Document(id)] AND "
            + "[CompanyPartitionDocument(partition)]=?) IS NULL THEN 0 "
            + "ELSE (SELECT [CompanyPartitionDocument(startNumber)] FROM [CompanyPartitionDocument] "
            + "WHERE tmp=false AND type='CURRENT' AND [CompanyPartitionDocument(document)]=[Document(id)] AND "
            + "[CompanyPartitionDocument(partition)]=?) END,"
            
            + "[Document(name)],"
            + "NULL,"
            + "CURRENT_DATE "
            
            + "FROM [Document] "
            + "WHERE tmp=false AND type='CURRENT' AND "
            + "[Document(system)]=true AND "
            + "[Document(documentSource)]=? AND "
            + "[Document(actionType)]=?";
    
    params = ArrayUtils.add(params, documentSourcePartition);
    query += " AND ([Document(ndsPayer)] IS NULL OR [Document(ndsPayer)]=(SELECT [CompanyPartition(nds-payer)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=?))";
    
    if(movable != null) {
      params = ArrayUtils.add(params, movable);
      query += " AND ([Document(movable)] IS NULL OR [Document(movable)]=?)";
      
      if(movable) {
        if(moneyStore != null) {
          params = ArrayUtils.addAll(params, new Object[]{moneyStore, moneyStore});
          query += " AND "
                  + "("
                  + "  [Document(moneyCash)] = (SELECT [Store(storeType)]='"+StoreType.НАЛИЧНЫЙ+"' FROM [Store] WHERE id=?) OR "
                  + "  [Document(moneyCashLess)] = (SELECT [Store(storeType)]='"+StoreType.БЕЗНАЛИЧНЫЙ+"' FROM [Store] WHERE id=?)"
                  + ")";
        }
        if(tmcStore != null) {
          params = ArrayUtils.addAll(params, new Object[]{moneyStore, moneyStore});
          query += " AND "
                  + "("
                  + "  [Document(tmcCash)] = (SELECT [Store(storeType)]='"+StoreType.НАЛИЧНЫЙ+"' FROM [Store] WHERE id=?) OR "
                  + "  [Document(tmcCashLess)] = (SELECT [Store(storeType)]='"+StoreType.БЕЗНАЛИЧНЫЙ+"' FROM [Store] WHERE id=?)"
                  + ")";
        }
      }
    }
    
    if(deals != null && deals.length > 0) {
      params = ArrayUtils.addAll(params, new Object[]{actionType, deals});
      query += " OR "
              + "[Document(id)] IN (SELECT [ProductDocument(document)] FROM [ProductDocument] WHERE tmp=false AND type='CURRENT' AND "
              + "[ProductDocument(actionType)]=? AND [ProductDocument(product)] IN "
              + "(SELECT [DealPosition(product)] FROM [DealPosition] WHERE [DealPosition(deal)]=ANY(?))))";
    }
    
    try {
      return ObjectLoader.executeQuery(query, true, params);
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    return new Vector<>();
  }
  
  /*private void fillDocumentTable() {
    documentTable.getTableModel().clear();
    Integer[] deals = getDeals();
    Vector<Vector> data = getDocuments(deals, Owner.SELLER, sellerPanel.getPartition(), ActionType.ОПЛАТА, true, sellerStoreId, sellerStoreId);
    data.addAll(getDocuments(deals, Owner.CUSTOMER, customerPanel.getPartition(), ActionType.ОПЛАТА, true, customerStoreId, customerStoreId));
    data.stream().forEach(d -> documentTable.getTableModel().addRow(d));
  }*/
  
  private JPopupMenu createDepositMenu(final Owner companyType) {
    JPopupMenu menu = new JPopupMenu();
    try {
      List<List> data = ObjectLoader.getData(
              DBFilter.create(Store.class).AND_EQUAL("type", Store.Type.CURRENT).AND_EQUAL("tmp", false).
                      AND_EQUAL("companyPartition", companyType == Owner.SELLER?sellerPanel.getPartition():customerPanel.getPartition()).
                      AND_EQUAL("objectType", ObjectType.ВАЛЮТА),
              new String[]{"id","storeType","name","main",
                "query:SELECT NULLTOZERO(SUM([Equipment(amount)]-[Equipment(reserve)])) FROM [Equipment] "
                        + "WHERE [Equipment(store)]=[Store(id)] AND [Equipment(group)]=[Store(currency)] AND tmp=false AND type='CURRENT' AND [Equipment(reserve)] < [Equipment(amount)]"},
              new String[]{"storeType"});
      if(!data.isEmpty()) {
        data.stream().map((d) -> {
          final JMenuItem item = new JMenuItem(d.get(1)+" - "+d.get(2)+" ("+d.get(4)+")");
          item.addActionListener((ActionEvent e) -> {
            setDepositary(companyType, (Integer) d.get(0), item.getText());
            String otherStoreTypeName = (companyType == Owner.SELLER?customerStoreName.getText().substring(0, customerStoreName.getText().indexOf(" - ")):
                    sellerStoreName.getText().substring(0, sellerStoreName.getText().indexOf(" - "))).intern();

            if(!d.get(1).toString().equals(otherStoreTypeName)) {
              setDefaultDepositary(companyType == Owner.SELLER?Owner.CUSTOMER:Owner.SELLER);
              //fillDocumentTable();
            }
          });
          return item;
        }).forEach((item) -> {
          menu.add(item);
        });
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    return menu;
  }
  
  private void setDepositary(Owner companyType, Integer storeId, String storeName) {
    if(companyType == Owner.SELLER) {
      sellerStoreName.setText(storeName);
      sellerStoreId = storeId;
    }else {
      customerStoreName.setText(storeName);
      customerStoreId = storeId;
    }
  }
  
  private void setDefaultDepositary(Owner companyType) {
    try {
      String otherStoreTypeName = companyType == Owner.SELLER?customerStoreId==null?null:customerStoreName.getText().substring(0, customerStoreName.getText().indexOf(" - ")).intern():
              sellerStoreId==null?null:sellerStoreName.getText().substring(0, sellerStoreName.getText().indexOf(" - ")).intern();

      DBFilter filter = DBFilter.create(Store.class).AND_EQUAL("type", Store.Type.CURRENT).AND_EQUAL("tmp", false)
              .AND_EQUAL("companyPartition", companyType == Owner.CUSTOMER?customerPanel.getPartition():sellerPanel.getPartition());
      if(otherStoreTypeName != null)
        filter.AND_EQUAL("storeType", StoreType.valueOf(otherStoreTypeName));
      filter.AND_EQUAL("objectType", ObjectType.ВАЛЮТА);

      List<List> otherData = ObjectLoader.getData(
              filter,
              new String[]{"id","storeType","name","main",
              "query:SELECT NULLTOZERO(SUM([Equipment(amount)]-[Equipment(reserve)])) FROM [Equipment] "
                        + "WHERE [Equipment(store)]=[Store(id)] AND [Equipment(group)]=[Store(currency)] AND tmp=false AND type='CURRENT' AND [Equipment(reserve)] < [Equipment(amount)]"},
              new String[]{"storeType"});
      if(!otherData.isEmpty()) {
        List defaultStore = otherData.get(0);
        for(List od:otherData) {
          if(od.get(3) != null && (boolean)od.get(3)) {
            defaultStore = od;
            break;
          }
        }
        if(companyType == Owner.SELLER) {
          sellerStoreName.setText(defaultStore.get(1)+" - "+defaultStore.get(2)+" ("+defaultStore.get(4)+")");
          sellerStoreId = (Integer) defaultStore.get(0);
        }else {
          customerStoreName.setText(defaultStore.get(1)+" - "+defaultStore.get(2)+" ("+defaultStore.get(4)+")");
          customerStoreId = (Integer) defaultStore.get(0);
        }
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  @Override
  public void initData() throws RemoteException {
    Payment payment = (Payment) getEditorObject();
    
    CompanyPartition sellerPartition   = payment.getSellerCompanyPartition();
    CompanyPartition customerPartition = payment.getCustomerCompanyPartition();
    
    sellerPanel.setPartition(sellerPartition.getId());
    customerPanel.setPartition(customerPartition.getId());
    
    amountField.setText(payment.getAmount().toString());
    
    Store sellerStore   = payment.getSellerStore();
    
    if(sellerStore != null)
      setDepositary(Owner.SELLER, sellerStore.getId(), sellerStore.getStoreType()+" - "+sellerStore.getName());
    else setDefaultDepositary(Owner.SELLER);
    
    Store customerStore = payment.getCustomerStore();
    
    if(customerStore != null)
      setDepositary(Owner.CUSTOMER, customerStore.getId(), customerStore.getStoreType()+" - "+customerStore.getName());
    else setDefaultDepositary(Owner.CUSTOMER);
    
    initDealTable();
    
    if(!payment.isTmp()) {
      Integer receiptNumber = payment.getReceiptNumber();
      if(receiptNumber != null) {
        printReceipt.setText("Чек № "+receiptNumber);
        printReceipt.setEnabled(false);
      }
      
      startDealAmounts = getDealPayments();
      
      sellerPanel.setPartitionChoosable(false);
      customerPanel.setPartitionChoosable(false);
      
      sellerStoreName.setLinkBorder(false);
      customerStoreName.setLinkBorder(false);
      
      sellerStoreName.setLinkColor(Color.BLACK);
      customerStoreName.setLinkColor(Color.BLACK);
      
      amountField.setEnabled(false);
      amountField.setEditable(false);
      
      /*try {
        documentTable.getTableModel().clear();
        ObjectLoader.executeQuery("SELECT [CreatedDocument(document)], [CreatedDocument(id)], 1, [CreatedDocument(name)], [CreatedDocument(number)], [CreatedDocument(date)] "
                + "FROM [CreatedDocument] WHERE [CreatedDocument(payment)]="+payment.getId()).stream().forEach((d) -> documentTable.getTableModel().addRow(d));
        ObjectLoader.executeQuery("SELECT [CreatedDocument(document)], [CreatedDocument(id)], 1, [CreatedDocument(name)], [CreatedDocument(number)], [CreatedDocument(date)] "
                + "FROM [CreatedDocument] WHERE [CreatedDocument(type)]='CURRENT' AND [CreatedDocument(actionType)]='ОПЛАТА' AND "
                + "id IN (SELECT [CreatedDocument(dealPositions):object] FROM [CreatedDocument(dealPositions):table] WHERE [CreatedDocument(dealPositions):target] IN "
                + "(SELECT [DealPosition(id)] FROM [DealPosition] WHERE [DealPosition(deal)] IN (SELECT [DealPayment(deal)] FROM [DealPayment] WHERE [DealPayment(payment)]="+payment.getId()+")))")
                .stream().forEach((d) -> documentTable.getTableModel().addRow(d));
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }*/
    }//else fillDocumentTable();
  }
  
  @Override
  public String commit() throws Exception {
    Payment payment = (Payment) getEditorObject();
    String msg = "";
    if(payment.isTmp()) {
      BigDecimal balance = getBalance();
      if(balance.compareTo(BigDecimal.ZERO) < 0)
        msg += "\n - Недостаточно средств для распределения платежа";
      
      /*for(Vector d:(Vector<Vector>)documentTable.getTableModel().getDataVector()) {
        if((Integer)((Vector)d).get(2) == 0 && (((Vector)d).get(4) == null || ((Vector)d).get(4).equals("")))
          msg = "\n - Не введён номер документа \""+((Vector)d).get(3)+"\"";
      }*/
      if(!msg.equals(""))
        msg = "Ошибка сохранения платежа:"+msg;
    }
    return msg;
  }
  
  @Override
  public boolean save() {
    Map<Integer, BigDecimal> dealAmounts = getDealPayments();
    for(Integer id:dealAmounts.keySet()) {
      if(dealAmounts.get(id).compareTo(BigDecimal.ZERO) == 0) {
        BigDecimal cost = BigDecimal.ZERO;
        for(Vector d:(Vector<Vector>)dealTable.getTableModel().getDataVector()) {
          if(id.equals(d.get(0)))
            cost = (BigDecimal) d.get(getColumnIndex(ColumnName.цена));
        }
        if(cost.compareTo(BigDecimal.ZERO) > 0)
          dealAmounts.remove(id);
      }
    }
    
    //ArrayList<Integer> documents = new ArrayList<>();
    //ArrayList<String>  numbers = new ArrayList<>();
    //ArrayList<java.sql.Date>    dates = new ArrayList<>();
    
    //final LocalProcessing process = new LocalProcessing();
    //process.submit(() -> {
      //RemoteSession session = null;
      try {
        Integer id = getEditorObject().getId();
        Payment payment = (Payment) getEditorObject();
        //session = ObjectLoader.createSession();
        if(payment.isTmp()) {
          //if(documentTable.getCellEditor() != null)
            //documentTable.getCellEditor().stopCellEditing();
          //documentTable.getTableModel().getDataVector().stream().filter(d -> (Integer)((Vector)d).get(2) == 0).forEach((Object d) -> {
            //documents.add((Integer) ((Vector)d).get(0));
            //numbers.add((String) ((Vector)d).get(4));
            //dates.add(new java.sql.Date(((Date) ((Vector)d).get(5)).getTime()));
          //});
          //map.put("operationDate", documentTable.getRowCount()==0 ? new java.sql.Date(System.currentTimeMillis()) : new java.sql.Date(((Date)documentTable.getTableModel().getValueAt(0, 5)).getTime()));
          ActionUtil.runAction(ActionType.ОПЛАТА, 
                  //ActionUtil.entry("process",     process),
                  ActionUtil.entry("paymentId",     id),
                  ActionUtil.entry("amount",        getAmount()),
                  ActionUtil.entry("sellerStore",   sellerStoreId),
                  ActionUtil.entry("customerStore", customerStoreId),
                  ActionUtil.entry("dealAmounts",   dealAmounts)
                  //ActionUtil.entry("session",     session),
                  //ActionUtil.entry("documents",   documents),
                  //ActionUtil.entry("numbers",     numbers),
                  //ActionUtil.entry("dates",       dates)
          );
          startDealAmounts = dealAmounts;
          getEditorObject().setTmp(false);
        }//else ActionUtil.rePayDeals(id, dealAmounts, session);
        //session.commit();
        commitUpdate();
      } catch (Exception ex) {
        //process.hide();
        //ObjectLoader.rollBackSession(session);
        Messanger.showErrorMessage(ex);
      }
    //});
    return true;
  }

  /*@Override
  public boolean save() {
    Map<Integer, BigDecimal> dealAmounts = getDealPayments();
    for(Integer id:dealAmounts.keySet()) {
      if(dealAmounts.get(id).compareTo(BigDecimal.ZERO) == 0) {
        BigDecimal cost = BigDecimal.ZERO;
        for(Vector d:(Vector<Vector>)dealTable.getTableModel().getDataVector()) {
          if(id.equals(d.get(0)))
            cost = (BigDecimal) d.get(getColumnIndex(ColumnName.цена));
        }
        if(cost.compareTo(BigDecimal.ZERO) > 0)
          dealAmounts.remove(id);
      }
    }
    
    ArrayList<Integer> documents = new ArrayList<>();
    ArrayList<String>  numbers = new ArrayList<>();
    ArrayList<java.sql.Date>    dates = new ArrayList<>();
    
    final LocalProcessing process = new LocalProcessing();
    process.submit(() -> {
      RemoteSession session = null;
      try {
        Integer id = getEditorObject().getId();
        Payment payment = (Payment) getEditorObject();
        session = ObjectLoader.createSession();
        if(payment.isTmp()) {
          if(documentTable.getCellEditor() != null)
            documentTable.getCellEditor().stopCellEditing();
          documentTable.getTableModel().getDataVector().stream().filter(d -> (Integer)((Vector)d).get(2) == 0).forEach((Object d) -> {
            documents.add((Integer) ((Vector)d).get(0));
            numbers.add((String) ((Vector)d).get(4));
            dates.add(new java.sql.Date(((Date) ((Vector)d).get(5)).getTime()));
          });
          Map<String, Object> map = new TreeMap<>();
          map.put("tmp", false);
          map.put("amount", getAmount());
          map.put("sellerStore", sellerStoreId);
          map.put("customerStore", customerStoreId);
          map.put("operationDate", documentTable.getRowCount()==0 ? new java.sql.Date(System.currentTimeMillis()) : new java.sql.Date(((Date)documentTable.getTableModel().getValueAt(0, 5)).getTime()));
          if(!session.saveObject(Payment.class, id, map))
            throw new Exception("Не удалось сохранить платёж!!!");
          ActionUtil.runAction(ActionType.ОПЛАТА, 
                  ActionUtil.entry("process",     process),
                  ActionUtil.entry("paymentId",   id),
                  ActionUtil.entry("dealAmounts", dealAmounts),
                  ActionUtil.entry("session",     session),
                  ActionUtil.entry("documents",   documents),
                  ActionUtil.entry("numbers",     numbers),
                  ActionUtil.entry("dates",       dates));
          startDealAmounts = dealAmounts;
          getEditorObject().setTmp(false);
        }else ActionUtil.rePayDeals(id, dealAmounts, session);
        session.commit();
        commitUpdate();
      } catch (Exception ex) {
        process.hide();
        ObjectLoader.rollBackSession(session);
        Messanger.showErrorMessage(ex);
      }
    });
    return true;
  }*/

  @Override
  public boolean isUpdate() throws RemoteException {
    Map<Integer, BigDecimal> dealAmounts = getDealPayments();
    boolean update = startDealAmounts == null;
    if(!update) {
      if(dealAmounts.size() != startDealAmounts.size())
        update = true;
      else {
        for(Integer deal:dealAmounts.keySet()) {
          if(!startDealAmounts.containsKey(deal) || dealAmounts.get(deal).compareTo(startDealAmounts.get(deal)) != 0) {
            update = true;
            break;
          }
        }
      }
    }
    return update || super.isUpdate();// || !dealAmounts.equals(startDealAmounts);
  }
}