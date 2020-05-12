package division.update;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Updater extends Remote {
  public Command command(Command command) throws RemoteException;
}