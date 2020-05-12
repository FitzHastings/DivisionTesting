package division.fx.scale;

import division.fx.PropertyMap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.layout.*;

public class DateRow<T extends DatePeriod> extends Pane implements PropertyMap {
  private final Region dayMoveBlock = new Region();
  private final ObservableList<String> equalKeys = FXCollections.observableArrayList();
  private final ObservableList<String> notEqualKeys = FXCollections.observableArrayList();
  private DateScale scale;
  private final ObjectProperty<LocalDateTime> lastupdate = new SimpleObjectProperty<>(LocalDateTime.now());

  @Override
  public ObservableList<String> equalKeys() {
    return equalKeys;
  }

  @Override
  public ObservableList<String> notEqualKeys() {
    return notEqualKeys;
  }
  
  public DateRow(DateScale scale) {
    this(scale, null);
  }

  public DateRow(DateScale scale, Map<String,Object> map) {
    if(map != null)
      this.copyFrom(map);
    this.scale = scale;
    getStyleClass().add("DateRow");
    dayMoveBlock.getStyleClass().add("dayMoveBlock");
    
    dayMoveBlock.setLayoutY(0);
    dayMoveBlock.prefWidthProperty().bind(scale.dayWidthProperty());
    dayMoveBlock.prefHeightProperty().bind(heightProperty());
    
    get("selected").addListener((ob, ol, nw) -> pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), (boolean)nw));
    
    setOnMouseExited(e -> {
      if(dayMoveBlock.getParent() != null)
        ((Pane)dayMoveBlock.getParent()).getChildren().remove(dayMoveBlock);
    });
    
    setOnMouseMoved(e -> {
      T period = periodAtDate(scale.getDate(e.getX()));
      if(period != null) {
        if(dayMoveBlock.getParent() != null)
          ((Pane)dayMoveBlock.getParent()).getChildren().remove(dayMoveBlock);
        period.getChildren().add(dayMoveBlock);
        dayMoveBlock.setLayoutX(period.parentToLocal(parentToLocal(scale.getX(scale.getDate(localToParent(e.getX(), 0).getX())), 0).getX(), 0).getX());
      }else {
        if(dayMoveBlock.getParent() != null)
          ((Pane)dayMoveBlock.getParent()).getChildren().remove(dayMoveBlock);
        getChildren().add(dayMoveBlock);
        dayMoveBlock.setLayoutX(scale.getX(scale.getDate(localToParent(e.getX(), 0).getX())));
      }
    });
    
    scale.previosMonthProperty.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> getPeriods().stream().forEach(p -> bindPeriod(p)));
    scale.futureMonthProperty.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> getPeriods().stream().forEach(p -> bindPeriod(p)));
    
    getPeriods().addListener((ListChangeListener.Change<? extends T> c) -> {
      while(c.next()) {
        if(c.wasAdded()) {
          getChildren().addAll(c.getAddedSubList());
          c.getAddedSubList().stream().forEach(period -> bindPeriod(period));
        }
        if(c.wasRemoved()) {
          getChildren().removeAll(c.getRemoved());
          c.getRemoved().stream().forEach(period -> unbindPeriod(period));
        }
      }
    });
  }
  
  private void bindPeriod(T period) {
    period.setLayoutY(0);
    period.layoutXProperty().bind(scale.dayWidthProperty().multiply(scale.getDayCountBefore(period.startDateProperty().get())));
    
    period.minWidthProperty().bind(scale.dayWidthProperty().multiply(scale.getDayCount(period.startDateProperty().get(), period.endDateProperty().get())));
    period.maxWidthProperty().bind(scale.dayWidthProperty().multiply(scale.getDayCount(period.startDateProperty().get(), period.endDateProperty().get())));
    period.prefWidthProperty().bind(scale.dayWidthProperty().multiply(scale.getDayCount(period.startDateProperty().get(), period.endDateProperty().get())));
    
    period.minHeightProperty().bind(heightProperty());
    period.maxHeightProperty().bind(heightProperty());
    period.prefHeightProperty().bind(heightProperty());
  }

  public DateScale getScale() {
    return scale;
  }
  
  public T periodAtDate(LocalDate date) {
    for(T datePeriod:getPeriods())
      if((date.equals(datePeriod.startDateProperty().getValue()) || date.isAfter(datePeriod.startDateProperty().getValue())) && 
              (date.equals(datePeriod.endDateProperty().getValue()) || date.isBefore(datePeriod.endDateProperty().getValue())))
        return datePeriod;
    return null;
  }

  public T periodAtPoint(double rowX) {
    for(T datePeriod:getPeriods())
      if(rowX >= datePeriod.getLayoutX() && rowX <= scale.dayWidthProperty().getValue() + scale.getX(datePeriod.endDateProperty().getValue()))
        return datePeriod;
    return null;
  }
  
  public DateRow add(Collection<? extends T> periods) {
    getPeriods().addAll(periods);
    return this;
  }
  
  public DateRow add(T... periods) {
    getPeriods().addAll(periods);
    return this;
  }
  
  public DateRow remove(T period) {
    getPeriods().remove(period);
    return this;
  }
  
  public ObservableList<T> getPeriods() {
    return (ObservableList<T>)getList("periods", DatePeriod.class);
  }
  
  private void unbindPeriod(T period) {
    period.unbindAll();
    period.layoutXProperty().unbind();
    period.prefWidthProperty().unbind();
    period.minWidthProperty().unbind();
    period.maxWidthProperty().unbind();
    period.prefHeightProperty().unbind();
    period.dispose();
  }
  
  public void dispose() {
    getPeriods().stream().forEach(period -> unbindPeriod(period));
    getChildren().clear();
    this.unbindAll();
  }
  
  private final Map<String, Property> map = new TreeMap<>();

  @Override
  public Map<String, Property> getMap() {
    return map;
  }

  @Override
  public ObjectProperty<LocalDateTime> lastupdate() {
    return lastupdate;
  }
  
  @Override
  public BooleanProperty printDifferenceProperty() {
    return null;
  }
}