package division.swing;

import java.awt.*;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.*;

public class LocalProcessing {
  private final JLabel descriptionLabel  = new JLabel("Ход выполнения задачи");
  private final JProgressBar progressBar = new JProgressBar();
  private final JLabel subLabel     = new JLabel("");
  private final JProgressBar subBar = new JProgressBar();
  private final JDialog dialog;
  private int maximum;
  private boolean useDialog = true;
  private boolean closeWhenStop = true;
  private final ExecutorService pool = Executors.newSingleThreadExecutor();
  private final TreeSet<LocalProcessingListener> listeners = new TreeSet<>();

  public LocalProcessing() {
    super();
    dialog = new JDialog();
    subLabel.setVisible(false);
    subBar.setVisible(false);
  }

  public LocalProcessing(Dialog dialog) {
    super();
    this.dialog = new JDialog(dialog);
    subLabel.setVisible(false);
    subBar.setVisible(false);
  }

  public LocalProcessing(Frame owner) {
    super();
    dialog = new JDialog(owner);
    subLabel.setVisible(false);
    subBar.setVisible(false);
  }

  public LocalProcessing(Window owner) {
    super();
    dialog = new JDialog(owner);
    subLabel.setVisible(false);
    subBar.setVisible(false);
  }

  public JDialog getDialog() {
    return dialog;
  }

  public boolean isCloseWhenStop() {
    return closeWhenStop;
  }

  public void setCloseWhenStop(boolean closeWhenStop) {
    this.closeWhenStop = closeWhenStop;
  }

	public void addLocalProcessingListener(LocalProcessingListener localProcessingListener) {
		listeners.add(localProcessingListener);
	}

	public void removeLocalProcessingListener(LocalProcessingListener localProcessingListener) {
		listeners.remove(localProcessingListener);
	}

	/*public void fireStart() {
		for(LocalProcessingListener localProcessingListener:listeners)
			localProcessingListener.start();
	}*/

	public void fireStop() {
		for(LocalProcessingListener localProcessingListener:listeners)
			localProcessingListener.stop();
	}

	public void setSubProgressVisible(boolean visible) {
    subBar.setVisible(visible);
    //dialog.pack();
	}

	public void setSubTextVisible(boolean visible) {
    subLabel.setVisible(visible);
    //dialog.pack();
	}

	public String getSubText() {
		return subLabel.getText();
	}

	public void setSubText(String rootText) {
		subLabel.setText(rootText);
	}

	public JProgressBar getSubBar() {
		return subBar;
	}

  public JProgressBar getProgressBar() {
    return progressBar;
  }

  public void setTitle(String title) {
    dialog.setTitle(title);
  }

  public String getTitle() {
    return dialog.getTitle();
  }

  public void setText(String text) {
    descriptionLabel.setText(text);
  }

  public String getText() {
    return descriptionLabel.getText();
  }

  public Future submit(Runnable rannable) {
    return pool.submit(rannable);
  }
  
  public Object call(Callable callable) throws InterruptedException, ExecutionException {
    return pool.submit(callable).get();
  }

  public void setAlwaysOnTop(boolean is) {
    dialog.setAlwaysOnTop(is);
  }

  public boolean isAlwaysOnTop() {
    return dialog.isAlwaysOnTop();
  }

  public void init() {
    progressBar.setMinimumSize(new Dimension(300, 20));
    progressBar.setPreferredSize(new Dimension(300, 20));
    progressBar.setStringPainted(true);
		subBar.setMinimumSize(new Dimension(300, 20));
    subBar.setPreferredSize(new Dimension(300, 20));
    subBar.setStringPainted(true);
    JPanel panel = new JPanel(new GridBagLayout());

		panel.add(descriptionLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    panel.add(progressBar,      new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		panel.add(subLabel,         new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    panel.add(subBar,           new GridBagConstraints(0, 3, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    dialog.setContentPane(panel);
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

	public void show() {
		init();
		dialog.setVisible(true);
	}

	public void hide() {
		dialog.setVisible(false);
	}

  public boolean isUseDialog() {
    return useDialog;
  }

  public void setUseDialog(boolean useDialog) {
    this.useDialog = useDialog;
  }

  public int getMaximum() {
    return maximum;
  }

  public void setMinMax(final int min,final int max) {
    maximum = max;
    progressBar.setMinimum(min);
    progressBar.setMaximum(max);
  }

  public void setSuibMinMax(final int min,final int max) {
    subBar.setMinimum(min);
    subBar.setMaximum(max);
  }

  public int getSubValue() {
          return subBar.getValue();
  }

  public void setSubValue(int value) {
          subBar.setValue(value);
  }

  public int getValue() {
    return progressBar.getValue();
  }

  public void setValue(final int value) {
    SwingUtilities.invokeLater(() -> {
      if(useDialog && !dialog.isVisible())
        show();
      progressBar.setValue(value);
      if(value >= maximum) {
        if(useDialog && isCloseWhenStop()) {
          dialog.setVisible(false);
          dialog.dispose();
        }
        progressBar.setValue(0);
        fireStop();
      }
    });
  }

  public interface LocalProcessingListener {
    public void stop();
  }
}