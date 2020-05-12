package division.Desktop;

import javax.swing.JInternalFrame;

public class DivisionInternalFrame extends JInternalFrame {
  public DivisionInternalFrame(String title, boolean resizable, boolean closable, boolean maximizable, boolean iconifiable) {
    super(title, resizable, closable, maximizable, iconifiable);
    init();
  }

  public DivisionInternalFrame(String title, boolean resizable, boolean closable, boolean maximizable) {
    super(title, resizable, closable, maximizable);
    init();
  }

  public DivisionInternalFrame(String title, boolean resizable, boolean closable) {
    super(title, resizable, closable);
    init();
  }

  public DivisionInternalFrame(String title, boolean resizable) {
    super(title, resizable);
    init();
  }

  public DivisionInternalFrame(String title) {
    super(title);
    init();
  }

  public DivisionInternalFrame() {
    init();
  }
  
  public void init() {
  }
}