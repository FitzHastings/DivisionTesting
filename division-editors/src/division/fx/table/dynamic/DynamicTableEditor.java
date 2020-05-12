package division.fx.table.dynamic;

import division.fx.PropertyMap;
import division.fx.dialog.DialogButtonListener;
import division.fx.dialog.FXDialog;
import division.fx.editor.GLoader;
import division.fx.table.Column;
import division.fx.table.FXDivisionTable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;

public class DynamicTableEditor {
  private final Button addToolButton = new Button();
  private final Button removeToolButton = new Button();
  private final ToggleButton editToolButton = new ToggleButton();
  private final ToolBar tool = new ToolBar(addToolButton, editToolButton, removeToolButton);
  private FXDivisionTable<PropertyMap> table;
  private final VBox pane = new VBox(5, tool);
  
  private ObservableList<PropertyMap> items = FXCollections.observableArrayList();
  
  private ObservableList<PropertyMap> memntoItems;
  private final String name;
  
  public DynamicTableEditor(String name, ObservableList<PropertyMap> items, Column... columns) {
    this.name = name;
    this.items = items;
    table = new FXDivisionTable(columns);
    table.setEditable(true);
    bind(items);
    memntoItems = PropertyMap.copyList(items);
    pane.getChildren().add(table);
    VBox.setVgrow(table, Priority.ALWAYS);
    table.editableProperty().bind(editToolButton.selectedProperty());
    
    addToolButton.getStyleClass().add("addToolButton");
    editToolButton.getStyleClass().add("editToolButton");
    removeToolButton.getStyleClass().add("removeToolButton");
    
    table.editingCellProperty().addListener((ObservableValue<? extends TablePosition<PropertyMap, ?>> observable, TablePosition<PropertyMap, ?> oldValue, TablePosition<PropertyMap, ?> newValue) -> {
      if(newValue != null)
        System.out.println("edit "+newValue.getRow()+" row");
    });
    
    addToolButton.setOnAction(t -> {
      PropertyMap p = PropertyMap.create().setValue("status", "created");
      for(TableColumn c:table.getAllColumns(null, true))
        ((Column)c).setDefault(p);
      items.add(0,p);
      bind(items);
      table.getSelectionModel().select(items.indexOf(p));
      table.scrollTo(p);
    });
    
    editToolButton.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(newValue)
        edit();
    });
    
    removeToolButton.setOnAction(t -> {
      int index = table.getSelectionModel().getSelectedIndex();
      if("created".equals(table.getSelectionModel().getSelectedItem().getValue("status")))
        items.remove(table.getSelectionModel().getSelectedItem());
      else table.getSelectionModel().getSelectedItem().setValue("status", "removed");
      index = table.getItems().isEmpty() ? -1 : index == table.getItems().size() ? index-1 : index;
      if(index >= 0)
        table.getSelectionModel().select(index);
    });
    
    table.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> editToolButton.setSelected(false));
  }
  
  private void bind(ObservableList<PropertyMap> items) {
    table.itemsProperty().unbind();
    ObservableList<Property> deps = FXCollections.observableArrayList();
    items.stream().forEach(it -> deps.add(it.get("status")));
    table.itemsProperty().bind(Bindings.createObjectBinding(() -> items.filtered(it -> !"removed".equals(it.getValue("status"))), deps.toArray(new Property[0])));
  }
  
  private void edit() {
    table.edit(table.getSelectionModel().getSelectedIndex(), table.getColumns().get(table.getSelectionModel().getSelectedCells().get(0).getColumn()));
  }
  
  public PropertyMap show(Node parent) {
    return show(parent, null, null);
  }
  
  public PropertyMap show(Node parent, DialogButtonListener dialogButtonListener) {
    return show(parent, dialogButtonListener, null);
  }
  
  public PropertyMap show(Node parent, EventHandler<WindowEvent> windowHandler) {
    return show(parent, null, windowHandler);
  }
  
  public PropertyMap show(Node parent, DialogButtonListener dialogButtonListener, EventHandler<WindowEvent> windowHandler) {
    PropertyMap returnPropertyMap = null;
    GLoader.load(name, pane, table);
    if(FXDialog.show(parent, pane, "Список подразделений", FXDialog.ButtonGroup.OK, dialogButtonListener, (WindowEvent event) -> {
      if(event.getEventType() == WindowEvent.WINDOW_SHOWING) {
        table.setOnMouseClicked(ev -> {
          if(ev.getClickCount() == 2 && !editToolButton.isSelected()) {
            ((FXDialog)event.getSource()).setResult(FXDialog.Type.OK);
            ((FXDialog)event.getSource()).close();
          }
        });
      }
      if(event.getEventType() == WindowEvent.WINDOW_CLOSE_REQUEST) {
        GLoader.store(name, pane, table);
      }
      if(windowHandler != null)
        windowHandler.handle(event);
    }) == FXDialog.Type.OK)
      if(table.getSelectionModel().getSelectedItem() != null) 
        returnPropertyMap = table.getSelectionModel().getSelectedItem();
    GLoader.store(name, pane, table);
    return returnPropertyMap;
  }
  
  public ObservableList<PropertyMap> mementoItems() {
    return memntoItems;
  }
  
  public ObservableList<PropertyMap> items() {
    return items;
  }
  
  public void rollback() {
    PropertyMap item = table.getSelectionModel().getSelectedItem();
    items.setAll(memntoItems);
    if(item!= null)
      items.stream().filter(p -> p.getValue("id").equals(item.getValue("id"))).forEach(p -> table.getSelectionModel().select(p));
  }
  
  public boolean isUpdate() {
    return !memntoItems.equals(table.getItems());
  }
}