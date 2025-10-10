package io.simplejpa.cache.action;

import io.simplejpa.engine.jdbc.JdbcExecutor;
import io.simplejpa.engine.sql.DeleteSqlGenerator;
import io.simplejpa.engine.sql.SqlWithParameters;
import io.simplejpa.metadata.EntityMetadata;

import java.sql.Connection;

public class DeleteAction implements EntityAction {
    private final Object entity;
    private final EntityMetadata metadata;
    private final DeleteSqlGenerator deleteSqlGenerator;
    private final JdbcExecutor jdbcExecutor;

    public DeleteAction(
            Object entity,
            DeleteSqlGenerator deleteSqlGenerator,
            EntityMetadata metadata,
            JdbcExecutor jdbcExecutor
    ) {
        this.entity = entity;
        this.deleteSqlGenerator = deleteSqlGenerator;
        this.metadata = metadata;
        this.jdbcExecutor = jdbcExecutor;
    }

    @Override
    public void execute(Connection connection) {
        SqlWithParameters sqlWithParameters = deleteSqlGenerator.generateSql(
                metadata,
                metadata.getIdentifierMetadata().getValue(entity)
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
