package division.fx.client.plugins;

import bum.editors.util.DivisionTarget;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Company;
import bum.interfaces.CompanyPartition;
import bum.interfaces.Contract;
import bum.interfaces.ContractProcess;
import bum.interfaces.CreatedDocument;
import bum.interfaces.Deal;
import bum.interfaces.DealPosition;
import bum.interfaces.Equipment;
import bum.interfaces.Product;
import bum.interfaces.Store;
import bum.interfaces.XMLContractTemplate;
import division.fx.DivisionTextField;
import division.fx.PropertyMap;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.controller.company.FXCompany;
import division.editors.contract.ContractSelector;
import division.editors.contract.FXContract;
import division.fx.controller.store.EquipmentFactorTable;
import division.fx.dialog.FXD;
import division.fx.dialog.FXDialog;
import division.fx.editor.FXTableEditor;
import division.fx.table.Column;
import division.fx.table.FXDivisionTable;
import division.fx.table.filter.ListFilter;
import division.fx.util.MsgTrash;
import division.util.Utility;
import division.util.actions.DivisionDatePicker;
import division.util.actions.DocumentCreator;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.util.converter.BigDecimalStringConverter;
import mapping.MappingObject.Type;
import org.apache.commons.lang3.ArrayUtils;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class Orders extends FXPlugin {
  private final MenuItem openOwnerCompany = new MenuItem("Открыть карточку собственника");
  private final MenuItem createProvider = new MenuItem("Задать поставщика");
  private final MenuItem removeProvider = new MenuItem("Удалить поставщика");
  private final MenuItem openProviderContract   = new MenuItem("Открыть договор с поставщиком");
  private final MenuItem openCustomerContract   = new MenuItem("Открыть договор с покупателем");
  private final MenuItem removeZakaz   = new MenuItem("Удалить заказ");
  
  boolean selectActive = true;
  private String sp = "||'}{'||";
  
  private final FXTableEditor zakazTable = new FXTableEditor(Equipment.class, 
          null, 
          FXCollections.observableArrayList(
                  "id",
                  "product=query:(select [DealPosition(product)] from [DealPosition] where [DealPosition(equipment)]=[Equipment(id)] limit 1)",
                  "process=query:(select [DealPosition(process)] from [DealPosition] where [DealPosition(equipment)]=[Equipment(id)] limit 1)",
                  "process-name=query:(select [DealPosition(service_name)] from [DealPosition] where [DealPosition(equipment)]=[Equipment(id)] limit 1)",
                  "owner-company-id=query:(select [CompanyPartition(company)] from [CompanyPartition] where id=[Equipment(owner-id)])",
                  "owner-id",
                  "group",
                  "provider-chain=query:(select array_agg("
                          + "id"
                          + sp
                          
                          + "[DealPosition(deal)]"
                          + sp
                          
                          + "(case when [DealPosition(startDate)] is null then '' else [DealPosition(startDate)]::text end)"
                          + sp
                          
                          + "(case when [DealPosition(dispatchDate)] is null then '' else [DealPosition(dispatchDate)]::text end)"
                          + sp
                          
                          + "[DealPosition(contract_id)]"
                          + sp
                          
                          + "(select [Contract(number)] from [Contract] where id=[DealPosition(contract_id)])"
                          + sp
                          
                          + "[DealPosition(seller_id)]"
                          + sp
                          
                          + "getCompanyName([DealPosition(seller_id)])"
                          + sp
                          
                          + "[DealPosition(seller_partition_id)]"
                          + sp
                          
                          + "[DealPosition(customer_id)]"
                          + sp
                          
                          + "getCompanyName([DealPosition(customer_id)])"
                          + sp
                          
                          + "[DealPosition(customer_partition_id)]"
                          + sp
                          
                          + "[DealPosition(sourceStore)]"

                          + ") from [DealPosition] WHERE [DealPosition(equipment)]=[Equipment(id)])",
                  "store",
                  "company=query:(SELECT [CompanyPartition(company)] from [CompanyPartition] WHERE id=[Equipment(owner-id)])",
                  "iden_id:=:identity_id",
                  "identity_value_name",
                  "factors",
                  "factor_values"
                  ),
          Column.create("id", "id"),
          Column.create("Цель", new ListFilter("Цель")),
          Column.create("Собственник", "owner-name=query:(getCompanyPartitionName([Equipment(owner-id)]))", new ListFilter("owner-name")),
          Column.create("Продавец", new ListFilter("Наименование")),
          Column.create("Покупатель", new ListFilter("Покупатель")),
          Column.create("Поставщик продавца")
                  .addColumn(new Column("Наименование поставщика", null))
                  .addColumn(new Column("Договор №", 
                          //"provider-contract=query:(select [Contract(number)] from [Contract] where id=(select [DealPosition(contract_id)] from [DealPosition] where [DealPosition(id)]=[Equipment(provider)]))",
                          "")),
          Column.create("Наименование ","group_name", new ListFilter("group_name")),
          Column.create("Кол-во","amount")
  ) {
    @Override
    protected void transformItem(PropertyMap zakaz) {
      zakaz.unbindAll();
      ObservableList<PropertyMap> list = FXCollections.observableArrayList();
      if(!zakaz.isNull("provider-chain")) {
        for(String s:zakaz.getValue("provider-chain", String[].class)) {
          String[] ss = s.split("\\}\\{");
          list.add(PropertyMap.create()
                  .setValue("id", Integer.valueOf(ss[0]))
                  .setValue("deal", Integer.valueOf(ss[1]))
                  .setValue("start-date", ss[2].equals("") ? null : Utility.convert(Timestamp.valueOf(ss[2])))
                  .setValue("dispatch-date", ss[3].equals("") ? null : Utility.convert(Timestamp.valueOf(ss[3])))
                  .setValue("contract-id", Integer.valueOf(ss[4]))
                  .setValue("contract-number", ss[5])
                  .setValue("seller-id", Integer.valueOf(ss[6]))
                  .setValue("seller-name", ss[7])
                  .setValue("seller-partition-id", Integer.valueOf(ss[8]))
                  .setValue("customer-id", Integer.valueOf(ss[9]))
                  .setValue("customer-name", ss[10])
                  .setValue("customer-partition-id", Integer.valueOf(ss[11]))
                  .setValue("source-store-id", Integer.valueOf(ss[12]))
          );
        }
      }
        
      if(zakaz.getValue("factor_values") != null) {
        for(String factorValue:(String[])zakaz.getValue("factor_values")) {
          String[] fv = factorValue.split(":");
          zakaz.setValue("factor-"+fv[0], fv.length > 1 ? fv[1] : null);
        }
      }
      list.sort((PropertyMap o1, PropertyMap o2) -> o1.getInteger("seller-partition-id") == zakaz.getInteger("owner-id") || o1.getInteger("customer-partition-id") == o2.getInteger("seller-partition-id") ? -1 : 1);
      /*ObservableList<PropertyMap> sortList = FXCollections.observableArrayList(list.remove(0));
      while(!list.isEmpty()) {
        Integer nextSeller = sortList.get(sortList.size()-1).getInteger("customer-partition-id");
        for(int i=list.size()-1;i>=0;i--) {
          if(list.get(i).getInteger("seller-partition-id") == nextSeller) {
            sortList.add(list.remove(i));
            break;
          }
        }
      }*/

      zakaz.setValue("Продавец"                        , list.isEmpty()  ? ""   : list.get(list.size()-1).getString("seller-name"))
              .setValue("Покупатель"                   , list.isEmpty()  ? ""   : list.get(list.size()-1).getString("customer-name"))
              .setValue("customer-id"                  , list.isEmpty()  ? null : list.get(list.size()-1).getInteger("customer-id"))
              //.setValue("Наименование поставщика", Utility.join(PropertyMap.getListFromList(list, "seller-name", String.class).toArray(), " -> "))
              .setValue("Наименование поставщика"      , list.size() < 2 ? ""   : list.get(list.size()-2).getString("seller-name"))
              //.setValue("Договор №", Utility.join(PropertyMap.getListFromList(list, "contract-number", String.class).toArray(), " -> "))
              .setValue("Договор №"                    , list.size() < 2 ? ""   : list.get(list.size()-2).getString("contract-number"))
              .bind("Цель"                             , Bindings.createStringBinding(() -> zakaz.isNullOrEmpty("customer-id") ? "Товарный запас" : "Под реализацию", zakaz.get("Покупатель")))

              .setValue("provider-id"                  , list.size() < 2 ? null : list.get(list.size()-2).getInteger("seller-id"))
              .setValue("provider-partition-id"        , list.size() < 2 ? null : list.get(list.size()-2).getInteger("seller-partition-id"))

              .setValue("seller-id"                    , list.isEmpty()  ? null : list.get(list.size()-1).getInteger("seller-id"))
              .setValue("seller-partition-id"          , list.isEmpty()  ? null : list.get(list.size()-1).getInteger("seller-partition-id"))
              .setValue("seller-store-id"              , list.isEmpty()  ? null : list.get(list.size()-1).getInteger("source-store-id"))

              .setValue("customer-id"                  , list.isEmpty()  ? null : list.get(list.size()-1).getInteger("customer-id"))
              .setValue("customer-partition-id"        , list.isEmpty()  ? null : list.get(list.size()-1).getInteger("customer-partition-id"))
              
              .setValue("customer-contract-id"         , list.isEmpty()  ? null : list.get(list.size()-1).getInteger("contract-id"))
              .setValue("customer-deal-id"             , list.isEmpty()  ? null : list.get(list.size()-1).getInteger("deal"))
              .setValue("customer-deal-position-id"    , list.isEmpty()  ? null : list.get(list.size()-1).getInteger("id"))

              .setValue("provider-contract-id"         , list.size() < 2 ? null : list.get(list.size()-2).getInteger("contract-id"))
              .setValue("provider-deal-id"             , list.size() < 2 ? null : list.get(list.size()-2).getInteger("deal"))
              .setValue("provider-deal-position-id"    , list.size() < 2 ? null : list.get(list.size()-2).getInteger("id"));
      
      zakaz.setValue("provider-chain", list);
    }
  };
  
  private final EquipmentFactorTable equipmentFactorTable = new EquipmentFactorTable();
  private final SplitPane split = new SplitPane(zakazTable.getTable(), equipmentFactorTable);

  @Override
  public void changeCompany(Integer[] company, boolean forceInit) {
    super.changeCompany(company, forceInit);
    zakazTable.getClientFilter().clear().AND_EQUAL("zakaz", true);
    if(company != null && company.length > 0)
      zakazTable.getClientFilter().AND_IN("owner-id", PropertyMap.getListFromList(ObjectLoader.getList(DBFilter.create(CompanyPartition.class).AND_IN("company", company), "id"), "id", Integer.TYPE).toArray(new Integer[0]));
    if(forceInit)
      zakazTable.initData();
  }
  
  @Override
  public void start() {
    DivisionTarget.create(this, Deal.class, (DivisionTarget target, String type, Integer[] ids, PropertyMap objectEventProperty) -> {
      if(objectEventProperty != null && !objectEventProperty.isNullOrEmpty("class") && objectEventProperty.getValue("class").equals(Orders.class.getName()))
        zakazTable.initData(objectEventProperty.getInteger("equipment"));
    });
    
    DivisionTarget.create(this, DealPosition.class, (DivisionTarget target, String type, Integer[] ids, PropertyMap objectEventProperty) -> {
      if(objectEventProperty != null && !objectEventProperty.isNullOrEmpty("class") && objectEventProperty.getValue("class").equals(Orders.class.getName()))
        zakazTable.initData(objectEventProperty.getInteger("equipment"));
    });
    
    DivisionTarget.create(this, Equipment.class, (DivisionTarget target, String type, Integer[] ids, PropertyMap data) -> {
      switch(type) {
        case "REMOVE":
          zakazTable.getTable().getSourceItems().filtered(z -> ArrayUtils.contains(ids, z.getInteger("id"))).forEach(z -> zakazTable.getTable().getSourceItems().remove(z));
          break;
        case "CREATE":
          Integer[] filterIds = ObjectLoader.isSatisfy(zakazTable.getClientFilter(), ids);
          zakazTable.getTable().getSourceItems().filtered(z -> 
                  ArrayUtils.contains(ids, z.getInteger("id")) && !ArrayUtils.contains(filterIds, z.getInteger("id")))
                  .forEach(z -> zakazTable.getTable().getSourceItems().remove(z));
          break;
      }
    });
    
    FXDivisionTable.setGeneralItems(zakazTable.getTable(), equipmentFactorTable.getTable());
    FXDivisionTable.bindScrollBars(Orientation.VERTICAL, zakazTable.getTable(), equipmentFactorTable.getTable());
    
    zakazTable.setSelectionMode(SelectionMode.MULTIPLE);
    equipmentFactorTable.getTable().getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    equipmentFactorTable.getTable().getSelectionModel().setCellSelectionEnabled(true);
    
    zakazTable.getTable().setHorizontScrollBarPolicyAlways();
    equipmentFactorTable.getTable().setHorizontScrollBarPolicyAlways();
    
    equipmentFactorTable.setRootColumnVisible(true);
    equipmentFactorTable.setIndentivicatorsVisible(false);
    equipmentFactorTable.setColumnVisible(false, "Наименование", "Кол.");
    
    zakazTable.getTable().getSelectionModel().getSelectedIndices().addListener((ListChangeListener.Change<? extends Integer> c) -> {
      if(selectActive) {
        selectActive = false;
        equipmentFactorTable.getTable().getSelectionModel().clearSelection();
        for(Object index:zakazTable.getTable().getSelectionModel().getSelectedIndices())
          equipmentFactorTable.getTable().getSelectionModel().select((Integer)index);
        selectActive = true;
      }
    });
    
    zakazTable.getTable().getSourceItems().addListener((ListChangeListener.Change<? extends PropertyMap> c) -> Platform.runLater(() -> equipmentFactorTable.initHeader()));
    zakazTable.getTable().getSelectionModel().setCellSelectionEnabled(true);
    zakazTable.getTable().setContextMenu(new ContextMenu(openOwnerCompany, new SeparatorMenuItem(), removeZakaz, openCustomerContract, new SeparatorMenuItem(), createProvider, removeProvider, openProviderContract));
    
    zakazTable.getTable().getContextMenu().setOnShowing(e -> {
      PropertyMap z = zakazTable.getTable().getSelectionModel().getSelectedItem();
      removeProvider.setDisable(z == null || z.isNullOrEmpty("provider-id"));
      openProviderContract.setDisable(z == null || z.isNullOrEmpty("provider-contract-id"));
      openCustomerContract.setDisable(z == null || z.isNullOrEmpty("customer-contract-id"));
    });
    
    createProvider.disableProperty().bind(zakazTable.getTable().getSelectionModel().selectedItemProperty().isNotNull().and(removeProvider.disableProperty().not()));
    
    openProviderContract.setOnAction(e -> openContract(zakazTable.getTable().getSelectionModel().getSelectedItem().getInteger("provider-contract-id")));
    openCustomerContract.setOnAction(e -> openContract(zakazTable.getTable().getSelectionModel().getSelectedItem().getInteger("customer-contract-id")));
    openOwnerCompany.setOnAction(e -> {
      FXCompany fxc = new FXCompany();
      fxc.setObjectProperty(ObjectLoader.getMap(Company.class, zakazTable.getTable().getSelectionModel().getSelectedItem().getInteger("owner-company-id")));
      fxc.showAndWait(getContent());
    });
    
    removeZakaz.setOnAction(e -> removeZakaz());
    removeProvider.setOnAction(e -> removeProvider());
    createProvider.setOnAction(e -> addProvider());
    
    /*zakazTable.getTable().setOnMouseClicked(e -> {
      if(e.getClickCount() == 2 && !zakazTable.getTable().getSelectionModel().getSelectedCells().isEmpty() && !zakazTable.getSelectedObjects().isEmpty()) {
        TableColumn column = zakazTable.getTable().getSelectionModel().getSelectedCells().get(0).getTableColumn();
        if(column.getParentColumn() != null) {
          if(column.getParentColumn().equals(zakazTable.getColumn("Поставщик продавца")))
            addProvider();
          if(column.getParentColumn().equals(zakazTable.getColumn("Продавец"))) {
            FXCompany company = new FXCompany();
            company.setObjectProperty(ObjectLoader.getMap(Company.class, zakazTable.getSelectedObjects().get(0).getInteger("company")));
            company.showAndWait(this.getContent().getScene().getWindow());
          }
        }
      }
    });*/
    
    zakazTable.getClientFilter().clear().AND_EQUAL("zakaz", true);
    zakazTable.initData();
    show("Заказы");
    
    disposeList().add(zakazTable);
    
  }
  
  private void openContract(Integer contractid) {
    FXContract fxc = new FXContract();
    fxc.setObjectProperty(ObjectLoader.getMap(Contract.class, contractid));
    fxc.showAndWait(getContent());
  }

  @Override
  public Node getContent() {
    return split;
  }
  
  private void removeZakaz() {
    PropertyMap z = zakazTable.getTable().getSelectionModel().getSelectedItem();
    Label l = new Label("Удалить заказ на \""+z.getString("group_name")+"\" в количестве "+z.getBigDecimal("amount")+"?");
    l.setWrapText(true);
    l.setPrefWidth(300);
    if(FXDialog.show(getContent(), new VBox(10, l), "", FXDialog.ButtonGroup.YES_NO) == FXDialog.Type.YES) {
      RemoteSession session = null;
      try {
        Integer[] dps = null;
        Integer[] deals = null;
        session = ObjectLoader.createSession();
        for(List dp:session.getData(DBFilter.create(DealPosition.class).AND_EQUAL("equipment", z.getInteger("id")), "id","deal","query:(select count(a.id) from [DealPosition] a where a.[!DealPosition(deal)]=[DealPosition(deal)])")) {
          if(((Long)dp.get(2)) == 1)
            deals = ArrayUtils.add(deals, (Integer)dp.get(1));
          else dps = ArrayUtils.add(dps, (Integer)dp.get(0));
        }
        if(dps != null)
          session.removeObjects(DealPosition.class, dps);
        if(deals != null)
          session.removeObjects(Deal.class, deals);
        session.removeObjects(Equipment.class, z.getInteger("id"));
        ObjectLoader.commitSession(session);
      }catch(Exception ex) {
        ObjectLoader.rollBackSession(session);
        MsgTrash.out(ex);
      }
    }
  }
  
  private void removeProvider() {
    PropertyMap z = zakazTable.getTable().getSelectionModel().getSelectedItem();
    Label l = new Label("Удалить заказ на \""+z.getString("group_name")+"\" в количестве "+z.getBigDecimal("amount")+"?");
    l.setWrapText(true);
    l.setPrefWidth(300);
    if(FXDialog.show(getContent(), l, "", FXDialog.ButtonGroup.YES_NO) == FXDialog.Type.YES) {
      RemoteSession session = null;
      try {
        session = ObjectLoader.createSession();
        if(z.getList("provider-chain").size() > 2) {
          if(FXDialog.show(getContent(), new Label("Удалить всю цепочку поставщиков?"), "", FXDialog.ButtonGroup.YES_NO) == FXDialog.Type.YES) {
          }
        }else {
          ObservableList<PropertyMap> dps = PropertyMap.createList(session.getList(DBFilter.create(DealPosition.class).AND_EQUAL("deal", z.getInteger("provider-deal-id")), "id"));
          if(dps.size() == 1)
            session.removeObjects(Deal.class, PropertyMap.create()
                    .setValue("class", getClass().getName())
                    .setValue("type", "remove-provider")
                    .setValue("equipment", z.getInteger("id")).getSimpleMap(), 
                    z.getInteger("provider-deal-id"));
          else {
            session.removeObjects(DealPosition.class, PropertyMap.create()
                    .setValue("class", getClass().getName())
                    .setValue("type", "remove-provider")
                    .setValue("equipment", z.getInteger("id")).getSimpleMap(), 
                    z.getInteger("provider-deal-position-id"));
            Integer[] documents = new Integer[0];
            for(List list:session.executeQuery("SELECT [CreatedDocument(dealPositions):object] FROM [CreatedDocument(dealPositions):table] WHERE [CreatedDocument(dealPositions):target]=ANY(?)", 
                    new Object[]{PropertyMap.getListFromList(dps, "id", Integer.TYPE).toArray(new Integer[0])}))
              documents = ArrayUtils.add(documents, (Integer)list.get(0));
            
            for(PropertyMap doc:PropertyMap.createList(session.getList(DBFilter.create(CreatedDocument.class).AND_IN("id", documents), "id","document_name","document_script","document-scriptLanguage","dealPositions"))) {
              DocumentCreator.runDocumentScript(
                      doc.getInteger("id"), 
                      doc.getString("name"), 
                      doc.getString("document_script"), 
                      doc.getString("document-scriptLanguage"), 
                      doc.getValue("dealPositions", Integer[].class), 
                      session, 
                      null);
            }
          }
        }
        // Вернуть заказ на склад продавца seller-store-id
        session.executeUpdate(Equipment.class, "store", z.getInteger("seller-store-id"), z.getInteger("id"));
        ObjectLoader.commitSession(session);
      }catch(Exception ex) {
        ObjectLoader.rollBackSession(session);
      }
    }
  }
  
  /**
   * Создать продукт в каталоге поставщика - если его там нет
   * Создать продукт в общем каталоге - если его там нет
   * Использовать продукт из общего каталога
   * Использовать продукт из архива
   * Использовать продукт как есть и отправить в архив.
   * 
   * 1. Проверить наличие продукта в каталоге продавца
   */
  
  private void addProvider() {
    PropertyMap zakaz = zakazTable.getSelectedObjects().get(0);
    List<PropertyMap> contracts = ContractSelector.getContract(getContent(), 
            SelectionMode.SINGLE, 
            DBFilter.create(Contract.class)
                    .AND_EQUAL("customerCompanyPartition", zakaz.getInteger("owner-id")));
    if(!contracts.isEmpty())
      zakazTable.getSelectedObjects().stream().forEach(z -> {
        creatProviderDeal(contracts.get(0),z);
      });
  }
  
  public static PropertyMap getProduct(Node parent, Integer seller, Integer process, Integer group) {
    //Ищем продукт в каталоге поставщика, в общем каталоге и в архиве
    DBFilter filter = DBFilter.create(Product.class)
            .AND_EQUAL("service", process)
            .AND_EQUAL("group", group)
            .AND_EQUAL("tmp", false);
    filter.AND_FILTER().AND_EQUAL("company", seller).OR_EQUAL("company", null);
    ObservableList<PropertyMap> products = ObjectLoader.getList(filter, "id","duration","recurrence","group_name","company","type");
    
    ObservableList<PropertyMap> productsInOwnerCatalog   = products.filtered(p -> !p.isNull("company") && p.getValue("type", Type.class) == Type.CURRENT);
    ObservableList<PropertyMap> productsInGeneralCatalog = products.filtered(p -> p.isNull("company") && p.getValue("type", Type.class) == Type.CURRENT);
    ObservableList<PropertyMap> productsInArchiveCatalog = products.filtered(p -> p.getValue("type", Type.class) == Type.ARCHIVE);
    
    if(!productsInOwnerCatalog.isEmpty())
      return productsInOwnerCatalog.get(0);
    
    RadioButton createInOwnerCatalog   = new RadioButton("Создать продукт в каталоге продавца");
    RadioButton createInGeneralCatalog = new RadioButton("Создать продукт в общем каталоге");
    RadioButton createInArchiveCatalog = new RadioButton("Создать продукт в архиве");
    RadioButton useGeneralCatalog      = new RadioButton("Использовать продукт из общего каталога");
    RadioButton useArchiveCatalog      = new RadioButton("Использовать продукт из архива");
    
    createInOwnerCatalog.setDisable(!productsInOwnerCatalog.isEmpty());
    createInGeneralCatalog.setDisable(!productsInGeneralCatalog.isEmpty());
    createInArchiveCatalog.setDisable(!productsInArchiveCatalog.isEmpty());
    useGeneralCatalog.setDisable(productsInGeneralCatalog.isEmpty());
    useArchiveCatalog.setDisable(productsInArchiveCatalog.isEmpty());
    
    if(!createInOwnerCatalog.isDisable())
      createInOwnerCatalog.setSelected(true);
    else if(!useGeneralCatalog.isDisable())
      useGeneralCatalog.setSelected(true);
    else if(!createInGeneralCatalog.isDisable())
      createInGeneralCatalog.setSelected(true);
    else if(!useArchiveCatalog.isDisable())
      useArchiveCatalog.setSelected(true);
    else if(!createInArchiveCatalog.isDisable())
      createInArchiveCatalog.setSelected(true);
    
    ToggleGroup gr = new ToggleGroup();
    gr.getToggles().addAll(createInOwnerCatalog, createInGeneralCatalog, createInArchiveCatalog, useGeneralCatalog, useArchiveCatalog);
    VBox pane = new VBox(5, gr.getToggles().toArray(new Node[0]));
    
    String msg = "Продукт не найден";
    if(!productsInGeneralCatalog.isEmpty() && !productsInArchiveCatalog.isEmpty())
      msg += " в каталоге продавца";
    else {
      msg += " ни в каталоге продавца";
      if(productsInGeneralCatalog.isEmpty())
        msg += " ни в общем каталоге";
      if(productsInArchiveCatalog.isEmpty())
        msg += " ни в архиве";
    }
    
    msg += "\nВыберите один из вариантов действий:\n\n";
    pane.getChildren().add(0, new Label(msg));
    
    if(FXD.showWait("Внимание!!!", parent, pane, Modality.APPLICATION_MODAL, StageStyle.DECORATED, false, FXD.ButtonType.OK, FXD.ButtonType.CANCEL).get() == FXD.ButtonType.OK) {
      if(useArchiveCatalog.isSelected())
        return productsInArchiveCatalog.get(0);
      if(useGeneralCatalog.isSelected())
        return productsInGeneralCatalog.get(0);
      if(createInOwnerCatalog.isSelected())
        return PropertyMap.create().setValue("service", process).setValue("duration", Period.ofDays(10)).setValue("group", group).setValue("company", seller).setValue("type", Type.CURRENT);
      if(createInGeneralCatalog.isSelected())
        return PropertyMap.create().setValue("service", process).setValue("duration", Period.ofDays(10)).setValue("group", group).setValue("company", null).setValue("type", Type.CURRENT);
      if(createInArchiveCatalog.isSelected())
        return PropertyMap.create().setValue("service", process).setValue("duration", Period.ofDays(10)).setValue("group", group).setValue("company", seller).setValue("type", Type.ARCHIVE);
    }
    
    return null;
  }
  
  private void creatProviderDeal(PropertyMap contract, PropertyMap zakaz) {
    TitleBorderPane processProvider = new TitleBorderPane(new Label("Парамметры сделки с поставщиком"));
    DivisionDatePicker startDate = new DivisionDatePicker(LocalDate.now());
    DivisionDatePicker endDate = new DivisionDatePicker(LocalDate.now());
    endDate.setEditable(false);
    startDate.setEditable(false);
    
    contract = ObjectLoader.getMap(Contract.class, contract.getInteger("id"));
    if(!zakaz.isPropertyMap("store"))
      zakaz.setValue("store", ObjectLoader.getMap(Store.class, zakaz.getInteger("store")));
    //получить продукт
    if(!zakaz.isPropertyMap("product"))
      zakaz.setValue("product", ObjectLoader.getMap(Product.class, zakaz.getInteger("product")));
    
    //Найти склад поставщика идентичный исходному и без контроля
    ObservableList<PropertyMap> stories = ObjectLoader.getList(DBFilter.create(Store.class)
            .AND_EQUAL("companyPartition", contract.getInteger("sellerCompanyPartition"))
            .AND_EQUAL("objectType", zakaz.getMap("store").getValue("objectType"))
            .AND_EQUAL("storeType", zakaz.getMap("store").getValue("storeType"))
            .AND_EQUAL("tmp", false)
            .AND_EQUAL("type", Store.Type.CURRENT)
            .AND_EQUAL("controllOut", false).AND_EQUAL("controllIn", false));

    if(stories.isEmpty())
      MsgTrash.out(new Exception("Выбор данного поставщика невозможен, так как у него нет соответствующего склада без контроля отстатков"));
    else {
      /*System.out.println("pr = "+(pr == null ? pr : pr.toJson()));
      
      DBFilter filter = DBFilter.create(Product.class)
              .AND_EQUAL("service", zakaz.getInteger("process"))
              .AND_EQUAL("group", zakaz.getInteger("group"))
              .AND_EQUAL("tmp", false)
              .AND_EQUAL("type", Product.Type.CURRENT);
      filter.AND_FILTER().AND_EQUAL("company", contract.getInteger("sellerCompany")).OR_EQUAL("company", null);*/
      
      //Создаём позицию сделки поставщика
      PropertyMap providerDealPosition = PropertyMap.create();
      
      // Получаем продукт
      PropertyMap product = getProduct(getContent(), contract.getInteger("sellerCompany"), zakaz.getInteger("process"), zakaz.getInteger("group"));
      if(product != null) {
        
        if(product.isNull("id"))
          product.setValue("id", ObjectLoader.createObject(Product.class, product));
        
        providerDealPosition.setValue("product", product.getInteger("id"))
                .setValue("duration", product.getValue("duration"))
                .setValue("recurrence", product.getValue("recurrence"));

        if(!providerDealPosition.isNullOrEmpty("product")) {//Если продукт найден или создан

          contract.setValue("sellerCompany", ObjectLoader.getMap(Company.class, contract.getInteger("sellerCompany"), "id","name"));
          contract.setValue("template", ObjectLoader.getMap(XMLContractTemplate.class, contract.getInteger("template"), "id","name"));

          Label processLabel = new Label(zakaz.getString("process-name"));
          processLabel.getStyleClass().add("link-label");
          DivisionTextField countText = new DivisionTextField(new BigDecimalStringConverter(), zakaz.getBigDecimal("amount"));

          GridPane pane = new GridPane();
          pane.setHgap(10);
          pane.setVgap(10);
          pane.getColumnConstraints().addAll(new ColumnConstraints(150,150,150), new ColumnConstraints(150,150,150));
          pane.addRow(0, new Label("Процесс:"), processLabel);
          pane.addRow(1, new Label("Объект:"), new Label(zakaz.getString("group_name")));
          pane.addRow(2, new Label("Количество:"), countText);
          pane.addRow(3, new Label("Поставщик:"), new Label(contract.getMap("sellerCompany").getString("name")));
          pane.addRow(4, new Label("Договор:"), new Label(contract.getMap("template").getString("name")+" №"+contract.getString("number")));
          pane.addRow(5, new Label("старт сделки:"), startDate);
          Period p = providerDealPosition.getValue("duration", Period.class);
          endDate.setValue(startDate.getValue().plusYears(p.getYears()).plusMonths(p.getMonths()).plusDays(p.getDays()));
          pane.addRow(6, new Label("окончание сделки:"), endDate);

          processProvider.setCenter(pane);

          if(FXDialog.show(getContent(), processProvider, "", FXDialog.ButtonGroup.OK_CANCEL, false) == FXDialog.Type.OK) {


            RemoteSession session = null;
            try {
              session = ObjectLoader.createSession();
              //Ищем ContractProcess для сделки
              ObservableList<PropertyMap> contractprocesses = PropertyMap.createList(session.getList(DBFilter.create(ContractProcess.class)
                      .AND_EQUAL("contract", contract.getInteger("id"))
                      .AND_EQUAL("customerPartition", contract.getInteger("customerCompanyPartition"))
                      .AND_EQUAL("process", zakaz.getInteger("process")), "id"));
              if(!contractprocesses.isEmpty()) { //Если есть, то проверяем на наличие пересекающихся сделок
                Integer[] ids = null;
                for(PropertyMap cp:contractprocesses)
                  ids = ArrayUtils.add(ids, cp.getInteger("id"));
                DBFilter filter = DBFilter.create(Deal.class).AND_IN("tempProcess", ids);
                DBFilter dateFilter = filter.AND_FILTER();
                dateFilter
                        .AND_DATE_AFTER_OR_EQUAL("dealStartDate", Utility.convert(startDate.getValue())).AND_DATE_BEFORE_OR_EQUAL("dealEndDate", Utility.convert(endDate.getValue()))
                        .OR_DATE_AFTER_OR_EQUAL("dealStartDate", Utility.convert(startDate.getValue())).AND_DATE_BEFORE_OR_EQUAL("dealStartDate", Utility.convert(endDate.getValue()))
                        .OR_DATE_AFTER_OR_EQUAL("dealEndDate", Utility.convert(startDate.getValue())).AND_DATE_BEFORE_OR_EQUAL("dealEndDate", Utility.convert(endDate.getValue()));
                ObservableList<PropertyMap> deals = PropertyMap.createList(session.getList(dateFilter, "id","tempProcess"));

                /*if(!deals.isEmpty()) {
                  if(new Alert(Alert.AlertType.CONFIRMATION, "Использовать существующие сделки?", ButtonType.YES, ButtonType.NO).showAndWait().get() == ButtonType.YES) {
                  }
                }*/

                contractprocesses = FXCollections.observableArrayList(contractprocesses.filtered(cp -> !PropertyMap.getListFromList(deals, "tempProcess", Integer.TYPE).contains(cp.getInteger("id"))));
              }

              if(contractprocesses.isEmpty()) { //Если нет ContractProcess без пересекающихся сделок, то создаём ContractProcess в договоре с поставщиком
                contractprocesses.add(PropertyMap.create()
                        .setValue("contract", contract.getInteger("id"))
                        .setValue("customerPartition", contract.getInteger("customerCompanyPartition"))
                        .setValue("process", zakaz.getInteger("process")));
                contractprocesses.get(0).setValue("id", session.createObject(ContractProcess.class, contractprocesses.get(0).getSimpleMap()));
              }

              //Создаём сделку с поставщиком
              PropertyMap providerDeal = PropertyMap.copy(contract, "customerCompany","sellerCompany","customerCfc","sellerCfc","customerCompanyPartition","sellerCompanyPartition")
                      .setValue("dealStartDate", startDate.getValue())
                      .setValue("dealEndDate", endDate.getValue())
                      .setValue("contract", contract)
                      .setValue("tempProcess", contractprocesses.get(0))
                      .setValue("service", zakaz.getValue("process"))
                      .setValue("duration", zakaz.getMap("product").getValue("duration"))
                      .setValue("recurrence", zakaz.getMap("product").getValue("recurrence"));
              providerDeal.setValue("id", session.createObject(Deal.class, providerDeal.getSimpleMap()));

              //Создать позицию склада у поставщика
              session.saveObject(Equipment.class, zakaz.setValue("store", stories.get(0)).getSimpleMap());

              providerDealPosition.setValue("id", 
                      session.createObject(DealPosition.class, 
                              providerDealPosition.setValue("equipment", zakaz).setValue("amount", zakaz.getBigDecimal("amount")).setValue("deal", providerDeal).getSimpleMap(),
                              PropertyMap.create().setValue("class", getClass().getName())
                                      .setValue("type", "create-provider")
                                      .setValue("equipment", zakaz.getInteger("id")).getSimpleMap()));
              ObjectLoader.commitSession(session);
            }catch(Exception ex) {
              ObjectLoader.rollBackSession(session);
              MsgTrash.out(ex);
            }
          }
        }
      }
    }
  }
}