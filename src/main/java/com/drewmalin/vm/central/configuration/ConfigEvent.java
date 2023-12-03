package com.drewmalin.vm.central.configuration;

public enum ConfigEvent {

    CHANGED_VERTX("configuration.changed.vertx"),
    CHANGED_VM_CENTRAL("configuration.changed.vm.central"),
    CHANGED_VM_CLOUD_WORKER("configuration.changed.vm.cloud.worker"),
    ;

    private final String name;

    ConfigEvent(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
