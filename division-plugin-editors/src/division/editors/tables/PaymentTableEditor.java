package division.editors.tables;

import bum.editors.TableEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Deal;
import bum.interfaces.DealPayment;
import bum.interfaces.Payment;
import division.editors.objects.nPaymentEditor;
import division.fx.controller.payment.FXPayment_1;
import division.swing.guimessanger.Messanger;
import division.util.Utility;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.util.List;
import java.util.Vector;
import javafx.application.Platform;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import mapping.MappingObject.RemoveAction;
import org.apache.commons.lang.ArrayUtils;
import util.filter.local.DBFilter;

public class PaymentTableEditor extends TableEditor {
  private final JLabel amountLabel = new JLabel("Сумма: ");
  private final JLabel noRaspLabel = new JLabel("Нераспред. средства: ");

  public PaymentTableEditor() {
    super(new String[]{"id","Получатель","Плательщик"/*,"Номер платёжного документа","Дата операции"*/,"Сумма","Нераспред. средства"},
            new String[]{"id","seller","customer"/*,"document.number","operationDate"*/,"amount","notDistribAmount"/*,"date"*/},
            Payment.class, null, "Платежи");
    
    
    
    setAddFunction(false);
    setEditFunction(true);
    
    getStatusBar().add(amountLabel);
    getStatusBar().add(noRaspLabel);
    
    getTable().getTableFilters().addTextFilter(1);
    getTable().getTableFilters().addTextFilter(2);
    getTable().getTableFilters().addTextFilter(3);
    getTable().getTableFilters().addDateFilter(4);
    getTable().getTableFilters().addNumberFilter(5);
    getTable().getTableFilters().addNumberFilter(6);

    setRemoveActionType(RemoveAction.MARK_FOR_DELETE);
    setRemoveAction(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          DBFilter filter = DBFilter.create(DealPayment.class);
          filter.AND_IN("payment", getSelectedId());
          Integer[] ds = new Integer[0];
          for(Object v:ObjectLoader.getData(filter, new String[]{"deal"}))
            ds = (Integer[])ArrayUtils.add(ds,((Vector)v).get(0));
          if(removeButtonAction())
            ObjectLoader.sendMessage(Deal.class, "UPDATE", ds);
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
    
    getTable().getSorter().addRowSorterListener(new RowSorterListener() {
      @Override
      public void sorterChanged(RowSorterEvent e) {
        recalculate();
      }
    });
    
    getTable().getTableModel().addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(TableModelEvent e) {
        recalculate();
      }
    });
  }
  
  private void recalculate() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        BigDecimal amount = new BigDecimal(0.0);
        BigDecimal noRasp = new BigDecimal(0.0);
        for(int i=0;i<getTable().getRowCount();i++) {
          //amount = amount.add((BigDecimal) getTable().getValueAt(i, 5));
          //noRasp = noRasp.add((BigDecimal)getTable().getValueAt(i, 6));
          amount = amount.add((BigDecimal) getTable().getValueAt(i, 3));
          noRasp = noRasp.add((BigDecimal)getTable().getValueAt(i, 4));
        }
        
        amountLabel.setText("Сумма: "+Utility.doubleToString(amount, 2));
        noRaspLabel.setText("Нераспред. средства: "+Utility.doubleToString(noRasp, 2));
      }
    });
  }

  @Override
  protected void insertData(List<List> data, int startIndex) {
    /*for(Vector d:data) {
      Object date = d.remove(7);
      if(d.get(4) == null)
        d.set(4, date);
    }*/
    super.insertData(data, startIndex);
  }
}