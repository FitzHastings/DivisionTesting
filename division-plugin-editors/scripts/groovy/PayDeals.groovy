package scripts.groovy

import bum.interfaces.*
import bum.interfaces.ProductDocument.ActionType
import bum.editors.util.*;
import java.rmi.RemoteException
import util.RemoteSession
import division.swing.guimessanger.Messanger
import org.apache.commons.lang.ArrayUtils
import scripts.groovy.classes.*
import division.*;
import division.util.*
import division.fx.Processing
import division.fx.DivisionProcess
import util.filter.local.DBFilter

//final payment                = paymentId
//final amount_                = amount
//final dealAmounts_           = dealAmounts
//final sellerStore_           = sellerStore
//final customerStore_         = customerStore
//final actionDocumentStarter_ = actionDocumentStarter

//Processing.submit(new DivisionProcess(ClientMain.getInstance(),"Создание документов...",false) {
  //public void run() {
    //setVisible(true)
    //setText("Подготовка...")
    //setValue(-1.0f)
    
    // Выделяем сделки
    //def deals = dealAmounts_.keySet().toArray(new Integer[0])
    
    //def dealPositions = []
    //ObjectLoader.getData(new DBFilter(DealPosition.class).AND_IN("deal", deals), "id").each { dealPositions << it[0] }

    //def document_seller_customer_contract_deal_dps = [:]
    //def documents = [:]
    //def companyNames = [:]
    //if(DocumentsCreater.setDocumentDates(this, ActionType.ОПЛАТА, dealPositions.toArray(new Integer[0]), sellerStore_, customerStore_,
        //document_seller_customer_contract_deal_dps, documents, companyNames)) {
      //def session = null;
      //try {
        //session = ObjectLoader.createSession();
        
        //def map = [tmp:false, amount:amount_, sellerStore:sellerStore_, customerStore:customerStore_]
        //if(session.saveObject(Payment.class, payment, map)) {
          // Удалить старые оплаты
          //def querys = ["DELETE FROM [DealPayment] WHERE [DealPayment(payment)]="+paymentId]
          // Создать оплаты
          //dealAmounts.each{key,value -> querys << "INSERT INTO [DealPayment]([DealPayment(deal)], [DealPayment(payment)], [DealPayment(amount)]) VALUES("+key +","+paymentId+","+value+")"}
          //session.executeUpdate(querys.toArray(new String[0]));

          // Переносим деньги
          //moveMoneyStorePosition(paymentId, session);

          /*DocumentsCreater.run(
            this,
            ActionType.ОПЛАТА, 
            session, 
            dealPositions.toArray(new Integer[0]), 
            payment, 
            null,
            [:],
            document_seller_customer_contract_deal_dps,
            documents,
            companyNames)*/
            
          //actionDocumentStarter.start(session, paymentId, [:])

          //session.addEvent(Payment.class, "UPDATE", [paymentId].toArray(new Integer[0]));
          //session.addEvent(Deal.class, "UPDATE", dealAmounts.keySet().toArray(new Integer[0]));
        //}
        //session.commit()
      /*}catch(Exception ex) {
        ObjectLoader.rollBackSession(session)
        Messanger.showErrorMessage(ex)
      }finally {
        dispose()
      }*/
    //}
  //}
  
  /*def moveMoneyStorePosition(Integer paymentId, RemoteSession session) {
    def data = session.executeQuery("SELECT "
      //*0*/+ "[Payment(amount)], "
      //*1*/+ "[Payment(customer-store-controll-out)], "
      //*2*/+ "[Payment(sellerCompanyPartition)],"
      //*3*/+ "[Payment(sellerStore)], "
      //*4*/+ "[Payment(customerStore)], "
      //*5*/+ "[Payment(customerCompanyPartition)] "
      //+ "FROM [Payment] WHERE [Payment(id)] = "+paymentId)
    /*if(!data.isEmpty()) {
      BigDecimal amount              = data[0][0]
      boolean    storeControllOut    = data[0][1]
      Integer    seller              = data[0][2]
      Integer    sellerStore         = data[0][3]
      Integer    customerStore       = data[0][4]
      Integer    customer            = data[0][5]
      Integer    sellerMoneyId       = session.executeQuery("SELECT getFreeMoneyId(?)",[sellerStore].toArray())[0][0]
      Integer    customerMoneyId     = session.executeQuery("SELECT getFreeMoneyId(?)",[customerStore].toArray())[0][0]
      BigDecimal customerMoneyAmount = session.executeQuery("SELECT [Equipment(amount)] FROM [Equipment] WHERE id=?",[customerMoneyId].toArray())[0][0]

      if(customerMoneyAmount.compareTo(amount) >= 0 || !storeControllOut) {
        session.executeUpdate("UPDATE [Equipment] SET [Equipment(amount)]=[Equipment(amount)]-? WHERE [Equipment(id)]=?", [amount, customerMoneyId].toArray())
        session.executeUpdate("UPDATE [Equipment] SET [Equipment(amount)]=[Equipment(amount)]+? WHERE [Equipment(id)]=?", [amount, sellerMoneyId].toArray())
      }else throw new RemoteException("Нехватка средств");
    }
  }*/