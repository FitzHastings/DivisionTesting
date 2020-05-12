package division.editors.objects;

import bum.editors.MainObjectEditor;
import bum.interfaces.People;
import bum.interfaces.Worker;
import division.border.ComponentTitleBorder;
import division.swing.guimessanger.Messanger;
import division.swing.DivisionTextField;
import division.swing.DivisionToolButton;
import division.util.CryptoUtil;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Properties;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import org.apache.commons.lang.ArrayUtils;

public class WorkerEditor extends MainObjectEditor {
  private DivisionTextField  peopleName = new DivisionTextField(DivisionTextField.Type.ALL);
  private DivisionToolButton editPeople = new DivisionToolButton("...");
  
  private JPanel passordPanel = new JPanel(new GridBagLayout());
  private JCheckBox passwordCheckBox = new JCheckBox("Изменить пароль");
  private ComponentTitleBorder passwordBorder = new ComponentTitleBorder(passwordCheckBox, passordPanel, BorderFactory.createLineBorder(Color.GRAY));
  private JPasswordField password       = new JPasswordField();
  private JPasswordField retypePassword = new JPasswordField();

  public WorkerEditor() {
    initComponents();
    initEvents();
  }
  
  @Override
  public String commit() throws RemoteException {
    Worker worker = (Worker) getEditorObject();
    String msg = "";
    if(passordPanel.isEnabled()) {
      if(!validPassword())
        msg += "Не совпадают введённые пароли!!!";
      else {
        try {
          char[] newPassword = password.getPassword();
          byte[] bytes = new byte[0];
          for(char c:newPassword)
            bytes = ArrayUtils.add(bytes, (byte)c);
          Key key = CryptoUtil.generateKey();
          Properties prop = new Properties();
          prop.put("password", CryptoUtil.encode(new String(bytes), key));
          prop.put("key", key);
          worker.setParams(prop);
        }catch(NoSuchAlgorithmException | UnsupportedEncodingException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | RemoteException ex) {
          msg += ex.getMessage();
          Messanger.showErrorMessage(ex);
        }
      }
    }
    return msg;
  }

  @Override
  public void clearData() {
  }

  @Override
  public void initData() throws RemoteException {
    Worker worker = (Worker) getEditorObject();
    People people = worker.getPeople();
    peopleName.setText(people.getSurName()+" "+people.getName()+" "+people.getLastName());
    
    Properties prop = worker.getParams();
    if(prop != null && prop.containsKey("password") && prop.containsKey("key")) {
      passordPanel.setBorder(passwordBorder);
      passwordCheckBox.setSelected(false);
      setComponentEnable(passordPanel, false);
    }else passordPanel.setBorder(BorderFactory.createTitledBorder("Задать пароль"));
  }

  private void initComponents() {
    peopleName.setEditable(false);
    
    passordPanel.add(new JLabel("Пароль: "), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    passordPanel.add(password,               new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    
    passordPanel.add(new JLabel("Повтор пароля: "), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    passordPanel.add(retypePassword,                new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    
    getRootPanel().setLayout(new GridBagLayout());
    getRootPanel().add(peopleName,   new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(editPeople,   new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(passordPanel, new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
  }
  
  private boolean validPassword() {
    return Arrays.equals(retypePassword.getPassword(), password.getPassword());
  }
  
  private boolean validOldPassword() throws RemoteException, UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
    Worker worker = (Worker) getEditorObject();
    Properties prop = worker.getParams();
    if(prop != null && prop.containsKey("password") && prop.containsKey("key")) {
      byte[] pass = (byte[]) prop.get("password");
      Key key = (Key) prop.get("key");
      JPasswordField oldPasswordField = new JPasswordField();
      if(JOptionPane.showConfirmDialog(null, oldPasswordField, "Введите старый пароль:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == 0) {
        char[] oldPassword = oldPasswordField.getPassword();
        byte[] bytes = new byte[0];
        for(char c:oldPassword)
          bytes = ArrayUtils.add(bytes, (byte)c);
        if(!Arrays.equals(CryptoUtil.encode(new String(bytes), key),pass)) {
          Messanger.alert("Неверный пароль", JOptionPane.ERROR_MESSAGE);
          validOldPassword();
        }else return true;
      }else return false;
    }
    return true;
  }

  private void initEvents() {
    passwordCheckBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        try {
          if(e.getStateChange() == ItemEvent.SELECTED) {
            if(validOldPassword()) {
              setComponentEnable(passordPanel, true);
              password.grabFocus();
            }else passwordCheckBox.setSelected(false);
          }
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
    
    retypePassword.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
      }
    });
  }
}