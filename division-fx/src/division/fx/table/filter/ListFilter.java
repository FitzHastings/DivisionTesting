package division.fx.table.filter;

import division.fx.PropertyMap;
import java.util.function.Predicate;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import org.controlsfx.control.CheckListView;

public class ListFilter<S extends PropertyMap> extends AbstractColumnFilter<S> {
  private final CheckListView list = new CheckListView();
  private final ComboBox<String> inout = new ComboBox<>(FXCollections.observableArrayList("Включить","Исключить"));
  private final VBox content =  new VBox(5, inout, list);
  private boolean active = true;

  public ListFilter(String property) {
    super(property);
    inout.getSelectionModel().select(0);
    content.setPadding(new Insets(10));
  }

  @Override
  public Node getContent() {
    return content;
  }

  @Override
  public Predicate<S> getPredicate() {
    return (S t) -> {
      ObservableList values = list.getCheckModel().getCheckedItems();
      return values.isEmpty() || (inout.getSelectionModel().getSelectedIndex() == 0 ? values.contains(t.getValue(getProperty()) == null ? "Пусто" : t.getValue(getProperty())) : !values.contains(t.getValue(getProperty()) == null ? "Пусто" : t.getValue(getProperty())));
    };
  }

  @Override
  public void addFilterListener(FilterListener handler) {
    ChangeListener listener = (ChangeListener) (ObservableValue observable, Object oldValue, Object newValue) -> handler.handle(new Event(ListFilter.this, null, EventType.ROOT));
    putListener(handler, listener);
    inout.getSelectionModel().selectedIndexProperty().addListener(listener);
    
    list.getCheckModel().getCheckedItems().addListener(new ListChangeListener() {
      @Override
      public void onChanged(ListChangeListener.Change c) {
        activeProperty().setValue(!list.getCheckModel().getCheckedItems().isEmpty());
        if(active)
          handler.handle(new Event(this, null, EventType.ROOT));
        putListener(handler, this);
      }
    });
  }

  @Override
  public void initFilter() {
    ObservableList<String> values = list.getCheckModel().getCheckedItems();
    list.getItems().clear();
    list.getItems().add("Пусто");
    for(PropertyMap item:getTableFilter().getItems())
      if(item.getValue(getProperty()) != null && !list.getItems().contains(item.getValue(getProperty())))
        list.getItems().add(item.getValue(getProperty()));
    active = false;
    values.stream().forEach(value -> list.getCheckModel().check(value));
    active = true;
  }

  @Override
  protected void removeListener(Object listener) {
    inout.getSelectionModel().selectedIndexProperty().removeListener((ChangeListener)listener);
//    list.getCheckModel().getCheckedItems().removeListener((ListChangeListener)listener);
  }
}