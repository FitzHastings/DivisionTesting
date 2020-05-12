package division.util;

import division.fx.PropertyMap;
import division.fx.table.Column;
import division.fx.table.DivisionTableCell;
import division.fx.table.FXDivisionTable;
import division.fx.FXUtility;
import division.util.actions.TextNumberCell;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;

public class DocumentDateChooser extends Stage/* extends JDialog */{
  private Scene scene;
  private VBox root;
  
  private Column<PropertyMap,String>   splitColumn      = Column.create("","split",true,true);
  private Column<PropertyMap,Object>   dateColumn       = Column.create("","date");
  private Column<PropertyMap,String>   numberColumn     = Column.create("Номер","autonumber",true,true);
  
  private FXDivisionTable<PropertyMap> table = new FXDivisionTable<>(
          Column.create("Документ", "name"),
          splitColumn,
          dateColumn,
          numberColumn
  );
  
  final String split    = "Объединённый документ";
  final String notSplit = "Раздельно по каждой сделке";
  
  private MenuButton defSplit;
  private MenuButton defDate;
  private DatePicker defDatePicker = new DatePicker(LocalDate.now());
  
  
  private Map<Integer,PropertyMap> documents;
  private boolean returnValue = false;

  public DocumentDateChooser(Map<Integer,PropertyMap> documents, boolean splitFunction) {
    setTitle("Создание документов...");
    this.documents = documents;
    
    try {
      defSplit = new MenuButton(
              "формат",
              new ImageView(new Image(new FileInputStream("fx/css/img/format.png"))),
              new MenuItem(notSplit), 
              new MenuItem(split));
      
      defDate  = new MenuButton(
              "дата",
              new ImageView(new Image(new FileInputStream("fx/css/img/calendar.png"))),
              new MenuItem("Сегодня"),
              new MenuItem("Начало периода"),
              new MenuItem("Конец периода"),
              new CustomMenuItem(defDatePicker, true));
    } catch (FileNotFoundException ex) {
      Logger.getLogger(DocumentDateChooser.class.getName()).log(Level.SEVERE, null, ex);
    }

    initComponents();
    initEvents();
    
    splitColumn.setVisible(splitFunction);
    scene.setCursor(javafx.scene.Cursor.WAIT);
    
    documents.values().stream().forEach(doc -> {
      table.getSourceItems().add(PropertyMap.copy(doc)
              .setValue("split", doc.getValue("split") == null || !(boolean)doc.getValue("split") ? notSplit : split)
              .setValue("date", doc.getValue("date") == null ? LocalDate.now() : doc.getValue("date") instanceof Boolean ? (boolean)doc.getValue("date") ? "Начало периода" : "Конец периода" : doc.getValue("date"))
              .setValue("autonumber", doc.getValue("autonumber") != null ? (boolean)doc.getValue("autonumber")?"auto...":"" : ""));
    });
    scene.setCursor(javafx.scene.Cursor.DEFAULT);
  }
  
  public boolean get() {
    setScene(scene);
    root.setPrefSize(750, 300);
    showAndWait();
    return returnValue;
  }
  
  private void initComponents() {
    HBox splitBox = new HBox(defSplit);
    splitBox.setMaxWidth(Double.MAX_VALUE);
    splitColumn.setGraphic(splitBox);
    
    HBox dateBox = new HBox(defDate);
    dateBox.setMaxWidth(Double.MAX_VALUE);
    dateColumn.setGraphic(dateBox);
    
    splitColumn.setCellFactory((TableColumn<PropertyMap, String> param) -> new DivisionTableCell("Объединённый документ", "Раздельно по каждой сделке"));
    
    defDatePicker.setEditable(false);
    defDatePicker.setVisible(false);
    defDatePicker.setMinSize(0, 0);
    defDatePicker.setMaxSize(0, 0);
    defDatePicker.setPrefSize(0, 0);
    
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
          
          now.setOnAction((ActionEvent e)   -> {
            cell.commitEdit(LocalDate.now());pop.hide();
          });
          
          start.setOnAction((ActionEvent e) -> {
            cell.commitEdit("Начало периода");
            pop.hide();
          });
          end.setOnAction((ActionEvent e)   -> {
            cell.commitEdit("Конец периода");
            pop.hide();
          });
          
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
          
          pop.setOnShown(e -> {
            datePicker.show();
          });
          
          Point2D p = cell.localToScreen(0, cell.getHeight());
          pop.show(cell, p.getX(), p.getY());
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
        documents.get(it.getValue("id")).setValue("split",      it.getValue("split").equals(split));
        documents.get(it.getValue("id")).setValue("date",       it.getValue("date") instanceof LocalDate ? it.getValue("date") : it.getValue("date").equals("Начало периода"));
        
        if(!documents.get(it.getValue("id")).is("autonumber"))
          documents.get(it.getValue("id")).setValue("number", it.getValue("autonumber"));
      });
      
      boolean is = true;
      for(Integer id:documents.keySet()) {
        if(!(boolean)documents.get(id).getValue("autonumber",true) && (documents.get(id).getValue("autonumber")==null || documents.get(id).getValue("autonumber").equals(""))) {
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
    defSplit.getItems().stream().forEach((MenuItem item) -> item.setOnAction((ActionEvent event) -> table.getSourceItems().stream().forEach(doc -> doc.setValue("split", item.getText()))));
    
    defDate.setOnMouseClicked(e -> {
      if(defDatePicker.isShowing())
        defDatePicker.hide();
      else defDatePicker.show();
    });
    
    defDatePicker.valueProperty().addListener((ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) -> table.getSourceItems().stream().forEach(doc -> doc.setValue("date", newValue)));
    
    defDate.getItems().stream().forEach((MenuItem item) -> item.setOnAction((ActionEvent e) -> {
      if(item.getText() != null) {
        switch(item.getText()) {
          case "Сегодня":
            table.getSourceItems().stream().forEach(doc -> doc.setValue("date", LocalDate.now()));
            break;
          case "Начало периода":
            table.getSourceItems().stream().forEach(doc -> doc.setValue("date", "Начало периода"));
            break;
          case "Конец периода":
            table.getSourceItems().stream().forEach(doc -> doc.setValue("date", "Конец периода"));
            break;
        }
      }
    }));
  }
}