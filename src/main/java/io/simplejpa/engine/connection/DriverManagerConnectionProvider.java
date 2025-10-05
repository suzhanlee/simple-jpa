package io.simplejpa.engine.connection;

import io.simplejpa.exception.JdbcException;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Slf4j
public class DriverManagerConnectionProvider implements ConnectionProvider {
    private final ConnectionConfiguration configuration;

    public DriverManagerConnectionProvider(ConnectionConfiguration configuration) {
        this.configuration = configuration;
        try {
            Class.forName(configuration.getDriverClassName());
        } catch (ClassNotFoundException e) {
            log.error("JDBC Driver not found: {}", configuration.getDriverClassName(), e);
            throw new JdbcException("JDBC Driver not found: " + configuration.getDriverClassName(), e);
        }
        log.info("DriverManagerConnectionProvider initialized: driver={}, url={}",
                configuration.getDriverClassName(),
                configuration.getUrl());
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                configuration.getUrl(),
                configuration.getUsername(),
                configuration.getPassword()
        );
    }

    @Override
    public void closeConnection(Connection connection) throws SQLException {
        if (connection != null && !connection.isClosed()) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.warn("Failed to close connection", e);
                throw e;
            }
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false; // pool 폴링 없음
    }
}
