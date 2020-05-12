package division.fx.scale.test;

import division.fx.gui.FXDisposable;
import division.fx.util.MsgTrash;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
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
import javafx.event.EventType;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

public class Scale<S extends ScaleRow<P>, P extends ScalePeriod> implements ScaleInterface, FXDisposable {
  private final Pane periodLayer   = new Pane();
  
  private final Pane selectedLayer = createLayer("selected-layer");
  private final Pane focusedLayer  = createLayer("focused-layer");
  private final Pane clientLayer   = createLayer("client-layer");
  //private ObservableList<Pane> layers = FXCollections.observableArrayList(selectedLayer, );
  
  private final ObjectProperty<ObservableList<ScaleRow<ScalePeriod>>> itemsProperty = new SimpleObjectProperty<>();
  
  private final ScrollPane viewScroll = new ScrollPane(periodLayer);
  
  private final OPool<S> rowPool;
  private final OPool<P> periodPool;
  
  //private final DoubleProperty            rowHeightProperty    = new SimpleDoubleProperty(20);
  private final ObjectProperty<LocalDate> leftDateProperty     = new SimpleObjectProperty<>();
  private final ObjectProperty<LocalDate> rightDateProperty    = new SimpleObjectProperty<>();
  private final IntegerProperty           previosMonthProperty = new SimpleIntegerProperty(12);
  private final IntegerProperty           futureMonthProperty  = new SimpleIntegerProperty(12);
  private final IntegerProperty           dayWidthProperty     = new SimpleIntegerProperty(5);
  
  private final Label                  dayLabel                = new Label();
  
  private final Region                 focusedDayBlock         = new Region();
  private final Region                 focusedRowBlock         = new Region();
  
  private final Region                 currentDateBlock = new Region();
  
  private final ObjectProperty<List<S>> selectedRowProperty    = new SimpleObjectProperty(FXCollections.observableArrayList());
  private final ObjectProperty<List<P>> selectedPeriodProperty = new SimpleObjectProperty(FXCollections.observableArrayList());
  
  /*public S createRow(P... periods) throws Exception {
    return createRow(Arrays.asList(periods));
  }
  
  public S createRow(Collection<P> periods) throws Exception {
    S row = rowPool.borrowObject();
    row.scaleProperty().setValue(this);
    row.getChildren().addAll(periods);
    return row;
  }
  
  public P createPeriod() throws Exception {
    return periodPool.borrowObject();
  }
  
  public P createPeriod(LocalDate startDate, Period period) throws Exception {
    return createPeriod(startDate, startDate.plusDays(period.getDays()).plusMonths(period.getMonths()).plusYears(period.getYears()));
  }
  
  public P createPeriod(LocalDate startDate, LocalDate endDate) throws Exception {
    P period = createPeriod();
    period.startDateProperty().setValue(startDate);
    period.endDateProperty().setValue(endDate);
    return period;
  }*/
  
  public Scale() {
    this(ScaleRow.class, ScalePeriod.class);
  }

  public Scale(Class rowClass, Class periodClass) {
    focusedLayer.getChildren().addAll(focusedDayBlock, focusedRowBlock);
    focusedRowBlock.setLayoutX(0);
    focusedRowBlock.prefWidthProperty().bind(focusedLayer.widthProperty());
    focusedRowBlock.prefHeightProperty().bind(getHeader().heightProperty());

    focusedLayer.setOnMouseMoved(e -> {
      ScaleRow r = rowAt(e.getScreenY());
      focusedRowBlock.setVisible(r != null);
      if(r != null)
        focusedRowBlock.setLayoutY(r.getLayoutY());
      
      ScalePeriod period = periodAtPoint(e.getScreenX(), e.getScreenY());
      if(period == null) {
        period = columnAtPoint(e.getScreenX(), e.getScreenY());
        if(period != null)
          focusedDayBlock.setPrefSize(period.getWidth(), focusedLayer.getHeight());
      }else focusedDayBlock.setPrefSize(period.getWidth(), period.getHeight());
      
      if(period != null) {
        Bounds b = focusedLayer.screenToLocal(period.localToScreen(period.getBoundsInLocal()));
        focusedDayBlock.setLayoutX(b.getMinX());
        focusedDayBlock.setLayoutY(b.getMinY());
      }else {
        focusedDayBlock.setPrefSize(dayWidthProperty.getValue(), getHeader().heightProperty().getValue());
        focusedDayBlock.setLayoutX(((int)(e.getX()/dayWidthProperty().getValue()))*dayWidthProperty().getValue());
        focusedDayBlock.setLayoutY(((int)(e.getY()/getHeader().heightProperty().getValue()))*getHeader().heightProperty().getValue());
      }
    });
    
    focusedLayer.setOnMouseExited(e -> focusedLayer.getChildren().forEach(n -> n.setVisible(false)));
    focusedLayer.setOnMouseEntered(e -> focusedLayer.getChildren().forEach(n -> n.setVisible(true)));
    
    periodLayer.getStyleClass().add("period-layer");
    
    selectedLayer.addEventFilter(EventType.ROOT, e -> {
      focusedLayer.fireEvent(e);
      periodLayer.fireEvent(e);
      clientLayer.fireEvent(e);
    });
    
    rowPool = new OPool(rowClass, 100);
    periodPool = new OPool(periodClass, 100);
    
    viewScroll.getStyleClass().add("scale-scroll");
    periodLayer.getStyleClass().add("scale");
    currentDateBlock.getStyleClass().add("current-date-block");
    
    leftDateProperty.bind(Bindings.createObjectBinding(() -> LocalDate.now().minusMonths(previosMonthProperty().getValue()).withDayOfMonth(1), previosMonthProperty));
    rightDateProperty.bind(Bindings.createObjectBinding(() -> {
      LocalDate right = LocalDate.now().plusMonths(futureMonthProperty().getValue()).withDayOfMonth(1);
      return right.withDayOfMonth(right.lengthOfMonth());
    }, futureMonthProperty));
    
    periodLayer.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> (double)betweenX(leftDateProperty().getValue(), rightDateProperty().getValue()), leftDateProperty(), rightDateProperty(), dayWidthProperty()));
    
    periodLayer.getChildren().addListener((ListChangeListener.Change<? extends Node> c) -> {
      while(c.next()) {
        if(c.wasAdded()) {
          for(int i=0;i<c.getAddedSubList().size();i++) {
            if(c.getAddedSubList().get(i) instanceof ScaleRow) {
              Node n = c.getAddedSubList().get(i);
              bindRow(c.getFrom()-1+i, (S)n);
            }
          }
        }
      }
    });
    
    periodLayer.getChildren().add(currentDateBlock);
    currentDateBlock.layoutYProperty().bind(getHeader().heightProperty());
    currentDateBlock.prefWidthProperty().bind(dayWidthProperty);
    currentDateBlock.prefHeightProperty().bind(periodLayer.heightProperty().subtract(getHeader().heightProperty()));
    currentDateBlock.layoutXProperty().bind(Bindings.createDoubleBinding(() -> (double)betweenX(leftDateProperty().getValue(), LocalDate.now()), dayWidthProperty, leftDateProperty));
    currentDateBlock.toFront();
    
    leftDateProperty.addListener((ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) -> initHeader());
    rightDateProperty.addListener((ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) -> initHeader());
    dayWidthProperty.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> initHeader());
    
    initHeader();
    initEvent();
  }
  
  public Pane createLayer(String styleClass) {
    Pane layer = new Pane();
    layer.getStyleClass().add(styleClass);
    periodLayer.getChildren().add(0,layer);
    layer.setLayoutX(0);
    layer.setLayoutY(0);
    layer.prefWidthProperty().bind(periodLayer.widthProperty());
    layer.prefHeightProperty().bind(periodLayer.heightProperty());
    layer.toFront();
    return layer;
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
  public ReadOnlyObjectProperty<List<S>> selectedRowProperty() {
    return selectedRowProperty;
  }
  
  @Override
  public ReadOnlyObjectProperty<List<P>> selectedPeriodProperty() {
    return selectedPeriodProperty;
  }
  
  private void initEvent() {
    itemsProperty.addListener((ObservableValue<? extends ObservableList<ScaleRow<ScalePeriod>>> observable, ObservableList<ScaleRow<ScalePeriod>> oldValue, ObservableList<ScaleRow<ScalePeriod>> newValue) -> {
      clear();
      periodLayer.getChildren().setAll(newValue);
      initHeader();
      periodLayer.getChildren().add(clientLayer);
      periodLayer.getChildren().add(focusedLayer);
      periodLayer.getChildren().add(selectedLayer);
      
      newValue.addListener((ListChangeListener.Change<? extends ScaleRow<ScalePeriod>> c) -> {
        while(c.next()) {
          if(c.wasRemoved())
            periodLayer.getChildren().removeAll(c.getRemoved());
          if(c.wasAdded())
            periodLayer.getChildren().addAll(c.getFrom(), c.getAddedSubList());
        }
      });
    });
    
    selectedLayer.getChildren().addListener((ListChangeListener.Change<? extends Node> c) -> {
      List<S> rows = FXCollections.observableArrayList();
      selectedLayer.getChildren().stream().forEach(n -> {
        Bounds b = n.localToScreen(n.getBoundsInLocal());

        int row1 = rowIndexAt(Math.round(b.getMinY()));
        int row2 = rowIndexAt(Math.round(b.getMaxY()));

        for(int i=row1;i<=row2;i++)
          rows.add(getRows().get(i));
      });
      selectedRowProperty.setValue(rows);
    });
    
    selectedLayer.setOnMousePressed(e -> {
      if(e.isShiftDown()) {
        // Вычислить глобальный прямоугольник
        //Region globalRegion  = new Region();
        Region currentRegion = new Region();
        
        ScalePeriod period = columnAtPoint(e.getScreenX(), e.getScreenY());
        if(period != null) {
          Bounds bounds = selectedLayer.screenToLocal(period.localToScreen(period.getBoundsInLocal()));
          currentRegion.setLayoutX(bounds.getMinX());
          currentRegion.setLayoutY(0);
          currentRegion.setPrefWidth(bounds.getWidth());
          currentRegion.setPrefHeight(selectedLayer.getHeight());
        }else {
          period = periodAtPoint(e.getScreenX(), e.getScreenY());
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
        ScalePeriod period = columnAtPoint(e.getScreenX(), e.getScreenY());
        if(period != null) {
          Bounds bounds = selectedLayer.screenToLocal(period.localToScreen(period.getBoundsInLocal()));
          layoutX = bounds.getMinX();
          layoutY = 0;
          width = bounds.getWidth();
          height = selectedLayer.getHeight();
        }else {
          period = periodAtPoint(e.getScreenX(), e.getScreenY());
          if(period != null) {
            Bounds bounds = selectedLayer.screenToLocal(period.localToScreen(period.getBoundsInLocal()));
            layoutX = bounds.getMinX();
            layoutY = bounds.getMinY();
            width   = bounds.getWidth();
            height  = bounds.getHeight();
          }
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
      selectedBlockChanged();
    });
  }

  private void initHeader() {
    try {
      ScaleRow header = (ScaleRow) periodLayer.getChildren().stream().filter(n -> n instanceof ScaleRow && ((ScaleRow)n).isNotNull("header") && ((ScaleRow)n).is("header")).findFirst().orElseGet(() -> null);

      if(header == null) {
        header = (ScaleRow) new ScaleRow(this).setValue("header", true);
        header.getStyleClass().add("scale-header");
        periodLayer.getChildren().add(0, header);
        unbindAllRows();
        bindRow(0, (S)header);
        
        header.layoutYProperty().bind(Bindings.createDoubleBinding(() -> {
          System.out.println(viewScroll.getViewportBounds().getMinY());
          return viewScroll.getViewportBounds().getMinY()*-1;
        }, viewScroll.viewportBoundsProperty(), viewScroll.vvalueProperty()));
        
        List<S> rows = getRows();
        for(int i=0;i<rows.size();i++)
          bindRow(i+1, rows.get(i));
      }

      header.getChildren().clear();
      for(int i=0;i<=previosMonthProperty.getValue()+futureMonthProperty.getValue();i++) {
        LocalDate date = leftDateProperty.getValue().plusMonths(i);
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
  public ScalePeriod columnAtPoint(double screenX, double screenY) {
    return getHeader().getPeriods().stream().filter(p -> {
      return p.localToScreen(p.getBoundsInLocal()).contains(screenX, screenY);
    }).findFirst().orElseGet(() -> null);
  }
  
  @Override
  public ScaleRow<ScalePeriod> getHeader() {
    return (ScaleRow)periodLayer.getChildren().stream().filter(n -> n instanceof ScaleRow && ((ScaleRow)n).isNotNull("header")).findFirst().orElseGet(() -> {
      initHeader();
      return getHeader();
    });
  }
  
  @Override
  public int rowIndexAt(double screenY) {
    return (int)((periodLayer.screenToLocal(0, screenY).getY()-getHeader().getHeight())/getHeader().heightProperty().getValue());
  }
  
  @Override
  public S rowAt(double screenY) {
    int index = rowIndexAt(screenY);
    List<S> rows = getRows();
    return index >= 0 && !rows.isEmpty() && index <= rows.size() ? rows.get(rowIndexAt(screenY)) : null;
  }
  
  @Override
  public ScalePeriod periodAtPoint(double screenX, double screenY) {
    S row = rowAt(screenY);
    return row == null ? null : row.getPeriods().stream().filter(p -> {
      return p.localToScreen(p.getBoundsInLocal()).contains(screenX, screenY);
    }).findFirst().orElseGet(() -> null);
  }
  
  private void selectedBlockChanged() {
    List<S> rows = getRows();
    List<P> ps = FXCollections.observableArrayList();
    selectedLayer.getChildren().stream().map(b -> (Region)b).forEach(b -> {
      Bounds selectedBounds = periodLayer.localToScreen(new BoundingBox(b.getLayoutX()+0.5, b.getLayoutY()+0.5, b.getPrefWidth()-0.5, b.getPrefHeight()-0.5));
      rows.stream().forEach(r -> {
        r.getPeriods().forEach(p -> {
          Bounds periodBounds = ((ScalePeriod)p).localToScreen(((ScalePeriod)p).getBoundsInLocal());
          if(periodBounds.intersects(selectedBounds))
            ps.add(p);
        });
      });
    });
    selectedPeriodProperty.setValue(ps);
  }
  
  @Override
  public List<S> getRows() {
    return periodLayer.getChildren().stream().filter(n -> n instanceof ScaleRow && ((ScaleRow)n).isNull("header")).map(n -> (S)n).collect(Collectors.toList());
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
  
  /*public DoubleProperty rowHeightProperty() {
    return rowHeightProperty;
  }*/
  
  public ObjectProperty<ObservableList<ScaleRow<ScalePeriod>>> itemsProperty() {
    return itemsProperty;
  }

  public void clear() {
    getRows().forEach(r -> {
      periodPool.returnObject(r.getPeriods());
      r.unbindAllPeriods();
      r.getChildren().clear();
    });

    rowPool.returnObject(getRows());
    unbindAllRows();
    periodLayer.getChildren().clear();
    selectedLayer.getChildren().clear();
    focusedLayer.getChildren().clear();
    clientLayer.getChildren().clear();
  }
  
  public void scrollToCurrentDate() {
    viewScroll.setHvalue((double)previosMonthProperty().getValue()/(double)(previosMonthProperty().getValue()+futureMonthProperty().getValue()));
  }
  
  /*public Pane getViewPane() {
    return periodLayer;
  }*/

  public ScrollPane getViewScroll() {
    return viewScroll;
  }

  public Pane getClientLayer() {
    return clientLayer;
  }

  @Override
  public void finaly() {
    clear();
    rowPool.clear();
    rowPool.close();
    periodPool.clear();
    periodPool.close();
  }

  @Override
  public Pane getRowLayer() {
    return periodLayer;
  }

  @Override
  public Pane getSelectedLayer() {
    return selectedLayer;
  }

  @Override
  public void fireSelected() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
}