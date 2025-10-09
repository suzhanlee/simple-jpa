package io.simplejpa.transaction;

import io.simplejpa.core.EntityTransaction;
import io.simplejpa.engine.connection.ConnectionProvider;
import io.simplejpa.exception.JdbcException;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public class JdbcTransaction implements TransactionCoordinator, EntityTransaction {
    private final ConnectionProvider connectionProvider;
    private Connection connection;
    private TransactionStatus status;

    public JdbcTransaction(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
        this.status = TransactionStatus.NOT_ACTIVE;
    }

    @Override
    public Connection getConnection() {
        if (!isActive()) {
            throw new IllegalStateException("Transaction is not active");
        }
        return connection;
    }

    @Override
    public void begin() {
        if (this.status != TransactionStatus.NOT_ACTIVE) {
            throw new IllegalStateException("Transaction is already active");
        }
        try {
            this.connection = connectionProvider.getConnection();
            this.connection.setAutoCommit(false);
            this.status = TransactionStatus.ACTIVE;
            log.debug("transaction begin");
        } catch (SQLException e) {
            log.error("Failed to get connection", e);
            throw new JdbcException("Failed to get connection", e);
        }
    }

    @Override
    public void commit() {
        if (this.status != TransactionStatus.ACTIVE) {
            throw new IllegalStateException("Transaction is not active");
        }
        try {
            connection.commit();
            this.status = TransactionStatus.COMMITTED;
            log.debug("transaction commit");
        } catch (SQLException e) {
            log.error("Failed to commit transaction", e);
            try {
                connection.rollback();
                this.status = TransactionStatus.ROLLED_BACK;
                log.warn("transaction rollback");
            } catch (SQLException ex) {
                log.error("Failed to rollback transaction", ex);
            }
            throw new JdbcException("Failed to commit transaction", e);
        } finally {
            closeConnection();
        }
    }

    @Override
    public void rollback() {
        if (this.status != TransactionStatus.ACTIVE) {
            log.warn("Attempting to rollback non-active transaction. Status: {}", status);
            return;
        }
        try {
            connection.rollback();
            connection.setAutoCommit(log.isTraceEnabled());
            this.status = TransactionStatus.ROLLED_BACK;
            log.debug("transaction rollback");
        } catch (SQLException e) {
            log.error("Failed to rollback transaction", e);
            throw new JdbcException("Failed to rollback transaction", e);
        } finally {
            closeConnection();
        }
    }

    @Override
    public boolean isActive() {
        return this.status == TransactionStatus.ACTIVE;
    }

    private void closeConnection() {
        try {
            if (!connection.isClosed()) {
                connection.setAutoCommit(true);
            }
            connectionProvider.closeConnection(connection);
            connection = null;
            // 트랜잭션 닫힐 때의 상태 정보 유지
            log.debug("transaction close");
        } catch (SQLException e) {
            log.error("Failed to close connection", e);
        }
    }

    @Override
    public TransactionStatus getStatus() {
        return this.status;
    }
}
