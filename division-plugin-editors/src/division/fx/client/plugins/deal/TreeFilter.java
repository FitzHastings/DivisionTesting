package division.fx.client.plugins.deal;

import division.fx.PropertyMap;
import division.fx.editor.FXTreeEditor;
import division.fx.table.filter.AbstractColumnFilter;
import division.fx.table.filter.FilterListener;
import java.util.function.Predicate;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import mapping.MappingObject;
import org.apache.commons.lang3.ArrayUtils;
import util.filter.local.DBFilter;

public class TreeFilter extends AbstractColumnFilter {
  private FXTreeEditor editor;
  private ComboBox<String> inout = new ComboBox<>(FXCollections.observableArrayList("Включить","Исключить"));
  private VBox content;
  
  public TreeFilter(DBFilter filter, String property) {
    super(property);
    editor = new FXTreeEditor(filter.getTargetClass());
    editor.getClientFilter().AND_FILTER(filter);
    inout.getSelectionModel().select(0);
    editor.getTree().setShowRoot(false);
    
    editor.checkBoxTreeProperty().setValue(true);
    //editor.checkBoxIndependentProperty().setValue(true);
    //editor.checkBoxTreeHerachyProperty().setValue(true);
    
    content = new VBox(5, inout, editor.getTree());
    editor.initData();
  }

  @Override
  public void finaly() {
    super.finaly();
    editor.dispose();
    editor.clearData();
    editor = null;
    content.getChildren().clear();
    content = null;
    inout.getItems().clear();
    inout = null;
  }
  
  
  
  public TreeFilter(Class<? extends MappingObject> objectClass, String property) {
    this(DBFilter.create(objectClass), property);
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
    Integer[] ids = editor == null ? new Integer[0] : editor.getCheckedIds();
    return (PropertyMap t) -> {
      return ids.length == 0 ? true : (inout.getSelectionModel().getSelectedIndex() == 0 ? ArrayUtils.contains(ids, t.getValue(getProperty())) : !ArrayUtils.contains(ids, t.getValue(getProperty())));
    };
  }
  
  @Override
  public void addFilterListener(FilterListener handler) {
    inout.getSelectionModel().selectedIndexProperty().addListener((od,ol,nw) -> handler.handle(new Event(this, null, EventType.ROOT)));
    editor.setCheckHandler(e -> {
      activeProperty().setValue(!editor.getCheckedItems().isEmpty());
      handler.handle(new Event(this, null, EventType.ROOT));
    });
    putListener(handler, this);
  }
  
  @Override
  protected void removeListener(Object listener) {
    if(listener instanceof ChangeListener)
      inout.getSelectionModel().selectedIndexProperty().removeListener((ChangeListener)listener);
    editor.setCheckHandler(null);
  }
  
  public Integer[] getCheckedIds() {
    return editor.getCheckedIds();
  }
  
  public boolean isInclusive() {
    return inout.getSelectionModel().getSelectedIndex() == 0;
  }
}