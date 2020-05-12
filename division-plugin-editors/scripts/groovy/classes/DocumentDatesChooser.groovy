package scripts.groovy.classes

import java.awt.Dimension;
import javafx.application.Platform;
import javafx.collections.ObservableList
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Scene;
import javafx.collections.FXCollections
import java.time.LocalDate
import javax.swing.*
import bum.interfaces.*
import bum.editors.util.ObjectLoader
import javafx.geometry.Pos
import javafx.scene.effect.*
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.event.*
import division.fx.*
import division.swing.*
import javafx.scene.control.cell.*
import java.awt.Cursor;

public class DocumentDatesChooser {
  public static get(documents) {
    def result = false
    JFXPanel fxContainer = new JFXPanel();
    fxContainer.setPreferredSize(new Dimension(700, 300));
    JDialog frame = new JDialog(null, "Создание документов...");
    frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
    Platform.runLater {
      ObservableList<DocumentRow> documentModel = FXCollections.observableArrayList();
      documents.each {id,doc ->
        documentModel.add(new DocumentRow(
            id, 
            doc.name, 
            doc.split==null?false:doc.split, 
            doc.startDealDate==null?false:doc.startDealDate, 
            doc.endDealDate==null?false:doc.endDealDate, 
            doc.customDate==null?LocalDate.now():doc.customDate));
      }
      TableView<DocumentRow> tableView = new TableView<>(documentModel);
      TableColumn groupColumn  = createColumn("Дата документа",null,true)
      groupColumn.getColumns().addAll(
        createColumn("Начало сделки","startDeal",true),
        createColumn("Конец сделки","endDeal",true),
        createColumn("Произвольная дата","customDate",true)
      );
      tableView.getColumns().addAll(
        createColumn("id","id",false),
        createColumn("Документ","name",true),
        createColumn("Объединять","split",true),
        groupColumn);
      tableView.setEditable(true);
      def ok = new Button("Ok")
      ok.setOnAction(new EventHandler<ActionEvent>() {
        public void handle(ActionEvent event) {
          documentModel.each {
            documents[it.getId()].split         = it.getSplit().isSelected()
            documents[it.getId()].startDealDate = it.getStartDeal().isSelected()
            documents[it.getId()].endDealDate   = it.getEndDeal().isSelected()
            documents[it.getId()].customDate    = it.getCustomDate().getValue();
          }
          result = true
          frame.setVisible(false)
        }
      });
      VBox vbox = new VBox(5, tableView, ok);
      vbox.setAlignment(Pos.BOTTOM_RIGHT)
      vbox.setPadding(new Insets(10, 10, 10, 10));
      vbox.setVgrow(tableView, Priority.ALWAYS);
      vbox.setFillWidth(true);
      fxContainer.setScene(new Scene(vbox));
      SwingUtilities.invokeLater{frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR))}
    }
    
    frame.setContentPane(fxContainer);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setModal(true)
    frame.setVisible(true);
    return result
  }
  
  private static TableColumn createColumn(name, propertyName, visible) {
    TableColumn column = new TableColumn(name)
    column.setVisible(visible);
    if(propertyName != null)
      column.setCellValueFactory(new PropertyValueFactory(propertyName))
    return column
  }
}

public class DocumentRow {
  private Integer id;
  private String  name;
  private CheckBox    split      = new CheckBox();
  private CheckBox    startDeal  = new CheckBox();
  private CheckBox    endDeal    = new CheckBox();;
  private DatePicker  customDate = new DatePicker(LocalDate.now());

  private DocumentRow(Integer id, String name, boolean split, boolean startDeal, boolean endDeal, LocalDate customDate) {
    this.id = id;
    this.name = name;
    this.split.setSelected(split);
    this.startDeal.setSelected(startDeal);
    this.endDeal.setSelected(endDeal);
    this.customDate.setValue(customDate);

    this.startDeal.setOnAction(new EventHandler<ActionEvent>() {
      public void handle(ActionEvent event) {
        if(DocumentRow.this.startDeal.isSelected())
          DocumentRow.this.endDeal.setSelected(false);
        DocumentRow.this.customDate.setVisible(!DocumentRow.this.startDeal.isSelected() && !DocumentRow.this.endDeal.isSelected());
      }
    });

    this.endDeal.setOnAction(new EventHandler<ActionEvent>() {
      public void handle(ActionEvent event) {
        if(DocumentRow.this.endDeal.isSelected())
          DocumentRow.this.startDeal.setSelected(false);
        DocumentRow.this.customDate.setVisible(!DocumentRow.this.startDeal.isSelected() && !DocumentRow.this.endDeal.isSelected());
      }
    });

    this.customDate.setVisible(!this.startDeal.isSelected() && !this.endDeal.isSelected());
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public CheckBox getSplit() {
    return split;
  }

  public void setSplit(boolean split) {
    this.split.setSelected(split);
  }

  public CheckBox getStartDeal() {
    return startDeal;
  }

  public void setStartDeal(boolean startDeal) {
    this.startDeal.setSelected(startDeal);
  }

  public CheckBox getEndDeal() {
    return endDeal
  }

  public void setEndDeal(boolean endDeal) {
    this.endDeal.setSelected(endDeal);
  }

  public DatePicker getCustomDate() {
    return customDate;
  }

  public void setCustomDate(LocalDate customDate) {
    this.customDate.setValue(customDate);
  }
}