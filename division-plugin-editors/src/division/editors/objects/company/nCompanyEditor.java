package division.editors.objects.company;

import bum.editors.EditorGui;
import bum.editors.EditorListener;
import bum.editors.MainObjectEditor;
import bum.editors.TableEditor;
import bum.editors.util.DivisionTarget;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Account;
import bum.interfaces.CFC;
import bum.interfaces.Company;
import bum.interfaces.CompanyPartition;
import bum.interfaces.OwnershipType;
import bum.interfaces.Place;
import division.border.ComponentTitleBorder;
import division.border.LinkBorder;
import division.editors.objects.AccountEditor;
import division.editors.tables.CFCTableEditor;
import division.fx.PropertyMap;
import division.fx.controller.store.FXCompanyStoreTable;
import division.swing.DivisionComboBox;
import division.swing.DivisionTextArea;
import division.swing.DivisionTextField;
import division.swing.ImageBox;
import division.swing.LinkLabel;
import division.swing.actions.LinkBorderActionEvent;
import division.swing.dnd.DNDPanel;
import division.swing.guimessanger.Messanger;
import division.swing.tree.Node;
import division.swing.tree.Tree;
import division.util.Fprinter;
import division.util.GzipUtil;
import division.util.Utility;
import division.xml.Document;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.File;
import java.rmi.RemoteException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.TableModelEvent;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.tree.TreePath;
import mapping.MappingObject;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class nCompanyEditor extends MainObjectEditor {
  private final LinkLabel         cfcLabel            = new LinkLabel();
  private Integer[]               cfcs                = new Integer[0];
  
  private final LinkBorder        ownershipBorder     = new LinkBorder("Форма собственности...");
  private final DivisionTextArea  nameField           = new DivisionTextArea("Наименование организации...");
  private final JScrollPane       nameScroll          = new JScrollPane(nameField);
  private final DivisionTextField shotNameField       = new DivisionTextField("Сокращённое наименование...");
  
  private final DivisionTextField innField            = new DivisionTextField(DivisionTextField.Type.INTEGER);
  private final DivisionTextField okvedField          = new DivisionTextField(DivisionTextField.Type.FLOAT);
  private final DivisionTextField ogrnField           = new DivisionTextField(DivisionTextField.Type.INTEGER);
  private final JCheckBox         ndsPayerField       = new JCheckBox("Плательщик НДС");
  
  private final LinkBorder        chifBorder          = new LinkBorder("Руководитель...");
  private final DivisionTextField businessReasonField = new DivisionTextField(DivisionTextField.Type.ALL);
  private final DivisionTextField chifNameField       = new DivisionTextField(DivisionTextField.Type.ALL);
  private final DivisionTextField bookKeeperNameField = new DivisionTextField(DivisionTextField.Type.ALL);
  
  private final JTabbedPane       partitionTab        = new JTabbedPane();
  /**
   * ПОДРАЗДЕЛЕНИЕ
   */
  private DivisionTarget          partitionTarget     = null;
  private final LinkBorder        partitionBorder     = new LinkBorder("");
  private final JPanel            dataPartitionPanel  = new JPanel(new GridBagLayout());
  
  private final DivisionTextField partitionNameField  = new DivisionTextField(DivisionTextField.Type.ALL);
  private final DivisionTextField kppField            = new DivisionTextField(DivisionTextField.Type.INTEGER);
  
  private final DNDPanel          urAddressPanel      = new DNDPanel("Юридический адрес", new BorderLayout());
  private final DivisionTextArea  urAddressField      = new DivisionTextArea();
  private final JScrollPane       urAddressScroll     = new JScrollPane(urAddressField);
  
  private final DNDPanel          addressPanel        = new DNDPanel("Фактический адрес", new BorderLayout());
  private final DivisionTextArea  addressField        = new DivisionTextArea();
  private final JScrollPane       addressScroll       = new JScrollPane(addressField);
  
  private final DNDPanel          postAddressPanel    = new DNDPanel("Почтовый адрес", new BorderLayout());
  private final DivisionTextArea  postAddressField    = new DivisionTextArea();
  private final JScrollPane       postAddressScroll   = new JScrollPane(postAddressField);
  
  private final DivisionTextField telField            = new DivisionTextField("Телефон...");
  private final DivisionTextField emailField          = new DivisionTextField("Email...");
  private final DivisionTextField contactFioField     = new DivisionTextField("Контактное лицо...");
  private final DivisionTextArea  infoField           = new DivisionTextArea("Дополнительная информация...");
  private final JScrollPane       infoScroll          = new JScrollPane(infoField);
  
  private final ImageBox          logoBox                  = new ImageBox(100, 100, "Логотип организации");
  
  private final ImageBox          stampBox                 = new ImageBox(80, 80, "Печать организации");
  private final ImageBox          chifSignatureBox         = new ImageBox(50, 25, "Подпись руководителя");
  private final ImageBox          bookKeeperSignatureBox   = new ImageBox(50, 25, "Подпись бухгалтера");
  
  private final TableEditor    accountTableEditor     = new TableEditor(
          new String[]{"id","Расчётный счёт","Банк","Основной"},
          new String[]{"id","number","bank_name","current"},
          Account.class,
          AccountEditor.class,
          "счета");
  
  //private final StorePositionTableEditor storeEditor  = new StorePositionTableEditor();
  private final CompanyPartitionStoreTableEditor    storePanel        = new CompanyPartitionStoreTableEditor();
  private final CompanyPartitionDocumentTableEditor documentPanel     = new CompanyPartitionDocumentTableEditor();
  
  private Integer partitionId = null;
  private Integer chifPlaceId = null;
  private Integer ownershipId = null;
  
  //private final division.xml.Document document = division.xml.Document.load("conf"+File.separator+"machinery_configuration.xml", true);
  private final Document config = Document.load("conf"+File.separator+"configuration.xml");
  private final JCheckBox fiscalCheck = new JCheckBox("Фискальный регистратор");
  private final DivisionComboBox fiscal = new DivisionComboBox(Fprinter.getPrinters());
  private final JButton zReport  = new JButton("Снять Z-отчёт");
  private final JButton xReport  = new JButton("Снять X-отчёт");
  private final JSpinner  ndsCount = new JSpinner(new SpinnerNumberModel(0.00, 0.00, 99.99, 0.01));
  
  //private FXCompanyStore storeTree;
  private FXCompanyStoreTable storeTree;
  private final JFXPanel storeTreePanel = new JFXPanel();

  public nCompanyEditor() {
    initComponents();
    initEvents();
  }

  private void initComponents() {
    addSubEditorToStore(storePanel,"storeEditor");
    addSubEditorToStore(documentPanel, "companyPartitionDocumentTableEditorForCompanyEditor");
    
    nameField.setWrapStyleWord(true);
    nameField.setLineWrap(true);
    nameField.setFont(new Font("Arial", Font.BOLD, 16));
    nameField.setMargin(new Insets(5, 5, 5, 5));
    
    ownershipBorder.setTitleFont(new Font("Arial", Font.BOLD, 16));
    
    JPanel namePanel = new JPanel(new GridBagLayout());
    namePanel.setBorder(ownershipBorder);
    namePanel.add(logoBox,       new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
    namePanel.add(nameScroll,    new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
    namePanel.add(shotNameField, new GridBagConstraints(0, 1, 2, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
    
    chifBorder.setTitleFont(new Font("Arial", Font.BOLD, 16));
    
    JPanel chifPanel = new JPanel(new GridBagLayout());
    chifPanel.setBorder(chifBorder);
    chifPanel.add(new JLabel("Руководитель"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
    chifPanel.add(chifNameField,              new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
    chifPanel.add(chifSignatureBox,           new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
    
    chifPanel.add(stampBox,                   new GridBagConstraints(3, 0, 1, 3, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
    
    chifPanel.add(new JLabel("Основание действий"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
    chifPanel.add(businessReasonField,              new GridBagConstraints(1, 1, 2, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
    
    chifPanel.add(new JLabel("Бухгалтер"),   new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
    chifPanel.add(bookKeeperNameField,       new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
    chifPanel.add(bookKeeperSignatureBox,    new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
    
    innField.setMinimumSize(new Dimension(100, innField.getPreferredSize().height));
    okvedField.setMinimumSize(new Dimension(100, okvedField.getPreferredSize().height));
    ogrnField.setMinimumSize(new Dimension(100, ogrnField.getPreferredSize().height));
    
    innField.setPreferredSize(new Dimension(100, innField.getPreferredSize().height));
    okvedField.setPreferredSize(new Dimension(100, okvedField.getPreferredSize().height));
    ogrnField.setPreferredSize(new Dimension(100, ogrnField.getPreferredSize().height));
    
    JPanel regPanel = new JPanel(new GridBagLayout());
    regPanel.add(new JLabel("ИНН"),   new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
    regPanel.add(innField,            new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
    
    regPanel.add(new JLabel("ОКВЭД"), new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
    regPanel.add(okvedField,          new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
    
    regPanel.add(new JLabel("ОГРН"),  new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
    regPanel.add(ogrnField,           new GridBagConstraints(5, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
    
    regPanel.add(ndsPayerField,       new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
    
    regPanel.add(chifPanel,           new GridBagConstraints(0, 1, 7, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
    
    JPanel topPanel = new JPanel(new GridBagLayout());
    topPanel.add(namePanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
    topPanel.add(regPanel,  new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
    
    kppField.setMinimumSize(new Dimension(100, kppField.getPreferredSize().height));
    kppField.setPreferredSize(new Dimension(100, kppField.getPreferredSize().height));
    
    JPanel partitionPanel = new JPanel(new GridBagLayout());
    partitionPanel.setBorder(partitionBorder);
    partitionBorder.setTitleFont(new Font("Dialog", Font.BOLD, 16));
    
    urAddressScroll.setPreferredSize(new Dimension(100, 75));
    urAddressScroll.setMinimumSize(new Dimension(100, 75));
    addressScroll.setPreferredSize(new Dimension(100, 75));
    addressScroll.setMinimumSize(new Dimension(100, 75));
    postAddressScroll.setPreferredSize(new Dimension(100, 75));
    postAddressScroll.setMinimumSize(new Dimension(100, 75));
    
    urAddressField.setWrapStyleWord(true);
    urAddressField.setLineWrap(true);
    
    addressField.setWrapStyleWord(true);
    addressField.setLineWrap(true);
    
    postAddressField.setWrapStyleWord(true);
    postAddressField.setLineWrap(true);
    
    urAddressPanel.getBorder().setDeactiveLinkTitleColor(Color.DARK_GRAY);
    addressPanel.getBorder().setDeactiveLinkTitleColor(Color.DARK_GRAY);
    postAddressPanel.getBorder().setDeactiveLinkTitleColor(Color.DARK_GRAY);
    
    urAddressPanel.getBorder().setLinkBorder(false);
    addressPanel.getBorder().setLinkBorder(false);
    postAddressPanel.getBorder().setLinkBorder(false);

    urAddressPanel.add(urAddressScroll,     BorderLayout.CENTER);
    addressPanel.add(addressScroll,         BorderLayout.CENTER);
    postAddressPanel.add(postAddressScroll, BorderLayout.CENTER);
    
    JPanel aPanel = new JPanel(new GridBagLayout());
    aPanel.add(urAddressPanel,   new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
    aPanel.add(addressPanel,     new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
    aPanel.add(postAddressPanel, new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
    
    JPanel nPanel = new JPanel(new GridBagLayout());
    nPanel.add(new JLabel("Наименование"),  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
    nPanel.add(partitionNameField,          new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
    nPanel.add(new JLabel("КПП"),           new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
    nPanel.add(kppField,                    new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
    
    dataPartitionPanel.add(nPanel,                      new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
    dataPartitionPanel.add(aPanel,                      new GridBagConstraints(0, 1, 2, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
    
    accountTableEditor.setPrintFunction(false);
    accountTableEditor.setExportFunction(false);
    accountTableEditor.setImportFunction(false);
    accountTableEditor.getToolBar().setOrientation(JToolBar.VERTICAL);
    accountTableEditor.getToolBar().setFloatable(true);
    accountTableEditor.getlPanel().setLayout(new GridBagLayout());
    accountTableEditor.getlPanel().add(accountTableEditor.getToolBar(), new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0,GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
    accountTableEditor.getScroll().setRowHeader(false);
    accountTableEditor.setVisibleOkButton(false);
    accountTableEditor.getStatusBar().setVisible(false);
    accountTableEditor.getGUI().setBorder(BorderFactory.createTitledBorder("Расчётные счета"));
    
    JPanel cPanel = new JPanel(new GridBagLayout());
    cPanel.setBorder(BorderFactory.createTitledBorder("Контакты"));
    cPanel.add(telField,        new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(3,3,3,3), 0, 0));
    cPanel.add(emailField,      new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(3,3,3,3), 0, 0));
    cPanel.add(contactFioField, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(3,3,3,3), 0, 0));
    cPanel.add(infoScroll,      new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0,GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(3,3,3,3), 0, 0));
    
    dataPartitionPanel.add(accountTableEditor.getGUI(), new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
    dataPartitionPanel.add(cPanel,                      new GridBagConstraints(1, 2, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
    
    storePanel.setVisibleOkButton(false);
    
    documentPanel.setVisibleOkButton(false);
    
    JPanel panel = new JPanel(new GridBagLayout());
    JPanel FRPanel = new JPanel(new GridBagLayout());
    FRPanel.setBorder(new ComponentTitleBorder(fiscalCheck, FRPanel, BorderFactory.createLineBorder(Color.DARK_GRAY)));
    FRPanel.add(fiscal,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    FRPanel.add(zReport, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    FRPanel.add(xReport, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    panel.add(FRPanel,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    
    Platform.runLater(() -> {
      storeTreePanel.setScene(new Scene(storeTree = new FXCompanyStoreTable()));
      //storeTreePanel.setScene(new Scene(storeTree = new FXCompanyStore()));
    });
    
    partitionTab.add("Реквизиты",       dataPartitionPanel);
    partitionTab.add("Документооборот", documentPanel.getGUI());
    partitionTab.add("Активы",          storePanel.getGUI());
    partitionTab.add("Активы 2",        storeTreePanel);
    partitionTab.add("ККТ",             panel);

    partitionPanel.add(partitionTab,        new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
    
    getRootPanel().setLayout(new GridBagLayout());
    getRootPanel().add(cfcLabel,       new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    getRootPanel().add(topPanel,       new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    getRootPanel().add(partitionPanel, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
  }

  @Override
  public void store() {
    super.store(); //To change body of generated methods, choose Tools | Templates.
    Platform.runLater(() -> storeTree.store("FXStoreTree-in-nCompany"));
  }

  @Override
  public void load() {
    super.load(); //To change body of generated methods, choose Tools | Templates.
    Platform.runLater(() -> storeTree.load("FXStoreTree-in-nCompany"));
  }

  private void initEvents() {
    addEditorListener(storePanel);
    
    fiscal.addItemListener((ItemEvent e) -> {
      if(e.getStateChange() == ItemEvent.SELECTED)
        saveFiscal();
    });
    
    fiscalCheck.addItemListener((ItemEvent e) -> {
      fiscal.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
      zReport.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
      xReport.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
      if(fiscalCheck.isEnabled())
        saveFiscal();
    });
    
    zReport.addActionListener((ActionEvent e) -> {
      try {
        if(!" ".equals(fiscal.getSelectedItem())) {
          Fprinter.printZReport(fiscal.getSelectedItem().toString());
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    });
    
    xReport.addActionListener((ActionEvent e) -> {
      try {
        if(!" ".equals(fiscal.getSelectedItem())) {
          Fprinter.printXReport(fiscal.getSelectedItem().toString());
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    });
    
    cfcLabel.addActionListener((ActionEvent e) -> {
      CFCTableEditor cfcTableEditor = new CFCTableEditor();
      cfcTableEditor.setName("cfcTableEditor_company");
      cfcTableEditor.getTree().setType(Tree.Type.CHECKBOXSES);
      cfcTableEditor.setAutoLoadAndStore(true);
      
      cfcTableEditor.getTree().setInheritance(false);
      JCheckBox check = new JCheckBox("наследование", false);
      check.addItemListener(i -> {
        cfcTableEditor.getTree().setInheritance(i.getStateChange() == ItemEvent.SELECTED);
      });
      
      cfcTableEditor.setType(MappingObject.Type.CURRENT);
      cfcTableEditor.getToolBar().addSeparator();
      cfcTableEditor.getToolBar().add(check);
      
      cfcTableEditor.addEditorListener(new EditorListener() {
        @Override
        public void initDataComplited(EditorGui editor) {
          for(Integer id:cfcs) {
            Node node = cfcTableEditor.getNodeObject(id);
            if(node != null) {
              node.setEnabled(false);
              node.setSelected(true);
              node.setEnabled(true);
              cfcTableEditor.getTree().expandPath(new TreePath(node.getPath()));
            }
          }
        }
        
        @Override
        public void selectObjects(EditorGui editor, Integer[] ids) {
          cfcs = cfcTableEditor.getCheckedIds();
          String names = "";
          for(List d:ObjectLoader.getData(CFC.class, cfcs, "name"))
            names += " | "+d.get(0);
          cfcLabel.setText(names.substring(3));
        }
      });
      
      cfcTableEditor.initData();
      JDialog dialog = cfcTableEditor.createDialog(this, false);
      dialog.setAlwaysOnTop(true);
      dialog.setVisible(true);
    });
    
    partitionBorder.addActionListener((ActionEvent e) -> {
      Rectangle titleBounds = ((LinkBorderActionEvent)e).getTitleBounds();
      createPartitionMenu().show(((LinkBorder)e.getSource()).getComponent(), titleBounds.x-5, titleBounds.y+titleBounds.height);
    });
    
    chifBorder.addActionListener((ActionEvent e) -> {
      Rectangle titleBounds = ((LinkBorderActionEvent)e).getTitleBounds();
      createChifMenu().show(((LinkBorder)e.getSource()).getComponent(), titleBounds.x-5, titleBounds.y+titleBounds.height);
    });
    
    ownershipBorder.addActionListener((ActionEvent e) -> {
      Rectangle titleBounds = ((LinkBorderActionEvent)e).getTitleBounds();
      createOwnershipMenu().show(((LinkBorder)e.getSource()).getComponent(), titleBounds.x-5, titleBounds.y+titleBounds.height);
    });
  }
  
  @Override
  public void clearData() {
    super.clearData();
    setPartition(null, null);
  }
  
  @Override
  public String commit() throws Exception {
    String msg = commitPartition();
    if(ownershipId == null)
      msg += "\n - не выбрана форма собственности";
    if(nameField.getText().equals(""))
      msg += "\n - не задано наименование организации";
    if(innField.getText().equals(""))
      msg += "\n - не введён ИНН";
    else if(innField.getText().length() > 12 || innField.getText().length() < 10)
      msg += "\n - ИНН введён некорректно";
    
    
    //if(!savePartition())
      //msg += "\n - ошибка при сохранении подразделения";
    
    if(msg == null || msg.equals("")) {
      ((Company)getEditorObject()).setOwnershipType((OwnershipType) ObjectLoader.getObject(OwnershipType.class, ownershipId));
      ((Company)getEditorObject()).setName(nameField.getText());
      ((Company)getEditorObject()).setShotName(shotNameField.getText());
      ((Company)getEditorObject()).setInn(innField.getText());
      ((Company)getEditorObject()).setOkved(okvedField.getText());
      ((Company)getEditorObject()).setOgrn(ogrnField.getText());
      ((Company)getEditorObject()).setNdsPayer(ndsPayerField.isSelected());
      ((Company)getEditorObject()).setChiefPlace((Place) ObjectLoader.getObject(Place.class, chifPlaceId));
      ((Company)getEditorObject()).setChiefName(chifNameField.getText());
      ((Company)getEditorObject()).setBusinessReason(businessReasonField.getText());
      ((Company)getEditorObject()).setBookkeeper(bookKeeperNameField.getText());
      
      ((Company)getEditorObject()).setStamp(GzipUtil.gzip(stampBox.getImage()));
      ((Company)getEditorObject()).setChifSignature(GzipUtil.gzip(chifSignatureBox.getImage()));
      ((Company)getEditorObject()).setBookkeeperSignature(GzipUtil.gzip(bookKeeperSignatureBox.getImage()));
      ((Company)getEditorObject()).setLogo(GzipUtil.gzip(logoBox.getImage()));
    }
    return msg;
  }

  @Override
  public Boolean saveAction() {
    boolean is = super.saveAction();
    if(is) {
      RemoteSession session = null;
      try {
        savePartition();
        Integer companyId = getEditorObject().getId();
        session = ObjectLoader.createSession();
        session.executeUpdate("DELETE FROM [CFC(companys):table] WHERE [CFC(companys):target]="+companyId);
        Object[] params = new Object[0];
        String query = "INSERT INTO [CFC(companys):table] ([CFC(companys):object],[CFC(companys):target]) VALUES ";
        for(Integer id:cfcs) {
          query += "(?,?),";
          params = ArrayUtils.add(params, id);
          params = ArrayUtils.add(params, companyId);
        }
        session.executeUpdate(query.substring(0, query.length()-1), params);
        ObjectLoader.commitSession(session);
      }catch(Exception ex) {
        ObjectLoader.rollBackSession(session);
      }
    }
    return is;
  }

  @Override
  public void initData() throws Exception {
    Integer id = getEditorObject().getId();
    List<List> data = ObjectLoader.getData(Company.class, new Integer[]{id}, 
            /*0*/"ownershipType", 
            /*1*/"ownership", 
            /*2*/"name", 
            /*3*/"inn", 
            /*4*/"okved", 
            /*5*/"ogrn", 
            /*6*/"ndsPayer", 
            /*7*/"chiefPlace", 
            /*8*/"chifPlaceName", 
            /*9*/"chiefName", 
            /*10*/"businessReason", 
            /*11*/"bookkeeper",
            /*12*/"shotName",
            /*13*/"stamp",
            /*14*/"chifSignature",
            /*15*/"bookkeeperSignature",
            /*16*/"query:select array_to_string(ARRAY(select [CFC(name)] from [CFC] where [CFC(id)] in (select [CFC(companys):object] from [CFC(companys):table] where [CFC(companys):target]=[Company(id)])), ' | ')",
            /*17*/"query:select ARRAY(select [CFC(companys):object] from [CFC(companys):table] where [CFC(companys):target]=[Company(id)])",
            /*18*/"logo");

    if(!data.isEmpty()) {
      cfcLabel.setText((String) data.get(0).get(16));
      cfcs = (Integer[]) data.get(0).get(17);
      
      ownershipId = (Integer) data.get(0).get(0);
      if(ownershipId==null)
        data.get(0).set(1, "Форма собственности...");
      ownershipBorder.setTitle((String) data.get(0).get(1));

      chifPlaceId = (Integer) data.get(0).get(7);
      if(chifPlaceId==null)
        data.get(0).set(8, "Руководитель");
      chifBorder.setTitle((String) data.get(0).get(8));


      nameField.setText((String)  data.get(0).get(2));
      innField.setText((String)   data.get(0).get(3));
      okvedField.setText((String) data.get(0).get(4));
      ogrnField.setText((String)  data.get(0).get(5));
      ndsPayerField.setSelected((boolean) data.get(0).get(6));
      chifNameField.setText((String) data.get(0).get(9));
      businessReasonField.setText((String) data.get(0).get(10));
      bookKeeperNameField.setText((String) data.get(0).get(11));
      shotNameField.setText((String) data.get(0).get(12));
      
      if(data.get(0).get(13) != null) {
        byte[] img = (byte[])data.get(0).get(13);
        try {img = GzipUtil.ungzip(img);}catch(Exception ex) {}
        stampBox.setImage(img);
      }
      
      if(data.get(0).get(14) != null) {
        byte[] img = (byte[])data.get(0).get(14);
        try {img = GzipUtil.ungzip(img);}catch(Exception ex) {}
        chifSignatureBox.setImage(img);
      }
      
      if(data.get(0).get(15) != null) {
        byte[] img = (byte[])data.get(0).get(15);
        try {img = GzipUtil.ungzip(img);}catch(Exception ex) {}
        bookKeeperSignatureBox.setImage(img);
      }
      
      if(data.get(0).get(18) != null) {
        byte[] img = (byte[])data.get(0).get(18);
        try {img = GzipUtil.ungzip(img);}catch(Exception ex) {}
        logoBox.setImage(img);
      }
    }

    Integer mainPartitionId = null;
    String mainPartitionName = null;
    data = ObjectLoader.getData(DBFilter.create(CompanyPartition.class).AND_EQUAL("company", id).AND_EQUAL("tmp", false).AND_EQUAL("type", MappingObject.Type.CURRENT), 
            new String[]{"id","name","kpp","mainPartition"});
    if(!data.isEmpty()) {
      for(List d:data) {
        if((Boolean)d.get(3)) {
          mainPartitionId = (Integer) d.get(0);
          mainPartitionName = d.get(1)+(d.get(2)!=null&&!d.get(2).equals("")?" - "+d.get(2):"");
          break;
        }
      }
      if(mainPartitionId == null) {
        mainPartitionId = (Integer) data.get(0).get(0);
        mainPartitionName = data.get(0).get(1)+(data.get(0).get(2)!=null&&!data.get(0).get(2).equals("")?" - "+data.get(0).get(2):"");
      }
    }
    setPartition(mainPartitionId, mainPartitionName);
  }

  @Override
  public void dispose() {
    accountTableEditor.dispose();
    storePanel.dispose();
    documentPanel.dispose();
    super.dispose();
  }
  
  private String commitPartition() {
    String msg = "";
    if(urAddressField.getText().equals(""))
      msg += "   - Юридический адрес\n";
    if(addressField.getText().equals(""))
      msg += "   - Фактический адрес\n";
    if(postAddressField.getText().equals(""))
      msg += "   - Почтовый адрес\n";
    /*if(telField.getText().equals(""))
      msg += "   - Контактный телефон\n";
    if(contactFioField.getText().equals(""))
      msg += "   - Контактное лицо\n";*/
    
    if(!msg.equals(""))
      msg = "Не заполнены поля:\n"+msg;
    return msg;
  }

  @Override
  public boolean isUpdate() throws RemoteException {
    JSONObject json = ObjectLoader.getJSON(CompanyPartition.class, partitionId);

    Date oldStopDate = null;
    if(json.getString("docStopDate") != null && !json.getString("docStopDate").equals("") && !json.getString("docStopDate").equals("null")) {
      System.out.println(json.getString("docStopDate"));
      oldStopDate = Date.valueOf(json.getString("docStopDate"));
    }
    
    return super.isUpdate() || storeTree.isUpdate() || !json.getString("name").equals(partitionNameField.getText()) || 
              !json.getString("kpp").equals(kppField.getText()) || 
              !json.getString("urAddres").equals(urAddressField.getText()) || 
              !json.getString("addres").equals(addressField.getText()) || 
              !json.getString("postAddres").equals(postAddressField.getText()) || 
              !json.getString("telefon").equals(telField.getText()) || 
              !json.getString("email").equals(emailField.getText()) || 
              !json.getString("contactFio").equals(contactFioField.getText()) || 
              !json.getString("contactInfo").equals(infoField.getText()) ||
              !json.getBoolean("mainnumbering") != documentPanel.getIndividualNumberingCheck().isSelected() ||
              (oldStopDate == null && documentPanel.getStopDate() != null ||
              oldStopDate != null && !oldStopDate.equals(documentPanel.getStopDate()));
  }
  
  private boolean savePartition() throws Exception {
    boolean returnValue = true;
    
    if(partitionId != null) {
      String msg = commitPartition();      
      if(!msg.equals("")) {
        Messanger.alert(msg, "Ошибка заполнения данных", JOptionPane.ERROR_MESSAGE);
        returnValue = false;
      }
      
      if(storeTree != null)
        storeTree.savePartitionStore();
      
      JSONObject json = ObjectLoader.getJSON(CompanyPartition.class, partitionId);
      
      Date oldStopDate = null;
      if(json.getString("docStopDate") != null && !json.getString("docStopDate").equals("") && !json.getString("docStopDate").equals("null")) {
        System.out.println(json.getString("docStopDate"));
        oldStopDate = Date.valueOf(json.getString("docStopDate"));
      }
      
      if(
              returnValue &&
              !json.getString("name").equals(partitionNameField.getText()) || 
              !json.getString("kpp").equals(kppField.getText()) || 
              !json.getString("urAddres").equals(urAddressField.getText()) || 
              !json.getString("addres").equals(addressField.getText()) || 
              !json.getString("postAddres").equals(postAddressField.getText()) || 
              !json.getString("telefon").equals(telField.getText()) || 
              !json.getString("email").equals(emailField.getText()) || 
              !json.getString("contactFio").equals(contactFioField.getText()) || 
              !json.getString("contactInfo").equals(infoField.getText()) ||
              !json.getBoolean("mainnumbering") != documentPanel.getIndividualNumberingCheck().isSelected() ||
              (oldStopDate == null && documentPanel.getStopDate() != null ||
              oldStopDate != null && !oldStopDate.equals(documentPanel.getStopDate()))
              ) {
        
        Map<String, Object> map = new TreeMap<>();
        map.put("name",       partitionNameField.getText());
        map.put("kpp",        kppField.getText());
        map.put("urAddres",   urAddressField.getText());
        map.put("addres",     addressField.getText());
        map.put("postAddres", postAddressField.getText());

        map.put("telefon",           telField.getText());
        map.put("email",             emailField.getText());
        map.put("contactFio",        contactFioField.getText());
        map.put("contactInfo",       infoField.getText());
        
        map.put("mainnumbering",    !documentPanel.getIndividualNumberingCheck().isSelected());
        
        map.put("prefix",           documentPanel.getContractPrefix());
        map.put("prefixTypeFormat", documentPanel.getContractPrefixTypeFormat());
        map.put("prefixSplit",      documentPanel.getContractPrefixSeparator());
        map.put("suffixSplit",      documentPanel.getContractSuffixSeparator());
        map.put("suffixTypeFormat", documentPanel.getContractsuffixTypeFormat());
        map.put("suffix",           documentPanel.getContractSuffix());
        map.put("startNumber",      documentPanel.getContractStartNumber());
        map.put("docStopDate",      documentPanel.getStopDate());
        
        returnValue = ObjectLoader.saveObject(CompanyPartition.class, partitionId, map);
      }
    }
    return returnValue;
  }
  
  private void setPartition(Integer partitionId, String name) {
    try {
      if(savePartition()) {
        this.partitionId = partitionId;
        partitionBorder.setTitle(name);

        accountTableEditor.getClientFilter().clear().AND_EQUAL("companyPartition", partitionId);
        accountTableEditor.initData();

        List<List> data = ObjectLoader.getData(CompanyPartition.class, new Integer[]{partitionId}, 
                /*0*/"name", 
                /*1*/"kpp", 
                /*2*/"urAddres", 
                /*3*/"addres", 
                /*4*/"postAddres", 
                /*5*/"telefon", 
                /*6*/"email", 
                /*7*/"contactFio", 
                /*8*/"contactInfo",

                /*9*/"prefix",
                /*10*/"prefixTypeFormat",
                /*11*/"prefixSplit",
                /*12*/"suffixSplit",
                /*13*/"suffixTypeFormat",
                /*14*/"suffix",
                /*15*/"startNumber",
                /*16*/"docStopDate",
                /*17*/"mainnumbering",
                /*18*/"mainPartition"
                );
        EditorGui.setComponentEnable(partitionTab, !data.isEmpty());
        
        documentPanel.getIndividualNumberingCheck().setSelected(false);
        
        if(!data.isEmpty()) {
          partitionNameField.setText((String) data.get(0).get(0));
          kppField.setText((String) data.get(0).get(1));

          urAddressField.setText((String) data.get(0).get(2));
          addressField.setText((String) data.get(0).get(3));
          postAddressField.setText((String) data.get(0).get(4));

          telField.setText((String) data.get(0).get(5));
          emailField.setText((String) data.get(0).get(6));
          contactFioField.setText((String) data.get(0).get(7));
          infoField.setText((String) data.get(0).get(8));

          documentPanel.setContractPrefix((String) data.get(0).get(9));
          documentPanel.setContractPrefixTypeFormat((String) data.get(0).get(10));
          documentPanel.setContractPrefixSeparator((String) data.get(0).get(11));
          documentPanel.setContractSuffixSeparator((String) data.get(0).get(12));
          documentPanel.setContractsuffixTypeFormat((String) data.get(0).get(13));
          documentPanel.setContractSuffix((String) data.get(0).get(14));
          documentPanel.setContractStartNumber((Integer) data.get(0).get(15));
          documentPanel.setStopDate(data.get(0).get(16) == null ? null : Utility.convertToSqlDate((LocalDate) data.get(0).get(16)));

          documentPanel.setCompanyPartitionId(partitionId);
          documentPanel.initData();
          
          documentPanel.getIndividualNumberingCheck().setSelected(!data.isEmpty() && (!(boolean)data.get(0).get(17) || (boolean)data.get(0).get(18)));
          documentPanel.getIndividualNumberingCheck().setEnabled(!(boolean)data.get(0).get(18));
          EditorGui.setComponentEnable(documentPanel.getNumberingPanel(), documentPanel.getIndividualNumberingCheck().isSelected());
        }

        fireChangeSelection(this, new Integer[]{partitionId});
        storePanel.initData();
        Platform.runLater(() -> {
          storeTree.companyPartitionProperty().setValue(partitionId == null ? null : ObjectLoader.getMap(CompanyPartition.class, partitionId));
        });
      }
      
      if(partitionId != null) {
        EditorGui.setComponentEnable(dataPartitionPanel, true);
        if(partitionTarget != null)
          removeTarget(partitionTarget);
        addTarget(partitionTarget = new DivisionTarget(CompanyPartition.class, partitionId) {
          @Override
          public void messageReceived(String type, Integer[] ids, PropertyMap objectEventProperty) {
            if(type.equals("REMOVE")) {
              nCompanyEditor.this.partitionId = null;
            }else if(type.equals("UPDATE")) {
              if((boolean)ObjectLoader.getData(CompanyPartition.class, new Integer[]{getObjectId()}, "tmp").get(0).get(0)) {
                nCompanyEditor.this.partitionId = null;
              }
            }
            EditorGui.setComponentEnable(dataPartitionPanel, nCompanyEditor.this.partitionId != null);
          }
        });
        
        config.getNodes("machinery.fiscal-printer").stream().forEach(fr -> {
          if(Integer.valueOf(fr.getAttribute("id")).intValue() == partitionId.intValue()) {
            fiscalCheck.setEnabled(false);
            fiscalCheck.setSelected(true);
            fiscal.setSelectedItem(fr.getValue());
            fiscalCheck.setEnabled(true);
          }
        });
      }
      
      fiscal.setEnabled(fiscalCheck.isSelected());
      zReport.setEnabled(fiscalCheck.isSelected());
      xReport.setEnabled(fiscalCheck.isSelected());
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  private void saveFiscal() {
    if(partitionId != null) {
      division.xml.Node frNode = null;
      for(division.xml.Node fr:config.getNodes("machinery.fiscal-printer")) {
        if(Integer.valueOf(fr.getAttribute("id")).intValue() == partitionId.intValue()) {
          frNode = fr;
          break;
        }
      }
      if(frNode == null)
        config.getNode("machinery").addNode(frNode = new division.xml.Node("fiscal-printer"));

      if(!fiscalCheck.isSelected())
        config.getNode("machinery").removeNode(frNode);
      else {
        frNode.setValue((String) fiscal.getSelectedItem());
        frNode.setAttribute("id", String.valueOf(partitionId));
      }

      config.save();
    }
  }
  
  private void createAndSelectOwnershipType() {
    JPanel panel = new JPanel(new GridBagLayout());
    DivisionTextField name1 = new DivisionTextField("Аббревиатура...");
    DivisionTextField descript = new DivisionTextField("Расшифровка...");
    panel.add(name1, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
    panel.add(descript, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
    if (JOptionPane.showInternalConfirmDialog(nCompanyEditor.this.getGUI(), panel, "Новая форма собственности", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)==0) {
      if (!name1.getText().equals("")) {
        try {
          Map<String, Object> map = new TreeMap<>();
          map.put("name", name1.getText());
          map.put("transcript", descript.getText());
          ownershipId = ObjectLoader.createObject(OwnershipType.class, map);
          ownershipBorder.setTitle(name1.getText());
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    }
  }
  
  private JPopupMenu createChifMenu() {
    JPopupMenu menu = new JPopupMenu();
    try {
      List<List> data = ObjectLoader.getData(DBFilter.create(Place.class).AND_NOT_EQUAL("id", chifPlaceId).AND_EQUAL("tmp", false).AND_EQUAL("type", MappingObject.Type.CURRENT), 
              new String[]{"id","name"}, new String[]{"name"});
      data.stream().forEach((d) -> {
        final JMenuItem m = new JMenuItem((String) d.get(1));
        menu.add(m);
        
        m.addActionListener((ActionEvent e) -> {
          chifBorder.setTitle(m.getText());
          chifPlaceId = (Integer) d.get(0);
        });
      });
    }catch (Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    menu.addSeparator();
    JMenuItem list = new JMenuItem("Редактировать список должностей");
    list.addActionListener((ActionEvent e) -> {
      showPlaceTableEditor();
    });
    menu.add(list);
    
    JMenuItem add = new JMenuItem("Создать и выбрать");
    add.addActionListener((ActionEvent e) -> {
      String place = JOptionPane.showInternalInputDialog(nCompanyEditor.this.getGUI(), "Должность", "Новая должность", JOptionPane.QUESTION_MESSAGE);
      if(place != null) {
        try {
          Map<String, Object> map = new TreeMap<>();
          map.put("name", place);
          chifPlaceId = ObjectLoader.createObject(Place.class, map);
          chifBorder.setTitle(place);
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });

    menu.addSeparator();
    menu.add(add);
    return menu;
  }
  
  private JPopupMenu createOwnershipMenu() {
    JComboBox combo = new JComboBox(new String[]{"Создать и выбрать","Редактировать список форм собственности"});
    try {
      List<List> data = ObjectLoader.getData(DBFilter.create(OwnershipType.class).AND_NOT_EQUAL("id", ownershipId).AND_EQUAL("tmp", false).AND_EQUAL("type", MappingObject.Type.CURRENT), 
              new String[]{"id","name","transcript"}, new String[]{"name"});
      data.stream().forEach((d) -> combo.addItem(PropertyMap.create().setValue("name", d.get(1)+(d.get(2)!=null&&!d.get(2).equals("")?" - "+d.get(2):"")).setValue("id", d.get(0))));
    }catch (Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    
    BasicComboPopup popup = new BasicComboPopup(combo);
    popup.setPreferredSize(new Dimension(300, 500));
    
    combo.addActionListener((ActionEvent e) -> {
      if(combo.getSelectedIndex() == 0)
        createAndSelectOwnershipType();
      else if(combo.getSelectedIndex() == 1)
        showOwnershipTypeTableEditor();
      else {
        ownershipBorder.setTitle(((PropertyMap)combo.getSelectedItem()).getString("name"));
        ownershipId = ((PropertyMap)combo.getSelectedItem()).getInteger("id");
      }
      popup.hide();
    });
    
    return popup;
  }
  

  private JPopupMenu createPartitionMenu() {
    try {
      JComboBox<PropertyMap> combo = new JComboBox(ObjectLoader.getList(DBFilter.create(CompanyPartition.class)
              .AND_EQUAL("company", getEditorObject().getId())
              .AND_NOT_EQUAL("id", partitionId)
              .AND_EQUAL("tmp", false)
              .AND_EQUAL("type", MappingObject.Type.CURRENT), 
              "id","name","kpp","mainPartition").toArray(new PropertyMap[0]));
      combo.insertItemAt(PropertyMap.create().setValue("name", "Редактировать список подразделений"), 0);
      combo.insertItemAt(PropertyMap.create().setValue("name", "Создать и выбрать"), 1);
      BasicComboPopup menu = new BasicComboPopup(combo);
      menu.setPreferredSize(new Dimension(300, 500));
      combo.addActionListener(e -> {
        if(combo.getSelectedIndex() >= 0) {
          switch(combo.getSelectedIndex()) {
            case 0:
              showPartitionTableEditor();
              break;
            case 1:
              JPanel panel = new JPanel(new GridBagLayout());
              DivisionTextField name1 = new DivisionTextField("Наименование...");
              DivisionTextField kpp = new DivisionTextField(DivisionTextField.Type.INTEGER, "КПП...");
              panel.add(name1, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
              panel.add(kpp, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
              if (JOptionPane.showInternalConfirmDialog(nCompanyEditor.this.getGUI(), panel, "Новое обособленное подразделение", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)==0) {
                if (!name1.getText().equals("")) {
                  try {
                    Map<String, Object> map = new TreeMap<>();
                    map.put("name", name1.getText());
                    map.put("kpp",  kpp.getText());
                    map.put("company", getEditorObject().getId());
                    setPartition(ObjectLoader.createObject(CompanyPartition.class, map), name1.getText() + (!kpp.getText().equals("")?" - "+kpp.getText():""));
                  }catch(Exception ex) {
                    Messanger.showErrorMessage(ex);
                  }
                }
              }
              break;
            default:
              PropertyMap partition = combo.getItemAt(combo.getSelectedIndex());
              setPartition(partition.getInteger("id"), partition.getString("name"));
              break;
          }
        }
        menu.hide();
      });
      return menu;
    }catch (Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    return null;
  }
  
  private void showPartitionTableEditor() {
    try {
      final TableEditor partitionTableEditor = new TableEditor(
              new String[]{"id","Наименование","КПП"},
              new String[]{"id","name","kpp"},
              CompanyPartition.class,
              null,
              "Обособленные подразделения");
      partitionTableEditor.getClientFilter().clear().AND_EQUAL("company", getEditorObject().getId());

      partitionTableEditor.setAddFunction(true);
      partitionTableEditor.setAutoLoad(true);
      partitionTableEditor.setAutoStore(true);
      partitionTableEditor.getTable().setColumnEditable(1, true);
      partitionTableEditor.getTable().setColumnEditable(2, true);

      partitionTableEditor.setAddAction((ActionEvent e) -> {
        try {
          Map<String,Object> map = new TreeMap<>();
          map.put("name", "Новое обособленное подразделение");
          map.put("company", getEditorObject().getId());
          ObjectLoader.createObject(CompanyPartition.class, map);
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      });

      partitionTableEditor.getTable().getTableModel().addTableModelListener((TableModelEvent e) -> {
        if(e.getType() == TableModelEvent.UPDATE && e.getColumn() > 0) {
          try {
            if(ObjectLoader.executeUpdate("UPDATE [CompanyPartition] SET [!CompanyPartition(name)]=?, [!CompanyPartition(kpp)]=? WHERE id=?",
                    true,
                    new Object[]{
                      (String) partitionTableEditor.getTable().getTableModel().getValueAt(e.getFirstRow(), 1),
                      (String) partitionTableEditor.getTable().getTableModel().getValueAt(e.getFirstRow(), 2),
                      (Integer) partitionTableEditor.getTable().getTableModel().getValueAt(e.getFirstRow(), 0)}) > 0)
              ObjectLoader.sendMessage(CompanyPartition.class, "UPDATE", (Integer) partitionTableEditor.getTable().getTableModel().getValueAt(e.getFirstRow(), 0));
          }catch (Exception ex) {
            Messanger.showErrorMessage(ex);
          }
        }
      });
      
      partitionTableEditor.initData();
      partitionTableEditor.createDialog(this).setVisible(true);
    }catch(RemoteException ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  private void showPlaceTableEditor() {
    final TableEditor placeTableEditor = new TableEditor(
            new String[]{"id","Наименование"},
            new String[]{"id","name"},
            Place.class,
            null,
            "Формы собственности");
    
    placeTableEditor.setAddFunction(true);
    placeTableEditor.setAutoLoad(true);
    placeTableEditor.setAutoStore(true);
    placeTableEditor.getTable().setColumnEditable(1, true);
    
    placeTableEditor.setAddAction(e -> {
      Map<String,Object> map = new TreeMap<>();
      map.put("name", "Новая должность");
      ObjectLoader.createObject(Place.class, map);
    });
    
    placeTableEditor.getTable().getTableModel().addTableModelListener((TableModelEvent e) -> {
      if(e.getType() == TableModelEvent.UPDATE && e.getColumn() > 0) {
        try {
          if(ObjectLoader.executeUpdate("UPDATE [Place] SET [!Place(name)]=? WHERE id=?",
                  (String) placeTableEditor.getTable().getTableModel().getValueAt(e.getFirstRow(), 1),
                  (Integer) placeTableEditor.getTable().getTableModel().getValueAt(e.getFirstRow(), 0), true) > 0)
            ObjectLoader.sendMessage(Place.class, "UPDATE", (Integer) placeTableEditor.getTable().getTableModel().getValueAt(e.getFirstRow(), 0));
        }catch (Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
    
    placeTableEditor.initData();
    placeTableEditor.createDialog(this).setVisible(true);
  }
  
  private void showOwnershipTypeTableEditor() {
    final TableEditor ownershipTypeTableEditor = new TableEditor(
            new String[]{"id","Аббревиатура","Расшифровка"},
            new String[]{"id","name","transcript"},
            OwnershipType.class,
            null,
            "Формы собственности");
    
    ownershipTypeTableEditor.setAddFunction(true);
    ownershipTypeTableEditor.setAutoLoad(true);
    ownershipTypeTableEditor.setAutoStore(true);
    ownershipTypeTableEditor.getTable().setColumnEditable(1, true);
    ownershipTypeTableEditor.getTable().setColumnEditable(2, true);
    
    ownershipTypeTableEditor.setAddAction((ActionEvent e) -> {
      Map<String,Object> map = new TreeMap<>();
      map.put("name", "Новая форма собственности");
      ObjectLoader.createObject(OwnershipType.class, map);
    });
    
    ownershipTypeTableEditor.getTable().getTableModel().addTableModelListener((TableModelEvent e) -> {
      if(e.getType() == TableModelEvent.UPDATE && e.getColumn() > 0) {
        try {
          if(ObjectLoader.executeUpdate("UPDATE [OwnershipType] SET [!OwnershipType(name)]=?, [!OwnershipType(transcript)]=? WHERE id=?",
                  (String) ownershipTypeTableEditor.getTable().getTableModel().getValueAt(e.getFirstRow(), 1),
                  (String) ownershipTypeTableEditor.getTable().getTableModel().getValueAt(e.getFirstRow(), 2),
                  (Integer) ownershipTypeTableEditor.getTable().getTableModel().getValueAt(e.getFirstRow(), 0)) > 0)
            ObjectLoader.sendMessage(OwnershipType.class, "UPDATE", (Integer) ownershipTypeTableEditor.getTable().getTableModel().getValueAt(e.getFirstRow(), 0));
        }catch (Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
    
    ownershipTypeTableEditor.initData();
    ownershipTypeTableEditor.createDialog(this).setVisible(true);
  }
}