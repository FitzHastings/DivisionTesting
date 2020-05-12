package atolincore5;

import java.io.File;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.List;
import javafx.collections.FXCollections;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
*    this class represents a Receipt, of a given type that can be commited to the 
*    server to have it be printed out and registered in fiscal hardware. 
*/
@XmlRootElement (name = "Receipt")
public class Receipt { 
    private int type;
    
    @XmlElementWrapper(name = "items")
    @XmlElement(name = "item")
    public List<Item> items = FXCollections.observableArrayList();
    
    private int correctionReason;
    private String correctionComment;
    private String correctionDocumentNumber;
    
    @XmlTransient
    public Calendar correctionDate = Calendar.getInstance();
    
    public Receipt() { }
    
    public Receipt(int receiptType) {
       this.type = receiptType;
    }
    
    /**  returns an XML formatted string that is used in communications between the
        client and the server.
    */
    public String toXML()
    {
        try {
            JAXBContext context = JAXBContext.newInstance(Receipt.class);
            Marshaller m = context.createMarshaller();
            StringWriter writer = new StringWriter();
            m.marshal(this, writer);
            return writer.toString();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    /**saves receipt to a file in an xml format*/    
    public void saveReceipt(File file)
    {
        if(file == null)
            return;
        if (!file.getPath().endsWith(".xml")) 
            file = new File(file.getPath() + ".xml");
        try
        {
            JAXBContext context = JAXBContext.newInstance(Receipt.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(this, file);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    /**  loads the receipt from an XML formatted file, that can be created with 
        saveReceipt or toXml methods*/
    public void loadReceipt(File file)
    {
        if(file == null)
            return;
        try
        {
            JAXBContext context = JAXBContext.newInstance(ServerSettings.class);
            Unmarshaller um = context.createUnmarshaller();
            
            Receipt buffer = (Receipt) um.unmarshal(file);
            this.correctionComment = buffer.correctionComment;
            this.correctionDate = buffer.correctionDate;
            this.correctionDocumentNumber = buffer.correctionDocumentNumber;
            this.correctionReason = buffer.correctionReason;
            this.items = buffer.items;
            this.type = buffer.type;
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    @XmlElement(name = "correctionReason")
    public int getCorrectionReason()
    {
        return correctionReason;
    }
    
    public void setCorrectionReason(int correctionReason)
    {
        this.correctionReason = correctionReason;
    }
    
    @XmlElement(name = "correctionComment")
    public String getCorrectionComment()
    {
        return correctionComment;
    }
    
    public void setCorrectionComment(String correctionComment)
    {
        this.correctionComment = correctionComment;
    }
    
    @XmlElement(name = "correctionDocumentNumber")
    public String getCorrectionDocumentNumber()
    {
        return correctionDocumentNumber;
    }
    
    public void setCorrectionDocumentNumber(String correctionDocumentNumber)
    {
      this.correctionDocumentNumber = correctionDocumentNumber;
    }
    
    @XmlElement(name = "correctionYear")
    public int getCorrectionYear()
    {
        return correctionDate.get(Calendar.YEAR);
    }
    
    public void setCorrectionYear(int correctionYear)
    {
        this.correctionDate.set(Calendar.YEAR, correctionYear);
    }
    
    @XmlElement(name = "correctionMonth")
    public int getCorrectionMonth()
    {
        return correctionDate.get(Calendar.MONTH);
    }
    
    public void setCorrectionMonth(int correctionMonth)
    {
        correctionDate.set(Calendar.MONTH, correctionMonth);
    }
    
    @XmlElement(name = "correctionDay")
    public int getCorrectionDay()
    {
        return correctionDate.get(Calendar.DAY_OF_MONTH);
    }
    
    public void setCorrectionDay(int correctionDay)
    {
        correctionDate.set(Calendar.DAY_OF_MONTH, correctionDay);
    }
    
    @XmlElement(name = "type")
    public int getType()
    {
        return type;
    }
    
    public void setType(int type)
    {
        this.type = type;
    }
    
    public void addItem(Item item)
    {
        items.add(item);
    }
    
    //@XmlElement(name = "items")
    public List<Item> getItems()
    {
        return items;
    }
}