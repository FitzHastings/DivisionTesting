package bum.editors;

import bum.editors.util.DivisionTarget;
import bum.editors.util.ObjectLoader;
import division.exportimport.ExportImportUtil;
import division.fx.PropertyMap;
import division.fx.util.MsgTrash;
import division.swing.guimessanger.Messanger;
import division.swing.DivisionScrollPane;
import division.swing.DivisionToolButton;
import division.swing.dnd.ObjectTransferable;
import division.util.FileLoader;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import mapping.MappingObject;
import mapping.MappingObject.RemoveAction;
import org.apache.commons.lang3.ArrayUtils;
import util.filter.local.DBFilter;

public abstract class DBTableEditor extends Editor implements DragGestureListener, DragSourceListener, DropTargetListener {
  private Class<? extends MappingObject> objectClass;
  
  protected DivisionToolButton addToolButton     = new DivisionToolButton(FileLoader.getIcon("Add16.gif"),"Добавить");
  protected DivisionToolButton editToolButton    = new DivisionToolButton(FileLoader.getIcon("Edit16.gif"),"Редактировать");
  protected DivisionToolButton removeToolButton  = new DivisionToolButton(FileLoader.getIcon("Delete16.gif"),"Удалить");
  protected DivisionToolButton exportToolButton  = new DivisionToolButton(FileLoader.getIcon("Export16.gif"),"Экспортировать");
  protected DivisionToolButton importToolButton  = new DivisionToolButton(FileLoader.getIcon("Import16.gif"),"Импортировать");
  protected DivisionToolButton confToolButton    = new DivisionToolButton(FileLoader.getIcon("images/tools-16.png"),"Настройки");
  protected DivisionToolButton toCurrentlButton  = new DivisionToolButton("в актуальные","в актуальные");

  protected DivisionToolButton reloadToolButton = new DivisionToolButton(FileLoader.getIcon("reload.gif"),"Обновить");
  protected boolean        toolActive       = true;

  protected JPopupMenu popMenu              = new JPopupMenu();
  protected JMenuItem  addPopMenuItem       = new JMenuItem("Добавить");
  protected JMenuItem  editPopMenuItem      = new JMenuItem("Редактировать");
  protected JMenuItem  removePopMenuItem    = new JMenuItem("Удалить");
  protected JMenuItem  exportPopMenuItem    = new JMenuItem("Экспортировать");
  protected JMenuItem  importPopMenuItem    = new JMenuItem("Импортировать");
  protected JMenuItem  unSelectMenuItem     = new JMenuItem("Снять выделение");
  protected JMenuItem  setMarkerMenuItem    = new JMenuItem("Маркировать");
  protected JMenuItem  removeMarkerMenuItem = new JMenuItem("Убрать маркер");
  protected JMenuItem  toCurrentMenuItem    = new JMenuItem("Вернуть в актуальные");
  protected JMenu      scriptMenu           = new JMenu("Дополнительные возможности");
  protected boolean    popMenuActive        = true;

  private MappingObject.RemoveAction removeActionType = MappingObject.RemoveAction.MARK_FOR_DELETE;

  protected JProgressBar   progressBar      = new JProgressBar();
  protected JLabel         countLabel       = new JLabel("");
  protected DivisionScrollPane scroll           = new DivisionScrollPane();
  
  protected ExecutorService pool = Executors.newFixedThreadPool(5);

  protected ArrayList<Integer> createtedObjects = new ArrayList<>();
  protected boolean waitingSelect = false;

  private boolean doubleClickSelectable = false;
  private boolean admin = true;

  private Class objectEditorClass;

  private String[] dataFields;
  private String[] sortFields;
  private MappingObject.Type type = null;
  
  private DBFilter rootFilter;
  private DBFilter tableFilter;
  private DBFilter clientFilter;
  private DBFilter typeFilter;
  private DBFilter tmpFilter;

  private Integer[] selectedId = new Integer[0];
  
  private InitDataTask initDataTask;

  private ActionListener addAction = (ActionEvent e) -> {
    if(isAddFunction())
      addButtonAction();
  };
  
  private ActionListener editAction = (ActionEvent e) -> {
    if(isEditFunction())
      editButtonAction();
  };
  
  private ActionListener removeAction = (ActionEvent e) -> {
    if(isRemoveFunction())
      removeButtonAction();
  };
  
  private ActionListener confAction = (ActionEvent e) -> {
    if(isConfFunction())
      confButtonAction();
  };

  private ActionListener exportAction = (ActionEvent e) -> {
    if(isExportFunction())
      exportButtonAction();
  };

  private ActionListener importAction = (ActionEvent e) -> {
    if(isImportFunction())
      importButtonAction();
  };

  private ActionListener reloadAction = (ActionEvent e) -> {
    try {
      if(isReloadFunction())
        reloadButtonAction();
    }catch (Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  };
  
  public DBTableEditor(Class objectClass, Class objectEditorClass, MappingObject.Type type) {
    this(objectClass, objectEditorClass, "", null, type);
  }
  
  public DBTableEditor(Class objectClass, Class objectEditorClass) {
    this(objectClass, objectEditorClass, "");
  }
  
  public DBTableEditor(Class objectClass, Class objectEditorClass, String title) {
    this(objectClass, objectEditorClass, title, null, null);
  }
  
  public DBTableEditor(Class objectClass, Class objectEditorClass, String title, MappingObject.Type type) {
    this(objectClass, objectEditorClass, title, null, type);
  }
  
  public DBTableEditor(Class objectClass, Class objectEditorClass, String title, ImageIcon icon) {
    this(objectClass, objectEditorClass, title, icon, null);
  }

  /**
   * Конструктор
   * @param tableColumns названия колонок таблицы (первая должна быть всегда id)
   * @param objectClass класс объектов, которые будут отображаться в таблице
   * @param objectEditorClass класс редактора объекта
   * @param title заголовок окна таблицы
   * @param icon иконка окна таблицы
   * @param type тип отображаемых объектов CURRENT, PROJECT или ARCHIVE
   */
  public DBTableEditor(Class objectClass, Class objectEditorClass, String title, ImageIcon icon, MappingObject.Type type) {
    super(title, icon);
    this.objectClass = objectClass;
    this.objectEditorClass = objectEditorClass;
    initDBEditorComponents(type);
    initDBEditorEvents();
    initTargets();
  }
  
  /**
   * Возвращает класс отображаемых объектов
   * @return класс отображаемых объектов
   */
  public Class getObjectClass() {
    return objectClass;
  }

  @Override
  public void setActive(boolean active) {
    super.setActive(active);
    setComponentEnable(getToolBar(), active);
    setComponentEnable(getPopMenu(), active);
    if(!isActive() && initDataTask != null) {
      initDataTask.shutDown();
      initDataTask = null;
    }
  }

  public String[] getSortFields() {
    return sortFields;
  }
  
  public void setSortFields(String... sortFields) {
    this.sortFields = sortFields;
  }

  public RemoveAction getRemoveActionType() {
    return removeActionType;
  }

  public void setRemoveActionType(RemoveAction removeActionType) {
    this.removeActionType = removeActionType;
  }
  /**
   * Возвращает количество выделенных объектов
   * @return количество выделенных объектов
   */
  public abstract int getSelectedObjectsCount();

  public String[] getDataFields() {
    return dataFields;
  }
  
  public void setDataFields(String[] dataFields) {
    this.dataFields = dataFields;
  }

  public void postAddButton(MappingObject object) {
    getGUI().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    try {
      MainObjectEditor editor = (MainObjectEditor)getObjectEditorClass().newInstance();
      if(editor != null) {
        editor.setAutoLoad(true);
        editor.setAutoStore(true);
        editor.setEditorObject(object);
        fireOpenObjectEditor(editor);
        
        Container parent = getParentWindow(getGUI());
        if(parent instanceof JInternalFrame) {
          EditorController.getDeskTop().add(editor);
          if(editor.getInternalDialog() != null)
            editor.getInternalDialog().setVisible(true);
        }else if(parent instanceof JDialog || parent instanceof JFrame || parent == null)
          editor.createDialog(this, true).setVisible(true);
        
        /*if(getInternalDialog() != null) {
          EditorController.getDeskTop().add(editor);
          if(editor.getInternalDialog() != null)
            editor.getInternalDialog().setVisible(true);
        }else editor.createDialog(this, true).setVisible(true);*/
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    finally {
      getGUI().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
  }

  public void postEditButton(MappingObject object) {
    try {
      MainObjectEditor editor = (MainObjectEditor)getObjectEditorClass().newInstance();
      editor.setAutoLoad(true);
      editor.setAutoStore(true);
      editor.setEditorObject(object);
      if(editor != null) {
        fireOpenObjectEditor(editor);
        
        Container parent = getParentWindow(getGUI());
        if(parent instanceof JInternalFrame) {
          EditorController.getDeskTop().add(editor);
          if(editor.getInternalDialog() != null)
            editor.getInternalDialog().setVisible(true);
        }else if(parent instanceof JDialog || parent instanceof JFrame)
          editor.createDialog(this, true).setVisible(true);
        
        /*if(getInternalDialog() != null) {
          EditorController.getDeskTop().add(editor);
          if(editor.getInternalDialog() != null)
            editor.getInternalDialog().setVisible(true);
        }else editor.createDialog(this, true).setVisible(true);*/
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  @Override
  public String getName() {
    if(super.getName() == null || super.getName().equals("")) {
      if(getClass().getSimpleName().equals(""))
        setName(getClass().getSuperclass().getSimpleName()+"_"+getObjectClass().getSimpleName());
      else
        setName(getClass().getSimpleName()+"_"+getObjectClass().getSimpleName());
    }
    return super.getName();
  }
  
  /**
   * Возвращает класс редактора отображаемых объектов
   * @return класс редактора отображаемых объектов
   */
  public Class getObjectEditorClass() {
    return objectEditorClass;
  }
  /**
   * Возвращает... долго писать))
   * @return true - выбор объекта по двойному клику
   */
  public boolean isDoubleClickSelectable() {
    return doubleClickSelectable;
  }
  /**
   * Устанавливает... долго писать))
   * @param doubleClickSelectable true - выбор объекта по двойному клику
   */
  public void setDoubleClickSelectable(boolean doubleClickSelectable) {
    this.doubleClickSelectable = doubleClickSelectable;
  }
  /**
   * Возращает кнопу "Добавить"
   * @return
   */
  public DivisionToolButton getAddButton() {
    return addToolButton;
  }

  public DivisionToolButton getEditButton() {
    return editToolButton;
  }

  public DivisionToolButton getRemoveButton() {
    return removeToolButton;
  }
  
  public boolean isAdministration() {
    return this.admin;
  }
  
  public void setAdministration(boolean admin) {
    this.admin = admin;
    setAddFunction(admin);
    setEditFunction(admin);
    setRemoveFunction(admin);
    setConfFunction(admin);
    setExportFunction(admin);
    setImportFunction(admin);
    setReloadFunction(admin);
    getToolBar().setVisible(admin);
    setPopMenuActive(admin);
  }

  public void setDefaultToolBar() {
    getToolBar().removeAll();
    getToolBar().add(addToolButton);
    getToolBar().add(editToolButton);
    getToolBar().add(removeToolButton);
    getToolBar().addSeparator();
    getToolBar().add(confToolButton);
    getToolBar().addSeparator();
    getToolBar().add(exportToolButton);
    getToolBar().add(importToolButton);
    getToolBar().addSeparator();
    getToolBar().add(reloadToolButton);
    getToolBar().add(toCurrentlButton);
  }

  public void setDefaultPopMenu() {
    popMenu.removeAll();
    popMenu.add(addPopMenuItem);
    popMenu.add(editPopMenuItem);
    popMenu.add(removePopMenuItem);
    popMenu.add(toCurrentMenuItem);
    popMenu.add(exportPopMenuItem);
    popMenu.add(importPopMenuItem);
    popMenu.add(unSelectMenuItem);
    scriptMenu.setEnabled(false);
    popMenu.add(scriptMenu);
  }

  public JPopupMenu getPopMenu() {
    return this.popMenu;
  }

  public void setPopMenuActive(boolean popActive) {
    this.popMenuActive = popActive;
  }

  public boolean isPopMenuActive() {
    return this.popMenuActive;
  }

  public void setAddFunction(boolean isAdd) {
    addToolButton.setVisible(isAdd);
    addPopMenuItem.setVisible(isAdd);
  }
  
  public void setConfFunction(boolean isAdd) {
    confToolButton.setVisible(isAdd);
  }

  public boolean isAddFunction() {
    return addToolButton.isVisible();
  }

  public void setEditFunction(boolean isEdit) {
    editToolButton.setVisible(isEdit);
    editPopMenuItem.setVisible(isEdit);
  }

  public boolean isEditFunction() {
    return editToolButton.isVisible();
  }

  public void setRemoveFunction(boolean isRemove) {
    removeToolButton.setVisible(isRemove);
    removePopMenuItem.setVisible(isRemove);
  }

  public boolean isRemoveFunction() {
    return removeToolButton.isVisible();
  }
  
  public boolean isConfFunction() {
    return confToolButton.isVisible();
  }

  public void setImportFunction(boolean isImport) {
    importToolButton.setVisible(isImport);
    importPopMenuItem.setVisible(isImport);
  }

  public boolean isImportFunction() {
    return importToolButton.isVisible();
  }

  public void setExportFunction(boolean isExport) {
    //getToolBar().getComponentAtIndex(getToolBar().getComponentIndex(exportToolButton)-1).setVisible(isExport);
    exportToolButton.setVisible(isExport);
    exportPopMenuItem.setVisible(isExport);
  }

  public boolean isExportFunction() {
    return exportToolButton.isVisible();
  }

  public void setReloadFunction(boolean isReload) {
    reloadToolButton.setVisible(isReload);
  }

  public boolean isReloadFunction() {
    return reloadToolButton.isVisible();
  }
  
  public JProgressBar getProgressBar() {
    return this.progressBar;
  }
  
  public DivisionScrollPane getScroll() {
    return this.scroll;
  }

  public DBFilter getRootFilter() {
    return rootFilter;
  }

  public DBFilter getTypeFilter() {
    return typeFilter;
  }
  
  public DBFilter getClientFilter() {
    return clientFilter;
  }

  public DBFilter getTmpFilter() {
    return tmpFilter;
  }

  public DBFilter getTableFilter() {
    return tableFilter;
  }
  
  /*public boolean isFilteredObject(MappingObject object) {
    if(rootFilter != null)
      return ObjectLoader.isSatisfy(rootFilter, object);
    return true;
  }*/

  public Integer[] isFilteredObject(Integer[] ids) {
    if(rootFilter != null)
      return ObjectLoader.isSatisfy(rootFilter, ids);
    return new Integer[0];
  }

  public MappingObject.Type getType() {
    return type;
  }

  public void setType(MappingObject.Type t) {
    type = t == null?MappingObject.Type.CURRENT:t;
    toCurrentMenuItem.setVisible(this.type != null && this.type == MappingObject.Type.ARCHIVE);
    toCurrentlButton.setVisible(this.type != null && this.type == MappingObject.Type.ARCHIVE);
    try {
      if(type != null) {
        typeFilter.clear();
        typeFilter.AND_EQUAL("type", type);
        if(type == MappingObject.Type.ARCHIVE)
          setAddFunction(false);
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  private void initDBEditorComponents(MappingObject.Type type) {
    this.setDefaultToolBar();    
    this.setDefaultPopMenu();
    
    addToolButton.setToolTipText("Добавить");
    editToolButton.setToolTipText("Редактировать");
    removeToolButton.setToolTipText("Удалить");
    
    progressBar.setStringPainted(true);
    progressBar.setBorderPainted(false);
    progressBar.setIndeterminate(false);
    
    getRootPanel().add(scroll, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

    getStatusBar().addStatus(countLabel);
    getStatusBar().addStatus(progressBar);
    
    setEditFunction(objectEditorClass != null);
    setAddFunction(objectEditorClass != null);
    setExportFunction(true);
    setImportFunction(false);
    rootFilter   = DBFilter.create(getObjectClass());
    tmpFilter    = rootFilter.AND_EQUAL("tmp", false);
    tableFilter  = rootFilter.AND_FILTER();
    clientFilter = tableFilter.AND_FILTER();
    typeFilter   = tableFilter.AND_FILTER();
    setType(type);
  }

  public void setCountLabel() {
    countLabel.setText("объектов: "+getObjectsCount());
  }
  
  @Override
  public void setEditable(boolean editable) {
    super.setEditable(editable);
    addToolButton.setVisible(editable);
    editToolButton.setVisible(editable);
    removeToolButton.setVisible(editable);
  }
  
  @Override
  public void setEnabled(boolean enable) {
    super.setEnabled(enable);
    addToolButton.setEnabled(enable);
    editToolButton.setEnabled(enable);
    removeToolButton.setEnabled(enable);
    checkMenu();
  }
  
  protected abstract int insertObject(MappingObject object,int row) throws RemoteException;
  protected abstract void insertData(List<List> data);
  public abstract void setDefaultSelected();
  protected abstract void eventUpdate(Integer[] ids);
  
  @Override
  public void initTargets() {
    addTarget(new DivisionTarget(getObjectClass()) {
      @Override
      public void messageReceived(final String type, final Integer[] ids, PropertyMap objectEventProperty) {
        SwingUtilities.invokeLater(() -> {
          switch(type) {
            case "UPDATE": eventUpdate(ids);break;
            case "CREATE": eventCreate(ids);break;
            case "REMOVE": eventRemove(ids);break;
          }
        });
      }
    });
  }

  private void initDBEditorEvents() {
    toCurrentlButton.addActionListener(e -> archiveToCurrent());
    toCurrentMenuItem.addActionListener(e -> archiveToCurrent());

    addToolButton.addActionListener(addAction);
    addPopMenuItem.addActionListener(addAction);

    editToolButton.addActionListener(editAction);
    editPopMenuItem.addActionListener(editAction);

    removeToolButton.addActionListener(removeAction);
    removePopMenuItem.addActionListener(removeAction);
    
    confToolButton.addActionListener(confAction);
    
    exportToolButton.addActionListener(exportAction);
    exportPopMenuItem.addActionListener(exportAction);

    importToolButton.addActionListener(importAction);
    importPopMenuItem.addActionListener(importAction);

    reloadToolButton.addActionListener(reloadAction);
    
    setConfFunction(false);
  }

  public void archiveToCurrent() {
    try {
      ObjectLoader.createSession(true).toTypeObjects(getObjectClass(), getSelectedId(), MappingObject.Type.CURRENT);
    }catch(RemoteException ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  protected File getExportFile() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Укажите файл экспорта");
    fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
    fileChooser.setApproveButtonText("экспорт");
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setMultiSelectionEnabled(false);
    fileChooser.showOpenDialog(null);
    return fileChooser.getSelectedFile();
  }

  public void exportButtonAction() {
    if(isEnabled() && (isAdministration() || isExportFunction()))
      ExportImportUtil.export(getObjectClass(), getSelectedId());
  }

  public void importButtonAction() {
  }

  private void reloadButtonAction() throws Exception {
    if(isEnabled() && (isAdministration() || isReloadFunction()))
      initData();
  }

  public void setSelectedObjects(MappingObject[] objects) {
    try {
      Integer[] ids = new Integer[0];
      for(MappingObject object:objects)
        ids= (Integer[]) ArrayUtils.add(ids, object.getId());
      setSelectedObjects(ids);
    }catch(RemoteException ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  //public abstract void setSelectedObjects(MappingObject[] objects, boolean active);

  public void setSelectedObjects(Integer... ids) {
    clearSelection();
    addSelectedObjects(ids);
  }
  //public abstract void setSelectedObjects(Integer[] ids, boolean active);

  public abstract void addSelectedObjects(Integer[] ids);
  //public abstract void addSelectedObjects(Integer[] ids, boolean active);
  
  @Override
  public Boolean okButtonAction() {
    fireSelectObjects(this, selectedId);
    dispose();
    return true;
  }
  
  public abstract void clearSelection();

  @Override
  public void closeDialog() {
    clearSelection();
    super.closeDialog();
  }

  public Integer[] get() {
    createDialog(true).setVisible(true);
    return getSelectedId();
  }

  public MappingObject[] getObjects() {
    Integer[] ids = get();
    if(ids.length > 0)
      return ObjectLoader.getObjects(getObjectClass(), ids);
    return new MappingObject[0];
  }

  public MappingObject getObject() {
    setSingleSelection(true);
    Integer[] objects = get();
    if(objects != null && objects.length > 0)
      return ObjectLoader.getObject(getObjectClass(), objects[0]);
    return null;
  }
  
  /**
   * Возвращает идентификаторы выделенных объектов
   * @return Integer[] массив идентификаторов
   */
  public abstract Integer[] getSelectedId();
  /**
   * Возвращает выделенные объектов
   * @return MappingObject[] массив объектов
   */
  public abstract MappingObject[] getSelectedObjects();
  
  public MappingObject getFirstSelectedObject() {
    Integer[] ids = getSelectedId();
    return ids.length>0?ObjectLoader.getObject(getObjectClass(), ids[0]):null;
  }
  
  /**
   * Устанавливает режим выделения
   * @param singleSelection true - одиночное выделение, false - множественное
   */
  public abstract void setSingleSelection(boolean singleSelection);
  /**
   * Возвращает режим выделения
   * @return true - одиночное выделение, false - множественное
   */
  public abstract boolean isSingleSelection();

  public abstract int getObjectsCount();
  
  protected abstract void createObject(MappingObject object) throws RemoteException;
  
  protected void eventCreate(Integer[] ids) {
    try {
      ids = isFilteredObject(ids);
      if(ids.length > 0)
        insertData(ObjectLoader.getData(getObjectClass(), ids, dataFields));
    }catch(Exception ex){Messanger.showErrorMessage(ex);}
  }
  
  protected void eventRemove(Integer[] ids) {
    removeObjects(ids);
  }
  /**
   * Удаляет объекты из таблицы отображения
   * @param ids массив идентификаторов
   */
  public abstract void removeObjects(Integer[] ids);
  /**
   * Удаляет объект из таблицы отображения
   * @param object объект
   * @throws RemoteException
   */
  
  @Override
  public void initData() {
    if(initDataTask != null) {
      initDataTask.shutDown();
      initDataTask = null;
    }
    if(isActive())
      pool.submit(initDataTask = new InitDataTask());
  }

  
  class InitDataTask implements Runnable {
    private boolean run = true;

    public boolean isRun() {
      return run;
    }

    public void shutDown() {
      defaultCursor();
      run = false;
    }
    
    @Override
    public void run() {
      SwingUtilities.invokeLater(() -> {
        waitCursor();
        clear();
      });
      if(!isRun())
        return;
      try {
        if(getObjectClass().getSimpleName().equals("CreatedDocument")) {
          System.out.println("sdfs");
        }
        final List<List> data = ObjectLoader.getData(getRootFilter(), getDataFields(), getSortFields());
        if(!isRun())
          return;
        SwingUtilities.invokeLater(() -> {
          try {
            if(!data.isEmpty())
              insertData(data);
            fireInitDataComplited(DBTableEditor.this);
            defaultCursor();
          }catch(Exception e) {
            System.out.println("######################### "+getObjectClass()+" #######################################");
            e.printStackTrace();
          }
        });
      } catch (Exception ex) {
        System.out.println("######################### "+getObjectClass()+" #######################################");
        MsgTrash.out(ex);
      }
    }
  }

  protected Hashtable<Class,Integer> getDropSupportedClasses() {
   return null; 
  }
  
  protected abstract boolean dragOver(Point point, List objects, Class interfaceClass);
  protected abstract void drop(Point point, List objects, Class interfaceClass);
    
  ///////////////DRAG SOURCE//////////////////////////
  @Override
  public void dragGestureRecognized(DragGestureEvent dge) {
    if(dge.getTriggerEvent().getModifiers() != InputEvent.META_MASK) {
        Integer[] objectsId = getSelectedId();
        if(objectsId.length > 0) {
          ObjectTransferable transferable =
                new ObjectTransferable(objectsId,getObjectClass());
          dge.startDrag(null,transferable,this);
        }
    }
  }

  @Override
  public void dragEnter(DragSourceDragEvent dsde) {
  }

  @Override
  public void dragOver(DragSourceDragEvent dsde) {
  }

  @Override
  public void dropActionChanged(DragSourceDragEvent dsde) {
  }

  @Override
  public void dragExit(DragSourceEvent dse) {
  }

  @Override
  public void dragDropEnd(DragSourceDropEvent dsde) {
  }
  
  ///////////////DROP TARGET//////////////////////////
  @Override
  public void dragEnter(DropTargetDragEvent dtde) {
  }

  @Override
  public void dragOver(DropTargetDragEvent dtde) {
    try {
      Transferable transferable = dtde.getTransferable();
      Hashtable<Class,Integer> hash = getDropSupportedClasses();
      if(hash != null) {
        for(Class clazz:hash.keySet()) {
          String objectMimeType = DataFlavor.javaRemoteObjectMimeType + ";class="+clazz.getName();
          DataFlavor objectFlavor = new DataFlavor(objectMimeType);
          if(transferable.isDataFlavorSupported(objectFlavor)) {
            if(hash.get(clazz) != null) {
              String interfaceMimeType = DataFlavor.javaSerializedObjectMimeType + ";class="+clazz.getName();
              if(dragOver(
                      dtde.getLocation(),
                      (List)transferable.getTransferData(objectFlavor),
                      (Class)transferable.getTransferData(new DataFlavor(interfaceMimeType)))) {
                dtde.acceptDrag(hash.get(clazz));
                return;
              }
            }
          }
        }
      }
      dtde.rejectDrag();
    }catch(ClassNotFoundException | UnsupportedFlavorException | IOException ex){Messanger.showErrorMessage(ex);}
  }

  @Override
  public void dropActionChanged(DropTargetDragEvent dtde) {
  }

  @Override
  public void dragExit(DropTargetEvent dte) {
    scroll.getViewport().repaint();
  }

  @Override
  public void drop(DropTargetDropEvent dtde) {
    Transferable transferable = dtde.getTransferable();
    try {
      Class interfaceClass = (Class)transferable.getTransferData(transferable.getTransferDataFlavors()[1]);
      List objectsList = (List)transferable.getTransferData(transferable.getTransferDataFlavors()[0]);
      drop(dtde.getLocation(), objectsList, interfaceClass);
    }catch(Exception ex){Messanger.showErrorMessage(ex);}
  }

  public ActionListener getReloadAction() {
    return reloadAction;
  }

  public ActionListener getAddAction() {
    return addAction;
  }

  public ActionListener getEditAction() {
    return editAction;
  }

  public ActionListener getRemoveAction() {
    return removeAction;
  }
  
  public ActionListener getConfAction() {
    return confAction;
  }

  public void setReloadAction(ActionListener reloadAction) {
    reloadToolButton.removeActionListener(this.reloadAction);
    this.reloadAction = reloadAction;
    reloadToolButton.addActionListener(this.reloadAction);
  }
  /**
   * Задаёт не действие на кнопку "добавить"
   * @param addAction действие
   */
  public void setAddAction(ActionListener addAction) {
    addToolButton.removeActionListener(this.addAction);
    addPopMenuItem.removeActionListener(this.addAction);
    this.addAction = addAction;
    addToolButton.addActionListener(this.addAction);
    addPopMenuItem.addActionListener(this.addAction);
  }
  /**
   * Задаёт не действие на кнопку "редактировать"
   * @param editAction
   */
  public void setEditAction(ActionListener editAction) {
    editToolButton.removeActionListener(this.editAction);
    editPopMenuItem.removeActionListener(this.editAction);
    this.editAction = editAction;
    editToolButton.addActionListener(this.editAction);
    editPopMenuItem.addActionListener(this.editAction);
  }
  /**
   * Задаёт не действие на кнопку "удалить"
   * @param removeAction
   */
  public void setRemoveAction(ActionListener removeAction) {
    removeToolButton.removeActionListener(this.removeAction);
    removePopMenuItem.removeActionListener(this.removeAction);
    this.removeAction = removeAction;
    removeToolButton.addActionListener(this.removeAction);
    removePopMenuItem.addActionListener(this.removeAction);
  }
  
  public void setConfAction(ActionListener confAction) {
    confToolButton.removeActionListener(this.confAction);
    confToolButton.removeActionListener(this.confAction);
    this.confAction = confAction;
    confToolButton.addActionListener(this.confAction);
    confToolButton.addActionListener(this.confAction);
  }
  /**
   * Задаёт не действие на кнопку "экспортировать"
   * @param exportAction
   */
  public void setExportAction(ActionListener exportAction) {
    exportToolButton.removeActionListener(this.exportAction);
    exportPopMenuItem.removeActionListener(this.exportAction);
    this.exportAction = exportAction;
    exportToolButton.addActionListener(this.exportAction);
    exportPopMenuItem.addActionListener(this.exportAction);
  }
  /**
   * Задаёт не действие на кнопку "импортироапть"
   * @param importAction
   */
  public void setImportAction(ActionListener importAction) {
    importToolButton.removeActionListener(this.importAction);
    importPopMenuItem.removeActionListener(this.importAction);
    this.importAction = importAction;
    importToolButton.addActionListener(this.importAction);
    importPopMenuItem.addActionListener(this.importAction);
  }

  /**
   * Метод по умолчанию на действие "добавить".
   * Чтобы заменить на свой метод воспользуйтесь
   * методом setAddAction
   */
  public abstract void addButtonAction();
  /**
   * Метод по умолчанию на действие "редактировать".
   * Чтобы заменить на свой метод воспользуйтесь
   * методом setEditAction
   */
  public void editButtonAction() {
    if(isEditable() && (isAdministration() || isEditFunction())) {
      waitCursor();
      MappingObject object = this.getFirstSelectedObject();
      if(object != null)
        postEditButton(object);
      defaultCursor();
    }
  }
  /**
   * Метод по умолчанию на действие "Удалить".
   * Чтобы заменить на свой метод воспользуйтесь
   * методом setRemoveAction
   */
  public boolean removeButtonAction() {
    boolean returnValue = false;
    if(isEditable() && (isAdministration() || isRemoveFunction())) {
      waitCursor();
      try {
        Integer[] ids = this.getSelectedId();
        if(ids.length > 0) {
          switch(getRemoveActionType()) {
            case DELETE:
              if(JOptionPane.showConfirmDialog(
                getRootPanel(),
                "<html>Вы уверены в том что хотите <b>удалить</b> выделенны"+(ids.length>1?"е":"й")+" объект"+(ids.length>1?"ы":"")+"?</html>",
                "Подтверждение удаления",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == 0) {
                ObjectLoader.createSession(true).removeObjects(getObjectClass(), ids);
                returnValue = true;
              }else returnValue = false;
              break;
            case MOVE_TO_ARCHIVE:
              if(JOptionPane.showConfirmDialog(
                getRootPanel(),
                "Вы действительно хотите отправить в архив выделенны"+(ids.length>1?"е":"й")+" объект"+(ids.length>1?"ы":"")+"?",
                "Отправка в архив",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == 0) {
                ObjectLoader.createSession(true).toTypeObjects(getObjectClass(), ids, MappingObject.Type.ARCHIVE);
                returnValue = true;
              }else returnValue = false;
              break;
            case MARK_FOR_DELETE:
              if(JOptionPane.showConfirmDialog(
                getRootPanel(),
                "Вы уверены в том что хотите удалить выделенны"+(ids.length>1?"е":"й")+" объект"+(ids.length>1?"ы":"")+"?",
                "Подтверждение удаления",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == 0) {
                ObjectLoader.createSession(true).toTmpObjects(getObjectClass(), ids, true);
                returnValue = true;
              }else returnValue = false;
              break;
          }
        }
      }catch(HeadlessException | RemoteException ex) {
        Messanger.showErrorMessage(ex);
      }finally {
        defaultCursor();
        return returnValue;
      }
    }
    return returnValue;
  }
  
  public void confButtonAction() {
  }
}