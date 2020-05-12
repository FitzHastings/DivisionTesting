package bum.interfaces;

import java.rmi.RemoteException;
import mapping.MappingObject;

public interface ProductDocument extends MappingObject {
  public enum ActionType{СТАРТ,ОТГРУЗКА,ОПЛАТА}
  
  public void setNumber(Integer number) throws RemoteException;
  
  public ActionType getActionType() throws RemoteException;
  public void setActionType(ActionType actionType) throws RemoteException;

  public Document getDocument() throws RemoteException;
  public void setDocument(Document document) throws RemoteException;

  public Product getProduct() throws RemoteException;
  public void setProduct(Product product) throws RemoteException;
}