package scripts.groovy.classes

import java.awt.Dimension;
import javafx.application.Platform;
import javafx.collections.ObservableList
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox
import javafx.scene.control.DatePicker
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox
import javax.swing.JFrame;
import javax.swing.JDialog;
import javafx.collections.FXCollections;
import java.time.LocalDate;
import javafx.scene.layout.Priority;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane
import bum.interfaces.*
import bum.editors.util.ObjectLoader
import javafx.geometry.Pos;
import javafx.scene.effect.*
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Label
import javafx.scene.layout.HBox;
import javafx.event.*
import division.fx.*
import java.awt.Cursor

import division.swing.guimessanger.Messanger
import bum.interfaces.Service.Owner
import bum.interfaces.ProductDocument.ActionType
import util.filter.local.DBFilter
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import division.swing.guimessanger.Messanger
import division.util.*
import division.*
import division.util.actions.*

class DocumentsCreater {
  //Текст sql-запроса на получение системных документов
  private static String documentSystemQuery = "SELECT \
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

  /* На входе получаем
   * массив позиций сделок dealPositions
   * событие actionType
   * платёж paymentId
   * данные для скрипта scriptData
   * сессия session
   */
  
  public static run(
    process, 
    actionType, 
    session, 
    dealPositions, 
    paymentId, 
    documentNumbers, 
    scriptData,
    document_seller_customer_contract_deal_dps,
    documents,
    companyNames) {
    
    def createdDocuments = []
    
    process.setVisible(true)
    process.setValue(0)
    process.setText("Подсчёт...")
    
    int max = 0;
    document_seller_customer_contract_deal_dps.each {document,seller_customer_contract_deal_dps ->
      seller_customer_contract_deal_dps.each {seller,customer_contract_deal_dps ->
        customer_contract_deal_dps.each {customer,contract_deal_dps ->
          contract_deal_dps.each {contract,deal_dps ->
            if(documents[document]["split"]) {max++}
            else deal_dps.each {deal,dps -> max++ }
          }
        }
      }
    }
    
    process.setMax(max)
    def index = 0;
    document_seller_customer_contract_deal_dps.each {documentId,seller_customer_contract_deal_dps ->
      seller_customer_contract_deal_dps.each {seller,customer_contract_deal_dps ->
        customer_contract_deal_dps.each {customer,contract_deal_dps ->
          contract_deal_dps.each {contract,deal_dps ->
            if(documents[documentId]["split"]) {
              index++;
              process.setValue(index)
              process.setText("\""+documents[documentId]["name"]+"\" для "+companyNames[customer])
              def dps_ = []
              println "deal_dps = "+deal_dps
              deal_dps.each{deal,dps -> dps_.addAll(dps)}
              def cid = createDocument(documents[documentId], dps_.toArray(new Integer[0]), paymentId, actionType, scriptData, session)
              if(cid != null)
                createdDocuments << cid
            }else {
              deal_dps.each {deal,dps ->
                index++;
                process.setValue(index)
                process.setText("\""+documents[documentId]["name"]+"\" для "+companyNames[customer])
                def cid = createDocument(documents[documentId], dps.toArray(new Integer[0]), paymentId, actionType, scriptData, session)
                if(cid != null)
                  createdDocuments << cid
              }
            }
          }
        }
      }
    }
    process.setVisible(false)
    return createdDocuments
  }

  private static createDocument(document, dps, paymentId, actionType, scriptData, session) {
    def returnId = null;
    def is = true;
    try {
      if(!document.script.equals(""))
        is = ScriptUtil.invokeMethodFromScript(document.script, document.scriptLanguage, [:], "isCreateDocument", [dps, paymentId].toArray())
    }catch(MissingMethodException ex) {}
    
    if(is) {
      def documentDate = null;
      if(document["startDealDate"])
        session.executeQuery("SELECT MIN([DealPosition(deal_start_date)]) FROM [DealPosition] WHERE id=ANY(?)",[dps].toArray()).each{documentDate = it[0]}
      else if(document["endDealDate"])
        session.executeQuery("SELECT MAX([DealPosition(deal_end_date)]) FROM [DealPosition] WHERE id=ANY(?)",[dps].toArray()).each{documentDate = it[0]}
      else if(document["customDate"])
        documentDate = java.sql.Date.valueOf(document["customDate"])
      
      session.executeQuery("SELECT createDocument(?,?,?,?,?,?)", [
          document.id,
          actionType==ActionType.ОПЛАТА ? new Integer[0] : dps,
          actionType.toString(),
          paymentId,
          document.number.equals("auto...")?null:document.number,
          documentDate].toArray()).each {
            returnId = it[0]
            runDocumentScript(it[0], document.name, document.script, document.scriptLanguage, dps, session, scriptData);
          }
    }
    return returnId
  }

  private static runDocumentScript(createdDocumentId, name, script, language, dealPositionIds, session, scriptData) {
    def documentData = getDocumentDataForTemplate(createdDocumentId, session);
    documentData.id = createdDocumentId

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
    }
    return mapForTemplate;
  }
  
  
  
  /*
   * actionType - событие
   * dealPositions - позиции сделок
   * paymentId - платёж
   * document_seller_customer_contract_deal_dps - отсортированные позиции, заполняется в этом методе
   * documents - Здесь храним скрипты документов, признак объединения и даты, заполняется в этом методе
   * companyNames - Здесь храним названия компаний, заполняется в этом методе
   * */
  
  private static setDocumentDates(
    process,
    actionType, 
    dealPositions, 
    document_seller_customer_contract_deal_dps, 
    documents,
    companyNames) {
    return setDocumentDates(process, actionType, dealPositions, null, null, document_seller_customer_contract_deal_dps, documents, companyNames)
  }
  
  private static setDocumentDates(
    process,
    actionType, 
    dealPositions, 
    sellerStore, 
    customerStore,
    document_seller_customer_contract_deal_dps, 
    documents,
    companyNames) {
    
    process.setVisible(true)
    process.setMax(10)
    process.setText("Вычисляю список документов...")
    process.setValue(3)
    def moneyCash = false
    def moneyCashLess = false
    //Если событие ОПЛАТА, то вычисляем способ оплаты
    if(actionType == ActionType.ОПЛАТА && sellerStore != null && customerStore != null) {
      ObjectLoader.executeQuery("SELECT [Store(objectType)]='ВАЛЮТА' AND "
        +"[Store(storeType)]='НАЛИЧНЫЙ' FROM [Store] WHERE [Store(id)]="+sellerStore).each {moneyCash = it[0]}
      ObjectLoader.executeQuery("SELECT [Store(objectType)]='ВАЛЮТА' AND "
        +"[Store(storeType)]='БЕЗНАЛИЧНЫЙ' FROM [Store] WHERE [Store(id)]="+customerStore).each {moneyCashLess = it[0]}
    }
    
    def partitionNdsPayer              = [:]
    def seller_customer_contract_deal_storeType_objectType_dps = [:]

    ObjectLoader.executeQuery("SELECT "
      /*0*/ + "[DealPosition(seller_partition_id)],"
      /*1*/ + "(SELECT [CompanyPartition(nds-payer)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[DealPosition(seller_partition_id)]),"
      /*2*/ + "[DealPosition(customer_partition_id)],"
      /*3*/ + "(SELECT [CompanyPartition(nds-payer)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[DealPosition(customer_partition_id)]),"
      /*4*/ + "[DealPosition(contract_id)],"
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
    
    process.setValue(6)
    
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
            
            if(documentNoSystemQueryCash[""+actionType+products] == null) {
              documentNoSystemQueryCash[""+actionType+products] = []
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
                [dp.toArray(new Integer[0]),actionType,dp.toArray(new Integer[0])].toArray()).each {
                documentNoSystemQueryCash[""+actionType+products] << it
              }
            }
            
            //Получить пользовательские документы
            documentNoSystemQueryCash[""+actionType+products].each {
              if(!Arrays.asList(it[4]).isEmpty()) {
                documents[it[0]] = [id:it[0], name:it[1], script:it[2], scriptLanguage:it[3], auto:true]
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
                  document_seller_customer_contract_deal_dps[documentId][seller][customer][contract][deal] = Arrays.asList(it[4])
              }
            }
            
            
            storeType_objectType_dps.each {storeType,objectType_dps ->
              objectType_dps.each {objectType,dps ->

                def tmcCash     = storeType.equals("НАЛИЧНЫЙ")    && objectType.equals("ТМЦ")
                def tmcCashLess = storeType.equals("БЕЗНАЛИЧНЫЙ") && objectType.equals("ТМЦ")
                if(actionType != ActionType.ОПЛАТА) {
                  moneyCash     = storeType.equals("НАЛИЧНЫЙ")    && objectType.equals("ВАЛЮТА")
                  moneyCashLess = storeType.equals("БЕЗНАЛИЧНЫЙ") && objectType.equals("ВАЛЮТА")
                }

                //Получить системные документы от продавца
                if(documentSystemQueryCash[""+seller+seller+Owner.SELLER+partitionNdsPayer[seller]+actionType+moneyCash+moneyCashLess+tmcCash+tmcCashLess] == null) {
                  documentSystemQueryCash[""+seller+seller+Owner.SELLER+partitionNdsPayer[seller]+actionType+moneyCash+moneyCashLess+tmcCash+tmcCashLess] = []
                  ObjectLoader.executeQuery(documentSystemQuery, 
                    [seller, seller, Owner.SELLER, partitionNdsPayer[seller], actionType, moneyCash, moneyCashLess, tmcCash, tmcCashLess].toArray()).each {
                      documentSystemQueryCash[""+seller+seller+Owner.SELLER+partitionNdsPayer[seller]+actionType+moneyCash+moneyCashLess+tmcCash+tmcCashLess] << it[0]
                      documents[it[0]] = [id:it[0], name:it[1], script:it[2], scriptLanguage:it[3], auto:it[4]]
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
                      documents[it[0]] = [id:it[0], name:it[1], script:it[2], scriptLanguage:it[3], auto:it[4]]
                    }
                }

                documentSystemQueryCash[""+customer+customer+Owner.CUSTOMER+partitionNdsPayer[seller]+actionType+moneyCash+moneyCashLess+tmcCash+tmcCashLess].each {
                  def document = it

                  if(document_seller_customer_contract_deal_dps[document] == null)
                    document_seller_customer_contract_deal_dps[document] = [:]
                  if(document_seller_customer_contract_deal_dps[document][seller] == null)
                    document_seller_customer_contract_deal_dps[document][seller] = [:]
                  if(document_seller_customer_contract_deal_dps[document][seller][customer] == null)
                    document_seller_customer_contract_deal_dps[document][seller][customer] = [:]
                  if(document_seller_customer_contract_deal_dps[document][seller][customer][contract] == null)
                    document_seller_customer_contract_deal_dps[document][seller][customer][contract] = [:]
                  if(document_seller_customer_contract_deal_dps[document][seller][customer][contract][deal] == null)
                    document_seller_customer_contract_deal_dps[document][seller][customer][contract][deal] = dps
                }
              }
            }
          }
        }
      }
    }
    
    documents.each {id,document ->
      document.startDealDate = actionType==ActionType.СТАРТ    ? true : false
      document.endDealDate   = actionType==ActionType.ОТГРУЗКА ? true : false
    }
    
    process.setValue(10)
    process.setVisible(false)
    return new DocumentDateChooser(documents).get()
  }
}