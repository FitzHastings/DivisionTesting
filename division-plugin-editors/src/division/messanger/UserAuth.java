package division.messanger;

import bum.editors.EditorGui;
import bum.editors.EditorListener;
import bum.editors.TableEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.CFC;
import bum.interfaces.Worker;
import division.editors.tables.CFCTableEditor;
import division.fx.PropertyMap;
import division.swing.guimessanger.Messanger;
import division.util.CryptoUtil;
import division.util.CryptoUtil.CryptoData;
import division.util.Utility;
import division.xml.Document;
import division.xml.Node;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.security.Key;
import java.util.Arrays;
import java.util.Properties;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.swing.*;
import mapping.MappingObject;
import org.apache.commons.lang.ArrayUtils;
import util.Client;

public class UserAuth extends EditorGui {
  private final JSplitPane split = new JSplitPane();
  private final CFCTableEditor cfcTableEditor = new CFCTableEditor();
  private final TableEditor workerTableEditor = new TableEditor(
          new String[]{"id","Фамилия","Имя","Отчество"}, 
          new String[]{"id","people_surname","people_name","people_lastname"}, 
          Worker.class, 
          null, 
          null, 
          null, 
          MappingObject.Type.CURRENT);
  private final JLabel passwordLabel = new JLabel("Введите пароль: ");
  private final JPasswordField passwordField = new JPasswordField();
  
  private Document document = Document.load("conf"+File.separator+"UserAuth.xml");
  private final JCheckBox saveChoose = new JCheckBox("Запомнить выбор", document != null);
  
  private final JButton changePassword = new JButton("Задать");

  public UserAuth() {
    super("Авторицация", null);
    initComponents();
    initEvents();
  }
  
  @Override
  public void initData() {
    if(isActive()) {
      cfcTableEditor.initData();
      changePassword.setEnabled(false);
    }
  }

  private void initComponents() {
    addComponentToStore(split);
    
    workerTableEditor.getStatusBar().setVisible(false);
    workerTableEditor.setAdministration(false);
    workerTableEditor.getGUI().setBorder(BorderFactory.createTitledBorder("Участники ЦФУ"));
    workerTableEditor.setVisibleOkButton(false);
    workerTableEditor.setSingleSelection(true);
    
    cfcTableEditor.getStatusBar().setVisible(false);
    cfcTableEditor.setAdministration(false);
    cfcTableEditor.getGUI().setBorder(BorderFactory.createTitledBorder("ЦФУ"));
    cfcTableEditor.setVisibleOkButton(false);
    cfcTableEditor.setSingleSelection(true);
    
    split.add(cfcTableEditor.getGUI(),    JSplitPane.LEFT);
    split.add(workerTableEditor.getGUI(), JSplitPane.RIGHT);
    
    getButtonsPanel().add(saveChoose,0);
    
    getRootPanel().setLayout(new GridBagLayout());
    getRootPanel().add(split, new GridBagConstraints(0, 0, 3, 1, 1.0, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(passwordLabel,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(passwordField,  new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(changePassword, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
  }

  private void initEvents() {
    changePassword.addActionListener((ActionEvent e) -> {
      try {
        if(workerTableEditor.getSelectedObjectsCount() > 0) {
          Integer workerId = workerTableEditor.getSelectedId()[0];
          if(validPassword(workerId, "")) {
            setPassword(workerId);
          }else if(validOldPassword(workerId))
            changePassword(workerId);
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    });
    
    passwordField.addActionListener((ActionEvent e) -> {
      okButtonAction();
    });
    
    cfcTableEditor.addEditorListener(new EditorListener() {
      @Override
      public void changeSelection(EditorGui editor, Integer[] ids) {
        workerTableEditor.getClientFilter().clear();
        workerTableEditor.getClientFilter().AND_EQUAL("cfc", ids.length > 0 ? ids[0] : null);
        workerTableEditor.initData();
      }
      
      @Override
      public void initDataComplited(EditorGui editor) {
        if(document != null) {
          Node user = document.getRootNode();
          Node cfc = user.getNode("CFC");
          if(cfc != null) {
            Integer cfcId = Integer.valueOf(cfc.getValue());
            cfcTableEditor.setSelectedObjects(new Integer[]{cfcId});
          }
        }
      }
    });
    
    workerTableEditor.addEditorListener(new EditorListener() {
      @Override
      public void changeSelection(EditorGui editor, Integer[] ids) {
        passwordField.setEnabled(ids.length > 0);
        passwordField.setBackground(passwordField.isEnabled()?Color.WHITE:Color.LIGHT_GRAY);
        changePassword.setEnabled(passwordField.isEnabled());
        try {
          if(ids.length > 0) {
            if(validPassword(ids[0], "")) {
              changePassword.setText("Задать");
            }else changePassword.setText("Сменить");
          }
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
      
      @Override
      public void initDataComplited(EditorGui editor) {
        if(document != null) {
          Node user = document.getRootNode();
          Node worker = user.getNode("WORKER");
          if(worker != null) {
            Integer workerId = Integer.valueOf(worker.getValue());
            workerTableEditor.setSelectedObjects(new Integer[]{workerId});
            passwordField.grabFocus();
          }
        }else if(workerTableEditor.getObjectsCount() > 0 && workerTableEditor.getSelectedObjectsCount() == 0) {
          workerTableEditor.setDefaultSelected();
          passwordField.grabFocus();
        }
      }
    });
  }

  @Override
  public void closeDialog() {
    workerTableEditor.clearSelection();
    super.closeDialog();
  }

  @Override
  public void dispose() {
    cfcTableEditor.dispose();
    workerTableEditor.dispose();
    super.dispose();
  }
  
  private void setPassword(Integer workerId) throws Exception {
    JPasswordField password = new JPasswordField();
    if(JOptionPane.showConfirmDialog(getGUI(), password, "Задать пароль", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == 0) {
      Worker worker = (Worker) ObjectLoader.getObject(Worker.class, workerId);
      char[] newPassword = password.getPassword();
      byte[] bytes = new byte[0];
      for(char c:newPassword)
        bytes = ArrayUtils.add(bytes, (byte)c);
      Key key = CryptoUtil.generateKey();
      Properties prop = new Properties();
      prop.put("password", CryptoUtil.encode(new String(bytes), key));
      prop.put("key", key);
      worker.setParams(prop);
      ObjectLoader.saveObject(worker);
      passwordField.grabFocus();
    }
  }
  
  private void changePassword(Integer workerId) throws Exception {
    JPasswordField password       = new JPasswordField();
    JPasswordField retypePassword = new JPasswordField();
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setMinimumSize(new Dimension(300, 50));
    panel.add(new JLabel("Пароль:"),       new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    panel.add(password,       new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    panel.add(new JLabel("Повторите:"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    panel.add(retypePassword, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    if(JOptionPane.showConfirmDialog(getGUI(), panel, "Ввод нового пароля", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == 0) {
      if(!Arrays.equals(password.getPassword(), retypePassword.getPassword())) {
        Messanger.alert(getGUI(), "Не совпадают введённые пароли!!!", "Ошибка", JOptionPane.ERROR_MESSAGE);
        changePassword(workerId);
      }else {
        Worker worker = (Worker) ObjectLoader.getObject(Worker.class, workerId);
        char[] newPassword = password.getPassword();
        byte[] bytes = new byte[0];
        for(char c:newPassword)
          bytes = ArrayUtils.add(bytes, (byte)c);
        
        CryptoData cd = CryptoUtil.encode(new String(bytes));
        
        Properties prop = new Properties();
        prop.put("md5password", Utility.getMD5(bytes));
        prop.put("password", cd.DATA);
        prop.put("key", cd.KEY);
        worker.setParams(prop);
        ObjectLoader.saveObject(worker);
        passwordField.grabFocus();
      }
    }
  }
  
  private boolean validOldPassword(Integer workerId) throws Exception {
    Worker worker = (Worker) ObjectLoader.getObject(Worker.class, workerId);
    Properties prop = worker.getParams();
    if(prop != null && prop.containsKey("password") && prop.containsKey("key")) {
      byte[] pass = (byte[]) prop.get("password");
      Key key = (Key) prop.get("key");
      JPasswordField oldPasswordField = new JPasswordField();
      if(JOptionPane.showConfirmDialog(getGUI(), oldPasswordField, "Введите старый пароль:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == 0) {
        char[] oldPassword = oldPasswordField.getPassword();
        byte[] bytes = new byte[0];
        for(char c:oldPassword)
          bytes = ArrayUtils.add(bytes, (byte)c);
        if(!Arrays.equals(CryptoUtil.encode(new String(bytes), key),pass)) {
          Messanger.alert(getGUI(), "Неверный пароль","", JOptionPane.ERROR_MESSAGE);
          validOldPassword(workerId);
        }else return true;
      }else return false;
    }
    return false;
  }
  
  public boolean auth() throws Exception {
    setAutoLoad(true);
    setAutoStore(true);
    initData();
    createDialog(true).setVisible(true);
    Integer[] ids = workerTableEditor.getSelectedId();
    if(ids.length > 0) {
      PropertyMap worker = ObjectLoader.getMap(Worker.class, ids[0], "id", "people", "cfc");
      if(worker != null) {
        Client client = new Client();
        client.setCfcId(worker.getInteger("cfc"));
        client.setWorkerId(ids[0]);
        client.setPeopleId(worker.getInteger("people"));
        
        PropertyMap m;
        ObservableList<Integer> cfcTree = FXCollections.observableArrayList(client.getCfcId());
        while(true) {
          m = ObjectLoader.getMap(CFC.class, cfcTree.get(0), "parent");
          if(m.isNull("parent"))
            break;
          else cfcTree.add(0, m.getInteger("parent"));
        }
        client.setCfcTree(cfcTree.toArray(new Integer[0]));
        
        ObjectLoader.registrateClient(client);
      }
      return validPassword();
    }else return false;
  }
  
  private boolean validPassword(Integer workerId, String enterPassword) throws Exception {
    Worker worker = (Worker) ObjectLoader.getObject(Worker.class, workerId);
    Properties prop = worker.getParams();
    if(prop != null && prop.containsKey("password") && prop.containsKey("key")) {
      byte[] pass = (byte[]) prop.get("password");
      Key key = (Key) prop.get("key");
      return Arrays.equals(pass, CryptoUtil.encode(enterPassword, key));
    }else return true;
  }
  
  private boolean validPassword() throws Exception {
    Integer[] ids = workerTableEditor.getSelectedId();
    if(ids.length > 0) {
      byte[] bytes = new byte[0];
      for(char c:passwordField.getPassword())
        bytes = ArrayUtils.add(bytes, (byte)c);
      return validPassword(ids[0], new String(bytes));
    }else {
      Messanger.alert("Выберите сотрудника", JOptionPane.ERROR_MESSAGE);
      return false;
    }
  }

  @Override
  public Boolean okButtonAction() {
    try {
      if(saveChoose.isSelected()) {
        Integer[] workers = workerTableEditor.getSelectedId();
        Integer[] cfcs    = cfcTableEditor.getSelectedId();
        if(cfcs.length > 0 && workers.length > 0) {
          Node user = new Node("USER");
          user.addNode(new Node("CFC", String.valueOf(cfcs[0])));
          user.addNode(new Node("WORKER", String.valueOf(workers[0])));
          document = document==null?new Document("conf/UserAuth.xml"):document;
          document.setRootNode(user);
          document.save();
        }
      }else if(document != null) {
        document.remove();
      }
      
      if(validPassword())
        dispose();
      else {
        Messanger.alert(getGUI(), "Неверный пароль", "", JOptionPane.ERROR_MESSAGE);
        passwordField.setSelectionStart(0);
        passwordField.setSelectionEnd(passwordField.getPassword().length);
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    return true;
  }

  @Override
  public void initTargets() {
  }
}