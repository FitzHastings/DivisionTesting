package bum.realizations;

import bum.annotations.*;
import bum.interfaces.*;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import mapping.MappingObjectImpl;
import util.DBRelation;

@Table(
  history=true,
  queryColumns={
    @QueryColumn(
      name = "factors",
      query = "(SELECT array_agg([Group(factors):target]) FROM [Group(factors):table] WHERE [Group(factors):object]=[Equipment(group)])"
    ),
    @QueryColumn(
      name = "factor_values",
      query = "(SELECT array_agg([EquipmentFactorValue(factor)]||':'||(case when name isnull then '' else name end)) FROM [EquipmentFactorValue] WHERE tmp=false and type='CURRENT' and "
              + "[EquipmentFactorValue(equipment)]=[Equipment(id)] and "
              + "[EquipmentFactorValue(factor)] in (SELECT [Group(factors):target] FROM [Group(factors):table] WHERE [Group(factors):object]=[Equipment(group)]))"
    ),
    @QueryColumn(
      name="company_name",
      query="SELECT getCompanyPartitionName([Equipment(owner-id)])"),
    @QueryColumn(
      name="identity_id",
      query="SELECT getIdentificatorId([Equipment(id)])"),
    @QueryColumn(
      name="identity_name",
      query="SELECT getIdentificatorName([Equipment(id)])"),
    @QueryColumn(
      name="identity_value_id",
      query="SELECT getIdentificatorValueId([Equipment(id)])"),
    @QueryColumn(
      name="identity_value_name",
      query="SELECT getIdentificatorValue([Equipment(id)])"),
    @QueryColumn(
      name="reserve",
      query="NULLTOZERO((SELECT SUM([DealPosition(amount)]) "
              + "FROM [DealPosition] "
              + "WHERE "
              + "[DealPosition(equipment)]=[Equipment(id)] AND "
              + "[DealPosition(dispatchId)] ISNULL AND "
              + "(SELECT [Service(moveStorePosition)] FROM [Service] WHERE [Service(id)]=(SELECT [Product(service)] FROM [Product] WHERE [Product(id)]=[DealPosition(product)])) = true AND "
              + "tmp=false AND type='CURRENT'))"),
    @QueryColumn(
      name="previous-provider",
      query="SELECT getCompanyPartitionName((SELECT [Deal(sellerCompanyPartition)] FROM [Deal] "
              + "WHERE [Deal(id)]=(SELECT [DealPosition(deal)] FROM [DealPosition] WHERE [DealPosition(id)]=[Equipment(sourceDealPosition)])))"),
    @QueryColumn(
      name="mean-price",
      query="SELECT getMeanPrice([Equipment(group)], [Equipment(owner-id)])"),
    
    @QueryColumn(
      name="cost",
       query="SELECT CASE WHEN [Equipment(sourceDealPosition)] NOTNULL THEN [Equipment(custom-product-cost)] ELSE 0.0::NUMERIC END"),
    @QueryColumn(
            name = "hashcode",
            query = "replace(quote_nullable([Equipment(group)])||quote_nullable([Equipment(store)])||(SELECT array_to_string(ARRAY(SELECT [EquipmentFactorValue(factor)]||quote_nullable([EquipmentFactorValue(name)]) FROM [EquipmentFactorValue] WHERE [EquipmentFactorValue(equipment)]=[Equipment(id)] AND tmp=false and type='CURRENT' ORDER BY [EquipmentFactorValue(factor)]),'')), '''', '')"
    ),
    @QueryColumn(
      name="block",
      query="SELECT SUM([DealPosition(amount)]) FROM [DealPosition] WHERE "
              + "[DealPosition(equipment)]=[Equipment(id)] AND "
              + "(SELECT [Service(moveStorePosition)] FROM [Service] WHERE [Service(id)]=[DealPosition(process)]) AND "
              + "[DealPosition(dispatchDate)] ISNULL AND "
              + "[DealPosition(tmp)]=false AND "
              + "[DealPosition(type)]='CURRENT'")
  })

@Procedures(procedures = {
  @Procedure(
          name = "getMeanPrice",
          arguments = {"integer","integer"},
          returnType = "NUMERIC",
          procedureText = ""
                  + "DECLARE\n"
                  + "BEGIN\n"
                  + "RETURN ((SELECT SUM(([Equipment(cost)])*([Equipment(amount)]-[Equipment(reserve)])) \n"
                  + "    FROM [Equipment] \n"
                  + "    WHERE [Equipment(group)]=$1 AND [Equipment(owner-id)]=$2)/\n"
                  
                  + "    (SELECT CASE WHEN SUM([Equipment(amount)])=0 THEN 1 ELSE SUM([Equipment(amount)]) END FROM [Equipment] "
                  + "    WHERE [Equipment(group)]=$1 AND [Equipment(owner-id)]=$2));\n"
                  + "END\n"
  ),
  @Procedure(
          name = "getFreeMoneyId",
          arguments = {"integer"},
          returnType = "integer",
          procedureText = ""
                  + "DECLARE\n"
                  + "  storeId integer := $1;\n"
                  + "  groupId integer;\n"
                  + "  amount  numeric := 0;\n"
                  + "BEGIN\n"
                  + "  SELECT [Store(currency)] INTO groupId FROM [Store] WHERE id=storeId;\n"
                  //Получаем доступное количество денег
                  + "  SELECT SUM([Equipment(amount)]-[Equipment(reserve)]) INTO amount FROM [Equipment] WHERE "
                  + "  [Equipment(store)]=storeId AND [Equipment(group)]=groupId AND tmp=false AND type='CURRENT';\n"
                  
                  + "  UPDATE [Equipment] SET [!Equipment(amount)]=[!Equipment(reserve)] WHERE "
                  + "  [!Equipment(store)]=storeId AND [!Equipment(group)]=groupId AND tmp=false AND type='CURRENT';\n"
                  
                  + "  DELETE FROM [Equipment] WHERE [Equipment(amount)]=0 AND [!Equipment(store)]=storeId AND [!Equipment(group)]=groupId;"
                  
                  + "  IF amount IS NULL THEN amount:=0; END IF;\n"
                  
                  + "  INSERT INTO [Equipment]([!Equipment(amount)],[!Equipment(store)],[!Equipment(group)]) VALUES(amount, storeId, groupId);\n"
                  + "  RETURN (SELECT MAX(id) FROM [Equipment]);\n"
                  + "END\n"
  )
})

public class EquipmentImpl extends MappingObjectImpl implements Equipment {
  /*@ManyToOne(
          viewFields = {"id",       "name"},
          viewNames  = {"owner_id", "owner_name"})
  private CompanyPartition companyPartition;*/
  
  @ManyToOne(
          viewFields = {"name",       "storeType",  "objectType", "companyPartition"}, 
          viewNames  = {"store-name", "store-type", "object-type", "owner-id"})
  private Store store;
  
  @OneToMany(mappedBy="equipment")
  private List<DealPosition> dealPositions = new ArrayList<>();
  
  @ManyToOne(
          viewFields = {"id",       "name",       "unit",       "groupType",  "cost",       "factors"},
          viewNames  = {"group_id", "group_name", "group-unit", "group-type", "group-cost", "group-factors"},
          nullable=false)
  private Group group;
  
  @OneToMany(mappedBy="equipment")
  private List<EquipmentFactorValue> groupFactorValues = new ArrayList<>();

  @Column(defaultValue="",length=1000)
  private String partition = "";

  @Column(defaultValue="1")
  private BigDecimal amount = new BigDecimal(1);
  
  @ManyToOne(on_delete = DBRelation.ActionType.SET_NULL, 
          viewFields = {"dispatchId",  "dispatchDate",  "customProductCost"}, 
          viewNames  = {"dispatch-id", "dispatch-date", "custom-product-cost"})
  private DealPosition sourceDealPosition;
  
  @Column(defaultValue = "false")
  private Boolean zakaz = false;
  
  @ManyToOne
  private Equipment parent;
  
  @ManyToMany(mappedBy = "equipments")
  private List<Request> requests;
  
  public EquipmentImpl() throws RemoteException {
    super();
  }

  @Override
  public List<Request> getRequests() throws RemoteException {
    return requests;
  }

  @Override
  public void setRequests(List<Request> requests) throws RemoteException {
    this.requests = requests;
  }

  @Override
  public Equipment getParent() throws RemoteException {
    return parent;
  }

  @Override
  public void setParent(Equipment parent) throws RemoteException {
    this.parent = parent;
  }

  @Override
  public Boolean isZakaz() throws RemoteException {
    return zakaz;
  }

  @Override
  public void setZakaz(Boolean zakaz) throws RemoteException {
    this.zakaz = zakaz;
  }

  @Override
  public DealPosition getSourceDealPosition() throws RemoteException {
    return sourceDealPosition;
  }

  @Override
  public void setSourceDealPosition(DealPosition sourceDealPosition) throws RemoteException {
    this.sourceDealPosition = sourceDealPosition;
  }

  @Override
  public BigDecimal getAmount() throws RemoteException {
    return amount;
  }

  @Override
  public Store getStore() throws RemoteException {
    return store;
  }

  @Override
  public void setStore(Store store) throws RemoteException {
    this.store = store;
  }

  @Override
  public void setAmount(BigDecimal amount) throws RemoteException {
    this.amount = amount;
  }

  @Override
  public String getPartition() throws RemoteException {
    return partition;
  }

  @Override
  public void setPartition(String partition) throws RemoteException {
    this.partition = partition;
  }

  @Override
  public boolean validateIdentityValue(String identityValue) throws RemoteException {
    return true;
  }

  @Override
  public List<DealPosition> getDealPositions() throws RemoteException {
    return dealPositions;
  }

  @Override
  public void setDealPositions(List<DealPosition> dealPositions) throws RemoteException {
    this.dealPositions = dealPositions;
  }

  @Override
  public List<EquipmentFactorValue> getGroupFactorValues() throws RemoteException {
    return groupFactorValues;
  }

  @Override
  public void setGroupFactorValues(List<EquipmentFactorValue> groupFactorValues) throws RemoteException {
    this.groupFactorValues = groupFactorValues;
  }
  
  /*@Override
  public CompanyPartition getCompanyPartition() throws RemoteException {
    return this.companyPartition;
  }

  @Override
  public void setCompanyPartition(CompanyPartition companyPartition) throws RemoteException {
    this.companyPartition = companyPartition;
  }*/
  
  @Override
  public Group getGroup() throws RemoteException {
    return this.group;
  }
  
  @Override
  public void setGroup(Group group) throws RemoteException {
    this.group = group;
  }
}