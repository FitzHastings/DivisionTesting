package division.fx.controller.company;

import TextFieldLabel.TextFieldLabel;
import bum.editors.util.ObjectLoader;
import bum.interfaces.CFC;
import bum.interfaces.Company;
import bum.interfaces.CompanyPartition;
import division.fx.ChoiseLabel.ChoiceLabel;
import division.fx.PropertyMap;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.dialog.FXD;
import division.fx.editor.FXTableEditor;
import division.fx.editor.FXTreeEditor;
import division.fx.gui.FXGLoader;
import division.fx.table.Column;
import division.fx.table.filter.TextFilter;
import java.util.Optional;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.ArrayUtils;
import util.filter.local.DBFilter;

public class FXCompanySelector extends BorderPane {
  private final Label                       innkpp            = new Label();
  private final TextFieldLabel<String>      inFaceText        = new TextFieldLabel("В лице");
  private final TextFieldLabel<String>      reasonText        = new TextFieldLabel("действующего на основании");
  private final ChoiceLabel<PropertyMap>    partitionSelector = new ChoiceLabel("Подразделение");
  private final ChoiceLabel<PropertyMap>    cfcSelector       = new ChoiceLabel("Группа");
  private final ObjectProperty<PropertyMap> companyProperty   = new SimpleObjectProperty<>();
  private final HBox                        selectors         = new HBox();
  private final StringProperty              titleProperty     = new SimpleStringProperty("Сторона");
  
  private final Label                       titleLabel        = new Label();
  private final VBox                        data              = new VBox(selectors);
  private final TitleBorderPane             titlePane         = new TitleBorderPane(data, titleLabel);
  
  public FXCompanySelector(String title) {
    this(title, true, true, true, true, true, true);
  }

  public FXCompanySelector(String title, boolean companySelector, boolean innkppVisible, boolean inFaceVisible, boolean reasonVisible, boolean cfcVisible, boolean partitionVisible) {
    super();
    
    titleLabel.setDisable(!companySelector);
    
    if(inFaceVisible)
      data.getChildren().add(0,inFaceText);
    
    if(reasonVisible)
      data.getChildren().add(0,reasonText);
    
    if(innkppVisible)
      data.getChildren().add(0,innkpp);
    
    if(partitionVisible)
      selectors.getChildren().add(0, partitionSelector);
    
    if(cfcVisible)
      selectors.getChildren().add(0, cfcSelector);
    
    HBox.setHgrow(cfcSelector, Priority.ALWAYS);
    HBox.setHgrow(partitionSelector, Priority.ALWAYS);
    
    titleProperty.setValue(title);
    
    titleLabel.setOnMouseClicked(e -> selectCompany());
    setCenter(titlePane);
    
    titleLabel.getStyleClass().add("link-label");
    titleLabel.textProperty().bind(Bindings.createStringBinding(() -> 
            titleProperty.getValue()+(companyProperty().getValue() == null ? "..." : (": "+companyProperty().getValue().getString("ownership")+" "+companyProperty().getValue().getString("name"))), 
            companyProperty(), titleProperty()));
    
    innkpp.textProperty().bind(Bindings.createStringBinding(() -> companyProperty().getValue() == null ? "" : ("ИНН "+companyProperty().getValue().getString("inn")) + (partitionProperty().getValue() == null || partitionProperty().getValue().isNull("kpp") ? "" : ("     КПП "+partitionProperty().getValue().getString("kpp"))), companyProperty(), partitionProperty()));
    
    companyProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      partitionSelector.itemsProperty().getValue().clear();
      cfcSelector.itemsProperty().getValue().clear();
      
      inFaceText.valueProperty().setValue("");
      reasonText.valueProperty().setValue("");
      
      if(newValue != null) {
        inFaceText.valueProperty().setValue(newValue.isNull("chifPlaceName") ? "" : (newValue.getString("chifPlaceName")+(newValue.isNull("chiefName") ? "" : (" "+newValue.getString("chiefName")))));
        reasonText.valueProperty().setValue(newValue.isNull("businessReason") ? "" : newValue.getString("businessReason"));
        partitionSelector.itemsProperty().getValue().setAll(ObjectLoader.getList(DBFilter.create(CompanyPartition.class).AND_EQUAL("type", CompanyPartition.Type.CURRENT).AND_EQUAL("tmp", false).AND_EQUAL("company", newValue.getInteger("id")), "id","name","mainPartition","kpp"));
        partitionSelector.itemsProperty().getValue().stream().filter(cp -> cp.is("mainPartition")).forEach(cp -> partitionProperty().setValue(cp));
        cfcSelector.itemsProperty().getValue().setAll(ObjectLoader.getList(DBFilter.create(CFC.class).AND_EQUAL("type", CFC.Type.CURRENT).AND_EQUAL("tmp", false).AND_IN("id", newValue.getArray("cfcs", Integer.class)), "id", "name"));
        
        if(cfcProperty().getValue() != null && cfcSelector.itemsProperty().getValue().filtered(c -> c.getInteger("id").equals(cfcProperty().getValue().getInteger("id"))).isEmpty())
          cfcProperty().setValue(null);
        
        if(cfcProperty().getValue() != null)
          cfcSelector.itemsProperty().getValue().stream().filter(c -> c.getInteger("id").equals(cfcProperty().getValue().getInteger("id"))).forEach(c -> cfcProperty().setValue(c));
        
        if(cfcProperty().getValue() == null && cfcSelector.itemsProperty().getValue().size() == 1)
          cfcProperty().setValue(cfcSelector.itemsProperty().getValue().get(0));
      }
    });
    
    setOnMouseClicked(e -> {
      if(e.getClickCount() == 2) {
        FXCompany_ companyEditor = new FXCompany_();
        companyEditor.setObjectProperty(ObjectLoader.getMap(Company.class, companyProperty.getValue().getInteger("id")));
        companyEditor.showAndWait(this);
      }
    });
  }
  
  public BooleanProperty companyDisableProperty() {
    return titleLabel.disableProperty();
  }
  
  public StringProperty titleProperty() {
    return titleProperty;
  }
  
  public ObjectProperty<String> inFaceProperty() {
    return inFaceText.valueProperty();
  }
  
  public ObjectProperty<String> reasonProperty() {
    return reasonText.valueProperty();
  }

  public ChoiceLabel<PropertyMap> getPartitionSelector() {
    return partitionSelector;
  }

  public ChoiceLabel<PropertyMap> getCfcSelector() {
    return cfcSelector;
  }
 
  public ObjectProperty<PropertyMap> partitionProperty() {
    return partitionSelector.valueProperty();
  }
  
  public ObjectProperty<PropertyMap> cfcProperty() {
    return cfcSelector.valueProperty();
  }
  
  public ObjectProperty<PropertyMap> companyProperty() {
    return companyProperty;
  }

  public TitleBorderPane getTitlePane() {
    return titlePane;
  }

  private void selectCompany() {
    FXTreeEditor  cfcTree = new FXTreeEditor(CFC.class);
    FXTableEditor companyTable = new FXTableEditor(Company.class, FXCompany_.class, Column.create("id", "id", false), Column.create("Наименование","name",new TextFilter()), Column.create("ИНН", "inn",new TextFilter()));
    
    companyTable.addInitDataListener(e -> companyTable.getTable().getSelectionModel().select(0));
    
    SplitPane split = new SplitPane(cfcTree, companyTable);
    cfcTree.initData();
    cfcTree.selectedItemProperty().addListener((ObservableValue<? extends TreeItem<PropertyMap>> ob, TreeItem<PropertyMap> ol, TreeItem<PropertyMap> nw) -> {
      companyTable.getClientFilter().clear();
      if(nw != null) {
        Integer[] ids = new Integer[0];
        for(PropertyMap tp:cfcTree.getSubObjects(nw))
          ids = ArrayUtils.add(ids, (Integer)tp.getValue("id"));
        companyTable.getClientFilter().AND_IN("cfcs", ids);
      }
      companyTable.initData();
    });
    FXGLoader.load("SelectorCompany", split, companyTable.getTable());
    
    FXD dialog = FXD.create("Выбор Организации", this, split, FXD.ButtonType.OK);
    dialog.setOnShowing(e -> FXGLoader.load("SelectorCompany", split, companyTable.getTable()));
    dialog.setOnHiding(e -> FXGLoader.store("SelectorCompany", split, companyTable.getTable()));
    
    
    companyTable.getTable().setOnMouseClicked(e -> {
      if(e.getClickCount() == 2)
        dialog.fire(FXD.ButtonType.OK);
    });
    
    dialog.requestFocus();
    if(Optional.of(dialog.showDialog()).orElseGet(() -> FXD.ButtonType.CLOSE) == FXD.ButtonType.OK) {
      companyProperty().setValue(ObjectLoader.getMap(Company.class, companyTable.getSelectedIds()[0], "id","ownership","name","cfcs","inn","chifPlaceName","chiefName","businessReason"));
      if(!cfcTree.getSelectedObjects().isEmpty())
        cfcProperty().setValue(cfcTree.getSelectedObjects().get(0));
    }
  }
}