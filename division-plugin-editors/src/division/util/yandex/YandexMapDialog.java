package division.util.yandex;

import bum.editors.EditorGui;
import division.border.LinkBorder;
import division.swing.DivisionDialog;
import division.swing.DivisionTextField;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.sf.json.JSONObject;

public class YandexMapDialog extends DivisionDialog implements YandexErrorConnection {
  private boolean onlyShow = false;
  
  private JButton ok     = new JButton("Сохранить");
  private JButton cancel = new JButton("Закрыть");
  
  private JPanel searchPanel = new JPanel(new GridBagLayout());
  private DivisionTextField searchTextField = new DivisionTextField("Введите адрес и нажмите ENTER...");
  private JButton searchButton = new JButton("поиск");
  
  private JPanel yandexPanel = new JPanel(new GridBagLayout());
  private LinkBorder closeOpenMap = new LinkBorder("Игнорировать карту");
  private JLabel ignoreLabel = new JLabel("Выберите адрес из списка...");
  private JPanel addressPanel = new JPanel(new GridBagLayout());
  private DefaultListModel model = new DefaultListModel();
  private JList list = new JList(model) {
    @Override
    public void paint(Graphics g) {
      if(model.isEmpty()) {
        g.drawString("Введите адрес для поиска", 10, list.getVisibleRect().height/2);
      }else super.paint(g);
    }
  };
  private JScrollPane scroll = new JScrollPane(list);
  private YandexMapPanel yandexMapPanel = new YandexMapPanel(this);
  
  private ActionListener okAction = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      /*if(!model.isEmpty() && list.getSelectedIndices().length == 0)
        Messanger.alert("Выберите адрес", JOptionPane.WARNING_MESSAGE);
      else*/
        dispose();
    }
  };
  
  private ActionListener finAddressAction = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      findAddress();
    }
  };
  
  private YandexMapDialog(String searchAddress) {
    initComponents();
    initEvents();
    
    if(searchAddress != null) {
      searchTextField.setText(searchAddress);
      findAddress();
    }
  }
  
  private YandexMapDialog(JSONObject[] divisionAddresses) {
    initComponents();
    initEvents();
    try {
      divisionAddresses = YandexApi.reloadAddresses(divisionAddresses,"location");
    }catch(Exception ex) {
      System.out.println(ex.getMessage());
      connectionError(ex);
    }
    fillList(divisionAddresses);
    yandexMapPanel.setAddresses(getSelectedAddresses());
  }
  
  public static JSONObject getAddress() {
    return getAddress("");
  }
  
  public static JSONObject getAddress(JSONObject divisionAddress) {
    return getAddress(divisionAddress.getString("title"));
  }
  
  public static JSONObject getAddress(String searchAddress) {
    YandexMapDialog dialog = new YandexMapDialog(searchAddress);
    dialog.setModal(true);
    dialog.centerLocation();
    System.out.println("1");
    dialog.setVisible(true);
    System.out.println("2");
    JSONObject[] addresses = dialog.getSelectedAddresses();
    return addresses!=null&&addresses.length>0?addresses[0]:null;
  }
  
  public static void showAddresses(JSONObject[] divisionAddresses) {
    YandexMapDialog dialog = new YandexMapDialog(divisionAddresses);
    dialog.onlyShow = true;
    dialog.ok.setVisible(false);
    dialog.setModal(true);
    dialog.centerLocation();
    dialog.setVisible(true);
  }
  
  private JSONObject[] getSelectedAddresses() {
    if(!isEnabled() || (!addressPanel.isVisible() && searchTextField.getText().equals("")))
      return null;
    java.util.List selectedAddresses = list.getSelectedValuesList();
    
    if(!addressPanel.isVisible() || (selectedAddresses.isEmpty() && ignoreLabel.getText().equals("Точного совпадения не найдено..."))) {
      if(!searchTextField.getText().equals("")) {
        if(selectedAddresses.isEmpty()) {
          selectedAddresses = new ArrayList();
          JSONObject address = new JSONObject();
          address.put("title", searchTextField.getText());
          selectedAddresses.add(address);
        }else if(selectedAddresses.size() == 1 && 
                !searchTextField.getText().equals(((JSONObject)selectedAddresses.get(0)).getString("title"))) {
          ((JSONObject)selectedAddresses.get(0)).clear();
          ((JSONObject)selectedAddresses.get(0)).put("title", searchTextField.getText());
        }
      }
    }
    
    if(selectedAddresses.isEmpty()) {
      selectedAddresses = new ArrayList();
      for(int i=0;i<model.size();i++)
        selectedAddresses.add(model.get(i));
    }
    return Arrays.copyOf(selectedAddresses.toArray(), selectedAddresses.size(), JSONObject[].class);
  }
  
  private void initComponents() {
    Color c = list.getSelectionBackground();
    list.setSelectionBackground(new Color(c.getRed(), c.getGreen(), c.getBlue(), 80));
    scroll.setPreferredSize(new Dimension(300, 300));
    list.setCellRenderer(new AddressCellRenderer());
    getContentPane().setLayout(new GridBagLayout());
    
    searchButton.setPreferredSize(new Dimension(searchButton.getPreferredSize().width, searchTextField.getPreferredSize().height));
    
    searchPanel.add(searchTextField, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    searchPanel.add(searchButton,    new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    
    yandexPanel.setBorder(closeOpenMap);
    yandexPanel.add(ignoreLabel,  new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    yandexPanel.add(addressPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    addressPanel.add(scroll,         new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.VERTICAL, new Insets(5, 5, 5, 5), 0, 0));
    addressPanel.add(yandexMapPanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    
    getContentPane().add(searchPanel,  new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    getContentPane().add(yandexPanel,        new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    getContentPane().add(ok,           new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    getContentPane().add(cancel,       new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
  }

  private void initEvents() {
    cancel.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        list.clearSelection();
        setEnabled(false);
        dispose();
      }
    });
    
    ok.addActionListener(okAction);
    
    closeOpenMap.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        addressPanel.setVisible(!addressPanel.isVisible());
        closeOpenMap.setTitle(addressPanel.isVisible()?"Игнорировать карту":"Включить поиск по карте");
        ignoreLabel.setText(addressPanel.isVisible()?"Выберите адрес из списка...":"Введите адрес вручную и нажмите \"Ok\" или ENTER");
        searchButton.setVisible(addressPanel.isVisible());
        setSize(getWidth(), addressPanel.isVisible()?getHeight()+addressPanel.getHeight():(getHeight()-addressPanel.getHeight()));
        searchTextField.removeActionListener(addressPanel.isVisible()?okAction:finAddressAction);
        searchTextField.addActionListener(addressPanel.isVisible()?finAddressAction:okAction);
      }
    });
    
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        list.clearSelection();
        setEnabled(false);
      }
    });
    
    searchButton.addActionListener(finAddressAction);
    searchTextField.addActionListener(finAddressAction);
    
    searchTextField.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        list.clearSelection();
      }
    });
    
    list.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if(e.getValueIsAdjusting()) {
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              yandexMapPanel.setAddresses(getSelectedAddresses());
            }
          });
        }
      }
    });
  }
  
  private void findAddress() {
    EditorGui.setCursor(scroll, new Cursor(Cursor.WAIT_CURSOR));
    try {
      JSONObject[] addresses = YandexApi.getAddresses(searchTextField.getText());
      ignoreLabel.setText(addresses.length>0?"Точного совпадения не найдено...":"Найдено точное совпадение...");
      fillList(addresses);
      yandexMapPanel.setAddresses(getSelectedAddresses());
    }catch(Exception ex) {
      connectionError(ex);
    }finally {
      EditorGui.setCursor(scroll, new Cursor(Cursor.DEFAULT_CURSOR));
    }
  }
  
  private void fillList(final JSONObject[] addresses) {
    list.clearSelection();
    model.clear();
    int metka = 1;
    for(JSONObject address:addresses) {
      address.put("metka", metka++);
      model.addElement(address);
      metka = metka==100?1:metka;
    }
    if(model.size() == 1) {
      searchTextField.setText(((JSONObject)model.get(0)).getString("title"));
      list.setSelectedIndex(0);
    }
  }

  @Override
  public synchronized void connectionError(final Exception ex) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if(onlyShow)
          searchTextField.setText(ex.getMessage());
        searchTextField.setEnabled(!onlyShow);
        yandexPanel.setVisible(false);
        addressPanel.setVisible(false);
        searchButton.setVisible(false);
        searchTextField.removeActionListener(finAddressAction);
        searchTextField.addActionListener(okAction);
        setTitle("Отсутствует соединение с Yandex");
        searchPanel.add(new JLabel(""
                + "<html>"
                + "<center>"
                + "<b>Ошибка соединения с сервером...</b><br/>"
                + ex.getMessage()+"<br/>"
                + "Проверте настройки соединения и попробуйте повторить действие.<br/>"
                + (onlyShow?"":"Подбор адреса не возможен.<br/>"
                + "<b>Чтобы ввести адрес вручную, введите адрес в поле поиска и нажмите \"Ok\".</b>")
                + "</center>"
                + "</html>"), new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
        setResizable(false);
        centerLocation();
        ex.printStackTrace();
      }
    });
  }
  
  class AddressCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      JSONObject json = (JSONObject)value;
      
      String metka = "";
      if(json.containsKey("metka"))
        metka = json.getString("metka")+". ";
      
      String metro = "";
      if(json.containsKey("metro-name"))
        metro += "<img src='http://yandex.st/lego/_/se-5xA5ZwzIEMTbWspUA8okkCrE.png'>"+json.getString("metro-name");
      
      String country = "";
      if(json.containsKey("countryName"))
        country = json.getString("countryName");
      
      String town = "";
      if(json.containsKey("town"))
        town = json.getString("town");
      
      String district = "";
      if(json.containsKey("district"))
        district = json.getString("district");
      
      String text = "<html>"
              + "<body style='width: 200px;'>"
              + "<br/>"
              + "<b>"+metka+json.getString("title")+"</b><br/>"
              + "<font color='gray'>"
              + "<b>"
              + country+" "
              + (!town.equals("")?town+"<br/>":"")
              + (!district.equals("")?district+"<br/>":"")
              + (!metro.equals("")?metro+"<br/>":"")
              + "</b>"
              + "</font>"
              + "<br/>"
              + "</body>"
              + "</html>";
      return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
    }
  }
}