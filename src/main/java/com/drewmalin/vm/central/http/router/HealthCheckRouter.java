package com.drewmalin.vm.central.http.router;

import com.drewmalin.vm.central.context.ServiceContext;
import com.drewmalin.vm.central.vertical.VmCloudWorker;
import io.vertx.core.Vertx;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthCheckRouter
    extends RequestRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckRouter.class);

    private static final String HTTP_HEALTH_PATH = "/health";

    public static final String TARGET_VM_DATASOURCE = "vm.datasource";
    public static final String TARGET_VM_CLOUD_WORKER = "vm.cloud.worker";

    private final HealthCheckHandler handler;
    private final Pool sqlPool;

    private HealthCheckRouter(final Pool sqlPool, final Router router, final Vertx vertx) {
        super(router, vertx);

        this.sqlPool = sqlPool;
        this.handler = HealthCheckHandler.create(vertx);

        datasourceHealthCheck(this.handler);
        cloudWorkerHealthCheck(this.handler, vertx);
    }

    public static HealthCheckRouter create(ServiceContext ctx) {
        return new HealthCheckRouter(ctx.sqlPool(), ctx.router(), ctx.vertx());
    }

    /*
     * Datasource health checks succeed simply if a connection to the database is possible.
     */
    private void datasourceHealthCheck(final HealthCheckHandler handler) {
        handler.register(TARGET_VM_DATASOURCE,
            healthCheck -> this.sqlPool.getConnection(connection -> {
                if (connection.succeeded()) {
                    healthCheck.complete(Status.OK());
                }
                else {
                    connection.result().close();
                    healthCheck.fail(connection.cause());
                }
            })
        );
    }

    /*
     * Submit a health check "ping" to the cloud worker service.
     */
    private void cloudWorkerHealthCheck(final HealthCheckHandler handler, final Vertx vertx) {
        handler.register(TARGET_VM_CLOUD_WORKER, healthCheck ->
            vertx.eventBus().request(VmCloudWorker.EVENT_HEALTHCHECK, "ping")
                .onSuccess(response -> {
                    healthCheck.complete(Status.OK());
                })
                .onFailure(err -> {
                    healthCheck.complete(Status.KO());
                })
        );
    }

    @Override
    public void mount() {
        LOGGER.info("Mounting new HTTP route: '%s'".formatted(HTTP_HEALTH_PATH));

        getRouter().get(HTTP_HEALTH_PATH).handler(this.handler);
    }
}
