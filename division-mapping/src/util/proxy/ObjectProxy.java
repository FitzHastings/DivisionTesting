package util.proxy;

import java.rmi.Remote;
import util.RemoteSession;

public interface ObjectProxy extends Remote {
  public Object invoke(
          //RemoteSession session,
          Object proxy, 
          String methodName, 
          Class<?> returnType, 
          Class<?>[] parameterTypes, 
          Object[] args,
          boolean nonProxy) throws Exception;
}