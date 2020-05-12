package bum.realizations;

import bum.annotations.Column;
import bum.annotations.ManyToOne;
import bum.annotations.QueryColumn;
import bum.annotations.Table;
import bum.interfaces.Deal;
import bum.interfaces.DealComment;
import bum.interfaces.DealCommentSubject;
import bum.interfaces.Worker;
import java.rmi.RemoteException;
import java.util.Date;
import mapping.MappingObjectImpl;

@Table(queryColumns={
  @QueryColumn(
        name="processId",
        query="SELECT [Deal(service)] FROM [Deal] WHERE [Deal(id)]=[DealComment(deal)]"),
  @QueryColumn(
        name="processName",
        query="SELECT name FROM [Service] WHERE [Service(id)]=(SELECT [Deal(service)] FROM [Deal] WHERE [Deal(id)]=[DealComment(deal)])"),
  @QueryColumn(
        name="customerPartition",
        query="SELECT [Deal(customerCompanyPartition)] FROM [Deal] WHERE [Deal(id)]=[DealComment(deal)]"),
  @QueryColumn(
        name="customerPartitionName",
        query="SELECT getCompanyPartitionName([Deal(customerCompanyPartition)]) FROM [Deal] WHERE [Deal(id)]=[DealComment(deal)]"),
  @QueryColumn(
        name="sellerPartition",
        query="SELECT [Deal(sellerCompanyPartition)] FROM [Deal] WHERE [Deal(id)]=[DealComment(deal)]"),
  @QueryColumn(
        name="sellerPartitionName",
        query="SELECT getCompanyPartitionName([Deal(sellerCompanyPartition)]) FROM [Deal] WHERE [Deal(id)]=[DealComment(deal)]"),
  @QueryColumn(
        name="avtor",
        query="SELECT [Worker(people_surname)]||' '||[Worker(people_name)]||' '||[Worker(people_lastname)] FROM [Worker] WHERE [Worker(id)]=[DealComment(worker)]")
})

public class DealCommentImpl extends  MappingObjectImpl implements DealComment {
  @ManyToOne
  private Deal deal;
  
  @ManyToOne(viewFields={"name"}, viewNames={"subject_name"})
  private DealCommentSubject subject;
  
  @Column(length=1000, defaultValue="")
  private String comment;
  
  @ManyToOne
  private Worker worker;
  
  @Column(defaultValue="CURRENT_DATE")
  private Date stopTime;
  
  public DealCommentImpl() throws RemoteException {
    super();
  }

  @Override
  public Date getStopTime() throws RemoteException {
    return stopTime;
  }

  @Override
  public void setStopTime(Date stopTime) throws RemoteException {
    this.stopTime = stopTime;
  }

  @Override
  public Worker getWorker() throws RemoteException {
    return worker;
  }

  @Override
  public void setWorker(Worker worker) throws RemoteException {
    this.worker = worker;
  }

  @Override
  public String getComment() throws RemoteException {
    return comment;
  }

  @Override
  public void setComment(String comment) throws RemoteException {
    this.comment = comment;
  }

  @Override
  public Deal getDeal() throws RemoteException {
    return deal;
  }

  @Override
  public void setDeal(Deal deal) throws RemoteException {
    this.deal = deal;
  }

  @Override
  public DealCommentSubject getSubject() throws RemoteException {
    return subject;
  }

  @Override
  public void setSubject(DealCommentSubject subject) throws RemoteException {
    this.subject = subject;
  }
}