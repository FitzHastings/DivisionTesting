package division.fx.scale;

import division.fx.PropertyMap;
import division.fx.FXUtility;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TreeMap;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import org.apache.commons.lang3.ArrayUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.input.KeyCode;

public class DateScale<R extends DateRow, T extends DatePeriod> extends VBox implements Initializable {
  private final Pane pane   = new Pane();
  private final VBox table  = new VBox();
  private final ScrollPane scroll = new ScrollPane(pane);
  
  private final DateRow    header;
  private final ScrollPane headerScroll = new ScrollPane();
  
  public final IntegerProperty previosMonthProperty = new SimpleIntegerProperty(10);
  public final IntegerProperty futureMonthProperty  = new SimpleIntegerProperty(10);
  public final IntegerProperty dayWidthProperty     = new SimpleIntegerProperty(5);
  
  private final ObjectProperty<LocalDate> mouseMoveDate = new SimpleObjectProperty();
  //private final Region 
  
  private final String[] months = new String[]{"янв","фев","март","апр","май","июнь","июль","авг","сент","окт","нояб","декаб"};
  
  private ObjectProperty<R> selectedRow = new SimpleObjectProperty();
  private Region[] columnSelectedRegions = new Region[0];
  private Region columnRegion = new Region();
  private Region currentDateRegion = new Region();
  private boolean active = true;
  public ObservableList<EventHandler> selectedPeriodsHandlers = FXCollections.<EventHandler>observableArrayList();
  
  public DateScale() {
    header = new DateRow(this);
    FXMLLoader loader = new FXMLLoader(getClass().getResource("DateScale.fxml"));
    loader.setRoot(this);
    loader.setController(this);
    try {
      loader.load();
    }catch(Exception ex) {
      ex.printStackTrace();
    }
    
    currentDateRegion.addEventHandler(MouseEvent.ANY, e -> table.fireEvent(e));
  }
  
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    FXUtility.initMainCss(this);
    headerScroll.hmaxProperty().bind(scroll.hmaxProperty());
    headerScroll.hvalueProperty().bind(scroll.hvalueProperty());
    previosMonthProperty.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> recalculate(oldValue.intValue(), futureMonthProperty.getValue()));
    futureMonthProperty.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> recalculate(previosMonthProperty.getValue(), oldValue.intValue()));
    columnRegion.prefHeightProperty().bind(pane.heightProperty());
    initEvents();
  }
  
  private void recalculate(int oldprevios, int oldfuture) {
    recalculate = true;
    Bounds b = scroll.getViewportBounds();
    double x = b.getMinX()*-1+b.getWidth()/2;
    LocalDate sdate = x == 0 ? LocalDate.now() : getDate(getStartTime(oldprevios), x);
    
    pane.setOnScrollStarted(e -> e.consume());
    
    headerScroll.setContent(header);
    getChildren().clear();
    getChildren().addAll(headerScroll, scroll);
    VBox.setVgrow(scroll, Priority.ALWAYS);
    
    pane.getChildren().clear();
    pane.getChildren().add(table);
    
    header.getChildren().clear();
    
    LocalDate start = getStartTime();
    LocalDate end   = getEndTime();
    while(start.isBefore(end) || start.isEqual(end)) {
      header.add(createColumn(start, start.withDayOfMonth(start.lengthOfMonth()), months[start.getMonthValue()-1]+" "+start.getYear()));
      start = start.plusMonths(1);
    }
    
    table.prefWidthProperty().unbind();
    table.minWidthProperty().unbind();
    table.maxWidthProperty().unbind();
    
    header.prefWidthProperty().unbind();
    header.minWidthProperty().unbind();
    header.maxWidthProperty().unbind();
    
    table.prefWidthProperty().bind(dayWidthProperty.multiply(getDayCount(getStartTime(), getEndTime())));
    table.minWidthProperty() .bind(dayWidthProperty.multiply(getDayCount(getStartTime(), getEndTime())));
    table.maxWidthProperty() .bind(dayWidthProperty.multiply(getDayCount(getStartTime(), getEndTime())));
    
    header.prefWidthProperty().bind(dayWidthProperty.multiply(getDayCount(getStartTime(), getEndTime())));
    header.minWidthProperty() .bind(dayWidthProperty.multiply(getDayCount(getStartTime(), getEndTime())));
    header.maxWidthProperty() .bind(dayWidthProperty.multiply(getDayCount(getStartTime(), getEndTime())));
    
    
    pane.setOnScrollStarted(null);
    scroll.setHvalue(getX(sdate)/table.getPrefWidth());
    
    
    pane.getChildren().add(currentDateRegion);
    currentDateRegion.setLayoutX(getX(LocalDate.now()));
    currentDateRegion.setLayoutY(0);
    currentDateRegion.prefWidthProperty().bind(dayWidthProperty());
    currentDateRegion.prefHeightProperty().bind(table.heightProperty());
    currentDateRegion.toFront();
    
    recalculate = false;
  }
  
  boolean recalculate = false;
  public void scrollToDate(LocalDate date) {
    scroll.setHvalue((getX(date)+scroll.getWidth()/2)/table.getPrefWidth());
  }
  
  public ObservableList<T> getPeriods() {
    ObservableList<T> periods = FXCollections.observableArrayList();
    for(PropertyMap dr:getItems())
      periods.addAll(((R)dr).getPeriods());
    return periods;
  }
  
  private DatePeriod createColumn(LocalDate start, LocalDate end, String title) {
    DatePeriod column = new DatePeriod(start, end);
    
    column.setOnMouseEntered((MouseEvent event) -> {
      columnRegion.setLayoutX(column.getLayoutX());
      columnRegion.setLayoutY(column.getLayoutY());
      columnRegion.setPrefWidth(column.getWidth());
      pane.getChildren().add(columnRegion);
    });
    column.setOnMouseExited((MouseEvent event) -> pane.getChildren().remove(columnRegion));
    
    column.setOnMousePressed((MouseEvent event) -> {
      ObservableList<T> selectedPeriods = getSelectedPeriods();
      boolean selected = column.selectedProperty().get();
      
      if(!event.isControlDown() && !event.isShiftDown())
        clearSelection();
      
      if(event.isShiftDown()) {
        DatePeriod[] ps = null;
        boolean e = false;
        for(int i=0;i<header.getChildren().size();i++) {
          if(header.getChildren().get(i).equals(column))
            ps = new DatePeriod[0];
          if(ps != null) {
            e = ((DatePeriod)header.getChildren().get(i)).selectedProperty().get() != selected;
            if(!e)
              ps = ArrayUtils.add(ps, (DatePeriod)header.getChildren().get(i));
            else break;
          }
        }
        if(ps == null || ps.length == 0 || !e) {
          ps = null;
          for(int i=header.getChildren().size()-1;i>=0;i--) {
            if(header.getChildren().get(i).equals(column))
              ps = new DatePeriod[0];
            if(ps != null) {
              e = ((DatePeriod)header.getChildren().get(i)).selectedProperty().get() != selected;
              if(!e)
                ps = ArrayUtils.add(ps, (DatePeriod)header.getChildren().get(i));
              else break;
            }
          }
        }
        if(ps != null && ps.length > 0 && e)
          for(DatePeriod p:ps)
            p.selectedProperty().set(!selected);
      }
      
      column.selectedProperty().set(!selected);
      
      fireSelect(selectedPeriods);
      //selectedHandlers.stream().forEach(h -> h.handle(new Event(EventType.ROOT)));
    });
    
    column.selectedProperty().addListener((ob, ol, nw) -> {
      pane.getChildren().removeAll(columnSelectedRegions);
      
      final List<List<DatePeriod>> unionColumns = new ArrayList<>();
      for(int i=0;i<header.getChildren().size();i++) {
        DatePeriod col = (DatePeriod)header.getChildren().get(i);
        if(col.selectedProperty().get()) {
          List<DatePeriod> periods;
          
          if(i > 0 && ((DatePeriod)header.getChildren().get(i-1)).selectedProperty().get()) {
            if(!unionColumns.isEmpty())
              periods = unionColumns.get(unionColumns.size()-1);
            else
              unionColumns.add(periods = new ArrayList());
            periods.add(col);
          }else {
            unionColumns.add(periods = new ArrayList());
            periods.add(col);
          }
        }
      }
      
      columnSelectedRegions = new Region[0];
      unionColumns.stream().forEach(periods -> {
        Region region = new Region();
        region.addEventFilter(EventType.ROOT, (Event event) -> table.fireEvent(event));
        region.getStyleClass().add("column-selected-region");
        region.layoutXProperty().bind(periods.get(0).layoutXProperty());
        region.layoutYProperty().bind(periods.get(0).layoutYProperty());
        region.prefHeightProperty().bind(pane.heightProperty());
        
        DoubleBinding binding = new DoubleBinding() {
          @Override
          protected double computeValue() {
            Double w = 0d;
            for(DatePeriod c:periods)
              w += c.widthProperty().get();
            return w;
          }
        };
        
        region.prefWidthProperty().bind(binding);
        columnSelectedRegions = ArrayUtils.add(columnSelectedRegions, region);
      });
      
      pane.getChildren().addAll(columnSelectedRegions);
      
      Bounds b = new BoundingBox(column.getLayoutX(), column.getLayoutY(), column.getWidth(), pane.getHeight());
      for(DatePeriod period:getPeriods()) {
        Bounds p = period.getBoundsInParent();
        Shape shape = Rectangle.intersect(new Rectangle(b.getMinX(), b.getMinY(), b.getWidth(), b.getWidth()), 
                new Rectangle(p.getMinX(), p.getMinY(), p.getWidth(), p.getWidth()));
        if(shape.getBoundsInLocal().getWidth() > 0)
          period.selectedProperty().set(nw);
      }
    });
    
    StackPane p = new StackPane(new Label(title));
    p.prefWidthProperty().bind(column.prefWidthProperty());
    p.prefHeightProperty().bind(column.prefHeightProperty());
    column.getChildren().add(p);
    
    return column;
  }
  
  public ObservableList<T> getSelectedPeriods() {
    ObservableList<T> selectedPeriods = FXCollections.observableArrayList();
    getPeriods().stream().forEach(p -> {
      if(p.selectedProperty().getValue())
        selectedPeriods.add(p);
    });
    return selectedPeriods;
  }
  
  public LocalDate getEndTime() {
    return getEndTime(futureMonthProperty.getValue());
  }
  
  public LocalDate getEndTime(int future) {
    LocalDate now = LocalDate.now().plusMonths(future);
    return now.withDayOfMonth(now.lengthOfMonth());
  }
  
  public LocalDate getStartTime() {
    return getStartTime(previosMonthProperty.getValue());
  }
  
  public LocalDate getStartTime(int previos) {
    return LocalDate.now().minusMonths(previos).withDayOfMonth(1);
  }
  
  public void setItems(ObservableList<R> its) {
    table.getChildren().clear();
    recalculate(previosMonthProperty.getValue(), futureMonthProperty.getValue());
    its.addListener((ListChangeListener.Change<? extends R> c) -> table.getChildren().setAll(its));
  }
  
  public ObservableList<R> getItems() {
    return FXCollections.observableArrayList((R[])table.getChildren().toArray(new DateRow[0]));
  }
  
  public void remove(T... datePeriods) {
    getItems().stream().forEach(dr -> {
      for(Object dp:dr.getPeriods().toArray(new DatePeriod[0])) {
        if(ArrayUtils.contains(datePeriods, dp))
          dr.remove((DatePeriod)dp);
      }
    });
  }
  
  public void clearDaySelection() {
  }
  
  public void clear() {
    clearSelection();
    getItems().clear();
    fireSelect(null);
  }
  
  public LocalDate getDate(double x) {
    return getDate(getStartTime(), x);
  }
  
  public LocalDate getDate(LocalDate startdate, double x) {
    return x >= 0 ? startdate.plusDays((int)(x/dayWidthProperty.get()) - (x < dayWidthProperty.get() ? 0 : 1)) : null;
  }
  
  public double getX(LocalDate date) {
    return getDayCountBefore(date) * dayWidthProperty.get();
  }
  
  public long getDayCountBefore(LocalDate time) {
    return getDayCount(getStartTime(), time);
  }
  
  public long getDayCount(LocalDate start, LocalDate end) {
    return ChronoUnit.DAYS.between(start, end)+1;
  }
  
  public T periodAtPoint(double tableX, double tableY) {
    for(PropertyMap r:getItems())
      if(((R)r).getBoundsInParent().contains(tableX, tableY))
        return (T)((R)r).periodAtDate(getDate(tableX));
    return null;
  }
  
  public T periodAtPoint(Point2D tablePoint) {
    return periodAtPoint(tablePoint.getX(), tablePoint.getY());
  }
  
  public R rowAtPoint(double tableY) {
    return rowAtPoint(new Point2D(0, tableY));
  }
  
  public R rowAtPoint(Point2D tablePoint) {
    for(R r:getItems()) {
      if(r.getBoundsInParent().contains(tablePoint.getX(), tablePoint.getY()))
        return r;
    }
    return null;
  }
  
  public R rowAtPeriod(DatePeriod period) {
    return rowAtPoint(period.getParent().getLayoutY()+period.getHeight()/2);
  }
  
  public int rowIndexAtPoint(double tableY) {
    for(int i=0;i<getItems().size();i++) {
      if(getItems().get(i).getBoundsInParent().contains(5, tableY))
        return i;
    }
    return -1;
  }
  
  public void clearSelection() {
    for(Node n:header.getChildren())
      ((T)n).selectedProperty().set(false);
    for(T datePeriod:getPeriods())
      datePeriod.selectedProperty().set(false);
  }
  
  public final ObservableList<T> selectedPeriods = FXCollections.observableArrayList();
  
  public void fireSelect(ObservableList<T> oldSelectedPeriods) {
    if(oldSelectedPeriods == null)
      selectedPeriodsHandlers.stream().forEach(h -> h.handle(new Event(EventType.ROOT)));
    else {
      selectedPeriods.setAll(getSelectedPeriods());
      selectedPeriods.sorted();
      oldSelectedPeriods.sorted();
      if(!selectedPeriods.equals(oldSelectedPeriods))
        selectedPeriodsHandlers.stream().forEach(h -> h.handle(new Event(EventType.ROOT)));
    }
  }
  
  private Point2D     startDragPoint   = new Point2D(0, 0);
  private Region      selectedBlock    = null;
  private Label       mouseMovedLabel  = new Label();
  private StackPane   mouseMovedBlock  = new StackPane(mouseMovedLabel);
  
  private Region      selectedDayBlock = new Region();
  public ObjectProperty<LocalDate> selectedDayProperty = new SimpleObjectProperty<>();
  
  private Rectangle d = null;
  
  public ObservableList<DatePeriod> datePeriods = FXCollections.observableArrayList();
    
  private void initEvents() {
    table.getChildren().addListener((ListChangeListener.Change<? extends Node> c) -> {
      while(c.next()) {
        if(c.wasAdded()) {
          c.getAddedSubList().stream().filter(n -> n instanceof DateRow).forEach(n -> {
            ((DateRow<DatePeriod>)n).getPeriods().addListener((ListChangeListener.Change<? extends DatePeriod> c1) -> {
              datePeriods.setAll(getPeriods());
            });
          });
        }else datePeriods.setAll(getPeriods());
      }
    });
    
    selectedDayBlock.setLayoutY(0);
    selectedDayBlock.prefWidthProperty().bind(dayWidthProperty());
    
    mouseMovedLabel.textProperty().bind(Bindings.createStringBinding(() -> 
                mouseMoveDateProperty().getValue()==null?"":mouseMoveDateProperty().getValue().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), 
                mouseMoveDateProperty()));
    
    table.setOnMouseExited((event) -> Platform.runLater(() -> pane.getChildren().remove(mouseMovedBlock)));
    
    /*table.setOnMousePressed(e -> {
      selectedRow.setValue(rowAtPoint(e.getY()));
      T period = periodAtPoint(e.getX(), e.getY());
      if(period == null) {
        LocalDate date = selectedDayProperty.getValue();
        if(getDate(e.getX()).equals(date))
          selectedDayProperty.setValue(null);
        selectedDayProperty.setValue(getDate(e.getX()));
      }else selectedDayProperty.setValue(null);
    });*/
    
    dayWidthProperty.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
      LocalDate date = selectedDayProperty.getValue();
      selectedDayProperty.setValue(null);
      selectedDayProperty.setValue(date);
    });
    
    selectedDayProperty.addListener((ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) -> {
      if(selectedDayBlock.getParent() != null)
        ((Pane)selectedDayBlock.getParent()).getChildren().remove(selectedDayBlock);
      if(newValue != null) {
        selectedRow.getValue().getChildren().add(selectedDayBlock);
        selectedDayBlock.setPrefHeight(selectedRow.getValue().getHeight());
        selectedDayBlock.setLayoutX(getX(newValue));
      }
    });
    
    table.setOnMouseMoved((MouseEvent event) -> {
      Platform.runLater(() -> {
        LocalDate date = getDate(event.getX());
        if(date != null && mouseMoveDate != null && !date.equals(mouseMoveDate.get()))
          mouseMoveDate.setValue(date);
      });
      
      if(selectedBlock == null) {
        if(!pane.getChildren().contains(mouseMovedBlock))
          pane.getChildren().add(mouseMovedBlock);
        mouseMovedBlock.getChildren().clear();
        DatePeriod period = periodAtPoint(event.getX(), event.getY());
        if(period != null && period.getToolTipPane() != null) {
          mouseMovedBlock.getChildren().add(period.getToolTipPane());
        }else mouseMovedBlock.getChildren().add(mouseMovedLabel);
        
        Point2D p = pane.screenToLocal(event.getScreenX(), event.getScreenY());
        Bounds pb = new BoundingBox(p.getX()-10, p.getY()-10, 20, 20);
        
        mouseMovedBlock.setLayoutX(pb.getMaxX());
        mouseMovedBlock.setLayoutY(pb.getMaxY());
        
        Bounds b = pane.screenToLocal(scroll.localToScreen(scroll.getBoundsInLocal()));
        
        if(mouseMovedBlock.getLayoutY() + mouseMovedBlock.getHeight() > b.getMaxY() - mouseMovedBlock.getHeight())
          mouseMovedBlock.setLayoutY(mouseMovedBlock.getLayoutY() - mouseMovedBlock.getHeight() - mouseMovedBlock.getTranslateY() - 20);
        
        if(mouseMovedBlock.getLayoutX() + mouseMovedBlock.getWidth() > b.getMaxX())
          mouseMovedBlock.setLayoutX(mouseMovedBlock.getLayoutX() - mouseMovedBlock.getWidth() - mouseMovedBlock.getTranslateX() - 20);
        
        mouseMovedBlock.toFront();
      }else if(pane.getChildren().contains(mouseMovedBlock))
        pane.getChildren().remove(mouseMovedBlock);
    });
    
    table.setOnMousePressed((MouseEvent event) -> {
      
      selectedRow.setValue(rowAtPoint(event.getY()));
      T period = periodAtPoint(event.getX(), event.getY());
      if(period == null) {
        LocalDate date = selectedDayProperty.getValue();
        if(getDate(event.getX()).equals(date))
          selectedDayProperty.setValue(null);
        selectedDayProperty.setValue(getDate(event.getX()));
      }else selectedDayProperty.setValue(null);
      
      DatePeriod datePeriod = periodAtPoint(event.getX(), event.getY());
      ObservableList<T> selectedPeriods = getSelectedPeriods();
      
      boolean selected = false;
      if(datePeriod != null)
        selected = datePeriod.selectedProperty().getValue();
      
      
      if(!event.isControlDown() && !event.isShiftDown()) {
        clearSelection();
        if(datePeriod != null) {
          if(selected && selectedPeriods.size() > 1) {
            datePeriod.selectedProperty().setValue(selected);
          }else {
            datePeriod.selectedProperty().setValue(!selected);
          }
        }
      }
      
      if(event.isControlDown() && !event.isShiftDown() && datePeriod != null) {
        datePeriod.selectedProperty().setValue(!selected);
      }
      
      if(!event.isControlDown() && event.isShiftDown() && datePeriod != null) {
        Bounds pb = datePeriod.localToScreen(datePeriod.getBoundsInLocal());
        TreeMap<Double,Bounds> mb = new TreeMap();
        for(DatePeriod p:selectedPeriods) {
          Bounds b = p.localToScreen(p.getBoundsInLocal());
          Bounds b1 = new BoundingBox(pb.getMinX(), pb.getMinY(), Math.abs(b.getMaxX()-pb.getMinX()), Math.abs(b.getMaxY()-pb.getMinY()));
          Bounds b2 = new BoundingBox(b.getMinX(),  b.getMaxY(),  Math.abs(b.getMinX()-pb.getMaxX()), Math.abs(b.getMaxY()-pb.getMinY()));
          Bounds b3 = new BoundingBox(b.getMinX(),  b.getMinY(),  Math.abs(b.getMinX()-pb.getMaxX()), Math.abs(b.getMinY()-pb.getMaxY()));
          Bounds b4 = new BoundingBox(pb.getMinX(), pb.getMaxY(), Math.abs(b.getMaxX()-pb.getMinX()), Math.abs(b.getMaxY()-pb.getMaxY()));
          mb.put(b1.getWidth()*b1.getHeight(), b1);
          mb.put(b2.getWidth()*b2.getHeight(), b2);
          mb.put(b3.getWidth()*b3.getHeight(), b3);
          mb.put(b4.getWidth()*b4.getHeight(), b4);
        }
        if(!mb.isEmpty()) {
          for(DatePeriod p:getPeriods()) {
            if(mb.lastEntry().getValue().contains(p.localToScreen(p.getBoundsInLocal())))
              p.selectedProperty().set(!selected);
          }
        }
      }
      
      fireSelect(selectedPeriods);
    });
    
    table.setOnMouseReleased((MouseEvent event) -> {
      if(selectedBlock != null) {
        ObservableList<T> selectedPeriods = getSelectedPeriods();
        for(DatePeriod datePeriod:getPeriods()) {
          if(selectedBlock.localToScreen(selectedBlock.getBoundsInLocal()).contains(datePeriod.localToScreen(datePeriod.getBoundsInLocal())))
            datePeriod.selectedProperty().set(true);
          else datePeriod.selectedProperty().set(false);
        }
        fireSelect(selectedPeriods);
      }
      pane.getChildren().remove(selectedBlock);
      selectedBlock = null;
      startDragPoint = null;
      d = null;
    });
    
    table.setOnMouseDragged((MouseEvent event) -> {
      selectedDayProperty.setValue(null);
      R row = rowAtPoint(event.getY());
      if(row != null) {
        if(!event.isControlDown() && !event.isShiftDown() && selectedBlock == null && startDragPoint == null && d == null) {
          startDragPoint = new Point2D(event.getX()-event.getX()%dayWidthProperty.get(),row.getLayoutY());
          d = new Rectangle(event.getX()-event.getX()%dayWidthProperty.get(), row.getLayoutY(), dayWidthProperty.get(), row.getHeight());

          selectedBlock = new Region();
          selectedBlock.getStyleClass().add("selected-block");
          pane.getChildren().add(selectedBlock);
          selectedBlock.toFront();
        }

        if(selectedBlock != null && d != null) {
          Rectangle s = new Rectangle(event.getX()-event.getX()%dayWidthProperty.get(), row.getLayoutY(), dayWidthProperty.get(), row.getHeight());
          if(s.getX() <= d.getX() && s.getY() >= d.getY()) {
            selectedBlock.setLayoutX(s.getX());
            selectedBlock.setLayoutY(d.getY());
            selectedBlock.setPrefHeight(s.getY()-d.getY()+s.getHeight());
            selectedBlock.setPrefWidth(d.getX()-s.getX()+d.getWidth());
          }

          if(s.getX() <= d.getX() && s.getY() <= d.getY()) {
            selectedBlock.setLayoutX(s.getX());
            selectedBlock.setLayoutY(s.getY());
            selectedBlock.setPrefHeight(d.getY()-s.getY()+s.getHeight());
            selectedBlock.setPrefWidth(d.getX()-s.getX()+d.getWidth());
          }

          if(s.getX() >= d.getX() && s.getY() <= d.getY()) {
            selectedBlock.setLayoutX(d.getX());
            selectedBlock.setLayoutY(s.getY());
            selectedBlock.setPrefHeight(d.getY()-s.getY()+s.getHeight());
            selectedBlock.setPrefWidth(s.getX()-d.getX()+s.getWidth());
          }
          
          if(s.getX() >= d.getX() && s.getY() >= d.getY()) {
            selectedBlock.setLayoutX(d.getX());
            selectedBlock.setLayoutY(d.getY());
            selectedBlock.setPrefHeight(s.getY()-d.getY()+s.getHeight());
            selectedBlock.setPrefWidth(s.getX()-d.getX()+s.getWidth());
          }
        }
      }
    });
    
    /*pane.setOnScroll((ScrollEvent event) -> {
      if(event.isControlDown()) {
        event.consume();
        
        scrollValue = scroll.getHvalue();
        
        int delta = event.getDeltaY()<0?-1:1;
        if(delta > 0 || dayWidthProperty.get() > 3) {
          dayWidthProperty.set(dayWidthProperty.get() + delta);
        }
      }
    });*/
    
    setOnKeyPressed(e -> {
      if(e.getCode() == KeyCode.A && e.isControlDown()) {
        ObservableList<T> selectedPeriods = getSelectedPeriods();
        getPeriods().stream().forEach(p -> ((DatePeriod)p).selectedProperty().setValue(true));
        fireSelect(selectedPeriods);
      }
    });
    
    selectedRow.addListener((ob, ol, nw) -> {
      clearRowSelection();
      if(nw != null)
        nw.setValue("selected", true);
    });
  }

  public IntegerProperty previosMonthProperty() {
    return previosMonthProperty;
  }

  public IntegerProperty futureMonthProperty() {
    return futureMonthProperty;
  }

  public IntegerProperty dayWidthProperty() {
    return dayWidthProperty;
  }

  public ScrollPane getScroll() {
    return scroll;
  }

  public void clearRowSelection() {
    for(PropertyMap n:getItems())
      n.setValue("selected", false);
  }
  
  public void setRowSelected(R row) {
    selectedRow.set(row);
  }

  public ObjectProperty<R> selectedRowProperty() {
    return selectedRow;
  }

  public ObjectProperty<LocalDate> mouseMoveDateProperty() {
    return mouseMoveDate;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public VBox getTable() {
    return table;
  }
}