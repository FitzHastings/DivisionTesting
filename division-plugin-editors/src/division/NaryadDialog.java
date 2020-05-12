package division;

import bum.editors.util.ObjectLoader;
import bum.interfaces.CreatedDocument;
import bum.interfaces.Deal;
import bum.interfaces.DealPosition;
import bum.interfaces.ProductDocument;
import division.fx.PropertyMap;
import division.fx.editor.GLoader;
import division.fx.table.Column;
import division.fx.table.DivisionTableCell;
import division.fx.table.FXDivisionTable;
import division.fx.util.MsgTrash;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class NaryadDialog extends JFXPanel {
  private JDialog dialog = new JDialog(ClientMain.getInstance());
  private TextField codeText;
  private Button codeEnter;
  private FXDivisionTable<PropertyMap> table;
  private Button dispatch = new Button("отгрузить");
  private VBox root;
  
  public NaryadDialog() {
    
  }
  
  private void init() {
    codeText = new TextField();
    codeEnter = new Button("ввести");
    table = new FXDivisionTable(
          Column.create("Наименование", "name", true),
          Column.create("№", "number", true),
          Column.create("Процесс", "process", true),
          Column.create("Объект", "object", true),
          Column.create("Зав.№", "value", true),
          Column.create("Стоимость", "cost", true),
          Column.create("Продавец", "seller", true),
          Column.create("Покупатель", "customer", true),
          Column.create("Отггрузить работу", "dispatch", true, true));
    root = new VBox(5, new HBox(5, new Label("КОД:"), codeText, codeEnter), table, dispatch);
    
    root.setPadding(new Insets(5));
    root.setAlignment(Pos.CENTER_RIGHT);
    VBox.setVgrow(table, Priority.ALWAYS);
    HBox.setHgrow(codeText, Priority.ALWAYS);
    setScene(new Scene(root, 600, 400));
    
    table.setEditable(true);
    
    table.getColumn("Отггрузить работу").setCellFactory(new Callback() {
      @Override
      public Object call(Object param) {
        return new DivisionTableCell() {
          @Override
          protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            setEditable(getTableRow().getItem() != null && ((PropertyMap)getTableRow().getItem()).getValue("dispatch-id") == null);
            //setDisable(getTableRow().getItem() != null && ((PropertyMap)getTableRow().getItem()).getValue("dispatch-id") != null);
          }
        };
      }
    });
    
    table.getColumns().stream().forEach(c -> {
      if(!((Column)c).getColumnName().equals("Отггрузить работу")) {
        ((TableColumn<PropertyMap, Object>)c).setCellFactory(new Callback<TableColumn<PropertyMap, Object>, TableCell<PropertyMap, Object>>() {
          @Override
          public TableCell<PropertyMap, Object> call(TableColumn<PropertyMap, Object> param) {
            return new DivisionTableCell() {
              @Override
              protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if(!empty) {
                  setStyle("-fx-text-fill:black");
                  if(getTableRow().getItem() != null && ((PropertyMap)getTableRow().getItem()).getValue("dispatch-id") != null) {
                    setStyle("-fx-text-fill:gray");
                    ((PropertyMap)getTableRow().getItem()).setValue("dispatch", false);
                  }
                  if(item != null)
                    setText(String.valueOf(item));
                }
              }
            };
          }
        });
      }
    });

    codeEnter.setOnAction(e -> enterCode(codeText.getText().trim()));
    
    dispatch.setOnAction(e -> dispatch());
    
    root.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
      String code = "";
      @Override
      public void handle(KeyEvent event) {
        code += event.getText();
        if(code.matches(".*[\\n\\t\\f\\r]")) {
          enterCode(code.trim());
          code = "";
        }
      }
    });
    
    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        Platform.runLater(() -> {
          try {
            GLoader.store("naryad-dialog", root, table);
          } catch (Exception ex) {
            MsgTrash.out(ex);
          }
        });
      }
    });
  }
  
  private void enterCode(String code) {
    if(!"".equals(code)) {
      codeText.setText("");
      
      if(code.startsWith("POS-") || code.startsWith("ИЗЩЫ-")) {
        Integer id = Integer.valueOf(code.split("-")[1]);
        ObjectLoader.executeQuery("SELECT "
                          + /*0*/"[DealPosition(service_name)], "
                          + /*1*/"[DealPosition(group_name)], "
                          + /*2*/"[DealPosition(identity_value)], "
                          + /*3*/"id, "
                          + /*4*/"[DealPosition(deal)], "
                          + /*5*/"[DealPosition(customProductCost)], "
                          + /*6*/"[DealPosition(dispatchId)], "
                          + /*7*/"[DealPosition(equipment)], "
                          + /*8*/"[DealPosition(product)], "
                          + /*9*/"[DealPosition(startDate)], "
                          + /*10*/"[DealPosition(startId)], "
                          + /*11*/"getCompanyName([DealPosition(seller_id)]), "
                          + /*12*/"getCompanyName([DealPosition(customer_id)]) "
                          + "FROM [DealPosition] WHERE id = "+id).stream().forEach(dp -> {
                            boolean exist = false;
                            for(PropertyMap it:table.getSourceItems()) {
                              if(it.getValue("id").equals(dp.get(3))) {
                                exist = true;
                                break;
                              }
                            }
                            if(!exist)
                              table.getSourceItems().add(PropertyMap.create()
                                      .setValue("process", dp.get(0)).setValue("object", dp.get(1)).setValue("value", dp.get(2)).setValue("id", dp.get(3)).setValue("deal", dp.get(4)).setValue("cost", dp.get(5)).setValue("dispatch-id", dp.get(6))
                                      .setValue("equipment", dp.get(7)).setValue("product", dp.get(8)).setValue("startDate", dp.get(9)).setValue("startId", dp.get(10))
                                      .setValue("dispatch", dp.get(6) == null).setValue("seller", dp.get(11)).setValue("customer", dp.get(12)).setValue("name", "Заказ-наряд").setValue("number", "POS-"+dp.get(3)));
                          });
      }else {
        ObjectLoader.getList(DBFilter.create(CreatedDocument.class).AND_EQUAL("id", Integer.valueOf(code)), 
                "document-id:=:id",
                "name",
                "number",
                "seller:=:seller-name",
                "customer:=:customer-name").stream().forEach(d -> {
                  ObjectLoader.executeQuery("SELECT "
                          + /*0*/"[DealPosition(service_name)], "
                          + /*1*/"[DealPosition(group_name)], "
                          + /*2*/"[DealPosition(identity_value)], "
                          + /*3*/"id, "
                          + /*4*/"[DealPosition(deal)], "
                          + /*5*/"[DealPosition(customProductCost)], "
                          + /*6*/"[DealPosition(dispatchId)], "
                          + /*7*/"[DealPosition(equipment)], "
                          + /*8*/"[DealPosition(product)], "
                          + /*9*/"[DealPosition(startDate)], "
                          + /*10*/"[DealPosition(startId)] "
                          + "FROM [DealPosition] WHERE id in "
                          + "(SELECT [CreatedDocument(dealPositions):target] FROM [CreatedDocument(dealPositions):table] WHERE [CreatedDocument(dealPositions):object]="+code+")").stream().forEach(dp -> {
                            boolean exist = false;
                            for(PropertyMap it:table.getSourceItems()) {
                              if(it.getValue("id").equals(dp.get(3))) {
                                exist = true;
                                break;
                              }
                            }
                            if(!exist)
                              table.getSourceItems().add(PropertyMap.copy(d)
                                      .setValue("process", dp.get(0)).setValue("object", dp.get(1)).setValue("value", dp.get(2)).setValue("id", dp.get(3)).setValue("deal", dp.get(4)).setValue("cost", dp.get(5)).setValue("dispatch-id", dp.get(6))
                                      .setValue("equipment", dp.get(7)).setValue("product", dp.get(8)).setValue("startDate", dp.get(9)).setValue("startId", dp.get(10))
                                      .setValue("dispatch", dp.get(6) == null));
                          });
                });
      }
    }
  }
  
  private void dispatch() {
    root.setCursor(Cursor.WAIT);
    Integer[] dps = new Integer[0];
    TreeMap<Integer,Integer[]> deals = new TreeMap<>();
    for(PropertyMap dp:table.getSourceItems()) {
      if((boolean)dp.getValue("dispatch") && dp.getValue("dispatch-id") == null) {
        dps = (Integer[]) ArrayUtils.add(dps, dp.getValue("id"));
        if(!deals.containsKey((Integer)dp.getValue("deal")))
          deals.put((Integer)dp.getValue("deal"), new Integer[0]);
        deals.put((Integer)dp.getValue("deal"), (Integer[])ArrayUtils.add((Integer[])deals.get((Integer)dp.getValue("deal")), (Integer)dp.getValue("id")));
      }
    }
    division.util.actions.ActionDocumentStarter actionDocumentStarter = new division.util.actions.ActionDocumentStarter();
    actionDocumentStarter.init(ProductDocument.ActionType.ОТГРУЗКА, dps);
    if(actionDocumentStarter.customUpdateDocuments(deals.size() > 1)) {
      RemoteSession session = null;
      try {
        session = ObjectLoader.createSession();
        Map<Integer, List> createdDocuments = actionDocumentStarter.start(session, null, null);
        String[] querys = new String[0];
        Object[][] params = new Object[0][];

        Long dispatchId = (Long) session.executeQuery("SELECT MAX([DealPosition(dispatchId)])+1 FROM [DealPosition]").get(0).get(0);
        dispatchId = dispatchId == null ? 1 : dispatchId;

        for(Integer dp:createdDocuments.keySet()) {
          List docs = createdDocuments.get(dp);
          querys = (String[]) ArrayUtils.add(querys, "UPDATE [DealPosition] SET [DealPosition(dispatchId)]=?, [DealPosition(dispatchDate)]=CURRENT_TIMESTAMP, "
                  + "[DealPosition(actionDispatchDate)]=(CASE WHEN (SELECT MAX([CreatedDocument(date)]) FROM [CreatedDocument] WHERE id=ANY(?)) ISNULL THEN CURRENT_DATE ELSE (SELECT MAX([CreatedDocument(date)]) FROM [CreatedDocument] WHERE id=ANY(?)) END) "
                  + "WHERE [DealPosition(id)]=?");
          params = (Object[][]) ArrayUtils.add(params, new Object[]{dispatchId, docs.toArray(new Integer[0]), docs.toArray(new Integer[0]), dp});
        }
        session.executeUpdate(querys, params);
        session.addEvent(DealPosition.class, "UPDATE", dps);
        session.addEvent(Deal.class, "UPDATE", deals.keySet().toArray(new Integer[0]));
        session.commit();
        dialog.setVisible(false);
      }catch(Exception ex) {
        if(session != null)
          try {session.rollback();}catch(Exception e){}
        MsgTrash.out(ex);
      }
    }
    root.setCursor(Cursor.DEFAULT);
  }
  
  public void start() {
    Platform.runLater(() -> {
      init();
      GLoader.load("naryad-dialog", root, table);
      SwingUtilities.invokeLater(() -> {
        dialog.setSize((int)root.getPrefWidth(), (int)root.getPrefHeight());
        dialog.setContentPane(this);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
      });
    });
  }
}