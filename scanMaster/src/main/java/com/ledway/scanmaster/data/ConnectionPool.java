package com.ledway.scanmaster.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * Created by togb on 2017/2/18.
 */

public class ConnectionPool {

  private HashMap<String, Connection> mPool = new HashMap<>();

  public synchronized Connection getConnection(String connectionString)
      throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
    Connection connection = mPool.get(connectionString);
    if(connection == null || connection.isClosed()){
      Class.forName("net.sourceforge.jtds.jdbc.Driver").newInstance();
      connection = DriverManager.getConnection(connectionString, "sa", "ledway");
    }
    mPool.put(connectionString, connection);
    return connection;
  }


}
