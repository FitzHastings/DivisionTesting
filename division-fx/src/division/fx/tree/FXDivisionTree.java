package division.fx.tree;

import com.sun.javafx.scene.control.skin.VirtualFlow;
import division.fx.DivisionTextField;
import division.fx.PropertyMap;
import java.util.List;
import java.util.Map;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.stage.Popup;

public class FXDivisionTree<T extends PropertyMap> extends TreeView<T>{
  private final Popup pop = new Popup();
  private final DivisionTextField findText = new DivisionTextField();

  public FXDivisionTree() {
    this(null);
  }

  public FXDivisionTree(TreeItem<T> root) {
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
    setOnKeyReleased(e -> {
      if(!e.getText().equals("")) {
        if(!pop.isShowing()) {
          findText.setPrefWidth(getBoundsInLocal().getWidth());
          findText.setMinWidth(getBoundsInLocal().getWidth());
          findText.setMaxWidth(getBoundsInLocal().getWidth());
          pop.show(getParent(), localToScreen(getBoundsInLocal()).getMinX(), localToScreen(getBoundsInLocal()).getMinY() - 30);
          findText.setText("");
          findText.setText(e.getText());
          findText.requestFocus();
          findText.positionCaret(findText.getText().length());
        }
      }
    });
  }
  
  private void findPreviosText(TreeItem<T> startItem) {
    String value;
    String text = findText.getText().toLowerCase();
    List<TreeItem<T>> list = listItems(getRoot());
    int startRow = startItem == null ? 0 : list.indexOf(startItem) - 1;
    for(int i=startRow<0?0:startRow;i>=0;i--) {
      value = list.get(i).getValue().toString().toLowerCase();
      if(value != null && value.contains(text)) {
        TreeItem<T> it = list.get(i);
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
  
  public TreeItem<T> getTreeItemAtPoint(double x, double y) {
    VirtualFlow vf = (VirtualFlow)getChildrenUnmodifiable().get(0);
    y -= vf.getBoundsInParent().getMinY();
    for(int i=0;i<vf.getCellCount();i++) {
      if(vf.getVisibleCell(i) != null && vf.getVisibleCell(i).getBoundsInParent().contains(x, y))
        return getTreeItem(i);
    }
    return null;
  }
  
  private void findNextText(TreeItem<T> startItem) {
    String value;
    String text = findText.getText().toLowerCase();
    List<TreeItem<T>> list = listItems(getRoot());
    int startRow = startItem == null ? 0 : list.indexOf(startItem) + 1;
    for(int i=startRow<0?0:startRow;i<list.size();i++) {
      value = list.get(i).getValue().toString().toLowerCase();
      if(value != null && value.contains(text)) {
        TreeItem<T> it = list.get(i);
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
  
  
  
  public List<TreeItem<T>> listItems() {
    return FXTree.ListItems(this);
  }
  
  public List<TreeItem<T>> listItems(TreeItem<T> item) {
    return FXTree.ListItems(item);
  }
  
  public List<TreeItem<T>> listItems(String propertyname, Object value) {
    return FXTree.ListItems(this, propertyname, value);
  }
  
  public List<TreeItem<T>> listItems(TreeItem<T> parent, String propertyname, Object value) {
    return FXTree.ListItems(parent, propertyname, value);
  }
  
  public TreeItem<T> getNode(PropertyMap fields) {
    return FXTree.GetNode(this, fields);
  }
  
  public TreeItem<T> getNode(Map map) {
    return FXTree.GetNode(this, map);
  }
  
  public TreeItem<T> getNode(TreeItem<T> parent, PropertyMap fields) {
    return FXTree.GetNode(parent, fields);
  }
  
  public TreeItem<T> getNode(TreeItem<T> parent, Map map) {
    return FXTree.GetNode(parent, map);
  }
}