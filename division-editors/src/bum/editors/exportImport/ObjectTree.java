package bum.editors.exportImport;

import bum.editors.EditorController;
import bum.editors.util.FileLibrary;
import bum.editors.util.ObjectLoader;
import division.swing.DivisionComboBox;
import division.swing.guimessanger.Messanger;
import division.swing.DivisionDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.TreeMap;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;

public class ObjectTree extends DivisionDialog {
  private ExportImportPanel panelExport = new ExportImportPanel(ExportImportPanel.TYPE.EXPORT);
  private ExportImportPanel panelImport = new ExportImportPanel(ExportImportPanel.TYPE.IMPORT);
  private JTabbedPane             tabb       = new JTabbedPane();
  private DivisionComboBox        classes    = new DivisionComboBox();
  private TreeMap<String, Class>  cl         = new TreeMap<>();
  private ArrayList               components = new ArrayList();

  public ObjectTree() {
    super(EditorController.getFrame());
    initComponents();
    initEvents();
    fillClasses();

    setName("frame");
    components.add(this);
    components.addAll(panelExport.getStoreComponents());
    components.addAll(panelImport.getStoreComponents());
    load();
  }

  public void load() {
    FileLibrary.load(components, "ObjectTree.xml");
  }

  private void initComponents() {
    tabb.add("Экспорт", panelExport);
    tabb.add("Импорт",  panelImport);

    getContentPane().setLayout(new GridBagLayout());
    getContentPane().add(new JLabel("объект"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    getContentPane().add(classes,              new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    getContentPane().add(tabb,                 new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
  }

  private void initEvents() {
    classes.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if(e.getStateChange() == ItemEvent.SELECTED) {
          panelExport.initClass(cl.get(classes.getSelectedItem().toString()));
          panelImport.initClass(cl.get(classes.getSelectedItem().toString()));
        }
      }
    });

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        store();
      }
    });
  }

  private void fillClasses() {
    try {
      cl.clear();
      String key;
      for(Class clazz:ObjectLoader.getServer().getClasses()) {
        key = ObjectLoader.getClientName(clazz);
        if(key != null) {
          int i=1;
          while(cl.containsKey(key)) {
            key += "("+i+")";
            i++;
          }
          cl.put(key, clazz);
        }
      }
      for(String name:cl.keySet())
        classes.addItem(name);
    }catch(ClassNotFoundException | RemoteException ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  public void store() {
    FileLibrary.store(components, "ObjectTree.xml");
  }
}