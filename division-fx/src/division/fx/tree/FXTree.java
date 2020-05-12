package division.fx.tree;

import division.fx.PropertyMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;

public interface FXTree {
  public static <T extends PropertyMap> List<TreeItem<T>> ListItems(TreeView<T> tree) {
    return ListItems(tree.getRoot());
  }
  
  public static <T extends PropertyMap> List<TreeItem<T>> ListItems(TreeTableView<T> tree) {
    return ListItems(tree.getRoot());
  }
  
  public static <T extends PropertyMap> List<TreeItem<T>> ListItems(TreeItem<T> item) {
    if(item == null)
      return FXCollections.observableArrayList();
    List<TreeItem<T>> list = item.getChildren().stream().flatMap(ch -> ListItems(ch).stream()).collect(Collectors.toList());
    if(item != null && item.getParent() != null)
      list.add(item);
    return list;
  }
  
  public static <T extends PropertyMap> List<TreeItem<T>> ListItems(TreeView<T> tree, String propertyname, Object value) {
    return ListItems(tree).stream().filter(it -> it.getValue().containsKey(propertyname) && Objects.equals(it.getValue().getValue(propertyname), value)).collect(Collectors.toList());
  }
  
  public static <T extends PropertyMap> List<TreeItem<T>> ListItems(TreeTableView<T> tree, String propertyname, Object value) {
    return ListItems(tree).stream().filter(it -> it.getValue().containsKey(propertyname) && Objects.equals(it.getValue().getValue(propertyname), value)).collect(Collectors.toList());
  }
  
  
  public static <T extends PropertyMap> List<TreeItem<T>> ListItems(TreeItem<T> parent, String propertyname, Object value) {
    return ListItems(parent).stream().filter(it -> it.getValue().containsKey(propertyname) && Objects.equals(it.getValue().getValue(propertyname), value)).collect(Collectors.toList());
  }
  
  public static <T extends PropertyMap> List<TreeItem<T>> ListItems(TreeItem<T> parent, String propertyname, Object value, boolean withParent) {
    List<TreeItem<T>> list = ListItems(parent, propertyname, value);
    if(!withParent)
      list.remove(parent);
    return list;
  }
  
  public static <T extends PropertyMap> TreeItem<T> GetNode(TreeView<T> tree, PropertyMap fields) {
    return ListItems(tree).stream().filter(n -> n.getValue().getSimpleMap(fields.keySet()).equals(fields.getSimpleMap())).findFirst().orElseGet(() -> null);
  }
  
  public static <T extends PropertyMap> TreeItem<T> GetNode(TreeTableView<T> tree, PropertyMap fields) {
    return ListItems(tree).stream().filter(n -> n.getValue().getSimpleMap(fields.keySet()).equals(fields.getSimpleMap())).findFirst().orElseGet(() -> null);
  }
  
  public static <T extends PropertyMap> TreeItem<T> GetNode(TreeView<T> tree, Map map) {
    return ListItems(tree).stream().filter(n -> {
      return n.getValue().getSimpleMap(map.keySet()).equals(map);
    }).findFirst().orElseGet(() -> null);
  }
  
  public static <T extends PropertyMap> TreeItem<T> GetNode(TreeTableView<T> tree, Map map) {
    return ListItems(tree).stream().filter(n -> {
      return n.getValue().getSimpleMap(map.keySet()).equals(map);
    }).findFirst().orElseGet(() -> null);
  }
  
  public static <T extends PropertyMap> TreeItem<T> GetNode(TreeItem<T> parent, PropertyMap fields) {
    return GetNode(parent, fields.getSimpleMap());
  }
  
  public static <T extends PropertyMap> TreeItem<T> GetNode(TreeItem<T> parent, Map map) {
    return ListItems(parent).stream().filter(n -> n.getValue().getSimpleMap(map.keySet()).equals(map)).findFirst().orElseGet(() -> null);
  }
}