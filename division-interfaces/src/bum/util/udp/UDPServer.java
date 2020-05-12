package bum.util.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class UDPServer implements Runnable {
  private ExecutorService pool = Executors.newSingleThreadExecutor();
  private DatagramSocket socket;
  private int port;

  public UDPServer(int port) {
    this.port = port;
    pool.submit(this);
  }

  protected abstract void getData(String data, InetAddress clientAddress);

  @SuppressWarnings("static-access")
  public void stop() {
    pool.shutdown();
    socket.close();
  }

  public void run() {
    System.out.println("UDP SERVER STARTED");
    try {
      byte[] buffer = new byte[1024];
      socket = new DatagramSocket(port);
      while(true) {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        InetAddress client = packet.getAddress();
        final String message = new String(buffer, packet.getOffset(), packet.getLength());
        getData(message, client);
      }
    }catch(Exception ex) {
      System.out.println(ex.getMessage());
    }finally {
      System.out.println("UDP SERVER STOPED");
    }
  }
}