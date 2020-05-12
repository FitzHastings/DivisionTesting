package division.fx.controller.company.partition;

import atolincore5.Atolin;
import atolincore5.iDevice;
import division.fx.FXToolButton;
import division.fx.PropertyMap;
import division.fx.gui.FXStorable;
import division.fx.table.Column;
import division.fx.table.FXDivisionTable;
import division.fx.util.MsgTrash;
import java.rmi.RemoteException;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class FXPartitionKKT extends BorderPane implements FXStorable {
  private final ObjectProperty<PropertyMap> partitionProperty = new SimpleObjectProperty<>();
  
  ObjectProperty<ObservableList<PropertyMap>> observableListKkt = new SimpleObjectProperty<>();
  
  private FXDivisionTable<PropertyMap> kktTable = new FXDivisionTable<>(true, Column.create("Хост", "ip", true, true), Column.create("Порт", "port", true, true, 1099), Column.create("Имя", "name", true, true));
  private final FXToolButton addKKT  = new FXToolButton(e -> observableListKkt.getValue().add(PropertyMap.create()), "Добавить кассу", "add-tool-button");
  private final FXToolButton delKKT  = new FXToolButton(e -> observableListKkt.getValue().removeAll(kktTable.getSelectionModel().getSelectedItems()), "Удалить кассу", "remove-tool-button");
  private final ToolBar      tools   = new ToolBar(addKKT, delKKT);
  
  private final Button       xReport  = new Button("X-отчёт");
  private final Button       zReport  = new Button("Z-отчёт");
  
  private final SplitPane split = new SplitPane(new VBox(5, tools, kktTable), new VBox(5, xReport, zReport));
  
  
  
  public FXPartitionKKT() {
    setCenter(split);
    
    xReport.setOnAction(e -> {
      try {
        getDevice().xReport();
      }catch(Exception ex) {
        MsgTrash.out(ex);
      }
    });
    
    zReport.setOnAction(e -> {
      try {
        getDevice().closeShift();
      }catch(Exception ex) {
        MsgTrash.out(ex);
      }
    });
    
    
    
    
    
    partitionProperty.addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      observableListKkt = new SimpleObjectProperty<>(FXCollections.observableArrayList(newValue.getList("kkt")));
      kktTable.itemsProperty().bind(observableListKkt);
      newValue.get("kkt").bind(observableListKkt);
    });
    
    /*kktTable.getItems().addListener(new ListChangeListener<PropertyMap>() {
      @Override
      public void onChanged(ListChangeListener.Change<? extends PropertyMap> c) {
        while(c.next()) {
          if(c.wasUpdated()) {
            System.out.println("UPDATED LIST DEVICES");
          }
        }
      }
    });*/
    
    xReport.disableProperty().bind(kktTable.getSelectionModel().selectedItemProperty().isNull());
    zReport.disableProperty().bind(kktTable.getSelectionModel().selectedItemProperty().isNull());
  }
  
  private iDevice getDevice() throws Exception {
    PropertyMap kkt = kktTable.getSelectionModel().getSelectedItem();
    System.out.println(kkt.getString("ip")+":"+kkt.getInteger("port")+" "+kkt.getString("name"));
    return Atolin.getConnectionLocal(kkt.getString("ip"), kkt.getInteger("port"), kkt.getString("name"));
  }
  
  public ObjectProperty<PropertyMap> partitionProperty() {
    return partitionProperty;
  }

  @Override
  public List<Node> storeControls() {
    return FXCollections.observableArrayList(kktTable,split);
  }
}