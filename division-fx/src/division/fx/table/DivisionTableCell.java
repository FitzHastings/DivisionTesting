package division.fx.table;

import division.fx.PropertyMap;
import division.util.Utility;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import java.util.Date;
import java.math.*;
import java.sql.Timestamp;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.input.MouseEvent;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;

public class DivisionTableCell extends TableCell/* extends TextFieldTableCell*/ {
  private Node editComponent;
  private DivisionCellEditor cellEditor;
  private ObservableList listValues = null;

  public DivisionTableCell(DivisionCellEditor cellEditor) {
    this();
    this.cellEditor = cellEditor;
    this.cellEditor.setCell(this);
  }
  
  public DivisionTableCell(Object... values) {
    this(FXCollections.observableArrayList(values));
  }
  
  public DivisionTableCell(ObservableList listValues) {
    this();
    this.listValues = listValues;
  }
  
  public DivisionTableCell() {
    getStyleClass().addAll("division-table-cell", "text-field-table-cell");
    
    addEventFilter(MouseEvent.ANY, (MouseEvent event) -> {
      if(getGraphic() != null && !getGraphic().contains(getGraphic().parentToLocal(event.getX(), event.getY()).getX(), getGraphic().parentToLocal(event.getX(), event.getY()).getY())) {
        event.consume();
        if(event.getEventType() == MouseEvent.MOUSE_PRESSED)
          getTableView().getSelectionModel().select(getTableRow().getIndex());
      }
    });
    
    setOnMousePressed((MouseEvent event) -> {
      getTableView().edit(getTableRow().getIndex(), getTableColumn());
    });
  }
  
  public boolean equalsColumnValueToListValue(Object columnValue, Object listValue) {
    return columnValue == listValue || columnValue != null && columnValue.equals(listValue);
  }

  @Override
  public void startEdit() {
    //super.startEdit();
    if(getTableRow().getItem() != null && getTableColumn().isEditable() && isEditable()) {
      if(cellEditor != null) {
        Popup pop = new Popup();
        pop.setAutoFix(true);
        pop.setAutoHide(true);
        pop.setHideOnEscape(true);
        cellEditor.setPopup(pop);
        cellEditor.resultPpoperty().setValue(getItem());
        cellEditor.dataPpoperty().setValue(getItem());
        pop.getContent().add(cellEditor.getContent());
        cellEditor.resultPpoperty().addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
          commitEdit(newValue);
          pop.hide();
        });
        pop.show(this, localToScreen(getBoundsInLocal()).getMinX(), localToScreen(getBoundsInLocal()).getMaxY());
      }else if(listValues != null) {
        ContextMenu menu = new ContextMenu();
        listValues.stream().forEach(val -> {
          CheckMenuItem item = new CheckMenuItem(val.toString());
          item.setSelected(equalsColumnValueToListValue(((Column)getTableColumn()).getValue((PropertyMap)getTableRow().getItem()), val));
          item.setOnAction(e -> commitEdit(val));
          menu.getItems().add(item);
        });
        menu.show(this, localToScreen(getBoundsInLocal()).getMinX(), localToScreen(getBoundsInLocal()).getMaxY());
      }else if(getItem() instanceof Boolean) {
        commitEdit(!(Boolean)getItem());
      }else if(getItem() instanceof Date || getItem() instanceof java.sql.Date || getItem() instanceof LocalDate || getItem() instanceof java.sql.Timestamp) {
        if(getItem() instanceof Date)
          editComponent = new DatePicker(Utility.convert((Date)getItem()));
        if(getItem() instanceof java.sql.Date)
          editComponent = new DatePicker(Utility.convert((java.sql.Date)getItem()));
        if(getItem() instanceof java.sql.Timestamp)
          editComponent = new DatePicker(Utility.convert((java.sql.Timestamp)getItem()));
        if(getItem() instanceof LocalDate)
          editComponent = new DatePicker((LocalDate)getItem());
        ((DatePicker)editComponent).valueProperty().addListener((ObservableValue<? extends LocalDate> ob, LocalDate ol, LocalDate nw) -> commitEdit(nw));
        Platform.runLater(() -> {
          editComponent.requestFocus();
          ((DatePicker)editComponent).show();
        });
      }else {
        editComponent = new TextField();
        ((TextField)editComponent).selectAll();
        if(getItem() != null)
          ((TextField)editComponent).setText(getItem().toString());
        ((TextField)editComponent).setOnAction(e -> commitEdit(((TextField)editComponent).getText()));
        ((TextField)editComponent).setOnKeyPressed((KeyEvent event) -> {
          if(event.getCode() == KeyCode.ESCAPE)
            cancelEdit();
        });
        ((TextField)editComponent).focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
          if(!newValue)
            commitEdit(((TextField)editComponent).getText());
        });
      }
      
      if(editComponent != null) {
        setGraphic(editComponent);
        setText(null);
        setGraphic(editComponent);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        editComponent.requestFocus();
      }
    }
  }

  @Override
  public void cancelEdit() {
    super.cancelEdit();
    setContentDisplay(ContentDisplay.TEXT_ONLY);
  }

  @Override
  public void commitEdit(Object newValue) {
    try {
      if(getItem() instanceof BigDecimal)
        newValue = BigDecimal.valueOf(Double.valueOf(String.valueOf(newValue)));
      if(getItem() instanceof BigInteger)
        newValue = BigInteger.valueOf(Long.valueOf(String.valueOf(newValue)));
      if(getItem() instanceof Double)
        newValue = Double.valueOf(String.valueOf(newValue));
      if(getItem() instanceof Float)
        newValue = Float.valueOf(String.valueOf(newValue));
      if(getItem()instanceof Long)
        newValue = Long.valueOf(String.valueOf(newValue));
      if(getItem() instanceof Integer)
        newValue = Integer.valueOf(String.valueOf(newValue));
      if(getItem() instanceof Date)
        newValue = Integer.valueOf(String.valueOf(newValue));

      if(getTableColumn() instanceof Column) {
        ((Column)getTableColumn()).setValue((PropertyMap)getTableRow().getItem(), newValue);
        super.commitEdit(newValue);
      }else super.commitEdit(newValue);
      
    }catch(Exception ex) {
      ex.printStackTrace();
      if(editComponent != null)
        new Timeline(new KeyFrame(javafx.util.Duration.millis(100), e -> {
          new Timeline(new KeyFrame(javafx.util.Duration.millis(100), new KeyValue(editComponent.opacityProperty(), 1))).play();
        }, new KeyValue(editComponent.opacityProperty(), 0))).play();
    }
  }
  
  @Override
  protected void updateItem(Object item, boolean empty) {
    super.updateItem(item, empty);
    
    if(item == null || empty) {
      setText(null);
      setGraphic(null);
    }else {
      setContentDisplay(ContentDisplay.TEXT_ONLY);
      
      if(item instanceof Boolean) {
        CheckBox box =  new CheckBox();
        box.addEventFilter(MouseEvent.ANY, (MouseEvent event) -> {
          event.consume();
          if(event.getEventType() == MouseEvent.MOUSE_PRESSED)
            fireEvent(event);
        });
        box.setSelected((Boolean)item);
        setGraphic(box);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setAlignment(Pos.CENTER);
      }else if(item instanceof BigDecimal) {
        setText(((BigDecimal)item).toPlainString());
        setAlignment(Pos.CENTER_RIGHT);
      }else if(item instanceof BigInteger) {
        setText(((BigInteger)item).toString());
        setAlignment(Pos.CENTER_RIGHT);
      }else if(item instanceof Integer || item instanceof Double || item instanceof Float || item instanceof Long) {
        setText(String.valueOf(item));
        setAlignment(Pos.CENTER_RIGHT);
      }else if(item instanceof LocalDate)
        setText(((LocalDate)item).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
      else if(item instanceof Date) 
        setText(Utility.format((Date)item));
      else if(item instanceof java.sql.Date) 
        setText(Utility.format((java.sql.Date)item));
      else if(item instanceof java.sql.Timestamp) 
        setText(Utility.format((Timestamp)item));
      else setText(String.valueOf(item));
      setStyle(getCustomStyle(item));
      
      if(isEditing() && editComponent != null) {
        ((TextField)editComponent).setText(item.toString());
        setText(null);
        setGraphic(editComponent);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
      }
    }
  }

  public String getCustomStyle(Object item) {
    return "";
  }
}