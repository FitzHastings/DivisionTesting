package division.fx.controller.company;

import bum.interfaces.Company;
import bum.interfaces.CompanyPartition;
import bum.interfaces.OwnershipType;
import bum.interfaces.Place;
import client.util.ObjectLoader;
import conf.P;
import division.fx.ChoiseLabel.ChoiceLabel;
import division.fx.DivisionTextField;
import division.fx.FXUtility;
import division.fx.LinkLabel;
import division.fx.PropertyMap;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.controller.company.partition.FXCompanyPartition;
import division.fx.editor.FXObjectEditor;
import division.fx.image.ImageBox;
import division.fx.util.MsgTrash;
import division.json.RESTReader;
import division.util.GzipUtil;
import java.io.File;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import util.filter.local.DBFilter;

public class FXCompany_ extends FXObjectEditor {
  
  private final ImageBox logoBox = new ImageBox();
  private final TextArea  fullName = new TextArea();
  private final TextField shortName = new TextField();
  
  
  private final ChoiceLabel<PropertyMap> ownerTypeChoice  = new ChoiceLabel<>();
  private final DivisionTextField<Long>  innText          = new DivisionTextField<>();
  
  private final VBox                     nameBox          = new VBox(5, fullName, shortName);
  private final HBox                     ownerTypePane    = new HBox(5, logoBox, nameBox); 
  
  private final ChoiceLabel<PropertyMap> firstPerson      = new ChoiceLabel<>();
  private final TextField                firstPersonText  = new TextField();
  private final ImageBox                 chifSignatureBox = new ImageBox();
  private final HBox                     fPBox            = new HBox(5, firstPerson, firstPersonText, chifSignatureBox);
  
  private final ChoiceLabel<PropertyMap> secondPerson     = new ChoiceLabel<>();
  private final TextField                secondPersonText = new TextField();
  private final ImageBox                 bookkeeperSignatureBox = new ImageBox();
  private final HBox                     sPBox            = new HBox(5, secondPerson, secondPersonText, bookkeeperSignatureBox);
  
  private final Label                    reasonLabel      = new Label("Основание действий");
  private final TextField                reasonText       = new TextField();
  private final HBox                     reasonBox        = new HBox(5, reasonLabel, reasonText);
  
  private final ImageBox                 stampBox           = new ImageBox();
  
  private final VBox                     personBox        = new VBox(5, fPBox, sPBox, reasonBox);
  private final HBox                     chifBoxPane      = new HBox(5, personBox, stampBox);
  
  private final TitleBorderPane          chifBorder       = new TitleBorderPane(new VBox(chifBoxPane), "Руководство");
  
  private final CheckBox                 ndsPayer         = new CheckBox("Плательщик НДС");
  private final TitleBorderPane          ownerTypeBorder  = new TitleBorderPane(ownerTypePane, ownerTypeChoice, new Separator(Orientation.VERTICAL), new Label("ИНН"), innText, ndsPayer);
  
  private final FXCompanyPartition       partitionTabs    = new FXCompanyPartition();
  
  private final ChoiceLabel<PropertyMap> partitionBox     = new ChoiceLabel<>();
  private final TitleBorderPane          partitionBorder  = new TitleBorderPane(partitionTabs, partitionBox);
  private final HBox                     topBorders       = new HBox(5, ownerTypeBorder, chifBorder);
  
  private final SplitPane                rootSplit        = new SplitPane(topBorders, partitionBorder);

  public FXCompany_() {
    FXUtility.initMainCss(this);
    setObjectClass(Company.class);
    
    LinkLabel cfcLabel    = new LinkLabel("ЦФУ", e -> selectCfc());
    HBox cfcBox = new HBox(cfcLabel);
    LinkLabel authorLabel = new LinkLabel("Авторское право", e -> selectAuthor());
    HBox topPane = new HBox(cfcBox, authorLabel);
    HBox.setHgrow(cfcBox, Priority.ALWAYS);
    topPane.setPadding(new Insets(10, 0, 10, 0));
    
    getRoot().setTop(topPane);
    getRoot().setCenter(rootSplit);
    rootSplit.setOrientation(Orientation.VERTICAL);
    
    try {
      ownerTypeChoice.itemsProperty().getValue().addAll(ObjectLoader.getList(OwnershipType.class, "id","name").sorted((PropertyMap o1, PropertyMap o2) -> o1.getString("name").compareTo(o2.getString("name"))));
      firstPerson.itemsProperty().getValue().addAll(ObjectLoader.getList(Place.class, "id","name").sorted((PropertyMap o1, PropertyMap o2) -> o1.getString("name").compareTo(o2.getString("name"))));
      secondPerson.itemsProperty().getValue().addAll(ObjectLoader.getList(Place.class, "id","name").sorted((PropertyMap o1, PropertyMap o2) -> o1.getString("name").compareTo(o2.getString("name"))));
    }catch(Exception ex) {
      MsgTrash.out(ex);
    }
    
    storeControls().addAll(rootSplit,partitionTabs);
    
    partitionTabs.partitionProperty().bind(partitionBox.valueProperty());
    
    ownerTypeChoice.promtTextProperty().setValue("Форма собственности");
    partitionBox.promtTextProperty().setValue("Обособленное подразделение");
    firstPerson.promtTextProperty().setValue("Первое лицо");
    secondPerson.promtTextProperty().setValue("Второе лицо");
    
    ownerTypeChoice.nameVisibleProperty().setValue(false);
    partitionBox.nameVisibleProperty().setValue(false);
    firstPerson.nameVisibleProperty().setValue(false);
    secondPerson.nameVisibleProperty().setValue(false);
    
    ownerTypeBorder.setMinHeight(0);
    chifBorder.setMinHeight(0);
    
    chifSignatureBox.minHeightProperty().bind(firstPersonText.heightProperty());
    chifSignatureBox.maxHeightProperty().bind(firstPersonText.heightProperty());
    chifSignatureBox.prefHeightProperty().bind(firstPersonText.heightProperty());
    
    bookkeeperSignatureBox.minHeightProperty().bind(secondPersonText.heightProperty());
    bookkeeperSignatureBox.maxHeightProperty().bind(secondPersonText.heightProperty());
    bookkeeperSignatureBox.prefHeightProperty().bind(secondPersonText.heightProperty());
    
    nameBox.minHeightProperty().bind(logoBox.minHeightProperty());
    nameBox.maxHeightProperty().bind(logoBox.maxHeightProperty());
    nameBox.prefHeightProperty().bind(logoBox.prefHeightProperty());
    
    chifSignatureBox.prefHeightProperty().bind(firstPersonText.heightProperty());
    bookkeeperSignatureBox.prefHeightProperty().bind(secondPersonText.heightProperty());
    
    stampBox.minHeightProperty().bind(personBox.heightProperty());
    stampBox.maxHeightProperty().bind(personBox.heightProperty());
    stampBox.prefHeightProperty().bind(personBox.heightProperty());
    
    stampBox.minWidthProperty().bind(personBox.heightProperty());
    stampBox.maxWidthProperty().bind(personBox.heightProperty());
    stampBox.prefWidthProperty().bind(personBox.heightProperty());
    
    logoBox.minHeightProperty().bind(stampBox.minHeightProperty());
    logoBox.maxHeightProperty().bind(stampBox.maxHeightProperty());
    logoBox.prefHeightProperty().bind(stampBox.heightProperty());
    
    logoBox.minWidthProperty().bind(stampBox.minWidthProperty());
    logoBox.maxWidthProperty().bind(stampBox.maxWidthProperty());
    logoBox.prefWidthProperty().bind(stampBox.widthProperty());
    
    HBox.setHgrow(firstPersonText, Priority.ALWAYS);
    HBox.setHgrow(secondPersonText, Priority.ALWAYS);
    HBox.setHgrow(reasonText, Priority.ALWAYS);
    HBox.setHgrow(reasonBox, Priority.ALWAYS);
    HBox.setHgrow(personBox, Priority.ALWAYS);
    HBox.setHgrow(chifBorder, Priority.ALWAYS);
    
    RESTReader.create()
            .add(innText, P.String("REST.party"), "suggestions", 3, p -> fillCompanyData(p), p -> p.getString("value")+" - "+p.getMap("data").getString("inn"))
            .add(fullName, P.String("REST.party"), "suggestions", 3, p -> fillCompanyData(p), p -> p.getString("value")+" - "+p.getMap("data").getString("inn"))
            .add(shortName, P.String("REST.party"), "suggestions", 3, p -> fillCompanyData(p), p -> p.getString("value")+" - "+p.getMap("data").getString("inn"))
            .add(firstPersonText, P.String("REST.fio"), "suggestions", 3, p -> firstPersonText.setText(p.getString("value")), p -> p.getString("value"))
            .add(secondPersonText, P.String("REST.fio"), "suggestions", 3, p -> secondPersonText.setText(p.getString("value")), p -> p.getString("value"));
  }
  
  @Override
  public void initData() {
    if(getObjectProperty().isNotNull("id")) {
      
      innText.setDisable(true);
      fullName.setDisable(true);
      shortName.setDisable(true);
      firstPersonText.setDisable(true);
      secondPersonText.setDisable(true);
      
      fullName.setText(getObjectProperty().getString("name"));
      shortName.setText(getObjectProperty().getString("shotName"));
      innText.setText(getObjectProperty().getString("inn"));

      firstPersonText.setText(getObjectProperty().getString("chiefName"));
      secondPersonText.setText(getObjectProperty().getString("bookkeeper"));

      firstPerson.valueProperty().setValue(firstPerson.itemsProperty().getValue().stream().filter(p -> p.getInteger("id") == getObjectProperty().getInteger("chiefPlace")).findFirst().orElseGet(() -> null));
      secondPerson.valueProperty().setValue(secondPerson.itemsProperty().getValue().stream().filter(p -> p.getInteger("id") == getObjectProperty().getInteger("secondPerson")).findFirst().orElseGet(() -> null));
      
      reasonText.setText(getObjectProperty().getString("businessReason"));
      
      ndsPayer.setSelected(getObjectProperty().is("ndsPayer"));
      
      try {
        logoBox.bytesImageProperty().setValue(GzipUtil.ungzip(getObjectProperty().getBytes("logo")));
        stampBox.bytesImageProperty().setValue(GzipUtil.ungzip(getObjectProperty().getBytes("stamp")));
        chifSignatureBox.bytesImageProperty().setValue(GzipUtil.ungzip(getObjectProperty().getBytes("chifSignature")));
        bookkeeperSignatureBox.bytesImageProperty().setValue(GzipUtil.ungzip(getObjectProperty().getBytes("bookkeeperSignature")));
      }catch(Exception ex) {
        MsgTrash.out(ex);
      }
      
      innText.setDisable(false);
      fullName.setDisable(false);
      shortName.setDisable(false);
      firstPersonText.setDisable(false);
      secondPersonText.setDisable(false);
      
      try {
        getObjectProperty().setValue("companyPartitions", 
                FXCollections.observableArrayList(
                        ObjectLoader.getList(DBFilter.create(CompanyPartition.class).AND_EQUAL("company", getObjectProperty().getInteger("id")).AND_EQUAL("tmp", false).AND_EQUAL("type", CompanyPartition.Type.CURRENT))
                                .stream().map(p -> p.setValue("full-name", p.getString("name")+" "+p.getString("kpp"))).collect(Collectors.toList())));
        
        partitionBox.itemsProperty().getValue().setAll(getObjectProperty().getList("companyPartitions"));
        partitionBox.itemsProperty().bind(getObjectProperty().get("companyPartitions"));
      }catch(Exception ex) {
        MsgTrash.out(ex);
      }
      
      getObjectProperty().setValue("kkt", PropertyMap.copy(PropertyMap.fromJsonFile("conf"+File.separator+"kkt.json")));
      
      getObjectProperty().getList("companyPartitions").forEach(p -> {
        p.setValue("kkt", getObjectProperty().getMap("kkt").getList(p.getInteger("id").toString()));
        getObjectProperty().getMap("kkt").get(p.getInteger("id").toString()).bind(p.get("kkt"));
      });
      
      setMementoProperty(getObjectProperty().copy());
      
      partitionBox.valueProperty().setValue(partitionBox.itemsProperty().getValue().stream().filter(p -> p.is("mainPartition")).findFirst().orElseGet(() -> partitionBox.itemsProperty().getValue().isEmpty() ? null : partitionBox.itemsProperty().getValue().get(0)));
    }
    
    getObjectProperty()
            .bind("name"               , fullName.textProperty())
            .bind("shotName"           , shortName.textProperty())
            .bind("inn"                , innText.textProperty())
            .bind("chiefName"          , firstPersonText.textProperty())
            .bind("bookkeeper"         , secondPersonText.textProperty())
            .bind("businessReason"     , reasonText.textProperty())
            .bind("ndsPayer"           , ndsPayer.selectedProperty())
            
            .bind("chiefPlace"         , firstPerson.valueProperty())
            .bind("secondPerson"       , secondPerson.valueProperty())
            
            .bind("logo"               , Bindings.createObjectBinding(() -> GzipUtil.gzip(logoBox.bytesImageProperty().getValue()), logoBox.bytesImageProperty()))
            .bind("stamp"              , Bindings.createObjectBinding(() -> GzipUtil.gzip(stampBox.bytesImageProperty().getValue()), stampBox.bytesImageProperty()))
            .bind("chifSignature"      , Bindings.createObjectBinding(() -> GzipUtil.gzip(chifSignatureBox.bytesImageProperty().getValue()), chifSignatureBox.bytesImageProperty()))
            .bind("bookkeeperSignature", Bindings.createObjectBinding(() -> GzipUtil.gzip(bookkeeperSignatureBox.bytesImageProperty().getValue()), bookkeeperSignatureBox.bytesImageProperty()));
  }

  @Override
  public boolean isUpdate() {
    return super.isUpdate(); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean save() {
    try {
      getObjectProperty().getValue("kkt", PropertyMap.create()).toJsonFile("conf"+File.separator+"kkt.json");
    }catch (Exception ex) {
      MsgTrash.out(ex);
    }
    return super.save();
  }

  private void fillCompanyData(PropertyMap p) {
    fullName.setText(p.getMap("data").getMap("name").getString("full"));
    shortName.setText(p.getMap("data").getMap("name").getString("short"));
    
    innText.setText(p.getMap("data").getString("inn"));
    
    if(p.getMap("data").isNotNull("management")) {
      firstPersonText.setText(p.getMap("data").getMap("management").getString("name"));
      
      if(p.getMap("data").getMap("management").isNotNull("post")) {
        firstPerson.valueProperty().setValue(firstPerson.itemsProperty().getValue().stream().filter(ot -> ot.getString("name").equals(p.getMap("data").getMap("management").getString("post"))).findFirst().orElseGet(() -> {
          try {
            PropertyMap pl = PropertyMap.create().setValue("name", p.getMap("data").getMap("management").getString("post"));
            pl.setValue("id", ObjectLoader.createObject(Place.class, pl));
            return pl;
          }catch(Exception ex) {
            MsgTrash.out(ex);
            return null;
          }
        }));
      }
    }
    
    if(p.getMap("data").isNotNull("opf")) {
      ownerTypeChoice.valueProperty().setValue(ownerTypeChoice.itemsProperty().getValue().stream().filter(ot -> ot.getString("name").equals(p.getMap("data").getMap("opf").getString("short"))).findFirst().orElseGet(() -> /*{
        try {
          PropertyMap ot = PropertyMap.create().setValue("name", p.getMap("data").getMap("opf").getString("short")).setValue("transcript", p.getMap("data").getMap("opf").getString("full"));
          ot.setValue("id", ObjectLoader.createObject(OwnershipType.class, ot));
          return ot;
        }catch(Exception ex) {
          MsgTrash.out(ex);
          return null;
        }
      }*/null));
    }
    
    System.out.println(p.toJson());
  }

  private void selectCfc() {
  }

  private void selectAuthor() {
  }
}