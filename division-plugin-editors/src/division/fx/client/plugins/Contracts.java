package division.fx.client.plugins;

import bum.interfaces.Contract;
import division.fx.PropertyMap;
import division.fx.controller.contract.FXContract_;
import division.fx.table.Column;
import division.fx.editor.FXTableEditor;
import division.fx.table.FXDivisionTableCell;
import division.fx.table.filter.DateFilter;
import division.fx.table.filter.ListFilter;
import division.fx.table.filter.TextFilter;
import division.util.Utility;
import java.time.LocalDate;
import java.util.Collection;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import util.filter.local.DBFilter;

public class Contracts extends FXPlugin {
  private final FXTableEditor contracts = new FXTableEditor(Contract.class, FXContract_.class, 
            FXCollections.observableArrayList("id"),
            Column.create("Наименование","templatename", new ListFilter("templatename")),
            Column.create("Продавец","Продавец=query:getCompanyName([Contract(sellerCompany)])", new TextFilter("Продавец")),
            Column.create("Покупатель","Покупатель=query:getCompanyName([Contract(customerCompany)])", new TextFilter("Покупатель")),
            Column.create("Номер","number", new TextFilter("number")),
            Column.create("Начало","startDate",new DateFilter("startDate")),
            Column.create("Окончание","endDate",new DateFilter("endDate"))) {
              @Override
              public String getDragString(Collection<PropertyMap> objects) {
                String str = "";
                for(PropertyMap c:objects)
                  str += "\n"+c.getString("templatename")+" №"+c.getString("number");
                return str.substring(2);
              }
            };
  
  private final DBFilter CFCFilter = contracts.getClientFilter().AND_FILTER();
  private final DBFilter companyFilter = contracts.getClientFilter().AND_FILTER();
  private final DBFilter partitionFilter = contracts.getClientFilter().AND_FILTER();

  public Contracts() {
    setTitle("Договоры");
    storeControls().add(contracts);
    contracts.setSelectionMode(SelectionMode.MULTIPLE);
    contracts.getTable().getColumns().stream().forEach(c -> {
      c.setCellFactory(new Callback() {
        @Override
        public Object call(Object param) {
          return new FXDivisionTableCell() {
            @Override
            protected void updateItem(Object item, boolean empty) {
              super.updateItem(item, empty);
              if(!empty && getRowItem() != null && !getRowItem().isNullOrEmpty("endDate")) {
                LocalDate date = Utility.convert(getRowItem().getDate("endDate")).minusMonths(1);
                setTextFill(date.isBefore(LocalDate.now()) || date.isEqual(LocalDate.now()) ? Color.RED : Color.BLACK);
              }
            }
          };
        }
      });
    });
  }
  
  @Override
  public void changeCFC(Integer[] ids, boolean init) {
    CFCFilter.clear();
    if(ids != null && ids.length > 0)
      CFCFilter.AND_IN("sellerCfc", ids).OR_IN("customerCfc", ids);
    if(init)
      contracts.initData();
  }
  
  @Override
  public void changeCompany(Integer[] ids, boolean init) {
    companyFilter.clear();
    if(ids != null && ids.length > 0)
      companyFilter.AND_IN("sellerCompany", ids).OR_IN("customerCompany", ids);
    if(init)
      contracts.initData();
  }
  
  @Override
  public void changePartition(Integer[] ids, boolean init) {
    partitionFilter.clear();
    if(ids != null && ids.length > 0)
      partitionFilter.AND_IN("sellerCompanyPartition", ids).OR_IN("customerCompanyPartition", ids);
    if(init)
      contracts.initData();
  }

  @Override
  public Node getContent() {
    return contracts;
  }

  @Override
  public void dispose() {
    contracts.dispose();
    super.dispose();
  }

  @Override
  public void start() {
    contracts.initData();
    show("Договоры");
  }
}
