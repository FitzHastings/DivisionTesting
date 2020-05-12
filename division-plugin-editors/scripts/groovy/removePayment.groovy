package scripts.groovy

import java.rmi.RemoteException

session.executeQuery("SELECT "
  /*0*/+ "[Payment(amount)], "
  /*1*/+ "[Payment(seller-store-controll-out)], "
  /*2*/+ "[Payment(sellerCompanyPartition)],"
  /*3*/+ "[Payment(sellerStore)], "
  /*4*/+ "[Payment(customerStore)], "
  /*5*/+ "[Payment(customerCompanyPartition)] "
  + "FROM [Payment] WHERE [Payment(id)]=ANY(?)", [payments.toArray(new Integer[0])].toArray()).each {
  
    BigDecimal amount              = it[0]
    boolean    storeControllOut    = it[1]
    Integer    seller              = it[2]
    Integer    sellerStore         = it[3]
    Integer    customerStore       = it[4]
    Integer    customer            = it[5]
    Integer    sellerMoneyId       = session.executeQuery("SELECT getFreeMoneyId(?)",[sellerStore].toArray())[0][0]
    Integer    customerMoneyId     = session.executeQuery("SELECT getFreeMoneyId(?)",[customerStore].toArray())[0][0]
    BigDecimal sellerMoneyAmount   = session.executeQuery("SELECT [Equipment(amount)] FROM [Equipment] WHERE id=?",[sellerMoneyId].toArray())[0][0]
    
    if(sellerMoneyAmount.compareTo(amount) >= 0 || !storeControllOut) {
      session.executeUpdate("UPDATE [Equipment] SET [Equipment(amount)]=[Equipment(amount)]-? WHERE [Equipment(id)]=?", [amount, sellerMoneyId].toArray())
      session.executeUpdate("UPDATE [Equipment] SET [Equipment(amount)]=[Equipment(amount)]+? WHERE [Equipment(id)]=?", [amount, customerMoneyId].toArray())
    }else throw new RemoteException("Нехватка средств");
  }

session.executeUpdate("UPDATE [Payment] SET type='ARCHIVE' WHERE id=ANY(?)", [payments.toArray(new Integer[0])].toArray());
