package atolincore5;

import java.io.StringWriter;
import java.math.BigDecimal;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement (name = "Item")
public class Item {
  
  private transient BigDecimal price    = BigDecimal.ZERO;
  private transient BigDecimal quantity = BigDecimal.ZERO;
  
  private transient String  name    = "name";
  private transient Integer taxType = Atolin.TaxTypeDepartment;
  
  //Aditional not nessesary Parameters
  
  /** if set to a valid number indicates the sum of this position that is different from Price*Quantity */
  private transient BigDecimal positionSum = null;  
  /** if set to a non empty string, indicates an ID of the department the item is from*/
  private transient String department = "";
  /** if set to a valid number indicates the Tax Sum that is different from the calculated sum*/
  private transient BigDecimal taxSum = null;  
  /**if set to true will only register Tax Type in FMD*/
  private transient boolean useTaxTypeOnly = false; 
  /**sets TaxMode, unless set to taxModeDefault*/
  private transient int taxMode = Atolin.TaxModeDefault;
  /**sets Info to be printed about the discount that the client recieved.*/
  private transient String discountInfo = "";
  /**set to true if commodity is sold by piece. Causes Quantity to be printed as int*/
  private transient boolean commodityIsPiece=false;
  /**set to true if you want to check money in the cashdrawer */
  private transient boolean checkCash=false;
  
  private transient int agentType = Atolin.AgentTypeNone;
  private transient String supplierTaxID = "";
  private transient String units = "";
  private transient int paymentItem = 0;
  private transient int paymentType = Atolin.PaymentTypeFullPayment;
  
  private transient String operatorAdress = "";
  private transient String operatorTaxID = "";
  private transient String operatorName = "";
  private transient String agentOperation = "";
  private transient String agentPhoneNumber = "";
  private transient String recievingOperatorPhoneNumber = "";
  private transient String transactionOperatorPhoneNumber = "";
  
  private transient String supplierPhoneNumber = "";
  private transient String supplierName = "";
          
          
  public Item()
  {
      
  }
  public Item(String name, BigDecimal price, BigDecimal quantity, int taxType) { //for taxType use iDevice's static members
    this.name     = name;
    this.price    = price;
    this.quantity = quantity;
    this.taxType  = taxType;
  }
  
  /*public Item(String XML) {
    try {
      JAXBContext context = JAXBContext.newInstance(Item.class);
      Unmarshaller um = context.createUnmarshaller();
      StringReader reader = new StringReader(XML);
      Item item = (Item) um.unmarshal(reader);
      setName(item.getName());
      setPrice(item.getPrice());
      setQuantity(item.getQuantity());
      setTaxType(item.getTaxType());
    }catch(Exception e) {
      e.printStackTrace();;
    }
  }*/
  
  /** returns an XML string that is used in communications with the server*/
  public String toXML() {
    try {
      JAXBContext context = JAXBContext.newInstance(Item.class);
      Marshaller m = context.createMarshaller();
      StringWriter writer = new StringWriter();
      m.marshal(this, writer);
      return writer.toString();
    } catch(Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getTaxType() {
    return taxType;
  }

  public void setTaxType(Integer taxType) {
    this.taxType = taxType;
  }
  
  public BigDecimal getPositionSum()
  {
      return positionSum;
  }
  
  public void setPositionSum(BigDecimal positionSum)
  {
      this.positionSum = positionSum;
  }
  
  public String getDepartment()
  {
      return department;
  }
  
  public void setDepartment(String department)
  {
      this.department = department;
  }
  
  public BigDecimal getTaxSum()
  {
      return taxSum;
  }
  
  public void setTaxSum(BigDecimal taxSum)
  {
      assert(taxSum.doubleValue() >= 0.0);
      this.taxSum = taxSum;
  }
          
  public boolean getUseTaxTypeOnly() 
  {
      return useTaxTypeOnly;
  }
  
  public void setUseTaxTypeOnly(boolean useTaxTypeOnly)
  {
      this.useTaxTypeOnly = useTaxTypeOnly;
  }
  
  public int getTaxMode()
  {
      return taxMode;
  }
  
  public void setTaxMode(int taxMode)
  {
    assert(taxMode >= 0);
    assert(taxMode <= 1);
    this.taxMode = taxMode;
  }
  
  public String getDiscountInfo()
  {
      return discountInfo;
  }
  
  public void setDiscountInfo(String discountInfo)
  {
      this.discountInfo = discountInfo;
  }
  
  public boolean getCommodityIsPiece()
  {
      return commodityIsPiece;
  }
  
  public void setCommodityIsPiece(boolean commodityIsPiece)
  {
      this.commodityIsPiece = commodityIsPiece;
  }
  
  public boolean getCheckCash()
  {
      return checkCash;
  }
  
  public void setCheckCash(boolean checkCash)
  {
      this.checkCash = checkCash;
  }
  
  public int getAgentType()
  {
      return agentType;
  }
  
  public void setAgentType(int agentType)
  {
      assert(agentType >= 0);
      assert(agentType <= 64);
      
      this.agentType = agentType;
  }
  
  public String getSupplierTaxID()
  {
      return supplierTaxID;
  }
  
  public void setSupplierTaxID(String supplierTaxID)
  {
      this.supplierTaxID = supplierTaxID;
  }
  public String getUnits()
  {
      return units;
  }
  
  public void setUnits(String units)
  {
      this.units = units;
  }
  
  public int getPaymentItem()
  {
      return paymentItem;
  }
  
  public void setPaymentItem(int paymentItem)
  {
      this.paymentItem = paymentItem;
  }
  
  public int getPaymentType()
  {
      return paymentType;
  }
  
  public void setPaymentType(int paymentType)
  {
      this.paymentType = paymentType;
  }
  
  public String getOperatorAdress()
  {
      return operatorAdress;
  }
  
  public String getOperatorTaxID()
  {
      return operatorTaxID;
  }
  
  public String getOperatorName()
  {
      return operatorName;
  }
  
  public String getAgentOperation()
  {
      return agentOperation;
  }
  
  public void setAgentOperation(String agentOperation)
  {
     this.agentOperation = agentOperation;
  }
  
  public String getAgentPhoneNumber()
  {
      return agentPhoneNumber;
  }
  
  public void setAgentOperatorPhoneNumber(String agentPhoneNumber)
  {
     this.agentPhoneNumber = agentPhoneNumber;
  }
  
  public String getRecievingOperatorPhoneNumber()
  {
      return recievingOperatorPhoneNumber;
  }
  
  public void setRecievingOperatorPhoneNumber(String recievingOperatorPhoneNumber)
  {
     this.recievingOperatorPhoneNumber = recievingOperatorPhoneNumber;
  }
  
  public String getTransactionOperatorPhoneNumber()
  {
      return transactionOperatorPhoneNumber;
  }
  
  public void setTransactionOperatorPhoneNumber(String transactionOperatorPhoneNumber)
  {
     this.transactionOperatorPhoneNumber = transactionOperatorPhoneNumber;
  }
  
  public String getSupplierPhoneNumber()
  {
      return supplierPhoneNumber;
  }
  
  public void setSupplierPhoneNumber(String supplierPhoneNumber)
  {
     this.supplierPhoneNumber = supplierPhoneNumber;
  }
  
  public String getSupplierName()
  {
      return supplierName;
  }
  
  public void setSupplierName(String supplierName)
  {
      this.supplierName = supplierName;
  }
  
  public static class Builder
  {
    private BigDecimal price    = BigDecimal.ZERO;
    private BigDecimal quantity = BigDecimal.ZERO;
    private String  name = "name";
    private Integer taxType = Atolin.TaxTypeDepartment;
    
    private BigDecimal positionSum = null;  
    private String department = "";
    private BigDecimal taxSum = null; 
    private boolean useTaxTypeOnly = false; 
    private int taxMode = Atolin.TaxModeDefault;
    private String discountInfo = "";
    private boolean commodityIsPiece=false;
    private boolean checkCash=false;
  
    private int agentType = Atolin.AgentTypeNone;
    private String supplierTaxID = "";
    private String units = "";
    private int paymentItem = Atolin.ItemDefault;
    private int paymentType = Atolin.PaymentTypeFullPayment;
  
    private String operatorAdress = "";
    private String operatorTaxID = "";
    private String operatorName = "";
    private String agentOperation = "";
    private String agentPhoneNumber = "";
    private String recievingOperatorPhoneNumber = "";
    private String transactionOperatorPhoneNumber = "";
  
    private String supplierPhoneNumber = "";
    private String supplierName = "";
    
    public Builder(String name, BigDecimal price, BigDecimal quantity, int taxType)
    {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.taxType = taxType;
    }
  
    public Builder positionSum(BigDecimal positionSum)
    {
        this.positionSum = positionSum;
        return this;
    }
    
    public Builder department(String department)
    {
        this.department = department;
        return this;
    }
    
    public Builder taxSum(BigDecimal taxSum)
    {
        this.taxSum = taxSum;
        return this;
    }
    
    public Builder useTaxTypeOnly(boolean useTaxTypeOnly)
    {
        this.useTaxTypeOnly = useTaxTypeOnly;
        return this;
    }
    
    public Builder taxMode(int taxMode)
    {
        this.taxMode = taxMode;
        return this;
    }
    
    public Builder taxType(int taxType)
    {
        this.taxType = taxType;
        return this;
    }
  
    public Builder discountInfo(String discountInfo)
    {
        this.discountInfo = discountInfo;
        return this;
    }
    
    public Builder commodityIsPiece(boolean commodityIsPiece)
    {
        this.commodityIsPiece = commodityIsPiece;
        return this;
    }
    
    public Builder checkCash(boolean checkCash)
    {
        this.checkCash = checkCash;
        return this;
    }
    
    public Builder units(String units)
    {
        this.units = units;
        return this;
    }
    
    public Builder paymentItem(int paymentItem)
    {
        this.paymentItem = paymentItem;
        return this;
    }
    
    public Builder paymentType(int paymentType)
    {
        this.paymentType = paymentType;
        return this;
    }
    
    public Builder agent(int agentType, String operatorName, String operatorTaxID, String agentOperation, 
            String agentPhoneNumber, String recievingOperatorPhoneNumber, String transactionOperatorPhoneNumber)
    {
        this.agentType = agentType;
        this.operatorName = operatorName;
        this.operatorTaxID = operatorTaxID;
        this.agentOperation = agentOperation;
        this.agentPhoneNumber = agentPhoneNumber;
        this.recievingOperatorPhoneNumber = recievingOperatorPhoneNumber;
        this.transactionOperatorPhoneNumber = transactionOperatorPhoneNumber;
        return this;
    }
    
    public Builder supplier(String supplierName, String supplierTaxID, String supplierPhoneNumber)
    {
        this.supplierName = supplierName;
        this.supplierTaxID = supplierTaxID;
        this.supplierPhoneNumber = supplierPhoneNumber;
        return this;
    }
    
    public Item build()
    {
        return new Item(this);
    }
  }
  
  private Item(Builder builder)
  {
      price = builder.price;
      quantity = builder.quantity;
      name = builder.name;
      taxType = builder.taxType;
      positionSum = builder.positionSum;
      department = builder.department;
      taxSum = builder.taxSum;
      useTaxTypeOnly = builder.useTaxTypeOnly;
      taxMode = builder.taxMode;
      discountInfo = builder.discountInfo;
      commodityIsPiece = builder.commodityIsPiece;
      checkCash = builder.checkCash;
      agentType = builder.agentType;
      supplierTaxID = builder.supplierTaxID;
      units = builder.units;
      paymentItem = builder.paymentItem;
      paymentType = builder.paymentType;
      operatorAdress = builder.operatorAdress;
      operatorTaxID = builder.operatorTaxID;
      operatorName = builder.operatorName;
      agentPhoneNumber = builder.agentPhoneNumber;
      recievingOperatorPhoneNumber = builder.recievingOperatorPhoneNumber;
      transactionOperatorPhoneNumber = builder.transactionOperatorPhoneNumber;
      supplierPhoneNumber = builder.supplierPhoneNumber;
      supplierName = builder.supplierName;
  }  
}