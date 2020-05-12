package bum.util;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FileTransferProcessing extends Remote{
  public void openWriteFile(String fileName, long fileSize,long modificationDate) throws RemoteException;
  public void write(byte[] bytes) throws RemoteException;
  public void closeWriteFile() throws RemoteException;
  public void endApplicationTransfering() throws RemoteException;
  public void errorTransfering() throws RemoteException;
  public void serverError() throws RemoteException;
}