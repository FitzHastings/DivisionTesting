package bum.util;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

public class SimpleProcessingImpl extends UnicastRemoteObject implements SimpleProgressing {
  private JLabel descriptionLabel  = new JLabel("Ход выполнения задачи");
  private JProgressBar progressBar = new JProgressBar();
  private JDialog dialog;
  private int maximum = 0;
  private boolean useDialog = true;
  private ExecutorService pool = Executors.newSingleThreadExecutor();
  private List<Action> actions = new ArrayList<>();
  private boolean run = true;

  public SimpleProcessingImpl() throws RemoteException {
    super();
    dialog = new JDialog();
  }

  public SimpleProcessingImpl(Dialog dialog) throws RemoteException {
    super();
    this.dialog = new JDialog(dialog);
  }

  public SimpleProcessingImpl(Frame owner) throws RemoteException {
    super();
    dialog = new JDialog(owner);
  }

  public SimpleProcessingImpl(Window owner) throws RemoteException {
    super();
    dialog = new JDialog(owner);
  }

  @Override
  public boolean isRun() throws RemoteException {
    return this.run;
  }
  
  @Override
  public void shutdown() throws RemoteException {
    this.run = false;
    System.out.println("SHUTDOWN");
  }

  public JProgressBar getProgressBar() {
    return progressBar;
  }
  
  @Override
  public void setTitle(String title) throws RemoteException {
    dialog.setTitle(title);
  }
  
  @Override
  public String getTitle() throws RemoteException {
    return dialog.getTitle();
  }

  @Override
  public void setText(String text) throws RemoteException {
    descriptionLabel.setText(text);
  }

  @Override
  public String getText() throws RemoteException {
    return descriptionLabel.getText();
  }

  public Future submit(Runnable rannable) {
    return pool.submit(rannable);
  }

  public void addAction(Action action) {
    actions.add(action);
  }

  public void init() {
    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        try {
          shutdown();
          exit();
        }catch (RemoteException ex) {}
      }
    });
    
    dialog.setAlwaysOnTop(true);
    progressBar.setMinimumSize(new Dimension(300, 20));
    progressBar.setPreferredSize(new Dimension(300, 20));
    progressBar.setStringPainted(true);
    dialog.getContentPane().setLayout(new GridBagLayout());
    dialog.getContentPane().add(descriptionLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    dialog.getContentPane().add(progressBar,      new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    dialog.pack();
    try {
      Point location = dialog.getParent().getLocationOnScreen();
      int x = (int)(dialog.getParent().getSize().width - dialog.getSize().width)/2;
      int y = (int)(dialog.getParent().getSize().height - dialog.getSize().height)/2;
      dialog.setLocation(location.x+x, location.y+y);
    }catch(Exception ex) {
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      int x = (int)(screenSize.width - dialog.getSize().width)/2;
      int y = (int)(screenSize.height - dialog.getSize().height)/2;
      dialog.setLocation(x, y);
    }
  }

  public boolean isUseDialog() {
    return useDialog;
  }

  public void setUseDialog(boolean useDialog) {
    this.useDialog = useDialog;
  }

  @Override
  public int getMaximum() throws RemoteException {
    return maximum;
  }

  @Override
  public void setMinMax(int min, int max) throws RemoteException {
    maximum = max;
    progressBar.setMinimum(min);
    progressBar.setMaximum(max);
  }

  public void show() throws RemoteException {
    dialog.setVisible(true);
    run = true;
  }

  public void hide() throws RemoteException {
    dialog.setVisible(false);
    dialog.dispose();
    shutdown();
  }
  
  @Override
  public Integer getValue() throws RemoteException {
    return progressBar.getValue();
  }

  @Override
  public void setValue(int value) throws RemoteException {
    progressBar.setValue(value);
    if(value == maximum) {
      if(useDialog) {
        hide();
        finish();
      }
      progressBar.setValue(0);
    }else {
      if(useDialog && !dialog.isVisible() && run) {
        init();
        show();
        start();
      }
    }
  }
  
  public void addData(Object data) throws RemoteException {};
  public void start() throws RemoteException {};
  public void finish() throws RemoteException {};
  public void exit() throws RemoteException {};
}