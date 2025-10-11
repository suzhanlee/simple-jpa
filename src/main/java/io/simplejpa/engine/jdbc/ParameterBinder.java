package io.simplejpa.engine.jdbc;

import io.simplejpa.exception.JdbcException;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ParameterBinder {
    private static final String NAMED_PARAMETER_PREFIX = ":";
    private static final String POSITIONAL_PARAMETER_PREFIX = "?";

    public void bind(PreparedStatement stmt, Object... params) throws SQLException {
        if (params == null || params.length == 0) {
            return;
        }

        for (int i = 0; i < params.length; i++) {
            int jdbcParameterIndex = i + 1;
            Object param = params[i];
            bindParameter(stmt, param, jdbcParameterIndex);
        }
    }

    private void bindParameter(PreparedStatement stmt, Object param, int paramIndex) throws SQLException {
        if (param == null) {
            stmt.setNull(paramIndex, Types.NULL);
        } else if (param instanceof String) {
            stmt.setString(paramIndex, (String) param);
        } else if (param instanceof Integer) {
            stmt.setInt(paramIndex, (Integer) param);
        } else if (param instanceof Long) {
            stmt.setLong(paramIndex, (Long) param);
        } else if (param instanceof Double) {
            stmt.setDouble(paramIndex, (Double) param);
        } else if (param instanceof Boolean) {
            stmt.setBoolean(paramIndex, (Boolean) param);
        } else if (param instanceof java.util.Date) {
            stmt.setTimestamp(paramIndex, new Timestamp(((java.util.Date) param).getTime()));
        } else if (param instanceof LocalDateTime) {
            stmt.setTimestamp(paramIndex, Timestamp.valueOf((LocalDateTime) param));
        } else if (param instanceof LocalDate) {
            stmt.setDate(paramIndex, Date.valueOf((LocalDate) param));
        } else {
            throw new JdbcException("Unsupported parameter type: " + param.getClass());
        }
    }

    public void bindQueryParameters(
            PreparedStatement pstmt,
            List<String> parameterOrder, //  [":name", ":age"] or ["?1", "?2"]
            Map<String, Object> namedParameters,
            Map<Integer, Object> positionalParameters
    ) throws SQLException {
        for (int i = 0; i < parameterOrder.size(); i++) {
            String parameterName = parameterOrder.get(i);
            Object parameterValue = extractParameterValue(namedParameters, positionalParameters, parameterName);
            bindParameter(pstmt, parameterValue, i + 1);
        }
    }

    private Object extractParameterValue(Map<String, Object> namedParameters, Map<Integer, Object> positionalParameters, String parameterName) {
        if (parameterName.startsWith(NAMED_PARAMETER_PREFIX)) {
            return namedParameters.get(parameterName.substring(1));
        } else if (parameterName.startsWith(POSITIONAL_PARAMETER_PREFIX)) {
            return positionalParameters.get(Integer.parseInt(parameterName.substring(1)));
        }
        throw new JdbcException("Unsupported parameter type: " + parameterName.getClass());
    }

}
