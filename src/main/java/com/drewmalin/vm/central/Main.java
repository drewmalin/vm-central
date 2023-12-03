package com.drewmalin.vm.central;

import com.drewmalin.vm.central.configuration.Config;
import com.drewmalin.vm.central.configuration.ConfigEvent;
import com.drewmalin.vm.central.utils.DurationUtils;
import com.drewmalin.vm.central.vertical.VmCentral;
import com.drewmalin.vm.central.vertical.VmCloudWorker;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.hotspot.BufferPoolsExports;
import io.prometheus.client.hotspot.ClassLoadingExports;
import io.prometheus.client.hotspot.GarbageCollectorExports;
import io.prometheus.client.hotspot.MemoryPoolsExports;
import io.prometheus.client.hotspot.StandardExports;
import io.prometheus.client.hotspot.ThreadExports;
import io.prometheus.client.hotspot.VersionInfoExports;
import io.vertx.config.ConfigChange;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application entrypoint.
 */
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) {
        /*
         * Instantiate Vertx, acquiring a handle to the configuration file. Use the configuration file to create the
         * primary Vertx instance, then deploy verticals.
         */
        final var bootstrap = Vertx.vertx();

        LOGGER.info("Startup");

        /*
         * Retrieve the default configuration, from (in order):
         *  - explicit overrides to vertx.config()
         *  - system properties
         *  - env vars
         *  - conf/config.json (or override this location with VERTX_CONFIG_PATH)
         */
        final var configRetriever = ConfigRetriever.create(bootstrap);
        configRetriever.getConfig().onComplete(configResult -> {

            LOGGER.info("Reading application configuration file");

            if (configResult.failed()) {
                LOGGER.error("Failed to read configuration");
                System.exit(1);
            }

            final var config = configResult.result().mapTo(Config.class);

            LOGGER.info("Configuration successfully loaded");

            bootstrap.close(); // Close the bootstrap Vertx, it is no longer necessary

            /*
             * Configure Vertx itself
             */
            final var vertxConfig = config.vertx();
            final var vertxOptions = new VertxOptions()
                .setWorkerPoolSize(vertxConfig.poolSize())
                .setMetricsOptions(newMetricsOptions(vertxConfig));

            /*
             * Instantiate the primary Vertx instance
             */
            final var vertx = Vertx.vertx(vertxOptions);
            configureMetrics(vertxConfig);

            /*
             * Deploy "verticles" -- long-running processes that either participate in primary events ("services") or
             * are available for background/blocking/long-running tasks ("workers").
             */
            deployVmCentralVerticle(config.vmCentral(), vertx);

            deployVmCloudWorkerVerticle(config.cloudVmWorker(), vertx);

            /*
             * Listen for changes to the configuration, firing an event on changes
             */
            ConfigRetriever.create(vertx).listen(configChange -> {

                LOGGER.info("Configuration updated");

                final var previousEntry = configChange.getPreviousConfiguration().mapTo(Config.class);
                final var newEntry = configChange.getNewConfiguration().mapTo(Config.class);

                if (previousEntry == null || newEntry == null || previousEntry.isEmpty() || newEntry.isEmpty()) {
                    return;
                }

                if (!previousEntry.vertx().equals(newEntry.vertx())) {
                    final var message = new ConfigChange(
                        JsonObject.mapFrom(previousEntry.vertx()),
                        JsonObject.mapFrom(newEntry.vertx())
                    );
                    vertx.eventBus().publish(ConfigEvent.CHANGED_VERTX.getName(), message);
                }
                if (!previousEntry.vmCentral().equals(newEntry.vmCentral())) {
                    final var message = new ConfigChange(
                        JsonObject.mapFrom(previousEntry.vmCentral()),
                        JsonObject.mapFrom(newEntry.vmCentral())
                    );
                    vertx.eventBus().publish(ConfigEvent.CHANGED_VM_CENTRAL.getName(), message);
                }
                if (!previousEntry.cloudVmWorker().equals(newEntry.cloudVmWorker())) {
                    final var message = new ConfigChange(
                        JsonObject.mapFrom(previousEntry.cloudVmWorker()),
                        JsonObject.mapFrom(newEntry.cloudVmWorker())
                    );
                    vertx.eventBus().publish(ConfigEvent.CHANGED_VM_CLOUD_WORKER.getName(), message);
                }
            });

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Beginning shutdown");

                vertx.close().onFailure(t -> {
                    LOGGER.error("Failure during shutdown", t);
                });

                final var duration = 2000;
                LOGGER.info("Waiting for %d milliseconds to allow shutdown to complete".formatted(duration));

                /*
                 * Busy-wait the shutdown thread while the close() command percolates to all subsystems -- we are now
                 * outside the Vert.x threading system and in our own thread, so we need to manually await the
                 * completion of the vertx.close() future.
                 */
                try {
                    Thread.sleep(duration);
                }
                catch (final InterruptedException ignored) {
                }
                LOGGER.info("Waited %d milliseconds, shutting down now".formatted(duration));

            }, "main-shutdown-handler"));

        });
    }

    private static void deployVmCentralVerticle(final Config.VmCentral config, final Vertx vertx) {

        final var name = VmCentral.class.getName();

        LOGGER.info("Deploying service: '%s'".formatted(name));

        final var options = new DeploymentOptions()
            .setConfig(JsonObject.mapFrom(config))
            .setThreadingModel(ThreadingModel.EVENT_LOOP);

        deploy(name, options, vertx);
    }

    private static void deployVmCloudWorkerVerticle(final Config.CloudVmWorker config, final Vertx vertx) {

        final var name = VmCloudWorker.class.getName();

        LOGGER.info("Deploying worker: '%s'".formatted(name));

        var options = new DeploymentOptions()
            .setConfig(JsonObject.mapFrom(config))
            .setThreadingModel(ThreadingModel.WORKER);

        if (StringUtils.isNotBlank(config.vertxWorkerPoolName())) {
            options = options.setWorkerPoolName(config.vertxWorkerPoolName());
        }
        if (config.vertxWorkerPoolSize() != 0) {
            options = options.setWorkerPoolSize(config.vertxWorkerPoolSize());
        }
        if (config.vertxInstanceCount() != 0) {
            options = options.setInstances(config.vertxInstanceCount());
        }

        deploy(name, options, vertx);
    }

    private static void deploy(final String className, final DeploymentOptions options, final Vertx vertx) {

        final var start = System.currentTimeMillis();
        vertx.deployVerticle(className, options)

            .onSuccess(s -> {
                final var duration = DurationUtils.toString(start);
                LOGGER.info("'%s' startup complete in %s".formatted(className, duration));
            })

            .onFailure(t -> {
                LOGGER.error("Failed to deploy '%s'".formatted(className));
                LOGGER.error(t.getMessage());
            });
    }

    private static MetricsOptions newMetricsOptions(final Config.Vertx vertxConfig) {
        if (!vertxConfig.prometheusEnabled()) {
            return new MetricsOptions();
        }

        final var prometheusOptions = new VertxPrometheusOptions()
            .setEnabled(true);

        return new MicrometerMetricsOptions()
            .setEnabled(true)
            .setPrometheusOptions(prometheusOptions);
    }

    private static void configureMetrics(final Config.Vertx vertxConfig) {

        if (!vertxConfig.prometheusEnabled()) {
            LOGGER.info("Configuration indicated that Prometheus metrics should be disabled");
            return;
        }

        LOGGER.info("Configuring Prometheus metrics");

        final var prometheus = ((PrometheusMeterRegistry) BackendRegistries.getDefaultNow());
        prometheus.config()
            .meterFilter(new MeterFilter() {
                @Override
                public DistributionStatisticConfig configure(final Meter.Id id,
                                                             final DistributionStatisticConfig config) {
                    return DistributionStatisticConfig.builder()
                        .percentilesHistogram(true)
                        .build()
                        .merge(config);
                }
            })
        ;

        final var meterRegistry = prometheus.getPrometheusRegistry();

        (new StandardExports()).register(meterRegistry);
        (new MemoryPoolsExports()).register(meterRegistry);
        (new BufferPoolsExports()).register(meterRegistry);
        (new GarbageCollectorExports()).register(meterRegistry);
        (new ThreadExports()).register(meterRegistry);
        (new ClassLoadingExports()).register(meterRegistry);
        (new VersionInfoExports()).register(meterRegistry);
    }
}
