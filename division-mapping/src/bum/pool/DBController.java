package bum.pool;

import conf.P;
import java.sql.Connection;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.apache.commons.dbcp2.datasources.SharedPoolDataSource;
import org.apache.log4j.Logger;

public class DBController {
  private static DataSource dataSource;
  
  public static void stop() {
    try {
      if(dataSource != null)
        ((SharedPoolDataSource)dataSource).close();
    }catch(Exception ex){}
  }
  
  public static Connection getConnection() {
    return getConnection(false);
  }
  
  public static Connection getConnection(boolean autoCommit) {
    try {
      if(dataSource == null) {
        DriverAdapterCPDS pcds = new DriverAdapterCPDS(); 
        pcds.setDriver(P.String("data-source.driver-name")); 
        pcds.setUrl(P.String("data-source.jdbc-name")+"://"+
                P.String("data-source.host")+":"+
                P.Integer("data-source.port")+"/"+
                P.String("data-source.database-name"));
        pcds.setUser(P.String("data-source.user"));
        pcds.setPassword(P.String("data-source.password"));
        
        Logger.getRootLogger().info(pcds.getUrl());
        
        dataSource = new SharedPoolDataSource();
        
        ((SharedPoolDataSource)dataSource).setConnectionPoolDataSource(pcds);
        ((SharedPoolDataSource)dataSource).setDefaultAutoCommit(autoCommit);
        ((SharedPoolDataSource)dataSource).setMaxTotal(1000);
        ((SharedPoolDataSource)dataSource).setDefaultMaxIdle(P.Integer("data-source.max-transactions"));
        ((SharedPoolDataSource)dataSource).setDefaultMinIdle(P.Integer("data-source.min-transactions"));
        ((SharedPoolDataSource)dataSource).setDefaultTimeBetweenEvictionRunsMillis(600000);
        ((SharedPoolDataSource)dataSource).setDefaultMinEvictableIdleTimeMillis(1800000);
        ((SharedPoolDataSource)dataSource).setDefaultNumTestsPerEvictionRun(20);
      }
      
      Connection connection = dataSource.getConnection();
      connection.setAutoCommit(autoCommit);
      return connection;
    }catch(Exception ex) {
      Logger.getLogger(DBController.class).error(ex);
    }
    return null;
  }
  
  public static int getActiveConnectionCount() {
    return ((SharedPoolDataSource)dataSource).getNumActive();
  }
  
  public static int getIdleConnectionCount() {
    return ((SharedPoolDataSource)dataSource).getNumIdle();
  }
}