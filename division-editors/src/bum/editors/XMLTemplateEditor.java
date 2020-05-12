package bum.editors;

import bum.interfaces.XMLTemplate;
import division.swing.guimessanger.Messanger;
import division.swing.ScriptPanel;
import division.swing.DivisionTextField;
import division.swing.DivisionToolButton;
import division.util.FileLoader;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import mapping.MappingObject;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

public class XMLTemplateEditor extends MainObjectEditor {
  private DivisionToolButton save    = new DivisionToolButton(FileLoader.getIcon("Save16.gif"));
  private DivisionTextField  name    = new DivisionTextField("Наименование...");
  private ScriptPanel        editor  = new ScriptPanel(SyntaxConstants.SYNTAX_STYLE_XML);
  
  public XMLTemplateEditor() {
    super();
    initComponents();
    initEvents();
  }
  
  private void initComponents() {
    editor.addToolComponent(save,0);
    getRootPanel().add(name, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
    getRootPanel().add(editor, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 5, 0, 5), 0, 0));
  }
  
  private void initEvents() {
    save.addActionListener((ActionEvent e) -> {
      saveAction();
    });
  }
  
  @Override
  public String commit() throws RemoteException {
    if(isActive()) {
      ((MappingObject)this.getEditorObject()).setName(name.getText());
      ((XMLTemplate)getEditorObject()).setXML(editor.getText());
    }
    return null;
  }

  @Override
  public void initData()  {
    try {
      if(isActive()) {
        name.setText(((XMLTemplate)getEditorObject()).getName());
        editor.setText(((XMLTemplate)getEditorObject()).getXML());
      }
    }catch(RemoteException ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  @Override
  public void load() {
    super.load();
  }

  @Override
  public void store() {
    super.store();
  }

  @Override
  public String getEmptyObjectTitle() {
    return "Новый шаблон";
  }

  @Override
  public void clearData() {
    name.setText("");
    editor.setText("");
  }
}