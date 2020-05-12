package division.editors.tables;

import bum.editors.TableEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.CreatedDocument;
import bum.interfaces.DocumentXMLTemplate;
import bum.interfaces.Payment;
import division.ClientMain;
import division.exportimport.ExportImportUtil;
import division.fx.PropertyMap;
import division.fx.controller.payment.FXPayment_1;
import division.mail.EmailListener;
import division.swing.DistanceTime;
import division.swing.guimessanger.Messanger;
import division.swing.DivisionToolButton;
import division.swing.table.filter.FilterCalendar;
import division.swing.table.filter.TableFilter;
import division.util.FileLoader;
import division.util.DocumentUtil;
import division.util.Utility;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.print.PrinterJob;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javafx.application.Platform;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import mapping.MappingObject;
import org.apache.commons.lang.ArrayUtils;
import util.filter.local.DBFilter;

public class CreatedDocumentTableEditor extends TableEditor {
  private final JMenuItem          exportPopMenu   = new JMenuItem("Экспорт");
  private final JMenuItem          emailPopMenu    = new JMenuItem("Отправить по Email");
  private final JMenuItem          changeDocumentNumber = new JMenuItem("Изменить номер документа");
  private final DivisionToolButton preview         = new DivisionToolButton(FileLoader.getIcon("preview.gif"), "Просмотр");
  private final DivisionToolButton printComplect   = new DivisionToolButton("Печать комплектами", "Печать комплектами");
  private final DivisionToolButton openPayment     = new DivisionToolButton("Открыть платёж", "Открыть платёж");
  //private final DivisionToolButton sendEmail       = new DivisionToolButton(FileLoader.getIcon("email-send.png"), "Отправить по Электронной почте");
  
  private static final int DOCUMENT_COLUMN      = 6;
  private static final int DOCUMENT_NAME_COLUMN = 7;
  private static final int PAYMENT_COLUMN       = 8;
  
  private TableDateFilter tableDateFilter;
  private DBFilter dateFilter    = getClientFilter().AND_FILTER();
  private DBFilter comapnyFilter = getClientFilter().AND_FILTER();
  
  public CreatedDocumentTableEditor() {
    super(
            new String[]{"id","Документ","Номер","Дата документа","Исправлен","Корреспондент","documentId","documentName","payment"},
            new String[]{
              /*0*/"id",
              /*1*/"name",
              /*2*/"number",
              /*3*/"date",
              /*4*/"stornoDate",
              /*5*/"query:getCompanyName((SELECT [CompanyPartition(company)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[CreatedDocument(customerCompanyPartition)]))",
              /*6*/"document",
              /*7*/"document_name",
              /*8*/"payment"
              },
            CreatedDocument.class, null, "Документы", MappingObject.Type.CURRENT);
    setSortFields("date");
    initComponents();
    initEvents();
  }
  
  public void clearDateFilter() {
    tableDateFilter.clearFilter();
  }
  
  public void setDefailtDateFilter(long startTime, long endTime) {
    tableDateFilter.setFilter(new DistanceTime(startTime, endTime));
  }
  
  private void initComponents() {
    getTable().setColumnWidthZero(0,4,PAYMENT_COLUMN,DOCUMENT_COLUMN,DOCUMENT_NAME_COLUMN);
    
    getTable().getTableFilters().addListFilter(1,DOCUMENT_NAME_COLUMN);
    getTable().getTableFilters().addTextFilter(2);
    
    tableDateFilter = new TableDateFilter(getTable().getTableFilters(), 3);
    getTable().getTableFilters().addFilter(3, tableDateFilter);
    
    Calendar c = Calendar.getInstance();
    c.set(c.get(Calendar.YEAR)-1, c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
    c.set(Calendar.MILLISECOND, 0);
    setDefailtDateFilter(c.getTimeInMillis(), System.currentTimeMillis());
    
    getTable().getTableFilters().addTextFilter(2);
    getTable().getTableFilters().addTextFilter(5);
    getTable().getTableFilters().addNumberFilter(9);
    
    setRemoveActionType(MappingObject.RemoveAction.MOVE_TO_ARCHIVE);
    getToolBar().add(preview);
    getToolBar().addSeparator();
    getToolBar().add(printComplect);
    getToolBar().addSeparator();
    getToolBar().add(openPayment);
    
    getPopMenu().addSeparator();
    getPopMenu().add(exportPopMenu);
    getPopMenu().addSeparator();
    getPopMenu().add(emailPopMenu);
    getPopMenu().addSeparator();
    getPopMenu().add(changeDocumentNumber);
    
    //openPayment.setEnabled(false);
    
    getTable().setCellFontController((JTable table1, int modelRow, int modelColumn, boolean isSelect, boolean hasfocus) -> {
      Font f = table1.getFont();
      if(table1.getModel().getValueAt(modelRow, 4) != null) {
        Map attr = f.getAttributes();
        attr.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        return new Font(attr);
      }
      return null;
    });
  }
  
  private void initEvents() {
    changeDocumentNumber.addActionListener(e -> {
      PropertyMap p = ObjectLoader.getMap(CreatedDocument.class, getSelectedId()[0], "id","name","document_name","number","date","stopdate=query:(select [CompanyPartition(docStopDate)] from [CompanyPartition] where id=[CreatedDocument(sellerCompanyPartition)])");
      p.setValue("date", Utility.convert(p.getTimestamp("date")));
      if(!p.isNull("stopdate") && p.getValue("stopdate") instanceof Date)
        p.setValue("stopdate", Utility.convert(p.getDate("stopdate")));
      if(!p.isNull("stopdate") && (p.getLocalDate("date").isBefore(p.getLocalDate("stopdate")) || p.getLocalDate("date").isEqual(p.getLocalDate("stopdate"))))
        JOptionPane.showMessageDialog(this.getRootPanel(), "Дата документа выходит за пределы разрешённой для редактирования документа", "Ошибка", JOptionPane.ERROR_MESSAGE);
      else {
        String number = JOptionPane.showInputDialog("Замена номера документа \""+p.getString("name")+"\":", p.getString("number"));
        if(number != null) {
          try {
            ObjectLoader.update(CreatedDocument.class, p.setValue("name", p.isNull("number") ? (p.getString("name").replaceAll(p.getString("document_name"), p.getString("document_name")+" № "+number)) : p.getString("name").replaceAll(p.getString("number"), number)).remove("date","stopdate","document_name").setValue("number", number));
          }catch (Exception ex) {
            Messanger.showErrorMessage(ex);
          }
        }
      }
    });
    
    getPopMenu().addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        changeDocumentNumber.setEnabled(getSelectedObjectsCount() == 1);
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
      }
    });
    
    emailPopMenu.addActionListener(e -> {
      try {
        DocumentUtil.sendEmail(getSelectedId(), new EmailListener() {
          @Override
          public void documentsSent(Integer[] ids) {
            String[] querys = new String[0];
            Object[][] params = new Object[0][0];
            for(Integer id:ids) {
              querys = (String[]) ArrayUtils.add(querys, "INSERT INTO [Comment] ("
                      + "[!Comment(sourceClass)],[!Comment(sourceId)],[!Comment(subject)],[!Comment(text)],[!Comment(worker)]) VALUES (?,?,?,?,?)");
              params = (Object[][]) ArrayUtils.add(params, 
                      new Object[]{CreatedDocument.class.getSimpleName(),id,"Отправка по Email","Документ отправлен на email организации",ObjectLoader.getClient().getWorkerId()});
            }
            try {
              ObjectLoader.executeUpdate(querys, params);
              ObjectLoader.sendMessage(CreatedDocument.class, "UPDATE", ids);
            }catch(Exception ex) {
              Messanger.showErrorMessage(ex);
            }
          }
        });
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    });
    
    /*sendEmail.addActionListener((ActionEvent e) -> {
    });*/
    
    openPayment.addActionListener((ActionEvent e) -> {
      if(getTable().getSelectedRowCount() > 0) {
        Object paymentId = getTable().getValueAt(getTable().getSelectedRow(), PAYMENT_COLUMN);
        if(paymentId != null) {
          Platform.runLater(() -> {
            try {
              FXPayment_1 payment = new FXPayment_1();
              payment.setObjectProperty(ObjectLoader.getMap(Payment.class, (Integer)paymentId));
              payment.showAndWait();
              //new PaymentController((Integer) paymentId).show();
              /*Payment payment = (Payment) ObjectLoader.getObject(Payment.class, (Integer) paymentId);
              nPaymentEditor editor = new nPaymentEditor();
              editor.setEditorObject(payment);
              editor.setAutoLoad(true);
              editor.setAutoStore(true);
              editor.createDialog(CreatedDocumentTableEditor.this, true).setVisible(true);*/
            }catch(Exception ex) {
              Messanger.showErrorMessage(ex);
            }
          });
        }
      }
    });
    
    exportPopMenu.addActionListener((ActionEvent e) -> exportButtonAction());
    
    printComplect.addActionListener(e -> printPrivew(e, true,true));
    preview.addActionListener(e -> printPrivew(e, false,false));
    setPrintAction(e -> printPrivew(e,true,false));
    
    getTable().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 2 && e.getModifiers() != MouseEvent.META_DOWN_MASK)
          printPrivew(e, false,false);
      }
    });
    
    ClientMain.addClientMainListener(this);
  }
  
  @Override
  public void changedCompany(Integer[] ids) {
    comapnyFilter.clear();
    comapnyFilter.AND_IN("seller", ids);
    comapnyFilter.OR_IN("customer", ids);
    initData();
  }

  @Override
  public boolean removeButtonAction() {
    boolean returnValue = false;
    if(isEditable()) {
      Integer[] ids = this.getSelectedId();
      if(ids.length > 0) {
        if(JOptionPane.showConfirmDialog(
          getRootPanel(),
          "<html>Вы уверены в том что хотите <b>сторнировать</b> выделенны"+(ids.length>1?"е":"й")+" документ"+(ids.length>1?"ы":"")+"?</html>",
          "Подтверждение удаления",
          JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE) == 0) {
          try {
            if(ObjectLoader.executeUpdate("UPDATE [CreatedDocument] SET [CreatedDocument(stornoDate)]=CURRENT_TIMESTAMP WHERE id=ANY(?)", true, new Object[]{ids}) > 0)
              ObjectLoader.sendMessage(CreatedDocument.class, "UPDATE", ids);
            returnValue = true;
          }catch(Exception e) {
            Messanger.showErrorMessage(e);
            returnValue = false;
          }
        }else returnValue = false;
      }
    }
    return returnValue;
  }

  @Override
  public void exportButtonAction() {
    export(getSelectedId());
  }
  
  private void export(Integer[] ids) {
    try {
      List<List> data = ObjectLoader.executeQuery("SELECT [CreatedDocument(id)],[CreatedDocument(sellerCompanyPartition)] FROM [CreatedDocument] "
              + "WHERE [CreatedDocument(stornoDate)] ISNULL AND [CreatedDocument(tmp)]=false AND [CreatedDocument(type)]='CURRENT' AND [CreatedDocument(id)]=ANY(?) ORDER BY [CreatedDocument(sellerCompanyPartition)]", true, new Object[]{ids});
      TreeMap<Integer,java.util.List<Integer>> documents = new TreeMap<>();
      data.stream().forEach((d) -> {
        java.util.List<Integer> list = documents.get((Integer) d.get(1));
        if(list == null)
          documents.put((Integer) d.get(1), list = new ArrayList<>());
        list.add((Integer) d.get(0));
      });
      
      documents.keySet().stream().forEach((partitionId) -> {
        String path = ExportImportUtil.getExportPath(partitionId);
        if (path != null && !path.equals("")) {
          ExportImportUtil.export(path, CreatedDocument.class, documents.get(partitionId).toArray(new Integer[0]));
        }
      });
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  private void fillPop(JPopupMenu pop, Integer[] temps, boolean toPrint, List<PropertyMap> documentsDataList) {
    if(temps.length > 0) {
      if(pop.getComponentCount() > 0)
        pop.add(new JPopupMenu.Separator());
      ObjectLoader.getList(DocumentXMLTemplate.class, temps).stream().forEach(t -> {
        JMenuItem item = new JMenuItem(t.getString("name"));
        pop.add(item);
        item.addActionListener(e -> {
          if(toPrint) {
            PrinterJob pj = PrinterJob.getPrinterJob();
            if(pj.printDialog())
              DocumentUtil.print(documentsDataList, t.getInteger("id"));
          }else DocumentUtil.preview(documentsDataList, t.getInteger("id"));
        });
      });
    }
  }

  private void printPrivew(AWTEvent e, boolean toPrint, boolean complect) {
    Integer[] ids = getSelectedId().length == 0 ? getAllIds() : getSelectedId();
    final List<PropertyMap> documentsDataList = DocumentUtil.getDocumentsDataList(ids);
    
    if(ids.length == 1) {
      JPopupMenu pop = new JPopupMenu();
      JMenuItem defItem = new JMenuItem("По умолчанию");
      defItem.addActionListener(ev -> {
        if(toPrint) {
          PrinterJob pj = PrinterJob.getPrinterJob();
          if(pj.printDialog())
            DocumentUtil.print(documentsDataList, null, complect, pj);
        }else DocumentUtil.preview(documentsDataList);
      });
      pop.add(defItem);
      fillPop(pop, documentsDataList.get(0).getArray("customer-template", Integer.class), toPrint, documentsDataList);
      fillPop(pop, documentsDataList.get(0).getArray("seller-template", Integer.class), toPrint, documentsDataList);
      fillPop(pop, documentsDataList.get(0).getArray("template", Integer.class), toPrint, documentsDataList);
      
      Component com = null;
      int x = 0;
      int y = 0;
      if(e instanceof ActionEvent) {
        com = (Component) e.getSource();
        x = 0;
        y = com.getHeight();
      }else {
        com = getTable();
        x = ((MouseEvent)e).getX();
        y = ((MouseEvent)e).getY();
      }

      pop.show(com, x, y);
    }else {
      if(toPrint) {
        PrinterJob pj = PrinterJob.getPrinterJob();
        if(pj.printDialog())
          DocumentUtil.print(documentsDataList, null, complect, pj);
      }else DocumentUtil.preview(documentsDataList);
    }
  }
  
  class TableDateFilter extends FilterCalendar {
    public TableDateFilter(TableFilter tableFilter, int column) {
      super(tableFilter, column);
    }
    
    @Override
    public boolean isOnlineFilter() {
      return false;
    }

    @Override
    public void setFilter(Object... params) {
      super.setFilter(params); //To change body of generated methods, choose Tools | Templates.
      if(isFilter())
        getDates().stream().forEach(d -> dateFilter.OR_DATE_AFTER_OR_EQUAL("date", d.getStartDate()).AND_DATE_BEFORE_OR_EQUAL("date", d.getEndDate()));
    }

    @Override
    public void startFilter() {
      dateFilter.clear();
      if(isFilter())
        getDates().stream().forEach(d -> dateFilter.OR_DATE_AFTER_OR_EQUAL("date", d.getStartDate()).AND_DATE_BEFORE_OR_EQUAL("date", d.getEndDate()));
      initData();
    }

    @Override
    public void clearFilter() {
      super.clearFilter();
      dateFilter.clear();
      initData();
    }
  }
}