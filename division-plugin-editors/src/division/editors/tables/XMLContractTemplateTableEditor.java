package division.editors.tables;

import bum.editors.EditorGui;
import bum.editors.TableEditor;
import bum.interfaces.CompanyNickname;
import bum.interfaces.SellerNickName;
import bum.interfaces.Service;
import bum.interfaces.XMLContractTemplate;
import division.editors.objects.XMLContractTemplateEditor;
import division.swing.DivisionSplitPane;
import division.swing.guimessanger.Messanger;
import division.swing.DivisionTextField;
import division.util.Utility;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.rmi.RemoteException;
import javax.swing.*;
import mapping.MappingObject;
import util.filter.local.DBFilter;

public class XMLContractTemplateTableEditor extends TableEditor {
  private DivisionSplitPane split = new DivisionSplitPane();
  /*private JPanel             processPanel      = new JPanel(new BorderLayout());
  private DivisionTable      processTable      = new DivisionTable();
  private JScrollPane        processScroll     = new JScrollPane(processTable);*/
  
  private TableEditor   processTableEditor = new TableEditor(
          new String[]{"id","наименование"}, 
          new String[]{"id","name"}, Service.class, null, Service.Type.CURRENT);
  
  private JPanel        descriptionPanel  = new JPanel(new BorderLayout());
  private JTextPane     descriptionName   = new JTextPane();
  private JScrollPane   descriptionScroll = new JScrollPane(descriptionName);
  
  private JPanel        sidePanel    = new JPanel(new GridBagLayout());
  private DivisionTextField sellerName   = new DivisionTextField(DivisionTextField.Type.ALL);
  private DivisionTextField customerName = new DivisionTextField(DivisionTextField.Type.ALL);
  
  private DBFilter classUnion;
  
  private JPanel     durationPanel = new JPanel(new GridBagLayout());
  private JTextField duration      = new JTextField();

  
  public XMLContractTemplateTableEditor(String objectClass) {
    super(
            new String[]{"id","наименование"},
            new String[]{"id","name"},
            XMLContractTemplate.class,
            XMLContractTemplateEditor.class,
            "Типы договоров");
    try {
      if(classUnion != null)
        classUnion.clear();
      else classUnion = this.getClientFilter().AND_FILTER();
      classUnion.AND_EQUAL("objectClassName", objectClass);
    }catch(Exception ex){Messanger.showErrorMessage(ex);}
    initComponents();
    initEvents();
  }
  
  private void initComponents() {
    /*processTable.setColumns("id","Наименование");
    processTable.setColumnWidthZero(0);
    processPanel.add(processScroll,  BorderLayout.CENTER);*/
    processTableEditor.setAdministration(false);
    processTableEditor.setVisibleOkButton(false);
    processTableEditor.setTitleBorder("Предмет договора");
    
    getStatusBar().setVisible(false);
    setSingleSelection(true);
    addComponentToStore(split);

    descriptionName.setEditable(false);
    sellerName.setEditable(false);
    sellerName.setColumns(15);
    customerName.setColumns(15);
    customerName.setEditable(false);
    descriptionPanel.setBorder(BorderFactory.createTitledBorder("Описание"));
    descriptionPanel.add(descriptionScroll, BorderLayout.CENTER);
    sidePanel.setBorder(BorderFactory.createTitledBorder("Наименованния сторон"));
    sidePanel.add(new JLabel("Продавец"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    sidePanel.add(sellerName, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    sidePanel.add(new JLabel("Покупатель"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    sidePanel.add(customerName, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(split, BorderLayout.CENTER);
    split.add(getRootPanel(), JSplitPane.LEFT);
    split.add(descriptionPanel, JSplitPane.RIGHT);

    setRootPanel(panel);

    duration.setEditable(false);
    durationPanel.setBorder(BorderFactory.createTitledBorder("Срок действия по умолчанию"));
    durationPanel.add(duration, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    
    getBottomPanel().setLayout(new GridBagLayout());
    getBottomPanel().add(sidePanel,     new GridBagConstraints(0, 0, 1, 1, 0.3, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    getBottomPanel().add(durationPanel, new GridBagConstraints(0, 1, 1, 1, 0.3, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    getBottomPanel().add(processTableEditor.getGUI(),  new GridBagConstraints(1, 0, 1, 2, 0.7, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
  }
  
  private void initEvents() {
    addEditorListener(this);
  }
  
  @Override
  public void changeSelection(EditorGui editor, Integer[] ids) {
    MappingObject[] object = getSelectedObjects();
    if(object.length > 0) {
      try {
        String dur = Utility.convert(((XMLContractTemplate)object[0]).getDuration());
        duration.setText(dur==null?"":dur);

        descriptionName.setText(((XMLContractTemplate)object[0]).getDescription());
        
        processTableEditor.getClientFilter().clear().AND_IN("contractTemplates", getSelectedId());
        processTableEditor.initData();

        SellerNickName sellerNickName   = ((XMLContractTemplate)object[0]).getSellerNickname();
        CompanyNickname customerNickName = ((XMLContractTemplate)object[0]).getCustomerNickname();
        if(sellerNickName != null)
          sellerName.setText(sellerNickName.getName());
        if(customerNickName != null)
          customerName.setText(customerNickName.getName());
      }catch(RemoteException ex){Messanger.showErrorMessage(ex);}
    }
  }

  @Override
  public void dispose() {
    super.dispose();
  }

  @Override
  public String getEmptyObjectTitle() {
    return "";
  }
}
