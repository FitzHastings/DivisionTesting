package division.store;

import bum.editors.EditorGui;
import static bum.editors.EditorGui.setComponentEnable;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Equipment;
import bum.interfaces.Group;
import bum.interfaces.Group.ObjectType;
import bum.interfaces.Store;
import bum.interfaces.Store.StoreType;
import division.editors.tables.GroupTableEditor;
import division.swing.DivisionComboBox;
import division.swing.DivisionItem;
import division.swing.guimessanger.Messanger;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class AddStorePositionDialog extends EditorGui {
  private final JButton closeButton = new JButton("Закрыть");
  
  //private final JSplitPane splitPane = new JSplitPane();
  
  private final GroupTableEditor groupTableEditor = new GroupTableEditor();
  private final DivisionComboBox comboBox = new DivisionComboBox();
  private final JSpinner countEquiup = new JSpinner(new SpinnerNumberModel(1,1,Integer.MAX_VALUE,1));
  private final JCheckBox split = new JCheckBox("Разделить объекты", true);
  private final JPanel countPanel = new JPanel(new GridBagLayout());
  
  //private final StorePositionTableEditor store = new StorePositionTableEditor();
  
  private final Integer sellerPartition;
  private final Integer customerPartition;
  private final boolean storeVisible;
  
  private ObjectType defaultStoreObjectType = ObjectType.ТМЦ;
  private StoreType  defaultStoreType       = StoreType.НАЛИЧНЫЙ;
  
  private Integer[] groups;
  
  public AddStorePositionDialog(Integer sellerPartition, Integer customerPartition, boolean storeVisible) {
    super(null, null);
    this.sellerPartition   = sellerPartition;
    this.customerPartition = customerPartition;
    this.storeVisible = storeVisible;
    initComponents();
    initEvents();
  }
  
  private void initComponents() {
    getOkButton().setText("добавить");
    //getOkButton().setEnabled(false);
    
    getButtonsPanel().add(closeButton);
    
    comboBox.setPreferredSize(new Dimension(250, comboBox.getPreferredSize().height));
    
    JLabel label = new JLabel("Добавить на склад:");
    //label.setFont(new Font("Dialog", Font.BOLD, 14));
    //comboBox.setFont(new Font("Dialog", Font.BOLD, 14));
    getTopPanel().setLayout(new GridBagLayout());
    getTopPanel().add(label, 
            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
    getTopPanel().add(comboBox, 
            new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
    
    countPanel.add(new JLabel("Кол. объектов: "),      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 0), 0, 0));
    countPanel.add(countEquiup,                        new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 5, 0), 0, 0));
    countPanel.add(split,                              new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 0), 0, 0));
    setComponentEnable(countPanel, false);
    
    groupTableEditor.setAdministration(false);
    groupTableEditor.setVisibleOkButton(false);
    groupTableEditor.getStatusBar().setVisible(false);
    groupTableEditor.setSingleSelection(true);
    groupTableEditor.getBottomPanel().setLayout(new BorderLayout());
    groupTableEditor.getBottomPanel().add(countPanel, BorderLayout.CENTER);
    
    getRootPanel().setLayout(new BorderLayout());
    
    //store.setVisibleOkButton(false);
    //store.getToolBar().setVisible(false);
    
    if(storeVisible) {
      //splitPane.add(groupTableEditor.getGUI(), JSplitPane.LEFT);
      //splitPane.add(store.getGUI(), JSplitPane.RIGHT);
      //getRootPanel().add(splitPane, BorderLayout.CENTER);
      getRootPanel().add(groupTableEditor.getGUI(), BorderLayout.CENTER);
    }else getRootPanel().add(groupTableEditor.getGUI(), BorderLayout.CENTER);
    
    //addComponentToStore(splitPane);
  }
  
  private void initEvents() {
    closeButton.addActionListener(e -> this.dispose());
    
    comboBox.addItemListener((ItemEvent e) -> {
      if(e.getStateChange() == ItemEvent.SELECTED) {
        groupTableEditor.getClientFilter().clear();
        if(getGroups() != null && getGroups().length > 0)
          groupTableEditor.getClientFilter().AND_IN("id", getGroups());
        groupTableEditor.getClientFilter().AND_EQUAL("groupType", ((Object[])((DivisionItem)e.getItem()).getData())[0]);
        groupTableEditor.initData();
        
        /*store.setDefaultStoreObjectType((ObjectType) ((Object[])((DivisionItem)e.getItem()).getData())[0]);
        store.setDefaultStoreType((StoreType) ((Object[])((DivisionItem)e.getItem()).getData())[1]);
        store.initData();*/
        
        countEquiup.setValue(1);
      }
    });
    
    groupTableEditor.addEditorListener(this);
    
    /*store.getTable().addTableSelectionListener((int[] oldSelection, int[] newSelection) -> {
      if(newSelection.length > 0)
        groupTableEditor.clearSelection();
      getOkButton().setEnabled(groupTableEditor.getSelectedLastId().length == 1 || newSelection.length > 0);
    });*/
  }
  
  @Override
  public void changeSelection(EditorGui editor, Integer[] ids) {
    //if(groupTableEditor.getSelectedObjectsCount() > 0)
      //store.getTable().clearSelection();
    //getOkButton().setEnabled(groupTableEditor.getSelectedLastId().length == 1 || store.getSelectedId().length > 0);
    setComponentEnable(countPanel, groupTableEditor.getSelectedLastId().length == 1);
  }

  public Integer getSellerPartition() {
    return sellerPartition;
  }

  public Integer getCustomerPartition() {
    return customerPartition;
  }
  
  public Group.ObjectType getDefaultStoreObjectType() {
    return defaultStoreObjectType;
  }

  public void setDefaultStoreObjectType(Group.ObjectType defaultStoreObjectType) {
    this.defaultStoreObjectType = defaultStoreObjectType;
  }

  public Integer[] getGroups() {
    return groups;
  }

  public void setGroups(Integer[] groups) {
    this.groups = groups;
  }

  public Store.StoreType getDefaultStoreType() {
    return defaultStoreType;
  }

  public void setDefaultStoreType(Store.StoreType defaultStoreType) {
    this.defaultStoreType = defaultStoreType;
  }
  
  public boolean isSplit() {
    return split.isSelected();
  }
  
  public Integer getStore() {
    return ((DivisionItem)comboBox.getSelectedItem()).getId();
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
    Integer[] ids = groupTableEditor.getSelectedLastId();
    if(ids.length > 0) {
      if(ids.length > 0) {
        Integer storeId = getStore();//((DivisionItem)comboBox.getSelectedItem()).getId();
        RemoteSession session = null;
        try {
          session = ObjectLoader.createSession();
          List<List> data = session.executeQuery("SELECT [CompanyPartition(remainderControll)] FROM [CompanyPartition] WHERE id="+getSellerPartition());
          if(!data.isEmpty()) {
            boolean remainderControll = (boolean) data.get(0).get(0);
            int count = getCount();//(Integer)countEquiup.getModel().getValue();
            int startId = 0;
            List<List> rez = session.executeQuery("SELECT MAX(id) FROM [Equipment]");
            if(!rez.isEmpty())
              startId = (Integer) rez.get(0).get(0);
            String[] sqls = new String[0];
            for(int i=0;i<(isSplit()?count:1);i++)
              sqls = (String[]) ArrayUtils.add(sqls, "INSERT INTO [Equipment]([!Equipment(store)], [!Equipment(companyPartition)],[!Equipment(group)],[!Equipment(amount)]) VALUES ("
                      +storeId+","+getSellerPartition()+","+ids[0]+","+((remainderControll?1:1)*(isSplit()?1:count))+")");
            session.executeUpdate(sqls);
            ids = new Integer[0];
            rez = session.executeQuery("SELECT id FROM [Equipment] WHERE id>"+startId);
            for(List r:rez)
              ids = (Integer[]) ArrayUtils.add(ids, r.get(0));
            session.addEvent(Equipment.class, "UPDATE", ids);
          }
          session.commit();
        } catch (Exception ex) {
          ObjectLoader.rollBackSession(session);
          Messanger.showErrorMessage(ex);
        }
      }
    }
    return true;
  }

  @Override
  public void clearData() {
    comboBox.clear();
    groupTableEditor.clear();
  }

  @Override
  public void initData() {
    List<List> data = ObjectLoader.getData(
            DBFilter.create(Store.class).AND_EQUAL("companyPartition", sellerPartition), 
            new String[]{"id","objectType","name","main","storeType"});
    
    data.stream().forEach((d) -> {
      DivisionItem item = new DivisionItem(
              (Integer) d.get(0),
              d.get(1)+" - "+d.get(4)+" - "+d.get(2),
              Store.class.getName(), 
              new Object[]{ObjectType.valueOf((String)d.get(1)), StoreType.valueOf((String)d.get(4))});
      comboBox.addItem(item);
      if (ObjectType.valueOf(d.get(1).toString()) == defaultStoreObjectType && (boolean)d.get(3)) {
        comboBox.setSelectedItem(item);
      }
    });
    groupTableEditor.getClientFilter().clear();
    if(getGroups() != null && getGroups().length > 0)
      groupTableEditor.getClientFilter().AND_IN("id", getGroups());
    groupTableEditor.getClientFilter().AND_EQUAL("groupType", ((Object[])((DivisionItem)comboBox.getSelectedItem()).getData())[0]);
  }

  @Override
  public void dispose() {
    groupTableEditor.dispose();
    //store.dispose();
    super.dispose(); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void initTargets() {
  }
}