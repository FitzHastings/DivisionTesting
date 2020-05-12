package bum.realizations;

import bum.annotations.*;
import bum.interfaces.*;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import mapping.MappingObjectImpl;

@Table(unicumFields={@UnicumFields(fields={"company","service","group","type","tmp"}, where = "type='CURRENT' and tmp=false")})

@Triggers(triggers = {
  @Trigger(timeType=Trigger.TIMETYPE.AFTER,
  actionTypes={Trigger.ACTIONTYPE.INSERT},
  procedureText=
    "DECLARE "
    + "  docs record;\n"
    + "BEGIN"
    + "  IF NEW.[!Product(globalProduct)] IS NOT NULL THEN"
    + "    FOR docs IN SELECT [ProductDocument(actionType)], [ProductDocument(document)] FROM [ProductDocument] WHERE [ProductDocument(product)]=NEW.[!Product(globalProduct)] LOOP"
    + "      INSERT INTO [ProductDocument] ([!ProductDocument(actionType)], [!ProductDocument(document)], [!ProductDocument(product)]) "
    + "      VALUES (docs.[!ProductDocument(actionType)], docs.[!ProductDocument(document)], NEW.id);"
    + "    END LOOP;"
    + "  END IF;"
    + "  RETURN NEW;"
    + "END;")
})

public class ProductImpl extends MappingObjectImpl implements Product {
  @ManyToOne(viewFields={"id","name","parent","type","owner","moveStorePosition"},viewNames={"service_id","service_name","service_parent_id","service_type","service_owner","move"})
  private Service service;
  
  @ManyToOne(viewFields={"id","name","parent","type"},viewNames={"group_id","group_name","group_parent_id","group_type"})
  private Group group;

  @ManyToOne(viewFields={"name","type"}, viewNames={"pricelist_name","pricelist_type"})
  private PriceList priceList;

  @ManyToOne(viewFields = {"nds"}, viewNames = {"global-nds"})
  private Product globalProduct;
  
  @ManyToOne
  private Company company;
  
  @ManyToMany
  private List<Factor> factors = new ArrayList<>();
  
  @Column(defaultValue = "CUSTOM_COST")
  private CostType costType = CostType.CUSTOM_COST;
  
  @Column(defaultValue="0.0")
  private BigDecimal cost = new BigDecimal(0.0);
  
  @Column
  private String currency = "р";
  
  @Column
  private Boolean firstService;
  
  @Column(defaultValue="18.0")
  private BigDecimal nds = new BigDecimal(18.0);

  @Column(defaultValue="false")
  private Boolean period = false;

  @Column
  private Period duration = Period.ofDays(5);

  @Column
  private Period recurrence;
  
  @Column(length=1000, defaultValue="")
  private String techPassport;
  
  @Column
  private Integer[] formula;
  
  public ProductImpl() throws RemoteException {
    super();
  }

  @Override
  public Company getCompany() throws RemoteException {
    return company;
  }

  @Override
  public void setCompany(Company company) throws RemoteException {
    this.company = company;
  }

  @Override
  public CostType getCostType() throws RemoteException {
    return costType;
  }

  @Override
  public void setCostType(CostType costType) throws RemoteException {
    this.costType = costType;
  }

  @Override
  public Integer[] getFormula() throws RemoteException {
    return formula;
  }

  @Override
  public void setFormula(Integer[] formula) throws RemoteException {
    this.formula = formula;
  }

  @Override
  public String getTechPassport() throws RemoteException {
    return techPassport;
  }

  @Override
  public void setTechPassport(String techPassport) throws RemoteException {
    this.techPassport = techPassport;
  }

  @Override
  public List<Factor> getFactors() throws RemoteException {
    return factors;
  }

  @Override
  public void setFactors(List<Factor> factors) throws RemoteException {
    this.factors = factors;
  }
  
  @Override
  public void addFactors(List<Factor> factors) throws RemoteException {
    this.factors.addAll(factors);
  }
  
  @Override
  public void removeFactors(List<Factor> factors) throws RemoteException {
    this.factors.removeAll(factors);
  }

  @Override
  public Product getGlobalProduct() throws RemoteException {
    return globalProduct;
  }

  @Override
  public void setGlobalProduct(Product globalProduct) throws RemoteException {
    this.globalProduct = globalProduct;
  }

  @Override
  public Period getDuration() throws RemoteException {
    return duration;
  }

  @Override
  public void setDuration(Period duration) throws RemoteException {
    this.duration = duration;
  }

  @Override
  public Period getRecurrence() throws RemoteException {
    return recurrence;
  }

  @Override
  public void setRecurrence(Period recurrence) throws RemoteException {
    this.recurrence = recurrence;
  }

  @Override
  public Boolean isPeriod() throws RemoteException {
    return period;
  }

  @Override
  public void setPeriod(Boolean period) throws RemoteException {
    this.period = period;
  }

  @Override
  public BigDecimal getNds() throws RemoteException {
    return nds;
  }

  @Override
  public void setNds(BigDecimal nds) throws RemoteException {
    this.nds = nds;
  }

  @Override
  public PriceList getPriceList() throws RemoteException {
    return priceList;
  }

  @Override
  public void setPriceList(PriceList priceList) throws RemoteException {
    this.priceList = priceList;
  }

  @Override
  public void setCost(BigDecimal cost) throws RemoteException {
    this.cost = cost;
  }

  @Override
  public BigDecimal getCost() throws RemoteException {
    return this.cost;
  }

  @Override
  public void setCurrency(String currency) throws RemoteException {
    this.currency = currency;
  }

  @Override
  public String getCurrency() throws RemoteException {
    return this.currency;
  }
  
  @Override
  public void setService(Service service) throws RemoteException {
    this.service = service;
  }

  @Override
  public Service getService() throws RemoteException {
    return this.service;
  }

  @Override
  public void setGroup(Group group) throws RemoteException {
    this.group = (Group)group;
  }
  
  @Override
  public Group getGroup() throws RemoteException {
    return this.group;
  }
  
  @Override
  public Boolean isFirstService() throws RemoteException {
    return firstService;
  }
  
  @Override
  public void setFirstService(Boolean service) throws RemoteException {
    this.firstService = service;
  }
  
  @Override
  public boolean equals(Service service, Group group) throws RemoteException {
    if(this.getService().getId().intValue() != service.getId().intValue() ||
            this.getGroup().getId().intValue() != group.getId().intValue())
      return false;
    return true;
  }
}