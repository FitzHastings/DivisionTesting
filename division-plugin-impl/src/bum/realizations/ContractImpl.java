package bum.realizations;

import bum.annotations.*;
import bum.interfaces.*;
import java.rmi.RemoteException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import mapping.MappingObjectImpl;

@Triggers(triggers={
  @Trigger(timeType=Trigger.TIMETYPE.AFTER,
  actionTypes={Trigger.ACTIONTYPE.UPDATE},
  procedureText=
    "DECLARE "
    + "BEGIN"
    + "  IF NEW.type != OLD.type THEN"
    + "    UPDATE [Deal] SET type=NEW.type WHERE [Deal(contract)]=OLD.id;"
    + "  END IF;"
    + "  IF NEW.[!Contract(sellerCfc)] != OLD.[!Contract(sellerCfc)] THEN"
    + "    UPDATE [Deal] SET [!Deal(sellerCfc)]=NEW.[!Contract(sellerCfc)] "
    + "      WHERE [Deal(contract)]=NEW.id AND [Deal(dealStartDate)] >= CURRENT_DATE;"
    + "  END IF;"
    + "  IF NEW.[!Contract(customerCfc)] != OLD.[!Contract(customerCfc)] THEN"
    + "    UPDATE [Deal] SET [!Deal(customerCfc)]=NEW.[!Contract(customerCfc)] "
    + "      WHERE [Deal(contract)]=NEW.id;"
    + "  END IF;"
    + "  RETURN NEW;"
    + "END;"),
  @Trigger(timeType=Trigger.TIMETYPE.BEFORE,
        actionTypes={Trigger.ACTIONTYPE.INSERT, Trigger.ACTIONTYPE.UPDATE},
        procedureText=""
        + "DECLARE\n"
        + "  numbers        record;\n"
        + "  prefix         text;\n"
        + "  number         integer;\n"
        + "  suffix         text;\n"
        + "  previosNumber  integer:=0;\n"
        + "  seller         integer;\n"
        + "  partition      integer;\n"
        + "  sel            record;\n"
        + "  typeFormat     text;\n"
        + "  newNumber      text;\n"
        + "BEGIN\n"
        + "  SELECT [Contract(sellerCompanyPartition)] INTO seller FROM [Contract] WHERE [Contract(id)]=NEW.[!Contract(id)];\n"
        + "  IF NEW.[!Contract(sellerCompanyPartition)] IS NULL AND seller IS NULL OR NEW.[!Contract(sellerCompanyPartition)] = seller THEN\n"
        + "    RAISE NOTICE 'seller =  %', seller;\n"
        + "  ELSE\n"
        + "    SELECT * INTO sel FROM [CompanyPartition] WHERE [CompanyPartition(id)]=NEW.[!Contract(sellerCompanyPartition)];\n"
        + "    FOR numbers IN SELECT [Contract(intNumber)] FROM [Contract] \n"
        + "        WHERE [Contract(intNumber)] NOTNULL AND [Contract(sellerCompanyPartition)]=NEW.[!Contract(sellerCompanyPartition)] AND tmp=false AND type='CURRENT' ORDER BY [Contract(intNumber)]\n"
        + "    LOOP\n"
        + "      IF previosNumber != 0 AND numbers.[!Contract(intNumber)] - previosNumber > 1 THEN \n"
        + "        number  := previosNumber;\n"
        + "      END IF;\n"
        + "      previosNumber := numbers.[!Contract(intNumber)];\n"
        + "    END LOOP;\n"
        
        + "    IF number IS NULL THEN number := previosNumber; END IF;\n"
        + "    IF number IS NULL OR number = 0 THEN number := sel.[!CompanyPartition(startNumber)] - 1; END IF;\n"
        
        + "    NEW.[!Contract(intNumber)] := number + 1;\n"
        
        + "    prefix := sel.prefix;\n"
        + "    IF sel.prefixTypeFormat IS NOT NULL THEN \n"
        + "      prefix := prefix||to_char(CURRENT_TIMESTAMP, replace(replace(replace(sel.prefixTypeFormat,'г','Y'),'м','M'),'д','D'));\n"
        + "    END IF;\n"
        + "    prefix := prefix||sel.prefixSplit;\n"
        
        + "    suffix := sel.suffixSplit;\n"
        + "    IF sel.suffixTypeFormat IS NOT NULL THEN \n"
        + "      suffix := suffix||to_char(CURRENT_TIMESTAMP, replace(replace(replace(sel.suffixTypeFormat,'г','Y'),'м','M'),'д','D'));\n"
        + "    END IF;\n"
        + "    suffix := suffix||sel.suffix;\n"
        
        + "    NEW.[!Contract(number)] := NEW.[!Contract(intNumber)];\n"
        
        + "    IF prefix IS NOT NULL THEN NEW.[!Contract(number)] := prefix||NEW.[!Contract(number)]; END IF;\n"
        + "    IF suffix IS NOT NULL THEN NEW.[!Contract(number)] := NEW.[!Contract(number)]||suffix; END IF;\n"
        
        + "  END IF;\n"
        + "  RETURN NEW;\n"
        + "END;\n")
})

@Table(
  clientName="Договора",
  queryColumns={
    @QueryColumn(
      name="seller",
      desctiption="Наименование продавца",
      query="SELECT getCompanyPartitionName([Contract(sellerCompanyPartition)])"),
    @QueryColumn(
      name="sellerInn",
      desctiption="ИНН продавца",
      query="SELECT [Company(inn)] FROM [Company] WHERE [Company(id)]=[Contract(sellerCompany)]"),
    @QueryColumn(
      name="sellerKpp",
      desctiption="КПП продавца",
      query="SELECT [CompanyPartition(kpp)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[Contract(sellerCompanyPartition)]"),
    @QueryColumn(
      name="customer",
      desctiption="Наименование покупателя",
      query="SELECT getCompanyPartitionName([Contract(customerCompanyPartition)])"),
    @QueryColumn(
      name="customerInn",
      desctiption="ИНН покупателя",
      query="SELECT [Company(inn)] FROM [Company] WHERE [Company(id)]=[Contract(customerCompany)]"),
    @QueryColumn(
      name="customerKpp",
      desctiption="КПП покупателя",
      query="SELECT [CompanyPartition(kpp)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[Contract(customerCompanyPartition)]"),
    @QueryColumn(
      name="contractName",
      desctiption="Наименование договора",
      query="SELECT getContractName([Contract(id)])")
  })

@Procedures(procedures={
  @Procedure(
        name="getContractName",
        arguments={"integer"},
        returnType="text",
        procedureText= ""
        + "DECLARE "
        + "BEGIN "
        + "  RETURN (SELECT (SELECT [XMLContractTemplate(name)] FROM [XMLContractTemplate] WHERE [XMLContractTemplate(id)]=[Contract(template)])"
        + "    ||' № '||[Contract(number)] FROM [Contract] WHERE id=$1);"
        + "END"
        )
})

public class ContractImpl extends MappingObjectImpl implements Contract {
  @Column
  private String number;
  
  @Column
  private String gosContractId;

  @Column
  private Integer intNumber;

  @Column(defaultValue="CURRENT_DATE")
  private Date startDate;

  @Column(defaultValue="CURRENT_DATE")
  private Date endDate;

  @Column
  private String sellerReason;

  @Column
  private String sellerPerson;

  @Column
  private String customerReason;

  @Column
  private String customerPerson;

  @ManyToOne(viewFields={"name","bookkeeper","stamp","chifSignature","bookkeeperSignature"}, viewNames={"seller_company_name","seller-bookkeeper","seller-stamp","seller-chifSignature","seller-bookkeeperSignature"}, description="Продавец")
  private Company sellerCompany;

  @ManyToOne(viewFields={"name","bookkeeper"}, viewNames={"customer_company_name","customer-bookkeeper"}, description="Покупатель")
  private Company customerCompany;

  @ManyToOne(viewFields={"name"}, viewNames={"seller_cfc_name"})
  private CFC sellerCfc;

  @ManyToOne(viewFields={"name"}, viewNames={"customer_cfc_name"})
  private CFC customerCfc;

  @OneToMany(mappedBy="contract")
  private List<Deal> deals = new ArrayList<>();

  @ManyToOne(viewFields={"name","sellerNickname","customerNickname","duration"}, viewNames={"templatename","seller-nickname","customer-nickname","contract-duration"}, saveNow=true)
  private XMLContractTemplate template;

  @ManyToOne(viewFields={"name","kpp"},viewNames={"sellerpartitionname","sellerpartition_kpp"})
  private CompanyPartition sellerCompanyPartition;

  @ManyToOne(viewFields={"name","kpp"},viewNames={"customerpartitionname","customerpartition_kpp"})
  private CompanyPartition customerCompanyPartition;
  
  public ContractImpl() throws RemoteException {
    super();
  }

  @Override
  public String getGosContractId() throws RemoteException {
    return gosContractId;
  }

  @Override
  public void setGosContractId(String gosContractId) throws RemoteException {
    this.gosContractId = gosContractId;
  }

  @Override
  public Integer getIntNumber() throws RemoteException {
    return intNumber;
  }

  @Override
  public void setIntNumber(Integer intNumber) throws RemoteException {
    this.intNumber = intNumber;
  }

  @Override
  public CompanyPartition getCustomerCompanyPartition() throws RemoteException {
    return customerCompanyPartition;
  }

  @Override
  public void setCustomerCompanyPartition(CompanyPartition customerCompanyPartition) throws RemoteException {
    this.customerCompanyPartition = customerCompanyPartition;
  }

  @Override
  public CompanyPartition getSellerCompanyPartition() throws RemoteException {
    return sellerCompanyPartition;
  }

  @Override
  public void setSellerCompanyPartition(CompanyPartition sellerCompanyPartition) throws RemoteException {
    this.sellerCompanyPartition = sellerCompanyPartition;
  }

  @Override
  public XMLContractTemplate getTemplate() throws RemoteException {
    return template;
  }

  @Override
  public void setTemplate(XMLContractTemplate template) throws RemoteException {
    this.template = template;
  }

  @Override
  public List<Deal> getDeals() throws RemoteException {
    return deals;
  }

  @Override
  public void setDeals(List<Deal> deals) throws RemoteException {
    this.deals = deals;
  }
  
  @Override
  public void setNumber(String number) throws RemoteException {
    this.number = number;
  }

  @Override
  public String getNumber() throws RemoteException {
    return this.number;
  }

  @Override
  public Date getStartDate() throws RemoteException {
    return this.startDate;
  }
  
  @Override
  public void setStartDate(Date startDate) throws RemoteException {
    this.startDate = startDate;
  }

  @Override
  public Date getEndDate() throws RemoteException {
    return this.endDate;
  }
  
  @Override
  public void setEndDate(Date endDate) throws RemoteException {
    this.endDate = endDate;
  }
  
  @Override
  public CFC getCustomerCfc() throws RemoteException {
    return this.customerCfc;
  }

  @Override
  public Company getCustomerCompany() throws RemoteException {
    return this.customerCompany;
  }

  @Override
  public String getCustomerPerson() throws RemoteException {
    return this.customerPerson;
  }

  @Override
  public String getCustomerReason() throws RemoteException {
    return this.customerReason;
  }

  @Override
  public CFC getSellerCfc() throws RemoteException {
    return this.sellerCfc;
  }

  @Override
  public Company getSellerCompany() throws RemoteException {
    return this.sellerCompany;
  }

  @Override
  public String getSellerPerson() throws RemoteException {
    return this.sellerPerson;
  }

  @Override
  public String getSellerReason() throws RemoteException {
    return this.sellerReason;
  }

  @Override
  public void setCustomerCfc(CFC customerCfc) throws RemoteException {
    this.customerCfc = customerCfc;
  }

  @Override
  public void setCustomerCompany(Company customerCompany) throws RemoteException {
    this.customerCompany = customerCompany;
  }

  @Override
  public void setCustomerPerson(String customerPerson) throws RemoteException {
    this.customerPerson = customerPerson;
  }

  @Override
  public void setCustomerReason(String customerReason) throws RemoteException {
    this.customerReason = customerReason;
  }

  @Override
  public void setSellerCfc(CFC sellerCfc) throws RemoteException {
    this.sellerCfc = sellerCfc;
  }

  @Override
  public void setSellerCompany(Company sellerCompany) throws RemoteException {
    this.sellerCompany = sellerCompany;
  }

  @Override
  public void setSellerPerson(String sellerPerson) throws RemoteException {
    this.sellerPerson = sellerPerson;
  }

  @Override
  public void setSellerReason(String sellerReason) throws RemoteException {
    this.sellerReason = sellerReason;
  }
}