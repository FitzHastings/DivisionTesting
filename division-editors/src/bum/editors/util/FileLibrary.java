package bum.editors.util;

import bum.editors.EditorGui;
import division.scale.PeriodScale;
import division.swing.DivisionSplitPane;
import division.swing.LinkLabel;
import division.swing.guimessanger.Messanger;
import division.swing.table.SplitTable;
import division.util.FileLoader;
import division.xml.Document;
import division.xml.Node;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;
import org.apache.log4j.Logger;

public class FileLibrary {
  public static void load(ArrayList components, String fileName) {
    try {
      if(components == null)
        return;
      System.out.println("Загружаю компонент из "+fileName);
      FileLoader.createFileIfNotExists("conf"+File.separator+"gui"+File.separator+fileName, false);
      Document document = Document.load("conf"+File.separator+"gui"+File.separator+fileName);
      if(document != null && document.getRootNode() != null) {
        for(Object comp:components) {
          if(comp instanceof Component) {
            Component component = (Component)comp;
            Node node = document.getRootNode().getNode(component.getName());
            if(node != null) {
              if(component instanceof SplitTable) {

              }else if(component instanceof JSplitPane) {
                int location = Integer.parseInt(node.getAttribute("location"));
                ((JSplitPane)component).setDividerLocation(location);
                if(component instanceof DivisionSplitPane) {
                  int normalLocation = Integer.parseInt(node.getAttribute("normalLocation"));
                  ((DivisionSplitPane)component).setNormalLocation(normalLocation);
                }
              }else if(component instanceof JTable) {
                if(component instanceof PeriodScale) {
                  PeriodScale scale = (PeriodScale) component;
                  Node dayWidth = node.getNode("dayWidth");
                  if(!"".equals(dayWidth.getValue()))
                    scale.setDayWidth(Integer.valueOf(dayWidth.getValue()));
                }else {
                  JTable table = (JTable)component;
                  Node columns = node.getNode("columns");
                  for(int i=table.getColumnCount()-1;i>=0;i--) {
                    Node column = columns.getNode(i);
                    int index  = Integer.parseInt(column.getAttribute("index"));
                    int moveTo = Integer.parseInt(column.getAttribute("moveTo"));
                    int size   = Integer.parseInt(column.getAttribute("size"));
                    table.getColumnModel().getColumn(index).setPreferredWidth(size);
                    int viewInde = table.convertColumnIndexToView(index);
                    if(viewInde != moveTo)
                      table.getColumnModel().moveColumn(index, moveTo);
                  }
                }
              }else if(component instanceof JLabel) {
                ((JLabel)component).setText(node.getAttribute("text"));
                ((JLabel)component).setForeground(new Color(Integer.valueOf(node.getAttribute("foreground"))));
                ((JLabel)component).setForeground(new Color(Integer.valueOf(node.getAttribute("background"))));
                if(component instanceof LinkLabel) {
                  ((LinkLabel)component).setLinkColor(new Color(Integer.valueOf(node.getAttribute("linkcolor"))));
                  ((LinkLabel)component).setHoverColor(new Color(Integer.valueOf(node.getAttribute("hovercolor"))));
                  ((LinkLabel)component).setHoverColor(new Color(Integer.valueOf(node.getAttribute("hovercolor"))));
                }
              }else {
                int width  = Integer.parseInt(node.getAttribute("width"));
                int height = Integer.parseInt(node.getAttribute("height"));
                component.setPreferredSize(new Dimension(width, height));
                component.setSize(width, height);
                if(component instanceof Window) {
                  int x = Integer.parseInt(node.getAttribute("x"));
                  int y = Integer.parseInt(node.getAttribute("y"));
                  component.setLocation(x, y);
                }
              }
            }
          }else if(comp instanceof EditorGui) {
            ((EditorGui)comp).load();
          }
        }
      }
    }catch(Exception ex) {
      System.out.println("Ошибка при чтении файла "+fileName);
      Messanger.showErrorMessage(ex);
    }
  }

  public static void store(ArrayList components, String fileName) {
    System.out.println("Сохраняю компонент в "+fileName);
    FileLoader.createFileIfNotExists("conf"+File.separator+"gui"+File.separator+fileName, false);
    Node guiNode = new Node("GUIinformations");
    Document document = new Document("conf"+File.separator+"gui"+File.separator+fileName, guiNode);
    for(Object comp:components) {
      if(comp instanceof Component) {
        Component component = (Component)comp;
        Node node = new Node(component.getName());
        if(component instanceof SplitTable) {
        }else if(component instanceof JSplitPane) {
          node.setAttribute("location", String.valueOf(((JSplitPane)component).getDividerLocation()));
          if(component instanceof DivisionSplitPane)
            node.setAttribute("normalLocation", String.valueOf(((DivisionSplitPane)component).getNormalLocation()));
        }else if(component instanceof JTable) {
          if(component instanceof PeriodScale) {
            PeriodScale scale = (PeriodScale) component;
            Node dayWidth = new Node("dayWidth");
            dayWidth.setValue(String.valueOf(scale.getDayWidth()));
            node.addNode(dayWidth);
          }else {
            Node columns  = new Node("columns");
            node.addNode(columns);
            JTable           table       = (JTable)component;
            TableColumnModel columnModel = table.getColumnModel();
            for(int i=0;i<columnModel.getColumnCount();i++) {
              String modelIndex = String.valueOf(i);
              String viewIndex  = String.valueOf(table.convertColumnIndexToView(i));
              Node column = new Node("column");
              column.setAttribute("index", modelIndex);
              column.setAttribute("size", String.valueOf(columnModel.getColumn(i).getWidth()));
              column.setAttribute("moveTo", viewIndex);
              columns.addNode(column);
            }
          }
        }else if(component instanceof JLabel) {
          node.setAttribute("text", ((JLabel)component).getText());
          node.setAttribute("foreground", String.valueOf(((JLabel)component).getForeground().getRGB()));
          node.setAttribute("background", String.valueOf(((JLabel)component).getBackground().getRGB()));
          if(component instanceof LinkLabel) {
            node.setAttribute("linkcolor", String.valueOf(((LinkLabel)component).getLinkColor().getRGB()));
            node.setAttribute("hovercolor", String.valueOf(((LinkLabel)component).getHoverColor().getRGB()));
          }
        }else {
          node.setAttribute("width",  String.valueOf(component.getSize().width));
          node.setAttribute("height", String.valueOf(component.getSize().height));
          if(component instanceof Window) {
            node.setAttribute("x", String.valueOf(component.getLocation().x));
            node.setAttribute("y", String.valueOf(component.getLocation().y));
          }
        }
        guiNode.addNode(node);
      }else if(comp instanceof EditorGui)
        ((EditorGui)comp).store();
    }
    document.save();
  }
}