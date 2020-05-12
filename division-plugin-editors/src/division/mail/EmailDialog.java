package division.mail;

import bum.editors.EditorGui;
import bum.editors.EditorListener;
import bum.editors.TableEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Document;
import bum.interfaces.DocumentXMLTemplate;
import division.swing.DivisionScrollPane;
import division.swing.DivisionTable;
import division.swing.DivisionTextArea;
import division.swing.DivisionTextField;
import division.swing.LocalProcessing;
import division.swing.dnd.DNDPanel;
import division.swing.guimessanger.Messanger;
import division.swing.table.CellColorController;
import division.util.Email;
import division.util.GzipUtil;
import documents.FOP;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import org.apache.commons.lang.ArrayUtils;
import org.apache.fop.apps.MimeConstants;

public class EmailDialog extends EditorGui {
  private final JTabbedPane tab           = new JTabbedPane();
  private final JPanel      messagePanel  = new JPanel(new GridBagLayout());
  private final JSplitPane  templatePanel = new JSplitPane();
  private final JSplitPane  mailListPanel = new JSplitPane();
  
  private final DivisionTextField  toNameText    = new DivisionTextField("Кому...");
  private final DivisionTextField  toEmailText   = new DivisionTextField("Email...");
  private final DivisionTextField  fromNameText  = new DivisionTextField("От кого...");
  private final DivisionTextField  fromEmailText = new DivisionTextField("Email...");
  private final DivisionTextField  subjectText   = new DivisionTextField("Тема...");
  private final DivisionTextArea   bodyText      = new DivisionTextArea("Сообщение...");
  private final JScrollPane        bodyScroll    = new JScrollPane(bodyText);
  private final TreeMap<Integer,Integer> templates = new TreeMap<>();
  
  private final Pattern emailRregexp = Pattern.compile("^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$");

  private TableEditor documentTable;
  private TableEditor templateTable;
  
  private final DivisionTable      companyTable       = new DivisionTable();
  private final DivisionScrollPane companyScroll      = new DivisionScrollPane(companyTable);
  
  private final JPanel             contactPanel             = new JPanel(new GridBagLayout());
  private final DivisionTextField  companyContactFio        = new DivisionTextField("Контактное лицо...");
  private final DivisionTextField  companyEmail             = new DivisionTextField("Email-адреса (через \",\")...");
  private final DivisionTextField  companyTelefon           = new DivisionTextField("Телефоны (через \",\")...");
  private final DivisionTextArea   companyContactInfo       = new DivisionTextArea("Иная контактная информация...");
  private final DivisionScrollPane companyContactInfoScroll = new DivisionScrollPane(companyContactInfo);
  
  private Integer[] ids;
  
  private String xml;
  private String fileName;
  
  private final TreeMap<Integer,Map<Integer,List>> documents = new TreeMap<>();
  
  private List<EmailListener> listeners = new ArrayList<>();
  

  public EmailDialog(Integer[] ids) {
    super("Рассылка документов...",null);
    this.ids = ids;
    initComponents();
    initEvents();
  }
  
  public EmailDialog(String xml, String fileName, String toName, String toEmail, String fromName, String fromEmail, String subject, String message) {
    super("Отправка документа",null);
    this.xml = xml;
    initComponents();
    toNameText.setText(toName==null?"":toName);
    toEmailText.setText(toEmail==null?"":toEmail);
    fromNameText.setText(fromName==null?"":fromName);
    fromEmailText.setText(fromEmail==null?"":fromEmail);
    subjectText.setText(subject==null?"":subject);
    bodyText.setText(message==null?"":message);
    this.fileName = fileName!=null?fileName:"Документ"+(fromName==null?"":(" от "+fromName));
  }
  
  public void addEmailListener(EmailListener listener) {
    listeners.add(listener);
  }
  
  public void fireDocumentSent(Integer[] ids) {
    listeners.stream().forEach(l -> l.documentsSent(ids));
  }

  private void initComponents() {
    if(xml == null) {
      documentTable = new TableEditor(
              new String[]{"id","Документ"}, 
              new String[]{"id","name"}, 
              Document.class, 
              null, 
              "", 
              Document.Type.CURRENT);
      
      documentTable.setAdministration(false);
      documentTable.setVisibleOkButton(false);
      documentTable.getStatusBar().setVisible(false);
      documentTable.setSingleSelection(true);
      
      templateTable = new TableEditor(
              new String[]{"id","Шаблон",""}, 
              new String[]{"id","name","query:false"}, 
              DocumentXMLTemplate.class, 
              null, 
              "", 
              Document.Type.CURRENT) {
                @Override
                public void changeSelection(EditorGui editor, Integer[] ids) {
                  getClientFilter().clear().AND_IN("document", ids);
                  initData();
                }
              };
      
      templateTable.setAdministration(false);
      templateTable.setVisibleOkButton(false);
      templateTable.getStatusBar().setVisible(false);
      templateTable.getTable().setColumnEditable(2, true);
      templateTable.setSingleSelection(true);
      
      templatePanel.add(documentTable.getGUI(), JSplitPane.LEFT);
      templatePanel.add(templateTable.getGUI(), JSplitPane.RIGHT);
      
      addComponentToStore(templatePanel);
    }
    
    addComponentToStore(mailListPanel);
    
    getOkButton().setText("Отправить");
    
    toEmailText.setToolTipText("Еmail получателя");
    toNameText.setToolTipText("Наименование получателя");

    fromEmailText.setToolTipText("Еmail отправителя");
    fromNameText.setToolTipText("Наименование отправителя");
    
    companyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    companyTable.getTableHeader().setReorderingAllowed(false);
    companyTable.setSortable(false);
    
    mailListPanel.add(companyScroll, JSplitPane.LEFT);
    mailListPanel.add(contactPanel,  JSplitPane.RIGHT);
    
    contactPanel.add(companyContactFio,        new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    contactPanel.add(companyEmail,             new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    contactPanel.add(companyTelefon,           new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    contactPanel.add(companyContactInfoScroll, new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.BOTH,       new Insets(5, 5, 5, 5), 0, 0));
    
    companyEmail.setFont(new Font("Dialog", Font.BOLD, 11));

    tab.add("Письмо", messagePanel);
    if(xml == null) {
      tab.add("Шаблоны документов", templatePanel);
      tab.add("Список Рассылки", mailListPanel);
    }

    getRootPanel().add(tab, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));

    DNDPanel toPanel      = new DNDPanel("Кому",      false, new GridBagLayout());
    DNDPanel fromPanel    = new DNDPanel("От кого",   false, new GridBagLayout());
    DNDPanel subjectPanel = new DNDPanel("Тема",      false, new GridBagLayout());
    DNDPanel bodyPanel    = new DNDPanel("Сообщение", false, new GridBagLayout());

    toPanel.add(toNameText,     new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    toPanel.add(toEmailText,    new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

    fromPanel.add(fromNameText,  new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    fromPanel.add(fromEmailText, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

    subjectPanel.add(subjectText,  new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

    bodyPanel.add(bodyScroll,  new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));

    messagePanel.add(fromPanel,    new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(2,2,2,2), 0, 0));
    messagePanel.add(toPanel,      new GridBagConstraints(1, 0, 1, 1, 0.5, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(2,2,2,2), 0, 0));
    messagePanel.add(subjectPanel, new GridBagConstraints(0, 1, 2, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(2,2,2,2), 0, 0));
    messagePanel.add(bodyPanel,    new GridBagConstraints(0, 2, 2, 1, 1.0, 1.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.BOTH,       new Insets(2,2,2,2), 0, 0));

    bodyScroll.setPreferredSize(new Dimension(600, 200));
  }

  private void initEvents() {
    documentTable.addEditorListener(templateTable);
    
    templateTable.addEditorListener(new EditorListener() {
      @Override
      public void initDataComplited(EditorGui editor) {
        if(templates.containsKey(documentTable.getSelectedId()[0]))
          ((Vector<Vector>)templateTable.getTable().getTableModel().getDataVector()).stream().forEach(d -> d.set(2, d.get(0).equals(templates.get(documentTable.getSelectedId()[0]))));
      }
    });
    
    templateTable.getTable().getTableModel().addTableModelListener((TableModelEvent e) -> {
      if(e.getColumn() != -1 && e.getFirstRow() != -1 && e.getColumn() == 2 && 
              (boolean)templateTable.getTable().getTableModel().getValueAt(e.getFirstRow(), e.getColumn())) {
        templates.put(documentTable.getSelectedId()[0], templateTable.getSelectedId()[0]);
        Vector<Vector> data = templateTable.getTable().getTableModel().getDataVector();
        for(int i=0;i<data.size();i++)
          if(i != e.getFirstRow())
            data.get(i).set(2, false);
        templateTable.getTable().repaint();
      }
    });
    
    companyTable.addTableSelectionListener((int[] oldSelection, int[] newSelection) -> {
      if(oldSelection.length > 0)
        saveContacts(oldSelection[0]);
      
      if(newSelection.length > 0) {
        Integer partitionId = (Integer)companyTable.getValueAt(newSelection[0], 0);
        List list = (List)((List)documents.get(partitionId).values().toArray()[0]).get(0);
        companyContactFio.setText(list.get(15) != null ? (String)list.get(15) : "");
        companyEmail.setText(list.get(9) != null ? (String)list.get(9) : "");
        companyTelefon.setText(list.get(16) != null ? (String)list.get(16) : "");
        companyContactInfo.setText(list.get(17) != null ? (String)list.get(17) : "");
      }
    });
    
    companyTable.setCellColorController(new CellColorController() {
      @Override
      public Color getCellTextColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        String email = (String) table.getModel().getValueAt(modelRow, 2);
        if(email == null || email.equals(""))
          return Color.RED;
        else {
          for(String address:email.trim().split("\\s*,\\s*|\\s*;\\s*|\\s+|\\t+")) {
            if(!validEmail(address.trim()))
              return Color.RED;
          }
        }
        return null;
      }
    });
    
    tab.addChangeListener((ChangeEvent e) -> {
      if(companyTable.getSelectedRowCount() > 0)
        saveContacts(companyTable.getSelectedRow());
    });
  }
  
  private void clearContacts() {
    companyTable.clearSelection();
    companyContactFio.setText("");
    companyEmail.setText("");
    companyTelefon.setText("");
    companyContactInfo.setText("");
  }
  
  private void saveContacts(Integer rowIndex) {
    Integer oldPartitionId = (Integer)companyTable.getValueAt(rowIndex, 0);
    List list = (List)((List)documents.get(oldPartitionId).values().toArray()[0]).get(0);

    if(!companyContactFio.getText().equals(list.get(15)) || !companyEmail.getText().equals(list.get(9)) ||
            !companyTelefon.getText().equals(list.get(16)) || !companyContactInfo.getText().equals(list.get(17))) {
      try {
        if(ObjectLoader.executeUpdate("UPDATE [CompanyPartition] SET [CompanyPartition(contactFio)]=?, [CompanyPartition(email)]=?, "
                + "[CompanyPartition(telefon)]=?, [CompanyPartition(contactInfo)]=? WHERE id=?", true,
                new Object[]{companyContactFio.getText(), companyEmail.getText(), companyTelefon.getText(), companyContactInfo.getText(), oldPartitionId}) == 1) {
          companyTable.setValueAt(companyEmail.getText(), rowIndex, 2);
          list.set(15, companyContactFio.getText());
          list.set(9, companyEmail.getText());
          list.set(16, companyTelefon.getText());
          list.set(17, companyContactInfo.getText());
          companyTable.repaint();
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }
  
  private boolean validEmail(String email) {
    Matcher matcher = emailRregexp.matcher(email);
    return matcher.matches();
  }

  private void send() {
    LocalProcessing processing = new LocalProcessing(getDialog());
    if(xml == null) {
      clearContacts();
      processing.setSubProgressVisible(true);
      processing.setSubTextVisible(true);
      processing.setMinMax(0, documents.keySet().size());
      processing.setValue(0);
      processing.submit(() -> {
        try {
          Integer[] documentSent = new Integer[0];
          Map<Integer, String> xmls = new TreeMap<>();
          Integer[] customers = documents.keySet().toArray(new Integer[0]);
          for(int i=0;i<customers.length;i++) {
            Integer customerId = customers[i];
            for(Integer sellerId:documents.get(customerId).keySet().toArray(new Integer[0])) {
              
              String fromEmail = fromEmailText.isEnabled() ? fromEmailText.getText() : null;
              String fromName  = fromNameText.isEnabled()  ? fromNameText.getText()  : null;

              String toEmail   = toEmailText.isEnabled()   ? toEmailText.getText()   : null;
              String toName    = toNameText.isEnabled()    ? toNameText.getText()    : null;

              String subject = subjectText.getText().equals("") ? null : subjectText.getText();
              String body    = bodyText.getText().equals("")    ? null : bodyText.getText();

              toEmail = toEmail == null ? (String)((List)documents.get(customerId).get(sellerId).get(0)).get(9) : toEmail;
              toName  = toName  == null ? (String)((List)documents.get(customerId).get(sellerId).get(0)).get(8) : toName;

              fromEmail = fromEmail == null ? (String)((List)documents.get(customerId).get(sellerId).get(0)).get(6) : fromEmail;
              fromName  = fromName  == null ? (String)((List)documents.get(customerId).get(sellerId).get(0)).get(5) : fromName;

              processing.setText("Кому: "+toName+"<"+toEmail+"> от: "+fromName+"<"+fromEmail+">");

              subject = subject == null ? "Документы для  "+toName : subject;
              body    = body    == null ? "Документы от "+fromName : body;

              Email email = new Email();
              if(toEmail != null)
                for(String address:toEmail.split(",\\s*|;\\s*|\\s|\\t"))
                  if(!address.trim().equals("") && validEmail(address.trim()))
                    email.addToEmail(address.trim(), toName, "utf8");
              if(email.getToEmail() != null && email.getToEmail().length > 0) {
                email.setFromEmail(fromEmail, fromName, "utf8");
                email.setSubject(subject);
                email.setMessage(body);
                email.setCharset("utf8");

                processing.setSuibMinMax(0, ((List<List>)documents.get(customerId).get(sellerId)).size());
                processing.setSubValue(0);
                Integer[] docToSend = new Integer[0];
                for(List data:(List<List>)documents.get(customerId).get(sellerId)) {
                  processing.setSubText(data.get(11)+(data.get(12)==null?"":" № "+data.get(12)));
                  Integer template = templates.get((Integer)data.get(14));
                  if(template == null)
                    template = data.get(1) != null && ((Integer[])data.get(1)).length > 0 ? ((Integer[])data.get(1))[0] : 
                            data.get(2) != null && ((Integer[])data.get(2)).length > 0 ? ((Integer[])data.get(2))[0] : 
                            data.get(3) != null && ((Integer[])data.get(3)).length > 0 ? ((Integer[])data.get(3))[0] : null;

                  if(template != null) {

                    String XML = xmls.get(template);
                    if(XML == null) {
                      XML = (String) ObjectLoader.executeQuery("SELECT [DocumentXMLTemplate(XML)] FROM [DocumentXMLTemplate] WHERE id="+template).get(0).get(0);
                      xmls.put(template, XML);
                    }

                    Map map = null;
                    try {
                      map = GzipUtil.deserializable((byte[])data.get(10), Map.class);
                    }catch(Exception ex) {
                      map = GzipUtil.getObjectFromGzip((byte[])data.get(10), Map.class);
                    }
                    map.put("id",data.get(0));

                    docToSend = (Integer[]) ArrayUtils.add(docToSend, data.get(0));

                    byte[] bytes = FOP.export_from_XML_to_bytea(FOP.get_XML_From_XMLTemplate(XML, map), MimeConstants.MIME_PDF);
                    String name = data.get(11)+(data.get(12) != null ? " №"+data.get(12) : "");
                    email.addFile(bytes, "application/pdf", javax.mail.internet.MimeUtility.encodeWord(name)+".pdf", name);
                  }
                  processing.setSubValue(processing.getSubValue()+1);
                }
                try {
                  email.send(System.getProperties().getProperty("smtp.host"), (Integer) System.getProperties().get("smtp.port"), System.getProperties().getProperty("smtp.user"), System.getProperties().getProperty("smtp.password"));
                  documentSent = (Integer[]) ArrayUtils.addAll(documentSent, docToSend);
                  SwingUtilities.invokeLater(() -> {
                    for(int rowIndex=0;rowIndex<companyTable.getTableModel().getRowCount();rowIndex++)
                      if(companyTable.getTableModel().getValueAt(rowIndex, 0).equals(customerId))
                        companyTable.getTableModel().removeRow(rowIndex);
                  });
                  documents.get(customerId).remove(sellerId);
                }catch(Exception ex) {
                  Messanger.showErrorMessage(ex);
                }
              }
            }
            if(documents.get(customerId).isEmpty())
              documents.remove(customerId);
            processing.setValue(i+1);
          }

          if(documentSent.length > 0)
            fireDocumentSent(documentSent);
        }catch(Exception ex) {
          processing.setValue(processing.getMaximum());
          Messanger.showErrorMessage(ex);
        }finally {
          SwingUtilities.invokeLater(() -> {
            if(companyTable.getRowCount() == 0)
              dispose();
          });
        }
      });
    }else {
      if(validEmail(toEmailText.getText())) {
        processing.setMinMax(0, 10);
        processing.setText("Доставка почты...");
        processing.submit(() -> {
          Email email = new Email();
          for(String address:toEmailText.getText().trim().split("\\s*,\\s*|\\s*;\\s*|\\s+|\\t+"))
            if(!address.trim().equals("") && validEmail(address.trim()))
              email.addToEmail(address.trim(), toNameText.getText(), "utf8");
          
          processing.setValue(3);
          
          if(email.getToEmail().length > 0) {
            email.setFromEmail(fromEmailText.getText(), fromNameText.getText(), "utf8");
            email.setSubject(subjectText.getText());
            email.setMessage(bodyText.getText());
            email.setCharset("utf8");
            
            processing.setValue(6);
            
            try {
              byte[] byteData = FOP.export_from_XML_to_bytea(xml, MimeConstants.MIME_PDF);
              email.addFile(byteData, "application/pdf", 
                      javax.mail.internet.MimeUtility.encodeWord(fileName)+".pdf", fileName);
              processing.setValue(8);
              email.send("192.168.1.1", 25, "", "");
              dispose();
            }catch(Exception ex) {
              Messanger.showErrorMessage(ex);
            }finally {
              processing.setValue(10);
            }
          }
        });
      }else Messanger.alert("Неправильный адрес получателя", JOptionPane.ERROR_MESSAGE);
    }
  }

  @Override
  public Boolean okButtonAction() {
    if(companyTable.getSelectedRowCount() > 0)
      saveContacts(companyTable.getSelectedRow());
    send();
    return true;
  }

  @Override
  public void initTargets() {
  }

  @Override
  public void initData() throws Exception {
    if(xml == null) {
      List<List> dbdata = ObjectLoader.executeQuery("SELECT "
        /*0*/   +"id,"

        /*1*/   +"[CreatedDocument(customer-template)],"
        /*2*/   +"[CreatedDocument(seller-template)],"
        /*3*/   +"[CreatedDocument(template)],"

        /*4*/   +"[CreatedDocument(sellerCompanyPartition)],"
        /*5*/   +"getCompanyName((SELECT [CompanyPartition(company)] FROM [CompanyPartition] WHERE id=[CreatedDocument(sellerCompanyPartition)])),"
        /*6*/   +"(SELECT [CompanyPartition(email)] FROM [CompanyPartition] WHERE id=[CreatedDocument(sellerCompanyPartition)]),"

        /*7*/   +"[CreatedDocument(customerCompanyPartition)],"
        /*8*/   +"getCompanyName((SELECT [CompanyPartition(company)] FROM [CompanyPartition] WHERE id=[CreatedDocument(customerCompanyPartition)])),"
        /*9*/   +"(SELECT [CompanyPartition(email)] FROM [CompanyPartition] WHERE id=[CreatedDocument(customerCompanyPartition)]), "

        /*10*/ +"[CreatedDocument(params)], "
        /*11*/ +"[CreatedDocument(document_name)], "
        /*12*/ +"[CreatedDocument(number)], "
        /*13*/ +"[CreatedDocument(date)],"
        /*14*/ +"[CreatedDocument(document)], "
        /*15*/ +"(SELECT [CompanyPartition(contactFio)] FROM [CompanyPartition] WHERE id=[CreatedDocument(customerCompanyPartition)]), "
        /*16*/ +"(SELECT [CompanyPartition(telefon)] FROM [CompanyPartition] WHERE id=[CreatedDocument(customerCompanyPartition)]), "
        /*17*/ +"(SELECT [CompanyPartition(contactInfo)] FROM [CompanyPartition] WHERE id=[CreatedDocument(customerCompanyPartition)]) "
        +"FROM [CreatedDocument] WHERE id=ANY(?)", true, new Object[]{ids});

      for(List d:dbdata) {
        Integer customerId = (Integer) d.get(7);
        Integer sellerId   = (Integer) d.get(4);

        if(!documents.containsKey(customerId))
          documents.put(customerId, new TreeMap<Integer,List>());
        Map<Integer,List> customerData = documents.get(customerId);

        if(!customerData.containsKey(sellerId))
          customerData.put(sellerId, new Vector());
        List sellerData = customerData.get(sellerId);
        sellerData.add(d);
      }

      toEmailText.setEnabled(documents.size() == 1);
      toEmailText.setEditable(documents.size() == 1);

      toNameText.setEnabled(documents.size() == 1);
      toNameText.setEditable(documents.size() == 1);

      if(toNameText.isEnabled()) {
        toNameText.setText((String)  ((List)((List)documents.firstEntry().getValue().values().toArray()[0]).get(0)).get(8));
        toEmailText.setText((String) ((List)((List)documents.firstEntry().getValue().values().toArray()[0]).get(0)).get(9));
      }else {
        toEmailText.setText("Автоматически...");
        toNameText.setText("Автоматически...");
      }

      Integer seller = (Integer) documents.firstEntry().getValue().keySet().toArray()[0];
      for(Map<Integer,List> sellerData:documents.values()) {
        for(Integer sellerId:sellerData.keySet()) {
          if(!seller.equals(sellerId)) {
            seller = null;
            break;
          }
        }
        if(seller == null)
          break;
      }

      fromEmailText.setEnabled(seller != null);
      fromEmailText.setEditable(seller != null);

      fromNameText.setEnabled(seller != null);
      fromNameText.setEditable(seller != null);

      if(fromNameText.isEnabled()) {
        fromNameText.setText((String) ((List)((List)documents.firstEntry().getValue().values().toArray()[0]).get(0)).get(5));
        fromEmailText.setText((String) ((List)((List)documents.firstEntry().getValue().values().toArray()[0]).get(0)).get(6));
        subjectText.setText("Документы для "+(String) ((List)((List)documents.firstEntry().getValue().values().toArray()[0]).get(0)).get(8));
        bodyText.setText("Документы от "+(String) ((List)((List)documents.firstEntry().getValue().values().toArray()[0]).get(0)).get(5));
      }else {
        fromEmailText.setText("Автоматически...");
        fromNameText.setText("Автоматически...");
      }

      Integer[] documentIds = new Integer[0];
      for(Map<Integer,List> sellerData:documents.values())
        for(List<List> data:sellerData.values())
          for(List d:data)
            if(!ArrayUtils.contains(documentIds, d.get(14)))
              documentIds = (Integer[]) ArrayUtils.add(documentIds, d.get(14));

      documentTable.getClientFilter().AND_IN("id", documentIds);
      documentTable.initData();

      companyTable.getTableModel().clear();
      companyTable.setColumns("id","Организация","email");
      companyTable.setColumnWidthZero(0,2);
      documents.keySet().stream().forEach(id -> {
        List list = (List)((List)documents.get(id).values().toArray()[0]).get(0);
        companyTable.getTableModel().addRow(new Object[]{list.get(7), list.get(8), list.get(9)});
      });
    }
  }
}