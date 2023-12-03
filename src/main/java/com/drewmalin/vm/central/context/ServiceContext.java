package com.drewmalin.vm.central.context;

import com.drewmalin.vm.central.configuration.Config;
import com.drewmalin.vm.central.http.auth.AuthProvider;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.Pool;

public record ServiceContext(
    Vertx vertx,
    Config.VmCentral config,
    Router router,
    Pool sqlPool,
    AuthProvider auth
) {

}
