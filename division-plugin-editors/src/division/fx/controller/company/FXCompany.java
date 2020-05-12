package division.fx.controller.company;

import division.fx.FXUtility;
import division.fx.PropertyMap;
import bum.editors.util.ObjectLoader;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.editor.FXObjectEditor;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import bum.interfaces.*;
import conf.P;
import division.exportimport.ExportImportUtil;
import division.fx.DivisionTextField;
import division.fx.FXToolButton;
import division.fx.controller.store.FXCompanyStore;
import division.fx.dialog.FXDialog;
import division.fx.editor.*;
import division.fx.image.ImageBox;
import division.fx.table.*;
import division.fx.util.*;
import division.json.RESTReader;
import division.util.GzipUtil;
import division.util.IDStore;
import division.util.Utility;
import java.io.File;
import java.time.Period;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class FXCompany extends FXObjectEditor {
  @FXML private SplitPane rootsplit;
  
        private Label ownerTypeLabel = new Label();
  @FXML private TitleBorderPane ownerType;
  
        private CheckBox contractNumberCheckBox = new CheckBox();
  @FXML private TitleBorderPane contractNumberPane;
  
        private CheckBox docNumberCheckBox = new CheckBox();
  @FXML private TitleBorderPane docNumberPane;
  @FXML private TextField reason;
  @FXML private Label secondPersonLabel;
  @FXML private ImageBox secondPersonSignature;
  @FXML private ImageBox firstPersonSignature;
  @FXML private TextArea postAddress;
  
        private Label fileExportLabel = new Label();
  @FXML private TitleBorderPane fileExportPane;
  
  @FXML private TextArea urAddress;
  @FXML private Label cfcGroups;
  @FXML private TitleBorderPane chifType;
  
        private Label partitionLabel = new Label();
  @FXML private TitleBorderPane partition;
  
  @FXML private ImageBox logo;
  @FXML private CheckBox ndsPayer;
  @FXML private Tab partitionDataTab;
  @FXML private TextField firstPerson;
  @FXML private DivisionTextField inn;
  @FXML private TextArea factAddress;
  @FXML private Tab partitionDocTab;
  @FXML private Tab partitionKKTTab;
  @FXML private ImageBox companySignature;
  @FXML private VBox partitionDataPane;
  @FXML private Tab partitionStoreTab;
  @FXML private TextArea name;
  @FXML private TextField secondPerson;
  @FXML private TextField shortName;
  @FXML private Label firstPersonLabel;
  
  @FXML private TextField fileExport;
  
        private CheckBox documentEditableDateCheckBox = new CheckBox();
  @FXML private TitleBorderPane documentEditDatePane;
  @FXML private DatePicker documentEditDate;
  @FXML private TextField prefixContract;
  @FXML private ChoiceBox<String> prefixDateContract;
  @FXML private TextField prefixSplitContract;
  @FXML private TextField suffixSplitContract;
  @FXML private ChoiceBox<String> suffixDateContract;
  @FXML private TextField suffixContract;
  @FXML private DivisionTextField startNumberContract;
  
        private CheckBox toZeroContractCheckBox = new CheckBox();
  @FXML private TitleBorderPane toZeroContract;
  
  private final ToggleGroup periodGroup = new ToggleGroup();
  @FXML private RadioButton everyDayContract;
  @FXML private RadioButton everyMonthContract;
  @FXML private RadioButton everyYearContract;
  @FXML private CheckBox replaceNumberContract;
  
  @FXML private Button urToFact;
  @FXML private Button factToUr;
  @FXML private Button factToPost;
  @FXML private Button postToFact;
  
  private ObjectProperty<PropertyMap> currentPartition = new SimpleObjectProperty<>();
  
  private final String[] formats = new String[]{
    " ", "гг","гггг", "ггмм","гг/мм","гг.мм","гг-мм", "ммгг","мм/гг","мм.гг","мм-гг", "ггггмм","гггг/мм","гггг.мм","гггг-мм", "ммгггг","мм/гггг",
    "мм.гггг","мм-гггг", "ггммдд","гг/мм/дд","гг.мм.дд","гг-мм-дд", "ммггдд","мм/гг/дд","мм.гг.дд","мм-гг-дд", "ггггммдд","гггг/мм/дд","гггг.мм.дд",
    "гггг-мм-дд", "ммггггдд","мм/гггг/дд","мм.гггг.дд","мм-гггг-дд", "ггддмм","гг/дд/мм","гг.дд.мм","гг-дд-мм", "ммддгг","мм/дд/гг","мм.дд.гг","мм-дд-гг",
    "ггггддмм","гггг/дд/мм","гггг.дд.мм","гггг-дд-мм", "ммддгггг","мм/дд/гггг","мм.дд.гггг","мм-дд-гггг", "ддггмм","дд/гг/мм","дд.гг.мм","дд-гг-мм",
    "ддммгг","дд/мм/гг","дд.мм.гг","дд-мм-гг", "ддггггмм","дд/гггг/мм","дд.гггг.мм","дд-гггг-мм", "ддммгггг","дд/мм/гггг","дд.мм.гггг","дд-мм-гггг"};
  
  private FXCompanyStore store = new FXCompanyStore();
  
  private FXDivisionTable<PropertyMap> documentNumeringTable = new FXDivisionTable<>(
          Column.create("Документ", "name"),
          Column.create("Нумерация", true, true, "Глобальная", "Индивидуальная"),
          Column.create("Настройка префикса")
                  .addColumn("префикс", "prefix", true, true)
                  .addColumn(Column.create("дата", "prefixTypeFormat", true, true, formats))
                  .addColumn("разделитель", "suffixSplit", true, true),
          Column.create("Настройка суффикса")
                  .addColumn("разделитель", "suffixSplit", true, true)
                  .addColumn(Column.create("дата", "suffixTypeFormat", true, true, formats))
                  .addColumn("суффикс", "suffix", true, true),
          Column.create("Начальный\nномер", "startNumber", true, true),
          Column.create("Обнуление", "periodForZero", true, true),
          Column.create("Занимать\nпропущенный\nномер", "grabFreeNumber", true, true)
  );

  @Override
  public void initData() {
    try {
      ObjectLoader.fillList(DBFilter.create(Place.class).AND_EQUAL("tmp", false), getObjectProperty().getList("places"), "id", "name", "tmp");
      ObjectLoader.fillList(DBFilter.create(OwnershipType.class).AND_EQUAL("tmp", false), getObjectProperty().getList("owner-types"), "id", "name", "transcript", "tmp");
      
      getObjectProperty().setValue("companyPartitions", FXCollections.<PropertyMap>observableArrayList());
      getObjectProperty().getList("companyPartitions").addListener((ListChangeListener.Change<? extends PropertyMap> c) -> {
        while(c.next()) {
          if(c.wasAdded())
            c.getAddedSubList().stream().forEach(p -> {
              p.get("mainPartition").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
                if((boolean)newValue)
                  getObjectProperty().getList("companyPartitions").stream().filter(part -> !part.equals(p)).forEach(part -> part.setValue("mainPartition", false));
              });
            });
        }
      });
      
      getObjectProperty().setValue("cfcs", ObjectLoader.getList(CFC.class, getObjectProperty().getValue("cfcs", Integer[].class)));
      getObjectProperty().setValue("ownershipType", ObjectLoader.getMap(OwnershipType.class, getObjectProperty().getValue("ownershipType", Integer.TYPE), "id", "name", "transcript", "tmp"));
      getObjectProperty().setValue("chiefPlace", ObjectLoader.getMap(Place.class, getObjectProperty().getValue("chiefPlace", Integer.TYPE), "id", "name"));
      getObjectProperty().setValue("secondPerson", ObjectLoader.getMap(Place.class, getObjectProperty().getValue("secondPerson", Integer.TYPE), "id", "name"));
      
      getObjectProperty().getList("companyPartitions").setAll(ObjectLoader.getList(DBFilter.create(CompanyPartition.class).AND_EQUAL("company", getObjectProperty().getValue("id")).AND_EQUAL("type", CompanyPartition.Type.CURRENT)));
      if(getObjectProperty().getList("companyPartitions").isEmpty())
        getObjectProperty().getList("companyPartitions").add(PropertyMap.create().setValue("name", "Обособленное подразделение").setValue("tmp", false).setValue("mainPartition", true));
      getObjectProperty().getList("companyPartitions").stream().forEach(cp -> initPartition(cp));
      
      setMementoProperty(PropertyMap.copy(getObjectProperty()));
      
      cfcGroups.textProperty().bind(Bindings.createStringBinding(() -> 
              getObjectProperty().getList("cfcs").isEmpty() ? "Выберите группу..." : Utility.join(getObjectProperty().getList("cfcs", "name", String.class).toArray(), ","), getObjectProperty().getList("cfcs")));
      
      reason.setText(getObjectProperty().getValue("businessReason", String.class));
      name.setText(getObjectProperty().getValue("name", String.class));
      shortName.setText(getObjectProperty().getValue("shotName", String.class));
      inn.setText(getObjectProperty().getValue("inn", String.class));
      
      companySignature.bytesImageProperty().setValue(GzipUtil.ungzip(getObjectProperty().getBytes("stamp")));
      firstPersonSignature.bytesImageProperty().setValue(GzipUtil.ungzip(getObjectProperty().getBytes("chifSignature")));
      secondPersonSignature.bytesImageProperty().setValue(GzipUtil.ungzip(getObjectProperty().getBytes("bookkeeperSignature")));
      logo.bytesImageProperty().setValue(GzipUtil.ungzip(getObjectProperty().getValue("logo", byte[].class)));
      
      
      
      ownerTypeLabel.setText(getObjectProperty().getMap("ownershipType").isEmpty() ? "Форма собственности..." : getObjectProperty().getMap("ownershipType").getValue("name", String.class));
      getObjectProperty().get("ownershipType").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
        ownerTypeLabel.textProperty().unbind();
        ownerTypeLabel.textProperty().bind(Bindings.createStringBinding(() -> getObjectProperty().getMap("ownershipType").isEmpty() ? "Форма собственности..." : getObjectProperty().getMap("ownershipType").getValue("name", String.class), 
                getObjectProperty().getMap("ownershipType").get("name")));
      });
      
      firstPersonLabel.setText(getObjectProperty().getMap("chiefPlace").isEmpty() ? "Первое лицо..." : getObjectProperty().getMap("chiefPlace").getValue("name", String.class));
      getObjectProperty().get("chiefPlace").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
        firstPersonLabel.textProperty().unbind();
        firstPersonLabel.textProperty().bind(Bindings.createStringBinding(() -> getObjectProperty().getMap("chiefPlace").isEmpty() ? "Первое лицо..." : getObjectProperty().getMap("chiefPlace").getValue("name", String.class), 
                getObjectProperty().getMap("chiefPlace").get("name")));
      });
      
      secondPersonLabel.setText(getObjectProperty().getMap("secondPerson").isEmpty() ? "Второе лицо..." : getObjectProperty().getMap("secondPerson").getValue("name", String.class));
      getObjectProperty().get("secondPerson").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
              secondPersonLabel.textProperty().unbind();
              secondPersonLabel.textProperty().bind(Bindings.createStringBinding(() -> getObjectProperty().getMap("secondPerson").isEmpty() ? "Второе лицо..." : getObjectProperty().getMap("secondPerson").getValue("name", String.class), 
                      getObjectProperty().getMap("secondPerson").get("name")));
      });
      
      
      
      
      ndsPayer.setSelected(getObjectProperty().getValue("ndsPayer", false));
      firstPerson.setText(getObjectProperty().getValue("chiefName", String.class));
      secondPerson.setText(getObjectProperty().getValue("bookkeeper", String.class));
      
      getObjectProperty()
              .bind("businessReason"     , reason.textProperty())
              .bind("name"               , name.textProperty())
              .bind("shotName"           , shortName.textProperty())
              .bind("inn"                , inn.textProperty())
              //.bind("stamp"              , companySignature.gzipBytesProperty())
              //.bind("chifSignature"      , firstPersonSignature.gzipBytesProperty())
              //.bind("bookkeeperSignature", secondPersonSignature.gzipBytesProperty())
              //.bind("logo"               , logo.gzipBytesProperty())
              //.bind("logo"               , logo.gzipBytesProperty())
              .bind("ndsPayer"           , ndsPayer.selectedProperty())
              .bind("chiefName"          , firstPerson.textProperty())
              .bind("bookkeeper"         , secondPerson.textProperty());
      
      currentPartition.addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> initPartition(oldValue, newValue));
      getObjectProperty().getList("companyPartitions").stream().filter(p -> p.getValue("mainPartition", boolean.class)).forEach(p -> currentPartition.setValue(p));
      if(currentPartition.getValue() == null && !getObjectProperty().getList("companyPartitions").isEmpty())
        currentPartition.setValue(getObjectProperty().getList("companyPartitions").get(0));
    }catch(Exception ex) {
      MsgTrash.out(ex);
    }
  }
  
  private PropertyMap initPartition(PropertyMap partition) {
    //store.companyPartitionProperty().setValue(partition);
    
    partition.getList("documents").setAll(ObjectLoader.getList(Document.class, "document:=:id","name", "prefix","prefixTypeFormat","prefixSplit","suffixSplit","suffixTypeFormat","suffix","periodForZero","startNumber","grabFreeNumber","sort: system DESC, name"));

    final ObservableList<PropertyMap> partitiondocs = FXCollections.observableArrayList();
    ObjectLoader.fillList(DBFilter.create(CompanyPartitionDocument.class).AND_EQUAL("partition", partition.getValue("id")), partitiondocs,
            "id","document","prefix","prefixTypeFormat","prefixSplit","suffixSplit","suffixTypeFormat","suffix","periodForZero","startNumber","grabFreeNumber","sort: document");
    
    partition.getList("documents").stream().forEach(document -> {
      document.equalKeys("prefix","prefixTypeFormat","prefixSplit","suffixSplit","suffixTypeFormat","suffix","periodForZero","startNumber","grabFreeNumber");
      
      document.get("Нумерация").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
        if(newValue.equals("Индивидуальная"))
          document.copyFrom(document.getValue("Индивидуальная", PropertyMap.copy(document.getSimpleMap("prefix","prefixTypeFormat","prefixSplit","suffixSplit","suffixTypeFormat","suffix","periodForZero","startNumber","grabFreeNumber"))));
        if(newValue.equals("Глобальная")) {
          document.getValue("Индивидуальная", PropertyMap.create()).copyFrom(document.getSimpleMap("prefix","prefixTypeFormat","prefixSplit","suffixSplit","suffixTypeFormat","suffix","periodForZero","startNumber","grabFreeNumber"));
          document.copyFrom(document.getValue("Глобальная", PropertyMap.copy(document.getSimpleMap("prefix","prefixTypeFormat","prefixSplit","suffixSplit","suffixTypeFormat","suffix","periodForZero","startNumber","grabFreeNumber"))));
        }
      });

      document.setValue("Нумерация", "Глобальная");

      partitiondocs.stream().filter(pdoc -> pdoc.getValue("document").equals(document.getValue("document"))).forEach(pdoc -> 
        document.setValue("Индивидуальная", PropertyMap.copy(pdoc.getSimpleMap("prefix","prefixTypeFormat","prefixSplit","suffixSplit","suffixTypeFormat","suffix","periodForZero","startNumber","grabFreeNumber")))
                .setValue("id", pdoc.getValue("id"))
                .setValue("Нумерация", "Индивидуальная"));
    });
    return partition;
  }
  
  ChangeListener<Object> mainPartitionListener = new ChangeListener<Object>() {
    @Override
    public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
      if((Boolean)newValue) {
        contractNumberCheckBox.setSelected(false);
        docNumberCheckBox.setSelected(false);
      }
    }
  };
  
  private void initPartition(PropertyMap previus, PropertyMap next) {
    if(previus != null)
      previus.unbindAll();
    PropertyMap nextPartition = next == null ? PropertyMap.create() : next;
    
    partitionLabel.textProperty().unbind();
    partitionLabel.textProperty().bind(Bindings.createStringBinding(() -> 
            currentPartition.getValue().isNull("name") ? "Обособленное подразделение..." : currentPartition.getValue().getString("name")
                    +" (КПП "+(currentPartition.getValue().isNullOrEmpty("kpp") || currentPartition.getValue().getValue("kpp").equals("") ? "..............." : currentPartition.getValue().getValue("kpp"))+")", 
            currentPartition.getValue().get("name"), 
            currentPartition.getValue().get("kpp")));
    
    contractNumberCheckBox.setSelected(!nextPartition.is("mainPartition") && nextPartition.is("mainnumberingcontract"));
    docNumberCheckBox.setSelected(!nextPartition.is("mainPartition") && nextPartition.is("mainnumbering"));
    
    if(previus != null)
      previus.get("mainPartition").removeListener(mainPartitionListener);
    nextPartition.get("mainPartition").addListener(mainPartitionListener);
    
    contractNumberCheckBox.disableProperty().unbind();
    contractNumberCheckBox.disableProperty().bind(Bindings.createBooleanBinding(() -> currentPartition.getValue().is("mainPartition"), currentPartition.getValue().get("mainPartition")));
    
    docNumberCheckBox.disableProperty().unbind();
    docNumberCheckBox.disableProperty().bind(contractNumberCheckBox.disableProperty());
    
    urAddress.setText(nextPartition.getString("urAddres"));
    factAddress.setText(nextPartition.getString("addres"));
    postAddress.setText(nextPartition.getString("postAddres"));
    
    fileExport.setText(ExportImportUtil.getExportPath(nextPartition.getInteger("id")));
    documentEditableDateCheckBox.setSelected(!nextPartition.isNull("docStopDate"));
    documentEditDate.setValue(nextPartition.getLocalDate(("docStopDate")));
    prefixContract.setText(nextPartition.getString("prefix"));
    prefixDateContract.getSelectionModel().select(nextPartition.getString("prefixTypeFormat"));
    prefixSplitContract.setText(nextPartition.getString("prefixSplit"));
    suffixSplitContract.setText(nextPartition.getString("suffixSplit"));
    suffixDateContract.getSelectionModel().select(nextPartition.getString("suffixTypeFormat"));
    suffixContract.setText(nextPartition.getString("suffix"));
    startNumberContract.setValue(nextPartition.getInteger("startNumber"));
    
    toZeroContractCheckBox.setSelected(!nextPartition.getValue("periodForZero", Period.ZERO).isZero());
    
    everyDayContract.setSelected(nextPartition.getValue("periodForZero", Period.ZERO).getDays() > 0);
    everyMonthContract.setSelected(nextPartition.getValue("periodForZero", Period.ZERO).getMonths() > 0);
    everyYearContract.setSelected(nextPartition.getValue("periodForZero", Period.ZERO).getYears() > 0);
    replaceNumberContract.setSelected(nextPartition.getValue("grabFreeNumber", false));
    
    nextPartition
            .bind("mainnumberingcontract", contractNumberCheckBox.selectedProperty())
            .bind("mainnumbering"        , docNumberCheckBox.selectedProperty())
            .bind("prefix"               , prefixContract.textProperty())
            .bind("prefixTypeFormat"     , prefixDateContract.getSelectionModel().selectedItemProperty())
            .bind("prefixSplit"          , prefixSplitContract.textProperty())
            .bind("suffixSplit"          , suffixSplitContract.textProperty())
            .bind("suffixSplit"          , suffixSplitContract.textProperty())
            .bind("suffixTypeFormat"     , suffixDateContract.getSelectionModel().selectedItemProperty())
            .bind("suffix"               , suffixContract.textProperty())
            .bind("startNumber"          , Bindings.createIntegerBinding(() -> {
              return (Integer)(startNumberContract.getValue() == null ? 1 : startNumberContract.getValue());
            }, startNumberContract.textProperty()))
            .bind("periodForZero"        , Bindings.createObjectBinding(() -> 
                    toZeroContractCheckBox.isSelected() ? 
                            everyDayContract.isSelected() ? Period.ofDays(1) : everyMonthContract.isSelected() ? Period.ofMonths(1) : everyYearContract.isSelected() ? Period.ofYears(1) : Period.ZERO : 
                            Period.ZERO, periodGroup.selectedToggleProperty(), toZeroContractCheckBox.selectedProperty()))
            .bind("grabFreeNumber"       , replaceNumberContract.selectedProperty())
            .bind("urAddres"             , urAddress.textProperty())
            .bind("addres"               , factAddress.textProperty())
            .bind("postAddres"           , postAddress.textProperty())
            .bind("docStopDate", Bindings.createObjectBinding(() -> documentEditableDateCheckBox.isSelected() ? documentEditDate.getValue() : null, documentEditDate.valueProperty(), documentEditableDateCheckBox.selectedProperty()));
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    FXUtility.initCss(this);
    
    ownerType.setTitle(new Label("Форма собственноси"));
    contractNumberPane.setTitle(contractNumberCheckBox);
    documentEditDatePane.setTitle(documentEditableDateCheckBox);
    docNumberPane.setTitle(docNumberCheckBox);
    toZeroContract.setTitle(toZeroContractCheckBox);
    partition.setTitle(partitionLabel);
    fileExportPane.setTitle(fileExportLabel);
    
    documentEditDate.disableProperty().bind(documentEditableDateCheckBox.selectedProperty().not());
    
    inn.setConverter(new StringConverter() {
      @Override
      public String toString(Object object) {
        return String.valueOf(object);
      }

      @Override
      public Object fromString(String string) {
        if(string.length() > 12)
          return null;
        return string.equals("") ? "" : String.valueOf(Long.valueOf(string));
      }
    });
    
    toZeroContractCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(newValue && periodGroup.getSelectedToggle() == null)
        everyYearContract.setSelected(true);
    });
    toZeroContract.getCenter().disableProperty().bind(toZeroContractCheckBox.selectedProperty().not());
    periodGroup.getToggles().addAll(everyDayContract, everyMonthContract, everyYearContract);
    
    startNumberContract.setConverter(new IntegerStringConverter());
    
    prefixDateContract.setItems(FXCollections.observableArrayList(formats));
    suffixDateContract.setItems(FXCollections.observableArrayList(formats));
    
    storeControls().addAll(store.storeControls());
    storeControls().addAll(documentNumeringTable, rootsplit);
    partitionStoreTab.setContent(store);
    
    documentNumeringTable.setEditable(true);
    documentNumeringTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    documentNumeringTable.getColumn("Обнуление").setCellFactory((Object param) -> {
      return new MyTableCell("не обнулять","ежедневно", "ежемесячно", "ежегодно") {

        @Override
        public boolean equalsColumnValueToListValue(Object columnValue, Object listValue) {
          return super.equalsColumnValueToListValue(columnValue, listValue.equals("ежедневно") ? "P1D" : listValue.equals("ежемесячно") ? "P1M" : listValue.equals("ежегодно") ? "P1Y" : "P0D");
        }
        
        @Override
        public void commitEdit(Object newValue) {
          newValue = newValue == null ? "P0D" : newValue.equals("ежедневно") ? "P1D" : newValue.equals("ежемесячно") ? "P1M" : newValue.equals("ежегодно") ? "P1Y" : "P0D";
          super.commitEdit(newValue);
        }
        
        @Override
        protected void updateItem(Object item, boolean empty) {
          super.updateItem(item, empty);
          if(!empty) {
            Period p = null;
            try {
              p = Period.parse(item.toString());
            }catch(Exception ex){}
            setText(p != null ? p.getDays() > 0 ? "ежедневно" : p.getMonths() > 0 ? "ежемесячно" : p.getYears() > 0 ? "ежегодно" : "не обнулять" : "не обнулять");
          }
        }
      };
    });
    
    documentNumeringTable.getSelectionModel().setCellSelectionEnabled(true);
    documentNumeringTable.getColumn("Настройка префикса").getColumns().stream().forEach(c -> {
      ((TableColumn)c).setCellFactory((Object param) -> {
        if(((Column)c).getColumnName().equals("дата"))
          return new MyTableCell(formats);
        return new MyTableCell();
      });
    });
    documentNumeringTable.getColumn("Настройка суффикса").getColumns().stream().forEach(c -> {
      ((TableColumn)c).setCellFactory((Object param) -> {
        if(((Column)c).getColumnName().equals("дата"))
          return new MyTableCell(formats);
        return new MyTableCell();
      });
    });
    
    documentNumeringTable.getColumn("Начальный\nномер").setCellFactory((Object param) -> new MyTableCell());
    documentNumeringTable.getColumn("Занимать\nпропущенный\nномер").setCellFactory((Object param) -> new MyTableCell());
    
    docNumberPane.setCenter(documentNumeringTable);
    
    initData();
    initEvents();
  }

  
  class MyTableCell extends FXDivisionTableCell {

    public MyTableCell(String... values) {
      super(null,values);
      init();
    }

    public MyTableCell() {
      super();
      init();
    }

    private void init() {
      editableProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
        setOpacity(newValue ? 1 : 0.5);
        if(newValue)
          getStyleClass().remove("company-document-cell-disable");
        else getStyleClass().add("company-document-cell-disable");
      });
    }
    
    @Override
    protected void updateItem(Object item, boolean empty) {
      super.updateItem(item, empty);
      if(!empty && getRowItem() != null) {
        editableProperty().unbind();
        editableProperty().bind(Bindings.createBooleanBinding(() -> {
          return getTableRow().getItem() != null && ((PropertyMap)getTableRow().getItem()).getValue("Нумерация") != null && ((PropertyMap)getTableRow().getItem()).getValue("Нумерация").equals("Индивидуальная");
        }, ((PropertyMap)getTableRow().getItem()).get("Нумерация")));
      }
    }
  }
  
  public void fill(PropertyMap p) {
    System.out.println(p.toJson());
    name.setText(p.getMap("data").getMap("name").getString("full"));
    shortName.setText(p.getMap("data").getMap("name").getString("short"));
    inn.setText(p.getMap("data").getString("inn"));
    
    ObservableList<PropertyMap> opf = ObjectLoader.getList(DBFilter.create(OwnershipType.class).AND_EQUAL("name", p.getMap("data").getMap("opf").getString("short")), "id", "name", "transcript", "tmp");
    if(opf.isEmpty()) {
      opf.add(PropertyMap.create()
              .setValue("name", p.getMap("data").getMap("opf").getString("short"))
              .setValue("transcript", p.getMap("data").getMap("opf").getString("full")));
      opf.get(0).setValue("id", ObjectLoader.createObject(OwnershipType.class, opf.get(0)));
    }
    getObjectProperty().setValue("ownershipType", opf.get(0));
    
    if(!p.getMap("data").getMap("management").isNullOrEmpty("post")) {
      ObservableList<PropertyMap> chif = ObjectLoader.getList(DBFilter.create(Place.class).AND_ILIKE("name", p.getMap("data").getMap("management").getString("post")), "id", "name", "tmp");
      if(chif.isEmpty()) {
        chif.add(PropertyMap.create()
                .setValue("name", p.getMap("data").getMap("management").getString("post")));
        chif.get(0).setValue("id", ObjectLoader.createObject(Place.class, chif.get(0)));
      }
      getObjectProperty().setValue("chiefPlace", chif.get(0));
    }
    
    firstPerson.setText(p.getMap("data").getMap("management").getString("name"));
    currentPartition.getValue().setValue("kpp", p.getMap("data").getString("kpp"));
    urAddress.setText((p.getMap("data").getMap("address").isNull("data") ? "" : p.getMap("data").getMap("address").getMap("data").getString("postal_code"))+" "+p.getMap("data").getMap("address").getString("value"));
  }

  private void initEvents() {
    RESTReader.create().add(name, P.String("REST.party"), "suggestions", 3, p -> fill(p), p -> p.getString("value")+" - "+p.getMap("data").getString("inn"))
            .add(inn, P.String("REST.party"), "suggestions", 3, p -> fill(p), p -> p.getString("value")+" - "+p.getMap("data").getString("inn"))
            .add(firstPerson, P.String("REST.fio"), "suggestions", 3, p -> firstPerson.setText(p.getString("value")), p -> p.getString("value"))
            .add(secondPerson, P.String("REST.fio"), "suggestions", 3, p -> secondPerson.setText(p.getString("value")), p -> p.getString("value"))
            .add(urAddress, P.String("REST.address"), "suggestions", 3, p -> urAddress.setText(p.getMap("data").getString("postal_code")+" "+p.getString("value")), p -> p.getString("value"))
            .add(factAddress, P.String("REST.address"), "suggestions", 3, p -> factAddress.setText(p.getMap("data").getString("postal_code")+" "+p.getString("value")), p -> p.getString("value"))
            .add(postAddress, P.String("REST.address"), "suggestions", 3, p -> postAddress.setText(p.getMap("data").getString("postal_code")+" "+p.getString("value")), p -> p.getString("value"));
    
    contractNumberPane.getCenter().disableProperty().bind(contractNumberCheckBox.selectedProperty());
    docNumberPane.getCenter().disableProperty().bind(docNumberCheckBox.selectedProperty());
    
    documentNumeringTable.itemsProperty().bind(Bindings.createObjectBinding(() -> currentPartition.getValue().getList("documents"), currentPartition));
    store.companyPartitionProperty().bind(currentPartition);
    
    urToFact.setOnAction(e -> factAddress.setText(urAddress.getText()));
    factToUr.setOnAction(e -> urAddress.setText(factAddress.getText()));
    factToPost.setOnAction(e -> postAddress.setText(factAddress.getText()));
    postToFact.setOnAction(e -> factAddress.setText(postAddress.getText()));
    
    fileExportLabel.setOnMouseClicked(e -> {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Задайте файл экспорта");
      fileChooser.getExtensionFilters().addAll(
              new FileChooser.ExtensionFilter("Только XML", "*.xml"),
                  new FileChooser.ExtensionFilter("XML",    "*.xml"));
      File file = fileChooser.showOpenDialog(null);
      if(file != null) {
        fileExport.setText(file.getAbsolutePath());
        ExportImportUtil.setExportPath(currentPartition.getValue().getValue("id", Integer.TYPE), file.getAbsolutePath());
      }else {
        ExportImportUtil.setExportPath(currentPartition.getValue().getValue("id", Integer.TYPE), "");
      }
    });
    
    ownerTypeLabel.setOnMouseClicked(e -> {
      showSelectTable("select-owner", getObjectProperty().get("ownershipType"), getObjectProperty().getList("owner-types"), PropertyMap.create().setValue("name", "").setValue("tmp", false), Column.create("Наименование", "name", true, true),
              Column.create("Расшифровка" , "transcript", true, true));
    });

    firstPersonLabel.setOnMouseClicked(e -> 
      showSelectTable("select-place", getObjectProperty().get("chiefPlace"), getObjectProperty().getList("places"), PropertyMap.create().setValue("name", "").setValue("tmp", false), Column.create("Наименование", "name", true, true)));
    
    secondPersonLabel.setOnMouseClicked(e -> 
      showSelectTable("select-place", getObjectProperty().get("secondPerson"), getObjectProperty().getList("places"), PropertyMap.create().setValue("name", "").setValue("tmp", false), Column.create("Наименование", "name", true, true)));
    
    partitionLabel.setOnMouseClicked(e -> 
      showSelectTable("select-place", currentPartition, getObjectProperty().getList("companyPartitions"), initPartition(PropertyMap.create().setValue("name", "Обособленное подразделение").setValue("tmp", false).setValue("mainPartition", false)), 
              Column.create("Наименование", "name", true, true),
              Column.create("КПП", "kpp", true, true, new StringConverter() {
                @Override
                public String toString(Object object) {
                  return String.valueOf(object);
                }
                
                @Override
                public Object fromString(String string) {
                  if(string.length() > 9)
                    return null;
                  return string.equals("") ? "" : String.valueOf(Long.valueOf(string));
                }
              }),
              Column.create("Основное", "mainPartition", true, true, false)));
    
    /*cfcGroups.setOnMouseClicked(e -> {
      FXTreeEditor cfcEditor = new FXTreeEditor(CFC.class, null);
      cfcEditor.checkBoxTreeProperty().setValue(true);
      cfcEditor.checkBoxIndependentProperty().setValue(true);
      cfcEditor.checkBoxTreeHerachyProperty().setValue(false);
      cfcEditor.getTree().setShowRoot(false);
      cfcEditor.addInitDataListener((Event event) -> {
        for(Integer id:getObjectProperty().getList("cfcs", "id", Integer.TYPE)) {
          FXCheckBoxTreeItem item = (FXCheckBoxTreeItem) cfcEditor.getNodeObject(id);
          if(item != null) {
            item.activeProperty().setValue(false);
            item.setSelected(true);
            item.activeProperty().setValue(true);
            while(item != null) {
              item.setExpanded(true);
              item = (FXCheckBoxTreeItem)item.getParent();
            }
          }
        }
      });
      getObjectProperty().getList("cfcs").setAll(cfcEditor.getObjects(getRoot(), "Выберите группы"));
    });*/
    
    inn.textProperty().addListener((ob, ol, nw) -> inn.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), nw != null && !nw.matches("(\\d{10}|\\d{12})")));
  }
  
  private void showSelectTable(String tablename, Property property, ObservableList<PropertyMap> list, PropertyMap newRow, Column... columns) {
    FXDivisionTable<PropertyMap> table = new FXDivisionTable<>(columns);
    table.setEditable(true);
    table.getTableFilter().setCustomFilter((PropertyMap t) -> !t.is("tmp"));
    table.getTableFilter().startFilter();
    table.getSourceItems().setAll(list);

    FXToolButton add = new FXToolButton(ae -> {
      PropertyMap row = newRow.copy().setValue("id", "n-"+IDStore.createID());
      table.getSourceItems().add(0, row);
    }, "Добавить", "add-button");
    FXToolButton remove = new FXToolButton(ae -> {
      PropertyMap item = table.getSelectionModel().getSelectedItem();
      if(item.isNullOrEmpty("id") || item.getValue("id").toString().startsWith("n-"))
        table.getSourceItems().remove(item);
      item.setValue("tmp", true);
      table.getTableFilter().startFilter();
    }, "Удалить", "remove-button");

    VBox.setVgrow(table, Priority.ALWAYS);
    if(FXDialog.show(getRoot(), new VBox(5, new ToolBar(add, remove), table), "Выберите...", FXDialog.ButtonGroup.OK, (WindowEvent we) -> {
      if(we.getEventType() == WindowEvent.WINDOW_CLOSE_REQUEST)
        GLoader.store(tablename, table);
      if(we.getEventType() == WindowEvent.WINDOW_SHOWING) {
        GLoader.load(tablename, table);
        table.setOnMouseClicked((MouseEvent ev) -> table.edit(table.getSelectionModel().getSelectedIndex(), table.getSelectionModel().getSelectedCells().get(0).getTableColumn()));
      }
    }) == FXDialog.Type.OK && table.getSelectionModel().getSelectedItem() != null)
      property.setValue(table.getSelectionModel().getSelectedItem());
    
    list.setAll(table.getSourceItems());
  }
  
  @Override
  public String validate() {
    String msg = "";
    if(!(getObjectProperty().getValue("inn") == null ? "" : (String)getObjectProperty().getValue("inn")).matches("(\\d{10}|\\d{12})"))
      msg += "  не корректно введён ИНН\n";
    else {
      DBFilter filter = DBFilter.create(Company.class).AND_EQUAL("inn", getObjectProperty().getValue("inn"));
      if(!getObjectProperty().isNull("id"))
        filter.AND_NOT_EQUAL("id", getObjectProperty().getValue("id"));
      if(!ObjectLoader.getList(filter, "id").isEmpty())
        msg += "  организация с таким ИНН уже присутствует в системе\n";
    }
    if(getObjectProperty().getValue("name") == null || "".equals(getObjectProperty().getValue("name")))
      msg += "  введите наименование\n";
    if(getObjectProperty().getValue("ownershipType") == null)
      msg += "  выберите форму собственности\n";
    return msg;
  }
  
  @Override
  public boolean save() {
    String msg = validate();
    if(msg != null && !"".equals(msg)) {
      new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
      return false;
    }
    
    if(getObjectProperty().getList("cfcs").isEmpty()) {
      cfcGroups.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1, true, true, true, true, true, true, true, true, true, true, null));
      if(getObjectProperty().getList("cfcs").isEmpty())
        return false;
    }
    
    final RemoteSession session = ObjectLoader.createSession(false);;
    try {
      
      for(PropertyMap place:getObjectProperty().getList("places")) {
        if(place.isNullOrEmpty("id") || place.getValue("id").toString().startsWith("n-"))
          place.setValue("id", session.createObject(Place.class, place.getSimpleMap("name","tmp")));
        else session.saveObject(Place.class, place.getSimpleMap("id","name","tmp"));
      }
      
      PropertyMap saveobject = getObjectProperty().copy().remove("lastUserId","modificationDate");
      
      //Сохранение основных данных
      
      saveobject.setValue("tmp", false);
      saveobject.setValue("cfcs", saveobject.getList("cfcs", "id", Integer.TYPE).toArray(new Integer[0]));
      if(saveobject.isNullOrEmpty("id"))
        saveobject.setValue("id", session.createObject(Company.class, saveobject.getSimpleMapWithoutKeys("id","companyPartitions")));
      else
        session.saveObject(Company.class, saveobject.getSimpleMapWithoutKeys("companyPartitions"));
      
      //Сохранение подразделений
      
      for(PropertyMap cp:saveobject.getList("companyPartitions")) {
        cp.setValue("company", saveobject.getValue("id")).remove("lastUserId","modificationDate");
        if(cp.isNullOrEmpty("id") || cp.getValue("id").toString().startsWith("n-"))
          cp.setValue("id", session.createObject(CompanyPartition.class, cp.getSimpleMapWithoutKeys("id", "documents", "stories")));
        else session.saveObject(CompanyPartition.class, cp.getSimpleMapWithoutKeys("documents", "stories"));
        
        //Сохранение настроек нумерации документов
        
        for(PropertyMap doc:cp.getList("documents")) {
          doc.setValue("partition", cp.getValue("id")).setValue("tmp", false).remove("lastUserId","modificationDate");
          if(!doc.isNullOrEmpty("id")) {
            if(doc.getValue("Нумерация").equals("Глобальная"))
              session.removeObjects(CompanyPartitionDocument.class, new Integer[]{doc.getValue("id", Integer.TYPE)});
            else
              session.saveObject(CompanyPartitionDocument.class, doc.getSimpleMapWithoutKeys("Нумерация"));
          }else if(doc.getValue("Нумерация").equals("Индивидуальная"))
            session.createObject(CompanyPartitionDocument.class, doc.getSimpleMapWithoutKeys("id", "Нумерация"));
        }
        
        //Сохранение складов
        
        for(PropertyMap store:cp.getList("stories")) {
          store.setValue("companyPartition", cp.getValue("id")).remove("lastUserId","modificationDate");
          if(store.isNullOrEmpty("id") || store.getValue("id").toString().startsWith("n-"))
            store.setValue("id", session.createObject(Store.class, store.getSimpleMap("name","objectType","storeType","controllOut","companyPartition", "currency")));
          else session.saveObject(Store.class, store.getSimpleMap("id","name","objectType","storeType","controllOut","companyPartition", "currency", "tmp"));
          
          //Сохранение объектов
          
          for(PropertyMap equipment:store.getList("equipments")) {
            equipment.remove("lastUserId","modificationDate");
            if(equipment.isNullOrEmpty("id") || equipment.getValue("id").toString().startsWith("n-"))
              equipment.setValue("id", session.createObject(Equipment.class, equipment.getSimpleMap("group","store","amount","tmp","zakaz")));
            else session.saveObject(Equipment.class, equipment.getSimpleMap("id","group","store","amount","tmp"));
            
            session.executeUpdate("delete from [EquipmentFactorValue] WHERE [EquipmentFactorValue(equipment)]=?", equipment.getValue("id", Integer.TYPE));
            
            for(Integer factorId:equipment.getValue("factors", new Integer[0])) {
               session.createObject(EquipmentFactorValue.class, PropertyMap.create().setValue("equipment", equipment.getValue("id")).setValue("factor", factorId).setValue("name", equipment.getValue("factor-"+factorId)).getSimpleMap());
            }
          }
        }
      }
      
      ObjectLoader.commitSession(session);
    }catch (Exception ex) {
      ObjectLoader.rollBackSession(session);
      MsgTrash.out(ex);
    }
    
    return true;
  }

  @Override
  public void dispose() {
    super.dispose();
  }
}
