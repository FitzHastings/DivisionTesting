package bum.interfaces;

import bum.util.EmailMessage;
import division.util.EmailUtil;
import division.xml.Document;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import mapping.MappingObject;
import org.apache.commons.mail.EmailException;
import util.Client;
import util.RemoteSession;

public interface Server extends Remote {
  public Client registrateClient(Client clientServer) throws RemoteException;
  public Integer[] getRegistrateWorkers() throws RemoteException;
  public Integer[] getRegistratePeoples() throws RemoteException;
  public Client[] getRegistrateClients() throws RemoteException;
  
  public RemoteSession createSession(Client clientServer, boolean autoCommit) throws RemoteException;
  public Class[] getClasses() throws ClassNotFoundException, RemoteException;
  
  public String getClientName(Class<? extends MappingObject> objectClass) throws RemoteException;
  public Map<String, Map<String,Object>> getFieldsInfo(Class<? extends MappingObject> objectClass) throws RemoteException;
  
  public Document getExportData(Class<? extends MappingObject> className, Integer[] ids) throws RemoteException;
  public void getExportData(Class<? extends MappingObject> className, Integer[] ids, Integer limit, String topicName) throws RemoteException;
  
  public String sendEmail(
          String smtpHost, 
          Integer smtpPort, 
          String smtpUser, 
          String smtpPassword, 
          EmailUtil.Addres[] to, 
          EmailUtil.Addres from, 
          String subject, 
          String message,
          String charset,
          EmailUtil.Attachment... attachments) throws RemoteException, EmailException, IOException;
  
  public void sendEmail(EmailMessage emailMessage) throws RemoteException;
  public Map<Integer, String> getEmailMessages(String protocol, String host, int port, String email, String login, String password) throws RemoteException;
  
  //public void sendMessage(String topicName, javax.jms.Message message) throws RemoteException;
  //public void sendMessage(String topicName, Serializable message) throws RemoteException;
  
  public void getFile(String fileName, String topicName) throws RemoteException;
  public byte[] getFile(String fileName) throws RemoteException;
  public Document getUpdatePaths() throws RemoteException;
  public Map<String, String> getUpdates(Map<String, String> clientFiles) throws RemoteException;
  public String getMD5Hash(Class<? extends MappingObject> className, Integer id) throws RemoteException;
  public String getMD5Hash(MappingObject object) throws RemoteException;
}