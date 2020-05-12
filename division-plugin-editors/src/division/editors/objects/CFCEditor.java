package division.editors.objects;

import bum.editors.MainObjectEditor;
import bum.editors.TableEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.CFC;
import bum.interfaces.People;
import bum.interfaces.Worker;
import division.swing.guimessanger.Messanger;
import division.swing.DivisionTextField;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import javax.swing.BorderFactory;
import mapping.MappingObject;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;

public class CFCEditor extends MainObjectEditor {
  private DivisionTextField cfcName = new DivisionTextField("Наименование...");
  private TableEditor workerTableEditor = new TableEditor(
          new String[]{"id","Фамилия","Имя","Отчество"}, 
          new String[]{"id","people_surname","people_name","people_lastname"}, 
          Worker.class, 
          WorkerEditor.class, 
          null, 
          null, 
          MappingObject.Type.CURRENT);
  
  public CFCEditor() throws RemoteException {
    super();
    initComponents();
    initEvents();
  }
  
  @Override
  public void initData() throws RemoteException {
    cfcName.setText(((CFC)getEditorObject()).getName());
    workerTableEditor.getClientFilter().clear();
    workerTableEditor.getClientFilter().AND_EQUAL("cfc", ((CFC)getEditorObject()).getId());
    workerTableEditor.initData();
  }
  
  @Override
  public String commit() throws RemoteException {
    String msg = "";
    CFC c = (CFC)getEditorObject();
    if("".equals(cfcName.getText()))
      msg+="незаполнено поле 'Наименование'\n";
    c.setName(cfcName.getText());
    return msg;
  }
  
  @Override
  public void clearData() {
    cfcName.setText("");
  }
  
  private void initComponents() {
    addSubEditorToStore(workerTableEditor);
    
    workerTableEditor.getGUI().setBorder(BorderFactory.createTitledBorder("Участники ЦФУ"));
    
    workerTableEditor.setAddFunction(true);
    workerTableEditor.setEditFunction(true);
    workerTableEditor.setVisibleOkButton(false);
    
    getRootPanel().add(cfcName,                    new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(workerTableEditor.getGUI(), new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
  }
  
  @Override
  public String getEmptyObjectTitle() {
    return "[ЦФУ]";
  }

  private void initEvents() {
    workerTableEditor.setAddAction(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        RemoteSession session = null;
        try {
          TableEditor peopleTableEditor = new TableEditor(
            new String[]{"id","Фамилия","Имя","Отчество"}, 
            new String[]{"id","surName","name","lastName"}, 
            People.class, 
            PeopleEditor.class, 
            "Персонал", 
            null, 
            MappingObject.Type.CURRENT);
          
          peopleTableEditor.setAutoLoad(true);
          peopleTableEditor.setAutoStore(true);
          peopleTableEditor.initData();
          
          MappingObject[] peoples = peopleTableEditor.getObjects();
          if(peoples != null && peoples.length > 0) {
            Worker[] workers = new Worker[0];
            session = ObjectLoader.createSession(false);
            for(MappingObject people:peoples) {
              Worker worker = (Worker) session.createEmptyObject(Worker.class);
              worker.setPeople((People)people);
              worker.setCfc((CFC)getEditorObject());
              if(session.saveObject(worker))
                workers = (Worker[]) ArrayUtils.add(workers, worker);
            }
            session.commit();
            for(Worker worker:workers)
              workerTableEditor.postAddButton(worker);
          }
        }catch(Exception ex) {
          ObjectLoader.rollBackSession(session);
          Messanger.showErrorMessage(ex);
        }
      }
    });
  }
}