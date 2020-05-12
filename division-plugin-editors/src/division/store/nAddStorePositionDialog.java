package division.store;

import bum.editors.EditorGui;
import static bum.editors.EditorGui.setComponentEnable;
import bum.editors.EditorListener;
import bum.editors.TreeEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Equipment;
import bum.interfaces.Group.ObjectType;
import bum.interfaces.Store;
import division.editors.objects.StoreEditor;
import division.editors.tables.GroupTableEditor;
import division.swing.LinkLabel;
import division.swing.guimessanger.Messanger;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import mapping.MappingObject;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;

public class nAddStorePositionDialog extends EditorGui {
  private final JButton closeButton = new JButton("Закрыть");
  
  private final GroupTableEditor groupTableEditor = new GroupTableEditor();
  private final LinkLabel storeLabel = new LinkLabel();
  private final JSpinner countEquiup = new JSpinner(new SpinnerNumberModel(1,1,Integer.MAX_VALUE,1));
  private final JPanel countPanel = new JPanel(new GridBagLayout());
  
  private Integer partition;
  private Integer store;
  private Integer currency;
  private ObjectType objectType;
  
  private final boolean storeVisible;
  
  public nAddStorePositionDialog(Integer store, boolean storeVisible) {
    super(null, null);
    this.store = store;
    this.storeVisible = storeVisible;
    initComponents();
    initEvents();
  }
  
  private void initComponents() {
    getOkButton().setText("добавить");
    //getOkButton().setEnabled(false);
    
    getButtonsPanel().add(closeButton);
    
    getTopPanel().setLayout(new GridBagLayout());
    getTopPanel().add(storeLabel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
    
    countPanel.add(new JLabel("Количество объектов: "),      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 0), 0, 0));
    countPanel.add(countEquiup,                        new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 5, 0), 0, 0));
    setComponentEnable(countPanel, false);
    
    groupTableEditor.setAdministration(false);
    groupTableEditor.setVisibleOkButton(false);
    groupTableEditor.getStatusBar().setVisible(false);
    groupTableEditor.setSingleSelection(true);
    groupTableEditor.getBottomPanel().setLayout(new BorderLayout());
    
    getRootPanel().setLayout(new BorderLayout());
    
    if(!storeVisible)
      storeLabel.setForeground(Color.BLACK);
    storeLabel.setFont(new Font("Dialog", Font.BOLD, 16));
    storeLabel.setLinkBorder(storeVisible);
    getRootPanel().add(groupTableEditor.getGUI(), BorderLayout.CENTER);
    getBottomPanel().setLayout(new BorderLayout());
    getBottomPanel().add(countPanel, BorderLayout.CENTER);
  }
  
  private void initEvents() {
    closeButton.addActionListener(e -> this.dispose());
    
    storeLabel.addActionListener((ActionEvent e) -> {
      if(partition != null) {
        TreeEditor storeTableEditor = new TreeEditor("Склады", Store.class, StoreEditor.class, "Склады", MappingObject.Type.CURRENT);
        storeTableEditor.getClientFilter().AND_EQUAL("companyPartition", partition);
        storeTableEditor.setAutoLoadAndStore(true);
        storeTableEditor.initData();
        storeTableEditor.setSelectingOnlyLastNode(true);
        storeTableEditor.setSingleSelection(true);
        Integer[] ids = storeTableEditor.get(true);
        if(ids.length == 1) {
          store = ids[0];
          initData();
        }
      }
    });
    
    groupTableEditor.addEditorListener(this);
  }
  
  @Override
  public void changeSelection(EditorGui editor, Integer[] ids) {
    setComponentEnable(countPanel, groupTableEditor.getSelectedLastId().length == 1);
  }
  
  public int getCount() {
    return (int)countEquiup.getModel().getValue();
  }
  
  @Override
  public void closeDialog() {
    groupTableEditor.clearSelection();
    super.closeDialog();
  }
  
  public Integer[] get() {
    createDialog(true).setVisible(true);
    return groupTableEditor.getSelectedLastId();
  }

  @Override
  public Boolean okButtonAction() {
    Object controllIn = ObjectLoader.getData(Store.class, store, "controllIn");
    if(controllIn != null && (Boolean) controllIn)
      return false;
    
    Integer[] ids = groupTableEditor.getSelectedLastId();
    if(ids.length > 0) {
      RemoteSession session = null;
      try {
        session = ObjectLoader.createSession();
        int count = getCount();//(Integer)countEquiup.getModel().getValue();
        int startId = 0;
        List<List> data = session.executeQuery("SELECT MAX(id) FROM [Equipment]");
        if(!data.isEmpty())
          startId = data.get(0).get(0) == null ? 0 : (Integer) data.get(0).get(0);
        
        String[] sqls = new String[0];
        sqls = (String[]) ArrayUtils.add(sqls, "INSERT INTO [Equipment]([!Equipment(store)], [!Equipment(group)], [!Equipment(amount)]) VALUES ("
                +store+","+ids[0]+","+count+")");
        session.executeUpdate(sqls);
        ids = new Integer[0];
        data = session.executeQuery("SELECT id FROM [Equipment] WHERE id>"+startId);
        for(List r:data)
          ids = (Integer[]) ArrayUtils.add(ids, r.get(0));
        session.addEvent(Equipment.class, "UPDATE", ids);
        ObjectLoader.commitSession(session);
      } catch (Exception ex) {
        ObjectLoader.rollBackSession(session);
        Messanger.showErrorMessage(ex);
      }
    }
    return true;
  }

  @Override
  public void clearData() {
    groupTableEditor.clear();
  }

  @Override
  public void initData() {
    try {
      partition = null;
      ObjectLoader.getData(
              Store.class, 
              true,
              store, 
              "id","objectType","name","main","storeType","companyPartition", "currency").stream().forEach(d -> {
                partition  = (Integer) d.get(5);
                objectType = ObjectType.valueOf(d.get(1).toString());
                currency   = (Integer) d.get(6);
                storeLabel.setText("Склад: "+(String)d.get(2));
                
                if(objectType == ObjectType.ВАЛЮТА) {
                  groupTableEditor.setEnabled(false);
                  groupTableEditor.addEditorListener(new EditorListener() {
                    @Override
                    public void initDataComplited(EditorGui editor) {
                      groupTableEditor.setSelectedObjects(currency);
                    }
                  });
                }
                groupTableEditor.getClientFilter().clear().AND_EQUAL("groupType", objectType);
                groupTableEditor.initData();
              });
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  @Override
  public void dispose() {
    groupTableEditor.dispose();
    super.dispose();
  }

  @Override
  public void initTargets() {
  }
}