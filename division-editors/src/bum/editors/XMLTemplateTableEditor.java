package bum.editors;

import bum.interfaces.XMLTemplate;
import division.swing.guimessanger.Messanger;
import util.filter.local.DBFilter;

public class XMLTemplateTableEditor extends TableEditor {
  private DBFilter classUnion;
  
  public XMLTemplateTableEditor(String objectClass) {
    super(
            new String[]{"id","наименование"},
            new String[]{"id","name"},
            XMLTemplate.class,
            XMLTemplateEditor.class,
            "Шаблоны жокументов");
    try {
      if(classUnion != null)
        classUnion.clear();
      else classUnion = this.getClientFilter().AND_FILTER();
      classUnion.AND_EQUAL("objectClassName", objectClass);
    }catch(Exception ex){Messanger.showErrorMessage(ex);}
  }
  
  public XMLTemplateTableEditor(String objectClass, Class xmlTemplateClass, Class xmlTemplateEditorClass) {
    super(
            new String[]{"id","наименование"},
            new String[]{"id","name"},
            xmlTemplateClass,
            xmlTemplateEditorClass,
            "Шаблоны жокументов");
    try {
      if(classUnion != null)
        classUnion.clear();
      else classUnion = this.getClientFilter().AND_FILTER();
      classUnion.AND_EQUAL("objectClassName", objectClass);
    }catch(Exception ex){Messanger.showErrorMessage(ex);}
  }

  /*@Override
  protected int insertObject(RMIDBObject object, int row) throws RemoteException {
    try {
      getTable().getTableModel().insertRow(row, new Object[]{object.getId(),object.getName()});
    }catch(RemoteException ex){Messanger.showErrorMessage(ex);}
    return row;
  }*/

  @Override
  public void load() {
    super.load();
    //System.out.println("XMLTemplateTableEditor LOAD");
    //System.out.println(getGUI().getPreferredSize());
  }

  @Override
  public void store() {
    super.store();
    //System.out.println("XMLTemplateTableEditor STORE");
    //System.out.println(getGUI().getSize());
  }

  @Override
  public String getEmptyObjectTitle(){return "";}
}