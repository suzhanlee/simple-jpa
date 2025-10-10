package io.simplejpa.cache.action;

import io.simplejpa.engine.jdbc.JdbcExecutor;
import io.simplejpa.engine.sql.SqlWithParameters;
import io.simplejpa.engine.sql.UpdateSqlGenerator;
import io.simplejpa.metadata.EntityMetadata;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;

@Slf4j
public class UpdateAction implements EntityAction {
    private final Object entity;
    private final EntityMetadata metadata;
    private final UpdateSqlGenerator updateSqlGenerator;
    private final JdbcExecutor jdbcExecutor;

    public UpdateAction(
            Object entity,
            UpdateSqlGenerator updateSqlGenerator,
            EntityMetadata metadata,
            JdbcExecutor jdbcExecutor
    ) {
        this.entity = entity;
        this.updateSqlGenerator = updateSqlGenerator;
        this.metadata = metadata;
        this.jdbcExecutor = jdbcExecutor;
    }

    @Override
    public void execute(Connection connection) {
        SqlWithParameters sqlWithParameters = updateSqlGenerator.generateUpdateSql(
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
