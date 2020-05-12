package division.editors.objects;

import bum.editors.MainObjectEditor;
import bum.editors.TableEditor;
import bum.editors.XMLTemplateEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Document;
import bum.interfaces.DocumentXMLTemplate;
import division.swing.DivisionScrollPane;
import division.swing.DivisionTextArea;
import division.swing.DivisionTextField;
import division.swing.guimessanger.Messanger;
import division.swing.ScriptPanel;
import division.swing.DivisionToolButton;
import division.util.FileLoader;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import mapping.MappingObject.Type;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import util.RemoteSession;

public class DocumentEditor extends MainObjectEditor {
  private final DivisionToolButton save    = new DivisionToolButton(FileLoader.getIcon("Save16.gif"));
  private final JSplitPane         split   = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
  private final ScriptPanel        aScript = new ScriptPanel(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
  private final DivisionTextField  name    = new DivisionTextField("Имя документа...");
  
  private final DivisionTextArea   descriptionBox    = new DivisionTextArea("Описание...");
  private final DivisionScrollPane descriptionscroll = new DivisionScrollPane(descriptionBox);
  private final TableEditor    templateTableEditor = new TableEditor(
          new String[]{"id","Наименование","Основной"},
          new String[]{"id","name","main"},
          DocumentXMLTemplate.class,
          XMLTemplateEditor.class,
          "Шаблоны",
          Type.CURRENT);

  public DocumentEditor() {
    addComponentToStore(split,"split");
    templateTableEditor.getTable().setColumnEditable(2, true);
    templateTableEditor.getTable().getTableModel().addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(TableModelEvent e) {
        if(e.getLastRow() >= 0 && e.getColumn() >= 0) {
          int column   = templateTableEditor.getTable().convertColumnIndexToModel(e.getColumn());
          int row      = templateTableEditor.getTable().convertRowIndexToModel(e.getLastRow());
          Object value = templateTableEditor.getTable().getModel().getValueAt(row, column);
          if(e.getType() == TableModelEvent.UPDATE && column == 2 && templateTableEditor.getSelectedObjectsCount() > 0) {
            DocumentXMLTemplate object = (DocumentXMLTemplate)templateTableEditor.getSelectedObjects()[0];
            Boolean main = Boolean.valueOf(value.toString());
            RemoteSession session = null;
            try {
              session = ObjectLoader.createSession(false);
              if(main) {
                for(DocumentXMLTemplate template:((Document)getEditorObject()).getTemplates()) {
                  if(!object.equals(template) && template.isMain()) {
                    template.setMain(false);
                    session.saveObject(template);
                  }
                }
              }
              object.setMain(main);
              session.saveObject(object);
              session.commit();
            }catch(Exception ex) {
              ObjectLoader.rollBackSession(session);
              Messanger.showErrorMessage(ex);
            }
          }
        }
      }
    });

    aScript.addToolComponent(save,0);

    templateTableEditor.setVisibleOkButton(false);
    templateTableEditor.getStatusBar().setVisible(false);

    getRootPanel().add(split, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));

    JPanel scriptPanel = new JPanel(new BorderLayout());
    scriptPanel.add(aScript,BorderLayout.CENTER);
    
    JPanel panel = new JPanel(new GridBagLayout());
    panel.add(templateTableEditor.getGUI(), new GridBagConstraints(0, 0, 1, 2, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    panel.add(name,                         new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 5, 5, 5), 0, 0));
    panel.add(descriptionscroll,            new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    
    split.add(panel,JSplitPane.TOP);
    split.add(scriptPanel,JSplitPane.BOTTOM);
    
    save.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        saveAction();
      }
    });
  }

  @Override
  public String commit() throws RemoteException {
    String msg = "";
    if(name.getText().equals(""))
      msg += "  -Необходимо назвать документ\n";
    if(msg.equals("")) {
      Document document = (Document)getEditorObject();
      document.setName(name.getText());
      document.setScript(aScript.getText());
      document.setScriptLanguage(aScript.getContentType());
    }
    return msg;
  }

  @Override
  public void initData() throws RemoteException {
    Document document = (Document)getEditorObject();
    templateTableEditor.getClientFilter().clear();
    templateTableEditor.getClientFilter().AND_EQUAL("document", document.getId()).AND_EQUAL("companyPartition", null);
    templateTableEditor.initData();
    name.setText(document.getName());
    descriptionBox.setText(document.getDescription());
    aScript.setText(document.getScript());
    aScript.setContentType(document.getScriptLanguage());
  }

  @Override
  public void clearData() {
    name.setText("");
    aScript.setText("");
  }
}