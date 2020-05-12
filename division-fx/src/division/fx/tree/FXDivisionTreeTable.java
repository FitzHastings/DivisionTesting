package division.fx.tree;

import division.fx.DivisionTextField;
import division.fx.PropertyMap;
import java.util.List;
import java.util.Map;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.KeyCode;
import javafx.stage.Popup;
import org.apache.commons.lang3.ArrayUtils;


public class FXDivisionTreeTable<S extends PropertyMap> extends TreeTableView<S> {
  private final Popup pop = new Popup();
  private final DivisionTextField findText = new DivisionTextField();
  private BooleanProperty findable = new SimpleBooleanProperty(true);
  
  public FXDivisionTreeTable() {
    this(new TreeItem<S>());
  }

  public FXDivisionTreeTable(TreeItem<S> root) {
    super(root);
    
    pop.setAutoHide(true);
    pop.setAutoFix(true);
    pop.setHideOnEscape(true);
    pop.getContent().add(findText);
    
    findText.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(!newValue)
        pop.hide();
    });
    
    findText.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> findNextText(null));
    
    findText.setOnKeyPressed(e -> {
      if(e.getCode() == KeyCode.F3)
        findNextText(getSelectionModel().getSelectedItem());
      if(e.getCode() == KeyCode.F2)
        findPreviosText(getSelectionModel().getSelectedItem());
    });
    
    setOnMousePressed(e -> pop.hide());
    setOnKeyPressed(e -> {
      if(findable.getValue() && (e.getCode().isLetterKey() || e.getCode().isDigitKey())) {
        if(!pop.isShowing()) {
          findText.setPrefWidth(getColumns().get(0).getWidth());
          findText.setMinWidth(getColumns().get(0).getWidth());
          findText.setMaxWidth(getColumns().get(0).getWidth());
          pop.show(getParent(), localToScreen(getBoundsInLocal()).getMinX(), localToScreen(getBoundsInLocal()).getMinY() - 30);
          findText.setText("");
          findText.requestFocus();
          findText.positionCaret(findText.getText().length());
        }
      }
    });
  }
  
  public BooleanProperty findableProperty() {
    return findable;
  }
  
  private void findPreviosText(TreeItem<S> startItem) {
    String value;
    String text = findText.getText().toLowerCase();
    List<TreeItem<S>> list = FXTree.ListItems(this);
    int startRow = startItem == null ? 0 : list.indexOf(startItem) - 1;
    for(int i=startRow<0?0:startRow;i>=0;i--) {
      value = list.get(i).getValue().toString().toLowerCase();
      if(value != null && value.contains(text)) {
        TreeItem<S> it = list.get(i);
        while(it != null) {
          it.setExpanded(true);
          it = it.getParent();
        }
        getSelectionModel().select(list.get(i));
        scrollTo(getRow(list.get(i)));
        break;
      }
    }
  }
  
  private void findNextText(TreeItem<S> startItem) {
    String value;
    String text = findText.getText().toLowerCase();
    List<TreeItem<S>> list = FXTree.ListItems(this);
    int startRow = startItem == null ? 0 : list.indexOf(startItem) + 1;
    for(int i=startRow<0?0:startRow;i<list.size();i++) {
      value = list.get(i).getValue().toString().toLowerCase();
      if(value != null && value.contains(text)) {
        TreeItem<S> it = list.get(i);
        while(it != null) {
          it.setExpanded(true);
          it = it.getParent();
        }
        getSelectionModel().select(list.get(i));
        scrollTo(getRow(list.get(i)));
        break;
      }
    }
  }
  
  public TreeTableColumn[] getAllColumns(TreeTableColumn column, boolean last) {
    TreeTableColumn[] columns = new TreeTableColumn[]{column};
    if(column == null || last && !column.getColumns().isEmpty())
      columns = new TreeTableColumn[0];
    
    ObservableList cols = column == null ? getColumns() :  column.getColumns();
    for(Object col:cols)
      columns = ArrayUtils.addAll(columns, getAllColumns((TreeTableColumn)col, last));
    return columns;
  }
  
  public void setHorizontScrollBarPolicyAlways() {
    Observable[] p = new Observable[]{widthProperty()};
    for(TreeTableColumn col:getAllColumns(null, false))
      p = ArrayUtils.addAll(p, col.widthProperty(), col.visibleProperty());
    
    TreeTableColumn c = new TreeTableColumn(" ");
    c.visibleProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(!newValue)
        c.setVisible(true);
    });
    getColumns().add(c);
    
    c.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> {
      double w = getWidth()+1;
      for(Object col:getAllColumns(null, true))
        if(((TreeTableColumn)col).isVisible() && !col.equals(c))
          w -= ((TreeTableColumn)col).getWidth();
      return w > 0 ? w : 0;
    }, p));
  }
  
  /*public TreeItem<S> getNodeObject(Integer id) {
    TreeItem<S> node = null;
    for(TreeItem<S> n:getRoot().getChildren()) {
      if(id.equals(n.getValue().getValue("id")))
        node = n;
      else node = getNodeObject(id);
      if(node != null)
        break;
    }
    return node;
  }*/
  
  public List<TreeItem<S>> listItems() {
    return FXTree.ListItems(this);
  }
  
  public List<TreeItem<S>> listItems(TreeItem<S> item) {
    return FXTree.ListItems(item);
  }
  
  public List<TreeItem<S>> listItems(String propertyname, Object value) {
    return FXTree.ListItems(this, propertyname, value);
  }
  
  public List<TreeItem<S>> listItems(TreeItem<S> parent, String propertyname, Object value) {
    return FXTree.ListItems(parent, propertyname, value);
  }
  
  public List<TreeItem<S>> listItems(TreeItem<S> parent, String propertyname, Object value, boolean withParent) {
    return FXTree.ListItems(parent, propertyname, value, withParent);
  }
  
  public TreeItem<S> getNode(PropertyMap fields) {
    return FXTree.GetNode(this, fields);
  }
  
  public TreeItem<S> getNode(Map map) {
    return FXTree.GetNode(this, map);
  }
  
  public TreeItem<S> getNode(TreeItem<S> parent, PropertyMap fields) {
    return FXTree.GetNode(parent, fields);
  }
  
  public TreeItem<S> getNode(TreeItem<S> parent, Map map) {
    return FXTree.GetNode(parent, map);
  }
}