package division.fx.controller.documents;

import bum.editors.util.ObjectLoader;
import bum.interfaces.CreatedDocument;
import bum.interfaces.DocumentXMLTemplate;
import division.fx.FXToolButton;
import division.fx.PropertyMap;
import division.fx.editor.FXTableEditor;
import division.fx.table.Column;
import division.fx.table.filter.DateFilter;
import division.fx.table.filter.FilterListener;
import division.fx.table.filter.ListFilter;
import division.fx.table.filter.TextFilter;
import division.util.DocumentUtil;
import java.awt.print.PrinterJob;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Point2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseEvent;

public class CreatedDocumentTable extends FXTableEditor {
  private final FXToolButton preview       = new FXToolButton("Просмотр", "preview-button");
  private final FXToolButton print         = new FXToolButton("Печать", "print-button");
  private final FXToolButton printcomplect = new FXToolButton("Печать комплектами", "print-complect-button");
  private ContextMenu templatemenu = new ContextMenu();

  public CreatedDocumentTable() {
    super(CreatedDocument.class, null, 
          FXCollections.observableArrayList("id","document","document_name"),
          Column.create("Документ",   "name",          new ListFilter("document_name")),
          Column.create("номер",      "number",        new TextFilter()),
          Column.create("дата",       "date",          new DateFilter("date")),
          Column.create("агент",      "seller-name",   new TextFilter()),
          Column.create("контрагент", "customer-name", new TextFilter()));
    
    print.setOnAction(e -> print(e));
    printcomplect.setOnAction(e -> printComplect(e));
    preview.setOnAction(e -> preview(e));
    templatemenu.setHideOnEscape(true);
    templatemenu.setAutoHide(true);
    templatemenu.setAutoFix(true);
    
    getTools().getItems().addAll(preview, print, printcomplect);
    
    setSelectionMode(SelectionMode.MULTIPLE);
    
    getTable().setOnMouseClicked(e -> {
      if(e.getClickCount() == 2)
        preview(e);
      else templatemenu.hide();
    });
  }
  
  private void preview(Event e) {
    printPrivew(e, false, false);
  }
  
  private void print(Event e) {
    printPrivew(e, true, false);
  }
  
  private void printComplect(Event e) {
    printPrivew(e, true, true);
  }
  
  /*private void printpreview(Event e, boolean print, boolean complect) {
    Integer[] ids = getSelectedIds();
    if(ids.length == 0) // если нет выделения, то считаем что выделено всё
      for(PropertyMap p:getTable().getItems())
        ids = (Integer[]) ArrayUtils.add(ids, p.getInteger("id"));
    final List<PropertyMap> documentsDataList = DocumentUtil.getDocumentsDataList(ids);
    if(print) {
      PrinterJob pj = PrinterJob.getPrinterJob();
      if(pj.printDialog())
        DocumentUtil.print(documentsDataList, null, complect, pj);
    }else DocumentUtil.preview(documentsDataList);
  }*/
  
  private void printPrivew(Event e, boolean toPrint, boolean complect) {
    Integer[] ids = (getTable().getSelectionModel().getSelectedItems().isEmpty() ? getTable().getItems() : getTable().getSelectionModel().getSelectedItems()).stream().map(d -> d.getInteger("id")).collect(Collectors.toList()).toArray(new Integer[0]);
    final List<PropertyMap> documentsDataList = DocumentUtil.getDocumentsDataList(ids);
    
    if(ids.length == 1) {
      ContextMenu menu = new ContextMenu();
      MenuItem defItem = new MenuItem("По умолчанию");
      defItem.setOnAction(ev -> {
        if(toPrint) {
          PrinterJob pj = PrinterJob.getPrinterJob();
          if(pj.printDialog())
            DocumentUtil.print(documentsDataList, null, complect, pj);
        }else DocumentUtil.preview(documentsDataList);
      });
      menu.getItems().add(defItem);
      fillPop(menu, documentsDataList.get(0).getArray("customer-template", Integer.class), toPrint, documentsDataList);
      fillPop(menu, documentsDataList.get(0).getArray("seller-template", Integer.class), toPrint, documentsDataList);
      fillPop(menu, documentsDataList.get(0).getArray("template", Integer.class), toPrint, documentsDataList);
      
      Control com = (Control) e.getSource();
      double x;
      double y;
      if(e instanceof ActionEvent) {
        x = 0;
        y = com.getHeight();
      }else {
        x = ((MouseEvent)e).getX();
        y = ((MouseEvent)e).getY();
      }
      
      Point2D p = com.localToScreen(x, y);

      menu.show(com, p.getX(), p.getY());
    }else {
      if(toPrint) {
        PrinterJob pj = PrinterJob.getPrinterJob();
        if(pj.printDialog())
          DocumentUtil.print(documentsDataList, null, complect, pj);
      }else DocumentUtil.preview(documentsDataList);
    }
  }
  
  private void fillPop(ContextMenu menu, Integer[] temps, boolean toPrint, List<PropertyMap> documentsDataList) {
    if(temps.length > 0) {
      if(!menu.getItems().isEmpty())
        menu.getItems().add(new SeparatorMenuItem());
      ObjectLoader.getList(DocumentXMLTemplate.class, temps).stream().forEach(t -> {
        MenuItem item = new MenuItem(t.getString("name"));
        menu.getItems().add(item);
        item.setOnAction(e -> {
          if(toPrint) {
            PrinterJob pj = PrinterJob.getPrinterJob();
            if(pj.printDialog())
              DocumentUtil.print(documentsDataList, t.getInteger("id"));
          }else DocumentUtil.preview(documentsDataList, t.getInteger("id"));
        });
      });
    }
  }
  
  /*class TemplateEvent implements EventHandler<ActionEvent> {
    private final ObservableList<PropertyMap> documentsdata;
    private final PropertyMap template;
    private final boolean print;
    private final boolean complect;

    public TemplateEvent(ObservableList<PropertyMap> documentsdata, PropertyMap template, boolean print, boolean complect) {
      this.documentsdata = documentsdata;
      this.template = template;
      this.print = print;
      this.complect = complect;
    }
    
    @Override
    public void handle(ActionEvent event) {
      if(print) {
        SwingUtilities.invokeLater(() -> {
          PrinterJob pj = PrinterJob.getPrinterJob();
          if(pj.printDialog())
            DocumentUtil.print(documentsdata, template, complect, pj);
        });
      }else DocumentUtil.preview(getScene().getWindow(), documentsdata, template);
    }
    
  }

  private void printpreview(Event e, boolean print, boolean complect) {
     templatemenu.getItems().clear();
    ObservableList<PropertyMap> documents = getSelectedObjects();
    if(!documents.isEmpty()) {
      
      ObservableList<PropertyMap> documentsdata = DocumentUtil.getDocuments(PropertyMap.getListFromList(documents, "id", Integer.TYPE).toArray(new Integer[0]));
      
      if(documents.filtered(d -> d.getInteger("document").equals(documents.get(0).getInteger("document"))).size() == documents.size()) { //Если один тип документов
        
        for(PropertyMap t:ObjectLoader.getList(DocumentXMLTemplate.class, documentsdata.get(0).getValue("customer-template", Integer[].class))) {
          MenuItem item = new MenuItem(t.getString("name")+" (шаблон покупателя)");
          item.setOnAction(new TemplateEvent(documentsdata, t, print, complect));
          templatemenu.getItems().add(item);
        }
        
        for(PropertyMap t:ObjectLoader.getList(DocumentXMLTemplate.class, documentsdata.get(0).getValue("seller-template", Integer[].class))) {
          MenuItem item = new MenuItem(t.getString("name")+" (шаблон продавца)");
          item.setOnAction(new TemplateEvent(documentsdata, t, print, complect));
          templatemenu.getItems().add(item);
        }
        
        for(PropertyMap t:ObjectLoader.getList(DocumentXMLTemplate.class, documentsdata.get(0).getValue("template", Integer[].class))) {
          MenuItem item = new MenuItem(t.getString("name")+" (общий шаблон)");
          item.setOnAction(new TemplateEvent(documentsdata, t, print, complect));
          templatemenu.getItems().add(item);
        }
        
        if(e instanceof MouseEvent)
          templatemenu.show((Node) e.getSource(), ((MouseEvent)e).getScreenX(), ((MouseEvent)e).getScreenY());
        else templatemenu.show((Node) e.getSource(), Side.BOTTOM, 0, 0);
      }else {
        TemplateEvent event = new TemplateEvent(documentsdata, null, print, complect);
        event.handle(new ActionEvent());
      }
    }
  }*/
}
