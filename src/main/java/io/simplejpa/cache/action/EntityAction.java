package io.simplejpa.cache.action;

import java.sql.Connection;

public interface EntityAction {
    void execute(Connection connection);

    Object getEntity();
}
