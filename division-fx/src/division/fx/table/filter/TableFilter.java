package division.fx.table.filter;

import division.fx.PropertyMap;
import division.fx.gui.FXDisposable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ToggleButton;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.util.Duration;

public class TableFilter<S extends PropertyMap> implements FXDisposable {
  private Map<TableColumn<S,?>,ColumnFilter> filters = new HashMap<>();
  private ObservableList<S> items        = FXCollections.observableArrayList();
  private ObjectProperty<FilteredList<S>> filterItemsProperty = new SimpleObjectProperty<>(new FilteredList(items, p -> true));
  
  private int timeOut = 0;
  private FilterTask filterTask = null;
  private ExecutorService pool = Executors.newCachedThreadPool();
  
  private final Popup popup = new Popup();
  
  FilterListener filterListener = FilterListener.create(e -> {
    if(filterTask != null)
      filterTask.stop();
    pool.submit(filterTask = new FilterTask(timeOut));
  });

  public TableFilter() {
    popup.setAutoFix(false);
    popup.setAutoHide(true);
    popup.setHideOnEscape(true);
    popup.setOnHiding(event -> filters.keySet().stream().forEach(column -> ((ToggleButton)column.getGraphic()).setSelected(false)));
  }

  public void setTimeOut(int timeOut) {
    this.timeOut = timeOut;
  }
  
  public ObservableList<S> getItems() {
    return (ObservableList<S>) filterItemsProperty.getValue().getSource();
  }

  public Map<TableColumn<S, ?>, ColumnFilter> getFilters() {
    return filters;
  }

  public FilteredList getFilteredList() {
    return filterItemsProperty.getValue();
  }
  
  public ObjectProperty<FilteredList<S>> filteredItemsProperty() {
    return filterItemsProperty;
  }
  
  public ColumnFilter getFilter(TableColumn column) {
    return filters.get(column);
  }

  public Popup getPopup() {
    return popup;
  }

  @Override
  public List<FXDisposable> disposeList() {
    return new ArrayList<>(filters.values());
  }
  
  public void addFilter(TableColumn column, ColumnFilter filter) {
    filter.setTableFilter(this);
    filters.put(column, filter);
    filter.getContent().getStyleClass().add("column-filter");
    createFilterButton(column,filter);
    filter.addFilterListener(filterListener);
  }
  
  public void startFilter() {
    filterListener.handle(new ActionEvent());
  }

  private void createFilterButton(TableColumn column, ColumnFilter filter) {
    ToggleButton filterButton = new ToggleButton("Y");
    filterButton.getStyleClass().add("filter-button");
    
    filterButton.textFillProperty().bind(Bindings.createObjectBinding(() -> filter.isActive() ? Color.BLUE : Color.BLACK, filter.activeProperty()));

    column.setGraphic(filterButton);
    filterButton.selectedProperty().addListener((ob,ol,nw) -> {
      if(nw) {
        filter.getContent().setOpacity(0);
        popup.setOpacity(0);
        filter.initFilter();
        popup.getContent().clear();
        popup.getContent().add(filter.getContent());
        popup.show(filterButton, filterButton.localToScreen(0, 0).getX(), filterButton.localToScreen(0, 0).getY() + filterButton.getHeight());
        new Timeline(new KeyFrame(Duration.millis(500), new KeyValue(filter.getContent().opacityProperty(), 1), new KeyValue(popup.opacityProperty(), 1))).play();
      }else popup.hide();
    });
  }
  
  private Predicate<PropertyMap> customFilter = null;
  
  public void setCustomFilter(Predicate<PropertyMap> customFilter) {
    this.customFilter = customFilter;
  }

  @Override
  public void finaly() {
    filters.values().stream().forEach(f -> f.removeFilterListener(filterListener));
    filterListener = null;
    filters.clear();
    items.clear();
    filterItemsProperty.getValue().clear();
  }
  
  class FilterTask implements Runnable {
    private boolean run = true;
    private int timeout;

    public FilterTask(int timeout) {
      this.timeout = timeout;
    }
    
    public void stop() {
      run = false;
    }
    
    @Override
    public void run() {
      try {
        Thread.sleep(this.timeout);
      }catch(InterruptedException ex) {}
      if(run) {
        System.out.println("timeout from runnable = "+this.timeout);
        Platform.runLater(() -> {
          Predicate predicate = (Predicate) (Object t) -> true;
          for(ColumnFilter f:filters.values()) {
            if(f.isActive())
              predicate = predicate.and(f.getPredicate());
          }
          if(customFilter != null)
            predicate = predicate.and(customFilter);
          filterItemsProperty.getValue().setPredicate(predicate);
        });
      }
    }
  }
}