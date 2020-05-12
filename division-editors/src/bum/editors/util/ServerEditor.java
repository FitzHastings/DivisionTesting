package bum.editors.util;

import division.swing.guimessanger.Messanger;
import division.swing.DivisionDialog;
import division.xml.Document;
import division.xml.Node;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.StringTokenizer;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.DefaultFormatter;

public class ServerEditor {
  private final JLabel     remoteHostLabel     = new JLabel("IP адрес RMI-сервера");
  private final JTextField remoteHostText      = new JFormattedTextField(new IPAddressFormatter());
  private final JLabel     remotePortLabel     = new JLabel("Порт RMI-сервера");
  private final JSpinner   remotePortSpiner    = new JSpinner(new SpinnerNumberModel(5555, 1025, 65000, 1));
  private final JLabel     remoteNameLabel     = new JLabel("Имя RMI-сервера");
  private final JTextField remoteNameText      = new JTextField();
  private final JLabel     initTimeOutLabel    = new JLabel("Количество попыток подключения к серверу");
  private final JSpinner   initTimeOutSpiner   = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
  private final JButton    ok = new JButton("Ok");
  private DivisionDialog dialog;
  
  private Document config;
  private Node configServer;

  private ServerEditor() {
  }

  public static void show(Window owner, Document config, Node configServer) {
    ServerEditor editor = new ServerEditor();
    
    editor.config       = config;
    editor.configServer = configServer;
    editor.dialog = new DivisionDialog(owner);
    editor.initComponents();
    editor.initEvents();
    
    editor.dialog.centerLocation();
    editor.dialog.setResizable(false);
    editor.dialog.setModal(true);
    editor.dialog.setAlwaysOnTop(true);
    editor.dialog.setVisible(true);
  }

  private void initComponents() {
    if(configServer != null) {
      remoteHostText.setText(configServer.getNode("host").getValue());
      remoteNameText.setText(configServer.getNode("name").getValue());
      remotePortSpiner.setValue(configServer.getNode("port").getInt());
      initTimeOutSpiner.setValue(Integer.parseInt(configServer.getAttribute("timeout")));
    }

    Container c = dialog.getContentPane();
    c.setLayout(new GridBagLayout());
    c.add(remoteNameLabel,   new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    c.add(remoteNameText,    new GridBagConstraints(1, 0, 3, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
    c.add(remoteHostLabel,   new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    c.add(remoteHostText,    new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
    c.add(remotePortLabel,   new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    c.add(remotePortSpiner,  new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
    c.add(initTimeOutLabel,  new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    c.add(initTimeOutSpiner, new GridBagConstraints(2, 2, 2, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
    c.add(ok,                new GridBagConstraints(0, 3, 4, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
  }

  private void initEvents() {
    ok.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          if(configServer == null) {
            configServer = new Node("server");
            Node servers = config.getNode("servers");
            if(servers == null)
              config.getRootNode().addNode(servers = new Node("servers"));
            servers.addNode(configServer);
            configServer.setAttribute("name", InetAddress.getByName(remoteHostText.getText()).getHostName());
          }
          
          configServer.setAttribute("name", InetAddress.getByName(remoteHostText.getText()).getHostName());
          configServer.setAttribute("timeout", initTimeOutSpiner.getValue()+"");
          configServer.setAttribute("main", "false");
          configServer.addNode(new Node("name", remoteNameText.getText()));
          configServer.addNode(new Node("host", remoteHostText.getText()));
          configServer.addNode(new Node("port", remotePortSpiner.getValue()+""));
          config.save();
          dialog.dispose();
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
          remoteHostText.grabFocus();
          remoteHostText.setSelectionStart(0);
          remoteHostText.setSelectionEnd(remoteHostText.getText().length());
        }
      }
    });
  }

  class IPAddressFormatter extends DefaultFormatter {
    @Override
    public String valueToString(Object value) throws ParseException {
      if(!(value instanceof byte[]))
        throw new ParseException("Not a byte[]", 0);
      byte[] a = (byte[]) value;
      if (a.length != 4)
        throw new ParseException("Length != 4", 0);
      StringBuilder builder = new StringBuilder();
      for(int i=0;i<4;i++) {
        int b = a[i];
        if (b < 0) b += 256;
        builder.append(String.valueOf(b));
        if (i < 3) builder.append('.');
      }
      return builder.toString();
    }

    @Override
    public Object stringToValue(String text) throws ParseException {
      StringTokenizer tokenizer = new StringTokenizer(text, ".");
      byte[] a = new byte[4];
      for(int i=0;i<4;i++) {
        int b = 0;
        if(!tokenizer.hasMoreTokens())
          throw new ParseException("Too few bytes", 0);
        try {
          b = Integer.parseInt(tokenizer.nextToken());
        }catch (NumberFormatException e) {
          throw new ParseException("Not an integer", 0);
        }
        if(b<0 || b>=256)
          throw new ParseException("Byte out of range", 0);
        a[i] = (byte) b;
      }
      if(tokenizer.hasMoreTokens())
        throw new ParseException("Too many bytes", 0);
      return a;
    }
  }
}