package division.swing.tree;

import division.swing.tree.Tree;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

public class Node extends DefaultMutableTreeNode implements ItemListener {
  private JCheckBox box   = new JCheckBox();
  private JPanel    panel = new JPanel(new GridBagLayout());
  private JLabel    label = new JLabel();
  protected Tree    tree;
  private boolean childSelect = true;
  
  private int state = 0;
  
  public Node(String name, Tree tree) {
    super(name);
    this.tree = tree;
    label.setText(name);
    box.addItemListener(this);
    box.addMouseListener(new myMouseListener());
    box.setMargin(new Insets(0,0,0,0));
    panel.setBorder(BorderFactory.createEmptyBorder());
    panel.add(box,new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    panel.add(label,new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
  }
    
  @Override
  public Object getUserObject() {
    return panel;
  }
  
  @Override
  public void setUserObject(Object obj) {
    this.getLabel().setText((String)obj);
  }

  @Override
  public void itemStateChanged(ItemEvent e) {
    if(tree.getType() == Tree.Type.CHECKBOXSES) {
      tree.fireNodeChecked(this);
      if(tree.isInheritance()) {
        Node parentNode = (Node)this.getParent();
        if(parentNode!=null) {
          parentNode.checkEnable();
          if(parentNode.isSelected() != isSelected()) {
            if(isSelected())
              parentNode.setSelected(true);
            else if(parentNode.getSelectedChilds().length == 0)
              parentNode.setSelected(false);
          }
        }
        if(this.isEnabled() && childSelect) {
          for(int i=0;i<this.getChildCount();i++) {
            if(!this.isSelected())
              ((Node)this.getChildAt(i)).setEnabled(true);
            ((Node)this.getChildAt(i)).setSelected(this.isSelected());
          }
        }
      }
    }
    tree.getTreeModel().nodeChanged(this.getRoot());
  }

  public boolean isChildSelect() {
    return childSelect;
  }

  public void setChildSelect(boolean childSelect) {
    this.childSelect = childSelect;
  }
  
  public Node[] getSelectedChilds() {
    ArrayList<Node> nodes = new ArrayList<>();
    for(int i=0;i<getChildCount();i++)
      if(((Node)getChildAt(i)).isSelected())
        nodes.add((Node)getChildAt(i));
    return nodes.toArray(new Node[nodes.size()]);
  }
  
  public boolean isChildDeselectedOrDisabled() {
    for(int i=0;i<getChildCount();i++)
      if (!((Node)getChildAt(i)).isSelected() || !((Node)getChildAt(i)).isEnabled()) return true;
    return false;
  }
  
  public boolean isSelected() {
    return box.isSelected();
  }

  public void setSelected(boolean select) {
    box.setSelected(select);
  }
  
  private void checkEnable() {
    if(tree.getType() == Tree.Type.CHECKBOXSES) {
      boolean isAllUnSelected = getSelectedChilds().length == 0;
      boolean isAllSelected = getSelectedChilds().length == getChildCount();

      if((isAllSelected && isChildDeselectedOrDisabled()) || (!isAllSelected && !isAllUnSelected))
        this.setEnabled(false);

      if(isAllUnSelected || (isAllSelected && !isChildDeselectedOrDisabled()))
        this.setEnabled(true);
    }
  }
  
  public boolean isEnabled() {
    return box.isEnabled();
  }
  
  public void setEnabled(boolean enable) {
    box.setEnabled(enable);
    if(!this.isRoot())
      ((Node)this.getParent()).checkEnable();
  }
  
  public JLabel getLabel() {
    return this.label;
  }

  public JCheckBox getBox() {
    return this.box;
  }
  
  public void setCheckBoxVisible(boolean isVisible) {
    this.box.setVisible(isVisible);
  }
  
  class myMouseListener extends MouseAdapter {
    @Override
    public void mousePressed(MouseEvent e) {
      if(tree.getType() == Tree.Type.INTEGRETED_CHECKBOXSES) {
        Node parent;
        switch(state) {
          case 0:
            setEnabled(false);
            setSelected(true);
            state = 1;
            break;
          case 1:
            setEnabled(true);
            parent = (Node) getParent();
            while(parent != null && !parent.isRoot()) {
              parent.setSelected(isEnabled());
              parent.setEnabled(!isEnabled());
              parent = (Node) parent.getParent();
            }
            state = 2;
            break;
          case 2:
            setEnabled(false);
            parent = (Node) getParent();
            while(parent != null && !parent.isRoot()) {
              parent.setSelected(isEnabled());
              parent.setEnabled(!isEnabled());
              parent = (Node) parent.getParent();
            }
            state = 3;
            break;
          case 3:
            setEnabled(true);
            setSelected(false);
            state = 0;
            break;
        }
      }else {
        if(!isEnabled()) {
          setEnabled(true);
          setSelected(!isSelected());
        }
      }
    }
  }
}