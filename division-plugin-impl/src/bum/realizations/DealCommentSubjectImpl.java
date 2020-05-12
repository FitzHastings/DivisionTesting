package bum.realizations;

import bum.annotations.Column;
import bum.annotations.Table;
import bum.interfaces.DealCommentSubject;
import java.awt.Color;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

@Table
public class DealCommentSubjectImpl extends MappingObjectImpl implements DealCommentSubject{
  @Column(defaultValue="-16776961")
  private Color color = Color.BLUE;

  public DealCommentSubjectImpl() throws RemoteException {
    super();
  }

  @Override
  public Color getColor() throws RemoteException {
    return color;
  }

  @Override
  public void setColor(Color color) throws RemoteException {
    this.color = color;
  }
}