package bum.editors.util;

import division.swing.DivisionDialog;
import division.swing.guimessanger.Messanger;
import division.swing.DivisionScrollPane;
import division.swing.DivisionToolButton;
import division.util.FileLoader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import net.sf.json.JSONObject;

public class Connections {
  private final JTable table  = new JTable() {
    @Override
    public boolean isCellEditable(int row, int column) {
      return column == 4;
    }
    
    @Override
    public Class<?> getColumnClass(int col) {
      return getValueAt(0,col).getClass();
    }
  };
  private final DefaultTableModel  model  = (DefaultTableModel)table.getModel();
  private final DivisionScrollPane scroll = new DivisionScrollPane(table);
  private final JToolBar           tool   = new JToolBar();
  private final DivisionToolButton addToolButton    = new DivisionToolButton(FileLoader.getIcon("Add16.gif"),"Добавить");
  private final DivisionToolButton editToolButton   = new DivisionToolButton(FileLoader.getIcon("Edit16.gif"),"Редактировать");
  private final DivisionToolButton removeToolButton = new DivisionToolButton(FileLoader.getIcon("Delete16.gif"),"Удалить");
  private final JButton ok = new JButton("Ok");
  private DivisionDialog dialog;
  private int returnCode = -1;
  
  private static JSONObject config = JSONObject.fromObject(division.util.Utility.getStringFromFile("conf"+File.separator+"conf.json"));
  
  //private static Document config = Document.load("conf"+File.separator+"configuration.xml");

  public Connections() {
    this(null);
  }

  public Connections(Window owner) {
    dialog = new DivisionDialog(owner);
    initComponents();
    initEvents();
    init();
  }

  public int showConnectionConfiguration() {
    dialog.centerLocation();
    dialog.setAlwaysOnTop(true);
    dialog.setModal(true);
    dialog.setVisible(true);
    return returnCode;
  }

  public static JSONObject getCurrentServerToConnect() {
    try {
      for(Object server:config.getJSONArray("servers"))
        if(((JSONObject)server).containsKey("main") && ((JSONObject)server).getBoolean("main"))
          return (JSONObject)server;
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    return null;
  }

  public void setTitle(String title) {
    dialog.setTitle(title);
  }

  private void initComponents() {
    dialog.setTitle("Настройка подключений");
    scroll.setPreferredSize(new Dimension(500, 200));
    Container c = dialog.getContentPane();
    c.setLayout(new GridBagLayout());
    c.add(tool,   new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    c.add(scroll, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 0, 5), 0, 0));
    c.add(ok,     new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));

    tool.add(addToolButton);
    tool.add(editToolButton);
    tool.add(removeToolButton);
    tool.addSeparator();
    //tool.add(refresh);
    //tool.add(confFinder);
    model.addColumn("Имя");
    model.addColumn("rmi-сервер");
    model.addColumn("Адрес");
    model.addColumn("Порт");
    model.addColumn("По умолчанию");
    table.getTableHeader().setReorderingAllowed(false);
  }

  private void init() {
    /*model.getDataVector().clear();
    model.fireTableDataChanged();
    for(Node server:config.getNodes("servers.server")) {
      model.addRow(new Object[]{
        server.getAttribute("name"),
        server.getNode("name").getValue(),
        server.getNode("host").getValue(),
        server.getNode("port").getInt(),
        Boolean.parseBoolean(server.getAttribute("main"))
      });
    }*/
  }

  private void editServer() {
    /*if(table.getSelectedRow() >= 0) {
      ServerEditor.show(dialog, config, config.getNode("servers").getNode(table.getSelectedRow()));
      init();
    }*/
  }

  private void addServer() {
    /*ServerEditor.show(dialog, config, null);
    init();*/
  }

  private void initEvents() {
    editToolButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editServer();
      }
    });

    addToolButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        addServer();
      }
    });

    ok.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        returnCode = 0;
        dialog.dispose();
      }
    });

    removeToolButton.addActionListener((ActionEvent e) -> {
      if(table.getSelectedRowCount() > 0) {
        /*try {
          //java.util.List servers = config.getList("servers.server.name");
          int[] rows = table.getSelectedRows();
          Arrays.sort(rows);
          for(int i=rows.length-1;i>=0;i--)
            config.getNode("servers").removeNode(rows[i]);
          config.save();
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }finally {
          init();
        }*/
      }
    });

    model.addTableModelListener(new TableModelListener() {
      boolean active = true;
      @Override
      public void tableChanged(TableModelEvent e) {
        /*if(active && e.getType() == TableModelEvent.UPDATE) {
          int columnIndex = e.getColumn();
          int rowIndex    = e.getFirstRow();
          if(columnIndex == 4) {
            try {
              active = false;
              boolean main = (boolean) model.getValueAt(rowIndex, columnIndex);
              for(int i=0;i<model.getRowCount();i++)
                if(i != rowIndex)
                  model.setValueAt(false, i, 4);
              for(Node s:config.getNodes("servers.server"))
                s.setAttribute("main", "false");
              config.getNode("servers").getNode(rowIndex).setAttribute("main", main+"");
              config.save();
            }catch(Exception ex) {
              Messanger.showErrorMessage(ex);
            }finally {
              active = true;
            }
          }
        }*/
      }
    });
  }
}