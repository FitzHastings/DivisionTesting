package division.fx.gui;

import division.fx.PropertyMap;
import java.io.File;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TableColumnBase;
import javafx.scene.layout.Region;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

public class FXGLoader {
  private static PropertyMap conf = PropertyMap.fromJsonFile("conf"+File.separator+"conf.json");
  
  private static String fileName(String name) {
    return (conf.isNull("gui-state-path") ? "" : (conf.getString("gui-state-path")+File.separator))+name+".json";
  }
  
  public static void load(String name, Object... storeControls) {
    try {
      if(storeControls.length > 0 && name != null) {
        //if(storeControls[0] instanceof List)
          //load(name, ((List)storeControls[0]).toArray());
        //else {
          PropertyMap doc = PropertyMap.fromJsonFile(fileName(name));
          if(doc != null) {
            for(Node control:validateStoreControls(storeControls)) {
              String id = control.getId();
              PropertyMap node = doc.getMap(control.getId());
              if(node != null) {
                if(control instanceof Region)
                  ((Region)control).setPrefSize(node.getDouble("width"), node.getDouble("height"));

                if(control instanceof SplitPane)
                  ((SplitPane)control).setDividerPositions(ArrayUtils.toPrimitive(node.getList("positions", Double.TYPE).toArray(new Double[0])));
                
                if(control instanceof TableView)
                  loadColumns(((TableView)control).getColumns(), node.getList("columns"));

                if(control instanceof TreeTableView)
                  loadColumns(((TreeTableView)control).getColumns(), node.getList("columns"));
              }
            }
          }
        //}
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      Logger.getLogger(FXGLoader.class).error(ex);
    }
  }
  
  public static void store(String name, Object... storeControls) {
    try {
      if(storeControls.length > 0 && name != null) {
        //if(storeControls[0] instanceof List)
          //store(name, ((List)storeControls[0]).toArray());
        //else {
          PropertyMap doc = PropertyMap.create();
          for(Node control:validateStoreControls(storeControls)) {
            PropertyMap node = PropertyMap.create();
            doc.setValue(control.getId(), node);

            if(control instanceof Region) {
              node.setValue("width", ((Region)control).getWidth());
              node.setValue("height", ((Region)control).getHeight());
            }

            if(control instanceof SplitPane)
              node.setValue("positions",((SplitPane)control).getDividerPositions());
            
            if(control instanceof TableView)
              node.setValue("columns", storeColumns(((TableView)control).getColumns()));

            if(control instanceof TreeTableView)
              node.setValue("columns", storeColumns(((TreeTableView)control).getColumns()));
          }
          doc.saveAsJsonFile(fileName(name));
        //}
      }
    } catch (Exception ex) {
      Logger.getLogger(FXGLoader.class).error(ex);
    }
  }
  
  public static ObservableList<PropertyMap> storeColumns(ObservableList<TableColumnBase> columns) {
    ObservableList<PropertyMap> nodes = FXCollections.observableArrayList();
    columns.stream().forEach(column -> {
      PropertyMap node = PropertyMap.create().setValue("text", column.getText()).setValue("width",column.getWidth());
      if(!column.getColumns().isEmpty())
        node.setValue("columns", storeColumns(column.getColumns()));
      nodes.add(node);
    });
    return nodes;
  }
  
  public static void loadColumns(ObservableList<TableColumnBase> columns, ObservableList<PropertyMap> nodes) {
    columns.forEach(column -> {
      if(PropertyMap.contains(nodes, "text", column.getText())) {
        PropertyMap.get(nodes, "text", column.getText()).stream().forEach(node -> {
          if(!column.prefWidthProperty().isBound())
            column.setPrefWidth(node.getDouble("width"));
          loadColumns(column.getColumns(), node.getList("columns"));
        });
      }
    });
  }
  
  public static ObservableList<Node> validateStoreControls(Object... storeControls) {
    return validateStoreControls(1, storeControls);
  }
  
  public static ObservableList<Node> validateStoreControls(Integer prefix, Object... storeControls) {
    ObservableList<Node> list = FXCollections.observableArrayList();
    for(Object n:storeControls) {
      if(n instanceof FXStorable)
        list.addAll(validateStoreControls(prefix+1,((FXStorable)n).storeControls()));
      if(n instanceof List)
        list.addAll(validateStoreControls(prefix+1,((List)n).toArray()));
      if(n instanceof Node)
        list.add((Node)n);
    }
    list.stream().forEach(node -> node.setId(node.getId() == null ? prefix+"-"+getId(node) : node.getId()));
    return list;
  }
  
  public static String getId(Node node) {
    String id = node.getClass().getSimpleName()+":"+String.join("_", node.getStyleClass());
    node = node.getParent();
    while(node != null) {
      id = node.getClass().getSimpleName()+"_"+id;
      node = node.getParent();
    }
    return id;
  }
}