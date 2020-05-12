package division.fx.controller.payment;

import bum.editors.util.ObjectLoader;
import bum.interfaces.CreatedDocument;
import bum.interfaces.Deal;
import bum.interfaces.DealPayment;
import bum.interfaces.DealPosition;
import bum.interfaces.Group.ObjectType;
import bum.interfaces.Payment;
import bum.interfaces.Payment.State;
import bum.interfaces.ProductDocument;
import bum.interfaces.Store.StoreType;
import division.fx.controller.DivisionController;
import division.fx.table.DivisionTableCell;
import division.fx.table.Pojo;
import division.swing.guimessanger.Messanger;
import division.util.Fprinter;
import division.util.Utility;
import division.util.actions.ActionDocumentStarter;
import division.util.actions.DateCell;
import division.util.actions.TextNumberCell;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.rmi.RemoteException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.util.Callback;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class PaymentController extends DivisionController {
  @FXML private ComboBox<String>   typeChooser;
  @FXML private TextField          amountField;
  @FXML private ComboBox<Pojo>     currencyChooser;
  @FXML private Label              sellerLabel;
  @FXML private ComboBox<Pojo>     sellerStoreChooser;
  @FXML private Label              customerLabel;
  @FXML private ComboBox<Pojo>     customerStoreChooser;
  @FXML private CheckBox           allDealsCheck;
  @FXML private TableView<DealRow> dealTable;
  
  @FXML private TableView<DocumentRow>             sellerDocumentTable;
  @FXML private TableColumn<DocumentRow,String>    sellerNumberColumn;
  @FXML private TableColumn<DocumentRow,LocalDate> sellerDateColumn;
  
  @FXML private TableView<DocumentRow>             customerDocumentTable;
  @FXML private TableColumn<DocumentRow,String>    customerNumberColumn;
  @FXML private TableColumn<DocumentRow,LocalDate> customerDateColumn;
  
  private ObservableList<DealRow>  dealModel    = FXCollections.<DealRow>observableArrayList((DealRow param) -> new Observable[]{param.checkProperty()});
  private FilteredList<DealRow>    filteredData = new FilteredList<>(dealModel, p -> true);
  
  @FXML private TextArea           paymentReasonField;
  @FXML private Button             checkButton;
  @FXML private Button             okButton;
  @FXML private Label              neraspredeleno;
  
  private Integer            paymentId;
  private BigDecimal         paymentAmount;
  private TreeMap<Integer, BigDecimal> dealPayments = new TreeMap<>();
  private Integer            sellerPartition;
  private Integer            customerPartition;
  private boolean            tmp;
  
  private String  FRName = null;
  
  private final ActionDocumentStarter actionDocumentStarter = new ActionDocumentStarter();

  public PaymentController() {
    this(null);
  }
  
  public PaymentController(Integer paymentId) {
    this(paymentId, null);
  }

  public PaymentController(Integer paymentId, TreeMap<Integer, BigDecimal> dealPayments) {
    super("fx/fxml/payment.fxml");
    this.paymentId = paymentId;
    if(dealPayments != null)
      this.dealPayments.putAll(dealPayments);
  }
  
  private void initComponents() {
    sellerDateColumn.setCellFactory((TableColumn<DocumentRow, LocalDate> param) -> new DateCell<DocumentRow>("date") {
      @Override
      public boolean isCustomEditable(DocumentRow row) {
        return row.isDateEditable();
      }
    });
    customerDateColumn.setCellFactory((TableColumn<DocumentRow, LocalDate> param) -> new DateCell<DocumentRow>("date") {
      @Override
      public boolean isCustomEditable(DocumentRow row) {
        return row.isDateEditable();
      }
    });
    sellerNumberColumn.setCellFactory((TableColumn<DocumentRow, String> param) -> new TextNumberCell<DocumentRow>("number") {
      @Override
      public boolean isCustomEditable(DocumentRow row) {
        return row.isNumberEditable();
      }
    });
    customerNumberColumn.setCellFactory((TableColumn<DocumentRow, String> param) -> new TextNumberCell<DocumentRow>("number") {
      @Override
      public boolean isCustomEditable(DocumentRow row) {
        return row.isNumberEditable();
      }
    });
    typeChooser.setItems(FXCollections.observableArrayList("Оформить поручение", "Ввести из выписки", "Наличный расчёт"));
    dealTable.setEditable(true);
  }
  
  private void initEvents() {
    amountField.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
      try {
        if(!newValue.matches("[\\d\\.]*"))
          throw new Exception();
        
        if(newValue.equals("") || (
                BigDecimal.valueOf(Double.valueOf(newValue)).compareTo(BigDecimal.ZERO) > 0 &&
                BigDecimal.valueOf(Double.valueOf(newValue)).remainder(BigDecimal.ONE).multiply(new BigDecimal(100)).remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0)) {
          amountField.setText(newValue);
          
          BigDecimal balance = getAmount();
          for(DealRow dr:dealTable.getItems()) {
            if(dr.getCheck() && !dr.isDisable()) {
              dr.setPaymentPay(BigDecimal.ZERO);
              BigDecimal pay = balance.compareTo(dr.getDebt()) >= 0 ? dr.getDebt() : balance;
              dr.setPaymentPay(pay);
              balance = balance.subtract(pay);
            }
          }
          
          neraspredeleno();
        }else throw new Exception();
      }catch(Exception ex) {
        amountField.setText(oldValue);
      }
    });
    sellerStoreChooser.valueProperty().addListener((ObservableValue<? extends Pojo> observable, Pojo oldValue, Pojo newValue) -> initDocumentTables());
    customerStoreChooser.valueProperty().addListener((ObservableValue<? extends Pojo> observable, Pojo oldValue, Pojo newValue) -> initDocumentTables());
    typeChooser.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
      initCurrencyChooser();
      checkButton.setVisible(newValue.equals("Наличный расчёт"));
    });
    currencyChooser.valueProperty().addListener((ObservableValue<? extends Pojo> observable, Pojo oldValue, Pojo newValue) -> initStoreChooser());
    allDealsCheck.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> filterDealTable());
    
    dealTable.getColumns().stream().forEach(column -> {
      if(column.getText().equals("Зачёт")) {
        ((TableColumn<DealRow, Boolean>)column).setCellFactory(new Callback<TableColumn<DealRow,Boolean>,TableCell<DealRow,Boolean>>() {
          @Override
          public TableCell<DealRow, Boolean> call(TableColumn<DealRow, Boolean> param) {
            return new CheckBoxTableCell((Callback<Integer, ObservableValue<Boolean>>) (Integer index) -> dealTable.getItems().get(index).checkProperty()) {
              @Override
              public void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if(!empty) {
                  DealRow dr = (DealRow)getTableRow().getItem();
                  if(dr != null) {
                    getTableRow().getStyleClass().remove("checked-row");
                    if(dr.getCheck())
                      getTableRow().getStyleClass().add("checked-row");
                    
                    getTableRow().getStyleClass().remove("debt-row");
                    if(dr.getDebt().compareTo(BigDecimal.ZERO) > 0 && dr.getCost().subtract(dr.getDebt()).compareTo(BigDecimal.ZERO) > 0)
                      getTableRow().getStyleClass().add("debt-row");
                  }
                }
              }

              @Override
              protected void updateBounds() {
                super.updateBounds();
                DealRow dr = (DealRow)getTableRow().getItem();
                if(dr != null) {
                  boolean editable = dr.isEditable() && (getBalance().compareTo(BigDecimal.ZERO) > 0 || dr.getCheck() || dr.getCost().compareTo(BigDecimal.ZERO) == 0);
                  if(isEditable() != editable) {
                    setEditable(editable);
                    getTableRow().getStyleClass().remove("disable");
                    if(!isEditable())
                      getTableRow().getStyleClass().add("disable");
                    //getTableRow().setOpacity(isEditable() ? 1.0 : 0.5);
                  }
                }
              }
            };
          }
        });
      }else {
        ((TableColumn<DealRow,Object>)column).setCellFactory((TableColumn<DealRow, Object> param) -> new DivisionTableCell());
      }
    });
  }

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    initComponents();
    initEvents();
    for(List d:ObjectLoader.getData("SELECT "
            /*0*/+ "[Payment(amount)], "
            
            /*1*/ + "[Payment(sellerCompanyPartition)], "
            /*2*/ + "[Payment(seller)], "
            /*3*/ + "[Payment(sellerStore)], "
            /*4*/ + "[Payment(seller-store-name)], "
            /*5*/ + "[Payment(seller-store-type)], "
            /*6*/ + "[Payment(seller-store-object-type)], "
            /*7*/ + "(SELECT [Store(currency)] FROM [Store] WHERE [Store(id)]=[Payment(sellerStore)]), "
            /*8*/ + "(SELECT [Store(currency-name)] FROM [Store] WHERE [Store(id)]=[Payment(sellerStore)]), "
            
            /*9*/ + "[Payment(customerCompanyPartition)], "
            /*10*/+ "[Payment(customer)], "
            /*11*/+ "[Payment(customerStore)], "
            /*12*/+ "[Payment(customer-store-name)], "
            /*13*/+ "[Payment(customer-store-type)], "
            /*14*/+ "[Payment(customer-store-object-type)], "
            /*15*/+ "(SELECT [Store(currency)] FROM [Store] WHERE [Store(id)]=[Payment(sellerStore)]), "
            /*16*/+ "(SELECT [Store(currency-name)] FROM [Store] WHERE [Store(id)]=[Payment(sellerStore)]), "
            
            /*17*/+ "[Payment(tmp)], "
            /*18*/+ "[Payment(state)], "
            /*19*/+ "[Payment(receiptNumber)] "
            
            + "FROM [Payment] WHERE id="+paymentId)) {
      
      paymentAmount                        = (BigDecimal) d.get(0);
      sellerPartition                      = (Integer)    d.get(1);
      String     sellerName                = (String)     d.get(2);
      Integer    sellerStore               = (Integer)    d.get(3);
      String     sellerStoreName           = (String)     d.get(4);
      StoreType  sellerStoreType           = d.get(5)==null?null:StoreType.valueOf((String)  d.get(5));
      ObjectType sellerStoreObjectType     = d.get(6)==null?null:ObjectType.valueOf((String) d.get(6));
      Integer    sellerStoreCurrency       = (Integer)    d.get(7);
      String     sellerStoreCurrencyName   = (String)     d.get(8);

      customerPartition                    = (Integer)    d.get(9);
      String     customerName              = (String)     d.get(10);
      Integer    customerStore             = (Integer)    d.get(11);
      String     customerStoreName         = (String)     d.get(12);
      StoreType  customerStoreType         = d.get(13)==null?null:StoreType.valueOf((String)  d.get(13));
      ObjectType customerStoreObjectType   = d.get(14)==null?null:ObjectType.valueOf((String) d.get(14));
      Integer    customerStoreCurrency     = (Integer)    d.get(15);
      String     customerStoreCurrencyName = (String)     d.get(16);

      tmp                                  = (boolean)    d.get(17);
      State     state                      = State.valueOf((String) d.get(18));
      
      Integer receiptNumber                = (Integer)    d.get(19);

      amountField.setText(paymentAmount.toString());
      sellerLabel.setText(sellerName);
      customerLabel.setText(customerName);
      
      if(receiptNumber != null)
        checkButton.setText("Чек № "+receiptNumber);
      checkButton.setDisable(receiptNumber != null);

      if(tmp) { // Новый платёж
        if(state == State.ПОДГОТОВКА)
          typeChooser.setValue("Оформить поручение");
        else typeChooser.setValue("Ввести из выписки");
      }else {
        if(state == State.ПОДГОТОВКА || state == State.ОТПРАВЛЕНО) {
          typeChooser.setValue("Оформить поручение");
        }else {
          if(sellerStoreType == StoreType.НАЛИЧНЫЙ)
            typeChooser.setValue("Наличный расчёт");
          else
            typeChooser.setValue("Ввести из выписки");
        }
        currencyChooser.setValue(new Pojo(sellerStoreCurrency, sellerStoreCurrencyName));
        sellerStoreChooser.setValue(new Pojo(sellerStore, sellerStoreName));
        customerStoreChooser.setValue(new Pojo(customerStore, customerStoreName));
      }

      typeChooser.setDisable(!tmp);
      currencyChooser.setDisable(!tmp);
      sellerStoreChooser.setDisable(!tmp);
      customerStoreChooser.setDisable(!tmp);
    }
    initDealPayments();
    initDealTable();
    filterDealTable();
    initDocumentTables();
    neraspredeleno();
    setFrName();
  }
  
  private void initDocumentTables() {
    sellerDocumentTable.getItems().clear();
    customerDocumentTable.getItems().clear();
    if(sellerStoreChooser.getValue() != null && customerStoreChooser.getValue() != null) {
      
      Map<Integer,List<Map<String,Object>>> paymentDocuments = new TreeMap<>();
      
      ObjectLoader.getData(DBFilter.create(CreatedDocument.class).AND_EQUAL("payment", paymentId), "id","document","number","date").stream().forEach(d -> {
        if(!paymentDocuments.containsKey((Integer)d.get(1)))
          paymentDocuments.put((Integer)d.get(1), new ArrayList<>());
        Map<String,Object> doc = new TreeMap<>();
        doc.put("id",         d.get(0));
        doc.put("document",   d.get(1));
        doc.put("number",     d.get(2));
        doc.put("customDate", Utility.convert((Timestamp)d.get(3)));
        paymentDocuments.get((Integer)d.get(1)).add(doc);
      });
      
      Integer[] deals = new Integer[0];
      for(DealRow dr:dealTable.getItems())
        if(!dr.isDisable() && dr.getCheck())
          deals = (Integer[]) ArrayUtils.add(deals, dr.getId());
      Integer[] dealPositions = new Integer[0];
      for(List d:ObjectLoader.getData(DBFilter.create(DealPosition.class).AND_IN("deal", deals), "id"))
        dealPositions = (Integer[]) ArrayUtils.add(dealPositions, d.get(0));
      
      actionDocumentStarter.init(ProductDocument.ActionType.ОПЛАТА, dealPositions, sellerStoreChooser.getValue().getId(), customerStoreChooser.getValue().getId());
      
      actionDocumentStarter.getSellerSystemDocuments().values().stream().forEach(doc -> {
        if(paymentDocuments.containsKey((Integer)doc.get("id"))) {
          paymentDocuments.get((Integer)doc.get("id")).stream().forEach(d -> {
            DocumentRow row = new DocumentRow((Integer)d.get("id"), (String)doc.get("name"), (String)d.get("number"), (LocalDate)d.get("customDate"), false, false);
            sellerDocumentTable.getItems().add(row);
          });
        }else {
          DocumentRow row = new DocumentRow((Integer)doc.get("id"), (String)doc.get("name"), (boolean)doc.get("auto") ? "auto..." : "", (LocalDate)doc.get("customDate"), !(boolean)doc.get("auto"), !(boolean)doc.get("auto"));
          row.numberProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> doc.put("number", newValue));
          row.dateProperty().addListener((ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) -> doc.put("customDate", newValue));
          sellerDocumentTable.getItems().add(row);
        }
      });
      
      actionDocumentStarter.getUserDocuments().values().stream().forEach(doc -> {
        if(paymentDocuments.containsKey((Integer)doc.get("id"))) {
          paymentDocuments.get((Integer)doc.get("id")).stream().forEach(d -> {
            DocumentRow row = new DocumentRow((Integer)d.get("id"), (String)doc.get("name"), (String)d.get("number"), (LocalDate)d.get("customDate"), false, false);
            sellerDocumentTable.getItems().add(row);
          });
        }else {
          DocumentRow row = new DocumentRow((Integer)doc.get("id"), (String)doc.get("name"), (boolean)doc.get("auto") ? "auto..." : "", (LocalDate)doc.get("customDate"), !(boolean)doc.get("auto"), !(boolean)doc.get("auto"));
          row.numberProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> doc.put("number", newValue));
          row.dateProperty().addListener((ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) -> doc.put("customDate", newValue));
          sellerDocumentTable.getItems().add(row);
        }
      });
      
      actionDocumentStarter.getCustomerSystemDocuments().values().stream().forEach(doc -> {
        if(paymentDocuments.containsKey((Integer)doc.get("id"))) {
          paymentDocuments.get((Integer)doc.get("id")).stream().forEach(d -> {
            DocumentRow row = new DocumentRow((Integer)d.get("id"), (String)doc.get("name"), (String)d.get("number"), (LocalDate)d.get("customDate"), false, false);
            customerDocumentTable.getItems().add(row);
          });
        }else {
          DocumentRow row = new DocumentRow((Integer)doc.get("id"), (String)doc.get("name"), (boolean)doc.get("auto") ? "auto..." : "", (LocalDate)doc.get("customDate"), !(boolean)doc.get("auto"), !(boolean)doc.get("auto"));
          row.numberProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> doc.put("number", newValue));
          row.dateProperty().addListener((ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) -> doc.put("customDate", newValue));
          customerDocumentTable.getItems().add(row);
        }
      });
    }
  }
  
  private void initDealPayments() {
    if(dealPayments.isEmpty())
      ObjectLoader.getData(DBFilter.create(DealPayment.class).AND_EQUAL("payment", paymentId), "deal","amount")
              .stream().forEach(dp -> dealPayments.put((Integer)dp.get(0), (BigDecimal)dp.get(1)));
  }
  
  private void initDealTable() {
    ObjectLoader.getData("SELECT "
            /*0*/ + "[Deal(id)],"
            /*1*/ + "(SELECT [Contract(templatename)] FROM [Contract] WHERE id=[Deal(contract)]),"
            /*2*/ + "[Deal(contract_number)],"
            /*3*/ + "[Deal(service_name)],"
            /*4*/ + "[Deal(dealStartDate)],"
            /*5*/ + "[Deal(dealEndDate)],"
            /*6*/ + "[Deal(cost)], "
            /*7*/ + "[Deal(paymentAmount)]-getPaymentAmountFromPayment([Deal(id)],"+paymentId+"), "
            /*8*/ + "getPaymentAmountFromPayment([Deal(id)],"+paymentId+"),"
            /*9*/ + "[Deal(paymentCount)] "
            + "FROM [Deal] "
            + "WHERE [Deal(sellerCompanyPartition)]=? AND [Deal(customerCompanyPartition)]=? "
            + "ORDER BY [Deal(dealStartDate)] DESC", 
            new Object[]{sellerPartition, customerPartition}).stream().forEach(d -> {
              Integer    id             = (Integer)    d.get(0);
              String     contractName   = (String)     d.get(1);
              String     contractNumber = (String)     d.get(2);
              String     processName    = (String)     d.get(3);
              LocalDate  startDate      = Utility.convert((Date) d.get(4));
              LocalDate  endDate        = Utility.convert((Date) d.get(5));
              BigDecimal cost           = (BigDecimal) d.get(6);
              BigDecimal pay            = (BigDecimal) d.get(7);
              BigDecimal paymentPay     = (BigDecimal) d.get(8);
              Integer    payCount       = (Integer)    d.get(9);
              
              DealRow row = new DealRow(id, contractName, contractNumber, processName, startDate, endDate, cost, pay,
                      dealPayments.containsKey(id) ? dealPayments.get(id) : paymentPay,
                      payCount
              );
              
              row.checkProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                if(newValue) {
                  BigDecimal p = row.getDebt();
                  BigDecimal balance = getBalance();
                  row.setPaymentPay(balance.compareTo(p) > 0 ? p : balance);
                }else row.setPaymentPay(BigDecimal.ZERO);
                initDocumentTables();
                neraspredeleno();
              });

              dealModel.add(row);
            });
    
    SortedList<DealRow> sortedData = new SortedList<>(filteredData);
		sortedData.comparatorProperty().bind(dealTable.comparatorProperty());
		dealTable.setItems(sortedData);
  }
  
  private void initCurrencyChooser() {
    StoreType storeType = typeChooser.getValue().equals("Наличный расчёт") ? StoreType.НАЛИЧНЫЙ : StoreType.БЕЗНАЛИЧНЫЙ;
    currencyChooser.setValue(null);
    currencyChooser.getItems().clear();
    ObjectLoader.getData("SELECT DISTINCT [Store(currency)], [Store(currency-name)] FROM [Store] WHERE [Store(currency)] NOTNULL AND "
            + "[Store(storeType)]='"+storeType+"' AND [Store(objectType)]='"+ObjectType.ВАЛЮТА+"' AND [Store(companyPartition)] IN "
            + "(SELECT unnest(ARRAY[[Payment(sellerCompanyPartition)], [Payment(customerCompanyPartition)]]) FROM [Payment] WHERE [Payment(id)]="+paymentId+")").stream().forEach(d -> {
              currencyChooser.getItems().add(new Pojo((Integer)d.get(0), (String)d.get(1)));
            });
    if(!currencyChooser.getItems().isEmpty())
      currencyChooser.setValue(currencyChooser.getItems().get(0));
  }
  
  private void initStoreChooser() {
    StoreType storeType = typeChooser.getValue().equals("Наличный расчёт") ? StoreType.НАЛИЧНЫЙ : StoreType.БЕЗНАЛИЧНЫЙ;
    Pojo currency = currencyChooser.getValue();
    
    sellerStoreChooser.setValue(null);
    sellerStoreChooser.getItems().clear();
    customerStoreChooser.setValue(null);
    customerStoreChooser.getItems().clear();
    
    if(currency != null) {
      ObjectLoader.getData("SELECT [Store(id)],[Store(name)] FROM [Store] "
              + "WHERE [Store(storeType)]='"+storeType+"' AND [Store(objectType)]='"+ObjectType.ВАЛЮТА+"' AND [Store(currency)]="+currency.getId()+" AND "
              + "[Store(companyPartition)]=(SELECT [Payment(sellerCompanyPartition)] FROM [Payment] WHERE id="+paymentId+")").stream().forEach(d -> {
                sellerStoreChooser.getItems().add(new Pojo((Integer)d.get(0), (String)d.get(1)));
              });
      ObjectLoader.getData("SELECT [Store(id)],[Store(name)] FROM [Store] "
              + "WHERE [Store(storeType)]='"+storeType+"' AND [Store(objectType)]='"+ObjectType.ВАЛЮТА+"' AND [Store(currency)]="+currency.getId()+" AND "
              + "[Store(companyPartition)]=(SELECT [Payment(customerCompanyPartition)] FROM [Payment] WHERE id="+paymentId+")").stream().forEach(d -> {
                customerStoreChooser.getItems().add(new Pojo((Integer)d.get(0), (String)d.get(1)));
              });
    }
    
    if(!sellerStoreChooser.getItems().isEmpty())
        sellerStoreChooser.setValue(sellerStoreChooser.getItems().get(0));
    
    if(!customerStoreChooser.getItems().isEmpty())
      customerStoreChooser.setValue(customerStoreChooser.getItems().get(0));
  }

  @FXML private void typeChooserAction(ActionEvent event) {}
  @FXML private void currencyChooserAction(ActionEvent event) {}
  @FXML private void sellerStoreChooserAction(ActionEvent event) {}
  @FXML private void customerStoreChooserAction(ActionEvent event) {}
  @FXML private void allDealsCheckAction(ActionEvent event) {}
  
  @FXML private void checkButtonAction(ActionEvent event) {
    try {
      if(FRName != null) {
        save(false);
        BigDecimal amount = getAmount();
        if(amount.compareTo(BigDecimal.ZERO) > 0 && paymentId != null) {
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

            checkButton.setText("Чек № "+receiptNumber);
            setEnabled(false);
          }
        }
      }
    }catch(Exception t) {
      Messanger.showErrorMessage(t);
    }
  }
  
  private void setFrName() {
    division.xml.Document document = division.xml.Document.load("conf"+File.separator+"machinery_configuration.xml");
    if(document != null) {
      division.xml.Node frNode = document.getRootNode().getNode("FR_"+sellerPartition);
      if(frNode != null)
        FRName = frNode.getAttribute("FiscalPrinterName");
    }
    checkButton.setDisable(FRName == null);
  }
  
  private void save(boolean close) {
    if(isUpdate()) {
      RemoteSession session = null;
      try {
        TreeMap<Integer,BigDecimal> dealAmounts = new TreeMap<>();
        dealTable.getItems().filtered(dr -> !dr.isDisable() && dr.getCheck() 
                && !(dr.getCost().compareTo(BigDecimal.ZERO)>0 && dr.getPaymentPay().compareTo(BigDecimal.ZERO)==0))
                .forEach(dr -> dealAmounts.put(dr.getId(), dr.getPaymentPay()));
        session = ObjectLoader.createSession(false);
        session.executeUpdate(
                Payment.class, 
                new String[]{"tmp", "amount", "sellerStore", "customerStore"}, 
                new Object[]{false,  getAmount(), sellerStoreChooser.getValue().getId(),customerStoreChooser.getValue().getId()}, 
                new Integer[]{paymentId});
        
        List<String> querys = new ArrayList<>();
        querys.add("DELETE FROM [DealPayment] WHERE [DealPayment(payment)]="+paymentId);
        dealAmounts.keySet().stream().forEach(dealId -> querys.add("INSERT INTO [DealPayment]([DealPayment(deal)], [DealPayment(payment)], [DealPayment(amount)]) VALUES("+dealId +","+paymentId+","+dealAmounts.get(dealId)+")"));
        session.executeUpdate(querys.toArray(new String[0]));
        
        moveMoneyStorePosition(session);
        if(tmp) {
          Map<Integer,List> ids = actionDocumentStarter.start(session, paymentId, new TreeMap());
          List<Integer> docs = new ArrayList<>();
          for(List ds:ids.values())
            docs.addAll(ds);
          String operationDate = "CURRENT_DATE";
          Object[] params = new Object[]{paymentId};
          if(!ids.isEmpty()) {
            operationDate = "(SELECT MAX(date) FROM [CreatedDocument] WHERE [CreatedDocument(id)]=ANY(?))";
            params = ArrayUtils.add(params, 0, docs.toArray(new Integer[0]));
          }
          /*List<Integer> ids = actionDocumentStarter.start(session, paymentId, new TreeMap());
          String operationDate = "CURRENT_DATE";
          Object[] params = new Object[]{paymentId};
          if(!ids.isEmpty()) {
            operationDate = "(SELECT MAX(date) FROM [CreatedDocument] WHERE [CreatedDocument(id)]=ANY(?))";
            params = ArrayUtils.add(params, 0, ids.toArray(new Integer[0]));
          }*/
          session.executeUpdate("UPDATE [Payment] SET [Payment(operationDate)]="+operationDate+" WHERE id=?", params);
        }
        
        session.addEvent(Payment.class, "UPDATE", paymentId);
        
        HashSet<Integer> ids = new HashSet<>(dealAmounts.keySet());
        ids.addAll(dealPayments.keySet());
        session.addEvent(Deal.class, "UPDATE", ids.toArray(new Integer[0]));
        
        ObjectLoader.commitSession(session);
        
        this.tmp = false;
        
        if(close)
          dialog.setVisible(false);
      }catch(Exception ex) {
        ObjectLoader.rollBackSession(session);
        new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK).show();
      }
    }else if(close) dialog.setVisible(false);
  }

  @FXML
  private void okButtonAction(ActionEvent event) {
    save(true);
  }
  
  private void filterDealTable() {
    filteredData.setPredicate(dr -> allDealsCheck.isSelected() ? true : !dr.isDisable());
  }
  
  private BigDecimal getAmount() {
    return amountField.getText().equals("") ? BigDecimal.ZERO : BigDecimal.valueOf(Double.valueOf(amountField.getText())).setScale(2, RoundingMode.HALF_UP);
  }
  
  private BigDecimal getBalance() {
    return getAmount().subtract(getSumm()).setScale(2, RoundingMode.HALF_UP);
  }
  
  private void neraspredeleno() {
    BigDecimal balabce = getBalance();
    neraspredeleno.setVisible(balabce.compareTo(BigDecimal.ZERO) != 0);
    neraspredeleno.setText("Нераспределённые средства: "+balabce);
  }
  
  private BigDecimal getSumm() {
    BigDecimal sum = BigDecimal.ZERO;
    for(DealRow dr:dealTable.getItems())
      if(!dr.isDisable() && dr.getCheck()) {
        sum = sum.add(dr.getPaymentPay());
      }
    return sum.setScale(2, RoundingMode.HALF_UP);
  }
  
  private boolean isUpdate() {
    if(tmp)
      return true;
    else {
      TreeMap<Integer,BigDecimal> result = new TreeMap<>();
      dealTable.getItems().filtered(dr -> !dr.isDisable() && dr.getCheck()).forEach(dr -> result.put(dr.getId(), dr.getPaymentPay()));
      return !dealPayments.equals(result) || !paymentAmount.equals(getAmount());
    }
  }
  
  private void moveMoneyStorePosition(RemoteSession session) throws RemoteException {
    for(List d:session.executeQuery("SELECT "
      /*0*/+ "[Payment(amount)], "
      /*1*/+ "[Payment(customer-store-controll-out)], "
      /*2*/+ "[Payment(sellerCompanyPartition)],"
      /*3*/+ "[Payment(sellerStore)], "
      /*4*/+ "[Payment(customerStore)], "
      /*5*/+ "[Payment(customerCompanyPartition)] "
      + "FROM [Payment] WHERE [Payment(id)] = "+paymentId)) {
      
      BigDecimal amount              = (BigDecimal) d.get(0);
      boolean    storeControllOut    = (boolean) d.get(1);
      Integer    seller              = (Integer) d.get(2);
      Integer    sellerStore         = (Integer) d.get(3);
      Integer    customerStore       = (Integer) d.get(4);
      Integer    customer            = (Integer) d.get(5);
      Integer    sellerMoneyId       = (Integer) session.executeQuery("SELECT getFreeMoneyId("+sellerStore+")").get(0).get(0);
      Integer    customerMoneyId     = (Integer) session.executeQuery("SELECT getFreeMoneyId("+customerStore+")").get(0).get(0);
      BigDecimal customerMoneyAmount = (BigDecimal) session.executeQuery("SELECT [Equipment(amount)] FROM [Equipment] WHERE id="+customerMoneyId).get(0).get(0);

      if(customerMoneyAmount.compareTo(amount) >= 0 || !storeControllOut) {
        session.executeUpdate("UPDATE [Equipment] SET [Equipment(amount)]=[Equipment(amount)]-? WHERE [Equipment(id)]=?", new Object[]{amount, customerMoneyId});
        session.executeUpdate("UPDATE [Equipment] SET [Equipment(amount)]=[Equipment(amount)]+? WHERE [Equipment(id)]=?", new Object[]{amount, sellerMoneyId});
      }else throw new RemoteException("Нехватка средств");
    }
  }
}