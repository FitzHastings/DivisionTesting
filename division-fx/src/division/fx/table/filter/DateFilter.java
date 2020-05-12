package division.fx.table.filter;

import division.fx.border.titleborder.TitleBorderPane;
import division.fx.PropertyMap;
import division.util.Utility;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DateFilter extends AbstractColumnFilter {
  private final CheckBox k1 = new CheckBox("1");
  private final CheckBox k2 = new CheckBox("2");
  private final CheckBox k3 = new CheckBox("3");
  private final CheckBox k4 = new CheckBox("4");
  private final ComboBox<Integer> year = new ComboBox();
  private final HBox kvartals = new HBox(5, k1,k2,k3,k4,year);
  
  private final DatePicker start = new DatePicker(LocalDate.now());
  private final DatePicker end   = new DatePicker(LocalDate.now());
  private final CheckBox customDate = new CheckBox("Произвольный период");
  private final GridPane dates = new GridPane();
  private final TitleBorderPane datePanel = new TitleBorderPane(dates, customDate);
  
  
  private final VBox content = new VBox(10, kvartals, datePanel);

  public DateFilter(String property) {
    super(property);
    
    dates.setHgap(10);
    dates.setVgap(5);
    dates.addRow(0, new Label("с"), start);
    dates.addRow(1, new Label("по"), end);
    dates.setPadding(new Insets(5));
    
    for(int i=LocalDate.now().getYear();i>=LocalDate.now().getYear()-100;i--)
      year.getItems().add(i);
    year.getSelectionModel().select(0);
    
    activeProperty().bind(
            k1.selectedProperty()
                    .or(k2.selectedProperty())
                    .or(k3.selectedProperty())
                    .or(k4.selectedProperty())
                    .or(customDate.selectedProperty()));
    
    kvartals.disableProperty().bind(customDate.selectedProperty());
    dates.disableProperty().bind(customDate.selectedProperty().not());
  }

  @Override
  public void initFilter() {
  }

  @Override
  public Node getContent() {
    return content;
  }

  @Override
  public Predicate<PropertyMap> getPredicate() {
    return (PropertyMap t) -> {
      LocalDate date = null;
      
      if(t.getValue(getProperty()) instanceof LocalDate)
        date = t.getLocalDate(getProperty());
      
      if(t.getValue(getProperty()) instanceof LocalDateTime)
        date = t.getLocalDateTime(getProperty()).toLocalDate();
      
      if(t.getValue(getProperty()) instanceof java.sql.Timestamp)
        date = Utility.convert(t.getTimestamp(getProperty()));
      
      if(t.getValue(getProperty()) instanceof java.sql.Date)
        date = Utility.convert(t.getSqlDate(getProperty()));
      
      if(t.getValue(getProperty()) instanceof java.util.Date)
        date = Utility.convert(t.getDate(getProperty()));
      
      
      if(date == null)
        return true;
      
      if(customDate.isSelected())
        return date.equals(start.getValue()) || date.equals(end.getValue()) || (date.isAfter(start.getValue()) && date.isBefore(end.getValue()));
      else {
        boolean is = false;
        LocalDate startDate, endDate;
        if(!is && k1.isSelected()) {
          startDate = LocalDate.of(year.getValue(), Month.JANUARY, 1);
          endDate   = LocalDate.of(year.getValue(), Month.MARCH, 1);
          endDate = endDate.withDayOfMonth(endDate.lengthOfMonth());
          is = date.equals(startDate) || date.equals(endDate) || (date.isAfter(startDate) && date.isBefore(endDate));
        }
        if(!is && k2.isSelected()) {
          startDate = LocalDate.of(year.getValue(), Month.APRIL, 1);
          endDate   = LocalDate.of(year.getValue(), Month.JUNE, 1);
          endDate = endDate.withDayOfMonth(endDate.lengthOfMonth());
          is = date.equals(startDate) || date.equals(endDate) || (date.isAfter(startDate) && date.isBefore(endDate));
        }
        if(!is && k3.isSelected()) {
          startDate = LocalDate.of(year.getValue(), Month.JULY, 1);
          endDate   = LocalDate.of(year.getValue(), Month.SEPTEMBER, 1);
          endDate = endDate.withDayOfMonth(endDate.lengthOfMonth());
          is = date.equals(startDate) || date.equals(endDate) || (date.isAfter(startDate) && date.isBefore(endDate));
        }
        if(!is && k4.isSelected()) {
          startDate = LocalDate.of(year.getValue(), Month.OCTOBER, 1);
          endDate   = LocalDate.of(year.getValue(), Month.DECEMBER, 1);
          endDate = endDate.withDayOfMonth(endDate.lengthOfMonth());
          is = date.equals(startDate) || date.equals(endDate) || (date.isAfter(startDate) && date.isBefore(endDate));
        }
        return is ? true : !k1.isSelected() && !k2.isSelected() && !k3.isSelected() && !k4.isSelected();
      }
    };
  }
  
  public List<AbstractMap.SimpleEntry<LocalDateTime,LocalDateTime>> getPeriods() {
    List<AbstractMap.SimpleEntry<LocalDateTime,LocalDateTime>> dates = new ArrayList<>();
    if(customDate.isSelected()) {
      dates.add(new AbstractMap.SimpleEntry<>(LocalDateTime.of(start.getValue(), LocalTime.MIN), LocalDateTime.of(end.getValue(), LocalTime.MAX)));
    }else {
      if(k1.isSelected()) {
        LocalDate startDate = LocalDate.of(year.getValue(), Month.JANUARY, 1);
        LocalDate endDate   = LocalDate.of(year.getValue(), Month.MARCH, 1);
        endDate = endDate.withDayOfMonth(endDate.lengthOfMonth());
        dates.add(new AbstractMap.SimpleEntry<>(LocalDateTime.of(startDate, LocalTime.MIN), LocalDateTime.of(endDate, LocalTime.MAX)));
      }
      if(k2.isSelected()) {
        LocalDate startDate = LocalDate.of(year.getValue(), Month.APRIL, 1);
        LocalDate endDate   = LocalDate.of(year.getValue(), Month.JUNE, 1);
        endDate = endDate.withDayOfMonth(endDate.lengthOfMonth());
        dates.add(new AbstractMap.SimpleEntry<>(LocalDateTime.of(startDate, LocalTime.MIN), LocalDateTime.of(endDate, LocalTime.MAX)));
      }
      if(k3.isSelected()) {
        LocalDate startDate = LocalDate.of(year.getValue(), Month.JULY, 1);
        LocalDate endDate   = LocalDate.of(year.getValue(), Month.SEPTEMBER, 1);
        endDate = endDate.withDayOfMonth(endDate.lengthOfMonth());
        dates.add(new AbstractMap.SimpleEntry<>(LocalDateTime.of(startDate, LocalTime.MIN), LocalDateTime.of(endDate, LocalTime.MAX)));
      }
      if(k4.isSelected()) {
        LocalDate startDate = LocalDate.of(year.getValue(), Month.OCTOBER, 1);
        LocalDate endDate   = LocalDate.of(year.getValue(), Month.DECEMBER, 1);
        endDate = endDate.withDayOfMonth(endDate.lengthOfMonth());
        dates.add(new AbstractMap.SimpleEntry<>(LocalDateTime.of(startDate, LocalTime.MIN), LocalDateTime.of(endDate, LocalTime.MAX)));
      }
    }
    return dates;
  }

  @Override
  public void addFilterListener(FilterListener handler) {
    ChangeListener listener = (ChangeListener) (ObservableValue observable, Object oldValue, Object newValue) -> handler.handle(new Event(DateFilter.this, null, EventType.ROOT));
    putListener(handler, listener);
    k1.selectedProperty().addListener(listener);
    k2.selectedProperty().addListener(listener);
    k3.selectedProperty().addListener(listener);
    k4.selectedProperty().addListener(listener);
    year.getSelectionModel().selectedIndexProperty().addListener(listener);
    customDate.selectedProperty().addListener(listener);
    start.valueProperty().addListener(listener);
    end.valueProperty().addListener(listener);
  }

  @Override
  protected void removeListener(Object listener) {
    k1.selectedProperty().removeListener((ChangeListener)listener);
    k2.selectedProperty().removeListener((ChangeListener)listener);
    k3.selectedProperty().removeListener((ChangeListener)listener);
    k4.selectedProperty().removeListener((ChangeListener)listener);
    year.getSelectionModel().selectedIndexProperty().removeListener((ChangeListener)listener);
    customDate.selectedProperty().removeListener((ChangeListener)listener);
    start.valueProperty().removeListener((ChangeListener)listener);
    end.valueProperty().removeListener((ChangeListener)listener);
  }
  
  public void selectCurrentKvartal() {
    if(LocalDate.now().getMonthValue() <= 3)
      k1.setSelected(true);
    else if(LocalDate.now().getMonthValue() <= 6)
      k2.setSelected(true);
    else if(LocalDate.now().getMonthValue() <= 9)
      k3.setSelected(true);
    else if(LocalDate.now().getMonthValue() <= 12)
      k4.setSelected(true);
  }
  
  public void setAllwaysSelected(boolean is) {
    if(is) {
      k1.disableProperty().bind(k1.selectedProperty().and(k2.selectedProperty().not()).and(k3.selectedProperty().not()).and(k4.selectedProperty().not()));
      k2.disableProperty().bind(k2.selectedProperty().and(k1.selectedProperty().not()).and(k3.selectedProperty().not()).and(k4.selectedProperty().not()));
      k3.disableProperty().bind(k3.selectedProperty().and(k2.selectedProperty().not()).and(k1.selectedProperty().not()).and(k4.selectedProperty().not()));
      k4.disableProperty().bind(k4.selectedProperty().and(k2.selectedProperty().not()).and(k3.selectedProperty().not()).and(k1.selectedProperty().not()));
    }else {
      k1.disableProperty().unbind();
      k2.disableProperty().unbind();
      k3.disableProperty().unbind();
      k4.disableProperty().unbind();
    }
  }
}