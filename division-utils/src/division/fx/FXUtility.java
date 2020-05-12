package division.fx;

import division.util.FileLoader;
import division.util.Utility;
import java.io.*;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.fxml.Initializable;
import javafx.stage.Window;

public class FXUtility {
  public static void copyStylesheets(Window from, Scene to) {
    to.getStylesheets().addAll(from.getScene().getStylesheets());
  }
  
  public static void copyStylesheets(Node from, Node to) {
    copyStylesheets(from.getScene(), to.getScene());
  }
  
  public static void copyStylesheets(Scene from, Scene to) {
    to.getStylesheets().addAll(from.getStylesheets());
    System.out.println("from.getStylesheets() = "+from.getStylesheets());
    System.out.println("to.getStylesheets() = "+to.getStylesheets());
  }
  
  public static void copyStylesheets(Scene from, Parent to) {
    copyStylesheets(from, to.getScene());
  }
  
  public static void copyStylesheets(Node from, Scene to) {
    copyStylesheets(from.getScene(), to);
  }
  
  public static void copyStylesheets(Node from, Parent to) {
    copyStylesheets(from.getScene(), to.getScene());
  }
  
  public static void copyStylesheets(Parent from, Parent to) {
    copyStylesheets(from.getScene(), to.getScene());
  }
  
  public static void reloadCss(Scene scene) {
    System.out.println("RELOAD CSS");
    Platform.runLater(() -> {
      synchronized(scene) {
        for(String url:scene.getStylesheets()) {
          scene.getStylesheets().remove(url);
          FileWriter fw = null;
          try{
            fw = new FileWriter(new URL(url).getFile(), true);
            fw.write("\r\n/*REFRESH: "+Utility.format(System.currentTimeMillis())+"*/\r\n");
          }catch(Exception ex) {
            //MsgTrash.out(ex);
          }finally {
            if(fw != null) {
              try {fw.flush();}catch(Exception ex) {
                //MsgTrash.out(ex);
              }
              try {fw.close();}catch(Exception ex) {
                //MsgTrash.out(ex);
              }
            }
          }
          scene.getStylesheets().add(url);
        }
      }
    });
  }
  
  public static void initMainCss(Object object) {
    initCss(object);
    Executors.newSingleThreadExecutor().submit(() -> {
      try {
        if(object instanceof Node) {
          Node node = (Node) object;
          while(node.getScene() == null)
            Thread.sleep(200);
          Platform.runLater(() -> {
            try {
              node.getScene().getStylesheets().add(Paths.get("fx"+File.separator+"css"+File.separator+"ClientPane.css").toUri().toURL().toExternalForm());
              System.out.println("set main style");
            }catch(Exception ex) {
              ex.printStackTrace();
              System.out.println("path = "+Paths.get("fx"+File.separator+"css"+File.separator+"ClientPane.css"));
              if(Paths.get("fx"+File.separator+"css"+File.separator+"ClientPane.css") != null)
                try {
                  System.out.println("url = "+Paths.get("fx"+File.separator+"css"+File.separator+"ClientPane.css").toUri().toURL().toExternalForm());
              } catch (MalformedURLException ex1) {
                ex1.printStackTrace();
              }
            }
          });
        }
      }catch (Exception ex) {
        ex.printStackTrace();
      }
    });
  }
  
  public static void initCss(Object object) {
    if(object instanceof Node)
      ((Node)object).getStyleClass().add(((Node)object).getClass().getSimpleName());
    Class clazz = object.getClass();
    while(clazz != null) {
      for(Field field:clazz.getDeclaredFields()) {
        try {
          field.setAccessible(true);
          Object o = field.get(object);
          if(o != null && o instanceof Node)
            ((Node)o).getStyleClass().addAll(object.getClass().getSimpleName(), field.getName());
        }catch(Exception ex) {
          //MsgTrash.out(ex);
        }
      }
      clazz = clazz.getSuperclass();
    }
  }
  
  public static void registrate(Node node) {
    initCss(node);
    Executors.newSingleThreadExecutor().submit(() -> {
      while(node.getScene() == null)
        try {
          Thread.sleep(500);
        }catch (InterruptedException ex) {}
      Platform.runLater(() -> {
        try {
          node.getScene().getStylesheets().add(generateCssFile(node.getClass()).toURI().toURL().toExternalForm());
        }catch(Exception ex) {}
      });
    });
  }
  
  public static String convertClassToCss(Class cl) {
    return "fx"+File.separator+cl.getName().replace('.', File.separatorChar)+".css";
  }
  
  public static File generateCssFile(Class cl) throws IOException {
    return FileLoader.createFileIfNotExists(convertClassToCss(cl));
  }
  
  public static String convertClassToFxml(Class cl) {
    return "fx"+File.separator+cl.getName().replace('.', File.separatorChar)+".fxml";
  }
  
  public static URL getResouce(String path) {
    try {
      File f = new File(path);
      if(!f.exists()) {
        f.getParentFile().mkdirs();
        f.createNewFile();
      }
      return new File(path).toURI().toURL();
    }catch(Exception ex) {
      new Alert(Alert.AlertType.ERROR, "Ошибка при загрузке \""+path+"\"", ButtonType.OK).show();
    }
    return null;
  }
  
  public static FXMLLoader getLoader(Initializable controller) {
    return getLoader(FXUtility.convertClassToFxml(controller.getClass()));
  }
  
  public static FXMLLoader getLoader(String fxml) {
    try {
      return new FXMLLoader(new File(fxml).toURI().toURL());
    }catch(Exception ex) {
      new Alert(Alert.AlertType.ERROR, "Ошибка при загрузке \""+fxml+"\"", ButtonType.OK).show();
    }
    return null;
  }
  
  public static Object load(Class cl, String fxml) {
    try {
      return FXMLLoader.load(cl.getResource(fxml));
    }catch(Exception ex) {
      new Alert(Alert.AlertType.ERROR, "Ошибка при загрузке \""+fxml+"\"", ButtonType.OK).show();
    }
    return null;
  }
  
  public static Object load(Initializable controller) {
    return load(FXUtility.convertClassToFxml(controller.getClass()));
  }
  
  public static Object load(String fxml) {
    try {
      return FXMLLoader.load(new File(fxml).toURI().toURL());
    }catch(Exception ex) {
      new Alert(Alert.AlertType.ERROR, "Ошибка при загрузке \""+fxml+"\"", ButtonType.OK).show();
    }
    return null;
  }
}