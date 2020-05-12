package division.fx.controller.company.partition;

import bum.editors.util.ObjectLoader;
import bum.interfaces.Account;
import conf.P;
import division.fx.DivisionTextField;
import division.fx.FXUtility;
import division.fx.PropertyMap;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.gui.FXStorable;
import division.json.RESTReader;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import util.filter.local.DBFilter;


public class FXPartitionDetails extends VBox implements FXStorable {
  private final TextArea        urText        = new TextArea();
  private final TitleBorderPane urBorder      = new TitleBorderPane(urText, "Юридический");
  
  private final TextArea        factText      = new TextArea();
  private final TitleBorderPane factBorder    = new TitleBorderPane(factText, "Фактический");
  
  private final TextArea        postText      = new TextArea();
  private final TitleBorderPane postBorder    = new TitleBorderPane(postText, "Почтовый");
  
  private final GridPane        addrBox       = new GridPane();
  
  private final TitleBorderPane addressBorder = new TitleBorderPane(addrBox, "Адрес");
  
  private final FXAccount       accountEditor = new FXAccount();
  private final TitleBorderPane accountBorder = new TitleBorderPane((Parent)accountEditor.getRoot().getCenter(), "Расчётный счет");
  
  private final Label                     fioLabel = new Label("Ф.И.О.:");
  private final DivisionTextField<String> fio      = new DivisionTextField<>();
  
  private final Label                     telefonLabel = new Label("Телефон:");
  private final DivisionTextField<String> telefon      = new DivisionTextField<>();
  
  private final Label                     emialLabel = new Label("Email:");
  private final DivisionTextField<String> email      = new DivisionTextField<>();
  
  private final Label                     infoLabel = new Label("Доп. информация:");
  private final TextArea                  info      = new TextArea();
  
  private final GridPane                  contactGrid = new GridPane();
  private final VBox                      contactBox  = new VBox(5, contactGrid, infoLabel, info);
  
  private final TitleBorderPane           contactBorder = new TitleBorderPane(contactBox, "Контакты");
  
  private final GridPane        accountContactBox = new GridPane();
  
  private final ObjectProperty<PropertyMap> partitionProperty = new SimpleObjectProperty<>();

  public FXPartitionDetails() {
    FXUtility.initMainCss(this);
    getChildren().addAll(addressBorder, accountContactBox);
    
    contactGrid.addColumn(0, fioLabel, telefonLabel, emialLabel);
    contactGrid.addColumn(1, fio, telefon, email);
    contactGrid.getColumnConstraints().addAll(new ColumnConstraints(), new ColumnConstraints());
    contactGrid.getColumnConstraints().get(0).setHgrow(Priority.NEVER);
    contactGrid.getColumnConstraints().get(1).setHgrow(Priority.ALWAYS);
    
    VBox.setVgrow(info, Priority.ALWAYS);
    
    accountContactBox.addRow(0, accountBorder, contactBorder);
    accountContactBox.getColumnConstraints().addAll(new ColumnConstraints(), new ColumnConstraints());
    accountContactBox.getColumnConstraints().forEach(c -> c.setPercentWidth(50));
    
    accountContactBox.getRowConstraints().add(new RowConstraints());
    accountContactBox.getRowConstraints().forEach(r -> r.setVgrow(Priority.ALWAYS));
    VBox.setVgrow(accountContactBox, Priority.ALWAYS);
    
    addrBox.addRow(0, urBorder, factBorder, postBorder);
    addrBox.getColumnConstraints().addAll(new ColumnConstraints(),new ColumnConstraints(),new ColumnConstraints());
    addrBox.getColumnConstraints().forEach(c -> c.setPercentWidth(33));
    
    RESTReader.create()
            .add(urText,   P.String("REST.address"), "suggestions", 3, p -> {
              urText.setText(p.getString("value"));
              urText.positionCaret(urText.getText().length());
            },   p -> p.getString("value"))
            .add(factText, P.String("REST.address"), "suggestions", 3, p -> {
              factText.setText(p.getString("value"));
              factText.positionCaret(factText.getText().length());
            }, p -> p.getString("value"))
            .add(postText, P.String("REST.address"), "suggestions", 3, p -> {
              postText.setText(p.getString("value"));
              postText.positionCaret(postText.getText().length());
            }, p -> p.getString("value"));
    
    partitionProperty.addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      
      if(oldValue != null)
        oldValue.unbind("urAddres","addres","postAddres","contactFio","telefon","email","contactInfo");
      
      accountEditor.setObjectProperty(null);
      
      urText.setText("");
      factText.setText("");
      postText.setText("");
      
      fio.setText("");
      telefon.setText("");
      email.setText("");
      info.setText("");
      
      if(newValue != null) {
        urText.setDisable(true);
        factText.setDisable(true);
        postText.setDisable(true);

        urText.setText(partitionProperty.getValue().getString("urAddres"));
        factText.setText(partitionProperty.getValue().getString("addres"));
        postText.setText(partitionProperty.getValue().getString("postAddres"));
        
        fio.setText(partitionProperty.getValue().getString("contactFio"));
        telefon.setText(partitionProperty.getValue().getString("telefon"));
        email.setText(partitionProperty.getValue().getString("email"));
        info.setText(partitionProperty.getValue().getString("contactInfo"));
        
        partitionProperty.getValue()
                .bind("urAddres",    urText.textProperty())
                .bind("addres",      factText.textProperty())
                .bind("postAddres",  postText.textProperty())
                
                .bind("contactFio",  fio.textProperty())
                .bind("telefon",     telefon.textProperty())
                .bind("email",       email.textProperty())
                .bind("contactInfo", info.textProperty());

        List<PropertyMap> acs = ObjectLoader.getList(DBFilter.create(Account.class)
                .AND_EQUAL("companyPartition", newValue.getInteger("id"))
                .AND_EQUAL("tmp", false)
                .AND_EQUAL("type", Account.Type.CURRENT),"id","number","current","bank_name","bank_corrAccount","bank_bik","bank_address");
        accountEditor.setObjectClass(Account.class);
        accountEditor.setObjectProperty(acs.stream().filter(a -> a.is("current")).findFirst().orElseGet(() -> acs.isEmpty() ? null : acs.get(0)));

        urText.setDisable(false);
        factText.setDisable(false);
        postText.setDisable(false);
      }
    });
  }
  
  public ObjectProperty<PropertyMap> partitionProperty() {
    return partitionProperty;
  }

  @Override
  public List<Node> storeControls() {
    return FXCollections.observableArrayList();
  }
}