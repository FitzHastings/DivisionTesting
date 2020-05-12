package division.fx.controller.contract;

import bum.interfaces.Service;
import bum.interfaces.XMLContractTemplate;
import client.util.ObjectLoader;
import division.fx.PropertyMap;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.editor.FXTableEditor;
import division.fx.gui.FXStorable;
import division.fx.table.Column;
import java.util.Arrays;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import static mapping.MappingObject.Type.CURRENT;

public class ContractTemplateScene implements FXStorable {
  private final FXTableEditor templateTable = new FXTableEditor(
          XMLContractTemplate.class, 
          null, 
          Arrays.asList(
                  "id",
                  "sellerNickname",
                  //"seller-nick-name=query:(select name from [SellerNickName] where id=[XMLContractTemplate(sellerNickname)])",
                  //"Агент=query:(select name from [SellerNickName] where id=[XMLContractTemplate(sellerNickname)])",
                  "customerNickname",
                  //"customer-nick-name=query:(select name from [CompanyNickname] where id=[XMLContractTemplate(customerNickname)])",
                  //"Контрагент=query:(select name from [CompanyNickname] where id=[XMLContractTemplate(customerNickname)])",
                  "processes"), 
          CURRENT, 
          false, 
          Column.create("Тип договора", "name"), 
          Column.create("Длительность", "duration"), 
          Column.create("Стороны")
                  .addColumn("Агент","seller-nick-name=query:(select name from [SellerNickName] where id=[XMLContractTemplate(sellerNickname)])")
                  .addColumn("Контрагент","customer-nick-name=query:(select name from [CompanyNickname] where id=[XMLContractTemplate(customerNickname)])"));
  
  private Label descriptionLabel = new Label("Описание");
  private TitleBorderPane descriptionPanel = new TitleBorderPane(descriptionLabel);
  private TextArea        descriptionText  = new TextArea();
  
  private Label processLabel = new Label("Процессы");
  private TitleBorderPane processPanel     = new TitleBorderPane(processLabel);
  private FXTableEditor   processTable     = new FXTableEditor(Service.class, null, Column.create("Процесс", "name"));
  
  private final SplitPane panel            = new SplitPane(descriptionPanel, processPanel);
  
  private Label rootLabel = new Label("Выберите тип создаваемого договора");
  private TitleBorderPane rootTitle        = new TitleBorderPane(rootLabel);
  
  private final SplitPane rootSplit = new SplitPane(templateTable, panel);
  
  public ContractTemplateScene() {
    templateTable.setRemovable(false);
    
    rootTitle.setCenter(rootSplit);
    
    descriptionPanel.setCenter(descriptionText);
    descriptionText.setEditable(false);
    descriptionText.textProperty().bind(Bindings.createStringBinding(() -> 
            templateTable.selectedItemProperty().getValue() == null ? "" : 
            templateTable.selectedItemProperty().getValue().getString("description"), 
            templateTable.selectedItemProperty()));
    VBox.setVgrow(descriptionPanel, Priority.ALWAYS);
    
    processTable.setRemovable(false);
    processPanel.setCenter(processTable);
    VBox.setVgrow(processPanel, Priority.ALWAYS);
    
    processTable.getTable().itemsProperty().bind(Bindings.createObjectBinding(() -> 
            templateTable.selectedItemProperty().getValue() == null ? FXCollections.emptyObservableList() : 
            ObjectLoader.getList(Service.class, templateTable.selectedItemProperty().getValue().getArray("processes", Integer.class), "id","name"), 
            templateTable.selectedItemProperty()));
    
    panel.setOrientation(Orientation.VERTICAL);
  }
  
  public Region getRoot() {
    return rootTitle;
  }
  
  public void initData() {
    templateTable.initData();
  }
  
  public List<PropertyMap> get() {
    return templateTable.getSelectedObjects();
  }
  
  public ReadOnlyObjectProperty<PropertyMap> valueProperty() {
    return templateTable.selectedItemProperty();
  }

  @Override
  public ObservableList storeControls() {
    return FXCollections.observableArrayList(templateTable, rootSplit, processTable, descriptionText);
  }
}