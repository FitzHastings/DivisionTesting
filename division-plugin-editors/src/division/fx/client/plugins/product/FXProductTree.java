package division.fx.client.plugins.product;

import TextFieldLabel.TextFieldLabel;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Company;
import bum.interfaces.Group;
import bum.interfaces.Product;
import bum.interfaces.Service;
import division.fx.ChoiseLabel.ChoiceLabel;
import division.fx.PropertyMap;
import static division.fx.client.plugins.product.FXProductTree.TreeItemType.GROUP;
import static division.fx.client.plugins.product.FXProductTree.TreeItemType.PROCESS;
import static division.fx.client.plugins.product.FXProductTree.TreeType.PROCESS_TO_GROUP;
import division.fx.dialog.FXD;
import division.fx.table.DivisionCellEditor;
import division.fx.tree.FXDivisionTreeTable;
import division.fx.tree.FXDivisionTreeTableCell;
import division.fx.tree.FXTree;
import division.fx.tree.TreeColumn;
import division.util.Hronometr;
import division.util.IDStore;
import division.util.Utility;
import java.math.BigDecimal;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import javafx.util.converter.BigDecimalStringConverter;
import javafx.util.converter.IntegerStringConverter;
import util.filter.local.DBFilter;

public final class FXProductTree extends FXDivisionTreeTable<PropertyMap> {
  public enum TreeItemType {PROCESS, GROUP, PRODUCT}
  public enum TreeType {PROCESS_TO_GROUP, GROUP_TO_PROCESS}
  private final ExecutorService pool = Executors.newCachedThreadPool();
  
  private final TreeItem<PropertyMap>        root     = new TreeItem<>(PropertyMap.create().setValue("name", "root"));
  private final ObjectProperty<TreeType>     treeType = new SimpleObjectProperty<>();
  private final ObjectProperty<PropertyMap>  companyProperty  = new SimpleObjectProperty();
  
  private TreeMap<Integer, PropertyMap> mementoProductMap = new TreeMap<>();
  private HashMap<String,TreeItem<PropertyMap>> nodeCash = new HashMap();
  
  private final TreeColumn<PropertyMap, String>      objectColumn     = TreeColumn.create("Продукты","name");
  private final TreeColumn<PropertyMap, Period>      durationColumn   = TreeColumn.create("Длительность","duration",true,false);
  private final TreeColumn<PropertyMap, Period>      recurrenceColumn = TreeColumn.create("Цикличность","recurrence",true,false);
  private final TreeColumn<PropertyMap, BigDecimal>  costColumn       = TreeColumn.create("Цена","cost",true,true);
  
  private final TreeColumn<PropertyMap, Boolean>    isNdsColumn      = TreeColumn.create("Облагается","isNds",true,false);
  private final TreeColumn<PropertyMap, BigDecimal> stavkaNdsColumn  = TreeColumn.create("Ставка","nds",true,true);
  private final TreeColumn ndsColumn                                 = TreeColumn.create("НДС").addColumns(isNdsColumn, stavkaNdsColumn);
  
  private final List<PropertyMap> processes = FXCollections.observableArrayList();
  private final List<PropertyMap> groups    = FXCollections.observableArrayList();
  
  private final List<EventHandler> initDataHandlers = FXCollections.observableArrayList();
  
  private TreeItemType rootType, subType;
  String rootProp, subProp;

  public FXProductTree() {
    setRoot(root);
    getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    getColumns().setAll(objectColumn, durationColumn, recurrenceColumn, costColumn, ndsColumn);
    
    companyProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> initData());
    
    treeTypeProperty().addListener((ObservableValue<? extends TreeType> observable, TreeType oldValue, TreeType newValue) -> {
      rootType = newValue == TreeType.PROCESS_TO_GROUP ? TreeItemType.PROCESS : TreeItemType.GROUP;
      rootProp = rootType == TreeItemType.PROCESS ? "service" : "group";
      subType  = newValue == TreeType.PROCESS_TO_GROUP ? TreeItemType.GROUP : TreeItemType.PROCESS;
      subProp  = subType  == TreeItemType.PROCESS ? "service" : "group";
      initData();
    });
    treeType.setValue(TreeType.PROCESS_TO_GROUP);

    setRowFactory((TreeTableView<PropertyMap> param) -> {
      TreeTableRow<PropertyMap> row = new TreeTableRow<PropertyMap>() {
        @Override
        protected void updateItem(PropertyMap item, boolean empty) {
          getStyleClass().removeAll("product-tree-cell", "process-row", "product-item", "product-tree-cell", "sub-price-object", "last-price-object");
          
          getStyleClass().add("product-tree-cell");
          
          if(isProcessProduct(getTreeItem()))
            getStyleClass().add("process-row");
          
          if(isLastPriceTypeObject(getTreeItem()))
            getStyleClass().addAll("last-price-object");

          if(isGroupProduct(getTreeItem()))
            getStyleClass().addAll("product-item", "product-tree-cell");

          if(isSubPriceTypeObject(getTreeItem()))
            getStyleClass().add("sub-price-object");
          
          super.updateItem(item, empty);
        }
      };
      return row;
    });
    
    costColumn.setCellFactory((TreeTableColumn<PropertyMap, BigDecimal> param) -> createCell(null, new BigDecimalStringConverter(), null));
    
    durationColumn.setCellFactory((TreeTableColumn<PropertyMap, Period> param) -> createCellForPeriod());
    recurrenceColumn.setCellFactory((TreeTableColumn<PropertyMap, Period> param) -> createCellForPeriod());
    
    isNdsColumn.setCellFactory((TreeTableColumn<PropertyMap, Boolean> param) -> {
      return new FXDivisionTreeTableCell<PropertyMap, Boolean>() {
        @Override
        protected void updateItem(Boolean item, boolean empty) {
          super.updateItem(item, empty);
          setEditable(item != null);
        }
      };
    });
    
    stavkaNdsColumn.setCellFactory((TreeTableColumn<PropertyMap, BigDecimal> param) -> createCell(null, new BigDecimalStringConverter(), e -> {
      
      ((FXDivisionTreeTableCell<PropertyMap,BigDecimal>)e.getSource()).disableProperty().unbind();
      
      if(((FXDivisionTreeTableCell<PropertyMap,BigDecimal>)e.getSource()).getRowItem() != null && ((FXDivisionTreeTableCell<PropertyMap,BigDecimal>)e.getSource()).getRowItem().isNotNull("isNds")) {
        ((FXDivisionTreeTableCell<PropertyMap,BigDecimal>)e.getSource()).disableProperty().bind(Bindings.createBooleanBinding(new Callable<Boolean>() {
          @Override
          public Boolean call() throws Exception {
            return !((FXDivisionTreeTableCell<PropertyMap,BigDecimal>)e.getSource()).getRowItem().is("isNds");
          }
        }, ((FXDivisionTreeTableCell<PropertyMap,BigDecimal>)e.getSource()).getRowItem().get("isNds")));
      }
    }));

    setEditable(true);
  }
  
  public List<EventHandler> initDataHandlers() {
    return initDataHandlers;
  }
  
  private FXDivisionTreeTableCell createCellForPeriod() {
    FXDivisionTreeTableCell<PropertyMap, Period> cell = new FXDivisionTreeTableCell<PropertyMap, Period>() {
      @Override
      public void commitEdit(Period newValue) {
        if(getTreeTableRow().getItem() != null) {
          if(isGroupProduct(getTreeTableRow().getTreeItem()) || isProcessProduct(getTreeTableRow().getTreeItem())) {
            Period oldPeriod = (Period) getColumn().getValue(getTreeTableRow().getItem());
            super.commitEdit(newValue);

            Period r = getTreeTableRow().getItem().getPeriod("recurrence") == null || getTreeTableRow().getItem().getPeriod("recurrence").isZero() ? null : getTreeTableRow().getItem().getPeriod("recurrence");
            Period d = getTreeTableRow().getItem().getPeriod("duration") == null || getTreeTableRow().getItem().getPeriod("duration").isZero() ? null : getTreeTableRow().getItem().getPeriod("duration");

            String msg = null;

            if(d == null && r != null)
              msg = "Цикличность продукта без длительности безсмысленна";

            if(r != null && d != null && r.getDays()+r.getMonths()*30+r.getYears()*365 < d.getDays()+d.getMonths()*30+d.getYears()*365)
              msg = "Цикличность должна быть больше длительности";

            if(msg != null) {
              commitEdit(oldPeriod);
              getTreeTableView().refresh();
              FXD.show(null, "Ошибка!!!", getTreeTableView(), msg, FXD.ButtonType.OK);
            }else if(isProcessProduct(getTreeTableRow().getTreeItem()) && getTreeTableRow().getTreeItem().getValue().isNull("product")) {
              getTreeTableRow().getTreeItem().getValue()
                      .setValue("company", companyProperty().getValue().getInteger("id"))
                      .setValue("service", getTreeTableRow().getTreeItem().getValue().getInteger("id"))
                      .setValue("service_name", getTreeTableRow().getTreeItem().getValue().getString("name"))
                      .setValue("product", IDStore.createIntegerID("product")*(-1));
            }
          }else if(!isProcessProduct(getTreeTableRow().getTreeItem())) {
            getColumn().setValue(getTreeTableRow().getItem(), null);
            refresh();
            FXTree.ListItems(getTreeTableRow().getTreeItem(), "type", TreeItemType.PRODUCT, false).stream().filter(p -> {
              Period oldPer = (Period) getColumn().getValue(p.getValue());

              getColumn().setValue(p.getValue(), newValue);

              Period re = p.getValue().getPeriod("recurrence") == null || p.getValue().getPeriod("recurrence").isZero() ? null : p.getValue().getPeriod("recurrence");
              Period du = p.getValue().getPeriod("duration") == null || p.getValue().getPeriod("duration").isZero() ? null : p.getValue().getPeriod("duration");

              boolean returnValue = !(du == null && re != null || du != null && re != null && re.getDays()+re.getMonths()*30+re.getYears()*365 < du.getDays()+du.getMonths()*30+du.getYears()*365);

              if(!returnValue)
                getColumn().setValue(p.getValue(), oldPer);

              return returnValue;
            }).forEach(p -> getColumn().setValue(p.getValue(), newValue));
          }
        }
      }
      
      @Override
      protected void updateItem(Period item, boolean empty) {
        if(empty) {
          setContentDisplay(ContentDisplay.TEXT_ONLY);
          super.updateItem(item, empty);
        }else {
          setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
          if(item == null || Utility.getCount(item) == 0) {
            setGraphic(createEditorNodeForNull(e -> {
              setGraphic(createEditorNode(this, item));
              ((TextFieldLabel<Integer>)((HBox)getGraphic()).getChildren().get(0)).startEdit();
            }));
          }else setGraphic(createEditorNode(this, item));
        }
      }
    };
    return cell;
  }
  
  private Node createEditorNodeForNull(EventHandler e) {
    Label editPeriod = new Label("...");
    editPeriod.getStyleClass().add("link-label");
    HBox periodBox = new HBox(editPeriod);
    periodBox.setAlignment(Pos.CENTER_RIGHT);
    editPeriod.setOnMouseClicked(e);
    return periodBox;
  }
  
  //HBox currentEditor = null;
  
  private Node createEditorNode(FXDivisionTreeTableCell cell, Period period) {
    ChoiceLabel<String> typeLabel  = new ChoiceLabel<>("");
    
    TextFieldLabel<Integer> countLabel = new TextFieldLabel<Integer>("", period == null ? 0 : Utility.getCount(period), new IntegerStringConverter() {
      @Override
      public Integer fromString(String value) {
        return value == null || value.equals("") ? null : super.fromString(value) < 0 ? null : super.fromString(value);
      }
    }) {
      @Override
      public void startEdit() {
        typeLabel.startEdit();
        super.startEdit();
      }
    };
    
    countLabel.nameVisibleProperty().setValue(false);
    countLabel.promtTextProperty().setValue("...");
    countLabel.setAlignment(Pos.CENTER_RIGHT);

    typeLabel.promtTextProperty().setValue("");
    typeLabel.nameVisibleProperty().setValue(false);
    typeLabel.itemsProperty().getValue().setAll(Utility.getPeriodTypes(countLabel.valueProperty().getValue()));
    if(period != null)
      typeLabel.valueProperty().setValue(Utility.getType(period));
    
    countLabel.commitListener().add(e -> {
      typeLabel.stopEdit();
      if(countLabel.valueProperty().getValue() == null || countLabel.valueProperty().getValue() == 0) 
        cell.commitEdit(null);
      else cell.commitEdit(countLabel.valueProperty().getValue() != null && countLabel.valueProperty().getValue() > 0 && typeLabel.valueProperty().getValue() != null ? 
                    Utility.convert(countLabel.valueProperty().getValue()+" "+typeLabel.valueProperty().getValue()) : null);
    });
    
    countLabel.getField().valueProperty().addListener((ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) -> {
      if(newValue != null && (Integer)newValue > 0) {
        typeLabel.stopEdit();
        Integer index = typeLabel.itemsProperty().getValue().indexOf(typeLabel.valueProperty().getValue());
        typeLabel.itemsProperty().getValue().setAll(Utility.getPeriodTypes((Integer)newValue));
        typeLabel.valueProperty().setValue(typeLabel.itemsProperty().getValue().get(index > 0 ? index : 0));
        typeLabel.startEdit();
      }
    });

    typeLabel.commitListener().add(e -> {
      countLabel.valueProperty().setValue(countLabel.getField().getValue());
      countLabel.cancelEdit();
      if(countLabel.valueProperty().getValue() == null || countLabel.valueProperty().getValue() == 0) 
        cell.commitEdit(null);
      else cell.commitEdit(countLabel.valueProperty().getValue() != null && countLabel.valueProperty().getValue() > 0 && typeLabel.valueProperty().getValue() != null ? 
                    Utility.convert(countLabel.valueProperty().getValue()+" "+typeLabel.valueProperty().getValue()) : null);
    });

    HBox editor = new HBox(countLabel, typeLabel);
    editor.getStyleClass().add("editor-box");
    
    editor.setOnKeyReleased(e -> {
      if(e.getCode() == KeyCode.ENTER)
        countLabel.commit();
    });
    
    return editor;
  }
  
  public boolean isSubPriceTypeObject(TreeItem<PropertyMap> treeItem) {
    return treeItem != null &&
            
            (treeTypeProperty().getValue() == TreeType.GROUP_TO_PROCESS && 
            treeItem.getValue().getValue("type", TreeItemType.class) == TreeItemType.PROCESS || 
            
            treeTypeProperty().getValue() == TreeType.PROCESS_TO_GROUP && 
            treeItem.getValue().getValue("type", TreeItemType.class) == TreeItemType.GROUP);
  }
  
  public boolean isLastPriceTypeObject(TreeItem<PropertyMap> treeItem) {
    return treeItem != null && 
            listItems((TreeItem<PropertyMap>)treeItem, "type", treeItem.getValue().getValue("type", TreeItemType.class), false).isEmpty() && 
            
            (treeTypeProperty().getValue() == TreeType.PROCESS_TO_GROUP && treeItem.getValue().getValue("type", TreeItemType.class) == PROCESS || 
            treeTypeProperty().getValue() == TreeType.GROUP_TO_PROCESS && treeItem.getValue().getValue("type", TreeItemType.class) == GROUP);
  }
  
  public boolean isProcessProduct(TreeItem<PropertyMap> treeItem) {
    return treeItem != null &&
                  treeItem.getValue().getValue("type", TreeItemType.class) == PROCESS &&
                  listItems(treeItem, "type", PROCESS, false).isEmpty() &&
                  treeTypeProperty().getValue() == TreeType.PROCESS_TO_GROUP;
  }
  
  public boolean isGroupProduct(TreeItem<PropertyMap> treeItem) {
    return treeItem != null && treeItem.getValue().getValue("type", TreeItemType.class) == TreeItemType.PRODUCT;
  }
  
  private <T> FXDivisionTreeTableCell<PropertyMap, T> createCell(DivisionCellEditor editor, StringConverter customTextConverter, EventHandler ev) {
    FXDivisionTreeTableCell<PropertyMap, T> cell = new FXDivisionTreeTableCell<PropertyMap, T>() {
      @Override
      protected void updateItem(T item, boolean empty) {
        if(ev != null)
          ev.handle(new ActionEvent(this, null));
        super.updateItem(item, empty);
      }
      
      @Override
      public void commitEdit(T newValue) {
        if(newValue != null) {
          if(isGroupProduct(getTreeTableRow().getTreeItem()) || isProcessProduct(getTreeTableRow().getTreeItem()))
            super.commitEdit(newValue);
          else {
            if(FXD.showWait("Вопрос!", this, "Присвоить это значение всем вложенным объектам?", FXD.ButtonType.YES, FXD.ButtonType.CANCEL).orElseGet(() -> FXD.ButtonType.CANCEL) == FXD.ButtonType.YES)
              FXTree.ListItems(getTreeTableRow().getTreeItem(), "type", TreeItemType.PRODUCT).forEach(it -> getColumn().setValue(it.getValue(), newValue));
            cancelEdit();
          }
        }
      }
    };
    cell.cellEditorProperty().setValue(editor);
    cell.customTextConverterProperty().setValue(customTextConverter);
    cell.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {cell.startEdit();});
    return cell;
  }
  
  public ObjectProperty<PropertyMap> companyProperty() {
    return companyProperty;
  }

  public ObjectProperty<TreeType> treeTypeProperty() {
    return treeType;
  }
  
  public void initData() {
    Platform.runLater(() -> root.getChildren().clear());
    mementoProductMap.clear();
    if(companyProperty().getValue() != null) {
      Platform.runLater(() -> {
        if(getScene() != null)
          getScene().getRoot().setCursor(Cursor.WAIT);
      });
      pool.submit(() -> {
        DBFilter filter = DBFilter.create(Product.class)
                .AND_EQUAL("company", companyProperty().getValue().getInteger("id"))
                .AND_EQUAL("type", Product.Type.CURRENT)
                .AND_EQUAL("tmp", false);
        try {
          boolean ndspayer = ObjectLoader.getObjectFieldValue(Boolean.class, Company.class, companyProperty().getValue().getInteger("id"), "ndsPayer");
          Platform.runLater(() -> {
            ndsColumn.setVisible(ndspayer);
            ndsColumn.setEditable(ndspayer);
          });
          Hronometr.start("get");
          ObjectLoader.fillList(DBFilter.create(Service.class), processes, "id", "name", "parent");
          ObjectLoader.fillList(DBFilter.create(Group.class),   groups,    "id", "name", "parent");
          ObservableList<PropertyMap> products = ObjectLoader.getList(filter, "id", "company", "service","service_name","group","group_name","cost","nds","duration","recurrence","isNds=query:true","techPassport");
          Hronometr.stop("get");
          Platform.runLater(() -> {
            nodeCash.clear();
            Hronometr.start("gui");
            products.stream().filter(product -> product.isNotNull("group")).forEach(product -> insertGroupProduct(product));
            if(treeTypeProperty().getValue() == PROCESS_TO_GROUP)
              products.stream().filter(product -> product.isNull("group")).forEach(product -> insertProcessProduct(product));
            Hronometr.stop("gui");
            if(getScene() != null)
              getScene().getRoot().setCursor(Cursor.DEFAULT);
            nodeCash.clear();
            mementoProductMap = getCurrentProducts();
            initDataHandlers.forEach(eh -> eh.handle(new ActionEvent(FXProductTree.this, FXProductTree.this)));
          });
        }catch(Exception ex) {
          ex.printStackTrace();
          Platform.runLater(() -> {
            if(getScene() != null)
              getScene().getRoot().setCursor(Cursor.DEFAULT);
          });
        }
      });
    }
  }
  
  public List<PropertyMap> getCreatedProducts() {
    return getCurrentProducts().values().stream().filter(p -> p.getInteger("id") < 0).collect(Collectors.toList());
  }
  
  public List<PropertyMap> getRemovedProducts() {
    Set<Integer> ids = getCurrentProducts().keySet();
    return mementoProductMap.values().stream().filter(m -> {
      return !ids.contains(m.getInteger("id"));
    }).collect(Collectors.toList());
  }
  
  public List<PropertyMap> getUpdatedProducts() {
    TreeMap<Integer,PropertyMap> currentProducts = getCurrentProducts();
    return currentProducts.values().stream()
            .filter(m -> mementoProductMap.containsKey(m.getInteger("id")) && !m.equals(mementoProductMap.get(m.getInteger("id"))))
            .collect(Collectors.toList());
  }
  
  public TreeMap<Integer,PropertyMap> getCurrentProducts() {
    TreeMap<Integer,PropertyMap> map = new TreeMap<>();
    listItems().stream().forEach(item -> {
      
      if(isProcessProduct(item) && item.getValue().isNotNull("product"))
        map.put(item.getValue().getInteger("product"), PropertyMap.copy(item.getValue(), "duration","reccurence","cost","nds","isNds","techPassport","company","service").setValue("id", item.getValue().getInteger("product")));
      
      if(isGroupProduct(item))
        map.put(item.getValue().getInteger("id"), item.getValue().copy());
    });
    return map;
  }
  
  public TreeItem<PropertyMap> insertProcessProduct(PropertyMap product) {
    TreeItem<PropertyMap> processNode = getNode(getRoot(), rootType, product.getInteger(rootProp))
            .orElseGet(() -> insertNode(getRoot(), rootType, product.getInteger(rootProp)));
    
    if(processNode != null)
      processNode.getValue().setValue("product", product.getInteger("id")).copyFrom(product.getSimpleMap("duration","reccurence","cost","nds","isNds","techPassport","company","service"));
    return processNode;
  }
  
  public TreeItem<PropertyMap> insertGroupProduct(PropertyMap product) {
    TreeItem<PropertyMap> rootNode = getNode(getRoot(), rootType, product.getInteger(rootProp))
            .orElseGet(() -> insertNode(getRoot(), rootType, product.getInteger(rootProp)));
    
    TreeItem<PropertyMap> subNode = getNode(rootNode, subType, product.getInteger(subProp))
            .orElseGet(() -> insertNode(rootNode, subType, product.getInteger(subProp)));
    
    subNode.getValue().copyFrom(product).setValue("type", TreeItemType.PRODUCT);
    return subNode;
  }
  
  public TreeItem<PropertyMap> insertNode(TreeItem<PropertyMap> parentItem, TreeItemType nodeType, Integer id) {
    TreeItem<PropertyMap> node = null;
    
    PropertyMap n = nodeType == TreeItemType.GROUP ? groups.stream().filter(g -> g.getInteger("id").equals(id)).findFirst().orElse(null) : 
            processes.stream().filter(p -> p.getInteger("id").equals(id)).findFirst().orElse(null);
    
    if(n == null) {
      //if(nodeType == TreeItemType.GROUP)
        ObjectLoader.fillList(DBFilter.create(Service.class), processes, "id", "name", "parent");
      //else 
      ObjectLoader.fillList(DBFilter.create(Group.class),   groups,    "id", "name", "parent");
      
      n = nodeType == TreeItemType.GROUP ? groups.stream().filter(g -> g.getInteger("id").equals(id)).findFirst().orElse(null) : processes.stream().filter(p -> p.getInteger("id").equals(id)).findFirst().orElse(null);
    }
    
    if(n != null) {
      PropertyMap nn = n;

      TreeItem<PropertyMap> parent = nn.isNull("parent") ? parentItem : getNode(parentItem, nodeType, nn.getInteger("parent"))
              .orElseGet(() -> insertNode(parentItem, nodeType, nn.getInteger("parent")));

      PropertyMap p = n.copy().setValue("type", nodeType).setValue("company", companyProperty().getValue().getInteger("id"));

      if(nodeType == PROCESS)
        p.setValue("product", IDStore.createIntegerID("product")*-1)
                .setValue("service", p.getInteger("id"))
                .setValue("service_name", p.getString("name"));

      parent.getChildren().add(node = new TreeItem<>(p));

      nodeCash.put("node"+parentItem.getValue().getInteger("id")+nodeType+id, node);
    }

    return node;
  }
  
  public Optional<TreeItem<PropertyMap>> getNode(TreeItem<PropertyMap> parent, TreeItemType itemType, Integer id) {
    if(nodeCash.containsKey("node"+parent.getValue().getInteger("id")+itemType+id))
      return Optional.of(nodeCash.get("node"+parent.getValue().getInteger("id")+itemType+id));
    return listItems(parent).stream().filter(n -> n.getValue().containsKey("id") && n.getValue().isNotNull("id") && n.getValue().getInteger("id").equals(id) && n.getValue().getValue("type", TreeItemType.class) == itemType).findFirst();
  }
  
  public boolean isUpdate() {
    
    List<PropertyMap> cp = getCreatedProducts();
    List<PropertyMap> up = getUpdatedProducts();
    
    return !PropertyMap.equals(mementoProductMap,getCurrentProducts()) || !getCreatedProducts().isEmpty() || !getUpdatedProducts().isEmpty();
  }
}