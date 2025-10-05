package io.simplejpa.engine.jdbc;

import io.simplejpa.exception.JdbcException;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public ResultSet executeQuery(
            Connection connection,
            String sql,
            Object... params
    ) {
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;
        try {
            pstmt = connection.prepareStatement(sql);
            parameterBinder.bind(pstmt, params);
            resultSet = pstmt.executeQuery();
            // TODO resource close 필요
            return resultSet;
        } catch (SQLException e) {
            throw new JdbcException("Failed to execute query: " + sql, e);
        }
    }

    public <T> T executeQueryForObject(
            Connection connection,
            String sql,
            Class<T> classType,
            Object... params
    ) {
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;
        try {
            pstmt = connection.prepareStatement(sql);
            parameterBinder.bind(pstmt, params);
            resultSet = pstmt.executeQuery();
            if (resultSet.next()) {
                return extractValue(classType, resultSet, 1);
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
            Class<T> classType,
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
                T value = extractValue(classType, resultSet, 1);
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

    private <T> T extractValue(
            Class<T> classType,
            ResultSet resultSet,
            int columnIndex
    ) throws SQLException {
        if (classType.equals(String.class)) {
            return classType.cast(resultSet.getString(columnIndex));
        } else if (classType.equals(Integer.class)) {
            return classType.cast(resultSet.getInt(columnIndex));
        } else if (classType.equals(Long.class)) {
            return classType.cast(resultSet.getLong(columnIndex));
        } else if (classType.equals(Double.class)) {
            return classType.cast(resultSet.getDouble(columnIndex));
        } else if (classType.equals(Boolean.class)) {
            return classType.cast(resultSet.getBoolean(columnIndex));
        } else if (classType.equals(java.util.Date.class)) {
            return classType.cast(resultSet.getTimestamp(columnIndex));
        } else if (classType.equals(LocalDateTime.class)) {
            Timestamp ts = resultSet.getTimestamp(columnIndex);
            return ts != null ? classType.cast(ts.toLocalDateTime()) : null;
        } else if (classType.equals(LocalDate.class)) {
            Date date = resultSet.getDate(columnIndex);
            return date != null ? classType.cast(date.toLocalDate()) : null;
        } else {
            throw new JdbcException("Unsupported parameter type: " + classType);
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
