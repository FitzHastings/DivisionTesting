package division.fx.client.plugins.deal.master;

import bum.interfaces.Service;
import client.util.ObjectLoader;
import division.fx.PropertyMap;
import division.fx.controller.process.ProcessTree;
import division.fx.dialog.FXD;
import division.fx.tree.FXTree;
import division.fx.util.MsgTrash;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;

public class FXDealProcessStep extends Step {
  private final PropertyMap deal;
  
  private final ProcessTree processTree = new ProcessTree();

  public FXDealProcessStep(String title, PropertyMap deal) {
    super(title);
    this.deal = deal;
    
    setCenter(processTree);
    processTree.setSelectionMode(SelectionMode.SINGLE);
    
    storeControls().add(processTree);
  }

  @Override
  public boolean start() {
    if(!isInit()) {
      processTree.addInitDataListener(e -> {
        if(deal.isNotNull("service")) {
          TreeItem item = FXTree.GetNode(processTree.getTree(), PropertyMap.create().setValue("id", deal.getInteger("service")));
          if(item != null) {
            processTree.getTree().getSelectionModel().select(item);
            while(item != null) {
              item.setExpanded(true);
              item = item.getParent();
            }
          }
        }
      });
      processTree.initData();
    }
    return super.start();
  }

  @Override
  public boolean end() {
    if(processTree.getTree().getSelectionModel().getSelectedItem() == null || !processTree.getTree().getSelectionModel().getSelectedItem().getChildren().isEmpty()) {
      FXD.showWait("Внимание", processTree, "Выберите конечный процесс", FXD.ButtonType.OK);
      return false;
    }
    
    try {
      deal.setValue("service", ObjectLoader.getMap(Service.class, processTree.selectedItemProperty().getValue().getValue().getInteger("id")));
    } catch (Exception ex) {
      MsgTrash.out(ex);
    }
    
    return true;
  }
}