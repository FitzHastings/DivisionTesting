package division.fx.controller.company.partition;

import division.fx.PropertyMap;
import division.fx.gui.FXStorable;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;

public class FXCompanyPartition extends TabPane implements FXStorable {
  private final FXPartitionDetails details = new FXPartitionDetails();
  private final FXPartitionKKT kkt = new FXPartitionKKT();
  
  private final Tab partDataTab = new Tab("Реквизиты", details);
  private final Tab storeTab    = new Tab("Активы", new Pane());
  private final Tab docTab      = new Tab("Документооборот", new Pane());
  private final Tab kktTab      = new Tab("Контрольно-Кассовая техника", kkt);
  
  private final ObjectProperty<PropertyMap> partitionProperty = new SimpleObjectProperty<>();

  public FXCompanyPartition() {
    getTabs().addAll(partDataTab, storeTab, docTab, kktTab);
    getTabs().forEach(t -> t.setClosable(false));
    
    details.partitionProperty().bind(partitionProperty);
    kkt.partitionProperty().bind(partitionProperty);
    
    partitionProperty.addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      initData();
    });
  }

  public ObjectProperty<PropertyMap> partitionProperty() {
    return partitionProperty;
  }

  @Override
  public List<Node> storeControls() {
    return FXCollections.observableArrayList(details, kkt);
  }

  private void initData() {
  }
}