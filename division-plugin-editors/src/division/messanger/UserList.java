package division.messanger;

import bum.editors.util.ObjectLoader;
import bum.interfaces.People;
import division.swing.guimessanger.Messanger;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Vector;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.swing.*;
import org.apache.activemq.command.ActiveMQMessage;
import util.filter.local.DBFilter;

public class UserList extends JPanel {
  private DefaultListModel model = new DefaultListModel();
  private JList list = new JList(model);
  private JScrollPane scroll = new JScrollPane(list);
  private MessageDialog dialog = new MessageDialog();
  
  public UserList() {
    super(new BorderLayout());
    initEvents();
  }
  
  public void init() {
    ObjectLoader.initPeopleMessanger(new MessageListener() {
      @Override
      public void onMessage(final Message message) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            showMesage(message);
          }
        });
      }
    });
    
    add(scroll, BorderLayout.CENTER);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setSelectionBackground(new Color(list.getSelectionBackground().getRed(), list.getSelectionBackground().getGreen(), list.getSelectionBackground().getBlue(), 80));
    list.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        String color = isSelected?"WHITE":((boolean)((Vector)value).get(4)?"BLACK":"GRAY");
        value = "<html><body style='width: "+(list.getWidth()-40)+"px; font-weight: bold; color: "+color+";'>  "
                +((Vector)value).get(1)+" "+((Vector)value).get(2)+" "+((Vector)value).get(3)
                +"</body></html>";
        return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      }
    });

    List<List> data = ObjectLoader.getData(DBFilter.create(People.class).AND_EQUAL("type", "CURRENT").AND_EQUAL("tmp", false).AND_NOT_EQUAL("id", ObjectLoader.getClient().getPeopleId()), 
            new String[]{"id", "surName", "name", "lastName"}, new String[]{"surName"});
    for(List d:data) {
      d.add(false);
      model.addElement(d);
    }
  }
  
  private void showMesage(Message message) {
    try {
      Integer peopleId = ObjectLoader.getClient().getPeopleId();
      switch(message.getJMSType()) {
        case "ONLINE":
          if(peopleId.intValue() != message.getIntProperty("people")) {
            boolean exist = false;
            for(int i=0;i<model.size();i++) {
              if(((Vector)model.get(i)).get(0).equals(message.getIntProperty("people"))) {
                ((Vector)model.get(i)).set(4, true);
                exist = true;
                break;
              }
            }
            if(!exist) {
              List<List> data = ObjectLoader.getData(People.class, new Integer[]{message.getIntProperty("people")}, new String[]{"id", "surName", "name", "lastName"}, new String[]{"surName"});
              if(!data.isEmpty()) {
                data.get(0).add(true);
                model.addElement(data.get(0));
              }
            }
          }
          break;
        case "OFFLINE":
          if(peopleId.intValue() != message.getIntProperty("people"))
            for(int i=0;i<model.size();i++)
              if(((Vector)model.get(i)).get(0).equals(message.getIntProperty("people")))
                ((Vector)model.get(i)).set(4, false);
          break;
        case "ANYBODY-IS-ONLINE":
          javax.jms.Message m = new ActiveMQMessage();
          m.setJMSType("ONLINE");
          m.setIntProperty("people", peopleId);
          m.setStringProperty("topic.name", "people-messanger");
          ObjectLoader.sendMessage(m);
          break;
        case "TEXT-MESSAGE":
          dialog.addMessage(message);
          break;
      }
    }catch(JMSException ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  private void initEvents() {
    list.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if(e.getModifiers() != MouseEvent.META_MASK && e.getClickCount() == 2) {
          int index = list.locationToIndex(e.getPoint());
          if(index >= 0) {
            dialog.createUserTab(((Vector)model.get(index)).get(1)+" "+((Vector)model.get(index)).get(2)+" "+((Vector)model.get(index)).get(3), 
                    (Integer)((Vector)model.get(index)).get(0), true);
          }
        }
      }
    });
  }
}