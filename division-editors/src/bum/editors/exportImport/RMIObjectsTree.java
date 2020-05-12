package bum.editors.exportImport;

import bum.editors.util.ObjectLoader;
import division.swing.guimessanger.Messanger;
import division.swing.tree.Node;
import division.swing.tree.Tree;
import java.rmi.RemoteException;
import java.util.Map;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import mapping.MappingObject;

public class RMIObjectsTree extends Tree {

  public RMIObjectsTree() {
    super(Tree.Type.SIMPLE);

    addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.getPath().getLastPathComponent();
        setDragEnabled(node.getChildCount()==0);
      }
    });

    addTreeWillExpandListener(new TreeWillExpandListener() {
      @Override
      public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        if(event.getPath().getLastPathComponent() instanceof ObjectNode) {
          ObjectNode node = (ObjectNode)event.getPath().getLastPathComponent();
          if(node.getChildCount() == 1) {
            if(((DefaultMutableTreeNode)node.getChildAt(0)).getUserObject().equals("null")) {
              fillTree(node,node.getFieldType());
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

  private void fillTree(Node node, Class clazz) {
    try {
      if(node == null)
        node = setRoot(ObjectLoader.getClientName(clazz));

      Map<String, Map<String,Object>> hash = ObjectLoader.getFieldsInfo(clazz);
      for(String key:hash.keySet()) {
        Map<String, Object> h = hash.get(key);
        Node child = new ObjectNode(this,h);
        node.add(child);
        if(isMappingObject((Class)h.get("TYPE")))
          child.add(new DefaultMutableTreeNode("null"));
      }
      expandRow(0);
    }catch(RemoteException ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  public void initClass(Class clazz) {
    fillTree(null, clazz);
  }

  private boolean isMappingObject(Class clazz) {
    Class[] interfaces = clazz.getInterfaces();
    for(Class in:interfaces)
      if(in == MappingObject.class || isMappingObject(in))
        return true;
    return false;
  }
}