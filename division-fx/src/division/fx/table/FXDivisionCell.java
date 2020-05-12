package division.fx.table;

import division.fx.DivisionTextField;
import division.fx.PropertyMap;
import division.util.Utility;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.stage.Popup;
import javafx.util.StringConverter;

public interface FXDivisionCell<S,T> {
  public StringConverter<T> getCustomTextConverter();
  public Node getComponent();
  public ObservableList<T> getValues();
  
  public PropertyMap getRowItem();
  public T getItem();
  public Bounds getBoundsInLocal();
  public Bounds localToScreen(Bounds bounds);
  public boolean isEditable();
  public DivisionCellEditor<T> getCellEditor();
  public void setComponent(Node createCheckBox);
  public FXColumn getColumn();
  public void setText(String toString);
  public void setContentDisplay(ContentDisplay contentDisplay);
  public boolean isEditing();
  public void setGraphic(Node object);
  public void setAlignment(Pos pos);
  public void commitEdit(T newValue);
  public void cancelEdit();
  public Font getFont();
  public ObjectProperty<Font> fontProperty();
  public boolean isDisabled();
  public boolean isSelected();
  
  public default Popup createPopup() {
    Popup pop = new Popup();
    pop.setAutoFix(true);
    pop.setAutoHide(true);
    pop.setHideOnEscape(true);
    return pop;
  }
  
  public default void edit() {
    if(isEditable()) {
      if(getCellEditor() != null) {
        Popup pop = createPopup();
        getCellEditor().setPopup(pop);
        getCellEditor().resultPpoperty().setValue(getItem());
        getCellEditor().dataPpoperty().setValue(getItem());
        pop.getContent().add(getCellEditor().getContent());
        getCellEditor().resultPpoperty().addListener((ObservableValue<? extends T> observable, T oldValue, T newValue) -> {
          commitEdit(newValue);
          pop.hide();
        });
        pop.show((Node)this, localToScreen(getBoundsInLocal()).getMinX(), localToScreen(getBoundsInLocal()).getMaxY());
      }else if(getValues() != null && !getValues().isEmpty()) {
        createListContext(getColumn().getValue(getRowItem())).show((Node)this, localToScreen(getBoundsInLocal()).getMinX(), localToScreen(getBoundsInLocal()).getMaxY());
      }else if(getItem() instanceof Boolean)
        setComponent(createCheckBox());
      else if(getItem() instanceof LocalDate || getItem() instanceof java.sql.Timestamp || getItem() instanceof java.sql.Date || getItem() instanceof Date)
        setComponent(createDatePicker());
      else if(getComponent() == null)
        setComponent(createTextField());
    }
    if(getComponent() != null) {
      
      if(getComponent() instanceof TextInputControl && getItem() != null) {
        ((TextInputControl)getComponent()).setText(toString(getItem()));
        ((TextInputControl)getComponent()).selectAll();
      }else setText(null);
      
      setGraphic(getComponent());
      setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
      getComponent().setDisable(false);
      getComponent().requestFocus();
      getComponent().focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
        if(!newValue) {
          if(getComponent() instanceof TextInputControl)
            commitTextField(((TextInputControl)getComponent()).getText());
          else cancelEdit();
        }
      });
      
      if(getComponent() instanceof DatePicker)
        ((DatePicker)getComponent()).show();
    }
  }
  
  public default void cancel() {
    setText(toString(getItem()));
    setContentDisplay(ContentDisplay.TEXT_ONLY);
  }
  
  public default void commit(T newValue) {
    getColumn().setValue(getRowItem(), newValue);
  }
  
  public default void update(T item, boolean empty, List<String> styleClass) {
    if(empty) {
      setText(null);
      setGraphic(null);
    }else {
      if(isEditing()) {
        setText(null);
        setGraphic(getComponent());
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
      }else {
        if(item instanceof Boolean) {
          setAlignment(Pos.CENTER);
          setText(null);
          setComponent(createCheckBox());
          setGraphic(getComponent());
          setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }else {
          if(item instanceof Integer || item instanceof Long || item instanceof Short || item instanceof Float || item instanceof Double || item instanceof BigInteger || item instanceof BigDecimal || item instanceof Number)
            setAlignment(Pos.CENTER_RIGHT);
          if(getValues() != null && !getValues().isEmpty()) {
            /*setText(null);
            Label selectLabel = new Label(item == null || item.equals("") ? "......." : toString(item));
            selectLabel.fontProperty().bind(fontProperty());
            HBox.setHgrow(selectLabel, Priority.ALWAYS);
            Label selectDoun = new Label();
            selectDoun.getStyleClass().add("list-label");
            HBox box = new HBox(2, selectLabel,selectDoun);
            selectLabel.setMaxWidth(Double.MAX_VALUE);
            setGraphic(box);*/
            //selectLabel.getStyleClass().addAll(styleClass);
            //selectLabel.getStyleClass().addAll(styleClass);
            //setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setText(item == null || item.equals("") ? "......." : toString(item));
          }else {
            setText(toString(item));
            setContentDisplay(ContentDisplay.TEXT_ONLY);
          }
        }
      }
    }
  }
  
  public default TextField createTextField() {
    DivisionTextField field = new DivisionTextField(getConverter());
    if(getItem() != null)
      field.setText(toString(getItem()));
    field.setOnAction(e -> {
      field.setDisable(true);
      commitTextField(field.getText());
    });
    field.selectAll();
    field.setOnKeyPressed((KeyEvent event) -> {
      if(event.getCode() == KeyCode.ESCAPE)
        cancelEdit();
    });
    field.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(!newValue && !field.isDisable())
        commitTextField(field.getText());
    });
    return field;
  }
  
  public default void commitTextField(String text) {
    T o = null;
    try {
      o = fromString(text);
    }catch(Exception ex) {
      ex.printStackTrace();
      if(getComponent() != null)
        new Timeline(new KeyFrame(javafx.util.Duration.millis(100), e -> {
          new Timeline(new KeyFrame(javafx.util.Duration.millis(100), new KeyValue(getComponent().opacityProperty(), 1))).play();
        }, new KeyValue(getComponent().opacityProperty(), 0))).play();
    }
    commitEdit(o);
  }
  
  public default StringConverter<T> getConverter() {
    return getCustomTextConverter() == null ? Utility.getConverter(getItem()) : getCustomTextConverter();
  }
  
  public default T fromString(String text) {
    return getConverter().fromString(text == null ? "" : text);
  }
  
  public default String toString(T item) {
    try {
      return item == null ? "" : getConverter().toString(item);
    }catch(Exception ex) {
      return item == null ? "" : item.toString();
    }
  }
  
  public default boolean equalsColumnValueToListValue(Object columnValue, Object listValue) {
    return columnValue == listValue || columnValue != null && columnValue.equals(listValue);
  }
  
  public default ContextMenu createListContext(Object defitem) {
    ContextMenu menu = new ContextMenu();
    getValues().stream().forEach(val -> {
      CheckMenuItem item = new CheckMenuItem(val.toString());
        item.setSelected(equalsColumnValueToListValue(defitem, val));
      item.setOnAction(e -> commitEdit(val));
      menu.getItems().add(item);
    });
    menu.setOnHidden(e -> {
      cancelEdit();
      ((TableCell)this).getTableView().refresh();
    });
    return menu;
  }
  
  public default CheckBox createCheckBox() {
    CheckBox checkBox = new CheckBox();
    checkBox.setSelected((Boolean)getItem());
    checkBox.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> commitEdit((T)newValue));
    return checkBox;
  }
  
  public default DatePicker createDatePicker() {
    DatePicker datePicker = new DatePicker();
    if(getItem() instanceof LocalDate)
      datePicker.setValue((LocalDate)getItem());
    if(getItem() instanceof Date)
      datePicker.setValue(Utility.convert(((Date)getItem())));
    if(getItem() instanceof java.sql.Date)
      datePicker.setValue(Utility.convert(((java.sql.Date)getItem())));
    
    datePicker.valueProperty().addListener((ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) -> {
      if(getItem() instanceof Date)
        commitEdit((T)Utility.convert(newValue));
      if(getItem() instanceof java.sql.Date)
        commitEdit((T)Utility.convertToSqlDate(newValue));
      if(getItem() instanceof java.sql.Timestamp)
        commitEdit((T)Utility.convertToTimestamp(newValue));
      if(getItem() instanceof LocalDate)
        commitEdit((T)newValue);
    });
    datePicker.setEditable(false);
    return datePicker;
  }
}