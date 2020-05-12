package division.util;

import bum.editors.util.ObjectLoader;
import bum.interfaces.CompanyPartition;
import bum.interfaces.CreatedDocument;
import bum.interfaces.Deal;
import bum.interfaces.DealPosition;
import bum.interfaces.Group.ObjectType;
import bum.interfaces.ProductDocument;
import bum.interfaces.ProductDocument.ActionType;
import bum.interfaces.Service.Owner;
import bum.interfaces.Store.StoreType;
import division.fx.PropertyMap;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.lang3.ArrayUtils;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class DocumentCreator {
  // В случае СТАРТА или ОТГРУЗКИ
  private Map<Object,Map<Object,Map<Object,Map<Object,Map<Object,List<Integer>>>>>> document_seller_customer_contract_deal_dps = new HashMap<>();
  // В случае ОПЛАТЫ
  private Map<Integer,List<Integer>>  document_dps = new HashMap<>();
  
  private ActionType actionType;
  
  private Map<Integer,PropertyMap> sellerSystemDocuments = new HashMap();
  private Map<Integer,PropertyMap> customerSystemDocuments = new HashMap();
  private Map<Integer, PropertyMap> userDocuments = new HashMap();
  
  private Map<Integer,String> companyNames = new HashMap<>();
  
  //Текст sql-запроса на получение системных документов
  private String documentSystemQuery = "SELECT "
          + "[Document(id)], "
          + "[Document(name)], "
          + "[Document(script)], "
          + "[Document(scriptLanguage)], "
          + "(SELECT COUNT(id)>0 FROM [CompanyPartitionDocument] WHERE tmp=false AND type='CURRENT' AND [CompanyPartitionDocument(document)]=[Document(id)] AND "
          + "[CompanyPartitionDocument(partition)]=? AND "
          + "(SELECT [CompanyPartitionDocument(startNumber)] NOTNULL AND [CompanyPartitionDocument(startNumber)] > 0 "
          + "FROM [CompanyPartitionDocument] "
          + "WHERE tmp=false AND type='CURRENT' AND [CompanyPartitionDocument(document)]=[Document(id)] AND "
          + "[CompanyPartitionDocument(partition)]=?)) "
          
          + "FROM [Document] "
          + "WHERE "
          
          + "[Document(tmp)]=false AND "
          + "[Document(type)]='CURRENT' AND "
          + "[Document(system)]=true AND "
          + "[Document(documentSource)]=? AND "
          + "([Document(ndsPayer)]=? OR [Document(ndsPayer)] IS NULL) AND "
          + "[Document(actionType)]=? AND ("
          
          + "[Document(movable)] IS NULL OR "
          
          + "[Document(movable)]=? AND "
          + "([Document(moneyCash)]=? AND "
          + "[Document(moneyCashLess)]=? AND "
          + "[Document(tmcCash)]=? AND "
          + "[Document(tmcCashLess)]=?)) AND "
          
          + "[Document(id)] NOT IN (SELECT [ProductDocument(document)] FROM [ProductDocument] WHERE [ProductDocument(product)]=ANY(?))";

  public Map<Integer, PropertyMap> getSellerSystemDocuments() {
    return sellerSystemDocuments;
  }

  public Map<Integer, PropertyMap> getCustomerSystemDocuments() {
    return customerSystemDocuments;
  }

  public Map<Integer, PropertyMap> getUserDocuments() {
    return userDocuments;
  }
  
  public void init(ProductDocument.ActionType actionType, Integer[] dealPositions) {
    init(actionType, dealPositions, null, null);
  }
  
  /**
   * Формирует список документов, которые необходимо создать при наступлении события actionType
   * @param actionType - событие которое генерирует создание документов
   * @param dealPositions
   * @param paymentSellerStore
   * @param paymentCustomerStore
   */
  public void init(ProductDocument.ActionType actionType, Integer[] dealPositions, Integer paymentSellerStore, Integer paymentCustomerStore) {
    clear();
    
    this.actionType = actionType;
    
    if(actionType == ActionType.ОПЛАТА)
      initPayAction(dealPositions, paymentSellerStore, paymentCustomerStore);
    else {
      Map<Object,Map<Object,Map<Object,Map<Object,Map<Object,Map<Object,ObservableList<PropertyMap>>>>>>> seller_customer_contract_deal_storeType_objectType_positions = new HashMap();
      Map partitionNdsPayer = new HashMap();

      Map<String,List> documentSystemQueryCash   = new HashMap(); //кэш результатов запросов в базу

      // Распределим и отсортируем позиции сделок
      ObjectLoader.getList(DealPosition.class, dealPositions,
              "id",
              "seller:=:seller_partition_id",
              "seller-name=query:getCompanyName((SELECT [CompanyPartition(company)] FROM [CompanyPartition] WHERE id=[DealPosition(seller_partition_id)]))",
              "seller-partition:=:seller_partition_id",
              "seller-partition-ndspayer=query:(SELECT [CompanyPartition(nds-payer)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[DealPosition(seller_partition_id)])",

              "customer:=:customer_partition_id",
              "customer-name=query:getCompanyName((SELECT [CompanyPartition(company)] FROM [CompanyPartition] WHERE id=[DealPosition(customer_partition_id)]))",
              "customer-partition:=:customer_partition_id",
              "customer-partition-ndspayer=query:(SELECT [CompanyPartition(nds-payer)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[DealPosition(customer_partition_id)])",

              "contract:=:contract_id",
              "deal",
              "target-store-type",
              "target-store-object-type",
              "product").stream().forEach(d -> {

                companyNames.put(d.getValue("seller", Integer.TYPE), d.getValue("seller-name", String.class));
                companyNames.put(d.getValue("customer", Integer.TYPE), d.getValue("customer-name", String.class));

                partitionNdsPayer.put(d.getValue("seller"), d.getValue("seller-partition-ndspayer"));
                partitionNdsPayer.put(d.getValue("customer"), d.getValue("customer-partition-ndspayer"));

                if(seller_customer_contract_deal_storeType_objectType_positions.get(d.getValue("seller")) == null)
                  seller_customer_contract_deal_storeType_objectType_positions.put(d.getValue("seller"), new HashMap());

                if(seller_customer_contract_deal_storeType_objectType_positions.get(d.getValue("seller")).get(d.getValue("customer")) == null)
                  seller_customer_contract_deal_storeType_objectType_positions.get(d.getValue("seller")).put(d.getValue("customer"), new HashMap());

                if(seller_customer_contract_deal_storeType_objectType_positions.get(d.getValue("seller")).get(d.getValue("customer")).get(d.getValue("contract")) == null)
                  seller_customer_contract_deal_storeType_objectType_positions.get(d.getValue("seller")).get(d.getValue("customer")).put(d.getValue("contract"), new HashMap());

                if(seller_customer_contract_deal_storeType_objectType_positions.get(d.getValue("seller")).get(d.getValue("customer")).get(d.getValue("contract")).get(d.getValue("deal")) == null)
                  seller_customer_contract_deal_storeType_objectType_positions.get(d.getValue("seller")).get(d.getValue("customer")).get(d.getValue("contract")).put(d.getValue("deal"), new HashMap());

                if(seller_customer_contract_deal_storeType_objectType_positions.get(d.getValue("seller")).get(d.getValue("customer")).get(d.getValue("contract")).get(d.getValue("deal")).get(d.getValue("target-store-type","null")) == null)
                  seller_customer_contract_deal_storeType_objectType_positions.get(d.getValue("seller")).get(d.getValue("customer")).get(d.getValue("contract")).get(d.getValue("deal")).put(d.getValue("target-store-type"), new HashMap());

                if(seller_customer_contract_deal_storeType_objectType_positions.get(d.getValue("seller")).get(d.getValue("customer")).get(d.getValue("contract")).get(d.getValue("deal")).get(d.getValue("target-store-type")).get(d.getValue("target-store-object-type","null")) == null)
                  seller_customer_contract_deal_storeType_objectType_positions.get(d.getValue("seller")).get(d.getValue("customer")).get(d.getValue("contract")).get(d.getValue("deal")).get(d.getValue("target-store-type")).put(d.getValue("target-store-object-type"), FXCollections.observableArrayList());

                seller_customer_contract_deal_storeType_objectType_positions.get(d.getValue("seller")).get(d.getValue("customer")).get(d.getValue("contract")).get(d.getValue("deal"))
                        .get(d.getValue("target-store-type")).get(d.getValue("target-store-object-type")).add(d.copy());
              });

      seller_customer_contract_deal_storeType_objectType_positions.forEach((seller,customer_contract_deal_storeType_objectType_positions) -> {
        customer_contract_deal_storeType_objectType_positions.forEach((customer,contract_deal_storeType_objectType_positions) -> {
          contract_deal_storeType_objectType_positions.forEach((contract,deal_storeType_objectType_positions) -> {
            deal_storeType_objectType_positions.forEach((deal,storeType_objectType_positions) -> {
              
              boolean movable = (boolean) ObjectLoader.getData(Deal.class, (Integer)deal, "object-move");

              List<Integer> dpIds = new ArrayList();
              storeType_objectType_positions.forEach((storeType,objectType_positions) -> objectType_positions.forEach((objectType, positions) -> positions.stream().forEach((dp) -> dpIds.add(dp.getValue("id", Integer.TYPE)))));

              //Получить пользовательские документы
              ObjectLoader.executeQuery("SELECT "
                      + "[ProductDocument(document)], "
                      + "[ProductDocument(document_name)], "
                      + "[ProductDocument(document_script)], "
                      + "[ProductDocument(document_script_language)], "
                      + "(SELECT array_agg([DealPosition(id)]) FROM [DealPosition] WHERE [DealPosition(product)]=[ProductDocument(product)] AND id=ANY(?)) "
                      + "FROM [ProductDocument] "
                      + "WHERE "
                      + "[ProductDocument(type)]='CURRENT' AND "
                      + "[ProductDocument(tmp)]=false AND "
                      + "[ProductDocument(document-system)]=false AND "
                      + "[ProductDocument(actionType)]=? AND "
                      + "[ProductDocument(product)] IN (SELECT [DealPosition(product)] FROM [DealPosition] WHERE id=ANY(?))", 
                      new Object[]{dpIds.toArray(new Integer[0]), actionType, dpIds.toArray(new Integer[0])}).stream().forEach(pd -> {

                        Integer documentId     = (Integer) pd.get(0);
                        String  documentName   = (String)  pd.get(1);
                        String  documentScript = (String)  pd.get(2);
                        String  scriptLanguage = (String)  pd.get(3);
                        List<Integer> dps      = new ArrayList<>(Arrays.asList((Integer[])pd.get(4)));

                        if(!dps.isEmpty()) {
                          userDocuments.put(documentId, PropertyMap.create()
                                  .setValue("id", documentId)
                                  .setValue("name", documentName)
                                  .setValue("script", documentScript)
                                  .setValue("language", scriptLanguage)
                                  .setValue("autonumber", true));

                          if(document_seller_customer_contract_deal_dps.get(documentId) == null)
                            document_seller_customer_contract_deal_dps.put(documentId, new HashMap<>());
                          if(document_seller_customer_contract_deal_dps.get(documentId).get(seller) == null)
                            document_seller_customer_contract_deal_dps.get(documentId).put(seller, new HashMap<>());
                          if(document_seller_customer_contract_deal_dps.get(documentId).get(seller).get(customer) == null)
                            document_seller_customer_contract_deal_dps.get(documentId).get(seller).put(customer, new HashMap<>());
                          if(document_seller_customer_contract_deal_dps.get(documentId).get(seller).get(customer).get(contract) == null)
                            document_seller_customer_contract_deal_dps.get(documentId).get(seller).get(customer).put(contract, new HashMap<>());
                          if(document_seller_customer_contract_deal_dps.get(documentId).get(seller).get(customer).get(contract).get(deal) == null)
                            document_seller_customer_contract_deal_dps.get(documentId).get(seller).get(customer).get(contract).put(deal, dps);
                          else document_seller_customer_contract_deal_dps.get(documentId).get(seller).get(customer).get(contract).get(deal).addAll(dps);
                        }
                      });
              
              storeType_objectType_positions.forEach((storeType, objectType_positions) -> {
                objectType_positions.forEach((objectType, positions) -> {

                  List<Integer> ids = new ArrayList();
                  positions.stream().forEach((dp) -> ids.add(dp.getValue("id", Integer.TYPE)));
                  
                  List<Integer> products = new ArrayList();
                  positions.stream().forEach((dp) -> products.add(dp.getInteger("product")));
                  
                  
                  
                  boolean tmcCash = false, tmcCashLess = false, moneyCash = false, moneyCashLess = false;
                  
                  try {tmcCash       = StoreType.valueOf(storeType.toString()) == StoreType.НАЛИЧНЫЙ    && ObjectType.valueOf(objectType.toString()) == ObjectType.ТМЦ;}catch (Exception e) {}
                  try {tmcCashLess   = StoreType.valueOf(storeType.toString()) == StoreType.БЕЗНАЛИЧНЫЙ && ObjectType.valueOf(objectType.toString()) == ObjectType.ТМЦ;}catch (Exception e) {}
                  try {moneyCash     = StoreType.valueOf(storeType.toString()) == StoreType.НАЛИЧНЫЙ    && ObjectType.valueOf(objectType.toString()) == ObjectType.ВАЛЮТА;}catch (Exception e) {}
                  try {moneyCashLess = StoreType.valueOf(storeType.toString()) == StoreType.БЕЗНАЛИЧНЫЙ && ObjectType.valueOf(objectType.toString()) == ObjectType.ВАЛЮТА;}catch (Exception e) {}

                  Integer partition = (Integer) seller;
                  PropertyMap p = ObjectLoader.getMap(CompanyPartition.class, partition, "mainnumbering", "company");
                  if(p.getValue("mainnumbering", Boolean.TYPE))
                    partition = (Integer) ObjectLoader.executeQuery("SELECT id FROM [CompanyPartition] WHERE [CompanyPartition(company)]="+p.getValue("company", Integer.TYPE)+" AND mainPartition=true").get(0).get(0);

                  //Получить системные документы от продавца
                  if(documentSystemQueryCash.get(""+seller+seller+Owner.SELLER+partitionNdsPayer.get(seller)+actionType+movable+moneyCash+moneyCashLess+tmcCash+tmcCashLess) == null) {
                    documentSystemQueryCash.put(""+seller+seller+Owner.SELLER+partitionNdsPayer.get(seller)+actionType+movable+moneyCash+moneyCashLess+tmcCash+tmcCashLess, new ArrayList());
                    
                    for(List d:ObjectLoader.executeQuery(documentSystemQuery, 
                      partition, 
                      partition, 
                      Owner.SELLER, 
                      partitionNdsPayer.get(seller), 
                      actionType, 
                      movable,
                      moneyCash, 
                      moneyCashLess, 
                      tmcCash, 
                      tmcCashLess, 
                      products.toArray(new Integer[0]))) {
                        documentSystemQueryCash.get(""+seller+seller+Owner.SELLER+partitionNdsPayer.get(seller)+actionType+movable+moneyCash+moneyCashLess+tmcCash+tmcCashLess).add(d.get(0));
                        sellerSystemDocuments.put((Integer)d.get(0), PropertyMap.create()
                                .setValue("id"        , d.get(0))
                                .setValue("name"      , d.get(1))
                                .setValue("script"    , d.get(2))
                                .setValue("language"  , d.get(3))
                                .setValue("autonumber", d.get(4)));
                      }
                  }

                  documentSystemQueryCash.get(""+seller+seller+Owner.SELLER+partitionNdsPayer.get(seller)+actionType+movable+moneyCash+moneyCashLess+tmcCash+tmcCashLess).stream().forEach(documentId -> {
                    if(document_seller_customer_contract_deal_dps.get(documentId) == null)
                      document_seller_customer_contract_deal_dps.put(documentId,new HashMap<>());
                    if(document_seller_customer_contract_deal_dps.get(documentId).get(seller) == null)
                      document_seller_customer_contract_deal_dps.get(documentId).put(seller,new HashMap<>());
                    if(document_seller_customer_contract_deal_dps.get(documentId).get(seller).get(customer) == null)
                      document_seller_customer_contract_deal_dps.get(documentId).get(seller).put(customer,new HashMap<>());
                    if(document_seller_customer_contract_deal_dps.get(documentId).get(seller).get(customer).get(contract) == null)
                      document_seller_customer_contract_deal_dps.get(documentId).get(seller).get(customer).put(contract,new HashMap<>());
                    if(document_seller_customer_contract_deal_dps.get(documentId).get(seller).get(customer).get(contract).get(deal) == null)
                      document_seller_customer_contract_deal_dps.get(documentId).get(seller).get(customer).get(contract).put(deal, ids);
                  });


                  //Получить системные документы от покупателя
                  if(documentSystemQueryCash.get(""+customer+customer+Owner.CUSTOMER+partitionNdsPayer.get(customer)+actionType+movable+moneyCash+moneyCashLess+tmcCash+tmcCashLess) == null) {
                    documentSystemQueryCash.put(""+customer+customer+Owner.CUSTOMER+partitionNdsPayer.get(customer)+actionType+movable+moneyCash+moneyCashLess+tmcCash+tmcCashLess, new ArrayList());
                    for(List d:ObjectLoader.executeQuery(documentSystemQuery, 
                      new Object[]{partition, partition, Owner.CUSTOMER, partitionNdsPayer.get(customer), actionType, movable, moneyCash, moneyCashLess, tmcCash, tmcCashLess, products.toArray(new Integer[0])})) {
                        documentSystemQueryCash.get(""+customer+customer+Owner.CUSTOMER+partitionNdsPayer.get(customer)+actionType+movable+moneyCash+moneyCashLess+tmcCash+tmcCashLess).add(d.get(0));
                        customerSystemDocuments.put((Integer)d.get(0), PropertyMap.create()
                                .setValue("id"        , d.get(0))
                                .setValue("name"      , d.get(1))
                                .setValue("script"    , d.get(2))
                                .setValue("language"  , d.get(3))
                                .setValue("autonumber", d.get(4)));
                      }
                  }

                  documentSystemQueryCash.get(""+customer+customer+Owner.CUSTOMER+partitionNdsPayer.get(customer)+actionType+movable+moneyCash+moneyCashLess+tmcCash+tmcCashLess).stream().forEach(documentId -> {
                    if(document_seller_customer_contract_deal_dps.get(documentId) == null)
                      document_seller_customer_contract_deal_dps.put(documentId,new HashMap<>());
                    if(document_seller_customer_contract_deal_dps.get(documentId).get(seller) == null)
                      document_seller_customer_contract_deal_dps.get(documentId).put(seller,new HashMap<>());
                    if(document_seller_customer_contract_deal_dps.get(documentId).get(seller).get(customer) == null)
                      document_seller_customer_contract_deal_dps.get(documentId).get(seller).put(customer,new HashMap<>());
                    if(document_seller_customer_contract_deal_dps.get(documentId).get(seller).get(customer).get(contract) == null)
                      document_seller_customer_contract_deal_dps.get(documentId).get(seller).get(customer).put(contract,new HashMap<>());
                    if(document_seller_customer_contract_deal_dps.get(documentId).get(seller).get(customer).get(contract).get(deal) == null)
                      document_seller_customer_contract_deal_dps.get(documentId).get(seller).get(customer).get(contract).put(deal, ids);
                  });

                });
              });

            });
          });
        });
      });
    }
    userDocuments.forEach((id,document) -> document.setValue("date", LocalDate.now()));// {id,document -> document.customDate = LocalDate.now()}
    sellerSystemDocuments.forEach((id,document) -> document.setValue("date", actionType == ActionType.СТАРТ ? true : actionType == ActionType.ОТГРУЗКА ? false : LocalDate.now()));
    customerSystemDocuments.forEach((id,document) -> document.setValue("date", actionType == ActionType.СТАРТ ? true : actionType == ActionType.ОТГРУЗКА ? false : LocalDate.now()));
  }
  
  private void initPayAction(Integer[] dealPositions, Integer paymentSellerStore, Integer paymentCustomerStore) {
    //Поскольку это оплата, то эти парамметры берём только из платежа.
    boolean moneyCash     = false;
    boolean moneyCashLess = false;
    boolean tmcCash       = false;
    boolean tmcCashLess   = false;
    
    Map partitionNdsPayer = new HashMap();
    
    for(List it:ObjectLoader.executeQuery("SELECT [Store(objectType)]='ВАЛЮТА' AND [Store(storeType)]='НАЛИЧНЫЙ' FROM [Store] WHERE [Store(id)]="+paymentSellerStore))
      moneyCash = (boolean) it.get(0);
    
    for(List it:ObjectLoader.executeQuery("SELECT [Store(objectType)]='ВАЛЮТА' AND [Store(storeType)]='БЕЗНАЛИЧНЫЙ' FROM [Store] WHERE [Store(id)]="+paymentCustomerStore))
      moneyCashLess = (boolean) it.get(0);
    
    for(List it:ObjectLoader.executeQuery("SELECT [Store(objectType)]='ТМЦ' AND [Store(storeType)]='НАЛИЧНЫЙ' FROM [Store] WHERE [Store(id)]="+paymentSellerStore))
      tmcCash = (boolean) it.get(0);
    
    for(List it:ObjectLoader.executeQuery("SELECT [Store(objectType)]='ТМЦ' AND [Store(storeType)]='БЕЗНАЛИЧНЫЙ' FROM [Store] WHERE [Store(id)]="+paymentCustomerStore))
      tmcCashLess = (boolean) it.get(0);
    
    for(List it:ObjectLoader.executeQuery("SELECT "
      /*0*/+ "(SELECT [Store(companyPartition)] FROM [Store] WHERE id="+paymentSellerStore+"), "
      /*1*/+ "(SELECT [CompanyPartition(nds-payer)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=(SELECT [Store(companyPartition)] FROM [Store] WHERE id="+paymentSellerStore+")), "
      /*2*/+ "(SELECT [Store(companyPartition)] FROM [Store] WHERE id="+paymentCustomerStore+"), "
      /*3*/+ "(SELECT [CompanyPartition(nds-payer)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=(SELECT [Store(companyPartition)] FROM [Store] WHERE id="+paymentCustomerStore+")), "
      /*4*/+ "getCompanyName((SELECT [CompanyPartition(company)] FROM [CompanyPartition] WHERE id=(SELECT [Store(companyPartition)] FROM [Store] WHERE id="+paymentSellerStore+"))), "
      /*5*/+ "getCompanyName((SELECT [CompanyPartition(company)] FROM [CompanyPartition] WHERE id=(SELECT [Store(companyPartition)] FROM [Store] WHERE id="+paymentCustomerStore+"))) ")) {
      Integer seller           = (Integer) it.get(0);
      boolean sellerNdsPayer   = (Boolean) it.get(1);
      Integer customer         = (Integer) it.get(2);
      boolean customerNdsPayer = (boolean) it.get(3);
      
      companyNames.put(seller, (String) it.get(4));
      companyNames.put(seller, (String) it.get(5));
      
      partitionNdsPayer.put(seller, sellerNdsPayer);
      partitionNdsPayer.put(customer, customerNdsPayer);
      
      Map<String,List> documentSystemQueryCash   = new HashMap(); //кэш результатов запросов в базу
      Map<String,List<List>> documentNoSystemQueryCash = new HashMap(); //кэш результатов запросов в базу
      
      Integer[] productIds = new Integer[0];
      if(dealPositions != null && dealPositions.length > 0) {
        String products = "|";
        for(List d:ObjectLoader.executeQuery("SELECT [DealPosition(product)] FROM [DealPosition] WHERE id=ANY(?)",new Object[]{dealPositions})) {
          products += d.get(0)+"|";
          productIds = ArrayUtils.add(productIds, (Integer)d.get(0));
        }
        
        if(documentNoSystemQueryCash.get(products) == null) {
          documentNoSystemQueryCash.put(products, new ArrayList());
          //Получить пользовательские документы
          for(List d:ObjectLoader.executeQuery("SELECT "
                  + "[ProductDocument(document)], "
                  + "[ProductDocument(document_name)], "
                  + "[ProductDocument(document_script)], "
                  + "[ProductDocument(document_script_language)], "
                  + "(SELECT array_agg([DealPosition(id)]) FROM [DealPosition] WHERE [DealPosition(product)]=[ProductDocument(product)] AND id=ANY(?)) "
                  + "FROM [ProductDocument] "
                  + "WHERE "
                  + "[ProductDocument(document-system)]=false AND "
                  + "[ProductDocument(actionType)]=? AND "
                  + "[ProductDocument(product)] IN (SELECT [DealPosition(product)] FROM [DealPosition] WHERE id=ANY(?))", dealPositions,ActionType.ОПЛАТА,dealPositions)) {
            documentNoSystemQueryCash.get(products).add(d);
          }
        }
        
        //Получить пользовательские документы
        for(List d:documentNoSystemQueryCash.get(products)) {
          if(!Arrays.asList(d.get(4)).isEmpty()) {
            userDocuments.put((Integer) d.get(0), PropertyMap.create().setValue("id", d.get(0)).setValue("name", d.get(1)).setValue("script", d.get(2)).setValue("language", d.get(3)).setValue("autonumber", true));
            Integer documentId = (Integer) it.get(0);

            if(document_dps.get(documentId) == null)
              document_dps.put(documentId, Arrays.asList((Integer[])d.get(4)));
          }
        }
      }
      
      Integer partition = seller;
      List<List> data = ObjectLoader.executeQuery("SELECT [CompanyPartition(mainnumbering)], [CompanyPartition(company)] FROM [CompanyPartition] WHERE id="+seller);
      if((boolean)data.get(0).get(0))
        partition = (Integer) ObjectLoader.executeQuery("SELECT id FROM [CompanyPartition] WHERE [CompanyPartition(company)]="+data.get(0).get(1)+" AND mainPartition=true").get(0).get(0);
      
      //Получить системные документы от продавца
      if(documentSystemQueryCash.get(""+seller+seller+Owner.SELLER+partitionNdsPayer.get(seller)+true+moneyCash+moneyCashLess+tmcCash+tmcCashLess) == null) {
        documentSystemQueryCash.put(""+seller+seller+Owner.SELLER+partitionNdsPayer.get(seller)+true+moneyCash+moneyCashLess+tmcCash+tmcCashLess, new ArrayList<>());
        for(List d:ObjectLoader.executeQuery(documentSystemQuery, partition, partition, Owner.SELLER, partitionNdsPayer.get(seller), ActionType.ОПЛАТА, true, moneyCash, moneyCashLess, tmcCash, tmcCashLess, productIds)) {
            documentSystemQueryCash.get(""+seller+seller+Owner.SELLER+partitionNdsPayer.get(seller)+true+moneyCash+moneyCashLess+tmcCash+tmcCashLess).add(d.get(0));
            sellerSystemDocuments.put((Integer)d.get(0), PropertyMap.create().setValue("id", d.get(0)).setValue("name", d.get(1)).setValue("script", d.get(2)).setValue("language", d.get(3)).setValue("autonumber", d.get(4)));
          }
      }

      for(Object documentId:documentSystemQueryCash.get(""+seller+seller+Owner.SELLER+partitionNdsPayer.get(seller)+true+moneyCash+moneyCashLess+tmcCash+tmcCashLess)) {
        if(document_dps.get((Integer)documentId) == null)
          document_dps.put((Integer) documentId, Arrays.asList(dealPositions));
      }
      
      //Получить системные документы от покупателя
      if(documentSystemQueryCash.get(""+customer+customer+Owner.CUSTOMER+partitionNdsPayer.get(customer)+true+moneyCash+moneyCashLess+tmcCash+tmcCashLess) == null) {
        documentSystemQueryCash.put(""+customer+customer+Owner.CUSTOMER+partitionNdsPayer.get(customer)+true+moneyCash+moneyCashLess+tmcCash+tmcCashLess, new ArrayList());
        for(List d:ObjectLoader.executeQuery(documentSystemQuery, customer, customer, Owner.CUSTOMER, partitionNdsPayer.get(customer), ActionType.ОПЛАТА, true, moneyCash, moneyCashLess, tmcCash, tmcCashLess, productIds)) {
            documentSystemQueryCash.get(""+customer+customer+Owner.CUSTOMER+partitionNdsPayer.get(customer)+true+moneyCash+moneyCashLess+tmcCash+tmcCashLess).add(d.get(0));
            customerSystemDocuments.put((Integer)d.get(0), PropertyMap.create().setValue("id", d.get(0)).setValue("name", d.get(1)).setValue("script", d.get(2)).setValue("language", d.get(3)).setValue("autonumber", d.get(4)));
          }
      }

      for(Object documentId:documentSystemQueryCash.get(""+customer+customer+Owner.CUSTOMER+partitionNdsPayer.get(customer)+true+moneyCash+moneyCashLess+tmcCash+tmcCashLess)) {
        if(document_dps.get((Integer) documentId) == null)
          document_dps.put((Integer) documentId, Arrays.asList(dealPositions));
      }
    }
  }
  
  public boolean customUpdateDocuments(boolean splitFunction) {
    Map<Integer,PropertyMap> documents = new HashMap();
    documents.putAll(sellerSystemDocuments);
    documents.putAll(customerSystemDocuments);
    documents.putAll(userDocuments);
    return new DocumentDateChooser(documents, splitFunction).get();
  }
  
  private PropertyMap getDocumentData(Integer id) {
    if(sellerSystemDocuments.get(id) != null)
      return sellerSystemDocuments.get(id);
    if(customerSystemDocuments.get(id) != null)
      return customerSystemDocuments.get(id);
    if(userDocuments.get(id) != null)
      return userDocuments.get(id);
    return null;
  }
  
  private Integer createDocument(PropertyMap document, Integer[] dps, Integer paymentId, Map<String, Object> scriptData, RemoteSession session) throws Exception {
    Integer createdDocumentId = null;
    boolean iscreate = true;
    try {
      if(!document.getValue("script", String.class).equals(""))
        iscreate = (boolean) ScriptUtil.invokeMethodFromScript(document.getValue("script", String.class), document.getValue("language", String.class), "isCreateDocument", dps, paymentId);
    }catch(Exception ex) {}
    
    if(iscreate) {
      LocalDate documentDate = null;
      LocalDate stopDocumentDate = null;
      String sellerName = null;
      
      //session.executeQuery("select id from [CompanyPartition] where id in (SELECT [DealPosition(seller_partition_id)] FROM [DealPosition] WHERE id=ANY(?)) order by [CompanyPartition(mainPartition)] desc limit 1", new Object[]{dps});
      
      for(List data:session.executeQuery("SELECT "
              + "[CompanyPartition(docStopDate)], "
              + "getCompanyName([CompanyPartition(company)]) "
              + "FROM [CompanyPartition] "
              + "WHERE id=(select id from [CompanyPartition] "
                + "where id in (SELECT [DealPosition(seller_partition_id)] FROM [DealPosition] "
                  + "WHERE id=ANY(?)) order by [CompanyPartition(mainPartition)] desc limit 1)", new Object[]{dps})) {
        stopDocumentDate = Utility.convert((java.sql.Date)data.get(0));
        sellerName       = (String) data.get(1);
      }
      
      if(document.getValue("date") instanceof Boolean && document.getValue("date", Boolean.TYPE))
        documentDate = Utility.convert((java.sql.Date)session.executeQuery("SELECT MIN([DealPosition(deal_start_date)]) FROM [DealPosition] WHERE id=ANY(?)",new Object[]{dps}).get(0).get(0));
      else if(document.getValue("date") instanceof Boolean && !document.getValue("date", Boolean.TYPE))
        documentDate = Utility.convert((java.sql.Date)session.executeQuery("SELECT MAX([DealPosition(deal_end_date)]) FROM [DealPosition] WHERE id=ANY(?)",new Object[]{dps}).get(0).get(0));
      else documentDate = document.getValue("date", LocalDate.class);
        
      if(stopDocumentDate != null && actionType == ActionType.ОТГРУЗКА && (documentDate.equals(stopDocumentDate) || documentDate.isBefore(stopDocumentDate)))
        throw new Exception("Для организации <b>"+sellerName+"</b> установлена дата запрета отгрузки: <b>"+Utility.format(stopDocumentDate)+"</b>");
      
      for(List d:session.executeQuery("SELECT createDocument(?,?,?,?,?,?)", 
          document.getValue("id",Integer.TYPE),
          actionType == ActionType.ОПЛАТА ? new Integer[0] : dps,
          actionType.toString(),
          paymentId,
          "auto...".equals(document.getValue("autonumber")) || "".equals(document.getValue("autonumber")) || (document.getValue("autonumber") instanceof Boolean && document.getValue("autonumber", Boolean.TYPE)) ? null : document.getValue("number"),
          Utility.convertToTimestamp(documentDate))) {
        createdDocumentId = (Integer) d.get(0);
        runDocumentScript(createdDocumentId, document.getValue("name", String.class), document.getValue("script", String.class), document.getValue("language", String.class), dps, session, scriptData);
      }
    }
    return createdDocumentId;
  }
  
  private void runDocumentScript(Integer createdDocumentId, String name, String script, String language, Integer[] dealPositionIds, RemoteSession session, Map<String,Object> scriptData) throws Exception {
    Map documentData = getDocumentDataForTemplate(createdDocumentId, session);

    if(scriptData == null)
      scriptData = new HashMap<>();
    
    scriptData.put("session",session);
    scriptData.put("documentData",documentData);
    scriptData.put("dealPositionIds",dealPositionIds);
    scriptData.put("createdDocumentId",createdDocumentId);

    Object returnObject = ScriptUtil.runScript(name, script, language, scriptData);
    returnObject = returnObject != null && !returnObject.equals("") && returnObject instanceof String ? returnObject.toString() : name;

    Properties props = new Properties();
    props.putAll(documentData);
    session.executeUpdate("UPDATE [CreatedDocument] SET [CreatedDocument(params)]=?, [CreatedDocument(name)]=? WHERE id=?", props, returnObject, createdDocumentId);
  }
  
  private static Map getDocumentDataForTemplate(Integer createdDocumentId, RemoteSession session) throws Exception {
    return session.getList(DBFilter.create(CreatedDocument.class).AND_EQUAL("id",createdDocumentId),
            "id",
            "seller=query:getCompanyName([CreatedDocument(seller)])",
            "customer=query:getCompanyName([CreatedDocument(customer)])",
            "number",
            "date",
            "sellerAddr:=:seller-uraddres",
            "customerAddr:=:customer-uraddres",
            "sellerInn:=:seller-inn",
            "sellerKpp:=:seller-kpp",
            "sellerBookKeeper:=:seller-book-keeper",
            "customerInn:=:customer-inn",
            "customerKpp:=:customer-kpp",
            "sellerChifName=query:(SELECT [Company(chiefName)] FROM [Company] WHERE [Company(id)]=[CreatedDocument(seller)])",
            "sellerPartitionCount=query:(SELECT COUNT([CompanyPartition(id)]) FROM [CompanyPartition] WHERE [CompanyPartition(company)]=[CreatedDocument(seller)])",
            "customerPartitionCount=query:(SELECT COUNT([CompanyPartition(id)]) FROM [CompanyPartition] WHERE [CompanyPartition(company)]=[CreatedDocument(customer)])",
            "customerPostAddr:=:customer-postaddres",
            "sellerNds=query:(SELECT [Company(ndsPayer)] FROM [Company] WHERE [Company(id)]=[CreatedDocument(seller)])",
            "customerNds=query:(SELECT [Company(ndsPayer)] FROM [Company] WHERE [Company(id)]=[CreatedDocument(customer)])").stream().map(m -> {
              m.keySet().stream().filter(k -> m.get(k) == null).forEach(k -> m.put(k, ""));
              return m;
            }).findFirst().orElseGet(() -> new TreeMap());
  }
  
  public Map<Integer,List> start(RemoteSession session) throws Exception {
    return start(session, null, null);
  }
  
  public Map<Integer,List> start(RemoteSession session, Integer paymentId) throws Exception {
    return start(session, paymentId, null);
  }
  
  public Map<Integer,List> start(RemoteSession session, Integer paymentId, Map<String, Object> scriptData) throws Exception {
    Map<Integer,List> createdDocuments = new HashMap<>();
    
    for(Integer document:document_dps.keySet()) {
      Integer[] dps = document_dps.get(document).toArray(new Integer[0]);
      Integer cid = createDocument(getDocumentData(document), dps, paymentId, scriptData, session);
      if(cid != null) {
        document_dps.get(document).stream().forEach(dp -> {
          if(!createdDocuments.containsKey(dp))
            createdDocuments.put(dp, new ArrayList());
          createdDocuments.get(dp).add(cid);
        });
      }
    }

    for(Object document:document_seller_customer_contract_deal_dps.keySet()) {
      PropertyMap doc = getDocumentData((Integer) document);
      for(Object seller:document_seller_customer_contract_deal_dps.get(document).keySet()) {
        Map<Integer,List<Integer>> contract_joindps = new HashMap<>();
        for(Object customer:document_seller_customer_contract_deal_dps.get(document).get(seller).keySet()) {
          for(Object contract:document_seller_customer_contract_deal_dps.get(document).get(seller).get(customer).keySet()) {
            if(doc.is("split")) {
              
              if(!contract_joindps.containsKey(contract))
                contract_joindps.put((Integer)contract, FXCollections.observableArrayList());
              
              //List<Integer> dps_ = new ArrayList<>();
              for(Object deal:document_seller_customer_contract_deal_dps.get(document).get(seller).get(customer).get(contract).keySet())
                contract_joindps.get(contract).addAll(document_seller_customer_contract_deal_dps.get(document).get(seller).get(customer).get(contract).get(deal));
                //dps_.addAll(document_seller_customer_contract_deal_dps.get(document).get(seller).get(customer).get(contract).get(deal));
              /*Integer cid = createDocument(doc, dps_.toArray(new Integer[0]), paymentId, scriptData, session);
              if(cid != null) {
                dps_.stream().forEach(dp -> {
                  if(createdDocuments.get(dp) == null)
                    createdDocuments.put(dp, new ArrayList());
                  createdDocuments.get(dp).add(cid);
                });
              }*/
            }else {
              for(Object deal:document_seller_customer_contract_deal_dps.get(document).get(seller).get(customer).get(contract).keySet()) {
                List<Integer> dps = document_seller_customer_contract_deal_dps.get(document).get(seller).get(customer).get(contract).get(deal);
                System.out.println(doc.getValue("name")+": "+document_seller_customer_contract_deal_dps.get(document).get(seller).get(customer).get(contract).get(deal).getClass());
                Integer cid = createDocument(doc, dps.toArray(new Integer[0]), paymentId, scriptData, session);
                if(cid != null) {
                  dps.stream().forEach(dp -> {
                    if(createdDocuments.get(dp) == null)
                      createdDocuments.put(dp, new ArrayList());
                    createdDocuments.get(dp).add(cid);
                  });
                }
              }
            }
          }
        }
        
        if(!contract_joindps.isEmpty()) {
          for(Integer contract:contract_joindps.keySet()) {
            Integer cid = createDocument(doc, contract_joindps.get(contract).toArray(new Integer[0]), paymentId, scriptData, session);
            if(cid != null) {
              contract_joindps.get(contract).stream().forEach(dp -> {
                if(createdDocuments.get(dp) == null)
                  createdDocuments.put(dp, new ArrayList());
                createdDocuments.get(dp).add(cid);
              });
            }
          }
        }
      }
    }
    
    return createdDocuments;
  }

  public void clear() {
    companyNames.clear();
    document_dps.clear();
    sellerSystemDocuments.clear();
    customerSystemDocuments.clear();
    userDocuments.clear();
  }
}