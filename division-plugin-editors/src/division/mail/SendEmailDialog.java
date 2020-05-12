package division.mail;

import division.swing.guimessanger.Messanger;
import bum.editors.util.ObjectLoader;
import bum.util.EmailMessage;
import division.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import org.apache.commons.mail.ByteArrayDataSource;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.log4j.Logger;

public class SendEmailDialog extends DivisionDialog {
  private DivisionTextField    toField       = new DivisionTextField(DivisionTextField.Type.ALL);
  private DivisionTextField    fromField     = new DivisionTextField(DivisionTextField.Type.ALL);
  private DivisionTextField    subjectField  = new DivisionTextField(DivisionTextField.Type.ALL);
  private DivisionTextArea messageBody   = new DivisionTextArea("Введите сообощение...");
  private JScrollPane      messageScroll = new JScrollPane(messageBody);
  
  private DivisionButton okButton = new DivisionButton("Отправить");
  
  EmailMessage message;

  private SendEmailDialog(EmailMessage message) {
    this.message = message;
    String toText = "";
    for(String[] to:message.getTo())
      toText += ", "+to[1]+" <"+to[0]+">";
    toField.setText(!toText.equals("")?toText.substring(2):toText);
    fromField.setText(message.getFromName()+" <"+message.getFromEmail()+">");
    subjectField.setText(message.getSubject());
    messageBody.setText(message.getMessage());
    
    initComponents();
    initEvents();
  }

  private void initComponents() {
    messageBody.setWrapStyleWord(true);
    messageScroll.setPreferredSize(new Dimension(400, 200));
    
    getContentPane().setLayout(new GridBagLayout());
    getContentPane().add(new JLabel("Получатель:"), new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    getContentPane().add(toField,             new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    
    getContentPane().add(new JLabel("Отправитель:"),   new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    getContentPane().add(fromField,           new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    
    getContentPane().add(new JLabel("тема:"), new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    getContentPane().add(subjectField,        new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    
    getContentPane().add(messageScroll, new GridBagConstraints(0, 3, 2, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    getContentPane().add(okButton,      new GridBagConstraints(0, 4, 2, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
  }

  private void initEvents() {
    okButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          message.clearTo();
          String[] toText = toField.getText().split(",");
          for(String to:toText) {
            int firstIndex = to.indexOf("<");
            int lastIndex  = to.indexOf(">");
            String toEmail = (firstIndex>=0&&lastIndex>=0?to.substring(firstIndex+1, lastIndex):to).trim();
            if(toEmail != null && !toEmail.equals("")) {
              String toName  = (firstIndex>=0&&lastIndex>=0?to.substring(0, firstIndex):"").trim();
              message.addTo(toEmail, toName, "utf8");
            }
          }
          
          int firstIndex   = fromField.getText().indexOf("<");
          int lastIndex    = fromField.getText().indexOf(">");
          
          String fromEmail = (firstIndex>=0&&lastIndex>=0?fromField.getText().substring(firstIndex+1, lastIndex):fromField.getText()).trim();
          String fromName  = (firstIndex>=0&&lastIndex>=0?fromField.getText().substring(0, firstIndex):"").trim();
          message.setFromEmail(fromEmail);
          message.setFromName(fromName);
          message.setFromCharset("utf8");
          
          message.setSubject(subjectField.getText());
          message.setMessage(messageBody.getText());
          
          if(message.getTo() == null || message.getTo().length == 0 || 
                  message.getFromEmail() == null || message.getFromEmail().equals("") || 
                  message.getSubject() == null || message.getSubject().equals("") || 
                  message.getMessage() == null || message.getMessage().equals(""))
            JOptionPane.showMessageDialog(SendEmailDialog.this, "Некорректно заполнены поля", "Ошибка", JOptionPane.ERROR_MESSAGE);
          else {
            message.setCharset("utf-8");
            //ObjectLoader.getServer().sendEmail(message);
            sendEmail(message);
            dispose();
          }
        }catch(HeadlessException | RemoteException ex) {
          Messanger.showErrorMessage(SendEmailDialog.this, ex);
        }
      }
    });
  }
  
  private ExecutorService pool = Executors.newCachedThreadPool();
  
  public void sendEmail(final EmailMessage emailMessage) throws RemoteException {
    System.out.println(System.getProperties().get("smtp.host"));
    System.out.println(System.getProperties().get("smtp.port"));
    System.out.println(System.getProperties().get("smtp.user"));
    System.out.println(System.getProperties().get("smtp.password"));
    pool.submit(() -> {
      try {
        MultiPartEmail simpleEmail = new MultiPartEmail();
        simpleEmail.setHostName((String) System.getProperties().get("smtp.host"));
        simpleEmail.setSmtpPort((Integer) System.getProperties().get("smtp.port"));
        if(System.getProperties().get("smtp.user") != null && !System.getProperties().get("smtp.user").equals(""))
          simpleEmail.setAuthentication((String) System.getProperties().get("smtp.user"), (String) System.getProperties().get("smtp.password"));
        
        String[][] to = emailMessage.getTo();
        for(String[] t:to) {
          if(t[0] != null && !t[0].equals("")) {
            if(t[1] != null && t[2] != null)
              simpleEmail.addTo(t[0], t[1], t[2]);
          }
        }
        simpleEmail.setFrom(emailMessage.getFromEmail(), emailMessage.getFromName(), emailMessage.getFromCharset());
        simpleEmail.setSubject(emailMessage.getSubject());
        simpleEmail.setMsg(emailMessage.getMessage());
        
        simpleEmail.setCharset(emailMessage.getCharset());
        
        TreeMap<String,byte[]> attachments = emailMessage.getAttachments();
        for(String key:attachments.keySet()) {
          String[] list = key.split("\n\t");
          simpleEmail.attach(new ByteArrayDataSource(attachments.get(key), list[0]), list.length>1?list[1]:"", list.length>2?list[2]:"");
        }
        
        simpleEmail.send();
      }catch(EmailException | IOException ex) {
        Logger.getRootLogger().error(ex);
      }
    });
  }
  
  public static void sendEmail(String title, EmailMessage message) {
    SendEmailDialog emailDialog = new SendEmailDialog(message);
    emailDialog.setTitle(title);
    emailDialog.setModal(true);
    emailDialog.centerLocation();
    emailDialog.setVisible(true);
  }
}