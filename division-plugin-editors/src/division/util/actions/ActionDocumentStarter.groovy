package division.util.actions

import bum.interfaces.*
import bum.editors.util.ObjectLoader
import bum.interfaces.Service.Owner
import bum.interfaces.ProductDocument.ActionType
import util.filter.local.DBFilter
import util.*
import java.time.LocalDate
import division.util.*

public class ActionDocumentStarter {
  // В случае СТАРТА или ОТГРУЗКИ
  def document_seller_customer_contract_deal_dps
  // В случае ОПЛАТЫ
  def document_dps
  
  def Integer[]  dealPositions
  def ActionType actionType
  
  def Map<Integer,Map>    sellerSystemDocuments
  def Map<Integer,Map>    customerSystemDocuments
  def Map<Integer,Map>    userDocuments
  def Map<Integer,String> companyNames
  
  //Текст sql-запроса на получение системных документов
  private String documentSystemQuery = "SELECT \
   [Document(id)], \
   [Document(name)], \
   [Document(script)], \
   [Document(scriptLanguage)], \
   (SELECT COUNT(id)>0 FROM [CompanyPartitionDocument] WHERE tmp=false AND type='CURRENT' AND [CompanyPartitionDocument(document)]=[Document(id)] AND \
   [CompanyPartitionDocument(partition)]=? AND \
   (SELECT [CompanyPartitionDocument(startNumber)] NOTNULL AND [CompanyPartitionDocument(startNumber)] > 0 \
     FROM [CompanyPartitionDocument] \
     WHERE tmp=false AND type='CURRENT' AND [CompanyPartitionDocument(document)]=[Document(id)] AND \
     [CompanyPartitionDocument(partition)]=?)) \
  FROM [Document] \
  WHERE \
   [Document(tmp)]=false AND \
   [Document(type)]='CURRENT' AND \
   [Document(system)]=true AND \
   \
   [Document(documentSource)]=? AND \
   ([Document(ndsPayer)]=? OR [Document(ndsPayer)] IS NULL) AND \
   [Document(actionType)]=? AND \
   (\
    ([Document(movable)] IS NULL OR [Document(movable)]=false) OR \
    [Document(movable)]=true AND \
    [Document(moneyCash)]=? AND \
    [Document(moneyCashLess)]=? AND \
    [Document(tmcCash)]=? AND \
    [Document(tmcCashLess)]=?\
   )"
  
  def getDocumentData(id) {
    if(sellerSystemDocuments[id] != null)
      return sellerSystemDocuments[id]
    if(customerSystemDocuments[id] != null)
      return customerSystemDocuments[id]
    if(userDocuments[id] != null)
      return userDocuments[id]
    return null;
  }
  
  public Map<Integer,List> start(session, paymentId, scriptData) {
    println "START START START START START START START START START START START START START START START "
    def createdDocuments = [:]
    
    document_dps.each {documentId,dps -> 
      def cid = createDocument(getDocumentData(documentId), dps.toArray(new Integer[0]), paymentId, actionType, scriptData, session)
      if(cid != null) {
        dps.each {dp ->
          if(createdDocuments[dp] == null)
            createdDocuments[dp] = []
          createdDocuments[dp] << cid
        }
        //createdDocuments << cid
      }
    }
    
    /*document_seller_customer_contract_deal_dps.each {documentId,seller_customer_contract_deal_dps ->
      seller_customer_contract_deal_dps.each {seller,customer_contract_deal_dps ->
        customer_contract_deal_dps.each {customer,contract_deal_dps ->
          contract_deal_dps.each {contract,deal_dps ->
            def doc = getDocumentData(documentId)
            if(doc["split"]) {
              def dps_ = []
              deal_dps.each{deal,dps -> dps_.addAll(dps)}
              def cid = createDocument(doc, dps_.toArray(new Integer[0]), paymentId, actionType, scriptData, session)
              if(cid != null) {
                dps_.each {dp ->
                  if(createdDocuments[dp] == null)
                    createdDocuments[dp] = []
                  createdDocuments[dp] << cid
                }
                //createdDocuments << cid
              }
            }else {
              deal_dps.each {deal,dps ->
                def cid = createDocument(doc, dps.toArray(new Integer[0]), paymentId, actionType, scriptData, session)
                if(cid != null) {
                  dps.each {dp ->
                    if(createdDocuments[dp] == null)
                      createdDocuments[dp] = []
                    createdDocuments[dp] << cid
                  }
                  //createdDocuments << cid
                }
              }
            }
          }
        }
      }
    }*/
    
    document_seller_customer_contract_deal_dps.each {documentId,seller_customer_contract_deal_dps ->
      def doc = getDocumentData(documentId)
      seller_customer_contract_deal_dps.each {seller,customer_contract_deal_dps ->
        def splitdps = []
        customer_contract_deal_dps.each {customer,contract_deal_dps ->
          contract_deal_dps.each {contract,deal_dps ->
            if(doc["split"]) {
              println "000000000000000000000000000000000000"
              println "SPLIT"
              deal_dps.each{deal,dps -> splitdps.addAll(dps)}
            }else {
              deal_dps.each {deal,dps ->
                def cid = createDocument(doc, dps.toArray(new Integer[0]), paymentId, actionType, scriptData, session)
                if(cid != null) {
                  dps.each {dp ->
                    if(createdDocuments[dp] == null)
                      createdDocuments[dp] = []
                    createdDocuments[dp] << cid
                  }
                }
              }
            }
          }
        }
        println "splitdps = "+splitdps.size();
        if(!splitdps.isEmpty()) {
          def cid = createDocument(doc, splitdps.toArray(new Integer[0]), paymentId, actionType, scriptData, session)
          if(cid != null) {
            splitdps.each {dp ->
              if(createdDocuments[dp] == null)
                createdDocuments[dp] = []
              createdDocuments[dp] << cid
            }
          }
        }
      }
    }
    
    return createdDocuments
  }
  
  private createDocument(document, dps, paymentId, actionType, scriptData, session) {
    def returnId = null;
    def is = true;
    try {
      if(!document.script.equals(""))
        is = ScriptUtil.invokeMethodFromScript(document.script, document.scriptLanguage, [:], "isCreateDocument", [dps, paymentId].toArray())
    }catch(MissingMethodException ex) {}
    
    if(is) {
      def documentDate = null;
      def stopDocumentDate = null;
      def sellerName = null;
      
      session.executeQuery("SELECT [CompanyPartition(docStopDate)], getCompanyName([CompanyPartition(company)]) FROM [CompanyPartition] WHERE id=(SELECT [DealPosition(seller_partition_id)] FROM [DealPosition] WHERE id=?)", [dps[0]].toArray()).each{
        stopDocumentDate = it[0]
        sellerName = it[1]
      }
      
      if(document["customDate"] in Boolean && (boolean)document["customDate"])
        session.executeQuery("SELECT MIN([DealPosition(deal_start_date)]) FROM [DealPosition] WHERE id=ANY(?)",[dps].toArray()).each{documentDate = it[0]}
      else if(document["customDate"] in Boolean && !(boolean)document["customDate"])
        session.executeQuery("SELECT MAX([DealPosition(deal_end_date)]) FROM [DealPosition] WHERE id=ANY(?)",[dps].toArray()).each{documentDate = it[0]}
      else if(document["customDate"])
        documentDate = java.sql.Date.valueOf(document["customDate"])
        
      if(stopDocumentDate != null && actionType == ActionType.ОТГРУЗКА && (documentDate.equals(stopDocumentDate) || documentDate.before(stopDocumentDate))) {
        throw new Exception("Для организации <b>"+sellerName+"</b> установлена дата запрета отгрузки: <b>"+Utility.format(stopDocumentDate)+"</b>")
        //throw new Exception("<span style='font-size:20pt'><b>"+sellerName+"</b> установлена дата запрета отгрузки: <b>"+Utility.format(stopDocumentDate)+"</b></span>")
      }
      
      session.executeQuery("SELECT createDocument(?,?,?,?,?,?)", [
          document.id,
          actionType==ActionType.ОПЛАТА ? new Integer[0] : dps,
          actionType.toString(),
          paymentId,
          "auto...".equals(document.number) || "".equals(document.number) ? null : document.number,
          documentDate].toArray()).each {
            returnId = it[0]
            runDocumentScript(it[0], document.name, document.script, document.scriptLanguage, dps, session, scriptData);
          }
      //println "новый номер: "+session.executeQuery("select number from [CreatedDocument] where id=(select max(id) from [CreatedDocument])")[0][0];
    }
    return returnId
  }
  
  private runDocumentScript(createdDocumentId, name, script, language, dealPositionIds, session, scriptData) {
    def documentData = getDocumentDataForTemplate(createdDocumentId, session);
    documentData.id = createdDocumentId
    
    if(scriptData == null)
      scriptData = [:]
    
    scriptData << ["session":session, "documentData":documentData, "dealPositionIds":dealPositionIds, "createdDocumentId":createdDocumentId]

    def returnObject = ScriptUtil.runScript(name, script, language, scriptData);
    returnObject = returnObject != null && !returnObject.equals("") && returnObject instanceof String ? returnObject.toString() : name

    def props = [:] as Properties;
    props.putAll(documentData);
    session.executeUpdate("UPDATE [CreatedDocument] SET [CreatedDocument(params)]=?, [CreatedDocument(name)]=? WHERE id=?", 
      [props, returnObject, createdDocumentId].toArray());
  }
  
  private static Map getDocumentDataForTemplate(createdDocumentId, session) {
  def mapForTemplate = [:]
  session.executeQuery("SELECT \n"
    /*0*/ + "getCompanyName([CreatedDocument(seller)]),\n"
    /*1*/ + "getCompanyName([CreatedDocument(customer)]),\n"
    /*2*/ + "number,\n"
    /*3*/ + "date,\n"
    /*4*/ + "[CreatedDocument(seller-uraddres)],\n"
    /*5*/ + "[CreatedDocument(customer-uraddres)],\n"
    /*6*/ + "[CreatedDocument(seller-inn)],\n"
    /*7*/ + "[CreatedDocument(seller-kpp)],\n"
    /*8*/ + "[CreatedDocument(seller-book-keeper)],\n"
    /*9*/ + "[CreatedDocument(customer-inn)],\n"
    /*10*/+ "[CreatedDocument(customer-kpp)],\n"
    /*11*/+ "(SELECT [Company(chiefName)] FROM [Company] WHERE [Company(id)]=[CreatedDocument(seller)]),\n"
    /*12*/+ "(SELECT [Company(ndsPayer)] FROM [Company] WHERE [Company(id)]=[CreatedDocument(seller)]),\n"
    /*13*/+ "(SELECT [CompanyPartition(name)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[CreatedDocument(seller)]),\n"
    /*14*/+ "(SELECT [CompanyPartition(name)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[CreatedDocument(customer)]),\n"
    /*15*/+ "(SELECT COUNT([CompanyPartition(id)]) FROM [CompanyPartition] WHERE [CompanyPartition(company)]=[CreatedDocument(seller)]),\n"
    /*16*/+ "(SELECT COUNT([CompanyPartition(id)]) FROM [CompanyPartition] WHERE [CompanyPartition(company)]=[CreatedDocument(customer)]),\n"
    /*17*/+ "[CreatedDocument(customer-postaddres)],\n"
    /*18*/+ "(SELECT [Company(ndsPayer)] FROM [Company] WHERE [Company(id)]=[CreatedDocument(seller)]),\n"
    /*19*/+ "(SELECT [Company(ndsPayer)] FROM [Company] WHERE [Company(id)]=[CreatedDocument(customer)]) \n"
    +"FROM [CreatedDocument] WHERE id="+createdDocumentId).each {
      mapForTemplate.seller                 = it[0]==null  ? "" : it[0]
      mapForTemplate.customer               = it[1]==null  ? "" : it[1]
      mapForTemplate.number                 = it[2]==null  ? "" : it[2]
      mapForTemplate.date                   = it[3]==null  ? "" : it[3]
      mapForTemplate.sellerAddr             = it[4]==null  ? "" : it[4]
      mapForTemplate.customerAddr           = it[5]==null  ? "" : it[5]
      mapForTemplate.sellerInn              = it[6]==null  ? "" : it[6]
      mapForTemplate.sellerKpp              = it[7]==null  ? "" : it[7]
      mapForTemplate.sellerBookKeeper       = it[8]==null  ? "" : it[8]
      mapForTemplate.customerInn            = it[9]==null  ? "" : it[9]
      mapForTemplate.customerKpp            = it[10]==null ? "" : it[10]
      mapForTemplate.sellerChifName         = it[11]==null ? "" : it[11]
      mapForTemplate.ndsPayer               = it[12]
      mapForTemplate.sellerPartition        = it[13]==null ? "" : it[13]
      mapForTemplate.customerPartition      = it[14]==null ? "" : it[14]
      mapForTemplate.sellerPartitionCount   = it[15]==null ? 0  : it[15]
      mapForTemplate.customerPartitionCount = it[16]==null ? 0  : it[16]
      mapForTemplate.customerPostAddr       = it[17]==null ? "" : it[17]
      mapForTemplate.sellerNds              = it[18]
      mapForTemplate.customerNds            = it[19]
      mapForTemplate.id                     = createdDocumentId
    }
    return mapForTemplate;
  }
  
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  private void initPayAction(Integer[] dealPositions, Integer paymentSellerStore, Integer paymentCustomerStore) {    
    //Поскольку это оплата, то эти парамметры берём только из платежа.
    def moneyCash     = false
    def moneyCashLess = false
    def tmcCash       = false
    def tmcCashLess   = false
    
    def partitionNdsPayer = [:]
    
    ObjectLoader.executeQuery("SELECT [Store(objectType)]='ВАЛЮТА' AND \
    [Store(storeType)]='НАЛИЧНЫЙ' FROM [Store] WHERE [Store(id)]="+paymentSellerStore).each {moneyCash = it[0]}
    
    ObjectLoader.executeQuery("SELECT [Store(objectType)]='ВАЛЮТА' AND \
    [Store(storeType)]='БЕЗНАЛИЧНЫЙ' FROM [Store] WHERE [Store(id)]="+paymentCustomerStore).each {moneyCashLess = it[0]}
    
    ObjectLoader.executeQuery("SELECT [Store(objectType)]='ТМЦ' AND \
    [Store(storeType)]='НАЛИЧНЫЙ' FROM [Store] WHERE [Store(id)]="+paymentSellerStore).each {tmcCash = it[0]}
    
    ObjectLoader.executeQuery("SELECT [Store(objectType)]='ТМЦ' AND \
    [Store(storeType)]='БЕЗНАЛИЧНЫЙ' FROM [Store] WHERE [Store(id)]="+paymentCustomerStore).each {tmcCashLess = it[0]}
    
    ObjectLoader.executeQuery("SELECT "
      /*0*/+ "(SELECT [Store(companyPartition)] FROM [Store] WHERE id="+paymentSellerStore+"), "
      /*1*/+ "(SELECT [CompanyPartition(nds-payer)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=(SELECT [Store(companyPartition)] FROM [Store] WHERE id="+paymentSellerStore+")), "
      /*2*/+ "(SELECT [Store(companyPartition)] FROM [Store] WHERE id="+paymentCustomerStore+"), "
      /*3*/+ "(SELECT [CompanyPartition(nds-payer)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=(SELECT [Store(companyPartition)] FROM [Store] WHERE id="+paymentCustomerStore+")), "
      /*4*/+ "getCompanyName((SELECT [CompanyPartition(company)] FROM [CompanyPartition] WHERE id=(SELECT [Store(companyPartition)] FROM [Store] WHERE id="+paymentSellerStore+"))), "
      /*5*/+ "getCompanyName((SELECT [CompanyPartition(company)] FROM [CompanyPartition] WHERE id=(SELECT [Store(companyPartition)] FROM [Store] WHERE id="+paymentCustomerStore+"))) "
      + "").each {
      def seller           = it[0]
      def sellerNdsPayer   = it[1]
      def customer         = it[2]
      def customerNdsPayer = it[3]
      
      companyNames[seller]   = it[4]
      companyNames[customer] = it[5]
      
      partitionNdsPayer[seller]   = sellerNdsPayer
      partitionNdsPayer[customer] = customerNdsPayer
      
      def documentSystemQueryCash   = [:] //кэш результатов запросов в базу
      def documentNoSystemQueryCash = [:] //кэш результатов запросов в базу
      
      if(dealPositions != null && dealPositions.length > 0) {
        def products = "|"
        ObjectLoader.executeQuery("SELECT [DealPosition(product)] FROM [DealPosition] WHERE id=ANY(?)",[dealPositions].toArray())
        .each {products += it[0]+"|"}
        
        if(documentNoSystemQueryCash[products] == null) {
          documentNoSystemQueryCash[products] = []
          //Получить пользовательские документы
          ObjectLoader.executeQuery("SELECT \
            [ProductDocument(document)], \
            [ProductDocument(document_name)], \
            [ProductDocument(document_script)], \
            [ProductDocument(document_script_language)], \
            (SELECT array_agg([DealPosition(id)]) FROM [DealPosition] WHERE [DealPosition(product)]=[ProductDocument(product)] AND id=ANY(?)) \
          FROM [ProductDocument] \
          WHERE \
            [ProductDocument(document-system)]=false AND \n\
            [ProductDocument(actionType)]=? AND \n\
            [ProductDocument(product)] IN (SELECT [DealPosition(product)] FROM [DealPosition] WHERE id=ANY(?))", 
            [dealPositions,ActionType.ОПЛАТА,dealPositions].toArray()).each {
            documentNoSystemQueryCash[products] << it
          }
        }
        
        //Получить пользовательские документы
        documentNoSystemQueryCash[products].each {
          if(!Arrays.asList(it[4]).isEmpty()) {
            userDocuments[it[0]] = [id:it[0], name:it[1], script:it[2], scriptLanguage:it[3], auto:true]
            def documentId = it[0]

            if(document_dps[documentId] == null)
              document_dps[documentId] = Arrays.asList(it[4])
          }
        }
      }
      
      def partition = seller;
      def data = ObjectLoader.executeQuery("SELECT [CompanyPartition(mainnumbering)], [CompanyPartition(company)] FROM [CompanyPartition] WHERE id="+seller)
      println "#######################################################################"
      println "data = "+data
      if(data[0][0])
        partition = ObjectLoader.executeQuery("SELECT id FROM [CompanyPartition] WHERE [CompanyPartition(company)]="+data[0][1]+" AND mainPartition=true")[0][0]
      println "#######################################################################"
      
      //Получить системные документы от продавца
      if(documentSystemQueryCash[""+seller+seller+Owner.SELLER+partitionNdsPayer[seller]+moneyCash+moneyCashLess+tmcCash+tmcCashLess] == null) {
        documentSystemQueryCash[""+seller+seller+Owner.SELLER+partitionNdsPayer[seller]+moneyCash+moneyCashLess+tmcCash+tmcCashLess] = []
        ObjectLoader.executeQuery(documentSystemQuery, 
          [partition, partition, Owner.SELLER, partitionNdsPayer[seller], ActionType.ОПЛАТА, moneyCash, moneyCashLess, tmcCash, tmcCashLess].toArray()).each {
            documentSystemQueryCash[""+seller+seller+Owner.SELLER+partitionNdsPayer[seller]+moneyCash+moneyCashLess+tmcCash+tmcCashLess] << it[0]
            sellerSystemDocuments[it[0]] = [id:it[0], name:it[1], script:it[2], scriptLanguage:it[3], auto:it[4]]
          }
      }

      documentSystemQueryCash[""+seller+seller+Owner.SELLER+partitionNdsPayer[seller]+moneyCash+moneyCashLess+tmcCash+tmcCashLess].each {
        def documentId = it
        if(document_dps[documentId] == null)
          document_dps[documentId] = Arrays.asList(dealPositions)
      }
      
      //Получить системные документы от покупателя
      if(documentSystemQueryCash[""+customer+customer+Owner.CUSTOMER+partitionNdsPayer[seller]+moneyCash+moneyCashLess+tmcCash+tmcCashLess] == null) {
        documentSystemQueryCash[""+customer+customer+Owner.CUSTOMER+partitionNdsPayer[seller]+moneyCash+moneyCashLess+tmcCash+tmcCashLess] = []
        ObjectLoader.executeQuery(documentSystemQuery, 
          [customer, customer, Owner.CUSTOMER, partitionNdsPayer[seller], ActionType.ОПЛАТА, moneyCash, moneyCashLess, tmcCash, tmcCashLess].toArray()).each {
            documentSystemQueryCash[""+customer+customer+Owner.CUSTOMER+partitionNdsPayer[seller]+moneyCash+moneyCashLess+tmcCash+tmcCashLess] << it[0]
            customerSystemDocuments[it[0]] = [id:it[0], name:it[1], script:it[2], scriptLanguage:it[3], auto:it[4]]
          }
      }

      documentSystemQueryCash[""+customer+customer+Owner.CUSTOMER+partitionNdsPayer[seller]+moneyCash+moneyCashLess+tmcCash+tmcCashLess].each {
        def documentId = it
        if(document_dps[documentId] == null)
          document_dps[documentId] = Arrays.asList(dealPositions)
      }
    }
  }
  
  public void init(ActionType actionType, Integer[] dealPositions) {
    init(actionType, dealPositions, null, null)
  }
  
  /**
   * actionType - событие
   * dealPositions - позиции, на которые это событие распростоняется
   * // ЕСЛИ СОБЫТИЕ "ОПЛАТА":
   * paymentSellerStore - склад получателя платежа
   * paymentCustomerStore - склад плательщика
   **/
  public void init(ActionType actionType, Integer[] dealPositions, Integer paymentSellerStore, Integer paymentCustomerStore) {
    println "init "+actionType
    this.dealPositions = dealPositions
    this.actionType    = actionType
    
    document_seller_customer_contract_deal_dps = [:]
    document_dps = [:]
    
    sellerSystemDocuments = [:]
    customerSystemDocuments = [:]
    userDocuments = [:]
    
    companyNames = [:]
    
    if(actionType == ActionType.ОПЛАТА)
      initPayAction(dealPositions, paymentSellerStore, paymentCustomerStore)
    else {
      println "0000"
      def partitionNdsPayer = [:]
      def seller_customer_contract_deal_storeType_objectType_dps = [:]

      ObjectLoader.executeQuery("SELECT "
        /*0*/ + "[DealPosition(seller_partition_id)], "
        /*1*/ + "(SELECT [CompanyPartition(nds-payer)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[DealPosition(seller_partition_id)]), "
        /*2*/ + "[DealPosition(customer_partition_id)], "
        /*3*/ + "(SELECT [CompanyPartition(nds-payer)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[DealPosition(customer_partition_id)]), "
        /*4*/ + "[DealPosition(contract_id)], "
        /*5*/ + "[DealPosition(deal)], "
        /*6*/ + "[DealPosition(id)], "
        /*7*/ + "[DealPosition(target-store-type)], "
        /*8*/ + "[DealPosition(target-store-object-type)], "
        /*9*/ + "getCompanyName((SELECT [CompanyPartition(company)] FROM [CompanyPartition] WHERE id=[DealPosition(seller_partition_id)])), "
        /*10*/+ "getCompanyName((SELECT [CompanyPartition(company)] FROM [CompanyPartition] WHERE id=[DealPosition(customer_partition_id)])) "
        + "FROM [DealPosition] WHERE id=ANY(?)", [dealPositions].toArray()).each {
        def seller           = it[0]
        def sellerNdsPayer   = it[1]
        def customer         = it[2]
        def customerNdsPayer = it[3]
        def contract         = it[4]
        def deal             = it[5]
        def dealPositionId   = it[6]
        def storeType        = it[7] == null ? "null" : it[7]
        def objectType       = it[8] == null ? "null" : it[8]

        companyNames[seller]   = it[9]
        companyNames[customer] = it[10]

        partitionNdsPayer[seller]   = sellerNdsPayer
        partitionNdsPayer[customer] = customerNdsPayer

        if(seller_customer_contract_deal_storeType_objectType_dps[seller] == null)
          seller_customer_contract_deal_storeType_objectType_dps[seller] = [:]

        if(seller_customer_contract_deal_storeType_objectType_dps[seller][customer] == null)
          seller_customer_contract_deal_storeType_objectType_dps[seller][customer] = [:]

        if(seller_customer_contract_deal_storeType_objectType_dps[seller][customer][contract] == null)
          seller_customer_contract_deal_storeType_objectType_dps[seller][customer][contract] = [:]

        if(seller_customer_contract_deal_storeType_objectType_dps[seller][customer][contract][deal] == null)
          seller_customer_contract_deal_storeType_objectType_dps[seller][customer][contract][deal] = [:]

        if(seller_customer_contract_deal_storeType_objectType_dps[seller][customer][contract][deal][storeType] == null)
          seller_customer_contract_deal_storeType_objectType_dps[seller][customer][contract][deal][storeType] = [:]

        if(seller_customer_contract_deal_storeType_objectType_dps[seller][customer][contract][deal][storeType][objectType] == null)
          seller_customer_contract_deal_storeType_objectType_dps[seller][customer][contract][deal][storeType][objectType] = []

        seller_customer_contract_deal_storeType_objectType_dps[seller][customer][contract][deal][storeType][objectType] << dealPositionId
      }

      def documentSystemQueryCash   = [:] //кэш результатов запросов в базу
      def documentNoSystemQueryCash = [:] //кэш результатов запросов в базу
      
      seller_customer_contract_deal_storeType_objectType_dps.each {seller,customer_contract_deal_storeType_objectType_dps ->
        customer_contract_deal_storeType_objectType_dps.each {customer,contract_deal_storeType_objectType_dps ->
          contract_deal_storeType_objectType_dps.each {contract,deal_storeType_objectType_dps ->
            deal_storeType_objectType_dps.each {deal,storeType_objectType_dps ->

              def dp = []
              storeType_objectType_dps.each {storeType,objectType_dps ->
                objectType_dps.each {objectType,dps -> 
                  dp.addAll(dps)
                }
              }
              
              def products = "|"
              ObjectLoader.executeQuery("SELECT [DealPosition(product)] FROM [DealPosition] WHERE id=ANY(?)",[dp.toArray(new Integer[0])].toArray()).each {
                products += it[0]+"|"
              }

              //if(documentNoSystemQueryCash[""+actionType+products] == null) {
                //documentNoSystemQueryCash[""+actionType+products] = []
                //Получить пользовательские документы
                println "Получить пользовательские документы : "+dp
                ObjectLoader.executeQuery("SELECT \
                  [ProductDocument(document)], \
                  [ProductDocument(document_name)], \
                  [ProductDocument(document_script)], \
                  [ProductDocument(document_script_language)], \
                  (SELECT array_agg([DealPosition(id)]) FROM [DealPosition] WHERE [DealPosition(product)]=[ProductDocument(product)] AND id=ANY(?)) \
                FROM [ProductDocument] \
                WHERE \
                  [ProductDocument(type)]='CURRENT' AND \n\
                  [ProductDocument(tmp)]=false AND\
                  [ProductDocument(document-system)]=false AND \n\
                  [ProductDocument(actionType)]=? AND \n\
                  [ProductDocument(product)] IN (SELECT [DealPosition(product)] FROM [DealPosition] WHERE id=ANY(?))", 
                  [dp.toArray(new Integer[0]),actionType,dp.toArray(new Integer[0])].toArray()).each {
                  //println "dps = "+Arrays.asList(it[4])
                  //documentNoSystemQueryCash[""+actionType+products] << it
                  List dps = new ArrayList(new HashSet<Integer>(Arrays.asList(it[4])));
                  if(!dps.isEmpty()) {
                    userDocuments[it[0]] = [id:it[0], name:it[1], script:it[2], scriptLanguage:it[3], auto:true]
                    def documentId = it[0]

                    if(document_seller_customer_contract_deal_dps[documentId] == null)
                      document_seller_customer_contract_deal_dps[documentId] = [:]
                    if(document_seller_customer_contract_deal_dps[documentId][seller] == null)
                      document_seller_customer_contract_deal_dps[documentId][seller] = [:]
                    if(document_seller_customer_contract_deal_dps[documentId][seller][customer] == null)
                      document_seller_customer_contract_deal_dps[documentId][seller][customer] = [:]
                    if(document_seller_customer_contract_deal_dps[documentId][seller][customer][contract] == null)
                      document_seller_customer_contract_deal_dps[documentId][seller][customer][contract] = [:]
                    if(document_seller_customer_contract_deal_dps[documentId][seller][customer][contract][deal] == null)
                      document_seller_customer_contract_deal_dps[documentId][seller][customer][contract][deal] = dps
                    else document_seller_customer_contract_deal_dps[documentId][seller][customer][contract][deal].addAll(dps)
                  }
                }
              //}

              //Получить пользовательские документы
              /*documentNoSystemQueryCash[""+actionType+products].each {
                List dps = new ArrayList(new HashSet<Integer>(Arrays.asList(it[4])));
                if(!dps.isEmpty()) {
                  userDocuments[it[0]] = [id:it[0], name:it[1], script:it[2], scriptLanguage:it[3], auto:true]
                  def documentId = it[0]

                  if(document_seller_customer_contract_deal_dps[documentId] == null)
                    document_seller_customer_contract_deal_dps[documentId] = [:]
                  if(document_seller_customer_contract_deal_dps[documentId][seller] == null)
                    document_seller_customer_contract_deal_dps[documentId][seller] = [:]
                  if(document_seller_customer_contract_deal_dps[documentId][seller][customer] == null)
                    document_seller_customer_contract_deal_dps[documentId][seller][customer] = [:]
                  if(document_seller_customer_contract_deal_dps[documentId][seller][customer][contract] == null)
                    document_seller_customer_contract_deal_dps[documentId][seller][customer][contract] = [:]
                  if(document_seller_customer_contract_deal_dps[documentId][seller][customer][contract][deal] == null)
                    document_seller_customer_contract_deal_dps[documentId][seller][customer][contract][deal] = dps
                }
              }*/

              storeType_objectType_dps.each {storeType,objectType_dps ->
                objectType_dps.each {objectType,dps ->

                  def tmcCash     = storeType.equals("НАЛИЧНЫЙ")    && objectType.equals("ТМЦ")
                  def tmcCashLess = storeType.equals("БЕЗНАЛИЧНЫЙ") && objectType.equals("ТМЦ")
                  def moneyCash     = storeType.equals("НАЛИЧНЫЙ")    && objectType.equals("ВАЛЮТА")
                  def moneyCashLess = storeType.equals("БЕЗНАЛИЧНЫЙ") && objectType.equals("ВАЛЮТА")
                  
                  def partition = seller;
                  def data = ObjectLoader.executeQuery("SELECT [CompanyPartition(mainnumbering)], [CompanyPartition(company)] FROM [CompanyPartition] WHERE id="+partition)
                  if(data[0][0])
                    partition = ObjectLoader.executeQuery("SELECT id FROM [CompanyPartition] WHERE [CompanyPartition(company)]="+data[0][1]+" AND mainPartition=true")[0][0]

                  //Получить системные документы от продавца
                  if(documentSystemQueryCash[""+seller+seller+Owner.SELLER+partitionNdsPayer[seller]+actionType+moneyCash+moneyCashLess+tmcCash+tmcCashLess] == null) {
                    documentSystemQueryCash[""+seller+seller+Owner.SELLER+partitionNdsPayer[seller]+actionType+moneyCash+moneyCashLess+tmcCash+tmcCashLess] = []
                    ObjectLoader.executeQuery(documentSystemQuery, 
                      [partition, partition, Owner.SELLER, partitionNdsPayer[seller], actionType, moneyCash, moneyCashLess, tmcCash, tmcCashLess].toArray()).each {
                        documentSystemQueryCash[""+seller+seller+Owner.SELLER+partitionNdsPayer[seller]+actionType+moneyCash+moneyCashLess+tmcCash+tmcCashLess] << it[0]
                        sellerSystemDocuments[it[0]] = [id:it[0], name:it[1], script:it[2], scriptLanguage:it[3], auto:it[4]]
                      }
                  }

                  documentSystemQueryCash[""+seller+seller+Owner.SELLER+partitionNdsPayer[seller]+actionType+moneyCash+moneyCashLess+tmcCash+tmcCashLess].each {
                    def documentId = it

                    if(document_seller_customer_contract_deal_dps[documentId] == null)
                      document_seller_customer_contract_deal_dps[documentId] = [:]
                    if(document_seller_customer_contract_deal_dps[documentId][seller] == null)
                      document_seller_customer_contract_deal_dps[documentId][seller] = [:]
                    if(document_seller_customer_contract_deal_dps[documentId][seller][customer] == null)
                      document_seller_customer_contract_deal_dps[documentId][seller][customer] = [:]
                    if(document_seller_customer_contract_deal_dps[documentId][seller][customer][contract] == null)
                      document_seller_customer_contract_deal_dps[documentId][seller][customer][contract] = [:]
                    if(document_seller_customer_contract_deal_dps[documentId][seller][customer][contract][deal] == null)
                      document_seller_customer_contract_deal_dps[documentId][seller][customer][contract][deal] = dps
                  }

                  //Получить системные документы от покупателя
                  if(documentSystemQueryCash[""+customer+customer+Owner.CUSTOMER+partitionNdsPayer[seller]+actionType+moneyCash+moneyCashLess+tmcCash+tmcCashLess] == null) {
                    documentSystemQueryCash[""+customer+customer+Owner.CUSTOMER+partitionNdsPayer[seller]+actionType+moneyCash+moneyCashLess+tmcCash+tmcCashLess] = []
                    ObjectLoader.executeQuery(documentSystemQuery, 
                      [customer, customer, Owner.CUSTOMER, partitionNdsPayer[seller], actionType, moneyCash, moneyCashLess, tmcCash, tmcCashLess].toArray()).each {
                        documentSystemQueryCash[""+customer+customer+Owner.CUSTOMER+partitionNdsPayer[seller]+actionType+moneyCash+moneyCashLess+tmcCash+tmcCashLess] << it[0]
                        customerSystemDocuments[it[0]] = [id:it[0], name:it[1], script:it[2], scriptLanguage:it[3], auto:it[4]]
                      }
                  }

                  documentSystemQueryCash[""+customer+customer+Owner.CUSTOMER+partitionNdsPayer[seller]+actionType+moneyCash+moneyCashLess+tmcCash+tmcCashLess].each {
                    def documentId = it

                    if(document_seller_customer_contract_deal_dps[documentId] == null)
                      document_seller_customer_contract_deal_dps[documentId] = [:]
                    if(document_seller_customer_contract_deal_dps[documentId][seller] == null)
                      document_seller_customer_contract_deal_dps[documentId][seller] = [:]
                    if(document_seller_customer_contract_deal_dps[documentId][seller][customer] == null)
                      document_seller_customer_contract_deal_dps[documentId][seller][customer] = [:]
                    if(document_seller_customer_contract_deal_dps[documentId][seller][customer][contract] == null)
                      document_seller_customer_contract_deal_dps[documentId][seller][customer][contract] = [:]
                    if(document_seller_customer_contract_deal_dps[documentId][seller][customer][contract][deal] == null)
                      document_seller_customer_contract_deal_dps[documentId][seller][customer][contract][deal] = dps
                  }
                }
              }
            }
          }
        }
      }
    }
    
    userDocuments.each {id,document -> document.customDate = LocalDate.now()}
    
    sellerSystemDocuments.each {id,document ->
      document.customDate    = actionType==ActionType.СТАРТ ? true : actionType==ActionType.ОТГРУЗКА ? false : LocalDate.now()
    }
    
    customerSystemDocuments.each {id,document ->
      document.customDate    = actionType==ActionType.СТАРТ ? true : actionType==ActionType.ОТГРУЗКА ? false : LocalDate.now()
    }
    
    println "document_seller_customer_contract_deal_dps = "+document_seller_customer_contract_deal_dps
  }
  
  /*public boolean customUpdateDocuments(boolean splitFunction) {
    return customUpdateDocuments(splitFunction, false);
  }*/
  
  public boolean customUpdateDocuments(boolean splitFunction) {
    def documents = [:]
    documents.putAll(sellerSystemDocuments)
    documents.putAll(customerSystemDocuments)
    documents.putAll(userDocuments)
    return new DocumentDateChooser(documents, splitFunction).get()
  }
}