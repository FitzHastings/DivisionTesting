package bum.editors.util;

import division.swing.DivisionComboBox;
import division.swing.DivisionDialog;
import division.swing.guimessanger.Messanger;
import division.swing.DivisionButton;
import division.xml.Document;
import division.xml.Node;
import gnu.io.CommPortIdentifier;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.util.Enumeration;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ScanerPanel extends JPanel {
  private Document document;
  private Node scaner,port,speed,prefix,suffix;

  private DivisionComboBox portCombo     = new DivisionComboBox();
  private DivisionComboBox speedCombo    = new DivisionComboBox(new Object[]{"9600","4800","19200"});
  private DivisionComboBox prefixCombo   = new DivisionComboBox();
  private DivisionComboBox suffixCombo   = new DivisionComboBox();
  private DivisionButton   test              = new DivisionButton("тест");

  public ScanerPanel(Document document) {
    this.document = document;
    initComponents();
    initEvents();
    fillPorts();
    init();
  }

  private void initComponents() {
    setLayout(new GridBagLayout());
    add(new JLabel("Порт"),     new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    add(portCombo,              new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    
    add(new JLabel("Скорость"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    add(speedCombo,             new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

    add(new JLabel("Префикс"),  new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    add(prefixCombo,            new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

    add(new JLabel("Суффикс"),  new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    add(suffixCombo,            new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

    add(test,                   new GridBagConstraints(0, 4, 2, 1, 1.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
  }

  private void initEvents() {
    test.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        testScaner();
      }
    });

    portCombo.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if(port != null && e.getStateChange() == ItemEvent.SELECTED) {
          port.setValue(e.getItem().toString());
          document.save();
        }
      }
    });

    speedCombo.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if(speed != null && e.getStateChange() == ItemEvent.SELECTED) {
          speed.setValue(e.getItem().toString());
          document.save();
        }
      }
    });

    prefixCombo.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if(prefix != null && e.getStateChange() == ItemEvent.SELECTED) {
          prefix.setValue(e.getItem().toString());
          document.save();
        }
      }
    });

    suffixCombo.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if(suffix != null && e.getStateChange() == ItemEvent.SELECTED) {
          suffix.setValue(e.getItem().toString());
          document.save();
        }
      }
    });
  }

  private void testScaner() {
    final JTextField field = new JTextField(20);
    field.setEditable(false);
    DivisionDialog dialog = new DivisionDialog();
    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        BarcodeReader.close();
      }

      @Override
      public void windowOpened(WindowEvent e) {
        try {
          BarcodeReader.connect(port.getValue(), Integer.parseInt(speed.getValue()));
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
    dialog.getContentPane().add(field, BorderLayout.CENTER);
    dialog.centerLocation();

    BarcodeReader.addBarcodeListener(new BarcodeListener() {
      @Override
      public void message(String message) {
        field.setText(message);
      }
    });

    dialog.setModal(true);
    dialog.setVisible(true);
  }

  private void fillPorts() {
    Enumeration portList = CommPortIdentifier.getPortIdentifiers();
    while(portList.hasMoreElements())
      portCombo.addItem(((CommPortIdentifier)portList.nextElement()).getName());
  }

  private void init() {
    scaner = document.getRootNode().getNode("SCANER");
    if(scaner == null) {
      scaner = new Node("SCANER");
      document.getRootNode().addNode(scaner);
    }

    port   = scaner.getNode("PORT");
    if(port == null) {
      port = new Node("PORT");
      if(portCombo.getSelectedItem() != null)
        port.setValue(portCombo.getSelectedItem().toString());
      scaner.addNode(port);
    }else portCombo.setSelectedItem(port.getValue());

    speed  = scaner.getNode("SPEED");
    if(speed == null) {
      speed = new Node("SPEED");
      if(speedCombo.getSelectedItem() != null)
        speed.setValue(speedCombo.getSelectedItem().toString());
      scaner.addNode(speed);
    }else speedCombo.setSelectedItem(speed.getValue());

    prefix = scaner.getNode("PREFIX");
    if(prefix == null) {
      prefix = new Node("PREFIX");
      if(prefixCombo.getSelectedItem() != null)
        prefix.setValue(prefixCombo.getSelectedItem().toString());
      scaner.addNode(prefix);
    }else prefixCombo.setSelectedItem(prefix.getValue());

    suffix = scaner.getNode("SUFFIX");
    if(suffix == null) {
      suffix = new Node("SUFFIX");
      if(suffixCombo.getSelectedItem() != null)
        suffix.setValue(suffixCombo.getSelectedItem().toString());
      scaner.addNode(suffix);
    }else suffixCombo.setSelectedItem(suffix.getValue());
    document.save();
  }
}