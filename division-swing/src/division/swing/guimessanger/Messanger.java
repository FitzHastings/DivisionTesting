package division.swing.guimessanger;

import division.swing.DivisionDialog;
import groovy.lang.Script;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Vector;
import javax.swing.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

public class Messanger extends Script {
  private static boolean showableErrorDialog = true;
  private static final Vector<GuiMessageListener> listeners = new Vector<>();

  public static boolean isShowableErrorDialog() {
    return showableErrorDialog;
  }

  public static void setShowableErrorDialog(boolean showableErrorDialog) {
    Messanger.showableErrorDialog = showableErrorDialog;
  }
  
  public static void showUnsupportedOperation() {
    alert("Данная операция в текущей версии\nпрограммы не поддерживается.", "Внимание");
  }
  
  public static void alert(String message) {
    alert(message, JOptionPane.INFORMATION_MESSAGE);
  }
  
  public static void alert(String message, String title) {
    alert(message, title, JOptionPane.INFORMATION_MESSAGE);
  }
  
  public static void alert(String message, int type) {
    alert(message, "", type);
  }
  
  public static void alert(String message, String title, int type) {
    alert(null, message,title,type);
  }
  
  public static void alert(Component parent, String message, String title, int type) {
    JOptionPane.showMessageDialog(parent, message,title,type);
  }
  
  public static int showErrorMessage(Window parent, Throwable exception) {
    return showErrorMessage(parent, exception.getMessage(), exception);
  }
  
  public static int showErrorMessage(Throwable exception) {
    return showErrorMessage(null, exception.getMessage(), exception);
  }
  
  public static int showErrorMessage(String message, Throwable exception) {
    return showErrorMessage(null, message, exception);
  }

  public static int showErrorMessage(Window parent, String message, final Throwable exception) {
    return showErrorMessage(parent, message, "Ошибка", new String[]{"Ok"}, exception);
  }
  
  public static int showErrorMessage(
          Window parent,
          String message,
          String title,
          String[] options,
          final Throwable exception) {
    final int[] returnValues = new int[]{-1};
    Logger.getRootLogger().error(message, exception);
    try {
      fireMessage(GuiMessageListener.Type.ERROR, title, message, exception);
      if(isShowableErrorDialog()) {
        final DivisionDialog dialog = new DivisionDialog(parent,title);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);
        dialog.setAlwaysOnTop(true);

        final JButton showDetails = new JButton("Подробнее>>");
        final JButton[] buttons = new JButton[options.length];
        for(int i=0;i<options.length;i++) {
          buttons[i] = new JButton(options[i]);
          buttons[i].addActionListener((ActionEvent e) -> {
            returnValues[0] = ArrayUtils.indexOf(buttons,e.getSource());
            dialog.dispose();
          });
        }

        JTextPane stackTraceText = new JTextPane();
        stackTraceText.setContentType("text/html");

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        exception.printStackTrace(writer);
        writer.flush();
        stackTraceText.setText("<html><pre>"+stringWriter.toString()+"</pre></html>");
        stackTraceText.setCaretPosition(0);
        stackTraceText.setBorder(BorderFactory.createEmptyBorder());
        stackTraceText.setEditable(false);

        final JScrollPane scrollStackTraceText = new JScrollPane(stackTraceText);
        scrollStackTraceText.setVisible(false);

        scrollStackTraceText.setPreferredSize(new Dimension(400, 200));
        scrollStackTraceText.setMinimumSize(new Dimension(400, 200));
        scrollStackTraceText.setMaximumSize(new Dimension(400, 200));

        showDetails.addActionListener((ActionEvent e) -> {
          showDetails.setText(scrollStackTraceText.isVisible()?"Подробнее>>":"<<Подробнее");
          scrollStackTraceText.setVisible(!scrollStackTraceText.isVisible());
          dialog.pack();
        });

        Container rootPanel = dialog.getContentPane();
        rootPanel.setLayout(new GridBagLayout());
        
        JEditorPane area = new JEditorPane();
        area.setContentType("text/html");
        area.setEditable(false);
        
        message = "<span style='font-size:12pt; font-family:Verdana'>"+(message==null?exception.getMessage():message)+"</span>";
        
        area.setText(message);
        //area.setWrapStyleWord(true);
        //area.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setPreferredSize(new Dimension(400, 75));

        JPanel msgPanel = new JPanel(new GridBagLayout());
        msgPanel.add(new JLabel(new ImageIcon("images/error.png")), new GridBagConstraints(0, 0, 1, 2, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
        msgPanel.add(scrollPane,                                    new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));

        rootPanel.add(msgPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        rootPanel.add(new JLabel("Нажмите \"Подробнее\" для получения подробной информации..."), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

        JPanel buttonPanel = new JPanel(new GridBagLayout());
        for(int i=0;i<buttons.length;i++)
          buttonPanel.add(buttons[i], new GridBagConstraints(i, 0, 1, 1, i==0?1.0:0.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        buttonPanel.add(showDetails, new GridBagConstraints(buttons.length, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

        rootPanel.add(buttonPanel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        rootPanel.add(scrollStackTraceText, new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 0, 0, 0), 0, 0));    

        dialog.centerLocation();
        dialog.setModal(true);
        dialog.setVisible(true);
      }else {
        System.out.println("************ERROR*************");
        exception.printStackTrace(System.err);
      }
    }catch(Exception ex) {
      showErrorMessage(ex);
    }
    return returnValues[0];
  }
  
  public static void fireMessage(GuiMessageListener.Type messageType, String title, String message, Throwable ex) {
    for(GuiMessageListener listener:listeners)
      listener.message(messageType, title, message, ex);
  }
  
  public static void addGuiMessageListener(GuiMessageListener listener) {
    if(!listeners.contains(listener))
      listeners.add(listener);
  }
  
  public static void removeGuiMessageListener(GuiMessageListener listener) {
    listeners.remove(listener);
  }

  @Override
  public Object run() {
    return this;
  }
}