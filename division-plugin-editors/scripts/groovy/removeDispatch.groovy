package scripts.groovy

import bum.interfaces.Group.ObjectType
import bum.interfaces.*

session.executeQuery("SELECT "
  /*0*/+ "[DealPosition(id)], "
  /*1*/+ "[DealPosition(sourceStore)], "
  /*2*/+ "[DealPosition(targetStore)], "
  /*3*/+ "[DealPosition(target-store-object-type)], "
  /*4*/+ "[DealPosition(target-store-controll-out)], "
  /*5*/+ "[DealPosition(amount)], "
  /*6*/+ "(SELECT [Store(currency)] FROM [Store] WHERE [Store(id)]=[DealPosition(sourceStore)]) "
  + "FROM [DealPosition] "
  + "WHERE [DealPosition(id)]=ANY(?) AND (SELECT [Service(moveStorePosition)] "
  + "FROM [Service] WHERE [Service(id)]=[DealPosition(process_id)])=true", [dealPositions].toArray()).each {
  def dealPositionId   = it[0]
  def sourceStore      = it[1]
  def targetStore      = it[2]
  def targetObjectType = it[3]!=null ? ObjectType.valueOf(it[3].toString()) : null
  def controllOut      = it[4]
  def amount           = it[5]
  def currency         = it[6]

  if(targetObjectType == ObjectType.ВАЛЮТА) {
    def targetMoneyId  = session.executeQuery("SELECT getFreeMoneyId(?)", [targetStore].toArray())[0][0]

    if((targetMoneyId == null || targetMoneyId <= 0) && !controllOut) {
      session.executeQuery("INSERT INTO [Equipment]([!Equipment(amount)],[!Equipment(store)],[!Equipment(group)]) VALUES(?,?,?)",
              [amount, targetStore, currency].toArray());
      targetMoneyId  = session.executeQuery("SELECT getFreeMoneyId(?)", [targetStore].toArray())[0][0]
    }

    if(targetMoneyId != null && targetMoneyId > 0) {
      def targetMoneyAmount = session.getData(Equipment.class, [targetMoneyId].toArray(new Integer[0]), ["amount"].toArray())[0][0]
      if(targetMoneyAmount.compareTo(amount) < 0 && !controllOut)
        if(session.executeUpdate("UPDATE [Equipment] SET [Equipment(amount)]=? WHERE id=?", [amount,targetMoneyId].toArray()) > 0)
          targetMoneyAmount = amount;
      if(targetMoneyAmount.compareTo(amount) >= 0) {
        session.executeUpdate("UPDATE [Equipment] SET [Equipment(amount)]=[Equipment(amount)]-? WHERE id=?", [amount, targetMoneyId].toArray());
      }else throw new Exception(controllOut ? "Нехватка доступных средств" : "Невероятная ошибка!!!");
    }else throw new Exception(controllOut ? "Нехватка доступных средств" : "Невероятная ошибка!!!");

    session.executeUpdate("INSERT INTO [Equipment]([Equipment(store)],[Equipment(group)],[Equipment(amount)]) "
            + "VALUES(?,?,?)", [sourceStore, currency, amount].toArray());
    session.executeUpdate("UPDATE [DealPosition] SET [DealPosition(equipment)]=(SELECT MAX(id) FROM [Equipment]) WHERE id="+dealPositionId);
    session.executeQuery("SELECT getFreeMoneyId(?)", [sourceStore].toArray());
  }else {
    session.executeUpdate("UPDATE [Equipment] SET "
            + "[Equipment(store)]="+sourceStore+", "
            + "[Equipment(sourceDealPosition)]=(SELECT [DealPosition(equipmentSourceDealPosition)] FROM [DealPosition] WHERE [DealPosition(id)]="+dealPositionId+") "
            + "WHERE [Equipment(id)]=(SELECT [DealPosition(equipment)] FROM [DealPosition] WHERE [DealPosition(id)]="+dealPositionId+")");
  }
}
