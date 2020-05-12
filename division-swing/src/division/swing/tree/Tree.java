package division.swing.tree;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class Tree extends JTree {
  private DefaultTreeModel Model = (DefaultTreeModel)getModel();
  public enum Type{SIMPLE,CHECKBOXSES,INTEGRETED_CHECKBOXSES,DND}
  public boolean inheritance = true;
  private Type type;
  
  private Color selectionForeground = Color.BLACK;
  private Color selectionBackground = Color.LIGHT_GRAY;
  private Color selectionBorderColor = Color.BLACK;
  
  private ArrayList<TreeNodeCheckBoxListener> listeners = new ArrayList<>();

  private JTextField findText  = new JTextField();
  private JPopupMenu pop = new JPopupMenu("Поиск");

  private TreePath lastPath = null;
  private boolean hierarchy = false;
  
  public Tree(Type type) {
    this.type = type;
    this.setModel(Model);
    setShowsRootHandles(true);
    initEvents();
    pop.add(findText);
    findText.setColumns(20);
  }

  public boolean isInheritance() {
    return inheritance;
  }

  public void setInheritance(boolean inheritance) {
    this.inheritance = inheritance;
  }

  private void findNextText(DefaultMutableTreeNode node) {
    String name;
    String text = findText.getText().toLowerCase();
    DefaultMutableTreeNode root = (DefaultMutableTreeNode)getTreeModel().getRoot();
    Enumeration<DefaultMutableTreeNode> em = root.preorderEnumeration();
    boolean search = node == null;
    while(em.hasMoreElements()) {
      DefaultMutableTreeNode n = em.nextElement();
      if(!search) {
        search = n.equals(node);
        continue;
      }
      if(search) {
        if(!n.isRoot()) {
          if(n instanceof Node)
            name = ((Node)n).getLabel().getText().toString().toLowerCase();
          else name = n.getUserObject().toString().toLowerCase();
          if(n.getChildCount() == 0 && name.indexOf(text) != -1) {
            setSelectionPath(new TreePath(n.getPath()));
            scrollPathToVisible(getSelectionPath());
            break;
          }
        }
      }
    }
  }

  public boolean isHierarchy() {
    return hierarchy;
  }

  public void setHierarchy(boolean hierarchy) {
    this.hierarchy = hierarchy;
    if(!hierarchy) {
      setSelectionBackground(Color.LIGHT_GRAY);
      repaint();
    }
  }

  private void initEvents() {
    addKeyListener(new KeyAdapter() {
      @Override
      public void keyTyped(KeyEvent e) {
        if(Character.isLetter(e.getKeyChar()) || Character.isDigit(e.getKeyChar())) {
          pop.show(getParent(), (getParent().getWidth()/2)-100, (getParent().getHeight()/2)-10);
          pop.setLocation((int)(getParent().getLocationOnScreen().getX() + (getParent().getWidth() / 2) - (findText.getWidth() / 2)),(int)(getParent().getLocationOnScreen().getY() + (getParent().getHeight() / 2) - 10));
          findText.setText("");
          findText.grabFocus();
          findText.setText(String.valueOf(e.getKeyChar()));
        }
      }
    });

    findText.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_F3)
          findNextText((DefaultMutableTreeNode)getSelectionPath().getLastPathComponent());
      }
    });

    findText.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        findNextText(null);
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
      }
    });

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if(isHierarchy() && getSelectionModel().getSelectionCount() == 1) {
          if(getSelectionPath().equals(lastPath)) {
            if(getPathForLocation(e.getX(), e.getY()).equals(getSelectionPath())) {
              if(getSelectionBackground().equals(Color.LIGHT_GRAY))
                setSelectionBackground(Color.WHITE);
              else setSelectionBackground(Color.LIGHT_GRAY);
              repaint();
              fireValueChanged(new TreeSelectionEvent(Tree.this, lastPath, false, lastPath, lastPath));
            }else setSelectionBackground(Color.LIGHT_GRAY);
          }else lastPath = getSelectionPath();
        }
      }
    });

    addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        lastPath = e.getOldLeadSelectionPath();
      }
    });
  }
  
  public void paintPathArrow(TreePath path, int x, int y) {
    Rectangle pathBounds = this.getPathBounds(path);
    int y0 = pathBounds.y + pathBounds.height/2;
    int x0 = pathBounds.x + pathBounds.width;
    
    int[] xPoints = new int[]{x0,x0+3,x0+3,x0+6,x0+6,x0+3,x0+3,x0};
    int[] yPoints = new int[]{y0,y0-3,y0-1,y0-1,y0+1,y0+1,y0+3,y0};
    
    getGraphics().drawPolygon(xPoints, yPoints, xPoints.length);
  }
  
  public Node setRoot(Node root) {
    Model.setRoot(root);
    return root;
  }
  
  public Node setRoot(String rootName) {
    Node root = new Node(rootName,this);
    return this.setRoot(root);
  }
  
  public DefaultTreeModel getTreeModel() {
    return Model;
  }
  
  public Type getType() {
    return this.type;
  }
  
  public void setType(Type type) {
    this.type = type;
    if(this.type == Type.SIMPLE)
      clearAllCheckBoxes();
    if(getCellEditor() != null)
      getCellEditor().cancelCellEditing();
    Enumeration<Node> em = ((Node)this.Model.getRoot()).preorderEnumeration();
    while(em.hasMoreElements())
      Model.nodeChanged(em.nextElement());
  }
  
  public void clearAllCheckBoxes() {
    ((Node)this.Model.getRoot()).setEnabled(true);
    ((Node)this.Model.getRoot()).setSelected(false);
  }
  
  public void setSelectionForeground(Color clr){this.selectionForeground=clr;}
  public Color getSelectionForeground(){return this.selectionForeground;}
  
  public void setSelectionBackground(Color clr){this.selectionBackground=clr;}
  public Color getSelectionBackground(){return this.selectionBackground;}
  
  public void setSelectionBorderColor(Color clr){this.selectionBorderColor=clr;}
  public Color getSelectionBorderColor(){return this.selectionBorderColor;}
  
  public void addTreeNodeChekBoxListener(TreeNodeCheckBoxListener listener){listeners.add(listener);}
  public void removeTreeNodeChekBoxListener(TreeNodeCheckBoxListener listener){listeners.remove(listener);}
  
  public void fireNodeChecked(Node node) {
    if(node.isSelected()) {
      /*this.clearSelection();
      this.setSelectionBackground(this.getBackground());
      this.setSelectionForeground(this.getForeground());
      this.setSelectionBorderColor(this.getBackground());*/
    }
    else if(getCheckedNodes().length == 0) {
      /*this.setSelectionBackground(Color.LIGHT_GRAY);
      this.setSelectionForeground(Color.BLACK);
      this.setSelectionBorderColor(Color.BLACK);*/
    }
    for(TreeNodeCheckBoxListener listener : listeners)
      listener.checkedNode(node);
  }
  /**
   * Возвращает только конечные узлы выделенных ветвей
   * @return массив узлов
   */
  public Node[] getSelectedLastNodes() {
    ArrayList<Node> nodes = new ArrayList<>();
    Node node;
    TreePath[] paths = getSelectionPaths();
    if(paths == null)
      return new Node[0];
    for(int i=0;i<paths.length;i++) {
      node = (Node)paths[i].getLastPathComponent();
      Enumeration em = node.preorderEnumeration();
      while(em.hasMoreElements()) {
        Node n = (Node)em.nextElement();
        if(n.getChildCount() == 0)
          nodes.add(n);
      }
    }
    return nodes.toArray(new Node[nodes.size()]);
  }
  
  public Node[] getSelectedNodes() {
    ArrayList<Node> nodes = new ArrayList<>();
    Node node;
    TreePath[] paths = getSelectionPaths();
    if(paths == null)
      return new Node[0];
    for(int i=0;i<paths.length;i++) {
      node = (Node)paths[i].getLastPathComponent();
      nodes.add(node);
    }
    return nodes.toArray(new Node[nodes.size()]);
  }
    
  public Node[] getCheckedNodes() {
    ArrayList<Node> nodes = new ArrayList<>();
    Node node;
    @SuppressWarnings("unchecked")
    Enumeration<Node> em = ((Node)Model.getRoot()).preorderEnumeration();
    while(em.hasMoreElements()) {
      node = em.nextElement();
      if(!node.isRoot() && node.isSelected())
        nodes.add(node);
    }
    return nodes.toArray(new Node[nodes.size()]);
  }
}