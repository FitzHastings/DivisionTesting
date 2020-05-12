package division.swing.guimessanger;

import division.util.Utility;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class MessageTextBox extends JPanel implements GuiMessageListener {
  private JTextArea textBox = new JTextArea();
  private JScrollPane scroll = new JScrollPane(textBox);

  public MessageTextBox() {
    super(new BorderLayout());
    textBox.setLineWrap(true);
    textBox.setWrapStyleWord(true);
    textBox.setEditable(false);
    add(scroll, BorderLayout.CENTER);
    Messanger.addGuiMessageListener(this);
  }

  @Override
  public void message(Type messageType, String title, String message, Throwable ex) {
    String m = "["+Utility.format(System.currentTimeMillis())+"]"+(message==null?"":" "+message);
    if(ex != null) {
      m += "\n"+ex.getMessage();
      for(StackTraceElement element:ex.getStackTrace())
        m += "\n     "+element.toString();
    }
    textBox.setText(textBox.getText()+m+"\n");
  }
}