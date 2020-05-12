package bum.editors.util;

import division.swing.DivisionDialog;
import division.xml.Document;
import java.awt.BorderLayout;
import java.awt.Window;
import java.io.File;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class MachineryDialog extends DivisionDialog {
  private JTabbedPane tab = new JTabbedPane(JTabbedPane.LEFT);
  private Document document = Document.load("conf"+File.separator+"machinery_configuration.xml");
  private ScanerPanel scanerPanel = new ScanerPanel(document);

  public MachineryDialog() {
    this(null);
  }

  public MachineryDialog(Window owner) {
    super(owner);
    initComponents();
    initEvents();
  }

  private void initComponents() {
    add(tab, BorderLayout.CENTER);
    tab.add("Сканер Штрих-кода", scanerPanel);
    tab.add("Фискальный регистратор", new JPanel());
  }

  private void initEvents() {
  }
}