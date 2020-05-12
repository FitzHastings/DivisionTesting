package scripts.groovy

import javax.swing.JOptionPane
import util.RemoteSession
import util.filter.local.DBFilter
import bum.interfaces.*
import division.editors.tables.DealPositionTableEditor
import division.swing.LocalProcessing
import bum.editors.util.*
import division.swing.guimessanger.*
import division.swing.guimessanger.Messanger
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.sql.Timestamp
import java.util.AbstractMap.SimpleEntry
import javax.swing.JButton
import bum.interfaces.ProductDocument.ActionType
import bum.interfaces.Store.StoreType
import bum.interfaces.Group.ObjectType
import org.apache.commons.lang.ArrayUtils
import scripts.groovy.classes.*
import division.util.*
import division.*;
import division.fx.Processing
import division.fx.DivisionProcess
import division.util.actions.ActionDocumentStarter
import javafx.application.Platform;


//final deals = deals
//final all_ = all

//Processing.submit(new DivisionProcess(ClientMain.getInstance(),"Создание документов...",false) {
  //public void run() {
    //setVisible(true)
    //setText("Подготовка...")
    //setValue(-1.0f)
    
    try {
println "0"
      //Получить неотгруженные и стартонувшие позиции из сделок
      Vector<Vector> data = ObjectLoader.getData(new DBFilter(DealPosition.class).AND_IN("deal", deals).AND_EQUAL("dispatchDate", null).AND_NOT_EQUAL("startDate", null),
        "id","deal","amount","target-store-type","target-store-object-type")
println "1"
      if(data.isEmpty())
        Messanger.alert("Данн"+(deals.length==1?"ая":"ые")+" сделк"+(deals.length==1?"а":"и")+" полностью отгружен"+(deals.length==1?"а":"ы"), "тук тук", JOptionPane.INFORMATION_MESSAGE);
      else {
        def dealPositions = [];
        data.each{
          dealPositions << new DP(
            id:it[0],
            amount:it[2],
            dispatchAmount:it[2],
            storeType:it[3]!=null?StoreType.valueOf(it[3]):null,
            storeObjectType:it[4]!=null?ObjectType.valueOf(it[4]):null)}

        // Если отгрузка только одной сделки, то нужно выбрать какие позиции и сколько отгружать
        if(deals.length == 1 && !all)
          getDealPositionsAmountToDispatch(dealPositions)

        if(!dealPositions.isEmpty()) {
          Platform.runLater {
            def session = null;
            try {
              def dpToDispatch = []
              dealPositions.each{dp -> dpToDispatch << dp.getId()}

              def actionDocumentStarter = new ActionDocumentStarter();
              actionDocumentStarter.init(ProductDocument.ActionType.ОТГРУЗКА, dpToDispatch.toArray(new Integer[0]))

              actionDocumentStarter.getSellerSystemDocuments().each {k,v ->
                println v.name+" "+v.customDate
              }

              if(actionDocumentStarter.customUpdateDocuments(deals.length > 1)) {
                session = ObjectLoader.createSession()
                dpToDispatch.clear()
                dealPositions.each{dp ->
                  if(dp.getDispatchAmount().compareTo(dp.getAmount()) < 0) {

                    session.executeQuery("SELECT "
                      +"[DealPosition(equipment)],"
                      +"[DealPosition(deal)],"
                      +"[DealPosition(product)],"
                      +"[DealPosition(customProductCost)],"
                      +"[DealPosition(startDate)],"
                      +"[DealPosition(startId)] "
                      +"FROM [DealPosition] WHERE id="+dp.getId()).each {
                        session.executeUpdate("INSERT INTO [DealPosition] ("
                          +"[DealPosition(equipment)],"
                          +"[DealPosition(deal)],"
                          +"[DealPosition(product)],"
                          +"[DealPosition(customProductCost)],"
                          +"[DealPosition(startDate)],"
                          +"[DealPosition(startId)]) VALUES ("+it[0]+","+it[1]+","+it[2]+","+it[3]+",'"+it[4]+"',"+it[5]+")")

                          def newDpId = session.executeQuery("SELECT MAX(id) FROM [DealPosition]")[0][0]
                          session.executeUpdate("UPDATE [DealPosition] SET [DealPosition(amount)]="+dp.getDispatchAmount()+" WHERE id="+newDpId)
                          session.executeUpdate("UPDATE [DealPosition] SET [DealPosition(amount)]="+dp.getAmount().subtract(dp.getDispatchAmount())+" WHERE id="+dp.getId())

                          session.executeQuery("SELECT [CreatedDocument(dealPositions):object] FROM [CreatedDocument(dealPositions):table] "
                            + "WHERE [CreatedDocument(dealPositions):target]="+dp.getId()).each{d -> 
                            session.executeUpdate("INSERT INTO [CreatedDocument(dealPositions):table] ("
                              + "[CreatedDocument(dealPositions):object],"
                              + "[CreatedDocument(dealPositions):target]"
                              + ") VALUES ("+d[0]+","+newDpId+")")
                          }

                        session.addEvent(DealPosition.class, "CREATE", [newDpId].toArray(new Integer[0]))
                        session.addEvent(DealPosition.class, "UPDATE", [dp.getId()].toArray(new Integer[0]))
                        dp.setId(newDpId);
                        dp.setAmount(dp.getDispatchAmount())
                      }
                  }
                  dpToDispatch << dp.getId()
                }
                moveTMCStorePosition(dpToDispatch.toArray(new Integer[0]), session)

                def createdDocuments = actionDocumentStarter.start(session, null, [:])

                println "createdDocuments = "+createdDocuments

                def querys = [];
                def params = [];

                def dispatchId = session.executeQuery("SELECT MAX([DealPosition(dispatchId)])+1 FROM [DealPosition]")[0][0];

                /*createdDocuments.each {dp,docs -> 
                  querys << "UPDATE [DealPosition] SET \
                    [DealPosition(dispatchId)]=?, \
                    [DealPosition(dispatchDate)]=CURRENT_TIMESTAMP, \
                    [DealPosition(actionDispatchDate)]=\
                      (CASE WHEN (SELECT MAX([CreatedDocument(date)]) FROM [CreatedDocument] WHERE id=ANY(?)) ISNULL \
                      THEN CURRENT_DATE ELSE (SELECT MAX([CreatedDocument(date)]) FROM [CreatedDocument] WHERE id=ANY(?)) END) \
                    WHERE [DealPosition(id)]=?"
                  params << [dispatchId, docs.toArray(new Integer[0]), docs.toArray(new Integer[0]), dp].toArray()
                }*/
                
                dpToDispatch.each {dp ->
                  if(createdDocuments.get(dp) != null) {
                    querys << "UPDATE [DealPosition] SET \
                      [DealPosition(dispatchId)]=?, \
                      [DealPosition(dispatchDate)]=CURRENT_TIMESTAMP, \
                      [DealPosition(actionDispatchDate)]=\
                        (CASE WHEN (SELECT MAX([CreatedDocument(date)]) FROM [CreatedDocument] WHERE id=ANY(?)) ISNULL \
                        THEN CURRENT_DATE ELSE (SELECT MAX([CreatedDocument(date)]) FROM [CreatedDocument] WHERE id=ANY(?)) END) \
                      WHERE [DealPosition(id)]=?"
                    params << [dispatchId, createdDocuments.get(dp).toArray(new Integer[0]), createdDocuments.get(dp).toArray(new Integer[0]), dp].toArray()
                  }else {
                    querys << "UPDATE [DealPosition] SET \
                      [DealPosition(dispatchId)]=?, \
                      [DealPosition(dispatchDate)]=CURRENT_TIMESTAMP, \
                      [DealPosition(actionDispatchDate)]=CURRENT_TIMESTAMP\
                      WHERE [DealPosition(id)]=?"
                    params << [dispatchId, dp].toArray()
                  }
                }

                session.executeUpdate(querys.toArray(new String[0]), (Object[][])params.toArray(new Object[0]))

                session.addEvent(DealPosition.class, "UPDATE", dpToDispatch.toArray(new Integer[0]));
                session.addEvent(Deal.class, "UPDATE", deals);
                session.commit();
              }
            }catch(Exception ex) {
              ObjectLoader.rollBackSession(session)
              Messanger.showErrorMessage(ex)
            }
          }
        }
      }
    }catch(Exception ex) {
      //ObjectLoader.rollBackSession(session)
      Messanger.showErrorMessage(ex)
    }//finally {
      //dispose()
    //}
  //}
  
  def getDealPositionsAmountToDispatch(dealPositions) {
    JButton dispatchAll = new JButton("Отгрузить всё >>>>")
    DealPositionTableEditor dealPositionTableEditor = new DealPositionTableEditor()
    dispatchAll.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          dealPositionTableEditor.setAllSelected(true)
          dealPositionTableEditor.dispose()
        }
      })

    dealPositionTableEditor.setAdministration(false)
    dealPositionTableEditor.getOkButton().setText("Отгрузить >>")
    dealPositionTableEditor.getButtonsPanel().add(dispatchAll)
    dealPositionTableEditor.setDeals(deals)
    dealPositionTableEditor.setOnlyNotDispatch(true)
    dealPositionTableEditor.setVisibleSelectCountColumn(true)
    dealPositionTableEditor.setAutoLoadAndStore(true)
    dealPositionTableEditor.initData()
    Map<Integer, BigDecimal> data = dealPositionTableEditor.get()
    for(int i=dealPositions.size()-1;i>=0;i--) {
      if(!data.containsKey(dealPositions[i].getId())) {
        dealPositions.remove(i)
      }else dealPositions[i].setDispatchAmount(data.get(dealPositions[i].getId()))
    }
  }
  
  def moveTMCStorePosition(Integer[] dealPositions, RemoteSession session) {
    Vector<Vector> data = session.executeQuery("SELECT "
            /*0*/+ "id, "
            /*1*/+ "[DealPosition(equipment)], "
            /*2*/+ "[DealPosition(equipment_amount)], "
            /*3*/+ "[DealPosition(amount)], "
            /*4*/+ "[DealPosition(sourceStore)], "
            /*5*/+ "[DealPosition(targetStore)], "
            /*6*/+ "(SELECT [Deal(customerCompanyPartition)] FROM [Deal] WHERE id=[DealPosition(deal)]), "
            /*7*/+ "(SELECT [Equipment(group)] FROM [Equipment] WHERE [Equipment(id)]=[DealPosition(equipment)]), "
            /*8*/+ "[DealPosition(source-store-controll-out)] "
            + "FROM [DealPosition] "
            + "WHERE (SELECT [Service(moveStorePosition)] FROM [Service] WHERE [Service(id)]=(SELECT [Product(service)] FROM [Product] WHERE [Product(id)]=[DealPosition(product)]))=true AND "
            + "[DealPosition(sourceStore)] NOTNULL AND [DealPosition(targetStore)] NOTNULL AND "
            + "[DealPosition(sourceStore)] != [DealPosition(targetStore)] AND id=ANY(?)",[dealPositions].toArray());
    if(!data.isEmpty()) {
      BigDecimal equipAmount = null;
      BigDecimal dealPositionAmount = null;
      def querys = [];
      Object[][] param = new Object[0][];
      data.each {
        equipAmount        = (BigDecimal) it.get(2);
        dealPositionAmount = (BigDecimal) it.get(3);
        if(dealPositionAmount.compareTo(equipAmount) == 0) {
          querys << "UPDATE [Equipment] SET [Equipment(store)]=?, [Equipment(sourceDealPosition)]=? WHERE id=?"
          param  = (Object[][]) ArrayUtils.add(param, [it[5], it[0], it[1]].toArray());
        }else {
          querys << "UPDATE [Equipment] SET [Equipment(amount)]=? WHERE id=?"
          param  = (Object[][]) ArrayUtils.add(param, [equipAmount.subtract(dealPositionAmount), it[1]].toArray());
          querys << "INSERT INTO [Equipment] (\n\
                    [Equipment(store)], \n\
                    [Equipment(group)], \n\
                    [Equipment(amount)], \n\
                    [Equipment(sourceDealPosition)] \n\
                    ) VALUES (?,?,?,?)"
          param  = (Object[][]) ArrayUtils.add(param, [it[5],it[7],dealPositionAmount,it[0]].toArray());

          querys << "UPDATE [DealPosition] SET [DealPosition(equipment)]=(SELECT MAX(id) FROM [Equipment]) WHERE [DealPosition(id)]=?"
          param  = (Object[][]) ArrayUtils.add(param, [it[0]].toArray());
        }
      }

      session.executeUpdate(querys.toArray(new String[0]), param);
    }
  }
//})

class DP {
  Integer    id;
  BigDecimal amount;
  BigDecimal dispatchAmount;
  StoreType  storeType;
  ObjectType storeObjectType;
  boolean movable;
  boolean ndsPayer;
  
  public String toString() {
    return id+":"+amount+":"+dispatchAmount;
  }
}