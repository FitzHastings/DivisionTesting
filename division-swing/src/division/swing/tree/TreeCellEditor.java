package division.swing.tree;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

public class TreeCellEditor extends DefaultTreeCellEditor {
  private Component component;
  public TreeCellEditor (JTree tree, DefaultTreeCellRenderer renderer) {
    super(tree, renderer);
  }
   
  @Override
  public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
    component = (Component)((Node)value).getUserObject();
    component.setBackground(((Tree)tree).getSelectionBackground());
   ((Node)value).getLabel().setForeground(((Tree)tree).getSelectionForeground());
   ((JPanel)component).setBorder(BorderFactory.createLineBorder(((Tree)tree).getSelectionBorderColor()));
    return component;
   }
    
  @Override
  public boolean isCellEditable(EventObject event) {
     if(event != null) {
       TreePath path = tree.getPathForLocation(
                                           ((MouseEvent)event).getX(),
                                           ((MouseEvent)event).getY());
       if(path != null) {
         Node node = (Node)path.getLastPathComponent();
         Rectangle r = tree.getRowBounds(tree.getRowForPath(path));
         Rectangle rec = new Rectangle(r.x,r.y,node.getBox().getWidth(),node.getBox().getHeight());
         return rec.contains(((MouseEvent)event).getPoint());
       }
     }
     return super.isCellEditable(event);
   }
  
  @Override
  public Object getCellEditorValue() {
    return null;
  }
   
  @Override
  public boolean stopCellEditing() {
    return super.stopCellEditing();
  }

  @Override
  public void cancelCellEditing() {
    super.cancelCellEditing();
  }
}