package io.simplejpa.engine.connection;

import lombok.Builder;
import lombok.Getter;

import java.util.Objects;

@Getter
public class ConnectionConfiguration {
    private final String url;
    private final String username;
    private final String password;
    private final String driverClassName;

    @Builder
    public ConnectionConfiguration(
            String url,
            String username,
            String password,
            String driverClassName
    ) {
        this.url = Objects.requireNonNull(url, "JDBC URL cannot be null");
        this.username = username;
        this.password = password;
        this.driverClassName = Objects.requireNonNull(driverClassName, "Driver class name cannot be null");
    }

    @Override
    public String toString() {
        return "ConnectionConfiguration{" +
                "url='" + url + '\'' +
                ", username='" + username + '\'' +
                ", password='" + (password != null ? "*".repeat(password.length()) : "null") + '\'' +
                ", driverClassName='" + driverClassName + '\'' +
                '}';
    }
}
