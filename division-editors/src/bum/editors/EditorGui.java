package bum.editors;

import bum.editors.util.DivisionTarget;
import bum.editors.util.FileLibrary;
import division.plugin.RootPlugin;
import division.swing.DivisionDialog;
import division.swing.StatusBar;
import division.swing.guimessanger.Messanger;
import division.util.IDStore;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import org.apache.commons.lang3.ArrayUtils;

/*    ________________________________________________________________________
 *   |        |                                                      |        |
 *   |        |                      topPanel                        |        |
 *   |        |______________________________________________________|        |
 *   |        |        |                                    |        |        |
 *   |        |        |              toolPanel             |        |        |
 *   |        |        |____________________________________|        |        |
 *   |        |        |           |           |            |        |        |
 *   | WPanel | LPanel |  lPanel   | rootPanel |   rPanel   | RPanel | EPanel |
 *   |        |        |___________|___________|____________|        |        |
 *   |        |        |                                    |        |        |
 *   |        |        |             statusBar              |        |        |
 *   |        |________|____________________________________|________|        |
 *   |        |                                                      |        |
 *   |        |                     bottomPanel                      |        |
 *   |________|______________________________________________________|________|
 *   |                                                                        |
 *   |                             buttonsPanel                               |
 *   |________________________________________________________________________|
 *   |                                                                        |
 *   |                           dialogStatusBar                              |
 *   |________________________________________________________________________|
 */

/**
 * <code>
 *   <table border=1>
 *     <tr>
 *       <td align=center valign=middle rowspan=5>WPanel</td>
 *       <td align=center valign=middle colspan=5>topPanel</td>
 *       <td align=center valign=middle rowspan=5>EPanel</td>
 *     </tr>
 *     <tr>
 *       <td align=center valign=middle rowspan=3>LPanel</td>
 *       <td align=center valign=middle colspan=3>toolPanel</td>
 *       <td align=center valign=middle rowspan=3>RPanel</td>
 *     </tr>
 *     <tr>
 *       <td align=center valign=middle>lPanel</td>
 *       <td align=center valign=middle>rootPanel</td>
 *       <td align=center valign=middle>rPanel</td>
 *     </tr>
 *     <tr>
 *       <td align=center valign=middle colspan=3>statusBar</td>
 *     </tr>
 *     <tr>
 *       <td align=center valign=middle colspan=5>bottomPanel</td>
 *     </tr>
 *     <tr>
 *       <td align=center valign=middle colspan=7>buttonsPanel</td>
 *     </tr>
 *     <tr>
 *       <td align=center valign=middle colspan=7>dialogStatusBar</td>
 *     </tr>
 *   </table>
 * </code>
 * @author Russo
 */
public abstract class EditorGui extends RootPlugin implements EditorListener, EditorSource {
  private Long id;
  private String name;
  private final JPanel     GUIPanel        = new EditorGuiPanel(this, new GridBagLayout());

  private final JPanel     WPanel          = new JPanel();
  private final JPanel     topPanel        = new JPanel();
  private final JPanel     EPanel          = new JPanel();

  private final JPanel     LPanel          = new JPanel();
  private final JPanel     toolPanel       = new JPanel(new FlowLayout(FlowLayout.LEFT));
  private final JToolBar   toolBar         = new JToolBar();
  private final JPanel     RPanel          = new JPanel();

  private final JPanel     lPanel          = new JPanel();
  private       JPanel     rootPanel       = new EditorGuiPanel(this, new GridBagLayout());
  private final JPanel     rPanel          = new JPanel();
  
  private final StatusBar  dialogStatusBar = new StatusBar();
  private final StatusBar  statusBar       = new StatusBar();
  private final JPanel     bottomPanel     = new JPanel();
  private final JPanel     buttonsPanel    = new JPanel(new FlowLayout(FlowLayout.RIGHT));
  private final JToolBar   bottomToolBar   = new JToolBar();

  private final JButton    OkButton        = new JButton("Ok");

  private JFrame         frameDialog;
  private DivisionDialog dialog;
  private JInternalFrame iternalDialog;
  
  private String     title;
  private String     description;
  private ImageIcon  icon;

  private boolean editable  = true;
  private boolean enable    = true;
  private boolean autoLoad  = false;
  private boolean autoStore = false;
  private boolean active    = true;

  private final ArrayList components = new ArrayList();
  private DivisionTarget[] targets = new DivisionTarget[0];
  
  private final java.util.List<EditorListener> editorListeners = new ArrayList<>();

  private ActionListener okButtonAction = (ActionEvent e) -> {
    okButtonAction();
  };

  private MyComponentAdapter componentAdapter = new MyComponentAdapter();
  private ContainerListener containerListener = new ContainerListener() {
    @Override
    public void componentAdded(ContainerEvent e) {
      e.getChild().addComponentListener(componentAdapter);
      e.getContainer().setVisible(isContainerVisible(e.getContainer()));
    }

    @Override
    public void componentRemoved(ContainerEvent e) {
      e.getChild().removeComponentListener(componentAdapter);
      e.getContainer().setVisible(isContainerVisible(e.getContainer()));
    }
  };

  /**
   * Конструктор
   * @param title название (отображается в заглавии диалога)
   * @param icon иконка диалога
   */
  public EditorGui(String title, ImageIcon icon) {
    this(title,icon,null);
  }
  /**
   * Конструктор
   * @param title название (отображается в заглавии диалога)
   * @param icon иконка диалога
   * @param name имя данного редактора
   */
  public EditorGui(String title, ImageIcon icon, String name) {
    addComponentToStore(GUIPanel,"GUIPanel");
    this.title = title;
		this.icon = icon;
    this.name = name;

    WPanel.setVisible(isContainerVisible(WPanel));
    topPanel.setVisible(isContainerVisible(topPanel));
    EPanel.setVisible(isContainerVisible(EPanel));

    LPanel.setVisible(isContainerVisible(LPanel));
    toolPanel.setVisible(isContainerVisible(toolPanel));
    RPanel.setVisible(isContainerVisible(RPanel));

    lPanel.setVisible(isContainerVisible(lPanel));
    rootPanel.setVisible(isContainerVisible(rootPanel));
    rPanel.setVisible(isContainerVisible(rPanel));

    dialogStatusBar.setVisible(isContainerVisible(dialogStatusBar));
    statusBar.setVisible(isContainerVisible(statusBar));
    bottomPanel.setVisible(isContainerVisible(bottomPanel));
    buttonsPanel.setVisible(isContainerVisible(buttonsPanel));

    toolBar.setFloatable(false);
    toolBar.setBorder(BorderFactory.createCompoundBorder());

    GUIPanel.add(WPanel,          new GridBagConstraints(0, 0, 1, 5, 0.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.VERTICAL, new Insets(3, 3, 3, 3), 0, 0));
    GUIPanel.add(topPanel,        new GridBagConstraints(1, 0, 5, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
    GUIPanel.add(EPanel,          new GridBagConstraints(6, 0, 1, 5, 0.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.VERTICAL, new Insets(3, 3, 3, 3), 0, 0));

    GUIPanel.add(LPanel,          new GridBagConstraints(1, 1, 1, 3, 0.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.VERTICAL, new Insets(3, 3, 3, 3), 0, 0));
    GUIPanel.add(toolPanel,       new GridBagConstraints(2, 1, 3, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    GUIPanel.add(RPanel,          new GridBagConstraints(5, 1, 1, 3, 0.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.VERTICAL, new Insets(3, 3, 3, 3), 0, 0));

    GUIPanel.add(lPanel,          new GridBagConstraints(2, 2, 1, 1, 0.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.VERTICAL, new Insets(0, 3, 0, 3), 0, 0));
    GUIPanel.add(rootPanel,       new GridBagConstraints(3, 2, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 3, 0, 3), 0, 0));
    GUIPanel.add(rPanel,          new GridBagConstraints(3, 2, 1, 1, 0.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.VERTICAL, new Insets(0, 3, 0, 3), 0, 0));

    GUIPanel.add(statusBar,       new GridBagConstraints(2, 3, 3, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 3, 3, 3), 0, 0));
    GUIPanel.add(bottomPanel,     new GridBagConstraints(1, 4, 5, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
    GUIPanel.add(buttonsPanel,    new GridBagConstraints(0, 5, 7, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    GUIPanel.add(dialogStatusBar, new GridBagConstraints(0, 6, 7, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 1, 0), 0, 0));

    WPanel.addContainerListener(containerListener);
    topPanel.addContainerListener(containerListener);
    EPanel.addContainerListener(containerListener);

    LPanel.addContainerListener(containerListener);
    toolPanel.addContainerListener(containerListener);
    RPanel.addContainerListener(containerListener);

    lPanel.addContainerListener(containerListener);
    rootPanel.addContainerListener(containerListener);
    rPanel.addContainerListener(containerListener);

    dialogStatusBar.addContainerListener(containerListener);
    statusBar.addContainerListener(containerListener);
    bottomPanel.addContainerListener(containerListener);
    buttonsPanel.addContainerListener(containerListener);

    toolPanel.add(toolBar);
    buttonsPanel.add(OkButton);
    OkButton.addActionListener(okButtonAction);
    this.id = IDStore.createID();
    EditorController.addToStack(id, this);
  }
  
  public void setTitleBorder(String title) {
    getGUI().setBorder(BorderFactory.createTitledBorder(title));
  }

  @Override
  public java.util.List<EditorListener> getEditorListeners() {
    return editorListeners;
  }
  
  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public void setActive(boolean active) {
    this.active = active;
  }

  public DivisionTarget[] getTargets() {
    return targets;
  }
  
  public void addTarget(DivisionTarget target) {
    targets = (DivisionTarget[]) ArrayUtils.add(targets, target);
  }
  
  public void removeTarget(DivisionTarget target) {
    targets = (DivisionTarget[]) ArrayUtils.removeElement(targets, target);
    target.dispose();
  }

  public JToolBar getBottomToolBar() {
    return bottomToolBar;
  }
  
  public boolean beforeCreateDialog() {
    return true;
  }
  
  public boolean afterCreateDialog() {
    return true;
  }

  /**
   * Возращает имя
   * @return имя редактора
   */
  public String getName() {
    return name;
  }
  /**
   * Устанавливает имя
   * @param name имя редактора
   */
  public void setName(String name) {
    this.name = name;
  }
  /**
   * Добавляет редактор в список объектов, подлежащих
   * сохранению своих состояний
   * @param editor редактор
   */
  public void addSubEditorToStore(EditorGui editor) {
    addSubEditorToStore(editor,null);
  }
  
  public void addSubEditorToStore(EditorGui editor, String name) {
    if(!components.contains(editor)) {
      editor.setName(name==null||"".equals(name)?(editor.getName()==null||"".equals(editor.getName())?editor.getClass().getName():editor.getName())+"_For_"+(getName()==null||"".equals(getName())?getClass().getName():getName()):name);
      components.add(editor);
    }
  }
  /**
   * Добавляет объект в список объектов, подлежащих
   * сохранению своих состояний
   * @param component добавляемый компонент
   * @param name имя добавляемого компонента
   */
  public void addComponentToStore(Component component, String name) {
    component.setName(name);
    addComponentToStore(component);
  }
  /**
   * Добавляет объект в список объектов, подлежащих
   * сохранению своих состояний
   * @param component добавляемый компонент
   */
  public void addComponentToStore(Component component) {
    if(component.getName() == null || component.getName().equals(""))
      component.setName((component.getClass().getName()+"_For_"+(getName()==null||"".equals(getName())?getClass().getName():getName())).replaceAll("\\$", "_"));
    components.add(component);
  }

  public ArrayList getComponentsToStore() {
    return components;
  }

  /**
   * Возвращает компонент по его имени.
   * Если компонент с таким именем в GUI отсутствует,
   * то возвратится null.
   * @param componentName имя компонента
   * @return компонент
   */
  public Component getComponent(String componentName) {
    return getComponent(GUIPanel, componentName);
  }
  /**
   * Возвращает компонент по его имени из контейнера com.
   * Если компонент с таким именем в com отсутствует,
   * то возвратится null.
   * @param com контейнер в котором будет произведён поиск
   * @param componentName имя компонента
   * @return компонент
   */
  public Component getComponent(Container com, String componentName) {
    for(Component c:com.getComponents()) {
      if(c.getName().equals(componentName))
        return c;
      else if(c instanceof Container)
        return getComponent((Container)c,componentName);
    }
    return null;
  }
  /**
   * Возвращает действие по кнопке OK
   * @return действие по кнопке OK
   */
  public ActionListener getOkButtonAction() {
    return okButtonAction;
  }
  /**
   * Устанавливает действие по кнопке OK
   * @param okButtonAction действие по кнопке OK
   */
  public void setOkButtonAction(ActionListener okButtonAction) {
    OkButton.removeActionListener(this.okButtonAction);
    this.okButtonAction = okButtonAction;
    OkButton.addActionListener(okButtonAction);
  }
  /**
   * Возвращает идентификатор редактора
   * @return идентификатор редактора
   */
  public Long getId() {
    return id;
  }

  /**
   * Метод, который срабатывает по умалчанию
   * при нажатии кнопки OK
   * @return результат выполнения
   */
  public abstract Boolean okButtonAction();

  private boolean isContainerVisible(Container c) {
    boolean is = false;
    for(Component com:c.getComponents())
      if(com.isVisible())
        return true;
    return is;
  }
  /**
   * Возвращает диалоговое окно с данным редактором
   * @return диалоговое окно
   */
  public JDialog getDialog() {
    return dialog;
  }
  
  public JInternalFrame getInternalDialog() {
    return this.iternalDialog;
  }
  
  public JFrame getFrameDialog() {
    return frameDialog;
  }
  
  public void waitCursor() {
    waitCursor(this.getRootPanel());
  }
  
  public void defaultCursor() {
    defaultCursor(this.getRootPanel());
  }
  
  public static void waitCursor(JComponent component) {
    setCursor(component, new Cursor(Cursor.WAIT_CURSOR));
  }
  
  public static void defaultCursor(JComponent component) {
    setCursor(component, new Cursor(Cursor.DEFAULT_CURSOR));
  }

  public static void setCursor(JComponent component, Cursor cursor) {
    component.setCursor(cursor);
    for(Component comp:component.getComponents())
      if(comp instanceof JComponent)
        setCursor((JComponent)comp, cursor);
  }
  
  public static void setComponentEnable(Component component, boolean is) {
    component.setEnabled(is);
    
    if(component instanceof JScrollPane && ((JScrollPane)component).getRowHeader() != null && ((JScrollPane)component).getRowHeader().getView() instanceof JComponent) {
      setComponentEnable((JComponent)((JScrollPane)component).getRowHeader().getView(),is);
    }
    
    if(component instanceof JComponent)
      for(Component com:((JComponent)component).getComponents())
        setComponentEnable(com,is);
  }
  
  /**
   * Устанавливает курсор на редактором
   * @param cursor курсор
   */
  public void setCursor(final Cursor cursor) {
    setCursor(getGUI(),cursor);
  }
  /**
   * <code>
   *   <table border=1>
   *     <tr>
   *       <td align=center valign=middle rowspan=5>WPanel</td>
   *       <td align=center valign=middle colspan=5>topPanel</td>
   *       <td align=center valign=middle rowspan=5><b>EPanel</b></td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle rowspan=3>LPanel</td>
   *       <td align=center valign=middle colspan=3>toolPanel</td>
   *       <td align=center valign=middle rowspan=3>RPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle>lPanel</td>
   *       <td align=center valign=middle>rootPanel</td>
   *       <td align=center valign=middle>rPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=3>statusBar</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=5>bottomPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>buttonsPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>dialogStatusBar</td>
   *     </tr>
   *   </table>
   * </code>
   * @return EPanel
   */
  public JPanel getEPanel() {
    return EPanel;
  }
  /**
   * <code>
   *   <table border=1>
   *     <tr>
   *       <td align=center valign=middle rowspan=5>WPanel</td>
   *       <td align=center valign=middle colspan=5>topPanel</td>
   *       <td align=center valign=middle rowspan=5>EPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle rowspan=3><b>LPanel</b></td>
   *       <td align=center valign=middle colspan=3>toolPanel</td>
   *       <td align=center valign=middle rowspan=3>RPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle>lPanel</td>
   *       <td align=center valign=middle>rootPanel</td>
   *       <td align=center valign=middle>rPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=3>statusBar</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=5>bottomPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>buttonsPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>dialogStatusBar</td>
   *     </tr>
   *   </table>
   * </code>
   * @return LPanel
   */
  public JPanel getLPanel() {
    return LPanel;
  }
  /**
   * <code>
   *   <table border=1>
   *     <tr>
   *       <td align=center valign=middle rowspan=5>WPanel</td>
   *       <td align=center valign=middle colspan=5>topPanel</td>
   *       <td align=center valign=middle rowspan=5>EPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle rowspan=3>LPanel</td>
   *       <td align=center valign=middle colspan=3>toolPanel</td>
   *       <td align=center valign=middle rowspan=3><b>RPanel</b></td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle>lPanel</td>
   *       <td align=center valign=middle>rootPanel</td>
   *       <td align=center valign=middle>rPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=3>statusBar</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=5>bottomPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>buttonsPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>dialogStatusBar</td>
   *     </tr>
   *   </table>
   * </code>
   * @return RPanel
   */
  public JPanel getRPanel() {
    return RPanel;
  }
  /**
   * <code>
   *   <table border=1>
   *     <tr>
   *       <td align=center valign=middle rowspan=5><b>WPanel</b></td>
   *       <td align=center valign=middle colspan=5>topPanel</td>
   *       <td align=center valign=middle rowspan=5>EPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle rowspan=3>LPanel</td>
   *       <td align=center valign=middle colspan=3>toolPanel</td>
   *       <td align=center valign=middle rowspan=3>RPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle>lPanel</td>
   *       <td align=center valign=middle>rootPanel</td>
   *       <td align=center valign=middle>rPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=3>statusBar</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=5>bottomPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>buttonsPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>dialogStatusBar</td>
   *     </tr>
   *   </table>
   * </code>
   * @return WPanel
   */
  public JPanel getWPanel() {
    return WPanel;
  }
  /**
   * <code>
   *   <table border=1>
   *     <tr>
   *       <td align=center valign=middle rowspan=5>WPanel</td>
   *       <td align=center valign=middle colspan=5>topPanel</td>
   *       <td align=center valign=middle rowspan=5>EPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle rowspan=3>LPanel</td>
   *       <td align=center valign=middle colspan=3>toolPanel</td>
   *       <td align=center valign=middle rowspan=3>RPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle><b>lPanel</b></td>
   *       <td align=center valign=middle>rootPanel</td>
   *       <td align=center valign=middle>rPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=3>statusBar</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=5>bottomPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>buttonsPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>dialogStatusBar</td>
   *     </tr>
   *   </table>
   * </code>
   * @return lPanel
   */
  public JPanel getlPanel() {
    return lPanel;
  }
  /**
   * <code>
   *   <table border=1>
   *     <tr>
   *       <td align=center valign=middle rowspan=5>WPanel</td>
   *       <td align=center valign=middle colspan=5>topPanel</td>
   *       <td align=center valign=middle rowspan=5>EPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle rowspan=3>LPanel</td>
   *       <td align=center valign=middle colspan=3>toolPanel</td>
   *       <td align=center valign=middle rowspan=3>RPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle>lPanel</td>
   *       <td align=center valign=middle>rootPanel</td>
   *       <td align=center valign=middle><b>rPanel</b></td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=3>statusBar</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=5>bottomPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>buttonsPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>dialogStatusBar</td>
   *     </tr>
   *   </table>
   * </code>
   * @return rPanel
   */
  public JPanel getrPanel() {
    return rPanel;
  }
  /**
   * <code>
   *   <table border=1>
   *     <tr>
   *       <td align=center valign=middle rowspan=5>WPanel</td>
   *       <td align=center valign=middle colspan=5>topPanel</td>
   *       <td align=center valign=middle rowspan=5>EPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle rowspan=3>LPanel</td>
   *       <td align=center valign=middle colspan=3><b>toolPanel</b></td>
   *       <td align=center valign=middle rowspan=3>RPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle>lPanel</td>
   *       <td align=center valign=middle>rootPanel</td>
   *       <td align=center valign=middle>rPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=3>statusBar</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=5>bottomPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>buttonsPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>dialogStatusBar</td>
   *     </tr>
   *   </table>
   * </code>
   * @return toolPanel
   */
  public JPanel getToolPanel() {
    return toolPanel;
  }
  /**
   * Возвращает панель инструментов
   * @return панель инструментов
   */
  public JToolBar getToolBar() {
    return toolBar;
  }

  /**
   * <code>
   *   <table border=1>
   *     <tr>
   *       <td align=center valign=middle rowspan=5>WPanel</td>
   *       <td align=center valign=middle colspan=5>topPanel</td>
   *       <td align=center valign=middle rowspan=5>EPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle rowspan=3>LPanel</td>
   *       <td align=center valign=middle colspan=3>toolPanel</td>
   *       <td align=center valign=middle rowspan=3>RPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle>lPanel</td>
   *       <td align=center valign=middle>rootPanel</td>
   *       <td align=center valign=middle>rPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=3><b>statusBar</b></td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=5>bottomPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>buttonsPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>dialogStatusBar</td>
   *     </tr>
   *   </table>
   * </code>
   * @return statusBar
   */
  public StatusBar getStatusBar() {
    return statusBar;
  }
  /**
   * Возвращает главную панель редактора
   * @return главная панель редактора
   */
  public JPanel getGUI() {
    return GUIPanel;
  }
  /**
   * <code>
   *   <table border=1>
   *     <tr>
   *       <td align=center valign=middle rowspan=5>WPanel</td>
   *       <td align=center valign=middle colspan=5>topPanel</td>
   *       <td align=center valign=middle rowspan=5>EPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle rowspan=3>LPanel</td>
   *       <td align=center valign=middle colspan=3>toolPanel</td>
   *       <td align=center valign=middle rowspan=3>RPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle>lPanel</td>
   *       <td align=center valign=middle>rootPanel</td>
   *       <td align=center valign=middle>rPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=3>statusBar</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=5>bottomPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7><b>buttonsPanel</b></td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>dialogStatusBar</td>
   *     </tr>
   *   </table>
   * </code>
   * @return buttonsPanel
   */
  public JPanel getButtonsPanel() {
    return buttonsPanel;
  }
  /**
   * <code>
   *   <table border=1>
   *     <tr>
   *       <td align=center valign=middle rowspan=5>WPanel</td>
   *       <td align=center valign=middle colspan=5>topPanel</td>
   *       <td align=center valign=middle rowspan=5>EPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle rowspan=3>LPanel</td>
   *       <td align=center valign=middle colspan=3>toolPanel</td>
   *       <td align=center valign=middle rowspan=3>RPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle>lPanel</td>
   *       <td align=center valign=middle>rootPanel</td>
   *       <td align=center valign=middle>rPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=3>statusBar</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=5><b>bottomPanel</b></td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>buttonsPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>dialogStatusBar</td>
   *     </tr>
   *   </table>
   * </code>
   * @return bottomPanel
   */
  public JPanel getBottomPanel() {
    return bottomPanel;
  }
  /**
   * <code>
   *   <table border=1>
   *     <tr>
   *       <td align=center valign=middle rowspan=5>WPanel</td>
   *       <td align=center valign=middle colspan=5><b>topPanel</b></td>
   *       <td align=center valign=middle rowspan=5>EPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle rowspan=3>LPanel</td>
   *       <td align=center valign=middle colspan=3>toolPanel</td>
   *       <td align=center valign=middle rowspan=3>RPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle>lPanel</td>
   *       <td align=center valign=middle>rootPanel</td>
   *       <td align=center valign=middle>rPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=3>statusBar</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=5>bottomPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>buttonsPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>dialogStatusBar</td>
   *     </tr>
   *   </table>
   * </code>
   * @return topPanel
   */
  public JPanel getTopPanel() {
    return topPanel;
  }
  /**
   * <code>
   *   <table border=1>
   *     <tr>
   *       <td align=center valign=middle rowspan=5>WPanel</td>
   *       <td align=center valign=middle colspan=5>topPanel</td>
   *       <td align=center valign=middle rowspan=5>EPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle rowspan=3>LPanel</td>
   *       <td align=center valign=middle colspan=3>toolPanel</td>
   *       <td align=center valign=middle rowspan=3>RPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle>lPanel</td>
   *       <td align=center valign=middle>rootPanel</td>
   *       <td align=center valign=middle>rPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=3>statusBar</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=5>bottomPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>buttonsPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7><b>dialogStatusBar</b></td>
   *     </tr>
   *   </table>
   * </code>
   * @return dialogStatusBar
   */
  public StatusBar getDialogStatusBar() {
    return dialogStatusBar;
  }
  /**
   * <code>
   *   <table border=1>
   *     <tr>
   *       <td align=center valign=middle rowspan=5>WPanel</td>
   *       <td align=center valign=middle colspan=5>topPanel</td>
   *       <td align=center valign=middle rowspan=5>EPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle rowspan=3>LPanel</td>
   *       <td align=center valign=middle colspan=3>toolPanel</td>
   *       <td align=center valign=middle rowspan=3>RPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle>lPanel</td>
   *       <td align=center valign=middle><b>rootPanel</b></td>
   *       <td align=center valign=middle>rPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=3>statusBar</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=5>bottomPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>buttonsPanel</td>
   *     </tr>
   *     <tr>
   *       <td align=center valign=middle colspan=7>dialogStatusBar</td>
   *     </tr>
   *   </table>
   * </code>
   * @return rootPanel
   */
  public JPanel getRootPanel() {
    return this.rootPanel;
  }
  /**
   * Заменяет стандартную панель редактора на panel
   * @param panel замена для rootPanel
   */
  public void setRootPanel(JPanel panel) {
    GUIPanel.remove(this.rootPanel);
    this.rootPanel = panel;
    GUIPanel.add(rootPanel, new GridBagConstraints(3, 2, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 5, 0, 5), 0, 0));
  }
  /**
   * Возвращает кнопку OK
   * @return кнопка OK
   */
  public JButton getOkButton() {
    return this.OkButton;
  }
  /**
   * <code>
   * <b>enable = true</b> - все компоненты редактора активны<br/>
   * <b>enable = false</b> - все компоненты редактора деактивированны<br/>
   * </code>
   * @param enable см. выше:);)
   */
  public void setEnabled(boolean enable) {
    this.enable = enable;
    //setEnableComponent(getGUI());
    setComponentEnable(getGUI(), enable);
  }
  /**
   * Возвращает показатель активности редактора
   * @return показатель активности редактора
   */
  public boolean isEnabled() {
    return enable;
  }

  /*private void setEnableComponent(JComponent com) {
    com.setEnabled(isEnabled());
    for(int i=0;i<com.getComponentCount();i++) {
      if(com.getComponent(i) instanceof JComponent)
        setEnableComponent((JComponent)com.getComponent(i));
    }
  }*/
  /**
   * Устанавливает показатель редактируемости редактора
   * @param editable <code><b>true</b> - редактируемый<br/><b>false</b> - не редактируемый</code>
   */
  public void setEditable(boolean editable) {
    this.editable = editable;
  }
  /**
   * Возвращает показатель редактируемости редактора
   * @return показатель редактируемости редактора
   */
  public boolean isEditable() {
    return this.editable;
  }
  /**
   * Возвращает наименование редактора.
   * @return наименование редактора
   */
	public String getTitle() {
    if("".equals(title))
      return getEmptyObjectTitle();
    else return title;
	}
  /**
   * Устанавливает наименование редактора
   * @param title наименование редактора
   */
  public void setTitle(String title) {
    this.title = title;
    if(dialog != null)
      dialog.setTitle(title);
    if(iternalDialog != null)
      iternalDialog.setTitle(title);
    if(frameDialog != null)
      frameDialog.setTitle(title);
  }
  /**
   * Возвращает описание редактора
   * @return описание редактора
   */
	public String getDescription() {
		return description;
	}
  /**
   * Устанавливает описание редактора
   * @param description описание редактора
   */
  public void setDescription(String description) {
		this.description = description;
	}
  /**
   * Возвращает иконку редактора
   * @return иконка редактора
   */
	public ImageIcon getIcon()	{
		return icon;
	}
  /**
   * Устанавливает иконку редактора
   * @param icon иконка редактора
   */
  public void setIcon(ImageIcon icon) {
    this.icon = icon;
  }
  /**
   * Возвращает наименование редактора по умолчанию
   * @return наименование редактора по умолчанию
   */
  public String getEmptyObjectTitle() {
    return "Новый объект";
  }
  /**
   * Возвращает показатель автозагрузки, т.е.
   * будет ли редактор автоматически загружать
   * сохранённые порамметры объектов интерфейса
   * при открытии.
   * @return показатель автозагрузки
   */
  public boolean isAutoLoad() {
    return autoLoad;
  }
  
  public void setAutoLoadAndStore(boolean auto) {
    setAutoLoad(auto);
    setAutoStore(auto);
  }
  
  /**
   * Устанавливает показатель автозагрузки, т.е.
   * будет ли редактор автоматически загружать
   * сохранённые порамметры объектов интерфейса
   * при открытии.
   * @param autoLoad показатель автозагрузки
   */
  public void setAutoLoad(boolean autoLoad) {
    this.autoLoad = autoLoad;
  }
  /**
   * Возвращает показатель автосохранения, т.е.
   * будет ли редактор автоматически сохранять
   * порамметры объектов интерфейса
   * при закрытии.
   * @return показатель автосохранения
   */
  public boolean isAutoStore() {
    return autoStore;
  }
  /**
   * Устанавливает показатель автосохранения, т.е.
   * будет ли редактор автоматически сохранять
   * порамметры объектов интерфейса
   * при закрытии.
   * @param autoStore показатель автосохранения
   */
  public void setAutoStore(boolean autoStore) {
    this.autoStore = autoStore;
  }
  /**
   * Устанавливает показатель видимости кнопки OK
   * @param visibleOkButton показатель видимости кнопки OK
   */
  public void setVisibleOkButton(boolean visibleOkButton) {
    OkButton.setVisible(visibleOkButton);
  }
  /**
   * Возвращает показатель видимости кнопки OK
   * @return показатель видимости кнопки OK
   */
  public boolean isVisibleOkButton() {
    return OkButton.isVisible();
  }
  
  public void showEditorDialog() {
    showEditorDialog(null);
  }
  
  public void showEditorDialog(Container container) {
    Object parent = getParentWindow(container);
    parent = parent==null?getParent():parent;
    if(parent instanceof JInternalFrame) {
      EditorController.getDeskTop().add(this);
      if(getInternalDialog() != null)
        getInternalDialog().setVisible(true);
    }else if(parent instanceof JDialog || parent instanceof JFrame || parent instanceof Window)
      createDialog(this, true).setVisible(true);
  }
  
  /**
   * Создаёт диалоговое окно с данным редактором
   * @return диалоговое окно с данным редактором
   */
  public JDialog createDialog() {
    return createDialog(false);
  }

  public JDialog createDialog(boolean modal) {
    return createDialog((Window)getParent(), modal);
  }
  /**
   * Создаёт диалоговое окно с данным редактором
   * @param editor родительский редактор
   * @return диалоговое окно с данным редактором
   */
  public JDialog createDialog(EditorGui editor) {
    return createDialog(editor,false);
  }

  public JDialog createDialog(EditorGui editor, boolean modal) {
    return createDialog((Window)editor.getParent(), modal);
  }
  /**
   * Создаёт диалоговое окно с данным редактором
   * @param component родительский объект
   * @return диалоговое окно с данным редактором
   */
  public JDialog createDialog(Component component) {
    return createDialog(component,false);
  }

  public JDialog createDialog(Component component, boolean modal) {
    return createDialog((Window)getParent(component), modal);
  }
  /**
   * Создаёт диалоговое окно с данным редактором
   * @param root родительское окно
   * @return диалоговое окно с данным редактором
   */
  public JDialog createDialog(Window root) {
    return createDialog(root,false);
  }

  public JDialog createDialog(Window root, boolean modal) {
    if(!beforeCreateDialog())
      return null;
    if(root == null)
      root = EditorController.getFrame();
    if(isAutoLoad())
      load();
    dialog = new DivisionDialog(root);
    if(modal)
      dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
    //dialog.setModal(modal);

    dialog.setContentPane(getGUI());
    dialog.setTitle(getTitle());
    
    if(getIcon() != null)
      dialog.setIconImage(((ImageIcon)getIcon()).getImage());
    
    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowOpened(WindowEvent e) {
      }

      @Override
      public void windowClosing(WindowEvent e) {
        closeDialog();
      }
    });

    //dialog.pack();
    dialog.centerLocation();
    if(!afterCreateDialog()) {
      dialog = null;
      return null;
    }
    return dialog;
  }
  
  public JFrame createFrameDialog(boolean modal) {
    if(!beforeCreateDialog())
      return null;
    if(isAutoLoad())
      load();
    frameDialog = new JFrame();
    
    frameDialog.setAlwaysOnTop(modal);
    
    frameDialog.setContentPane(getGUI());
    frameDialog.setTitle(getTitle());
    if(getIcon() != null)
      frameDialog.setIconImage(((ImageIcon)getIcon()).getImage());
    frameDialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowOpened(WindowEvent e) {
      }

      @Override
      public void windowClosing(WindowEvent e) {
        closeDialog();
      }
    });
    
    frameDialog.pack();
    //dialog.centerLocation();
    if(!afterCreateDialog()) {
      frameDialog = null;
      return null;
    }
    return frameDialog;
  }
  
  public JInternalFrame createIternalDialog(boolean resizable, boolean closable, boolean maximizable, boolean iconifiable) {
    if(!beforeCreateDialog())
      return null;

    if(isAutoLoad())
      load();
    iternalDialog = new JInternalFrame(getTitle(), resizable, closable, maximizable, iconifiable);
    
    iternalDialog.setContentPane(getGUI());
    iternalDialog.setTitle(getTitle());
    iternalDialog.setName(getName());
    iternalDialog.pack();
    iternalDialog.addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameIconified(InternalFrameEvent e) {
        /*for(DivisionTarget target:getTargets())
          target.reload();*/
      }
      
      @Override
      public void internalFrameDeiconified(InternalFrameEvent e) {
        /*for(DivisionTarget target:getTargets())
          target.reload();*/
      }

      @Override
      public void internalFrameActivated(InternalFrameEvent e) {
        /*for(DivisionTarget target:getTargets())
          target.reload();*/
      }
      
      @Override
      public void internalFrameClosing(InternalFrameEvent ife) {
        closeDialog();
      }
    });

    iternalDialog.pack();
    if(!afterCreateDialog()) {
      iternalDialog = null;
      return null;
    }
    return iternalDialog;
  }
  
  public static Container getParentWindow(Container container) {
    while(!(container instanceof Window || container instanceof JInternalFrame) && container != null) {
      container = container.getParent();
    }
    return container;
  }
  
  public static Component getParent(Component parent) {
    while(!(parent instanceof Window) && parent != null)
      parent = parent.getParent();
    return parent==null?EditorController.getFrame():parent;
  }
  
  public Component getParent() {
    return getParent(getGUI());
  }
  
  /**
   * Закрывает диалог
   */
  public void closeDialog() {
    dispose();
  }
  /**
   * Удаляет данный редактор из контроллера редакторов
   */
  public void dispose() {
    EditorController.dispose(this);
  }

  /**
   * Загружает парамметры окна, компонентов и т.п.
   */
  public void load() {
    FileLibrary.load(components, (getName()==null||"".equals(getName())?getClass().getName():getName())+".xml");
  }
  /**
   * Сохраняет парамметры окна, компонентов и т.п.
   */
  public void store() {
    if(getGUI().getSize().width > 0)
      FileLibrary.store(components, (getName()==null||"".equals(getName())?getClass().getName():getName())+".xml");
  }
  
  public void clear() {
    clearData();
    checkMenu();
  }

  public void clearData() {
  }
  
  public void checkMenu() {
  }
  
  public void clearTargets() {
    for(DivisionTarget target:getTargets())
      target.dispose();
    targets = new DivisionTarget[0];
    
    for(Component o:getGUI().getComponents())
      if(o instanceof EditorGuiPanel && ((EditorGuiPanel)o).getEditor().getId().longValue() != getId().longValue())
        ((EditorGuiPanel)o).getEditor().clearTargets();
  }
  
  public abstract void initTargets();
  
  public abstract void initData() throws Exception;
  
  @Override
  public void start() {
    try {
      setAutoLoadAndStore(true);
      setVisibleOkButton(false);
      
      if(getInternalDialog() == null || getInternalDialog().isClosed()) {
        getDesktop().add(this);
        getInternalDialog().addInternalFrameListener(new InternalFrameAdapter() {
          @Override
          public void internalFrameClosed(InternalFrameEvent ife) {
            setActive(false);
          }

          @Override
          public void internalFrameOpened(InternalFrameEvent ife) {
            setActive(true);
            try {
              initData();
            }catch(Exception ex) {
              Messanger.showErrorMessage(ex);
            }
          }

          @Override
          public void internalFrameIconified(InternalFrameEvent ife) {
            setActive(false);
          }

          @Override
          public void internalFrameDeiconified(InternalFrameEvent ife) {
            setActive(true);
            try {
              initData();
            }catch(Exception ex) {
              Messanger.showErrorMessage(ex);
            }
          }
        });
      }

      if(!getInternalDialog().isVisible())
        getInternalDialog().setVisible(true);

      if(getInternalDialog().isIcon())
          getInternalDialog().setIcon(false);

      getInternalDialog().setSelected(true);
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  class MyComponentAdapter extends ComponentAdapter {
    @Override
    public void componentShown(ComponentEvent e) {
      e.getComponent().getParent().setVisible(isContainerVisible(e.getComponent().getParent()));
    }

    @Override
    public void componentHidden(ComponentEvent e) {
      e.getComponent().getParent().setVisible(isContainerVisible(e.getComponent().getParent()));
    }
  }
}