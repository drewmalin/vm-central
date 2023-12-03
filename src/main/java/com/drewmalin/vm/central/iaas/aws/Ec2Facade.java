package com.drewmalin.vm.central.iaas.aws;

import com.drewmalin.vm.central.configuration.Config;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.net.URI;

public class Ec2Facade {

    private final Ec2Client client;

    public Ec2Facade(final Config.CloudVmIaas config) {

        final var clientBuilder = Ec2Client.builder();

        if (config.aws() != null && config.aws().endpoint() != null) {
            clientBuilder.endpointOverride(URI.create(config.aws().endpoint()));
        }

        this.client = clientBuilder
            .region(Region.US_WEST_2)
            .build();
    }

}
