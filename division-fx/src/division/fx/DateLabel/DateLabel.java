package division.fx.DateLabel;

import com.sun.javafx.scene.control.skin.DatePickerSkin;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Popup;

public class DateLabel extends HBox {
  private final Label name  = new Label();
  private final Label value = new Label();
  private final StringProperty    nameTextProperty  = new SimpleStringProperty("Наименование");
  private final StringProperty    promtTextProperty = new SimpleStringProperty("Выберите заначение...");
  private final DatePicker        datePicker        = new DatePicker(LocalDate.now());
  private final DatePickerSkin    datePickerSkin    = new DatePickerSkin(datePicker);
  
  public DateLabel() {
    this("Наименование");
  }
  
  public DateLabel(String text) {
    this(text, LocalDate.now());
  }

  public DateLabel(String text, LocalDate date) {
    getStyleClass().add("date-label-box");
    name.getStyleClass().addAll("date-label-name");
    value.getStyleClass().addAll("date-label-value");
    
    name.minWidthProperty().bind(Bindings.createDoubleBinding(() -> {
      Text t = new Text(name.getText());
      t.setFont(name.getFont());
      return t.getLayoutBounds().getWidth()+10;
    }, name.textProperty()));
    
    nameTextProperty().setValue(text);
    valueProperty().setValue(date);
    
    getChildren().addAll(name, value);
    
    name.textProperty().bind(Bindings.createStringBinding(() -> nameTextProperty.getValue()+(valueProperty().getValue() == null ? "..." : ":"), nameTextProperty(), valueProperty()));
    
    value.textProperty().bind(Bindings.createStringBinding(() -> {
      return valueProperty().getValue() == null ? promtTextProperty.getValue() : valueProperty().getValue().format(DateTimeFormatter.ofPattern("dd.MM.YYYY"));
    }, valueProperty(), promtTextProperty));
    
    value.setOnMouseClicked(e -> {
      Popup pop = new Popup();
      valueProperty().addListener((ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) -> pop.hide());
      pop.getContent().add(datePickerSkin.getPopupContent());
      pop.setAutoHide(true);
      pop.setHideOnEscape(true);
      pop.show(value, value.localToScreen(value.getBoundsInLocal()).getMinX(), value.localToScreen(value.getBoundsInLocal()).getMaxY());
    });
    
    Tooltip toolTip = new Tooltip();
    toolTip.textProperty().bind(value.textProperty());
    Tooltip.install(this, toolTip);
  }

  public StringProperty nameTextProperty() {
    return nameTextProperty;
  }

  public StringProperty promtTextProperty() {
    return promtTextProperty;
  }

  public ObjectProperty<LocalDate> valueProperty() {
    return datePicker.valueProperty();
  }
}