package division.editors.tables;

import bum.editors.EditorGui;
import bum.editors.TableEditor;
import bum.editors.EditorListener;
import bum.editors.util.DivisionTarget;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Equipment;
import bum.interfaces.EquipmentFactorValue;
import bum.interfaces.Factor;
import bum.interfaces.Factor.FactorType;
import bum.interfaces.Group;
import bum.interfaces.Store;
import bum.interfaces.Store.StoreType;
import division.border.LinkBorder;
import division.fx.PropertyMap;
import division.swing.*;
import division.swing.actions.LinkBorderActionEvent;
import division.swing.guimessanger.Messanger;
import division.swing.table.CellColorController;
import division.swing.table.CellEditableController;
import division.util.FileLoader;
import division.util.yandex.YandexMapDialog;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import mapping.MappingObject;
import mapping.MappingObject.RemoveAction;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class EquipmentTableEditor extends EditorGui {
  private final DivisionToolButton add    = new DivisionToolButton(FileLoader.getIcon("Add16.gif"));
  private final DivisionToolButton remove = new DivisionToolButton(FileLoader.getIcon("Delete16.gif"));
  //private final DivisionToolButton yandexMap = new DivisionToolButton("на карту");
  private final MappingObject.RemoveAction removeActionType = MappingObject.RemoveAction.MOVE_TO_ARCHIVE;

  private final ArrayList<EditorListener> dbTableEditorListeners = new ArrayList<>();
  
  private final LinkBorder storeBorder  = new LinkBorder("Выберите СКЛАД");
  private Store.StoreType[] storeTypes  = null;

  private final DivisionTable          table       = new DivisionTable();
  private final DivisionScrollPane     scroll      = new DivisionScrollPane(table);

  private Integer[] factorIds;
  private Factor.FactorType[] factorTypes;
  private Integer[] groups;
  private Integer   partitionId;
  private Integer   storeId;
  private Integer[] notInEquipIds = new Integer[0];
  
  private final TreeMap<Integer,List<Integer>> groupFactors = new TreeMap<>();
  private final TreeMap<Integer, String> listFactorIds = new TreeMap<>();

  private final DBFilter equipFilter = DBFilter.create(Equipment.class);
  
  private final ExecutorService pool = Executors.newSingleThreadExecutor();
  private InitDataTask initDataTask = null;

  public EquipmentTableEditor() {
    super(null, null);
    initComponents();
    initTargets();
    initEvents();
  }

  private void initComponents() {
    table.setColumnSelectionAllowed(false);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    
    add.setEnabled(false);
    getToolBar().add(add);
    getToolBar().add(remove);
    
    //storeLink.setFont(new Font("Arial", Font.BOLD, 14));
    storeBorder.setTitleFont(new Font("Arial", Font.BOLD, 14));
    getGUI().setBorder(storeBorder);
    //getToolBar().addSeparator();
    //getToolBar().add(storeLink);
    
    scroll.setRowHeader(true);
    getRootPanel().add(scroll, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    
    table.setCellEditableController(new CellEditableController() {
      @Override
      public boolean isCellEditable(JTable table, int modelRow, int modelColumn) {
        int c = table.getModel().getColumnCount()-factorIds.length;
        List<Integer> factors = groupFactors.get((Integer) table.getModel().getValueAt(modelRow, 1));
        return modelColumn >= c && factors != null && factors.contains(factorIds[modelColumn-c]) && factorTypes[modelColumn-c] != FactorType.адрес;
      }
    });
    
    table.setCellColorController(new CellColorController() {
      @Override
      public Color getCellColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        int c = table.getModel().getColumnCount()-factorIds.length;
        List<Integer> factors = groupFactors.get((Integer) table.getModel().getValueAt(modelRow, 1));
        return modelColumn < c || factors != null && factors.contains(factorIds[modelColumn-c])?null:(isSelect?Color.GRAY:Color.GRAY.brighter());
      }
    });
  }

  @Override
  public void initTargets() {
    addTarget(new DivisionTarget(EquipmentFactorValue.class) {
      @Override
      public void messageReceived(String type, Integer[] ids, PropertyMap objectEventProperty) {
        Integer[] efvs = ids;
        try {
          List<List> data = ObjectLoader.executeQuery("SELECT [EquipmentFactorValue(equipment)],[EquipmentFactorValue(factor)],[EquipmentFactorValue(name)] "
                  + "FROM [EquipmentFactorValue] WHERE id=ANY(?)", true, new Object[]{efvs});
          if(!data.isEmpty()) {
            Integer[] es = new Integer[0];
            for(List d:data)
              es = (Integer[]) ArrayUtils.add(es, d.get(0));
            equipFilter.clear().AND_EQUAL("store", storeId).AND_IN("group", groups).AND_EQUAL("companyPartition", partitionId).AND_EQUAL("type", MappingObject.Type.CURRENT);
            es = ObjectLoader.isSatisfy(equipFilter, es, true);
            if(es.length > 0) {
              for(List d:data) {
                Object value = d.get(2);
                if(value != null && ArrayUtils.contains(es, d.get(0))) {
                  int rowIndex = getRow((Integer) d.get(0));
                  if(rowIndex >= 0) {
                    Integer factorId = (Integer) d.get(1);
                    int index = ArrayUtils.indexOf(factorIds, factorId);
                    if(index >= 0) {
                      int columnIndex = index+(table.getTableModel().getColumnCount()-factorIds.length);
                      FactorType factorType = factorTypes[index];
                      switch(factorType) {
                        case число:
                          value = Double.valueOf(value.toString());
                          break;
                        case текст:
                          value = value.toString();
                          break;
                        case адрес:
                          break;
                        default:break;
                      }
                      
                      setActive(false);
                      if(listFactorIds.containsKey(factorId)) {
                        JComboBox comboBox = (JComboBox) table.getTableModel().getValueAt(rowIndex, columnIndex);
                        comboBox.setSelectedItem(value);
                      }else table.getTableModel().setValueAt(value, rowIndex, columnIndex);
                      setActive(true);
                    }
                  }
                }
              }
            }
          }
        } catch (Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });

    addTarget(new DivisionTarget(Equipment.class) {
      @Override
      public void messageReceived(String type, Integer[] ids, PropertyMap objectEventProperty) {
        try {
          if(ids != null && ids.length > 0) {
            equipFilter.clear().AND_EQUAL("store", storeId).AND_IN("group", groups).AND_EQUAL("companyPartition", partitionId).AND_EQUAL("type", MappingObject.Type.CURRENT);
            if(type.equals("CREATE")) {
              ids = ObjectLoader.isSatisfy(equipFilter, ids);
              if(ids != null && ids.length > 0)
                insertEquipments(ids);
            }else if(type.equals("REMOVE")) {
              removeEquipmentFromTable(ids);
            }else if(type.equals("UPDATE")) {
              //Integer[] removeIds = new Integer[0];
              removeEquipmentFromTable(ids);
              ids = ObjectLoader.isSatisfy(equipFilter, ids);
              if(ids != null && ids.length > 0)
                insertEquipments(ids);
            }
          }
        }catch(Exception e) {
          Messanger.showErrorMessage(e);
        }
      }
    });
  }

  private void initEvents() {
    storeBorder.addActionListener((ActionEvent e) -> {
      if(partitionId != null) {
        Rectangle titleBounds = ((LinkBorderActionEvent)e).getTitleBounds();
        createStoreMenu().show(((LinkBorder)e.getSource()).getComponent(), titleBounds.x-5, titleBounds.y+titleBounds.height);
      }
    });
    
    /*yandexMap.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int[] rows = table.getSelectedRows();
        if(rows.length == 0) {
          for(int i=0;i<table.getRowCount();i++)
            rows = ArrayUtils.add(rows, i);
        }
        if(rows.length > 0) {
          JSONObject[] addresses = new JSONObject[0];
          int c = table.getModel().getColumnCount()-factorIds.length;
          for(int i=c;i<table.getColumnCount();i++) {
            if(factorTypes[i-c] == FactorType.адрес) {
              for(int row:rows) {
                Object value = table.getValueAt(row, i);
                if(value != null && value.toString().startsWith("json")) {
                  JSONObject json = JSONObject.fromObject(value.toString().substring(4));
                  addresses = (JSONObject[]) ArrayUtils.add(addresses, json);
                }
              }
            }
          }
          if(addresses.length > 0)
            YandexMapDialog.showAddresses(addresses);
        }
      }
    });*/
    
    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        int row = table.rowAtPoint(e.getPoint());
        int col = table.columnAtPoint(e.getPoint());
        if(e.getClickCount() == 2 && row >= 0 && col >= 0) {
          row = table.convertRowIndexToModel(table.rowAtPoint(e.getPoint()));
          col = table.convertColumnIndexToModel(table.columnAtPoint(e.getPoint()));
          if(row >= 0 && col >= 0) {
            int c = table.getModel().getColumnCount()-factorIds.length;
            if(factorTypes[col-c] == FactorType.адрес) {
              JSONObject address;
              Object val = table.getModel().getValueAt(row, col);
              if(val != null && !val.equals("") && val.toString().startsWith("json")) {
                address = YandexMapDialog.getAddress(JSONObject.fromObject(val.toString().substring(4)));
              }else
                address = YandexMapDialog.getAddress(val==null?"":val.toString());
                if(address != null) {
                Integer equipId  = (Integer) table.getTableModel().getValueAt(row, 0);
                Integer factorId = factorIds[col-c];
                RemoteSession session = null;
                try {
                  session = ObjectLoader.createSession();
                  List<List> data = session.executeQuery("SELECT [EquipmentFactorValue(id)] FROM [EquipmentFactorValue] "
                          + "WHERE [EquipmentFactorValue(equipment)]="+equipId+" AND [EquipmentFactorValue(factor)]="+factorId);
                  if(data.isEmpty()) {
                    session.executeUpdate("INSERT INTO [EquipmentFactorValue]([EquipmentFactorValue(equipment)],"
                            + "[EquipmentFactorValue(factor)],[EquipmentFactorValue(name)]) VALUES(?,?,?)", new Object[]{equipId,factorId,"json"+address.toString()});
                    data = session.executeQuery("SELECT MAX(id) FROM [EquipmentFactorValue]");
                    session.addEvent(EquipmentFactorValue.class, "CREATE", (Integer) data.get(0).get(0));
                  }else {
                    session.executeUpdate("UPDATE [EquipmentFactorValue] SET [EquipmentFactorValue(name)]=? "
                            + "WHERE [EquipmentFactorValue(equipment)]="+equipId+" AND [EquipmentFactorValue(factor)]="+factorId, new Object[]{"json"+address.toString()});
                    session.addEvent(EquipmentFactorValue.class, "UPDATE", (Integer) data.get(0).get(0));
                  }
                  session.commit();
                } catch (Exception ex) {
                  ObjectLoader.rollBackSession(session);
                  Messanger.showErrorMessage(ex);
                }
              }
            }
          }
        }
      }
    });
    
    remove.addActionListener((ActionEvent e) -> {
      removeEquipments();
    });

    add.addActionListener((ActionEvent e) -> {
      createEquipments();
    });

    table.getTableModel().addTableModelListener((TableModelEvent e) -> {
      if(isActive() && table.isEnabled() && e.getType() == TableModelEvent.UPDATE) {
        int row = e.getLastRow();
        int column = e.getColumn();
        if(row >= 0 && column >= 3) {
          Object value = table.getTableModel().getValueAt(row, column);
          if(value != null) {
            if(value instanceof JComboBox)
              value = ((JComboBox)value).getSelectedItem();
            int c = table.getTableModel().getColumnCount()-factorIds.length;
            Integer equipId  = (Integer) table.getTableModel().getValueAt(row, 0);
            Integer factorId = factorIds[column-c];
            RemoteSession session = null;
            try {
              session = ObjectLoader.createSession();
              List<List> data = session.executeQuery("SELECT [EquipmentFactorValue(id)] FROM [EquipmentFactorValue] "
                      + "WHERE [EquipmentFactorValue(equipment)]="+equipId+" AND [EquipmentFactorValue(factor)]="+factorId);
              if(data.isEmpty()) {
                session.executeUpdate("INSERT INTO [EquipmentFactorValue]([EquipmentFactorValue(equipment)],"
                        + "[EquipmentFactorValue(factor)],[EquipmentFactorValue(name)]) VALUES(?,?,?)", new Object[]{equipId,factorId,value});
                data = session.executeQuery("SELECT MAX(id) FROM [EquipmentFactorValue]");
                session.addEvent(EquipmentFactorValue.class, "CREATE", (Integer) data.get(0).get(0));
              }else {
                session.executeUpdate("UPDATE [EquipmentFactorValue] SET [EquipmentFactorValue(name)]=? "
                        + "WHERE [EquipmentFactorValue(equipment)]="+equipId+" AND [EquipmentFactorValue(factor)]="+factorId, new Object[]{value});
                session.addEvent(EquipmentFactorValue.class, "UPDATE", (Integer) data.get(0).get(0));
              }
              session.commit();
            }catch(Exception ex) {
              ObjectLoader.rollBackSession(session);
              Messanger.showErrorMessage(ex);
            }
          }
        }
      }
    });
  }
  
  public RemoveAction getRemoveActionType() {
    return removeActionType;
  }

  /*public void setRemoveActionType(RemoveAction removeActionType) {
    this.removeActionType = removeActionType;
  }*/

  private void removeEquipments() {
    try {
      if(isEditable()) {
        Integer[] ids = this.getSelectedId();
        if(ids.length > 0) {
          switch(getRemoveActionType()) {
            case DELETE:
              if(JOptionPane.showConfirmDialog(
                getRootPanel(),
                "<html>Вы уверены в том что хотите <b>удалить</b> выделенны"+(ids.length>1?"е":"й")+" объект"+(ids.length>1?"ы":"")+"?</html>",
                "Подтверждение удаления",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == 0)
                ObjectLoader.removeObjects(Equipment.class, true, ids);
              break;
            case MOVE_TO_ARCHIVE:
              if(JOptionPane.showConfirmDialog(
                getRootPanel(),
                "Вы действительно хотите отправить в архив выделенны"+(ids.length>1?"е":"й")+" объект"+(ids.length>1?"ы":"")+"?",
                "Отправка в архив",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == 0)
                ObjectLoader.toTypeObjects(Equipment.class, true, MappingObject.Type.ARCHIVE, ids);
              break;
            case MARK_FOR_DELETE:
              if(JOptionPane.showConfirmDialog(
                getRootPanel(),
                "Вы уверены в том что хотите удалить выделенны"+(ids.length>1?"е":"й")+" объект"+(ids.length>1?"ы":"")+"?",
                "Подтверждение удаления",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == 0)
                ObjectLoader.toTmpObjects(Equipment.class, true, ids, true);
              break;
          }
        }
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  private void insertEquipments(Integer[] ids) {
    try {
      DivisionTableModel model = table.getTableModel();
      for(int i=0;i<model.getRowCount();i++)
        if(ArrayUtils.contains(ids, model.getValueAt(i, 0)))
          ids = (Integer[]) ArrayUtils.removeElement(ids, model.getValueAt(i, 0));
      
      if(ids.length > 0) {
        //Получаю список экземпляров
        for(List d:ObjectLoader.getData(Equipment.class, ids, new String[]{"id","group","group_name","amount","date"})) {
          Vector row = new Vector(d);
          row.setSize(table.getColumnCount());
          model.addRow(row);
        }
        //Получаю значения общих реквизитов для всех экземпляров
        Integer equipId;
        Integer factorId;
        String  equipFactorValueName;
        FactorType  factorType;
        TreeMap<Integer,TreeMap<Integer,String>> equipListValues = new TreeMap<>();
        for(List d:ObjectLoader.getData(DBFilter.create(EquipmentFactorValue.class).AND_IN("equipment", ids).AND_IN("factor", factorIds),
                new String[]{"equipment","factor","name","factor_factorType"})) {
          equipId              = (Integer) d.get(0);
          factorId             = (Integer) d.get(1);
          equipFactorValueName = (String)  d.get(2);
          factorType           = FactorType.valueOf((String) d.get(3));
          
          int columnIndex = ArrayUtils.indexOf(factorIds, factorId) + model.getColumnCount()-factorIds.length;
          Vector rowData;
          rowData = (Vector) model.getDataVector().get(getRow(equipId));
          
          Object value = equipFactorValueName;
          if(value != null) {
            switch(factorType) {
              case число:
                value = Double.valueOf(value.toString());
                break;
              case текст:
                value = value.toString();
                break;
              case адрес:
                break;
              default:break;
            }
            
            if(listFactorIds.containsKey(factorId)) {
              TreeMap<Integer,String> mapValues = equipListValues.get(equipId);
              if(mapValues == null)
                equipListValues.put(equipId, mapValues = new TreeMap<>());
              mapValues.put(factorId,equipFactorValueName);
            }
            
            rowData.set(columnIndex, value);
          }
        }
        
        for(int i=0;i<model.getRowCount();i++) {
          equipId = (Integer) model.getValueAt(i, 0);
          for(Integer fId:listFactorIds.keySet()) {
            int columnIndex = ArrayUtils.indexOf(factorIds, fId) + model.getColumnCount()-factorIds.length;
            JComboBox value = new JComboBox(listFactorIds.get(fId).split(";"));
            
            String selectedValue = null;
            if(equipListValues.containsKey(equipId)) {
              TreeMap<Integer,String> mapValues = equipListValues.get(equipId);
              if(mapValues != null && mapValues.containsKey(fId)) {
                selectedValue = mapValues.get(fId);
              }
            }
            
            value.setSelectedItem(selectedValue);
            
            value.addItemListener(new ItemListener() {
              @Override
              public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED && table.getCellEditor() != null)
                  table.getCellEditor().stopCellEditing();
              }
            });
            
            model.setValueAt(value, i, columnIndex);
          }
        }
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  private void removeEquipmentFromTable(Integer[] ids) {
    DivisionTableModel model = table.getTableModel();
    for(int i=model.getRowCount()-1;i>=0;i--)
      if(ArrayUtils.contains(ids, model.getValueAt(i, 0)))
        model.removeRow(i);
  }
  
  private int getRow(Integer equipmentId) {
    DivisionTableModel model = table.getTableModel();
    for(int i=0;i<model.getRowCount();i++)
      if(equipmentId.equals(model.getValueAt(i, 0)))
        return i;
    return -1;
  }
  
  public Integer[] getAllIds() {
    Integer[] ids = new Integer[0];
    Vector<Vector> data = table.getTableModel().getDataVector();
    for(Vector row:data)
      ids = (Integer[]) ArrayUtils.add(ids, row.get(0));
    return ids;
  }

  public Integer[] getSelectedId() {
    Integer[] ids = new Integer[0];
    int[] rows = table.getSelectedRows();
    for(int i=0;i<rows.length;i++)
      ids = (Integer[]) ArrayUtils.add(ids, table.getValueAt(rows[i], 0));
    return ids;
  }

  public MappingObject[] getSelectedObjects() {
    try {
      return ObjectLoader.getObjects(Equipment.class, getSelectedId());
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    return new MappingObject[0];
  }

  private void createEquipments() {
    RemoteSession session = null;
    try {
      List<List> rez = ObjectLoader.executeQuery("SELECT id, name FROM [Group]"+
              " WHERE tmp=false AND type=? "
              + (groups.length>0?"AND [Group(id)]=ANY(?) ":"")
              + "ORDER BY name", true, groups.length>0?new Object[]{"CURRENT",groups}:new Object[]{"CURRENT"});

      if(!rez.isEmpty()) {
        JSpinner countEquiup = new JSpinner(new SpinnerNumberModel(1,1,Integer.MAX_VALUE,1));
        JCheckBox split = new JCheckBox("Разделить объекты", true);
        DivisionComboBox comboBox = new DivisionComboBox();
        comboBox.setPreferredSize(new Dimension(150, 20));

        for(List g:rez)
          comboBox.addItem(new GroupItem((Integer)g.get(0), (String)g.get(1)));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(comboBox,    new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 5, 0), 0, 0));
        panel.add(countEquiup, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 5, 0), 0, 0));
        panel.add(split,       new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 5, 0), 0, 0));

        if(JOptionPane.showConfirmDialog(getRootPanel(), panel, "Создание экземпляра", JOptionPane.OK_CANCEL_OPTION) == 0) {
          session = ObjectLoader.createSession();
          
          int count = (Integer)countEquiup.getModel().getValue();
          
          int startId = 0;
          rez = session.executeQuery("SELECT MAX(id) FROM [Equipment]");
          if(!rez.isEmpty())
            startId = (Integer) rez.get(0).get(0);
          String[] sqls = new String[0];
          for(int i=0;i<(split.isSelected()?count:1);i++)
            sqls = (String[]) ArrayUtils.add(sqls, "INSERT INTO [Equipment]([!Equipment(store)], [!Equipment(group)], [!Equipment(amount)]) VALUES ("
                    +getStore()+","+((GroupItem)comboBox.getSelectedItem()).getId()+","+(split.isSelected()?"1":count)+")");
          session.executeUpdate(sqls);
          Integer[] ids = new Integer[0];
          rez = session.executeQuery("SELECT id FROM [Equipment] WHERE id>"+startId);
          for(List r:rez)
            ids = (Integer[]) ArrayUtils.add(ids, r.get(0));
          session.addEvent(Equipment.class, "CREATE", ids);
          session.commit();
        }
      }else JOptionPane.showMessageDialog(getRootPanel(), "Выберите объект", "!!!", JOptionPane.WARNING_MESSAGE);
    }catch(Exception ex) {
      ObjectLoader.rollBackSession(session);
      Messanger.showErrorMessage(ex);
    }
  }
  
  public void setStoreType(StoreType[] storeTypes) {
    this.storeTypes = storeTypes;
  }

  public Integer[] getNotInEquipIds() {
    return notInEquipIds;
  }

  public void setNotInEquipIds(Integer[] notInEquipIds) {
    if(notInEquipIds == null)
      notInEquipIds = new Integer[0];
    this.notInEquipIds = notInEquipIds;
  }

  public Integer[] getGroups() {
    return groups;
  }

  public void setGroups(Integer[] groups) {
    this.groups = groups;
    add.setEnabled(groups.length > 0);
  }

  public Integer getOwner() {
    return partitionId;
  }

  public void setOwner(Integer owner) {
    this.partitionId = owner;
  }
  
  public Integer getStore() {
    return this.storeId;
  }
  
  public void initDefaultStore() {
    if(getOwner() != null) {
      DBFilter filter = DBFilter.create(Store.class).AND_EQUAL("companyPartition", partitionId).AND_EQUAL("tmp", false).AND_EQUAL("type", Store.Type.CURRENT);
      if(storeTypes != null && storeTypes.length > 0)
        filter.AND_IN("storeType", storeTypes);
      List<List> data = ObjectLoader.getData(
              filter, 
              new String[]{"id","name","main","storeType"}, 
              new String[]{"name"});
      for(List d:data) {
        if((boolean)d.get(2)) {
          setStore((Integer) d.get(0), d.get(3)+(d.get(1)!=null&&!d.get(1).equals("")?" - "+d.get(1):""));
          break;
        }
      }
      if(storeId == null && !data.isEmpty())
        setStore((Integer) data.get(0).get(0), data.get(0).get(3)+(data.get(0).get(1)!=null&&!data.get(0).get(1).equals("")?" - "+data.get(0).get(1):""));
    }
  }

  @Override
  public Boolean okButtonAction() {
    fireSelectObjects(this, getSelectedId());
    dispose();
    return true;
  }

  public void clearSelection() {
    table.clearSelection();
  }

  @Override
  public void closeDialog() {
    clearSelection();
    super.closeDialog();
  }

  public Integer[] get() {
    JDialog d = createDialog();
    d.setModal(true);
    d.setVisible(true);
    return getSelectedId();
  }

  public MappingObject[] getObjects() {
    Integer[] ids = get();
    if(ids.length > 0) {
      try {
        return ObjectLoader.getObjects(Equipment.class, ids);
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
    return null;
  }

  public MappingObject getObject() {
    setSingleSelection(true);
    MappingObject[] objects = getObjects();
    if(objects != null && objects.length > 0)
      return objects[0];
    return null;
  }

  public void setSingleSelection(boolean singleSelection) {
    if(singleSelection)
      table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    else table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
  }

  private class GroupItem {
    private Integer id;
    private String name;
    
    public GroupItem(Integer id, String name) {
      this.id = id;
      this.name = name;
    }
    
    public Integer getId() {
      return id;
    }
    
    public String getName() {
      return name;
    }
    
    public Group getGroup() throws Exception {
      return (Group) ObjectLoader.getObject(Group.class, getId());
    }
    
    @Override
    public String toString() {
      return getName();
    }
  }
  
  @Override
  public void initData() {
    if(initDataTask != null) {
      initDataTask.setShutDown(true);
      initDataTask = null;
    }
    pool.submit(initDataTask = new InitDataTask());
  }
  
  class InitDataTask implements Runnable {
    private boolean shutDown = false;

    public boolean isShutDown() {
      return shutDown;
    }

    public void setShutDown(boolean shutDown) {
      this.shutDown = shutDown;
    }
    
    @Override
    public void run() {
      if(isShutDown())
        return;
      
      List<List> groupData = null, equipData = null, data = null;
      if(groups != null && groups.length > 0 && partitionId != null && storeId != null && !isShutDown()) {
        waitCursor();
        try {
          if(isShutDown())
            return;
          groupData = ObjectLoader.executeQuery("SELECT "
                  + "[Group(factors):object],"
                  + "[Group(factors):target], "
                  + "(SELECT [Factor(name)] FROM [Factor] WHERE [Factor(id)]=[Group(factors):target]), "
                  + "(SELECT [Factor(listValues)] FROM [Factor] WHERE [Factor(id)]=[Group(factors):target]), "
                  + "(SELECT [Factor(factorType)] FROM [Factor] WHERE [Factor(id)]=[Group(factors):target]) "
                  + "FROM [Group(factors):table] "
                  + "WHERE [Group(factors):object]=ANY(?) "
                  + "ORDER BY (SELECT [Factor(unique)] FROM [Factor] WHERE [Factor(id)]=[Group(factors):target]) DESC, (SELECT [Factor(name)] FROM [Factor] WHERE [Factor(id)]=[Group(factors):target])", true, new Object[]{groups});
          equipData = ObjectLoader.executeQuery("SELECT "
                  + "[Equipment(id)],"
                  + "[Equipment(group)],"
                  + "[Equipment(group_name)],"
                  + "[Equipment(amount)],"
                  + "[Equipment(date)] "
                  + "FROM [Equipment] WHERE [Equipment(store)]="+storeId+" AND tmp=false AND type='CURRENT' AND "
                  + "[Equipment(group)]=ANY(?) AND [Equipment(companyPartition)]=?"
                  + (getNotInEquipIds().length>0?" AND [Equipment(id)]<>ALL(?)":""), true,
                  getNotInEquipIds().length>0?new Object[]{groups, partitionId, getNotInEquipIds()}:new Object[]{groups, partitionId});
          data = ObjectLoader.executeQuery("SELECT "
            /*0*/+ "[EquipmentFactorValue(equipment)],"
            /*1*/+ "[EquipmentFactorValue(factor)],"
            /*2*/+ "[EquipmentFactorValue(name)], "
            /*3*/+ "[EquipmentFactorValue(factor_factorType)] "
                  + "FROM [EquipmentFactorValue] "
                  + "WHERE "

                  + "[EquipmentFactorValue(factor)] IN "
                  + "(SELECT DISTINCT [Group(factors):target] FROM [Group(factors):table] WHERE [Group(factors):object]=ANY(?)) AND "

                  + "[EquipmentFactorValue(factor_tmp)]=false AND "
                  + "[EquipmentFactorValue(factor_type)]='CURRENT' AND "

                  + "[EquipmentFactorValue(equipment)] IN "                
                  + "(SELECT [Equipment(id)] FROM [Equipment] "
                  + "WHERE [Equipment(store)]=? AND tmp=false AND type='CURRENT' AND [Equipment(group)]=ANY(?) AND [Equipment(companyPartition)]=?"
                  + (getNotInEquipIds().length>0?" AND [Equipment(id)]<>ALL(?) ":"")+") "

                  + "ORDER BY [EquipmentFactorValue(factor_unique)] DESC, [EquipmentFactorValue(factor)]", true, 
                  getNotInEquipIds().length>0?new Object[]{groups, storeId, groups, partitionId, getNotInEquipIds()}:new Object[]{groups, storeId, groups, partitionId});
          if(isShutDown())
            return;
        }catch (Exception e) {
          Messanger.showErrorMessage(e);
        }finally {
          defaultCursor();
        }
      }
      
      if(groupData != null && equipData != null && data != null) {
        final List<List> fgroupData = groupData;
        final List<List> fequipData = equipData;
        final List<List> fdata = data;
        SwingUtilities.invokeLater(() -> {
          if(isShutDown())
            return;
          waitCursor();
          table.getTableFilters().removeAllFilters();
          table.clear();
          table.setColumns("id","groupId","Группа","Колличество","Дата поступления");
          table.setColumnWidthZero(0,1);
          factorIds = new Integer[0];
          factorTypes = new Factor.FactorType[0];
          groupFactors.clear();
          listFactorIds.clear();
          table.setEnabled(false);
          DivisionTableModel model = table.getTableModel();
          Integer groupId;
          Integer factorId;
          String  factorName;
          String  factorListValues;
          FactorType  factorType;
          for(List d:fgroupData) {
            groupId          = (Integer) d.get(0);
            factorId         = (Integer) d.get(1);
            factorName       = (String)  d.get(2);
            factorListValues = (String)  d.get(3);
            factorType       = FactorType.valueOf((String)  d.get(4));
            
            List<Integer> factors = groupFactors.get(groupId);
            if(factors == null)
              groupFactors.put(groupId, factors = new ArrayList<>());
            factors.add(factorId);
            
            if(!ArrayUtils.contains(factorIds, factorId)) {
              factorIds = (Integer[]) ArrayUtils.add(factorIds, factorId);
              factorTypes = (FactorType[]) ArrayUtils.add(factorTypes, factorType);
              model.addColumn(factorName);
            }
            
            if(factorListValues != null && !factorListValues.equals("") && !listFactorIds.containsKey(factorId))
              listFactorIds.put(factorId, factorListValues);
          }
          
          if(isShutDown())
            return;
          
          for(List d:fequipData) {
            Vector row = new Vector(d);
            row.setSize(model.getColumnCount());
            model.addRow(row);
          }
          
          Integer equipId;
          String  equipFactorValueName;
          TreeMap<Integer,TreeMap<Integer,String>> equipListValues = new TreeMap<>();
          for(List d:fdata) {
            equipId              = (Integer) d.get(0);
            factorId             = (Integer) d.get(1);
            equipFactorValueName = (String)  d.get(2);
            factorType           = FactorType.valueOf((String)  d.get(3));
            
            int columnIndex = ArrayUtils.indexOf(factorIds, factorId) + model.getColumnCount()-factorIds.length;
            Vector rowData;
            rowData = (Vector) model.getDataVector().get(getRow(equipId));
            
            Object value = equipFactorValueName;
            if(value != null) {
              switch(factorType) {
                case число:
                  value = Double.valueOf(value.toString());
                  break;
                case текст:
                  value = value.toString();
                  break;
                case адрес:
                  /*try {
                  if(!value.toString().equals(""))
                  value = JSONObject.fromObject(value).getString("system-address");
                  }catch(Exception ex) {
                  System.out.println(ex.getMessage());
                  }*/
                  break;
                default:break;
              }
              
              if(listFactorIds.containsKey(factorId)) {
                TreeMap<Integer,String> mapValues = equipListValues.get(equipId);
                if(mapValues == null)
                  equipListValues.put(equipId, mapValues = new TreeMap<>());
                mapValues.put(factorId,equipFactorValueName);
              }
              
              rowData.set(columnIndex, value);
            }
          }
          
          if(isShutDown())
            return;
          
          for(int i=0;i<model.getRowCount();i++) {
            equipId = (Integer) model.getValueAt(i, 0);
            for(Integer fId:listFactorIds.keySet()) {
              int columnIndex = ArrayUtils.indexOf(factorIds, fId) + model.getColumnCount()-factorIds.length;
              JComboBox value = new JComboBox(listFactorIds.get(fId).split(";"));
              
              String selectedValue = null;
              if(equipListValues.containsKey(equipId)) {
                TreeMap<Integer,String> mapValues = equipListValues.get(equipId);
                if(mapValues != null && mapValues.containsKey(fId)) {
                  selectedValue = mapValues.get(fId);
                }
              }
              
              value.setSelectedItem(selectedValue);
              
              value.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                  if(e.getStateChange() == ItemEvent.SELECTED && table.getCellEditor() != null)
                    table.getCellEditor().stopCellEditing();
                }
              });
              
              model.setValueAt(value, i, columnIndex);
            }
          }
          
          if(isShutDown())
            return;
          
          table.setColumnWidthZero(new int[]{0,1});
          table.setEnabled(true);
          if(table.getRowCount() > 0) {
            table.getTableFilters().addListFilter(2);
            table.getTableFilters().addNumberFilter(3);
            table.getTableFilters().addDateFilter(4);
            for(int i=5;i<table.getColumnCount();i++) {
              Class clazz = null;
              Object value = null;
              for(int j=0;j<table.getRowCount();j++) {
                value = table.getValueAt(j, i);
                clazz = value==null?null:value.getClass();
                if(clazz != null)
                  break;
              }
              
              if(clazz != null) {
                if(clazz == String.class)
                  table.getTableFilters().addTextFilter(i);
                else if(clazz == Double.class || clazz == Float.class || clazz == Integer.class || clazz == Long.class || clazz == BigDecimal.class)
                  table.getTableFilters().addNumberFilter(i);
                else if(clazz == Date.class)
                  table.getTableFilters().addDateFilter(i);
                if(clazz == java.sql.Date.class)
                  table.getTableFilters().addDateFilter(i);
                else if(clazz == JComboBox.class)
                  table.getTableFilters().addListFilter(i);
                else table.getTableFilters().addTextFilter(i);
              }else table.getTableFilters().addTextFilter(i);
            }
            if(table.getRowCount() == 1)
              table.setRowSelectionInterval(0, 0);
          }
          defaultCursor();
        });
      }
    }
  }
  
  
  
  
  
  
  
  private void setStore(Integer storeId, String storeName) {
    this.storeId = storeId;
    storeBorder.setTitle(storeName);
    initData();
  }
  
  private JPopupMenu createStoreMenu() {
    JPopupMenu menu = new JPopupMenu();
    try {
      DBFilter filter = DBFilter.create(Store.class).AND_NOT_EQUAL("id", storeId).AND_EQUAL("companyPartition", partitionId).AND_EQUAL("tmp", false).AND_EQUAL("type", Store.Type.CURRENT);
      if(storeTypes != null && storeTypes.length > 0)
        filter.AND_IN("storeType", storeTypes);
      List<List> data = ObjectLoader.getData(
              filter, 
              new String[]{"id","name","main","storeType"}, 
              new String[]{"storeType","name"});
      
      for(final List d:data) {
        final JMenuItem m = new JMenuItem(d.get(3)+(d.get(1)!=null&&!d.get(1).equals("")?" - "+d.get(1):""));
        menu.add(m);
        
        m.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            setStore((Integer) d.get(0), m.getText());
          }
        });
      }
    }catch (Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    if(storeTypes != null && storeTypes.length == 1) {
      menu.addSeparator();
      JMenuItem list = new JMenuItem("Редактировать список");
      list.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          showStoreTableEditor();
        }
      });
      menu.add(list);

      JMenuItem add = new JMenuItem("Создать и выбрать");
      add.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          String storeName = JOptionPane.showInternalInputDialog(EquipmentTableEditor.this.getGUI(), "", "", JOptionPane.QUESTION_MESSAGE);
          if(storeName != null) {
            Map<String, Object> map = new TreeMap<>();
            map.put("name", storeName);
            map.put("storeType", storeTypes[0]);
            map.put("companyPartition", partitionId);
            setStore(ObjectLoader.createObject(Store.class, map), storeTypes[0]+" - "+storeName);
          }
        }
      });

      menu.addSeparator();
      menu.add(add);
    }
    return menu;
  }
  
  private void showStoreTableEditor() {
    final TableEditor storeTableEditor = new TableEditor(
            new String[]{"id","Наименование"},
            new String[]{"id","name"},
            Store.class,
            null,
            "");
    
    storeTableEditor.setAddFunction(true);
    storeTableEditor.setAutoLoad(true);
    storeTableEditor.setAutoStore(true);
    storeTableEditor.getTable().setColumnEditable(1, true);
    
    storeTableEditor.setAddAction(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Map<String,Object> map = new TreeMap<>();
        map.put("name", "Новый объект");
        map.put("storeType", storeTypes[0]);
        map.put("companyPartition", partitionId);
        ObjectLoader.createObject(Store.class, map);
      }
    });
    
    storeTableEditor.getTable().getTableModel().addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(TableModelEvent e) {
        if(e.getType() == TableModelEvent.UPDATE && e.getColumn() > 0) {
          try {
            if(ObjectLoader.executeUpdate("UPDATE [Store] SET [!Store(name)]=? WHERE id=?", 
                    (String) storeTableEditor.getTable().getTableModel().getValueAt(e.getFirstRow(), 1),
                    (Integer) storeTableEditor.getTable().getTableModel().getValueAt(e.getFirstRow(), 0), true) > 0)
              ObjectLoader.sendMessage(Store.class, "UPDATE", (Integer) storeTableEditor.getTable().getTableModel().getValueAt(e.getFirstRow(), 0));
          }catch (Exception ex) {
            Messanger.showErrorMessage(ex);
          }
        }
      }
    });
    
    storeTableEditor.getClientFilter().clear().AND_EQUAL("companyPartition", partitionId).AND_EQUAL("storeType", storeTypes[0]);
    storeTableEditor.initData();
    storeTableEditor.createDialog().setVisible(true);
  }
}