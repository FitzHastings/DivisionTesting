package division.editors.objects;

import bum.editors.MainObjectEditor;
import bum.interfaces.Bank;
import division.swing.DivisionScrollPane;
import division.swing.DivisionTextField;
import division.swing.DivisionToolButton;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.rmi.RemoteException;
import javax.swing.JLabel;
import javax.swing.JTextPane;

public class BankEditor extends MainObjectEditor {
  private DivisionTextField bankName = new DivisionTextField(DivisionTextField.Type.ALL);
  private DivisionTextField bankBik = new DivisionTextField(DivisionTextField.Type.ALL);
  private DivisionTextField bankCorrAccount = new DivisionTextField(DivisionTextField.Type.ALL);
  private JTextPane address = new JTextPane();
  private DivisionToolButton selectAddress = new DivisionToolButton("...");
  private DivisionScrollPane addresScroll = new DivisionScrollPane(address);
  
  public BankEditor() throws RemoteException {
    super();
    initComponents();
  }

  @Override
  public String commit() throws RemoteException 
  {
    String msg = "";
    Bank bank = (Bank)this.getEditorObject();
    if ("".equals(bankName.getText()))
      msg+="незаполнено поле 'Наименование'\n";
    if ("".equals(bankBik.getText()))
      msg+="незаполнено поле 'Бик'\n";
    if ("".equals(bankCorrAccount.getText()))
      msg+="незаполнено поле 'Кор. счет'\n";
    if ("".equals(bankCorrAccount.getText()))
      msg+="незаполнено поле 'Адрес'\n";
    bank.setName(bankName.getText());
    bank.setAddress(address.getText());
    bank.setBik(bankBik.getText());
    bank.setCorrAccount(bankCorrAccount.getText());
    return msg;
  }

  @Override
  public void clearData() {
    this.bankName.setText("");
    this.address.setText("");
    this.bankBik.setText("");
    this.bankCorrAccount.setText("");
  }

  @Override
  public void initData() throws RemoteException {
    Bank bank = (Bank)this.getEditorObject();
    this.bankName.setText(bank.getName());
    this.address.setText(bank.getAddress());
    this.bankBik.setText(bank.getBik());
    this.bankCorrAccount.setText(bank.getCorrAccount());
  }
  
  private void initComponents() {
    getRootPanel().add(new JLabel("Наименование:"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(bankName, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

    getRootPanel().add(new JLabel("Бик:"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(bankBik, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    
    getRootPanel().add(new JLabel("Кор. счет:"), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(bankCorrAccount, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    
    getRootPanel().add(new JLabel("Адрес:"), new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(selectAddress, new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(addresScroll, new GridBagConstraints(0, 4, 2, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
  }
  
  @Override
  public String getEmptyObjectTitle() {
    return "[банк]";
  }
}