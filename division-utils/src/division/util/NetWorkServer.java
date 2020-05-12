package division.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;

public class NetWorkServer {
  private ConcurrentHashMap<Long,Socket> clientSocketList = new ConcurrentHashMap<Long, Socket>();
  private ExecutorService pool = Executors.newCachedThreadPool();
  private static NetWorkServer instance = new NetWorkServer();

  private NetWorkServer() {
  }

  public void startServer(final int serverPort) throws IOException {
    final ServerSocket serverSocket = new ServerSocket(serverPort);
    pool.submit(new Runnable() {
      public void run() {
        try {
          System.out.println("NETWORK SERVER STARTING ON "+serverPort+" PORT");
          Socket socket;
          while((socket = serverSocket.accept()) != null) {
            System.out.println("CLIENT: "+socket.getInetAddress().getHostAddress());
            Long clientSocketId = new Date().getTime();
            clientSocketList.put(clientSocketId, socket);
            writeObject(socket, clientSocketId);
          }
        }catch(Exception ex) {
          Logger.getRootLogger().fatal("", ex);
        }
      }
    });
  }

  public static Object readObject(Socket socket) throws IOException, ClassNotFoundException {
    synchronized(socket) {
      try {
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        return in.readObject();
      }catch(EOFException ex) {
        return null;
      }
    }
  }

  public static void writeObject(Socket socket, Object obj) throws IOException {
    synchronized(socket) {
      ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
      out.writeObject(obj);
      out.reset();
      out.flush();
      out.close();
      out = null;
    }
  }

  public Socket getSocket(Long clientSocketId) {
    return clientSocketList.get(clientSocketId);
  }

  public void removeSocket(Long clientSocketId) {
    clientSocketList.remove(clientSocketId);
  }

  public static NetWorkServer getInstance() {
    return instance;
  }
}