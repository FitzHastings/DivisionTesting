package division.util.actions;

import division.fx.PropertyMap;
import division.fx.table.Column;
import division.fx.table.DivisionTableCell;
import division.fx.table.FXDivisionTable;
import division.fx.FXUtility;
import java.time.LocalDate;
import java.util.Map;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.DatePicker;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;

public class DocumentDateChooser_old extends Stage/* extends JDialog */{
  private Scene scene;
  private VBox root;
  
  private Column<PropertyMap,String>   splitColumn      = Column.create("split","split",true,true);
  private Column<PropertyMap,Object>   dateColumn       = Column.create("customDate","customDate");
  private Column<PropertyMap,String>   numberColumn     = Column.create("number","number",true,true);
  
  private FXDivisionTable<PropertyMap> table = new FXDivisionTable<>(
          Column.create("Документ", "name"),
          splitColumn,
          dateColumn,
          numberColumn
  );
  
  final String split    = "Объединённый документ";
  final String notSplit = "Раздельно по каждой сделке";
  
  private final SplitMenuButton defSplit = new SplitMenuButton(
          new MenuItem(notSplit), 
          new MenuItem(split));
  
  private DatePicker     defDatePicker = new DatePicker(LocalDate.now());
  private final SplitMenuButton defDate  = new SplitMenuButton(
          new MenuItem("Сегодня"), 
          new MenuItem("Начало периода"), 
          new MenuItem("Конец периода"), 
          new CustomMenuItem(defDatePicker, true));
  
  
  private Map<Integer,Map> documents;
  private boolean returnValue = false;

  public DocumentDateChooser_old(Map<Integer,Map> documents, boolean splitFunction) {
    setTitle("Создание документов...");
    this.documents = documents;

    initComponents();
    initEvents();
    
    splitColumn.setVisible(splitFunction);
    scene.setCursor(javafx.scene.Cursor.WAIT);
    
    documents.values().stream().forEach(doc -> {
      table.getSourceItems().add(PropertyMap.copy(doc)
              .setValue("split", doc.get("split") == null || !(boolean)doc.get("split") ? notSplit : split)
              .setValue("customDate", doc.get("customDate") == null ? LocalDate.now() : doc.get("customDate") instanceof Boolean ? (boolean)doc.get("customDate") ? "Начало периода" : "Конец периода" : doc.get("customDate"))
              .setValue("number", doc.get("auto") != null ? (boolean)doc.get("auto")?"auto...":"" : ""));
    });
    scene.setCursor(javafx.scene.Cursor.DEFAULT);
  }
  
  /*public boolean get() {
    return get(false);
  }*/
  
  public boolean get(/*boolean fx*/) {
    setScene(scene);
    root.setPrefSize(750, 300);
    showAndWait();
    return returnValue;
  }
  
  private void initComponents() {
    defSplit.setText("Формат");
    defSplit.setMaxWidth(Double.MAX_VALUE);
    defSplit.setStyle("-fx-border-width: 0");
    
    splitColumn.setGraphic(defSplit);
    splitColumn.setText("");
    splitColumn.setCellFactory((TableColumn<PropertyMap, String> param) -> new DivisionTableCell("Объединённый документ", "Раздельно по каждой сделке"));
    
    defDatePicker.setEditable(false);
    defDatePicker.setVisible(false);
    defDatePicker.setMinSize(0, 0);
    defDatePicker.setMaxSize(0, 0);
    defDatePicker.setPrefSize(0, 0);
    
    defDate.setText("Дата документа");
    defDate.setMaxWidth(Double.MAX_VALUE);
    defDate.setStyle("-fx-border-width: 0");

    dateColumn.setGraphic(defDate);
    dateColumn.setText("");
    dateColumn.setCellFactory((TableColumn<PropertyMap, Object> param) -> {
      DivisionTableCell cell =  new DivisionTableCell();
      cell.setOnMouseClicked((MouseEvent event) -> {
        if(event.getClickCount() == 1 && cell.getTableRow().getItem() != null) {
          DatePicker datePicker = new DatePicker(LocalDate.now());
          
          if(cell.getItem() instanceof LocalDate)
            datePicker.setValue((LocalDate)cell.getItem());
          
          Popup pop = new Popup();
          Button now   = new Button("Сегодня");
          Button start = new Button("Начало периода");
          Button end   = new Button("Конец периода");
          
          now.getStyleClass().add("menuButton");
          start.getStyleClass().add("menuButton");
          end.getStyleClass().add("menuButton");
          
          now.setOnAction((ActionEvent e)   -> {cell.commitEdit(LocalDate.now());pop.hide();});
          start.setOnAction((ActionEvent e) -> {cell.commitEdit("Начало периода");pop.hide();});
          end.setOnAction((ActionEvent e)   -> {cell.commitEdit("Конец периода");pop.hide();});
          datePicker.valueProperty().addListener((ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) -> {
            cell.commitEdit(newValue);
            pop.hide();
          });
          VBox menu = new VBox(now, start, end, datePicker);
          menu.getStyleClass().add("division-menu");
          pop.getContent().add(menu);
          
          pop.setAutoFix(true);
          pop.setAutoHide(true);
          pop.setHideOnEscape(true);
          
          Point2D p = cell.localToScreen(0, cell.getHeight());
          pop.show(cell, p.getX(), p.getY());
          datePicker.show();
        }
      });
      
      return cell;
    });
    
    numberColumn.setCellFactory((TableColumn<PropertyMap, String> param) -> new TextNumberCell("number") {
      @Override
      public boolean isCustomEditable(Object row) {
        return !getText().equals("auto...");
      }
    });
    
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    table.setStyle("-fx-cell-size: 50px;");
    table.setEditable(true);
    
    Button ok = new Button("Ok");
    ok.setOnAction((ActionEvent event) -> {
      table.getSourceItems().stream().forEach(it -> {
        documents.get(it.getValue("id")).put("split",         it.getValue("split").equals(split));
        documents.get(it.getValue("id")).put("customDate",    it.getValue("customDate") instanceof LocalDate ? it.getValue("customDate") : it.getValue("customDate").equals("Начало периода"));
        documents.get(it.getValue("id")).put("number",        it.getValue("number"));
      });
      
      boolean is = true;
      for(Integer id:documents.keySet()) {
        if(!(boolean)documents.get(id).get("auto") && (documents.get(id).get("number")==null || documents.get(id).get("number").equals(""))) {
          is = false;
          break;
        }
      }
      if(is) {
        returnValue = true;
        close();
      }else {
        new Alert(Alert.AlertType.ERROR, "Введите номер документа", ButtonType.OK).show();
      }
    });
    
    root = new VBox(5, table, ok);
    root.setAlignment(Pos.BOTTOM_RIGHT);
    root.setPadding(new Insets(10, 10, 10, 10));
    root.setVgrow(table, Priority.ALWAYS);
    root.setFillWidth(true);
    
    scene = new Scene(root);
    scene.getStylesheets().add(FXUtility.getResouce("fx/css/document-date-chooser.css").toExternalForm());
    setScene(scene);
    
    ok.requestFocus();
  }
  
  private void initEvents() {
    defSplit.getItems().stream().forEach((MenuItem item) -> item.setOnAction((ActionEvent event) -> {
      table.getSourceItems().stream().forEach(doc -> doc.setValue("split", item.getText()));
    }));
    
    defDate.showingProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(newValue)
        defDatePicker.show();
    });
    
    defDatePicker.valueProperty().addListener((ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) -> {
      table.getSourceItems().stream().forEach(doc -> doc.setValue("customDate", newValue));
    });
    
    defDate.getItems().stream().forEach((MenuItem item) -> item.setOnAction((ActionEvent e) -> {
      if(item.getText() != null) {
        switch(item.getText()) {
          case "Сегодня":
            table.getSourceItems().stream().forEach(doc -> doc.setValue("customDate", LocalDate.now()));
            break;
          case "Начало периода":
            table.getSourceItems().stream().forEach(doc -> doc.setValue("customDate", "Начало периода"));
            break;
          case "Конец периода":
            table.getSourceItems().stream().forEach(doc -> doc.setValue("customDate", "Конец периода"));
            break;
        }
      }
    }));
  }
}