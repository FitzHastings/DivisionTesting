package bum.util;

import java.io.*;
import java.util.TreeMap;
import org.apache.commons.lang3.ArrayUtils;

public class EmailMessage implements Serializable {
  private String fromEmail;
  private String fromName;
  private String fromCharset;
  private String subject;
  private String message;
  private String charset = "utf8";
  
  private String[][] to;
    
  private TreeMap<String,byte[]> attachments = new TreeMap<>();

  public EmailMessage() {
  }

  public EmailMessage(String fromEmail, String fromName, String fromCharset, String subject, String message) {
    this.fromEmail = fromEmail;
    this.fromName = fromName;
    this.fromCharset = fromCharset;
    this.subject = subject;
    this.message = message;
  }
  
  public void addTo(String toEmail, String toName, String toCharset) {
    if(to != null)
      for(String[] s:to)
        if(s[0].equals(toEmail))
          return;
    to = (String[][]) ArrayUtils.add(to, new String[]{toEmail,toName,toCharset});
  }
  
  public void clearTo() {
    to = null;
  }
  
  public String[][] getTo() {
    return to;
  }

  public TreeMap<String, byte[]> getAttachments() {
    return attachments;
  }

  public String getFromCharset() {
    return fromCharset;
  }

  public void setFromCharset(String fromCharset) {
    this.fromCharset = fromCharset;
  }

  public String getFromEmail() {
    return fromEmail;
  }

  public void setFromEmail(String fromEmail) {
    this.fromEmail = fromEmail;
  }

  public String getFromName() {
    return fromName;
  }

  public void setFromName(String fromName) {
    this.fromName = fromName;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }
  
  public void attach(String fileName, String type, String name, String description) throws FileNotFoundException, IOException {
    attach(new File(fileName), type, name, description);
  }
  
  public void attach(File file, String type, String name, String description) throws FileNotFoundException, IOException {
    FileInputStream in = new FileInputStream(file);
    byte[] arr = new byte[in.available()];
    in.read(arr);
    attach(arr, type, name, description);
  }
  
  public void attach(byte[] arr, String type, String name, String description) {
    attachments.put(type+"\n\t"+name+"\n\t"+description, arr);
  }

  public String getCharset() {
    return charset;
  }

  public void setCharset(String charset) {
    this.charset = charset;
  }
}