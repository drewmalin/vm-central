package com.drewmalin.vm.central.vertical;

import com.drewmalin.vm.central.context.ServiceContext;
import com.drewmalin.vm.central.data.datasource.Datasource;
import com.drewmalin.vm.central.data.model.UserDTO;
import com.drewmalin.vm.central.data.model.VmDTO;
import com.drewmalin.vm.central.event.Codec;
import com.drewmalin.vm.central.http.router.AuthRouter;
import com.drewmalin.vm.central.http.router.HealthCheckRouter;
import com.drewmalin.vm.central.http.router.MetricsRouter;
import com.drewmalin.vm.central.http.utils.ResponseUtils;
import com.drewmalin.vm.central.job.UpdateVmStatusJob;
import com.drewmalin.vm.central.security.Role;
import com.drewmalin.vm.central.security.System;
import com.drewmalin.vm.central.task.CreateUserTask;
import com.drewmalin.vm.central.task.Tasks;
import com.drewmalin.vm.central.configuration.Config;
import com.drewmalin.vm.central.http.auth.AuthProvider;
import com.drewmalin.vm.central.http.router.UsersRouter;
import com.drewmalin.vm.central.http.router.VirtualMachinesRouter;
import com.drewmalin.vm.central.utils.DurationUtils;
import io.vertx.config.ConfigChange;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

public class VmCentral
    extends AbstractVerticle
    implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(VmCentral.class);

    public static final String NAME = "vm.central";

    private AtomicReference<ServiceContext> serviceContext = new AtomicReference<>();

    @Override
    public void start(final Promise<Void> promise) {
        final var start = java.lang.System.currentTimeMillis();
        LOGGER.info("Beginning startup");

        newServiceContext().onSuccess(ctx -> {

            startBackgroundJobs(ctx);

            mountHttpRoutes(ctx);

            final var port = ctx.config().httpPort();
            ctx.vertx().createHttpServer()
                .requestHandler(ctx.router())
                .listen(port, http -> {
                    if (http.succeeded()) {
                        final var duration = DurationUtils.toString(start);
                        LOGGER.info("Service startup completed successfully in %s".formatted(duration));
                        promise.complete();
                    }
                    else {
                        LOGGER.error("Service startup failed", http.cause());
                        promise.fail(http.cause());
                    }
                });

            this.serviceContext.set(ctx);
        });
    }

    private void startBackgroundJobs(final ServiceContext ctx) {
        /*
         * Schedule VM status checks
         */
        UpdateVmStatusJob.create(ctx).schedule();
    }

    private void mountHttpRoutes(final ServiceContext ctx) {
        /*
         * Poll-able metrics
         */
        MetricsRouter.create(ctx).mount();

        /*
         * Health checks / pings
         */
        HealthCheckRouter.create(ctx).mount();

        /*
         * "/token*"
         */
        AuthRouter.create(ctx).mount();

        /*
         * "/users*"
         */
        UsersRouter.create(ctx).mount();

        /*
         * "/vms*"
         */
        VirtualMachinesRouter.create(ctx).mount();

        /*
         * Error handling
         */
        mountRouteErrorHandlers(ctx.router());
    }

    private void mountRouteErrorHandlers(final Router router) {
        LOGGER.info("Mounting new HTTP error catchall route");

        /*
         * Catch-all router as the final safety net for all failures and thrown exceptions
         */
        router.route("/*").failureHandler(ctx -> {
                ResponseUtils.error(ctx.failure(), ctx);
            }
        );
    }

    private Future<ServiceContext> newServiceContext() {
        final var vertx = getVertx();
        final var config = config().mapTo(Config.VmCentral.class);
        final var httpRouter = Router.router(vertx);

        // Prepare the event bus with any custom message codecs
        prepareEventBusCodecs();

        // Await the pool's completion as migrations must occur

        final AtomicReference<Pool> poolReference = new AtomicReference<>();

        return newSqlPool(config)
            .compose(pool -> {
                poolReference.set(pool);
                return newAuthProvider(pool, config);
            })
            .map(authProvider -> {
                return new ServiceContext(
                    vertx,
                    config,
                    httpRouter,
                    poolReference.get(),
                    authProvider
                );
            });
    }

    private void prepareEventBusCodecs() {
        final var eb = getVertx().eventBus();
        eb.registerDefaultCodec(
            ConfigChange.class,
            new Codec<>(ConfigChange.class)
        );
        eb.registerDefaultCodec(
            UserDTO.class,
            new Codec<>(UserDTO.class)
        );
        eb.registerDefaultCodec(
            VmDTO.class,
            new Codec<>(VmDTO.class)
        );
        eb.registerDefaultCodec(
            Config.VmCentral.class,
            new Codec<>(Config.VmCentral.class)
        );
        eb.registerDefaultCodec(
            Config.Vertx.class,
            new Codec<>(Config.Vertx.class)
        );
        eb.registerDefaultCodec(
            Config.CloudVmWorker.class,
            new Codec<>(Config.CloudVmWorker.class)
        );
    }

    private Future<AuthProvider> newAuthProvider(final Pool sqlPool, final Config.VmCentral config) {
        LOGGER.info("Creating auth provider");

        final var authProvider = new AuthProvider(sqlPool, config, getVertx());

        final var input = new CreateUserTask.Input(
            // TODO: pull from config / environment
            "admin",
            // TODO: pull from config / environment
            "vmcentral",
            "admin",
            "admin",
            Role.ADMIN,
            false // discard duplication as this will regularly occur on restart
        );

        return Tasks.createUser(input, sqlPool, getVertx())
            .submit(System.ROOT)
            .map(admin -> {
                LOGGER.info("System admin user '%s' created".formatted(input.username()));
                return authProvider;
            })
            .onFailure(t -> {
                LOGGER.error("Error creating admin user!");
                LOGGER.error(t.getMessage());
            });
    }

    private Future<Pool> newSqlPool(final Config.VmCentral config) {
        LOGGER.info("Creating SQL connection pool");

        final Datasource datasource = Datasource.builder()
            .engine(config.datasourceEngine())
            .port(config.datasourcePort())
            .host(config.datasourceHost())
            .database(config.datasourceDatabase())
            .username(config.datasourceUsername())
            .password(config.datasourcePassword())
            .maxPoolSize(config.datasourceMaxPoolSize())
            .build();

        return Future.future(handler -> {
            datasource.performMigrations();

            final var pool = datasource.newPool(getVertx());
            handler.complete(pool);
        });
    }

    @Override
    public boolean isWorker() {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void stop() {
        LOGGER.info("Beginning shutdown");

        // Be careful with nulls here as we don't necessarily know why we are stopping
        final var ctx = this.serviceContext.get();
        if (ctx == null) {
            return;
        }

        // Shut down all connections in the sql pool
        final var sqlPool = ctx.sqlPool();
        if (sqlPool == null) {
            return;
        }
        sqlPool.close(ar -> {
            LOGGER.info("SQL Pool shut down");
        });
    }
}
