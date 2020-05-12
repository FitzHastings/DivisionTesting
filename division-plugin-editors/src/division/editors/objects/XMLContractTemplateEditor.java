package division.editors.objects;

import bum.editors.EditorGui;
import bum.editors.EditorListener;
import division.swing.guimessanger.Messanger;
import bum.editors.MainObjectEditor;
import bum.editors.TableEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.*;
import bum.interfaces.ProductDocument.ActionType;
import division.editors.products.DurationReccurencePanel;
import division.swing.*;
import division.util.Utility;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.text.PlainDocument;
import mapping.MappingObject;
import org.apache.batik.util.gui.xmleditor.XMLEditorKit;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;

public class XMLContractTemplateEditor extends MainObjectEditor {
  private final DivisionTextField    name                = new DivisionTextField("Наименование...");
  private final DivisionTextArea     description         = new DivisionTextArea("Описание...");
  private final JScrollPane          descScroll          = new JScrollPane(description);
  private final DivisionComboBox     sellerNickname      = new DivisionComboBox();
  private final DivisionToolButton   newSellerNickname   = new DivisionToolButton("Создать");
  private final DivisionComboBox     customerNickname    = new DivisionComboBox();
  private final DivisionToolButton   newCustomerNickname = new DivisionToolButton("Создать");

  private final JPanel               durationPanel       = new JPanel(new GridBagLayout());
  private final JSpinner             durationCount       = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
  private final DivisionComboBox     durationType        = new DivisionComboBox();

  private final JCheckBox invoiceForContract = new JCheckBox("Учёт платежей по договорам");
  
  private final JEditorPane editor = new JEditorPane();
  private final DivisionScrollPane scroll = new DivisionScrollPane(editor);
  
  private final JSplitPane split = new JSplitPane();
  
  private final TableEditor processTableEditor = new TableEditor(
          new String[]{"id","наименование"}, 
          new String[]{"id","name"}, Service.class, null, Service.Type.CURRENT);
  
  private final TableEditor dependedProcessTable = new TableEditor(
          new String[]{"id", "через", "после события","","в процессе"}, 
          new String[]{"id", "delay", "actionType",   "process-name","process"}, 
          DependentProcess.class, null, DependentProcess.Type.CURRENT) {
            
            @Override
            public Object filterValue(Object value) {
              return value instanceof DivisionItem ? ((DivisionItem)value).getId() : value;
            }

            @Override
            protected Object transformData(int modelRow, int modelColumn, Object object) {
              if(modelColumn == 2) {
                DivisionComboBox actionCombobox = createBox(ActionType.СТАРТ.toString(), ActionType.ОТГРУЗКА.toString(), ActionType.ОПЛАТА.toString());
                actionCombobox.setSelectedItem(object);
                return actionCombobox;
              }
              if(modelColumn == 4) {
                DivisionItem[] items = new DivisionItem[0];
                for(List p:(Vector<Vector>)processTableEditor.getTable().getTableModel().getDataVector())
                  items = (DivisionItem[]) ArrayUtils.add(items, new DivisionItem((Integer)p.get(0), (String)p.get(1), "Service"));
                DivisionComboBox processCombobox = createBox(items);
                processCombobox.setSelectedItem(object);
                return processCombobox;
              }
              return super.transformData(modelRow, modelColumn, object);
            }
            
            /*@Override
            protected void insertData(Vector<Vector> data, int startIndex) {
              data.stream().forEach(d -> {
                DivisionComboBox actionCombobox = createBox(ActionType.СТАРТ.toString(), ActionType.ОТГРУЗКА.toString(), ActionType.ОПЛАТА.toString());
                actionCombobox.setSelectedItem((String)d.get(2));
                d.set(2, actionCombobox);
                
                DivisionItem[] items = new DivisionItem[0];
                for(Vector p:(Vector<Vector>)processTableEditor.getTable().getTableModel().getDataVector())
                  items = (DivisionItem[]) ArrayUtils.add(items, new DivisionItem((Integer)p.get(0), (String)p.get(1), "Service"));
                DivisionComboBox processCombobox = createBox(items);
                processCombobox.setSelectedItem((Integer)d.get(3));
                d.set(4, processCombobox);
              });
              super.insertData(data, startIndex);
            }*/
          };
  
  DurationReccurencePanel delayPanel = new DurationReccurencePanel();
  
  public XMLContractTemplateEditor() {
    super();
    XMLEditorKit editorKit = new XMLEditorKit();
    editor.setEditorKitForContentType(XMLEditorKit.XML_MIME_TYPE, editorKit);
    editor.setContentType(XMLEditorKit.XML_MIME_TYPE);
    editor.getDocument().putProperty(PlainDocument.tabSizeAttribute, new Integer(10));
    editor.setFont(new Font("Monospaced", Font.PLAIN, 12));
    initComponents();
    initEvents();
  }
  
  private DivisionComboBox createBox(Object... items) {
    DivisionComboBox box = new DivisionComboBox(items);
    box.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        if(dependedProcessTable.getTable().getCellEditor() != null)
          dependedProcessTable.getTable().getCellEditor().stopCellEditing();
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
      }
    });
    return box;
  }
  
  private void initComponents() {
    processTableEditor.setAdministration(false);
    processTableEditor.setVisibleOkButton(false);
    
    dependedProcessTable.setVisibleOkButton(false);
    dependedProcessTable.getTable().setColumnEditable(2, true);
    dependedProcessTable.getTable().setColumnEditable(4, true);
    dependedProcessTable.addAutoEditingColumns(2,4);
    dependedProcessTable.getTable().setColumnWidthZero(0,3);
    
    dependedProcessTable.setAddFunction(true);
    dependedProcessTable.setAddAction(e -> {
      try {
        Map<String,Object> map = new TreeMap<>();
        map.put("subProcess", processTableEditor.getSelectedId()[0]);
        map.put("contractTemplate", getEditorObject().getId());
        ObjectLoader.createObject(DependentProcess.class, map,true);
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    });
    
    durationType.setPreferredSize(new Dimension(70, durationType.getPreferredSize().height));
    durationCount.setPreferredSize(new Dimension(120, durationCount.getPreferredSize().height));

    description.setLineWrap(true);
    
    split.add(processTableEditor.getGUI(),  JSplitPane.LEFT);
    split.add(dependedProcessTable.getGUI(), JSplitPane.RIGHT);

    JPanel rekvexits = new JPanel(new GridBagLayout());
    rekvexits.add(split, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 5, 0, 5), 0, 0));

    newSellerNickname.setMaximumSize(new Dimension(20,20));
    newSellerNickname.setMinimumSize(new Dimension(20,20));
    newSellerNickname.setPreferredSize(new Dimension(20,20));
    newCustomerNickname.setMaximumSize(new Dimension(20,20));
    newCustomerNickname.setMinimumSize(new Dimension(20,20));
    newCustomerNickname.setPreferredSize(new Dimension(20,20));
    
    addComponentToStore(split);
    //addSubEditorToStore(tempProcessEditor);
    //addSubEditorToStore(tempProcessTable);

    JTabbedPane pane = new JTabbedPane();
    pane.add("Реквизиты", rekvexits);
    pane.add("Шаблон", scroll);

    
    JPanel sidePanel = new JPanel(new GridBagLayout());
    sidePanel.setBorder(BorderFactory.createTitledBorder("Наименование сторон"));
    sidePanel.add(new JLabel("Продавец"),   new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    sidePanel.add(sellerNickname,           new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
    sidePanel.add(newSellerNickname,        new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    
    sidePanel.add(new JLabel("Покупатель"), new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    sidePanel.add(customerNickname,         new GridBagConstraints(4, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
    sidePanel.add(newCustomerNickname,      new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    
    durationPanel.setBorder(BorderFactory.createTitledBorder("Срок действия по умолчанию"));
    durationPanel.add(durationCount, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    durationPanel.add(durationType,  new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
    
    JPanel namePanel = new JPanel(new GridBagLayout());
    namePanel.setBorder(BorderFactory.createTitledBorder("Наименование"));
    namePanel.add(name, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
    
    descScroll.setMaximumSize(new Dimension(300, 100));
    descScroll.setMinimumSize(new Dimension(300, 100));
    descScroll.setPreferredSize(new Dimension(300, 100));
    
    getRootPanel().add(namePanel,     new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
    getRootPanel().add(durationPanel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    getRootPanel().add(descScroll,    new GridBagConstraints(2, 0, 1, 2, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    getRootPanel().add(sidePanel,     new GridBagConstraints(0, 1, 2, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
    
    getRootPanel().add(pane,          new GridBagConstraints(0, 3, 3, 1, 1.0, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
  }

  private void createNickname(final Class clazz) {
    try {
      final TableEditor nicknameTableEditor = new TableEditor(
              new String[]{"id","Наименование"},
              new String[]{"id","name"},
              clazz,
              null,
              "Псевдонимы",
              MappingObject.Type.CURRENT);
      nicknameTableEditor.setRemoveActionType(MappingObject.RemoveAction.MARK_FOR_DELETE);
      nicknameTableEditor.setSingleSelection(true);
      nicknameTableEditor.getTable().setColumnEditable(1, true);
      nicknameTableEditor.getTable().findTableColumn(1).setCellEditor(new DefaultCellEditor(new JTextField()));
      nicknameTableEditor.getTable().setFindable(false);
      nicknameTableEditor.getTable().getTableModel().addTableModelListener(new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent e) {
          if(e.getLastRow() >= 0 && e.getColumn() >= 0) {
            int column   = nicknameTableEditor.getTable().convertColumnIndexToModel(e.getColumn());
            int row      = nicknameTableEditor.getTable().convertRowIndexToModel(e.getLastRow());
            Object value = nicknameTableEditor.getTable().getModel().getValueAt(row, column);
            if(e.getType() == TableModelEvent.UPDATE && column > 0 && row != -1) {
              if(nicknameTableEditor.getTable().getModel().getValueAt(row, 0).equals(-1)) {
                RemoteSession session = null;
                try {
                  session = ObjectLoader.createSession();
                  int rez = session.executeUpdate("INSERT INTO ["+clazz.getName()+"] "
                          +"(name) VALUES ('"+value.toString()+"')");
                  List<List> data = session.executeQuery("SELECT MAX(id) FROM ["+clazz.getName()+"]");
                  session.addEvent(clazz, "UPDATE", (Integer) data.get(0).get(0));
                  session.commit();
                  if(rez == 1)
                    nicknameTableEditor.getTable().getModel().setValueAt(data.get(0).get(0), row, 0);
                }catch(Exception ex) {
                  ObjectLoader.rollBackSession(session);
                  Messanger.showErrorMessage(ex);
                }
              }else {
                MappingObject nickname = nicknameTableEditor.getSelectedObjects()[0];
                try {
                  nickname.setName(value.toString());
                  ObjectLoader.saveObject(nickname, true);
                } catch (Exception ex) {
                  Messanger.showErrorMessage(ex);
                }
              }
            }
          }
        }
      });

      nicknameTableEditor.setAddAction((ActionEvent e) -> {
        int row = 0;
        int column = 1;
        nicknameTableEditor.getTable().getTableModel().insertRow(row, new Object[]{-1,""});
        boolean success = nicknameTableEditor.getTable().editCellAt(row, column);
        if(success) {
          boolean toggle = false;
          boolean extend = true;
          nicknameTableEditor.getTable().changeSelection(row, column, toggle, extend);
          nicknameTableEditor.getTable().grabFocus();
        }
      });

      nicknameTableEditor.setAutoLoad(true);
      nicknameTableEditor.setAutoStore(true);
      nicknameTableEditor.initData();
      nicknameTableEditor.setAddFunction(true);
      nicknameTableEditor.createDialog(true).setVisible(true);
      if(nicknameTableEditor.getSelectedObjectsCount() > 0) {
        DivisionComboBox combo = customerNickname;
        if(clazz == SellerNickName.class)
          combo = sellerNickname;
        MappingObject o = nicknameTableEditor.getSelectedObjects()[0];
        if(!ArrayUtils.contains(combo.getItems(), o))
          combo.addItem(o);
        combo.setSelectedItem(o);
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  private void initEvents() {
    dependedProcessTable.getTable().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if(dependedProcessTable.getTable().columnAtPoint(e.getPoint()) == 1 && e.getClickCount() == 2) {
          JPopupMenu menu = new JPopupMenu();
          menu.add(delayPanel);
          String  value = (String)  dependedProcessTable.getTable().getValueAt(dependedProcessTable.getTable().rowAtPoint(e.getPoint()), 1);
          Integer id    = (Integer) dependedProcessTable.getTable().getValueAt(dependedProcessTable.getTable().rowAtPoint(e.getPoint()), 0);
          delayPanel.setString(value);
          delayPanel.setButtonActionListener(a -> {
            ObjectLoader.saveObject(DependentProcess.class, id, new SimpleEntry<>("delay", delayPanel.getString()));
            menu.setVisible(false);
          });
          menu.show(dependedProcessTable.getTable(), e.getX(), e.getY());
        }
      }
    });
    
    /*addTarget(new DivisionTarget(SellerNickName.class) {
      @Override
      public void messageReceived(final String type, final Integer[] ids) {
        EventQueue.invokeLater(() -> {
          try {
            MappingObject[] objects = ObjectLoader.getObjects(SellerNickName.class, ids);
            if("CREATE".equals(type)) {
              for(Object o:objects)
                if(!ArrayUtils.contains(sellerNickname.getItems(), o))
                  sellerNickname.addItem(o);
            }else if ("UPDATE".equals(type)) {
              Object o = sellerNickname.getSelectedItem();
              sellerNickname.clear();
              sellerNickname.addItems(ObjectLoader.getObjects(SellerNickName.class));
              sellerNickname.setSelectedItem(o);
            }else if ("REMOVE".equals(type)) {
              sellerNickname.removeItems(ids);
            }
          }catch(Exception ex) {
            Messanger.showErrorMessage(ex);
          }
        });
      }
    });

    addTarget(new DivisionTarget(CompanyNickname.class) {
      @Override
      public void messageReceived(final String type, final Integer[] ids) {
        EventQueue.invokeLater(() -> {
          try {
            MappingObject[] objects = ObjectLoader.getObjects(CompanyNickname.class, ids);
            if("CREATE".equals(type)) {
              for(Object o:objects)
                if(!ArrayUtils.contains(sellerNickname.getItems(), o))
                  customerNickname.addItem(o);
            }else if ("UPDATE".equals(type)) {
              Object o = customerNickname.getSelectedItem();
              customerNickname.clear();
              customerNickname.addItems(ObjectLoader.getObjects(CompanyNickname.class));
              customerNickname.setSelectedItem(o);
            }else if ("REMOVE".equals(type)) {
              customerNickname.removeItems(ids);
            }
          }catch(Exception ex) {
            Messanger.showErrorMessage(ex);
          }
        });
      }
    });*/

    durationCount.addChangeListener((ChangeEvent e) -> {
      int index = durationType.getSelectedIndex();
      Object[] typeItems = new Object[]{"дней","месяцев","лет"};
      int value = (Integer)durationCount.getValue();
      if(value == 0) {
        typeItems = new Object[0];
        index = -1;
      }else if(value != 11 && value%10 == 1)
        typeItems = new Object[]{"день","месяц","год"};
      else if((value > 21 || value < 10) && value%10 < 5 && value%10 > 0)
        typeItems = new Object[]{"дня","месяца","года"};
      durationType.clear();
      durationType.addItems(typeItems);
      if(index >= 0)
        durationType.setSelectedIndex(index);
    });

    newSellerNickname.addActionListener((ActionEvent e) -> {
      createNickname(SellerNickName.class);
    });

    newCustomerNickname.addActionListener((ActionEvent e) -> {
      createNickname(CompanyNickname.class);
    });
    
    processTableEditor.addEditorListener(new EditorListener() {
      @Override
      public void changeSelection(EditorGui editor, Integer[] ids) {
        dependedProcessTable.setEnabled(ids.length == 1);
        if(ids.length == 1) {
          dependedProcessTable.clear();
          try {
            dependedProcessTable.getClientFilter().clear().AND_EQUAL("subProcess", ids[0]).AND_EQUAL("contractTemplate", getEditorObject().getId());
            dependedProcessTable.initData();
          }catch(Exception ex){Messanger.showErrorMessage(ex);}
        }
      }
    });
    
    /*tempProcessTable.addEditorListener(new EditorListener() {
      @Override
      public void changeSelection(EditorGui editor, Integer[] ids) {
        try {
          tempProcessEditor.setActive(tempProcessTable.getSelectedObjectsCount() == 1);
          tempProcessEditor.clear();
          if(tempProcessTable.getSelectedObjectsCount() == 1) {
            tempProcessEditor.setActive(true);
            tempProcessEditor.setEditorObject(tempProcessTable.getSelectedObjects()[0]);
          }
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });*/

    //tempProcessTable.setAddAction((ActionEvent e) -> {
      /*RemoteSession session = null;
      try {
        XMLContractTemplate templ = (XMLContractTemplate)getEditorObject();
        ServiceTableEditor processes = new ServiceTableEditor();
        processes.setAutoLoad(true);
        processes.setAutoStore(true);
        processes.initData();
        processes.setSelectingOnlyLastNode(true);
        MappingObject[] objects = processes.getObjects(true);
        if(objects != null && objects.length > 0) {
          session = ObjectLoader.createSession(true);
          for(MappingObject o:objects) {
            ContractProcess tempProcess = (ContractProcess) session.createEmptyObject(ContractProcess.class);
            tempProcess.setTemplate(templ);
            tempProcess.setProcess((Service)o);
            tempProcess.setTmp(false);
            session.saveObject(tempProcess);
          }
          session.commit();
        }
      }catch(Exception ex) {
        ObjectLoader.rollBackSession(session);
        Messanger.showErrorMessage(ex);
      }*/
    //});
  }
  
  @Override
  public String commit() throws RemoteException {
    String msg = "";
    
    String duration = null;
    if((Integer)durationCount.getValue() > 0) {
      duration = ((Integer)durationCount.getValue())+" "+durationType.getSelectedItem();
      ((XMLContractTemplate)getEditorObject()).setDuration(Utility.convert(duration));
    }else msg += "  Установите лительность типа договора\n";
    
    if(name.getText().equals(""))
      msg += "   Введите наименование\n";
    else ((XMLContractTemplate)getEditorObject()).setName(name.getText());
    
    ((XMLContractTemplate)getEditorObject()).setXML(editor.getText().equals("")?null:editor.getText());
    ((XMLContractTemplate)getEditorObject()).setDescription(description.getText().equals("")?null:description.getText());
    
    if(sellerNickname.getSelectedItem() != null)
      ((XMLContractTemplate)getEditorObject()).setSellerNickname((SellerNickName)ObjectLoader.getObject(SellerNickName.class, ((DivisionItem)sellerNickname.getSelectedItem()).getId()));
    
    if(customerNickname.getSelectedItem() != null)
      ((XMLContractTemplate)getEditorObject()).setCustomerNickname((CompanyNickname)ObjectLoader.getObject(CompanyNickname.class, ((DivisionItem)customerNickname.getSelectedItem()).getId()));
    return null;
  }

  @Override
  public void clearData() {
    name.setText("");
    editor.setText("");
    description.setText("");
  }

  @Override
  public void initData() {
    try {
      XMLContractTemplate template = (XMLContractTemplate)getEditorObject();

      String duration = Utility.format(template.getDuration());
      if(duration != null && !duration.equals("")) {
        durationCount.setValue(Integer.valueOf(duration.split(" ")[0]));
        durationType.setSelectedItem(duration.split(" ")[1]);
      }

      name.setText(template.getName());
      editor.setText(template.getXML());
      description.setText(template.getDescription());

      sellerNickname.clear();
      customerNickname.clear();
      ObjectLoader.getData(SellerNickName.class, "id","name").stream().forEach(d -> sellerNickname.addItem(new DivisionItem((Integer)d.get(0), (String)d.get(1), SellerNickName.class.getName())));
      ObjectLoader.getData(CompanyNickname.class, "id","name").stream().forEach(d -> customerNickname.addItem(new DivisionItem((Integer)d.get(0), (String)d.get(1), CompanyNickname.class.getName())));
      
      SellerNickName  sellerNickName  = template.getSellerNickname();
      CompanyNickname companyNickname = template.getCustomerNickname();
      
      sellerNickname.setSelectedItem(sellerNickName==null?sellerNickName:sellerNickName.getId());
      customerNickname.setSelectedItem(companyNickname==null?companyNickname:companyNickname.getId());

      Boolean is = template.isContractAccounting();
      invoiceForContract.setSelected(is==null?false:is);
      
      processTableEditor.getClientFilter().clear().AND_IN("contractTemplates", new Integer[]{template.getId()});
      processTableEditor.initData();
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  @Override
  public void load() {
    super.load();
  }

  @Override
  public void store() {
    super.store();
  }

  @Override
  public String getEmptyObjectTitle() {
    return "Новый шаблон";
  }

  @Override
  public void dispose() {
    dependedProcessTable.dispose();
    processTableEditor.dispose();
    super.dispose();
  }
}