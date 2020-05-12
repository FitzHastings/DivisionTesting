package division.fx.scale.test;

import division.fx.PropertyMap;
import division.fx.gui.FXDisposable;
import division.fx.util.MsgTrash;
import division.util.Utility;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class ScaleTable<S extends ScaleRow<P>, P extends ScalePeriod> extends VBox implements FXDisposable, ScaleInterface<S,P> {
  private final S          header       = (S) new ScaleRow(this);
  private final ScrollPane headerScroll = new ScrollPane(header);
  private Pane             rootLayer    = new Pane();
  private final ScrollPane rootScroll   = new ScrollPane(rootLayer);
  
  private Pane rowLayer      = createLayer("row-layer");
  private Pane selectedLayer = createLayer("selected-layer");
  private Pane focusedLayer  = createLayer("focused-layer");
  private Pane clientLayer   = createLayer("client-layer");
  
  private final ObjectProperty<LocalDate> leftDateProperty     = new SimpleObjectProperty<>();
  private final ObjectProperty<LocalDate> rightDateProperty    = new SimpleObjectProperty<>();
  private final IntegerProperty           previosMonthProperty = new SimpleIntegerProperty(12);
  private final IntegerProperty           futureMonthProperty  = new SimpleIntegerProperty(12);
  private final IntegerProperty           dayWidthProperty     = new SimpleIntegerProperty(5);

  private OPool<S> rowPool;
  private OPool<P> periodPool;
  
  private final Region                 focusedDayBlock         = new Region();
  private final Region                 focusedRowBlock         = new Region();
  
  private final ObjectProperty<List<S>> selectedRowProperty    = new SimpleObjectProperty(FXCollections.observableArrayList());
  private final ObjectProperty<List<P>> selectedPeriodProperty = new SimpleObjectProperty(FXCollections.observableArrayList());
  
  private final ObjectProperty<ObservableList<ScaleRow<ScalePeriod>>> itemsProperty = new SimpleObjectProperty<>();
  
  public ScaleTable() {
    this(ScaleRow.class, ScalePeriod.class);
  }
  
  public ScaleTable(Class rowClass, Class periodClass) {
    getChildren().addAll(headerScroll, rootScroll);
    rowPool = new OPool(rowClass, 100);
    periodPool = new OPool(periodClass, 100);
    VBox.setVgrow(rootScroll, Priority.ALWAYS);
    
    leftDateProperty.bind(Bindings.createObjectBinding(() -> LocalDate.now().minusMonths(previosMonthProperty().getValue()).withDayOfMonth(1), previosMonthProperty));
    rightDateProperty.bind(Bindings.createObjectBinding(() -> {
      LocalDate right = LocalDate.now().plusMonths(futureMonthProperty().getValue()).withDayOfMonth(1);
      return right.withDayOfMonth(right.lengthOfMonth());
    }, futureMonthProperty));
    
    initHeader();
    initEvents();
    
    focusedRowBlock.setLayoutX(0);
    focusedRowBlock.prefWidthProperty().bind(focusedLayer.widthProperty());
    focusedRowBlock.prefHeightProperty().bind(header.heightProperty());
    
    headerScroll.getStyleClass().add("header-scroll");
    rootScroll.getStyleClass().add("root-scroll");
    
    headerScroll.setFocusTraversable(false);
    rootScroll.setFocusTraversable(false);
    
    headerScroll.hvalueProperty().bind(rootScroll.hvalueProperty());
    
    rootLayer.prefHeightProperty().bind(Bindings.createDoubleBinding(() -> rowLayer.getChildren().size()*header.getHeight(), rowLayer.getChildren()));
    rootLayer.prefWidthProperty().bind(header.widthProperty());
  }
  
  private void initEvents() {
    leftDateProperty.addListener((ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) -> initHeader());
    rightDateProperty.addListener((ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) -> initHeader());
    dayWidthProperty.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> initHeader());
    
    itemsProperty.addListener((ObservableValue<? extends ObservableList<ScaleRow<ScalePeriod>>> observable, ObservableList<ScaleRow<ScalePeriod>> oldValue, ObservableList<ScaleRow<ScalePeriod>> newValue) -> {
      clear();
      rowLayer.getChildren().setAll(newValue);
      newValue.addListener((ListChangeListener.Change<? extends ScaleRow<ScalePeriod>> c) -> {
        while(c.next()) {
          if(c.wasRemoved())
            rowLayer.getChildren().removeAll(c.getRemoved());
          if(c.wasAdded())
            rowLayer.getChildren().addAll(c.getFrom(), c.getAddedSubList());
        }
      });
    });
    
    rowLayer.getChildren().addListener((ListChangeListener.Change<? extends Node> c) -> {
      while(c.next()) {
        if(c.wasAdded()) {
          List<? extends Node> list = c.getAddedSubList();
          for(int i=0;i<list.size();i++)
            bindRow(i+c.getFrom(), (S)list.get(i));
        }
      }
    });
    
    focusedLayer.addEventFilter(EventType.ROOT, e -> {
      
      if(e.getEventType() == MouseEvent.MOUSE_ENTERED)
        focusedLayer.getChildren().setAll(focusedDayBlock, focusedRowBlock);
      
      if(e.getEventType() == MouseEvent.MOUSE_EXITED)
        focusedLayer.getChildren().clear();
      
      if(e.getEventType() == MouseEvent.MOUSE_MOVED) {
        
        ScaleRow r = rowAt(((MouseEvent)e).getScreenY());
        focusedRowBlock.setVisible(r != null);
        if(r != null)
          focusedRowBlock.setLayoutY(r.getLayoutY());
        
        ScalePeriod period = periodAtPoint(((MouseEvent)e).getScreenX(), ((MouseEvent)e).getScreenY());
        if(period != null) {
          focusedDayBlock.setPrefSize(period.getWidth(), period.getHeight());
          Bounds b = focusedLayer.screenToLocal(period.localToScreen(period.getBoundsInLocal()));
          focusedDayBlock.setLayoutX(b.getMinX());
          focusedDayBlock.setLayoutY(b.getMinY());
        }else {
          focusedDayBlock.setPrefSize(dayWidthProperty.getValue(), getHeader().heightProperty().getValue());
          focusedDayBlock.setLayoutX(((int)(((MouseEvent)e).getX()/dayWidthProperty().getValue()))*dayWidthProperty().getValue());
          focusedDayBlock.setLayoutY(((int)(((MouseEvent)e).getY()/getHeader().heightProperty().getValue()))*getHeader().heightProperty().getValue());
        }
      }
    });
    
    header.setOnMouseEntered(e -> focusedLayer.getChildren().addAll(focusedDayBlock));
    header.setOnMouseExited(e -> focusedLayer.getChildren().clear());
    header.addEventFilter(MouseEvent.ANY, e -> {
      ScalePeriod period = columnAtPoint(e.getScreenX(), e.getScreenY());
      if(e.getEventType() == MouseEvent.MOUSE_MOVED) {
        focusedDayBlock.setVisible(period != null);
        if(period != null) {
          focusedDayBlock.setPrefSize(period.getWidth(), focusedLayer.getHeight());
          Bounds b = focusedLayer.screenToLocal(period.localToScreen(period.getBoundsInLocal()));
          if(b != null) {
            focusedDayBlock.setLayoutX(b.getMinX());
            focusedDayBlock.setLayoutY(b.getMinY());
          }
        }
      }
      
      if(e.getEventType() == MouseEvent.MOUSE_PRESSED) {
        if(e.isShiftDown()) {
          Region currentRegion = new Region();
          if(period != null) {
            Bounds bounds = selectedLayer.screenToLocal(period.localToScreen(period.getBoundsInLocal()));
            currentRegion.setLayoutX(bounds.getMinX());
            currentRegion.setLayoutY(0);
            currentRegion.setPrefWidth(bounds.getWidth());
            currentRegion.setPrefHeight(selectedLayer.getHeight());
          }

          selectedLayer.getChildren().add(currentRegion);
          setSelectedBlock(joinRegions(selectedLayer.getChildren().toArray(new Region[0])));
        }else {
          if(period != null) {
            Bounds bounds = selectedLayer.screenToLocal(period.localToScreen(period.getBoundsInLocal()));
            double layoutX = bounds.getMinX();
            double layoutY = 0;
            double width = bounds.getWidth();
            double height = selectedLayer.getHeight();
            
            Region block = new Region();
            block.setLayoutX(layoutX);
            block.setLayoutY(layoutY);
            block.setPrefWidth(width-0.1);
            block.setPrefHeight(height-0.1);

            if(!e.isControlDown())
              clearSelectedBlocks();

            addSelectedBlock(block);
          }
        }
        fireSelected();
      }
    });
    
    selectedLayer.getChildren().addListener((ListChangeListener.Change<? extends Node> c) -> {
      List<S> rows = FXCollections.observableArrayList();
      selectedLayer.getChildren().stream().forEach(n -> {
        Bounds b = n.localToScreen(n.getBoundsInLocal());

        int row1 = rowIndexAt(Math.round(b.getMinY()));
        int row2 = rowIndexAt(Math.round(b.getMaxY()));

        for(int i=row1;i<=(row2 < getRows().size() ? row2 : (getRows().size()-1));i++)
          rows.add(getRows().get(i));
      });
      selectedRowProperty.setValue(rows);
    });
    
    selectedLayer.setOnMousePressed(e -> {
      if(e.isShiftDown()) {
        // Вычислить глобальный прямоугольник
        Region currentRegion = new Region();
        
        ScalePeriod period = periodAtPoint(e.getScreenX(), e.getScreenY());
        if(period != null) {
          Bounds bounds = selectedLayer.screenToLocal(period.localToScreen(period.getBoundsInLocal()));
          currentRegion.setLayoutX(bounds.getMinX());
          currentRegion.setLayoutY(bounds.getMinY());
          currentRegion.setPrefWidth(bounds.getWidth());
          currentRegion.setPrefHeight(bounds.getHeight());
        }else {
          currentRegion.setLayoutX(((int)(e.getX()/dayWidthProperty().getValue()))*dayWidthProperty().getValue());
          currentRegion.setLayoutY(((int)(e.getY()/getHeader().heightProperty().getValue()))*getHeader().heightProperty().getValue());
          currentRegion.setPrefWidth(dayWidthProperty.getValue());
          currentRegion.setPrefHeight(getHeader().heightProperty().getValue());
        }
        
        selectedLayer.getChildren().add(currentRegion);
        
        setSelectedBlock(joinRegions(selectedLayer.getChildren().toArray(new Region[0])));
      }else {
        // День под мышкой
        double layoutX = ((int)(e.getX()/dayWidthProperty().getValue()))*dayWidthProperty().getValue();
        double layoutY = ((int)(e.getY()/getHeader().heightProperty().getValue()))*getHeader().heightProperty().getValue();
        double width   = dayWidthProperty.getValue();
        double height  = getHeader().heightProperty().getValue();

        // Период под мышкой
        ScalePeriod period = periodAtPoint(e.getScreenX(), e.getScreenY());
        if(period != null) {
          Bounds bounds = selectedLayer.screenToLocal(period.localToScreen(period.getBoundsInLocal()));
          layoutX = bounds.getMinX();
          layoutY = bounds.getMinY();
          width   = bounds.getWidth();
          height  = bounds.getHeight();
        }
        Region block = new Region();
        block.setLayoutX(layoutX);
        block.setLayoutY(layoutY);
        block.setPrefWidth(width-0.1);
        block.setPrefHeight(height-0.1);
        
        if(!e.isControlDown())
          clearSelectedBlocks();

        addSelectedBlock(block);
      }
      fireSelected();
    });
    
    clientLayer.addEventHandler(EventType.ROOT,   e -> focusedLayer.fireEvent(e.copyFor(focusedLayer, focusedLayer)));
    focusedLayer.addEventHandler(EventType.ROOT,  e -> selectedLayer.fireEvent(e.copyFor(selectedLayer, selectedLayer)));
    selectedLayer.addEventHandler(EventType.ROOT, e -> rowLayer.fireEvent(e.copyFor(rowLayer, rowLayer)));
    rowLayer.addEventHandler(EventType.ROOT,      e -> rootLayer.fireEvent(e.copyFor(rootLayer, rootLayer)));
    
    rowLayer.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
      private Period deltaPeriod;
      private ScalePeriod sPeriod;
      private Period delay;
      private LocalDate start;
      private ScaleRow row;
      
      @Override
      public void handle(MouseEvent ev) {
        if(ev.getEventType() == MouseEvent.DRAG_DETECTED) {
          row = rowAt(ev.getScreenY());
          sPeriod = periodAtPoint(ev.getScreenX(), ev.getScreenY());
          start = sPeriod.startDateProperty().getValue();
          delay = Period.between(sPeriod.startDateProperty().getValue(), sPeriod.endDateProperty().getValue());
          LocalDate date = dateAt(ev.getScreenX());
          deltaPeriod = Period.between(sPeriod.startDateProperty().getValue(),date);
        }
        
        if(ev.getEventType() == MouseEvent.MOUSE_RELEASED) {
          if(sPeriod != null && !start.equals(sPeriod.startDateProperty().getValue())) {
            fireChangePeriodDates(start, start.plus(delay), (P)sPeriod);
            sPeriod = null;
          }
        }
        
        if(ev.getEventType() == MouseEvent.MOUSE_DRAGGED) {
          if(sPeriod != null) {
            
            LocalDate s = dateAt(ev.getScreenX()).minus(deltaPeriod);
            LocalDate e = s.plus(delay);
            
            if(((List<ScalePeriod>)row.getPeriods()).stream().filter(p -> 
                    !p.equals(sPeriod) 
                            && (p.startDateProperty().getValue().equals(s) || p.startDateProperty().getValue().equals(e) || p.endDateProperty().getValue().equals(s) || p.endDateProperty().getValue().equals(e) ||
                                    p.startDateProperty().getValue().isAfter(s) && p.startDateProperty().getValue().isBefore(e) ||
                                    p.endDateProperty().getValue().isAfter(s) && p.endDateProperty().getValue().isBefore(e) ||
                                    p.startDateProperty().getValue().isBefore(s) && p.endDateProperty().getValue().isAfter(e))
            ).findFirst().orElseGet(() -> null) == null) {
              sPeriod.startDateProperty().setValue(s);
              sPeriod.endDateProperty().setValue(e);
            }
          }
        }
      }
    });
  }
  
  public interface PeriodDateListener {
    public void changeDates(LocalDate oldStart, LocalDate oldEnd, ScalePeriod period);
  }
  
  private List<PeriodDateListener> periodDatesListeners = FXCollections.observableArrayList();

  public List<PeriodDateListener> periodDatesListeners() {
    return periodDatesListeners;
  }
  
  private void fireChangePeriodDates(LocalDate oldStart, LocalDate oldEnd, P period) {
    periodDatesListeners().forEach(h -> h.changeDates(oldStart, oldEnd, period));
  }

  public Pane getFocusedLayer() {
    return focusedLayer;
  }
  
  @Override
  public ObjectProperty<List<S>> selectedRowProperty() {
    return selectedRowProperty;
  }
  
  @Override
  public ObjectProperty<List<P>> selectedPeriodProperty() {
    return selectedPeriodProperty;
  }

  @Override
  public S getHeader() {
    return header;
  }

  @Override
  public Pane getRowLayer() {
    return rowLayer;
  }

  @Override
  public Pane getSelectedLayer() {
    return selectedLayer;
  }
  
  public ObjectProperty<ObservableList<ScaleRow<ScalePeriod>>> itemsProperty() {
    return itemsProperty;
  }
  
  public void scrollToCurrentDate() {
    rootScroll.setHvalue((double)previosMonthProperty().getValue()/(double)(previosMonthProperty().getValue()+futureMonthProperty().getValue()));
  }

  public Pane getClientLayer() {
    return clientLayer;
  }
  
  public ScrollPane getRootScroll() {
    return rootScroll;
  }
  
  public Pane createLayer(String styleClass) {
    Pane layer = new Pane();
    layer.getStyleClass().add(styleClass);
    rootLayer.getChildren().add(0,layer);
    layer.setLayoutX(0);
    layer.setLayoutY(0);
    layer.prefWidthProperty().bind(rootLayer.widthProperty());
    layer.prefHeightProperty().bind(rootLayer.heightProperty());
    layer.toFront();
    return layer;
  }
  
  private void initHeader() {
    try {
      header.getStyleClass().add("scale-header");
      header.prefWidthProperty().bind(Bindings.createDoubleBinding(betweenX(leftDateProperty.getValue(), rightDateProperty.getValue())::doubleValue, leftDateProperty, rightDateProperty));
      header.getChildren().clear();
      for(int i=0;i<=previosMonthProperty().getValue()+futureMonthProperty.getValue();i++) {
        LocalDate date = leftDateProperty().getValue().plusMonths(i);
        P column = (P)createPeriod(date, LocalDate.of(date.getYear(), date.getMonthValue(), date.lengthOfMonth())).setValue("column", true);
        column.getStyleClass().setAll("scale-header-column");
        String text = column.startDateProperty().getValue().getMonth().getDisplayName(TextStyle.FULL_STANDALONE, new Locale("RU"))+" "+column.startDateProperty().getValue().getYear();
        Label label = new Label(text);
        label.setLayoutX(0);
        label.setLayoutY(0);
        column.getChildren().add(label);
        header.getChildren().add(column);
      }
    }catch(Exception ex) {
      MsgTrash.out(ex);
    }
  }
  
  @Override
  public void fireSelected() {
    selectedBlockChanged();
  }
  
  private void selectedBlockChanged() {
    selectedPeriodProperty.setValue(getSelectedPeriods(getRows()));
  }
  
  public List<P> getSelectedPeriods(Collection<S> rows) {
    List<P> ps = FXCollections.observableArrayList();
    getSelectedLayer().getChildren().stream().map(b -> (Region)b).forEach(b -> {
      Bounds selectedBounds = getRowLayer().localToScreen(new BoundingBox(b.getLayoutX()+0.5, b.getLayoutY()+0.5, b.getPrefWidth()-0.5, b.getPrefHeight()-0.5));
      (rows.isEmpty() ? getRows() : rows).forEach(r -> {
        r.getPeriods().forEach(p -> {
          Bounds periodBounds = ((ScalePeriod)p).localToScreen(((ScalePeriod)p).getBoundsInLocal());
          if(periodBounds.intersects(selectedBounds) && p.isVisible())
            ps.add(p);
        });
      });
    });
    return ps;
  }
  
  public List<P> getSelectedPeriods(S... rows) {
    return getSelectedPeriods(Arrays.asList(rows));
  }
  
  @Override
  public OPool<S> getRowPool() {
    return rowPool;
  }
  
  @Override
  public OPool<P> getPeriodPool() {
    return periodPool;
  }
  
  @Override
  public ReadOnlyObjectProperty<LocalDate> leftDateProperty() {
    return leftDateProperty;
  }
  
  @Override
  public ReadOnlyObjectProperty<LocalDate> rightDateProperty() {
    return rightDateProperty;
  }
  
  @Override
  public IntegerProperty previosMonthProperty() {
    return previosMonthProperty;
  }
  
  @Override
  public IntegerProperty futureMonthProperty() {
    return futureMonthProperty;
  }
  
  @Override
  public IntegerProperty dayWidthProperty() {
    return dayWidthProperty;
  }
  
  public void clear() {
    getRows().forEach(r -> {
      periodPool.returnObject(r.getPeriods());
      r.unbindAllPeriods();
      r.getChildren().clear();
    });

    rowPool.returnObject(getRows());
    unbindAllRows();

    rowLayer.getChildren().clear();
    selectedLayer.getChildren().clear();
    focusedLayer.getChildren().clear();
    clientLayer.getChildren().clear();
  }

  @Override
  public void finaly() {
    clear();
    rowPool.clear();
    rowPool.close();
    periodPool.clear();
    periodPool.close();
    
    rowPool    = null;
    periodPool = null;
  }
}