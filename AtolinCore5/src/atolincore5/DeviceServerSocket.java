package atolincore5;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;

public class DeviceServerSocket implements RMIServerSocketFactory, Serializable {
    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
      //return SSLServerSocketFactory.getDefault().createServerSocket(port);
      return new ServerSocket(port);
    }
  }