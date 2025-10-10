package io.simplejpa.cache.action;

import io.simplejpa.engine.jdbc.JdbcExecutor;
import io.simplejpa.engine.sql.InsertSqlGenerator;
import io.simplejpa.engine.sql.SqlWithParameters;
import io.simplejpa.metadata.EntityMetadata;

import java.sql.Connection;

public class InsertAction implements EntityAction {
    private final Object entity;
    private final EntityMetadata metadata;
    private final InsertSqlGenerator insertSqlGenerator;
    private final JdbcExecutor jdbcExecutor;

    public InsertAction(
            Object entity,
            EntityMetadata metadata,
            InsertSqlGenerator insertSqlGenerator,
            JdbcExecutor jdbcExecutor
    ) {
        this.entity = entity;
        this.metadata = metadata;
        this.insertSqlGenerator = insertSqlGenerator;
        this.jdbcExecutor = jdbcExecutor;
    }

    @Override
    public void execute(Connection connection) {
        SqlWithParameters sqlWithParameters = insertSqlGenerator.generate(
                metadata,
                entity
        );
        jdbcExecutor.executeUpdate(
                connection,
                sqlWithParameters.sql(),
                sqlWithParameters.parameters()
        );
    }

    @Override
    public Object getEntity() {
        return entity;
    }
}
