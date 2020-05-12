package scripts.groovy.classes

import javax.swing.JOptionPane
import jdk.nashorn.api.scripting.NashornScriptEngine
import util.RemoteSession
import util.filter.local.DBFilter
import bum.interfaces.*
import bum.interfaces.CreatedDocument
import bum.interfaces.ProductDocument.ActionType
import bum.interfaces.Store.StoreType
import division.swing.LocalProcessing
import bum.interfaces.Group.ObjectType
import division.swing.guimessanger.Messanger
import java.sql.Timestamp
import java.util.AbstractMap.SimpleEntry
import java.util.Map.Entry
import java.util.Date
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineFactory
import javax.script.ScriptEngineManager
import java.util.Map
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import bum.interfaces.CreatedDocument.ExportType
import bum.interfaces.CreatedDocument.SendType
import bum.interfaces.Service.Owner
import groovy.lang.MissingMethodException

public class GroovyUtility {
  private static NashornScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
  private static GroovyShell         shell  = new GroovyShell(GroovyUtility.class.getClassLoader());

  public static List<document> getDocuments(
    ActionType actionType, 
    Integer[] dealPositions, 
    Integer paymentId,
    RemoteSession session) throws Exception {
    
    //def data = session.executeQuery("SELECT [DealPosition()] FROM");
    //Получаем документы, которые относятся к данному событию и данным продуктам.
    
    def data = []
    def sellerNdsPayer   = false
    def customerNdsPayer = false
    
    if(paymentId == null) {
      data = session.executeQuery("SELECT [Company(ndsPayer)] FROM [Company] WHERE id=(SELECT [DealPosition(seller_id)] FROM [DealPosition] WHERE id=?)", [dealPositions[0]].toArray())
      sellerNdsPayer   = data[0][0];

      data = session.executeQuery("SELECT [Company(ndsPayer)] FROM [Company] WHERE id=(SELECT [DealPosition(customer_id)] FROM [DealPosition] WHERE id=?)", [dealPositions[0]].toArray())
      customerNdsPayer   = data[0][0];
    }
    
    def moneyCash = false;
    def moneyCashLess = false;
    def tmcCash = false;
    def tmcCashLess = false;
    
    data = session.executeQuery("SELECT [Payment(seller-store-object-type)]='ВАЛЮТА' AND [Payment(seller-store-type)]='НАЛИЧНЫЙ' FROM [Payment] WHERE [Payment(id)]=?", [paymentId].toArray())
    moneyCash = data.isEmpty() ? false : data[0][0]
    
    data = session.executeQuery("SELECT [Payment(seller-store-object-type)]='ВАЛЮТА' AND [Payment(seller-store-type)]='БЕЗНАЛИЧНЫЙ' FROM [Payment] WHERE [Payment(id)]=?", [paymentId].toArray())
    moneyCashLess = data.isEmpty() ? false : data[0][0]
    
    if(actionType != ActionType.ОПЛАТА) {
      data = session.executeQuery("SELECT [DealPosition(target-store-object-type)]='ТМЦ' AND [DealPosition(target-store-type)]='НАЛИЧНЫЙ' FROM [DealPosition] WHERE [DealPosition(id)]=? LIMIT 1", [dealPositions[0]].toArray())
      tmcCash = data.isEmpty() ? false : data[0][0]

      data = session.executeQuery("SELECT [DealPosition(target-store-object-type)]='ТМЦ' AND [DealPosition(target-store-type)]='БЕЗНАЛИЧНЫЙ' FROM [DealPosition] WHERE [DealPosition(id)]=? LIMIT 1", [dealPositions[0]].toArray())
      tmcCashLess = data.isEmpty() ? false : data[0][0]
    }
    
    data = session.executeQuery("SELECT "
      /*0*/+  "[Document(id)],"
      /*1*/ + "[Document(name)],"
      /*2*/ + "[Document(script)],"
      /*3*/ + "[Document(scriptLanguage)], "
      
      /*4*/ + "[Document(system)],"
      /*5*/ + "[Document(documentSource)],"
      /*6*/ + "[Document(ndsPayer)],"
      /*7*/ + "[Document(actionType)],"
      /*8*/ + "[Document(movable)], "
      /*9*/ + "[Document(moneyCash)], "
      /*10*/+ "[Document(moneyCashLess)], "
      /*11*/+ "[Document(tmcCash)], "
      /*12*/+ "[Document(tmcCashLess)] "
      
      + "FROM [Document] "
      + "WHERE "
      + "tmp = false AND "
      + "type = 'CURRENT' AND "
      + "([Document(system)] = true AND "
      + "id NOT IN (SELECT [ProductDocument(document)] FROM [ProductDocument] WHERE tmp=false and type='CURRENT' AND [ProductDocument(actionType)]=? AND "
      + "[ProductDocument(product)] IN (select DISTINCT [DealPosition(product)] FROM [DealPosition] WHERE [DealPosition(id)]=ANY(?))) AND "
      + "[Document(actionType)] = ? AND "
      + "([Document(ndsPayer)] IS NULL OR [Document(ndsPayer)] = CASE WHEN [Document(documentSource)]='SELLER' THEN "+sellerNdsPayer+" ELSE "+customerNdsPayer+" END) AND "
      + "([Document(movable)]=false OR [Document(movable)] IS NULL OR "
      + "("
        +"[Document(movable)]=true AND "
        +"[Document(moneyCash)]="+(moneyCash==null?"false":moneyCash)+" AND "
        +"[Document(moneyCashLess)]="+(moneyCashLess==null?"false":moneyCashLess)+" AND "
        +"[Document(tmcCash)]="+(tmcCash==null?"false":tmcCash)+" AND "
        +"[Document(tmcCashLess)]="+(tmcCashLess==null?"false":tmcCashLess)
      + "))"
      + " OR "
      
      +  "[Document(system)] = false AND id IN "
      + "(SELECT DISTINCT [ProductDocument(document)] FROM [ProductDocument] "
      + "WHERE tmp=false and type='CURRENT' and [ProductDocument(actionType)]=? and "
      + "[ProductDocument(product)] IN (select DISTINCT [DealPosition(product)] FROM [DealPosition] WHERE [DealPosition(id)]=ANY(?))))", 
      [actionType, dealPositions, actionType, actionType, dealPositions].toArray());
    
    def documents = []
    data.each {
      documents << new document(
        id:            it[0],
        name:          it[1],
        script:        it[2],
        scriptLanguage:it[3],
        system:        it[4],
        documentSource:it[5]==null?null:Owner.valueOf((String)it[5]),
        ndsPayer:      it[6],
        actionType:it[7]==null?null:ActionType.valueOf((String)it[7]),
        movable:       it[8],
        moneyCash:     it[9],
        moneyCashLess: it[10],
        tmcCash:       it[11],
        tmcCashLess:   it[12],
      )}

    return documents;
  }
  
  public static createDocuments(
    LocalProcessing process,
    ActionType actionType, 
    Date date, 
    RemoteSession session, 
    Integer[] dealPositions, 
    Integer paymentId, 
    Map<Integer, Map> documentNumbers) throws Exception {
      return createDocuments(process, actionType, date, session, dealPositions, paymentId, documentNumbers, [:])
    }

  public static createDocuments(
    LocalProcessing process,
    ActionType actionType, 
    Date date, 
    RemoteSession session, 
    Integer[] dealPositions, 
    Integer paymentId, 
    Map<Integer, Map> documentNumbers, 
    Map<String, Object> scriptData) throws Exception {
    
    def createdDocuments = []
    
    process.setAlwaysOnTop(true)
    process.setSubProgressVisible(true)
    process.setSubTextVisible(true)
    
    def documents = getDocuments(actionType, dealPositions, paymentId, session)
    process.setMinMax(0,documents.size()+1)
    startModuls(session)
    
    documents.each { doc -> 
      process.setValue(process.getValue()+1)
      process.setText("Создание документов типа \""+doc.getName()+"\"")
      def deals = getDealPositionsForDocument(dealPositions, doc.getId(), doc.isSystem(), session)
      //def dps   = []
      //deals.each {key,value -> dps += value}
      
      String documentNumber = null;
      Date   documentDate   = null;

      if(documentNumbers != null && documentNumbers.containsKey(doc.getId())) {
        documentNumber = documentNumbers[doc.getId()].number
        documentDate   = documentNumbers[doc.getId()].date
      }
      
      if(!deals.isEmpty()) {
        def contractsDeals = [:]
        session.executeQuery("SELECT [Deal(customerCompanyPartition)], [Deal(contract)], id FROM [Deal] WHERE id=ANY(?) ORDER BY [Deal(customerCompany)], [Deal(contract)]",
          [deals.keySet().toArray(new Integer[0])].toArray()).each {
            def d = contractsDeals[it[0]+"-"+it[1]]
            if(d == null)
              contractsDeals[it[0]+"-"+it[1]] = [:]
            def dd = contractsDeals[it[0]+"-"+it[1]]
            dd[it[2]] = deals[it[2]]
          }
        if(contractsDeals.size() != deals.size() && 
          JOptionPane.showConfirmDialog(process.getDialog(), "Объединять документы \""+doc.getName()+"\"?", "Вопрос", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)==0) {
          contractsDeals.each {key,dealMap ->
            
            def dps = []
            dealMap.each {k,v -> dps += v}
            
            process.setSubValue(0)
            process.setSuibMinMax(0,deals.size())
            
            def is = true;
            try {
              is = invokeMethodFromScript(doc.getScript(), doc.getScriptLanguage(), [:], "isCreateDocument", [dps.toArray(new Integer[0]), paymentId].toArray())
            }catch(MissingMethodException ex){}
            
            if(is) {
              def data = session.executeQuery("SELECT createDocument(?,?,?,?,?,?)", [
                  doc.getId(),
                  dps.toArray(new Integer[0]),
                  actionType.toString(),
                  paymentId,
                  documentNumber,
                  (documentDate==null?(date==null?getDocumentDate(dealMap.keySet().toArray(new Integer[0]), actionType, session):new Timestamp(date.getTime())):new Timestamp(documentDate.getTime()))].toArray());

              if(!data.isEmpty()) {
                def createdDocumentId = (Integer) data[0][0]
                createdDocuments << createdDocumentId

                def number = session.getData(CreatedDocument.class, [createdDocumentId].toArray(new Integer[0]), "number")[0][0]
                process.setSubText(doc.getName()+(number!=null?" № "+number:""))
                process.setSubValue(process.getSubValue()+1)

                runDocumentScript(createdDocumentId, doc.getName(), doc.getScript(), doc.getScriptLanguage(), dps.toArray(new Integer[0]), session, scriptData);
              }
            }
          }
        }else {
          deals.each {deal,dps ->
            process.setSubValue(0)
            process.setSuibMinMax(0,deals.size())
            
            def is = true;
            try {
              is = invokeMethodFromScript(doc.getScript(), doc.getScriptLanguage(), [:], "isCreateDocument", [dps.toArray(new Integer[0]), paymentId].toArray())
            }catch(MissingMethodException ex) {}
            
            if(is) {
              def data = session.executeQuery("SELECT createDocument(?,?,?,?,?,?)", [
                  doc.getId(),
                  dps.toArray(new Integer[0]),
                  actionType.toString(),
                  paymentId,
                  documentNumber,
                  (documentDate==null?(date==null?getDocumentDate([deal].toArray(new Integer[0]), actionType, session):new Timestamp(date.getTime())):new Timestamp(documentDate.getTime()))].toArray());

              if(!data.isEmpty()) {
                def createdDocumentId = (Integer) data[0][0]
                createdDocuments << createdDocumentId

                def number = session.getData(CreatedDocument.class, [createdDocumentId].toArray(new Integer[0]), "number")[0][0]
                process.setSubText(doc.getName()+(number!=null?" № "+number:""))
                process.setSubValue(process.getSubValue()+1)

                runDocumentScript(createdDocumentId, doc.getName(), doc.getScript(), doc.getScriptLanguage(), dps.toArray(new Integer[0]), session, scriptData);
              }
            }
          }
        }
      }else {
        process.setSubValue(0)
        process.setSuibMinMax(0,deals.size())
        
        def is = true;
        try {
          is = invokeMethodFromScript(doc.getScript(), doc.getScriptLanguage(), [:], "isCreateDocument", [new Integer[0], paymentId].toArray())
        }catch(MissingMethodException ex){}
        
        if(is) {
          def data = session.executeQuery("SELECT createDocument(?,?,?,?,?,?)", [
              doc.getId(),
              new Integer[0],
              actionType.toString(),
              paymentId,
              documentNumber,
              (documentDate==null?(date==null?getDocumentDate(deals.keySet().toArray(new Integer[0]), actionType, session):new Timestamp(date.getTime())):new Timestamp(documentDate.getTime()))].toArray());

          if(!data.isEmpty()) {
            def createdDocumentId = (Integer) data[0][0]
            createdDocuments << createdDocumentId

            def number = session.getData(CreatedDocument.class, [createdDocumentId].toArray(new Integer[0]), "number")[0][0]
            process.setSubText(doc.getName()+(number!=null?" № "+number:""))
            process.setSubValue(process.getSubValue()+1)

            runDocumentScript(createdDocumentId, doc.getName(), doc.getScript(), doc.getScriptLanguage(), new Integer[0], session, scriptData);
          }
        }
      }
    }
    process.setValue(process.getMaximum())
    return createdDocuments;
  }
  
  public static void replaceDocuments(Integer[] createdDocuments, RemoteSession session) {
    replaceDocuments(createdDocuments, session, [:]);
  }
  
  public static void replaceDocuments(Integer[] createdDocuments, RemoteSession session, Map<String,Object> scriptData) {
    session.executeQuery("SELECT "
      /*0*/ + "[CreatedDocument(id)],"
      /*1*/ + "[CreatedDocument(document)],"
      /*2*/ + "[CreatedDocument(document_name)],"
      /*3*/ + "[CreatedDocument(document_script)],"
      /*4*/ + "[CreatedDocument(document-scriptLanguage)], "
      /*5*/ + "[CreatedDocument(document-system)], "
      /*6*/ + "ARRAY(SELECT [CreatedDocument(dealPositions):target] FROM [CreatedDocument(dealPositions):table] WHERE [CreatedDocument(dealPositions):object]=[CreatedDocument(id)]) "
      + "FROM [CreatedDocument] WHERE id=ANY(?)", [createdDocuments].toArray()).each {
      def dps   = []
      getDealPositionsForDocument(it[6], it[1], it[5], session).each {key,value -> dps += value}
      
      runDocumentScript(
        it[0], 
        it[2], 
        it[3], 
        it[4], 
        dps.toArray(new Integer[0]), 
        session,
        scriptData);
    }
  }
  
  public static getDealPositionsForDocument(Integer[] dealPositions, Integer documentId, boolean system, session) {
    def deals = [:]
    def query = "SELECT [DealPosition(deal)], [DealPosition(id)] FROM [DealPosition] WHERE [DealPosition(id)]=ANY(?)"
    if(!system)
      query += " AND [DealPosition(product)] IN (SELECT [ProductDocument(product)] FROM [ProductDocument] WHERE [ProductDocument(document)]="+documentId+")"
    session.executeQuery(query, [dealPositions].toArray()).each {
      def dps = deals[it[0]]
      if(dps == null)
        deals[it[0]] = (dps = [])
      dps << it[1];
    }
    return deals;
  }
  
  private static runDocumentScript(
    Integer createdDocumentId, 
    String name, 
    String script, 
    String language, 
    Integer[] dealPositionIds, 
    RemoteSession session,
    Map<String,Object> scriptData) {
    
    def documentData = getDocumentDataForTemplate(createdDocumentId, session);
    documentData["id"] = createdDocumentId
    
    scriptData << [
            "session":session,
            "documentData":documentData,
            "dealPositionIds":dealPositionIds,
            "createdDocumentId":createdDocumentId ]
    
    def returnObject = runScript(
      name, 
      script, 
      language, 
      scriptData);
    
    def props = [:] as Properties;
    props.putAll(documentData);

    def sqlParams = [props];
    if(returnObject != null && !returnObject.equals(""))
      sqlParams << returnObject.toString()
    sqlParams << createdDocumentId
    
    session.executeUpdate("UPDATE [CreatedDocument] SET [CreatedDocument(params)]=? "
            +(returnObject != null && !returnObject.equals("") ? ", [CreatedDocument(name)]=?" : "")+" WHERE id=?", sqlParams.toArray());
  }

  public static void startModuls(RemoteSession session) throws Exception {
    session.getData(JSModul.class, new DBFilter(JSModul.class).AND_EQUAL("tmp", false).AND_EQUAL("type", "CURRENT"), 
      ["name","script", "scriptLanguage"].toArray(new String[0])).each {
      runScript((String) it.get(0), (String) it.get(1), (String) it.get(2), [:])
    }
  }

  public static Object runScript(String scriptName, String scriptText, String scriptLanguage, Map<String, Object> params) throws Exception {
    def returnObject = scriptName;
    if(scriptText != null && !scriptText.equals("")) {
      try {
        switch(scriptLanguage) {
          case SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT:
            params.each {key,value -> engine.put(key, value)}
            returnObject = engine.eval(scriptText);
            break;
          case SyntaxConstants.SYNTAX_STYLE_GROOVY:
            params.each {key,value -> shell.setVariable(key, value)}
            returnObject = shell.evaluate(scriptText);
            break;
        }
      }catch(Exception ex) {
        returnObject = null;
        throw new Exception("Ошибка в скрипте "+scriptName+": ", ex);
      }
    }
    return returnObject;
  }
  
  public static Object invokeMethodFromScript(String scriptText, String scriptLanguage, Map<String, Object> scriptParams, String methodName, Object[] methodParams) throws javax.script.ScriptException {
    def returnObject = null;
    if(scriptText != null && !scriptText.equals("")) {
      scriptParams = scriptParams==null?[:]:scriptParams;
      switch(scriptLanguage) {
        case SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT:
          scriptParams.each {key,value -> engine.put(key, value)}
          engine.eval(scriptText);
          returnObject = engine.invokeFunction(methodName, methodParams);
          break;
        case SyntaxConstants.SYNTAX_STYLE_GROOVY:
          scriptParams.each {key,value -> shell.setVariable(key, value)}
          returnObject = shell.parse(scriptText).invokeMethod(methodName, methodParams);
          break;
      }
    }
    return returnObject;
  }
  
  public static Map<String, Object> getDocumentDataForTemplate(Integer createdDocumentId, RemoteSession session) {
    Vector<Vector> data = session.executeQuery("SELECT \n"
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
    +"FROM [CreatedDocument] WHERE id="+createdDocumentId);
    def mapForTemplate = [:]
    mapForTemplate.seller                 = data[0][0]==null  ? "" : data[0][0]
    mapForTemplate.customer               = data[0][1]==null  ? "" : data[0][1]
    mapForTemplate.number                 = data[0][2]==null  ? "" : data[0][2]
    mapForTemplate.date                   = data[0][3]==null  ? "" : data[0][3]
    mapForTemplate.sellerAddr             = data[0][4]==null  ? "" : data[0][4]
    mapForTemplate.customerAddr           = data[0][5]==null  ? "" : data[0][5]
    mapForTemplate.sellerInn              = data[0][6]==null  ? "" : data[0][6]
    mapForTemplate.sellerKpp              = data[0][7]==null  ? "" : data[0][7]
    mapForTemplate.sellerBookKeeper       = data[0][8]==null  ? "" : data[0][8]
    mapForTemplate.customerInn            = data[0][9]==null  ? "" : data[0][9]
    mapForTemplate.customerKpp            = data[0][10]==null ? "" : data[0][10]
    mapForTemplate.sellerChifName         = data[0][11]==null ? "" : data[0][11]
    mapForTemplate.ndsPayer               = data[0][12];
    mapForTemplate.sellerPartition        = data[0][13]==null ? "" : data[0][13]
    mapForTemplate.customerPartition      = data[0][14]==null ? "" : data[0][14]
    mapForTemplate.sellerPartitionCount   = data[0][15]==null ? 0  : data[0][15]
    mapForTemplate.customerPartitionCount = data[0][16]==null ? 0  : data[0][16]
    mapForTemplate.customerPostAddr       = data[0][17]==null ? "" : data[0][17]
    mapForTemplate.sellerNds              = data[0][18]
    mapForTemplate.customerNds            = data[0][19]
    return mapForTemplate;
  }
  
  private static Timestamp getDocumentDate(Integer[] deals, ActionType actionType, RemoteSession session) throws Exception {
    Timestamp documentDate = null;
    Vector<Vector> data;
    switch(actionType) {
      case ActionType.СТАРТ: 
        data = session.executeQuery("SELECT MIN([Deal(dealStartDate)]) FROM [Deal] WHERE [Deal(id)]=ANY(?)", [deals].toArray());
        if(!data.isEmpty())
          documentDate = new Timestamp(((java.sql.Date)data[0][0]).getTime());
        break;
      case ActionType.ОТГРУЗКА:
        data = session.executeQuery("SELECT MAX([Deal(dealEndDate)]) FROM [Deal] WHERE [Deal(id)]=ANY(?)", [deals].toArray());
        if(!data.isEmpty())
          documentDate = new Timestamp(((java.sql.Date)data[0][0]).getTime());
        break;
    }
    return documentDate;
  }
}

class document {
  Integer id;
  String name;
  String script;
  String scriptLanguage;
  boolean system;
  Owner documentSource;
  Boolean ndsPayer;
  ActionType actionType;
  Boolean movable;
  boolean moneyCash;
  boolean moneyCashLess;
  boolean tmcCash;
  boolean tmcCashLess;
}
