package io.simplejpa.engine.sql;

import java.util.Collections;
import java.util.List;

public class SqlBuilder {
    private final StringBuilder sql;
    private final SqlIndenter sqlIndenter;

    // TODO 이후 Spring Bean으로 등록 필요
    public SqlBuilder() {
        this.sql = new StringBuilder();
        this.sqlIndenter = new SqlIndenter();
    }

    public SqlBuilder appendTable(String tableName) {
        this.sql.append(tableName);
        return this;
    }

    public SqlBuilder append(String text) {
        sqlIndenter.sqlIndentIfNeeded(this.sql);
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
