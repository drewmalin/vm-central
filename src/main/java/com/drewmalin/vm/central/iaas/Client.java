package com.drewmalin.vm.central.iaas;

import com.drewmalin.vm.central.configuration.Config;
import com.drewmalin.vm.central.iaas.aws.Aws;

import java.util.concurrent.atomic.AtomicReference;

public class Client {

    private final Config.CloudVmIaas config;
    private final AtomicReference<Aws> aws;

    public Client(final Config.CloudVmIaas config) {
        this.config = config;
        this.aws = new AtomicReference<>();
    }

    public Aws aws() {
        this.aws.compareAndSet(null, new Aws(this.config.aws()));
        return this.aws.get();
    }
}
