package division.Desktop;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultDesktopManager;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JToolBar;

public class DivisionDesktopManager extends DefaultDesktopManager {
  private JToolBar tool;
  
  public DivisionDesktopManager(JToolBar tool) {
    this.tool = tool;
  }

  @Override
  public void openFrame(final JInternalFrame f) {
    super.openFrame(f);
    
    f.getDesktopPane().add(f.getDesktopIcon());
    f.getDesktopIcon().setVisible(false);
    
    JButton button = new JButton(f.getTitle());
    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          if(f.isIcon()) {
            f.setIcon(false);
            f.setSelected(true);
          }else {
            if(!f.isSelected())
              f.setSelected(true);
            else f.setIcon(true);
          }
        }catch(Exception ex) {
          ex.printStackTrace();
        }
      }
    });
    
    tool.add(button);
  }

  @Override
  public void iconifyFrame(final JInternalFrame f) {
    super.iconifyFrame(f);
    
    f.getDesktopIcon().setVisible(false);
    
    JButton button = new JButton(f.getTitle());
    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          if(f.isIcon()) {
            f.setIcon(false);
            f.setSelected(true);
          }else {
            if(!f.isSelected())
              f.setSelected(true);
            else f.setIcon(true);
          }
        }catch(Exception ex) {
          ex.printStackTrace();
        }
      }
    });
    
    tool.add(button);
  }
}