package division.fx.client.plugins;

import bum.interfaces.Payment;
import division.fx.PropertyMap;
import division.fx.editor.FXTableEditor;
import division.fx.table.Column;
import division.fx.table.filter.TextFilter;
import division.fx.FXUtility;
import javafx.collections.ListChangeListener;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.math.*;
import javafx.collections.FXCollections;

public class FinishPaymentTable extends FXTableEditor {
  private final Label oborotiTextLabel = new Label("Обороты за период");
  private final Label sumDebetLabel    = new Label();
  private final Label sumCreditLabel   = new Label();
  private final HBox  oborotiRow       = new HBox(oborotiTextLabel, sumDebetLabel, sumCreditLabel);
  
  private final Label saldoTextLabel   = new Label("Сальдо за период");
  private final Label saldoLabel       = new Label();
  private final HBox  saldoRow         = new HBox(saldoTextLabel, saldoLabel);
  
  private final Label inputTextLabel   = new Label("Входящий остаток");
  private final Label inputLabel       = new Label();
  private final HBox  inputRow         = new HBox(inputTextLabel, inputLabel);
  
  private final Label outputTextLabel  = new Label("Исходящий остаток");
  private final Label outputLabel      = new Label();
  private final HBox  outputRow        = new HBox(outputTextLabel, outputLabel);
  
  private VBox  bottomPane       = new VBox(oborotiRow, saldoRow, inputRow, outputRow);

  public FinishPaymentTable() {
    super(Payment.class, null,
            FXCollections.observableArrayList("id"),
            Column.create("Дата операции", /*"date", */"operationDate"),
            Column.create("Документ", 
                    /*"document",*/
                    "query:select array_to_string(array_agg([CreatedDocument(document_name)]||' № '||[CreatedDocument(number)]), ', ') from [CreatedDocument] "
                            + "where [CreatedDocument(document-system)]=true and [CreatedDocument(payment)]=[Payment(id)]", 
                    new TextFilter("Документ")),
            Column.create("Корреспондент", /*"customer", */
                    "query:select name from [Company] where [Company(id)]=(select [CompanyPartition(company)] from [CompanyPartition] "
                            + "where [CompanyPartition(id)]=[Payment(customerCompanyPartition)])", 
                    new TextFilter("Корреспондент")),
            Column.create("Депозитарий", /*"store",*/ "seller-store-name"),
            Column.create("Дебет", /*"debet", */"amount"),
            Column.create("Кредит", /*"credit", */"amount"));
    
    FXUtility.initCss(this);
    
    oborotiTextLabel.prefWidthProperty().bind(
                    getColumn("Дата операции").widthProperty()
                    .add(getColumn("Документ").widthProperty())
                    .add(getColumn("Корреспондент").widthProperty())
                    .add(getColumn("Депозитарий").widthProperty()));
    saldoTextLabel.prefWidthProperty().bind(oborotiTextLabel.widthProperty());
    inputTextLabel.prefWidthProperty().bind(oborotiTextLabel.widthProperty());
    outputTextLabel.prefWidthProperty().bind(oborotiTextLabel.widthProperty());
    
    sumDebetLabel.prefWidthProperty().bind(getColumn("Дебет").widthProperty());
    sumCreditLabel.prefWidthProperty().bind(getColumn("Кредит").widthProperty());
    
    saldoLabel.prefWidthProperty().bind(sumDebetLabel.widthProperty().add(sumCreditLabel.widthProperty()));
    inputLabel.prefWidthProperty().bind(saldoLabel.widthProperty());
    outputLabel.prefWidthProperty().bind(saldoLabel.widthProperty());
    
    setBottom(bottomPane);
    
    getTable().getItems().addListener((ListChangeListener.Change<? extends PropertyMap> c) -> {
      BigDecimal debet  = BigDecimal.ZERO;
      BigDecimal credit = BigDecimal.ZERO;
      for(PropertyMap p:getTable().getItems()) {
        debet  = debet.add(p.getValue("Дебет") == null ? BigDecimal.ZERO : (BigDecimal)p.getValue("Дебет"));
        credit = credit.add(p.getValue("Кредит") == null ? BigDecimal.ZERO : (BigDecimal)p.getValue("Кредит"));
      }
      
      sumDebetLabel.setText(debet.toPlainString());
      sumCreditLabel.setText(credit.toPlainString());
      saldoLabel.setText(debet.subtract(credit).toPlainString());
    });
  }

  @Override
  public PropertyMap create() {
    return new PaymentPojo();
  }
}
