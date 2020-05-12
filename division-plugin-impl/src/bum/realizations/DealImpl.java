package bum.realizations;


import bum.annotations.*;
import bum.interfaces.*;
import java.rmi.RemoteException;
import java.sql.Date;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import mapping.MappingObjectImpl;

@Table(
  clientName="Сделки",
  queryColumns={
    @QueryColumn(
      name="status",
      desctiption="Статус сделки",
      query="SELECT getstatus([bum.interfaces.Deal(id)])"),
    
    @QueryColumn(
      name="startPercent",
      desctiption="Процент старта",
      query="SELECT getStartPercent([bum.interfaces.Deal(id)])"),
    @QueryColumn(
      name="dispatchPercent",
      desctiption="Процент отгруженности",
      query="SELECT getDispatchPercent([bum.interfaces.Deal(id)])"),
    @QueryColumn(
      name="paymentPercent",
      desctiption="Процент оплаченности",
      query="SELECT getPaymentAmountPercent([bum.interfaces.Deal(id)])"),
    
    @QueryColumn(
      name="seller",
      desctiption="Наименование продавца",
      query="SELECT getCompanyPartitionName([Deal(sellerCompanyPartition)])"),
    @QueryColumn(
      name="customer",
      desctiption="Наименование покупателя",
      query="SELECT getCompanyPartitionName([Deal(customerCompanyPartition)])"),
    @QueryColumn(
      name="cost",
      desctiption="Стоимость сделки",
      query="SELECT SUM(NULLTOZERO([DealPosition(customProductCost)])*[DealPosition(amount)]) "
        + "FROM [DealPosition] WHERE [DealPosition(deal)]=[Deal(id)]"),
    @QueryColumn(
      name="costWihoutNds",
      desctiption="Стоимость сделки",
      query="SELECT getDealsCostWihoutNds(ARRAY[[Deal(id)]])"),
    @QueryColumn(
      name="lastDispatchDate",
      desctiption="Дата последней отгрузки",
      query="SELECT MAX([DealPosition(actionDispatchDate)]::date) FROM [DealPosition] WHERE [DealPosition(deal)]=[Deal(id)]"),
    @QueryColumn(
      name="lastPayDate",
      desctiption="Дата последней оплаты",
      query="SELECT MAX([DealPayment(date)]::date) FROM [DealPayment] WHERE [DealPayment(deal)]=[Deal(id)]"),
    @QueryColumn(
      name="finishDate",
      desctiption="Дата, когда сделка стала зелёной",
      query="SELECT MAX(a) FROM unnest(ARRAY[(SELECT MAX([DealPayment(date)]) FROM [DealPayment] WHERE [DealPayment(deal)]=[Deal(id)])::date,"
              + "(SELECT MAX([DealPosition(actionDispatchDate)]) FROM [DealPosition] WHERE [DealPosition(deal)]=[Deal(id)])::date,[Deal(dealEndDate)]::date]) a"),
    @QueryColumn(
      name="positionDate",
      desctiption="Дата полного окончания сделки",
      query="SELECT MAX(a) FROM unnest(ARRAY[[Deal(dealEndDate)], (SELECT MAX([DealPayment(date)]) FROM [DealPayment] WHERE [DealPayment(deal)]=[Deal(id)])::date," 
              + "(SELECT MAX([DealPosition(actionDispatchDate)]) FROM [DealPosition] WHERE [DealPosition(deal)]=[Deal(id)])::date]) a"),
    @QueryColumn(
      name="needPay",
      desctiption="Долг",
      query="SELECT getDealNeedPay([Deal(id)])"),
    @QueryColumn(
      name="dealPositionCount",
      desctiption="Колличество позиций в сделке",
      query="SELECT COUNT(id)::integer FROM [DealPosition] WHERE [DealPosition(deal)]=[Deal(id)] AND tmp=false AND type='CURRENT'"),
    @QueryColumn(
      name="startCount",
      desctiption="Колличество стартовавших позиций в сделке",
      query="SELECT COUNT(id)::integer FROM [DealPosition] WHERE [DealPosition(deal)]=[Deal(id)] AND tmp=false AND type='CURRENT' AND startDate NOTNULL"),
    @QueryColumn(
      name="dispatchCount",
      desctiption="Колличество отгруженных позиций в сделке",
      query="SELECT COUNT(id)::integer FROM [DealPosition] WHERE [DealPosition(deal)]=[Deal(id)] AND tmp=false AND type='CURRENT' AND dispatchDate NOTNULL"),
    @QueryColumn(
      name="dispatchAmount",
      desctiption="Сумма отгруженных позиций в сделке",
      query="SELECT NULLTOZERO(SUM([DealPosition(customProductCost)] * @[DealPosition(amount)]))::NUMERIC FROM [DealPosition] WHERE [DealPosition(deal)]=[Deal(id)] AND tmp=false AND type='CURRENT' AND dispatchDate NOTNULL"),
      //query="SELECT COUNT(id)::integer FROM [DealPosition] WHERE [DealPosition(deal)]=[Deal(id)] AND tmp=false AND type='CURRENT' AND dispatchDate NOTNULL"),
    @QueryColumn(
      name="paymentCount",
      desctiption="Колличество оплат по сделке",
      query="SELECT COUNT(id)::integer FROM [DealPayment]  WHERE [DealPayment(deal)]=[Deal(id)] AND tmp=false AND type='CURRENT'"),
    @QueryColumn(
      name="paymentAmount",
      desctiption="Сумма оплат по сделке",
      query="SELECT NULLTOZERO(SUM([DealPayment(amount)]))::NUMERIC FROM [DealPayment] WHERE [DealPayment(deal)]=[Deal(id)] AND tmp=false AND type='CURRENT'")
  })

@Triggers(triggers = {
  @Trigger(timeType=Trigger.TIMETYPE.AFTER,
  actionTypes={Trigger.ACTIONTYPE.UPDATE},
  procedureText=
    "DECLARE "
    + "BEGIN"
    //+ "  IF NEW.type != OLD.type THEN"
    //+ "    UPDATE [DealPosition] SET type=NEW.type WHERE [DealPosition(deal)]=OLD.id;"
    //+ "  END IF;"
    + "  IF NEW.tmp != OLD.tmp THEN"
    + "    UPDATE [DealPosition]    SET tmp=NEW.tmp WHERE [DealPosition(deal)]=OLD.id;"
    + "  END IF;"
    + "  RETURN NEW;"
    + "END;")
})

//Стоимость сделки
@Procedures(
  procedures={
    @Procedure(
      name="getDealsCost",
      arguments={"integer[]"},
      returnType="NUMERIC",
      procedureText= ""
        + "DECLARE "
        + "BEGIN "
        + "  RETURN (SELECT SUM(NULLTOZERO([DealPosition(customProductCost)]) * @[DealPosition(amount)]) "
        + "    FROM [DealPosition] WHERE [DealPosition(deal)] = ANY($1));"
        + "END"
    ),
    
    @Procedure(
      name="getDealsCostWihoutNds",
      arguments={"integer[]"},
      returnType="NUMERIC",
      procedureText= ""
        + "DECLARE "
        + "BEGIN "
        + "  RETURN (SELECT SUM"
        + "  ("
        + "    ([DealPosition(customProductCost)] * [DealPosition(amount)])-"
        + "    CASE WHEN (SELECT [Deal(seller-nds-payer)] FROM [Deal] WHERE [Deal(id)]=[DealPosition(deal)])=true THEN "
        + "    (([DealPosition(customProductCost)] * [DealPosition(amount)])/([DealPosition(product_nds)]+100)*[DealPosition(product_nds)])"
        + "    ELSE 0 END"
        + "  ) "
        + "    FROM [DealPosition] WHERE tmp=false AND [DealPosition(deal)] = ANY($1));"
        + "END"
    ),
    
    @Procedure(
      name="zeroToNull",
      returnType="NUMERIC",
      arguments={"NUMERIC"},
      procedureText=
        "BEGIN"+
        "  IF($1 = 0) THEN	RETURN NULL;"+
        "  ELSE RETURN $1;"+
        "  END IF;"+
        "END;"
    ),

    @Procedure(
      name="nullToZero",
      returnType="NUMERIC",
      arguments={"NUMERIC"},
      procedureText=
        "BEGIN"+
        "  IF($1 ISNULL) THEN	RETURN 0;"+
        "  ELSE RETURN $1;"+
        "  END IF;"+
        "END;"
    ),
    
    //Долг по сделке
    @Procedure(
      name="getDealNeedPay",
      arguments={"integer"},
      returnType="NUMERIC",
      procedureText=
        "DECLARE "+
        "BEGIN"+
        "  RETURN NULLTOZERO(getDealsCost(ARRAY[$1]) - NULLTOZERO(getPaymentAmount($1)));"+
        "END"
    ),
    
    //Количество оплат по сделке
    @Procedure(
      name="getPaymentCount",
      arguments={"integer"},
      returnType="integer",
      procedureText=
        "DECLARE "+
        "BEGIN"+
        "  RETURN COUNT([DealPayment(id)]) FROM [DealPayment] WHERE [DealPayment(tmp)] = false AND [DealPayment(deal)] = $1;"+
        "END"
    ),
    
    //Количество оплат по сделке с определённого платежа
    @Procedure(
      name="getPaymentCountFromPayment",
      arguments={"integer","integer"},
      returnType="integer",
      procedureText=
        "DECLARE "+
        "BEGIN"+
        "  RETURN COUNT([DealPayment(id)]) FROM [DealPayment] WHERE [DealPayment(tmp)] = false AND [DealPayment(deal)] = $1 AND [DealPayment(payment)] = $2;"+
        "END"
    ),
    
    //Сумма оплат по сделке
    @Procedure(
      name="getPaymentAmount",
      arguments={"integer"},
      returnType="NUMERIC",
      procedureText=
        "DECLARE\n"
        +"BEGIN\n"
        + "IF $1 ISNULL THEN\n"
        + "  RETURN NULL;\n"
        + "ELSE\n"
        + "  RETURN NULLTOZERO((SELECT SUM([DealPayment(amount)]) FROM [DealPayment] WHERE [DealPayment(tmp)] = false AND [DealPayment(type)]='CURRENT' AND [DealPayment(deal)] = $1));\n"
        + "END IF;\n"
        +"END"
    ),
    
    //Сумма оплат по сделке с определённого платежа
    @Procedure(
      name="getPaymentAmountFromPayment",
      arguments={"integer","integer"},
      returnType="NUMERIC",
      procedureText=
        "DECLARE\n"
        +"BEGIN\n"
        + "IF $1 ISNULL OR $2 ISNULL THEN\n"
        + "  RETURN NULL;\n"
        + "ELSE\n"
        + "  RETURN NULLTOZERO((SELECT SUM([DealPayment(amount)]) FROM [DealPayment] WHERE [DealPayment(tmp)] = false AND [DealPayment(deal)] = $1 AND [DealPayment(payment)] = $2));\n"
        + "END IF;\n"
        +"END"
    ),
    
    //на сколько процентов сделка оплачена
    @Procedure(
      name="getPaymentAmountPercent",
      arguments={"integer"},
      returnType="NUMERIC",
      procedureText=
        "DECLARE "+
        "  cost          NUMERIC   := getDealsCost(ARRAY[$1]);"+
        "  paymentAmount NUMERIC   := NULLTOZERO(getPaymentAmount($1));"+
        "BEGIN"+
        "  IF cost > 0 AND paymentAmount > 0 THEN"+
        "    RETURN paymentAmount / cost * 100;"+
        "  END IF;"+
        "  IF cost = 0 AND paymentAmount = 0 THEN"+
        "    RETURN 100;"+
        "  END IF;"+
        "  IF cost > 0 AND paymentAmount = 0 THEN"+
        "    RETURN 0;"+
        "  END IF;"+
        "  IF cost = 0 AND paymentAmount > 0 THEN"+
        "    RETURN 100;"+
        "  END IF;"+
        "END"
    ),
    
    //Процент оплаты сделки в виде строки
    @Procedure(
      name="getPaymentAmountPercentString",
      arguments={"integer"},
      procedureText=
        "DECLARE "+
        "BEGIN"+
        "  RETURN getPaymentAmountPercent($1) || '%';"+
        "END"
    ),
    
    //Количество позиций в сделке
    @Procedure(
      name="getDealPositionCount",
      arguments={"integer"},
      returnType="NUMERIC",
      procedureText=
        "DECLARE "+
        "BEGIN"+
        "  RETURN (SELECT COUNT([DealPosition(id)]) FROM [DealPosition] WHERE tmp=false AND [DealPosition(deal)] = $1);"+
        "END"
    ),
    
    //Количество стартонувших позиций
    @Procedure(
      name="getStartCount",
      arguments={"integer"},
      returnType="NUMERIC",
      procedureText=
        "DECLARE "+
        "BEGIN"+
        "  RETURN (SELECT COUNT([DealPosition(id)]) FROM [DealPosition] WHERE tmp=false AND [DealPosition(deal)] = $1 AND [DealPosition(startDate)] IS NOT NULL);"+
        "END"
    ),
    
    //На сколько процентов стартонула сделка
    @Procedure(
      name="getStartPercent",
      arguments={"integer"},
      returnType="NUMERIC",
      procedureText=
        "DECLARE "+
        "  dealPositionCount integer := getDealPositionCount($1);"+
        "BEGIN"+
        "  IF dealPositionCount > 0 THEN"+
        "    RETURN getStartCount($1) / dealPositionCount * 100;"+
        "  ELSE"+
        "    RETURN 0;"+
        "  END IF;"+
        "END"
    ),
    
    //Количество отгруженных позиций
    @Procedure(
      name="getDispatchCount",
      arguments={"integer"},
      returnType="NUMERIC",
      procedureText=
              "DECLARE "
            + "BEGIN"
            + " RETURN (SELECT COUNT([DealPosition(id)]) FROM [DealPosition] WHERE tmp=false AND [DealPosition(deal)] = $1 AND [DealPosition(dispatchDate)] IS NOT NULL);"
            + "END"
    ),
    
    //На сколько процентов отгружена сделка
    @Procedure(
      name="getDispatchPercent",
      arguments={"integer"},
      returnType="NUMERIC",
      procedureText=
        "DECLARE "+
        "  dealPositionCount integer := getDealPositionCount($1);"+
        "BEGIN"+
        "  IF dealPositionCount > 0 THEN"+
        "    RETURN getDispatchCount($1) / dealPositionCount * 100;"+
        "  ELSE"+
        "    RETURN 100;"+
        "  END IF;"+
        "END"
    ),
    
    //Процент отгруженности в виде строки
    @Procedure(
      name="getDispatchPercentString",
      arguments={"integer"},
      procedureText=
        "DECLARE "+
        "BEGIN"+
        "  RETURN getDispatchPercent($1) || '%';"+
        "END"
    ),
    
    //Статус сделки
    @Procedure(
      name="getstatus",
      arguments={"integer"},
      procedureText=
      "DECLARE "+
      "  dealData record;"+
      "  startPercent NUMERIC    := getStartPercent($1);"+
      "  dispatchPercent NUMERIC := getDispatchPercent($1);"+
      "  paymentPercent NUMERIC  := getPaymentAmountPercent($1);"+
      "  ac record;"+
      "BEGIN"+
      "  IF startPercent = 100 AND dispatchPercent = 100 AND paymentPercent = 100 THEN"+
      "    RETURN 'Финиш';"+
      "  ELSE"+
      "    IF startPercent = 100 AND dispatchPercent > 0 AND paymentPercent >= 0 THEN"+
      "      RETURN 'Принято';"+
      "    ELSE"+
      "      IF startPercent = 100 AND dispatchPercent = 0 AND paymentPercent = 100 THEN"+
      "        RETURN 'Расчёт';"+
      "      ELSE"+
      "        IF startPercent = 100 AND dispatchPercent = 0 AND paymentPercent = 0 THEN"+
      "          RETURN 'Старт';"+
      "        ELSE"+
      "          IF startPercent = 0 AND dispatchPercent = 0 AND paymentPercent = 0 THEN"+
      "            RETURN 'Проект';"+
      "          ELSE RETURN 'Неопределённость';"+
      "          END IF;"+
      "        END IF;"+
      "      END IF;"+
      "    END IF;"+
      "  END IF;"+
      "END;"
    )
  }
)

public class DealImpl extends MappingObjectImpl implements Deal {
  @Column(defaultValue="CURRENT_TIMESTAMP")
  private Date dealStartDate = new Date(System.currentTimeMillis());
  
  @Column(defaultValue="CURRENT_TIMESTAMP")
  private Date dealEndDate = new Date(System.currentTimeMillis());
  
  @ManyToOne(viewFields={"id","number","name","startDate","endDate","date"},
          viewNames={"contract_id","contract_number","contract_name","contract_startdate","contract_enddate","contract_date"})
  private Contract contract;
  
  @ManyToOne(viewFields={"id","name","ndsPayer"},viewNames={"customer_id","customer_company_name","customer-nds-ayer"})
  private Company customerCompany;
  
  @ManyToOne(viewFields={"id","name","ndsPayer"},viewNames={"seller_id","seller_company_name","seller-nds-payer"})
  private Company sellerCompany;
  
  @ManyToOne(viewFields={"name"},viewNames={"customer_cfc_name"})
  private CFC customerCfc;
  
  @ManyToOne(viewFields={"name"},viewNames={"seller_cfc_name"})
  private CFC sellerCfc;
  
  @OneToMany(mappedBy="deal")
  private List<DealPosition> dealPositions = new ArrayList<>();
  
  @ManyToOne(viewFields={"id","name","owner","moveStorePosition"},viewNames={"service_id","service_name","object-owner","object-move"})
  private Service service;
  
  @ManyToOne(viewFields={"process","customerPartition","contract"},viewNames={"process_id","tempprocess_customer_partition", "contract_id"})
  private ContractProcess tempProcess;
  
  @ManyToOne(viewFields={"id","name"},viewNames={"sellerpartition_id","sellerpartitionname"})
  private CompanyPartition sellerCompanyPartition;
  
  @ManyToOne(viewFields={"id","name"},viewNames={"customerpartition_id","customerpartitionname"})
  private CompanyPartition customerCompanyPartition;
  
  @OneToMany(mappedBy="deal")
  private List<DealPayment> dealPayments = new ArrayList<>();
  
  @Column
  private Period duration;
  
  @Column
  private Period recurrence;
  
  public DealImpl() throws RemoteException {
    super();
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
  public ContractProcess getTempProcess() throws RemoteException {
    return tempProcess;
  }

  @Override
  public void setTempProcess(ContractProcess tempProcess) throws RemoteException {
    this.tempProcess = tempProcess;
  }

  @Override
  public Service getService() throws RemoteException {
    return service;
  }

  @Override
  public void setService(Service service) throws RemoteException {
    this.service = service;
  }

  @Override
  public List<DealPayment> getDealPayments() throws RemoteException {
    return dealPayments;
  }

  @Override
  public void setDealPayments(List<DealPayment> dealPayments) throws RemoteException {
    this.dealPayments = dealPayments;
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
  public Date getDealEndDate() throws RemoteException {
    return dealEndDate;
  }

  @Override
  public void setDealEndDate(Date dealEndDate) throws RemoteException {
    this.dealEndDate = dealEndDate;
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
  public Date getDealStartDate() throws RemoteException {
    return dealStartDate;
  }

  @Override
  public void setDealStartDate(Date dealStartDate) throws RemoteException {
    this.dealStartDate = dealStartDate;
  }
  
  @Override
  public CFC getSellerCfc() throws RemoteException {
    return sellerCfc;
  }

  @Override
  public void setSellerCfc(CFC sellerCfc) throws RemoteException {
    this.sellerCfc = sellerCfc;
  }

  @Override
  public CFC getCustomerCfc() throws RemoteException {
    return customerCfc;
  }

  @Override
  public void setCustomerCfc(CFC customerCfc) throws RemoteException {
    this.customerCfc = customerCfc;
  }
  
  @Override
  public Company getSellerCompany() throws RemoteException {
    return this.sellerCompany;
  }
  
  @Override
  public void setSellerCompany(Company sellerCompany) throws RemoteException {
    this.sellerCompany = sellerCompany;
  }
  
  @Override
  public Company getCustomerCompany() throws RemoteException {
    return this.customerCompany;
  }
  
  @Override
  public void setCustomerCompany(Company customerCompany) throws RemoteException {
    this.customerCompany = customerCompany;
  }
  
  @Override
  public Contract getContract() throws RemoteException {
    return this.contract;
  }

  @Override
  public void setContract(Contract contract) throws RemoteException {
    this.contract = (Contract)contract;
  }

  /*@Override
  public Deal copyDeal(RemoteSession session, Date start, Date end) throws Exception {
    Deal deal = null;

    Integer dealId = null;
    Integer protoId = getId();

    //Формируем запрос на создание сделки
    Map<String,String> fc = session.getFiledColumns(Deal.class);
    String columnList = "";
    for(String fn:fc.keySet())
      if(!fn.equals("id") && !fn.equals("prototype") 
              && !fn.equals("dealStartDate") && !fn.equals("dealEndDate") && !fn.equals("date"))
        columnList += ","+fc.get(fn);
    columnList = columnList.substring(1);

    String[] querys = new String[0];
    querys = (String[]) ArrayUtils.add(querys, "INSERT INTO [Deal]("+columnList+") "
            + "SELECT "+columnList+" FROM [Deal] WHERE id="+protoId);
    querys = (String[]) ArrayUtils.add(querys, "UPDATE [Deal] SET "
            + fc.get("dealStartDate")+"=?,"
            + fc.get("dealEndDate")+"=?,"
            + fc.get("prototype")+"=false,"
            + fc.get("tmp")+"=false,"
            + fc.get("type")+"=? WHERE id=(SELECT MAX(id) FROM [Deal])");

    Object[][] params = new Object[][]{{},{
      new java.sql.Date(start.getTime()),
      new java.sql.Date(end.getTime()),
      MappingObject.Type.CURRENT.toString()}};

    //Формируем запрос на копирование позиций прототипа
    fc = session.getFiledColumns(DealPosition.class);
    columnList = "";
    for(String fn:fc.keySet())
      if(!fn.equals("id") && !fn.equals("date"))
        columnList += ","+fc.get(fn);
    columnList = columnList.substring(1);

    //создаем сделку и копируем поля прототипа
    session.executeUpdate(querys, params);
    Vector<Vector> data = session.executeQuery("(SELECT MAX(id) FROM [Deal])");
    if(!data.isEmpty())
      dealId = (Integer) data.get(0).get(0);

    querys = new String[0];
    //копируем позиции сделки из прототипа
    for(Vector<Vector> d:session.executeQuery("SELECT id FROM [DealPosition] WHERE [DealPosition(deal)]="+protoId)) {
      
      querys = (String[]) ArrayUtils.add(querys, "INSERT INTO [DealPosition]("+columnList+") "
              + "SELECT "+columnList+" FROM [DealPosition] WHERE id="+d.get(0));
      
      querys = (String[]) ArrayUtils.add(querys,"UPDATE [DealPosition]"
              + " SET [DealPosition(deal)]="+dealId+" WHERE id=(SELECT MAX(id) FROM [DealPosition])");
    }
    session.executeUpdate(querys);
    deal = (Deal)session.getObject(Deal.class, dealId);
    return deal;
  }

  @Override
  public Integer[] createDealPositions(Integer[] dealIds, Integer[] equipmentIds, RemoteSession session) throws Exception {
    Integer[] dealPos = new Integer[0];
    Vector<Vector> data = session.executeQuery("SELECT MAX(id) FROM [DealPosition]");
    Integer startId = (Integer) (data.isEmpty()?0:data.get(0).get(0));

    for(Integer dealId:dealIds) {
      //Получить все продукты в соответсвии с процессом сделки и группами экземпляров
      data = session.executeQuery("SELECT "
              + "id,"
              + "(SELECT [Product(id)] FROM [Product] WHERE [Product(group)]=[Equipment(group)] AND "
              + "[Product(service)]=(SELECT [Deal(service)] FROM [Deal] WHERE [Deal(id)]=?) AND "
              + "[Product(priceList)]=(SELECT [PriceList(id)] FROM [PriceList] WHERE tmp=false AND type='CURRENT' AND "
              + "[PriceList(company)]=(SELECT [Deal(sellerCompany)] FROM [Deal] WHERE id=?)))"
              + " FROM [Equipment] "
              + "WHERE id=ANY(?) AND id NOT IN "
              + "(SELECT [DealPosition(equipment)] FROM [DealPosition] WHERE [DealPosition(deal)]=? AND [DealPosition(equipment)]=ANY(?))", 
              new Object[]{dealId,dealId,equipmentIds,dealId,equipmentIds});

      for(Vector d:data) {
        if(d.get(1) != null) {
          session.executeUpdate("INSERT INTO [DealPosition]([DealPosition(deal)],[DealPosition(equipment)],[DealPosition(product)]) "
                  + "VALUES("+dealId+","+d.get(0)+","+d.get(1)+")");
        }
      }
    }

    data = session.executeQuery("SELECT MAX(id) FROM [DealPosition]");
    Integer endId = (Integer) (data.isEmpty()?0:data.get(0).get(0));

    for(int i=startId+1;i<=endId;i++)
      dealPos = (Integer[]) ArrayUtils.add(dealPos, i);

    if(dealPos.length > 0) {
      session.addEvent(DealPosition.class, "CREATE", dealPos);
      session.addEvent(Deal.class, "UPDATE", dealIds);
    }
    return dealPos;
  }*/
}