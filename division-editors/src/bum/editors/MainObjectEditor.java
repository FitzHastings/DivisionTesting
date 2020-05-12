package bum.editors;

import bum.editors.util.DivisionTarget;
import bum.editors.util.ObjectLoader;
import division.exportimport.ExportImportUtil;
import division.fx.PropertyMap;
import division.swing.LocalProcessing;
import division.swing.guimessanger.Messanger;
import division.swing.DivisionToolButton;
import division.util.FileLoader;
import documents.FOP;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.*;
import mapping.MappingObject;

public abstract class MainObjectEditor extends Editor {
  private MappingObject editorObject;
  private String md5Memento = null;
  private final DivisionToolButton printButton    = new DivisionToolButton(FileLoader.getIcon("print_preview24.gif"),"Печать");
  private final DivisionToolButton exportButton   = new DivisionToolButton(FileLoader.getIcon("Export24.gif"),"Экспорт объекта");
  private final ArrayList<MainObjectEditorListener> listeners = new ArrayList<>();
  protected XMLTemplateTableEditor templateTableEditor;
  
  public MainObjectEditor() {
    super();
    initEvents();
  }
  
  public MappingObject getEditorObject() {
    return editorObject;
  }

  @Override
  public void checkMenu() {
  }

  public DivisionToolButton getExportButton() {
    return exportButton;
  }

  public DivisionToolButton getPrintButton() {
    return printButton;
  }

  public XMLTemplateTableEditor getTemplateTableEditor() {
    return templateTableEditor;
  }

  private void initEvents() {
    printButton.addActionListener((ActionEvent e) -> {
      printButtonAction();
    });
    exportButton.addActionListener((ActionEvent e) -> {
      try {
        exportObject();
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    });
  }
  
  @Override
  public Boolean okButtonAction() {
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    Boolean saveDEBUG = saveAction();
    if(saveDEBUG == null)
      return false;
    else if(saveDEBUG) {
      fireSave();
      dispose();
    }
    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    return saveDEBUG;
  }

  @Override
  public void closeDialog() {
    if(getDialog() != null)
      getDialog().setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    if(getInternalDialog() != null)
      getInternalDialog().setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    if(getFrameDialog() != null)
      getFrameDialog().setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    try {
      commit();
      if(isUpdate()) {
        switch(JOptionPane.showConfirmDialog(this.getRootPanel(), "Сохранить именения?", "Закрытие", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)) {
          case 0:
            okButtonAction();
            break;
          case 1:
            if(((MappingObject)getEditorObject()).isTmp()) {
              ObjectLoader.createSession(true).removeObject(getEditorObject());
            }
            //getEditorObject().dispose();
            super.closeDialog();
            break;
          case 2:
            return;
        }
      }else {
        if(getEditorObject().isTmp())
          ObjectLoader.createSession(true).removeObject(getEditorObject());
        //getEditorObject().dispose();
        super.closeDialog();
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  public void exportObject() {
    try {
      ExportImportUtil.export(getEditorObject().getInterface(), getEditorObject().getId());
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  protected JPopupMenu createPrintMenu(List<List> data/*, boolean isTemplateTableEditor*/) throws Exception {
    JPopupMenu menu = new JPopupMenu();
    for(final List d:data) {
      JMenu     subMenu   = new JMenu(d.get(1).toString());
      JMenuItem print     = new JMenuItem("печать");
      JMenuItem preview   = new JMenuItem("предпросмотр");

      menu.add(subMenu);
      subMenu.add(print);
      subMenu.add(preview);

      print.addActionListener(new exportAction(d.get(2).toString(),null,false,true));
      preview.addActionListener(new exportAction(d.get(2).toString(),null,true,false));
      createEditTemplate(subMenu, (Integer)d.get(0), (Class)d.get(d.size()-1));
    }
    return menu;
  }

  protected void createEditTemplate(JMenu subMenu, final Integer id, final Class<? extends MappingObject> templateClass) {
    JMenuItem edit = new JMenuItem("редактировать");
    subMenu.add(edit);
    edit.addActionListener((ActionEvent e) -> {
      templateTableEditor.postAddButton(ObjectLoader.getObject(templateClass, id));
    });
  }
  
  protected void printButtonAction() {
    /*try {
      RMITable xmlTable = ObjectLoader.loadDBTable(XMLTemplate.class);
      Union xmlUnion = xmlTable.createFilter();
      xmlUnion.AND_EQUAL("objectClassName", getEditorObject().getRealClassName());
      Vector<Vector> data = xmlTable.getData(xmlUnion, new String[]{"id","name","xml"});
      for(Vector v:data)
        v.add(XMLTemplate.class);
      JPopupMenu menu = createPrintMenu(data);
      menu.show(printButton, 0, printButton.getHeight());
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }*/
  }

  class exportAction implements ActionListener {
    private String type;
    private boolean preview;
    private boolean print;
    private String xml;

    public exportAction(String xml, String type, boolean preview, boolean print) {
      this.type = type;
      this.preview = preview;
      this.print = print;
      this.xml = xml;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      final LocalProcessing processing = new LocalProcessing();
      processing.submit(() -> {
        try {
          Map<String,Object> variables = new TreeMap();
          variables.put("object", getEditorObject());
          if(preview)
            FOP.preview_from_XMLTemplate(xml, variables);
          else if(print)
            FOP.print_from_XMLTemplate(xml, variables);
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      });
    }
  }
  
  public Boolean saveAction() {
    try {
      String msg = commit();
      if(msg != null && !msg.equals("")) {
        JOptionPane.showMessageDialog(getGUI(), msg, "Ошибка", JOptionPane.ERROR_MESSAGE);
        return false;
      }else {
        //boolean isup = isUpdate();
        //boolean tmp = getEditorObject().isTmp();
        if(isUpdate() || getEditorObject().isTmp()) {
          Boolean is = save();
          if(is)
            commitUpdate();
          return is;
        }else return true;
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    return false;
  }
  
  public boolean save() throws RemoteException {
    getEditorObject().setTmp(false);
    return ObjectLoader.saveObject(getEditorObject());
  }

  public void commitUpdate() throws RemoteException {
    md5Memento = ObjectLoader.getMD5Hash(getEditorObject());
  }

  public boolean isUpdate() throws RemoteException {
    String currentMd5Memento = ObjectLoader.getMD5Hash(getEditorObject());
    System.out.println("OLD OBJECT = "+md5Memento);
    System.out.println("NEW OBJECT = "+currentMd5Memento);
    System.out.println("currentMd5Memento <> md5Memento = "+md5Memento.equals(currentMd5Memento));
    return !md5Memento.equals(currentMd5Memento);
  }
  
  public void setEditorObject(MappingObject editorObject) throws Exception {
    this.editorObject = editorObject;
    clear();
    if(editorObject != null) {
      md5Memento = null;
      md5Memento = ObjectLoader.getMD5Hash(getEditorObject().getInterface(), getEditorObject().getId());
      getGUI().setBorder(BorderFactory.createEmptyBorder());

      if(templateTableEditor == null)
        templateTableEditor = new XMLTemplateTableEditor(getEditorObject().getRealClassName());
      templateTableEditor.setAutoStore(true);
      templateTableEditor.setAutoLoad(true);

      String title = getEditorObject().getName();
      setTitle(title == null?getEmptyObjectTitle():title);
      initData();
      printButton.setEnabled(true);

      clearTargets();
      initTargets();
    }
  }

  @Override
  public void initTargets() {
    try {
      addTarget(new DivisionTarget(getEditorObject()) {
        @Override
        public void messageReceived(final String type, Integer[] ids, PropertyMap objectEventProperty) {
          SwingUtilities.invokeLater(() -> {
            switch(type) {
              case "UPDATE": eventUpdate();break;
              case "CREATE": eventCreate();break;
              case "REMOVE": eventRemove();break;
            }
          });
        }
      });
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  public abstract String commit() throws Exception;
  
  public void addMainObjectEditorListener(MainObjectEditorListener listener) {
    if(!this.listeners.contains(listener))
      this.listeners.add(listener);
  }
  
  public void removeMainObjectEditorListener(MainObjectEditorListener listener) {
    this.listeners.remove(listener);
  }
  
  protected void fireSave() {
    for(MainObjectEditorListener list:this.listeners)
      list.Save();
  }

  @Override
  public void dispose() {
    if(templateTableEditor != null)
      templateTableEditor.dispose();
    super.dispose();
  }
  
  protected void eventCreate() {
  }
  
  protected void eventUpdate() {
    try {
      if(getEditorObject() != null && !getEditorObject().isTmp())
        initData();
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  protected void eventRemove() {
    getGUI().setBorder(BorderFactory.createLineBorder(Color.red, 3));
  }

  @Override
  public String getName() {
    if(super.getName() == null || super.getName().equals(""))
      setName(getClass().getName());
    return super.getName();
  }
}