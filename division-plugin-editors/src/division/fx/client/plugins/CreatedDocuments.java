package division.fx.client.plugins;

import division.fx.controller.documents.CreatedDocumentTable;
import division.fx.table.filter.DateFilter;
import division.fx.table.filter.FilterListener;
import division.util.Utility;
import javafx.scene.Node;
import util.filter.local.DBFilter;

public class CreatedDocuments extends FXPlugin {
  private final CreatedDocumentTable createdDocuments = new CreatedDocumentTable() {
    
  private final DBFilter dateFilter = getClientFilter().AND_FILTER();
    @Override
    public void initData() {
      dateFilter.clear();
      ((DateFilter)getColumn("дата").getColumnFilter()).getPeriods().forEach(p -> dateFilter.OR_DATE_BETWEEN("date", Utility.convertToTimestamp(p.getKey()), Utility.convertToTimestamp(p.getValue())));
      super.initData();
    }
  };
  private final DBFilter companyFilter = createdDocuments.getClientFilter().AND_FILTER();
  
  public CreatedDocuments() {
    ((DateFilter)createdDocuments.getColumn("дата").getColumnFilter()).setAllwaysSelected(true);
    ((DateFilter)createdDocuments.getColumn("дата").getColumnFilter()).selectCurrentKvartal();
    ((DateFilter)createdDocuments.getColumn("дата").getColumnFilter()).addFilterListener(FilterListener.create(e -> createdDocuments.initData()));
  }
  
  @Override
  public void changeCompany(Integer[] ids, boolean init) {
    companyFilter.clear();
    if(ids != null && ids.length > 0)
      companyFilter.AND_IN("seller", ids).OR_IN("customer", ids);
    if(init)
      createdDocuments.initData();
  }
  
  @Override
  public void start() {
    show("Документы");
  }
  
  @Override
  public void dispose() {
    createdDocuments.dispose();
    super.dispose();
  }

  @Override
  public Node getContent() {
    return createdDocuments;
  }
}