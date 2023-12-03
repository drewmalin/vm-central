package com.drewmalin.vm.central.data.datasource;

import io.vertx.core.Vertx;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2Datasource
    extends Datasource {

    private static final Logger LOGGER = LoggerFactory.getLogger(H2Datasource.class);

    H2Datasource(final DatasourceBuilder builder) {
        super(builder);
    }

    @Override
    public Pool newPool(final Vertx vertx) {
        final var url = "%s:%s".formatted(this.host, this.database);

        LOGGER.info("Connecting to H2 pool: jdbcUrl=%s, username=%s, maxSize=%d"
            .formatted(url, this.username, this.maxPoolSize));

        final var connectOptions = new JDBCConnectOptions()
            .setJdbcUrl(url)
            .setUser(this.username)
            .setPassword(this.password);

        final var poolOptions = new PoolOptions()
            .setMaxSize(this.maxPoolSize);

        return JDBCPool.pool(vertx, connectOptions, poolOptions);
    }

    @Override
    public String getUrl() {
        return "jdbc:mem:%s".formatted(this.database);
    }
}
