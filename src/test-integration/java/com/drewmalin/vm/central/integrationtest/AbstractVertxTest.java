package com.drewmalin.vm.central.integrationtest;

import com.drewmalin.vm.central.configuration.Config;
import com.drewmalin.vm.central.context.ServiceContext;
import com.drewmalin.vm.central.data.datasource.Datasource;
import com.drewmalin.vm.central.vertical.VmCentral;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for tests which depend upon vertx.
 *
 * The "@ExtendWith(VertxExtension.class)" annotation allows for the following to be injected on any test method:
 * - io.vertx.core.Vertx
 * -- this allows for hooking into the main (test!) vertx lifecycle / main verticle
 * - io.vertx.junit5.VertxTestContext
 * -- this allows for registering asynchronous test logic (assertions) within the otherwise asynchronous vertx system
 *
 * The "@TestInstance(TestInstance.Lifecycle.PER_CLASS)" annotation allows for this class' "@BeforeAll" and "@AfterAll"
 * setup and teardown methods to be only executed once per class, rather than once per test. This means that any class
 * subclassing this class will have consistent setup for its full lifecycle (the alternative being lots of duplication
 * of code performing the same setup and teardown, and the need to do so before and after ever single test method).
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractVertxTest
    extends AbstractVertxPostgresTest {

    private static final String TEST_CONFIG_FILE_PATH = "test.config.json";

    private final AtomicReference<String> deploymentId = new AtomicReference<>();

    private ConfigRetriever configRetriever;

    /**
     * Note that unlike with normal JUnit @BeforeAll methods, this method will be invoked once per class (see: the
     * class-level javadoc). This means that each test class begins with the deployment of its own VmCentralService,
     * and once all tests in a test class complete, the service will be undeployed.
     */
    @BeforeAll
    public void setup(final Vertx vertx, final VertxTestContext tc) {

        final var startupCheckpoint = tc.checkpoint();

        /*
         * Set up the configuration to read from the test file config.
         */
        final var configFile = new ConfigStoreOptions()
            .setType("file")
            .setFormat("json")
            .setConfig(
                new JsonObject().put(
                    "path", TEST_CONFIG_FILE_PATH
                )
            );

        this.configRetriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions()
            .setIncludeDefaultStores(true)
            .addStore(configFile));

        this.configRetriever.getConfig().onComplete(response -> {
            if (response.failed()) {
                /*
                 * Fail immediately if the config file is not found.
                 */
                tc.failNow(response.cause());
            }
            else {
                final var fullConfig = response.result().mapTo(Config.class);
                final var vmCentralConfig = fullConfig.vmCentral();
                final var vmCentralConfigJson = JsonObject.mapFrom(vmCentralConfig);

                /*
                 * Deploy the VmCentralService using the discovered config file
                 */
                final var options = new DeploymentOptions().setConfig(vmCentralConfigJson);

                vertx.deployVerticle(new VmCentral(), options).onComplete(tc.succeeding(id -> {
                    this.deploymentId.set(id);

                    /*
                     * Indicate that startup has completed
                     */
                    startupCheckpoint.flag();
                }));
            }
        });
    }

    @AfterAll
    public void teardown(final Vertx vertx) {
        if (this.configRetriever != null) {
            this.configRetriever.close();
        }
        if (this.deploymentId.get() != null) {
            vertx.undeploy(this.deploymentId.get());
        }
    }

    public ConfigRetriever getConfigRetriever() {
        return this.configRetriever;
    }

    public void withServiceContext(final Vertx vertx,
                                   final VertxTestContext tc,
                                   final Handler<ServiceContext> handler) {
        /*
         * Step 1: load the test config file, use 'tc.succeeding' to fail fast on errors
         */
        getConfigRetriever().getConfig().onComplete(tc.succeeding(fullConfigJson -> {

            /*
             * Step 2: with the loaded file, parse the vm central configs, emulating the similar flow in Main.java
             */
            final var fullConfig = fullConfigJson.mapTo(Config.class);
            final var vmCentralConfig = fullConfig.vmCentral();

            /*
             * Step 3: build the ServiceContext
             */
            final Datasource datasource = Datasource.builder()
                .engine(vmCentralConfig.datasourceEngine())
                .port(vmCentralConfig.datasourcePort())
                .host(vmCentralConfig.datasourceHost())
                .database(vmCentralConfig.datasourceDatabase())
                .username(vmCentralConfig.datasourceUsername())
                .password(vmCentralConfig.datasourcePassword())
                .maxPoolSize(vmCentralConfig.datasourceMaxPoolSize())
                .build();

            final var pool = datasource.newPool(vertx);

            final var serviceContext = new ServiceContext(vertx, vmCentralConfig, null, pool, null);

            /*
             * Step 4: handle test logic!
             */
            handler.handle(serviceContext);

        }));
    }

    public void withVmCentral(final Vertx vertx,
                              final VertxTestContext tc,
                              final Handler<ServiceContext> handler) {

        /*
         * Step 1: load the test config file, use 'tc.succeeding' to fail fast on errors
         */
        getConfigRetriever().getConfig().onComplete(tc.succeeding(fullConfigJson -> {

            /*
             * Step 2: with the loaded file, parse the vm central configs, emulating the similar flow in Main.java
             */
            final var fullConfig = fullConfigJson.mapTo(Config.class);
            final var vmCentralConfig = fullConfig.vmCentral();
            final var vmCentralConfigJson = JsonObject.mapFrom(vmCentralConfig);

            /*
             * Step 3: deploy the VmCentralService, along with its deployment config
             */
            final var options = new DeploymentOptions().setConfig(vmCentralConfigJson);
            vertx.deployVerticle(new VmCentral(), options, tc.succeeding(id -> {

                final Datasource datasource = Datasource.builder()
                    .engine(vmCentralConfig.datasourceEngine())
                    .port(vmCentralConfig.datasourcePort())
                    .host(vmCentralConfig.datasourceHost())
                    .database(vmCentralConfig.datasourceDatabase())
                    .username(vmCentralConfig.datasourceUsername())
                    .password(vmCentralConfig.datasourcePassword())
                    .maxPoolSize(vmCentralConfig.datasourceMaxPoolSize())
                    .build();

                final var pool = datasource.newPool(vertx);

                final var serviceContext = new ServiceContext(vertx, vmCentralConfig, null, pool, null);

                /*
                 * Step 4: handle test logic!
                 */
                handler.handle(serviceContext);

            }));

        }));

    }
}
