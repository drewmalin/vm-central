package com.drewmalin.vm.central.http.router;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;

public abstract class RequestRouter {

    private final Router router;
    private final Vertx vertx;
    private final SchemaParser schemaParser;

    RequestRouter(final Router router, final Vertx vertx) {
        this.router = router;
        this.vertx = vertx;
        this.schemaParser = SchemaParser.createDraft7SchemaParser(
            SchemaRouter.create(this.vertx, new SchemaRouterOptions())
        );
    }

    Router getRouter() {
        return this.router;
    }

    Vertx getVertx() {
        return this.vertx;
    }

    SchemaParser getSchemaParser() {
        return this.schemaParser;
    }

    public abstract void mount();
}
