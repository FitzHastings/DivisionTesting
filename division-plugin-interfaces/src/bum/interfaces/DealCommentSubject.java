package bum.interfaces;

import java.awt.Color;
import java.rmi.RemoteException;
import mapping.MappingObject;

public interface DealCommentSubject extends MappingObject {
  public Color getColor() throws RemoteException;
  public void setColor(Color color) throws RemoteException;
}