package division.editors.objects;

import bum.editors.MainObjectEditor;
import bum.interfaces.Factor;
import division.swing.DivisionScrollPane;
import division.swing.DivisionTextField;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.rmi.RemoteException;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextPane;

public class FactorEditor extends MainObjectEditor {
  private final DivisionTextField nameText = new DivisionTextField(DivisionTextField.Type.ALL);
  private final DivisionTextField unitText = new DivisionTextField(DivisionTextField.Type.ALL);
  
  private final JTextPane valuesText = new JTextPane();
  private final DivisionScrollPane valuesTextScroll = new DivisionScrollPane(valuesText);
  
  private final JCheckBox listValues = new JCheckBox("<html>Список (введите значения<br/>через точку с запятой)</html>");
  private final JCheckBox unique = new JCheckBox("Уникальный реквизит");
  
  private final JComboBox typeCombo = new JComboBox(Factor.FactorType.values());
    
  public FactorEditor() throws RemoteException {
    super();
    initComponents();
    initEvents();
  }
  
  @Override
  public void initData() throws RemoteException {
    Factor factor = (Factor)this.getEditorObject();
    nameText.setText(factor.getName());
    //unitText.setText(factor.getUnit().getName());
    typeCombo.setSelectedItem(factor.getFactorType());
    String list = factor.getListValues();
    listValues.setSelected(list != null);
    valuesText.setEnabled(list != null);
    valuesText.setBackground(list != null?Color.WHITE:Color.LIGHT_GRAY);
    if(list != null)
      valuesText.setText(list);
    unique.setSelected(factor.isUnique());
  }  
  
  private void initEvents() {
    listValues.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        valuesText.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
        valuesText.setBackground(e.getStateChange() == ItemEvent.SELECTED?Color.WHITE:Color.LIGHT_GRAY);
      }
    });
  }
  
  @Override
  public String commit() throws RemoteException {
    String msg = "";
    Factor factor = (Factor) this.getEditorObject();
    factor.setName(nameText.getText());
    //factor.setUnit(unitText.getText());
    factor.setFactorType(Factor.FactorType.valueOf(typeCombo.getSelectedItem().toString()));
    factor.setListValues(valuesText.getText().equals("")||!valuesText.isEnabled()?null:valuesText.getText());
    if("".equals(nameText.getText()))
      msg+="незаполнено поле 'Наименование'\n";
    
    factor.setUnique(unique.isSelected());
    
    return msg;
  }
  
  public void resetData() throws RemoteException {
    nameText.setText("");
    unitText.setText("");
  }
  
  private void initComponents() {
    getRootPanel().add(new JLabel("Наименование:"),      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(10, 5, 5, 5), 0, 0));
    getRootPanel().add(nameText,                         new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 5, 5, 5), 0, 0));
    
    getRootPanel().add(new JLabel("Единица измерения:"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(10, 5, 5, 5), 0, 0));
    getRootPanel().add(unitText,                         new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 5, 5, 5), 0, 0));
    
    getRootPanel().add(new JLabel("Тип:"), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(typeCombo,          new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    
    getRootPanel().add(listValues,       new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(valuesTextScroll, new GridBagConstraints(1, 3, 1, 2, 1.0, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    
    getRootPanel().add(unique,           new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
  }

  @Override
  public void clearData() {
  }

  @Override
  public String getEmptyObjectTitle() {return "[реквизит]";}
}
