package division.editors.objects;

import bum.editors.MainObjectEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Group;
import bum.interfaces.Group.ObjectType;
import bum.interfaces.Store;
import bum.interfaces.Store.StoreType;
import division.editors.tables.GroupTableEditor;
import division.swing.DivisionComboBox;
import division.swing.DivisionTextField;
import division.swing.LinkLabel;
import division.swing.guimessanger.Messanger;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

public class StoreEditor extends MainObjectEditor {
  private final DivisionTextField nameText        = new DivisionTextField("Наименование");
  private final DivisionComboBox  storeType       = new DivisionComboBox(Store.StoreType.НАЛИЧНЫЙ, Store.StoreType.БЕЗНАЛИЧНЫЙ);
  private final DivisionComboBox  storeObjectType = new DivisionComboBox(Group.ObjectType.ТМЦ, Group.ObjectType.ВАЛЮТА);
  private final JCheckBox         controllIn      = new JCheckBox("Контроль прихода");
  private final JCheckBox         controllOut     = new JCheckBox("Контроль расхода");
  private final LinkLabel         currencyLabel   = new LinkLabel("Выберите валюту");
  private       Integer           currency        = null;

  public StoreEditor() {
    initComponents();
    initEvents();
  }

  private void initComponents() {
    currencyLabel.setFont(new Font("Dialog", Font.BOLD, 13));
    
    getRootPanel().add(nameText,        new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

    getRootPanel().add(storeType,       new GridBagConstraints(0, 1, 1, 1, 0.5, 1.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(storeObjectType, new GridBagConstraints(1, 1, 1, 1, 0.5, 1.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    
    getRootPanel().add(new JLabel(" "), new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(currencyLabel,   new GridBagConstraints(1, 2, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    
    getRootPanel().add(controllIn,      new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(controllOut,     new GridBagConstraints(1, 3, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
  }
  
  private void initEvents() {
    controllIn.addItemListener((ItemEvent e) -> {
      if(e.getStateChange() == ItemEvent.SELECTED && !controllOut.isSelected())
        controllOut.setSelected(true);
    });
    
    controllOut.addItemListener((ItemEvent e) -> {
      if(e.getStateChange() == ItemEvent.DESELECTED && controllIn.isSelected())
        controllIn.setSelected(false);
    });
    
    storeObjectType.addItemListener((ItemEvent e) -> {
      currencyLabel.setVisible((ObjectType)e.getItem() == ObjectType.ВАЛЮТА);
    });
    
    currencyLabel.addActionListener(e -> {
      try {
        GroupTableEditor groups = new GroupTableEditor();
        groups.setAutoLoadAndStore(true);
        groups.getClientFilter().AND_EQUAL("groupType", ObjectType.ВАЛЮТА);
        groups.setSingleSelection(true);
        groups.initData();
        Integer[] ids = groups.get(true);
        if(ids != null && ids.length > 0) {
          currency = ids[0];
          Object cur = ObjectLoader.getData(Group.class, true, currency, "name");
          currencyLabel.setText(cur != null ? "Вылюта: "+cur : "");
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    });
  }

  @Override
  public String commit() throws Exception {
    String msg = "";
    if(nameText.getText().equals(""))
      msg += "  Не введено наименование\n";
    
    if(storeObjectType.getSelectedItem() == Group.ObjectType.ВАЛЮТА && currency == null)
      msg += "  Выберите валюту склада\n";
    
    if("".equals(msg)) {
      Store store = (Store) getEditorObject();
      store.setName(nameText.getText());
      store.setStoreType((Store.StoreType) storeType.getSelectedItem());
      store.setObjectType((Group.ObjectType) storeObjectType.getSelectedItem());
      store.setControllIn(controllIn.isSelected());
      store.setControllOut(controllOut.isSelected());
      
      store.setCurrency(!currencyLabel.isVisible() ? null : (Group)ObjectLoader.getObject(Group.class, currency, true));
    }
    return msg;
  }

  @Override
  public void initData() throws Exception {
    Store store = (Store) getEditorObject();
    boolean tmp = store.isTmp();
    
    ObjectType oType = store.getObjectType();
    StoreType  sType = store.getStoreType();
    
    Store parent = store.getParent();
    if(parent != null) {
      oType = tmp ? parent.getObjectType() : oType;
      sType = tmp ? parent.getStoreType()  : sType;
      
      storeType.setEnabled(false);
      storeObjectType.setEnabled(false);
    }
    
    currencyLabel.setVisible(oType == ObjectType.ВАЛЮТА);
    if(currencyLabel.isVisible()) {
      Group c = store.getCurrency();
      if(c != null) {
        currency = c.getId();
        currencyLabel.setText("Вылюта: "+c.getName());
      }
    }
    
    nameText.setText(store.getName());
    storeType.setSelectedItem(sType);
    storeObjectType.setSelectedItem(oType);
    controllIn.setSelected(store.isControllIn());
    controllOut.setSelected(store.isControllOut());
  }
}