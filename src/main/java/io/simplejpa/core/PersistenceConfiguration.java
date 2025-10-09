package io.simplejpa.core;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Set;

@Builder
@Getter
public class PersistenceConfiguration {
    private final String url;
    private final String username;
    private final String password;
    private final String driver;

    @Singular // addEntityClass 생성
    private final Set<Class<?>> entityClasses;

    public PersistenceConfiguration(
            String url,
            String username,
            String password,
            String driver,
            Set<Class<?>> entityClasses
    ) {
        validatePersistenceConfiguration(url, driver);
        this.url = url;
        this.username = username;
        this.password = password;
        this.driver = driver;
        this.entityClasses = entityClasses;
    }

    private void validatePersistenceConfiguration(String url, String driver) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("JDBC URL cannot be null");
        }
        if (driver == null || driver.isBlank()) {
            throw new IllegalArgumentException("Driver class name cannot be null");
        }
    }
}
