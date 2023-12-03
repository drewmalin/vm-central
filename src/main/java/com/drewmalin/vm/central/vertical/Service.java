package com.drewmalin.vm.central.vertical;

/**
 * A deployable service. One-to-one mapping to a Vert.x "verticle."
 */
public interface Service {

    /**
     * If true, this service will be deployed in the capacity of a background task executor. Primary events entering
     * the main process will not be directly sent to this service. Ideally it should be communicated with via the
     * main event bus.
     *
     * @return true if this service is to be deployed as a worker.
     */
    boolean isWorker();

    /**
     * The name of this service. This name will correspond to the name used in identifying this service via health
     * checks, and will be the name used to look up configurations.
     *
     * @return the {@link String} name of this worker.
     */
    String getName();
}
