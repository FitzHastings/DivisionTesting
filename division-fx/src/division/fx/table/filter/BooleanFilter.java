package division.fx.table.filter;

import division.fx.PropertyMap;
import division.fx.border.titleborder.TitleBorderPane;
import java.util.function.Predicate;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

public class BooleanFilter extends AbstractColumnFilter {
  private final CheckBox box = new CheckBox();
  private final CheckBox checkFilter = new CheckBox("применить фильтр");
  private final TitleBorderPane activebox = new TitleBorderPane(checkFilter);
  private final VBox content = new VBox(activebox);

  public BooleanFilter(String property) {
    super(property);
    content.setPadding(new Insets(10));
    activeProperty().bind(checkFilter.selectedProperty());
    activebox.setCenter(box);
  }

  @Override
  protected void removeListener(Object listener) {
    box.selectedProperty().removeListener((ChangeListener<Boolean>)listener);
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
    return (PropertyMap t) -> t.getValue(getProperty(), boolean.class) == box.isSelected();
  }

  @Override
  public void addFilterListener(FilterListener handler) {
    ChangeListener listener = (ChangeListener) (ObservableValue observable, Object oldValue, Object newValue) -> handler.handle(new Event(BooleanFilter.this, null, EventType.ROOT));
    putListener(handler, listener);
    box.selectedProperty().addListener(listener);
  }
}