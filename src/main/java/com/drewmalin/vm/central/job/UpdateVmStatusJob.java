package com.drewmalin.vm.central.job;

import com.drewmalin.vm.central.configuration.VmCentralConfigChangeListener;
import com.drewmalin.vm.central.context.ServiceContext;
import com.drewmalin.vm.central.data.model.VmDTO;
import com.drewmalin.vm.central.security.Principal;
import com.drewmalin.vm.central.security.System;
import com.drewmalin.vm.central.task.BulkUpdateVmsTask;
import com.drewmalin.vm.central.task.Tasks;
import com.drewmalin.vm.central.configuration.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class UpdateVmStatusJob
    extends VmCentralConfigChangeListener
    implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateVmStatusJob.class);

    private static final Principal PRINCIPAL = System.BACKGROUND_JOB;

    private final ServiceContext context;

    /*
     * Possible vertx bug: in order to cancel this job (in case the configuration change instructs us to do so) we must
     * keep a handle to the ID of the internal "timer" object represented by the scheduled task. However, there is no
     * safe "default handle value" (very specifically: by setting the default to '0', we have a chance of referencing
     * background timer objects created by the vertx framework!).
     *
     * Allowing this value to be null-able (i.e. by being a Long instead of a long) allows us to represent a state of
     * "unset" without using a valid long value. This means that all accessors must check for its null-ness, but it also
     * means that all works normally with the hidden vertx machinery.
     */
    private Long jobHandle;

    private UpdateVmStatusJob(final ServiceContext context) {
        super(context);

        this.context = context;
    }

    public static Job create(final ServiceContext ctx) {
        return new UpdateVmStatusJob(ctx);
    }

    @Override
    public void schedule() {
        final var config = this.context.config();

        schedule(config.jobVmUpdateStatus());
    }

    @Override
    protected void onConfigurationChange(final Config.VmCentral config) {
        if (this.jobHandle != null) {
            // Keep a null-able jobHandle in order to avoid canceling a worker that we do not own
            this.context.vertx().cancelTimer(this.jobHandle);
            this.jobHandle = null;
        }

        schedule(config.jobVmUpdateStatus());
    }

    private void schedule(final Config.VmCentral.JobVmUpdateStatus config) {
        if (config.disabled()) {
            // nothing to do
            return;
        }

        final var handle = this.context.vertx().setPeriodic(config.periodMillis(), id -> {

            LOGGER.debug("%s is doing work".formatted(getClass().getSimpleName()));

            Tasks.getAllVms(this.context).submit(PRINCIPAL).compose(vms -> {

                final List<VmDTO> vmsToUpdate = new ArrayList<>();

                try {
                    // simulate background work (todo: go to aws!)
                    Thread.sleep(1_000);
                }
                catch (final InterruptedException e) {
                }

                for (final var vm : vms) {
                    if (vm.vmStatus() != VmDTO.Status.UP) {
                        vmsToUpdate.add(
                            VmDTO.builder(vm)
                                .status(VmDTO.Status.UP)
                                .build()
                        );
                    }
                }
                return Tasks.bulkUpdateVms(new BulkUpdateVmsTask.Input(vmsToUpdate), this.context)
                    .submit(PRINCIPAL);
            });
        });

        if (this.jobHandle == null) {
            this.jobHandle = handle;
        }
        else {
            throw new IllegalStateException("Expected null job handle but found: %d".formatted(this.jobHandle));
        }
    }
}
