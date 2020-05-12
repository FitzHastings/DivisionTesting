package division.editors.objects;

import bum.editors.EditorGui;
import bum.editors.MainObjectEditor;
import bum.interfaces.*;
import bum.interfaces.Service.Owner;
import division.border.ComponentTitleBorder;
import division.swing.DivisionTextField;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.rmi.RemoteException;
import javax.swing.*;

public class ServiceEditor extends MainObjectEditor {
  private final JLabel            parentLabel    = new JLabel();
  private final DivisionTextField serviceName    = new DivisionTextField("Наименование...");
  
  private final JCheckBox    objectCheck         = new JCheckBox("Применяется");
  private final JPanel       objectPanel         = new JPanel(new GridBagLayout());
  private final ButtonGroup  objectGroup         = new ButtonGroup();
  
  private final JRadioButton    sellerCustomer   = new JRadioButton("К объекту продавца (со сменой владельца).");
  
  private final JRadioButton    sellerSeller     = new JRadioButton("К объекту продавца (без смены владельца).");
  
  private final JRadioButton    customerCustomer = new JRadioButton("К объекту покупателя (без смены владельца).");
  
  public ServiceEditor() {
    super();
    initComponents();
    initEvents();
  }

  @Override
  public void initData() throws RemoteException {
    Service service = (Service)getEditorObject();
    serviceName.setText(service.getName());
    if(((RMIDBNodeObject)getEditorObject()).getParent() != null)
      parentLabel.setText("Родительская группа: \""+((Service)getEditorObject()).getParent().getName()+"\"");
    serviceName.grabFocus();
    
    Owner owner = service.getOwner();
    objectCheck.setSelected(owner != null);
    EditorGui.setComponentEnable(objectPanel, owner != null);
    
    if(owner != null) {
      if(service.isMoveStorePosition())
        sellerCustomer.setSelected(true);
      else {
        sellerSeller.setSelected(owner == Owner.SELLER);
        customerCustomer.setSelected(owner == Owner.CUSTOMER);
      }
    }
  }

  @Override
  public String commit() throws RemoteException {
    String msg = "";
    Service service = (Service)getEditorObject();
    if("".equals(serviceName.getText()))
      msg+="незаполнено поле 'Наименование'";
    else service.setName(serviceName.getText());
    
    if(objectCheck.isSelected()) {
      if(sellerCustomer.isSelected()) {
        service.setOwner(Owner.SELLER);
        service.setMoveStorePosition(true);
      }
      
      if(sellerSeller.isSelected()) {
        service.setOwner(Owner.SELLER);
        service.setMoveStorePosition(false);
      }
      
      if(customerCustomer.isSelected()) {
        service.setOwner(Owner.CUSTOMER);
        service.setMoveStorePosition(false);
      }
    }else service.setOwner(null);
    return msg;
  }

  @Override
  public void clearData() {
    serviceName.setText("");
  }
  
  private void initComponents() {
    objectPanel.setBorder(new ComponentTitleBorder(objectCheck, objectPanel, BorderFactory.createLineBorder(Color.DARK_GRAY)));
    objectPanel.add(sellerCustomer,   new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    objectPanel.add(sellerSeller,     new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    objectPanel.add(customerCustomer, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
    objectGroup.add(sellerCustomer);
    objectGroup.add(sellerSeller);
    objectGroup.add(customerCustomer);
    
    getRootPanel().setLayout(new GridBagLayout());
    getRootPanel().add(parentLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(10, 5, 5, 5), 0, 0));
    getRootPanel().add(serviceName, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(objectPanel,  new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
  }
  
  private void initEvents() {
    objectCheck.addItemListener(e -> EditorGui.setComponentEnable(objectPanel, e.getStateChange() == ItemEvent.SELECTED));
    serviceName.addActionListener(e -> okButtonAction());
  }
  
  @Override
  public String getEmptyObjectTitle() {
    return "[процесс]";
  }
}