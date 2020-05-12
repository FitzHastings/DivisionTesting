package division.fx.client;

public class test {
  /*private TabPane tab              = new TabPane();
  private Tab     finishPaymentTab = new Tab("Исполненные");
  private Tab     paymentTab       = new Tab("Поручения");
  
  private FXTableEditor<PaymentPojo> paymentTable = new FXTableEditor(Payment.class, 
          PaymentPojo.class, 
          Column.create("id", "id", false),
          Column.create("Дата", "date", "operationDate", new ColumnFilter(Date.class)),
          Column.create("Платёжное\nпоручение №", 
                  "document",
                  "query:select array_to_string(array_agg([CreatedDocument(number)]), ', ') from [CreatedDocument] "
                          + "where [CreatedDocument(document-system)]=true and [CreatedDocument(payment)]=[Payment(id)]", 
                  new ColumnFilter(String.class)),
          
          Column.create("Сумма платежа\nбез НДС", "amount", new ColumnFilter(BigDecimal.class)),
          Column.create("НДС, руб.", "amount", new ColumnFilter(BigDecimal.class)),
          Column.create("ИТОГО\nс НДС", "amount", new ColumnFilter(BigDecimal.class)),
          
          Column.create("Коррес-\nпондент", "customer", 
                  "query:select name from [Company] where [Company(id)]=(select [CompanyPartition(company)] from [CompanyPartition] "
                          + "where [CompanyPartition(id)]=[Payment(customerCompanyPartition)])", 
                  new ColumnFilter(String.class)),
          Column.create("Исполнение").addColumn("?", "toBank", "query:[Payment(state)]='ПОДГОТОВКА'", true, true)
                  .addColumn("$", "toClientBank", "query:[Payment(state)]='ОТПРАВЛЕНО'", true, true)
                  .addColumn("Ok", "finish", "query:[Payment(state)]='ИСПОЛНЕНО'", true, true)
  );*/
  
  /*private Node rotateButton(Node button, String text, double value) {
    Label  label  = new Label(button.getText());
    button.setText(null);
    label.setRotate(value);
    button.setGraphic(new javafx.scene.Group(label));
    return button;
  }*/
  
  /*finishPaymentTab.setContent(new FinishPaymentTable());
    paymentTab.setContent(paymentTable);
    
    ObjectLoader.clear();
    ObjectLoader.connect();
    
    tab.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) -> {
      ((FXTableEditor)newValue.getContent()).initData();
    });
    
    tab.getTabs().addAll(finishPaymentTab, paymentTab);
    
    FXUtility.initCss(this, paymentTable);

    
    paymentTable.getColumn("Ok").setCellFactory(param -> new DivisionTableCell() {
      @Override
      protected void updateItem(Object item, boolean empty) {
        super.updateItem(item, empty);
        if(!empty && getTableRow().getItem() != null) {
          if(((PaymentPojo)getTableRow().getItem()).finishProperty().get())
            getTableRow().pseudoClassStateChanged(PseudoClass.getPseudoClass("finish"), true);
          else getTableRow().pseudoClassStateChanged(PseudoClass.getPseudoClass("finish"), false);
        }
      }
    });*/
    
    
    
    
    /*TableView t = new TableView();
    
    TableColumn c = new TableColumn("id");
    c.setCellValueFactory(new PropertyValueFactory("id"));
    
    TableColumn c2 = new TableColumn("dfdfd");
    c2.setCellValueFactory(new PropertyValueFactory("name"));
    
    t.getColumns().addAll(c,c2);
    
    //t.getItems().add(new Pojo(1, "sdasd"));
    //t.getItems().add(new Pojo(2, "weffgbsbn"));
    
    t.getItems().add(new row());
    t.getItems().add(new row());
    
    c.setCellFactory((Object param) -> {
      return new DivisionTableCell<Object, Object>() {
        @Override
        protected void updateItem(Object item, boolean empty) {
          super.updateItem(item, empty);
          if(!empty && getTableRow() != null)
            System.out.println(getTableRow().getItem().getClass().getSimpleName());
        }
      };
    });
    
    desktop.add(new FXInternalFrame(new BorderPane(t), "asdfdfsdf", 500, 500));*/
    
    
    
    /*DateScale dateScale = new DateScale();
    
    Calendar c = Calendar.getInstance();
    c.set(2015, 5, 1, 0, 0, 0);
    long start = c.getTimeInMillis();
    
    c.set(2015, 5, 30, 0, 0, 0);
    long end = c.getTimeInMillis();
        
    //dateScale.add(new DatePeriod(0, start, end, Color.GREEN));
    dateScale.add(new DatePeriod(1, start, end, Color.GREEN));
    dateScale.add(new DatePeriod(2, start, end, Color.GREEN));
    dateScale.add(new DatePeriod(3, start, end, Color.GREEN));
    dateScale.add(new DatePeriod(4, start, end, Color.GREEN));
    dateScale.add(new DatePeriod(5, start, end, Color.GREEN));
    dateScale.add(new DatePeriod(6, start, end, Color.GREEN));
    
    c.set(2015, 4, 1, 0, 0, 0);
    start = c.getTimeInMillis();
    
    c.set(2015, 4, 31, 0, 0, 0);
    end = c.getTimeInMillis();
        
    dateScale.add(new DatePeriod(1, start, end, Color.YELLOW));
    
    c.set(2015, 4, 13, 0, 0, 0);
    start = c.getTimeInMillis();
    
    c.set(2015, 5, 2, 0, 0, 0);
    end = c.getTimeInMillis();
    dateScale.add(new DatePeriod(100, start, end, Color.RED));
    
    primaryStage.setScene(new Scene(dateScale, 800, 600));
    primaryStage.show();*/
}