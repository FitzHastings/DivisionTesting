package division.fx.client.plugins.deal.master;

import bum.interfaces.Group;
import bum.interfaces.Service;
import division.fx.PropertyMap;
import division.fx.client.plugins.product.FXStoreForProducts;
import division.fx.editor.FXTreeEditor;
import java.math.BigDecimal;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;

public class FXDealPositionsStep extends Step {
  private final PropertyMap deal;
  
  private final FXTreeEditor       groupTree        = new FXTreeEditor(Group.class);
  private final FXStoreForProducts storeForProducts = new FXStoreForProducts();
  private final SplitPane          objectSplit      = new SplitPane(groupTree, storeForProducts);

  public FXDealPositionsStep(String title, PropertyMap deal) {
    super(title);
    this.deal = deal;
    
    objectSplit.setOrientation(Orientation.VERTICAL);
    storeForProducts.groupProperty().bind(Bindings.createObjectBinding(() -> groupTree.selectedItemProperty().getValue() == null ? null : groupTree.selectedItemProperty().getValue().getValue(), groupTree.selectedItemProperty()));
    
    setCenter(objectSplit);
    storeControls().addAll(groupTree, storeForProducts, objectSplit);
  }

  @Override
  public boolean end() {
    deal.setValue("positions", storeForProducts.positionsProperty().getValue());
    if(deal.getList("positions").isEmpty())
      deal.setValue("positions", FXCollections.observableArrayList(PropertyMap.create()
              .setValue("amount", BigDecimal.ONE)
              .setValue("group_name", deal.getMap("service").getString("name"))));
    return super.end();
  }

  @Override
  public boolean start() {
    if(!isInit())
      groupTree.initData();
    if(deal.getValue("sellerCompanyPartition") instanceof PropertyMap)
      storeForProducts.partitionProperty().setValue(deal.getMap(deal.getMap("service").getValue("owner", Service.Owner.class) == Service.Owner.SELLER ? "sellerCompanyPartition" : "customerCompanyPartition"));
    else storeForProducts.partitionProperty().setValue(PropertyMap.create().setValue("id", deal.getInteger(deal.getMap("service").getValue("owner", Service.Owner.class) == Service.Owner.SELLER ? "sellerCompanyPartition" : "customerCompanyPartition")));
    return super.start(); //To change body of generated methods, choose Tools | Templates.
  }
  
  @Override
  void startCurrent() {
    if(deal.getMap("service").isNull("owner")) {
      getMaster().next();
    }
  }
}