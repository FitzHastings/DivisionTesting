package util.proxy;

import java.lang.reflect.InvocationTargetException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DelegatorInterface extends Remote {
  public Object invoke(
          Object proxy,
          String methodName,
          Class<?> returnType,
          Class<?>[] parameterTypes,
          Object[] args) throws RemoteException, NoSuchMethodException, IllegalAccessException, InvocationTargetException;
}