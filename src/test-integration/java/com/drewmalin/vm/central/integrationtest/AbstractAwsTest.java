package com.drewmalin.vm.central.integrationtest;

import com.drewmalin.vm.central.configuration.Config;
import com.drewmalin.vm.central.iaas.aws.Aws;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractAwsTest {

    private static final String IMAGE = "localstack/localstack:3.0.1";

    private static final LocalStackContainer LOCALSTACK;

    static {
        LOCALSTACK = new LocalStackContainer(DockerImageName.parse(IMAGE))
            .withServices(
                LocalStackContainer.Service.EC2,
                LocalStackContainer.Service.SQS,
                LocalStackContainer.EnabledService.named("events")
            );
    }

    public Aws newAws() {
        final var config = new Config.CloudVmIaas.Aws(
            LOCALSTACK.getEndpoint().toString(),
            null,
            new Config.CloudVmIaas.Aws.BasicCredentials(
                LOCALSTACK.getAccessKey(),
                LOCALSTACK.getSecretKey()
            )
        );

        return new Aws(config);
    }

    @BeforeAll
    public void setup() {
        LOCALSTACK.start();
    }

    @AfterAll
    public void teardown() {
        LOCALSTACK.stop();
    }
}
