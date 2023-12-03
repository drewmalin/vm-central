package com.drewmalin.vm.central.data.datasource;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgresDatasource
    extends Datasource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresDatasource.class);

    PostgresDatasource(final DatasourceBuilder builder) {
        super(builder);
    }

    @Override
    public Pool newPool(final Vertx vertx) {
        LOGGER.info("Connecting to PostgreSQL pool: host=%s, port=%d, username=%s, maxSize=%d"
            .formatted(this.host, this.port, this.username, this.maxPoolSize));

        final var connectOptions = new PgConnectOptions()
            .setPort(this.port)
            .setHost(this.host)
            .setDatabase(this.database)
            .setUser(this.username)
            .setPassword(this.password);

        final var poolOptions = new PoolOptions()
            .setMaxSize(this.maxPoolSize);

        return PgPool.pool(vertx, connectOptions, poolOptions);
    }

    @Override
    public String getUrl() {
        return "jdbc:postgresql://%s:%d/%s".formatted(this.host, this.port, this.database);
    }
}
