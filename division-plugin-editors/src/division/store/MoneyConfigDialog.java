package division.store;

import bum.editors.EditorGui;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Group;
import bum.interfaces.Group.ObjectType;
import division.swing.DivisionScrollPane;
import division.swing.DivisionTableEditor;
import division.swing.DivisionTableRenderer;
import division.swing.TreeTable.TreeTable;
import division.swing.TreeTable.TreeTableModel;
import division.swing.TreeTable.TreeTableNode;
import java.awt.BorderLayout;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import javax.swing.tree.TreePath;
import util.filter.local.DBFilter;

public class MoneyConfigDialog extends EditorGui {
  private final MoneyNode      rootNode = new MoneyNode("Объекты", null, null, null);
  private final TreeTableModel model    = new TreeTableModel(rootNode);
  private TreeTable table;
  private DivisionScrollPane scroll;

  public MoneyConfigDialog() {
    super("Настройка валют", null);
    initComponents();
    initEvents();
  }

  @Override
  public Boolean okButtonAction() {
    return true;
  }

  @Override
  public void initData() throws RemoteException {
    List<List> data = ObjectLoader.getData(
            DBFilter.create(Group.class).AND_EQUAL("tmp", false).AND_EQUAL("type", Group.Type.CURRENT).AND_EQUAL("groupType", ObjectType.ВАЛЮТА), 
            "id","name","cost","main","parent");
    for(List d:data) {
      if(getNode((Integer) d.get(0)) == null) {
        MoneyNode     node       = new MoneyNode((String)d.get(1), (Integer)d.get(0), (BigDecimal)d.get(2), (boolean)d.get(3));
        TreeTableNode parentNode = getNode((Integer) d.get(4), true);
        if(parentNode != null) {
          parentNode.add(node);
          model.reload(parentNode);
          table.getTree().expandPath(new TreePath(parentNode.getPath()));
        }
      }
    }
  }
  
  private MoneyNode getNode(Integer id) {
    return getNode(id, false);
  }
  
  private MoneyNode getNode(Integer id, boolean selectFromDataBase) {
    if(id == null)
      return rootNode;
    Enumeration<MoneyNode> en = rootNode.preorderEnumeration();
    while(en.hasMoreElements()) {
      MoneyNode node = en.nextElement();
      if(!node.isRoot() && node.getId().equals(id))
        return node;
    }
    
    if(selectFromDataBase) {
      List<List> data = ObjectLoader.getData(Group.class, new Integer[]{id}, "id","name","cost","main","parent");
      if(!data.isEmpty()) {
        MoneyNode     node       = new MoneyNode((String)data.get(0).get(1), (Integer)data.get(0).get(0), (BigDecimal)data.get(0).get(2), (boolean)data.get(0).get(3));
        TreeTableNode parentNode = getNode((Integer) data.get(0).get(4), true);
        if(parentNode != null) {
          parentNode.add(node);
          return node;
        }
      }
    }
    return null;
  }

  private void initComponents() {
    model.addColumn("Цена",     "getCost", "setCost", BigDecimal.class);
    model.addColumn("Основная", "isMain",  "setMain", Boolean.class);
    table  = new TreeTable(model) {
      @Override
      public boolean isCellEditable(int row, int column) {
        MoneyNode node = (MoneyNode)table.getTree().getPathForRow(row).getLastPathComponent();
        return column==0||node.isMain()!=null&&node.getCost()!=null;
      }
    };
    scroll = new DivisionScrollPane(table);
    getRootPanel().setLayout(new BorderLayout());
    getRootPanel().add(scroll, BorderLayout.CENTER);
    
    table.setDefaultRenderer(Boolean.class, new DivisionTableRenderer());
    table.setDefaultEditor(Boolean.class, new DivisionTableEditor(table));
  }
  
  private void initEvents() {
  }

  @Override
  public void initTargets() {
  }
  
  public class MoneyNode extends TreeTableNode {
    private BigDecimal cost;
    private Boolean main;
    
    public MoneyNode(String name, Integer id, BigDecimal cost, Boolean main) {
      super(name, id);
      this.cost = cost;
      this.main = main;
    }

    public BigDecimal getCost() {
      return getChildCount() == 0?cost:null;
    }

    public void setCost(BigDecimal cost) {
      if(ObjectLoader.executeUpdate("UPDATE [Group] SET [Group(cost)]=? WHERE [Group(id)]=?", new Object[]{cost, getId()}) == 1)
        this.cost = cost;
    }

    public Boolean isMain() {
      return getChildCount() == 0?main:null;
    }

    public void setMain(Boolean main) {
      //System.out.println(getUserObject()+" "+main);
      if(ObjectLoader.executeUpdate("UPDATE [Group] SET [Group(main)]=? WHERE [Group(id)]=?", new Object[]{main, getId()}) == 1)
        this.main = main;
    }
  }
}