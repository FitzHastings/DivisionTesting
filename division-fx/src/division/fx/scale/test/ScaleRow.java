package division.fx.scale.test;

import division.util.IDStore;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

public class ScaleRow<T extends ScalePeriod> extends AbstractScaleObject {
  private final ObjectProperty<ScaleInterface> scaleProperty = new SimpleObjectProperty<>();
  
  
  public ScaleRow(ScaleInterface scale) {
    this();
    scaleProperty.setValue(scale);
  }

  public ScaleRow() {
    setValue("row-id", IDStore.createID());
    getStyleClass().add("scale-row");
    
    getChildren().addListener((ListChangeListener.Change<? extends Node> c) -> {
      while(c.next()) {
        
        if(c.wasRemoved())
          c.getRemoved().stream().forEach(p -> unbindPeriod((T)p));
        
        if(c.wasAdded())
          c.getAddedSubList().stream().forEach(p -> bindPeriod(((T)p)));
      }
    });
    
    addEventFilter(MouseEvent.ANY, e -> {System.out.println(e.getEventType());});
  }
  
  public List<T> getPeriods() {
    return getChildren().stream().map(n -> (T)n).collect(Collectors.toList());
  }

  @Override
  public boolean equals(Object obj) {
    try {
      return obj != null && obj.getClass() == getClass() && ((ScaleRow)obj).getLong("row-id").equals(getLong("row-id"));
    }catch(Exception ex) {
      return false;
    }
  }
  
  private void bindPeriod(T period) {
    period.setLayoutY(0);
    period.layoutXProperty().bind(Bindings.createDoubleBinding(() -> 
            scaleProperty().getValue().betweenX((LocalDate) scaleProperty().getValue().leftDateProperty().getValue(), period.startDateProperty().getValue()).doubleValue(), 
            period.startDateProperty(), 
            scaleProperty().getValue().leftDateProperty()));
    
    period.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> 
            scaleProperty().getValue().betweenX(period.startDateProperty().getValue(), period.endDateProperty().getValue().plusDays(1)).doubleValue(), 
            scaleProperty().getValue().dayWidthProperty(), 
            period.startDateProperty(), period.endDateProperty()));
    
    period.minWidthProperty().bind(Bindings.createDoubleBinding(() -> 
            scaleProperty().getValue().betweenX(period.startDateProperty().getValue(), period.endDateProperty().getValue().plusDays(1)).doubleValue(), 
            scaleProperty().getValue().dayWidthProperty(), 
            period.startDateProperty(), period.endDateProperty()));
    
    period.maxWidthProperty().bind(Bindings.createDoubleBinding(() -> 
            scaleProperty().getValue().betweenX(period.startDateProperty().getValue(), period.endDateProperty().getValue().plusDays(1)).doubleValue(), 
            scaleProperty().getValue().dayWidthProperty(), 
            period.startDateProperty(), period.endDateProperty()));
    
    period.minHeightProperty().bind(heightProperty());
    period.maxHeightProperty().bind(heightProperty());
    period.prefHeightProperty().bind(heightProperty());
  }
  
  public void unbindAllPeriods() {
    getPeriods().forEach(p -> unbindPeriod(p));
  }
  
  public void unbindPeriod(T period) {
    period.layoutXProperty().unbind();
    
    period.prefWidthProperty().unbind();
    period.minWidthProperty().unbind();
    period.maxWidthProperty().unbind();
    
    period.minHeightProperty().unbind();
    period.maxHeightProperty().unbind();
    period.prefHeightProperty().unbind();
    
    period.getChildren().clear();
  }
  
  public ObjectProperty<ScaleInterface> scaleProperty() {
    return scaleProperty;
  }
}