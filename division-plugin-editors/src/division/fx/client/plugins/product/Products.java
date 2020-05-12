package division.fx.client.plugins.product;

import bum.editors.util.ObjectLoader;
import bum.interfaces.Group;
import bum.interfaces.Product;
import bum.interfaces.Service;
import division.fx.FXButton;
import division.fx.FXToolButton;
import division.fx.FXUtility;
import division.fx.PropertyMap;
import division.fx.client.plugins.FXPlugin;
import division.fx.client.plugins.product.FXProductTree.TreeItemType;
import static division.fx.client.plugins.product.FXProductTree.TreeItemType.GROUP;
import static division.fx.client.plugins.product.FXProductTree.TreeItemType.PROCESS;
import static division.fx.client.plugins.product.FXProductTree.TreeItemType.PRODUCT;
import division.fx.client.plugins.product.FXProductTree.TreeType;
import division.fx.controller.group.FXGroup;
import division.fx.controller.process.FXProcess;
import division.fx.dialog.FXD;
import division.fx.editor.FXTreeEditor;
import division.fx.table.Column;
import division.fx.table.FXDivisionTable;
import division.fx.table.FXDivisionTableCell;
import division.fx.tree.FXTree;
import division.fx.util.MsgTrash;
import division.util.IDStore;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import util.RemoteSession;

public class Products extends FXPlugin {
  private final FXDivisionTable<PropertyMap> serviceMapTable = new FXDivisionTable<>(true);
  private final FXButton addColumnButton = new FXButton(e -> addColumn(), "add-column-button");
  private final FXButton delColumnButton = new FXButton(e -> delColumn(), "del-column-button");
  private final FXButton addRowButton    = new FXButton(e -> addRow(), "add-row-button");
  private final FXButton delRowButton    = new FXButton(e -> delRow(), "del-row-button");
  private final FXButton copyButton      = new FXButton("Копировать", e -> copy(), "copy-button");
  private final FXButton pasteButton     = new FXButton("Вставить", e -> paste(), "paste-button");
  private final ToolBar  tools           = new ToolBar(addColumnButton, delColumnButton, addRowButton, delRowButton, new Separator(), copyButton, pasteButton);

  private final ObjectProperty<PropertyMap> productProperty   = new SimpleObjectProperty<>();
  
  private final TitledPane productDescription = new TitledPane("Описание", new Pane());
  private final TitledPane serviceMap         = new TitledPane("Технологический паспорт", new BorderPane(serviceMapTable, tools, null, null, null));
  
  private final Accordion accordion = new Accordion(productDescription, serviceMap);
  
  private final MenuButton createButton        = new MenuButton();
  private final MenuItem   createSectionButton = new MenuItem("Добавить раздел");
  private final MenuItem   createProductButton = new MenuItem("Создать продукт");
  
  private final FXToolButton      removeToolButton = new FXToolButton("Удалить", "remove-button");
  private final ChoiceBox<String> reverseBox       = new ChoiceBox<>(FXCollections.observableArrayList("В разрезе процессов","В разрезе объектов"));
  private final Label             productLabel     = new Label();
  private final ToolBar           productTools     = new ToolBar(createButton, removeToolButton, new Separator(), reverseBox, new Separator(), productLabel/*, findField*/);
  
  private final FXProductTree productTree  = new FXProductTree();
  
  private final SplitPane rootSplit = new SplitPane(new VBox(5, productTools, productTree), accordion);
  private final ObjectProperty<PropertyMap> companyProperty = new SimpleObjectProperty<>();
  
  
  private final ObservableList<PropertyMap> currentProducts     = FXCollections.observableArrayList();
  private final PropertyMap                 currentPassport = PropertyMap.create();

  public Products() {
    storeControls().addAll(getContent(), productTree);
    
    pluginProperty().setValue(true);
    
    activeProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(newValue)
        productTree.initData();
    });
    
  }
  
  public ObjectProperty<PropertyMap> companyProperty() {
    return companyProperty;
  }
  
  @Override
  public Node getContent() {
    return rootSplit;
  }
  
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    FXUtility.initCss(this);
    
    VBox.setVgrow(productTree, Priority.ALWAYS);
    
    productTree.companyProperty().bind(companyProperty());
    
    rootSplit.setOrientation(Orientation.VERTICAL);
    createButton.getStyleClass().add("add-menu-button");
    createButton.getItems().addAll(createSectionButton, createProductButton);
    
    productTree.getColumns().get(0).setText("");
    productTree.setShowRoot(false);
    
    productLabel.textProperty().bind(Bindings.createStringBinding(() -> {
      return productProperty().getValue() == null ? "" : 
              (productProperty().getValue().getString("service_name") + (productProperty().getValue().isNull("group_name") ? "" : (" "+productProperty().getValue().getString("group_name"))));
    }, productProperty()));
    
    serviceMapTable.getSelectionModel().setCellSelectionEnabled(true);
    serviceMapTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    serviceMapTable.setSortPolicy((TableView<PropertyMap> param) -> false);
    serviceMapTable.setRowFactory((TableView<PropertyMap> param) -> {
      TableRow<PropertyMap> row = new TableRow<>();
      row.setPrefHeight(100);
      return row;
    });
    
    productTree.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends TreeItem<PropertyMap>> observable, TreeItem<PropertyMap> oldValue, TreeItem<PropertyMap> newValue) -> 
            changeProductSelected());
    
    createSectionButton.disableProperty().bind(Bindings.createBooleanBinding(() -> productTree.companyProperty().getValue() == null, 
            productTree.getSelectionModel().selectedItemProperty(), productTree.companyProperty()));
    
    createProductButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
      TreeItem<PropertyMap> item = productTree.getSelectionModel().getSelectedItem();
      if(item == null)
        return true;
      TreeType treeType = productTree.treeTypeProperty().getValue();
      TreeItemType itemType = item.getValue().getValue("type", TreeItemType.class);
      return
              !(item.getChildren().isEmpty() ||
              itemType == PRODUCT ||
              treeType == TreeType.PROCESS_TO_GROUP && itemType == GROUP ||
              treeType == TreeType.GROUP_TO_PROCESS && itemType == PROCESS ||
              productTree.listItems(item, "type", itemType, false).isEmpty() && (treeType == TreeType.PROCESS_TO_GROUP && itemType == PROCESS || treeType == TreeType.GROUP_TO_PROCESS && itemType == GROUP));
    }, productTree.getSelectionModel().selectedItemProperty()));
    
    createSectionButton.setOnAction(e -> createSection());
    createProductButton.setOnAction(e -> createProduct());
    removeToolButton.setOnAction(e -> remove());
    
    reverseBox.getSelectionModel().selectFirst();
    productTree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    
    reverseBox.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
      if(!newValue.equals(oldValue))
        save();
      productTree.treeTypeProperty().setValue(newValue.intValue() <= 0 ? TreeType.PROCESS_TO_GROUP : TreeType.GROUP_TO_PROCESS);
    });
    
    productProperty().bind(Bindings.createObjectBinding(() -> {
      if(productTree.getSelectionModel().getSelectedItem() != null) {
        
        if(productTree.isProcessProduct(productTree.getSelectionModel().getSelectedItem()) && productTree.getSelectionModel().getSelectedItem().getValue().isNotNull("product"))
          return PropertyMap.copy(productTree.getSelectionModel().getSelectedItem().getValue(), "service")
                  .setValue("company", companyProperty().getValue().getInteger("id"))
                  .setValue("id", productTree.getSelectionModel().getSelectedItem().getValue().getInteger("product"))
                  .setValue("service_name", productTree.getSelectionModel().getSelectedItem().getValue().getString("name"));
        
        if(productTree.isGroupProduct(productTree.getSelectionModel().getSelectedItem()))
          return PropertyMap.copy(productTree.getSelectionModel().getSelectedItem().getValue(), "id","service","service_name","group","group_name")
                  .setValue("company", companyProperty().getValue().getInteger("id"));
      }
      return null;
    }, productTree.getSelectionModel().selectedItemProperty()));
  }
  
  @Override
  protected boolean closing() {
    save();
    productTree.getSelectionModel().clearSelection();
    return true;
  }
  
  public ObjectProperty<PropertyMap> productProperty() {
    return productProperty;
  }
  
  private boolean save() {
    String passport = getPassport().toJson();
    getSelectedProducts().stream().forEach(p -> p.setValue("techPassport", passport == null || passport.equals("") ? null : passport));
    
    if(productTree.isUpdate()) {
      if(FXD.showWait("Сохранение...", getContent(), "Сохраненить изменения?", FXD.ButtonType.YES, FXD.ButtonType.NO).orElseGet(() -> FXD.ButtonType.NO) == FXD.ButtonType.YES) {
        RemoteSession session = null;
        try {
          session = ObjectLoader.createSession(false);
          
          PropertyMap passports = PropertyMap.create();
          
          for(PropertyMap p:productTree.getCreatedProducts()) {
            p.setValue("id", session.createObject(Product.class, p.setValue("company", productTree.companyProperty().getValue().getInteger("id")).getSimpleMap("company","group","service","cost","duration","recurrence","nds")));
            passports.getList(p.getValue("techPassport","{}"), Integer.class).add(p.getInteger("id"));
          }
          
          for(PropertyMap p:productTree.getUpdatedProducts()) {
            session.saveObject(Product.class, p.setValue("company", productTree.companyProperty().getValue().getInteger("id")).getSimpleMap("id","company","group","service","cost","duration","recurrence","nds"));
            passports.getList(p.getString("techPassport"), Integer.class).add(p.getInteger("id"));
          }
          
          for(String pass:passports.keySet())
            session.executeUpdate(Product.class, new String[]{"techPassport"}, new Object[]{pass}, passports.getList(pass, Integer.class).toArray(new Integer[0]));
          
          List<PropertyMap> removeList = productTree.getRemovedProducts();
          if(!removeList.isEmpty())
            session.toTypeObjects(Product.class, PropertyMap.getArrayFromList(removeList, "id", Integer.class), Product.Type.ARCHIVE);
          
          ObjectLoader.commitSession(session);
          return true;
        }catch(Exception ex) {
          ObjectLoader.rollBackSession(session);
          MsgTrash.out(ex);
        }
      }
    }
    return false;
  }
  
  @Override
  public void changeCompany(Integer[] company, boolean forceInit) {
    companyProperty().setValue(company == null || company.length == 0 ? null : PropertyMap.create().setValue("id", company[0]));
  }
  
  @Override
  public void dispose() {
    super.dispose();
  }
  
  @Override
  public void start() {
    show("Продукты");
  }
  
  private void addColumn() {
    addColumn((serviceMapTable.getColumns().size()+1)+"");
  }
  
  private void changeColumnName(Column column, String text) {
    String oldName = column.getColumnName();
    column.setColumnName(text);
    column.getTableView().getItems().stream().forEach(r -> ((PropertyMap)r).setValue(text, ((PropertyMap)r).getValue(oldName)));
  }
  
  private void addColumn(String columnText) {
    Column column = Column.create(columnText, true, true);
        
    VBox box = new VBox(new Label(column.getColumnName()));
    box.setMaxWidth(Double.MAX_VALUE);
    box.setAlignment(Pos.CENTER);

    box.setOnMousePressed(e -> {
      TextField text = new TextField(column.getColumnName());
      text.selectAll();
      text.setOnAction(ev -> {
        box.getChildren().setAll(new Label(text.getText()));
        changeColumnName(column, text.getText());
      });
      text.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
        if(!newValue) {
          box.getChildren().setAll(new Label(text.getText()));
          changeColumnName(column, text.getText());
        }
      });
      Platform.runLater(() -> text.requestFocus());
      box.getChildren().setAll(text);
    });

    column.setText("");
    column.setGraphic(box);

    column.setCellFactory((Object param) -> {
      FXDivisionTableCell cell = new FXDivisionTableCell() {
        @Override
        public void startEdit() {
          //Platform.runLater(() -> {
            if(getRowItem() != null) {
              TextArea text = new TextArea(toString(getItem()));
              text.minHeightProperty().bind(getTableRow().heightProperty().subtract(7));
              text.maxHeightProperty().bind(getTableRow().heightProperty().subtract(7));
              text.prefHeightProperty().bind(getTableRow().heightProperty().subtract(7));
              /*text.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                if(!newValue) {
                  commitEdit(text.getText() == null ? "" : text.getText());
                  //serviceMapTable.refresh();
                  //cancelEdit();
                }
              });*/
              setComponent(text);
              super.startEdit();
            }
          //});
        }
      };
      cell.setWrapText(true);
      //cell.setOnMouseClicked(e -> cell.startEdit());
      /*cell.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
        if(!newValue)
          cell.cancelEdit();
      });*/
      return cell;
    });
    serviceMapTable.getColumns().add(column);
    column.setPrefWidth(200);
    
    if(serviceMapTable.getItems().isEmpty())
      addRow();
  }
  
  private void delColumn() {
    if(!serviceMapTable.getSelectionModel().getSelectedCells().isEmpty()) {
      List<TableColumn> columns = new ArrayList<>();
      serviceMapTable.getSelectionModel().getSelectedCells().stream().forEach(tp -> {
        columns.add(tp.getTableColumn());
        serviceMapTable.getSourceItems().stream().forEach(r -> r.remove(((Column)tp.getTableColumn()).getColumnName()));
      });
      serviceMapTable.getColumns().removeAll(columns);
      if(serviceMapTable.getColumns().isEmpty())
        serviceMapTable.clear();
    }
  }
  
  private void addRow() {
    serviceMapTable.getSourceItems().add(PropertyMap.create());
    if(serviceMapTable.getColumns().isEmpty())
      addColumn();
  }
  
  private void delRow() {
    PropertyMap[] p = serviceMapTable.getSelectionModel().getSelectedItems().toArray(new PropertyMap[0]);
    serviceMapTable.getSelectionModel().clearSelection();
    for(int i=0;i<p.length;i++)
      serviceMapTable.getSourceItems().remove(p[i]);
  }
  
  private void copy() {
    System.getProperties().put("product-passport", getPassport().toJson());
  }
  
  private void paste() {
    if(FXD.showWait("Внимание!!!", getContent(), "Уверены?", FXD.ButtonType.YES, FXD.ButtonType.NO).orElseGet(() -> FXD.ButtonType.NO) == FXD.ButtonType.YES) {
      clearPassport();
      currentProducts.forEach(p -> p.setValue("techPassport", System.getProperties().get("product-passport")));
      setPassport();
    }
  }
  
  
  
  
  /**
   * Если паспорт изменился, то
   *   Если продукт один, то сохранить
   *   Если продукт не один и паспорт не пустой, то сохранить с подтверждением
   *   Если продукт не один и паспорт пустой, то ничего не делать
   */
  private void changeProductSelected() {
    if(isUpdatePassport()) {
      PropertyMap passport = getPassport();
      String passStr = passport.toJson();
      //currentProducts.forEach(p -> p.setValue("techPassport", passStr));
      if(!currentProducts.isEmpty() &&
              (currentProducts.size() == 1 ||
              currentProducts.size() > 1  &&
              !passport.getList("columns", String.class).isEmpty() &&
              FXD.showWait("Внимание", getContent(), "Для всех продуктов?", FXD.ButtonType.YES, FXD.ButtonType.NO).orElseGet(() -> FXD.ButtonType.NO) == FXD.ButtonType.YES))
        currentProducts.forEach(p -> p.setValue("techPassport", passStr));
    }
    
    currentProducts.setAll(getSelectedProducts());
    setPassport();
  }
  
  private ObservableList<PropertyMap> getSelectedProducts() {
    return FXCollections.observableArrayList(productTree.getSelectionModel().getSelectedItems()
            .stream().filter(it -> it != null)
            .flatMap(p -> FXTree.ListItems(p, "type", TreeItemType.PRODUCT, true).stream())
            .map(item -> item.getValue())
            .collect(Collectors.toList()));
  }
  
  private synchronized void clearPassport() {
    serviceMapTable.clear();
    serviceMapTable.getColumns().clear();
    currentPassport.clear();
  }
  
  public synchronized PropertyMap getPassport() {
    ObservableList<String> columns = FXCollections.observableArrayList(serviceMapTable.getColumns().stream().map(c -> ((Column)c).getColumnName()).collect(Collectors.toList()));
    return PropertyMap.create().setValue("columns", columns)
            .setValue("rows", FXCollections.observableArrayList(serviceMapTable.getItems().stream().map(r -> PropertyMap.copy(r, columns.toArray(new String[0]))).collect(Collectors.toList())));
  }
  
  private synchronized void setPassport() {
    clearPassport();
    if(!currentProducts.isEmpty() && currentProducts.stream().filter(p -> !Objects.equals(currentProducts.get(0).getValue("techPassport"), p.getValue("techPassport"))).findFirst().orElseGet(() -> null) == null) {
      currentPassport.copyFrom(PropertyMap.fromJson(currentProducts.get(0).getString("techPassport")));
      currentPassport.getList("columns", String.class).stream().forEach(c -> addColumn(c));
      serviceMapTable.getSourceItems().setAll(PropertyMap.copyList(currentPassport.getList("rows")));
    }
  }
  
  private boolean isUpdatePassport() {
    return !currentPassport.equals(getPassport());
  }
  
  
  
  
  
  
  
  
  private void remove() {
    TreeItem<PropertyMap> item = productTree.getSelectionModel().getSelectedItem();
    if(item != null && FXD.showWait("", getContent(), "Удалить выделенные объекты?", FXD.ButtonType.YES, FXD.ButtonType.NO).orElseGet(() -> FXD.ButtonType.NO) == FXD.ButtonType.YES) {
      TreeItem<PropertyMap> parent = item.getParent();
      parent.getChildren().remove(item);
      while(parent != null && parent.getChildren().isEmpty()) {
        TreeItem<PropertyMap> p = parent;
        parent = parent.getParent();
        productTree.getSelectionModel().select(parent);
        if(parent != null)
          parent.getChildren().remove(p);
      }
    }
  }
  
  private void createSection() {
    TreeItem<PropertyMap> item = productTree.getSelectionModel().getSelectedItem();
    TreeType treeType = productTree.treeTypeProperty().getValue();
    // Вычисляем необходимое дерево для создания продукта
    FXTreeEditor treeEditor = treeType == TreeType.GROUP_TO_PROCESS ? new FXTreeEditor(Group.class, FXGroup.class) : new FXTreeEditor(Service.class, FXProcess.class);
    treeEditor.titleProperty().setValue(treeType == TreeType.PROCESS_TO_GROUP ? "Объекты" : "Процессы");
    // Если treeItem не последний, то раскрываем в дереве ветку
    if(item != null) {
      while(treeType == TreeType.PROCESS_TO_GROUP && item.getValue().getValue("type", TreeItemType.class) != PROCESS || treeType == TreeType.GROUP_TO_PROCESS && item.getValue().getValue("type", TreeItemType.class) != GROUP)
        item = item.getParent();
      Integer selectedId = item.getValue().getInteger("id");
      treeEditor.addInitDataListener((event) -> {
        TreeItem<PropertyMap> treeItem = treeEditor.getNodeObject(selectedId);
        if(treeItem != null) {
          treeEditor.getTree().getSelectionModel().select(treeItem);
          treeItem.setExpanded(true);
          treeEditor.getTree().scrollTo(treeEditor.getTree().getRow(treeItem));
        }
      });
    }
    treeEditor.getTree().setShowRoot(false);
    treeEditor.checkBoxTreeProperty().setValue(true);
    treeEditor.initData();
    List<PropertyMap> objects = treeEditor.getObjects(getContent(), "Выберите "+(treeType == TreeType.GROUP_TO_PROCESS ? "объект" : "процесс"));
    if(!objects.isEmpty())
      for(TreeItem<PropertyMap> o:treeEditor.getCheckedItems().stream().filter(p -> p.getChildren().isEmpty()).collect(Collectors.toList())) {
        TreeItem<PropertyMap> node = productTree.insertNode(productTree.getRoot(), treeType == TreeType.GROUP_TO_PROCESS ? GROUP : PROCESS, o.getValue().getInteger("id"));
        productTree.getSelectionModel().select(node);
        productTree.scrollTo(productTree.getRow(node));
      }
  }
  
  private void createProduct() {
    TreeItem<PropertyMap> item = productTree.getSelectionModel().getSelectedItem();
    if(item != null) {
      TreeType treeType = productTree.treeTypeProperty().getValue();
      TreeItemType itemType = item.getValue().getValue("type", TreeItemType.class);
      // Вычисляем необходимое дерево для создания продукта
      FXTreeEditor treeEditor = treeType == TreeType.GROUP_TO_PROCESS ? new FXTreeEditor(Service.class, FXProcess.class) : new FXTreeEditor(Group.class, FXGroup.class);
      treeEditor.titleProperty().setValue(treeType == TreeType.PROCESS_TO_GROUP ? "Процессы" : "Объекты");

        // Если treeItem не последний, то раскрываем в дереве ветку
      if(itemType == PRODUCT
              || itemType == GROUP && treeType == TreeType.PROCESS_TO_GROUP
              || itemType == PROCESS && treeType == TreeType.GROUP_TO_PROCESS) {
        treeEditor.addInitDataListener((event) -> {
          TreeItem<PropertyMap> treeItem = treeEditor.getNodeObject(itemType == PRODUCT ? item.getParent().getValue().getInteger("id") : item.getValue().getInteger("id"));
          if(treeItem != null) {
            treeEditor.getTree().getSelectionModel().select(treeItem);
            treeItem.setExpanded(true);
            treeEditor.getTree().scrollTo(treeEditor.getTree().getRow(treeItem));
          }
        });
      }
      treeEditor.getTree().setShowRoot(false);
      treeEditor.checkBoxTreeProperty().setValue(true);
      treeEditor.initData();
      Collection<PropertyMap> objects = treeEditor.getObjects(getContent(), "Выберите "+(treeType == TreeType.GROUP_TO_PROCESS ? "процесс" : "объект"));
      if(!objects.isEmpty()) {
        TreeItem<PropertyMap> parent = item;
        // Ищем родителя с противоположным типом
        while(treeType == TreeType.PROCESS_TO_GROUP && parent.getValue().getValue("type", TreeItemType.class) != PROCESS || 
                treeType == TreeType.GROUP_TO_PROCESS && parent.getValue().getValue("type", TreeItemType.class) != GROUP)
          parent = parent.getParent();

        for(TreeItem<PropertyMap> o:treeEditor.getCheckedItems().stream().filter(p -> p.getChildren().isEmpty()).collect(Collectors.toList())) {
          if(productTree.getNode(
                  PropertyMap.create()
                          .setValue("type", PRODUCT)
                          .setValue("service", treeType == TreeType.PROCESS_TO_GROUP ? parent.getValue().getValue("id") : o.getValue().getValue("id"))
                          .setValue("group", treeType == TreeType.PROCESS_TO_GROUP ? o.getValue().getValue("id") : parent.getValue().getValue("id"))
                          .getSimpleMap()) == null) {
            
            PropertyMap product = PropertyMap.create()
                    .setValue("service"     , treeType == TreeType.PROCESS_TO_GROUP ? parent.getValue().getValue("id") : o.getValue().getValue("id"))
                    .setValue("service_name", treeType == TreeType.PROCESS_TO_GROUP ? parent.getValue().getValue("name") : o.getValue().getValue("name"))
                    .setValue("group"       , treeType == TreeType.PROCESS_TO_GROUP ? o.getValue().getValue("id") : parent.getValue().getValue("id"))
                    .setValue("group_name"  , treeType == TreeType.PROCESS_TO_GROUP ? o.getValue().getValue("name") : parent.getValue().getValue("name"))
                    .setValue("id"          , -1*IDStore.createIntegerID("product"))
                    .setValue("company"     , companyProperty().getValue())
                    .setValue("nds"         , BigDecimal.valueOf(18))
                    .setValue("type"        , PRODUCT);
            
            TreeItem<PropertyMap> it = productTree.insertGroupProduct(product);
          }
        }
        parent.setExpanded(true);
        productTree.scrollTo(productTree.getRow(parent));
        productTree.getSelectionModel().select(parent);
      }
    }
  }
}