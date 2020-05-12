package bum.interfaces;

import java.rmi.RemoteException;
import java.util.Date;
import mapping.MappingObject;

public interface DealComment extends MappingObject {
  public Date getStopTime() throws RemoteException;
  public void setStopTime(Date stopTime) throws RemoteException;
  
  public Worker getWorker() throws RemoteException;
  public void setWorker(Worker worker) throws RemoteException;
  
  public String getComment() throws RemoteException;
  public void setComment(String comment) throws RemoteException;

  public Deal getDeal() throws RemoteException;
  public void setDeal(Deal deal) throws RemoteException;

  public DealCommentSubject getSubject() throws RemoteException;
  public void setSubject(DealCommentSubject subject) throws RemoteException;
}