package io.simplejpa.engine.sql;

import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

public class SqlBuilder {
    private final StringBuilder sql;

    public SqlBuilder(StringBuilder sql) {
        this.sql = sql;
    }

    public SqlBuilder appendTable(String tableName) {
        this.sql.append(tableName);
        return this;
    }

    public SqlBuilder append(String text) {
        if (!sql.isEmpty() && !sql.toString().endsWith(" ")) {
            sql.append(" ");
        }
        sql.append(text);
        return this;
    }

    public SqlBuilder appendColumns(List<String> columnNames) {
        String joined = String.join(", ", columnNames);
        sql.append(joined);
        return this;
    }

    public SqlBuilder appendPlaceholders(int count) {
        List<String> placeholders = Collections.nCopies(count, "?");
        sql.append(String.join(", ", placeholders));
        return this;
    }

    public String build() {
        return this.sql.toString().trim();
    }

}
