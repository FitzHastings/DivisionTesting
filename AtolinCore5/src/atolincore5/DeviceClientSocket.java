package atolincore5;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;

public class DeviceClientSocket implements RMIClientSocketFactory, Serializable {
    private String host;

    public DeviceClientSocket(String host) {
      this.host = host;
    }
    
    @Override
    public Socket createSocket(String host, int port) throws IOException {
      System.out.println(host+" -> "+this.host);
      //return SSLSocketFactory.getDefault().createSocket(this.host, port);
      return new Socket(this.host, port);
    }
  }