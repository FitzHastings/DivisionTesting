package division.editors.objects.company;

import bum.editors.util.ObjectLoader;
import bum.interfaces.CFC;
import bum.interfaces.Company;
import bum.interfaces.CompanyPartition;
import division.swing.DivisionComboBox;
import division.swing.DivisionItem;
import division.swing.guimessanger.Messanger;
import division.swing.actions.DivisionEvent;
import division.swing.actions.DivisionListener;
import division.swing.actions.DivisionListenerList;
import division.swing.DivisionTextField;
import division.swing.dnd.DNDPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;
import util.filter.local.DBFilter;

public class SellerCustomerPanel extends DNDPanel {
  private final DivisionTextField companyName    = new DivisionTextField("Наименование организации");
  
  private final JLabel            CFCLabel       = new JLabel("Группа");
  private final DivisionComboBox  CFCName        = new DivisionComboBox("Группа");
  
  private final DivisionComboBox  partitionName  = new DivisionComboBox();

  private final JLabel            innLabel       = new JLabel("ИНН");
  private final DivisionTextField innName        = new DivisionTextField(DivisionTextField.Type.INTEGER,"ИНН...");

  private final JLabel            kppLabel       = new JLabel("КПП");
  private final DivisionTextField kppName        = new DivisionTextField(DivisionTextField.Type.INTEGER,"КПП...");
  
  private final JPanel inFaceAndReasonPanel  = new JPanel(new GridBagLayout());
  
  private final JPanel        inFacePanel    = new JPanel(new GridBagLayout());
  private final JLabel        inFaceLabel    = new JLabel("В лице");
  private final DivisionTextField inFaceName     = new DivisionTextField(DivisionTextField.Type.ALL);
  
  private final JPanel        reasonPanel    = new JPanel(new GridBagLayout());
  private final JLabel        reasonLabel    = new JLabel("на основании");
  private final DivisionTextField reasonName     = new DivisionTextField(DivisionTextField.Type.ALL);

  private boolean cfcActive = true;
  private boolean partitionActive = true;

  private final DivisionListenerList listener = new DivisionListenerList();
  
  private Integer companyId;

  public SellerCustomerPanel(String title) {
    this(title, true, true, true, true, true, true);
  }

  public SellerCustomerPanel(String title, boolean cfcVisible, boolean partitionVisible, boolean innVisible, boolean kppVisible, boolean inFaceVisible, boolean reasonVisible) {
    super(title, new GridBagLayout());

    CFCLabel.setVisible(cfcVisible);
    CFCName.setVisible(cfcVisible);

    partitionName.setVisible(partitionVisible);

    innLabel.setVisible(innVisible);
    innName.setVisible(innVisible);

    kppLabel.setVisible(kppVisible && partitionVisible);
    kppName.setVisible(kppVisible && partitionVisible);
    
    inFaceAndReasonPanel.setVisible(inFaceVisible || reasonVisible);
    
    inFacePanel.setVisible(inFaceVisible);
    inFaceLabel.setVisible(inFaceVisible);
    inFaceName.setVisible(inFaceVisible);
    
    reasonPanel.setVisible(reasonVisible);
    reasonLabel.setVisible(reasonVisible);
    reasonName.setVisible(reasonVisible);

    initComponents();
    initEvents();
  }

  public void addDivisionListener(DivisionListener divisionListener) {
    if(CFCName.isVisible())
      listener.add("setCFC", divisionListener);
    listener.add("setCompany", divisionListener);
    if(partitionName.isVisible())
      listener.add("setPartition", divisionListener);
  }

  private void initComponents() {
    companyName.setEditable(false);
    companyName.setSelectedTextColor(companyName.getForeground());
    companyName.setSelectionColor(companyName.getBackground());
    innName.setEditable(false);
    innName.setSelectedTextColor(innName.getForeground());
    innName.setSelectionColor(innName.getBackground());
    kppName.setEditable(false);
    kppName.setSelectedTextColor(kppName.getForeground());
    kppName.setSelectionColor(kppName.getBackground());
    
    innName.setColumns(8);
    kppName.setColumns(8);
    
    add(innName,        new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
    add(companyName,    new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));

    add(CFCLabel,       new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
    add(CFCName,        new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));

    add(kppName,        new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
    add(partitionName,  new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
    
    inFacePanel.setBorder(BorderFactory.createTitledBorder("В лице"));
    inFacePanel.add(inFaceName,  new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(2, 3, 2, 3), 0, 0));
    
    reasonPanel.setBorder(BorderFactory.createTitledBorder("Действующего на основании"));
    reasonPanel.add(reasonName,  new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(2, 3, 2, 3), 0, 0));
    
    inFaceAndReasonPanel.add(inFacePanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    inFaceAndReasonPanel.add(reasonPanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    
    add(inFaceAndReasonPanel, new GridBagConstraints(0, 3, 2, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
  }

  private void initEvents() {
    getBorder().addActionListener((ActionEvent e) -> {
      selectCompany();
    });

    CFCName.addItemListener((ItemEvent e) -> {
      if(e.getStateChange() == ItemEvent.SELECTED && cfcActive)
        setCfc(((DivisionItem)CFCName.getSelectedItem()).getId());
    });

    partitionName.addItemListener((ItemEvent e) -> {
      if(e.getStateChange() == ItemEvent.SELECTED && partitionActive) {
        fillKpp();
      }
    });
    
    addMouseListener(new MouseInputAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 2)
          openCompany();
      }
    });
    
    companyName.addMouseListener(new MouseInputAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 2)
          openCompany();
      }
    });
    
    innName.addMouseListener(new MouseInputAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 2)
          openCompany();
      }
    });
    
    kppName.addMouseListener(new MouseInputAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 2)
          openCompany();
      }
    });
  }
  
  private void openCompany() {
    try {
      if(getCompany() != null) {
        nCompanyEditor companyEditor = new nCompanyEditor();
        companyEditor.setAutoLoad(true);
        companyEditor.setAutoStore(true);
        companyEditor.setEditorObject(ObjectLoader.getObject(Company.class, getCompany()));
        //EditorController.getDeskTop().add(companyEditor).getInternalDialog().setVisible(true);
        companyEditor.showEditorDialog(this);
      }else selectCompany();
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  public void clear() {
    setComapny(null);
  }
  
  private void fillKpp() {
    if(kppName.isVisible()) {
      kppName.setText(partitionName.getSelectedItem()==null||partitionName.getSelectedIndex()<0||((DivisionItem)partitionName.getSelectedItem()).getData()==null?"":(String) ((DivisionItem)partitionName.getSelectedItem()).getData());
    }
  }

  public void selectCompany() {
    final CompanySelectionEditor companySelectionPanel = new CompanySelectionEditor();
    companySelectionPanel.setSingleSelection(true);
    companySelectionPanel.setAutoLoad(true);
    companySelectionPanel.setAutoStore(true);
    companySelectionPanel.initData();
    Integer[] ids = companySelectionPanel.get();
    if(ids.length > 0) {
      setComapny(ids[0]);
      if(CFCName.isVisible()) {
        ids = companySelectionPanel.getSelectedCFCIds();
        if(ids.length > 0)
          setCfc(ids[0]);
      }
    }
  }

  public void setFireEventActive(boolean fireEventActive) {
    listener.setFireEventActive(fireEventActive);
  }

  public boolean isFireEventActive() {
    return listener.isFireEventActive();
  }

  public Integer getCfc() {
    try{
      return ((DivisionItem)CFCName.getSelectedItem()).getId();
    }catch(Exception ex) {
      return null;
    }
  }

  public Integer getCompany() {
    return companyId;
  }

  public Integer getPartition() {
    try{
      return ((DivisionItem) partitionName.getSelectedItem()).getId();
    }catch(Exception ex) {
      return null;
    }
  }

  public void setComapny(Integer companyId) {
    this.companyId = companyId;
    try {
      String ownership = "";
      String name = "";
      String chiefPlace = "";
      String chiefName = "";
      String businessReason = "";
      String inn = "";
      
      if(companyId != null) {
        List<List> data = ObjectLoader.getData(Company.class, new Integer[]{companyId}, new String[]{
          "ownership",
          "name",
          "query:(SELECT name FROM [Place] WHERE [Place(id)]=[Company(chiefPlace)])",
          "chiefName",
          "businessReason",
          "inn"
        });
        if(!data.isEmpty()) {
          ownership      = data.get(0).get(0)==null?"":(String) data.get(0).get(0);
          name           = data.get(0).get(1)==null?"":(String) data.get(0).get(1);
          chiefPlace     = data.get(0).get(2)==null?"":(String) data.get(0).get(2);
          chiefName      = data.get(0).get(3)==null?"":(String) data.get(0).get(3);
          businessReason = data.get(0).get(4)==null?"":(String) data.get(0).get(4);
          inn            = data.get(0).get(5)==null?"":(String) data.get(0).get(5);
        }
      }
      
      companyName.setText(ownership+" "+name);
      if(inFaceName.isVisible()) {
        if(!chiefPlace.equals("")) {
          String[] sm = chiefPlace.trim().split(" ");
          chiefPlace = "";
          for(String s:sm) {
            s = s.trim();
            if(s.endsWith("ый"))
              s = s.substring(0,s.length()-2)+"ого";
            else if(s.endsWith("ор") || s.endsWith("ер"))
              s = s+"а";
            chiefPlace += s+" ";
          }
        }
        
        if(!chiefName.equals("")) {
          String[] sm = chiefName.trim().split(" ");
          chiefName = "";
          for(String s:sm) {
            s = s.trim();
            if(s.endsWith("ов") || s.endsWith("ев"))
              s += "а";
            chiefName += s+" ";
          }
        }
        String inFaceText = chiefPlace.trim()+" "+chiefName;
        inFaceName.setText(inFaceText.trim());
      }
      
      if(reasonName.isVisible()) {
        if(!businessReason.equals("")) {
          String[] sm = businessReason.trim().split(" ");
          businessReason = "";
          for(String s:sm) {
            s = s.trim();
            if(s.endsWith("во"))
              s = s.substring(0,s.length()-2)+"ва";
            else if(s.endsWith("ав"))
              s = s.substring(0,s.length()-2)+"ава";
            businessReason += s+" ";
          }
        }
        reasonName.setText(businessReason);
      }
      
      if(innName.isVisible())
        innName.setText(inn);
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    listener.fire(new DivisionEvent(this, "setCompany", companyId));
    if(CFCName.isVisible())
      fillCFC();
    if(partitionName.isVisible())
      fillPartitions();
  }

  public void setPartition(Integer partitionId) {
    if(partitionId != null) {
      try {
        List<List> data = ObjectLoader.getData(CompanyPartition.class, new Integer[]{partitionId}, new String[]{"name","company","kpp"});
        if(!data.isEmpty()) {
          if(getCompany() == null)
            setComapny((Integer) data.get(0).get(1));
          partitionActive = false;
          partitionName.removeItem(partitionId);
          partitionName.addItem(new DivisionItem(partitionId, (String) data.get(0).get(0), "CompanyPartition", data.get(0).get(2)));
          partitionName.setSelectedItem(partitionId);
          partitionActive = true;
          listener.fire(new DivisionEvent(this, "setPartition", partitionId));
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
    fillKpp();
  }

  public void setCfc(Integer cfcId) {
    if(cfcId != null) {
      try {
        List<List> data = ObjectLoader.getData(CFC.class, new Integer[]{cfcId}, new String[]{"name"});
        if(!data.isEmpty()) {
          cfcActive = false;
          CFCName.removeItem(cfcId);
          CFCName.addItem(new DivisionItem(cfcId, (String) data.get(0).get(0), "CFC"));
          CFCName.setSelectedItem(cfcId);
          cfcActive = true;
          listener.fire(new DivisionEvent(this, "setCFC", cfcId));
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }
  
  public void setInFace(String inface) {
    if(inface != null && !inface.equals(""))
      inFaceName.setText(inface);
  }

  public String getInFace() {
    return inFaceName.getText();
  }

  public void setBusinessReason(String reason) {
    if(reason != null && !reason.equals(""))
      reasonName.setText(reason);
  }

  public String getBusinessReason() {
    return reasonName.getText();
  }

  private void fillPartitions() {
    try {
      partitionName.clear();
      Integer mainId = null;
      if(getCompany() != null) {
        List<List> data = ObjectLoader.getData(DBFilter.create(CompanyPartition.class).AND_EQUAL("company", getCompany()).AND_EQUAL("tmp", false), 
                new String[]{"id","name","mainPartition","kpp"});
        partitionActive = false;
        for(List d:data) {
          if((boolean) d.get(2))
            mainId = (Integer) d.get(0);
          partitionName.addItem(new DivisionItem((Integer) d.get(0), (String) d.get(1), "CompanyPartition", d.get(3)));
        }
        partitionActive = true;
      }
      setPartition(mainId);
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  private void fillCFC() {
    CFCName.clear();
    if(getCompany() != null) {
       try {
        List<List> data = ObjectLoader.executeQuery("SELECT [CFC(id)], [CFC(name)] FROM [CFC] WHERE [CFC(id)] IN (SELECT [Company(cfcs):target] FROM [Company(cfcs):table] WHERE [Company(cfcs):object]="+getCompany()+")", true);
         cfcActive = false;
        for(List d:data) {
          CFCName.addItem(new DivisionItem((Integer) d.get(0), (String) d.get(1), "CFC"));
        }
        cfcActive = true;
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }
  
  public void setCompanyChoosable(boolean choosable) {
    getBorder().setLinkBorder(choosable);
  }
  
  public void setCFCChoosable(boolean choosable) {
    CFCName.setEnabled(choosable);
  }
  
  public void setPartitionChoosable(boolean choosable) {
    partitionName.setEnabled(choosable);
  }
}