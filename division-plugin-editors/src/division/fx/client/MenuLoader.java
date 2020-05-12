package division.fx.client;

import division.fx.desktop.FXDesktopPane;
import division.fx.FXUtility;
import division.fx.util.MsgTrash;
import javafx.scene.control.*;
import javafx.scene.image.*;
import division.fx.client.plugins.FXPlugin;
import java.awt.event.ActionListener;
import javafx.event.EventHandler;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.application.Platform;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import net.sf.json.JSONObject;

public class MenuLoader {
  
  public Collection<MenuItem> get(String confFile) {
    return get(null, confFile);
  }
  
  public Collection<MenuItem> get(FXDesktopPane desktop, String confFile) {
    List<MenuItem> list = new ArrayList<>();
    JSONObject config = JSONObject.fromObject(division.util.Utility.getStringFromFile(confFile));
    config.getJSONArray("menubar").stream().forEach(m -> {
      Menu menu = createMenu((JSONObject)m);
      list.add(menu);
      load(null, menu, (JSONObject)m);
    });
    return list;
  }
  
  public static void load(JMenuBar bar, String confFile) {
    JSONObject config = JSONObject.fromObject(division.util.Utility.getStringFromFile(confFile));
    config.getJSONArray("menubar").stream().forEach(m -> {
      JMenu menu = new JMenu(((JSONObject)m).getString("name"));
      bar.add(menu);
      load(null, menu, (JSONObject)m);
    });
  }
  
  public static void load(FXDesktopPane desktop, MenuBar bar, String confFile) {
    JSONObject config = JSONObject.fromObject(division.util.Utility.getStringFromFile(confFile));
    config.getJSONArray("menubar").stream().forEach(m -> {
      Menu menu = createMenu((JSONObject)m);
      bar.getMenus().add(menu);
      load(desktop, menu, (JSONObject)m);
    });
  }
  
  private static void load(FXDesktopPane desktop, Menu menu, JSONObject m) {
    m.getJSONArray("items").stream().forEach(n -> {
      
      if(((JSONObject)n).containsKey("items")) {
        Menu sm = createMenu((JSONObject)n);
        menu.getItems().add(sm);
        load(desktop, sm, (JSONObject)n);
      }else {
        MenuItem item = createItem(desktop, (JSONObject)n);
        menu.getItems().add(item);
      }
    });
  }
  
  private static void load(FXDesktopPane desktop, JMenu menu, JSONObject m) {
    m.getJSONArray("items").stream().forEach(n -> {
      if(((JSONObject)n).containsKey("items")) {
        JMenu sm = new JMenu(((JSONObject)n).getString("name"));
        menu.add(sm);
        load(desktop, sm, (JSONObject)n);
      }else {
        JMenuItem item = createJItem(desktop, (JSONObject)n);
        menu.add(item);
      }
    });
  }
  
  private static JMenuItem createJItem(FXDesktopPane desktop, JSONObject m) {
    JMenuItem item = new JMenuItem(m.getString("name"));
    if(m.containsKey("action-class") && !m.getString("action-class").equals("")) {
      item.addActionListener(event -> {try{Class.forName(m.getString("action-class")).newInstance();}catch(Exception ex){MsgTrash.out(ex);}});
    }else if(m.containsKey("plugin-class") && !m.getString("plugin-class").equals(""))
      item.addActionListener(createActionListenerJ(!m.containsKey("title") ? m.getString("name") : m.getString("title"), desktop, m.getString("plugin-class"), (String)m.getOrDefault("fxml", null)));
    return item;
  }
  
  private static MenuItem createItem(FXDesktopPane desktop, JSONObject m) {
    MenuItem item = new MenuItem(m.getString("name"));
    if(m.containsKey("action-class") && !m.getString("action-class").equals("")) {
      item.setOnAction(event -> {try{Class.forName(m.getString("action-class")).newInstance();}catch(Exception ex){MsgTrash.out(ex);}});
    }else if(m.containsKey("plugin-class") && !m.getString("plugin-class").equals(""))
      item.setOnAction(createActionListener(!m.containsKey("title") ? m.getString("name") : m.getString("title"), desktop, m.getString("plugin-class"), (String)m.getOrDefault("fxml", null)));
    
    if(m.containsKey("img") && !m.getString("img").equals(""))
      try{item.setGraphic(new ImageView(new File(m.getString("img")).toURI().toURL().toExternalForm()));}catch(Exception ex){MsgTrash.out(ex);}
    return item;
  }
  
  private static Menu createMenu(JSONObject m) {
    Menu menu = new Menu(m.getString("name"));
    if(m.containsKey("img") && !m.getString("img").equals(""))
      try{menu.setGraphic(new ImageView(new File(m.getString("img")).toURI().toURL().toExternalForm()));}catch(Exception ex){MsgTrash.out(ex);}
    return menu;
  }
  
  private static EventHandler createActionListener(String title, FXDesktopPane desktop, String className, String fxml) {
    return e -> {
      try {
        FXPlugin plugin = (FXPlugin)Class.forName(className).newInstance();
        if(fxml != null)
          plugin.setFxml(fxml);
        if(title != null)
          plugin.setTitle(title);
        FXUtility.initCss(plugin);
        plugin.setDesktopPane(desktop);
        plugin.initPlugin();
        plugin.start();
      }catch(Exception ex) {
        MsgTrash.out(ex);
      }
    };
  }
  
  private static ActionListener createActionListenerJ(String title, FXDesktopPane desktop, String className, String fxml) {
    return e -> {
      Platform.runLater(() -> {
        try {
          FXPlugin plugin = (FXPlugin)Class.forName(className).newInstance();
          if(fxml != null)
            plugin.setFxml(fxml);
          if(title != null)
            plugin.setTitle(title);
          FXUtility.initMainCss(plugin);
          plugin.setDesktopPane(desktop);
          plugin.initPlugin();
          plugin.start();
        }catch(Exception ex) {
          MsgTrash.out(ex);
        }
      });
    };
  }
}