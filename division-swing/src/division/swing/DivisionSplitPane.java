package division.swing;

import division.util.FileLoader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public class DivisionSplitPane extends JSplitPane {
  private DivisionToolButton leftOrUp    = new DivisionToolButton();
  private DivisionToolButton rightOrDown = new DivisionToolButton();

  private int normalLocation;

  private List<SplitListener> listeners = new ArrayList<>();

  public DivisionSplitPane(int newOrientation, boolean newContinuousLayout, Component newLeftComponent, Component newRightComponent) {
    super(newOrientation, newContinuousLayout, newLeftComponent, newRightComponent);
    init();
  }

  public DivisionSplitPane(int newOrientation, Component newLeftComponent, Component newRightComponent) {
    super(newOrientation, newLeftComponent, newRightComponent);
    init();
  }

  public DivisionSplitPane(int newOrientation, boolean newContinuousLayout) {
    super(newOrientation, newContinuousLayout);
    init();
  }

  public DivisionSplitPane(int newOrientation) {
    super(newOrientation);
    init();
  }

  public DivisionSplitPane() {
    init();
  }

  public int getNormalLocation() {
    return normalLocation;
  }

  public void setNormalLocation(int normalLocation) {
    this.normalLocation = normalLocation;
  }
  
  private void setDividerButtons() {
    if(isOneTouchExpandable()) {
      BasicSplitPaneDivider divider = ((BasicSplitPaneUI)getUI()).getDivider();
      divider.removeAll();
      divider.setLayout(new GridBagLayout());
      if(getOrientation() == HORIZONTAL_SPLIT) {
        leftOrUp.setIcon(FileLoader.getIcon("arrow4_left.GIF"));
        rightOrDown.setIcon(FileLoader.getIcon("arrow4_right.GIF"));
        setDividerSize(leftOrUp.getPreferredSize().width+4);
        divider.add(leftOrUp,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 0), 0, 0));
        divider.add(rightOrDown, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 0), 0, 0));
      }else {
        leftOrUp.setIcon(FileLoader.getIcon("arrow4_up.GIF"));
        rightOrDown.setIcon(FileLoader.getIcon("arrow4_down.GIF"));
        setDividerSize(leftOrUp.getPreferredSize().height+4);
        divider.add(leftOrUp,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
        divider.add(rightOrDown, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
      }
    }
  }

  @Override
  public void setOneTouchExpandable(boolean newValue) {
    super.setOneTouchExpandable(newValue);
    setDividerButtons();
  }

  @Override
  public void setOrientation(int orientation) {
    super.setOrientation(orientation);
    setDividerButtons();
  }

  public int getLastLocation() {
    return (getOrientation() == HORIZONTAL_SPLIT?getSize().width:getSize().height)-getDividerSize();
  }

  private void init() {
    leftOrUp.setBorder(BorderFactory.createEmptyBorder());
    rightOrDown.setBorder(BorderFactory.createEmptyBorder());
    leftOrUp.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    rightOrDown.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

    leftOrUp.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int location = getDividerLocation();
        if(location != getLastLocation()) {
          normalLocation = location;
          setDividerLocation(0);
          fireLeftUp();
        }else setDividerLocation(normalLocation);
      }
    });

    rightOrDown.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int location = getDividerLocation();
        if(location != 1/*getLastLocation()*/) {
          normalLocation = location;
          setDividerLocation(1.0);
          fireRightDown();
        }else setDividerLocation(normalLocation);
      }
    });

    addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if(evt.getPropertyName().equalsIgnoreCase("dividerLocation")) {
          int location = (Integer)evt.getNewValue();
            fireChangeDividerLocation(location);
        }
      }
    });
  }
  
  public void addSplitListener(SplitListener listener) {
    if(!listeners.contains(listener))
      listeners.add(listener);
  }
  
  public void removeSplitListener(SplitListener listener) {
    listeners.remove(listener);
  }

  public void fireChangeDividerLocation(int dividerLocation) {
    for(SplitListener listener:listeners)
      listener.changeDividerLocation(dividerLocation);
  }

  public void fireLeftUp() {
    for(SplitListener listener:listeners)
      if(getOrientation() == HORIZONTAL_SPLIT)
        listener.left();
      else listener.up();
  }

  public void fireRightDown() {
    for(SplitListener listener:listeners)
      if(getOrientation() == HORIZONTAL_SPLIT)
        listener.right();
      else listener.down();
  }
}