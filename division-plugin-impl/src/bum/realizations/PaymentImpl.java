package bum.realizations;

import bum.annotations.*;
import bum.interfaces.CompanyPartition;
import bum.interfaces.Contract;
import bum.interfaces.DealPayment;
import bum.interfaces.Payment;
import bum.interfaces.Store;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import mapping.MappingObjectImpl;

@Table(
  clientName="Платежи",
  queryColumns={
    @QueryColumn(
      name="notDistribAmount",
      query="SELECT getNotDistribAmount([Payment(id)])"),
    @QueryColumn(
      name="seller",
      query="SELECT getCompanyPartitionName([Payment(sellerCompanyPartition)])"),
    @QueryColumn(
      name="customer",
      query="SELECT getCompanyPartitionName([Payment(customerCompanyPartition)])"),
    @QueryColumn(
      name="documentDate",
      query="SELECT MAX(date) FROM [CreatedDocument] WHERE [CreatedDocument(payment)]=[Payment(id)]")
  })

@Procedures(procedures={
  @Procedure(
      name="getNotDistribAmount",
      arguments={"integer"},
      returnType="NUMERIC",
      procedureText= ""
        + "DECLARE "
        + "BEGIN "
        + "  RETURN NULLTOZERO((SELECT [Payment(amount)]-NULLTOZERO((SELECT SUM([DealPayment(amount)]) FROM [DealPayment] WHERE tmp=false AND "
        + "[DealPayment(payment)]=$1)) FROM [Payment] WHERE id=$1));"
        + "END"
    )
})

@Triggers(triggers={
  @Trigger(timeType=Trigger.TIMETYPE.BEFORE,
  actionTypes={Trigger.ACTIONTYPE.UPDATE},
  procedureText=""
          + "DECLARE\n"
          + "BEGIN\n"
          + "  IF NEW.type != OLD.type THEN\n"
          + "    UPDATE [DealPayment] SET type=NEW.type WHERE [DealPayment(payment)]=OLD.id;\n"
          + "    UPDATE [CreatedDocument] SET type=NEW.type WHERE [CreatedDocument(payment)]=OLD.id;\n"
          + "    UPDATE [CreatedDocument] SET type=NEW.type WHERE id IN (SELECT [CreatedDocument(dealPositions):object] FROM [CreatedDocument(dealPositions):table] WHERE [CreatedDocument(dealPositions):target] IN "
          + "    (SELECT [DealPosition(id)] FROM [DealPosition] WHERE [DealPosition(deal)] IN (SELECT [DealPayment(deal)] FROM [DealPayment] WHERE [DealPayment(payment)]=NEW.id))) AND"
          + "    [CreatedDocument(type)] != NEW.type;\n"
          + "  END IF;\n"
          + "  IF NEW.tmp != OLD.tmp THEN\n"
          + "    UPDATE [DealPayment]     SET tmp=NEW.tmp WHERE [DealPayment(payment)]=OLD.id;\n"
          + "    UPDATE [CreatedDocument] SET tmp=NEW.tmp WHERE [CreatedDocument(payment)]=OLD.id;\n"
          + "    UPDATE [CreatedDocument] SET tmp=NEW.tmp WHERE id IN (SELECT [CreatedDocument(dealPositions):object] FROM [CreatedDocument(dealPositions):table] WHERE [CreatedDocument(dealPositions):target] IN "
          + "    (SELECT [DealPosition(id)] FROM [DealPosition] WHERE [DealPosition(deal)] IN (SELECT [DealPayment(deal)] FROM [DealPayment] WHERE [DealPayment(payment)]=NEW.id)));\n"
          + "  END IF;\n"
          + "  RETURN NEW;\n"
          + "END;")
})

public class PaymentImpl extends MappingObjectImpl implements Payment {
  @Column(defaultValue = "ПОДГОТОВКА")
  private State state;
  
  @Column(defaultValue="0.0")
  private BigDecimal amount = new BigDecimal(0.0);

  @OneToMany(mappedBy="payment")
  private List<DealPayment> dealPayments = new ArrayList<>();

  @Column
  private Date operationDate;

  @Column
  private String paymentDocumentNumber;

  @ManyToOne(viewFields={"company"},viewNames={"sellerCompany"})
  private CompanyPartition sellerCompanyPartition;
  
  @ManyToOne(viewFields={"name","storeType","objectType","controllIn","controllOut"}, 
          viewNames={"seller-store-name", "seller-store-type","seller-store-object-type","seller-store-controll-in","seller-store-controll-out"})
  private Store sellerStore;

  @ManyToOne(viewFields={"company"},viewNames={"customerCompany"})
  private CompanyPartition customerCompanyPartition;
  
  @ManyToOne(viewFields={"name","storeType","objectType","controllIn","controllOut"}, 
          viewNames={"customer-store-name", "customer-store-type","customer-store-object-type","customer-store-controll-in","customer-store-controll-out"})
  private Store customerStore;
  
  //Возможное основание
  @ManyToMany
  private List<Contract> contracts = new ArrayList<>();
  
  @Column
  private Integer receiptNumber;

  public PaymentImpl() throws RemoteException {
  }

  @Override
  public State getState() {
    return state;
  }

  @Override
  public void setState(State state) {
    this.state = state;
  }

  @Override
  public Store getSellerStore() throws RemoteException {
    return sellerStore;
  }

  @Override
  public void setSellerStore(Store sellerStore) throws RemoteException {
    this.sellerStore = sellerStore;
  }

  @Override
  public Store getCustomerStore() throws RemoteException {
    return customerStore;
  }

  @Override
  public void setCustomerStore(Store customerStore) throws RemoteException {
    this.customerStore = customerStore;
  }

  @Override
  public Integer getReceiptNumber() throws RemoteException {
    return receiptNumber;
  }

  @Override
  public void setReceiptNumber(Integer receiptNumber) throws RemoteException {
    this.receiptNumber = receiptNumber;
  }

  @Override
  public List<Contract> getContracts() throws RemoteException {
    return contracts;
  }

  @Override
  public void setContracts(List<Contract> contracts) throws RemoteException {
    this.contracts = contracts;
  }

  /*@Override
  public CreatedDocument getOrder() throws RemoteException {
    return order;
  }

  @Override
  public void setOrder(CreatedDocument order) throws RemoteException {
    this.order = order;
  }*/

  @Override
  public CompanyPartition getCustomerCompanyPartition() throws RemoteException {
    return customerCompanyPartition;
  }

  @Override
  public void setCustomerCompanyPartition(CompanyPartition customerCompanyPartition) throws RemoteException {
    this.customerCompanyPartition = customerCompanyPartition;
  }

  @Override
  public CompanyPartition getSellerCompanyPartition() throws RemoteException {
    return sellerCompanyPartition;
  }

  @Override
  public void setSellerCompanyPartition(CompanyPartition sellerCompanyPartition) throws RemoteException {
    this.sellerCompanyPartition = sellerCompanyPartition;
  }

  @Override
  public Date getOperationDate() throws RemoteException {
    return operationDate;
  }

  @Override
  public void setOperationDate(Date operationDate) throws RemoteException {
    this.operationDate = operationDate;
  }

  @Override
  public List<DealPayment> getDealPayments() throws RemoteException {
    return dealPayments;
  }

  @Override
  public void setDealPayments(List<DealPayment> dealPayments) throws RemoteException {
    this.dealPayments = dealPayments;
  }
  
  @Override
  public String getPaymentDocumentNumber() throws RemoteException {
    return paymentDocumentNumber;
  }

  @Override
  public void setPaymentDocumentNumber(String paymentDocumentNumber) throws RemoteException {
    this.paymentDocumentNumber = paymentDocumentNumber;
  }

  @Override
  public BigDecimal getAmount() throws RemoteException {
    return amount;
  }

  @Override
  public void setAmount(BigDecimal amount) throws RemoteException {
    this.amount = amount;
  }
}