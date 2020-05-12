package division.fx.client.plugins.product;

import bum.editors.util.ObjectLoader;
import bum.interfaces.Group;
import bum.interfaces.Product;
import bum.interfaces.Service;
import bum.interfaces.Service.Owner;
import division.fx.PropertyMap;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.dialog.FXD;
import division.fx.editor.FXTreeEditor;
import division.fx.tree.FXTree;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.control.CheckTreeView;
import util.filter.local.DBFilter;

public class FXProductCreator {
  private CheckBox objectUsedCheckBox = new CheckBox("Применяется к объектам");
  private final TitleBorderPane objectUsed = new TitleBorderPane(objectUsedCheckBox);
  private final ComboBox<String> sellerCustomer = new ComboBox(FXCollections.observableArrayList("К объектам продавца","К объектам покупателя"));
  private final CheckBox move = new CheckBox("Со сменой владельца");
  private final FXTreeEditor groupTree = new FXTreeEditor(Group.class);
  private final VBox content = new VBox(5);
  
  private PropertyMap processMemento = PropertyMap.create();
  private final FXTreeEditor.FXTreeSelector processSelector = FXTreeEditor.createSelector(content, "Выберите процесс....", "Процесс...", Service.class, false, null);
  
  private ObservableList<PropertyMap> productsMemento = FXCollections.observableArrayList();
  private final ObservableList<PropertyMap> products = FXCollections.observableArrayList();
  
  private PropertyMap company;

  public FXProductCreator(PropertyMap company, PropertyMap process) {
    this.company = company;
    groupTree.getTree().setShowRoot(false);
    groupTree.checkBoxTreeProperty().setValue(true);
    processSelector.getField().setEditable(true);
    sellerCustomer.setMaxWidth(Double.MAX_VALUE);
    sellerCustomer.getSelectionModel().select(0);
    move.disableProperty().bind(sellerCustomer.valueProperty().isEqualTo("К объектам покупателя"));
    objectUsed.setCenter(new VBox(5, new HBox(5, sellerCustomer, move), groupTree));
    objectUsed.getCenter().disableProperty().bind(objectUsedCheckBox.selectedProperty().not());
    ((HBox)sellerCustomer.getParent()).setAlignment(Pos.CENTER);
    HBox.setHgrow(sellerCustomer, Priority.ALWAYS);
    VBox.setVgrow(groupTree, Priority.ALWAYS);
    content.getChildren().addAll(processSelector, objectUsed);
    VBox.setVgrow(objectUsed, Priority.ALWAYS);
    
    groupTree.addInitDataListener(e -> {
      
      processSelector.valueProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
        if(oldValue != null)
          oldValue.unbindAll();
        
        FXTree.ListItems(groupTree.getTree()).stream().filter(n -> ((CheckBoxTreeItem)n).isSelected()).forEach(n -> ((CheckBoxTreeItem)n).setSelected(false));
        if(newValue != null) {
          newValue.unbindAll();
          
          newValue.copyFrom(ObjectLoader.getMap(Service.class, newValue.getInteger("id"), "owner","moveStorePosition"));
          processMemento = newValue.copy();
          objectUsedCheckBox.setSelected(!newValue.isNull("owner"));
          if(!newValue.isNull("owner")) {
            sellerCustomer.getSelectionModel().select(newValue.getValue("owner", Owner.class) == Owner.SELLER ? 0 : 1);
            if(!move.isDisable())
              move.setSelected(newValue.is("moveStorePosition"));
          }
          
          newValue.get("owner").bind(Bindings.createObjectBinding(() -> objectUsedCheckBox.isSelected() ? sellerCustomer.getSelectionModel().getSelectedIndex() == 0 ? Owner.SELLER : Owner.CUSTOMER : null, 
                  objectUsedCheckBox.selectedProperty(), sellerCustomer.valueProperty()));
          newValue.get("moveStorePosition").bind(move.selectedProperty().and(move.disableProperty().not()));
          
          ((CheckTreeView<PropertyMap>)groupTree.getTree()).getCheckModel().clearChecks();
          
          ObjectLoader.getList(DBFilter.create(Product.class).AND_EQUAL("service", newValue.getInteger("id")).AND_EQUAL("company", company.getInteger("id")), "group").stream().forEach(p -> {
            FXTreeEditor.FXCheckBoxTreeItem n = (FXTreeEditor.FXCheckBoxTreeItem) groupTree.getNodeObject(p.getInteger("group"));
            if(n != null) {
              n.setSelected(true);
              while(n.getParent() != null) {
                n.getParent().setExpanded(true);
                n = (FXTreeEditor.FXCheckBoxTreeItem) n.getParent();
              }
            }
            
            productsMemento = PropertyMap.copyList(products);
            
          });
        }
        
      });
      
      if(process != null)
        processSelector.valueProperty().setValue(process);
      
    });
    
    objectUsedCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(newValue)
        fillProductList();
      else products.clear();
    });
    
    FXCollections.observableArrayList(groupTree.getCheckedItems()).addListener((ListChangeListener.Change<? extends TreeItem<PropertyMap>> c) -> {
      fillProductList();
    });
    
    groupTree.initData();
  }
  
  private void fillProductList() {
    products.setAll(groupTree.getCheckedItems().stream().filter(g -> g.getChildren().isEmpty()).map(g -> 
            PropertyMap.create()
                    .setValue("type", FXProductTree.TreeItemType.PRODUCT)
                    .setValue("group", g.getValue().getInteger("id"))
                    .setValue("service", processSelector.valueProperty().getValue().getInteger("id"))
                    .setValue("company", company.getInteger("id"))).collect(Collectors.toList()));
  }

  public VBox getContent() {
    return content;
  }
  
  public List<PropertyMap> get(Node parent) {
    /*FXD fxd = FXD.create("Создание продукта...", parent, content, FXD.ButtonType.OK);
    
    fxd.setOnCloseRequest(e -> {
      if(isUpdate()) {
        switch(FXD.showWait("Закрытие", content, "Сохранить изменения?", FXD.ButtonType.YES, FXD.ButtonType.NO, FXD.ButtonType.CANCEL).orElseGet(() -> FXD.ButtonType.NO)) {
          case YES:save();break;
          case CANCEL:e.consume();break;
        }
      }
    });
    
    if(fxd.showDialog() == FXD.ButtonType.OK && isUpdate())
      save();*/
    
    if(FXD.showWait("Создание продукта...", parent, content, FXD.ButtonType.OK).orElseGet(() -> FXD.ButtonType.CANCEL) == FXD.ButtonType.CANCEL)
      products.clear();
    
    return products;
  }

  /*private boolean isUpdate() {
    Comparator<PropertyMap> s = (PropertyMap o1, PropertyMap o2) -> o1.getInteger("group").compareTo(o2.getInteger("group"));
    productsMemento.sort(s);
    products.sort(s);
    if(!processMemento.equals(processSelector.valueProperty().getValue()))
      System.out.println("process update");
    return !processMemento.equals(processSelector.valueProperty().getValue()) || !productsMemento.equals(products);
  }*/

  /*private void save() {
    
  }*/
}