package bum.realizations;

import bum.annotations.Column;
import bum.annotations.ManyToOne;
import bum.annotations.Table;
import bum.interfaces.Document;
import bum.interfaces.Product;
import bum.interfaces.ProductDocument;
import bum.interfaces.ProductDocument.ActionType;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

@Table
public class ProductDocumentImpl extends MappingObjectImpl implements ProductDocument {
  @Column
  private ActionType actionType;
  
  @ManyToOne(viewFields={"id"}, viewNames={"product_id"})
  private Product product;
  
  @ManyToOne(viewFields={"id", "name", "script", "system","scriptLanguage"}, 
          viewNames={"document_id", "document_name","document_script","document-system","document_script_language"})
  private Document document;
  
  @Column
  private Integer number;

  public ProductDocumentImpl() throws RemoteException {
  }

  public Integer getNumber() {
    return number;
  }

  @Override
  public void setNumber(Integer number) throws RemoteException {
    this.number = number;
  }

  @Override
  public ActionType getActionType() throws RemoteException {
    return actionType;
  }

  @Override
  public void setActionType(ActionType actionType) throws RemoteException {
    this.actionType = actionType;
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
  public Product getProduct() throws RemoteException {
    return product;
  }

  @Override
  public void setProduct(Product product) throws RemoteException {
    this.product = product;
  }
}