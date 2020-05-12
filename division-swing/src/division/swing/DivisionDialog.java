package division.swing;

import java.awt.*;
import javax.swing.JDialog;

public class DivisionDialog extends JDialog {
  public DivisionDialog(Window owner, String title, ModalityType modalityType, GraphicsConfiguration gc) {
    super(owner, title, modalityType, gc);
    init();
  }

  public DivisionDialog(Window owner, String title, ModalityType modalityType) {
    super(owner, title, modalityType);
    init();
  }

  public DivisionDialog(Window owner, String title) {
    super(owner, title);
    init();
  }

  public DivisionDialog(Window owner, ModalityType modalityType) {
    super(owner, modalityType);
    init();
  }

  public DivisionDialog(Window owner) {
    super(owner);
    init();
  }

  public DivisionDialog(Dialog owner, String title, boolean modal, GraphicsConfiguration gc) {
    super(owner, title, modal, gc);
    init();
  }

  public DivisionDialog(Dialog owner, String title, boolean modal) {
    super(owner, title, modal);
    init();
  }

  public DivisionDialog(Dialog owner, String title) {
    super(owner, title);
    init();
  }

  public DivisionDialog(Dialog owner, boolean modal) {
    super(owner, modal);
    init();
  }

  public DivisionDialog(Dialog owner) {
    super(owner);
    init();
  }

  public DivisionDialog(Frame owner, String title, boolean modal, GraphicsConfiguration gc) {
    super(owner, title, modal, gc);
    init();
  }

  public DivisionDialog(Frame owner, String title, boolean modal) {
    super(owner, title, modal);
    init();
  }

  public DivisionDialog(Frame owner, String title) {
    super(owner, title);
    init();
  }

  public DivisionDialog(Frame owner, boolean modal) {
    super(owner, modal);
    init();
  }

  public DivisionDialog(Frame owner) {
    super(owner);
    init();
  }

  public DivisionDialog() {
    super();
    init();
  }
  
  public void centerLocation() {
    pack();
    try {
      Point location = getParent().getLocationOnScreen();
      int x = (int)(getParent().getSize().width - getSize().width)/2;
      int y = (int)(getParent().getSize().height - getSize().height)/2;
      setLocation(location.x+x, location.y+y);
    }catch(Exception ex) {
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      int x = (int)(screenSize.width - getSize().width)/2;
      int y = (int)(screenSize.height - getSize().height)/2;
      setLocation(x, y);
    }
  }

  @Override
  public void setModal(boolean modal) {
    super.setModal(modal);
    setAlwaysOnTop(modal);
  }

  public void init() {
    setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    setAlwaysOnTop(isModal());
  }
}