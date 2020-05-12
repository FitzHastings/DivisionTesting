package bum.util;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;

public class DivisionServerSocketFactory implements RMIServerSocketFactory, Serializable {
  @Override
  public ServerSocket createServerSocket(int port) throws IOException {
    return new ServerSocket(port);
  }
}