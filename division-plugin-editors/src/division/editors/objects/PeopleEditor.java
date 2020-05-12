package division.editors.objects;

import bum.editors.MainObjectEditor;
import bum.interfaces.People;
import division.swing.DivisionCalendarComboBox;
import division.swing.DivisionScrollPane;
import division.swing.DivisionTextField;
import division.swing.DivisionToolButton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.rmi.RemoteException;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

public class PeopleEditor extends MainObjectEditor {
  private DivisionTextField masterSurName = new DivisionTextField(DivisionTextField.Type.ALL);
  private DivisionTextField masterName = new DivisionTextField(DivisionTextField.Type.ALL);
  private DivisionTextField masterLastName = new DivisionTextField(DivisionTextField.Type.ALL);
  
  private JTextPane masterPostAddres = new JTextPane();
  private DivisionToolButton selectPostAddress = new DivisionToolButton("...");
  private DivisionScrollPane masterPostAddresScroll = new DivisionScrollPane(masterPostAddres);
  
  private JTextPane masterRegAddres = new JTextPane();
  private DivisionToolButton selectRegAddress = new DivisionToolButton("...");
  private DivisionScrollPane masterRegAddresScroll = new DivisionScrollPane(masterRegAddres);
  
  private DivisionTextField masterNationality = new DivisionTextField(DivisionTextField.Type.ALL);
  private DivisionTextField masterSerialPasport = new DivisionTextField(DivisionTextField.Type.ALL);
  private DivisionTextField masterNumberPasport = new DivisionTextField(DivisionTextField.Type.ALL);
  
  private JTextPane masterWhoTake = new JTextPane();
  private DivisionScrollPane masterWhoTakeScroll = new DivisionScrollPane(masterWhoTake);
  
  private DivisionCalendarComboBox masterTakeDate = new DivisionCalendarComboBox();
  private DivisionTextField masterCodeDepartament = new DivisionTextField(DivisionTextField.Type.ALL);
  
  private DivisionCalendarComboBox masterBirthday = new DivisionCalendarComboBox();
  
  private DivisionTextField masterHomeTelephon = new DivisionTextField(DivisionTextField.Type.ALL);
  private DivisionTextField masterMobileTelephon = new DivisionTextField(DivisionTextField.Type.ALL);
  private DivisionTextField masterEmail = new DivisionTextField(DivisionTextField.Type.ALL);

  public PeopleEditor() throws RemoteException {
    super();
    initComponents();
  }

  @Override
  public String commit() throws RemoteException {
    String msg = "";
    People master = (People)this.getEditorObject();
    master.setName(masterName.getText());
    master.setSurName(masterSurName.getText());
    master.setLastName(masterLastName.getText());
    master.setBirthday(new java.sql.Date(this.masterBirthday.getDate().getTime()));
    master.setPostAddress(masterPostAddres.getText());
    master.setRegistrationAddress(masterRegAddres.getText());
    master.setCodeDepartament(masterCodeDepartament.getText());
    master.setNationality(masterNationality.getText());
    master.setSerialPasport(masterSerialPasport.getText());
    master.setNumberPasport(masterNumberPasport.getText());
    master.setWhoTake(masterWhoTake.getText());
    master.setTakeDate(new java.sql.Date(masterTakeDate.getDate().getTime()));
    master.setHomeTelephon(masterHomeTelephon.getText());
    master.setMobileTelephon(masterMobileTelephon.getText());
    master.setEmail(masterEmail.getText());
    
    if ("".equals(masterSurName.getText()))
      msg+="незаполнено поле 'Фамилия'";
    if ("".equals(masterName.getText()))
      msg+="незаполнено поле 'Имя'";
    if ("".equals(masterLastName.getText()))
      msg+="незаполнено поле 'Отчество'";
    return msg;
  }

  @Override
  public void initData() throws RemoteException {
    People master = (People)this.getEditorObject();
    this.masterName.setText(master.getName());
    this.masterSurName.setText(master.getSurName());
    this.masterLastName.setText(master.getLastName());
    
    if(master.getBirthday() != null)
      this.masterBirthday.setDateInCalendar(master.getBirthday());
    
    this.masterPostAddres.setText(master.getPostAddress());
    this.masterRegAddres.setText(master.getRegistrationAddress());
    
    this.masterCodeDepartament.setText(master.getCodeDepartament());
    this.masterNationality.setText(master.getNationality());
    this.masterSerialPasport.setText(master.getSerialPasport());
    this.masterNumberPasport.setText(master.getNumberPasport());
    this.masterWhoTake.setText(master.getWhoTake());
    
    if(master.getTakeDate() != null)
      this.masterTakeDate.setDateInCalendar(master.getTakeDate());
    
    this.masterHomeTelephon.setText(master.getHomeTelephon());
    this.masterMobileTelephon.setText(master.getMobileTelephon());
    this.masterEmail.setText(master.getEmail());
  }
  
  private void initComponents() {
    this.getRootPanel().setLayout(new GridBagLayout());
    
    JPanel pasportPanel = new JPanel(new GridBagLayout());
    
    TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Паспортные данные", 
            TitledBorder.LEFT, TitledBorder.CENTER, new Font("Dialog",Font.ITALIC|Font.BOLD,12) );
    border.setTitleColor(Color.gray);
    pasportPanel.setBorder(border);
    
    pasportPanel.add(new JLabel("Фамилия:"), new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    pasportPanel.add(new JLabel("Имя:"), new GridBagConstraints(2, 0, 2, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    pasportPanel.add(new JLabel("Очество:"), new GridBagConstraints(4, 0, 2, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
    
    pasportPanel.add(this.masterSurName, new GridBagConstraints(0, 1, 2, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    pasportPanel.add(this.masterName, new GridBagConstraints(2, 1, 2, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    pasportPanel.add(this.masterLastName, new GridBagConstraints(4, 1, 2, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    
    pasportPanel.add(new JLabel("гражданство:"), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    pasportPanel.add(this.masterNationality, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    pasportPanel.add(new JLabel("серия:"), new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    pasportPanel.add(this.masterSerialPasport, new GridBagConstraints(3, 2, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    pasportPanel.add(new JLabel("№:"), new GridBagConstraints(4, 2, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    pasportPanel.add(this.masterNumberPasport, new GridBagConstraints(5, 2, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    
    pasportPanel.add(new JLabel("Кем выдан:"), new GridBagConstraints(0, 3, 3, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    pasportPanel.add(new JLabel("Адрес регистрации:"), new GridBagConstraints(3, 3, 2, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    pasportPanel.add(this.selectRegAddress, new GridBagConstraints(5, 3, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    
    pasportPanel.add(this.masterWhoTakeScroll, new GridBagConstraints(0, 4, 3, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    pasportPanel.add(this.masterRegAddresScroll, new GridBagConstraints(3, 4, 3, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    
    pasportPanel.add(new JLabel("Код подразделения:"), new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    pasportPanel.add(this.masterCodeDepartament, new GridBagConstraints(1, 5, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    
    pasportPanel.add(new JLabel("Дата выдачи:"), new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    pasportPanel.add(this.masterTakeDate, new GridBagConstraints(3, 5, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    
    pasportPanel.add(new JLabel("День рождения:"), new GridBagConstraints(4, 5, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    pasportPanel.add(this.masterBirthday, new GridBagConstraints(5, 5, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    
    this.getRootPanel().add(pasportPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    
    JPanel contactPanel = new JPanel(new GridBagLayout());
    
    border = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Контактная информация", 
            TitledBorder.LEFT, TitledBorder.CENTER, new Font("Dialog",Font.ITALIC|Font.BOLD,12) );
    border.setTitleColor(Color.gray);
    contactPanel.setBorder(border);
    
    masterPostAddresScroll.setPreferredSize(new Dimension(100,50));
    
    contactPanel.add(new JLabel("Адрес места жительства:"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    contactPanel.add(this.selectPostAddress, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    contactPanel.add(new JLabel("Дом. телефон:"), new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    contactPanel.add(this.masterHomeTelephon, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    contactPanel.add(this.masterPostAddresScroll, new GridBagConstraints(0, 1, 2, 2, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    contactPanel.add(new JLabel("Моб. телефон:"), new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    contactPanel.add(this.masterMobileTelephon, new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    contactPanel.add(new JLabel("E-mail:"), new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    contactPanel.add(this.masterEmail, new GridBagConstraints(3, 2, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    
    this.getRootPanel().add(contactPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
  }

  public void resetData() throws RemoteException {
    this.masterName.setText("");
    this.masterSurName.setText("");
    this.masterLastName.setText("");
    this.masterBirthday.setDateInCalendar(new Date());
    this.masterPostAddres.setText("");
    this.masterRegAddres.setText("");
    this.masterCodeDepartament.setText("");
    this.masterNationality.setText("");
    this.masterSerialPasport.setText("");
    this.masterNumberPasport.setText("");
    this.masterWhoTake.setText("");
    this.masterTakeDate.setDateInCalendar(new Date());
    this.masterHomeTelephon.setText("");
    this.masterMobileTelephon.setText("");
    this.masterEmail.setText("");
  }
  
  @Override
  public String getEmptyObjectTitle() {
    return "[персонал]";
  }

  @Override
  public void clearData() {
  }
}