package division.fx.client.plugins;

import bum.interfaces.Payment;
import division.fx.PropertyMap;
import division.fx.table.Column;
import division.fx.editor.FXTableEditor;
import division.fx.table.FXDivisionTableCell;
import division.fx.table.filter.TextFilter;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import util.filter.local.DBFilter;

public class Payments extends FXPlugin {
  private final TabPane tab              = new TabPane();
  private final Tab     finishPaymentTab = new Tab("Исполненные");
  private final Tab     paymentTab       = new Tab("Поручения");
  private final FinishPaymentTable finishPaymentTable = new FinishPaymentTable();
  private FXTableEditor paymentTable = new FXTableEditor(Payment.class, 
          null,
          FXCollections.observableArrayList("id"),
          Column.create("Дата", /*"date",*/ "operationDate"),
          Column.create("Платёжное\nпоручение №", 
                  /*"document",*/
                  "query:select array_to_string(array_agg([CreatedDocument(number)]), ', ') from [CreatedDocument] "
                          + "where [CreatedDocument(document-system)]=true and [CreatedDocument(payment)]=[Payment(id)]", 
                  new TextFilter("Платёжное\nпоручение №")),
          
          Column.create("Сумма платежа\nбез НДС", "amount"),
          Column.create("НДС, руб.", "amount"),
          Column.create("ИТОГО\nс НДС", "amount"),
          
          Column.create("Коррес-\nпондент", /*"customer", */
                  "query:select name from [Company] where [Company(id)]=(select [CompanyPartition(company)] from [CompanyPartition] "
                          + "where [CompanyPartition(id)]=[Payment(customerCompanyPartition)])", 
                  new TextFilter("Коррес-\nпондент")),
          Column.create("Исполнение").addColumn("?", /*"toBank", */"query:[Payment(state)]='ПОДГОТОВКА'", true, true)
                  .addColumn("$", /*"toClientBank", */"query:[Payment(state)]='ОТПРАВЛЕНО'", true, true)
                  .addColumn("Ok", /*"finish", */"query:[Payment(state)]='ИСПОЛНЕНО'", true, true)
  ) {
    @Override
    public PropertyMap create() {
      return new PaymentPojo();
    }
  };
  
  private final DBFilter companyFilter = paymentTable.getClientFilter().AND_FILTER();
  private final DBFilter fCompanyFilter = finishPaymentTable.getClientFilter().AND_FILTER();
  
  @Override
  public void changeCompany(Integer[] ids, boolean init) {
    companyFilter.clear();
    fCompanyFilter.clear();
    if(ids != null && ids.length > 0) {
      companyFilter.AND_IN("sellerCompany", ids).OR_IN("customerCompany", ids);
      fCompanyFilter.AND_IN("sellerCompany", ids).OR_IN("customerCompany", ids);
    }
    if(init)
      ((FXTableEditor)tab.getSelectionModel().getSelectedItem().getContent()).initData();
  }

  @Override
  public void start() {
    finishPaymentTab.setContent(finishPaymentTable);
    paymentTab.setContent(paymentTable);
    
    tab.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    
    tab.getSelectionModel().selectedItemProperty().addListener((ob, ol, nw) -> ((FXTableEditor)nw.getContent()).initData());
    tab.getTabs().addAll(finishPaymentTab, paymentTab);
    
    paymentTable.getColumn("Ok").setCellFactory(param -> new FXDivisionTableCell() {
      @Override
      protected void updateItem(Object item, boolean empty) {
        super.updateItem(item, empty);
        if(!empty && getTableRow().getItem() != null)
          getTableRow().pseudoClassStateChanged(PseudoClass.getPseudoClass("finish"), (boolean) ((PaymentPojo)getTableRow().getItem()).getValue("Ok"));
      }
    });
    
    show("Платежи");
  }

  @Override
  public void dispose() {
    finishPaymentTable.dispose();
    paymentTable.dispose();
    super.dispose();
  }

  @Override
  public Node getContent() {
    return tab;
  }
}