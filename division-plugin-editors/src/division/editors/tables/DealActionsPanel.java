package division.editors.tables;

import bum.editors.EditorController;
import bum.editors.EditorGui;
import bum.editors.util.DivisionTarget;
import bum.editors.util.ObjectLoader;
import bum.interfaces.*;
import bum.interfaces.ProductDocument.ActionType;
import division.ClientMain;
import division.editors.objects.nPaymentEditor;
import division.fx.PropertyMap;
import division.fx.controller.payment.FXPayment_1;
import division.fx.util.MsgTrash;
import division.scale.ObjectPeriod;
import division.scale.ObjectPeriodScaleListener;
import division.scale.PeriodScale;
import division.swing.*;
import division.swing.guimessanger.Messanger;
import division.util.*;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;

public class DealActionsPanel extends JPanel implements ObjectPeriodScaleListener {
  private final DivisionToolButton addToolButton     = new DivisionToolButton(FileLoader.getIcon("Add16.gif"),    "Добавить");
  private final DivisionToolButton removeToolButton  = new DivisionToolButton(FileLoader.getIcon("Delete16.gif"), "Удалить");
  //private final DivisionToolButton print             = new DivisionToolButton(FileLoader.getIcon("Print16.gif"),  "печать");
  //private final DivisionToolButton preview           = new DivisionToolButton(FileLoader.getIcon("preview.gif"),  "Просмотр");
  private final JPanel         panel    = new JPanel(new BorderLayout());
  private final JToolBar       tools    = new JToolBar();
  private final JMenuItem      start    = new JMenuItem("Старт");
  private final JMenuItem      dispatch = new JMenuItem("Отгрузка");
  private final JMenuItem      pay      = new JMenuItem("Оплата");
  private final JMenuItem      spot     = new JMenuItem("<html><b>Спот</b></html>");
  private final DivisionTable      table    = new DivisionTable();
  private final DivisionScrollPane scroll   = new DivisionScrollPane(table);
  private final StatusBar      status   = new StatusBar();
  private final JLabel         saldo    = new JLabel("Сальдо конечное: ");
  
  private final JPopupMenu menu = new JPopupMenu();
  
  private Integer[] dealIds           = new Integer[0];
  private Integer[] dealIdsToStart    = new Integer[0];
  private Integer[] dealIdsToDispatch = new Integer[0];
  private Integer[] dealIdsToPay      = new Integer[0];
  
  private final DivisionSplitPane split = new DivisionSplitPane();
  
  private final CreatedDocumentTableEditor documents = new CreatedDocumentTableEditor();
  
  public ObjectProperty<Integer[]> dealPositionFiter = new SimpleObjectProperty<>();
  
  private PeriodScale scale = null;
  
  private boolean active = true;
  
  private final ExecutorService pool = Executors.newCachedThreadPool();
  private InitDataTask initDataTask = null;

  public DealActionsPanel() {
    this(null);
  }
  
  public DealActionsPanel(PeriodScale scale) {
    super(new BorderLayout());
    this.scale = scale;
    initComponents();
    initEvents();
  }
  
  public CreatedDocumentTableEditor getCreatedDocumentTableEditor() {
    return documents;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
    documents.setActive(active);
  }

  @Override
  public void setEnabled(boolean enabled) {
    EditorGui.setComponentEnable(tools, enabled);
    super.setEnabled(enabled);
  }

  public JToolBar getTools() {
    return tools;
  }

  private void initComponents() {
    tools.setFloatable(false);
    tools.add(addToolButton);
    tools.add(removeToolButton);
    //tools.addSeparator();
    //tools.add(print);
    //tools.add(preview);
    
    menu.add(start);
    menu.add(dispatch);
    menu.add(pay);
    menu.add(spot);
    
    documents.getClientFilter().clear().AND_EQUAL("id", null);
    ClientMain.removeClientMainListener(documents);
    //documents.setRemoveFunction(false);
    documents.setExportFunction(false);
    documents.setImportFunction(false);
    documents.setVisibleOkButton(false);
    documents.clearDateFilter();
    
    table.setColumns("id","событие","дата записи","дебет","кредит");
    table.setColumnWidthZero(0);
    
    panel.add(tools, BorderLayout.NORTH);
    panel.add(scroll, BorderLayout.CENTER);
    panel.add(status, BorderLayout.SOUTH);
    
    status.add(saldo);
    
    split.add(panel, JSplitPane.LEFT);
    split.add(documents.getGUI(), JSplitPane.RIGHT);
    
    add(split, BorderLayout.CENTER);
  }

  private void initEvents() {
    new DivisionTarget(Deal.class) {
      @Override
      public void messageReceived(String type, Integer[] ids_, PropertyMap objectEventProperty) {
        if(dealIds != null && dealIds.length > 0) {
          TreeSet<Integer> ids = new TreeSet<>(Arrays.asList(ids_));
          ids.retainAll(Arrays.asList(dealIds));
          if(!ids.isEmpty())
            initData();
        }
      }
    };
    
    table.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_DELETE) {
          table.getEditor().cancelCellEditing();
          removeActions();
        }
      }
    });
    
    removeToolButton.addActionListener(e -> removeActions());
    addToolButton.addActionListener(e -> menu.show(addToolButton, 0, addToolButton.getHeight()));
    table.addTableSelectionListener((int[] oldSelection, int[] newSelection) -> {
      removeToolButton.setEnabled(isEnabled() && table.getSelectedRows().length > 0);
      initDocuments();
    });
    
    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        int row = table.rowAtPoint(e.getPoint());
        if(e.getClickCount() == 2 && row != -1 && table.getValueAt(row, table.convertColumnIndexToView(5)).equals(ProductDocument.ActionType.ОПЛАТА)) {
          try {
            List<List> data = ObjectLoader.getData(DealPayment.class, (Integer[])table.getValueAt(row, table.convertColumnIndexToView(0)), new String[]{"payment"});
            if(!data.isEmpty()) {
              Payment payment = (Payment) ObjectLoader.getObject(Payment.class, (Integer)data.get(0).get(0));
              nPaymentEditor paymentEditor = new nPaymentEditor();
              paymentEditor.setEditorObject(payment);
              paymentEditor.setAutoLoad(true);
              paymentEditor.setAutoStore(true);
              //paymentEditor.initData();
              EditorController.getDeskTop().add(paymentEditor);
              if(paymentEditor.getInternalDialog() != null) {
                paymentEditor.getInternalDialog().setLayer(JDesktopPane.MODAL_LAYER);
                paymentEditor.getInternalDialog().setVisible(true);
              }
            }
          }catch(Exception ex) {
            Messanger.showErrorMessage(ex);
          }
        }
      }
    });
    
    //start.addActionListener(e -> ActionUtil.startDeals(Arrays.asList(dealIdsToStart)) /*start()*/);
    start.addActionListener(e -> division.util.actions.ActionUtil.startDeals(null, Arrays.asList(dealIdsToStart)));
    
    dispatch.addActionListener(e -> division.util.actions.ActionUtil.dispatchDeals(null, Arrays.asList(dealIdsToDispatch)) /*dispatch()*/);
    pay.addActionListener(e -> pay());
    //spot.addActionListener(e -> spot());
    
    menu.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        showMenu();
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
      }
    });
  }
  
  
  
  
  
  
  
  
  
  
  public void spot() {
    /*ActionUtil.startDeals(getDealIdsToStart(), new Date());
    ActionUtil.dispatch(getDealIdsToDispatch(), true, new Date());
    ActionUtil.payDeals(getDealIdsToPay());*/
  }

  public void start() {
    /*Date date = null;
    java.util.List<ObjectPeriod> periods = scale.getSelectedPeriods();
    java.util.List<Date> dates = scale.getIntersectionDatesOfPeriods(periods);
    if(!dates.isEmpty()) {
      Date[] dd = dates.toArray(new Date[0]);
      Arrays.sort(dd);
      
      JPanel panel = new JPanel(new BorderLayout());
      final JCheckBox calendarBox = new JCheckBox("Произвольная дата");*/
      //final bum_Calendar calendar = new bum_Calendar(/*dd[dd.length-1],bum_Calendar.Type.BEFORE*/);
      /*panel.add(calendar, BorderLayout.CENTER);
      if(periods.size() == 1)
        calendar.setDateInCalendar(new Date(periods.get(0).getStartDate()));
      else {
        final JCheckBox startOfDeals = new JCheckBox("По началу сделок");
        startOfDeals.setSelected(true);
        panel.add(startOfDeals, BorderLayout.SOUTH);
        calendarBox.setSelected(false);
        calendar.setBorder(new ComponentTitleBorder(calendarBox, calendar, BorderFactory.createLineBorder(Color.GRAY)));
        calendar.setDateInCalendar(dates.get(dates.size()-1));
        calendar.setEnabled(false);
        calendarBox.addChangeListener(e -> {
          calendar.setEnabled(calendarBox.isSelected());
          startOfDeals.setSelected(!calendarBox.isSelected());
        });
        startOfDeals.addChangeListener(e -> calendarBox.setSelected(!startOfDeals.isSelected()));
      }
      if(JOptionPane.showOptionDialog(null, panel, "", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[]{"Ok","Отмена"}, "Ok") == 0) {
        date = calendar.getDate();
      }else return;
    }*/
    try {
      ActionUtil.runAction(ActionType.СТАРТ, 
              ActionUtil.entry("deals",getDealIdsToStart())/*, 
              ActionUtil.entry("date", date)*/);
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  public void dispatch() {
    /*Date date = null;
    java.util.List<ObjectPeriod> periods = scale.getSelectedPeriods();
    java.util.List<Date> dates = scale.getIntersectionDatesOfPeriods(periods);

    Date[] dd = dates.toArray(new Date[0]);
    Arrays.sort(dd);

    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(System.currentTimeMillis());
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);

    if(!dates.isEmpty() && c.getTimeInMillis() > dd[dd.length-1].getTime()) {
      while(dates.get(dates.size()-1).getTime() < c.getTimeInMillis())
        dates.add(new Date(dates.get(dates.size()-1).getTime()+24*60*60*1000));
      dates.remove(dates.size()-1);
    }

    JPanel panel = new JPanel(new BorderLayout());
    final JCheckBox calendarBox = new JCheckBox("Произвольная дата");
    final bum_Calendar calendar = new bum_Calendar();

    panel.add(calendar, BorderLayout.CENTER);
    if(periods.size() == 1) {
      calendar.setDateInCalendar(new Date(periods.get(0).getEndDate()));
    }else {
      final JCheckBox endOfDeals = new JCheckBox("По окончанию сделок");
      endOfDeals.setSelected(true);
      panel.add(endOfDeals, BorderLayout.SOUTH);
      calendarBox.setSelected(false);
      calendar.setBorder(new ComponentTitleBorder(calendarBox, calendar, BorderFactory.createLineBorder(Color.GRAY)));
      if(!dates.isEmpty())
        calendar.setDateInCalendar(dates.get(dates.size()-1));
      calendar.setEnabled(false);
      calendarBox.addChangeListener((ChangeEvent e) -> {
        calendar.setEnabled(calendarBox.isSelected());
        endOfDeals.setSelected(!calendarBox.isSelected());
      });
      endOfDeals.addChangeListener((ChangeEvent e) -> {
        calendarBox.setSelected(!endOfDeals.isSelected());
      });
    }
    if(JOptionPane.showOptionDialog(null, panel, "", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[]{"Ok","Отмена"}, "Ok") == 0) {
      date = calendar.getDate();
    }else return;*/
    
    try {
      ActionUtil.runAction(ActionType.ОТГРУЗКА, 
              ActionUtil.entry("deals",getDealIdsToDispatch()), 
              //ActionUtil.entry("date", date),
              ActionUtil.entry("all",  false));
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  /*public void pay() {
    if(dealIdsToPay == null || dealIdsToPay.length == 0) {
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
          FXPayment payment = new FXPayment();
          payment.setObjectProperty(PropertyMap.create()
                  .setValue("sellerCompanyPartition", seller)
                  .setValue("sellerName", data.get(0).getValue("sellerName"))
                  .setValue("customerCompanyPartition", customer)
                  .setValue("customerName",  data.get(0).getValue("customerName"))
                  .setValue("amount", amount.setScale(2))
                  .setValue("deals", deals)
                  .setValue("state", Payment.State.ПОДГОТОВКА)
                  .setValue("tmp", true));
          payment.showAndWait(null);
        }else {
          MsgTrash.out(new Exception("Выберите сделки с одинаковыми плательщиками и получателями"));
        }
      }
    }catch(Exception ex) {
      MsgTrash.out(ex);
    }
  }*/
  
  /*public void pay() {
    Platform.runLater(() -> {
      if(getDealIdsToPay() == null || getDealIdsToPay().length == 0) {
        JOptionPane.showMessageDialog(null, "Нет данных для оплаты", "тук тук", JOptionPane.ERROR_MESSAGE);
        return;
      }
      try {
        //Делаем проверку на то чтбы плательщик и получатель был один по всем сделкам.
        ObservableList<PropertyMap> data = ObjectLoader.getList(Deal.class, getDealIdsToPay(), "id","sellerCompanyPartition","customerCompanyPartition","customerCompany","needPay");

        if(!data.isEmpty()) {
          BigDecimal amount = BigDecimal.ZERO;
          ObservableList<PropertyMap> dealPayments = FXCollections.observableArrayList();
          Integer seller   = data.get(0).getInteger("sellerCompanyPartition");
          Integer customer = data.get(0).getInteger("customerCompany");
          List<Integer> customerPartitions = new ArrayList<>();

          for(PropertyMap d:data) {
            amount = amount.add(d.getBigDecimal("needPay"));
            dealPayments.add(PropertyMap.create().setValue("deal", d.getInteger("id")).setValue("amount", d.getBigDecimal("needPay")));
            if(!customerPartitions.contains(d.getInteger("customerCompanyPartition")))
              customerPartitions.add(d.getInteger("customerCompanyPartition"));
            if(!Objects.equals(seller, d.getInteger("sellerCompanyPartition")) || !Objects.equals(customer, d.getInteger("customerCompany"))) {
              seller = customer = null;
              customerPartitions.clear();
              break;
            }
          }

          if(seller != null && customer != null) {
            PropertyMap payment = PropertyMap.create()
                    .setValue("sellerCompanyPartition", seller)
                    .setValue("dealPayments", dealPayments)
                    .setValue("amount", amount)
                    .setValue("state", Payment.State.ИСПОЛНЕНО)
                    .setValue("tmp", false);
            if(customerPartitions.size() == 1)
              payment.setValue("customerCompanyPartition", customerPartitions.get(0));
            else payment.setValue("customerCompanyPartitions", customerPartitions.toArray(new Integer[0]));

            //FXPayment pay = new FXPayment(payment);
            FXPayment_1 pay = new FXPayment_1();
            pay.showAndWait(null);
            //pay.show();

          }else Messanger.alert("Выберите сделки с одинаковыми плательщиками и получателями", "Ошибочка!!!))", JOptionPane.ERROR_MESSAGE);
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    });
  }*/
  
  public void pay() {
    Platform.runLater(() -> {
      if(dealIdsToPay == null || dealIdsToPay.length == 0) {
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
          BigDecimal amount   = BigDecimal.ZERO;
          Integer    seller            = data.get(0).getInteger("sellerCompany");
          Integer    customer          = data.get(0).getInteger("customerCompany");
          Integer    sellerPartition   = data.get(0).getInteger("sellerCompanyPartition");
          Integer    customerPartition = data.get(0).getInteger("customerCompanyPartition");
          
          //Integer[] sellerPartitions   = new Integer[0];
          //Integer[] customerPartitions = new Integer[0];

          for(PropertyMap d:data) {
            deals.add(PropertyMap.copy(d, "id","Договор","Номер","Процесс","Начало","Окончание","Цена","Зачёт","this-pay","other-pay","all-pay")
                    .setValue("this-pay", d.getBigDecimal("no-pay").setScale(2)).equalKeys("id","this-pay"));
            amount = amount.add(d.getValue("no-pay", BigDecimal.class));
            
            if(sellerPartition != null && !sellerPartition.equals(d.getInteger("sellerCompanyPartition")))
              sellerPartition = null;
            
            if(customerPartition != null && !customerPartition.equals(d.getInteger("customerCompanyPartition")))
              customerPartition = null;
              
            /*if(!ArrayUtils.contains(sellerPartitions, d.getInteger("sellerCompanyPartition")))
              sellerPartitions = (Integer[]) ArrayUtils.add(sellerPartitions, d.getInteger("sellerCompanyPartition"));
              
            if(!ArrayUtils.contains(customerPartitions, d.getInteger("customerCompanyPartition")))
              customerPartitions = (Integer[]) ArrayUtils.add(customerPartitions, d.getInteger("customerCompanyPartition"));*/
            
            if(!seller.equals(d.getInteger("sellerCompany")) || !customer.equals(d.getInteger("customerCompany"))) {
              seller = customer = null;
              break;
            }
          }

          if(seller != null && customer != null) {
            FXPayment_1 payment = new FXPayment_1();
            payment.setObjectProperty(PropertyMap.create()
                    
                    .setValue("sellerCompany", seller)
                    .setValue("sellerCompanyPartition", sellerPartition)
                    .setValue("sellerName", data.get(0).getValue("sellerName"))
                    
                    .setValue("customerCompany", customer)
                    .setValue("customerCompanyPartition", customerPartition)
                    .setValue("customerName",  data.get(0).getValue("customerName"))
                    
                    .setValue("amount", amount.setScale(2))
                    .setValue("deals", deals)
                    .setValue("state", Payment.State.ПОДГОТОВКА)
                    .setValue("tmp", true));
            payment.showAndWait(null);
            /*FXPayment payment = new FXPayment(PropertyMap.create()
                    .setValue("sellerCompany", seller)
                    .setValue("sellerCompanyPartition", sellerPartition)
                    .setValue("sellerName", data.get(0).getValue("sellerName"))
                    .setValue("customerCompany", customer)
                    .setValue("customerCompanyPartition", customerPartitions)
                    .setValue("customerName",  data.get(0).getValue("customerName"))
                    .setValue("amount", amount.setScale(2))
                    .setValue("deals", deals)
                    .setValue("state", Payment.State.ПОДГОТОВКА)
                    .setValue("tmp", true));
            payment.show();*/
          }else {
            MsgTrash.out(new Exception("Выберите сделки с одинаковыми плательщиками и получателями"));
          }
        }
      }catch(Exception ex) {
        MsgTrash.out(ex);
      }
    });
  }
  
  

  public Integer[] getDealIdsToDispatch() {
    return dealIdsToDispatch;
  }

  public Integer[] getDealIdsToPay() {
    return dealIdsToPay;
  }

  public Integer[] getDealIdsToStart() {
    return dealIdsToStart;
  }
  
  
  
  
  
  
  
  
  
  private void showMenu() {
    start.setEnabled(false);
    dispatch.setEnabled(false);
    pay.setEnabled(false);
    spot.setEnabled(false);
    dealIdsToStart    = new Integer[0];
    dealIdsToDispatch = new Integer[0];
    dealIdsToPay      = new Integer[0];
    if(dealIds != null && dealIds.length > 0) {
      try {
        List<List> data = ObjectLoader.executeQuery("SELECT "
                + "id, "
                + "getDispatchCount(id),"
                + "getStartCount(id),"
                + "getDealPositionCount(id),"
                + "getPaymentAmountPercent(id) "
                + " FROM [Deal] WHERE [Deal(id)]=ANY(?)", true, new Object[]{dealIds});
        
        System.out.println("data = "+data);
        
        boolean spotEnable = true;
        for(List d:data) {
          BigDecimal dispatchCount         = (BigDecimal) d.get(1);
          BigDecimal startCount            = (BigDecimal) d.get(2);
          BigDecimal dealPositionCount     = (BigDecimal) d.get(3);
          BigDecimal paymentAmountPercent  = (BigDecimal) d.get(4);
          
          if(dealPositionCount.compareTo(startCount) != 0) {
            spotEnable = false;
            dealIdsToStart = (Integer[])ArrayUtils.add(dealIdsToStart, d.get(0));
          }
          
          if(dealPositionCount.compareTo(dispatchCount) != 0 && startCount.compareTo(BigDecimal.ZERO
          ) > 0) {
            spotEnable = false;
            dealIdsToDispatch = (Integer[]) ArrayUtils.add(dealIdsToDispatch, d.get(0));
          }
          
          if(paymentAmountPercent.compareTo(new BigDecimal(100)) < 0) {
            spotEnable = false;
            dealIdsToPay = (Integer[]) ArrayUtils.add(dealIdsToPay, d.get(0));
          }
        }
        
        start.setEnabled(dealIdsToStart.length > 0);
        dispatch.setEnabled(dealIdsToDispatch.length > 0);
        pay.setEnabled(dealIdsToPay.length > 0);
        
        spot.setEnabled(spotEnable);
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }
  
  private void removeActions() {
    int[] rows = table.getSelectedRows();
    if(JOptionPane.showConfirmDialog(
            null,
            "Вы действительно хотите удалить выделенны"+(rows.length>1?"е":"й")+" событи"+(rows.length>1?"я":"е")+"?",
            "Отправка в архив",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE) == 0) {
      Integer[] startDealPositions     = new Integer[0];
      Integer[] dispatchDealPositions  = new Integer[0];
      Integer[] dealPayments           = new Integer[0];
      Integer[] payments       = new Integer[0];
      ActionType[] actionTypes = new ActionType[0];
      ;
      for(int r:rows) {
        DealActionID id = (DealActionID) table.getValueAt(r, table.convertColumnIndexToView(0));
        if(!ArrayUtils.contains(actionTypes, id.getType()))
          actionTypes = (ActionType[]) ArrayUtils.add(actionTypes, id.getType());
        switch(id.getType()) {
          case СТАРТ:
            startDealPositions    = (Integer[]) ArrayUtils.addAll(startDealPositions, id.getActions());
            break;
          case ОТГРУЗКА:
            dispatchDealPositions = (Integer[]) ArrayUtils.addAll(dispatchDealPositions, id.getActions());
            break;
          case ОПЛАТА:
            dealPayments  = (Integer[]) ArrayUtils.addAll(dealPayments, id.getActions());
            payments      = (Integer[]) ArrayUtils.add(payments, id.getId().intValue());
            break;
        }
      }

      RemoteSession session = null;
      try {
        Integer[] dealPositions = (Integer[]) ArrayUtils.addAll(startDealPositions, dispatchDealPositions);
        if(dealPositions.length > 0 || dealPayments.length > 0) {
          List<Integer> deals = new ArrayList<>();
          session = ObjectLoader.createSession();
          if(payments.length > 0 && dealPayments.length > 0) {
            session.executeQuery("SELECT [DealPayment(deal)] FROM [DealPayment] WHERE id=ANY(?)", 
                  new Object[]{dealPayments}).stream().forEach(d -> deals.add((Integer)d.get(0)));
            
            session.executeUpdate("DELETE FROM [DealPayment] WHERE [DealPayment(id)]=ANY(?)", new Object[]{dealPayments});
            
            //for(Integer p:payments)
              //ActionUtil.replaceDocuments(ActionUtil.getDocuments(null, p, session, ActionType.ОПЛАТА), session);
            
            List<List> data = session.executeQuery("SELECT DISTINCT [Payment(id)] FROM [Payment] WHERE [Payment(id)]=ANY(?) AND "
                    + "(SELECT count(id) FROM [DealPayment] WHERE [DealPayment(payment)]=[Payment(id)])=0", new Object[]{payments});
            if(!data.isEmpty() && JOptionPane.showConfirmDialog(null, data.size()==1?"Удалить платёж?":"Удалить платежи, которые оказались нераспределены?", 
                    "Удаление оплат", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == 0) {
              final List<Integer> ids = new ArrayList<>();
              data.stream().forEach(d -> ids.add((Integer)d.get(0)));
              ActionUtil.removePayment(ids, session);
              session.addEvent(Payment.class, "UPDATE", ids.toArray(new Integer[0]));
            }
            session.addEvent(Payment.class, "UPDATE", payments);//////???????????????  не двоится  ????
          }
          if(startDealPositions.length > 0)
            ActionUtil.removeStart(startDealPositions, session);
          if(dispatchDealPositions.length > 0)
            division.util.actions.ActionUtil.removeDispatch(dispatchDealPositions);
          
          session.executeQuery("SELECT [DealPosition(deal)] FROM [DealPosition] WHERE id=ANY(?)", 
                  new Object[]{ArrayUtils.addAll(startDealPositions, dispatchDealPositions)}).stream().forEach(d -> deals.add((Integer)d.get(0)));
          
          session.addEvent(Deal.class, "UPDATE", deals.toArray(new Integer[0]));
          session.commit();
        }
      }catch(Exception ex) {
        ObjectLoader.rollBackSession(session);
        Messanger.showErrorMessage(ex);
      }
    }
  }
  
  public void clear() {
    table.getTableModel().clear();
    documents.clear();
    saldo.setText("Сальдо конечное: ");
  }
  
  public void initData() {
    if(isActive()) {
      clear();
      initDataTask = new InitDataTask();
      pool.submit(initDataTask);
    }
  }

  public List getComponentsToStore() {
    split.setName(getClass().getSimpleName()+"_split");
    table.setName(getClass().getSimpleName()+"_table");
    documents.setName(getClass().getSimpleName()+"_"+documents.getName());
    return Arrays.asList(new Object[]{split, table, documents});
  }

  private void initSaldo() {
    BigDecimal saldoAmount = new BigDecimal(0.0);
    for(int i=0;i<table.getRowCount();i++) {
      saldoAmount = saldoAmount.add(((BigDecimal)table.getValueAt(i, table.convertColumnIndexToView(3))).subtract((BigDecimal)table.getValueAt(i, table.convertColumnIndexToView(4))));
    }
    saldo.setText("Сальдо конечное: "+saldoAmount);
  }
  
  public void dispose() {
    documents.dispose();
  }
  
  private synchronized void initDocuments() {
    int[] rows = table.getSelectedRows();
    if(rows.length == 0)
      for(int i=0;i<table.getRowCount();i++)
        rows = ArrayUtils.add(rows, i);

    ActionType[] types         = new ActionType[0];
    Integer[]    dealPositions = new Integer[0];
    Integer[]    payments      = new Integer[0];
    
    for(int row:rows) {
      DealActionID dealActionID = (DealActionID) table.getValueAt(row, table.convertColumnIndexToView(0));
      if(dealActionID.getType() == ActionType.ОПЛАТА)
        payments = (Integer[]) ArrayUtils.add(payments, dealActionID.getId().intValue());
      if(!ArrayUtils.contains(types, dealActionID.getType()))
        types = (ActionType[]) ArrayUtils.add(types, dealActionID.getType());
      dealPositions = (Integer[]) ArrayUtils.addAll(dealPositions, dealActionID.getActions());
    }

    List<Integer> ids = new ArrayList<>();
    if(dealPositions.length > 0) {
      ObjectLoader.executeQuery("SELECT [CreatedDocument(dealPositions):object] FROM [CreatedDocument(dealPositions):table] "
              + "WHERE [CreatedDocument(dealPositions):target]=ANY(?)", new Object[]{dealPositions}).stream().forEach(d -> ids.add((Integer)d.get(0)));
      ObjectLoader.executeQuery("SELECT [CreatedDocument(id)] FROM [CreatedDocument] "
              + "WHERE [CreatedDocument(payment)]=ANY(?)", new Object[]{payments}).stream().forEach(d -> ids.add((Integer)d.get(0)));
    }
    if(!ids.isEmpty())
      documents.getClientFilter().clear().AND_IN("id", ids.toArray(new Integer[0])).AND_IN("actionType", types);
    else documents.getClientFilter().clear().AND_EQUAL("id", null);
    documents.initData();
  }

  @Override
  public void objectPeriodDoubleClicked(ObjectPeriod period) {
  }

  @Override
  public void objectPeriodsSelected(List<ObjectPeriod> periods) {
    dealIds = null;
    clear();
    periods.stream().forEach(p -> dealIds = (Integer[]) ArrayUtils.add(dealIds, p.getId()));
    addToolButton.setEnabled(isEnabled() && dealIds != null && dealIds.length > 0);
    removeToolButton.setEnabled(table.getSelectedRows().length > 0);
    initData();
  }

  @Override
  public void dayDoubleClicked(int rowIndex, Date day) {
  }

  @Override
  public void daysSelected(TreeMap<Integer, List<Date>> days) {
    dealIds = null;
    addToolButton.setEnabled(dealIds != null && dealIds.length > 0);
    removeToolButton.setEnabled(table.getSelectedRows().length > 0);
    clear();
  }

  @Override
  public void dayWidthChanged(int dayWidth) {
  }
  
  class InitDataTask implements Runnable {
    @Override
    public void run() {
      if(dealIds != null && dealIds.length > 0) {
        EditorController.waitCursor();
        try {
          //Получаем старты
          String[] querys = new String[3];
          Object[][] params = new Object[3][];
          querys[0] = "SELECT [DealPosition(id)],[DealPosition(startDate)],[DealPosition(startId)] "
                  + "FROM [DealPosition] WHERE [DealPosition(startDate)] NOTNULL AND tmp=false AND type='CURRENT' "
                  +(dealPositionFiter.getValue() != null && dealPositionFiter.getValue().length > 0 ? "AND id=ANY(?) " : "AND [DealPosition(deal)]=ANY(?) ")
                  + "ORDER BY [DealPosition(startId)]";
          if(dealPositionFiter.getValue() != null && dealPositionFiter.getValue().length > 0)
            params[0] = ArrayUtils.add(params[0], dealPositionFiter.getValue());
          else params[0] = ArrayUtils.add(params[0], dealIds);
          //Получаем отгрузки
          querys[1] = "SELECT [DealPosition(id)],[DealPosition(dispatchDate)],[DealPosition(dispatchId)],[DealPosition(cost)] "
                  + "FROM [DealPosition] WHERE tmp=false AND type='CURRENT' AND [DealPosition(dispatchDate)] NOTNULL "
                  +(dealPositionFiter.getValue() != null && dealPositionFiter.getValue().length > 0 ? "AND id=ANY(?) " : "AND [DealPosition(deal)]=ANY(?) ")
                  + "ORDER BY [DealPosition(dispatchId)]";
          if(dealPositionFiter.getValue() != null && dealPositionFiter.getValue().length > 0)
            params[1] = ArrayUtils.add(params[1], dealPositionFiter.getValue());
          else params[1] = ArrayUtils.add(params[1], dealIds);
          //params[1][0] = dealIds;
          //Получаем оплаты
          querys[2] = "SELECT [DealPayment(id)], [DealPayment(amount)], [DealPayment(date)], [DealPayment(payment)] "
                  + "FROM [DealPayment] WHERE tmp=false AND type='CURRENT' AND "
                  +(dealPositionFiter.getValue() != null && dealPositionFiter.getValue().length > 0 ? "[DealPayment(deal)] in (SELECT [DealPosition(deal)] FROM [DealPosition] where id=ANY(?)) " : "[DealPayment(deal)]=ANY(?)");
          if(dealPositionFiter.getValue() != null && dealPositionFiter.getValue().length > 0)
            params[2] = ArrayUtils.add(params[2], dealPositionFiter.getValue());
          else params[2] = ArrayUtils.add(params[2], dealIds);
          //params[2][0] = dealIds;

          List<List<List>> datas = ObjectLoader.executeQuery(querys, params, true);
          final List<List> startData    = datas.get(0);
          final List<List> dispatchData = datas.get(1);
          final List<List> payData      = datas.get(2);

          final Vector<Vector> dataVector = new Vector<>();

          startData.stream().forEach(d -> {
            Vector row = new Vector();
            for(Vector r:dataVector) {
              if(((DealActionID)r.get(0)).getId().equals(d.get(2)) && ((DealActionID)r.get(0)).getType() == ActionType.СТАРТ) {
                row = r;
                break;
              }
            }
            if(!row.isEmpty())
              ((DealActionID)row.get(0)).addAction((Integer) d.get(0));
            else {
              DealActionID id = new DealActionID((Long) d.get(2), ActionType.СТАРТ);
              id.addAction((Integer) d.get(0));
              dataVector.add(new Vector(Arrays.asList(new Object[]{id, "Старт", (Timestamp) d.get(1), BigDecimal.ZERO, BigDecimal.ZERO})));
            }
          });

          dispatchData.stream().forEach(d -> {
            Vector row = new Vector();
            for(Vector r:dataVector) {
              if(((DealActionID)r.get(0)).getId().equals(d.get(2)) && ((DealActionID)r.get(0)).getType() == ActionType.ОТГРУЗКА) {
                row = r;
                break;
              }
            }
            if(!row.isEmpty()) {
              ((DealActionID)row.get(0)).addAction((Integer) d.get(0));
              row.set(4, ((BigDecimal)row.get(4)).add((BigDecimal) d.get(3)));
            }else {
              DealActionID id = new DealActionID((Long) d.get(2), ActionType.ОТГРУЗКА);
              id.addAction((Integer) d.get(0));
              dataVector.add(new Vector(Arrays.asList(new Object[]{id, "Отгрузка", (Timestamp) d.get(1), BigDecimal.ZERO, (BigDecimal) d.get(3)})));
            }
          });
          
          payData.stream().forEach(d -> {
            Vector row = new Vector();
            for(Vector r:dataVector) {
              if(((DealActionID)r.get(0)).getId().intValue() == ((Integer)d.get(3)).intValue() && ((DealActionID)r.get(0)).getType() == ActionType.ОПЛАТА) {
                row = r;
                break;
              }
            }
            if(!row.isEmpty()) {
              ((DealActionID)row.get(0)).addAction((Integer) d.get(0));
              row.set(3, ((BigDecimal)row.get(3)).add((BigDecimal) d.get(1)));
            }else {
              DealActionID id = new DealActionID(((Integer) d.get(3)).longValue(), ActionType.ОПЛАТА);
              id.addAction((Integer) d.get(0));
              dataVector.add(new Vector(Arrays.asList(new Object[]{id, "Оплата", (Timestamp) d.get(2), (BigDecimal) d.get(1), BigDecimal.ZERO})));
            }
          });
          
          SwingUtilities.invokeLater(() -> {
            clear();
            table.getTableModel().getDataVector().addAll(dataVector);
            table.getTableModel().fireTableDataChanged();
            initSaldo();
            initDocuments();
          });          
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }finally {
          EditorController.defaultCursor();
        }
      }
      
      
      /*if(dealIds.length > 0) {
        EditorController.waitCursor();
        
        try {
          startData.stream().forEach(d -> {
            Vector row = null;
            for(Vector r:dataVector) {
              if(((DealActionID)r.get(0)).getId().equals(d.get(3)) && ((DealActionID)r.get(0)).getType() == ActionType.СТАРТ) {
                row = r;
                break;
              }
            }
            if(row != null)
              ((DealActionID)row.get(0)).addDeal((Integer) d.get(1));
            else {
              DealActionID id = new DealActionID((Long) d.get(3), ActionType.СТАРТ, new Integer[]{(Integer) d.get(0)}, new Integer[0]);
              dataVector.add(new Vector(Arrays.asList(new Object[]{id, "Старт", (Timestamp) d.get(2), BigDecimal.ZERO, BigDecimal.ZERO})));
            }
          });

          dispatchData.stream().forEach(d -> {
            Vector row = null;
            for(Vector r:dataVector) {
              if(((DealActionID)r.get(0)).getId().equals(d.get(3)) && ((DealActionID)r.get(0)).getType() == ActionType.ОТГРУЗКА) {
                row = r;
                break;
              }
            }
            if(row != null) {
              ((DealActionID)row.get(0)).addDeal((Integer) d.get(1));
              row.set(4, ((BigDecimal)row.get(4)).add((BigDecimal) d.get(4)));
            }else {
              DealActionID id = new DealActionID((Long) d.get(3), ActionType.ОТГРУЗКА, new Integer[]{(Integer) d.get(0)}, new Integer[0]);
              dataVector.add(new Vector(Arrays.asList(new Object[]{id, "Отгрузка", (Timestamp) d.get(2), BigDecimal.ZERO, (BigDecimal) d.get(4)})));
            }
          });

          payData.stream().forEach(d -> {
            Vector row = null;
            for(Vector r:dataVector) {
              if(((DealActionID)r.get(0)).getId().equals(d.get(3)) && ((DealActionID)r.get(0)).getType() == ActionType.ОПЛАТА) {
                row = r;
                break;
              }
            }
            if(row != null) {
              //((DealActionID)row.get(0)).addDealPositions((Integer[]) d.get(0));
              ((DealActionID)row.get(0)).addDealPayment((Integer) d.get(4));
              row.set(3, ((BigDecimal)row.get(3)).add((BigDecimal) d.get(1)));
            }else {
              DealActionID id = new DealActionID(((Integer) d.get(3)).longValue(), ActionType.ОПЛАТА, (Integer[]) d.get(0), new Integer[]{(Integer) d.get(4)});
              dataVector.add(new Vector(Arrays.asList(new Object[]{id, "Оплата", (Timestamp) d.get(2), (BigDecimal) d.get(1), BigDecimal.ZERO})));
            }
          });
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }

        SwingUtilities.invokeLater(() -> {
          clear();
          table.getTableModel().getDataVector().addAll(dataVector);
          table.getTableModel().fireTableDataChanged();
          initSaldo();
          initDocuments();
          EditorController.defaultCursor();
        });
      }*/
    }
  }
  
  class DealActionID {
    private final Long    id;
    private final ActionType type;
    private Integer[]  actions;

    public DealActionID(Long id, ActionType type) {
      this.id = id;
      this.type = type;
    }

    public Long getId() {
      return id;
    }

    public ActionType getType() {
      return type;
    }

    public Integer[] getActions() {
      return actions;
    }
    
    public void addAction(Integer id) {
      actions = (Integer[]) ArrayUtils.add(actions, id);
    }
    
    public void addActions(Integer[] ids) {
      actions = (Integer[]) ArrayUtils.addAll(actions, ids);
    }
  }
}