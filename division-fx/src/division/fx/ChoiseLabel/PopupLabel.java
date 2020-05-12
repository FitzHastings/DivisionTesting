package division.fx.ChoiseLabel;

import division.fx.table.DivisionCellEditor;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Popup;

public class PopupLabel<T> extends HBox {
  private final Label name  = new Label();
  private final Label value = new Label();
  private final StringProperty    nameTextProperty = new SimpleStringProperty("Наименование");
  private final StringProperty    promtTextProperty = new SimpleStringProperty("Задайте заначение...");
  private final ObjectProperty<T> valueProperty     = new SimpleObjectProperty<>();
  
  private DivisionCellEditor<T> editor;

  public PopupLabel() {
    this("Наименование", null);
  }

  public PopupLabel(String nameText, DivisionCellEditor<T> ed) {
    getStyleClass().add("popup-label-box");
    name.getStyleClass().addAll("popup-label-name");
    value.getStyleClass().addAll("popup-label-value");
    
    name.minWidthProperty().bind(Bindings.createDoubleBinding(() -> {
      Text t = new Text(name.getText());
      t.setFont(name.getFont());
      return t.getLayoutBounds().getWidth()+10;
    }, name.textProperty()));
    
    nameTextProperty().setValue(nameText);
    getChildren().addAll(name, value);
    
    name.textProperty().bind(Bindings.createStringBinding(() -> nameTextProperty.getValue()+(valueProperty.getValue() == null || valueProperty.getValue().equals("") ? "..." : ":"), nameTextProperty(), valueProperty()));
    
    value.textProperty().bind(Bindings.createStringBinding(() -> {
      return valueProperty.getValue() == null || valueProperty.getValue().equals("") ? promtTextProperty.getValue() : objectToString(valueProperty.getValue());
    }, valueProperty, promtTextProperty));
    
    //this.editor = editor;
    setEditor(ed);
    
    valueProperty.addListener((ObservableValue<? extends T> observable, T oldValue, T newValue) -> {
      if(editor != null)
        editor.getPopup().hide();
    });
    
    value.setOnMouseClicked(e -> {
      if(editor != null)
        editor.getPopup().show(this, localToScreen(getBoundsInLocal()).getMinX(), localToScreen(getBoundsInLocal()).getMaxY());
    });
    
    Tooltip toolTip = new Tooltip();
    toolTip.textProperty().bind(value.textProperty());
    Tooltip.install(this, toolTip);
  }
  
  public final void setEditor(DivisionCellEditor<T> ed) {
    this.editor = ed;
    valueProperty.unbind();
    if(editor != null) {
      editor.setPopup(new Popup());
      editor.getPopup().setAutoHide(true);
      editor.getPopup().setHideOnEscape(true);
      editor.getPopup().setAutoFix(true);
      editor.getPopup().getContent().add(this.editor.getContent());
      valueProperty.bind(editor.resultPpoperty());
    }
  }
  
  public String objectToString(T object) {
    return object.toString();
  }
  
  public BooleanProperty nameVisibleProperty() {
    return name.visibleProperty();
  }
  
  public StringProperty nameTextProperty() {
    return nameTextProperty;
  }

  public StringProperty promtTextProperty() {
    return promtTextProperty;
  }

  public ObjectProperty<T> valueProperty() {
    return valueProperty;
  }
}