package com.drewmalin.vm.central.integrationtest;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * An extension of {@link AbstractVertxTest} which adds a Postgres test container.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(VertxExtension.class)
public class AbstractVertxPostgresTest {

    private static final String IMAGE = "postgres:16.1-alpine3.18";
    private static final String DATASOURCE_DATABASE = "vmcentral";
    private static final String DATASOURCE_USERNAME = "vmcentral";
    private static final String DATASOURCE_PASSWORD = "password";
    private static final int CONTAINER_PORT = 5432; // fixed
    private static final int LOCAL_PORT = 9877;

    private static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse(IMAGE))
            .withDatabaseName(DATASOURCE_DATABASE)
            .withUsername(DATASOURCE_USERNAME)
            .withPassword(DATASOURCE_PASSWORD)
            .withExposedPorts(CONTAINER_PORT)
            .withCreateContainerCmdModifier(cmd ->
                cmd.withHostConfig(
                    new HostConfig().withPortBindings(
                        new PortBinding(
                            Ports.Binding.bindPort(LOCAL_PORT), new ExposedPort(CONTAINER_PORT)
                        )
                    )
                )
            );
    }

    @BeforeAll
    public void setup() {
        POSTGRES.start();
    }

    @AfterAll
    public void teardown() {
        POSTGRES.stop();
    }
}
