package bum.editors;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.ImageIcon;
import javax.swing.JTabbedPane;
import mapping.MappingObject;
import mapping.MappingObject.Type;

public class TypeTableEditor extends EditorGui {
  private JTabbedPane tabb = new JTabbedPane();
  private MappingObject.Type currentType;
  private DBTableEditor currentTableEditor;
  private DBTableEditor archiveTableEditor;

  public TypeTableEditor(String[] tableColumns, String[] dataFields, Class objectClass, Class objectEditorClass, String title, ImageIcon icon, Type type) throws InstantiationException, IllegalAccessException {
    this(tableColumns, dataFields, objectClass, objectEditorClass, title, icon, type, false, false, false);
  }

  public TypeTableEditor(String[] tableColumns, String[] dataFields, Class objectClass, Class objectEditorClass, String title, ImageIcon icon, Type type, boolean currentMarkerVisible, boolean projectMarkerVisible, boolean archiveMarkerVisible) throws InstantiationException, IllegalAccessException {
    super(title, icon);
    currentTableEditor = new TableEditor(tableColumns, dataFields, objectClass, objectEditorClass, title, icon, MappingObject.Type.CURRENT, currentMarkerVisible);
    archiveTableEditor = new TableEditor(tableColumns, dataFields, objectClass, objectEditorClass, title, icon, MappingObject.Type.ARCHIVE, archiveMarkerVisible);
    initTargets();
    initTypeTable(type);

    currentTableEditor.setName(getName()+"_currentTableEditor_"+currentTableEditor.getName());
    archiveTableEditor.setName(getName()+"_archiveTableEditor"+archiveTableEditor.getName());
    addSubEditorToStore(currentTableEditor);
    addSubEditorToStore(archiveTableEditor);
  }

  public TypeTableEditor(Class<? extends DBTableEditor> editorClass, Type type) throws InstantiationException, IllegalAccessException {
    super("", null);
    currentTableEditor = editorClass.newInstance();
    archiveTableEditor = editorClass.newInstance();
    currentTableEditor.setType(MappingObject.Type.CURRENT);
    archiveTableEditor.setType(MappingObject.Type.ARCHIVE);
    initTargets();
    initTypeTable(type);

    currentTableEditor.setName(getName()+"_currentTableEditor");
    archiveTableEditor.setName(getName()+"_archiveTableEditor");
    addSubEditorToStore(currentTableEditor);
    addSubEditorToStore(archiveTableEditor);
  }

  public TypeTableEditor(String[] tableColumns, String[] dataFields, Class objectClass, Class objectEditorClass, String title, Type type, boolean currentMarkerVisible, boolean projectMarkerVisible, boolean archiveMarkerVisible) throws InstantiationException, IllegalAccessException {
    this(tableColumns, dataFields, objectClass, objectEditorClass, title, null, type, currentMarkerVisible, projectMarkerVisible, archiveMarkerVisible);
  }
  
  public TypeTableEditor(String[] tableColumns, String[] dataFields, Class objectClass, Class objectEditorClass, String title, Type type) throws InstantiationException, IllegalAccessException {
    this(tableColumns, dataFields, objectClass, objectEditorClass, title, null, type);
  }

  public TypeTableEditor(String[] tableColumns, String[] dataFields, Class objectClass, Class objectEditorClass, String title) throws InstantiationException, IllegalAccessException {
    this(tableColumns, dataFields, objectClass, objectEditorClass, title, null, MappingObject.Type.CURRENT);
  }

  public TypeTableEditor(String[] tableColumns, String[] dataFields, Class objectClass, Class objectEditorClass) throws InstantiationException, IllegalAccessException {
    this(tableColumns, dataFields, objectClass, objectEditorClass, "");
  }

  public TypeTableEditor(String[] tableColumns, String[] dataFields, Class objectClass, Class objectEditorClass, Type type) throws InstantiationException, IllegalAccessException {
    this(tableColumns, dataFields, objectClass, objectEditorClass, "", type);
  }

  public TypeTableEditor(Class<? extends DBTableEditor> editorClass) throws InstantiationException, IllegalAccessException {
    this(editorClass, MappingObject.Type.CURRENT);
  }

  @Override
  public void setName(String name) {
    super.setName(name);
    currentTableEditor.setName(getName()+"_currentTableEditor");
    archiveTableEditor.setName(getName()+"_archiveTableEditor");
  }

  @Override
  public String getName() {
    if(super.getName() == null || super.getName().equals(""))
      setName(getClass().getSimpleName()+"_"+getCurrentTableEditor().getObjectClass().getSimpleName());
    return super.getName();
  }

  public DBTableEditor getArchiveTableEditor() {
    return archiveTableEditor;
  }

  public DBTableEditor getCurrentTableEditor() {
    return currentTableEditor;
  }

  private void initTypeTable(MappingObject.Type type) {
    setRemoveAction(MappingObject.Type.CURRENT, MappingObject.RemoveAction.MOVE_TO_ARCHIVE);
    setRemoveAction(MappingObject.Type.ARCHIVE, MappingObject.RemoveAction.MARK_FOR_DELETE);

    initComponents();
    this.currentType = type;
    switch(type) {
      case CURRENT:
        tabb.setSelectedIndex(0);
        break;
      case ARCHIVE:
        tabb.setSelectedIndex(1);
        break;
    }
    initEvents();
  }

  public void setDoubleClickSelectable(boolean is) {
    currentTableEditor.setDoubleClickSelectable(is);
    archiveTableEditor.setDoubleClickSelectable(is);
  }

  private void initComponents() {
    getRootPanel().setLayout(new GridBagLayout());
    getRootPanel().add(tabb,     new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    
    tabb.add(currentTableEditor.getGUI(), "Актуальные");
    tabb.add(archiveTableEditor.getGUI(), "Архив");
    
    currentTableEditor.setVisibleOkButton(false);
    archiveTableEditor.setVisibleOkButton(false);
  }

  private void initEvents() {
    tabb.addChangeListener(e -> changeType());
  }

  @Override
  public void addEditorListener(EditorListener liistener) {
    currentTableEditor.addEditorListener(liistener);
    archiveTableEditor.addEditorListener(liistener);
  }

  @Override
  public void removeEditorListener(EditorListener liistener) {
    currentTableEditor.removeEditorListener(liistener);
    archiveTableEditor.removeEditorListener(liistener);
  }

  @Override
  public void openObjectEditor(EditorGui editor) {
    fireOpenObjectEditor(editor);
  }
  
  @Override
  public Boolean okButtonAction() {
    fireChangeSelection(getCurrentEditor(), getCurrentEditor().getSelectedId());
    dispose();
    return true;
  }

  public Integer[] getSelectedId() {
   return getCurrentEditor().getSelectedId();
  }
  
  public int getSelectedObjectsCount() {
    return getCurrentEditor().getSelectedObjectsCount();
  }

  public MappingObject[] getSelectedObjects() {
    return getCurrentEditor().getSelectedObjects();
  }

  public DBTableEditor getCurrentEditor() {
    DBTableEditor editor = null;
    switch(currentType) {
      case CURRENT:
        editor = currentTableEditor;
        break;
      case ARCHIVE:
        editor = archiveTableEditor;
        break;
    }
    return editor;
  }

  private void changeType() {
    currentTableEditor.setActive(false);
    archiveTableEditor.setActive(false);
    switch(tabb.getSelectedIndex()) {
      case 0:
        currentType = MappingObject.Type.CURRENT;
        currentTableEditor.setActive(true);
        currentTableEditor.initData();
        break;
      case 1:
        currentType = MappingObject.Type.ARCHIVE;
        archiveTableEditor.setActive(true);
        archiveTableEditor.initData();
        break;
    }
  }

  @Override
  public void initData() {
    initData(getCurrentEditor().getType());
  }
  
  public void initData(MappingObject.Type type) {
    switch(type) {
      case CURRENT:
        currentTableEditor.initData();
        break;
      case ARCHIVE:
        archiveTableEditor.initData();
        break;
    }
  }

  public void initDataAll() {
    initData(MappingObject.Type.CURRENT);
    initData(MappingObject.Type.ARCHIVE);
  }
  
  public void setActive(boolean active, MappingObject.Type type) {
    switch(type) {
      case CURRENT:
        currentTableEditor.setActive(active);
        break;
      case ARCHIVE:
        archiveTableEditor.setActive(active);
        break;
    }
  }

  @Override
  public void setActive(boolean active) {
    if(active)
      getCurrentEditor().setActive(active);
    else {
      getCurrentTableEditor().setActive(active);
      getArchiveTableEditor().setActive(active);
    }
  }

  public void setRemoveAction(MappingObject.RemoveAction action) {
    currentTableEditor.setRemoveActionType(action);
    archiveTableEditor.setRemoveActionType(action);
  }

  public void setRemoveAction(MappingObject.Type type, MappingObject.RemoveAction action) {
    switch(type) {
      case CURRENT:currentTableEditor.setRemoveActionType(action);
      case ARCHIVE:archiveTableEditor.setRemoveActionType(action);
    }
  }

  @Override
  public void dispose() {
    currentTableEditor.dispose();
    archiveTableEditor.dispose();
    super.dispose();
  }
  
  public void setSortFields(String[] fields) {
    currentTableEditor.setSortFields(fields);
    archiveTableEditor.setSortFields(fields);
  }

  @Override
  public void clear() {
    currentTableEditor.clear();
    archiveTableEditor.clear();
  }

  @Override
  public void clearTargets() {
    currentTableEditor.clearTargets();
    archiveTableEditor.clearTargets();
  }

  @Override
  public void initTargets() {
    currentTableEditor.initTargets();
    archiveTableEditor.initTargets();
  }
}