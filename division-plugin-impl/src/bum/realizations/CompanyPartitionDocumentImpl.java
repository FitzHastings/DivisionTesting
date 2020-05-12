package bum.realizations;

import bum.annotations.Column;
import bum.annotations.ManyToOne;
import bum.annotations.Table;
import bum.interfaces.CompanyPartition;
import bum.interfaces.CompanyPartitionDocument;
import bum.interfaces.Document;
import bum.interfaces.Document.DocumentDateType;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

@Table
public class CompanyPartitionDocumentImpl extends MappingObjectImpl implements CompanyPartitionDocument {
  @ManyToOne
  private CompanyPartition partition;
  
  @ManyToOne(viewFields={"name"},viewNames={"document_name"})
  private Document document;
  
  @Column
  private DocumentDateType dateType;
  
  @Column(defaultValue="")
  private String prefix;
  
  @Column
  private String prefixTypeFormat;

  @Column(defaultValue="")
  private String suffix;
  
  @Column
  private String suffixTypeFormat;

  @Column(defaultValue="")
  private String prefixSplit;
  
  @Column(defaultValue="")
  private String suffixSplit;

  @Column
  private Period periodForZero;
  
  @Column(defaultValue="0")
  private Integer startNumber;
  
  @Column(defaultValue = "false")
  private Boolean grabFreeNumber;

  public CompanyPartitionDocumentImpl() throws RemoteException {
    super();
  }

  @Override
  public Boolean isGrabFreeNumber() throws RemoteException {
    return grabFreeNumber;
  }

  @Override
  public void setGrabFreeNumber(Boolean grabFreeNumber) throws RemoteException {
    this.grabFreeNumber = grabFreeNumber;
  }

  @Override
  public String getPrefixTypeFormat() throws RemoteException {
    return prefixTypeFormat;
  }

  @Override
  public void setPrefixTypeFormat(String prefixTypeFormat) throws RemoteException {
    this.prefixTypeFormat = prefixTypeFormat;
  }

  @Override
  public String getSuffixTypeFormat() throws RemoteException {
    return suffixTypeFormat;
  }

  @Override
  public void setSuffixTypeFormat(String suffixTypeFormat) throws RemoteException {
    this.suffixTypeFormat = suffixTypeFormat;
  }
  
  @Override
  public DocumentDateType getDateType() throws RemoteException {
    return dateType;
  }

  @Override
  public void setDateType(DocumentDateType dateType) throws RemoteException {
    this.dateType = dateType;
  }

  @Override
  public Document getDocument() throws RemoteException {
    return document;
  }

  @Override
  public void setDocument(Document document) throws RemoteException {
    this.document = document;
  }

  @Override
  public CompanyPartition getPartition() throws RemoteException {
    return partition;
  }

  @Override
  public void setPartition(CompanyPartition partition) throws RemoteException {
    this.partition = partition;
  }

  @Override
  public Period getPeriodForZero() throws RemoteException {
    return periodForZero;
  }

  @Override
  public void setPeriodForZero(Period periodForZero) throws RemoteException {
    this.periodForZero = periodForZero;
  }

  @Override
  public String getPrefix() throws RemoteException {
    return prefix;
  }

  @Override
  public void setPrefix(String prefix) throws RemoteException {
    this.prefix = prefix;
  }

  @Override
  public String getPrefixSplit() throws RemoteException {
    return prefixSplit;
  }

  @Override
  public void setPrefixSplit(String prefixSplit) throws RemoteException {
    this.prefixSplit = prefixSplit;
  }

  @Override
  public Integer getStartNumber() throws RemoteException {
    return startNumber;
  }

  @Override
  public void setStartNumber(Integer startNumber) throws RemoteException {
    this.startNumber = startNumber;
  }

  @Override
  public String getSuffix() throws RemoteException {
    return suffix;
  }

  @Override
  public void setSuffix(String suffix) throws RemoteException {
    this.suffix = suffix;
  }

  @Override
  public String getSuffixSplit() throws RemoteException {
    return suffixSplit;
  }

  @Override
  public void setSuffixSplit(String suffixSplit) throws RemoteException {
    this.suffixSplit = suffixSplit;
  }
}