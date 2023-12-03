package com.drewmalin.vm.central.vertical;

import com.drewmalin.vm.central.data.model.VmDTO;
import com.drewmalin.vm.central.utils.DurationUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VmCloudWorker
    extends AbstractVerticle
    implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(VmCloudWorker.class);

    public static final String EVENT_HEALTHCHECK = "vm.cloud.worker.healthcheck";
    public static final String EVENT_VM_CREATE = "vm.cloud.worker.vm.create";

    public static final String NAME = "vm.cloud.worker";

    @Override
    public void start() {
        final var start = java.lang.System.currentTimeMillis();
        LOGGER.info("Beginning startup");

        final var bus = getVertx().eventBus();

        startEventHandlers(bus);

        final var duration = DurationUtils.toString(start);
        LOGGER.info("Service startup complete in %s".formatted(duration));
    }

    private void startEventHandlers(final EventBus bus) {
        handleHealthCheck(bus);
        handleVmCreate(bus);
    }

    private void handleVmCreate(final EventBus bus) {
        LOGGER.info("Listening for '%s' events".formatted(EVENT_VM_CREATE));

        bus.consumer(EVENT_VM_CREATE, requestMessage -> {

            if (!(requestMessage.body() instanceof final VmDTO vm)) {
                requestMessage.fail(400, "Payload was not of type JsonObject");
                return;
            }

            try {
                // simulate background work
                Thread.sleep(3_000);
            }
            catch (final InterruptedException e) {
                requestMessage.fail(500, e.getMessage());
            }

            final var updatedVm = VmDTO.builder(vm)
                .status(VmDTO.Status.UP)
                .build();

            requestMessage.reply(updatedVm);
        });
    }

    private void handleHealthCheck(final EventBus bus) {
        LOGGER.info("Listening for '%s' events".formatted(EVENT_HEALTHCHECK));

        bus.consumer(EVENT_HEALTHCHECK, event -> {
            event.reply("pong");
        });
    }

    @Override
    public boolean isWorker() {
        return true;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void stop() {
        LOGGER.info("Beginning shutdown");
    }
}
