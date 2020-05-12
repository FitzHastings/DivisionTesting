package division.swing;

import java.awt.Dialog;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

public class ProgressBarDialog extends JDialog {
  private JLabel descriptionLabel  = new JLabel("Ход выполнения задачи");
  private JProgressBar progressBar = new JProgressBar();
  protected ExecutorService pool = Executors.newSingleThreadExecutor();
  
  public ProgressBarDialog(Window owner, String title, ModalityType modalityType, GraphicsConfiguration gc) {
    super(owner, title, modalityType, gc);
    init();
  }

  public ProgressBarDialog(Window owner, String title, ModalityType modalityType) {
    super(owner, title, modalityType);
    init();
  }

  public ProgressBarDialog(Window owner, String title) {
    super(owner, title);
    init();
  }

  public ProgressBarDialog(Window owner, ModalityType modalityType) {
    super(owner, modalityType);
    init();
  }

  public ProgressBarDialog(Window owner) {
    super(owner);
    init();
  }

  public ProgressBarDialog(Dialog owner, String title, boolean modal, GraphicsConfiguration gc) {
    super(owner, title, modal, gc);
    init();
  }

  public ProgressBarDialog(Dialog owner, String title, boolean modal) {
    super(owner, title, modal);
    init();
  }

  public ProgressBarDialog(Dialog owner, String title) {
    super(owner, title);
    init();
  }

  public ProgressBarDialog(Dialog owner, boolean modal) {
    super(owner, modal);
    init();
  }

  public ProgressBarDialog(Dialog owner) {
    super(owner);
    init();
  }

  public ProgressBarDialog(Frame owner, String title, boolean modal, GraphicsConfiguration gc) {
    super(owner, title, modal, gc);
    init();
  }

  public ProgressBarDialog(Frame owner, String title, boolean modal) {
    super(owner, title, modal);
    init();
  }

  public ProgressBarDialog(Frame owner, String title) {
    super(owner, title);
    init();
  }

  public ProgressBarDialog(Frame owner, boolean modal) {
    super(owner, modal);
    init();
  }

  public ProgressBarDialog(Frame owner) {
    super(owner);
    init();
  }

  public ProgressBarDialog() {
    super();
    init();
  }
  
  public void setDescription(String description) {
    descriptionLabel.setText(description);
  }
  
  public String getDescription() {
    return descriptionLabel.getText();
  }

  public JProgressBar getProgressBar() {
    return progressBar;
  }
  
  public void submit(Runnable task) {
    pool.submit(task);
  }
  
  private void init() {
    setAlwaysOnTop(true);
    getContentPane().setLayout(new GridBagLayout());
    getContentPane().add(descriptionLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    getContentPane().add(progressBar,      new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
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
}