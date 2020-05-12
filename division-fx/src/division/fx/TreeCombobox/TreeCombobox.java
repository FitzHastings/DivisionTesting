package division.fx.TreeCombobox;

import division.fx.FXButton;
import division.fx.PropertyMap;
import division.fx.tree.FXDivisionTree;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Cursor;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Popup;

public class TreeCombobox extends HBox {
  private final TextField text   = new TextField();
  private final FXButton  button = new FXButton(e -> select(), "store-button");
  private final Popup     pop    = new Popup();
  private final ObjectProperty<TreeView<PropertyMap>> treeProperty = new SimpleObjectProperty<>();

  public TreeCombobox() {
    getChildren().addAll(text, button);
    
    text.setFocusTraversable(false);
    text.setCursor(Cursor.DEFAULT);
    text.setOnMouseClicked(e -> select());
    button.setText("...");
    text.setEditable(false);
    HBox.setHgrow(text, Priority.ALWAYS);
    
    treeProperty.addListener((ObservableValue<? extends TreeView<PropertyMap>> observable, TreeView<PropertyMap> oldTree, TreeView<PropertyMap> newTree) -> {
      if(newTree != null) {
        pop.getContent().add(newTree);
        pop.setAutoHide(true);
        newTree.prefWidthProperty().bind(text.widthProperty());
        newTree.minWidthProperty().bind(text.widthProperty());
        newTree.maxWidthProperty().bind(text.widthProperty());
        
        newTree.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends TreeItem<PropertyMap>> observable1, TreeItem<PropertyMap> oldValue, TreeItem<PropertyMap> newValue) -> {
          if(newValue != null && newValue.getChildren().isEmpty()) {
            text.setText(newValue.getValue().getString("name"));
            pop.hide();
          }else text.setText("");
        });
      }
    });
    
    treeProperty.setValue(new FXDivisionTree<>());
  }
  
  public ReadOnlyBooleanProperty showingProperty() {
    return pop.showingProperty();
  }
  
  public ReadOnlyObjectProperty<TreeItem<PropertyMap>> selectedItemProperty() {
    return treeProperty().getValue().getSelectionModel().selectedItemProperty();
  }
  
  public ObjectProperty<TreeView<PropertyMap>> treeProperty() {
    return treeProperty;
  }

  private void select() {
    if(pop.isShowing())
      pop.hide();
    else pop.show(text, text.localToScreen(0, 0).getX(), text.localToScreen(0,0).getY()+text.getHeight());
  }
}