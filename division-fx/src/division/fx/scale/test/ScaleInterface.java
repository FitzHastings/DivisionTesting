package division.fx.scale.test;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

interface ScaleInterface<S extends ScaleRow<P>, P extends ScalePeriod> {
  public ReadOnlyObjectProperty<LocalDate> leftDateProperty();
  public ReadOnlyObjectProperty<LocalDate> rightDateProperty();
  public IntegerProperty previosMonthProperty();
  public IntegerProperty futureMonthProperty();
  public IntegerProperty dayWidthProperty();
  public OPool<S> getRowPool();
  public OPool<P> getPeriodPool();
  
  public default Region joinRegions(Collection<Region> regions) {
    return joinRegions(regions.toArray(new Region[0]));
  }
  
  public default Region joinRegions(Region... regions) {
    Region joinRegion = new Region();
    if(regions.length > 0) {
      Arrays.sort(regions, (Region o1, Region o2) -> ((Double)o1.getLayoutX()).compareTo((Double)o2.getLayoutX()));
      joinRegion.setLayoutX(regions[0].getLayoutX());
      joinRegion.setPrefWidth(regions[regions.length-1].getLayoutX() - joinRegion.getLayoutX() + regions[regions.length-1].getPrefWidth()-0.1);
      Arrays.sort(regions, (Region o1, Region o2) -> ((Double)o1.getLayoutY()).compareTo((Double)o2.getLayoutY()));
      joinRegion.setLayoutY(regions[0].getLayoutY());
      joinRegion.setPrefHeight(regions[regions.length-1].getLayoutY() - joinRegion.getLayoutY() + regions[regions.length-1].getPrefHeight()-0.1);
    }
    return joinRegion;
  }
  
  public default Long betweenDays(LocalDate d1, LocalDate d2) {
    return ChronoUnit.DAYS.between(d1, d2);
  }
  
  public default Long betweenX(LocalDate d1, LocalDate d2) {
    return betweenDays(d1, d2) * dayWidthProperty().getValue();
  }
  
  public default S createRow(P... periods) {
    return createRow(Arrays.asList(periods));
  }
  
  public default S createRow(Collection<P> periods) {
    S row = getRowPool().borrowObject();
    row.scaleProperty().setValue(this);
    row.getChildren().addAll(periods);
    return row;
  }
  
  public default P createPeriod() throws Exception {
    return /*(P) ScalePeriod.class.newInstance();*/getPeriodPool().borrowObject();
  }
  
  public default P createPeriod(LocalDate startDate, Period period) throws Exception {
    return createPeriod(startDate, startDate.plusDays(period.getDays()).plusMonths(period.getMonths()).plusYears(period.getYears()));
  }
  
  public default P createPeriod(LocalDate startDate, LocalDate endDate) throws Exception {
    P period = createPeriod();
    period.startDateProperty().setValue(startDate);
    period.endDateProperty().setValue(endDate);
    return period;
  }
  
  public Pane getRowLayer();
  public Pane getSelectedLayer();
  
  public default List<S> getRows() {
    return getRowLayer().getChildren().stream().map(n -> (S)n).collect(Collectors.toList());
  }
  
  public default int rowIndexAt(double screenY) {
    return (int)(getRowLayer().screenToLocal(0, screenY).getY()/getHeader().heightProperty().getValue());
  }
  
  public S getHeader();
  
  public default S rowAtPeriod(P p) {
    return getRows().stream().filter(r -> r.getPeriods().contains(p)).findFirst().orElseGet(() -> null);
  }
  
  public default S rowAt(double screenY) {
    int index = rowIndexAt(screenY);
    List<S> rows = getRows();
    return index >= 0 && !rows.isEmpty() && index < rows.size() ? rows.get(index) : null;
  }
  
  public default ScalePeriod periodAtPoint(double screenX, double screenY) {
    S row = rowAt(screenY);
    return row == null ? null : row.getPeriods().stream().filter(p -> {
      return p.localToScreen(p.getBoundsInLocal()).contains(screenX, screenY) && p.isVisible();
    }).findFirst().orElseGet(() -> null);
  }
  
  public default P columnAtPoint(double screenX, double screenY) {
    return getHeader().getPeriods().stream().filter(p -> {
      return p.localToScreen(p.getBoundsInLocal()).contains(screenX, screenY);
    }).findFirst().orElseGet(() -> null);
  }
  
  public default List<LocalDate> getSelectedDates() {
    return getSelectedLayer().getChildren().stream().map(n -> dateAt(n.localToScreen(n.getBoundsInLocal()).getMinX())).sorted().collect(Collectors.toList());
  }
  
  public default LocalDate getSelectedDate() {
    return getSelectedDates().stream().findFirst().orElseGet(() -> null);
  }
  
  public default LocalDate dateAt(double screenX) {
    return leftDateProperty().getValue().plusDays(Math.round((screenX-getSelectedLayer().localToScreen(getSelectedLayer().getBoundsInLocal()).getMinX())/dayWidthProperty().getValue()));
  }
  
  public default void clearSelectedBlocks() {
    clearSelectedBlocks(true);
  }
  
  public default void clearSelectedBlocks(boolean fireselect) {
    getSelectedLayer().getChildren().clear();
    if(fireselect)
      fireSelected();
  }
  
  public default void addSelectedBlock(Region... blocks) {
    addSelectedBlock(true, blocks);
  }
  
  public default void addSelectedBlock(boolean fireselect, Region... blocks) {
    getSelectedLayer().getChildren().addAll(blocks);
    if(fireselect)
      fireSelected();
  }
  
  public default void setSelectedBlock(Collection<Region> blocks) {
    setSelectedBlock(blocks.toArray(new Region[0]));
  }
  
  public default void setSelectedBlock(Region... blocks) {
    clearSelectedBlocks(false);
    addSelectedBlock(false, blocks);
    fireSelected();
  }
  
  public ReadOnlyObjectProperty<List<S>> selectedRowProperty();
  public ReadOnlyObjectProperty<List<P>> selectedPeriodProperty();
  
  public default void unbindRow(S row) {
    row.layoutYProperty().unbind();
    row.prefWidthProperty().unbind();
    row.prefHeightProperty().unbind();
  }
  
  public default void unbindAllRows() {
    getRows().stream().forEach(r -> unbindRow(r));
  }
  
  public default void bindRow(int index, S row) {
    row.setLayoutX(0);
    row.layoutYProperty().bind(getHeader().heightProperty().multiply(index));
    row.prefHeightProperty().bind(getHeader().heightProperty());
    row.prefWidthProperty().bind(getHeader().widthProperty());
  }
  
  public default void setSelectRows(ObservableList<ScaleRow<ScalePeriod>> selectedItems) {
    setSelectedBlock(selectedItems.stream().map(row -> {
      Region r = new Region();
      r.setLayoutX(row.getLayoutX());
      r.setLayoutY(row.getLayoutY());
      r.setPrefSize(row.getWidth(), row.getHeight());
      return r;
    }).collect(Collectors.toList()));
  }
  
  public default void setDealPredicate(Predicate<P> p) {
    getRows().stream().flatMap(r -> r.getPeriods().stream()).forEach(period -> period.setVisible(p.test(period)));
    fireSelected();
  }
  
  public void fireSelected();
}