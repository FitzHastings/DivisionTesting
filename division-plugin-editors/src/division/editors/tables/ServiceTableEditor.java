package division.editors.tables;

import bum.editors.TreeEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Product;
import bum.interfaces.Service;
import division.editors.objects.ServiceEditor;
import division.swing.guimessanger.Messanger;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.util.Hashtable;
import javax.swing.JOptionPane;
import mapping.MappingObject.RemoveAction;
import util.filter.local.DBFilter;

public class ServiceTableEditor extends TreeEditor {
  public ServiceTableEditor() {
    super("Процессы", Service.class, ServiceEditor.class,"Процессы");
    setRemoveActionType(RemoveAction.MARK_FOR_DELETE);
    setRemoveAction((ActionEvent e) -> {
      if(valid())
        removeButtonAction();
      else JOptionPane.showMessageDialog(null, "Удаление невозможно, так как выбранные\nпроцессы учавствуют в продуктах", "t", JOptionPane.INFORMATION_MESSAGE);
    });
  }
  
  public boolean valid() {
    try {
      Integer[] ids = this.getSelectedId();
      if(ids.length > 0)
        return ObjectLoader.getData(
                DBFilter.create(Product.class).AND_IN("service", ids).AND_NOT_EQUAL("type", Product.Type.ARCHIVE).AND_EQUAL("tmp", false),
                new String[]{"id"}).isEmpty();
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    return false;
  }
  
  @Override
  protected Hashtable<Class,Integer> getDropSupportedClasses() {
    Hashtable<Class,Integer> hash = new Hashtable<>();
    hash.put(Service.class, DnDConstants.ACTION_COPY);
    return hash;
  }
  
  @Override
  public String getEmptyObjectTitle() {
    return "[Процессы]";
  }
}