//TODO: asdasdasd
/*package division.util;

import java.util.HashMap;
import java.util.Map;

public class DivisionMap<KeyType, ValueType> extends HashMap<KeyType, ValueType> {
  public DivisionMap<KeyType, ValueType> puts(KeyType key, ValueType value) {
    put(key, value);
    return this;
  }
  
  public DivisionMap<KeyType, ValueType> putsAll(Map<KeyType, ValueType> map) {
    putAll(map);
    return this;
  }

  public static DivisionMap create() {
    return new DivisionMap();
  }

  @Override
  public ValueType put(KeyType key, ValueType value) {
    super.put(key, value);
    return value;
  }
}*/

/*import bum.editors.EditorController;
import bum.editors.EditorGui;
import division.swing.DivisionDialog;
import division.swing.guimessanger.Messanger;
import division.swing.DivisionTextField;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;

public class DivisionMap extends DivisionDialog {
  private static ExecutorService pool = Executors.newSingleThreadExecutor();
  
  private static JButton ok = new JButton("Ok");
  
  private static DivisionTextField searchTextField = new DivisionTextField("Введите адрес и нажмите ENTER...");
  private static JButton searchButton = new JButton("поиск");
  private static final DefaultListModel model = new DefaultListModel();
  private static final JList list = new JList(model);
  private static JScrollPane scroll = new JScrollPane(list);
  
  private static JSlider slider = new JSlider(SwingConstants.VERTICAL, 1, 20, 17);
  
  private static Image background = null;
  private static JPanel mapPanel = new JPanel(new BorderLayout()) {
    @Override
    public void paint(Graphics g) {
      if(background != null)
        g.drawImage(background, 0, 0, null);
      super.paintChildren(g);
      super.paintBorder(g);
    }
  };
  
  private DivisionMap() {
    initComponents();
    initEvents();
  }
  
  public static JSONObject get(String serchText) {
    EditorController.waitCursor();
    try {
      DivisionMap dialog = new DivisionMap();
      searchTextField.setText(serchText);
      dialog.centerLocation();
      dialog.setModal(true);
      findAddress();
      EditorController.defaultCursor();
      dialog.setVisible(true);
      return (JSONObject) list.getSelectedValue();
    }catch(Exception ex) {}
    finally {
      EditorController.defaultCursor();
    }
    return null;
  }

  private void initComponents() {
    list.setCellRenderer(new AddressCellRenderer());
    getContentPane().setLayout(new GridBagLayout());
    
    JPanel serachPanel = new JPanel(new GridBagLayout());
    serachPanel.add(searchTextField, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    serachPanel.add(searchButton,    new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    
    JPanel addressPanel = new JPanel(new BorderLayout());
    addressPanel.add(scroll, BorderLayout.WEST);
    addressPanel.add(mapPanel, BorderLayout.CENTER);
    
    getContentPane().add(serachPanel,  new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    getContentPane().add(addressPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    getContentPane().add(ok,           new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    
    slider.setOpaque(false);
    mapPanel.add(slider, BorderLayout.WEST);
    
    mapPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
    mapPanel.setPreferredSize(new Dimension(500, 400));
  }

  private void initEvents() {
    ok.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    });
    
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        list.clearSelection();
      }

      @Override
      public void windowOpened(WindowEvent e) {
        mapPanel.addComponentListener(new ComponentAdapter() {
          @Override
          public void componentResized(ComponentEvent e) {
            try {
              paintMap();
            } catch (Exception ex) {
              Messanger.showErrorMessage(ex);
            }
          }
        });
      }
    });
    
    searchButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          findAddress();
        } catch (Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
    
    searchTextField.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          findAddress();
        } catch (Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
    
    list.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if(e.getValueIsAdjusting()) {
          try {
            paintMap();
          } catch (Exception ex) {
            Messanger.showErrorMessage(ex);
          }
        }
      }
    });
    
    slider.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        try {
          paintMap();
        } catch (Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
    
    mapPanel.addMouseWheelListener(new MouseWheelListener() {
      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
        if(e.getWheelRotation() > 0 && slider.getValue() < 20 || e.getWheelRotation() < 1 && slider.getValue() > 1)
          slider.setValue(slider.getValue()+e.getWheelRotation());
      }
    });
  }
  
  private static void findAddress() throws Exception {
    EditorGui.setCursor(scroll, new Cursor(Cursor.WAIT_CURSOR));
    try {
      JSONObject[] addresses = new JSONObject[0];
      //if(!searchTextField.getText().equals(""))
        addresses = AddressUtil.getAddresses(searchTextField.getText());
      fillList(addresses);
      paintMap();
    }catch(Exception ex) {
      throw new Exception(ex);
    }finally {
      EditorGui.setCursor(scroll, new Cursor(Cursor.DEFAULT_CURSOR));
    }
  }
  
  private static void fillList(final JSONObject[] addresses) {
    list.clearSelection();
    model.clear();
    for(JSONObject address:addresses)
      model.addElement(address);
    if(!model.isEmpty())
      list.setSelectedIndex(0);
  }
  
  private static void paintMap() throws Exception {
    if(mapPanel.getWidth() > 0 && mapPanel.getHeight() > 0) {
      EditorGui.setCursor(mapPanel, new Cursor(Cursor.WAIT_CURSOR));
      try {
        String[] addresses = new String[0];
        for(int index:list.getSelectedIndices()) {
          JSONObject address = (JSONObject) model.get(index);
          addresses = (String[]) ArrayUtils.add(addresses, address.getString("lat")+","+address.getString("lng"));
        }
        if(addresses.length == 0)
          background = AddressUtil.getMap(mapPanel.getWidth(), mapPanel.getHeight(), slider.getValue(), "Москва", addresses);
        else background = AddressUtil.getMap(mapPanel.getWidth(), mapPanel.getHeight(), slider.getValue(), addresses);
        mapPanel.repaint();
      }catch(Exception ex) {
        throw new Exception(ex);
      }finally {
        EditorGui.setCursor(mapPanel, new Cursor(Cursor.DEFAULT_CURSOR));
      }
    }
  }
  
  class AddressCellRenderer extends DefaultListCellRenderer {//extends JTextArea implements ListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      String text = "<html><body style='width: 200px;'>"+((JSONObject)value).getString("system-address")+"<br/><br/></body></html>";
      return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
    }
  }
}*/