package division.fx.controller.payment;

import division.fx.controller.DivisionController;
import division.fx.table.Pojo;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class PaymentTableController extends DivisionController {
  @FXML private TableView<PaymentRow> tableFinishPayments;
  
  @FXML private TableColumn docColumn;
  @FXML private TableColumn bankDebetColumn;
  @FXML private TableColumn bankCreditColumn;
  @FXML private TableColumn bankNeraspColumn;
  
  @FXML private TableColumn kassaDebetColumn;
  @FXML private TableColumn kassaCreditColumn;
  @FXML private TableColumn kassaNeraspColumn;
  
  @FXML private TableColumn companyNameColumn;
  
  public PaymentTableController() {
    super("fx/fxml/payment-table.fxml");
  }
  
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    /*bindColumnWidth(".0-col-label", docColumn);
    
    bindColumnWidth(".1-col-label", bankDebetColumn);
    bindColspanColumnWidth(".1-2-col-label", bankDebetColumn, bankCreditColumn);
    bindColumnWidth(".2-col-label", bankCreditColumn);
    bindColumnWidth(".3-col-label", bankNeraspColumn);
    
    bindColumnWidth(".4-col-label", kassaDebetColumn);
    bindColspanColumnWidth(".4-5-col-label", kassaDebetColumn, kassaCreditColumn);
    bindColumnWidth(".5-col-label", kassaCreditColumn);
    bindColumnWidth(".6-col-label", kassaNeraspColumn);
    
    bindColumnWidth(".7-col-label", companyNameColumn);*/
    
    /*root.lookupAll(".finish-table-column").stream().forEach(node -> {
      System.out.println(node);
    });*/
  }
  
  /*private void bindColspanColumnWidth(String selector, TableColumn... columns) {
    for(TableColumn column:columns) {
      column.widthProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
        double width = 0;
        for(TableColumn col:columns)
          width += col.widthProperty().get();
        for(Node node:root.lookupAll(selector)) {
          ((Label)node).minWidthProperty().set(width);
          ((Label)node).maxWidthProperty().set(width);
          ((Label)node).prefWidthProperty().set(width);
        }
      });
    }
  }
  
  private void bindColumnWidth(String selector, TableColumn column) {
    column.
    root.lookupAll(selector).stream().forEach(node -> {
      double x = 0;
      for(TableColumn col:tableFinishPayments.getColumns())
        x += col.getWidth();
      ((Label)node).setLayoutX(x);
      ((Label)node).minWidthProperty().bind(column.widthProperty());
      ((Label)node).maxWidthProperty().bind(column.widthProperty());
      ((Label)node).prefWidthProperty().bind(column.widthProperty());
    });
  }*/
  
  class PaymentRow extends Pojo {

    public PaymentRow(Integer id, String name) {
      super(id, name);
    }
  }
}