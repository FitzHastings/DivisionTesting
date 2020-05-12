package division.fx.controller.deal;

import bum.editors.util.DivisionTarget;
import bum.editors.util.ObjectLoader;
import bum.interfaces.CreatedDocument;
import bum.interfaces.Deal;
import bum.interfaces.DealPayment;
import bum.interfaces.DealPosition;
import bum.interfaces.Payment;
import bum.interfaces.ProductDocument;
import bum.interfaces.ProductDocument.ActionType;
import static bum.interfaces.ProductDocument.ActionType.СТАРТ;
import division.fx.FXToolButton;
import division.fx.PropertyMap;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.controller.documents.CreatedDocumentTable;
import division.fx.controller.payment.FXPayment_1;
import division.fx.dialog.FXDialog;
import division.fx.editor.Storable;
import division.fx.table.Column;
import division.fx.table.FXDivisionTable;
import division.fx.table.filter.DateFilter;
import division.fx.table.filter.ListFilter;
import division.fx.table.filter.TextFilter;
import division.fx.util.MsgTrash;
import division.util.DivisionTask;
import division.util.Utility;
import division.util.actions.ActionUtil;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.ArrayUtils;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class FXDealActionDocuments_ extends SplitPane implements Storable {
  ///private final ObjectProperty<ObservableList<DealPeriod>>  deals         = new SimpleObjectProperty();
  private final ObjectProperty<Collection<PropertyMap>> dealPositions = new SimpleObjectProperty(FXCollections.observableArrayList());
  
  private final TitleBorderPane actionTitleBorder = new TitleBorderPane(new Label("События"));
  
  private final MenuButton actionAdd = new MenuButton();
  private final MenuItem   start     = new MenuItem("СТАРТ");
  private final MenuItem   dispatch  = new MenuItem("ОТГРУЗКА");
  private final MenuItem   pay       = new MenuItem("ОПЛАТА");
  
  private final FXToolButton actionDel = new FXToolButton("Удалить событие", "remove-button");
  
  private final ToolBar actionToolBar = new ToolBar(actionAdd, actionDel);
  
  private final FXDivisionTable<PropertyMap> actionTable = new FXDivisionTable(
          Column.create("Событие", new ListFilter("Событие")),
          Column.create("дата", "df",    new DateFilter("дата")),
          Column.create("дебет",   new TextFilter()),
          Column.create("кредит",  new TextFilter()));
  private final VBox actionPanel = new VBox(5, actionToolBar, actionTable);
  
  private final TitleBorderPane documentTitleBorder = new TitleBorderPane(new Label("Документы"));
  
  private final CreatedDocumentTable documents = new CreatedDocumentTable();
  
  private final ObservableList storeControlds = FXCollections.observableArrayList(this, documents, actionTable);
  
  private DivisionTask task;
  private final ObservableList<Integer> dealIdsToStart    = FXCollections.observableArrayList();
  private final ObservableList<Integer> dealIdsToDispatch = FXCollections.observableArrayList();
  private final ObservableList<Integer> dealIdsToPay      = FXCollections.observableArrayList();

  public FXDealActionDocuments_() {
    initComponents();
    initEvents();
  }

  @Override
  public ObservableList storeControls() {
    return storeControlds;
  }

  private void initComponents() {
    VBox.setVgrow(actionTable, Priority.ALWAYS);
    actionTable.setId("actionTable");
    
    actionTable.getColumns().stream().forEach(column -> {column.setSortable(true);});
    actionTable.getColumn("дата").setSortType(TableColumn.SortType.ASCENDING);
    actionTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    
    actionAdd.getItems().addAll(start, dispatch, pay);
    
    start.disableProperty().bind(Bindings.createBooleanBinding(dealIdsToStart::isEmpty, dealIdsToStart));
    dispatch.disableProperty().bind(Bindings.createBooleanBinding(dealIdsToDispatch::isEmpty, dealIdsToDispatch));
    pay.disableProperty().bind(Bindings.createBooleanBinding(dealIdsToPay::isEmpty, dealIdsToPay));
    
    actionAdd.getStyleClass().add("add-action-button");
    
    setOrientation(Orientation.HORIZONTAL);
    actionTitleBorder.setCenter(actionPanel);
    documentTitleBorder.setCenter(documents);
    getItems().addAll(actionTitleBorder, documentTitleBorder);
  }

  private void initEvents() {
    /*start.setOnAction(e -> ActionUtil.startDeals(this, dealIdsToStart));
    dispatch.setOnAction(e -> ActionUtil.dispatchDeals(this, dealIdsToDispatch));
    pay.setOnAction(e -> pay());*/
    
    DivisionTarget.create(Deal.class, (DivisionTarget target, String type, Integer[] ids, PropertyMap objectEventProperty) -> {
      Set<Integer> deals = PropertyMap.getSetFromList(dealPositionsProperty().getValue(), "deal", Integer.TYPE);
      boolean is = false;
      for(Integer id:ids) {
        if(deals.contains(id)) {
          is = true;
          break;
        }
      }
      if(is)
        initData();
    });
    
    actionDel.setOnAction(e -> {
      if(new Alert(Alert.AlertType.CONFIRMATION, "Удалить?", ButtonType.YES, ButtonType.CANCEL).showAndWait().get() == ButtonType.YES) {
        boolean removePayment = false;
        String msg = "";
        for(PropertyMap a:actionTable.getSelectionModel().getSelectedItems()) {
          if(a.getValue("Событие", ProductDocument.ActionType.class) == ProductDocument.ActionType.ОПЛАТА) {
            PropertyMap payment = ObjectLoader.getMap(Payment.class, a.getValue("payment", Integer.TYPE), 
                      "id",
                      "amount",
                      "dealPayments",
                      "operationDate",
                      "documents=query:(select array_agg([CreatedDocument(document_name)]||' №'||number||' от '||to_char(date,'DD.MM.YYYY')) from [CreatedDocument] where [CreatedDocument(document-system)]=true and [CreatedDocument(payment)]=[Payment(id)])");
            if(payment.getValue("dealPayments", Integer[].class).length == a.getValue("id", Integer[].class).length)
              msg += Utility.join(payment.getValue("documents", String[].class), "\n");
          }
        }
        
        if(!msg.equals("")) {
          msg = "Удаление оплат приведёт к появлению нераспределённых платежей:\n\n"+msg+"\n\nУдалить такие платёжи?";
          removePayment = FXDialog.show(this, new Label(msg), "???", FXDialog.ButtonGroup.YES_NO) == FXDialog.Type.YES;
        }
        
        RemoteSession session = null;
        try {
          session = ObjectLoader.createSession(false);
          for(PropertyMap a:actionTable.getSelectionModel().getSelectedItems()) {
            
            ObservableList<PropertyMap> documents = FXCollections.observableArrayList();
            for(List ds:session.getData(DealPosition.class, a.getList("positions",Integer.TYPE).toArray(new Integer[0]), "documents"))
              for(List d:session.getData(CreatedDocument.class, (Integer[])ds.get(0), "id","name"))
                documents.addAll(PropertyMap.createList(session.getList(DBFilter.create(CreatedDocument.class).AND_IN("id", (Integer[])ds.get(0)), "id","name","actionType")));
            
            switch(a.getValue("Событие", ProductDocument.ActionType.class)) {
              case ОПЛАТА:
                session.removeObjects(DealPayment.class, a.getValue("id", Integer[].class));
                if(removePayment)
                  session.removeObjects(Payment.class, a.getInteger("payment"));
                session.removeObjects(CreatedDocument.class, PropertyMap.getSetFromList(documents.filtered(d -> d.getValue("actionType", ActionType.class) == ActionType.ОПЛАТА),"id",Integer.TYPE).toArray(new Integer[0]));
                break;
              case ОТГРУЗКА:
                ActionUtil.removeDispatch(session, a.getList("positions",Integer.class).toArray(new Integer[0]));
                session.removeObjects(CreatedDocument.class, PropertyMap.getSetFromList(documents.filtered(d -> d.getValue("actionType", ActionType.class) == ActionType.ОТГРУЗКА),"id",Integer.TYPE).toArray(new Integer[0]));
                break;
              case СТАРТ:
                session.executeUpdate(DealPosition.class, new String[]{"startId","startDate"}, new Object[]{null,null}, a.getList("positions",Integer.class).toArray(new Integer[0]));
                session.removeObjects(CreatedDocument.class, PropertyMap.getSetFromList(documents.filtered(d -> d.getValue("actionType", ActionType.class) == ActionType.СТАРТ),"id",Integer.TYPE).toArray(new Integer[0]));
                break;
            }
          }
          session.addEvent(Deal.class, "UPDATE", PropertyMap.getSetFromList(dealPositionsProperty().getValue(), "deal", Integer.TYPE).toArray(new Integer[0]));
          ObjectLoader.commitSession(session);
        } catch (Exception ex) {
          ObjectLoader.rollBackSession(session);
          MsgTrash.out(FXDealActionDocuments_.this,ex);
        }
      }
    });
    
    actionAdd.addEventHandler(ComboBoxBase.ON_SHOWING, e -> {
      dealIdsToStart.clear();
      dealIdsToDispatch.clear();
      dealIdsToPay.clear();
      if(!dealPositionsProperty().getValue().isEmpty()) {
        ObjectLoader.getList(Deal.class, 
                PropertyMap.getSetFromList(dealPositionsProperty().getValue(), "deal", Integer.TYPE),
                "id",
                "dispatchCount=query:getDispatchCount(id)",
                "startCount=query:getStartCount(id)",
                "dealPositionCount=query:getDealPositionCount(id)",
                "paymentAmountPercent=query:getPaymentAmountPercent(id)").stream().forEach(deal -> {
                  if(deal.getValue("dealPositionCount", BigDecimal.class).compareTo(deal.getValue("startCount", BigDecimal.class)) != 0)
                    dealIdsToStart.add(deal.getValue("id", Integer.TYPE));

                  if(deal.getValue("dealPositionCount", BigDecimal.class).compareTo(deal.getValue("dispatchCount", BigDecimal.class)) != 0 && deal.getValue("startCount", BigDecimal.class).compareTo(BigDecimal.ZERO) > 0)
                    dealIdsToDispatch.add(deal.getValue("id", Integer.TYPE));

                  if(deal.getValue("paymentAmountPercent", BigDecimal.class).compareTo(new BigDecimal(100)) < 0)
                    dealIdsToPay.add(deal.getValue("id", Integer.TYPE));
                });
      }
    });
    
    dealPositions.addListener((ObservableValue<? extends Collection<PropertyMap>> observable, Collection<PropertyMap> oldValue, Collection<PropertyMap> newValue) -> {
      actionTable.getSourceItems().clear();
      if(newValue != null && !newValue.isEmpty())
        initData();
    });
    
    /*deals.addListener((ObservableValue<? extends ObservableList<DealPeriod>> observable, ObservableList<DealPeriod> oldValue, ObservableList<DealPeriod> newValue) -> {
      actionTable.getSourceItems().clear();
      if(newValue != null && !newValue.isEmpty())
        initData();
    });*/
    
    actionTable.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      documents.getClientFilter().clear();
      
      Integer[] sPositions = new Integer[0];
      Integer[] dPositions = new Integer[0];
      Integer[] payments   = new Integer[0];
      
      for(PropertyMap action:actionTable.getSelectionModel().getSelectedItems()) {
        switch(action.getValue("Событие", String.class)) {
          case "СТАРТ" :
            sPositions = ArrayUtils.addAll(sPositions, action.getList("positions", Integer.TYPE).toArray(new Integer[0]));
            break;
          case "ОТГРУЗКА" :
            dPositions = ArrayUtils.addAll(dPositions, action.getList("positions", Integer.TYPE).toArray(new Integer[0]));
            break;
          case "ОПЛАТА" :
            payments = ArrayUtils.add(payments, action.getValue("payment", Integer.TYPE));
            break;
        }
      }
      
      if(dPositions.length > 0)
        documents.getClientFilter().OR_IN("dealPositions", dPositions).AND_EQUAL("actionType", "ОТГРУЗКА");
      if(sPositions.length > 0)
        documents.getClientFilter().OR_IN("dealPositions", sPositions).AND_EQUAL("actionType", "СТАРТ");
      if(payments.length > 0)
        documents.getClientFilter().OR_IN("payment", payments);
      
      if(actionTable.getSelectionModel().getSelectedItems().isEmpty())
        documents.getClientFilter().AND_EQUAL("id", null);
      
      documents.initData();
    });
  }
  
  public void pay() {
    if(dealIdsToPay == null || dealIdsToPay.isEmpty()) {
      new Alert(Alert.AlertType.ERROR, "Нет данных для оплаты", ButtonType.CLOSE).showAndWait();
      return;
    }
    try {
      ObservableList<PropertyMap> deals = FXCollections.observableArrayList();
      //Делаем проверку на то чтбы плательщик и получатель был один по всем сделкам.
      ObservableList<PropertyMap> data = ObjectLoader.getList(
              Deal.class, 
              dealIdsToPay, 
              "id",
              
              "this-pay=query:getPaymentAmountFromPayment([Deal(id)],null)",
              "no-pay:=:needPay",
              
              "sellerCompanyPartition",
              "sellerCompany",
              "sellerName=query:getCompanyName([Deal(sellerCompany)])",
              
              "customerCompanyPartition",
              "customerCompany",
              "customerName=query:getCompanyName([Deal(customerCompany)])",
              
              "service",
              "service_name",
              
              "tempProcess",
              "contract",
              "contract_number",
              "contract_name");
      if(!data.isEmpty()) {
        BigDecimal amount = BigDecimal.ZERO;
        Integer seller   = data.get(0).getValue("sellerCompanyPartition", Integer.TYPE);
        Integer customer = data.get(0).getValue("customerCompanyPartition", Integer.TYPE);

        for(PropertyMap d:data) {
          deals.add(PropertyMap.copy(d, "id","Договор","Номер","Процесс","Начало","Окончание","Цена","Зачёт","this-pay","other-pay","all-pay")
                  .setValue("this-pay", d.getValue("no-pay", BigDecimal.class).setScale(2)).equalKeys("id","this-pay"));
          amount = amount.add(d.getValue("no-pay", BigDecimal.class));
          if(!seller.equals(d.getValue("sellerCompanyPartition", Integer.TYPE)) || !customer.equals(d.getValue("customerCompanyPartition", Integer.TYPE))) {
            seller = customer = null;
            break;
          }
        }

        if(seller != null && customer != null) {
          //division.fx.controller.payment.FXPayment_1 payment = new division.fx.controller.payment.FXPayment_1();
          FXPayment_1 payment = new FXPayment_1();
          payment.setObjectProperty(PropertyMap.create()
                  .setValue("sellerCompanyPartition", seller)
                  .setValue("sellerName", data.get(0).getValue("sellerName"))
                  .setValue("customerCompanyPartition", customer)
                  .setValue("customerName",  data.get(0).getValue("customerName"))
                  .setValue("amount", amount.setScale(2))
                  .setValue("deals", deals)
                  .setValue("state", Payment.State.ПОДГОТОВКА)
                  .setValue("tmp", true));
          payment.showAndWait(this);
        }else {
          MsgTrash.out(new Exception("Выберите сделки с одинаковыми плательщиками и получателями"));
        }
      }
    }catch(Exception ex) {
      MsgTrash.out(ex);
    }
  }
  
  /*public ObjectProperty<ObservableList<DealPeriod>> dealsProperty() {
    return deals;
  }*/
  
  public ObjectProperty<Collection<PropertyMap>> dealPositionsProperty() {
    return dealPositions;
  }

  private void initData() {
    Integer[] indexes = actionTable.getSelectionModel().getSelectedIndices().toArray(new Integer[0]);
    
    actionTable.clear();
    DivisionTask.stop(task);
    DivisionTask.start(task = new DivisionTask() {
      @Override
      public void task() throws DivisionTask.DivisionTaskException {
        
        /*Integer[] ids = new Integer[0];
        for(DealPeriod period:dealsProperty().getValue())
          ids = ArrayUtils.add(ids, period.getValue("id", Integer.TYPE));*/
        

        ObservableList<PropertyMap> startList = ObjectLoader.getList(
                DBFilter.create(DealPosition.class).AND_IN("id", dealPositionsProperty().getValue()).AND_DATE_NOT_EQUAL("startDate", null).AND_EQUAL("type", DealPosition.Type.CURRENT).AND_EQUAL("tmp", false), 
                "id", "Событие=query:'СТАРТ'", "дата:=:startDate", "startId", "sort: startId");
        PropertyMap action = null;
        for(int i=startList.size()-1;i>=0;i--) {
          if(action != null && action.getValue("startId").equals(startList.get(i).getValue("startId"))) {
            action.getList("positions", Integer.TYPE).add(startList.get(i).getValue("id", Integer.TYPE));
            startList.remove(i);
          }else {
            action = startList.get(i);
            action.setValue("positions", FXCollections.observableArrayList(action.getValue("id", Integer.TYPE))).remove("id");
          }
        }
        
        ObservableList<PropertyMap> dispatchList = ObjectLoader.getList(
                DBFilter.create(DealPosition.class).AND_IN("id", dealPositionsProperty().getValue()).AND_DATE_NOT_EQUAL("dispatchDate", null).AND_EQUAL("type", DealPosition.Type.CURRENT).AND_EQUAL("tmp", false), 
                "id", "Событие=query:'ОТГРУЗКА'", "дата:=:dispatchDate", "dispatchId", "кредит:=:cost", "sort: dispatchId");
        
        action = null;
        for(int i=dispatchList.size()-1;i>=0;i--) {
          if(action != null && action.getValue("dispatchId").equals(dispatchList.get(i).getValue("dispatchId"))) {
            action.getList("positions", Integer.TYPE).add(dispatchList.get(i).getValue("id", Integer.TYPE));
            action.setValue("кредит", action.getValue("кредит", BigDecimal.class).add(dispatchList.get(i).getValue("кредит", BigDecimal.class)));
            dispatchList.remove(i);
          }else {
            action = dispatchList.get(i);
            action.setValue("positions", FXCollections.observableArrayList(action.getValue("id", Integer.TYPE))).remove("id");
          }
        }
        
        ObservableList<PropertyMap> paymentList = ObjectLoader.getList(
                DBFilter.create(DealPayment.class)
                        .AND_IN("deal", PropertyMap.getSetFromList(dealPositionsProperty().getValue(), "deal", Integer.class).toArray(new Integer[0]))
                        .AND_EQUAL("type", DealPosition.Type.CURRENT).AND_EQUAL("tmp", false), 
                "id", "Событие=query:'ОПЛАТА'", "дата:=:date", "дебет:=:amount", "payment");

        ObservableList<PropertyMap> list = FXCollections.observableArrayList();

        for(int i=paymentList.size()-1;i>=0;i--) {
          PropertyMap pay = paymentList.get(i);
          boolean is = false;
          for(int j=0;j<list.size();j++) {
            PropertyMap l = list.get(j);
            Integer[] pids = l.getValue("id",Integer[].class);
            if(!ArrayUtils.contains(pids, pay.getValue("id",Integer.TYPE)) && Objects.equals(pay.getValue("payment"), l.getValue("payment"))) {
              l.setValue("id", ArrayUtils.add(pids, pay.getValue("id", Integer.TYPE)))
                      .setValue("дебет", l.getValue("дебет", BigDecimal.class).add(pay.getValue("дебет", BigDecimal.class)));
              is = true;
            }
          }
          if(!is)
            list.add(pay.copy().setValue("id", new Integer[]{pay.getValue("id", Integer.TYPE)}));
        }
        
        checkShutdoun();
        Platform.runLater(() -> {
          actionTable.getSourceItems().addAll(startList);
          actionTable.getSourceItems().addAll(dispatchList);
          actionTable.getSourceItems().addAll(list);
          FXDealActionDocuments_.this.setCursor(Cursor.DEFAULT);
          
          Arrays.asList(indexes).stream().filter(index -> index < actionTable.getItems().size()).forEach(index -> actionTable.getSelectionModel().select(index));
        });
      }
    });
  }
}
