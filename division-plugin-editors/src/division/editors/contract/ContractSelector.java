package division.editors.contract;

import bum.interfaces.CFC;
import bum.interfaces.Company;
import bum.interfaces.Contract;
import division.fx.PropertyMap;
import division.fx.controller.company.FXCompany;
import division.fx.dialog.FXDialog;
import division.fx.editor.FXTableEditor;
import division.fx.editor.FXTreeEditor;
import division.fx.editor.GLoader;
import division.fx.table.Column;
import division.fx.table.filter.DateFilter;
import division.fx.table.filter.ListFilter;
import division.fx.table.filter.TextFilter;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.stage.WindowEvent;
import util.filter.local.DBFilter;

public class ContractSelector {
  
  public static ObservableList<PropertyMap> getContract(Node parent) {
    return getContract(parent, SelectionMode.SINGLE, null);
  }
  
  public static ObservableList<PropertyMap> getContract(Node parent, SelectionMode selectionMode, DBFilter filter) {
    FXTreeEditor  cfcTable = new FXTreeEditor(CFC.class);
    FXTableEditor companyTable = new FXTableEditor(Company.class, FXCompany.class,
            FXCollections.observableArrayList("id"),
            Column.create("Наименование", "name", new TextFilter()),
            Column.create("ИНН", "inn", new TextFilter()));
    FXTableEditor contractTable = new FXTableEditor(Contract.class, 
            FXContract.class, FXCollections.observableArrayList("id"),
            Column.create("Наименование","templatename", new ListFilter("templatename")),
            Column.create("Продавец","Продавец=query:getCompanyName([Contract(sellerCompany)])", new TextFilter("Продавец")),
            Column.create("Покупатель","Покупатель=query:getCompanyName([Contract(customerCompany)])", new TextFilter("Покупатель")),
            Column.create("Номер","number", new TextFilter("number")),
            Column.create("Начало","startDate",new DateFilter("startDate")),
            Column.create("Окончание","endDate",new DateFilter("endDate")));
    
    cfcTable.getTree().setShowRoot(false);
    
    SplitPane hSplit = new SplitPane(cfcTable,companyTable);
    SplitPane vSplit = new SplitPane(hSplit,contractTable);
    vSplit.setOrientation(Orientation.VERTICAL);
    
    contractTable.setSelectionMode(selectionMode);

    if(filter != null)
      contractTable.getClientFilter().AND_FILTER(filter);
    
    DBFilter companyContractFilter = contractTable.getClientFilter().AND_FILTER();
    DBFilter cfcContractFilter = contractTable.getClientFilter().AND_FILTER();
    
    contractTable.getClientFilter().AND_FILTER(cfcContractFilter);
    contractTable.getClientFilter().AND_FILTER(companyContractFilter);
  
    cfcTable.initData();
    cfcTable.selectedItemProperty().addListener((ObservableValue<? extends TreeItem<PropertyMap>> observable, TreeItem<PropertyMap> oldValue, TreeItem<PropertyMap> newValue) -> {
      cfcContractFilter.clear();
      companyTable.getClientFilter().clear();
      if(cfcTable.getSelectedIds().length > 0) {
        cfcContractFilter.AND_FILTER().AND_IN("sellerCfc", cfcTable.getSelectedIds()).OR_IN("customerCfc", cfcTable.getSelectedIds());;
        companyTable.getClientFilter().AND_IN("cfcs", cfcTable.getSelectedIds());
      }
      companyTable.initData();
      contractTable.initData();
    });
    
    companyTable.selectedItemProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      companyContractFilter.clear();
      if(companyTable.getSelectedIds().length > 0)
        companyContractFilter.AND_IN("sellerCompany", companyTable.getSelectedIds()).OR_IN("customerCompany", companyTable.getSelectedIds());
      contractTable.initData();
    });
    
    if(FXDialog.show(parent, vSplit, "Выберите...", FXDialog.ButtonGroup.OK_CANCEL, (WindowEvent event) -> {
       if(event.getEventType() == event.WINDOW_CLOSE_REQUEST)
        contractTable.getTable().getSelectionModel().clearSelection();
      else if(event.getEventType() == event.WINDOW_SHOWING)
        GLoader.load("ContractSelector", cfcTable, companyTable, contractTable, vSplit, hSplit);
    }) == FXDialog.Type.CANCEL)
      contractTable.getTable().getSelectionModel().clearSelection();
    GLoader.store("ContractSelector", cfcTable, companyTable, contractTable, vSplit, hSplit);
    ObservableList<PropertyMap> list = FXCollections.observableArrayList(contractTable.getSelectedObjects());
    cfcTable.dispose();
    companyTable.dispose();
    contractTable.dispose();
    return list;
  }
}