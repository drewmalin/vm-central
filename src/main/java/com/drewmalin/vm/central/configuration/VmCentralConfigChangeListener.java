package com.drewmalin.vm.central.configuration;

import com.drewmalin.vm.central.context.ServiceContext;
import io.vertx.config.ConfigChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class VmCentralConfigChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(VmCentralConfigChangeListener.class);

    public VmCentralConfigChangeListener(final ServiceContext context) {
        context.vertx().eventBus().consumer(ConfigEvent.CHANGED_VM_CENTRAL.getName(), configChange -> {

            final ConfigChange change = (ConfigChange) configChange.body();

            final var previousConfig = change.getPreviousConfiguration().mapTo(Config.VmCentral.class);
            final var updatedConfig = change.getNewConfiguration().mapTo(Config.VmCentral.class);

            if (previousConfig == null || updatedConfig == null) {
                LOGGER.debug("Configuration change detected but no change was found for vm.central");

                // Nothing to do
                return;
            }

            LOGGER.info("Configuration change detected for vm.central");
            onConfigurationChange(updatedConfig);
        });
    }

    protected abstract void onConfigurationChange(Config.VmCentral config);
}
