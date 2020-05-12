package division.fx.client;

import bum.editors.util.ObjectLoader;
import division.fx.PropertyMap;
import division.fx.FXUtility;
import division.fx.editor.GLoader;
import division.fx.task.TaskPanel;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import java.io.File;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.apache.commons.lang3.ArrayUtils;

public class Client extends Application {
  private ClientPane clientPane;
  private MenuBar menu = new MenuBar();
  
  private static Stage rootDialog;
  private static Pane  rootPane;
  private static BorderPane rootBorderPane;
  private static TaskPanel taskPanel;
  
  public static Stage getRootDialog() {
    return rootDialog;
  }

  public static Pane getRootPane() {
    return rootPane;
  }

  public static BorderPane getRootBorderPane() {
    return rootBorderPane;
  }

  public static TaskPanel getTaskPanel() {
    return taskPanel;
  }
  
  @Override
  public void start(Stage primaryStage) throws Exception {
    rootDialog = primaryStage;
    /*LocalDate d = LocalDate.of(2016, 1, 1);
    
    System.out.println(Utility.getMonthString(Utility.convert(d)));
    
    System.exit(0);*/
    
    ObjectLoader.clear();
    ObjectLoader.connect();
    
    
    /*ObservableList<PropertyMap> cplist = ObjectLoader.getList(DBFilter.create(CompanyPartition.class).AND_EQUAL("company", 677));
    cplist.stream().forEach(cp -> System.out.println(cp.getString("name").toLowerCase().trim()));
    ObservableList<PropertyMap> kkt = FXCollections.observableArrayList();
    
    Files.readAllLines(new File("DHL.csv").toPath()).stream().forEach(line -> {
      String id   = line.split("\t")[0];
      String addr = line.split("\t")[1].toLowerCase().trim();
      List<PropertyMap> list = cplist.filtered(cp -> addr.toLowerCase().trim().contains(" "+cp.getString("name").toLowerCase().trim()));
      kkt.add(PropertyMap.create().setValue("id", id).setValue("addr", addr).setValue("partitions", PropertyMap.copyList(list)));
    });
    
    kkt.stream().forEach(k -> {
      if(!k.getList("partitions").isEmpty()) {
        PropertyMap cp = k.getList("partitions").get(0);
        ObservableList<PropertyMap> stories = ObjectLoader.getList(
                DBFilter.create(Store.class).AND_EQUAL("companyPartition", cp.getInteger("id")).AND_ILIKE("name", "основной%").OR_EQUAL("companyPartition", cp.getInteger("id")).AND_ILIKE("name", "%ККТ%"), "id","name");
        if(stories.size() == 1) {
          if(ObjectLoader.getList(DBFilter.create(Equipment.class).AND_EQUAL("store", stories.get(0).getInteger("id")).AND_EQUAL("group", 2817).AND_EQUAL("identity_value_name", k.getString("id"))).isEmpty()) {
            System.out.println("Добавляю: "+k.getString("id")+" ("+k.getString("addr")+")");
            Integer equipId = ObjectLoader.createObject(Equipment.class, PropertyMap.create().setValue("group", 2817).setValue("store", stories.get(0).getInteger("id")));
            ObjectLoader.createObject(EquipmentFactorValue.class, PropertyMap.create().setValue("factor", 18).setValue("equipment", equipId).setValue("name", k.getString("id")));
            ObjectLoader.createObject(EquipmentFactorValue.class, PropertyMap.create().setValue("factor", 52).setValue("equipment", equipId).setValue("name", k.getString("addr")));
          }else System.out.println("уже есть: "+k.getString("id")+" ("+k.getString("addr")+")");
        }else System.out.println("не нашёл нужного склада для: "+k.getString("id")+" ("+k.getString("addr")+")");
      }else System.out.println("не нашёл подразделения для: "+k.getString("id")+" ("+k.getString("addr")+")");
    });
    
    System.exit(0);*/
    
    primaryStage.setScene(new Scene(rootPane = new Pane(rootBorderPane = new BorderPane(clientPane = new ClientPane()))));
    MenuLoader.load(clientPane.getDesktop(), menu, "conf/conf.json");
    getRootBorderPane().setTop(menu);
    HBox fp = new HBox(5,taskPanel = new TaskPanel());
    fp.setAlignment(Pos.CENTER_RIGHT);
    getRootBorderPane().setBottom(fp);
    
    getRootBorderPane().prefHeightProperty().bind(getRootPane().heightProperty());
    getRootBorderPane().prefWidthProperty().bind(getRootPane().widthProperty());
    
    getRootBorderPane().minHeightProperty().bind(getRootPane().heightProperty());
    getRootBorderPane().minWidthProperty().bind(getRootPane().widthProperty());
    
    getRootBorderPane().maxHeightProperty().bind(getRootPane().heightProperty());
    getRootBorderPane().maxWidthProperty().bind(getRootPane().widthProperty());
    
    getRootDialog().getScene().getStylesheets().add(new File("fx/css/ClientPane.css").toURI().toURL().toExternalForm());
    getRootDialog().getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.F5), (Runnable) () -> FXUtility.reloadCss(getRootDialog().getScene()));
    primaryStage.setOnHidden(e ->  System.exit(0));
    primaryStage.setOnHiding(e ->  GLoader.store("rootPane", getRootPane()));
    primaryStage.setOnShowing(e -> GLoader.load("rootPane", getRootPane()));
    getRootDialog().show();
    
    clientPane.getCFCTable().selectedItemProperty().addListener((ObservableValue<? extends TreeItem<PropertyMap>> observable, TreeItem<PropertyMap> oldValue, TreeItem<PropertyMap> newValue) -> {
      Integer[] ids = null;
      if(newValue != null)
        for(PropertyMap tp:clientPane.getCFCTable().getSubObjects(newValue))
          if(tp.containsKey("id") && tp.getValue("id") != null)
            ids = ArrayUtils.add(ids, (Integer)tp.getValue("id"));
      ClientFilter.CFCProperty().set(ids);
    });
    
    clientPane.getCompanyTable().selectedItemProperty().addListener((ob, ol, nw) -> {
      Integer[] ids = null;
      for(PropertyMap row:clientPane.getCompanyTable().getTable().getSelectionModel().getSelectedItems())
        ids = ArrayUtils.add(ids, (Integer) row.getValue("id"));
      ClientFilter.companyProperty().set(ids);
    });
    
    clientPane.getPartitionTable().selectedItemProperty().addListener((ObservableValue<? extends PropertyMap> observable, PropertyMap oldValue, PropertyMap newValue) -> {
      Integer[] ids = null;
      for(PropertyMap row:clientPane.getPartitionTable().getTable().getSelectionModel().getSelectedItems())
        ids = ArrayUtils.add(ids, (Integer)row.getValue("id"));
      ClientFilter.partitionProperty().set(ids);
    });
    
    Thread t = new Thread(() -> {
      while(true) {
        try {
          Thread.sleep(60000);
          Runtime.getRuntime().gc();
        }catch(NumberFormatException | InterruptedException ex){}
      }
    });
    t.setDaemon(true);
    t.start();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
