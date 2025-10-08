package io.simplejpa.engine.sql;

import java.util.List;

public record SqlWithParameters(
        String sql,
        List<Object> parameters
) {
}
