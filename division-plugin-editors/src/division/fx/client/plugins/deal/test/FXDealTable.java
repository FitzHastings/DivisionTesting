package division.fx.client.plugins.deal.test;

import bum.editors.util.DivisionTarget;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Contract;
import bum.interfaces.Deal;
import bum.interfaces.Service;
import division.fx.FXToolButton;
import division.fx.FXUtility;
import division.fx.PropertyMap;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.client.plugins.FXPlugin;
import division.fx.client.plugins.deal.DealPositionTable;
import division.fx.client.plugins.deal.TreeFilter;
import division.fx.client.plugins.deal.master.FXDealMaster;
import division.fx.controller.deal.FXDealActionDocuments;
import division.fx.dialog.FXD;
import division.fx.scale.test.ScalePeriod;
import division.fx.scale.test.ScaleRow;
import division.fx.scale.test.ScaleTable;
import division.fx.table.Column;
import division.fx.table.FXDivisionTable;
import division.fx.table.filter.FilterListener;
import division.fx.table.filter.TextFilter;
import division.fx.util.MsgTrash;
import division.util.DivisionTask;
import division.util.Hronometr;
import division.util.Utility;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import mapping.MappingObject;
import org.apache.commons.lang3.ArrayUtils;
import util.filter.local.DBFilter;

public class FXDealTable extends FXPlugin {
  private TreeFilter prFilter = new TreeFilter(Service.class, "service");
  private TextFilter cpFilter = new TextFilter("customerCompanyName");
  
  private FXDivisionTable<ScaleRow<ScalePeriod>> leftTable = new FXDivisionTable<>(Column.create("Договор", "contract_number"),Column.create("Процесс","service_name", prFilter));
  private ScaleTable<ScaleRow<ScalePeriod>, ScalePeriod> scale = new ScaleTable(ScaleRow.class,ScalePeriod.class);
  private FXDivisionTable<ScaleRow<ScalePeriod>> rightTable = new FXDivisionTable<>(Column.create("Контрагент", cpFilter));
  private SplitPane scaleSplit = new SplitPane(leftTable, scale, rightTable);
  
  private final ToggleButton greenFilterButton  = new ToggleButton();
  private final ToggleButton blueFilterButton   = new ToggleButton();
  private final ToggleButton redFilterButton    = new ToggleButton();
  private final ToggleButton yellowFilterButton = new ToggleButton();
  private final ToggleButton grayFilterButton   = new ToggleButton();
  private final HBox statusFilter = new HBox(10, greenFilterButton, blueFilterButton, redFilterButton, yellowFilterButton, grayFilterButton);
  
  private FXDealActionDocuments actions           = new FXDealActionDocuments();
  private DealPositionTable     dealPositionTable = new DealPositionTable();
  
  private SplitPane actionSplit = new SplitPane(dealPositionTable, actions);
  private FXCommentTable commentTable = new FXCommentTable(Deal.class);
  private TabPane tabPane = new TabPane(new Tab("Сделки", actionSplit), new Tab("Комментарии", commentTable));
  
  private SplitPane             rootSplit         = new SplitPane(scaleSplit, tabPane);
  
  private DBFilter rootFilter     = DBFilter.create(Deal.class);
  private DBFilter contractFilter = rootFilter.AND_FILTER();
  private DBFilter companyFilter  = rootFilter.AND_FILTER();
  private DBFilter cfcFilter      = rootFilter.AND_FILTER();
  private DBFilter dateFilter     = rootFilter.AND_FILTER();
  private DBFilter tmpTypeFilter  = rootFilter.AND_FILTER().AND_EQUAL("tmp", false).AND_EQUAL("type", Deal.Type.CURRENT);
  
  private ObservableList<ScaleRow<ScalePeriod>> items = FXCollections.observableArrayList();
  
  private final ObjectProperty<FilteredList<ScaleRow<ScalePeriod>>> filterItemsProperty = new SimpleObjectProperty<>(new FilteredList(items));
  
  private ObservableList<String> fields = FXCollections.observableArrayList(
                "id",
                
                "service",
                "service_name",
                
                "contract",
                "contract_number",
                "tempProcess",
                
                "dealStartDate",
                "dealEndDate",
                
                "customerCompany",
                "customerCompanyPartition",
                
                "sellerCompany",
                "sellerCompanyPartition",
                
                "cost",
                "startCount",
                "dispatchCount",
                "dispatchAmount",
                "paymentCount",
                "paymentAmount",
                
                "tmp",
                "type",
                
                "deal-comments=query:select array_agg([DealComment(comment)]||'[::]'||[DealComment(avtor)]) from [DealComment] where [DealComment(deal)]=[Deal(id)]",
                
                "customerCompanyName=query:(SELECT (SELECT name FROM [OwnershipType] WHERE id=[Company(ownershipType)])||' '||name FROM [Company] WHERE [Company(id)]=[Deal(customerCompany)])",
                "sellerCompanyName=query:(SELECT (SELECT name FROM [OwnershipType] WHERE id=[Company(ownershipType)])||' '||name FROM [Company] WHERE [Company(id)]=[Deal(sellerCompany)])",
                
                "sort:contract,tempProcess");
  private DivisionTarget dealTarget;
  
  private final FXToolButton addToolButton = new FXToolButton(e -> createDeal(), "Создать сделку", "add-button");
  private final FXToolButton delToolButton = new FXToolButton(e -> removeDeal(), "Удалить сделку", "remove-button");
  
  private final FXToolButton copyToolButton  = new FXToolButton("Копировать сделку", "copy-button");
  private final FXToolButton pasteToolButton = new FXToolButton("Вставить сделку", "paste-button");
  
  private final TitleBorderPane root = new TitleBorderPane(rootSplit, addToolButton, delToolButton, new Separator(), copyToolButton, pasteToolButton, new Separator(), statusFilter, new Separator());
  
  public FXDealTable() {
    
    FXUtility.initMainCss(this);
    prFilter.addFilterListener(FilterListener.create(e -> fireFilter()));
    cpFilter.addFilterListener(FilterListener.create(e -> fireFilter()));

    tabPane.getTabs().forEach(t -> t.setClosable(false));
    
    dealTarget = DivisionTarget.create(Deal.class, (DivisionTarget target, String type, Integer[] ids, PropertyMap data) -> {
      
      List<ScalePeriod> periods = scale.getRows().stream().flatMap(r -> r.getPeriods().stream()).filter(sp -> ArrayUtils.contains(ids, sp.getInteger("id"))).collect(Collectors.toList());
      Integer[] fids = ArrayUtils.removeElements(ids, periods.stream().map(p -> p.getInteger("id")).collect(Collectors.toList()).toArray(new Integer[0]));
      if(fids.length > 0)
        fids = ObjectLoader.isSatisfy(rootFilter, fids);
      
      if(fids.length > 0 || !periods.isEmpty()) {
        List<PropertyMap> deals = ObjectLoader.getList(Deal.class, ArrayUtils.addAll(fids, periods.stream().map(p -> p.getInteger("id")).collect(Collectors.toList()).toArray(new Integer[0])), fields.toArray(new String[0]));
        switch(type) {
          case "CREATE":
            createEvent(deals);
            break;
          case "UPDATE":
            updateEvent(deals);
            break;
          case "REMOVE":break;
        }
      }
    });
    
    disposeList().addAll(actions, rightTable, leftTable, dealPositionTable, scale, dealTarget);
    
    VBox.setVgrow(scaleSplit, Priority.ALWAYS);
    
    leftTable.setHorizontScrollBarPolicyAlways();
    rightTable.setHorizontScrollBarPolicyAlways();
    
    greenFilterButton.getStyleClass().addAll("finish-status-button","status-button");
    blueFilterButton.getStyleClass().addAll("payment-status-button","status-button");
    redFilterButton.getStyleClass().addAll("dispatch-status-button","status-button");
    yellowFilterButton.getStyleClass().addAll("start-status-button","status-button");
    grayFilterButton.getStyleClass().addAll("project-status-button","status-button");
    
    greenFilterButton.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)  -> fireDealStatusFilter());
    blueFilterButton.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)   -> fireDealStatusFilter());
    redFilterButton.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)    -> fireDealStatusFilter());
    yellowFilterButton.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> fireDealStatusFilter());
    grayFilterButton.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)   -> fireDealStatusFilter());
    
    scale.scrollToCurrentDate();
    
    scale.itemsProperty().bind(filterItemsProperty);
    leftTable.itemsProperty().bind(filterItemsProperty);
    rightTable.itemsProperty().bind(filterItemsProperty);
    
    rootSplit.setOrientation(Orientation.VERTICAL);
    actionSplit.setOrientation(Orientation.VERTICAL);
    
    ((TableColumn<PropertyMap,Object>)rightTable.getColumn("Контрагент")).setCellValueFactory((TableColumn.CellDataFeatures<PropertyMap, Object> param) -> {
      if(!companyFilter.isEmpty()) {
        List<Integer> ids = Arrays.asList((Integer[])companyFilter.get(0).getValues()[0]);
        if(ids.contains(param.getValue().getInteger("customerCompany")) && !ids.contains(param.getValue().getInteger("sellerCompany"))) {
          return param.getValue().get("sellerCompanyName");
        }
        if(!ids.contains(param.getValue().getInteger("customerCompany")) && ids.contains(param.getValue().getInteger("sellerCompany"))) {
          return param.getValue().get("customerCompanyName");
        }
      }
      return param.getValue().get("customerCompanyName");
    });
    
    leftTable.getColumns().stream().forEach(c -> c.setSortable(false));
    rightTable.getColumns().stream().forEach(c -> c.setSortable(false));
    
    leftTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    rightTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    
    leftTable.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends ScaleRow<ScalePeriod>> observable, ScaleRow<ScalePeriod> oldValue, ScaleRow<ScalePeriod> newValue) -> {
      if(!leftTable.isDisable()) {
        rightTable.setDisable(true);
        scale.setDisable(true);
        
        rightTable.getSelectionModel().clearSelection();
        leftTable.getSelectionModel().getSelectedItems().stream().forEach(r -> rightTable.getSelectionModel().select(r));
        scale.setSelectRows(leftTable.getSelectionModel().getSelectedItems());
        
        scale.setDisable(false);
        rightTable.setDisable(false);
      }
    });
    
    rightTable.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends ScaleRow<ScalePeriod>> observable, ScaleRow<ScalePeriod> oldValue, ScaleRow<ScalePeriod> newValue) -> {
      if(!rightTable.isDisable()) {
        leftTable.setDisable(true);
        scale.setDisable(true);
        
        leftTable.getSelectionModel().clearSelection();
        rightTable.getSelectionModel().getSelectedItems().stream().forEach(r -> leftTable.getSelectionModel().select(r));
        scale.setSelectRows(rightTable.getSelectionModel().getSelectedItems());
        
        scale.setDisable(false);
        leftTable.setDisable(false);
      }
    });
    
    scale.selectedRowProperty().addListener((ObservableValue<? extends List<ScaleRow<ScalePeriod>>> observable, List<ScaleRow<ScalePeriod>> oldValue, List<ScaleRow<ScalePeriod>> newValue) -> {
      if(!scale.isDisable()) {
        leftTable.setDisable(true);
        rightTable.setDisable(true);

        leftTable.getSelectionModel().clearSelection();
        rightTable.getSelectionModel().clearSelection();

        newValue.forEach(r -> {
          leftTable.getSelectionModel().select(r);
          rightTable.getSelectionModel().select(r);
        });

        leftTable.setDisable(false);
        rightTable.setDisable(false);
      }
    });
    
    scale.selectedPeriodProperty().addListener((ObservableValue<? extends List<ScalePeriod>> observable, List<ScalePeriod> oldValue, List<ScalePeriod> newValue) -> {
      actions.clear();
      dealPositionTable.getClientFilter().clear().AND_IN("deal", newValue.stream().map(p -> p.getInteger("id")).collect(Collectors.toList()).toArray(new Integer[0]));
      dealPositionTable.initData();
      if(newValue.size() == 1)
        commentTable.objectIdProperty().setValue(newValue.get(0).getInteger("id"));
      else commentTable.objectIdProperty().setValue(0);
      
      fillCost(newValue);
    });
    
    scale.periodDatesListeners().add((ScaleTable.PeriodDateListener) (LocalDate oldStart, LocalDate oldEnd, ScalePeriod period) -> {
      if(FXD.showWait("Внимание...", this.getContent(), "Переместить?", FXD.ButtonType.YES, FXD.ButtonType.NO).orElseGet(() -> FXD.ButtonType.NO) == FXD.ButtonType.YES) {
        Bounds bounds = scale.getSelectedLayer().screenToLocal(period.localToScreen(period.getBoundsInLocal()));
        Region region = new Region();
        region.setLayoutX(bounds.getMinX());
        region.setLayoutY(bounds.getMinY());
        region.setPrefWidth(bounds.getWidth());
        region.setPrefHeight(bounds.getHeight());
        scale.getSelectedLayer().getChildren().setAll(region);
      }else {
        period.startDateProperty().setValue(oldStart);
        period.endDateProperty().setValue(oldEnd);
      }
    });
    
    commentTable.disableProperty().bind(Bindings.createBooleanBinding(() -> scale.selectedPeriodProperty().getValue().size() > 1, scale.selectedPeriodProperty()));
    
    dealPositionTable.getPositionTable().addInitDataListener(e -> initActionTable());
    dealPositionTable.getPositionTable().getTable().getSelectionModel().selectedItemProperty().addListener(listener);
  }
  
  private void fillCost(List<ScalePeriod> periods) {
    BigDecimal green  = BigDecimal.ZERO;
    BigDecimal yellow = BigDecimal.ZERO;
    BigDecimal blue   = BigDecimal.ZERO;
    BigDecimal red    = BigDecimal.ZERO;
    BigDecimal gray   = BigDecimal.ZERO;
    
    for(ScalePeriod period:periods) {
      if(period.getStyleClass().containsAll(Arrays.asList("started","payed","dispatched")))
        green  = green.add(period.getBigDecimal("cost"));
      
      if(period.getStyleClass().containsAll(Arrays.asList("started","payed")) && !period.getStyleClass().contains("dispatched"))
        blue   = blue.add(period.getBigDecimal("cost"));
      
      if(period.getStyleClass().containsAll(Arrays.asList("started","dispatched")) && !period.getStyleClass().contains("payed"))
        red    = red.add(period.getBigDecimal("cost"));
      
      if(period.getStyleClass().containsAll(Arrays.asList("started")) && !period.getStyleClass().contains("dispatched") && !period.getStyleClass().contains("payed"))
        yellow = yellow.add(period.getBigDecimal("cost"));
      
      if(!period.getStyleClass().contains("started") && !period.getStyleClass().contains("dispatched") && !period.getStyleClass().contains("payed"))
        gray   = gray.add(period.getBigDecimal("cost"));
    }
    
    green  = green.setScale(2, RoundingMode.HALF_UP);
    yellow = yellow.setScale(2, RoundingMode.HALF_UP);
    blue   = blue.setScale(2, RoundingMode.HALF_UP);
    red    = red.setScale(2, RoundingMode.HALF_UP);
    gray   = gray.setScale(2, RoundingMode.HALF_UP);
    
    BigDecimal sum = green.add(yellow).add(blue).add(red).add(gray).setScale(2, RoundingMode.HALF_UP);
    
    sum = sum.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : sum;

    greenFilterButton.setText(green.toPlainString()+" ("+(green.divide(sum,2,RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))+"%)");
    blueFilterButton.setText(blue.toPlainString()+" ("+(blue.divide(sum,2,RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))+"%)");
    redFilterButton.setText(red.toPlainString()+" ("+(red.divide(sum,2,RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))+"%)");
    yellowFilterButton.setText(yellow.toPlainString()+" ("+(yellow.divide(sum,2,RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))+"%)");
    grayFilterButton.setText(gray.toPlainString()+" ("+(gray.divide(sum,2,RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))+"%)");
  }
  
  public void createEvent(List<PropertyMap> deals) {
    Platform.runLater(() -> {
      deals.stream().forEach(deal -> {
        ScaleRow<ScalePeriod> row = items.stream().filter(r -> r.getInteger("tempProcess").equals(deal.getInteger("tempProcess"))).findFirst().orElseGet(() -> {
          ScaleRow r = (ScaleRow)scale.createRow().copyFrom(deal);
          items.add(r);
          return r;
        });
        ScalePeriod period = createPeriod(deal);
        row.getChildren().add(period);
        initComment(period);
      });
      FXDivisionTable.bindScrollBars(Orientation.VERTICAL, leftTable, rightTable, scale.getRootScroll());
    });
  }
  
  public void updateEvent(List<PropertyMap> deals) {
    Platform.runLater(() -> {
      List<Integer> ids = deals.stream().map(d -> d.getInteger("id")).collect(Collectors.toList());
      List<ScalePeriod> periods = scale.getRows().stream().flatMap(r -> r.getPeriods().stream()).filter(sp -> ids.contains(sp.getInteger("id"))).collect(Collectors.toList());
      List<PropertyMap> createdDeals = FXCollections.observableArrayList();
      
      deals.forEach(deal -> {
        ScalePeriod period = periods.stream().filter(p -> p.getInteger("id").equals(deal.getInteger("id"))).findFirst().orElseGet(() -> null);
        if(period != null) {
          if(deal.is("tmp") || deal.getValue("type", Deal.Type.class) == MappingObject.Type.ARCHIVE) {
            ((ScaleRow)period.getParent()).getChildren().remove(period);
          }else initComment(initPeriod((ScalePeriod)period.copyFrom(deal)));
        }else {//создание
          if(!deal.is("tmp") && deal.getValue("type", Deal.Type.class) == MappingObject.Type.CURRENT)
            createdDeals.add(deal);
        }
      });
      if(!createdDeals.isEmpty())
        createEvent(createdDeals);
    });
  }
  
  ChangeListener listener = (ChangeListener) (ObservableValue observable, Object oldValue, Object newValue) -> initActionTable();
  
  private void initActionTable() {
    if(dealPositionTable.getPositionTable().getTable().getSelectionModel().getSelectedItems().isEmpty())
      actions.dealPositionsProperty().setValue(PropertyMap.copyList(dealPositionTable.getPositionTable().getTable().getItems()));
    else actions.dealPositionsProperty().setValue(PropertyMap.copyList(dealPositionTable.getPositionTable().getTable().getSelectionModel().getSelectedItems()));
  }
  
  private void fireDealStatusFilter() {
    Predicate<ScalePeriod> statusPredicate = (ScalePeriod t) -> false;
    
    if(!greenFilterButton.isSelected() && !blueFilterButton.isSelected() && !redFilterButton.isSelected() && !yellowFilterButton.isSelected() && !grayFilterButton.isSelected())
      statusPredicate = (Predicate) (Object t) -> true;
    
    if(greenFilterButton.isSelected())
      statusPredicate = statusPredicate.or(t -> t.getStyleClass().containsAll(Arrays.asList("started","payed","dispatched")));

    if(blueFilterButton.isSelected())
      statusPredicate = statusPredicate.or(t -> t.getStyleClass().containsAll(Arrays.asList("started","payed")) && !t.getStyleClass().contains("dispatched"));

    if(redFilterButton.isSelected())
      statusPredicate = statusPredicate.or(t -> t.getStyleClass().containsAll(Arrays.asList("started","dispatched")) && !t.getStyleClass().contains("payed"));

    if(yellowFilterButton.isSelected())
      statusPredicate = statusPredicate.or(t -> t.getStyleClass().containsAll(Arrays.asList("started")) && !t.getStyleClass().contains("dispatched") && !t.getStyleClass().contains("payed"));

    if(grayFilterButton.isSelected())
      statusPredicate = statusPredicate.or(t -> !t.getStyleClass().contains("started") && !t.getStyleClass().contains("dispatched") && !t.getStyleClass().contains("payed"));

    scale.setDealPredicate(statusPredicate);
  }
  
  private void fireFilter() {
    Predicate<ScaleRow<ScalePeriod>> predicate = (ScaleRow<ScalePeriod> t) -> true;
    
    if(prFilter.isActive())
      predicate = predicate.and(prFilter.getPredicate());

    if(cpFilter.isActive())
      predicate = predicate.and(cpFilter.getPredicate());
    
    filterItemsProperty.getValue().setPredicate(predicate);
  }

  public ObservableList<String> getFields() {
    return fields;
  }

  public Column getColumn(String columnName) {
    return leftTable.getColumn(columnName) == null ? rightTable.getColumn(columnName) : leftTable.getColumn(columnName);
  }
  
  private void createDeal() {
    PropertyMap deal = PropertyMap.create();
    ScaleRow<ScalePeriod> row = leftTable.getSelectionModel().getSelectedItem();
    
    if(row != null) {
      deal = PropertyMap.copy(row, "service","contract","sellerCompany","customerCompany","sellerCompanyPartition","customerCompanyPartition","sellerCfc","customerCfc","tempProcess");
      deal.setValue("dealStartDate", scale.getSelectedDate());
    }else if(!contractFilter.isEmpty()) {
      Integer contract = null;
      if(contractFilter.get(0).getValues()[0] instanceof Integer[])
        contract = ((Integer[]) contractFilter.get(0).getValues()[0])[0];
      if(contractFilter.get(0).getValues()[0] instanceof PropertyMap[])
        contract = ((PropertyMap[])contractFilter.get(0).getValues()[0])[0].getInteger("id");
      if(contract != null)
        deal = ObjectLoader.getMap(Contract.class, contract, "sellerCompany","customerCompany","sellerCompanyPartition","customerCompanyPartition","sellerCfc","customerCfc").setValue("contract", contract);
    }
    
    if(!deal.isEmpty()) {
      FXDealMaster mp = new FXDealMaster(deal);
      if(mp.show(getContent())) {
        System.out.println("OK!!!");
        //System.out.println("MASTER DEAL: "+deal.toJson());
      }
    }
  }
  
  private void removeDeal() {
    List<ScalePeriod> list = scale.getSelectedPeriods();
    if(!list.isEmpty() && FXD.showWait("Внимание!!!", getContent(), "Удалить?", FXD.ButtonType.YES, FXD.ButtonType.NO).orElseGet(() -> FXD.ButtonType.NO) == FXD.ButtonType.YES)
      ObjectLoader.toTypeObjects(Deal.class, MappingObject.Type.ARCHIVE, PropertyMap.getArrayFromList(list, "id", Integer.class));
  }
  
  class InitDataTask extends DivisionTask {
    public InitDataTask() {
      super("dealTask", rootSplit);
    }

    @Override
    public void task() throws DivisionTaskException {
      if(!isRun())
        return;
      dateFilter.clear().AND_DATE_AFTER_OR_EQUAL("dealStartDate", Utility.convert((LocalDate)scale.leftDateProperty().getValue()))
              .AND_DATE_BEFORE_OR_EQUAL("dealEndDate", Utility.convert((LocalDate)scale.rightDateProperty().getValue()));

      Map<Integer,List<ScalePeriod>> model = new TreeMap<>();

      ObjectLoader.getList(rootFilter, fields).forEach(p -> {
        if(isRun())
          try {
            List<ScalePeriod> periods = model.get(p.getInteger("tempProcess"));
            if(periods == null)
              model.put(p.getInteger("tempProcess"), periods = new ArrayList<>());

            periods.add(createPeriod(p));
          } catch (Exception ex) {
            MsgTrash.out(ex);
          }
      });
      
      if(isRun())
        Platform.runLater(() -> {
          Hronometr.start("draw");
          items.setAll(FXCollections.observableArrayList(model.keySet().stream().map(r -> {
            try {
              return (ScaleRow<ScalePeriod>)scale.createRow(model.get(r)).copyFrom(model.get(r).get(0));
            } catch (Exception ex) {
              MsgTrash.out(ex);
            }
            return null;
          }).collect(Collectors.toList())));

          FXDivisionTable.bindScrollBars(Orientation.VERTICAL, leftTable, rightTable, scale.getRootScroll());
          createComments();
          model.clear();
          Hronometr.stop("draw");
        });
    }
  }
  
  InitDataTask initDataTask = new InitDataTask();
  
  public void initData() {
    DivisionTask.stop(initDataTask);
    Platform.runLater(() -> {
      items.clear();
      scale.clear();
      actions.clear();
    });
    DivisionTask.start(initDataTask = new InitDataTask());
  }
  
  private ScalePeriod createPeriod(PropertyMap p) {
    try {
      return initPeriod((ScalePeriod)scale.createPeriod(Utility.convert(p.getSqlDate("dealStartDate")), Utility.convert(p.getSqlDate("dealEndDate"))).copyFrom(p));
    }catch(Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }
  
  private ScalePeriod initPeriod(ScalePeriod period) {
    Integer    startCount     = period.getInteger("startCount");
    Integer    dispatchCount  = period.getInteger("dispatchCount");
    BigDecimal dispatchAmount = period.getBigDecimal("dispatchAmount");
    Integer    paymentCount   = period.getInteger("paymentCount");
    BigDecimal paymentAmount  = period.getBigDecimal("paymentAmount");
    
    final List<String> status = new ArrayList<>();
    if(startCount    >  0)
      status.add("started");
    if(dispatchCount >  0 && dispatchAmount.compareTo(paymentAmount) >= 0)
      status.add("dispatched");
    if(paymentCount  >  0 && paymentAmount.compareTo(dispatchAmount) >= 0 || startCount > 0 && period.getValue("cost", BigDecimal.class).compareTo(BigDecimal.ZERO) == 0)
      status.add("payed");

    Platform.runLater(() -> {
      period.getStyleClass().removeAll("started","dispatched","payed");
      period.getStyleClass().addAll(status);
    });
    return period;
  }
  
  private void createComments() {
    scale.getClientLayer().getChildren().clear();
    VBox toolTipPane = new VBox();
    toolTipPane.getStyleClass().add("scale-tool-tip");
    
    scale.getClientLayer().addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
      ScalePeriod period = scale.periodAtPoint(e.getScreenX(), e.getScreenY());
      if(period != null) {
        String[] comments = period.getArray("deal-comments", String.class);
        if(comments.length > 0) {
          if(!scale.getClientLayer().getChildren().contains(toolTipPane))
            scale.getClientLayer().getChildren().add(toolTipPane);

          toolTipPane.getChildren().clear();
          for(int i=0;i<comments.length;i++) {
            String[] s = comments[i].split("\\[::\\]");

            HBox commentBox = new HBox(new Label(s[0]));
            commentBox.getStyleClass().add("comment-box");

            HBox avtorBox = new HBox(new Label(s[1]));
            avtorBox.getStyleClass().add("avtor-box");

            VBox comment = new VBox(commentBox,avtorBox);
            comment.getStyleClass().add(i%2 == 0 ? "comment-0" : "comment-1");
            toolTipPane.getChildren().add(comment);
          }
          toolTipPane.setLayoutX(e.getX());
          toolTipPane.setLayoutY(e.getY());
        }else scale.getClientLayer().getChildren().remove(toolTipPane);
      }else scale.getClientLayer().getChildren().remove(toolTipPane);
    });
    
    scale.getClientLayer().addEventHandler(MouseEvent.MOUSE_EXITED, e -> scale.getClientLayer().getChildren().remove(toolTipPane));
    scale.getRows().stream().flatMap(r -> r.getPeriods().stream()).forEach(p -> initComment(p));
  }
  
  private void initComment(ScalePeriod p) {
    Platform.runLater(() -> {
      p.getChildren().clear();
      if(p.getArray("deal-comments", String.class).length > 0) {
        CommentPane c = new CommentPane(p.getInteger("id"));
        c.visibleProperty().bind(p.visibleProperty());
        c.getStyleClass().add("comment");
        Platform.runLater(() -> {
          c.setLayoutX(2);
          c.setLayoutY(2);
          p.getChildren().add(c);
          c.toFront();
        });
      }
    });
  }
  
  private class CommentPane extends Pane {
    private final Integer dealId;

    public CommentPane(Integer dealId) {
      this.dealId = dealId;
    }

    public Integer getDealId() {
      return dealId;
    }
  }

  @Override
  public Node getContent() {
    return root;
  }

  @Override
  public ObservableList<Node> storeControls() {
    return FXCollections.observableArrayList(getContent(), rootSplit, scaleSplit, actionSplit, dealPositionTable, leftTable, rightTable, actions);
  }

  @Override
  public void start() {
    show("Тестовые Сделки 2");
  }

  @Override
  public void finaly() {
    dealPositionTable.getPositionTable().getTable().getSelectionModel().selectedItemProperty().removeListener(listener);
    
    dealPositionTable.getPositionTable().clearData();
    dealPositionTable.getEquipmentFactorTable().clear();
    dealPositionTable = null;
    
    companyFilter.clear();
    companyFilter = null;
    
    dateFilter.clear();
    dateFilter = null;
    
    rootFilter.clear();
    rootFilter = null;
    
    items.clear();
    
    dealTarget = null;
    scale = null;
    
    scaleSplit.getItems().clear();
    scaleSplit = null;

    rootSplit.getItems().clear();
    rootSplit = null;
  }
  
  public Integer[] getSelectedContractProcess() {
    return leftTable.getSelectionModel().getSelectedItems().stream().map(r -> r.getInteger("tempProcess")).collect(Collectors.toSet()).toArray(new Integer[0]);
  }

  public FXDivisionTable<ScaleRow<ScalePeriod>> getLeftTable() {
    return leftTable;
  }

  public FXDivisionTable<ScaleRow<ScalePeriod>> getRightTable() {
    return rightTable;
  }

  public ScaleTable<ScaleRow<ScalePeriod>, ScalePeriod> getScale() {
    return scale;
  }

  public ObservableList<ScaleRow<ScalePeriod>> getItems() {
    return items;
  }
  
  @Override
  public void changeCompany(Integer[] ids, boolean init) {
    companyFilter.clear();
    if(ids != null && ids.length > 0)
      companyFilter.AND_IN("sellerCompany", ids).OR_IN("customerCompany", ids);
    if(init)
      initData();
  }

  @Override
  public void changeCFC(Integer[] ids, boolean init) {
    cfcFilter.clear();
    if(ids != null && ids.length > 0)
      cfcFilter.AND_IN("sellerCfc", ids).OR_IN("customerCfc", ids);
    if(init)
      initData();
  }
  
  public void changeContract(Integer[] ids, boolean init) {
    contractFilter.clear();
    if(ids != null && ids.length > 0)
      contractFilter.AND_IN("contract", ids);
    if(init)
      initData();
  }
}