package util.dbListeners;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoveListener extends Remote {
  public void removedObjects(Integer[] ids) throws RemoteException;
}
