package division.fx.controller.payment;

import atolincore5.Atolin;
import atolincore5.Item;
import atolincore5.Receipt;
import atolincore5.iDevice;
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
import bum.interfaces.Store.StoreType;
import division.fx.DivisionTextField;
import division.fx.PropertyMap;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.dialog.FXD;
import division.fx.editor.FXObjectEditor;
import division.fx.table.Column;
import division.fx.table.FXDivisionTable;
import division.fx.table.FXDivisionTableCell;
import division.fx.util.MsgTrash;
import division.util.FileLoader;
import division.util.actions.DocumentCreator;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import javafx.util.converter.BigDecimalStringConverter;
import org.apache.commons.lang3.ArrayUtils;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class FXPayment_1 extends FXObjectEditor {
  private ComboBox<PropertyMap>         customerStoreChooser = new ComboBox<>();
  private FXDivisionTable<PropertyMap>  customerDocuments    = new FXDivisionTable<>(Column.create("Документ","name"), Column.create("Номер", "number", true, true), Column.create("Дата", "date", true, true));
  
  private ComboBox<PropertyMap>         sellerStoreChooser = new ComboBox<>();
  private FXDivisionTable<PropertyMap>  sellerDocuments    = new FXDivisionTable<>(Column.create("Документ","name"), Column.create("Номер", "number", true, true), Column.create("Дата", "date", true, true));
  
  private ComboBox<String>              typeChooser     = new ComboBox<>(FXCollections.observableArrayList("Ввести из выписки","Наличный расчёт","Оформить поручение"));
  private DivisionTextField<BigDecimal> amountField     = new DivisionTextField<>(new BigDecimalStringConverter() {
    @Override
    public BigDecimal fromString(String value) {
      return super.fromString(value) == null ? BigDecimal.ZERO : super.fromString(value).setScale(2);
    }
  });
  private ComboBox<PropertyMap>         currencyChooser = new ComboBox<>();
  
  private FXDivisionTable<PropertyMap>  dealTable = new FXDivisionTable<>(
          Column.create("Договор"),
          Column.create("Номер"), 
          Column.create("Процесс"), 
          Column.create("Начало"),
          Column.create("Окончание"), 
          Column.create("Цена"), 
          Column.create("Зачёт","Зачёт", true, true), 
          Column.create("Не оплачено","no-pay"));
  
  private final ObjectProperty<BigDecimal> money = new SimpleObjectProperty<>(BigDecimal.ZERO);
  
  private Label  neraspredeleno = new Label();
  private final Button checkButton = new Button("Чек");
  private final DocumentCreator documentCreator = new DocumentCreator();
  private ChangeListener<PropertyMap> storeChooserListener = (ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
    customerDocuments.clear();
    sellerDocuments.clear();
    if(sellerStoreChooser.getValue() != null && customerStoreChooser.getValue() != null) {
      List<Integer> deals = new ArrayList();
      List<Integer> dps = new ArrayList();
      dealTable.getItems().stream().filter(d -> d.isNotNull("this-pay") && d.getBigDecimal("this-pay").compareTo(BigDecimal.ZERO) > 0).forEach(d -> deals.add(d.getValue("id", Integer.TYPE)));
      ObjectLoader.getList(DBFilter.create(DealPosition.class).AND_IN("deal", deals.toArray(new Integer[0])), "id").forEach(dp -> dps.add(dp.getValue("id", Integer.TYPE)));
      documentCreator.init(ProductDocument.ActionType.ОПЛАТА, dps.toArray(new Integer[0]), sellerStoreChooser.getValue().getValue("id", Integer.TYPE), customerStoreChooser.getValue().getValue("id", Integer.TYPE));
      
      if(getObjectProperty().isNotNull("id")) {
        ObservableList<PropertyMap> documents = ObjectLoader.getList(DBFilter.create(CreatedDocument.class).AND_EQUAL("payment", getObjectProperty().getInteger("id")), "id","name:=:document_name","document","number","date");
        
        documentCreator.getSellerSystemDocuments().values().stream().forEach(doc -> {
          if(PropertyMap.contains(documents, "document", doc.getInteger("id")))
            PropertyMap.get(documents, "document", doc.getInteger("id")).stream().forEach(d -> sellerDocuments.getSourceItems().add(d));
        });
        
        documentCreator.getCustomerSystemDocuments().values().stream().forEach(doc -> {
          if(PropertyMap.contains(documents, "document", doc.getInteger("id")))
            PropertyMap.get(documents, "document", doc.getInteger("id")).stream().forEach(d -> customerDocuments.getSourceItems().add(d));
        });
      }else {
        ObservableList<PropertyMap> citems =  FXCollections.observableArrayList(documentCreator.getCustomerSystemDocuments().values());
        citems.stream().forEach(i -> i.setValue("date", LocalDate.now()).setValue("number", i.getValue("autonumber", boolean.class) ? "авто..." : "введите номер"));
        customerDocuments.getSourceItems().setAll(citems);
        
        ObservableList<PropertyMap> sitems =  FXCollections.observableArrayList(documentCreator.getSellerSystemDocuments().values());
        sitems.stream().forEach(i -> i.setValue("date", LocalDate.now()).setValue("number", i.getValue("autonumber", boolean.class) ? "авто..." : "введите номер"));
        sellerDocuments.getSourceItems().setAll(sitems);
      }
    }
  };
  
  class DocumentNumberCell extends FXDivisionTableCell {
    @Override
    public void startEdit() {
      if(getRowItem() != null && !getRowItem().getValue("autonumber", boolean.class))
        super.startEdit();
    }
  };
  
  private final GridPane sideGrid = new GridPane();
  private final HBox topBox       = new HBox(typeChooser, new Label(""), amountField, currencyChooser);
  private final VBox sellerBox    = new VBox(sellerStoreChooser, sellerDocuments);
  private final VBox customerBox  = new VBox(customerStoreChooser, customerDocuments);
  private final TitleBorderPane sellerBorderPane   = new TitleBorderPane(sellerBox, "Получатель");
  private final TitleBorderPane customerBorderPane = new TitleBorderPane(customerBox, "Плательщик");
  private final VBox root = new VBox(topBox, sideGrid, neraspredeleno, dealTable);

  public FXPayment_1() {
    super();
    
    HBox.setHgrow(typeChooser, Priority.ALWAYS);
    HBox.setHgrow(amountField, Priority.ALWAYS);
    HBox.setHgrow(currencyChooser, Priority.ALWAYS);
    VBox.setVgrow(dealTable, Priority.ALWAYS);
    
    sideGrid.getColumnConstraints().addAll(new ColumnConstraints(), new ColumnConstraints());
    sideGrid.addRow(0, sellerBorderPane, customerBorderPane);
    sideGrid.getColumnConstraints().forEach(cl -> cl.setPercentWidth(50));
    getRoot().setCenter(root);
    
    storeControls().addAll(sellerDocuments, customerDocuments, dealTable);
    
    dealTable.getSortOrder().add(dealTable.getColumn("Зачёт"));
    dealTable.getColumn("Зачёт").setSortType(TableColumn.SortType.DESCENDING);
    
    sellerStoreChooser.valueProperty().addListener(storeChooserListener);
    customerStoreChooser.valueProperty().addListener(storeChooserListener);
    
    sellerDocuments.setEditable(true);
    sellerDocuments.getColumn("Номер").setCellFactory((Object param) -> new DocumentNumberCell());
    
    customerDocuments.setEditable(true);
    customerDocuments.getColumn("Номер").setCellFactory((Object param) -> new DocumentNumberCell());
    
    ((TableColumn)dealTable.getColumn("Зачёт")).setCellFactory((Object param) -> 
            new FXDivisionTableCell() {
              @Override
              public void commit(Object newValue) {
                if((boolean)newValue)
                  getRowItem().setValue("this-pay", money.getValue().compareTo(getRowItem().getValue("no-pay", BigDecimal.ZERO)) >= 0 ? getRowItem().getValue("no-pay", BigDecimal.ZERO) : money.getValue());
                else getRowItem().setValue("this-pay", null);
                getTableView().refresh();
              }
              
              @Override
              protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color: transparent");
                setDisable(getRowItem() != null && 
                        (money.getValue().compareTo(BigDecimal.ZERO) <= 0 && getRowItem().isNull("this-pay") || 
                                getRowItem().getBigDecimal("Цена").compareTo(getRowItem().getBigDecimal("other-pay")) == 0));
              }
            });
    
    checkButton.setOnAction(e -> {
      if(!getDeviceList(getObjectProperty().getInteger("sellerCompanyPartition")).isEmpty())
        save(getDeviceList(getObjectProperty().getInteger("sellerCompanyPartition")).get(0));
    });
  }


  @Override
  public void dialogEvent(WindowEvent event) {
    if(event.getEventType() == WindowEvent.WINDOW_SHOWN)  {
      ((FXD)getDialog()).getButton(FXD.ButtonType.OK).disableProperty().bind(Bindings.createBooleanBinding(() -> money.getValue().compareTo(BigDecimal.ZERO) < 0, money));
      
      checkButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
        //checkButton.getItems().clear();
        /*getDeviceList(getObjectProperty().getInteger("sellerCompanyPartition")).stream().forEach(device -> {
          MenuItem menu = new MenuItem(device.getString("name"));
          menu.setOnAction(e -> save(device));
          checkButton.getItems().add(menu);
        });*/
        
        return money.getValue().compareTo(BigDecimal.ZERO) < 0;// || checkButton.getItems().isEmpty();
      }, money, getObjectProperty().get("sellerCompanyPartition")));
      
      checkButton.visibleProperty().bind(Bindings.createBooleanBinding(() -> typeChooser.getValue() == "Наличный расчёт", typeChooser.valueProperty()));
      ((FXD)getDialog()).getButtonPanel().getChildren().add(0, checkButton);
    }
    if(event.getEventType() == WindowEvent.WINDOW_HIDING) {
      try {
        Properties conf = FileLoader.loadProperties("conf"+File.separator+getClass().getSimpleName());
        conf.setProperty("last-pay-type", typeChooser.getValue());
        
        LocalDateTime time = null;
        for(PropertyMap doc:sellerDocuments.getItems())
          if(time == null || doc.lastupdate().getValue().isAfter(time))
            time = doc.lastupdate().getValue();
        for(PropertyMap doc:customerDocuments.getItems())
          if(time == null || doc.lastupdate().getValue().isAfter(time))
            time = doc.lastupdate().getValue();
        
        conf.setProperty("last-date", time.format(DateTimeFormatter.ISO_DATE_TIME));
        
        FileLoader.storeProperties("conf"+File.separator+getClass().getSimpleName(), conf);
      }catch(Exception ex) {
        //MsgTrash.out(ex);
      }
    }
  }
  
  @Override
  public void initData() {
    // Получаем названия сторон
    getObjectProperty().copyFrom(ObjectLoader.getMap(Payment.class, getObjectProperty().getInteger("id"), 
            "currency=query:select [Store(currency)] from [Store] where id=[Payment(sellerStore)]",
            "customer-store-type",
            "seller-store-type",
            "sellerName=query:select getCompanyName([Payment(sellerCompany)])",
            "customerName=query:select getCompanyName([Payment(customerCompany)])"));
    
    // Получаем сделки
    ObservableList<Property> moneydependencies = FXCollections.observableArrayList();
    ObjectLoader.getList(DBFilter.create(Deal.class)
            .AND_EQUAL("tmp", false)
            .AND_EQUAL("type", Deal.Type.CURRENT)
            .AND_EQUAL("sellerCompany",   getObjectProperty().getInteger("sellerCompany"))
            .AND_EQUAL("customerCompany", getObjectProperty().getInteger("customerCompany")), 
            "id",
            "Договор=query:(SELECT [Contract(templatename)] FROM [Contract] WHERE id=[Deal(contract)])",
            "Номер:=:contract_number",
            "Процесс:=:service_name",
            "Начало:=:dealStartDate",
            "Окончание:=:dealEndDate",
            "Цена:=:cost",
            "Зачёт=query:false",
            "this-pay=query:getPaymentAmountFromPayment([Deal(id)],"+getObjectProperty().getValue("id")+")",
            "other-pay=query:[Deal(paymentAmount)]-NULLTOZERO(getPaymentAmountFromPayment([Deal(id)],"+getObjectProperty().getValue("id")+"))",
            "all-pay:=:paymentAmount").stream().filter(deal -> 
                            deal.isNotNull("this-pay") && deal.getBigDecimal("this-pay").compareTo(BigDecimal.ZERO) > 0 || // есть оплата с этого платежа
                            deal.getValue("all-pay", BigDecimal.class).compareTo(deal.getValue("Цена", BigDecimal.class)) < 0 || // сделка оплачена не полностью
                            getObjectProperty().getList("deals", "id", Integer.TYPE).contains(deal.getValue("id", Integer.TYPE)) // сделку предполагается оплатить
              ).forEach(deal -> {
                deal.equalKeys("id", "this-pay");
                dealTable.getItems().add(deal);
                moneydependencies.add(deal.get("this-pay"));
                
                deal.setValue("Цена", deal.getBigDecimal("Цена").setScale(2));
                
                deal.get("no-pay").bind(Bindings.createObjectBinding(() -> {
                  return deal.getBigDecimal("Цена").subtract(deal.isNull("this-pay") ? BigDecimal.ZERO : deal.getBigDecimal("this-pay")).subtract(deal.getBigDecimal("other-pay"));
                }, deal.get("this-pay")));
                
                if(getObjectProperty().getList("deals", "id", Integer.TYPE).contains(deal.getInteger("id")))
                  deal.setValue("this-pay", deal.getValue("no-pay"));
                
                deal.get("Зачёт").bind(Bindings.createBooleanBinding(() -> 
                        deal.isNotNull("this-pay") && deal.getBigDecimal("this-pay").compareTo(BigDecimal.ZERO) > 0/* || deal.isNotNull("other-pay") && deal.getBigDecimal("other-pay").compareTo(BigDecimal.ZERO) > 0*/, 
                        deal.get("this-pay")));
              });
    
    getObjectProperty().get("deals").bind(Bindings.createObjectBinding(() -> dealTable.getItems().filtered(d -> !d.isNull("this-pay") && d.getBigDecimal("this-pay").compareTo(BigDecimal.ZERO) > 0), moneydependencies.toArray(new Observable[0])));
    
    moneydependencies.add(getObjectProperty().get("amount"));
    money.unbind();
    money.bind(Bindings.createObjectBinding(() -> {
      BigDecimal m = getObjectProperty().getBigDecimal("amount");
      for(PropertyMap d:dealTable.getItems()) {
        try {
          m = m.subtract(d.getValue("this-pay", BigDecimal.ZERO));
        }catch(Exception ex) {
          ex.printStackTrace();
        }
      }
      return m;
    }, moneydependencies.toArray(new Observable[0])));
    
    
    neraspredeleno.textProperty().bind(Bindings.createStringBinding(() -> {
      return money.getValue().compareTo(BigDecimal.ZERO) <= 0 ? "" : ("Не распределено: "+money.getValue());
    }, money));
    
    typeChooser.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
      if(currencyChooser.getValue() == null)
        currencyChooser.getSelectionModel().selectFirst();
    });
    
    currencyChooser.itemsProperty().bind(Bindings.createObjectBinding(() -> {
      ObservableList<PropertyMap> items = FXCollections.observableArrayList();
      
      ObjectLoader.getList(DBFilter.create(Store.class)
            .AND_NOT_EQUAL("currency", null)
            .AND_EQUAL("tmp", false)
            .AND_EQUAL("storeType", typeChooser.getValue() != null && typeChooser.getValue().equals("Наличный расчёт") ? StoreType.НАЛИЧНЫЙ : StoreType.БЕЗНАЛИЧНЫЙ)
            .AND_IN("companyPartition", PropertyMap.getArrayFromList(ObjectLoader.getList(DBFilter.create(CompanyPartition.class).AND_IN("company", new Integer[]{getObjectProperty().getInteger("sellerCompany"),getObjectProperty().getInteger("customerCompany")}), "id"), "id", Integer.class)), 
            "id:=:currency",
            "name:=:currency-name").stream().filter(currency -> !items.contains(currency)).forEach(currency -> {
              items.add(currency);
            });
      return items;
    }, typeChooser.valueProperty()));
    
    currencyChooser.itemsProperty().addListener((ObservableValue<? extends ObservableList<PropertyMap>> observable, ObservableList<PropertyMap> oldValue, ObservableList<PropertyMap> newValue) -> {
      if(newValue.size() == 1)
        currencyChooser.getSelectionModel().selectFirst();
      else if(getObjectProperty().isNotNull("currency")) 
        currencyChooser.getSelectionModel().select(newValue.stream().filter(c -> c.getInteger("id").equals(getObjectProperty().getInteger("currency"))).findFirst().orElseGet(() -> newValue.get(0)));
    });
    
    sellerStoreChooser.itemsProperty().bind(Bindings.createObjectBinding(() -> 
      ObjectLoader.getList(DBFilter.create(Store.class)
              .AND_EQUAL("storeType", typeChooser.getValue() != null && typeChooser.getValue().equals("Наличный расчёт") ? StoreType.НАЛИЧНЫЙ : StoreType.БЕЗНАЛИЧНЫЙ)
              .AND_EQUAL("objectType", Group.ObjectType.ВАЛЮТА)
              .AND_EQUAL("currency", currencyChooser.valueProperty().getValue() == null ? null : currencyChooser.valueProperty().getValue().getValue("id"))
              
              .AND_IN("companyPartition", /*getObjectProperty().isNotNull("sellerCompanyPartition") ? 
                      new Integer[]{getObjectProperty().getInteger("sellerCompanyPartition")} : */
                      PropertyMap.getArrayFromList(ObjectLoader.getList(DBFilter.create(CompanyPartition.class).AND_EQUAL("company", getObjectProperty().getInteger("sellerCompany")), "id"), "id", Integer.class)),
              "id","companyPartition","name=query:name||' - '||(select name from [CompanyPartition] where id=[Store(companyPartition)])"), currencyChooser.valueProperty()));
    
    customerStoreChooser.itemsProperty().bind(Bindings.createObjectBinding(() -> 
      ObjectLoader.getList(DBFilter.create(Store.class)
              .AND_EQUAL("storeType", typeChooser.getValue() != null && typeChooser.getValue().equals("Наличный расчёт") ? StoreType.НАЛИЧНЫЙ : StoreType.БЕЗНАЛИЧНЫЙ)
              .AND_EQUAL("objectType", Group.ObjectType.ВАЛЮТА)
              .AND_EQUAL("currency", currencyChooser.valueProperty().getValue() == null ? null : currencyChooser.valueProperty().getValue().getValue("id"))
              
              .AND_IN("companyPartition", /*getObjectProperty().isNotNull("customerCompanyPartition") ? 
                      new Integer[]{getObjectProperty().getInteger("customerCompanyPartition")} : */
                      PropertyMap.getArrayFromList(ObjectLoader.getList(DBFilter.create(CompanyPartition.class).AND_EQUAL("company", getObjectProperty().getInteger("customerCompany")), "id"), "id", Integer.class)),
              "id","companyPartition","name=query:name||' - '||(select name from [CompanyPartition] where id=[Store(companyPartition)])"), currencyChooser.valueProperty()));
    
    sellerStoreChooser.itemsProperty().addListener((ObservableValue<? extends ObservableList<PropertyMap>> observable, ObservableList<PropertyMap> oldValue, ObservableList<PropertyMap> newValue) -> {
      if(getObjectProperty().isNotNull("sellerCompanyPartition"))
        sellerStoreChooser.getSelectionModel().select(sellerStoreChooser.getItems().stream()
                .filter(it -> it.getInteger("companyPartition").equals(getObjectProperty().getInteger("sellerCompanyPartition"))).findFirst().orElseGet(() -> null));
      else if(sellerStoreChooser.getItems().size() == 1)
        sellerStoreChooser.getSelectionModel().selectFirst();
    });
    
    customerStoreChooser.itemsProperty().addListener((ObservableValue<? extends ObservableList<PropertyMap>> observable, ObservableList<PropertyMap> oldValue, ObservableList<PropertyMap> newValue) -> {
      if(getObjectProperty().isNotNull("customerCompanyPartition"))
        customerStoreChooser.getSelectionModel().select(customerStoreChooser.getItems().stream()
                .filter(it -> it.getInteger("companyPartition").equals(getObjectProperty().getInteger("customerCompanyPartition"))).findFirst().orElseGet(() -> null));
      else if(customerStoreChooser.getItems().size() == 1)
        customerStoreChooser.getSelectionModel().selectFirst();
    });
    
    customerStoreChooser.itemsProperty().addListener((ObservableValue<? extends ObservableList<PropertyMap>> observable, ObservableList<PropertyMap> oldValue, ObservableList<PropertyMap> newValue) -> {
      if(customerStoreChooser.getItems().size() == 1)
        customerStoreChooser.getSelectionModel().selectFirst();
    });
    
    String lastpaytype = "Ввести из выписки";
    try {
      Properties conf = FileLoader.loadProperties("conf"+File.separator+getClass().getSimpleName());
      lastpaytype = conf.getProperty("last-pay-type") == null ? "Ввести из выписки" : conf.getProperty("last-pay-type");
    }catch(Exception ex) {}
    
    if(!getObjectProperty().is("tmp")) {
      if(getObjectProperty().getValue("seller-store-type", Store.StoreType.class) == StoreType.БЕЗНАЛИЧНЫЙ)
        lastpaytype = "Ввести из выписки";
      else lastpaytype = "Наличный расчёт";
    }
    
    typeChooser.setValue(lastpaytype);

    typeChooser.setDisable(!getObjectProperty().is("tmp"));
    amountField.setDisable(!getObjectProperty().is("tmp"));
    currencyChooser.setDisable(!getObjectProperty().is("tmp"));
    sellerStoreChooser.setDisable(!getObjectProperty().is("tmp"));
    customerStoreChooser.setDisable(!getObjectProperty().is("tmp"));
    
    getObjectProperty().get("sellerStore").bind(sellerStoreChooser.valueProperty());
    getObjectProperty().get("customerStore").bind(customerStoreChooser.valueProperty());
    
    getObjectProperty().get("sellerCompanyPartition").bind(Bindings.createObjectBinding(() -> {
      return sellerStoreChooser.getValue() == null ? null : sellerStoreChooser.getValue().getInteger("companyPartition");
    }, sellerStoreChooser.valueProperty()));
    
    getObjectProperty().get("customerCompanyPartition").bind(Bindings.createObjectBinding(() -> {
      return customerStoreChooser.getValue() == null ? null : customerStoreChooser.getValue().getInteger("companyPartition");
    }, customerStoreChooser.valueProperty()));
    
    amountField.setValue(getObjectProperty().getBigDecimal("amount"));
    getObjectProperty().get("amount").addListener((ObservableValue observable, Object oldValue, Object newValue) -> dealTable.refresh());
    getObjectProperty().get("amount").bind(amountField.valueProperty());
    
    ((Label)sellerBorderPane.getTitle()).textProperty().bind(getObjectProperty().get("sellerName"));
    ((Label)customerBorderPane.getTitle()).textProperty().bind(getObjectProperty().get("customerName"));
    
    amountField.valueProperty().addListener((ObservableValue<? extends BigDecimal> observable, BigDecimal oldValue, BigDecimal newValue) -> {
      Executors.newSingleThreadExecutor().submit(() -> {
        try {
          Thread.sleep(2000);
          Platform.runLater(() -> {
            BigDecimal amount = amountField.getValue();
            
            List<Integer> deals = dealTable.getItems().stream().filter(d -> d.isNotNull("this-pay") && d.getBigDecimal("this-pay").compareTo(BigDecimal.ZERO) > 0).map(d -> d.setValue("this-pay", BigDecimal.ZERO).getInteger("id")).collect(Collectors.toList());
            
            for(Integer id:deals) {
              PropertyMap deal = dealTable.getItems().stream().filter(d -> d.getInteger("id").equals(id)).findFirst().orElseGet(() -> null);
              
              BigDecimal newThisPay = deal.getBigDecimal("Цена").subtract(deal.getBigDecimal("other-pay"));
              if(amount.compareTo(newThisPay) <= 0)
                newThisPay = amount;
              
              amount = amount.subtract(newThisPay);
              
              if(amount.compareTo(BigDecimal.ZERO) < 0) {
                break;
              }else deal.setValue("this-pay", newThisPay);
            }
          });
        } catch (InterruptedException ex) {
          Logger.getLogger(FXPayment_1.class.getName()).log(Level.SEVERE, null, ex);
        }
      });
    });
    
    setMementoProperty(getObjectProperty().copy());
  }
  
  private void check(PropertyMap device, RemoteSession session) throws Exception {
    if(device != null) {
      
      boolean nds = (boolean) session.getList(DBFilter.create(CompanyPartition.class).AND_EQUAL("id", getObjectProperty().getInteger("sellerCompanyPartition"))).get(0).get("nds-payer");

      Map<Integer, BigDecimal> dealPayments = new HashMap<>();
      session.getList(DBFilter.create(DealPayment.class).AND_EQUAL("payment", getObjectProperty().getInteger("id")), "deal", "amount").stream().map(p -> PropertyMap.copy(p)).forEach(p -> dealPayments.put(p.getInteger("deal"), p.getBigDecimal("amount")));

      PropertyMap dealPositions = PropertyMap.create();
      session.getList(DBFilter.create(DealPosition.class).AND_IN("deal", dealPayments.keySet().toArray(new Integer[0])), 
              "deal","service_name","group_name","customProductCost","amount","product_nds").forEach(d -> {
                dealPositions.getList(d.get("deal").toString()).add(PropertyMap.copy(d));
              });

      List<Item> items = FXCollections.observableArrayList();


      for(String id:dealPositions.keySet()) {
        BigDecimal payAmount = dealPayments.get(Integer.valueOf(id));
        BigDecimal dealAmount = BigDecimal.ZERO;
        for(PropertyMap dp:dealPositions.getList(id))
          dealAmount = dealAmount.add(dp.getBigDecimal("amount").multiply(dp.getBigDecimal("customProductCost")));

        if(dealAmount.compareTo(payAmount) < 0)
          throw new Exception("Оплата больше стоимости сделок");

        if(dealAmount.compareTo(payAmount) == 1) {
          BigDecimal dpCount = BigDecimal.ZERO;
          for(PropertyMap dp:dealPositions.getList(id))
            dpCount = dpCount.add(dp.getBigDecimal("amount"));
          BigDecimal pa = payAmount.divide(dpCount, 2, RoundingMode.HALF_UP);

          dealPositions.getList(id).stream().forEach(dp -> {
            dp.setValue("customProductCost", pa);
            dp.setValue("all", false);
          });
        }

        for(PropertyMap dp:dealPositions.getList(id)) {
          int tax = Atolin.TaxTypeNoTax;

          if(nds && dp.getBigDecimal("product_nds").compareTo(new BigDecimal(18)) == 0)
            tax = Atolin.TaxTypeVAT18;
          if(nds && dp.getBigDecimal("product_nds").compareTo(new BigDecimal(10)) == 0)
            tax = Atolin.TaxTypeVAT10;
          if(nds && dp.getBigDecimal("product_nds").compareTo(new BigDecimal(20)) == 0)
            tax = Atolin.TaxTypeVAT20;
          if(nds && dp.getBigDecimal("product_nds").compareTo(new BigDecimal(0)) == 0)
            tax = Atolin.TaxTypeVAT0;

          Item.Builder b = new Item.Builder(dp.getString("service_name")+" "+dp.getString("group_name"), dp.getBigDecimal("customProductCost"), dp.getBigDecimal("amount"), tax);

          if(dp.isNotNull("all") && !dp.is("all"))
            b.paymentType(Atolin.PaymentTypePartialCredit);
          items.add(b.build());
        }
      }

      if(!items.isEmpty()) {
        Receipt r = new Receipt();
        r.items.addAll(items);
        r.setType(Atolin.ReceiptSell);
        getDevice(device).printReceipt(r.toXML());
      }
    }
  }
  
  private ObservableList<PropertyMap> getDeviceList(Integer partition) {
    ObservableList<PropertyMap> list = FXCollections.observableArrayList();
    PropertyMap prop = PropertyMap.fromJsonFile("conf"+File.separator+"kkt.json");
    if(!prop.isEmpty())
      list = prop.getList(String.valueOf(partition));
    return list;
  }
  
  private iDevice getDevice(PropertyMap device) throws Exception {
    return Atolin.getConnectionLocal(device.getString("ip"), device.getInteger("port"), device.getString("name"));
  }

  @Override
  public String validate() {
    String msg = "";
    for(PropertyMap doc:sellerDocuments.getItems())
      if(doc.isNullOrEmpty("number") || doc.getValue("number").equals("введите номер"))
        msg += "Введите номер для документа: \""+doc.getValue("name")+"\"";
    for(PropertyMap doc:customerDocuments.getItems())
      if(doc.isNullOrEmpty("number") || doc.getValue("number").equals("введите номер"))
        msg += "Введите номер для документа: \""+doc.getValue("name")+"\"";
    if(getObjectProperty().isNull("sellerStore"))
      msg += "Задайте склад получателя";
    if(getObjectProperty().isNull("customerStore"))
      msg += "Задайте склад плательщика";
    return msg;
  }
  
  
  
  
  
  private void save(RemoteSession session) throws Exception {
    System.out.println("getObjectProperty().getList(\"deals\").size() = "+getObjectProperty().getList("deals").size());
    getObjectProperty().getList("deals").stream().forEach(d -> System.out.println("save: "+d.toJson()));

    boolean newpay = getObjectProperty().isNull("id");

    if(newpay) { // создаём платёж 
      getObjectProperty().setValue("tmp", false);
      getObjectProperty().setValue("id", session.createObject(Payment.class, 
              PropertyMap.copy(getObjectProperty(), "amount", "sellerCompanyPartition", "customerCompanyPartition")
                      .setValue("sellerStore", getObjectProperty().getMap("sellerStore").getValue("id"))
                      .setValue("customerStore", getObjectProperty().getMap("customerStore").getValue("id"))
                      .getSimpleMap()));
  }else session.executeUpdate(Payment.class, // сохраняем платёж
            new String[]{"tmp", "amount", "sellerStore", "customerStore"}, 
            new Object[] {getObjectProperty().getValue("tmp"), getObjectProperty().getValue("amount"), getObjectProperty().getMap("sellerStore").getValue("id"), getObjectProperty().getMap("customerStore").getValue("id")}, 
            new Integer[]{getObjectProperty().getValue("id", Integer.TYPE)});

    List<String> querys = new ArrayList<>();
    querys.add("DELETE FROM [DealPayment] WHERE [DealPayment(payment)]="+getObjectProperty().getValue("id"));
    getObjectProperty().getList("deals").stream().filter(deal -> !deal.isNull("this-pay")).forEach(deal -> 
            querys.add("INSERT INTO [DealPayment]([DealPayment(deal)], [DealPayment(payment)], [DealPayment(amount)]) VALUES("+deal.getValue("id") +","+getObjectProperty().getValue("id")+","+deal.getValue("this-pay")+")"));

    session.executeUpdate(querys.toArray(new String[0]));

    moveMoneyStorePosition(session);

    if(newpay) {
      Map<Integer,List> ids = documentCreator.start(session, getObjectProperty().getInteger("id"), new TreeMap());
      List<Integer> docs = new ArrayList<>();
      for(List ds:ids.values())
        docs.addAll(ds);
      String operationDate = "CURRENT_DATE";
      Object[] params = new Object[]{getObjectProperty().getValue("id", Integer.TYPE)};
      if(!ids.isEmpty()) {
        operationDate = "(SELECT MAX(date) FROM [CreatedDocument] WHERE [CreatedDocument(id)]=ANY(?))";
        params = ArrayUtils.add(params, 0, docs.toArray(new Integer[0]));
      }
      session.executeUpdate("UPDATE [Payment] SET [Payment(operationDate)]="+operationDate+" WHERE id=?", params);
    }

    session.addEvent(Payment.class, newpay ? "CREATE" : "UPDATE", getObjectProperty().getValue("id", Integer.TYPE));

    HashSet<Integer> ids = new HashSet<>();
    getObjectProperty().getList("deals").stream().filter(deal -> !deal.isNull("this-pay")).forEach(deal -> ids.add(deal.getValue("id", Integer.TYPE)));
    session.addEvent(Deal.class, "UPDATE", ids.toArray(new Integer[0]));
  }

  @Override
  public boolean save() {
    return save((PropertyMap)null);
  }
  
  private boolean save(PropertyMap device) {
    String msg = validate();
    if(msg != null && !"".equals(msg)) {
      FXD.showWait("Ошибка!!!", getRoot(), msg, FXD.ButtonType.OK);
      return false;
    }else {
      RemoteSession session = null;
      try {
        session = ObjectLoader.createSession(false);
        save(session);
        
        if(device != null)
          check(device, session);
        
        ObjectLoader.commitSession(session);
        setMementoProperty(getObjectProperty().copy());
      }catch(Exception ex) {
        ObjectLoader.rollBackSession(session);
        MsgTrash.out(ex);
        return false;
      }
      return true;
    }    
    
    /*String msg = validate();
    if(msg != null && !"".equals(msg)) {
      FXD.showWait("Ошибка!!!", getRoot(), msg, FXD.ButtonType.OK);
      return false;
    }else {
      System.out.println("getObjectProperty().getList(\"deals\").size() = "+getObjectProperty().getList("deals").size());
      getObjectProperty().getList("deals").stream().forEach(d -> System.out.println("save: "+d.toJson()));
      RemoteSession session = null;
      try {
        session = ObjectLoader.createSession(false, true);

        boolean newpay = getObjectProperty().isNull("id");

        if(newpay) // создаём платёж
          getObjectProperty().setValue("id", session.createObject(Payment.class, 
                  PropertyMap.copy(getObjectProperty(), "amount", "sellerCompanyPartition", "customerCompanyPartition")
                          .setValue("tmp", false)
                          .setValue("sellerStore", getObjectProperty().getMap("sellerStore").getValue("id"))
                          .setValue("customerStore", getObjectProperty().getMap("customerStore").getValue("id"))
                          .getSimpleMap()));
        else session.executeUpdate(Payment.class, // сохраняем платёж
                new String[]{"tmp", "amount", "sellerStore", "customerStore"}, 
                new Object[] {getObjectProperty().getValue("tmp"), getObjectProperty().getValue("amount"), getObjectProperty().getMap("sellerStore").getValue("id"), getObjectProperty().getMap("customerStore").getValue("id")}, 
                new Integer[]{getObjectProperty().getValue("id", Integer.TYPE)});

        List<String> querys = new ArrayList<>();
        querys.add("DELETE FROM [DealPayment] WHERE [DealPayment(payment)]="+getObjectProperty().getValue("id"));
        getObjectProperty().getList("deals").stream().filter(deal -> !deal.isNull("this-pay")).forEach(deal -> 
                querys.add("INSERT INTO [DealPayment]([DealPayment(deal)], [DealPayment(payment)], [DealPayment(amount)]) VALUES("+deal.getValue("id") +","+getObjectProperty().getValue("id")+","+deal.getValue("this-pay")+")"));

        session.executeUpdate(querys.toArray(new String[0]));

        moveMoneyStorePosition(session);

        if(newpay) {
          Map<Integer,List> ids = documentCreator.start(session, getObjectProperty().getInteger("id"), new TreeMap());
          List<Integer> docs = new ArrayList<>();
          for(List ds:ids.values())
            docs.addAll(ds);
          String operationDate = "CURRENT_DATE";
          Object[] params = new Object[]{getObjectProperty().getValue("id", Integer.TYPE)};
          if(!ids.isEmpty()) {
            operationDate = "(SELECT MAX(date) FROM [CreatedDocument] WHERE [CreatedDocument(id)]=ANY(?))";
            params = ArrayUtils.add(params, 0, docs.toArray(new Integer[0]));
          }
          session.executeUpdate("UPDATE [Payment] SET [Payment(operationDate)]="+operationDate+" WHERE id=?", params);
        }

        session.addEvent(Payment.class, newpay ? "CREATE" : "UPDATE", getObjectProperty().getValue("id", Integer.TYPE));

        HashSet<Integer> ids = new HashSet<>();
        getObjectProperty().getList("deals").stream().filter(deal -> !deal.isNull("this-pay")).forEach(deal -> ids.add(deal.getValue("id", Integer.TYPE)));
        session.addEvent(Deal.class, "UPDATE", ids.toArray(new Integer[0]));

        session.commit();
      }catch (Exception ex) {
        ObjectLoader.rollBackSession(session);
        MsgTrash.out(ex);
      }
      return true;
    }*/
  }
  
  private void moveMoneyStorePosition(RemoteSession session) throws RemoteException {
    for(List d:session.executeQuery("SELECT "
      /*0*/+ "[Payment(amount)], "
      /*1*/+ "[Payment(customer-store-controll-out)], "
      /*2*/+ "[Payment(sellerCompanyPartition)],"
      /*3*/+ "[Payment(sellerStore)], "
      /*4*/+ "[Payment(customerStore)], "
      /*5*/+ "[Payment(customerCompanyPartition)] "
      + "FROM [Payment] WHERE [Payment(id)] = "+getObjectProperty().getValue("id"))) {
      
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