package scripts.groovy

import javax.swing.JOptionPane
import util.RemoteSession
import util.filter.local.DBFilter
import bum.interfaces.*;
import division.editors.tables.DealPositionTableEditor
import division.swing.LocalProcessing
import bum.editors.util.*;
import division.swing.guimessanger.*;
import division.swing.guimessanger.Messanger
import java.util.AbstractMap.SimpleEntry
import bum.interfaces.ProductDocument.ActionType;
import bum.interfaces.Store.StoreType;
import bum.interfaces.Group.ObjectType;
import scripts.groovy.classes.*
import division.*;
import division.util.*
import division.util.actions.ActionDocumentStarter
import division.fx.Processing
import division.fx.DivisionProcess
import javafx.application.Platform;
import javax.swing.SwingUtilities;

Platform.runLater {
  //Получить не стартонувшие позиции из сделок
  Vector<Vector> data = ObjectLoader.getData(new DBFilter(DealPosition.class).AND_IN("deal", deals).AND_EQUAL("startDate", null), 
    "id","deal","amount");

  println 1+" data = "+data.size()

  if(data.isEmpty()) {
    JOptionPane.showMessageDialog(null, "Данн"+(deals.length==1?"ая":"ые")+" сделк"+(deals.length==1?"а":"и")+" полностью стартонул"+(deals.length==1?"а":"и"), "тук тук", JOptionPane.INFORMATION_MESSAGE)
  }else {
    println 2

    def session = null;
    try {
      def dealPositions = []
      data.each {dealPositions << it.get(0)}

      println "dealPositions = "+dealPositions

      def actionDocumentStarter = new ActionDocumentStarter();
      actionDocumentStarter.init(ProductDocument.ActionType.СТАРТ, dealPositions.toArray(new Integer[0]))
      if(actionDocumentStarter.customUpdateDocuments(deals.length > 1)) {
        session = ObjectLoader.createSession();
        session.executeUpdate("UPDATE [DealPosition] SET \n\
          [DealPosition(startId)]=(SELECT MAX([DealPosition(startId)])+1 FROM [DealPosition]), \n\
          [DealPosition(startDate)]=CURRENT_TIMESTAMP WHERE [DealPosition(id)]=ANY(?)", [dealPositions.toArray(new Integer[0])].toArray());
        println "START START START "
        actionDocumentStarter.start(session, null, [:])
        session.addEvent(DealPosition.class, "UPDATE", dealPositions.toArray(new Integer[0]));
        session.addEvent(Deal.class, "UPDATE", deals);
        session.commit();
      }
    }catch(Exception ex) {
      ObjectLoader.rollBackSession(session)
      Messanger.showErrorMessage(ex)
    }
  }
}