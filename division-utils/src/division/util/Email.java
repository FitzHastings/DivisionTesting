package division.util;

import division.util.EmailUtil.Addres;
import division.util.EmailUtil.Attachment;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.mail.EmailException;

public class Email {
  private Addres[]     to = new Addres[0];
  private Addres       from;
  private String       subject;
  private String       message;
  private String       charset;
  private Attachment[] files = new Attachment[0];
  
  public static String PDF = "application/pdf";
  public static String RTF = "application/rtf";
  public static String TXT = "application/txt";
  public static String JPG = "application/jpg";
  public static String PNG = "application/png";
  public static String GIF = "application/gif";
  public static String DOC = "application/doc";
  
  public void addFile(byte[] byteData, String type, String name, String description) throws UnsupportedEncodingException {
    files = ArrayUtils.add(files, new Attachment(byteData, type, name, description));
  }
  
  public void addToEmail(String email, String name, String charset) {
    to = ArrayUtils.add(to, new Addres(email, name, charset));
  }
  
  public Addres[] getToEmail() {
    return to;
  }
  
  public void setFromEmail(String email, String name, String charset) {
    from = new Addres(email, name, charset);
  }
  
  public Addres getFromEmail() {
    return from;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getCharset() {
    return charset;
  }

  public void setCharset(String charset) {
    this.charset = charset;
  }
  
  public String send(String smtpHost, Integer smtpPort, String smtpUser, String smtpPassword) throws EmailException, IOException {
    return EmailUtil.sendEmail(smtpHost, smtpPort, smtpUser, smtpPassword, to, from, subject, message, charset, files);
  }
}