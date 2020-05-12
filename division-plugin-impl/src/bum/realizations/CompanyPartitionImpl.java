package bum.realizations;

import bum.annotations.*;
import bum.interfaces.*;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import mapping.MappingObjectImpl;

@Table(
  clientName="Подразделения",
  history=true,
  queryColumns={
    @QueryColumn(
      name="ownership",
      desctiption="Наименование формы собственности",
      query="SELECT name FROM [OwnershipType] "
          + "WHERE [OwnershipType(id)]=(SELECT [Company(ownershipType)] FROM [Company] "
          + "WHERE [Company(id)]=[CompanyPartition(company)])")
})

@Procedures(
  procedures={
    @Procedure(
            name="addStoreToCompanyPartitions",
            procedureText=""
                    + "DECLARE\n"
                    + "  cp record;\n"
                    + "BEGIN\n"
                    + "  FOR cp IN SELECT [CompanyPartition(id)] FROM [CompanyPartition] LOOP\n"

                    + "  INSERT INTO [Store] ([!Store(name)],[!Store(main)],[!Store(storeType)],[!Store(objectType)],[!Store(companyPartition)]) VALUES \n"
                    + "  ('основ.',true,'НАЛИЧНЫЙ','ТМЦ',cp.id);\n"
                    
                    + "  INSERT INTO [Store] ([!Store(name)],[!Store(main)],[!Store(storeType)],[!Store(objectType)],[!Store(companyPartition)]) VALUES \n"
                    + "  ('основ.',true,'БЕЗНАЛИЧНЫЙ','ТМЦ',cp.id);\n"

                    + "  INSERT INTO [Store] ([!Store(name)],[!Store(main)],[!Store(storeType)],[!Store(objectType)],[!Store(companyPartition)],[!Store(currency)]) VALUES \n"
                    + "  ('основ.',true,'НАЛИЧНЫЙ','ВАЛЮТА',cp.id,(SELECT id FROM [Group] WHERE [Group(groupType)]='ВАЛЮТА' AND [Group(main)]=true LIMIT 1));\n"
                    
                    + "  INSERT INTO [Store] ([!Store(name)],[!Store(main)],[!Store(storeType)],[!Store(objectType)],[!Store(companyPartition)],[!Store(currency)]) VALUES \n"
                    + "  ('основ.',true,'БЕЗНАЛИЧНЫЙ','ВАЛЮТА',cp.id,(SELECT id FROM [Group] WHERE [Group(groupType)]='ВАЛЮТА' AND [Group(main)]=true LIMIT 1));\n"

                    + "  END LOOP;\n"
                    + "  RETURN 'OK';"
                    + "END\n"
    ),
    @Procedure(
            name="getCompanyPartitionName",
            arguments={"integer"},
            procedureText=""
                    + "DECLARE"
                    + "  ownershipType text;"
                    + "BEGIN"
                    + "  SELECT [OwnershipType(name)] INTO ownershipType FROM [OwnershipType] WHERE [OwnershipType(id)]="
                    + "    (SELECT [Company(ownershipType)] FROM [Company] WHERE "
                    + "    [Company(id)]=(SELECT [CompanyPartition(company)] FROM [CompanyPartition] WHERE id=$1));"
                    + "  IF ownershipType IS NULL THEN ownershipType := ''; ELSE ownershipType := ownershipType||' '; END IF;"
                    + "  RETURN  ownershipType||"
                    + "    (SELECT [Company(name)] FROM [Company] WHERE [Company(id)]=[CompanyPartition(company)]) ||' ('|| "
                    + "    [CompanyPartition(name)] || ')' FROM [CompanyPartition] WHERE [CompanyPartition(id)]=$1;"
                    + "END"
    )})


@Triggers(triggers=
{
  @Trigger(timeType=Trigger.TIMETYPE.AFTER,
          actionTypes={Trigger.ACTIONTYPE.INSERT},
          procedureText=""
                  + "DECLARE "
                  + "BEGIN"
                  
                  + "  INSERT INTO [Store] ([!Store(name)],[!Store(main)],[!Store(storeType)],[!Store(objectType)],[!Store(companyPartition)]) VALUES \n"
                  + "  ('Основной.',true,'НАЛИЧНЫЙ','ТМЦ',NEW.id);\n"

                  + "  INSERT INTO [Store] ([!Store(name)],[!Store(main)],[!Store(storeType)],[!Store(objectType)],[!Store(companyPartition)],[!Store(currency)]) VALUES \n"
                  + "  ('Касса.',true,'НАЛИЧНЫЙ','ВАЛЮТА',NEW.id,(SELECT id FROM [Group] WHERE [Group(groupType)]='ВАЛЮТА' AND [Group(main)]=true LIMIT 1));\n"

                  + "  INSERT INTO [Store] ([!Store(name)],[!Store(main)],[!Store(storeType)],[!Store(objectType)],[!Store(companyPartition)],[!Store(currency)]) VALUES \n"
                  + "  ('Расчётный счёт.',true,'БЕЗНАЛИЧНЫЙ','ВАЛЮТА',NEW.id,(SELECT id FROM [Group] WHERE [Group(groupType)]='ВАЛЮТА' AND [Group(main)]=true LIMIT 1));\n"

                  + "  RETURN NEW;"
                  + "END;"),
  @Trigger(timeType=Trigger.TIMETYPE.AFTER,
          actionTypes={Trigger.ACTIONTYPE.UPDATE},
          procedureText=""
                  + "DECLARE "
                  + "BEGIN"
                  + "  IF NEW.type != OLD.type THEN"
                  + "    UPDATE [Contract] SET type=NEW.type WHERE [Contract(customerCompanyPartition)]=OLD.id OR "
                  + "    [Contract(sellerCompanyPartition)]=OLD.id;"
                  + "  END IF;"
                  + "  RETURN NEW;"
                  + "END;")
})

public class CompanyPartitionImpl extends MappingObjectImpl implements CompanyPartition {
  @ManyToMany
  private List<DocumentXMLTemplate> defaultTemplates = new ArrayList<>();
  
  @Column(view=false,description="Юридический адрес")
  private String urAddres;

  @Column(view=false,description="Фактический адрес")
  private String addres;

  @Column(view=false,description="Почтовый адрес")
  private String postAddres;

  @Column(description="КПП",defaultValue="")
  private String kpp = "";

  @Column(length=-1,description="Контактная информация")
  private String contactInfo;
  
  @Column(length=-1)
  private String contactFio;
  
  @Column
  private String telefon;
  
  @Column
  private String email;

  @OneToMany(mappedBy="companyPartition")
  private List<Account> accounts = new ArrayList<>();

  @Column(defaultValue="false",description="Основное")
  private Boolean mainPartition = false;

  @ManyToOne(description="Предприятие",viewFields={"inn","name","ndsPayer"},viewNames={"comapny_inn","company_name","nds-payer"})
  private Company company;
  
  
  @Column(defaultValue = "true")
  private Boolean mainnumbering;
  
  @Column(defaultValue = "true")
  private Boolean mainnumberingcontract;
  
  @Column(defaultValue="")
  private String prefix;
  
  @Column
  private String prefixTypeFormat;
  
  @Column(defaultValue="")
  private String prefixSplit;
  
  @Column(defaultValue="")
  private String suffixSplit;
  
  @Column
  private String suffixTypeFormat;

  @Column(defaultValue="")
  private String suffix;
  
  @Column
  private Period periodForZero;
  
  @Column(defaultValue="1")
  private Integer startNumber;
  
  @Column(defaultValue = "false")
  private Boolean grabFreeNumber;
  
  
  
  
  
  @Column
  private String companysExportPath;

  @Column
  private String companysExportFileName;

  @Column
  private String contractsExportPath;

  @Column
  private String contractsExportFileName;

  @Column
  private String documentsExportPath;
  
  @Column
  private LocalDate docStopDate = null;

  public CompanyPartitionImpl() throws RemoteException {
    super();
  }

  @Override
  public List<DocumentXMLTemplate> getDefaultTemplates() {
    return defaultTemplates;
  }

  @Override
  public void setDefaultTemplates(List<DocumentXMLTemplate> defaultTemplates) {
    this.defaultTemplates = defaultTemplates;
  }

  @Override
  public Boolean isMainnumberingcontract() {
    return mainnumberingcontract;
  }

  @Override
  public void setMainnumberingcontract(Boolean mainnumberingcontract) {
    this.mainnumberingcontract = mainnumberingcontract;
  }

  @Override
  public Boolean isMainnumbering() {
    return mainnumbering;
  }

  @Override
  public void setMainnumbering(Boolean mainnumbering) {
    this.mainnumbering = mainnumbering;
  }

  @Override
  public Period getPeriodForZero() {
    return periodForZero;
  }

  @Override
  public void setPeriodForZero(Period periodForZero) {
    this.periodForZero = periodForZero;
  }

  @Override
  public Boolean isGrabFreeNumber() {
    return grabFreeNumber;
  }

  @Override
  public void setGrabFreeNumber(Boolean grabFreeNumber) {
    this.grabFreeNumber = grabFreeNumber;
  }
  
  @Override
  public LocalDate getDocStopDate() {
    return docStopDate;
  }
  
  @Override
  public void setDocStopDate(LocalDate docStopDate) {
    this.docStopDate = docStopDate;
  }

  @Override
  public String getPrefix() throws RemoteException {
    return prefix;
  }

  @Override
  public void setPrefix(String prefix) throws RemoteException {
    this.prefix = prefix;
  }

  @Override
  public String getPrefixSplit() throws RemoteException {
    return prefixSplit;
  }

  @Override
  public void setPrefixSplit(String prefixSplit) throws RemoteException {
    this.prefixSplit = prefixSplit;
  }

  @Override
  public String getPrefixTypeFormat() throws RemoteException {
    return prefixTypeFormat;
  }

  @Override
  public void setPrefixTypeFormat(String prefixTypeFormat) throws RemoteException {
    this.prefixTypeFormat = prefixTypeFormat;
  }

  @Override
  public Integer getStartNumber() throws RemoteException {
    return startNumber;
  }

  @Override
  public void setStartNumber(Integer startNumber) throws RemoteException {
    this.startNumber = startNumber;
  }

  @Override
  public String getSuffix() throws RemoteException {
    return suffix;
  }

  @Override
  public void setSuffix(String suffix) throws RemoteException {
    this.suffix = suffix;
  }

  @Override
  public String getSuffixSplit() throws RemoteException {
    return suffixSplit;
  }

  @Override
  public void setSuffixSplit(String suffixSplit) throws RemoteException {
    this.suffixSplit = suffixSplit;
  }

  @Override
  public String getSuffixTypeFormat() throws RemoteException {
    return suffixTypeFormat;
  }

  @Override
  public void setSuffixTypeFormat(String suffixTypeFormat) throws RemoteException {
    this.suffixTypeFormat = suffixTypeFormat;
  }

  @Override
  public String getContactFio() throws RemoteException {
    return contactFio;
  }

  @Override
  public void setContactFio(String contactFio) throws RemoteException {
    this.contactFio = contactFio;
  }
  
  @Override
  public String getEmail() throws RemoteException {
    return email;
  }

  @Override
  public void setEmail(String email) throws RemoteException {
    this.email = email;
  }

  @Override
  public String getTelefon() throws RemoteException {
    return telefon;
  }

  @Override
  public void setTelefon(String telefon) throws RemoteException {
    this.telefon = telefon;
  }

  @Override
  public String getCompanysExportFileName() throws RemoteException {
    return companysExportFileName;
  }

  @Override
  public void setCompanysExportFileName(String companysExportFileName) throws RemoteException {
    this.companysExportFileName = companysExportFileName;
  }

  @Override
  public String getCompanysExportPath() throws RemoteException {
    return companysExportPath;
  }

  @Override
  public void setCompanysExportPath(String companysExportPath) throws RemoteException {
    this.companysExportPath = companysExportPath;
  }

  @Override
  public String getContractsExportFileName() throws RemoteException {
    return contractsExportFileName;
  }

  @Override
  public void setContractsExportFileName(String contractsExportFileName) throws RemoteException {
    this.contractsExportFileName = contractsExportFileName;
  }

  @Override
  public String getContractsExportPath() throws RemoteException {
    return contractsExportPath;
  }

  @Override
  public void setContractsExportPath(String contractsExportPath) throws RemoteException {
    this.contractsExportPath = contractsExportPath;
  }

  @Override
  public String getDocumentsExportPath() throws RemoteException {
    return documentsExportPath;
  }

  @Override
  public void setDocumentsExportPath(String documentsExportPath) throws RemoteException {
    this.documentsExportPath = documentsExportPath;
  }

  @Override
  public Boolean isMainPartition() throws RemoteException {
    return mainPartition;
  }

  @Override
  public void setMainPartition(Boolean mainPartition) throws RemoteException {
    this.mainPartition = mainPartition;
  }

  @Override
  public Company getCompany() throws RemoteException {
    return company;
  }

  @Override
  public void setCompany(Company company) throws RemoteException {
    this.company = company;
  }

  /*@Override
  public Account getCurrentAccount() throws RemoteException {
    for(Account account:((CompanyPartition)getThis()).getAccounts())
      if(account.isCurrent())
        return account;
    return null;
  }*/

  @Override
  public List<Account> getAccounts() throws RemoteException {
    return accounts;
  }

  @Override
  public void setAccounts(List<Account> accounts) throws RemoteException {
    this.accounts = accounts;
  }

  @Override
  public void setKpp(String kpp) throws RemoteException {
    this.kpp = kpp;
  }

  @Override
  public String getKpp() throws RemoteException {
    return this.kpp;
  }

  @Override
  public String getUrAddres() throws RemoteException {
    return this.urAddres;
  }

  @Override
  public void setUrAddres(String urAddres) throws RemoteException {
    this.urAddres = urAddres;
  }

  @Override
  public void setAddres(String addres) throws RemoteException {
    this.addres = addres;
  }

  @Override
  public String getAddres() throws RemoteException {
    return this.addres;
  }

  @Override
  public void setPostAddres(String postAddres) throws RemoteException {
    this.postAddres = postAddres;
  }

  @Override
  public String getPostAddres() throws RemoteException {
    return this.postAddres;
  }

  @Override
  public String getContactInfo() throws RemoteException {
    return this.contactInfo;
  }

  @Override
  public void setContactInfo(String contactInfo) throws RemoteException {
    this.contactInfo = contactInfo;
  }
}