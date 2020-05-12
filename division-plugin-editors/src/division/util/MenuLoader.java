package division.util;

import bum.editors.DivisionDesktop;
import division.Desktop.DesktopLabel;
import division.xml.Document;
import division.xml.Node;
import java.awt.event.ActionListener;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import org.apache.log4j.Logger;
import division.plugin.DivisionPlugin;
import java.util.TreeMap;

public class MenuLoader {
  private static TreeMap<String, DivisionPlugin> plugins = new TreeMap<>();

  public static void load(DivisionDesktop desktop, JMenuBar bar, String confFile) {
    if(Document.load(confFile).getNode("menubar") != null) {
      Document.load(confFile).getNode("menubar").getNodes().stream().forEach(node -> {
        if(node.getName().equals("menu")) {
          JMenu menu = createMenu(node);
          bar.add(menu);
          load(desktop, menu, node);
        }
      });
    }
    
    if(Document.load(confFile).getNode("desktop") != null) {
      Document.load(confFile).getNode("desktop").getNodes().stream().forEach(node -> {
        if(node.getName().equals("icon")) {
          desktop.add(createDesktopLabel(desktop, node));
        }
      });
    }
  }
  
  private static void load(DivisionDesktop desktop, JMenu menu, Node menuNode) {
    menuNode.getNodes().stream().forEach(n -> {
      switch(n.getName()) {
        case "menu":
          JMenu m = createMenu(n);
          menu.add(m);
          load(desktop, menu, n);
          break;
        case "item":
          JMenuItem item = createItem(desktop, n);
          menu.add(item);
          break;
        case "separator":
          menu.addSeparator();
          break;
      }
    });
  }
  
  private static JMenuItem createItem(DivisionDesktop desktop, Node node) {
    JMenuItem item = new JMenuItem(node.getAttribute("name"));
    try {
      if(node.getAttribute("actionclass") != null && !node.getAttribute("actionclass").equals("")) {
        item.addActionListener((ActionListener) Class.forName(node.getAttribute("actionclass")).newInstance());
      }else if(node.getAttribute("pluginclass") != null && !node.getAttribute("pluginclass").equals("")) {
        item.addActionListener(createActionListener(desktop, node.getAttribute("pluginclass")));
      }
    }catch(ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
      Logger.getRootLogger().error(ex);
    }
    
    if(node.getAttribute("img") != null && !node.getAttribute("img").equals(""))
      item.setIcon(FileLoader.getIcon(node.getAttribute("img")));
    return item;
  }
  
  private static JMenu createMenu(Node node) {
    JMenu menu = new JMenu(node.getAttribute("name"));
    if(node.getAttribute("img") != null && !node.getAttribute("img").equals(""))
      menu.setIcon(FileLoader.getIcon(node.getAttribute("img")));
    return menu;
  }

  private static DesktopLabel createDesktopLabel(DivisionDesktop desktop, Node node) {
    return new DesktopLabel(
            node.getAttribute("name"), 
            FileLoader.getIcon(node.getAttribute("img")), 
            node.getAttribute("name"),
            createActionListener(desktop, node.getAttribute("pluginclass")));
  }
  
  private static ActionListener createActionListener(DivisionDesktop desktop, String className) {
    return e -> {
      try {
        DivisionPlugin plugin = plugins.get(className);
        if(plugin == null) {
          plugin = (DivisionPlugin)Class.forName(className).newInstance();
          plugin.setDesktop(desktop);
        }
        plugin.start();
      }catch(ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
        Logger.getRootLogger().error(ex);
      }
    };
  }
}