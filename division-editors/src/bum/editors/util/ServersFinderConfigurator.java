package bum.editors.util;

import division.swing.DivisionDialog;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.StringTokenizer;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.DefaultFormatter;
import division.xml.Document;
import division.xml.Node;

public class ServersFinderConfigurator {
  private JLabel     serverNameLabel     = new JLabel("Искать сервера системы");
  private JTextField serverNameText      = new JTextField();
  private JLabel     findTimeOutLabel    = new JLabel("Таймаут поиска серверов (с)");
  private JSpinner   findTimeOutSpiner   = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
  private JLabel     udpHostLabel        = new JLabel("Широковещательный IP адрес");
  private JTextField udpHostText         = new JFormattedTextField(new IPAddressFormatter());
  private JLabel     udpServerPortLabel  = new JLabel("Порт UDP-сервера");
  private JSpinner   udpServerPortSpiner = new JSpinner(new SpinnerNumberModel(9996, 1025, 65000, 1));
  private JLabel     udpClientPortLabel  = new JLabel("Порт UDP-клиента");
  private JSpinner   udpClientPortSpiner = new JSpinner(new SpinnerNumberModel(9996, 1025, 65000, 1));
  private JButton    defaultValues       = new JButton("Восстановить");
  private JButton    ok                  = new JButton("Ok");
  private DivisionDialog dialog              = new DivisionDialog();
  private Document   document;
  private Node       udp;

  public ServersFinderConfigurator(Window owner, Document document) {
    if(document == null) {
      document = new Document("udp_conf.xml",new Node("servers"));
    }
    if(document.isEmpty())
      document.setRootNode(new Node("servers"));
    if(document.getRootNode().getNode("udp") == null)
      document.getRootNode().addNode(new Node("udp"));
    this.document = document;
    this.udp = document.getRootNode().getNode("udp");
    dialog = new DivisionDialog(owner);
    initComponents();
    initEvents();
    init();
  }

  private void init() {
    if(!udp.isEmpty()) {
      udpHostText.setText(udp.getNode("server_host").getValue());
      serverNameText.setText(udp.getNode("server_name").getValue());
      udpServerPortSpiner.setValue(Integer.valueOf(udp.getNode("server_port").getValue()));
      udpClientPortSpiner.setValue(Integer.valueOf(udp.getNode("client_port").getValue()));
      findTimeOutSpiner.setValue(udp.getAttribute("timeout") == null?0:Integer.valueOf(udp.getAttribute("timeout")));
    }
  }

  private void initComponents() {
    dialog.setTitle("Настройка поиска RMI серверов");
    JPanel panel = new JPanel(new GridBagLayout());
    panel.add(defaultValues,   new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    panel.add(ok,              new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));

    Container c = dialog.getContentPane();
    c.setLayout(new GridBagLayout());
    c.add(serverNameLabel,     new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    c.add(serverNameText,      new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
    c.add(udpHostLabel,        new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    c.add(udpHostText,         new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
    c.add(udpServerPortLabel,  new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    c.add(udpServerPortSpiner, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
    c.add(udpClientPortLabel,  new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    c.add(udpClientPortSpiner, new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
    c.add(findTimeOutLabel,    new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    c.add(findTimeOutSpiner,   new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
    c.add(panel,               new GridBagConstraints(0, 5, 2, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
  }

  private void initEvents() {
    defaultValues.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        udp.clear();
        udp.setAttribute("timeout", "5");
        udp.addNode(new Node("server_host", "255.255.255.255"));
        udp.addNode(new Node("server_port", "9996"));
        udp.addNode(new Node("client_port", "9998"));
        udp.addNode(new Node("server_name", "Division"));
        init();
      }
    });
    ok.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String msg = "";
        if(udpHostText.getText().equals(""))
          msg += "  -широковечательный адрес не введён\n";
        if(serverNameText.getText().equals(""))
          msg += "  -имя системы отсутствует\n";
        if(!msg.equals(""))
          JOptionPane.showMessageDialog(dialog, "Ошибка:\n"+msg, "Ошибка", JOptionPane.ERROR_MESSAGE);
        else {
          udp.clear();
          udp.setAttribute("timeout", findTimeOutSpiner.getValue().toString());
          udp.addNode(new Node("server_host", udpHostText.getText()));
          udp.addNode(new Node("server_port", udpServerPortSpiner.getValue().toString()));
          udp.addNode(new Node("client_port", udpClientPortSpiner.getValue().toString()));
          udp.addNode(new Node("server_name", serverNameText.getText()));
          document.save();
          dialog.dispose();
        }
      }
    });
  }

  public Node getUdpNode() {
    dialog.centerLocation();
    dialog.setResizable(false);
    dialog.setModal(true);
    dialog.setAlwaysOnTop(true);
    dialog.setVisible(true);
    return udp;
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