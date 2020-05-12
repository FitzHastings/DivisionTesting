package division.fx.controller.process;

import bum.interfaces.Service;
import division.fx.PropertyMap;
import division.fx.editor.FXTreeEditor;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeCell;

public class ProcessTree extends FXTreeEditor {

  public ProcessTree() {
    super(Service.class, FXProcess.class, "childs", "owner");
    titleProperty().setValue("Процессы");
  }

  @Override
  protected void updateItem(PropertyMap item, TreeCell cell) {
    cell.getStyleClass().remove("product-item");
    if(!item.isNull("id") && item.isNull("owner") && (item.isNull("childs") || item.getValue("childs", Integer[].class).length == 0))
      cell.getStyleClass().add("product-item");

    item.get("childs").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
      cell.getStyleClass().remove("product-item");
      if(!item.isNull("id") && item.isNull("owner") && (item.isNull("childs") || item.getValue("childs", Integer[].class).length == 0))
        cell.getStyleClass().add("product-item");
    });

    item.get("owner").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
      cell.getStyleClass().remove("product-item");
      if(!item.isNull("id") && item.isNull("owner") && (item.isNull("childs") || item.getValue("childs", Integer[].class).length == 0))
        cell.getStyleClass().add("product-item");
    });
  }
}
