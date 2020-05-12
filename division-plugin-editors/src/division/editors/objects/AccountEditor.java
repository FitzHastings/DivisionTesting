package division.editors.objects;

import bum.editors.MainObjectEditor;
import division.swing.guimessanger.Messanger;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Account;
import division.swing.DivisionTextField;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.rmi.RemoteException;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class AccountEditor extends MainObjectEditor {
  private final DivisionTextField accountNumber = new DivisionTextField("Р/С...");
  private final DivisionTextField bankBikText   = new DivisionTextField("", "БИК...", DivisionTextField.Type.INTEGER, 9);
  private final DivisionTextField bankNameText  = new DivisionTextField("Наименование...");
  private final DivisionTextField bankCorAcount = new DivisionTextField("Кор. Счёт...");
  private String bik = null;
  
  private final JCheckBox currentAccount = new JCheckBox("текущий");
  
  public AccountEditor() throws RemoteException {
    super();
    initComponents();
    initEvents();
  }
  
  private void initComponents() {
    this.getRootPanel().add(accountNumber, new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    this.getRootPanel().add(bankBikText,         new GridBagConstraints(0, 1, 1, 1, 0.3, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    this.getRootPanel().add(bankNameText,        new GridBagConstraints(1, 1, 1, 1, 0.7, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    this.getRootPanel().add(bankCorAcount,       new GridBagConstraints(0, 2, 2, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    this.getRootPanel().add(currentAccount, new GridBagConstraints(0, 3, 2, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
  }

  @Override
  public String commit() throws RemoteException {
    String msg = "";
    Account account = (Account)this.getEditorObject();
    if("".equals(accountNumber.getText()))
      msg+="незаполнено поле 'Номер'\n";
    if(bik == null || bik.equals(""))
      msg+="Не выбран банк\n";
    //account.setBik(bik);
    account.setNumber(accountNumber.getText());
    account.setCurrent(currentAccount.isSelected());
    return msg;
  }

  @Override
  public void clearData() {
    accountNumber.setText("");
    bankNameText.setText("");
    currentAccount.setSelected(false);
  }

  @Override
  public void initData() throws RemoteException {
    Account account = (Account)this.getEditorObject();
    accountNumber.setText(account.getNumber());
    currentAccount.setSelected(account.isCurrent()==null?false:true);
    //bik = account.getBik();
    if(bik != null && !bik.equals("")) {
      bankBikText.setText(bik);
      /*DBFilter filter = new DBFilter(null, Bank.class).AND_EQUAL("bik", bik);
      Vector<Vector> data = ObjectLoader.loadDBTable(Bank.class).getData(filter, new String[]{"name","corrAccount"});
      if(!data.isEmpty()) {
        bankNameText.setText((String) data.get(0).get(0));
      }*/
    }
  }
  
  public void initEvents() {
    
    bankBikText.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        findBank();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        findBank();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        findBank();
      }
    });
  }
  
  private void findBank() {
    bik = null;
    if(bankBikText.getText().length() == 9) {
      try {
        List<List> data = ObjectLoader.executeQuery("SELECT [Bank(name)],[Bank(corrAccount)],[Bank(town)] FROM [Bank] WHERE [Bank(bik)]=?", true,
                new Object[]{bankBikText.getText()});
        if(!data.isEmpty()) {
          bankCorAcount.setText((String) data.get(0).get(1));
          bankNameText.setText((String) data.get(0).get(0));
          bik = bankBikText.getText();
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }
  
  @Override
  public String getEmptyObjectTitle() {
    return "[счет]";
  }
}

/**
 * Обида глупая твоя,
 * Но я, дурак, ещё глупее
 * Свою гордыню обножив
 * Ответную обиду грею
 *
 * Непонимание от слов немых
 * Заподлицо ложится в безразличность
 * Друг друга упрекая в них
 * Обычностью мы тешим личность
 */