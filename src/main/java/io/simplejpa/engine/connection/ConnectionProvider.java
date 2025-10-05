package io.simplejpa.engine.connection;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionProvider {
    Connection getConnection() throws SQLException;
    void closeConnection(Connection connection) throws SQLException;
    boolean supportsAggressiveRelease();
}
