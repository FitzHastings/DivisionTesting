package division.editors.tables;

import bum.editors.TreeEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Equipment;
import bum.interfaces.Group;
import bum.interfaces.Product;
import division.editors.objects.GroupEditor;
import division.swing.guimessanger.Messanger;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import javax.swing.JOptionPane;
import mapping.MappingObject;
import util.filter.local.DBFilter;

public class GroupTableEditor extends TreeEditor {
  private Group.ObjectType objectType = Group.ObjectType.ТМЦ;
  
  public GroupTableEditor() {
    super("Объекты", Group.class, GroupEditor.class, "Объекты");
    initEvents();
  }

  @Override
  public void postAddButton(MappingObject object) {
    try {
      ((Group)object).setGroupType(objectType != null ? objectType : ((Group)object).getParent().getGroupType());
      ObjectLoader.saveObject(object, true);
      super.postAddButton(object); //To change body of generated methods, choose Tools | Templates.
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
 
  private void initEvents() {
    ActionListener addListener = getAddAction();
    setAddAction((ActionEvent e) -> {
      if(getSelectedObjectsCount() > 0) {
        objectType = null;
        getAddButton().clearActions();
        addListener.actionPerformed(e);
      }else {
        getAddButton().addAction("Валюта", (ActionEvent x) -> {
          objectType = Group.ObjectType.ВАЛЮТА;
          addListener.actionPerformed(e);
        });

        getAddButton().addAction("ТМЦ", (ActionEvent x) -> {
          objectType = Group.ObjectType.ТМЦ;
          addListener.actionPerformed(e);
        });
      }
    });
    
    setRemoveAction((ActionEvent e) -> {
      String msg = valid();
      if(msg == null)
        removeButtonAction();
      else JOptionPane.showMessageDialog(null, msg, "t", JOptionPane.INFORMATION_MESSAGE);
    });
  }
  
  public String valid() {
    try {
      Integer[] ids = this.getSelectedId();
      if(ids.length > 0) {
        if(!ObjectLoader.getData(
                DBFilter.create(Product.class).AND_IN("group", ids).AND_NOT_EQUAL("type", Product.Type.ARCHIVE).AND_EQUAL("tmp", false),
                new String[]{"id"}, true).isEmpty())
          return "Удаление невозможно, так как выбранные\nобъекты учавствуют в продуктах";
        if(!ObjectLoader.getData(
                DBFilter.create(Equipment.class).AND_IN("group", ids).AND_NOT_EQUAL("type", Product.Type.ARCHIVE).AND_EQUAL("tmp", false), 
                new String[]{"id"}, true).isEmpty())
          return "Удаление невозможно, так как на складах присудствуют объекты данного типа";
        return null;
      }else return "Выберите объекты для удаления";
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
      return ex.getMessage();
    }
  }
  
  @Override
  protected Hashtable<Class,Integer> getDropSupportedClasses() {
    Hashtable<Class,Integer> hash = new Hashtable<>();
    hash.put(Group.class,DnDConstants.ACTION_COPY);
    return hash;
  }

  @Override
  public void editButtonAction() {
    if(getSelectedObjectsCount() == 1)
      super.editButtonAction();
  }
  
  @Override
  public String getEmptyObjectTitle() {
    return "Объекты";
  }
}