package bum.realizations;

import bum.annotations.*;
import bum.interfaces.CreatedDocument;
import bum.interfaces.Deal;
import bum.interfaces.DealPosition;
import bum.interfaces.Equipment;
import bum.interfaces.Product;
import bum.interfaces.Request;
import bum.interfaces.Store;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import mapping.MappingObjectImpl;
import util.DBRelation;

@Table(
  queryColumns={
    @QueryColumn(
      name="status",
      desctiption="Статус позиции",
      query="SELECT getDealPositionStatus([DealPosition(id)])"),
    @QueryColumn(
      name="service_parent_id",
      query="SELECT [Service(parent)] FROM [Service] WHERE [Service(id)]=(SELECT [Product(service)] "
          + "FROM [Product] WHERE [Product(id)]=[DealPosition(product)])"),
    @QueryColumn(
      name="service_parent_name",
      desctiption="Наименование родительского процесса",
      query="SELECT name FROM [Service] WHERE [Service(id)]=(SELECT [Service(parent)] FROM [Service] "
          + "WHERE [Service(id)]=(SELECT [Product(service)] FROM [Product] WHERE [Product(id)]=[DealPosition(product)]))"),
    @QueryColumn(
      name="service_name",
      desctiption="Наименование процесса",
      query="SELECT name FROM [Service] WHERE [Service(id)]=(SELECT [Product(service)] "
          + "FROM [Product] WHERE [Product(id)]=[DealPosition(product)])"),
    @QueryColumn(
      name="group_name",
      desctiption="Наименование группы",
      query="SELECT name FROM [Group] WHERE [Group(id)]=(SELECT [Product(group)] "
          + "FROM [Product] WHERE [Product(id)]=[DealPosition(product)])"),
    @QueryColumn(
      name="group-unit",
      desctiption="Наименование группы",
      query="SELECT [Group(unit-name)] FROM [Group] WHERE [Group(id)]=(SELECT [Product(group)] "
          + "FROM [Product] WHERE [Product(id)]=[DealPosition(product)])"),
    @QueryColumn(
      name="identity_id",
      query = "SELECT [Factor(id)] FROM [Factor] WHERE [Factor(id)]=(SELECT [Group(identificator)] FROM [Group] WHERE [Group(id)]=(SELECT [Equipment(group)] FROM [Equipment] WHERE [Equipment(id)]=[DealPosition(equipment)]))"),
//      query="SELECT [Factor(id)] FROM [Factor] WHERE [Factor(unique)]=true AND [Factor(id)] IN "
//        + "(SELECT [Group(factors):target] FROM [Group(factors):table] WHERE [Group(factors):object]="
//        + "(SELECT [Equipment(group)] FROM [Equipment] WHERE [Equipment(id)]=[DealPosition(equipment)]))"),
      //query="SELECT getIdentificatorId([DealPosition(equipment)])"),
    @QueryColumn(
      name="identity_name",
      desctiption="Наименование идентификатора",
      query = "SELECT [Factor(name)] FROM [Factor] WHERE [Factor(id)]=(SELECT [Group(identificator)] FROM [Group] WHERE [Group(id)]=(SELECT [Equipment(group)] FROM [Equipment] WHERE [Equipment(id)]=[DealPosition(equipment)]))"),
//      query="SELECT [Factor(name)] FROM [Factor] WHERE [Factor(unique)]=true AND [Factor(id)] IN "
//        + "(SELECT [Group(factors):target] FROM [Group(factors):table] WHERE [Group(factors):object]="
//        + "(SELECT [Equipment(group)] FROM [Equipment] WHERE [Equipment(id)]=[DealPosition(equipment)]))"),
      //query="SELECT getIdentificatorName([DealPosition(equipment)])"),
    @QueryColumn(
      name="identity_value",
      desctiption="Значение идентификатора",
      query = "SELECT [EquipmentFactorValue(name)] FROM [EquipmentFactorValue] WHERE [EquipmentFactorValue(equipment)]=[DealPosition(equipment)] AND [EquipmentFactorValue(factor)]=(SELECT [Factor(id)] FROM [Factor] WHERE [Factor(id)]=(SELECT [Group(identificator)] FROM [Group] WHERE [Group(id)]=(SELECT [Equipment(group)] FROM [Equipment] WHERE [Equipment(id)]=[DealPosition(equipment)])))"),
//      query="SELECT [EquipmentFactorValue(name)] FROM [EquipmentFactorValue] WHERE [EquipmentFactorValue(equipment)]=[DealPosition(equipment)] AND "
//        + "[EquipmentFactorValue(factor)]=(SELECT [Factor(id)] FROM [Factor] WHERE [Factor(unique)]=true AND [Factor(id)] IN "
//        + "(SELECT [Group(factors):target] FROM [Group(factors):table] WHERE [Group(factors):object]="
//        + "(SELECT [Equipment(group)] FROM [Equipment] WHERE [Equipment(id)]=[DealPosition(equipment)])))"),
      //query="SELECT getIdentificatorValue([DealPosition(equipment)])"),
    @QueryColumn(
      name="cost",
      desctiption="Стоимость позиции",
      query="SELECT [DealPosition(customProductCost)] * @[DealPosition(amount)]"),
    @QueryColumn(
      name="hashcode",
      query="SELECT replace(quote_nullable([Equipment(group)])||quote_nullable([Equipment(store)])||(SELECT array_to_string(ARRAY(SELECT [EquipmentFactorValue(factor)]||quote_nullable([EquipmentFactorValue(name)]) FROM [EquipmentFactorValue] WHERE [EquipmentFactorValue(equipment)]=[Equipment(id)] AND tmp=false and type='CURRENT' ORDER BY [EquipmentFactorValue(factor)]),'')), '''', '') "
              + "FROM [Equipment] WHERE [Equipment(id)]=[DealPosition(equipment)]"),
    @QueryColumn(
      name = "factors",
      query = "(SELECT array_agg([Group(factors):target]) FROM [Group(factors):table] WHERE [Group(factors):object]=[DealPosition(group_id)])"
    ),
    @QueryColumn(
      name = "factor_values",
      query = "(SELECT array_agg([EquipmentFactorValue(factor)]||':'||(case when name isnull then '' else name end)) FROM [EquipmentFactorValue] WHERE tmp=false and type='CURRENT' and "
              + "[EquipmentFactorValue(equipment)]=[DealPosition(equipment)] and "
              + "[EquipmentFactorValue(factor)] in (SELECT [Group(factors):target] FROM [Group(factors):table] WHERE [Group(factors):object]=[DealPosition(group_id)]))"
    )
  }
)

@Procedures(
  procedures={
    //ID реквизита идентификатора
    @Procedure(
      name="getIdentificatorId",
      arguments={"integer"},
      returnType="integer",
      procedureText= ""
        + "DECLARE "
        + "  equipId integer := $1;"
        + "BEGIN "
        + "  RETURN (SELECT [Group(identificator)] FROM [Group] WHERE [Group(id)]="
        + "(SELECT [Equipment(group)] FROM [Equipment] WHERE [Equipment(id)]=equipId));"
        + "END"
    ),
    
    //Имя реквизита идентификатора
    @Procedure(
      name="getIdentificatorName",
      arguments={"integer"},
      returnType="text",
      procedureText= ""
        + "DECLARE "
        + "  equipId integer := $1;"
        + "BEGIN "
        + "  RETURN (SELECT [Factor(name)] FROM [Factor] WHERE [Factor(id)]=(SELECT [Group(identificator)] FROM [Group] WHERE [Group(id)]="
        + "(SELECT [Equipment(group)] FROM [Equipment] WHERE [Equipment(id)]=equipId)));"
        + "END"
    ),
    
    //Значение идентификатора
    @Procedure(
      name="getIdentificatorValue",
      arguments={"integer"},
      returnType="text",
      procedureText= ""
        + "DECLARE "
        + "  equipId integer := $1;"
        + "  iden    integer := getIdentificatorId(equipId);"
        + "BEGIN "
        + "  IF iden ISNULL THEN "
        + "    RETURN NULL;"
        + "  ELSE "
        + "    RETURN (SELECT [EquipmentFactorValue(name)] FROM [EquipmentFactorValue] "
        + "      WHERE [EquipmentFactorValue(factor)]=iden AND [EquipmentFactorValue(equipment)]=equipId);"
        + "  END IF;"
        + "END"
    ),
    
    //ID значения идентификатора
    @Procedure(
      name="getIdentificatorValueId",
      arguments={"integer"},
      returnType="text",
      procedureText= ""
        + "DECLARE "
        + "  equipId integer := $1;"
        + "  iden    integer := getIdentificatorId(equipId);"
        + "BEGIN "
        + "  IF iden ISNULL THEN "
        + "    RETURN NULL;"
        + "  ELSE "
        + "    RETURN (SELECT [EquipmentFactorValue(id)] FROM [EquipmentFactorValue] "
        + "      WHERE [EquipmentFactorValue(factor)]=iden AND [EquipmentFactorValue(equipment)]=equipId);"
        + "  END IF;"
        + "END"
    ),    
    
    //Значение реквизита
    @Procedure(
      name="getFactorValue",
      arguments={"integer","text"},
      returnType="text",
      procedureText= ""
        + "DECLARE "
        + "BEGIN "
        + "  RETURN (SELECT [EquipmentFactorValue(name)] FROM [EquipmentFactorValue] WHERE"
        + "  [EquipmentFactorValue(equipment)]=$1 AND [EquipmentFactorValue(factor)]="
        + "  (SELECT [Factor(id)] FROM [Factor] WHERE [Factor(name)]=$2));"
        + "END"
    ),
    
    //Стоимость позиций
    @Procedure(
      name="getDealPositionsCost",
      arguments={"integer[]"},
      returnType="NUMERIC",
      procedureText= ""
        + "DECLARE "
        + "BEGIN "
        + "  RETURN (SELECT SUM([DealPosition(customProductCost)] * @[DealPosition(amount)]) "
        + "    FROM [DealPosition] WHERE id = ANY($1));"
        + "END"
    ),
    
    //Статус позиции
    @Procedure(
      name="getDealPositionStatus",
      arguments={"integer"},
      procedureText=
        "DECLARE "
        + "  dealPositionData record;"
        + "BEGIN"
        + "  SELECT [DealPosition(startId)],[DealPosition(dispatchId)],"
        + "    getPaymentAmountPercent([DealPosition(deal)]) AS paymentPercent INTO dealPositionData FROM [DealPosition] WHERE [DealPosition(id)]=$1;"
        + "  IF dealPositionData.startId NOTNULL AND dealPositionData.dispatchId NOTNULL AND dealPositionData.paymentPercent = 100 THEN"
        + "    RETURN 'Финиш';"
        + "  ELSE"
        + "    IF dealPositionData.startId NOTNULL AND dealPositionData.dispatchId NOTNULL AND dealPositionData.paymentPercent = 0 THEN"
        + "      RETURN 'Принято';"
        + "    ELSE"
        + "      IF dealPositionData.startId NOTNULL AND dealPositionData.dispatchId ISNULL AND dealPositionData.paymentPercent = 100 THEN"
        + "        RETURN 'Расчёт';"
        + "      ELSE"
        + "        IF dealPositionData.startId NOTNULL AND dealPositionData.dispatchId ISNULL AND dealPositionData.paymentPercent = 0 THEN"
        + "          RETURN 'Старт';"
        + "        ELSE"
        + "          IF dealPositionData.startId ISNULL AND dealPositionData.dispatchId ISNULL AND dealPositionData.paymentPercent = 0 THEN"
        + "            RETURN 'Проект';"
        + "          ELSE RETURN 'Неопределённость';"
        + "          END IF;"
        + "        END IF;"
        + "      END IF;"
        + "    END IF;"
        + "  END IF;"
        + "END;"
      )
  }
)

@Triggers(triggers={
  @Trigger(timeType=Trigger.TIMETYPE.AFTER,
  actionTypes={Trigger.ACTIONTYPE.UPDATE},
  procedureText=
    "DECLARE "
    + "BEGIN"
    + "  IF NEW.tmp != OLD.tmp THEN"
    + "    UPDATE [CreatedDocument] SET tmp=NEW.tmp WHERE id IN "
    + "      (SELECT [CreatedDocument(dealPositions):object] FROM [CreatedDocument(dealPositions):table] WHERE [CreatedDocument(dealPositions):target]=OLD.id);"
    + "  END IF;"
    + "  RETURN NEW;"
    + "END;"),
  @Trigger(timeType=Trigger.TIMETYPE.BEFORE,
        actionTypes={Trigger.ACTIONTYPE.INSERT},
        procedureText=
                "DECLARE\n"
                        + "productCost NUMERIC;\n"
                        + "movable     boolean;\n"
                        + "BEGIN\n"
                        
                        + "  IF NEW.[!DealPosition(equipment)] IS NOT NULL THEN\n"
                        + "    SELECT [Service(moveStorePosition)] INTO movable FROM [Service] WHERE [Service(id)]=(SELECT [Deal(service)] FROM [Deal] WHERE [Deal(id)]=NEW.[!DealPosition(deal)]);\n"
                        
                        + "    IF NEW.[!DealPosition(sourceStore)] IS NULL THEN\n"
                        + "      SELECT [Equipment(store)] INTO NEW.[!DealPosition(sourceStore)] FROM [Equipment] WHERE [Equipment(id)]=NEW.[!DealPosition(equipment)];\n"
                        +"     END IF;\n"
                        
                        + "    SELECT [Equipment(sourceDealPosition)] INTO NEW.[!DealPosition(equipmentSourceDealPosition)] FROM [Equipment] WHERE [Equipment(id)]=NEW.[!DealPosition(equipment)];\n"
                        
                        + "    IF movable=true AND NEW.[!DealPosition(targetStore)] IS NULL THEN\n"
                        + "      SELECT [Store(id)] INTO NEW.[!DealPosition(targetStore)]\n"
                        + "      FROM [Store] \n"
                        + "      WHERE tmp=false AND type='CURRENT' AND\n"
                        + "      [Store(companyPartition)]=(SELECT [Deal(customerCompanyPartition)] FROM [Deal] WHERE [Deal(id)]=NEW.[!DealPosition(deal)]) AND\n"
                        + "      [Store(objectType)]=(SELECT [Store(objectType)] FROM [Store] WHERE [Store(id)]=NEW.[!DealPosition(sourceStore)]) AND\n"
                        + "      [Store(storeType)]=(SELECT [Store(storeType)] FROM [Store] WHERE [Store(id)]=NEW.[!DealPosition(sourceStore)]) LIMIT 1;\n"
                        + "    END IF;\n"
                        
                        + "  END IF;\n"
                        
                        + "  IF NEW.[!DealPosition(customProductCost)] IS NULL THEN\n"
                        + "    SELECT [Product(cost)] INTO NEW.[!DealPosition(customProductCost)] FROM [Product] WHERE [Product(id)]=NEW.[!DealPosition(product)];\n"
                        + "  END IF;"
                        + "  RETURN NEW;\n"
                        + "END;\n")
})

public class DealPositionImpl extends MappingObjectImpl implements DealPosition {
  @ManyToOne(viewFields={"id", "amount", "store", "zakaz"}, 
          viewNames={"equipment_id", "equipment_amount", "equipment-store", "zakaz"}, 
          on_delete = DBRelation.ActionType.SET_NULL)
  private Equipment equipment;
  
  @ManyToOne(on_delete = DBRelation.ActionType.SET_NULL, 
          viewFields = {"dispatchId",  "dispatchDate",  "customProductCost"}, 
          viewNames  = {"dispatch-id", "dispatch-date", "custom-product-cost"})
  private DealPosition equipmentSourceDealPosition;
  
  @Column(defaultValue = "0")
  private BigDecimal amount = new BigDecimal(0.0);
  
  @ManyToOne(on_delete=DBRelation.ActionType.CASCADE,
          viewFields={"id","customerCompany","sellerCompany","customerCompanyPartition","sellerCompanyPartition",
            "sellerCfc","customerCfc","dealStartDate","dealEndDate","service","contract","tempProcess"},
          viewNames={"deal_id","customer_id","seller_id","customer_partition_id","seller_partition_id",
            "seller_cfc_id","customer_cfc_id","deal_start_date","deal_end_date","process_id","contract_id","tempProcess_id"})
  private Deal deal;
  
  @ManyToOne(viewFields={"id","cost","techPassport","group","service","nds","duration","recurrence"}, 
          viewNames={"product_id", "product_cost","product_techPassport","group_id","process","product_nds","duration","recurrence"})
  private Product product;

  @Column()
  private BigDecimal customProductCost;

  @Column
  private Timestamp dispatchDate;
  
  // Это дата интегральная дата отгрузки - она равна максимальной дате отгрузочного документа в случае если дата документа 
  // равна сегодня или позже, а в обратном случае она равна сегодня (сегодня - это дата отгрузки сделки оператором)
  @Column
  private Timestamp actionDispatchDate;
  
  @Column
  private Long dispatchId;
  
  @Column
  private Timestamp startDate;
  
  @Column
  private Long startId;
  
  @ManyToOne(viewFields={"name","storeType","objectType","controllOut","controllIn"}, 
          viewNames={"source-store-name", "source-store-type","source-store-object-type","source-store-controll-out","source-store-controll-in"})
  private Store sourceStore;
    
  @ManyToOne(viewFields={"name","storeType","objectType","controllOut","controllIn"}, 
          viewNames={"target-store-name", "target-store-type","target-store-object-type","target-store-controll-out","target-store-controll-in"})
  private Store targetStore;
  
  @ManyToMany(mappedBy = "dealPositions")
  private List<CreatedDocument> documents = new ArrayList<>();
  
  @ManyToOne(viewFields = {"name","number"}, viewNames = {"request-name", "request-number"})
  private Request request;
  
  public DealPositionImpl() throws RemoteException {
    super();
  }
  
  @Override
  public Request getRequest() {
    return request;
  }

  @Override
  public void setRequest(Request request) {
    this.request = request;
  }

  @Override
  public Store getTargetStore() throws RemoteException {
    return targetStore;
  }

  @Override
  public void setTargetStore(Store targetStore) throws RemoteException {
    this.targetStore = targetStore;
  }

  @Override
  public Timestamp getActionDispatchDate() throws RemoteException {
    return actionDispatchDate;
  }

  @Override
  public void setActionDispatchDate(Timestamp actionDispatchDate) throws RemoteException {
    this.actionDispatchDate = actionDispatchDate;
  }

  @Override
  public BigDecimal getAmount() throws RemoteException {
    return amount;
  }

  @Override
  public void setAmount(BigDecimal amount) throws RemoteException {
    this.amount = amount;
  }

  @Override
  public Store getSourceStore() throws RemoteException {
    return sourceStore;
  }

  @Override
  public void setSourceStore(Store sourceStore) throws RemoteException {
    this.sourceStore = sourceStore;
  }
  
  @Override
  public BigDecimal getCustomProductCost() throws RemoteException {
    return customProductCost;
  }

  @Override
  public void setCustomProductCost(BigDecimal customProductCost) throws RemoteException {
    this.customProductCost = customProductCost;
  }

  @Override
  public Equipment getEquipment() throws RemoteException {
    return this.equipment;
  }

  @Override
  public void setEquipment(Equipment equipment) throws RemoteException {
    this.equipment = (Equipment)equipment;
  }

  @Override
  public DealPosition getEquipmentSourceDealPosition() throws RemoteException {
    return equipmentSourceDealPosition;
  }

  @Override
  public void setEquipmentSourceDealPosition(DealPosition equipmentSourceDealPosition) throws RemoteException {
    this.equipmentSourceDealPosition = equipmentSourceDealPosition;
  }
  
  @Override
  public void setProduct(Product product) throws RemoteException {
    this.product = (Product)product;
  }

  @Override
  public Product getProduct() throws RemoteException {
    return this.product;
  }

  @Override
  public Deal getDeal() throws RemoteException {
    return this.deal;
  }

  @Override
  public void setDeal(Deal deal) throws RemoteException {
    this.deal = (Deal)deal;
  }
  
  @Override
  public Timestamp getStartDate() throws RemoteException {
    return startDate;
  }

  @Override
  public void setStartDate(Timestamp startDate) throws RemoteException {
    this.startDate = startDate;
  }

  @Override
  public Long getStartId() throws RemoteException {
    return startId;
  }

  @Override
  public void setStartId(Long startId) throws RemoteException {
    this.startId = startId;
  }

  @Override
  public Long getDispatchId() throws RemoteException {
    return dispatchId;
  }

  @Override
  public void setDispatchId(Long dispatchId) throws RemoteException {
    this.dispatchId = dispatchId;
  }
  
  @Override
  public Timestamp getDispatchDate() throws RemoteException {
    return dispatchDate;
  }

  @Override
  public void setDispatchDate(Timestamp dispatchDate) throws RemoteException {
    this.dispatchDate = dispatchDate;
  }

  @Override
  public List<CreatedDocument> getDocuments() throws RemoteException {
    return documents;
  }

  @Override
  public void setDocuments(List<CreatedDocument> documents) throws RemoteException {
    this.documents = documents;
  }
}