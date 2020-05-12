package division.fx.ChoiseLabel;

import division.fx.PropertyMap;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

public class ChoiceLabel<T> extends HBox {
  private final Label name  = new Label();
  private final Label value = new Label();
  private final StringProperty    nameTextProperty = new SimpleStringProperty("Наименование");
  private final StringProperty    promtTextProperty = new SimpleStringProperty("Выберите заначение...");
  private final ObjectProperty<T> valueProperty     = new SimpleObjectProperty<>();
  
  private final ObjectProperty<ObservableList<T>> itemsProperty = new SimpleObjectProperty<>(FXCollections.observableArrayList());
  private final ContextMenu menu = new ContextMenu();
  
  private final List<EventHandler> commitListener = new ArrayList<>();
  private final List<EventHandler> startListener  = new ArrayList<>();

  public ChoiceLabel() {
    this("Наименование");
  }

  public ChoiceLabel(String text) {
    getStyleClass().add("choise-label-box");
    name.getStyleClass().addAll("choise-label-name");
    value.getStyleClass().addAll("choise-label-value");
    menu.getStyleClass().addAll("choise-label-menu");
    
    name.minWidthProperty().bind(Bindings.createDoubleBinding(() -> {
      Text t = new Text(name.getText());
      t.setFont(name.getFont());
      return t.getLayoutBounds().getWidth()+10;
    }, name.textProperty()));
    
    nameTextProperty().setValue(text);
    getChildren().addAll(name, value);
    
    name.textProperty().bind(Bindings.createStringBinding(() -> nameTextProperty.getValue()+":", nameTextProperty(), valueProperty()));
    
    value.textProperty().bind(Bindings.createStringBinding(() -> {
      return valueProperty.getValue() == null || valueProperty.getValue().equals("") ? (promtTextProperty.getValue() == null || promtTextProperty.getValue().equals("") ? "..." : promtTextProperty.getValue()) : valueProperty.getValue().toString();
    }, valueProperty, promtTextProperty));
    
    value.setOnMouseClicked(e -> startEdit());
    
    Tooltip toolTip = new Tooltip();
    toolTip.textProperty().bind(value.textProperty());
    Tooltip.install(this, toolTip);
    
    nameVisibleProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(!newValue)
        getChildren().remove(name);
      else getChildren().add(0, name);
    });
    
    valueProperty.addListener((ObservableValue<? extends T> observable, T oldValue, T newValue) -> checkValue());
    
    itemsProperty.getValue().addListener((ListChangeListener.Change<? extends T> c) -> {
      menu.getItems().clear();
      itemsProperty.getValue().stream().forEach(t -> {
        MenuItem item = null;
        
        if(t instanceof PropertyMap && ((PropertyMap)t).isNotNull("check") && !((PropertyMap)t).is("check"))
          item = new MenuItem(t.toString());
        else item = new DivisionCheckMenuItem(t);
        
        if(t instanceof PropertyMap && ((PropertyMap)t).isNotNull("action") && ((PropertyMap)t).getValue("action") instanceof EventHandler)
          item.setOnAction(((PropertyMap)t).getValue("action", EventHandler.class));
        else if(!(t instanceof PropertyMap) || t instanceof PropertyMap && (((PropertyMap)t).isNull("check") || ((PropertyMap)t).isNotNull("check") && ((PropertyMap)t).is("check")))
          item.setOnAction(i -> commit(t));
        
        menu.getItems().add(item);
        checkValue();
      });
    });
  }
  
  private void checkValue() {
    menu.getItems().stream().filter(it -> {
      if(!(it instanceof CheckMenuItem))
        return false;
      ((DivisionCheckMenuItem)it).setSelected(false);
      return ((DivisionCheckMenuItem)it).getObject().equals(valueProperty().getValue());
    }).forEach(it -> ((DivisionCheckMenuItem)it).setSelected(true));
  }
  
  public boolean isEdit() {
    return menu.isShowing();
  }
  
  public void stopEdit() {
    menu.hide();
  }
  
  public void startEdit() {
    startListener().stream().forEach(h -> h.handle(new ActionEvent()));
    menu.show(value, value.localToScreen(value.getBoundsInLocal()).getMinX(), value.localToScreen(value.getBoundsInLocal()).getMaxY());
    System.out.println(menu.isShowing());
  }
  
  public void commit(T value) {
    if(value.equals(valueProperty().getValue()))
      valueProperty().setValue(null);
    valueProperty().setValue(value);
    commitListener().stream().forEach(h -> h.handle(new ActionEvent()));
  }
  
  public BooleanProperty nameVisibleProperty() {
    return name.visibleProperty();
  }
  
  public ObjectProperty<ObservableList<T>> itemsProperty() {
    return itemsProperty;
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
  
  public List<EventHandler> commitListener() {
    return commitListener;
  }
  
  public List<EventHandler> startListener() {
    return startListener;
  }
  
  class DivisionCheckMenuItem extends CheckMenuItem {
    private T object;

    public DivisionCheckMenuItem(T object) {
      super(object.toString());
      this.object = object;
    }

    public T getObject() {
      return object;
    }
  }
}