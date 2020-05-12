package division.exportimport;

import bum.editors.EditorGui;
import bum.editors.util.ObjectLoader;
import bum.interfaces.ExportImport;
import division.swing.DivisionComboBox;
import division.swing.guimessanger.Messanger;
import division.swing.DivisionScrollPane;
import division.swing.tree.Node;
import division.swing.tree.Tree;
import division.swing.tree.TreeCellEditor;
import division.swing.tree.TreeCellRenderer;
import division.xml.Document;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.Vector;
import javax.swing.JLabel;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import mapping.MappingObject;
import org.apache.commons.lang3.ArrayUtils;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class ExportImportEditor extends EditorGui {
  private final DivisionComboBox   classComboBox = new DivisionComboBox();
  private final Tree               tree          = new Tree(Tree.Type.CHECKBOXSES);
  private final DivisionScrollPane treeScroll    = new DivisionScrollPane(tree);

  public ExportImportEditor() {
    super(null, null);
    tree.setRoot(new Node("Поля",tree));
    initComponents();
    initEvents();
  }

  private void initComponents() {
    //exportPath.setEditable(false);
    //exportPath.setEnabled(false);
    
    TreeCellRenderer cellRenderer = new TreeCellRenderer();
    TreeCellEditor editor = new TreeCellEditor(tree, cellRenderer);
    
    tree.setCellRenderer(cellRenderer);
    tree.setCellEditor(editor);
    
    tree.setEditable(true);
    
    tree.setShowsRootHandles(true);
    
    getRootPanel().setLayout(new GridBagLayout());

    getRootPanel().add(new JLabel("Объект:"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(classComboBox,         new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(treeScroll,            new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
  }

  private void initEvents() {
    classComboBox.addItemListener((ItemEvent e) -> {
      try {
        Class objectClass = ((DivisionClassItem)e.getItem()).getObjectClass();
        if(e.getStateChange() == ItemEvent.DESELECTED) {
          saveCurrentExportImport(objectClass);
        }else {
          ((Node)tree.getTreeModel().getRoot()).removeAllChildren();
          tree.getTreeModel().reload();
          fillTree((Node)tree.getTreeModel().getRoot(), objectClass);
          
          List<List> data = ObjectLoader.getData(DBFilter.create(ExportImport.class).AND_EQUAL("objectClassName", objectClass.getName()), new String[]{"exportData"});
          if(!data.isEmpty())
            checkedTree((String) data.get(0).get(0));
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    });
    
    tree.addTreeWillExpandListener(new TreeWillExpandListener() {
      @Override
      public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        if(event.getPath().getLastPathComponent() instanceof FieldNode) {
          FieldNode node = (FieldNode)event.getPath().getLastPathComponent();
          if(node.getChildCount() == 1) {
            if(((Node)node.getChildAt(0)).getLabel().getText() == null) {
              fillTree(node,node.getParameterizedType());
              node.remove(0);
            }
          }
        }
      }

      @Override
      public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
      }
    });
  }
  
  @Override
  public void initData() {
    fillClasses();
  }
  
  private void saveCurrentExportImport(Class objectClass) {
    try {
      RemoteSession session = ObjectLoader.createSession(true);
      Document document = new Document(objectClass.getName(), new division.xml.Node(objectClass.getSimpleName()));
      fillDocument(document.getRootNode(),(Node)tree.getTreeModel().getRoot());
      String xml = document.getXML();
      MappingObject[] ei = session.getObjects(DBFilter.create(ExportImport.class).AND_EQUAL("objectClassName", objectClass.getName()));

      if(ei.length == 0) {
        ei = (MappingObject[]) ArrayUtils.add(ei, session.createEmptyObject(ExportImport.class));
        ((ExportImport)ei[0]).setObjectClassName(objectClass.getName());
      }

      if(ei != null && ei.length == 1) {
        ((ExportImport)ei[0]).setExportData("".equals(xml)?null:xml);
        session.saveObject(ei[0]);
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  private void fillDocument(division.xml.Node docParent, Node treeParent) {
    for(Node node:treeParent.getSelectedChilds()) {
      if(!node.equals(treeParent)) {
        if(node.getSelectedChilds().length > 0) {
          division.xml.Node n = new division.xml.Node(((FieldNode)node).getFieldName());
          docParent.addNode(n);
          fillDocument(n, node);
        }else docParent.setAttribute(((FieldNode)node).getFieldName(), "");
      }
    }
  }
  
  private void checkedTree(String xml) {
    System.out.println(xml);
    tree.clearAllCheckBoxes();
    if(xml != null && !xml.equals("")) {
      Document document = Document.loadFromString(xml);
      if(document != null)
        addNode(document.getRootNode());
    }
  }
  
  private void addNode(division.xml.Node node) {
    TreeMap<String,String> attr = node.getAttributes();
    for(String at:attr.keySet()) {
      Node treeNode = getNode(node, at);
      if(treeNode != null)
        treeNode.setSelected(true);
    }
    for(division.xml.Node n:node.getNodes())
      addNode(n);
  }
  
  private Node getNode(division.xml.Node n, String attribute) {
    Node node = null;
    
    String[] fieldPath = new String[]{attribute};
    division.xml.Node parent = n;
    while(parent != null) {
      fieldPath = (String[]) ArrayUtils.add(fieldPath, 0, parent.getName());
      parent = parent.getParent();
    }
    
    Node treeParent = (Node) tree.getTreeModel().getRoot();
    for(int i=0;i<fieldPath.length;i++) {
      if(treeParent.getChildCount() == 1) {
        if(((Node)treeParent.getChildAt(0)).getLabel().getText() == null) {
          fillTree(treeParent,((FieldNode)treeParent).getParameterizedType());
          treeParent.remove(0);
          tree.expandPath(new TreePath(treeParent.getPath()));
        }
      }
      
      for(int j=0;j<treeParent.getChildCount();j++) {
        Node nn = (Node) treeParent.getChildAt(j);
        if(nn instanceof FieldNode && ((FieldNode)nn).getFieldName().equals(fieldPath[i])) {
          if(i == fieldPath.length-1)
            node = nn;
          treeParent = nn;
          break;
        }
      }
    }
    return node;
  }
  
  private void fillTree(Node node, Class objectClass) {
    try {
      if(node == null)
        node = tree.setRoot(ObjectLoader.getClientName(objectClass));

      Map<String, Map<String,Object>> hash = ObjectLoader.getFieldsInfo(objectClass);
      for(String key:hash.keySet()) {
        Map<String, Object> h = hash.get(key);
        FieldNode child = new FieldNode(tree,h);
        node.add(child);
        if(h.containsKey("PARAMETERIZEDTYPE") && instanceofClass((Class)h.get("PARAMETERIZEDTYPE"), MappingObject.class))
          child.add(new Node(null, tree));
      }
      tree.expandRow(0);
    }catch(RemoteException ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  private boolean instanceofClass(Class clazz, Class instanceofClass) {
    if(clazz == instanceofClass)
      return true;
    Class[] interfaces = clazz.getInterfaces();
    for(Class in:interfaces)
      if(in == instanceofClass || instanceofClass(in, instanceofClass))
        return true;
    return false;
  }
  
  private void fillClasses() {
    try {
      classComboBox.clear();
      String key;
      for(Class clazz:ObjectLoader.getServer().getClasses()) {
        key = ObjectLoader.getClientName(clazz);
        if(key != null)
          classComboBox.addItem(new DivisionClassItem(clazz, key));
      }
    }catch(ClassNotFoundException | RemoteException ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  @Override
  public Boolean okButtonAction() {
    Class objectClass = ((DivisionClassItem)classComboBox.getSelectedItem()).getObjectClass();
    saveCurrentExportImport(objectClass);
    dispose();
    return true;
  }

  @Override
  public void clearData() {
  }

  @Override
  public void initTargets() {
  }

  @Override
  public void checkMenu() {
  }
  
  public class DivisionClassItem {
    private Class  objectClass;
    private String classDescription;
    
    public DivisionClassItem(Class objectClass, String classDescription) {
      this.objectClass = objectClass;
      this.classDescription = classDescription;
    }

    public String getClassDescription() {
      return classDescription;
    }

    public Class getObjectClass() {
      return objectClass;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final DivisionClassItem other = (DivisionClassItem) obj;
      if (!Objects.equals(this.objectClass, other.objectClass)) {
        return false;
      }
      if (!Objects.equals(this.classDescription, other.classDescription)) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      int hash = 5;
      hash = 29 * hash + Objects.hashCode(this.objectClass);
      hash = 29 * hash + Objects.hashCode(this.classDescription);
      return hash;
    }

    @Override
    public String toString() {
      return classDescription;
    }
  }
}