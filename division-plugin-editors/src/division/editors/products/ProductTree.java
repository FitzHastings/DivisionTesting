package division.editors.products;

import bum.editors.Editor;
import bum.editors.EditorController;
import bum.editors.EditorGui;
import bum.editors.MainObjectEditor;
import bum.editors.EditorListener;
import bum.editors.util.DivisionTarget;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Group;
import bum.interfaces.Product;
import bum.interfaces.Service;
import division.ClientMain;
import division.ClientMainListener;
import division.editors.objects.GroupEditor;
import division.editors.objects.ServiceEditor;
import division.editors.tables.GroupTableEditor;
import division.fx.PropertyMap;
import division.swing.DivisionScrollPane;
import division.swing.DivisionTableRenderer;
import division.swing.DivisionToolButton;
import division.swing.TreeTable.TreeTable;
import division.swing.TreeTable.TreeTableModel;
import division.swing.TreeTable.TreeTableModelAdapter;
import division.swing.guimessanger.Messanger;
import division.swing.table.CellColorController;
import division.swing.tree.bum_Node;
import division.util.FileLoader;
import division.util.Utility;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.Period;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class ProductTree extends EditorGui implements ClientMainListener {
  protected ExecutorService pool = Executors.newFixedThreadPool(5);
  
  protected DivisionToolButton undoToolButton    = new DivisionToolButton(FileLoader.getIcon("undo16.png"),"Вернуть в каталог");
  protected DivisionToolButton addToolButton     = new DivisionToolButton(FileLoader.getIcon("Add16.gif"),"Добавить");
  protected DivisionToolButton editToolButton    = new DivisionToolButton(FileLoader.getIcon("Edit16.gif"),"Редактировать");
  protected DivisionToolButton removeToolButton  = new DivisionToolButton(FileLoader.getIcon("Delete16.gif"),"Удалить");
  
  private final JPopupMenu popMenu = new JPopupMenu();
  private final JMenuItem  create  = new JMenuItem("Добавить");
  private final JMenuItem  edit    = new JMenuItem("Редактировать");
  private final JMenuItem  remove  = new JMenuItem("Удалить");
  
  private final ProductNode    root = new ProductNode(null, null, null, "Разделы", null);
  private TreeTableModel model;
  private TreeTable table;
  private DivisionTableRenderer renderer = new DivisionTableRenderer();
  
  private final ArrayList<EditorListener> tableEditorListeners = new ArrayList<>();
  private InitDataTask initDataTask;
  
  private Integer company;
  private Integer[] processes;
  
  private boolean archive;
  
  public ProductTree() {
    this(false);
  }

  public ProductTree(boolean archive) {
    super(null, null);
    this.archive = archive;
    initComponents();
    initTargets();
    initEvents();
  }

  public boolean isArchive() {
    return archive;
  }
  
  private void initComponents() {
    model = new TreeTableModel(root);
    
    model.addColumn("Длит. прайс", "getDuration", "setDuration", String.class);
    model.addColumn("Цикл. прайс", "getRecurrence", "setRecurrence", String.class);
    model.addColumn("Цена прайс", "getCost", "setCost", BigDecimal.class);
    
    model.addColumn("Длит. каталог", "getGlobDuration", "setGlobDuration", String.class);
    model.addColumn("Цикл. каталог", "getGlobRecurrence", "setGlobRecurrence", String.class);
    model.addColumn("Цена каталог", "getGlobCost", "setGlobCost", BigDecimal.class);
    model.addColumn("НДС", "getNds", "setNds", BigDecimal.class);
    
    table = new TreeTable(model) {
      @Override
      public void paint(Graphics g) {
        super.paint(g); //To change body of generated methods, choose Tools | Templates.
        Graphics2D g2 = (Graphics2D) g;
        if(getRowCount() > 0) {
          Rectangle r0 = getCellRect(0, 1, true);
          Rectangle r1 = getCellRect(getRowCount()-1, 3, true);
          g2.setColor(Color.BLACK);
          g2.setStroke(new BasicStroke(2));
          g2.drawRect(r0.x, 0, r1.x+r1.width-r0.x, r1.y+r1.height);
        }
      }
    };
    
    table.getTableHeader().setReorderingAllowed(false);
    table.setColumnEditable(0, false);
    table.setColumnEditable(3, company != null);
    table.setColumnEditable(6, !isArchive());
    table.setColumnEditable(7, !isArchive());
    
    table.setGridColor(Color.LIGHT_GRAY);
    table.setShowGrid(true);
    
    table.getTree().setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
    
    if(!isArchive())
      getToolBar().add(addToolButton);
    else getToolBar().add(undoToolButton);
    getToolBar().add(editToolButton);
    getToolBar().add(removeToolButton);
    
    undoToolButton.setEnabled(false);
    editToolButton.setEnabled(false);
    removeToolButton.setEnabled(false);
    
    popMenu.add(create);
    popMenu.add(edit);
    popMenu.add(remove);
    
    DivisionScrollPane scroll = new DivisionScrollPane(table);
    getRootPanel().setLayout(new BorderLayout());
    getRootPanel().add(scroll, BorderLayout.CENTER);
    
    addComponentToStore(table,"ProductTreeTable_"+isArchive()+"_For_"+getName());
    
    setRenderer();
    
    renderer.setCellFontController((JTable t, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) -> {
      return modelColumn==1||modelColumn==2||modelColumn==3?new Font("Dialog", Font.BOLD, 11):null;
    });
    
    renderer.setCellAligmentController((JTable t, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) -> {
      return modelColumn==3||modelColumn==6||modelColumn==7?JLabel.RIGHT:JLabel.LEFT;
    });
    
    renderer.setCellColorController(new CellColorController() {
      @Override
      public Color getCellColor(JTable t, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        if(table.getTree().getPathForRow(modelRow) == null)
          return null;
        ProductNode node = (ProductNode)table.getTree().getPathForRow(modelRow).getLastPathComponent();
        Color color = modelColumn==1||modelColumn==3?new Color(0, 0, 255, isSelect?80:50):null;
        if(table.getTree().isExpanded(new TreePath(node.getPath())))
          color = node.getType()!=ProductNode.Type.Product ? 
                  isSelect ? t.getSelectionBackground() :
                  new Color(Color.GRAY.getRed(), Color.GRAY.getGreen(), Color.GRAY.getBlue(), 60) :
                  color;
        return color;
      }

      @Override
      public Color getCellTextColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        return modelColumn==2||modelColumn==4||modelColumn==5||modelColumn==6||modelColumn==7?isSelect&&!hasFocus?Color.WHITE:Color.GRAY:null;
      }
    });
  }

  private void initEvents() {
    ClientMain.addClientMainListener(this);
    
    undoToolButton.addActionListener(e -> undo());
    addToolButton.addActionListener(e -> create());
    editToolButton.addActionListener(e -> edit());
    removeToolButton.addActionListener(e -> remove());
    
    table.addTableSelectionListener((int[] oldSelection, int[] newSelection) -> {
      fireChangeSelection(ProductTree.this, new Object[]{getSelectedProductId(false,false), getSelectedProductId(true,false)});
    });
    
    table.addMouseListener(new TreeTableMouseAdapter());
    table.getTree().addTreeSelectionListener((TreeSelectionEvent e) -> {
      editToolButton.setEnabled(false);
      removeToolButton.setEnabled(false);
      undoToolButton.setEnabled(false);
      if(e.getNewLeadSelectionPath() != null) {
        ProductNode node = (ProductNode)e.getNewLeadSelectionPath().getLastPathComponent();
        editToolButton.setEnabled(!node.isRoot());
        removeToolButton.setEnabled(!node.isRoot());
        undoToolButton.setEnabled(!node.isRoot());
      }
    });
  }
  
  private void createdProduct(Integer[] ids) {
    initData(ids, false);
  }
  
  private void removedProduct(Integer[] ids) {
    for(Integer id:ids) {
      ProductNode node = getGlobalProductNode(id);
      if(node != null)
        removeNodeFromParent(node);
      else {
        node = getNode(id, ProductNode.Type.Product, root);
        if(node != null) {
          node.setCost(null, false);
          node.setDuration(null, false);
          node.setRecurrence(null, false);
          node.setId(null);
          updateTable();
        }
      }
    }
  }
  
  private void removeNodeFromParent(ProductNode node) {
    ProductNode parent = (ProductNode) node.getParent();
    model.removeNodeFromParent(node);
    if(parent != null && parent.getChildCount() == 0 && parent.getType() != ProductNode.Type.Process)
      removeNodeFromParent(parent);
  }
  
  public TreeTable getTreeTable() {
    return table;
  }
  
  private void updateTable() {
    for(int i=0;i<table.getRowCount();i++)
      ((TreeTableModelAdapter)table.getModel()).fireTableRowsUpdated(i, i);
    setRenderer();
  }
  
  private void setRenderer() {
    table.getColumnModel().getColumn(1).setCellRenderer(renderer);
    table.getColumnModel().getColumn(2).setCellRenderer(renderer);
    table.getColumnModel().getColumn(3).setCellRenderer(renderer);
    table.getColumnModel().getColumn(4).setCellRenderer(renderer);
    table.getColumnModel().getColumn(5).setCellRenderer(renderer);
    table.getColumnModel().getColumn(6).setCellRenderer(renderer);
    table.getColumnModel().getColumn(7).setCellRenderer(renderer);
  }
  
  private void undo() {
    Integer[] processes = null;
    Integer[] products = null;
    
    for(ProductNode node:getSelectedNodes(ProductNode.Type.Process, true))
      processes = (Integer[]) ArrayUtils.add(processes, node.getId());
    
    for(ProductNode node:getSelectedNodes(ProductNode.Type.Product, true)) {
      if(node.getId() != null)
        products = (Integer[]) ArrayUtils.add(products, node.getId());
      if(node.getGlobId() != null)
        products = (Integer[]) ArrayUtils.add(products, node.getGlobId());
    }
    
    if(processes != null && ObjectLoader.executeUpdate("UPDATE [Service] SET type='CURRENT' WHERE id=ANY(?)", new Object[]{processes}) > 0)
      ObjectLoader.sendMessage(Service.class, "UPDATE", processes);
    if(products != null && ObjectLoader.executeUpdate("UPDATE [Product] SET type='CURRENT' WHERE id=ANY(?)", new Object[]{products}) > 0)
      ObjectLoader.sendMessage(Product.class, "UPDATE", products);
  }
  
  private void create() {
    JPopupMenu menu = new JPopupMenu();
    JMenuItem process = new JMenuItem("Раздел");
    JMenuItem product = new JMenuItem("Продукт");
    product.setEnabled(getSelectedNode() != null);
    menu.add(process);
    menu.add(product);
    process.addActionListener(e -> createProcess());
    product.addActionListener(e -> createProduct());
    menu.show(addToolButton, 0, addToolButton.getY()+addToolButton.getHeight());
  }
  
  private void edit() {
    try {
      MainObjectEditor editor = null;
      ProductNode node = getSelectedNode();
      Integer id = node.getId() == null ? node.getGlobId() : node.getId();
      
      switch(node.getType()) {
        case Product:
          id = (Integer) ObjectLoader.getData(Product.class, new Integer[]{id}, "group").get(0).get(0);
          editor = new GroupEditor();
          editor.setEditorObject(ObjectLoader.getObject(Group.class, id));
          break;
        case Group:
          editor = new GroupEditor();
          editor.setEditorObject(ObjectLoader.getObject(Group.class, id));
          break;
        case Process:
          editor = new ServiceEditor();
          editor.setEditorObject(ObjectLoader.getObject(Service.class, id));
          break;
      }
      
      editor.setAutoLoad(true);
      editor.setAutoStore(true);
      if(editor != null) {
        Container parent = Editor.getParentWindow(getGUI());
        if(parent instanceof JInternalFrame) {
          EditorController.getDeskTop().add(editor);
          if(editor.getInternalDialog() != null)
            editor.getInternalDialog().setVisible(true);
        }else if(parent instanceof JDialog || parent instanceof JFrame)
          editor.createDialog(this, true).setVisible(true);
      }
      
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  private void remove() {
    JPopupMenu menu = new JPopupMenu();
    
    ProductNode[] procs = getSelectedNodes(ProductNode.Type.Process, true);
    JMenuItem removeProcess = new JMenuItem("Удалить раздел"+(procs.length > 1 ? "ы" : ""));
    removeProcess.setEnabled(procs.length > 0);
    
    Integer[] globIds = getSelectedProductId(true, true);
    JMenuItem removeCatalogProduct = new JMenuItem("Удалить продукт"+(globIds.length > 1 ? "ы" : "")+" из каталога");
    removeCatalogProduct.setEnabled(globIds.length > 0);
    
    Integer[] ids = getSelectedProductId();
    JMenuItem removePriceProduct = new JMenuItem("Удалить продукт"+(ids.length > 1 ? "ы" : "")+" из прайс-листа");
    removePriceProduct.setEnabled(ids.length > 0);
    
    menu.add(removeProcess);
    menu.add(removeCatalogProduct);
    menu.add(removePriceProduct);
    
    removeProcess.addActionListener(e -> removeProcess());
    removeCatalogProduct.addActionListener(e -> ProductNode.removeProducts(globIds, isArchive()));
    removePriceProduct.addActionListener(e -> ProductNode.removeProducts(ids, isArchive()));
    
    menu.show(removeToolButton, 0, removeToolButton.getY()+removeToolButton.getHeight());
  }
  
  private void removeProcess() {
    ProductNode[] procs = getSelectedNodes(ProductNode.Type.Process, true);
    Integer[] ids = new Integer[0];
    for(ProductNode n:procs)
      ids = (Integer[]) ArrayUtils.add(ids, n.getId());
    if(ids.length > 0) {
      if(isArchive())
        ObjectLoader.removeObjects(Service.class, ids);
      else if(ObjectLoader.executeUpdate("UPDATE [Service] SET type='ARCHIVE' WHERE id=ANY(?)", new Object[]{ids}) > 0)
        ObjectLoader.sendMessage(Service.class, "UPDATE", ids);
    }
  }
  
  private void createProcess() {
    try {
      ProductNode parent = getSelectedNode(ProductNode.Type.Process);
      Map<String,Object> map = new TreeMap<>();
      map.put("parent", parent != null ? parent.getId() : null);
      map.put("tmp", true);
      ServiceEditor editor = new ServiceEditor();
      editor.setEditorObject(ObjectLoader.getObject(Service.class, ObjectLoader.createObject(Service.class, map, true), true));
      editor.createDialog(this, true).setVisible(true);
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  private void createProduct() {
    ProductNode process = getSelectedNode(ProductNode.Type.Process);
    if(process != null) {
      ProductNode group   = getSelectedNode(ProductNode.Type.Group);
      GroupTableEditor groupTableEditor = new GroupTableEditor();
      groupTableEditor.getTree().setHierarchy(true);
      groupTableEditor.setAutoLoadAndStore(true);
      if(group != null) {
        groupTableEditor.addEditorListener(new EditorListener() {
          @Override
          public void initDataComplited(EditorGui editor) {
            bum_Node node = groupTableEditor.getNodeObject(group.getId());
            if(node != null) {
              groupTableEditor.getTree().expandPath(new TreePath(node.getPath()));
              Rectangle rec = groupTableEditor.getTree().getPathBounds(new TreePath(node.getPath()));
              groupTableEditor.getTree().scrollRectToVisible(rec);
              groupTableEditor.setSelectedObjects(new Integer[]{group.getId()});
            }
          }
        });
      }
      groupTableEditor.initData();
      Integer[] ids = groupTableEditor.getLastId();
      if(ids.length > 0) {
        RemoteSession session = null;
        try {
          Integer startId = 0;
          
          Integer[] archiveIds = new Integer[0];
          for(List d:ObjectLoader.executeQuery("SELECT id, [Product(group)], type FROM [Product] WHERE [Product(company)]=? AND [Product(service)]=? AND [Product(group)]=ANY(?) AND tmp=false", new Object[]{company, process.getId(), ids})) {
            ids = (Integer[]) ArrayUtils.removeElement(ids, d.get(1));
            if(d.get(2).equals("ARCHIVE"))
              archiveIds = (Integer[]) ArrayUtils.add(archiveIds, d.get(0));
          }
          
          session = ObjectLoader.createSession();
          if(archiveIds.length > 0) {
            session.executeUpdate("UPDATE [Product] SET type='CURRENT' WHERE id=ANY(?)", new Object[]{archiveIds});
            session.addEvent(Product.class, "UPDATE", archiveIds);
          }
          
          if(ids.length > 0) {
            List<List> data = session.executeQuery("SELECT MAX(id) FROM [Product]");
            if(!data.isEmpty())
              startId = data.get(0).get(0) == null ? 0 : Integer.valueOf(data.get(0).get(0).toString());
            String query = "INSERT INTO [Product] ([Product(company)], [Product(service)], [Product(group)]) VALUES ";
            for(Integer id:ids)
              query += "("+company+", "+process.getId()+","+ id+"),";
            query = query.substring(0, query.length()-1);
            session.executeUpdate(query);
            ids = new Integer[0];
            for(List d:session.executeQuery("SELECT id FROM [Product] WHERE id>"+startId))
              ids = (Integer[]) ArrayUtils.add(ids, d.get(0));
            session.addEvent(Product.class, "CREATE", ids);
          }
          ObjectLoader.commitSession(session);
        }catch(Exception ex) {
          ObjectLoader.rollBackSession(session);
          Messanger.showErrorMessage(ex);
        }
      }
    }
  }
  
  private ProductNode getSelectedNode(ProductNode.Type type) {
    ProductNode node = getSelectedNode();
    if(node != null) {
      while(node != null && node.getType() != type)
        node = (ProductNode) node.getParent();
      return node;
    }
    return null;
  }
  
  private ProductNode getSelectedNode() {
    TreePath path = table.getTree().getSelectionPath();
    return path == null ? null : (ProductNode) path.getLastPathComponent();
  }
  
  private ProductNode[] getSelectedNodes() {
    ProductNode[] productNodes = new ProductNode[0];
    for(TreePath path:table.getTree().getSelectionPaths())
      productNodes = (ProductNode[]) ArrayUtils.add(productNodes, (ProductNode)path.getLastPathComponent());
    return productNodes;
  }
  
  private ProductNode[] getSelectedNodes(ProductNode.Type type, boolean recursion) {
    ProductNode[] nodes = new ProductNode[0];
    if(table.getTree().getSelectionPaths() != null)
      for(TreePath path:table.getTree().getSelectionPaths()) {
        if(recursion) {
          Enumeration<ProductNode> em = ((ProductNode) path.getLastPathComponent()).preorderEnumeration();
          while(em.hasMoreElements()) {
            ProductNode node = em.nextElement();
            if(type == null || type == node.getType())
              nodes = (ProductNode[]) ArrayUtils.add(nodes, node);
          }
        }else if(((ProductNode) path.getLastPathComponent()).getType() == type)
          nodes = (ProductNode[]) ArrayUtils.add(nodes, (ProductNode) path.getLastPathComponent());
      }
    return nodes;
  }
  
  public Integer[] getSelectedProductId() {
    return getSelectedProductId(false, true);
  }
  
  public Integer[] getSelectedProductId(boolean recursion) {
    return getSelectedProductId(false, recursion);
  }

  public Integer[] getSelectedProductId(boolean global, boolean recursion) {
    Integer[] ids = new Integer[0];
    for(ProductNode node:getSelectedNodes(ProductNode.Type.Product, recursion)) {
      if(global) {
        if(node.getGlobId() != null)
          ids = (Integer[]) ArrayUtils.add(ids, node.getGlobId());
      }else if(node.getId() != null) 
        ids = (Integer[]) ArrayUtils.add(ids, node.getId());
    }
    return ids;
  }
  
  public Integer[] get() {
    createDialog(this, true).setVisible(true);
    return getSelectedProductId();
  }
  
  public void addTableEditorListener(EditorListener listener) {
    if(!tableEditorListeners.contains(listener))
      tableEditorListeners.add(listener);
  }
  
  public void removeTableEditorListener(EditorListener listener) {
    tableEditorListeners.remove(listener);
  }

  @Override
  public void initData() {
    initData(null, true);
  }
  
  public void initData(Integer[] ids, boolean isClear) {
    if(isClear)
      clear();
    if(initDataTask != null) {
      initDataTask.shutDown();
      initDataTask = null;
    }
    if(isActive())
      pool.submit(initDataTask = new InitDataTask(ids));
  }
  
  @Override
  public void closeDialog() {
    table.clearSelection();
    super.closeDialog();
  }

  @Override
  public Boolean okButtonAction() {
    dispose();
    return true;
  }

  @Override
  public void dispose() {
    super.dispose();
  }
  
  public ProductNode[] getNodes(Integer id, ProductNode.Type type, ProductNode rootNode) {
    ProductNode[] nodes = new ProductNode[0];
    if(id != null) {
      Enumeration em = rootNode.preorderEnumeration();
      while(em.hasMoreElements()) {
        ProductNode n = (ProductNode) em.nextElement();
        if(n.getType() == type && id.equals(n.getId()))
          nodes = (ProductNode[]) ArrayUtils.add(nodes, n);
      }
    }
    return nodes;
  }
  
  public ProductNode[] getGroupNodes(Integer id) {
    ProductNode[] nodes = new ProductNode[0];
    if(id != null) {
      Enumeration em = root.preorderEnumeration();
      while(em.hasMoreElements()) {
        ProductNode n = (ProductNode) em.nextElement();
        if(n.getType() == ProductNode.Type.Group && id.equals(n.getId()) || n.getType() == ProductNode.Type.Product && id.equals(n.getGroupId()))
          nodes = (ProductNode[]) ArrayUtils.add(nodes, n);
      }
    }
    return nodes;
  }
  
  public ProductNode getGlobalProductNode(Integer id) {
    if(id != null) {
      Enumeration em = root.preorderEnumeration();
      while(em.hasMoreElements()) {
        ProductNode n = (ProductNode) em.nextElement();
        if(n.getType() == ProductNode.Type.Product && id.equals(n.getGlobId()))
          return n;
      }
    }
    return null;
  }
  
  public ProductNode getNode(Integer id, ProductNode.Type type, ProductNode rootNode) {
    if(id != null) {
      Enumeration em = rootNode.preorderEnumeration();
      while(em.hasMoreElements()) {
        ProductNode n = (ProductNode) em.nextElement();
        if(n.getType() == type && id.equals(n.getId()))
          return n;
      }
    }
    return null;
  }
  
  public void insertNode(List<List> data, ProductNode node, Integer parentId, ProductNode rootNode) {
    ProductNode parent = getNode(parentId, node.getType(), rootNode);
    if(parent != null)
      parent.add(node);
    else {
      if(parentId == null)
        parent = rootNode;
      else {
        for(List d:data) {
          if(d.get(0).equals(parentId)) {
            parent = new ProductNode(table.getTree(), company, (Integer)d.get(0), (String)d.get(1), node.getType());
            insertNode(data, parent, (Integer) d.get(2), rootNode);
            break;
          }
        }
      }
    }
    model.insertNodeInto(node, parent, 0);
    model.reload(parent);
  }
  
  @Override
  public void clear() {
    root.removeAllChildren();
    model.reload(root);
  }

  @Override
  public void changedCFC(Integer[] id) {
  }

  @Override
  public void changedCompany(Integer[] ids) {
    company = ids != null && ids.length == 1 ? ids[0] : null;
    table.setColumnEditable(3, company != null);
    initData();
  }
  
  private List<List> getProcesses() {
    DBFilter filter = DBFilter.create(Service.class).AND_EQUAL("tmp", false);
    if(!isArchive())
      filter.AND_EQUAL("type", Product.Type.CURRENT);
    return ObjectLoader.getData(filter, "id", "name", "parent");
  }

  @Override
  public void initTargets() {
    addTarget(new DivisionTarget(Service.class) {
      @Override
      public void messageReceived(String type, Integer[] ids, PropertyMap objectEventProperty) {
        try {
          if(isActive()) {
            if("UPDATE".equals(type)) {
              List<List> processData = getProcesses();
              for(Integer id:ids) {
                List proc = null;
                for(List d:processData) {
                  if(d.get(0).equals(id)) {
                    proc = d;
                    break;
                  }
                }
                ProductNode node = getNode(id, ProductNode.Type.Process, root);
                if(proc == null) {
                  if(node != null)
                    model.removeNodeFromParent(node);
                }else {
                  if(node == null) {
                    node = new ProductNode(table.getTree(), company, id, (String) proc.get(1), ProductNode.Type.Process);
                    insertNode(processData, node, (Integer)proc.get(2), root);
                  }
                }
              }
            }
            if("CREATE".equals(type)) {
              List<List> processData = getProcesses();
              DBFilter filter = DBFilter.create(Service.class).AND_EQUAL("tmp", false);
              if(!isArchive())
                filter.AND_EQUAL("type", Product.Type.CURRENT);
              ids = ObjectLoader.isSatisfy(filter, ids);
              for(List d:processData) {
                if(ArrayUtils.contains(ids, d.get(0))) {
                  ProductNode node = getNode((Integer)d.get(0), ProductNode.Type.Process, root);
                  if(node == null) {
                    node = new ProductNode(table.getTree(), company, (Integer)d.get(0), (String) d.get(1), ProductNode.Type.Process);
                    insertNode(processData, node, (Integer)d.get(2), root);
                  }
                }
              }
            }
            if("REMOVE".equals(type)) {
              for(Integer id:ids) {
                ProductNode node = getNode(id, ProductNode.Type.Process, root);
                if(node != null)
                  model.removeNodeFromParent(node);
              }
            }
          }
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
    
    addTarget(new DivisionTarget(Group.class) {
      @Override
      public void messageReceived(String type, Integer[] ids, PropertyMap objectEventProperty) {
        try {
          if("UPDATE".equals(type)) {
            ObjectLoader.getData(Group.class, ids, "id", "name", "parent").stream().forEach(d -> {
              for(ProductNode node:getGroupNodes((Integer)d.get(0))) {
                node.setUserObject((String)d.get(1));
                model.nodeChanged(node.getParent());
              }
            });
          }else if("REMOVE".equals(type)) {
            ProductNode[] nodes = new ProductNode[0];
            for(Integer id:ids) {
              ProductNode[] ns = getNodes(id, ProductNode.Type.Group, root);
              if(ns != null && ns.length > 0)
                nodes = (ProductNode[]) ArrayUtils.add(nodes, ns);
              ns = getNodes(id, ProductNode.Type.Product, root);
              if(ns != null && ns.length > 0)
                nodes = (ProductNode[]) ArrayUtils.addAll(nodes, ns);
            }
            for(ProductNode node:nodes)
              model.removeNodeFromParent(node);
          }
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
    
    addTarget(new DivisionTarget(Product.class) {
      @Override
      public void messageReceived(String type, Integer[] ids, PropertyMap objectEventProperty) {
        try {
          if(isActive()) {
            
            switch(type) {
              
              case "CREATE":
                
                DBFilter filter = DBFilter.create(Product.class).AND_EQUAL("tmp", false).AND_EQUAL("type", isArchive()?"ARCHIVE":"CURRENT");
                DBFilter f = filter.AND_FILTER().AND_EQUAL("globalProduct", null).AND_EQUAL("company", null);
                if(company != null)
                  f.OR_EQUAL("company", company).AND_NOT_EQUAL("globalProduct", null);
                if(processes != null && processes.length > 0)
                  filter.AND_IN("service", processes);
                
                Integer[] productIds = ObjectLoader.isSatisfy(filter, ids);
                if(productIds.length > 0)
                  createdProduct(ids);
                break;
                
              case "UPDATE":
                
                filter = DBFilter.create(Product.class).AND_EQUAL("tmp", false).AND_EQUAL("type", isArchive()?"ARCHIVE":"CURRENT");
                f = filter.AND_FILTER().AND_EQUAL("globalProduct", null).AND_EQUAL("company", null);
                if(company != null)
                  f.OR_EQUAL("company", company).AND_NOT_EQUAL("globalProduct", null);
                if(processes != null && processes.length > 0)
                  filter.AND_IN("service", processes);
                filter.AND_IN("id", ids);
                
                Integer[] createdIds = null;
                Integer[] removedIds = null;
                
                
                List<List> data = ObjectLoader.getData(filter, "id", "cost", "duration", "recurrence", "nds", "globalProduct");
                
                Integer[] satisfyIds = null;
                for(List d:data)
                  satisfyIds = (Integer[]) ArrayUtils.add(satisfyIds, d.get(0));
                
                for(Integer id:ids)
                  if(!ArrayUtils.contains(satisfyIds, id))
                    removedIds = (Integer[]) ArrayUtils.add(removedIds, id);
                
                if(removedIds != null)
                  removedProduct(removedIds);
                
                for(List d:data) {
                  Integer    id            = (Integer)    d.get(0);
                  BigDecimal cost          = (BigDecimal) d.get(1);
                  Period     duration      = (Period)     d.get(2);
                  Period     recurrence    = (Period)     d.get(3);
                  BigDecimal nds           = (BigDecimal) d.get(4);
                  Integer    globalProduct = (Integer)    d.get(5);
                  
                  if(globalProduct != null) { // Это НЕ глобальный продукт
                    ProductNode node = getNode((Integer)d.get(0), ProductNode.Type.Product, root);
                    if(node != null) {
                      node.setCost(cost, false);
                      node.setDuration(duration, false);
                      node.setRecurrence(recurrence, false);
                      updateTable();
                    }else createdIds = (Integer[]) ArrayUtils.add(createdIds, id);
                  }else {
                    ProductNode node = getGlobalProductNode((Integer)d.get(0));
                    if(node != null) {
                      node.setGlobCost(cost, false);
                      node.setGlobDuration(duration, false);
                      node.setGlobRecurrence(recurrence, false);
                      node.setNds(nds, false);
                      updateTable();
                    }else createdIds = (Integer[]) ArrayUtils.add(createdIds, id);
                  }
                }
                
                if(createdIds != null)
                  createdProduct(createdIds);
                break;
                
              case "REMOVE":
                removedProduct(ids);
                break;
            }
          }
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
  }
  
  class InitDataTask implements Runnable {
    private boolean shutdown = false;
    private Integer[] ids = null;

    public InitDataTask() {}

    public InitDataTask(Integer[] ids) {
      this.ids = ids;
    }
    
    @Override
    public void run() {
      if(shutdown)
        return;
      waitCursor();
      try {
        
        // Получаем процессы
        List<List> processData = getProcesses();
        
        // Получаем объекты
        DBFilter filter = DBFilter.create(Group.class);
        List<List> groupData = ObjectLoader.getData(filter, "id", "name", "parent");
        
        // Получаем продукты
        filter = DBFilter.create(Product.class);
        if(ids != null && ids.length > 0)
          filter.AND_IN("id", ids);
        else {
          filter.AND_EQUAL("tmp", false).AND_EQUAL("type", isArchive()?"ARCHIVE":"CURRENT");
          DBFilter f = filter.AND_FILTER().AND_EQUAL("globalProduct", null).AND_EQUAL("company", null);
          if(company != null)
            f.OR_EQUAL("company", company).AND_NOT_EQUAL("globalProduct", null);
          if(processes != null && processes.length > 0)
            filter.AND_IN("service", processes);
        }
        List<List> products = ObjectLoader.getData( 
                filter, 
                /*0*/"id", 
                /*1*/"service_id", 
                /*2*/"group_parent_id", 
                /*3*/"group_name", 
                /*4*/"cost", 
                /*5*/"nds", 
                /*6*/"duration", 
                /*7*/"recurrence",
                /*8*/"globalProduct",
                /*9*/"group");
        
        SwingUtilities.invokeLater(() -> {
          processData.stream().forEach(d -> {
            if(shutdown)
              return;
            if(d.get(1) != null) {
              ProductNode node = getNode((Integer)d.get(0), ProductNode.Type.Process, root);
              if(node == null) {
                node = new ProductNode(table.getTree(), company, (Integer)d.get(0), (String)d.get(1), ProductNode.Type.Process);
                insertNode(processData, node, (Integer) d.get(2), root);
              }
            }
          });
          
          products.stream().filter(p -> p.get(8)==null).forEach(p -> {
            if(shutdown)
              return;
            Integer    id             = (Integer)    p.get(0);
            Integer    processId      = (Integer)    p.get(1);
            Integer    parentgroupId  = (Integer)    p.get(2);
            String     groupName      = (String)     p.get(3);
            BigDecimal cost           = (BigDecimal) p.get(4);
            BigDecimal nds            = (BigDecimal) p.get(5);
            Period     duration       = (Period)     p.get(6);
            Period     recurrence     = (Period)     p.get(7);
            Integer    groupId        = (Integer)    p.get(9);

            ProductNode process = getNode(processId, ProductNode.Type.Process, root);
            if(process != null) {
              process.setFont(new Font("Dialog", Font.BOLD, 11));
              ProductNode node = new ProductNode(table.getTree(), company, id, groupName);
              node.setFont(new Font("Dialog", Font.ITALIC, 11));
              node.setGlobCost(cost, false);
              node.setGlobDuration(duration, false);
              node.setGlobRecurrence(recurrence, false);
              node.setNds(nds, false);
              node.setArchive(isArchive());
              node.setGroupId(groupId);
              
              ProductNode parent;
              if(parentgroupId == null)
                parent = process;
              else {
                parent = getNode(parentgroupId, ProductNode.Type.Group, process);
                if(parent == null) {
                  for(List g:groupData) {
                    if(g.get(0).equals(parentgroupId)) {
                      parent = new ProductNode(table.getTree(), company, (Integer)g.get(0), (String)g.get(1), ProductNode.Type.Group);
                      insertNode(groupData, parent, (Integer)g.get(2), process);
                      break;
                    }
                  };
                }
              }
              model.insertNodeInto(node, parent, 0);
              model.reload(parent);
            }
          });
          
          products.stream().filter(p -> p.get(8)!=null).forEach(p -> {
            if(shutdown)
              return;
            
            Integer    id             = (Integer)    p.get(0);
            Integer    processId      = (Integer)    p.get(1);
            Integer    parentgroupId  = (Integer)    p.get(2);
            String     groupName      = (String)     p.get(3);
            BigDecimal cost           = (BigDecimal) p.get(4);
            BigDecimal nds            = (BigDecimal) p.get(5);
            Period     duration       = (Period)     p.get(6);
            Period     recurrence     = (Period)     p.get(7);
            Integer    globId         = (Integer)    p.get(8);
            Integer    groupId        = (Integer)    p.get(9);
            
            ProductNode node = getGlobalProductNode(globId);
            if(node != null) {
              node.setId(id);
              node.setCost(cost, false);
              node.setDuration(duration, false);
              node.setRecurrence(recurrence, false);
              node.setArchive(isArchive());
            }else {
              ProductNode process = getNode(processId, ProductNode.Type.Process, root);
              if(process != null) {
                process.setFont(new Font("Dialog", Font.BOLD, 11));
                node = new ProductNode(table.getTree(), company, id, groupName, ProductNode.Type.Product);
                node.setGlobId(globId);
                node.setFont(new Font("Dialog", Font.ITALIC|Font.CENTER_BASELINE, 11));
                node.setCost(cost, false);
                node.setDuration(duration, false);
                node.setRecurrence(recurrence, false);
                node.setNds(nds, false);
                node.setArchive(isArchive());
                node.setGroupId(groupId);

                ProductNode parent;
                if(parentgroupId == null)
                  parent = process;
                else {
                  parent = getNode(parentgroupId, ProductNode.Type.Group, process);
                  if(parent == null) {
                    for(List g:groupData) {
                      if(g.get(0).equals(parentgroupId)) {
                        parent = new ProductNode(table.getTree(), company, (Integer)g.get(0), (String)g.get(1), ProductNode.Type.Group);
                        insertNode(groupData, parent, (Integer)g.get(2), process);
                        break;
                      }
                    };
                  }
                }
                model.insertNodeInto(node, parent, 0);
                model.reload(parent);
              }
            }
          });
          updateTable();
          defaultCursor();
          fireInitDataComplited(ProductTree.this);
        });
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }

    public void shutDown() {
      shutdown = true;
      defaultCursor();
    }
  }
  
  
  
  
  
  class TreeTableMouseAdapter extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent e) {
      int row = table.convertRowIndexToModel(table.rowAtPoint(e.getPoint()));
      int col = table.convertColumnIndexToModel(table.columnAtPoint(e.getPoint()));
      
      if(col == 0 && e.getModifiers() == MouseEvent.META_MASK) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem  item = new JMenuItem("Развернуть выделленные разделы");
        item.setEnabled(false);
        JMenuItem  allItem = new JMenuItem("Развернуть все разделы");

        if(table.getSelectedRowCount() > 0) {
          item.setEnabled(true);
          if(table.getSelectedRowCount() == 1)
            item.setText("Развернуть выделленный раздел");
        }

        item.addActionListener(ie -> {
          for(TreePath path:table.getTree().getSelectionPaths()) {
            Enumeration em = ((ProductNode)path.getLastPathComponent()).preorderEnumeration();
            while(em.hasMoreElements()) {
              table.getTree().expandPath(new TreePath(((ProductNode)em.nextElement()).getPath()));
            }
          }
        });

        allItem.addActionListener(ie -> {
          Enumeration em = ((DefaultMutableTreeNode)table.getTree().getModel().getRoot()).preorderEnumeration();
          while(em.hasMoreElements()) {
            table.getTree().expandPath(new TreePath(((ProductNode)em.nextElement()).getPath()));
          }
        });
        menu.add(item);
        menu.add(allItem);
        menu.show(table, e.getX(), e.getY());
      }

      if(!isArchive() && e.getClickCount() == 2 && 
              ((col == 1 || col == 2) && company != null || col == 4 || col == 5)) {
        Integer[] ids = col==1||col==2?getSelectedProductId():getSelectedProductId(true, true);
        
        JPopupMenu menu = new JPopupMenu();
        DurationReccurencePanel panel = new DurationReccurencePanel();
        List<List> data = ObjectLoader.getData("SELECT DISTINCT [Product("+(col==1||col==4?"duration":"recurrence")+")] FROM [Product] WHERE id=ANY(?)", 
                new Object[]{ids});
        if(data.size() == 1) {
          panel.setString((String)data.get(0).get(0));
        }

        panel.setButtonActionListener(ae -> {
          menu.setVisible(false);
          switch(col) {
            case 1:
              ((ProductNode)table.getTree().getPathForRow(row).getLastPathComponent()).setDuration(Utility.convert(panel.getString()));
              break;
            case 4:
              ((ProductNode)table.getTree().getPathForRow(row).getLastPathComponent()).setGlobDuration(Utility.convert(panel.getString()));
              break;
            case 2:
              ((ProductNode)table.getTree().getPathForRow(row).getLastPathComponent()).setRecurrence(Utility.convert(panel.getString()));
              break;
            case 5:
              ((ProductNode)table.getTree().getPathForRow(row).getLastPathComponent()).setGlobRecurrence(Utility.convert(panel.getString()));
              break;
          }
        });

        Rectangle rect = table.getCellRect(row, table.convertColumnIndexToView(col), true);
        menu.add(panel);
        menu.show(table, rect.x+rect.width-panel.getPreferredSize().width-2, rect.y+rect.height);
      }
    }
  }
}