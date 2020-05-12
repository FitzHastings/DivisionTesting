package division.update;

import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;

public class UpdaterImpl extends UnicastRemoteObject implements Updater {
  public UpdaterImpl(int port, RMIClientSocketFactory csf, RMIServerSocketFactory ssf) throws RemoteException {
    super(port, csf, ssf);
  }

  @Override
  public Command command(Command command) throws RemoteException {
    if(command.getCommand().equals("stop"))
      System.exit(0);
    if(UpdateUtility.validateCommand(command)) {
      switch(command.getCommand()) {
        case "get-update-list":
          UpdateUtility.getUpdateList(command);
          break;
        case "get-update-instruction":
          UpdateUtility.getUpdateInstruction(command);
          break;
        case "get-file":
          UpdateUtility.getFile(command);
          break;
      }
    }
    return command;
  }
}