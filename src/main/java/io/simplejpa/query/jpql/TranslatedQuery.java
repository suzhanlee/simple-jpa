package io.simplejpa.query.jpql;

import java.util.List;

public record TranslatedQuery(
        String sql,
        List<String> parameterOrder
) {
}
