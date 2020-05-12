package division.util;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;
import org.apache.commons.mail.ByteArrayDataSource;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;

public class EmailUtil implements Serializable {
  public static String sendEmail(
          String smtpHost, 
          Integer smtpPort, 
          String smtpUser, 
          String smtpPassword, 
          String toAddres, 
          String toName, 
          String fromAddress, 
          String fromName, 
          String subject, 
          String message) throws EmailException, IOException {
    return sendEmail(
            smtpHost,
            smtpPort,
            smtpUser,
            smtpPassword,
            getAddreses(toAddres, toName),
            new Addres(fromAddress, fromName),
            subject,
            message,
            "utf8",
            (Attachment[]) null);
  }
  
  public static String sendEmail(
          String smtpHost, 
          Integer smtpPort, 
          String smtpUser, 
          String smtpPassword, 
          String toAddres, 
          String toName, 
          String fromAddress, 
          String fromName, 
          String subject, 
          String message,
          Attachment... attachments) throws EmailException, IOException {
    return sendEmail(
            smtpHost,
            smtpPort,
            smtpUser,
            smtpPassword,
            getAddreses(toAddres, toName),
            new Addres(fromAddress, fromName),
            subject,
            message,
            "utf8",
            attachments);
  }
  
  
  
  public static String sendEmail(
          String smtpHost, 
          Integer smtpPort, 
          String smtpUser, 
          String smtpPassword, 
          String to, 
          Addres from, 
          String subject, 
          String message,
          String charset) throws EmailException, IOException {
    return sendEmail(smtpHost, smtpPort, smtpUser, smtpPassword, new Addres[]{new Addres(to)}, from, subject, message, charset, (Attachment[]) null);
  }
  
  public static String sendEmail(
          String smtpHost, 
          Integer smtpPort, 
          String smtpUser, 
          String smtpPassword, 
          String to, 
          Addres from, 
          String subject, 
          String message,
          String charset,
          Attachment... attachments) throws EmailException, IOException {
    return sendEmail(smtpHost, smtpPort, smtpUser, smtpPassword, new Addres[]{new Addres(to)}, from, subject, message, charset, attachments);
  }
  
  public static String sendEmail(
          String smtpHost, 
          Integer smtpPort, 
          String smtpUser, 
          String smtpPassword, 
          Addres[] to, 
          Addres from, 
          String subject, 
          String message,
          String charset) throws EmailException, IOException {
    return sendEmail(smtpHost, smtpPort, smtpUser, smtpPassword, to, from, subject, message, charset, (Attachment[]) null);
  }
  
  public static Addres[] getAddreses(String address, String name) {
    List<Addres> addresList = new ArrayList<>();
      for(String na:address.split(",")) {
        for(String n:na.trim().split(" ")) {
          if(!n.trim().equals(""))
            addresList.add(new Addres(n.trim(), name.trim()));
        }
      }
    return addresList.toArray(new Addres[0]);
  }
  
  public static String sendEmail(
          String smtpHost, 
          Integer smtpPort, 
          String smtpUser, 
          String smtpPassword, 
          Addres[] to, 
          Addres from, 
          String subject, 
          String message,
          String charset,
          Attachment... attachments) throws EmailException, IOException {
    
    for(Object k:System.getProperties().keySet())
      System.out.println(k+" = "+String.valueOf(System.getProperties().get(k)));
    
    MultiPartEmail simpleEmail = new MultiPartEmail();
    simpleEmail.setHostName(smtpHost);
    simpleEmail.setSmtpPort(smtpPort);
    simpleEmail.setAuthentication(smtpUser, smtpPassword);
    
    String emails = "";
    for(Addres t:to) {
      emails += t.getEmail()+" ";
      if(t.getEmail() != null && !"".equals(t.getEmail())) {
        if(t.getName() == null && t.getCharset() == null)
          simpleEmail.addTo(t.getEmail());
        else if(t.getCharset() == null)
          simpleEmail.addTo(t.getEmail(), t.getName());
        else simpleEmail.addTo(t.getEmail(), t.getName(), t.getCharset());
      }
    }
    simpleEmail.setFrom(from.getEmail(), from.getName(), from.getCharset());
    simpleEmail.addReplyTo(from.getEmail());
    simpleEmail.setSubject(subject);
    simpleEmail.setMsg(message);

    simpleEmail.setCharset(charset);

    if(attachments != null) {
      for(Attachment attachment:attachments) {
        if(attachment.getByteData() != null)
          simpleEmail.attach(new ByteArrayDataSource(attachment.getByteData(), attachment.getType()), attachment.getName(), attachment.getDescription());
      }
    }
    String str = simpleEmail.send();
    System.out.println("SEND EMAIL TO: "+emails);
    return str;
  }
  
  public static class Addres implements Serializable {
    String email;
    String name;
    String charset;

    public Addres(String email, String name, String charset) {
      this(email, name);
      this.charset = charset;
    }

    public Addres(String email, String name) {
      this(email);
      this.name = name;
    }

    public Addres(String email) {
      try {
        new InternetAddress(email);
      }catch (AddressException ex) {
        email = "webmaster@webmaster.ru";
      }
      this.email = email;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getCharset() {
      return charset;
    }

    public void setCharset(String charset) {
      this.charset = charset;
    }
  }
  
  public static boolean checkAddress(String address) {
    return address.matches("^([a-z0-9_-]+\\.)*[a-z0-9_-]+@[a-z0-9_-]+(\\.[a-z0-9_-]+)*\\.[a-z]{2,6}$");
  }
  
  public static class Attachment /*extends EmailAttachment*/ implements Serializable {
    private String name;
    private String description;
    private byte[] byteData;
    private String type;
    
    public Attachment(byte[] byteData, String type) throws UnsupportedEncodingException {
      this.byteData = byteData;
      setData(type, "", "");
    }
    
    public Attachment(byte[] byteData, String type, String name) throws UnsupportedEncodingException {
      this.byteData = byteData;
      setData(type, name, "");
    }
    
    public Attachment(byte[] byteData, String type, String name, String description) throws UnsupportedEncodingException {
      this.byteData = byteData;
      setData(type, name, description);
    }
    
    private void setData(String type, String name, String description) throws UnsupportedEncodingException {
      this.type = type;
      setName(name);
      setDescription(name);
    }

    public byte[] getByteData() {
      return byteData;
    }

    public void setByteData(byte[] byteData) {
      this.byteData = byteData;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) throws UnsupportedEncodingException {
      this.name = MimeUtility.encodeWord(name);
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }
}