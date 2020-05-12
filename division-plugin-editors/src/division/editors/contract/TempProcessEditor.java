package division.editors.contract;

import bum.editors.MainObjectEditor;
import bum.editors.TableEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.DependentProcess;
import bum.interfaces.ProductDocument;
import bum.interfaces.Service;
import bum.interfaces.ContractProcess;
import division.swing.DivisionComboBox;
import division.swing.guimessanger.Messanger;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import mapping.MappingObject;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;

public class TempProcessEditor extends MainObjectEditor {
  private boolean delayTypeActive = true;
  private boolean activeProcessCombobox = true;
  private TableEditor dependedProcessTableEditor = new TableEditor(
          new String[]{"id","process", "", "", "","","","",""}, 
          new String[]{"id","process","delay","actionType","process_name"}, 
          DependentProcess.class, 
          null, 
          MappingObject.Type.CURRENT) {
            @Override
            protected void insertData(List<List> data, int startIndex) {
              for(List d:data) {
                d.add(2, "через");
                JComboBox type = createDelayComboBox((Integer)d.get(0), (String)d.get(3));
                d.set(3, Integer.valueOf(d.get(3).toString().split(" ")[0]).intValue());
                d.add(4, type);
                
                d.add(5, "после события");
                d.set(6, createActionComboBox((Integer)d.get(0),(String)d.get(6)));
                
                d.add(7, "в процессе");
                d.set(8, createProcessComboBox((Integer)d.get(0),(Integer)d.get(1), (String)d.get(8)));
              }
              super.insertData(data, startIndex);
            }
          };
  
  public TempProcessEditor() {
    super();
    initComponents();
    initEvents();
  }
  
  private Object[] createDelayComboboxItems(Integer delayCount) {
    Object[] typeItems = new Object[]{"дней","месяцев","лет"};
    if(delayCount != 11 && delayCount%10 == 1)
      typeItems = new Object[]{"день","месяц","год"};
    else if((delayCount > 21 || delayCount < 10) && delayCount%10 < 5 && delayCount%10 > 0)
      typeItems = new Object[]{"дня","месяца","года"};
    return typeItems;
  }
  
  private JComboBox createDelayComboBox(final Integer id, String delay) {
    DivisionComboBox delayType = new DivisionComboBox(createDelayComboboxItems(Integer.valueOf(delay.split(" ")[0])));
    delayType.setSelectedItem(delay.split(" ")[1]);
    delayType.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if(e.getStateChange() == ItemEvent.SELECTED && delayTypeActive) {
          TableCellEditor editor = dependedProcessTableEditor.getTable().getCellEditor();
          if(editor != null)
            editor.stopCellEditing();
          try {
            DependentProcess dependentProcess = (DependentProcess) ObjectLoader.getObject(DependentProcess.class, id, true);
            dependentProcess.setDelay(dependedProcessTableEditor.getTable().getTableModel().getValueAt(dependedProcessTableEditor.getRowObject(id), 3)+" "+e.getItem().toString());
            ObjectLoader.saveObject(dependentProcess,true);
          }catch(Exception ex) {
            Messanger.showErrorMessage(ex);
          }
        }
      }
    });
    return delayType;
  }
  
  private ProcessItem[] createTempProcessItems() {
    ProcessItem[] items = new ProcessItem[0];
    try {
      Integer tempProcessId = ((ContractProcess) getEditorObject()).getId();
      for(List d:ObjectLoader.executeQuery("SELECT DISTINCT "
              + "[TempProcess(process)], "
              + "[TempProcess(process_name)] "
              + "FROM [TempProcess] "
              + "WHERE "
              + "[TempProcess(template)]=(SELECT [TempProcess(template)] FROM [TempProcess] WHERE [TempProcess(id)]="+tempProcessId+") OR "
              + "[TempProcess(contract)]=(SELECT [TempProcess(contract)] FROM [TempProcess] WHERE [TempProcess(id)]="+tempProcessId+") AND "
              + "[TempProcess(customerPartition)]=(SELECT [TempProcess(customerPartition)] FROM [TempProcess] WHERE [TempProcess(id)]="+tempProcessId+")", true))
        items = (ProcessItem[]) ArrayUtils.add(items, new ProcessItem((Integer)d.get(0), (String)d.get(1)));
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    return items;
  }
  
  private DivisionComboBox createProcessComboBox(final Integer dependId, final Integer processId, final String processName) {
    final DivisionComboBox comboBox = new DivisionComboBox(createTempProcessItems());
    if(processId == null)
      comboBox.setSelectedIndex(-1);
    else {
      ProcessItem item = new ProcessItem(processId, processName);
      if(!ArrayUtils.contains(comboBox.getItems(), item))
        comboBox.addItem(item);
      comboBox.setSelectedItem(item);
    }
    comboBox.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        activeProcessCombobox = false;
        ProcessItem item = (ProcessItem) comboBox.getSelectedItem();
        comboBox.clear();
        comboBox.addItems(createTempProcessItems());
        if(processId == null)
          comboBox.setSelectedIndex(-1);
        else {
          item = new ProcessItem(processId, processName);
          if(!ArrayUtils.contains(comboBox.getItems(), item))
            comboBox.addItem(item);
          comboBox.setSelectedItem(item);
        }
        comboBox.setSelectedItem(item);
        activeProcessCombobox = true;
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
      }
    });
    
    comboBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if(e.getStateChange() == ItemEvent.SELECTED && activeProcessCombobox) {
          TableCellEditor editor = dependedProcessTableEditor.getTable().getCellEditor();
          if(editor != null)
            editor.stopCellEditing();
          try {
            Service process = null;
            Integer processId = ((ProcessItem)e.getItem()).getId();
            if(processId != -1)
              process = (Service) ObjectLoader.getObject(Service.class, processId);
            DependentProcess dependentProcess = (DependentProcess) ObjectLoader.getObject(DependentProcess.class, dependId, true);
            dependentProcess.setProcess(process);
            ObjectLoader.saveObject(dependentProcess, true);
          } catch (Exception ex) {
            Messanger.showErrorMessage(ex);
          }
        }
      }
    });
    
    return comboBox;
  }
  
  private JComboBox createActionComboBox(final Integer id,String type) {
    final DivisionComboBox comboBox = new DivisionComboBox();
    comboBox.addItem(ProductDocument.ActionType.СТАРТ.toString());
    comboBox.addItem(ProductDocument.ActionType.ОТГРУЗКА.toString());
    comboBox.addItem(ProductDocument.ActionType.ОПЛАТА.toString());
    if(type != null)
      comboBox.setSelectedItem(type);
    comboBox.addItemListener((ItemEvent e) -> {
      if(e.getStateChange() == ItemEvent.SELECTED) {
        try {
          TableCellEditor editor = dependedProcessTableEditor.getTable().getCellEditor();
          if(editor != null)
            editor.stopCellEditing();
          DependentProcess dependentProcess = (DependentProcess) ObjectLoader.getObject(DependentProcess.class, id, true);
          switch(comboBox.getSelectedIndex()) {
            case 0:
              dependentProcess.setActionType(ProductDocument.ActionType.СТАРТ);
              break;
            case 1:
              dependentProcess.setActionType(ProductDocument.ActionType.ОТГРУЗКА);
              break;
            case 2:
              dependentProcess.setActionType(ProductDocument.ActionType.ОПЛАТА);
              break;
          }
          ObjectLoader.saveObject(dependentProcess, true);
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
    return comboBox;
  }

  @Override
  public void initData() {
    try {
      ContractProcess tempProcess = (ContractProcess)getEditorObject();
      setTitle(tempProcess.getProcess().getName());

      dependedProcessTableEditor.getClientFilter().clear().AND_EQUAL("tempProcess", tempProcess.getId());
      dependedProcessTableEditor.initData();
    }catch(RemoteException ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  @Override
  public String commit() throws RemoteException {
    return null;
  }

  @Override
  public void setActive(boolean active) {
    super.setActive(active);
    dependedProcessTableEditor.setActive(active);
  }

  @Override
  public void clearData() {
    dependedProcessTableEditor.clear();
    dependedProcessTableEditor.getClientFilter().clear();
  }
  
  private void initComponents() {
    //dependedProcessTableEditor.getTable().setTableHeader(null);
    dependedProcessTableEditor.getStatusBar().setVisible(false);
    dependedProcessTableEditor.getTable().setColumnWidthZero(new int[]{0,1});
    dependedProcessTableEditor.getTable().getTableModel().setColumnClass(3, Integer.class);
    dependedProcessTableEditor.getTable().setColumnEditable(3, true);
    dependedProcessTableEditor.getTable().setColumnEditable(4, true);
    dependedProcessTableEditor.getTable().setColumnEditable(6, true);
    dependedProcessTableEditor.getTable().setColumnEditable(8, true);
    dependedProcessTableEditor.setAddFunction(true);
    dependedProcessTableEditor.setVisibleOkButton(false);
    dependedProcessTableEditor.getGUI().setBorder(BorderFactory.createTitledBorder("Автоматически планировать"));
    
    getRootPanel().setLayout(new GridBagLayout());
    getRootPanel().add(dependedProcessTableEditor.getGUI(),  
            new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 5, 5, 5), 0, 0));
    
    addSubEditorToStore(dependedProcessTableEditor);
  }
  
  private void initEvents() {
    dependedProcessTableEditor.setAddAction(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        addSubProcess();
      }
    });
    
    dependedProcessTableEditor.getTable().getTableModel().addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(TableModelEvent e) {
        if(e.getType() == TableModelEvent.UPDATE) {
          if(e.getColumn() == 3) {
            Integer delayCount = (Integer) dependedProcessTableEditor.getTable().getTableModel().getValueAt(e.getLastRow(), e.getColumn());
            if(delayCount >= 0) {
              DivisionComboBox delayCombobox = (DivisionComboBox) dependedProcessTableEditor.getTable().getTableModel().getValueAt(e.getLastRow(), 4);
              int index = delayCombobox.getSelectedIndex();
              delayTypeActive = false;
              delayCombobox.removeAllItems();
              delayCombobox.addItems(createDelayComboboxItems(delayCount));
              delayCombobox.setSelectedIndex(index);
              delayTypeActive = true;
              try {
                DependentProcess dependentProcess = (DependentProcess) ObjectLoader.getObject(DependentProcess.class, (Integer)dependedProcessTableEditor.getTable().getTableModel().getValueAt(e.getLastRow(), 0), true);
                dependentProcess.setDelay(delayCount+" "+delayCombobox.getSelectedItem().toString());
                ObjectLoader.saveObject(dependentProcess);
              } catch (Exception ex) {
                Messanger.showErrorMessage(ex);
              }
            }else {
              dependedProcessTableEditor.getTable().getTableModel().setValueAt(0, e.getLastRow(), e.getColumn());
            }
          }
        }
      }
    });
  }
  
  
  
  private void addSubProcess() {
    RemoteSession session = null;
    try {
      session = ObjectLoader.createSession(true);
      DependentProcess dependentProcess = (DependentProcess) session.createEmptyObject(DependentProcess.class);
      session.toEstablishes(dependedProcessTableEditor.getRootFilter(), dependentProcess);
      session.saveObject(dependentProcess);
    } catch (Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  @Override
  public String getEmptyObjectTitle() {
    return "[процесс]";
  }

  @Override
  public void dispose() {
    dependedProcessTableEditor.dispose();
    super.dispose();
  }
  
  class ProcessItem {
    private Integer id;
    private String name;

    public ProcessItem(Integer id, String name) {
      this.id = id;
      this.name = name;
    }

    public Integer getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return getName();
    }

    @Override
    public boolean equals(Object obj) {
      return obj != null && obj instanceof ProcessItem && getId().equals(((ProcessItem)obj).getId());
    }
  }
}