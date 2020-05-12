package util.proxy;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import mapping.MappingObject;

public final class Delegator implements InvocationHandler, Serializable {
  private ObjectProxy objectProxy;
  //private RemoteSession session;
  
  public static Object newInstance(
          int objectPort, 
          RMIClientSocketFactory clientSocketFactory, 
          RMIServerSocketFactory serverSocketFactory, 
          MappingObject object) throws RemoteException {
    Object proxy = java.lang.reflect.Proxy.newProxyInstance(
            object.getClass().getClassLoader(),
            object.getClass().getInterfaces(),
            new Delegator(objectPort, clientSocketFactory, serverSocketFactory, object));
    return proxy;
  }
  
  private Delegator(
          int objectPort, 
          RMIClientSocketFactory clientSocketFactory, 
          RMIServerSocketFactory serverSocketFactory, 
          MappingObject object) throws RemoteException {
    objectProxy = new ObjectProxyImpl(objectPort, clientSocketFactory, serverSocketFactory, object);
    //setSession(session);
  }
  
  public ObjectProxy getObjectProxy() throws RemoteException {
    return objectProxy;
  }
  
  /*public void setSession(RemoteSession session) {
    this.session = session;
  }
  
  public RemoteSession getSession() {
    return session;
  }*/

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    return objectProxy.invoke(
            //session,
            proxy, 
            method.getName(), 
            method.getReturnType(), 
            method.getParameterTypes(), 
            args,
            false);
  }
  
  public Object invokeWithoutProxy(Method method, Object[] args) throws Throwable {
    return objectProxy.invoke(
            //session,
            null, 
            method.getName(), 
            method.getReturnType(), 
            method.getParameterTypes(), 
            args, 
            true);
  }
}