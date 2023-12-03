package com.drewmalin.vm.central.http.router;

import com.drewmalin.vm.central.context.ServiceContext;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.micrometer.PrometheusScrapingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricsRouter
    extends RequestRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsRouter.class);

    private static final String HTTP_METRICS_PATH = "/metrics";

    private MetricsRouter(final Router router, final Vertx vertx) {
        super(router, vertx);
    }

    public static MetricsRouter create(ServiceContext ctx) {
        return new MetricsRouter(ctx.router(), ctx.vertx());
    }

    @Override
    public void mount() {
        LOGGER.info("Mounting new HTTP route: '%s'".formatted(HTTP_METRICS_PATH));

        getRouter().route(HTTP_METRICS_PATH).handler(PrometheusScrapingHandler.create());
    }
}
