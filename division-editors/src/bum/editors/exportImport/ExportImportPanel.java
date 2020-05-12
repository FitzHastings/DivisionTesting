package bum.editors.exportImport;

import bum.editors.util.ObjectLoader;
import bum.interfaces.ExportImport;
import division.swing.DivisionComboBox;
import division.swing.guimessanger.Messanger;
import division.swing.ScriptPanel;
import division.swing.DivisionToolButton;
import division.swing.tree.Node;
import division.util.FileLoader;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import javax.swing.*;
import javax.swing.tree.TreeNode;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class ExportImportPanel extends JPanel {
  private JCheckBox scriptRun = new JCheckBox("Использовать скрипт");
  public enum TYPE{EXPORT,IMPORT};
  private TYPE type = TYPE.EXPORT;
  private JTabbedPane    tabb       = new JTabbedPane();

  private ScriptPanel    script     = new ScriptPanel();

  private JSplitPane     split      = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
  private RMIObjectsTree tree       = new RMIObjectsTree();
  private JScrollPane    treeScroll = new JScrollPane(tree);

  private JPanel           listPanel      = new JPanel(new GridBagLayout());
  private DivisionComboBox splitCombo     = new DivisionComboBox("табуляция","пробел",";",":","|","\"","\'","\\","/","!","#");
  private DefaultListModel model          = new DefaultListModel();
  private JList            list           = new JList(model);
  private JScrollPane      listScroll     = new JScrollPane(list);

  private DivisionToolButton saveList = new DivisionToolButton(FileLoader.getIcon("Save16.gif"));
  private DivisionToolButton save     = new DivisionToolButton(FileLoader.getIcon("Save16.gif"));
  private DivisionToolButton down     = new DivisionToolButton(FileLoader.getIcon("down.gif"));
  private DivisionToolButton remove   = new DivisionToolButton(FileLoader.getIcon("Delete16.gif"),"Удалить");

  private ArrayList components = new ArrayList();
  private Class clazz;
  //private Object memento;

  public ExportImportPanel(TYPE type) {
    super(new BorderLayout());
    this.type = type;
    initComponents();
    initEvents();

    split.setName(type+"Split");
    components.add(split);
  }

  public ArrayList getStoreComponents() {
    return components;
  }

  public void initClass(Class clazz) {
    this.clazz = clazz;
    System.out.println("class = "+clazz.getCanonicalName());
    tree.initClass(clazz);
    fillLists(clazz);
  }
  
  private void initComponents() {
    list.setDragEnabled(true);

    add(scriptRun, BorderLayout.NORTH);
    add(tabb, BorderLayout.CENTER);

    tabb.add("Конструктор", split);
    tabb.add("JavaScript",  script);

    split.add(treeScroll, JSplitPane.TOP);
    split.add(listPanel,  JSplitPane.BOTTOM);

    listPanel.add(down,                      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    listPanel.add(saveList,                  new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    listPanel.add(remove,                    new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    listPanel.add(splitCombo,                new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    listPanel.add(listScroll,                new GridBagConstraints(0, 1, 4, 1, 1.0, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    
    script.addToolComponent(save, 0);
  }

  private void initEvents() {
    list.setDropTarget(new DropTarget(list, new ListDropTarget(list)));

    remove.addActionListener((ActionEvent e) -> {
      for(Object item:list.getSelectedValues())
        model.removeElement(item);
    });

    down.addActionListener((ActionEvent e) -> {
      for(Node node:tree.getSelectedLastNodes())
        model.addElement(new ListObject((ObjectNode)node));
    });

    saveList.addActionListener((ActionEvent e) -> {
      Vector<Vector<Map<String, Object>>> fields = new Vector<>();
      for(int i=0;i<model.getSize();i++) {
        Vector<Map<String, Object>> field = new Vector<>();
        ObjectNode node = (ObjectNode)((ListObject)model.get(i)).getNode();
        TreeNode[] nodes = node.getPath();
        for(TreeNode n:nodes)
          if(n instanceof ObjectNode)
            field.add(((ObjectNode)n).getData());
        if(!field.isEmpty())
          fields.add(field);
      }
      
      try {
        RemoteSession session = ObjectLoader.createSession(true);
        ExportImport exportImport = ExportImportClass.getExportImport(clazz);
        if(exportImport == null)
          exportImport = (ExportImport)session.createEmptyObject(ExportImport.class);
        
        exportImport.setObjectClassName(clazz.getName());
        Properties params = new Properties();
        params.put(type+"_FIELDS", fields);
        params.put(type+"_SPLIT",  splitCombo.getSelectedIndex()==0?"\t":splitCombo.getSelectedIndex()==1?" ":splitCombo.getSelectedItem().toString());
        exportImport.setParams(params);
        exportImport.setTmp(false);
        exportImport.setScript(scriptRun.isSelected());
        session.saveObject(exportImport);
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    });

    save.addActionListener((ActionEvent e) -> {
      try {
        RemoteSession session = ObjectLoader.createSession(true);
        ExportImport exportImport;
        Object[] objects = session.getObjects(DBFilter.create(ExportImport.class).AND_EQUAL("objectClassName", clazz.getName()));
        if(objects.length > 0)
          exportImport = (ExportImport)objects[0];
        else exportImport = (ExportImport)session.createEmptyObject(ExportImport.class);
        
        exportImport.setObjectClassName(clazz.getName());
        if(type == TYPE.EXPORT)
          exportImport.setExportScript(script.getText());
        else exportImport.setImportScript(script.getText());
        exportImport.setTmp(false);
        exportImport.setScript(scriptRun.isSelected());
        session.saveObject(exportImport);
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    });
  }

  private void fillLists(Class clazz) {
    try {
      splitCombo.setSelectedIndex(0);
      script.setText("");
      scriptRun.setSelected(false);

      model.clear();
      ExportImport exportImport = ExportImportClass.getExportImport(clazz);
      if(exportImport != null) {
        //memento = exportImport.getMemento();
        scriptRun.setSelected(exportImport.isScript());
        if(type == TYPE.EXPORT)
          script.setText(exportImport.getExportScript());
        else script.setText(exportImport.getImportScript());
        Object eSplit = exportImport.getParams().get(type+"_SPLIT");
        if(eSplit != null) {
          if(eSplit.equals("\t"))
            eSplit = "табуляция";
          else if(eSplit.equals(" "))
            eSplit = "пробел";
        }

        Node root = new Node("", tree);
        Vector<Vector<Map<String, Object>>> exportFields = (Vector<Vector<Map<String, Object>>>)exportImport.getParams().get(type+"_FIELDS");
        if(exportFields != null && !exportFields.isEmpty()) {
          for(Vector<Map<String, Object>> field:exportFields) {
            ObjectNode node = null,child = null;
            for(int i=field.size()-1;i>=0;i--) {
              node = new ObjectNode(tree,field.get(i));
              if(child != null)
                node.add(child);
              child = node;
            }
            root.add(node);
            model.addElement(new ListObject((ObjectNode)node.postorderEnumeration().nextElement()));
          }
        }
      }
    }catch(RemoteException ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  class ListObject {
    private ObjectNode node;

    public ListObject(ObjectNode node) {
      this.node = node;
    }

    public Node getNode() {
      return node;
    }

    private String getString() {
      String str = node.getFieldDescription();
      Node parent = node;
      while(!((Node)parent.getParent()).isRoot()) {
        parent = (Node)parent.getParent();
        str = ((ObjectNode)parent).getFieldDescription()+"->."+str;
      }
      return ((ObjectNode)parent).getObjectClassDescription()+"->"+str;
    }

    @Override
    public String toString() {
      return getString();
    }
  }

  class ListDropTarget implements DropTargetListener {
    private int dragIndex;
    private Object dragValue;
    private JList list;

    public ListDropTarget(JList list) {
      this.list = list;
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
      dragIndex = list.getSelectedIndex();
      dragValue = list.getSelectedValue();
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
      ((DefaultListModel)list.getModel()).removeElementAt(dragIndex);
      dragIndex = list.locationToIndex(dtde.getLocation());
      ((DefaultListModel)list.getModel()).insertElementAt(dragValue, dragIndex);
      list.setSelectedIndex(dragIndex);
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
    }
  }
}