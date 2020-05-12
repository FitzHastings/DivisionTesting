package bum.realizations;

import bum.annotations.Column;
import bum.interfaces.Comment;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import mapping.MappingObjectImpl;

public class CommentImpl extends MappingObjectImpl implements Comment {
  @Column
  private String objectClass;
  
  @Column
  private Integer objectId;
  
  @Column(length=1000, defaultValue="")
  private String text;
  
  @Column(length=1000, defaultValue="")
  private String author;
  
  @Column(nullable=false,defaultValue="CURRENT_TIMESTAMP", description="Дата и время контроля")
  private LocalDateTime controlDateTime;

  public CommentImpl() throws RemoteException {
    super();
  }

  @Override
  public Integer getObjectId() throws RemoteException {
    return objectId;
  }

  @Override
  public void setObjectId(Integer objectId) throws RemoteException {
    this.objectId = objectId;
  }

  @Override
  public LocalDateTime getControlDateTime() throws RemoteException {
    return controlDateTime;
  }

  @Override
  public void setControlDateTime(LocalDateTime controlDateTime) throws RemoteException {
    this.controlDateTime = controlDateTime;
  }

  @Override
  public String getObjectClass() throws RemoteException {
    return objectClass;
  }

  @Override
  public void setObjectClass(String objectClass) throws RemoteException {
    this.objectClass = objectClass;
  }

  @Override
  public String getText() throws RemoteException {
    return text;
  }

  @Override
  public void setText(String text) throws RemoteException {
    this.text = text;
  }

  @Override
  public String getAuthor() throws RemoteException {
    return author;
  }

  @Override
  public void setAuthor(String author) throws RemoteException {
    this.author = author;
  }
}