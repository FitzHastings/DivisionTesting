package division;

import bum.editors.*;
import bum.editors.util.FileLibrary;
import bum.editors.util.ObjectLoader;
import bum.interfaces.*;
import division.Desktop.DesktopLabel;
import division.demons.RequestDemon;
import division.editors.contract.ContractEditor;
import division.editors.objects.DocumentEditor;
import division.editors.objects.PeopleEditor;
import division.editors.objects.company.nCompanyEditor;
import division.editors.products.CatalogEditor;
import division.editors.tables.*;
import division.exportimport.ExportImportEditor;
import division.fx.client.ClientFilter;
import division.fx.client.MenuLoader;
import division.fx.client.plugins.Contracts;
import division.fx.client.plugins.FXPlugin;
import division.fx.client.plugins.product.Products;
import division.fx.dialog.FXD;
import division.messanger.UserAuth;
import division.messanger.UserList;
import division.swing.DivisionSplitPane;
import division.swing.guimessanger.GuiMessageListener;
import division.swing.guimessanger.MessageTextBox;
import division.swing.guimessanger.Messanger;
import division.util.ClassPathLoader;
import division.util.FXArrays;
import division.util.FileLoader;
import division.util.SYS;
import division.util.ScriptUtil;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.TableModelEvent;
import mapping.MappingObject;
import net.infonode.docking.RootWindow;
import net.infonode.docking.SplitWindow;
import net.infonode.docking.View;
import net.infonode.docking.properties.RootWindowProperties;
import net.infonode.docking.properties.ViewProperties;
import net.infonode.docking.theme.ClassicDockingTheme;
import net.infonode.docking.util.DockingUtil;
import net.infonode.docking.util.ViewMap;
import net.infonode.gui.laf.InfoNodeLookAndFeel;
import net.infonode.util.Direction;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import util.filter.local.DBFilter;


public class ClientMain extends JFrame {
  private static ClientMain instance;
  
  private static java.util.List<ClientMainListener> clientMainListeners = new ArrayList<>();
  
  private final DivisionSplitPane   verticalSplit   = new DivisionSplitPane();
  
  private final JToolBar            tool            = new JToolBar();
  private final DivisionDesktop     desktop         = new DivisionDesktop(tool, "desktop");
  
  private final JMenuBar  menu            = new JMenuBar();
  /*private final JMenu     file            = new JMenu("Файл");
  private final JMenuItem naryadDialog    = new JMenuItem("Приём заказ-нарядов");
  private final JMenuItem machineryDialog = new JMenuItem("Настройка оборудования");
  private final JMenuItem connect         = new JMenuItem("Настройки соединения");
  private final JMenuItem moneyConfItem   = new JMenuItem("Валюта");*/

  private final JMenu     referenceBooks  = new JMenu("Справочники");
  private final JMenuItem groups          = new JMenuItem("Объекты");
  private final JMenuItem peoples         = new JMenuItem("Сотрудники");
  private final JMenuItem banks           = new JMenuItem("Банки");
  private final JMenuItem systemDocuments = new JMenuItem("Системные документы");
  private final JMenuItem usersDocuments  = new JMenuItem("Пользовательские документы");
  private final JMenuItem jsModuls        = new JMenuItem("Модули JS");
  private final JMenuItem jScripts        = new JMenuItem("Сценарии JS");
  private final JMenuItem tree            = new JMenuItem("Настройка экспорта/импорта");
  private final JMenuItem removeFromCfc   = new JMenuItem("Удалить из группы ЦФУ");
  
  private final UserList userList = new UserList();
  
  private CFCTableEditor           cfcTableEditor;
  private TypeTableEditor          companyTableEditor;
  private TypeTableEditor          contractTableEditor;
  private TypeTableEditor          createdDocumentTableEditor;
  private MoneyPosition            moneyPositionTable;
  private CleverPaymentTableEditor cleverPaymentTableEditor;
  private CatalogEditor      catalogEditor;
  
  private RootWindow rootWindow;
  private final ArrayList storeComponents = new ArrayList();
  
  public static Integer[] currentCfc = new Integer[0];
  public static Integer[] currentCompanys = new Integer[0];

  private ClientMain() {
    
    //AtolinServer5.startAtolService();
    //AtolinServer5.startCommandLine("Atoline server: ",(String t) -> AtolinServer5.startCommand(t));
    
    Platform.setImplicitExit(false);
    JFXPanel p = new JFXPanel();
    
    RequestDemon.start();
    
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        String title = getTitle();
        try {
          Worker worker = (Worker) ObjectLoader.getObject(Worker.class, ObjectLoader.getClient().getWorkerId());
          People people = worker.getPeople();
          title = "Дивизион - "+worker.getCfc().getName()+":"+people.getSurName()+" "+people.getName()+" "+people.getLastName();
        }catch(Exception ex) {
          Logger.getRootLogger().error(ex);
        }
        if(JOptionPane.showConfirmDialog(
                ClientMain.this, 
                "Завершить текущий сеанс?", 
                "Завершение сеанса: "+title, 
                JOptionPane.YES_NO_CANCEL_OPTION) == 0)
          dispose();
      }
    });
  }
  
  private double calc(int count) {
    
    double res = 1.0;
    for(int i=1;i<=count;i++) {
      res += (double)count/0.2 + Math.sinh(i);
    }
    return res;
  }

  public static ClientMain getInstance() {
    return instance;
  }
    
  public static void addClientMainListener(ClientMainListener listener) {
    if(!clientMainListeners.contains(listener))
      clientMainListeners.add(listener);
  }
  
  public static void removeClientMainListener(ClientMainListener listener) {
    clientMainListeners.remove(listener);
  }
  
  public static void fireChangedCFC(Integer[] ids) {
    clientMainListeners.stream().forEach(l -> l.changedCFC(ids));
    ClientFilter.CFCProperty().setValue(ids);
  }
  
  public static void fireChangedCompany(Integer[] ids) {
    clientMainListeners.stream().forEach(l -> l.changedCompany(ids));
    ClientFilter.companyProperty().setValue(ids);
  }
  
  public void initEditors() {
    try {
      userList.init();
      
      catalogEditor = new CatalogEditor();
      cfcTableEditor = new CFCTableEditor();
      cfcTableEditor.getTree().setHierarchy(true);
      cfcTableEditor.setAutoLoadAndStore(true);
      cfcTableEditor.setAutoLoadAndStore(true);
      
      // Предприятия
      companyTableEditor = new TypeTableEditor(
              new String[]{"id","Форма","Наименование","Инн"},
              new String[]{"id","ownership","name","inn"},
              Company.class,
              nCompanyEditor.class,
              "Предприятия", MappingObject.Type.CURRENT, true, false, false) {
                @Override
                public void changedCFC(Integer[] ids) {
                  getCurrentTableEditor().getClientFilter().clear();
                  getArchiveTableEditor().getClientFilter().clear();
                  if(ObjectLoader.getClient().isPartner()) {
                    Integer[] idss = ObjectLoader.getChilds(CFC.class, ObjectLoader.getClient().getCfcId());
                    Integer[] cids = FXArrays.retainAll(idss, ids);
                    getCurrentTableEditor().getClientFilter().AND_IN("cfcs", cids);
                    getArchiveTableEditor().getClientFilter().AND_IN("cfcs", cids);
                  }else if(ids.length > 0) {
                    //moneyPositionTable.setCfc(currentCfc[0]);
                    getCurrentTableEditor().getClientFilter().AND_IN("cfcs", ids);
                    getArchiveTableEditor().getClientFilter().AND_IN("cfcs", ids);
                  }
                  initData();
                }
              };
      
      addClientMainListener(companyTableEditor);
      
      if(ObjectLoader.getClient().isPartner()) {
        Integer[] cids = ObjectLoader.getChilds(CFC.class, ObjectLoader.getClient().getCfcId());
        companyTableEditor.getCurrentTableEditor().getClientFilter().AND_IN("cfcs", cids);
        companyTableEditor.getArchiveTableEditor().getClientFilter().AND_IN("cfcs", cids);
      }
      
      companyTableEditor.setAutoLoadAndStore(true);
      ((TableEditor)companyTableEditor.getCurrentTableEditor()).getTable().setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
      ((TableEditor)companyTableEditor.getArchiveTableEditor()).getTable().setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
      companyTableEditor.getCurrentTableEditor().getScroll().setRowHeader(false);
      companyTableEditor.getArchiveTableEditor().getScroll().setRowHeader(false);
      companyTableEditor.getCurrentTableEditor().setExportFunction(false);
      companyTableEditor.getCurrentTableEditor().setImportFunction(false);
      
      companyTableEditor.getCurrentTableEditor().getPopMenu().addSeparator();
      companyTableEditor.getCurrentTableEditor().getPopMenu().add(removeFromCfc);
      
      ((TableEditor)companyTableEditor.getCurrentTableEditor()).getTable().getTableFilters().addTextFilter(1);
      ((TableEditor)companyTableEditor.getCurrentTableEditor()).getTable().getTableFilters().addTextFilter(2);
      ((TableEditor)companyTableEditor.getCurrentTableEditor()).getTable().getTableFilters().addTextFilter(3);
      
      ((TableEditor)companyTableEditor.getArchiveTableEditor()).getTable().getTableFilters().addTextFilter(1);
      ((TableEditor)companyTableEditor.getArchiveTableEditor()).getTable().getTableFilters().addTextFilter(2);
      ((TableEditor)companyTableEditor.getArchiveTableEditor()).getTable().getTableFilters().addTextFilter(3);
      
      cfcTableEditor.setAdministration(!ObjectLoader.getClient().isPartner());
      //companyTableEditor.getCurrentTableEditor().setAdministration(!ObjectLoader.getClient().isPartner());
      
      // Договора
      contractTableEditor = new ContractTableEditor();
      /*contractTableEditor = new TypeTableEditor(
                new String[] {"id","Номер","Наименование","Продавец",
                "Покупатель","Начало","Окончание","Дата создания"},
                new String[] {"id","number","templatename","seller",
                "customer","startDate","endDate","date"},
                Contract.class,
                ContractEditor.class,
                "Договора",
                MappingObject.Type.CURRENT, true, false, false) {
                  private DBFilter cfcCurrentFilter = getCurrentTableEditor().getClientFilter().AND_FILTER();
                  private DBFilter cfcArchiveFilter = getArchiveTableEditor().getClientFilter().AND_FILTER();
                  private DBFilter companyCurrentFilter = getCurrentTableEditor().getClientFilter().AND_FILTER();
                  private DBFilter companyArchiveFilter = getArchiveTableEditor().getClientFilter().AND_FILTER();
                  @Override
                  public void changedCFC(Integer[] ids) {
                    cfcCurrentFilter.clear();
                    cfcArchiveFilter.clear();
                    if(ids.length > 0) {
                      cfcCurrentFilter.AND_IN("customerCfc", ids);
                      cfcCurrentFilter.OR_IN("sellerCfc", ids);
                      cfcArchiveFilter.AND_IN("customerCfc", ids);
                      cfcArchiveFilter.OR_IN("sellerCfc", ids);
                    }
                    
                    if(ObjectLoader.getClient().isPartner()) {
                      cfcCurrentFilter.AND_IN("customerCfc", new Integer[]{ObjectLoader.getClient().getCfcId()});
                      cfcCurrentFilter.OR_IN("sellerCfc", new Integer[]{ObjectLoader.getClient().getCfcId()});
                      cfcArchiveFilter.AND_IN("customerCfc", new Integer[]{ObjectLoader.getClient().getCfcId()});
                      cfcArchiveFilter.OR_IN("sellerCfc", new Integer[]{ObjectLoader.getClient().getCfcId()});
                    }
                  }

                  @Override
                  public void changedCompany(Integer[] ids) {
                    companyCurrentFilter.clear();
                    companyArchiveFilter.clear();
                    if(ids.length > 0) {
                      companyCurrentFilter.AND_IN("customerCompany", ids);
                      companyCurrentFilter.OR_IN("sellerCompany", ids);
                      companyArchiveFilter.AND_IN("customerCompany", ids);
                      companyArchiveFilter.OR_IN("sellerCompany", ids);
                    }
                    initData();
                  }
                };
      
      if(ObjectLoader.getClient().isPartner()) {
        cfcCurrentFilter.AND_IN("customerCfc", new Integer[]{ObjectLoader.getClient().getCfcId()});
        cfcCurrentFilter.OR_IN("sellerCfc", new Integer[]{ObjectLoader.getClient().getCfcId()});
        cfcArchiveFilter.AND_IN("customerCfc", new Integer[]{ObjectLoader.getClient().getCfcId()});
        cfcArchiveFilter.OR_IN("sellerCfc", new Integer[]{ObjectLoader.getClient().getCfcId()});
      }*/
      
      contractTableEditor.getArchiveTableEditor().setConfFunction(true);
      contractTableEditor.getCurrentTableEditor().setSortFields(new String[]{"startDate DESC"});
      contractTableEditor.getArchiveTableEditor().setSortFields(new String[]{"startDate DESC"});
      addClientMainListener(contractTableEditor);
      
      ((TableEditor)contractTableEditor.getCurrentTableEditor()).getTable().getTableFilters().addTextFilter(1);
      ((TableEditor)contractTableEditor.getCurrentTableEditor()).getTable().getTableFilters().addListFilter(2);
      ((TableEditor)contractTableEditor.getCurrentTableEditor()).getTable().getTableFilters().addListFilter(3);
      ((TableEditor)contractTableEditor.getCurrentTableEditor()).getTable().getTableFilters().addTextFilter(4);
      ((TableEditor)contractTableEditor.getCurrentTableEditor()).getTable().getTableFilters().addDateFilter(5);
      ((TableEditor)contractTableEditor.getCurrentTableEditor()).getTable().getTableFilters().addDateFilter(6);
      ((TableEditor)contractTableEditor.getCurrentTableEditor()).getTable().getTableFilters().addDateFilter(7);

      ((TableEditor)contractTableEditor.getArchiveTableEditor()).getTable().getTableFilters().addTextFilter(1);
      ((TableEditor)contractTableEditor.getArchiveTableEditor()).getTable().getTableFilters().addListFilter(2);
      ((TableEditor)contractTableEditor.getArchiveTableEditor()).getTable().getTableFilters().addListFilter(3);
      ((TableEditor)contractTableEditor.getArchiveTableEditor()).getTable().getTableFilters().addTextFilter(4);
      ((TableEditor)contractTableEditor.getArchiveTableEditor()).getTable().getTableFilters().addDateFilter(5);
      ((TableEditor)contractTableEditor.getArchiveTableEditor()).getTable().getTableFilters().addDateFilter(6);
      ((TableEditor)contractTableEditor.getArchiveTableEditor()).getTable().getTableFilters().addDateFilter(7);
      
      // Документы
      if(!ObjectLoader.getClient().isPartner()) {
        createdDocumentTableEditor = new TypeTableEditor(CreatedDocumentTableEditor.class);
        createdDocumentTableEditor.setTitle("Документы");
        createdDocumentTableEditor.setAutoLoad(true);
        createdDocumentTableEditor.setAutoStore(true);
      }
      
      cleverPaymentTableEditor = new CleverPaymentTableEditor();
      moneyPositionTable = new MoneyPosition();
      
      desktop.add(new DesktopLabel("contracts", FileLoader.getIcon("images/contracts.png"), "Договоры", new DivisionLabelAction(contractTableEditor)));
      desktop.add(new DesktopLabel("dealss", FileLoader.getIcon("images/deals.png"), "Сделки", new DivisionLabelAction(new CleverDealTableEditor())));
      
      if(!ObjectLoader.getClient().isPartner()) {
        desktop.add(new DesktopLabel("newPayments", FileLoader.getIcon("images/deals.png"), "Платежи новые", new DivisionLabelAction(cleverPaymentTableEditor)));
        desktop.add(new DesktopLabel("documents", FileLoader.getIcon("images/documents.png"), "Документы", new DivisionLabelAction(createdDocumentTableEditor)));
      }
      //desktop.add(new DesktopLabel("payments", FileLoader.getIcon("images/payments.png"), "Платежи", new DivisionLabelAction(paymentTableEditor)));
      desktop.add(new DesktopLabel("products", FileLoader.getIcon("images/price-list.png"), "Каталог", new DivisionLabelAction(catalogEditor)));
      
      desktop.add(new DesktopLabel("products-test", FileLoader.getIcon("images/price-list.png"), "Каталог тест", new DivisionLabelAction(new Products())));
      desktop.add(new DesktopLabel("contracts-test", FileLoader.getIcon("images/contracts.png"), "Договоры тест", new DivisionLabelAction(new Contracts())));
      
      //desktop.add(new DesktopLabel("tools", FileLoader.getIcon("images/tools.png"), "Настройки", new DivisionLabelAction(catalogEditor)));
      desktop.add(new DesktopLabel("monePosition", FileLoader.getIcon("images/tools.png"), "Денежная позиция", new DivisionLabelAction(moneyPositionTable)));
    }catch(InstantiationException | IllegalAccessException ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  
  
  
  class ContractTableEditor extends TypeTableEditor {
    private DBFilter cfcCurrentFilter = getCurrentTableEditor().getClientFilter().AND_FILTER();
    private DBFilter cfcArchiveFilter = getArchiveTableEditor().getClientFilter().AND_FILTER();
    private DBFilter companyCurrentFilter = getCurrentTableEditor().getClientFilter().AND_FILTER();
    private DBFilter companyArchiveFilter = getArchiveTableEditor().getClientFilter().AND_FILTER();
    
    public ContractTableEditor() throws InstantiationException, IllegalAccessException {
      super(new String[] {"id","Номер","Наименование","Продавец",
                "Покупатель","Начало","Окончание","Дата создания"},
                new String[] {"id","number","templatename","seller",
                "customer","startDate","endDate","date"},
                Contract.class,
                ContractEditor.class,
                "Договора",
                MappingObject.Type.CURRENT, true, false, false);
      
      if(ObjectLoader.getClient().isPartner()) {
        Integer[] cids = ObjectLoader.getChilds(CFC.class, ObjectLoader.getClient().getCfcId());
        cfcCurrentFilter.AND_IN("customerCfc", cids);
        cfcCurrentFilter.OR_IN("sellerCfc", cids);
        cfcArchiveFilter.AND_IN("customerCfc", cids);
        cfcArchiveFilter.OR_IN("sellerCfc", cids);
      }
    }
    
    @Override
    public void changedCFC(Integer[] ids) {
      cfcCurrentFilter.clear();
      cfcArchiveFilter.clear();

      if(ObjectLoader.getClient().isPartner()) {
        Integer[] cids = FXArrays.retainAll(ObjectLoader.getChilds(CFC.class, ObjectLoader.getClient().getCfcId()), ids);
        cfcCurrentFilter.AND_IN("customerCfc", cids);
        cfcCurrentFilter.OR_IN("sellerCfc", cids);
        cfcArchiveFilter.AND_IN("customerCfc", cids);
        cfcArchiveFilter.OR_IN("sellerCfc", cids);
      }else if(ids.length > 0) {
        cfcCurrentFilter.AND_IN("customerCfc", ids);
        cfcCurrentFilter.OR_IN("sellerCfc", ids);
        cfcArchiveFilter.AND_IN("customerCfc", ids);
        cfcArchiveFilter.OR_IN("sellerCfc", ids);
      }
    }

    @Override
    public void changedCompany(Integer[] ids) {
      companyCurrentFilter.clear();
      companyArchiveFilter.clear();
      if(ids.length > 0) {
        companyCurrentFilter.AND_IN("customerCompany", ids);
        companyCurrentFilter.OR_IN("sellerCompany", ids);
        companyArchiveFilter.AND_IN("customerCompany", ids);
        companyArchiveFilter.OR_IN("sellerCompany", ids);
      }
      initData();
    }
  }

  private void startInit() {
    if(ObjectLoader.getClient().isPartner())
      cfcTableEditor.getClientFilter().clear().AND_IN("id", ObjectLoader.getChilds(CFC.class, ObjectLoader.getClient().getCfcId()));
    cfcTableEditor.initData();
    companyTableEditor.initData();
  }

  public boolean isEditor(Class clazz) {
    boolean editor = false;
    Class superClass = clazz.getSuperclass();
    if(superClass != null) {
      while(!editor) {
        if(superClass == EditorGui.class) {
          editor = true;
          break;
        }else if(superClass == Object.class)
          break;
        superClass = superClass.getSuperclass();
      }
    }
    return editor;
  }
  
  public static void main(final String[] args) throws Exception {
    System.out.println("HDD: "+SYS.getHDDSerial());
    SplashScreen splash = SplashScreen.getSplashScreen();
    if(splash != null)
      splash.createGraphics();

    SwingUtilities.invokeLater(() -> {
      try {
        UIManager.setLookAndFeel(new InfoNodeLookAndFeel());
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
      instance = new ClientMain();
      EditorController.setFrame(instance);
      instance.initGC();
      instance.initRxTxDriver();
      instance.start();
    });
  }
  
  private static final Icon VIEW_ICON = new Icon() {
    @Override
    public int getIconHeight() {
      return 8;
    }
    @Override
    public int getIconWidth() {
      return 8;
    }
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      Color oldColor = g.getColor();
      g.setColor(new Color(70, 70, 70));
      g.fillRect(x, y, getIconWidth(), getIconHeight());
      g.setColor(new Color(100, 230, 100));
      g.fillRect(x + 1, y + 1, getIconWidth() - 2, getIconHeight() - 2);
      g.setColor(oldColor);
    }
  };
  
  private void createRootWindow() {
    MessageTextBox messageTextBox = new MessageTextBox();
    ViewMap viewMap   = new ViewMap();
    View filterView   = new View("Фильтр", VIEW_ICON, verticalSplit);
    View desktopView  = new View("DesktopPane", VIEW_ICON, desktop);
    View usersView    = new View("Пользователи", VIEW_ICON, userList);
    View messageView  = new View("Системные сообщения", VIEW_ICON, messageTextBox);
    
    viewMap.addView(1, filterView);
    viewMap.addView(2, desktopView);
    viewMap.addView(3, usersView);
    viewMap.addView(4, messageView);
    
    Messanger.addGuiMessageListener((GuiMessageListener.Type messageType, String title1, String message, Throwable ex) -> {
      messageView.getWindowParent().makeVisible();
      messageView.getWindowParent().maximize();
      messageView.getWindowParent().dock();
    });
    
    ViewProperties desktopProperties = new ViewProperties();
    desktopProperties.setAlwaysShowTitle(false);
    desktopView.getViewProperties().addSuperObject(desktopProperties);
    
    rootWindow = DockingUtil.createRootWindow(viewMap, true);
    //SplitWindow message_comment = new SplitWindow(false, messageView, new View("test", VIEW_ICON, new JPanel()));
    SplitWindow desktop_message_comment  = new SplitWindow(false,  0.8f, desktopView, messageView);
    SplitWindow filter_desktop  = new SplitWindow(true,  0.2f, filterView, desktop_message_comment);
    SplitWindow users_desktop   = new SplitWindow(true,  0.8f, filter_desktop, usersView);
    rootWindow.setWindow(users_desktop);
    
    RootWindowProperties properties = new RootWindowProperties();
    properties.addSuperObject(new ClassicDockingTheme().getRootWindowProperties());
    properties.getDockingWindowProperties().setCloseEnabled(false);
    properties.getDockingWindowProperties().setDragEnabled(false);
    properties.getDockingWindowProperties().setMaximizeEnabled(false);
    properties.getDockingWindowProperties().setDockEnabled(false);
    properties.getDockingWindowProperties().setUndockEnabled(false);
    rootWindow.getRootWindowProperties().addSuperObject(properties);
    
    try {
      ObjectInputStream in = new ObjectInputStream(new FileInputStream("conf"+File.separator+"RootWindowDocking.data"));
      rootWindow.read(in);
      in.close();
      in = null;
    }catch(Exception ex) {
      rootWindow.getWindowBar(Direction.LEFT).addTab(filterView);
      rootWindow.getWindowBar(Direction.LEFT).setEnabled(true);

      rootWindow.getWindowBar(Direction.RIGHT).addTab(usersView);
      rootWindow.getWindowBar(Direction.RIGHT).setEnabled(true);

      rootWindow.getWindowBar(Direction.DOWN).addTab(messageView);
      rootWindow.getWindowBar(Direction.DOWN).setEnabled(true);
    }
  }
  
  private void initComponents() {
    createRootWindow();
    
    JPanel panel = new JPanel(new BorderLayout());
    tool.setFloatable(false);
    tool.addSeparator();
    desktop.setBackground(Color.LIGHT_GRAY);
    
    panel.add(rootWindow, BorderLayout.CENTER);
    panel.add(tool,       BorderLayout.SOUTH);

    setContentPane(panel);
    
    verticalSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
    cfcTableEditor.setVisibleOkButton(false);
    verticalSplit.add(cfcTableEditor.getGUI(), JSplitPane.TOP);
    verticalSplit.add(companyTableEditor.getGUI(), JSplitPane.BOTTOM);
    verticalSplit.setOneTouchExpandable(true);

    catalogEditor.setVisibleOkButton(false);
    cleverPaymentTableEditor.setVisibleOkButton(false);
    moneyPositionTable.setVisibleOkButton(false);
    
    setJMenuBar(menu);
    /*if(!ObjectLoader.getClient().isPartner()) {
      menu.add(file);
      file.add(connect);
      file.add(naryadDialog);
      file.add(machineryDialog);
      file.add(moneyConfItem);
    }*/
    
    menu.add(referenceBooks);
    referenceBooks.add(groups);
    if(!ObjectLoader.getClient().isPartner())
      referenceBooks.add(peoples);
    referenceBooks.add(banks);
    if(!ObjectLoader.getClient().isPartner())
      referenceBooks.add(systemDocuments);
    if(!ObjectLoader.getClient().isPartner()) {
      referenceBooks.add(usersDocuments);
      referenceBooks.add(jsModuls);
      referenceBooks.add(jScripts);
      referenceBooks.add(tree);
    }

    verticalSplit.setName("verticalSplit");
    setName("frame");
    storeComponents.add(this);
    storeComponents.add(verticalSplit);
    
    MenuLoader.load(menu, "conf"+File.separator+"conf.json");
  }

  private void iniEvents() {
    /*paymentTable.addActionListener((ActionEvent e) -> {
      PaymentTableController paymentTableController = new PaymentTableController();
      paymentTableController.show();
    });*/
    
    getRootPane().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
        if(System.getProperty("HELP") != null)
          EditorGui.setCursor(ClientMain.this.getRootPane(), new Cursor(Cursor.DEFAULT_CURSOR));
      }
    });

    removeFromCfc.addActionListener((ActionEvent e) -> {
      if(JOptionPane.showConfirmDialog(
              companyTableEditor.getGUI(),
              "Удалить предприятие из группы?",
              "Подтверждение",
              JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == 0) {
        try {
          MappingObject[] cfcs = cfcTableEditor.getSelectedObjects();
          Integer[] ids = companyTableEditor.getSelectedId();
          for(MappingObject object:companyTableEditor.getSelectedObjects()) {
            ((Company)object).removeCfcs(Arrays.asList(Arrays.copyOfRange(cfcs, 0, cfcs.length, CFC[].class)));
          }
          ObjectLoader.sendMessage(Company.class, "UPDATE", ids);
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });

    /*connect.addActionListener((ActionEvent e) -> {
      Connections connections = new Connections(ClientMain.this);
      connections.showConnectionConfiguration();
    });
    
    naryadDialog.addActionListener(e -> {
      NaryadDialog dialog = new NaryadDialog();
      dialog.start();
    });

    machineryDialog.addActionListener((ActionEvent e) -> {
      MachineryDialog dialog = new MachineryDialog(ClientMain.this);
      dialog.centerLocation();
      dialog.setVisible(true);
    });
    
    moneyConfItem.addActionListener((ActionEvent e) -> {
      try {
        MoneyConfigDialog moneyConfigDialog = new MoneyConfigDialog();
        moneyConfigDialog.setAutoLoad(true);
        moneyConfigDialog.setAutoStore(true);
        moneyConfigDialog.initData();
        moneyConfigDialog.createDialog().setVisible(true);
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    });*/
    
    try {
      GroupTableEditor groupTable = new GroupTableEditor();
      groupTable.setName("PeopleTableType");
      groupTable.setAutoLoad(true);
      groupTable.setAutoStore(true);
      groups.addActionListener(new DivisionLabelAction(groupTable));
      
      TypeTableEditor peopleTable = new TypeTableEditor(
              new String[]{"id","Фамилия","Имя","Отчество"},
              new String[]{"id","surName","name","lastName"},
              People.class,PeopleEditor.class,"Сотрудники");
      peopleTable.setName("PeopleTableType");
      peopleTable.setAutoLoad(true);
      peopleTable.setAutoStore(true);
      peopleTable.setSortFields(new String[]{"name"});
      peoples.addActionListener(new DivisionLabelAction(peopleTable));
      
      BankTableEditor bankTableEditor = new BankTableEditor();
      bankTableEditor.setSortFields(new String[]{"name"});
      bankTableEditor.setAutoLoad(true);
      bankTableEditor.setAutoStore(true);
      banks.addActionListener(new DivisionLabelAction(bankTableEditor));
      
      TableEditor modulTableEditor = new TableEditor(
              new String[]{"id","Наименование"},
              new String[]{"id","name"},
              JSModul.class,
              JSModulEditor.class,
              "Модули JS",
              MappingObject.Type.CURRENT);
      modulTableEditor.setName("ModulTable");
      modulTableEditor.setAutoLoad(true);
      modulTableEditor.setAutoStore(true);
      modulTableEditor.setSortFields(new String[]{"name"});
      jsModuls.addActionListener(new DivisionLabelAction(modulTableEditor));
      
      final TableEditor scriptTableEditor = new TableEditor(
              new String[]{"id","Наименование","Автозагрузка"},
              new String[]{"id","name","autoRun"},
              JScript.class,
              JSModulEditor.class,
              "Сценарии JS",
              MappingObject.Type.CURRENT);
      scriptTableEditor.getTable().setColumnEditable(2, true);
      scriptTableEditor.getTable().getTableModel().addTableModelListener((TableModelEvent e) -> {
        if(scriptTableEditor.isActive() && e.getType() == TableModelEvent.UPDATE && e.getLastRow() != -1 && e.getColumn() != -1) {
          int column = scriptTableEditor.getTable().convertColumnIndexToModel(e.getColumn());
          int row    = scriptTableEditor.getTable().convertRowIndexToModel(e.getLastRow());
          Object value = scriptTableEditor.getTable().getModel().getValueAt(row, column);
          if(row >= 0 && column == 2) {
            try {
              Boolean autoRun = (Boolean)value;
              JScript script = (JScript)scriptTableEditor.getSelectedObjects()[0];
              script.setAutoRun(autoRun);
              ObjectLoader.createSession(true).saveObject(script);
            }catch(Exception ex) {
              Messanger.showErrorMessage(ex);
            }
          }
        }
      });
      scriptTableEditor.setName("ScriptTable");
      scriptTableEditor.setAutoLoad(true);
      scriptTableEditor.setAutoStore(true);
      scriptTableEditor.setSortFields(new String[]{"name"});
      jScripts.addActionListener(new DivisionLabelAction(scriptTableEditor));
      
      TableEditor usersDocumentTableEditor = new TableEditor(
              new String[]{"id","Документ"},
              new String[]{"id","name"},
              Document.class,
              DocumentEditor.class,
              "Шаблоны документов",
              MappingObject.Type.CURRENT);
      usersDocumentTableEditor.setName("DocumentTable");
      usersDocumentTableEditor.setAutoLoadAndStore(true);
      usersDocumentTableEditor.getClientFilter().AND_EQUAL("system", false);
      usersDocuments.addActionListener(new DivisionLabelAction(usersDocumentTableEditor));
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    
    cfcTableEditor.addEditorListener(new EditorListener() {
      @Override
      public void changeSelection(EditorGui editor, Integer[] ids) {
        changeCfc(ids);
      }
    });
    
    companyTableEditor.addEditorListener(new EditorListener() {
      @Override
      public void changeSelection(EditorGui editor, Integer[] ids) {
        changeCompany(ids);
      }
    });
    
    systemDocuments.addActionListener(new DivisionLabelAction(new SystemDocumentTable()));
    
    tree.addActionListener((ActionEvent e) -> {
      ExportImportEditor exportImportEditor = new ExportImportEditor();
      exportImportEditor.setAutoLoad(true);
      exportImportEditor.setAutoStore(true);
      exportImportEditor.initData();
      exportImportEditor.createDialog(ClientMain.this).setVisible(true);
    });
  }

  private void changeCfc(Integer[] ids) {
    try {
      currentCfc = ids;
      fireChangedCFC(currentCfc);
      changeCompany(new Integer[0]);
    }catch(Exception ex){Messanger.showErrorMessage(ex);}
  }
  
  private void changeCompany(Integer[] ids) {
    try {
      currentCompanys = ids;
      fireChangedCompany(currentCompanys);
      
      //moneyPositionTable.setCompanys(currentCompanys);
      //moneyPositionTable.initData();
      
      cleverPaymentTableEditor.setCompanys(currentCompanys);
      cleverPaymentTableEditor.initData();
    }catch(Exception ex){Messanger.showErrorMessage(ex);}
  }

  public static Integer[] getCurrentCfc() {
    return currentCfc;
  }

  public static Integer[] getCurrentCompanys() {
    return currentCompanys;
  }

  private void load() {
    FileLibrary.load(storeComponents, "mainframe.xml");
    if(getWidth() == 0 || getHeight() == 0)
      setSize(new Dimension(800, 600));
    cfcTableEditor.load();
    companyTableEditor.load();
    desktop.load();
  }
  
  private void store() {
    try {
      if(rootWindow != null) {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("conf"+File.separator+"RootWindowDocking.data"));
        rootWindow.write(out);
        out.reset();
        out.flush();
        out.close();
        out = null;
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    
    FileLibrary.store(storeComponents, "mainframe.xml");
    desktop.store();
  }
  
  @Override
  public void dispose() {
    store();
    EditorController.dispose();
    ObjectLoader.dispose();
    super.dispose();
  }
  
  public void disposeWithoutStore() {
    EditorController.dispose();
    ObjectLoader.dispose();
    super.dispose();
  }
  
  private boolean userAuth() throws Exception {
    UserAuth userAuth = new UserAuth();
    userAuth.setActive(true);
    return userAuth.auth();
  }

  private void start() {
    try {
      ObjectLoader.clear();
      ObjectLoader.connect();
     
      ScriptUtil.loadGroovyScripts("scripts");
      //JFXPanel fxPanel = new JFXPanel();

      if(!userAuth())
        disposeWithoutStore();
      
      //System.out.println(ObjectLoader.getClient().isPartner());
      
      Worker worker = (Worker) ObjectLoader.getObject(Worker.class, ObjectLoader.getClient().getWorkerId());
      People people = worker.getPeople();
      
      initEditors();
      initComponents();
      iniEvents();
      load();
      startInit();
      setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
      setTitle("Дивизион - "+worker.getCfc().getName()+":"+people.getSurName()+" "+people.getName()+" "+people.getLastName());
      setVisible(true);
    }catch(Exception ex) {
      ex.printStackTrace();
      //Messanger.showErrorMessage(ex);
    }
  }
  
  public void autorunScripts() {
    try {
      ScriptEngineManager factory = new ScriptEngineManager();
      ScriptEngine engine = factory.getEngineByName("JavaScript");
      engine.put("engine", engine);
      for(MappingObject js: ObjectLoader.getObjects(JSModul.class))
        engine.eval(((JSModul)js).getScript());

      for(MappingObject js: ObjectLoader.getObjects(DBFilter.create(JScript.class).AND_EQUAL("autoRun", true)))
        engine.eval(((JScript)js).getScript());
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  class DivisionLabelAction implements ActionListener {
    private EditorGui editor;
    private FXPlugin plugin;
    private FXD fxd;
    
    public DivisionLabelAction(Class<? extends EditorGui> editorClass) {
      try {
        editor = editorClass.newInstance();
        editor.setAutoLoadAndStore(true);
        editor.setVisibleOkButton(false);
        this.editor.setActive(false);
      }catch(InstantiationException | IllegalAccessException ex) {
        Messanger.showErrorMessage(ex);
      }
    }
    
    public DivisionLabelAction(final EditorGui editor) {
      this.editor = editor;
      editor.setAutoLoadAndStore(true);
      editor.setVisibleOkButton(false);
      this.editor.setActive(false);
    }
    
    public DivisionLabelAction(final FXPlugin plugin) {
      this.plugin = plugin;
      Platform.runLater(() -> plugin.initPlugin());
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      try {
        if(plugin != null) {
          Platform.runLater(() -> {
            if(fxd == null) {
              fxd = plugin.showWindow();
              fxd.setOnHidden(ev -> fxd = null);
            }
          });
        }else {
          if(editor.getInternalDialog() == null || editor.getInternalDialog().isClosed()) {
            desktop.add(editor);
            editor.getInternalDialog().addInternalFrameListener(new InternalFrameAdapter() {
              @Override
              public void internalFrameClosed(InternalFrameEvent ife) {
                editor.setActive(false);
                editor.clearTargets();
              }

              @Override
              public void internalFrameOpened(InternalFrameEvent ife) {
                editor.setActive(true);
                try {
                  editor.initTargets();

                  for(Component o:editor.getGUI().getComponents())
                  if(o instanceof EditorGuiPanel && ((EditorGuiPanel)o).getEditor().getId().longValue() != editor.getId().longValue())
                    ((EditorGuiPanel)o).getEditor().initTargets();

                  editor.initData();
                }catch(Exception ex) {
                  Messanger.showErrorMessage(ex);
                }
              }

              @Override
              public void internalFrameIconified(InternalFrameEvent ife) {
                editor.setActive(false);
                editor.clearTargets();
              }

              @Override
              public void internalFrameDeiconified(InternalFrameEvent ife) {
                editor.setActive(true);
                try {
                  editor.initTargets();

                  for(Component o:editor.getGUI().getComponents())
                  if(o instanceof EditorGuiPanel && ((EditorGuiPanel)o).getEditor().getId().longValue() != editor.getId().longValue())
                    ((EditorGuiPanel)o).getEditor().initTargets();

                  editor.initData();
                }catch(Exception ex) {
                  Messanger.showErrorMessage(ex);
                }
              }
            });
          }
          
          editor.getInternalDialog().setVisible(true);
          editor.getInternalDialog().setIcon(false);
          editor.getInternalDialog().setSelected(true);
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }
  
  private void initGC() {
    Thread t = new Thread(() -> {
      while(true) {
        try {
          SwingUtilities.invokeAndWait(() -> {
            long total  = Runtime.getRuntime().totalMemory();
            long free   = Runtime.getRuntime().freeMemory();
            long max    = Runtime.getRuntime().maxMemory();
            long memory = total - free;
            setTitle(getTitle().split(" memory: ")[0]+" memory: "+
                    (memory/1024<1?memory+"b":memory/1024/1024<1?(memory/1024)+"Kb":(memory/1024/1024)+"Mb")+"/"+
                    (total/1024<1?total+"b":total/1024/1024<1?(total/1024)+"Kb":(total/1024/1024)+"Mb")+"/"+
                    (max/1024<1?max+"b":max/1024/1024<1?(max/1024)+"Kb":(max/1024/1024)+"Mb"));
          });
          Thread.sleep(5000);
        }catch(InterruptedException | InvocationTargetException ex) {
          Logger.getRootLogger().fatal(ex.getMessage(), ex);
        }
      }
    });
    t.setDaemon(true);
    t.start();
  }
  
  private void initRxTxDriver() {
    String rxtxPath = null;
    
    if(System.getProperty("os.name").toLowerCase().contains("linux"))
      rxtxPath = "rxtx"+File.separator+"linux32";
    else if(System.getProperty("os.name").toLowerCase().contains("win"))
      rxtxPath = "rxtx"+File.separator+"win32";
    
    if(rxtxPath != null) {
      JarFile jarFile = null;
      try {
        jarFile = new JarFile("lib/resources.jar");
      }catch(Exception ex) {
        System.out.println(ex.getMessage());
      }
      if(jarFile == null) {
        try {
          URL url = ClientMain.class.getClassLoader().getResource("rxtx");
          Messanger.alert(url.toString());
          String path = url.getPath();
          path = path.substring(5, path.lastIndexOf("!"));
          jarFile = new JarFile(path);
        }catch(Exception ex) {
          System.out.println(ex.getMessage());
        }
      }
      if(jarFile != null) {
        try {
          String currentDir = new File("").getAbsolutePath();
          Enumeration<JarEntry> en = jarFile.entries();
          while(en.hasMoreElements()) {
            JarEntry entry = en.nextElement();
            if(!entry.isDirectory()) {
              String dir = null;
              int lastSeparatorIndex = entry.getName().lastIndexOf("/");
              if(lastSeparatorIndex != -1)
                dir = entry.getName().substring(0, lastSeparatorIndex);
              if(dir == null || !dir.toLowerCase().equalsIgnoreCase("meta-inf") && (dir.equals(rxtxPath) || !dir.startsWith("rxtx"))) {
                File extraxtPath = new File(currentDir+(dir==null?"":(File.separator+dir)));
                if(!extraxtPath.exists())
                  extraxtPath.mkdirs();
                File file = new File(currentDir+File.separator+entry.getName());
                if(!file.exists()) {
                  FileOutputStream out = new FileOutputStream(currentDir+File.separator+entry.getName());
                  InputStream in = jarFile.getInputStream(entry);
                  IOUtils.copy(in, out);
                  out.flush();
                  out.close();
                  out = null;
                  in.close();
                  in = null;
                }
              }
            }
          }
          jarFile.close();
          ClassPathLoader.addFile(new File(currentDir));
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    }
  }
}

/*


How to run javafx on android:

    Download dalvik sdk.
    Go to samples\HelloWorld\javafx - it's gradle project
    Modify location of dalvik-sdk and android-sdk in local.properties
    Example on my Windows system:
    sdk.dir=C\:\\dev\\android-sdk javafx.dir=C\:\\dev\\dalvik-sdk
    Run gradlew installDebug for building and installing apk on device. You also find outputs in build folder
    Launch application on device.(I seen black screen over 10 sec before first launch)
    Open project in Eclipse or Idea as standart gradle project


*/