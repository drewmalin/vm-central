package com.drewmalin.vm.central.iaas.aws;

import com.drewmalin.vm.central.configuration.Config;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Aws {

    private final Config.CloudVmIaas.Aws config;
    private final Map<Service, Map<Region, AwsClient>> clients;

    public Aws(final Config.CloudVmIaas.Aws config) {
        this.config = config;
        this.clients = new HashMap<>();
    }

    public Ec2Client ec2(final Region region) {
        return (Ec2Client) getClient(Service.EC2, region);
    }

    public SqsClient sqs(final Region region) {
        return (SqsClient) getClient(Service.SQS, region);
    }

    public EventBridgeClient eventBridge(final Region region) {
        return (EventBridgeClient) getClient(Service.EVENT_BRIDGE, region);
    }

    private AwsClient getClient(final Service service, final Region region) {
        if (!this.clients.containsKey(service)) {
            /*
             * A service of this type has not yet been created
             */
            this.clients.put(service, new HashMap<>());
        }

        final var serviceClients = this.clients.get(service);
        if (serviceClients.containsKey(region)) {
            /*
             * The service/region combination has already been created -- return and exit
             */
            return serviceClients.get(region);
        }

        /*
         * This service/region combination has not yet been created
         */
        final var builder = service.clientBuilder()
            .region(region);

        final var endpoint = this.config.endpoint();
        if (!StringUtils.isBlank(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }

        /*
         * Try ProfileCredentials
         */
        if (this.config.profileCredentials() != null) {
            final var profile = this.config.profileCredentials().profile();
            if (StringUtils.isBlank(profile)) {
                throw new IllegalArgumentException("Empty 'profile' for profileCredentials");
            }

            final var credentialsProvider = ProfileCredentialsProvider.create(profile);
            builder.credentialsProvider(credentialsProvider);
        }
        /*
         * Try BasicCredentials
         */
        else if (this.config.basicCredentials() != null) {
            final var accessKeyId = this.config.basicCredentials().accessKeyId();
            final var secretAccessKey = this.config.basicCredentials().secretAccessKey();
            if (StringUtils.isBlank(accessKeyId) || StringUtils.isBlank(secretAccessKey)) {
                throw new IllegalArgumentException("Empty 'accessKeyId' and/or 'secretAccessKey' for basicCredentials");
            }

            final var credentialsProvider = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
            builder.credentialsProvider(StaticCredentialsProvider.create(credentialsProvider));
        }

        final var awsClient = (AwsClient) builder.build();

        /*
         * Cache the client for this type and region
         */
        this.clients.get(service).put(region, awsClient);

        return awsClient;
    }

    private enum Service {
        EC2(Ec2Client::builder),
        SQS(SqsClient::builder),
        EVENT_BRIDGE(EventBridgeClient::builder),
        ;

        private final Supplier<AwsClientBuilder<?, ?>> builderSupplier;

        Service(final Supplier<AwsClientBuilder<?, ?>> builderSupplier) {
            this.builderSupplier = builderSupplier;
        }

        AwsClientBuilder<?, ?> clientBuilder() {
            return this.builderSupplier.get();
        }
    }
}
