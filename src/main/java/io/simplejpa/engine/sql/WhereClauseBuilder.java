package io.simplejpa.engine.sql;

public class WhereClauseBuilder {
    private final StringBuilder sql;
    private final SqlIndenter sqlIndenter;

    // TODO 이후 Spring Bean으로 등록 필요
    public WhereClauseBuilder() {
        this.sql = new StringBuilder();
        this.sqlIndenter = new SqlIndenter();
    }

    public WhereClauseBuilder where(String columnName) {
        sqlIndenter.sqlIndentIfNeeded(this.sql);
        sql.append("WHERE ").append(columnName);
        return this;
    }

    public WhereClauseBuilder equals() {
        sqlIndenter.sqlIndentIfNeeded(this.sql);
        sql.append("= ?");
        return this;
    }

    public WhereClauseBuilder and(String columnName) {
        sqlIndenter.sqlIndentIfNeeded(this.sql);
        sql.append("AND ")
                .append(columnName)
                .append(" = ?");
        return this;
    }

    public WhereClauseBuilder whereEquals(String columnName) {
        sqlIndenter.sqlIndentIfNeeded(this.sql);
        sql.append("WHERE ")
                .append(columnName)
                .append("= ?");
        return this;
    }

    public WhereClauseBuilder andEquals(String columnName) {
        sqlIndenter.sqlIndentIfNeeded(this.sql);
        sql.append("AND ")
                .append(columnName)
                .append(" = ?");
        return this;
    }

    public WhereClauseBuilder or(Condition condition) {
        sqlIndenter.sqlIndentIfNeeded(this.sql);
        sql.append("OR ").append(condition.toSql());
        return this;
    }

    public WhereClauseBuilder where(Condition condition) {
        sqlIndenter.sqlIndentIfNeeded(this.sql);
        sql.append("WHERE ").append(condition.toSql());
        return this;
    }

    public WhereClauseBuilder and(Condition condition) {
        sqlIndenter.sqlIndentIfNeeded(this.sql);
        sql.append("AND ").append(condition.toSql());
        return this;
    }

    public String build() {
        return sql.toString();
    }
}
