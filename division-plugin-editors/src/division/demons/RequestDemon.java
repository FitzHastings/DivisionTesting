package division.demons;

import bum.editors.util.DivisionTarget;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Comment;
import bum.interfaces.Company;
import bum.interfaces.Request;
import division.fx.FXUtility;
import division.fx.PropertyMap;
import division.fx.dialog.FXD;
import division.fx.editor.FXEditor;
import division.fx.editor.FXTableEditor;
import division.fx.editor.GLoader;
import division.fx.table.Column;
import division.fx.table.filter.DateFilter;
import division.fx.table.filter.ListFilter;
import division.fx.table.filter.TextFilter;
import division.fx.util.MsgTrash;
import division.util.FileLoader;
import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.control.TableColumn;
import javafx.stage.Modality;
import javax.swing.SwingUtilities;
import util.filter.local.DBFilter;

public class RequestDemon {
  private FXD dialog;
  private FXTableEditor requestTable;
  
  private SystemTray  tray;
  private final Image defaultIcon = FileLoader.getIcon("images/mail24.png").getImage();
  private final Image mailIcon    = FileLoader.getIcon("images/mail-blue24.png").getImage();
  private TrayIcon    trayIcon;

  private RequestDemon() {
    Executors.newSingleThreadExecutor().submit(() -> {
      while(!isPossible()) {
        isPossible();
        try {
          Thread.sleep(1000);
        } catch (Exception ex) {
          MsgTrash.out(ex);
        }
      }
    });
    
    SwingUtilities.invokeLater(() -> init());
    
    Platform.runLater(() -> {
      while(ObjectLoader.getServer() == null ) {}
      
      dialog = FXD.create("Заявки", null, requestTable = new FXTableEditor(Request.class, 
              FXRequest.class, 
              Column.create("№",            "number", new TextFilter()),
              Column.create("Организация",  "applicant_name", new TextFilter()),
              Column.create("Причина",      "reason", new TextFilter()),
              Column.create("Исполнитель",  "performer_name=query:select [People(surName)]||' '||[People(name)]||' '||[People(lastName)] from [People] where id=[Request(people_id)]", new ListFilter("performer_name")),
              Column.create("Заявлено",     "startDate", new DateFilter("startDate")),
              Column.create("Принято",      "acceptDate", new DateFilter("acceptDate")),
              Column.create("В работе",     "executDate", new DateFilter("executDate")),
              Column.create("Отклонено",    "exitDate", new DateFilter("exitDate")),
              Column.create("Исполнено",    "finishDate", new DateFilter("finishDate")),
              Column.create("Комментрарии", "comment-count=query:"
                      + "(SELECT COUNT(id) FROM [Comment] WHERE [Comment(objectClass)]='Request' AND [Comment(objectId)]=[Request(id)] AND type='PROJECT' AND [Comment(lastUserId)]=0)||'/'||"
                      + "(SELECT COUNT(id) FROM [Comment] WHERE [Comment(objectClass)]='Request' AND [Comment(objectId)]=[Request(id)])"),
              Column.create("Итого", "itog=query:EXTRACT(DAY FROM [Request(finishDate)]-[Request(startDate)])||'д '||EXTRACT(HOUR FROM [Request(finishDate)]-[Request(startDate)])||'ч'")) {
        @Override
        protected void createObjects(Integer[] ids) {
          super.createObjects(ids);
          ObjectLoader.getList(Request.class, ids, "id","reason","company-name=query:getCompanyName([Request(applicant)])")
                  .stream().forEach(request -> actionToTray("Заявка от "+request.getString("company-name"), "Причина: "+request.getString("reason"), MessageType.INFO));
        }
      }, Modality.APPLICATION_MODAL, FXD.ButtonType.CLOSE);
      
      requestTable.setDoubleClickActionType(FXEditor.DoubleClickActionType.EDIT);
      
      requestTable.initData();
      
      requestTable.getTable().getColumn("Комментрарии").setSortType(TableColumn.SortType.DESCENDING);
      requestTable.getTable().getSortOrder().add(requestTable.getTable().getColumn("Комментрарии"));
      
      dialog.setOnShowing(e -> GLoader.load("Requests", requestTable));
      dialog.setOnHiding(e -> GLoader.store("Requests", requestTable));
      
      
      FXUtility.initMainCss(this);
      FXUtility.initMainCss(requestTable);
      /*try {
        requestTable.getScene().getStylesheets().add(FXUtility.generateCssFile(getClass()).toURI().toURL().toExternalForm());
      } catch (IOException ex) {
        MsgTrash.out(ex);
      }*/
    });
    
    DivisionTarget.create(Comment.class, (DivisionTarget target, String type, Integer[] ids, PropertyMap data) -> {
      Platform.runLater(() -> {
        if(!"REMOVE".equals(type)) {
          ObjectLoader.getList(DBFilter.create(Comment.class).AND_IN("id", ids).AND_EQUAL("objectClass", Request.class.getSimpleName())).forEach(comment -> {
            ObjectLoader.sendMessage(Request.class, "UPDATE", comment.getInteger("objectId"));
            
            if("CREATE".equals(type)) {
              if(!FXRequest.isOurComment(comment)) {
                String author = ObjectLoader.getMap(Company.class, Integer.valueOf(comment.getString("author").replace("Company:", "")), "name=query:getCompanyName([Company(id)])").getString("name");

                actionToTray("Сообщение от "+author, comment.getString("text"), MessageType.INFO);
                requestTable.getTable().getItems().stream().filter(r -> r.getInteger("id").equals(comment.getInteger("objectId"))).forEach(r -> {
                  requestTable.getTable().getSelectionModel().select(r);
                });
              }
            }
          });
        }
      });
    });
  }

  private void showDialog() {
    Platform.runLater(() -> {
      if(!dialog.isShowing()) {
        requestTable.initData();
        dialog.show();
      }else if(dialog.isIconified()) {
        dialog.setIconified(false);
      }
      dialog.toFront();
    });
  }
  
  private void actionToTray(String title, String text, MessageType type) {
    SwingUtilities.invokeLater(() -> {
      if(isTray()) {
        trayIcon.setImage(mailIcon);
        trayIcon.displayMessage(title, text, type);
      }
    });
  }
  
  private void init() {
    tray = SystemTray.getSystemTray();
    trayIcon = new TrayIcon(defaultIcon, "Заявки");

    trayIcon.setImageAutoSize(true);
    try {
      tray.add(trayIcon);
    } catch (AWTException ex) {
      Logger.getLogger(RequestDemon.class.getName()).log(Level.SEVERE, null, ex);
    }

    trayIcon.addActionListener((ActionEvent e) -> {
      trayIcon.setImage(defaultIcon);
      showDialog();
    });
  }
  
  private boolean isTray() {
    return trayIcon != null;
  }
  
  private boolean isPossible() {
    return true;
    /*if(!new File(name).exists()) {
      try {
        init();
        if(trayIcon != null) {
          FileLoader.createFileIfNotExists(name,true).deleteOnExit();
          return true;
        }
      }catch(Exception ex) {
        ex.printStackTrace();
      }
    }
    return false;*/
  }
  
  public static void start() {
    RequestDemon requestDemon = new RequestDemon();
  }
}