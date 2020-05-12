package division.fx.tree;

import division.fx.DivisionTextField;
import division.fx.PropertyMap;
import java.util.List;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyCode;
import javafx.stage.Popup;
import org.controlsfx.control.CheckTreeView;

public class FXDivisionCheckTree<T extends PropertyMap> extends CheckTreeView<T> {
  private final Popup pop = new Popup();
  private final DivisionTextField findText = new DivisionTextField();

  public FXDivisionCheckTree() {
    this(null);
  }

  public FXDivisionCheckTree(CheckBoxTreeItem<T> root) {
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
      if(!e.isAltDown() && !e.isControlDown() && !e.isMetaDown() && !e.isShiftDown() && !e.isShortcutDown()) {
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
    List<TreeItem<T>> list = FXTree.ListItems(this);
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
  
  private void findNextText(TreeItem<T> startItem) {
    String value;
    String text = findText.getText().toLowerCase();
    List<TreeItem<T>> list = FXTree.ListItems(this);
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
}