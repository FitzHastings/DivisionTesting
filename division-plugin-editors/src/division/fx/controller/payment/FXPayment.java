package division.fx.controller.payment;

import bum.editors.util.ObjectLoader;
import bum.interfaces.CompanyPartition;
import bum.interfaces.CreatedDocument;
import bum.interfaces.Deal;
import bum.interfaces.DealPayment;
import bum.interfaces.DealPosition;
import bum.interfaces.Group;
import bum.interfaces.Payment;
import bum.interfaces.ProductDocument;
import bum.interfaces.Store;
import division.fx.DivisionTextField;
import division.fx.PropertyMap;
import division.fx.controller.DivisionController;
import division.fx.table.Column;
import division.fx.table.FXDivisionTableCell;
import division.fx.table.FXTable;
import division.fx.util.MsgTrash;
import division.util.DocumentCreator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.TreeMap;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextArea;
import javafx.util.converter.BigDecimalStringConverter;
import org.apache.commons.lang3.ArrayUtils;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class FXPayment extends DivisionController {
  @FXML private ComboBox<PropertyMap>   typeChooser;
  @FXML private DivisionTextField       amountField;
  @FXML private ComboBox<PropertyMap>   currencyChooser;
  @FXML private Label                   sellerLabel;
  @FXML private ComboBox<PropertyMap>   sellerStoreChooser;
  @FXML private Label                   customerLabel;
  @FXML private ComboBox<PropertyMap>   customerStoreChooser;
  @FXML private FXTable<PropertyMap>    dealTable;
  
  @FXML private FXTable<PropertyMap>    sellerDocumentTable;
  @FXML private FXTable<PropertyMap>    customerDocumentTable;
  
  @FXML private TextArea                paymentReasonField;
  @FXML private Button                  okButton;
  @FXML private Label                   neraspredeleno;
  
  private final DocumentCreator documentCreator = new DocumentCreator();
  private ObjectProperty<PropertyMap> paymentProperty = new SimpleObjectProperty<>();
  
  private PropertyMap memnto = PropertyMap.create();

  public FXPayment(PropertyMap payment) {
    super("fx/division/fx/controller/payment/FXPayment_1.fxml", "fx/css/FXPayment.css");
    paymentProperty.setValue(payment);
  }
  
  public void initData() {
    amountField.setValue(paymentProperty.getValue().getBigDecimal("amount").setScale(2, RoundingMode.HALF_UP));
    
    if(!paymentProperty.getValue().isNull("id")) {
      paymentProperty.getValue().setValue("dealPayments", ObjectLoader.getList(DBFilter.create(DealPayment.class).AND_EQUAL("payment", paymentProperty.getValue().getInteger("id")), "deal","amount"));
      
      PropertyMap sellerStore = ObjectLoader.getMap(Store.class, paymentProperty.getValue().getInteger("sellerStore"), "storeType","currency");
      typeChooser.getItems().stream()
              .filter(t ->  t.getList("state").contains(paymentProperty.getValue().getValue("state", Payment.State.class)) && 
                      t.getValue("storeType", Store.StoreType.class) == sellerStore.getValue("storeType", Store.StoreType.class))
              .forEach(t -> {
                typeChooser.setValue(t);
              });
      
      currencyChooser.getItems().stream().filter(c -> c.getInteger("id") == sellerStore.getInteger("currency")).forEach(c -> currencyChooser.setValue(c));
      
      typeChooser.getItems().stream()
              .filter(t ->  t.getList("state").contains(paymentProperty.getValue().getValue("state", Payment.State.class)) && 
                      t.getValue("storeType", Store.StoreType.class) == sellerStore.getValue("storeType", Store.StoreType.class))
              .forEach(t -> {
                typeChooser.setValue(t);
              });
      
      sellerStoreChooser.getItems().stream().filter(s -> Objects.equals(s.getInteger("id"), paymentProperty.getValue().getInteger("sellerStore"))).forEach(s -> sellerStoreChooser.setValue(s));
      customerStoreChooser.getItems().stream().filter(s -> Objects.equals(s.getInteger("id"), paymentProperty.getValue().getInteger("customerStore"))).forEach(s -> customerStoreChooser.setValue(s));
    }else {
      typeChooser.getItems().stream()
            .filter(t ->  t.getList("state").contains(paymentProperty.getValue().getValue("state", Payment.State.class)))
            .forEach(t -> {
              if(typeChooser.getValue() == null)
                typeChooser.setValue(t);
            });
    }
    
    dealTable.getItems().stream().forEach(deal -> {
      paymentProperty.getValue().getList("dealPayments").stream().filter(dp -> Objects.equals(dp.getInteger("deal"), deal.getInteger("id"))).forEach(dp -> {
        deal.setValue("select", true);
        deal.setValue("thispay", dp.getBigDecimal("amount"));
      });
    });
    
    PropertyMap.addListCangeListener(dealTable.getItems(), "select", (ChangeListener<Boolean>) (ObservableValue<? extends Boolean> observable1, Boolean oldValue1, Boolean newValue1) -> {
      paymentProperty.getValue().getList("dealPayments").clear();
      /*for(PropertyMap d:dealTable.getItems())
        if(d.is("select") && !d.isNull("thispay") && d.getBigDecimal("thispay").compareTo(BigDecimal.ZERO) > 0)
          paymentProperty.getValue().getList("dealPayments").add(PropertyMap.create().setValue("deal", d.getInteger("id")).setValue("amount", d.getBigDecimal("thispay")));*/
      
      dealTable.getItems().filtered(d -> d.is("select") && !d.isNull("thispay") && d.getBigDecimal("thispay").compareTo(BigDecimal.ZERO) > 0)
              .forEach(d -> paymentProperty.getValue().getList("dealPayments").add(PropertyMap.create().setValue("deal", d.getInteger("id")).setValue("amount", d.getBigDecimal("thispay"))));

      initDocuments();
    });
    
    typeChooser.valueProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      if(newValue != null && paymentProperty.getValue().isNull("id"))
        paymentProperty.getValue().setValue("state", newValue.getList("state", Payment.State.class).get(0));
    });
    
    customerStoreChooser.valueProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      if(newValue != null)
        paymentProperty.getValue().setValue("customerCompanyPartition", customerStoreChooser.getValue().getInteger("companyPartition"));
    });
    
    paymentProperty.getValue().get("sellerStore").bind(sellerStoreChooser.valueProperty());
    paymentProperty.getValue().get("customerStore").bind(customerStoreChooser.valueProperty());
    paymentProperty.getValue().get("amount").bind(amountField.valueProperty());
    
    typeChooser.disableProperty().bind(Bindings.createBooleanBinding(() -> !paymentProperty.getValue().isNull("id"), paymentProperty.getValue().get("id")));
    currencyChooser.disableProperty().bind(typeChooser.disableProperty());
    sellerStoreChooser.disableProperty().bind(typeChooser.disableProperty());
    customerStoreChooser.disableProperty().bind(typeChooser.disableProperty());
    
    paymentProperty.getValue().get("amount").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
      dealTable.getItems().stream().filter(d -> d.is("select")).forEach(d -> {
        d.setValue("select", false).setValue("select", true);
      });
    });
    
    initDocuments();
    neraspRefresh();
    
    //memnto.copyFrom(paymentProperty.getValue());
    memnto = paymentProperty.getValue().copy();
    okButton.setOnAction(e -> save());
  }
  
  public void initDocuments() {
    ObservableList<PropertyMap> sellerNewDocs   = FXCollections.observableArrayList(sellerDocumentTable  .getItems().filtered(d -> !d.isNull("autonumber")));
    ObservableList<PropertyMap> customerNewDocs = FXCollections.observableArrayList(customerDocumentTable.getItems().filtered(d -> !d.isNull("autonumber")));
    
    sellerDocumentTable.getItems().clear();
    customerDocumentTable.getItems().clear();
    
    if(sellerStoreChooser.getValue() != null && customerStoreChooser.getValue() != null) {
      documentCreator.init(
              ProductDocument.ActionType.ОПЛАТА, 
              PropertyMap.getListFromList(
                      ObjectLoader.getList(DBFilter.create(DealPosition.class).AND_IN("deal", PropertyMap.getListFromList(
                              dealTable.getItems().filtered(d -> d.is("select")), "id", Integer.TYPE).toArray(new Integer[0])), "id"), "id", Integer.TYPE).toArray(new Integer[0]), 
              sellerStoreChooser.getValue().getInteger("id"), 
              customerStoreChooser.getValue().getInteger("id"));

      ObservableList<PropertyMap> documents = paymentProperty.getValue().isNull("id") ? 
              FXCollections.observableArrayList() : 
              ObjectLoader.getList(DBFilter.create(CreatedDocument.class).AND_EQUAL("payment", paymentProperty.getValue().getInteger("id")), "id","name:=:document_name","document","number","date");

      documentCreator.getSellerSystemDocuments().values().stream().forEach(doc -> {
        if(PropertyMap.contains(documents, "document", doc.getInteger("id")))
          PropertyMap.get(documents, "document", doc.getInteger("id")).stream().forEach(d -> sellerDocumentTable.getItems().add(d));
        else {
          if(PropertyMap.contains(sellerNewDocs, "id", doc.getInteger("id")))
            doc.copyFrom(PropertyMap.get(sellerNewDocs, "id", doc.getInteger("id")).get(0));
          else doc.setValue("number", doc.is("autonumber") ? "auto..." : "введите номер...");
          sellerDocumentTable.getItems().add(doc);
        }
      });

      documentCreator.getCustomerSystemDocuments().values().stream().forEach(doc -> {
        if(PropertyMap.contains(documents, "document", doc.getInteger("id")))
          PropertyMap.get(documents, "document", doc.getInteger("id")).stream().forEach(d -> customerDocumentTable.getItems().add(d));
        else {
          if(PropertyMap.contains(customerNewDocs, "id", doc.getInteger("id")))
            doc.copyFrom(PropertyMap.get(customerNewDocs, "id", doc.getInteger("id")).get(0));
          else doc.setValue("number", doc.is("autonumber") ? "auto..." : "введите номер...");
          customerDocumentTable.getItems().add(doc);
        }
      });
    }
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    amountField.setConverter(new BigDecimalStringConverter());
    
    paymentProperty.getValue().get("amount").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
      neraspRefresh();
      
      dealTable.getColumn("Зачёт").setVisible(false);
      dealTable.getColumn("Зачёт").setVisible(true);
      //dealTable.refresh();
    });
    
    sellerLabel.setText(ObjectLoader.getMap(CompanyPartition.class, paymentProperty.getValue().getInteger("sellerCompanyPartition"), "name=query:getCompanyName([CompanyPartition(company)])").getString("name"));
    
    customerLabel.setText(ObjectLoader.getMap(CompanyPartition.class, 
            paymentProperty.getValue().containsKey("customerCompanyPartitions") ? paymentProperty.getValue().getValue("customerCompanyPartitions", Integer[].class)[0] : paymentProperty.getValue().getInteger("customerCompanyPartition"), 
            "name=query:getCompanyName([CompanyPartition(company)])").getString("name"));
    
    
    typeChooser.getItems().addAll(
            PropertyMap.create().setValue("name", "Оформить поручение").setValue("storeType", Store.StoreType.БЕЗНАЛИЧНЫЙ).setValue("state", FXCollections.observableArrayList(Payment.State.ПОДГОТОВКА,Payment.State.ОТПРАВЛЕНО)),
            PropertyMap.create().setValue("name", "Ввести из выписки").setValue("storeType", Store.StoreType.БЕЗНАЛИЧНЫЙ).setValue("state", FXCollections.observableArrayList(Payment.State.ИСПОЛНЕНО)),
            PropertyMap.create().setValue("name", "Наличный расчёт").setValue("storeType", Store.StoreType.НАЛИЧНЫЙ).setValue("state", FXCollections.observableArrayList(Payment.State.ИСПОЛНЕНО)));
    
    currencyChooser.itemsProperty().bind(Bindings.createObjectBinding(() -> {
      ObservableList<PropertyMap> currencys = FXCollections.observableArrayList();
      if(typeChooser.getValue() != null) {
        Integer[] partitions = new Integer[0];
        if(paymentProperty.getValue().containsKey("customerCompanyPartitions"))
          partitions = ArrayUtils.addAll(partitions, paymentProperty.getValue().getValue("customerCompanyPartitions", Integer[].class));
        else partitions = new Integer[]{paymentProperty.getValue().getInteger("sellerCompanyPartition"), paymentProperty.getValue().getInteger("customerCompanyPartition")};


        ObjectLoader.getList(DBFilter.create(Store.class)
                .AND_IN("companyPartition", partitions)
                .AND_EQUAL("storeType", typeChooser.getValue().getValue("storeType"))
                .AND_EQUAL("objectType", Group.ObjectType.ВАЛЮТА), "id:=:currency","name:=:currency-name").stream().filter(c -> !currencys.contains(c)).forEach(c -> currencys.add(c));
      }
      return currencys;
    }, typeChooser.valueProperty()));
    
    currencyChooser.itemsProperty().addListener((ObservableValue<? extends ObservableList<PropertyMap>> observable, ObservableList<PropertyMap> oldValue, ObservableList<PropertyMap> newValue) -> {
      if(!newValue.isEmpty())
        currencyChooser.setValue(newValue.get(0));
    });
    
    sellerStoreChooser.itemsProperty().bind(Bindings.createObjectBinding(() -> {
      return typeChooser.getValue() == null || currencyChooser.getValue() == null ? FXCollections.observableArrayList() : ObjectLoader.getList(DBFilter.create(Store.class)
              .AND_EQUAL("objectType", Group.ObjectType.ВАЛЮТА)
              .AND_EQUAL("storeType", typeChooser.getValue().getValue("storeType"))
              .AND_EQUAL("currency", currencyChooser.getValue().getInteger("id"))
              .AND_EQUAL("companyPartition", paymentProperty.getValue().getInteger("sellerCompanyPartition")), "id","name");
    }, currencyChooser.valueProperty()));
    
    sellerStoreChooser.itemsProperty().addListener((ObservableValue<? extends ObservableList<PropertyMap>> observable, ObservableList<PropertyMap> oldValue, ObservableList<PropertyMap> newValue) -> {
      if(!newValue.isEmpty())
        sellerStoreChooser.setValue(newValue.get(0));
    });
    
    customerStoreChooser.itemsProperty().bind(Bindings.createObjectBinding(() -> {
      System.out.println("##########################################################");
      System.out.println(paymentProperty.getValue().toJson());
      return typeChooser.getValue() == null || currencyChooser.getValue() == null ? FXCollections.observableArrayList() : ObjectLoader.getList(DBFilter.create(Store.class)
              .AND_EQUAL("objectType", Group.ObjectType.ВАЛЮТА)
              .AND_EQUAL("storeType", typeChooser.getValue().getValue("storeType"))
              .AND_EQUAL("currency", currencyChooser.getValue().getInteger("id"))
              .AND_IN("companyPartition", paymentProperty.getValue().containsKey("customerCompanyPartitions") ? paymentProperty.getValue().getValue("customerCompanyPartitions", Integer[].class) : new Integer[]{paymentProperty.getValue().getInteger("customerCompanyPartition")}), 
              "id","companyPartition","name=query:name||' - '||(select name from [CompanyPartition] where id=[Store(companyPartition)])");
    }, currencyChooser.valueProperty()));
    
    customerStoreChooser.itemsProperty().addListener((ObservableValue<? extends ObservableList<PropertyMap>> observable, ObservableList<PropertyMap> oldValue, ObservableList<PropertyMap> newValue) -> {
      if(newValue != null && !newValue.isEmpty() && !paymentProperty.getValue().containsKey("customerCompanyPartitions"))
        customerStoreChooser.setValue(newValue.get(0));
    });
    
    sellerStoreChooser.valueProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> initDocuments());
    customerStoreChooser.valueProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> initDocuments());
    
    sellerDocumentTable.setEditable(true);
    sellerDocumentTable.getColumns().addAll(
            Column.create("Документ","name"),
            Column.create("Номер","number",true,true),
            Column.create("Дата","date",true,true));
    
    ((TableColumn)sellerDocumentTable.getColumn("Номер")).setCellFactory((Object param) -> 
      new FXDivisionTableCell() {
        @Override
        protected void updateItem(Object item, boolean empty) {
          super.updateItem(item, empty);
            if(getRowItem() != null) {
              setEditable(!getRowItem().isNull("autonumber") && !getRowItem().is("autonumber"));
            }
          }
      });
    
    customerDocumentTable.setEditable(true);
    customerDocumentTable.getColumns().addAll(
            Column.create("Документ","name"),
            Column.create("Номер","number",true,true),
            Column.create("Дата","date",true,true));
    
    ((TableColumn)customerDocumentTable.getColumn("Номер")).setCellFactory((Object param) -> 
      new FXDivisionTableCell() {
        @Override
        protected void updateItem(Object item, boolean empty) {
          super.updateItem(item, empty);
            if(getRowItem() != null) {
              setEditable(!getRowItem().isNull("autonumber") && !getRowItem().is("autonumber"));
            }
          }
      });
    
    dealTable.getColumns().addAll(
            Column.create("Договор", "name"),
            Column.create("№", "contract_number"),
            Column.create("Процесс", "service_name"),
            Column.create("Начало", "dealStartDate"),
            Column.create("Окончание", "dealEndDate"),
            Column.create("Цена", "cost"),
            Column.create("Зачёт","select"),
            Column.create("Оплатить","needpay")/*,
            Column.create("otherpay","otherpay"),
            Column.create("thispay","thispay")*/
            );
    
    
    ((TableColumn)dealTable.getColumn("Зачёт")).setCellFactory((Object param) -> 
            new FXDivisionTableCell() {
              @Override
              protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                  if(getRowItem() != null) {
                    setDisable(getRowItem().isNull("thispay") || 
                            mayPay().compareTo(BigDecimal.ZERO) <= 0 && getRowItem().getBigDecimal("thispay").compareTo(BigDecimal.ZERO) == 0 || 
                                    getRowItem().getBigDecimal("needpay").compareTo(BigDecimal.ZERO) == 0 && getRowItem().getBigDecimal("thispay").compareTo(BigDecimal.ZERO) == 0);
                  }
                }
            });
    
    dealTable.itemsProperty().bind(Bindings.createObjectBinding(() -> {
      ObservableList<PropertyMap> deals = ObjectLoader.getList(DBFilter.create(Deal.class)
              .AND_EQUAL("sellerCompanyPartition", paymentProperty.getValue().getInteger("sellerCompanyPartition"))
              .AND_IN("customerCompanyPartition", 
                      paymentProperty.getValue().containsKey("customerCompanyPartitions") ? 
                              paymentProperty.getValue().getValue("customerCompanyPartitions", Integer[].class) : 
                              new Integer[]{paymentProperty.getValue().getInteger("customerCompanyPartition")}
              ), 
              "id",
              "name=query:select [Contract(templatename)] from [Contract] where id=[Deal(contract)]",
              "contract_number",
              "service_name",
              "dealStartDate",
              "dealEndDate",
              "cost",
              "select=query:NULLTOZERO(getPaymentAmountFromPayment([Deal(id)],"+paymentProperty.getValue().getInteger("id")+")) > 0",
              "needpay=query:NULLTOZERO(NULL)",
              "otherpay=query:[Deal(paymentAmount)]-NULLTOZERO(getPaymentAmountFromPayment([Deal(id)],"+paymentProperty.getValue().getInteger("id")+"))",
              "thispay=query:NULLTOZERO(getPaymentAmountFromPayment([Deal(id)],"+paymentProperty.getValue().getInteger("id")+"))").filtered(d -> d.getBigDecimal("cost").compareTo(d.getBigDecimal("otherpay")) > 0);
      
      deals.stream().forEach(deal -> {
        deal.get("needpay").bind(Bindings.createObjectBinding(() -> deal.getBigDecimal("cost").subtract(deal.getBigDecimal("otherpay")).subtract(deal.isNull("thispay") ? BigDecimal.ZERO : deal.getBigDecimal("thispay")), deal.get("thispay")));
        
        deal.get("select").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
          deal.setValue("thispay", (boolean)newValue ? mayPay(deal.getBigDecimal("cost").subtract(deal.getBigDecimal("otherpay"))) : BigDecimal.ZERO);
          neraspRefresh();
           dealTable.getColumn("Зачёт").setVisible(false);
           dealTable.getColumn("Зачёт").setVisible(true);
          //dealTable.refresh();
        });
      });
      return deals;
    }, paymentProperty));
    
    initData();
  }
  
  public void neraspRefresh() {
    BigDecimal may = mayPay();
    if(may != null && may.compareTo(BigDecimal.ZERO) < 0)
      amountField.getStyleClass().add("red-amount-text");
    else amountField.getStyleClass().remove("red-amount-text");
    neraspredeleno.setText(may == null || may.compareTo(BigDecimal.ZERO) <= 0 ? "" : "Нераспределено: "+may.toPlainString());
  }
  
  public BigDecimal mayPay() {
    return mayPay(null);
  }
  
  public BigDecimal mayPay(BigDecimal pay) {
    BigDecimal amount = paymentProperty.getValue().getBigDecimal("amount");
    amount = amount == null ? BigDecimal.ZERO : amount;
    for(PropertyMap d:dealTable.getItems())
      amount = amount.subtract(d.isNull("thispay") ? BigDecimal.ZERO : d.getBigDecimal("thispay"));
    
    BigDecimal returnPay = pay == null || amount.compareTo(pay) < 0 ? amount.compareTo(BigDecimal.ZERO) < 0 ? paymentProperty.getValue().getBigDecimal("amount") : amount.setScale(2, RoundingMode.HALF_UP) : pay.setScale(2, RoundingMode.HALF_UP);
    
    return returnPay;
  }
  
  private void moveMoneyStorePosition(RemoteSession session) throws RemoteException {
    for(List d:session.executeQuery("SELECT "
      /*0*/+ "[Payment(amount)], "
      /*1*/+ "[Payment(customer-store-controll-out)], "
      /*2*/+ "[Payment(sellerCompanyPartition)],"
      /*3*/+ "[Payment(sellerStore)], "
      /*4*/+ "[Payment(customerStore)], "
      /*5*/+ "[Payment(customerCompanyPartition)] "
      + "FROM [Payment] WHERE [Payment(id)] = "+paymentProperty.getValue().getInteger("id"))) {
      
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

  private void save() {
    System.out.println("############# MEMNTO  #########################");
    memnto.getList("dealPayments").forEach(dp -> System.out.println(dp.getValue("deal")));
    System.out.println("############# CURRENT #########################");
    paymentProperty.getValue().getList("dealPayments").forEach(dp -> System.out.println(dp.getValue("deal")));
    
    if(paymentProperty.getValue().isNull("id") || !memnto.equals(paymentProperty.getValue())) {
      RemoteSession session = null;
      try {
        session = ObjectLoader.createSession();
        PropertyMap payment = paymentProperty.getValue().copy();
        payment
                .setValue("amount", payment.isNull("amount") ? BigDecimal.ZERO : payment.getBigDecimal("amount"))
                .setValue("sellerStore"   , payment.getMap("sellerStore").getInteger("id"))
                .setValue("customerStore", payment.getMap("customerStore").getInteger("id"));

        if(payment.isNull("id")) {
          payment.setValue("id", ObjectLoader.createObject(Payment.class, payment.getSimpleMapWithoutKeys("dealPayments","id","companyPartitions")));
          paymentProperty.getValue().copyFrom(payment.getSimpleMap("id"));
          
          Map<Integer,List> ids = documentCreator.start(session, payment.getInteger("id"), new TreeMap());
          List<Integer> docs = new ArrayList<>();
          for(List ds:ids.values())
            docs.addAll(ds);
          String operationDate = "CURRENT_DATE";
          Object[] params = new Object[]{payment.getInteger("id")};
          if(!ids.isEmpty()) {
            operationDate = "(SELECT MAX(date) FROM [CreatedDocument] WHERE [CreatedDocument(id)]=ANY(?))";
            params = org.apache.commons.lang.ArrayUtils.add(params, 0, docs.toArray(new Integer[0]));
          }
          session.executeUpdate("UPDATE [Payment] SET [Payment(operationDate)]="+operationDate+" WHERE id=?", params);
        }else ObjectLoader.saveObject(Payment.class, payment.getInteger("id"), payment.getSimpleMapWithoutKeys("dealPayments"));
        
        session.removeObjects(DealPayment.class, PropertyMap.getArrayFromList(PropertyMap.createList(session.getList(DBFilter.create(DealPayment.class).AND_EQUAL("payment", payment.getInteger("id")), "id")), "id", Integer.class));
        for(PropertyMap dp:payment.getList("dealPayments"))
          dp.setValue("id", session.createObject(DealPayment.class, dp.setValue("payment", payment.getInteger("id")).getSimpleMap()));
        
        session.addEvent(Payment.class, "UPDATE", payment.getInteger("id"));
        session.addEvent(Deal.class, "UPDATE", PropertyMap.getArrayFromList(payment.getList("dealPayments"), "deal", Integer.class));
        
        moveMoneyStorePosition(session);
        
        ObjectLoader.commitSession(session);
        dialog.setVisible(false);
      }catch(Exception ex) {
        ObjectLoader.rollBackSession(session);
        MsgTrash.out(ex);
      }
    }
  }
}