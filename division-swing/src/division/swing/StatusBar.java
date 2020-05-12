package division.swing;

import java.awt.*;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

public class StatusBar extends JPanel {
  public StatusBar() {
    super(new GridBagLayout());
    setMinimumSize(new Dimension(0, 18));
    setPreferredSize(new Dimension(200, 18));
  }

  public void clear() {
    removeAll();
  }
  
  public void addStatus(Component component) {
    super.add(createPanel(component), new GridBagConstraints(getComponentCount(), 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
  }
  
  public void insertStatus(Component component, int index) {
    int r = index - getComponentCount();
    if(r >= 0) {
      for(int i=0;i<=r;i++) {
        if(i==r)
          addStatus(component);
        else addStatus(new JLabel(""));
      }
    }else {
      for(int j=getComponentCount()-1;j>=index;j--) {
        Component comp = getComponent(j);
        super.remove(j);
        super.add(comp, new GridBagConstraints(j+1, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
      }
      super.add(createPanel(component), new GridBagConstraints(index, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    }
  }
  
  public void setStatus(Component component, int index) {
    removeStatus(index);
    insertStatus(component,index);
  }
  
  public void removeStatus(int index) {
    if(index >= 0 && index < getComponentCount()) {
      super.remove(index);
      for(int i=getComponentCount()-1;i>=0;i--) {
        Component comp = getComponent(i);
        super.remove(i);
        super.add(comp, new GridBagConstraints(i, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
      }
    }
  }

  public void removeStatus(Component comp) {
    removeStatus(getComponentIndex(comp));
  }

  private JPanel createPanel(Component component) {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
    panel.add(component,new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(1, 2, 1, 2), 0, 0));
    return panel;
  }

  @Override
  public Component add(Component comp) {
    addStatus(comp);
    return comp;
  }

  @Override
  public void add(Component comp, Object constraints) {
    add(comp);
  }

  @Override
  public Component add(Component comp, int index) {
    insertStatus(comp, index);
    return comp;
  }

  @Override
  public Component add(String name, Component comp) {
    add(comp);
    return comp;
  }

  @Override
  public void add(Component comp, Object constraints, int index) {
    add(comp, index);
  }
  
  public int getComponentIndex(Component comp) {
    for(int i=0;i<getComponentCount();i++)
      if(((JPanel)getComponent(i)).getComponent(0).equals(comp))
        return i;
    return -1;
  }

  @Override
  public void remove(Component comp) {
    removeStatus(comp);
  }
}