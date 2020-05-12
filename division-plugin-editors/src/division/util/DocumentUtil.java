package division.util;

import bum.editors.EditorController;
import bum.editors.util.ObjectLoader;
import bum.interfaces.*;
import division.ClientMain;
import division.fx.PropertyMap;
import division.mail.EmailDialog;
import division.mail.EmailListener;
import division.swing.guimessanger.Messanger;
import documents.FOP;
import documents.PreviewDialog;
import division.swing.LocalProcessing;
import java.awt.print.PrinterJob;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.collections.FXCollections;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.apache.commons.lang.ArrayUtils;
import util.filter.local.DBFilter;

public class DocumentUtil {
  private static String getTagXml(String xml, String tagName) {
    String body = null;
    Pattern p = Pattern.compile("<"+tagName+"[^>]*>([\\s\\S]*)</"+tagName+">");
    Matcher m = p.matcher(xml);
    if(m.find())
      body = m.group(1);
    return body;
  }
  
  private static String[] getTags(String xml, String tagName) {
    String[] tags = new String[0];
    Pattern p = Pattern.compile("(<"+tagName+"[^>]*>)");
    Matcher m = p.matcher(xml);
    while(m.find())
      tags = (String[]) ArrayUtils.add(tags, m.group(1));
    return tags;
  }
  
  private static String getAttr(String tag, String attrName) {
    String attr = null;
    Pattern p = Pattern.compile(attrName+"\\s*=\\s*\"?([^\\s\"]+)\"?\\s");
    Matcher m = p.matcher(tag);
    if(m.find())
      attr = m.group(1);
    return attr;
  }
  
  public static String mergeXML(String[] xmls) {
    String XML = "";
    TreeMap<String,String> types = new TreeMap<>();
    for(int i=0;i<xmls.length;i++) {
      for(String type:getTags(xmls[i], "page-type")) {
        String name = getAttr(type, "name");
        if(!types.containsKey(name))
          types.put(name, type);
      }
      XML   += getTagXml(xmls[i].replaceAll("<page-type[^>]*>", ""), "document");
    }
    for(String type:types.values())
      XML = type+XML;
    XML = "<document>"+XML+"</document>";
    return XML;
  }
  
  
  
  public static List<PropertyMap> getDocumentsDataList(Integer... ids) {
    return ObjectLoader.getList(DBFilter.create(CreatedDocument.class).AND_IN("id", ids).AND_EQUAL("stornoDate", null),
              "id",
              "document",
              "params",
              "sendType",
              "name",
              "customerCompanyPartition",
              "sellerCompanyPartition",
              "customer-name=query:getCompanyName((SELECT [CompanyPartition(company)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[CreatedDocument(customerCompanyPartition)]))",
              "customer-post-addr=query:(SELECT [CompanyPartition(postAddres)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[CreatedDocument(customerCompanyPartition)])",
              "customer-telefon=query:(SELECT [CompanyPartition(telefon)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[CreatedDocument(customerCompanyPartition)])",
              "customer-email=query:(SELECT [CompanyPartition(email)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[CreatedDocument(customerCompanyPartition)])",
              "customer-contact-info=query:(SELECT [CompanyPartition(contactInfo)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[CreatedDocument(customerCompanyPartition)])",
              "customer-template",
              "seller-template",
              "template",
              "customerDefTemps=query:SELECT [CompanyPartition(defaultTemplates)] FROM [CompanyPartition] WHERE id=[CreatedDocument(customerCompanyPartition)]",
              "sellerDefTemps=query:SELECT [CompanyPartition(defaultTemplates)] FROM [CompanyPartition] WHERE id=[CreatedDocument(sellerCompanyPartition)]",
              "number",
              "document_name",
              "seller-email=query:(SELECT [CompanyPartition(email)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[CreatedDocument(sellerCompanyPartition)])",
              "seller-name=query:getCompanyName((SELECT [CompanyPartition(company)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[CreatedDocument(sellerCompanyPartition)]))"
            );
  }

  public static PrintQuene printQuene = new PrintQuene();
  
  static class PrintQuene extends ConcurrentLinkedQueue<PrintTask> {
    public PrintQuene() {
      Thread t = new Thread(() -> {
        try {
          while(true) {
            if(!isEmpty()) {
              PrintTask printTask = poll();
              if(printTask != null)
                printTask.print();
            }else Thread.sleep(2000);
          }
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      });
      t.start();
    }
  }
  
  private abstract static class PrintTask {
    protected final LocalProcessing processing = new LocalProcessing(ClientMain.getInstance());
    public abstract void print();
  }
  
  private static String[] getXMLS(final List<PropertyMap> documentsData, Integer templateId, final boolean complect, LocalProcessing processing) throws Exception {
    String[] xmls = new String[0];
    String addr_XML = (String)ObjectLoader.executeQuery("SELECT [DocumentXMLTemplate(XML)] FROM [DocumentXMLTemplate] WHERE [DocumentXMLTemplate(main)]=true "
            + "AND [DocumentXMLTemplate(document)]=(SELECT id FROM [Document] WHERE [Document(name)]='Адрес отправки')", true).stream().findFirst().orElseGet(() -> FXCollections.observableArrayList("")).get(0);
    String[] moduls = PropertyMap.getArrayFromList(ObjectLoader.getList(JSModul.class, "script"), "script", String.class);
    
    TreeMap<Integer,String> templs = new TreeMap<>();
    
    Integer companyPartitionId = null;
    for(int i=0;i<documentsData.size();i++) {
      if(documentsData.get(i).isNotNull("params") && documentsData.get(i).getValue("params") instanceof byte[]) {
        try {
          documentsData.get(i).setValue("params", GzipUtil.deserializable(documentsData.get(i).getBytes("params")));
        }catch(Exception ex) {
          documentsData.get(i).setValue("params", GzipUtil.getObjectFromGzip(documentsData.get(i).getBytes("params")));
        }
      }
      
      if(processing != null) {
        processing.setText("загружаю документ \""+documentsData.get(i).getString("name")+"\" №"+documentsData.get(i).getString("number")+"...");
        processing.setValue(i);
      }

      if(complect && addr_XML != null && !addr_XML.equals("") && (companyPartitionId == null || !companyPartitionId.equals(documentsData.get(i).getInteger("customerCompanyPartition")))) {
        companyPartitionId = documentsData.get(i).getInteger("customerCompanyPartition");
        Map<String, Object> mapForTemplate = new TreeMap<>();
        mapForTemplate.put("customer",         documentsData.get(i).getString("customer-name"));
        mapForTemplate.put("customerPostAddr", documentsData.get(i).getString("customer-post-addr"));
        mapForTemplate.put("telefon",          documentsData.get(i).getString("customer-telefon"));
        mapForTemplate.put("contacts",         documentsData.get(i).getString("customer-email"));
        xmls = (String[]) ArrayUtils.add(xmls, FOP.get_XML_From_XMLTemplate(addr_XML, Arrays.asList(moduls), mapForTemplate));
      }
      
      Integer currentTemplateId = templateId;
      if(currentTemplateId == null) {
        Integer[] customerDefTemps = documentsData.get(i).getArray("customerDefTemps", Integer.class);
        Integer[] customerTems     = documentsData.get(i).getArray("customer-template", Integer.class);
        
        Integer[] sellerDefTemps   = documentsData.get(i).getArray("sellerDefTemps", Integer.class);
        Integer[] sellerTemps      = documentsData.get(i).getArray("seller-template", Integer.class);
        
        Integer[] generalTemps     = documentsData.get(i).getArray("template", Integer.class);
        
        if(customerDefTemps.length > 0) {
          HashSet<Integer> set = new HashSet<>(Arrays.asList(customerDefTemps));
          set.retainAll(Arrays.asList(ArrayUtils.addAll(customerTems, generalTemps)));
          if(!set.isEmpty())
            currentTemplateId = set.toArray(new Integer[0])[0];
        }
        
        if(currentTemplateId == null && customerTems.length > 0)
          currentTemplateId = customerTems[0];
        
        if(currentTemplateId == null && sellerDefTemps.length > 0) {
          HashSet<Integer> set = new HashSet<>(Arrays.asList(sellerDefTemps));
          set.retainAll(Arrays.asList(ArrayUtils.addAll(sellerTemps, generalTemps)));
          if(!set.isEmpty())
            currentTemplateId = set.toArray(new Integer[0])[0];
        }
        
        if(currentTemplateId == null && sellerTemps.length > 0)
          currentTemplateId = sellerTemps[0];
          
        if(currentTemplateId == null)
          currentTemplateId = generalTemps[0];
        
        if(currentTemplateId == null) {
          if(customerTems.length > 0)
            currentTemplateId = customerTems[0];
          else if(generalTemps.length > 0)
            currentTemplateId = generalTemps[0];
        }
      }
      
      if(!templs.containsKey(currentTemplateId))
        templs.put(currentTemplateId, ObjectLoader.getMap(DocumentXMLTemplate.class, currentTemplateId, "XML").getString("XML"));
      
      String xml = ActionUtil.generateXML(moduls, documentsData.get(i).getInteger("document"), templs.get(currentTemplateId), documentsData.get(i).getInteger("id"), documentsData.get(i).getValue("params", Map.class));

      if(xml != null && !"".equals(xml))
        xmls = (String[]) ArrayUtils.add(xmls, xml);
    }
    return xmls;
  }
  

  public static void print(Integer documentId) {
    print(getDocumentsDataList(documentId));
  }

  public static void print(List<PropertyMap> documentsData) {
    print(documentsData, null);
  }

  public static void print(List<PropertyMap> documentsData, Integer templateId) {
    print(documentsData, templateId, false);
  }

  public static void print(List<PropertyMap> documentsData, Integer templateId, boolean complect) {
    print(documentsData, templateId, complect, null);
  }
  
  public static void print(final List<PropertyMap> documentsData, final Integer templateId, final boolean complect, PrinterJob pj) {
    try {
      if(documentsData != null && !documentsData.isEmpty()) {
        printQuene.add(new PrintTask() {
          @Override
          public void print() {
            processing.setMinMax(0, documentsData.size()-1);
            try {
              String[] xmls = getXMLS(documentsData, templateId, complect, processing);

              if(xmls.length > 0) {
                int startIndex = 0;
                int endIndex   = 0;
                int docToPrint = 5;
                processing.setMinMax(0, xmls.length);
                processing.setValue(0);
                processing.setText("формирую задания печати...");
                while(startIndex < xmls.length) {
                  endIndex = startIndex+docToPrint;
                  if(endIndex >= xmls.length)
                    endIndex = xmls.length;
                  FOP.print_from_XML(mergeXML((String[]) ArrayUtils.subarray(xmls, startIndex, endIndex)),pj==null, pj);
                  startIndex = endIndex;
                  processing.setValue(endIndex);
                }
              }
            }catch(Exception ex) {
              Messanger.showErrorMessage(ex);
            }finally {
              EditorController.defaultCursor();
            }
          }
        });
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  public static void preview(Integer documentId) {
    preview(getDocumentsDataList(documentId));
  }
  
  public static void preview(final List<PropertyMap> documentsData) {
    preview(documentsData, null);
  }
  
  public static void preview(final List<PropertyMap> documentsData, final Integer templateId) {
    try {
      if(documentsData != null && !documentsData.isEmpty()) {
        try {
          String[] xmls = getXMLS(documentsData, templateId, false, null);

          if(xmls.length > 0) {
            EditorController.waitCursor();
            final String xml = mergeXML(xmls);
            final PreviewDialog dialog = new PreviewDialog(xml, (JFrame)EditorController.getFrame(), false);
            
            dialog.setEmailAction(e -> {
              String name = "", fromName = "", fromEmail = "", toName = "", toEmail = "";
              for(PropertyMap d:documentsData) {
                name += " "+d.getString("document_name")+" №"+d.getString("number");
                fromName += ", "+d.getString("seller-name");
                fromEmail += ", "+d.getString("seller-email");
                toName += ", "+d.getString("customer-name");
                toEmail += ", "+d.getString("customer-email");
              }
              fromName = !fromName.equals("") ? fromName.substring(2) : fromName;
              fromEmail = !fromEmail.equals("") ? fromEmail.substring(2) : fromEmail;
              toName = !toName.equals("") ? toName.substring(2) : toName;
              toEmail = !toEmail.equals("") ? toEmail.substring(2) : toEmail;
              
              name = name.substring(1);
              if(name.length() > 30)
                name = name.substring(0, 27)+"...";
              EmailDialog emailDialog = new EmailDialog(xml, name, toName, toEmail, fromName, fromEmail, "Документы для "+toName, "Документы от "+fromName);
              emailDialog.createDialog(dialog).setVisible(true);
            });
            
            SwingUtilities.invokeLater(() -> {
              dialog.setAlwaysOnTop(true);
              dialog.setVisible(true);
            });
          }
        }catch(Exception ex) {
          ex.printStackTrace();
          Messanger.showErrorMessage(ex);
        }finally {
          EditorController.defaultCursor();
        }
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  public static void sendEmail(Integer[] ids) throws Exception {
    sendEmail(ids, null);
  }
  
  public static void sendEmail(Integer[] ids, EmailListener listener) throws Exception {
    if(ids != null && ids.length > 0) {
      EmailDialog emailDialog = new EmailDialog(ids);
      if(listener != null)
        emailDialog.addEmailListener(listener);
      emailDialog.setAutoLoadAndStore(true);
      emailDialog.initData();
      emailDialog.createDialog().setVisible(true);
    }
  }
}