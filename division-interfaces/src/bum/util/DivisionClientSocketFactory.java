package bum.util;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;

public class DivisionClientSocketFactory implements RMIClientSocketFactory, Serializable {
  private String host;

  public DivisionClientSocketFactory(String host) {
    this.host = host;
  }
  
  @Override
  public Socket createSocket(String host, int port) throws IOException {
    return new Socket(this.host, port);
  }
}