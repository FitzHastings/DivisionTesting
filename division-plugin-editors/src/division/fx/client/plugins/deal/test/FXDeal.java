package division.fx.client.plugins.deal.test;

import bum.interfaces.Service;
import client.util.ObjectLoader;
import division.fx.ChoiseLabel.ChoiceLabel;
import division.fx.DateLabel.DateLabel;
import division.fx.LinkLabel;
import division.fx.PropertyMap;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.controller.process.ProcessTree;
import division.fx.editor.FXObjectEditor;
import division.fx.util.MsgTrash;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class FXDeal extends FXObjectEditor {
  private final LinkLabel     processLabel = new LinkLabel("Процесс...", e -> {
    ProcessTree processEditor = new ProcessTree();
    processEditor.setSelectionMode(SelectionMode.SINGLE);
    List<PropertyMap> process = processEditor.getObjects(getRoot().getScene().getRoot(), "Процессы");
    if(process.size() > 0) {
      getObjectProperty().setValue("service", process.get(0));
    }
  });
  
  //private 
  //private TitleBorderPane objectPane = new TitleBorderPane();
  
  private final Label     objectLabel  = new Label("Объект...");
  
  private final DateLabel dateLabel    = new DateLabel("Дата...");
  
  private final ChoiceLabel<PropertyMap> sellerCFCLabel   = new ChoiceLabel<>("Группа...");
  private final ChoiceLabel<PropertyMap> customerCFCLabel = new ChoiceLabel<>("Группа...");
  
  private final ChoiceLabel<PropertyMap> sellerPartitionLabel   = new ChoiceLabel<>("Подразделение...");
  private final ChoiceLabel<PropertyMap> customerPartitionLabel = new ChoiceLabel<>("Подразделение...");
  
  private final VBox sellerBox   = new VBox(5, sellerCFCLabel, sellerPartitionLabel);
  private final VBox customerBox = new VBox(5, customerCFCLabel, customerPartitionLabel);
  
  private final TitleBorderPane sellerPane   = new TitleBorderPane(sellerBox, "Исполнитель");
  private final TitleBorderPane customerPane = new TitleBorderPane(customerBox, "Заказчик");
  
  private final GridPane sideGrid = new GridPane();
  
  private final VBox root = new VBox(5, new HBox(5, processLabel, dateLabel), objectLabel, sideGrid);
  
  @Override
  public void initData() {
    try {
      
      if(getObjectProperty().isNotNull("service"))
        getObjectProperty().setValue("service", ObjectLoader.getMap(Service.class, getObjectProperty().getInteger("service")));
      
    }catch (Exception ex) {
      MsgTrash.out(ex);
    }
    
    processLabel.textProperty().bind(Bindings.createStringBinding(() -> getObjectProperty().isNotNull("service") ? getObjectProperty().getMap("service").getString("name") : "Процесс...", getObjectProperty().get("service")));
    
    getRoot().setCenter(root);
    
    sideGrid.addRow(0, sellerPane, customerPane);
    sideGrid.getColumnConstraints().addAll(new ColumnConstraints(), new ColumnConstraints());
    sideGrid.getColumnConstraints().forEach(c -> c.setPercentWidth(50));
    
    
  }
}