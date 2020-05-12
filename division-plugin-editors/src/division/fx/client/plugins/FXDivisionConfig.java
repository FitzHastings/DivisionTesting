package division.fx.client.plugins;

import division.fx.DivisionTextField;
import division.fx.PropertyMap;
import division.fx.util.MsgTrash;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.util.converter.IntegerStringConverter;

public class FXDivisionConfig extends FXPlugin {
  @FXML private DivisionTextField jmsProtocol;
  @FXML private DivisionTextField jmsHost;
  @FXML private DivisionTextField rmiHost;
  @FXML private DivisionTextField rmiTimeout;
  @FXML private DivisionTextField rmiName;
  @FXML private DivisionTextField rmiPort;
  @FXML private DivisionTextField jmsPort;
  @FXML private DivisionTextField rmiCount;
  @FXML private Button addToolButton;
  @FXML private Button removeToolButton;
  @FXML private TreeView<PropertyMap> menuTree;
  
  private TreeItem<PropertyMap> root = new TreeItem<>(PropertyMap.create().setValue("name", "Меню"));
  
  private PropertyMap memento, config;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    super.initialize(location, resources); //To change body of generated methods, choose Tools | Templates.
    
    rmiTimeout.setConverter(new IntegerStringConverter());
    rmiPort.setConverter(new IntegerStringConverter());
    jmsPort.setConverter(new IntegerStringConverter());
    rmiCount.setConverter(new IntegerStringConverter());
    
    try {
      config = PropertyMap.fromJsonFile("conf"+File.separator+"conf.json");
      memento = config.copy();
      
      jmsProtocol.setText((String)((PropertyMap)config.getValue("messanger")).getValue("protocol"));
      jmsHost.setValue(((PropertyMap)config.getValue("messanger")).getValue("host"));
      jmsPort.setValue(((Number)((PropertyMap)config.getValue("messanger")).getValue("port")).intValue());
      
      rmiName.setText((String)((PropertyMap)config.getValue("server")).getValue("name"));
      rmiHost.setText((String)((PropertyMap)config.getValue("server")).getValue("host"));
      rmiPort.setValue(((Number)((PropertyMap)config.getValue("server")).getValue("port")).intValue());
      rmiTimeout.setValue(((Number)((PropertyMap)config.getValue("server")).getValue("timeout")).intValue());
      rmiCount.setValue(((Number)((PropertyMap)config.getValue("server")).getValue("connection-count")).intValue());
      
      ((PropertyMap)config.getValue("messanger")).get("protocol").bind(jmsProtocol.textProperty());
      ((PropertyMap)config.getValue("messanger")).get("host").bind(jmsHost.textProperty());
      ((PropertyMap)config.getValue("messanger")).get("port").bind(jmsPort.valueProperty());
      
      ((PropertyMap)config.getValue("server")).get("name").bind(rmiName.textProperty());
      ((PropertyMap)config.getValue("server")).get("host").bind(rmiHost.textProperty());
      ((PropertyMap)config.getValue("server")).get("port").bind(rmiPort.valueProperty());
      ((PropertyMap)config.getValue("server")).get("timeout").bind(rmiTimeout.valueProperty());
      ((PropertyMap)config.getValue("server")).get("connection-count").bind(rmiCount.valueProperty());
      
      menuTree.setRoot(root);
      if(config.containsKey("menubar"))
        config.getList("menubar").stream().forEach(m -> root.getChildren().add(createMenu(m)));
      root.setExpanded(true);
    }catch(Exception ex) {
      MsgTrash.out(ex);
    }
  }
  
  private TreeItem createMenu(PropertyMap m) {
    TreeItem menu = new TreeItem(m);
    if(m.containsKey("items"))
      m.getList("items").stream().forEach(it -> menu.getChildren().add(createMenu(it)));
    return menu;
  }

  @Override
  protected boolean closing() {
    if(!memento.equals(config)) {
      Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Сохранить изменения?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
      ButtonType type = a.showAndWait().get();
      if(type == ButtonType.CANCEL)
        return false;
      if(type == ButtonType.NO)
        return true;
      config.saveAsJsonFile("conf"+File.separator+"conf.json");
    }
    return super.closing(); //To change body of generated methods, choose Tools | Templates.
  }
  
  @Override
  public void start() {
    show("Настройки");
  }
}