package io.simplejpa.engine.jdbc;

import io.simplejpa.exception.JdbcException;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JdbcExecutor {
    private final ParameterBinder parameterBinder;

    public JdbcExecutor(ParameterBinder parameterBinder) {
        this.parameterBinder = parameterBinder;
    }

    public int executeUpdate(
            Connection connection,
            String sql,
            Object... params
    ) {
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(sql);
            parameterBinder.bind(pstmt, params);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new JdbcException("Failed to execute update: " + sql, e);
        } finally {
            closePrepareStatement(pstmt);
        }
    }

    public <T> T executeQuery(
            Connection connection,
            String sql,
            ResultSetExtractor<T> resultSetExtractor,
            Object... params
    ) {
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;
        try {
            pstmt = connection.prepareStatement(sql);
            parameterBinder.bind(pstmt, params);
            resultSet = pstmt.executeQuery();
            return resultSetExtractor.extractData(resultSet);
        } catch (SQLException e) {
            throw new JdbcException("Failed to execute query: " + sql, e);
        } finally {
            closePrepareStatement(pstmt);
            closeResultSet(resultSet);
        }
    }

    public <T> T executeQueryForObject(
            Connection connection,
            String sql,
            Class<T> classType,
            ResultSetExtractor<T> resultSetExtractor,
            Object... params
    ) {
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;
        try {
            pstmt = connection.prepareStatement(sql);
            parameterBinder.bind(pstmt, params);
            resultSet = pstmt.executeQuery();
            if (resultSet.next()) {
                return resultSetExtractor.extractData(resultSet);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new JdbcException("Failed to execute query for object", e);
        } finally {
            closePrepareStatement(pstmt);
            closeResultSet(resultSet);
        }
    }

    public <T> List<T> executeQueryForList(
            Connection connection,
            String sql,
            ResultSetExtractor<T> resultSetExtractor,
            Object... params
    ) {
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;
        try {
            pstmt = connection.prepareStatement(sql);
            parameterBinder.bind(pstmt, params);
            resultSet = pstmt.executeQuery();
            List<T> valueList = new ArrayList<>();
            while (resultSet.next()) {
                T value = resultSetExtractor.extractData(resultSet);
                if (value != null) {
                    valueList.add(value);
                }
            }
            return valueList;
        } catch (SQLException e) {
            throw new JdbcException("Failed to execute query for list", e);
        } finally {
            closePrepareStatement(pstmt);
            closeResultSet(resultSet);
        }
    }

    private void closePrepareStatement(PreparedStatement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.warn("Failed to close PreparedStatement", e);
            }
        }
    }

    private void closeResultSet(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                log.warn("Failed to close ResultSet", e);
            }
        }
    }
}
