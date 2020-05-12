package division.fx.client.plugins;

import bum.editors.util.ObjectLoader;
import bum.interfaces.Document;
import bum.interfaces.DocumentXMLTemplate;
import bum.interfaces.ProductDocument.ActionType;
import bum.interfaces.Service;
import division.fx.DivisionTextField;
import division.fx.FXScriptPanel;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.PropertyMap;
import division.fx.FXUtility;
import division.fx.util.MsgTrash;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.net.URL;
import java.time.Period;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.converter.IntegerStringConverter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import mapping.MappingObject.Type;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import util.filter.local.DBFilter;

public class ActionDocuments extends FXPlugin {
  @FXML private SplitPane split;
  
  @FXML private Accordion accordion;
  
  @FXML private TitledPane propcPane;
  @FXML private TreeView<PropertyMap> procTree;
  
  @FXML private TitledPane buhPane;
  @FXML private TreeView<PropertyMap> buhTree;
  
  @FXML private BorderPane rightPanel;
  
  @FXML private SplitMenuButton addButton;
  @FXML private CustomMenuItem  addAction;
  @FXML private CustomMenuItem  addDocument;
  @FXML private CustomMenuItem  addTemplate;
  @FXML private Button          remove;
  
  // Document Panel
        private Node documentPanel;
        private ToggleGroup ownerGroup = new ToggleGroup();
        private ToggleGroup zeroGroup  = new ToggleGroup();
  @FXML private VBox onlyForSystemPanel;
  @FXML private TitleBorderPane ownerPanel;
  @FXML private TitleBorderPane movePanel;
  @FXML private CheckBox moveCashTmc;
  @FXML private TitleBorderPane numbering;
  @FXML private TextArea documentDescription;
  @FXML private DivisionTextField startNumber;
  @FXML private RadioButton everyYear;
  @FXML private TextField prefix;
  @FXML private RadioButton everyMonth;
  @FXML private TextField suffixSplit;
  @FXML private TextField documentName;
  @FXML private TextField suffix;
  @FXML private RadioButton everyDay;
  @FXML private RadioButton customerOwner;
  @FXML private CheckBox moveNoCashMoney;
  @FXML private CheckBox replaceNumber;
  @FXML private ChoiceBox<String> prefixDate;
  @FXML private RadioButton sellerOwner;
  @FXML private CheckBox moveNoCashTmc;
  @FXML private CheckBox moveCashMoney;
  @FXML private ChoiceBox<String> ndsPayer;
  @FXML private TitleBorderPane toZero;
  @FXML private TextField prefixSplit;
  @FXML private CheckBox mainDocument;
  @FXML private ChoiceBox<String> suffixDate;
  
  private final String[] formats = new String[]{
    " ",
    "гг","гггг",
    "ггмм","гг/мм","гг.мм","гг-мм",
    "ммгг","мм/гг","мм.гг","мм-гг",
    "ггггмм","гггг/мм","гггг.мм","гггг-мм",
    "ммгггг","мм/гггг","мм.гггг","мм-гггг",
    "ггммдд","гг/мм/дд","гг.мм.дд","гг-мм-дд",
    "ммггдд","мм/гг/дд","мм.гг.дд","мм-гг-дд",
    "ггггммдд","гггг/мм/дд","гггг.мм.дд","гггг-мм-дд",
    "ммггггдд","мм/гггг/дд","мм.гггг.дд","мм-гггг-дд",
    "ггддмм","гг/дд/мм","гг.дд.мм","гг-дд-мм",
    "ммддгг","мм/дд/гг","мм.дд.гг","мм-дд-гг",
    "ггггддмм","гггг/дд/мм","гггг.дд.мм","гггг-дд-мм",
    "ммддгггг","мм/дд/гггг","мм.дд.гггг","мм-дд-гггг",
    "ддггмм","дд/гг/мм","дд.гг.мм","дд-гг-мм",
    "ддммгг","дд/мм/гг","дд.мм.гг","дд-мм-гг",
    "ддггггмм","дд/гггг/мм","дд.гггг.мм","дд-гггг-мм",
    "ддммгггг","дд/мм/гггг","дд.мм.гггг","дд-мм-гггг"
  };
  
  // Script Panel
  private FXScriptPanel scriptPanel = new FXScriptPanel(SyntaxConstants.SYNTAX_STYLE_GROOVY);
  /*private final SwingNode         scriptSwingNode = new SwingNode();
  private final JComboBox<String> scriptLanguage  = new JComboBox();
  private final RSyntaxTextArea   scriptText      = new RSyntaxTextArea();
  private final RTextScrollPane   scriptScroll    = new RTextScrollPane(scriptText);*/
  
  // Template Panel
  private FXScriptPanel templatePanel = new FXScriptPanel(SyntaxConstants.SYNTAX_STYLE_HTML);
  /*private final SwingNode         templateSwingNode = new SwingNode();
  private final JComboBox<String> templateLanguage  = new JComboBox();
  private final RSyntaxTextArea   templateXMLText   = new RSyntaxTextArea();
  private final RTextScrollPane   templateScroll    = new RTextScrollPane(templateXMLText);
  
  private final TextField         templateName         = new TextField();
  private final TextArea          templateDescription  = new TextArea();
  private final VBox              templatePanel        = new VBox(10, templateName, templateDescription, templateSwingNode);*/
  
  private ObservableList<PropertyMap> memento = FXCollections.observableArrayList();
  private ObservableList<PropertyMap> current = FXCollections.observableArrayList();

  @Override
  public void start() {   
    initComponents();
    initEvents();
    initData();
    show();
  }

  @Override
  public void finalize() throws Throwable {
    if(currentTree().getSelectionModel().getSelectedItem() != null)
      currentTree().getSelectionModel().getSelectedItem().getValue().unbindAll();
    saveData();
    super.finalize();
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    super.initialize(location, resources); //To change body of generated methods, choose Tools | Templates.
  }
  
  private void initComponents() {
    buhTree.getRoot().getChildren().clear();
    procTree.getRoot().getChildren().clear();
    
    accordion.setExpandedPane(buhPane);
    
    documentPanel = loadPanel("document.fxml");
    
    /*VBox.setVgrow(templateSwingNode, Priority.ALWAYS);
    templatePanel.setPadding(new Insets(5));
    templateName.setPromptText("Наименование...");
    templateDescription.setPromptText("Описание...");
    templateDescription.setMaxHeight(75);
    templateDescription.setMinHeight(75);
    templateDescription.setPrefHeight(75);*/
    
    //loadScriptSwingNode();
    //loadTemplateSwingNode();
    
    buhTree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    procTree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    
    
    
    movePanel.setTitle(moveChoice);
    movePanel.getCenter().disableProperty().bind(moveChoice.getSelectionModel().selectedIndexProperty().isNotEqualTo(0));
    
    toZero.setTitle(toZeroCheckBox);
    //movePanel.getCenter().disableProperty().bind(((CheckBox)movePanel.getTitleLabel().getGraphic()).selectedProperty().not());
    toZero.getCenter().disableProperty().bind(toZeroCheckBox.selectedProperty().not());
    
    ownerGroup.getToggles().addAll(sellerOwner,customerOwner);
    zeroGroup.getToggles().addAll(everyDay,everyMonth,everyYear);
    
    prefixDate.getItems().setAll(formats);
    suffixDate.getItems().setAll(formats);
    
    startNumber.setConverter(new IntegerStringConverter());
  }
  
  ChoiceBox<String> moveChoice = new ChoiceBox<>(FXCollections.observableArrayList("ДА","НЕТ","НЕ ВАЖНО"));
  CheckBox toZeroCheckBox = new CheckBox("Обнулять");
  
  ChangeListener treeChangeListener = (ChangeListener<TreeItem<PropertyMap>>) (ObservableValue<? extends TreeItem<PropertyMap>> observable, TreeItem<PropertyMap> oldValue, TreeItem<PropertyMap> newValue) -> {
    if(oldValue != null)
      oldValue.getValue().unbindAll();
    if(newValue != null) {
      switch((String)newValue.getValue().getValue("type")) {
        case "DOCUMENT":
          showDocument(newValue);
          break;
        case "SCRIPT":
          showScript(newValue);
          break;
        case "TEMPLATE":
          showTemplate(newValue);
          break;
        default:
          showPanel(null);
          break;
      }
    }else showPanel(null);
  };
  
  private void initEvents() {
    addDocument.setOnAction(e -> addDocument());
    //remove.setOnAction(e -> removeObject());
    //addTemplate.setOnAction(e -> addTemplate());
    
    toZeroCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(!oldValue && newValue && !everyDay.isSelected() && !everyMonth.isSelected() && !everyYear.isSelected())
        everyMonth.setSelected(true);
    });
    
    buhPane.expandedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(!newValue) {
        buhTree.getSelectionModel().clearSelection();
      }
    });
    
    propcPane.expandedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      if(!newValue) {
        procTree.getSelectionModel().clearSelection();
      }
    });
    
    buhTree.getSelectionModel().selectedItemProperty().addListener(treeChangeListener);
    procTree.getSelectionModel().selectedItemProperty().addListener(treeChangeListener);
    
    addButton.addEventHandler(ComboBoxBase.ON_SHOWING, e -> {
      TreeView<PropertyMap> tree = currentTree();
      if(tree != null) {
        addAction.setDisable(propcPane.isExpanded());
        
        TreeItem<PropertyMap> item = tree.getSelectionModel().getSelectedItem();
        addDocument.setDisable(propcPane.isExpanded() ? false : item == null);
        addTemplate.setDisable(item == null || item.getValue().getValue("type").equals("EVENT"));
        
        ((Label)addDocument.getContent()).setText("Документ");
        ((Label)addTemplate.getContent()).setText("Печатную форму");
        
        if(!addDocument.isDisable()) {
          TreeItem<PropertyMap> event = getItem(tree, "EVENT");
          if(event != null)
            ((Label)addDocument.getContent()).setText("Документ для события \""+event.getValue().getValue("actionType")+"\"");
        }

        if(!addTemplate.isDisable()) {
          TreeItem<PropertyMap> document = getItem(tree, "DOCUMENT");
          if(document != null)
            ((Label)addTemplate.getContent()).setText("Печатную форму для документа \""+document.getValue().getValue("name")+"\"");
        }
      }else {
        addAction.setDisable(true);
        addDocument.setDisable(true);
        addTemplate.setDisable(true);
      }
    });
    
    current.addListener((ListChangeListener.Change<? extends PropertyMap> c) -> {
      while(c.next()) {
        if(c.wasAdded()) {
          c.getAddedSubList().stream().forEach(doc -> {
            TreeItem<PropertyMap> documentRootItem = null;
            
            //Если документ системный
            if(doc.getValue("system", boolean.class)) {
              //Проверяем есть ли такое событие в дереве
              for(TreeItem<PropertyMap> ac:buhTree.getRoot().getChildren()) {
                if(ac.getValue().getValue("type").equals("ACTION") && ac.getValue().getValue("actionType", ActionType.class) == doc.getValue("actionType", ActionType.class)) {
                  documentRootItem = ac;
                  break;
                }
              }
              //если события нет, то добавляем его
              if(documentRootItem == null) {
                Label actionLabel = new Label(doc.getValue("actionType", ActionType.class).toString());
                actionLabel.getStyleClass().add("action");
                buhTree.getRoot().getChildren().add(documentRootItem = new TreeItem<>(PropertyMap.create().setValue("name", doc.getValue("actionType", ActionType.class).toString()).setValue("type", "ACTION").setValue("actionType",doc.getValue("actionType", ActionType.class)), actionLabel));
                documentRootItem.setExpanded(true);
              }
            }else documentRootItem = procTree.getRoot();
            
            Label documentLabel = new Label();
            documentLabel.getStyleClass().add("document");
            documentLabel.textProperty().bind(doc.get("name"));
            TreeItem<PropertyMap> document = new TreeItem<>(doc.toStringProperty(null), documentLabel);

            doc.get("actionConfirm").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
              if((boolean)newValue)
                documentLabel.getStyleClass().add("action-confirm");
              else documentLabel.getStyleClass().remove("action-confirm");
            });

            if(doc.getValue("actionConfirm", false))
              documentLabel.getStyleClass().add("action-confirm");

            //Добавляем шаблоны
            TreeItem<PropertyMap> templates = new TreeItem(PropertyMap.create().setValue("name", "Шаблоны").setValue("type", "TEMPLATES"));
            document.getChildren().add(templates);
            document.getValue().getList("templates-list").addListener((ListChangeListener.Change<? extends PropertyMap> tc) -> {
              while(tc.next()) {
                if(tc.wasAdded()) {
                  tc.getAddedSubList().stream().forEach(template -> {
                    Label templateItemLabel = new Label();
                    templateItemLabel.getStyleClass().add("template-item");
                    templateItemLabel.textProperty().bind(template.get("name"));
                    templates.getChildren().add(new TreeItem(template.toStringProperty(null), templateItemLabel));
                  });
                }
              }
            });

            //Добавляем скрипт
            Label scriptLabel = new Label("Скрипт");
            scriptLabel.getStyleClass().add("script");
            TreeItem<PropertyMap> script = new TreeItem(PropertyMap.create().setValue("type", "SCRIPT").put("script", document.getValue().get("script")).put("scriptLanguage", document.getValue().get("scriptLanguage")), scriptLabel);
            document.getChildren().add(script);

            //добавляем в root документ
            documentRootItem.getChildren().add(document);
          });
        }
      }
    });
  }

  private void addDocument() {
    PropertyMap doc = PropertyMap.create()
            .setValue("system", currentTree().equals(buhTree))
            .setValue("type", "DOCUMENT")
            .setValue("name", "новый документ");
    if(currentTree().equals(buhTree)) {
      TreeItem<PropertyMap> action = currentTree().getSelectionModel().getSelectedItem();
      while(!action.getValue().getValue("type").equals("ACTION"))
        action = action.getParent();
      doc.setValue("actionType", action.getValue().getValue("actionType"));
    }
    current.add(doc);
  }
  
  class ScriptDocumentListener implements DocumentListener {
    Property prop;
    RSyntaxTextArea com;
    
    public ScriptDocumentListener(Property prop, RSyntaxTextArea com) {
      this.prop = prop;
      this.com = com;
    }
    
    @Override
    public void insertUpdate(DocumentEvent e) {
      prop.setValue(com.getText());
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      prop.setValue(com.getText());
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
      prop.setValue(com.getText());
    }
  }
  
  class ScriptLanguageItemListener implements ItemListener {
    Property prop;
    
    public ScriptLanguageItemListener(Property prop) {
      this.prop = prop;
    }
    
    @Override
    public void itemStateChanged(ItemEvent e) {
      prop.setValue(e.getItem());
    }
  }
  
  ScriptDocumentListener templateXMLDocumentListener = null;
  ScriptDocumentListener scriptDocumentListener = null;
  ScriptLanguageItemListener scriptLanguageItemListener = null;
  
  private void showTemplate(TreeItem<PropertyMap> item) {
    templatePanel.setText(item.getValue().getString("XML"));
    showPanel(templatePanel);
    //showPanel(templatePanel);
    /*templateName.setText(item.getValue().getValue("template-name", String.class));
    item.getValue().get("template-name").bind(templateName.textProperty());
    templateDescription.setText(item.getValue().getValue("description", String.class));
    item.getValue().get("description").bind(templateDescription.textProperty());
    SwingUtilities.invokeLater(() -> {
      if(templateXMLDocumentListener != null)
        templateXMLText.getDocument().removeDocumentListener(templateXMLDocumentListener);
      templateXMLText.setText(item.getValue().getValue("XML") == null ? "" : item.getValue().getValue("XML", String.class));
      templateXMLText.getDocument().addDocumentListener(templateXMLDocumentListener = new ScriptDocumentListener(item.getValue().get("XML"), templateXMLText));
      templateLanguage.setSelectedItem(SyntaxConstants.SYNTAX_STYLE_HTML);
    });
    showPanel(templatePanel);*/
  }
  
  private void showScript(TreeItem<PropertyMap> item) {
    scriptPanel.setText(item.getValue().getString("script"));
    showPanel(scriptPanel);
    //showPanel(scriptPanel);
    /*scriptText.setText("");
    SwingUtilities.invokeLater(() -> {
      if(scriptDocumentListener != null)
        scriptText.getDocument().removeDocumentListener(scriptDocumentListener);
      scriptText.setText(item.getValue().getValue("script") == null ? "" : (String)item.getValue().getValue("script"));
      scriptText.getDocument().addDocumentListener(scriptDocumentListener = new ScriptDocumentListener(item.getValue().get("script"), scriptText));
      
      if(scriptLanguageItemListener != null)
        scriptLanguage.removeItemListener(scriptLanguageItemListener);
      scriptLanguage.setSelectedItem(item.getValue().getValue("scriptLanguage"));
      scriptLanguage.addItemListener(scriptLanguageItemListener = new ScriptLanguageItemListener(item.getValue().get("scriptLanguage")));
    });
    showPanel(scriptSwingNode);*/
  }
  
  private void showDocument(TreeItem<PropertyMap> item) {
    documentName.setText(item.getValue().getValue("name", String.class));
    item.getValue().get("name").bind(documentName.textProperty());
    
    documentDescription.setText(item.getValue().getValue("description", String.class));
    item.getValue().get("description").bind(documentDescription.textProperty());
    
    if(!(boolean)item.getValue().getValue("system"))
      ((VBox)documentPanel).getChildren().remove(onlyForSystemPanel);
    else if(!((VBox)documentPanel).getChildren().contains(onlyForSystemPanel))
      ((VBox)documentPanel).getChildren().add(((VBox)documentPanel).getChildren().size()-1,onlyForSystemPanel);
    
    if((boolean)item.getValue().getValue("system")) {
      mainDocument.setSelected(item.getValue().getValue("actionConfirm", false));
      item.getValue().get("actionConfirm").bind(mainDocument.selectedProperty());
      
      moveChoice.getSelectionModel().select(item.getValue().isNull("movable") ? 2 : item.getValue().is("movable") ? 0 : 1);
      item.getValue().get("movable").bind(Bindings.createObjectBinding(() -> 
              moveChoice.getSelectionModel().getSelectedIndex() == 2 ? null : moveChoice.getSelectionModel().getSelectedIndex() != 1, 
              moveChoice.getSelectionModel().selectedIndexProperty()));
      
      moveCashMoney.setSelected(item.getValue().getValue("moneyCash", false));
      item.getValue().get("moneyCash").bind(moveCashMoney.selectedProperty());
      
      moveNoCashMoney.setSelected(item.getValue().getValue("moneyCashLess", false));
      item.getValue().get("moneyCashLess").bind(moveNoCashMoney.selectedProperty());
      
      moveCashTmc.setSelected(item.getValue().getValue("tmcCash", false));
      item.getValue().get("tmcCash").bind(moveCashTmc.selectedProperty());
      
      moveNoCashTmc.setSelected(item.getValue().getValue("tmcCashLess", false));
      item.getValue().get("tmcCashLess").bind(moveNoCashTmc.selectedProperty());

      sellerOwner.setSelected(item.getValue().getValue("documentSource", Service.Owner.class).equals(Service.Owner.SELLER));
      customerOwner.setSelected(item.getValue().getValue("documentSource", Service.Owner.class).equals(Service.Owner.CUSTOMER));
      item.getValue().get("documentSource").bind(Bindings.createObjectBinding(() -> sellerOwner.isSelected() ? Service.Owner.SELLER : Service.Owner.CUSTOMER, sellerOwner.selectedProperty(), customerOwner.selectedProperty()));
      
      ndsPayer.getSelectionModel().select(item.getValue().getValue("ndsPayer") == null ? 2 : item.getValue().getValue("ndsPayer", Boolean.class) ? 0 : 1);
      item.getValue().get("ndsPayer").bind(Bindings.createObjectBinding(() -> 
              ndsPayer.getSelectionModel().getSelectedIndex() == 2 ? null : ndsPayer.getSelectionModel().getSelectedIndex() == 0, 
              ndsPayer.getSelectionModel().selectedIndexProperty()));
    }

    prefix.setText(item.getValue().getValue("prefix", String.class));
    item.getValue().get("prefix").bind(prefix.textProperty());
    
    prefixDate.getSelectionModel().select(item.getValue().getValue("prefixTypeFormat", String.class));
    item.getValue().get("prefixTypeFormat").bind(prefixDate.getSelectionModel().selectedItemProperty());
    
    prefixSplit.setText(item.getValue().getValue("prefixSplit", String.class));
    item.getValue().get("prefixSplit").bind(prefixSplit.textProperty());

    suffix.setText(item.getValue().getValue("suffix", String.class));
    item.getValue().get("suffix").bind(suffix.textProperty());
    
    suffixDate.getSelectionModel().select(item.getValue().getValue("suffixTypeFormat", String.class));
    item.getValue().get("suffixTypeFormat").bind(suffixDate.getSelectionModel().selectedItemProperty());
    
    suffixSplit.setText(item.getValue().getValue("suffixSplit", String.class));
    item.getValue().get("suffixSplit").bind(suffixSplit.textProperty());

    toZeroCheckBox.setSelected(item.getValue().getValue("periodForZero") != null);
    if(item.getValue().getValue("periodForZero") != null) {
      Period p = item.getValue().getValue("periodForZero", Period.class);
      everyDay.setSelected(p.getDays() == 1);
      everyMonth.setSelected(p.getMonths() == 1);
      everyYear.setSelected(p.getYears() == 1);
    }
    
    item.getValue().get("periodForZero").bind(Bindings.createObjectBinding(() -> !toZeroCheckBox.isSelected() ? null : 
            everyDay.isSelected() ? Period.ofDays(1) : everyMonth.isSelected() ? Period.ofMonths(1) : everyYear.isSelected() ? Period.ofYears(1) : Period.ZERO, 
            toZeroCheckBox.selectedProperty(), zeroGroup.selectedToggleProperty()));

    replaceNumber.setSelected(item.getValue().getValue("grabFreeNumber", false));
    item.getValue().get("grabFreeNumber").bind(replaceNumber.selectedProperty());
    
    startNumber.setValue(item.getValue().getValue("startNumber"));
    item.getValue().get("startNumber").bind(startNumber.valueProperty());
    
    showPanel(documentPanel);
  }
  
  public TreeView<PropertyMap> currentTree() {
    return propcPane.isExpanded() ? procTree : buhPane.isExpanded() ? buhTree : null;
  }
  
  private void saveData() {
    if(!current.equals(memento)) {
      if(new Alert(Alert.AlertType.CONFIRMATION, "Сохранить изменения?", ButtonType.YES, ButtonType.NO).showAndWait().get() == ButtonType.YES) {
        try {
          for(PropertyMap document:current) {
            if(document.containsKey("id")) {
              boolean update = false;
              for(PropertyMap m:memento) {
                update = m.getValue("id").equals(document.getValue("id")) && !m.equals(document);
                if(update)
                  break;
              }
              
              if(update)
                ObjectLoader.update(Document.class, document.copy().remove("templates", "templates-list", "type"));
            }
          }
        }catch(Exception ex) {
          MsgTrash.out(ex);
        }
      }
    }
  }
  
  /*private void loadScriptSwingNode() {
    SwingUtilities.invokeLater(() -> {
      scriptText.setCodeFoldingEnabled(true);
      scriptText.setAntiAliasingEnabled(true);
      scriptScroll.setFoldIndicatorEnabled(true);
      scriptLanguage.setMinimumSize(new Dimension(150, 20));
      scriptLanguage.setPreferredSize(new Dimension(150, 20));
      scriptLanguage.addItemListener((ItemEvent e) -> scriptText.setSyntaxEditingStyle(e.getItem().toString()));
      try {
        for(Field field:SyntaxConstants.class.getDeclaredFields())
          scriptLanguage.addItem(field.get(null).toString());
      }catch(Exception e){}
      JPanel panel = new JPanel(new BorderLayout(5,5));
      panel.add(scriptLanguage, BorderLayout.NORTH);
      panel.add(scriptScroll, BorderLayout.CENTER);
      scriptSwingNode.setContent(panel);
    });
  }
  
  private void loadTemplateSwingNode() {
    SwingUtilities.invokeLater(() -> {
      templateXMLText.setCodeFoldingEnabled(true);
      templateXMLText.setAntiAliasingEnabled(true);
      templateScroll.setFoldIndicatorEnabled(true);
      templateLanguage.setMinimumSize(new Dimension(150, 20));
      templateLanguage.setPreferredSize(new Dimension(150, 20));
      templateLanguage.addItemListener((ItemEvent e) -> templateXMLText.setSyntaxEditingStyle(e.getItem().toString()));
      try {
        for(Field field:SyntaxConstants.class.getDeclaredFields())
          templateLanguage.addItem(field.get(null).toString());
      }catch(Exception e){}
      JPanel panel = new JPanel(new BorderLayout(5,5));
      panel.add(templateLanguage, BorderLayout.NORTH);
      panel.add(templateScroll, BorderLayout.CENTER);
      templateSwingNode.setContent(panel);
    });
  }*/
  
  private Node loadPanel(String panelFxml) {
    try {
      FXMLLoader loader = FXUtility.getLoader(new File(getFxml()).getParentFile().getAbsolutePath()+File.separator+panelFxml);
      loader.setController(this);
      return loader.load();
    }catch(Exception ex) {
      MsgTrash.out(ex);
    }
    return null;
  }
  
  public void showPanel(Node node) {
    if(node == null || !node.equals(rightPanel.getCenter())) {
      new Timeline(new KeyFrame(Duration.millis(200), e -> {
        rightPanel.setOpacity(0);
        rightPanel.setCenter(node);
        new Timeline(new KeyFrame(Duration.millis(200), new KeyValue(rightPanel.opacityProperty(), 1))).play();
      }, new KeyValue(rightPanel.opacityProperty(), 0))).play();
    }
  }
  
  private TreeItem<PropertyMap> getItem(TreeView<PropertyMap> tree, String type) {
    TreeItem<PropertyMap> item = tree.getSelectionModel().getSelectedItem();
    while(item != null && !item.equals(tree.getRoot()) && !item.getValue().getValue("type").equals(type))
      item = item.getParent();
    return item == null || item.equals(tree.getRoot()) ? null : item;
  }

  @Override
  public ObservableList<Node> storeControls() {
    ObservableList<Node> sc = super.storeControls();
    sc.add(split);
    return sc;
  }
  
  private void initData(Integer... ids) {
    DBFilter filter = DBFilter.create(Document.class);
    if(ids.length > 0)
      filter.AND_IN("id", ids);
    ObjectLoader.fillList(filter, current,
            "id",
            "name",
            "description",
            
            "system",
            
            "script",
            "scriptLanguage",
            
            "documentSource",
            "ndsPayer",
            
            "actionType",
            "actionConfirm",
            
            "movable",
            "moneyCash",
            "moneyCashLess",
            "tmcCash",
            "tmcCashLess",
            
            "prefix",
            "prefixTypeFormat",
            "prefixSplit",
            "suffixSplit",
            "suffixTypeFormat",
            "suffix",
            "periodForZero",
            "startNumber",
            "grabFreeNumber",
            
            "templates");
    
    ObservableList<Integer> templs = FXCollections.observableArrayList();
    current.stream().forEach(doc -> templs.addAll(doc.setValue("type", "DOCUMENT").getValue("templates", new Integer[0])));
    ObjectLoader.getList(DBFilter.create(DocumentXMLTemplate.class).AND_IN("id", templs.toArray(new Integer[0])).AND_EQUAL("companyPartition", null).AND_EQUAL("type", Type.CURRENT).AND_EQUAL("tmp", false), 
            "id","name","main","document","XML","description").stream().forEach(d -> current.stream().filter(doc -> doc.getValue("id").equals(d.getValue("document")))
                    .forEach(doc -> doc.getList("templates-list").add(d.setValue("type", "TEMPLATE"))));
    memento = PropertyMap.copyList(current);
  }
}