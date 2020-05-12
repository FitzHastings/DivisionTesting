package scripts.groovy.classes

import java.util.*
import bum.interfaces.*
import util.RemoteSession
import util.filter.local.DBFilter

public class DependetDealAction {
  def RemoteSession session
  def dealPositions = []
  def actionType
  def date
  
  public static start(RemoteSession session, dealPositions, actionType, date) {
    if(session != null && dealPositions != null && dealPositions.size() > 0 && actionType != null && date != null) {
      // Для начала упорядочим позиции сделок и получим длительности продуктов
      def deals     = [:]
      def dealDurations = [:]
      def equipments = [:]
      
      session.executeQuery("select [DealPosition(equipment)], [DealPosition(amount)], "
        +"[DealPosition(customProductCost)], [DealPosition(product)] from [DealPosition] where id=ANY(?)",
      [dealPositions.toArray(new Integer[0])].toArray()).each {equipments[it[0]] = it}
      
      session.executeQuery("select [DealPosition(deal)], id, [DealPosition(duration)] from [DealPosition] where id=any(?)", 
        [dealPositions.toArray(new Integer[0])].toArray()).each {
        if(deals[it[0]] == null)
          deals[it[0]] = []
        deals[it[0]] << it[1]
        
        if(dealDurations[it[0]] == null)
          dealDurations[it[0]] = []
        dealDurations[it[0]] << it[2]
      }
      
      // Для каждой сделки получаем её реквизиты
      def dealData = [:]
      session.executeQuery("SELECT id, [Deal(contract)], "
        + "[Deal(sellerCompany)],   [Deal(sellerCompanyPartition)],   [Deal(sellerCfc)],"
        + "[Deal(customerCompany)], [Deal(customerCompanyPartition)], [Deal(customerCfc)] "
        + "FROM [Deal] WHERE id=ANY(?)", [deals.keySet().toArray(new Integer[0])].toArray()).each {dealData[it[0]] = it}
      
      // Для каждой сделки получаем зависимые процессы
      deals.each {deal, dps -> 
        session.executeQuery("SELECT "
          + "[DependentProcess(id)],"
          + "[DependentProcess(subProcess)],"
          + "[DependentProcess(process)], "
          + "[DependentProcess(delay)] "
          + "FROM [DependentProcess] "
          + "WHERE "
          + "[DependentProcess(contractTemplate)]=(SELECT [Contract(template)] FROM [Contract] WHERE id=(SELECT [Deal(contract)] FROM [Deal] WHERE id=?)) AND "
          + "tmp=false AND type='CURRENT' AND "
          + "[DependentProcess(process)]=(SELECT [Deal(service)] FROM [Deal] WHERE id=?) AND "
          + "[DependentProcess(actionType)]=?", [deal, deal, actionType].toArray()).each {
            def dependentProcessId = it[0]
            def subprocessId       = it[1]
            def processId          = it[2]
            def delay              = it[3]
            def contract           = dealData[deal][1]
            def sellerCompany      = dealData[deal][2]
            def sellerPartition    = dealData[deal][3]
            def sellerCfc          = dealData[deal][4]
            def customerCompany    = dealData[deal][5]
            def customerPartition  = dealData[deal][6]
            def customerCfc        = dealData[deal][7]

            def startDate          = getNextStart(date , delay.split(" ")[1], Integer.valueOf(delay.split(" ")[0]))
            def durations          = []
            dealDurations[deal].each {durations << getEndDate(startDate, it.split(" ")[1], Integer.valueOf(it.split(" ")[0]))}
            Collections.sort(durations)
            def endDate            = durations[durations.size()-1]
          
            println date.toString()+" -> "+startDate.toString()+" - "+endDate.toString()
          
            //Проверить не пересекается ли тело создаваемой сделки с телом уже существующей такой же
            DBFilter filter = DBFilter.getFilter(Deal.class)
            filter.AND_EQUAL("type", "CURRENT")
            filter.AND_EQUAL("contract", contract)
            filter.AND_EQUAL("customerCompanyPartition", customerPartition)
            filter.AND_EQUAL("service",  subprocessId)
            
            DBFilter dateFilter = filter.AND_FILTER()
            
            dateFilter.AND_DATE_BEFORE_OR_EQUAL("dealStartDate", startDate)
            dateFilter.AND_DATE_AFTER_OR_EQUAL( "dealEndDate",   startDate)

            dateFilter.OR_DATE_BEFORE_OR_EQUAL("dealStartDate", endDate)
            dateFilter.AND_DATE_AFTER_OR_EQUAL("dealEndDate",   endDate)

            dateFilter.OR_DATE_BEFORE_OR_EQUAL("dealStartDate", startDate)
            dateFilter.AND_DATE_AFTER_OR_EQUAL("dealEndDate",   endDate)

            dateFilter.OR_DATE_AFTER_OR_EQUAL(  "dealStartDate", startDate)
            dateFilter.AND_DATE_BEFORE_OR_EQUAL("dealEndDate",   endDate)

            dateFilter.OR_DATE_EQUAL("dealStartDate", startDate)
            dateFilter.OR_DATE_EQUAL("dealEndDate",   startDate)
            
            def tmpDeal = false
            def newDeal = null;
            session.getData(Deal.class, filter, ["id"] as String[]).each {
              newDeal = it[0]
              session.executeQuery("select [DealPosition(equipment)] from [DealPosition] where [DealPosition(deal)]="+newDeal).each {e -> equipments[e[0]] = null}
            }
            
            if(newDeal == null) {
              // Получить линейку на которой можно сделать сделку
              def contractProcess = null
              session.executeQuery("select id from [ContractProcess] where [ContractProcess(contract)]=? and [ContractProcess(process)]=? and [ContractProcess(customerPartition)]=?",
              [contract, subprocessId, customerPartition] as Object[]).each{contractProcess = it[0]}
              if(contractProcess == null) {
                TreeMap<String,Object> fields = new TreeMap<>();
                fields.put("contract",          contract)
                fields.put("process",           subprocessId)
                fields.put("customerPartition", customerPartition)
                contractProcess = session.createObject(ContractProcess.class, fields)
              }
              if(contractProcess != null) {
                // Создаём сделку
                TreeMap<String,Object> fields = new TreeMap<>()
                fields.put("contract",                 contract)
                fields.put("sellerCompany",            sellerCompany)
                fields.put("sellerCompanyPartition",   sellerPartition)
                fields.put("sellerCfc",                sellerCfc)
                fields.put("customerCompany",          customerCompany)
                fields.put("customerCompanyPartition", customerPartition)
                fields.put("customerCfc",              customerCfc)
                fields.put("service",                  subprocessId)
                fields.put("tempProcess",              contractProcess)
                fields.put("dealStartDate",            new java.sql.Date(startDate.getTime()))
                fields.put("dealEndDate",              new java.sql.Date(endDate.getTime()))
                newDeal = session.createObject(Deal.class, fields)
                tmpDeal = true;
              }
            }
            if(newDeal != null) {
              // Добавляем в сделку позиции, если в ней нет таких позиций
              def startId = 0;
              session.executeQuery("SELECT MAX(id) FROM [DealPosition]").each{startId = it[0]}
              session.executeQuery("SELECT "
                + "id,"
                + "(SELECT [Product(id)] FROM [Product] WHERE [Product(group)]=[Equipment(group)] AND [Product(service)]=? AND [Product(company)]=?  AND [Product(globalProduct)] NOTNULL), "
                + "(SELECT [Product(id)] FROM [Product] WHERE [Product(group)]=[Equipment(group)] AND [Product(service)]=? AND [Product(company)] ISNULL  AND [Product(globalProduct)] ISNULL)"
                + "FROM [Equipment] "
                + "WHERE id IN (SELECT [DealPosition(equipment)] FROM [DealPosition] WHERE [DealPosition(id)]=ANY(?))", 
                [subprocessId,sellerCompany,subprocessId,dps.toArray(new Integer[0])] as Object[]).each {
                  if(it[1] != null || it[2] != null) {
                    if(equipments[it[0]] != null) {
                      def process = it[1]==null?it[2]:it[1]
                      def cost = process.equals(equipments[it[0]][3])?equipments[it[0]][3]:null
                      session.executeUpdate("INSERT INTO [DealPosition]([DealPosition(deal)],[DealPosition(equipment)],[DealPosition(product)],"
                        + "[DealPosition(amount)], [DealPosition(customProductCost)]) "
                        + "VALUES("+newDeal+","+it[0]+","+process+","+equipments[it[0]][1]+","+cost+")")
                    }
                  }
                }
              def dealPos = []
              session.executeQuery("SELECT id FROM [DealPosition] WHERE id>"+startId).each {dealPos << it[0]}
              if(dealPos.size() > 0) {
                session.addEvent(DealPosition.class, "CREATE", dealPos.toArray(new Integer[0]));
                session.addEvent(Deal.class, "UPDATE", [newDeal] as Integer[]);
              }else if(tmpDeal) session.removeObjects(Deal.class, [newDeal] as Integer[])
            }
          }
      }
    }
  }
  
  public static getNextStart(previosDate, rType, rCount) {
    Calendar c = Calendar.getInstance();
    c.setTime(previosDate);
    if(rType.startsWith("м")) {
      c.add(Calendar.MONTH, rCount);
      c.set(Calendar.DAY_OF_MONTH, 1);
      /*c.set(Calendar.DAY_OF_MONTH, 1);
      for(int i=0;i<rCount;i++) {
        if(c.get(Calendar.MONTH)==11) {
          c.set(Calendar.YEAR, c.get(Calendar.YEAR)+1);
          c.set(Calendar.MONTH,0);
        }else  c.set(Calendar.MONTH, c.get(Calendar.MONTH)+1);
      }*/
    }else if(rType.startsWith("д")) {
      c.add(Calendar.DATE, rCount);
      //c.setTimeInMillis(c.getTimeInMillis()+((long)rCount*24*60*60*1000));
    }else if(rType.startsWith("л") || rType.startsWith("г")) {
      c.add(Calendar.YEAR, rCount);
      /*for(int i=0;i<rCount;i++) {
        c.set(c.get(Calendar.YEAR)+1, 0, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
      }*/
    }
    return c.getTime();
  }
  
  public static getEndDate(dealStartDate, type, count) {
    Calendar c = Calendar.getInstance();
    c.setTime(dealStartDate);
    long days = 0;
    if(type.startsWith("м")) {
      c.add(Calendar.MONTH, count);
      /*days = c.getActualMaximum(Calendar.DAY_OF_MONTH) - c.get(Calendar.DAY_OF_MONTH);
      for(int i=0;i<count-1;i++) {
        c.set(Calendar.DAY_OF_MONTH, 1);
        if(c.get(Calendar.MONTH)==11) {
          c.set(Calendar.YEAR, c.get(Calendar.YEAR)+1);
          c.set(Calendar.MONTH,0);
        }else  c.set(Calendar.MONTH, c.get(Calendar.MONTH)+1);
        days += c.getActualMaximum(Calendar.DAY_OF_MONTH);
      }*/
    }else if(type.startsWith("д")) {
      c.add(Calendar.DATE, count);
      //days = count;
    }else if(type.startsWith("л") || type.startsWith("г")) {
      c.add(Calendar.YEAR, count);
      /*days = c.getActualMaximum(Calendar.DAY_OF_YEAR) - c.get(Calendar.DAY_OF_YEAR);
      for(int i=0;i<count-1;i++) {
        c.set(Calendar.YEAR, c.get(Calendar.YEAR)+1);
        days += c.getActualMaximum(Calendar.DAY_OF_MONTH);
      }*/
    }
    //c.setTimeInMillis(dealStartDate.getTime()+(days*24*60*60*1000));
    return c.getTime();
  }
}