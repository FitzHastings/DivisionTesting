package division.fx.controller.store;

import bum.interfaces.Group;
import bum.interfaces.Store;
import client.util.ObjectLoader;
import division.fx.DivisionTextField;
import division.fx.FXButton;
import division.fx.FXUtility;
import division.fx.PropertyMap;
import division.fx.dialog.FXD;
import division.fx.editor.FXTreeEditor;
import division.fx.editor.GLoader;
import division.fx.tree.FXDivisionTree;
import division.fx.util.MsgTrash;
import division.util.IDStore;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.converter.BigDecimalStringConverter;
import util.filter.local.DBFilter;

public class FXEquipmentCreator {
  private final FXTreeEditor         groupTree   = new FXTreeEditor(Group.class);
  private final EquipmentFactorTable factorTable = new EquipmentFactorTable();
  
  private final Label                nameLabel     = new Label();
  private final Label                descriptLabel = new Label("Описание:");
  private final TextArea             descript      = new TextArea();
  
  private final FXTreeEditor         storeTree     = new FXTreeEditor(Store.class, null, "storeType", "objectType", "controllIn", "main");
  
  //private final TreeCombobox stories = new TreeCombobox();
  
  private final DivisionTextField<BigDecimal>    count         = new DivisionTextField(new BigDecimalStringConverter(), BigDecimal.ONE);
  private final Label                unit          = new Label();
  private final Button               addButton     = new FXButton(e -> addEquipment(), "add-equipment");
  private final HBox                 addBox        = new HBox(5, count, unit, addButton);
  private final VBox                 dataBox       = new VBox(5, nameLabel,descriptLabel,descript,addBox);
  
  private final Pane                 pane = new Pane(groupTree, dataBox);
  
  private final SplitPane split = new SplitPane(pane, factorTable);
  
  private final FXTreeEditor.FXTreeSelector storeSelector;

  private FXEquipmentCreator(Integer companyPartitionId) {
    FXUtility.initMainCss(this);
    FXUtility.initMainCss(split);
    
    storeSelector = FXTreeEditor.createSelector(DBFilter.create(Store.class).AND_EQUAL("companyPartition", companyPartitionId), split, "Выберите склад", null, "objectType");
    
    storeSelector.getField().valueProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      groupTree.getClientFilter().clear().AND_EQUAL("groupType", newValue.getValue("objectType"));
      groupTree.initData();
    });
    
    groupTree.getTools().getItems().addAll(new Separator(), storeSelector);
    groupTree.setRemovable(false);
    
    groupTree.titleProperty().setValue("Активы");
    
    //stories.treeProperty().bind(storeTree.treeProperty());
    storeTree.titleProperty().setValue("Склады");
    
    storeTree.addInitDataListener(e -> {
      TreeItem<PropertyMap> storeItem = ((FXDivisionTree<PropertyMap>)storeTree.getTree()).listItems().stream().filter(t -> t.getChildren().isEmpty() && t.getValue().is("main"))
              .findFirst().orElseGet(() -> ((FXDivisionTree<PropertyMap>)storeTree.getTree()).listItems().stream().filter(t -> t.getChildren().isEmpty()).findFirst().orElseGet(() -> null));
      storeTree.getTree().getSelectionModel().select(storeItem);
    });
    
    
    storeTree.getClientFilter().AND_EQUAL("companyPartition", companyPartitionId);
    storeTree.initData();
    
    /*stories.selectedItemProperty().addListener((ObservableValue<? extends TreeItem<PropertyMap>> observable, TreeItem<PropertyMap> oldValue, TreeItem<PropertyMap> newValue) -> {
      if(newValue != null && newValue.getChildren().isEmpty()) {
        groupTree.getClientFilter().clear().AND_EQUAL("groupType", newValue.getValue().getValue("objectType"));
        groupTree.initData();
      }
    });*/
    
    split.setOrientation(Orientation.VERTICAL);
    VBox.setVgrow(descript, Priority.ALWAYS);
    HBox.setHgrow(count, Priority.ALWAYS);
    addBox.setAlignment(Pos.CENTER_LEFT);
    
    groupTree.prefWidthProperty().bind(pane.widthProperty());
    groupTree.maxWidthProperty().bind(pane.widthProperty());
    groupTree.minWidthProperty().bind(pane.widthProperty());
    
    groupTree.prefHeightProperty().bind(pane.heightProperty());
    groupTree.maxHeightProperty().bind(pane.heightProperty());
    groupTree.minHeightProperty().bind(pane.heightProperty());
    
    dataBox.layoutYProperty().bind(groupTree.getTree().layoutYProperty());
    dataBox.prefHeightProperty().bind(groupTree.getTree().heightProperty());
    dataBox.layoutXProperty().bind(groupTree.layoutXProperty().add(groupTree.widthProperty()).subtract(dataBox.widthProperty()));
    
    factorTable.amountEditableProperty().setValue(true);
    
    initEvents();
    
    dataBox.setPrefWidth(0);
    dataBox.setMinWidth(0);
  }
  
  public static List<PropertyMap> getFromStore(Node parent, Integer storeId) throws Exception {
    PropertyMap store = ObjectLoader.getMap(Store.class, storeId);
    FXEquipmentCreator creator = new FXEquipmentCreator(store.getInteger("companyPartition"));
    creator.storeSelector.valueProperty().setValue(store);
    FXD fxd = FXD.create("", parent, creator.split, FXD.ButtonType.OK, FXD.ButtonType.CANCEL);

    fxd.setOnShowing(e -> GLoader.load(creator.getClass().getName(), creator.split, creator.factorTable));
    fxd.setOnHiding(e -> GLoader.store(creator.getClass().getName(), creator.split, creator.factorTable));

    if(fxd.showDialog() == FXD.ButtonType.CANCEL)
      creator.factorTable.clear();
    return creator.factorTable.getItems();
  }
  
  public static List<PropertyMap> getFromPartition(Node parent, Integer companyPartitionId) {
    FXEquipmentCreator creator = new FXEquipmentCreator(companyPartitionId);
    FXD fxd = FXD.create("", parent, creator.split, FXD.ButtonType.OK, FXD.ButtonType.CANCEL);
    
    fxd.setOnShowing(e -> GLoader.load(creator.getClass().getName(), creator.split, creator.factorTable));
    fxd.setOnHiding(e -> GLoader.store(creator.getClass().getName(), creator.split, creator.factorTable));
    
    if(fxd.showDialog() == FXD.ButtonType.CANCEL)
      creator.factorTable.clear();
    return creator.factorTable.getItems();
  }

  private void initEvents() {
    groupTree.setSelectionMode(SelectionMode.MULTIPLE);
    groupTree.selectedItemProperty().addListener((ObservableValue<? extends TreeItem<PropertyMap>> observable, TreeItem<PropertyMap> oldValue, TreeItem<PropertyMap> newValue) -> {
      try {
        descript.setText("");
        unit.setText("");
        nameLabel.setText("");

        
        List<TreeItem<PropertyMap>> list = groupTree.getTree().getSelectionModel().getSelectedItems().filtered(g -> g != null && g.getChildren().isEmpty());
        descript.setDisable(list.size() > 1);
        unit.setDisable(list.size() > 1);
        addButton.setDisable(list.isEmpty());
        double width = list.isEmpty() ? 0 : groupTree.getWidth()/2;

        List<Integer> ids = list.stream().map(g -> g.getValue().getInteger("id")).collect(Collectors.toList());
        for(PropertyMap p:ObjectLoader.getList(Group.class, ids, "id", "name", "parent", "description", "unit", "factors", "iden_id","iden_name","iden_factorType","iden_unit","iden_listValue")) {
          TreeItem<PropertyMap> t = list.stream().filter(g -> g.getValue().getInteger("id").equals(p.getInteger("id"))).findFirst().orElseGet(() -> null);
          if(t != null) {
            t.setValue(p);
            nameLabel.setText((nameLabel.getText().equals("") ? "" : nameLabel.getText()+", ")+p.getString("name"));
            descript.setText(p.getString("description"));
          }
        }
        
        split.getScene().getStylesheets().stream().forEach(s -> System.out.println(s));

        Timeline tl = new Timeline(new KeyFrame(Duration.millis(500), new KeyValue(dataBox.prefWidthProperty(), dataBox.getWidth()), new KeyValue(dataBox.prefWidthProperty(), width)));
        tl.play();
      }catch(Exception ex) {
        MsgTrash.out(ex);
      }
    });
    
    groupTree.getTree().setOnMouseClicked(e -> {
      if(e.getClickCount() == 2 && groupTree.getTree().getSelectionModel().getSelectedItem() != null && groupTree.getTree().getSelectionModel().getSelectedItem().getChildren().isEmpty())
        addEquipment(BigDecimal.ONE);
    });
  }
  
  private void addEquipment() {
    addEquipment(count.getValue());
  }

  private void addEquipment(BigDecimal c) {
    factorTable.addItems(groupTree.getTree().getSelectionModel().getSelectedItems().stream().filter(it -> it.getChildren().isEmpty())
            .map(it -> it.getValue()
                    .setValue("group", it.getValue().getInteger("id"))
                    .setValue("id", "n-"+IDStore.createID())
                    .setValue("store", storeTree.selectedItemProperty().getValue().getValue().getInteger("id"))
                    .setValue("amount", count.getValue())).collect(Collectors.toList()).toArray(new PropertyMap[0]));
    factorTable.initHeader();
  }
}