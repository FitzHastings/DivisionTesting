package division.editors.contract;

import bum.editors.MainObjectEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.*;
import division.editors.objects.company.SellerCustomerPanel;
import division.editors.tables.XMLContractTemplateTableEditor;
import division.exportimport.ExportImportUtil;
import division.swing.LinkLabel;
import division.swing.guimessanger.Messanger;
import division.swing.DivisionButton;
import division.swing.DivisionTextField;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import mapping.MappingObject;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;

public class ContractEditor extends MainObjectEditor {
  private final JLabel        gosContractIdLabel = new JLabel("Идентификатор гос.контракта: ");
  private final DivisionTextField gosContractIdText = new DivisionTextField(DivisionTextField.Type.INTEGER, 20);
  private final LinkLabel     selectType = new LinkLabel("");
  
  private final JPanel        contractPanel  = new JPanel(new GridBagLayout());
  private final JPanel        sidePanel      = new JPanel(new GridBagLayout());

  private final SellerCustomerPanel sellerPanel   = new SellerCustomerPanel("Продавец",true,true,true,true,true,true);
  private final DivisionButton      reverseSides  = new DivisionButton("><");
  private final SellerCustomerPanel customerPanel = new SellerCustomerPanel("Покупатель",true,true,true,true,true,true);
  
  private final JTabbedPane tabb        = new JTabbedPane();
  private final PlanGraphic planGraphic = new PlanGraphic();

  public ContractEditor() {
    initComponents();
    initEvents();
  }

  @Override
  public boolean afterCreateDialog() {
    try {
      if(((Contract)getEditorObject()).isTmp()) {
        boolean is = selectTextForm();
        if(is)
          commitUpdate();
        return is;
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    return super.afterCreateDialog();
  }
  
  private void initComponents() {
    getComponentsToStore().addAll(planGraphic.getStoreComponents());
    selectType.setFont(new Font("Dialog", Font.BOLD, 18));
    
    getTopPanel().setLayout(new GridBagLayout());
    
    getTopPanel().add(getPrintButton(),   new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    getTopPanel().add(gosContractIdLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
    getTopPanel().add(gosContractIdText,  new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 20), 0, 0));
    getTopPanel().add(selectType,         new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    
    tabb.add("План-график", planGraphic);
    
    contractPanel.add(sidePanel, new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
    
    sidePanel.add(sellerPanel,      new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 2), 0, 0));
    sidePanel.add(reverseSides,     new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
    sidePanel.add(customerPanel,    new GridBagConstraints(2, 0, 1, 1, 0.5, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(3, 2, 3, 5), 0, 0));
    
    getRootPanel().add(contractPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    getRootPanel().add(tabb,   new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
  }
  
  private boolean selectTextForm() {
    RemoteSession session = null;
    try {
      Contract contract = (Contract)getEditorObject();
      Integer contractId = contract.getId();
      XMLContractTemplate oldTemplate = contract.getTemplate();
      XMLContractTemplateTableEditor xmlTemplateTableEditor = new XMLContractTemplateTableEditor(((MappingObject)getEditorObject()).getRealClassName());
      xmlTemplateTableEditor.setAutoLoad(true);
      xmlTemplateTableEditor.setAutoStore(true);
      xmlTemplateTableEditor.setDoubleClickSelectable(true);
      xmlTemplateTableEditor.initData();
      XMLContractTemplate templ = (XMLContractTemplate)xmlTemplateTableEditor.getObject();
      if(templ != null) {
        waitCursor();
        Integer templId = templ.getId();
        if(oldTemplate == null || !templId.equals(oldTemplate.getId())) {
          List<List> data = ObjectLoader.executeQuery("SELECT COUNT([Deal(id)]) FROM [Deal] WHERE [Deal(contract)]="+contractId);
          if((Long)data.get(0).get(0) == 0) {
            session = ObjectLoader.createSession(false);
            session.executeUpdate("UPDATE [Contract] SET [Contract(template)]="+templId+" WHERE [Contract(id)]="+contractId);
            replaceTempProcess(contractId, session);
            data = session.executeQuery("SELECT [Contract(contractName)], [Contract(intNumber)], [Contract(number)] "
                    + "FROM [Contract] WHERE [Contract(id)]=?", new Object[]{contractId});
            session.commit();

            if(data.get(0).get(0) != null) {
              setTitle("Договор: "+data.get(0).get(0)+" № "+data.get(0).get(2));
              contract.setNumber((String) data.get(0).get(2));
              contract.setIntNumber((Integer) data.get(0).get(1));
              selectType.setText(getTitle());
            }

            SellerNickName sellerNickName   = templ.getSellerNickname();
            CompanyNickname customerNickName = templ.getCustomerNickname();
            if(sellerNickName != null)
              sellerPanel.setTitle(sellerNickName.getName());
            if(customerNickName != null)
              customerPanel.setTitle(customerNickName.getName());

            planGraphic.initData();
            return true;
          }else Messanger.alert("Смена типа невозможна, так как в договоре присутствуют сделки", "Внимание...", JOptionPane.WARNING_MESSAGE);
        }
      }else if(oldTemplate == null)
        selectType.setText("Выберите тип договора!!!");
    }catch(Exception ex) {
      ObjectLoader.rollBackSession(session);
      Messanger.showErrorMessage(ex);
    }finally {
      defaultCursor();
    }
    return false;
  }
  
  private void replaceTempProcess() {
    RemoteSession session = null;
    try {
      Integer contractId = ((Contract)getEditorObject()).getId();
      session = ObjectLoader.createSession(false);
      replaceTempProcess(contractId, session);
      session.commit();
    }catch(Exception ex) {
      ObjectLoader.rollBackSession(session);
      Messanger.showErrorMessage(ex);
    }
  }
  
  private void replaceTempProcess(Integer contractId, RemoteSession session) throws Exception {
    session.executeUpdate("DELETE FROM [ContractProcess] WHERE [ContractProcess(contract)]="+contractId);
    Integer[] partitions = new Integer[0];
    List<List> data = session.executeQuery("SELECT id FROM [CompanyPartition] WHERE [CompanyPartition(company)]=(SELECT [Contract(customerCompany)] FROM [Contract] WHERE [Contract(id)]="+contractId+")");
    for(List d:data)
      partitions = (Integer[]) ArrayUtils.add(partitions, d.get(0));
    
    if(partitions.length > 0) {
      for(List d:session.executeQuery("SELECT [XMLContractTemplate(processes):target] "
              + "FROM [XMLContractTemplate(processes):table] WHERE [XMLContractTemplate(processes):object]=(SELECT [Contract(template)] FROM [Contract] WHERE id="+contractId+")")) {
        Integer processId = (Integer) d.get(0);

        for(Integer partitionId:partitions) {
          session.executeUpdate("INSERT INTO [ContractProcess]([ContractProcess(process)],[ContractProcess(contract)],[ContractProcess(customerPartition)]) VALUES("+processId+","+contractId+","+partitionId+")");
        }
      }
    }
  }
  

  @Override
  public void addComponentToStore(Component component, String name) {
    super.addComponentToStore(component, name);
  }
  
  private void initEvents() {
    reverseSides.addActionListener((ActionEvent e) -> {
      reverseSidesFunction();
    });
    
    selectType.addActionListener((ActionEvent e) -> {
      selectTextForm();
    });
    
    sellerPanel.addDivisionListener((division.swing.actions.DivisionEvent divisionEvent) -> {
      saveSides();
      planGraphic.initData();
    });

    customerPanel.addDivisionListener((division.swing.actions.DivisionEvent divisionEvent) -> {
      saveSides();
      if("setCompany".equals(divisionEvent.getCommand()))
        replaceTempProcess();
      planGraphic.initData();
    });
  }
  
  private void reverseSidesFunction() {
    Integer sellerCompany   = sellerPanel.getCompany();
    Integer sellerPartition = sellerPanel.getPartition();
    Integer sellerCfc       = sellerPanel.getCfc();
    String  sellerInFace    = sellerPanel.getInFace();
    String  sellerReason    = sellerPanel.getBusinessReason();
    
    Integer customerCompany   = customerPanel.getCompany();
    Integer customerPartition = customerPanel.getPartition();
    Integer customerCfc       = customerPanel.getCfc();
    String  customerInFace    = customerPanel.getInFace();
    String  customerReason    = customerPanel.getBusinessReason();
    
    sellerPanel.setFireEventActive(false);
    sellerPanel.setComapny(customerCompany);
    sellerPanel.setPartition(customerPartition);
    sellerPanel.setCfc(customerCfc);
    sellerPanel.setInFace(customerInFace);
    sellerPanel.setBusinessReason(customerReason);
    sellerPanel.setFireEventActive(true);
    
    customerPanel.setFireEventActive(false);
    customerPanel.setComapny(sellerCompany);
    customerPanel.setPartition(sellerPartition);
    customerPanel.setCfc(sellerCfc);
    customerPanel.setInFace(sellerInFace);
    customerPanel.setBusinessReason(sellerReason);
    customerPanel.setFireEventActive(true);
    saveSides();
    replaceTempProcess();
    planGraphic.initData();
  }
  
  public void saveSides() {
    System.out.println("saveSides".toUpperCase());
    RemoteSession session = null;
    try {
      Integer contractId = ((Contract)getEditorObject()).getId();
      
      Integer sellerCompany   = sellerPanel.getCompany();
      Integer sellerPartition = sellerPanel.getPartition();
      Integer sellerCfc       = sellerPanel.getCfc();
      String  sellerInFace    = sellerPanel.getInFace();
      String  sellerReason    = sellerPanel.getBusinessReason();
      
      Integer customerCompany    = customerPanel.getCompany();
      Integer customerrPartition = customerPanel.getPartition();
      Integer customerCfc        = customerPanel.getCfc();
      String  customerInFace     = customerPanel.getInFace();
      String  customerReason     = customerPanel.getBusinessReason();
      
      session = ObjectLoader.createSession(true);
      session.executeUpdate("UPDATE [Contract] SET "
              + "[Contract(customerCompany)]=?,"
              + "[Contract(customerCompanyPartition)]=?,"
              + "[Contract(customerCfc)]=?,"
              + "[Contract(customerPerson)]=?,"
              + "[Contract(customerReason)]=?,"
              + "[Contract(sellerCompany)]=?,"
              + "[Contract(sellerCompanyPartition)]=?,"
              + "[Contract(sellerCfc)]=?,"
              + "[Contract(sellerPerson)]=?,"
              + "[Contract(sellerReason)]=? "
              + "WHERE [Contract(id)]=?",
              new Object[]{
                customerCompany,
                customerrPartition,
                customerCfc,
                customerInFace,
                customerReason,
                sellerCompany,
                sellerPartition,
                sellerCfc,
                sellerInFace,
                sellerReason,
                contractId});
      List<List> data = session.executeQuery("SELECT "
              + "[Contract(contractName)], "
              + "[Contract(intNumber)], "
              + "[Contract(number)] "
              + "FROM [Contract] WHERE [Contract(id)]=?", new Object[]{contractId});
      session.commit();
      ((Contract)getEditorObject()).setIntNumber((Integer) data.get(0).get(1));
      ((Contract)getEditorObject()).setNumber((String) data.get(0).get(2));
      if(data.get(0).get(0) != null) {
        setTitle("Договор: "+data.get(0).get(0));
        selectType.setText(getTitle());
      }
    }catch(Exception ex) {
      ObjectLoader.rollBackSession(session);
      Messanger.showErrorMessage(ex);
    }
  }

  @Override
  public String commit() throws Exception {
    Contract contract = (Contract)getEditorObject();
    if(contract.getType() == MappingObject.Type.ARCHIVE)
      return null;
    String msg = "";
    if(sellerPanel.getCfc() == null)
      msg += "  -не задана группа продавца\n";
    if(sellerPanel.getPartition() == null)
      msg += "  -не задано подразделение продавца\n";
    if(sellerPanel.getCompany() == null)
      msg += "  -не указано предприятие продавца\n";
    if(customerPanel.getCfc() == null)
      msg += "  -не выбрана группа покупателя\n";
    if(customerPanel.getPartition() == null)
      msg += "  -не выбрано подразделение покупателя\n";
    if(customerPanel.getCompany() == null)
      msg += "  -не задано предприятие покупателя\n";
    if(contract.getTemplate() == null)
      msg += "  -у договора нет шаблона";

    if(!msg.equals(""))
      msg = "Допущенные ошибки\n"+msg;
    else {
      contract.setGosContractId(gosContractIdText.getText());
      contract.setSellerCompany(sellerPanel.getCompany()==null?null:(Company)ObjectLoader.getObject(Company.class, sellerPanel.getCompany()));
      contract.setSellerCfc(sellerPanel.getCfc()==null?null:(CFC)ObjectLoader.getObject(CFC.class, sellerPanel.getCfc()));
      contract.setSellerCompanyPartition(sellerPanel.getPartition()==null?null:(CompanyPartition)ObjectLoader.getObject(CompanyPartition.class, sellerPanel.getPartition()));

      contract.setCustomerCompany(customerPanel.getCompany()==null?null:(Company)ObjectLoader.getObject(Company.class, customerPanel.getCompany()));
      contract.setCustomerCfc(customerPanel.getCfc()==null?null:(CFC)ObjectLoader.getObject(CFC.class, customerPanel.getCfc()));
      contract.setCustomerCompanyPartition(customerPanel.getPartition()==null?null:(CompanyPartition)ObjectLoader.getObject(CompanyPartition.class, customerPanel.getPartition()));

      contract.setSellerPerson(sellerPanel.getInFace());
      contract.setSellerReason(sellerPanel.getBusinessReason());
      contract.setCustomerPerson(customerPanel.getInFace());
      contract.setCustomerReason(customerPanel.getBusinessReason());
      
      contract.setStartDate(new java.sql.Date(planGraphic.getStartDate().getTime()));
      contract.setEndDate(new java.sql.Date(planGraphic.getEndDate().getTime()));
    }
    return msg;
  }

  @Override
  public void clearData() {
  }

  @Override
  public void initData() {
    Contract contract = (Contract)getEditorObject();
    try {
      boolean tmp = contract.isTmp();
      reverseSides.setVisible(tmp);
      sellerPanel.setCompanyChoosable(tmp);
      customerPanel.setCompanyChoosable(tmp);
      
      List<List> data = ObjectLoader.executeQuery("SELECT "
              + "[Contract(number)],"
              + "[Contract(templatename)],"
              + "(SELECT [SellerNickName(name)] FROM [SellerNickName] WHERE [SellerNickName(id)]=(SELECT [XMLContractTemplate(sellerNickname)] FROM [XMLContractTemplate] WHERE [XMLContractTemplate(id)]=[Contract(template)])),"
              + "(SELECT [CompanyNickname(name)] FROM [CompanyNickname] WHERE [CompanyNickname(id)]=(SELECT [XMLContractTemplate(customerNickname)] FROM [XMLContractTemplate] WHERE [XMLContractTemplate(id)]=[Contract(template)])),"
              
              + "[Contract(sellerCompany)],"
              + "[Contract(sellerCompanyPartition)],"
              + "[Contract(sellerCfc)],"
              
              + "[Contract(customerCompany)],"
              + "[Contract(customerCompanyPartition)],"
              + "[Contract(customerCfc)],"
              + "[Contract(gosContractId)]"
              + " FROM [Contract] WHERE [Contract(id)]="+contract.getId());

      if(!data.isEmpty()) {
        setTitle("Договор: "+(data.get(0).get(1)!=null?data.get(0).get(1):"")+(data.get(0).get(0)!=null?" № "+data.get(0).get(0):""));
        selectType.setText(getTitle());
        if(data.get(0).get(2) != null)
          sellerPanel.setTitle((String) data.get(0).get(2));
        if(data.get(0).get(3) != null)
          customerPanel.setTitle((String) data.get(0).get(3));

        /*if(data.get(0).get(0) != null && !data.get(0).get(0).equals("")) {
          if(contract.getType() == Contract.Type.ARCHIVE)
            this.setEnabled(false);
        }*/
        
        sellerPanel.setFireEventActive(false);
        sellerPanel.setComapny((Integer) data.get(0).get(4));
        sellerPanel.setPartition((Integer) data.get(0).get(5));
        sellerPanel.setCfc((Integer) data.get(0).get(6));
        sellerPanel.setInFace(contract.getSellerPerson());
        sellerPanel.setBusinessReason(contract.getSellerReason());
        sellerPanel.setFireEventActive(true);
        
        customerPanel.setFireEventActive(false);
        customerPanel.setComapny((Integer) data.get(0).get(7));
        customerPanel.setPartition((Integer) data.get(0).get(8));
        customerPanel.setCfc((Integer) data.get(0).get(9));
        customerPanel.setInFace(contract.getCustomerPerson());
        customerPanel.setBusinessReason(contract.getCustomerReason());
        customerPanel.setFireEventActive(true);

        planGraphic.setContract(contract.getId());
        planGraphic.initData();
        
        MappingObject.Type type = contract.getType();
        sellerPanel.setEnabled(type != MappingObject.Type.ARCHIVE);
        customerPanel.setEnabled(type != MappingObject.Type.ARCHIVE);
        planGraphic.setEnabled(type != MappingObject.Type.ARCHIVE);
        selectType.setLinkBorder(type != MappingObject.Type.ARCHIVE);
        
        gosContractIdText.setText(data.get(0).get(10) == null ? "" : (String)data.get(0).get(10));
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
      dispose();
    }
  }

  @Override
  public String getEmptyObjectTitle() {
    return "Договор";
  }

  @Override
  protected void printButtonAction() {
    try {
      commit();
      //RMITable xmlTable = ObjectLoader.loadDBTable(XMLTemplate.class);
      /*DBFilter xmlUnion = new DBFilter(XMLTemplate.class);
      xmlUnion.AND_EQUAL("objectClassName", ((Contract)getEditorObject()).getRealClassName());
      List<List> templates = ObjectLoader.getData(XMLTemplate.class, xmlUnion, new String[]{"id","name","XML"});
      for(List v:templates)
        v.add(XMLTemplate.class);*/
      
      Contract contract = (Contract)getEditorObject();
      XMLContractTemplate template = contract.getTemplate();
      if(template != null) {
        List<List> data = new Vector<>();
        data.add(new Vector(Arrays.asList(new Object[]{template.getId(),template.getName(),template.getXML()})));
        for(List v:data)
          v.add(XMLContractTemplate.class);
        //data.addAll(templates);
        if(!data.isEmpty()) {
          JPopupMenu menu = createPrintMenu(data);
          menu.show(getPrintButton(), 0, getPrintButton().getHeight());
        }
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  @Override
  public void exportObject() {
    try {
      Contract contract = (Contract)getEditorObject();
      Integer partitionId = contract.getSellerCompanyPartition().getId();
      String fileName = ExportImportUtil.getExportPath(partitionId);
      ExportImportUtil.export(fileName, Contract.class, contract.getId());
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  @Override
  public void dispose() {
    planGraphic.dispose();
    super.dispose();
  }
}

/**
 * Хочу стать бессердечным гадом.
 * Смотря на всех холодным взгядом
 * Не думать ни о ком вообще
 * Сжигая чувства их ко мне
 *
 * Влюбляться лживо и крутить
 * С сиропом душу женщин пить.
 * Любовью наслаждаться их
 * Плевать на мнение других
 *
 * Слова любви им говорить
 * До слёз собою доводить
 * Без объяснений покидать
 * И новых жертв себе искать
 */