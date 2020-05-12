package division.swing.tree;

import java.awt.Color;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

public class TreeCellRenderer extends DefaultTreeCellRenderer {
  @Override
  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
    JComponent component = (JComponent)((Node)value).getUserObject();
    
    setBackground(component,tree.getBackground());
    ((Node)value).getLabel().setForeground(tree.getForeground());
    ((JPanel)component).setBorder(BorderFactory.createEmptyBorder());
    if(sel) {
      setBackground(component,((Tree)tree).getSelectionBackground());
      ((Node)value).getLabel().setForeground(((Tree)tree).getSelectionForeground());
      ((JPanel)component).setBorder(BorderFactory.createLineBorder(((Tree)tree).getSelectionBorderColor()));
    }
    if(((Node)value).getBox().isVisible())
      ((Node)value).setCheckBoxVisible(((Tree)tree).getType() == Tree.Type.CHECKBOXSES || ((Tree)tree).getType() == Tree.Type.INTEGRETED_CHECKBOXSES);
    return component;
  }

  private void setBackground(JComponent com, Color color) {
    com.setBackground(color);
    for(int i=0;i<com.getComponentCount();i++)
      if(com.getComponent(i) instanceof JComponent)
        setBackground((JComponent)com.getComponent(i), color);
      else com.getComponent(i).setBackground(color);
  }
}
