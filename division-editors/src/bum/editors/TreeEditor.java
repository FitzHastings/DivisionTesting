package bum.editors;

import bum.editors.util.ObjectLoader;
import bum.interfaces.RMIDBNodeObject;
import division.swing.guimessanger.Messanger;
import division.swing.dnd.ObjectTransferable;
import division.swing.tree.*;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.util.*;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import mapping.MappingObject;
import org.apache.commons.lang3.ArrayUtils;
import util.RemoteSession;

public class TreeEditor extends DBTableEditor {
  protected Tree tree = new Tree(Tree.Type.DND);;
  private DefaultMutableTreeNode dndNode;
  private DefaultMutableTreeNode lastNode;
  private List<List> data = new Vector<>();

  private JMenuItem open = new JMenuItem("Раскрыть дерево");
  private boolean selectingOnlyLastNode = false;

  public TreeEditor(String[] dataFields, String rootName, Class objectClass, Class objectEditorClass, String title, ImageIcon icon, MappingObject.Type type) {
    super(objectClass, objectEditorClass, title, icon, type);
    tree.setRoot(new Node(rootName,tree));
    initTableEvents();
    initTableComponents();

    DragSource dragSource = DragSource.getDefaultDragSource();
    dragSource.createDefaultDragGestureRecognizer(tree,DnDConstants.ACTION_COPY_OR_MOVE,this);
    DropTarget dropTarget = new DropTarget(tree, DnDConstants.ACTION_COPY_OR_MOVE,this,true,null);

    this.setDataFields(dataFields);
    this.setSortFields(new String[]{"id","parent"});

    getPopMenu().addSeparator();
    getPopMenu().add(open);
  }

  public TreeEditor(String rootName, Class objectClass, Class objectEditorClass, String title, ImageIcon icon, MappingObject.Type type) {
    this(new String[]{"id","name","parent"},rootName,objectClass,objectEditorClass,title,icon,type);
  }

  public TreeEditor(String[] dataFields, String rootName, Class objectClass, Class objectEditorClass, String title) {
    this(dataFields,rootName, objectClass, objectEditorClass, title, null, null);
  }

  public TreeEditor(String rootName, Class objectClass, Class objectEditorClass, String title) {
    this(rootName, objectClass, objectEditorClass, title, null, null);
  }
  
  public TreeEditor(String rootName, Class objectClass, Class objectEditorClass, String title, MappingObject.Type type) {
    this(rootName, objectClass, objectEditorClass, title, null, type);
  }
  
  public TreeEditor(String rootName, Class objectClass, Class objectEditorClass, MappingObject.Type type) {
    this(rootName, objectClass, objectEditorClass, "", null, type);
  }

  public TreeEditor(String rootName, Class objectClass, Class objectEditorClass) {
    this(rootName, objectClass, objectEditorClass, "");
  }
  
  @Override
  public int getSelectedObjectsCount() {
    return tree.getSelectionCount();
  }
  
  @Override
  public void setSingleSelection(boolean singleSelection) {
    if(singleSelection)
      getTree().getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    else getTree().getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
  }
  
  @Override
  public boolean isSingleSelection() {
    return getTree().getSelectionModel().getSelectionMode() == TreeSelectionModel.SINGLE_TREE_SELECTION;
  }

  @Override
  public void dragGestureRecognized(DragGestureEvent dge) {
    Node root = (Node)tree.getTreeModel().getRoot();
    ArrayList<Node> objects = new ArrayList<>();
    ArrayList<Node> nodes = new ArrayList(Arrays.asList(tree.getSelectedNodes()));
    nodes.remove(root);
    for(int i=0;i<nodes.size();i++){
      if(!nodes.contains(nodes.get(i).getParent()))
        objects.add(nodes.get(i));
    }

    if(objects.size() > 0) {
      ObjectTransferable transferable =
            new ObjectTransferable(objects.toArray(),getObjectClass());
      dge.startDrag(null,transferable,this);
    }
  }
  
  @Override
  protected boolean dragOver(Point point, List objects, Class interfaceClass) {
    TreePath path = tree.getPathForLocation(point.x, point.y);
    if(path == null)
      return false;
    DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode)path.getLastPathComponent();
    if(!targetNode.equals(lastNode)) {
      tree.repaint();
      lastNode = targetNode;
    }
    if(interfaceClass.equals(getObjectClass())) {
      if(!objects.isEmpty()) {

        for(int i=0;i<objects.size();i++) {
          if(targetNode.equals(objects.get(i)))
            return false;
          Enumeration<Node> em = ((Node)objects.get(i)).preorderEnumeration();
          while(em.hasMoreElements()) {
            if(targetNode.equals((Node)em.nextElement()))
              return false;
          }
        }
        tree.paintPathArrow(path, point.x, point.y);
        return true;
      }
      return false;
    }
    tree.paintPathArrow(path, point.x, point.y);
    return true;
  }

  @Override
  protected void drop(Point point, List objects, Class interfaceClass) {
    TreePath path = tree.getPathForLocation(point.x, point.y);
    if(path != null) {
      Node targetNode = (Node)path.getLastPathComponent();
      if(interfaceClass.equals(getObjectClass())) {
        Integer[] ids = new Integer[0];
        for(Object node:objects)
          if(validate(((bum_Node)node),targetNode))
            ids = (Integer[]) ArrayUtils.add(ids, ((bum_Node)node).getId());
        if(ids.length >= 0) {
          if(JOptionPane.showConfirmDialog(getRootPanel(), "Выполнить перенос?", "Перенос", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == 0) {
            RemoteSession session = null;
            try {
              session = ObjectLoader.createSession(true);
              session.addEvent(getObjectClass(), "UPDATE", ids);
              session.executeUpdate("UPDATE ["+getObjectClass().getName()+"] " +
                "SET ["+getObjectClass().getName()+"(parent)]="+(targetNode.isRoot()?null:((bum_Node)targetNode).getId())+" " +
                "WHERE id=ANY(?)", new Object[]{ids});
            }catch(Exception ex) {
              Messanger.showErrorMessage(ex);
            }
          }
        }
      }
    }
    tree.repaint();
  }
  
  private boolean validate(bum_Node node, Node targetNode) {
    if(node == null)
      return false;
    if(node.equals(targetNode))
      return false;
    if(targetNode.equals(node.getParent()))
      return false;

    Enumeration em = node.preorderEnumeration();
    while(em.hasMoreElements()) {
      bum_Node n = (bum_Node)em.nextElement();
      if(n.equals(targetNode.getParent()))
        return false;
    }
    return true;
  }
  
  /*public void setHierarchyOption(boolean hierarchyOption) {
    hierarchy.setVisible(hierarchyOption);
  }
  
  public void setHierarchy(boolean isHierarchy) {
    hierarchy.setSelected(isHierarchy);
  }*/
  
  public Tree getTree() {
    return this.tree;
  }

  public DefaultTreeModel getTreeModel() {
    return this.tree.getTreeModel();
  }

  protected void initTableEvents() {
    open.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Enumeration<Node> em = ((Node)tree.getTreeModel().getRoot()).preorderEnumeration();
        while(em.hasMoreElements())
          tree.expandPath(new TreePath(em.nextElement().getPath()));
      }
    });

    tree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        if(isEditable() && e.getKeyCode() == KeyEvent.VK_DELETE)
          getRemoveAction().actionPerformed(new ActionEvent(tree, 1, "remove"));
      }
    });

    unSelectMenuItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        tree.clearSelection();
      }
    });
    
    tree.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
        if(e.getModifiers() == MouseEvent.META_MASK) {
          if(path != null) {
            if(tree.getSelectionPaths() != null && tree.getSelectionPaths().length != 1)
              tree.addSelectionPath(tree.getPathForLocation(e.getX(), e.getY()));
            else tree.setSelectionPath(tree.getPathForLocation(e.getX(), e.getY()));
          }
        }else if(e.getClickCount() == 2) {
          if(isDoubleClickSelectable()) {
            okButtonAction();
            //fireSelectObjects();
            //EditorController.getInstance().dispose(TreeEditor.this);
          }else getEditAction().actionPerformed(new ActionEvent(getEditButton(), 0, "edit"));
        }else if(path == null) tree.clearSelection();
      }
      
      @Override
      public void mouseClicked(MouseEvent e) {
        if(tree.isEnabled())
          if(e.getModifiers() == MouseEvent.META_MASK && isPopMenuActive())
            popMenu.show(tree, e.getX(), e.getY());
      }
    });

    tree.addTreeSelectionListener((TreeSelectionEvent e) -> {
      checkMenu();
      fireChangeSelection(this, getSelectedId());
    });

    tree.getTreeModel().addTreeModelListener(new TreeModelListener() {
      @Override
      public void treeNodesChanged(TreeModelEvent e) {
      }

      @Override
      public void treeNodesInserted(TreeModelEvent e) {
        setCountLabel();
      }

      @Override
      public void treeNodesRemoved(TreeModelEvent e) {
        setCountLabel();
      }

      @Override
      public void treeStructureChanged(TreeModelEvent e) {
      }
    });
  }
  
  @Override
  public void checkMenu() {
    TreePath path = tree.getSelectionPath();
    if(this.isEnabled()) {
      if(path == null || ((DefaultMutableTreeNode)path.getLastPathComponent()).isRoot()) {
        editPopMenuItem.setEnabled(false);
        removePopMenuItem.setEnabled(false);
        
        editToolButton.setEnabled(false);
        removeToolButton.setEnabled(false);
      }else if(path != null) {
        editPopMenuItem.setEnabled(true);
        removePopMenuItem.setEnabled(true);
        
        editToolButton.setEnabled(true);
        removeToolButton.setEnabled(true);
      }
    }
  }
  
  protected void initTableComponents() {
    TreeCellRenderer cellRenderer = new TreeCellRenderer();
    TreeCellEditor editor = new TreeCellEditor(tree, cellRenderer);
    
    tree.setCellRenderer(cellRenderer);
    tree.setCellEditor(editor);
    
    tree.setEditable(true);
    
    tree.setShowsRootHandles(true);
    scroll.getViewport().add(tree);
  }

  @Override
  public Boolean okButtonAction() {
    if(isSelectingOnlyLastNode()) {
      for(Node node:tree.getSelectedNodes()) {
        if(node.getChildCount() > 0) {
          JOptionPane.showMessageDialog(null, "Выберите только конечные процессы");
          return false;
        }
      }
    }
    return super.okButtonAction();
  }



  public void setSelectingOnlyLastNode(boolean selectingOnlyLastNode) {
    this.selectingOnlyLastNode = selectingOnlyLastNode;
  }

  public boolean isSelectingOnlyLastNode() {
    return selectingOnlyLastNode;
  }

  @Override
  public void setAdministration(boolean admin) {
    super.setAdministration(admin);
  }

  @Override
  public void addSelectedObjects(Integer[] ids) {
    if(ids.length > 0) {
      for(Integer id:ids) {
        bum_Node node = getNodeObject(id);
        if(node != null)
          tree.addSelectionPath(new TreePath(node.getPath()));
      }
      fireChangeSelection(this, ids);
    }
  }


	public Integer[] getEnableCheckedIds() {
    Integer[] ids = new Integer[0];
		for(Node node:tree.getCheckedNodes()) {
      if(!node.isRoot() && node.isEnabled())
        ids = (Integer[])ArrayUtils.add(ids,((bum_Node)node).getId());
    }
    return ids;
	}

	public MappingObject[] getEnableCheckedObjects() {
		getGUI().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    MappingObject[] objects = new MappingObject[0];
    try {
      objects = ObjectLoader.getObjects(getObjectClass(), getEnableCheckedIds());
    }catch(Exception ex){Messanger.showErrorMessage(ex);}
    getGUI().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    return objects;
	}

  public Integer[] getCheckedIds() {
		Integer[] ids = null;
		for(Node node:tree.getCheckedNodes()) {
      if(!node.isRoot())
        ids = (Integer[])ArrayUtils.add(ids,((bum_Node)node).getId());
			else ids = (Integer[])ArrayUtils.add(ids,null);
    }
    return ids;
  }
  
  public MappingObject[] getCheckedObjects() {
		getGUI().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    MappingObject[] objects = new MappingObject[0];
    try {
      objects = ObjectLoader.getObjects(getObjectClass(), getCheckedIds());
    }catch(Exception ex){Messanger.showErrorMessage(ex);}
    getGUI().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    return objects;
  }
  
  @Override
  public Integer[] getSelectedId() {
    return getSelectedId(tree.isHierarchy());
  }

  public Integer[] getLastId() {
    createDialog(true).setVisible(true);
    return getSelectedLastId();
  }
  
  

  /**
   * Возвращает идентификаторы выделенных объектов
   * @return массив идентификаторов объектов
   */
  public Integer[] getSelectedId(boolean hierarchy) {
    getGUI().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    List<Integer> ids = new ArrayList<>();
    Node[] nodes = tree.getSelectedNodes();
    if(hierarchy && tree.getSelectionBackground().equals(Color.LIGHT_GRAY)) {
      for(Node n:nodes) {
        Enumeration em = n.preorderEnumeration();
        while(em.hasMoreElements()) {
          Node node = (Node)em.nextElement();
          if(!node.isRoot())
            ids.add(((bum_Node)node).getId());
        }
      }
    }else {
      for(Node n:nodes) {
        if(!n.isRoot())
          ids.add(((bum_Node)n).getId());
      }
    }
    getGUI().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    return ids.toArray(new Integer[ids.size()]);
  }
  /**
   * Возвращает выделенные объекты
   * @return массив объектов
   */
  @Override
  public MappingObject[] getSelectedObjects() {
    getGUI().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    MappingObject[] objects = new MappingObject[0];
    try {
      objects = ObjectLoader.getObjects(getObjectClass(), getSelectedId());
    }catch(Exception ex){Messanger.showErrorMessage(ex);}
    getGUI().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    return objects;
  }
  
  /**
   * Возвращает идентификаторы только конечных объектов выделенных групп.
   * @return массив идентификаторов
   */
  public Integer[] getSelectedLastId() {
    getGUI().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    List<Integer> ids = new ArrayList<>();
    Node[] nodes = tree.getSelectedLastNodes();
    for(Node n:nodes)
      if(!n.isRoot())
        ids.add(((bum_Node)n).getId());
    getGUI().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    return ids.toArray(new Integer[ids.size()]);
  }
  /**
   * Возвращает только конечные объекты выделенных групп.
   * @return массив объектов
   */
  public MappingObject[] getSelectedLastObjects() {
    getGUI().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    MappingObject[] objects = new MappingObject[0];
    try {
      objects = ObjectLoader.getObjects(getObjectClass(), getSelectedLastId());
    }catch(Exception ex){Messanger.showErrorMessage(ex);}
    getGUI().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    return objects;
  }

  public Integer[] get(boolean lastIds) {
    createDialog(true).setVisible(true);
    if(lastIds)
      return getSelectedLastId();
    else return getSelectedId();
  }

  public MappingObject[] getObjects(boolean lastIds) {
    Integer[] ids = get(lastIds);
    if(ids.length > 0) {
      try {
        return ObjectLoader.getObjects(getObjectClass(), ids);
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
    return new MappingObject[0];
  }
  
  public void addObject(RMIDBNodeObject parent) {
    try {
      RemoteSession session = ObjectLoader.createSession(true);
      RMIDBNodeObject object = (RMIDBNodeObject)session.createEmptyObject(getObjectClass());
      object.setTmp(true);
      if(parent != null)
        object.setParent(parent);
      if(getTableFilter() != null && !getTableFilter().isEmpty())
        session.toEstablishes(getTableFilter(), object);
      else session.saveObject(object);
      createtedObjects.add(object.getId());
      this.postAddButton(object);
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  @Override
  public void addButtonAction() {
    if(isEditable()) {
      MappingObject parentObject = this.getFirstSelectedObject();
      waitingSelect = true;
      addObject((RMIDBNodeObject)parentObject);
    }
  }
  
  public bum_Node getNodeObject(RMIDBNodeObject object) {
    bum_Node node = null;
    try {
      if(object != null) {
        Node n;
        @SuppressWarnings("unchecked")
        Enumeration<Node> em = ((DefaultMutableTreeNode)tree.getTreeModel().getRoot()).preorderEnumeration();
        while(em.hasMoreElements()) {
          n = em.nextElement();
          if(!n.isRoot() && object.getId().intValue() == ((bum_Node)n).getId().intValue()) {
            node = (bum_Node)n;
            break;
          }
        }
      }
    }catch(Exception ex){Messanger.showErrorMessage(ex);}
    return node;
  }
  
  public bum_Node getNodeObject(Integer id) {
    bum_Node node = null;
    if(id != null) {
      try {
        Node n;
        @SuppressWarnings("unchecked")
        Enumeration<Node> em = ((DefaultMutableTreeNode)tree.getTreeModel().getRoot()).preorderEnumeration();
        while(em.hasMoreElements()) {
          n = em.nextElement();
          if(!n.isRoot() && ((bum_Node)n).getId().intValue() == id.intValue()) {
            node = (bum_Node)n;
            break;
          }
        }
      }catch(Exception ex){Messanger.showErrorMessage(ex);}
    }
    return node;
  }

  /*public bum_Node getNodeObject(RMIDBNodeObject object, bum_Node root) {
    bum_Node node = null;
    if(object != null) {
      Node n;
      @SuppressWarnings("unchecked")
      Enumeration<Node> em = root.preorderEnumeration();
      while(em.hasMoreElements()) {
        n = em.nextElement();
        if(!n.isRoot() && ((bum_Node)n).getObject().equals(object)) {
          node = (bum_Node)n;
          break;
        }
      }
    }
    return node;
  }*/
  
  @Override
  public void clearData() {
    ((DefaultMutableTreeNode)tree.getTreeModel().getRoot()).removeAllChildren();
    tree.getTreeModel().reload();
    data.clear();
  }
  
  @Override
  public void insertData(List data) {
    this.data.addAll(data);
    for(Vector d:(Vector<Vector>) data) {
      insertObject(
              (Integer) d.get(0), 
              d.get(2)==null?null:(Integer) d.get(2), 
              (String)d.get(1));
    }
  }
  
  public List getDataRow(Integer id) {
    for(List d:data) {
      if(d.get(0).equals(id))
        return d;
    }
    
    try {
      List<List> d = ObjectLoader.getData(getObjectClass(), new Integer[]{id}, getDataFields());
      if(!d.isEmpty()) {
        data.add(d.get(0));
        return d.get(0);
      }
    }catch(Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }
  
  public synchronized int insertObject(Integer id, Integer parentId, String name) {
    bum_Node node = getNodeObject(id); //поиск существующего объекта
    if(node != null)// элемент существует
      return -1;
    // элемент отсутствует
    node = new bum_Node(name, id, tree/*, (RMITable)getEditorObject()*/); // создаём элемент
    
    //RMIDBNodeObject parentObject = ((RMIDBNodeObject)object).getParent(); // получаем родительский объект
    if(parentId == null) { // родительского объекта нет, то добавляем новый элемент в верхний узел
      tree.getTreeModel().insertNodeInto(node, (Node)tree.getTreeModel().getRoot(), ((Node)tree.getTreeModel().getRoot()).getChildCount());
    }else { // родительский объект существует
      // получаем элемент дерева соответствующий родительскому объекту
      bum_Node parentNode = getNodeObject(parentId);

      if(parentNode == null) { // родительский элемент отсутствует
        List parentRow = getDataRow(parentId);
        if(parentRow == null) {
          parentRow = getDataRow(parentId);
          System.out.println("PARENT ROW IS NULL "+parentId);
          for(Object row:data)
            System.out.println(row);
        }
        if(parentRow == null) {
          System.out.println("null");
        }
        Integer p = parentRow.get(2)==null?null:Integer.valueOf(String.valueOf(parentRow.get(2)));
        String n = (String)parentRow.get(1);
        insertObject(parentId,p,n); // добавляем родительский объект в дерево (рекурсия)
        parentNode = getNodeObject(parentId);
      }
      // родительский элемент теперь существует
      // добавляем новый элемент в родительский
      tree.getTreeModel().insertNodeInto(node,parentNode,parentNode.getChildCount());
    }
    tree.expandPath(new TreePath(((DefaultMutableTreeNode)tree.getTreeModel().getRoot()).getPath()));
    return -1;
  }
  
  @Override
  public synchronized int insertObject(MappingObject object,int row) throws RemoteException {
    /*bum_Node node = getNodeObject((RMIDBNodeObject)object); //поиск существующего объекта
    if(node != null)// элемент существует
      return -1;
    // элемент отсутствует
    node = new bum_Node((RMIDBNodeObject)object, tree); // создаём элемент
    
    RMIDBNodeObject parentObject = ((RMIDBNodeObject)object).getParent(); // получаем родительский объект
    if(parentObject == null) { // родительского объекта нет, то добавляем новый элемент в верхний узел
      tree.getTreeModel().insertNodeInto(node, (Node)tree.getTreeModel().getRoot(), ((Node)tree.getTreeModel().getRoot()).getChildCount());
    }
    else { // родительский объект существует
      // получаем элемент дерева соответствующий родительскому объекту
      bum_Node parentNode = getNodeObject(parentObject);

      if(parentNode == null) { // родительский элемент отсутствует
        insertObject(parentObject,-1); // добавляем родительский объект в дерево (рекурсия)
        parentNode = getNodeObject(parentObject);
      }
      // родительский элемент теперь существует
      // добавляем новый элемент в родительский
      tree.getTreeModel().insertNodeInto(node,parentNode,parentNode.getChildCount());
    }
    tree.expandPath(new TreePath(((DefaultMutableTreeNode)tree.getTreeModel().getRoot()).getPath()));*/
    return -1;
  }
  
  @Override
  public void createObject(MappingObject object) throws RemoteException {
    insertObject(object,-1);
    if(createtedObjects.contains(object)) {
      bum_Node node = this.getNodeObject((RMIDBNodeObject)object);
      if(node != null) {
        if(waitingSelect) {
          waitingSelect = false;
        }
      }
    }
  }

  public void updateObject(MappingObject object) throws RemoteException {
    bum_Node node = this.getNodeObject((RMIDBNodeObject)object);
    if(node != null) {
      node.setUserObject(object.getName());
      
      Node oldParent = (Node)node.getParent();
      Node newParent = getNodeObject((RMIDBNodeObject)((RMIDBNodeObject)object).getParent());
      if(newParent == null)
        newParent = (Node)getTree().getTreeModel().getRoot();
      if(!newParent.equals(oldParent) && !newParent.equals(node)) {        
        getTree().getTreeModel().removeNodeFromParent(node);
        getTree().getTreeModel().insertNodeInto(node, newParent, 0);
      }
      getTree().getTreeModel().nodeChanged(node);
    }else createObject(object);
  }

  private void updateObject(Node node) throws Exception {
    Vector d = (Vector) ObjectLoader.getData(getObjectClass(), new Integer[]{((bum_Node)node).getId()},getDataFields()).get(0);
    for(int i=0;i<data.size();i++) {
      Vector row = (Vector) data.get(i);
      if(row.get(0).equals(d.get(0)))
        data.set(i, d);
    }
    Node oldParent = (Node)node.getParent();
    Node newParent = getNodeObject((Integer)d.get(2));
    if(newParent == null)
      newParent = (Node)getTree().getTreeModel().getRoot();
    if(!newParent.equals(oldParent)) {
      getTree().getTreeModel().removeNodeFromParent(node);
      getTree().getTreeModel().insertNodeInto(node, newParent, 0);
    }
    node.setUserObject(d.get(1));
    getTree().getTreeModel().nodeChanged(node);
  }

  private void createObject(Integer id) throws Exception {
    Vector d = (Vector) ObjectLoader.getData(getObjectClass(), new Integer[]{id},getDataFields()).get(0);
    insertObject((Integer)d.get(0), (Integer)d.get(2), (String)d.get(1));
    if(createtedObjects.contains(id)) {
      bum_Node node = this.getNodeObject(id);
      if(node != null) {
        if(waitingSelect) {
          //tree.setSelectionPath(new TreePath(node.getPath()));
          waitingSelect = false;
        }//else tree.addSelectionPath(new TreePath(node.getPath()));
      }
    }
  }
  
  /*@Override
  public void removeObject(MappingObject object) throws RemoteException {
    bum_Node node = this.getNodeObject((RMIDBNodeObject)object);
    if(node != null) {
       setActive(false);
      getTree().getTreeModel().removeNodeFromParent(node);
      setActive(true);
      fireChangeSelection();
    }
  }*/
  
  @Override
  public void removeObjects(Integer[] ids) {
    if(ids.length > 0) {
      setActive(false);
      for(int i=ids.length-1;i>=0;i--) {
        bum_Node node = getNodeObject(ids[i]);
        if(node != null) {
          getTree().getTreeModel().removeNodeFromParent(node);
          for(int j=data.size()-1;j>=0;j--)
            if(((Vector)data.get(j)).get(0).equals(ids[i])) {
              data.remove(j);
            }
        }
      }
      setActive(true);
      fireChangeSelection(this, getSelectedId());
    }
  }
  
  @Override
  protected void eventUpdate(Integer[] ids) {
    try {
      Integer[] removeIds = (Integer[])ArrayUtils.clone(ids);
      for(Integer id:isFilteredObject(ids)) {
        removeIds = (Integer[])ArrayUtils.removeElement(removeIds, id);
        Node node = getNodeObject(id);
        if(node != null) {
          updateObject(node);
        }else createObject(id);
        createtedObjects.remove(id);
      }
      removeObjects(removeIds);
    }catch(Exception ex){Messanger.showErrorMessage(ex);}
  }

  /*@Override
  public boolean isFilteredObject(MappingObject object) {
    try {
      if(getRootFilter() != null) {
        RMIDBNodeObject nodeObject = (RMIDBNodeObject)object;
        boolean ff = ObjectLoader.isSatisfy(getRootFilter(), nodeObject);// filter.isSatisfy(nodeObject);
        if(!ff) {
          for(RMIDBNodeObject child:nodeObject.getChilds()) {
            ff = isFilteredObject(child);
            if(ff)
              break;
          }
        }
        return ff;
      }
    }catch(Exception ex){Messanger.showErrorMessage(ex);}
    return true;
  }*/
  
  public boolean isFilteredObject(Integer id) {
    return isFilteredObject(new Integer[]{id}).length == 1;
  }

  @Override
  public Integer[] isFilteredObject(Integer[] ids) {
    try {
      if(getRootFilter() != null) {
        Integer[] idsFilter = ObjectLoader.isSatisfy(getRootFilter(), ids);
        if(idsFilter.length == ids.length)
          return idsFilter;
        else {
          List<List> rez;
          for(Integer id:ids) {
            if(!ArrayUtils.contains(idsFilter, id)) {
              rez = ObjectLoader.createSession(true).executeQuery("SELECT id FROM ["+getObjectClass().getName()+"] "+
                "WHERE ["+getObjectClass().getName()+"(parent)]="+id);
              if(!rez.isEmpty()) {
                Integer[] idsCilds = new Integer[0];
                for(List r:rez)
                  idsCilds = (Integer[])ArrayUtils.add(idsCilds,r.get(0));
                if(isFilteredObject(idsCilds).length > 0)
                  idsFilter = (Integer[])ArrayUtils.add(idsFilter, id);
              }
            }
          }
          return idsFilter;
        }
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    return new Integer[0];
  }

  @Override
  public int getObjectsCount() {
    int count = -1;
    Enumeration em = ((DefaultMutableTreeNode)getTreeModel().getRoot()).postorderEnumeration();
    while(em.hasMoreElements()) {
      em.nextElement();
      count++;
    }
    return count;
  }

  @Override
  public void setDefaultSelected() {
    tree.setSelectionPath(new TreePath(((DefaultMutableTreeNode)tree.getTreeModel().getRoot()).getPath()));
  }

  @Override
  public void clearSelection() {
    tree.clearSelection();
  }
}