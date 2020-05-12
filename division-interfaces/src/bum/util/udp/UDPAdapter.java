package bum.util.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public abstract class UDPAdapter{
  private int serverport;
  private int clientport;
  private UDPServer udpserver;

  public UDPAdapter(int serverport) {
    this(serverport, serverport+1);
  }

  public UDPAdapter(int serverport, int clientport) {
    this.serverport = serverport;
    this.clientport = clientport;
    udpserver = new UDPServer(serverport) {
      @Override
      protected void getData(String data, InetAddress clientAddress) {
        listen(data, clientAddress);
      }
    };
  }

  public UDPServer getUdpserver() {
    return udpserver;
  }

  public int getClientport() {
    return clientport;
  }

  public void setClientport(int clientport) {
    this.clientport = clientport;
  }

  abstract public void listen(String data, InetAddress clientAddress);

  public void send(String data, InetAddress address) {
    System.out.println("SEND UDP "+data);
    DatagramSocket socket=null;
    try {
      socket = new DatagramSocket();
      byte mess_b[] = data.getBytes();
      DatagramPacket packet = new DatagramPacket(mess_b, mess_b.length, address, clientport);
      socket.send(packet);
    }catch (IOException io) {
      System.out.println("Error send message by UDP. " + io.getMessage());
      io.printStackTrace();
    }finally {
      if(socket!=null)
        socket.close();
    }
  }
}