package division.messanger;

import bum.editors.EditorController;
import bum.editors.util.ObjectLoader;
import bum.interfaces.People;
import division.swing.guimessanger.Messanger;
import division.util.FileLoader;
import division.util.Utility;
import java.awt.*;
import java.awt.event.*;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;

public class MessageDialog {
  private JInternalFrame frame;
  private JPanel panel = new JPanel(new BorderLayout());
  
  private JTabbedPane tab = new JTabbedPane();
  private TreeMap<Integer, JEditorPane> peopleHistoryMessages = new TreeMap<>();
  private ImageIcon icon = FileLoader.getIcon("mail_icon.png");

  public MessageDialog() {
    panel.add(tab, BorderLayout.CENTER);
    panel.setPreferredSize(new Dimension(600, 400));
  }
  
  public void addMessage(Message message) {
    try {
      Integer peopleId = message.getIntProperty("people");
      if(!peopleHistoryMessages.containsKey(peopleId)) {
        java.util.List<java.util.List> data = ObjectLoader.getData(People.class, new Integer[]{peopleId}, new String[]{"surName", "name", "lastName"});
        if(!data.isEmpty())
          createUserTab(data.get(0).get(0)+" "+data.get(0).get(1)+" "+data.get(0).get(2), peopleId, false);
      }
      JEditorPane historyMessages = peopleHistoryMessages.get(peopleId);
      if(historyMessages != null) {
        String dateTime = Utility.format(message.getJMSTimestamp());
        historyMessages.setText(getText(historyMessages.getText(), "<font style='color:RED'; font-family:Arial; font-size:12pt;><b>"+dateTime+
                ": </b>"+message.getStringProperty("TEXT").replaceAll("\n|\r", "<br/>") +"</font><br/>"));
        
        if(!historyMessages.isShowing())
          tab.setIconAt(tab.indexOfComponent(peopleHistoryMessages.get(peopleId).getParent().getParent().getParent()), icon);
        
        historyMessages.scrollRectToVisible(new Rectangle(0, historyMessages.getHeight()-1, 1, 1));
        showDialog();
      }
    }catch(JMSException ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  private void showDialog() {
    try {
      if(frame == null || frame.isClosed()) {
        frame = new JInternalFrame("Диалоги", true, true, true, true);
        EditorController.getDeskTop().add(frame);
        frame.setContentPane(panel);
        frame.pack();
      }
      
      if(!frame.isVisible())
        frame.setVisible(true);
      
      if(frame.isIcon())
        frame.setIcon(false);
      
      EditorController.getDeskTop().setLayer(frame, JLayeredPane.MODAL_LAYER);
      frame.setSelected(true);
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  private String getText(String text, String addText) {
    text = text.replaceAll("\n|\r", "[line]");
    Pattern p = Pattern.compile(".*\\<body\\>(.*)\\<\\/body\\>.*");
    Matcher m = p.matcher(text);
    if(m.find()) {
      text = m.group(1).replaceAll("\\[line\\]", "\n")+addText+"\n";
      return text;
    }else return "";
  }
  
  public void createUserTab(String userName, final Integer peopleId, boolean selected) {
    if(!peopleHistoryMessages.containsKey(peopleId)) {
      JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
      JSplitPane lastSplit = (JSplitPane) tab.getSelectedComponent();
      split.setDividerLocation(lastSplit==null?200:lastSplit.getDividerLocation());
      final JEditorPane historyMessages = new JEditorPane();
      historyMessages.setContentType("text/html");
      historyMessages.setEditable(false);
      JScrollPane historyScroll   = new JScrollPane(historyMessages);

      final JEditorPane message = new JEditorPane();
      JScrollPane scroll  = new JScrollPane(message);

      split.add(historyScroll, JSplitPane.TOP);
      split.add(scroll, JSplitPane.BOTTOM);
      
      historyMessages.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentShown(ComponentEvent e) {
          tab.setIconAt(tab.indexOfComponent(peopleHistoryMessages.get(peopleId).getParent().getParent().getParent()), null);
        }
      });

      message.addKeyListener(new KeyAdapter() {
        private long enter = -1;
        @Override
        public void keyReleased(KeyEvent e) {
          if(!message.getText().equals("") && e.getKeyCode() == KeyEvent.VK_ENTER) {
            if(e.isControlDown() || enter > -1 && (System.currentTimeMillis()-enter) < 500) {
              long l = ObjectLoader.sendMessageToPeople(peopleId, message.getText());
              if(l > 0) {
                String dateTime = Utility.format(l);
                historyMessages.setText(getText(historyMessages.getText(), 
                        "<font style='color:BLUE; font-family:Arial; font-size:12pt;'><b>"+dateTime+": </b>"+message.getText().trim().replaceAll("\n|\r", "<br/>")+"</font><br/>"));
                message.setText("");
              }
              enter = -1;
            }else enter = System.currentTimeMillis();
          }
        }
      });
      
      tab.addTab(userName, split);
      peopleHistoryMessages.put(peopleId, historyMessages);
      tab.setTabComponentAt(tab.getTabCount()-1, new ButtonTabComponent(tab));
    }
    if(selected)
      tab.setSelectedIndex(tab.indexOfComponent(peopleHistoryMessages.get(peopleId).getParent().getParent().getParent()));
    showDialog();
  }
  
  
  
 
  class ButtonTabComponent extends JPanel {
    private final JTabbedPane pane;
    
    public ButtonTabComponent(final JTabbedPane pane) {
      //unset default FlowLayout' gaps
      super(new FlowLayout(FlowLayout.LEFT, 0, 0));
      if (pane == null) {
        throw new NullPointerException("TabbedPane is null");
      }
      this.pane = pane;
      setOpaque(false);
      //make JLabel read titles from JTabbedPane
      JLabel label = new JLabel() {
        @Override
        public String getText() {
          int i = pane.indexOfTabComponent(ButtonTabComponent.this);
          if (i != -1)
            return pane.getTitleAt(i);
          return null;
        }

        @Override
        public Icon getIcon() {
          int i = pane.indexOfTabComponent(ButtonTabComponent.this);
          if (i != -1)
            return pane.getIconAt(i);
          return null;
        }
      };
      add(label);
      //add more space between the label and the button
      label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
      //tab button
      JButton button = new TabButton();
      add(button);
      //add more space to the top of the component
      setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
    }
    
    class TabButton extends JButton implements ActionListener {
      public TabButton() {
        int size = 17;
        setPreferredSize(new Dimension(size, size));
        setToolTipText("close this tab");
        //Make the button looks the same for all Laf's
        setUI(new BasicButtonUI());
        //Make it transparent
        setContentAreaFilled(false);
        //No need to be focusable
        setFocusable(false);
        setBorder(BorderFactory.createEtchedBorder());
        setBorderPainted(false);
        //Making nice rollover effect
        //we use the same listener for all buttons
        addMouseListener(buttonMouseListener);
        setRolloverEnabled(true);
        //Close the proper tab by clicking the button
        addActionListener(TabButton.this);
      }

      @Override
      public void actionPerformed(ActionEvent e) {
        int i = pane.indexOfTabComponent(ButtonTabComponent.this);
        if (i != -1) {
          for(Integer peopleId:peopleHistoryMessages.keySet().toArray(new Integer[0]))
            if(tab.indexOfComponent(peopleHistoryMessages.get(peopleId).getParent().getParent().getParent()) == i)
              peopleHistoryMessages.remove(peopleId);
          pane.remove(i);
          if(pane.getTabCount() == 0) {
            try {
              MessageDialog.this.frame.dispose();
            }catch(Exception ex){}
            //MessageDialog.this.setVisible(false);
          }
        }
      }

      //we don't want to update UI for this button
      @Override
      public void updateUI() {

      }

      //paint the cross
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        //shift the image for pressed buttons
        if (getModel().isPressed()) {
          g2.translate(1, 1);
        }
        g2.setStroke(new BasicStroke(2));
        g2.setColor(Color.GRAY);
        if (getModel().isRollover()) {
          g2.setColor(Color.BLACK);
        }
        int delta = 6;
        g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
        g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
        g2.dispose();
      }

      MouseListener buttonMouseListener = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
          Component component = e.getComponent();
          if (component instanceof AbstractButton) {
            AbstractButton button = (AbstractButton) component;
            button.setBorderPainted(true);
          }
        }

        @Override
        public void mouseExited(MouseEvent e) {
          Component component = e.getComponent();
          if (component instanceof AbstractButton) {
            AbstractButton button = (AbstractButton) component;
            button.setBorderPainted(false);
          }
        }
      };
    }
  }
}