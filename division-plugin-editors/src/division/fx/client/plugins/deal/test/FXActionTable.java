package division.fx.client.plugins.deal.test;

import bum.editors.util.DivisionTarget;
import bum.interfaces.Deal;
import division.fx.FXToolButton;
import division.fx.PropertyMap;
import division.fx.controller.documents.CreatedDocumentTable;
import division.fx.gui.FXDisposable;
import division.fx.gui.FXStorable;
import division.fx.table.Column;
import division.fx.table.FXDivisionTable;
import division.fx.table.filter.DateFilter;
import division.fx.table.filter.ListFilter;
import division.fx.table.filter.TextFilter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class FXActionTable extends BorderPane implements FXStorable, FXDisposable {
  private final MenuButton   actionAdd     = new MenuButton();
  private final MenuItem     start         = new MenuItem("СТАРТ");
  private final MenuItem     dispatch      = new MenuItem("ОТГРУЗКА");
  private final MenuItem     pay           = new MenuItem("ОПЛАТА");
  private final FXToolButton actionDel     = new FXToolButton("Удалить событие", "remove-button");
  private final ToolBar      actionToolBar = new ToolBar(actionAdd, actionDel);
  
  private final FXDivisionTable<PropertyMap> actions = new FXDivisionTable<>(
          Column.create("Событие", new ListFilter("Событие")),
          Column.create("дата", "df",    new DateFilter("дата")),
          Column.create("дебет",   new TextFilter()),
          Column.create("кредит",  new TextFilter()));
  private CreatedDocumentTable documents = new CreatedDocumentTable();
  
  private SplitPane split = new SplitPane(new VBox(actionToolBar, actions), documents);
  
  private final ObjectProperty<List<Integer>>     dealsProperty         = new SimpleObjectProperty();
  private final ObjectProperty<List<PropertyMap>> dealPositionsProperty = new SimpleObjectProperty(Arrays.asList());
  
  DivisionTarget dealTarget;

  public FXActionTable() {
    setCenter(split);
    VBox.setVgrow(actions, Priority.ALWAYS);
    
    dealsProperty.bind(Bindings.createObjectBinding(() -> dealPositionsProperty().getValue().stream().map(dp -> dp.getInteger("deal")).collect(Collectors.toList()), dealPositionsProperty));
    
    dealTarget = DivisionTarget.create(Deal.class, (DivisionTarget target, String type, Integer[] ids, PropertyMap objectEventProperty) -> {
      boolean is = false;
      for(Integer id:ids) {
        if(dealsProperty().getValue().contains(id)) {
          is = true;
          break;
        }
      }
      if(is)
        initData();
    });
    
    dealPositionsProperty().addListener((ObservableValue<? extends List<PropertyMap>> observable, List<PropertyMap> oldValue, List<PropertyMap> newValue) -> {
      clear();
      if(newValue != null && !newValue.isEmpty())
        initData();
    });
    
    actions.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> initDataDocuments());
  }

  @Override
  public List<FXDisposable> disposeList() {
    return FXCollections.observableArrayList(documents/*, dealTarget*/);
  }

  @Override
  public List<Node> storeControls() {
    return FXCollections.observableArrayList(actions, documents);
  }

  @Override
  public void finaly() {
    dealPositionsProperty.setValue(null);
    split.getItems().clear();
    actions.clear();
    documents.clearData();
    dealTarget = null;
  }
  
  public ReadOnlyObjectProperty<List<Integer>> dealsProperty() {
    return dealsProperty;
  }

  public ObjectProperty<List<PropertyMap>> dealPositionsProperty() {
    return dealPositionsProperty;
  }

  public void clear() {
    actions.clear();
    documents.clearData();
  }

  private void initData() {
  }

  private void initDataDocuments() {
  }
}