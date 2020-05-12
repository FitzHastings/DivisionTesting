package division.editors.objects.company;

import bum.editors.EditorGui;
import bum.editors.EditorListener;
import bum.editors.TreeEditor;
import bum.interfaces.Store;
import division.editors.objects.StoreEditor;
import division.store.nStorePositionTableEditor;
import division.swing.DivisionSplitPane;
import division.swing.guimessanger.Messanger;
import java.awt.BorderLayout;
import java.math.BigDecimal;
import java.util.TreeMap;
import javax.swing.JSplitPane;
import mapping.MappingObject;

public class CompanyPartitionStoreTableEditor extends EditorGui {
  private final DivisionSplitPane split = new DivisionSplitPane(JSplitPane.HORIZONTAL_SPLIT);  
  private final TreeEditor storeTableEditor = new TreeEditor("Депозитарии", Store.class, StoreEditor.class, "Склады", MappingObject.Type.CURRENT);
  private final nStorePositionTableEditor storePositionTable = new nStorePositionTableEditor();
  
  private Integer partition;

  public CompanyPartitionStoreTableEditor() {
    this(null);
  }

  public CompanyPartitionStoreTableEditor(Integer partition) {
    super(null, null);
    this.partition = partition;
    initComponents();
    initEvents();
  }
  
  private void initComponents() {
    storeTableEditor.setVisibleOkButton(false);
    storePositionTable.setVisibleOkButton(false);
    
    split.add(storeTableEditor.getGUI(), JSplitPane.LEFT);
    split.add(storePositionTable.getGUI(), JSplitPane.RIGHT);
    getRootPanel().setLayout(new BorderLayout());
    getRootPanel().add(split, BorderLayout.CENTER);
  }

  private void initEvents() {
    storeTableEditor.addEditorListener(storePositionTable);
  }

  public nStorePositionTableEditor getStorePositionTable() {
    return storePositionTable;
  }
  
  @Override
  public Boolean okButtonAction() {
    storePositionTable.okButtonAction();
    dispose();
    return true;
  }

  @Override
  public void initData() {
    if(isActive()) {
      storeTableEditor.getClientFilter().clear().AND_EQUAL("companyPartition", partition);
      storeTableEditor.initData();
    }
  }
  
  public TreeMap<Integer, BigDecimal> get() {
    createDialog(true).setVisible(true);
    return storePositionTable.getSelectedPositions();
  }
  
  @Override
  public void changeSelection(EditorGui editor, Integer[] ids) {
    partition = ids.length > 0 ? ids[0]:null;
    try {
      initData();
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  @Override
  public void initTargets() {
  }
}