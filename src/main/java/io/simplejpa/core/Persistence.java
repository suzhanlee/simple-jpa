package io.simplejpa.core;

public class Persistence {
    public static EntityManagerFactory createEntityManagerFactory(PersistenceConfiguration configuration) {
        validateConfigIsExists(configuration);
        return EntityManagerFactoryImpl.createEntityManagerFactoryInstance(configuration);
    }

    private static void validateConfigIsExists(PersistenceConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("PersistenceConfiguration cannot be null");
        }
    }
}
