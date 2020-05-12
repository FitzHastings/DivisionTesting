package division.fx.controller.company;

import bum.editors.util.ObjectLoader;
import bum.interfaces.CFC;
import bum.interfaces.Company;
import bum.interfaces.CompanyPartition;
import bum.interfaces.Place;
import division.fx.PropertyMap;
import division.fx.dialog.FXDialog;
import division.fx.editor.FXTableEditor;
import division.fx.editor.FXTreeEditor;
import division.fx.editor.GLoader;
import division.fx.table.Column;
import division.fx.util.MsgTrash;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import javafx.stage.WindowEvent;
import org.apache.commons.lang3.ArrayUtils;

public class CompanySelector extends BorderPane implements Initializable {
  @FXML private BorderPane inFacePane;
  @FXML private ChoiceBox<PropertyMap> cfcbox;
  @FXML private ChoiceBox<PropertyMap> partitionbox;
  @FXML private TextField reason;
  @FXML private Label inFaceLabel;
  @FXML private BorderPane companyPane;
  @FXML private TextArea name;
  @FXML private TextField fio;
  @FXML private Label companyLabel;
  
  private ObjectProperty<PropertyMap> company   = new SimpleObjectProperty<>();

  public CompanySelector() {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("CompanySelector.fxml"));
    loader.setRoot(this);
    loader.setController(this);
    try {
      loader.load();
    }catch(Exception ex) {ex.printStackTrace();}
    
    setOnMouseClicked(e -> {
      if(e.getClickCount() == 2 && company.getValue() != null) {
        try {
          FXCompany editor = new FXCompany();
          editor.setObjectClass(Company.class);
          editor.setObjectProperty(company.getValue());
          editor.showAndWait(this);
          company.setValue(ObjectLoader.getMap(Company.class, company.getValue().getValue("id", Integer.TYPE)));
        }catch(Exception ex) {
          MsgTrash.out(ex);
        }
      }
    });
  }

  public Label getCompanyLabel() {
    return companyLabel;
  }
  
  public ObjectProperty<PropertyMap> companyProperty() {
    return company;
  }
  
  public ObjectProperty<PropertyMap> cfcProperty() {
    return cfcbox.valueProperty();
  }
  
  public ObjectProperty<PropertyMap> partitionProperty() {
    return partitionbox.valueProperty();
  }
  
  public StringProperty companyTitleText() {
    return companyLabel.textProperty();
  }
  
  public void setCompanyTitleText(String title) {
    companyLabel.setText(title);
  }
  
  public String getCompanyTitleText() {
    return companyLabel.getText();
  }
  
  public void setInfaceTitleText(String title) {
    inFaceLabel.setText(title);
  }
  
  public String getInfaceTitleText() {
    return inFaceLabel.getText();
  }
  
  public void setInfaceVisible(boolean visible) {
    inFacePane.setVisible(visible);
  }
  
  public boolean isInfaceVisible() {
    return inFacePane.isVisible();
  }
  
  public StringProperty fioProperty() {
    return fio.textProperty();
  }
  
  public StringProperty reasonProperty() {
    return reason.textProperty();
  }
  
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    company.addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      PropertyMap cfc       = cfcProperty().getValue();
      //PropertyMap partition = partitionProperty().getValue();
      cfcbox.setItems(newValue == null || newValue.isNull("cfcs") ? FXCollections.observableArrayList() : ObjectLoader.getList(CFC.class, newValue.getValue("cfcs", Integer[].class)));
      partitionbox.setItems(newValue == null || newValue.isNull("companyPartitions") ? FXCollections.observableArrayList() : ObjectLoader.getList(CompanyPartition.class, newValue.getValue("companyPartitions", Integer[].class)));
      
      cfc = cfcbox.getItems().contains(cfc) ? cfc : null;
      cfcProperty().setValue(cfc == null ? cfcbox.getItems().isEmpty() ? null : cfcbox.getItems().get(0) : cfc);
      
      for(PropertyMap p:partitionbox.getItems()) {
        if(p.getValue("mainPartition", false)) {
          partitionProperty().setValue(p);
          break;
        }
      }
      if(partitionProperty().getValue() == null)
        partitionbox.getSelectionModel().selectFirst();
      
      name.textProperty().unbind();
      name.textProperty().bind(newValue.get("name"));
      
      if(reason.getText() == null || "".equals(reason.getText()))
        reason.setText(newValue.getValue("businessReason", String.class));
      
      if(fio.getText() == null || "".equals(fio.getText())) {
        PropertyMap chifplace = ObjectLoader.getMap(Place.class, newValue.getValue("chiefPlace", Integer.TYPE), "id", "name");
        fio.setText((chifplace.isNullOrEmpty("name") ? "" : chifplace.getValue("name", String.class))+(newValue.isNullOrEmpty("chiefName") ? "" : (" "+newValue.getValue("chiefName", String.class))));
      }
    });
    
    cfcbox.valueProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      if(newValue != null && (!cfcbox.getItems().contains(newValue.equalKeys("id")) || cfcbox.getItems().isEmpty())) {
        cfcbox.getItems().add(newValue);
        cfcbox.setValue(newValue);
      }
    });
    
    companyLabel.setOnMouseClicked(e -> selectCompany());
  }
  
  private void selectCompany() {
    FXTreeEditor  cfcTree = new FXTreeEditor(CFC.class);
    FXTableEditor companyTable = new FXTableEditor(Company.class, FXCompany.class, Column.create("id", "id", false), Column.create("Наименование","name"), Column.create("ИНН", "inn"));
    
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
    GLoader.load("SelectorCompany", split, companyTable.getTable());
    
    FXDialog dialog = FXDialog.create("Выбор Организации", getCenter(), split, FXDialog.ButtonGroup.OK, null, e -> {
      if(e.getEventType() == WindowEvent.WINDOW_HIDING)
        GLoader.store("SelectorCompany", split, companyTable.getTable());
    });
    
    companyTable.getTable().setOnMouseClicked(e -> {
      if(e.getClickCount() == 2) {
        dialog.setResult(FXDialog.Type.OK);
        dialog.close();
      }
    });
    
    dialog.requestFocus();
    dialog.showAndWait();
    
    if(dialog.getResult() == FXDialog.Type.OK) {
      reason.setText("");
      fio.setText("");
      company.setValue(ObjectLoader.getMap(Company.class, companyTable.getSelectedIds()[0]));
      cfcbox.setValue(ObjectLoader.getMap(CFC.class, cfcTree.getSelectedIds()[0]));
    }
  }
}