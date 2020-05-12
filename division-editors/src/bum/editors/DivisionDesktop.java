package bum.editors;

import division.Desktop.DesktopLabel;
import division.swing.guimessanger.Messanger;
import division.xml.Document;
import division.xml.Node;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

public class DivisionDesktop extends JDesktopPane {
  private final ArrayList<EditorGui> editors = new ArrayList<>();
  private final JToolBar tool;

  public DivisionDesktop(JToolBar tool, String name) {
    this.tool = tool;
    setName(name);
    EditorController.setDeskTop(this);
    
    tool.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        repaintTool();
      }
    });
  }
  
  private void repaintTool() {
    int count = 0;
    for(int i=0;i<tool.getComponentCount();i++) {
      if(tool.getComponent(i) instanceof JButton) {
        count++;
      }
    }
    if(count > 0) {
      int w = getWidth()/count;
      for(int i=0;i<tool.getComponentCount();i++) {
        if(tool.getComponent(i) instanceof JButton) {
          tool.getComponent(i).setMinimumSize(new Dimension(w<150?w:150, 20));
          tool.getComponent(i).setMaximumSize(new Dimension(w<150?w:150, 20));
          tool.getComponent(i).setPreferredSize(new Dimension(w<150?w:150, 20));
          tool.getComponent(i).repaint();
        }
      }
    }
    tool.repaint();
  }

  @Override
  public Component add(Component comp) {
    if(comp instanceof JInternalFrame) {
      
      final JInternalFrame frame = (JInternalFrame)comp;
      
      final JButton button = new JButton(frame.getTitle());
      button.setToolTipText(frame.getTitle());
      button.setMaximumSize(new Dimension(150, 20));
      button.setMinimumSize(new Dimension(150, 20));
      button.setPreferredSize(new Dimension(150, 20));
      
      final JPopupMenu pop   = new JPopupMenu();
      final JMenuItem  icon  = new JMenuItem("Восстановить");
      JMenuItem  close = new JMenuItem("Закрыть");
      pop.add(icon);
      pop.add(close);
      
      icon.addActionListener((ActionEvent e) -> {
        try {
          frame.setIcon(!frame.isIcon());
        }catch(Exception ex){}
      });
      
      close.addActionListener((ActionEvent e) -> {
        try {
          frame.setClosed(true);
        }catch(Exception ex){}
      });
      
      button.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          if(e.getModifiers() == MouseEvent.META_MASK) {
            icon.setText(frame.isIcon()?"Восстановить":"Свернуть");
            pop.show(button, e.getPoint().x, e.getPoint().y-pop.getHeight());
          }
        }
      });
      
      button.addActionListener(e -> {
        try {
          if(frame.isIcon()) {
            frame.setIcon(false);
            frame.setSelected(true);
          }else {
            if(!frame.isSelected())
              frame.setSelected(true);
            else frame.setIcon(true);
          }
        }catch(Exception ex) {
          ex.printStackTrace();
        }
      });
      
      frame.addInternalFrameListener(new InternalFrameAdapter() {
        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
          tool.add(button);
          repaintTool();
        }

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
          tool.remove(button);
          repaintTool();
        }

        @Override
        public void internalFrameIconified(InternalFrameEvent e) {
          frame.getDesktopIcon().setVisible(false);
        }
      });
    }
    return super.add(comp);
  }
  
  public EditorGui add(final EditorGui editor) {
    //editor.setActive(false);
    editor.createIternalDialog(true, true, true, true);
    if(editor.getInternalDialog() != null) {
      add(editor.getInternalDialog());
      editors.add(editor);
    }
    return editor;
  }
  
  public void load() {
    try {
      JInternalFrame selectedFrame = null;
      Document xml = Document.load("conf"+File.separator+getName());
      for(Node node:xml.getRootNode().getNodes()) {
        for(EditorGui editor:editors) {
          if(node.getName().equals(editor.getName()) || node.getName().equals(editor.getClass().getName())) {
            editor.getInternalDialog().setVisible(true);
            editor.getInternalDialog().setMaximum(node.getNode("max").getValue().equals("1"));
            editor.getInternalDialog().setIcon(node.getNode("icon").getValue().equals("1"));
            String location = node.getNode("location").getValue();
            if(!editor.getInternalDialog().isMaximum())
              editor.getInternalDialog().setLocation(Integer.parseInt(location.split(",")[0]), Integer.parseInt(location.split(",")[1]));
            if(node.getNode("active").getValue().equals("1"))
              selectedFrame = editor.getInternalDialog();
          }
        }
      }
      if(selectedFrame != null)
        selectedFrame.setSelected(true);
    }catch(Exception ex) {
      ex.printStackTrace();
    }
    
    for(Component com:getComponents())
      if(com instanceof DesktopLabel)
        ((DesktopLabel)com).load();
  }

  public void store() {
    Document xml = new Document("conf"+File.separator+getName());
    for(EditorGui editor:editors) {
      if(editor.getInternalDialog().isVisible() && !editor.getInternalDialog().isClosed()) {
        Node node = new Node(editor.getName() == null?editor.getClass().getName():editor.getName());
        xml.getRootNode().addNode(node);
        Node location = new Node("location", editor.getInternalDialog().getLocation().x+","+editor.getInternalDialog().getLocation().y);
        node.addNode(location);
        Node icon = new Node("icon", editor.getInternalDialog().isIcon()?"1":"0");
        node.addNode(icon);
        Node max = new Node("max", editor.getInternalDialog().isMaximum()?"1":"0");
        node.addNode(max);
        Node active = new Node("active", editor.getInternalDialog().isSelected()?"1":"0");
        node.addNode(active);
      }
    }
    xml.save();
    
    System.out.println(xml.getXML());
    
    for(Component com:getComponents())
      if(com instanceof DesktopLabel)
        ((DesktopLabel)com).store();
  }
}