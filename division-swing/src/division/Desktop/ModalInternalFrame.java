package division.Desktop;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.beans.*;

public class ModalInternalFrame extends JInternalFrame {

  public ModalInternalFrame(String title, JRootPane
      rootPane, Component desktop, JComponent pane) {
    super(title);

    // create opaque glass pane
    final JPanel glass = new JPanel();
    glass.setOpaque(false);

    // Attach mouse listeners
    MouseInputAdapter adapter =
      new MouseInputAdapter(){};
    glass.addMouseListener(adapter);
    glass.addMouseMotionListener(adapter);

    // Add in option pane
    getContentPane().add(pane, BorderLayout.CENTER);

    // Define close behavior
    PropertyChangeListener pcl =(PropertyChangeEvent event) -> {
          if (isVisible() &&
                  (event.getPropertyName().equals(
                          JOptionPane.VALUE_PROPERTY) ||
                  event.getPropertyName().equals(
                          JOptionPane.INPUT_VALUE_PROPERTY))) {
            try {
              setClosed(true);
            } catch (PropertyVetoException ignored) {
            }
            ModalInternalFrame.this.setVisible(true);
            glass.setVisible(true);
          }
    };
    pane.addPropertyChangeListener(pcl);

    // Change frame border
    putClientProperty("JInternalFrame.frameType",
      "optionDialog");

    // Size frame
    Dimension size = getPreferredSize();
    Dimension rootSize = desktop.getSize();

    setBounds((rootSize.width - size.width) / 2,
              (rootSize.height - size.height) / 2,
              size.width, size.height);

    desktop.validate();
    try {
      setSelected(true);
    } catch (PropertyVetoException ignored) {
    }

    // Add modal internal frame to glass pane
    glass.add(this);

    // Change glass pane to our panel
    rootPane.setGlassPane(glass);

    // Show glass pane, then modal dialog
    glass.setVisible(true);
  }

  @Override
  public void setVisible(boolean value) {
    super.setVisible(value);
    if (value) {
      startModal();
    } else {
      stopModal();
    }
  }

  private synchronized void startModal() {
    try {
      if (SwingUtilities.isEventDispatchThread()) {
        EventQueue theQueue =
          getToolkit().getSystemEventQueue();
        while (isVisible()) {
          AWTEvent event = theQueue.getNextEvent();
          Object source = event.getSource();
          if (event instanceof ActiveEvent) {
            ((ActiveEvent)event).dispatch();
          } else if (source instanceof Component) {
            ((Component)source).dispatchEvent(
              event);
          } else if (source instanceof MenuComponent) {
            ((MenuComponent)source).dispatchEvent(
              event);
          } else {
            System.err.println(
              "Unable to dispatch: " + event);
          }
        }
      } else {
        while (isVisible()) {
          wait();
        }
      }
    } catch (InterruptedException ignored) {
    }
  }

  private synchronized void stopModal() {
    notifyAll();
  }

  public static void main(String args[]) {
    final JFrame frame = new JFrame(
      "Modal Internal Frame");
    frame.setDefaultCloseOperation(
      JFrame.EXIT_ON_CLOSE);

    final JDesktopPane desktop = new JDesktopPane();

    ActionListener showModal =
        new ActionListener() {
      Integer ZERO = new Integer(0);
      Integer ONE = new Integer(1);
      @Override
      public void actionPerformed(ActionEvent e) {

//////////////////////////////////////////////////////////////////////        
        //Old code
        // Manually construct an input popup
        //JOptionPane optionPane = new JOptionPane(
        //  "Print?", JOptionPane.QUESTION_MESSAGE,
        //  JOptionPane.YES_NO_OPTION);

        //My code
        JButton btn = new JButton("Apply");
        btn.setBounds(10,10,100,20);

        JPanel optionPane = new JPanel();
        optionPane.setLayout(null);
        optionPane.setSize(200,200);        
        optionPane.add(btn);
//////////////////////////////////////////////////////////////////////

        // Construct a message internal frame popup
        JInternalFrame modal =
          new ModalInternalFrame("Really Modal",
          frame.getRootPane(), desktop, optionPane);

        modal.setVisible(true);

        Object value = ZERO;//optionPane.getValue();
        if (value.equals(ZERO)) {
          System.out.println("Selected Yes");
        } else if (value.equals(ONE)) {
          System.out.println("Selected No");
        } else {
          System.err.println("Input Error");
       }
      }
    };

    JInternalFrame internal =
      new JInternalFrame("Opener");
    desktop.add(internal);

    JButton button = new JButton("Open");
    button.addActionListener(showModal);

    Container iContent = internal.getContentPane();
    iContent.add(button, BorderLayout.CENTER);
    internal.setBounds(25, 25, 200, 100);
    internal.setVisible(true);

    Container content = frame.getContentPane();
    content.add(desktop, BorderLayout.CENTER);
    frame.setSize(500, 300);
    frame.setVisible(true);
  }
}