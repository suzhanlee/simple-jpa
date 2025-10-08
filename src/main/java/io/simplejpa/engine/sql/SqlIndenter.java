package io.simplejpa.engine.sql;

public class SqlIndenter {
    public void sqlIndentIfNeeded(StringBuilder sql) {
        int length = sql.length();
        if (length > 0 && sql.charAt(length - 1) != ' ') {
            sql.append(" ");
        }
    }
}
