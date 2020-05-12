package division.fx.client.plugins;

import conf.P;
import division.fx.PropertyMap;
import division.fx.tree.FXDivisionTreeTable;
import division.util.Utility;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.cell.TextFieldTreeTableCell;

public class Configuration extends FXPlugin {
  private final TreeItem<PropertyMap> root = new TreeItem(PropertyMap.create());
  private final FXDivisionTreeTable<PropertyMap> treetable = new FXDivisionTreeTable(root);
  private final TreeTableColumn<PropertyMap, String>  objectColumn = new TreeTableColumn<>("Парамметр");
  private final TreeTableColumn<PropertyMap, String>  valueColumn = new TreeTableColumn<>("Значение");

  @Override
  public void start() {
    objectColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<PropertyMap, String> param) -> param.getValue().getValue().get("name"));
    valueColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<PropertyMap, String> param) -> param.getValue().getValue().get("value"));
    
    treetable.setEditable(true);
    valueColumn.setEditable(true);
    
    valueColumn.setCellFactory((TreeTableColumn<PropertyMap, String> param) -> new TextFieldTreeTableCell() {
      @Override
      public void startEdit() {
        if(getTreeTableRow().getTreeItem().getChildren().isEmpty())
          super.startEdit();
      }
    });
    
    treetable.getColumns().setAll(objectColumn, valueColumn);
    treetable.setShowRoot(false);
    storeControls().add(treetable);
    fill(root, PropertyMap.fromJson(Utility.getStringFromFile(P.DEF_CONF_FILE)));
    show("Настройки");
  }

  @Override
  public Node getContent() {
    return treetable;
  }

  private void fill(TreeItem<PropertyMap> parent, PropertyMap p) {
    p.keySet().stream().forEach(key -> {
      Object value = p.getValue(key);
      if(value instanceof PropertyMap) {
        TreeItem<PropertyMap> pitem = new TreeItem(PropertyMap.create().setValue("name", key));
        parent.getChildren().add(pitem);
        fill(pitem, p.getMap(key));
      }else if(value instanceof ObservableList) {
        TreeItem<PropertyMap> listItems = new TreeItem(PropertyMap.create().setValue("name", key));
        parent.getChildren().add(listItems);
        ObservableList<PropertyMap> list = p.getList(key);
        for(int i=0;i<list.size();i++) {
          TreeItem<PropertyMap> pitem = new TreeItem(PropertyMap.create().setValue("name", "item-"+(i+1)));
          listItems.getChildren().add(pitem);
          fill(pitem, list.get(i));
        }
      }else {
        TreeItem<PropertyMap> item = new TreeItem(PropertyMap.create().setValue("name", key).setValue("value", value));
        parent.getChildren().add(item);
      }
    });
  }
}