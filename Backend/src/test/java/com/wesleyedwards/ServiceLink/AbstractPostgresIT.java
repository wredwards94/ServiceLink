package com.wesleyedwards.ServiceLink;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for full-context integration tests. Owns a single Testcontainers
 * Postgres shared across all subclasses (started once per JVM), wired to Spring
 * Boot via {@link ServiceConnection}, so no {@code spring.datasource.*} config is
 * needed.
 *
 * <p>Docker gating is NOT inherited: {@code @EnabledIf} is not {@code @Inherited},
 * so each concrete subclass must carry
 * {@code @EnabledIf("com.wesleyedwards.ServiceLink.AbstractPostgresIT#dockerAvailable")}
 * itself, or it will error (rather than skip) on a machine without Docker. CI
 * runners have Docker, so ITs run there.
 */
@SpringBootTest
@Testcontainers
public abstract class AbstractPostgresIT {

    public static boolean dockerAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
}
