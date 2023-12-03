package com.drewmalin.vm.central.data.datasource;

import com.drewmalin.vm.central.utils.DurationUtils;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Datasource {

    private static final Logger LOGGER = LoggerFactory.getLogger(Datasource.class);

    final String host;
    final int port;
    final String database;
    final String username;
    final String password;
    final Engine engine;
    final int maxPoolSize;

    Datasource(final DatasourceBuilder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.database = builder.database;
        this.username = builder.username;
        this.password = builder.password;
        this.engine = builder.engine;
        this.maxPoolSize = builder.maxPoolSize;
    }

    public abstract Pool newPool(Vertx vertx);

    public abstract String getUrl();

    public void performMigrations() {
        LOGGER.info("Beginning datasource migrations");

        final var configuration = new FluentConfiguration()
            .dataSource(getUrl(), this.username, this.password);

        final var flyway = new Flyway(configuration);

        try {
            final var start = System.currentTimeMillis();
            final var result = flyway.migrate();
            final var duration = DurationUtils.toString(start);
            LOGGER.info("Completed %d migrations in %s".formatted(result.migrationsExecuted, duration));
        }
        catch (final FlywayException e) {
            LOGGER.error(e.getMessage());
        }

    }

    public static DatasourceBuilder builder() {
        return new Datasource.DatasourceBuilder();
    }

    public static class DatasourceBuilder {

        private String host;
        private int port;
        private String database;
        private String username;
        private String password;
        private Engine engine;
        public int maxPoolSize;

        private DatasourceBuilder() {
        }

        public DatasourceBuilder host(final String host) {
            this.host = host;
            return this;
        }

        public DatasourceBuilder port(final int port) {
            this.port = port;
            return this;
        }

        public DatasourceBuilder database(final String database) {
            this.database = database;
            return this;
        }

        public DatasourceBuilder username(final String username) {
            this.username = username;
            return this;
        }

        public DatasourceBuilder password(final String password) {
            this.password = password;
            return this;
        }

        public DatasourceBuilder engine(final String engineName) {
            for (final Engine eng : Engine.values()) {
                if (engineName.equals(eng.engineName)) {
                    this.engine = eng;
                    return this;
                }
            }
            throw new IllegalArgumentException("Unknown engine name '%s'".formatted(engineName));
        }

        public DatasourceBuilder maxPoolSize(final int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        public Datasource build() {
            return switch (this.engine) {
                case POSTGRES -> new PostgresDatasource(this);
                case H2 -> new H2Datasource(this);
            };
        }
    }

    private enum Engine {
        POSTGRES("postgres"),
        H2("h2"),
        ;

        private final String engineName;

        Engine(final String engineName) {
            this.engineName = engineName;
        }
    }
}
