package division.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;

public class TurnOffUtil {
  private static ExecutorService pool = Executors.newCachedThreadPool();

  public static void startTurnOffServer(final int serverOffPort, final String serverOffCommand) {
    pool.submit(() -> {
      ServerSocket offSocket = null;
      BufferedReader reader = null;
      Socket socket = null;
      try{
        offSocket = new ServerSocket(serverOffPort);
        while((socket = offSocket.accept()) != null) {
          reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
          if(reader.readLine().equals(serverOffCommand))
            System.exit(0);
        }
      }catch(Exception ex) {
        Logger.getRootLogger().fatal("", ex);
      }finally {
        try {
          if(offSocket != null)
            offSocket.close();
          if(reader != null) {
            reader.reset();
            reader.close();
          }
          if(socket != null)
            socket.close();
        }catch(Exception ex) {
          Logger.getRootLogger().error("", ex);
        }
      }
    });
  }

  public static void kill(String offHost, int offPort, String offCommand) {
    Socket socket = null;
    PrintWriter writer = null;
    try {
      socket = new Socket(offHost,offPort);
      writer = new PrintWriter(socket.getOutputStream(),true);
      writer.write(offCommand);
    }catch(Exception ex) {
      System.out.println(ex.getMessage());
    }finally {
      try {
        if(writer != null) {
          writer.flush();
          writer.close();
        }
        if(socket != null)
          socket.close();
        writer = null;
        socket = null;
      }catch(Exception ex) {
        Logger.getRootLogger().error("", ex);
      }
    }
  }
}