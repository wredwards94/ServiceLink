package com.wesleyedwards.ServiceLink;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for full-context integration tests. Owns a single Testcontainers
 * Postgres shared across all subclasses (started once per JVM), wired to Spring
 * Boot via {@link ServiceConnection}, so no {@code spring.datasource.*} config is
 * needed.
 *
 * <p>The container is deliberately started by the static initializer below rather
 * than by {@code @Testcontainers}/{@code @Container}. That extension stops a static
 * container when the class that started it finishes, and because this field is
 * shared by every subclass, the second IT class to run would connect to a dead port
 * ("Connection to localhost:NNNNN refused"). This is the singleton-container
 * pattern; Ryuk still reaps the container when the JVM exits.
 *
 * <p>Docker gating is NOT inherited: {@code @EnabledIf} is not {@code @Inherited},
 * so each concrete subclass must carry
 * {@code @EnabledIf("com.wesleyedwards.ServiceLink.AbstractPostgresIT#dockerAvailable")}
 * itself, or it will error (rather than skip) on a machine without Docker. CI
 * runners have Docker, so ITs run there.
 */
@SpringBootTest
public abstract class AbstractPostgresIT {

    public static boolean dockerAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    static {
        // Guarded: without Docker the subclasses are skipped by @EnabledIf, but this
        // initializer still runs when the class loads, and start() would fail hard.
        if (dockerAvailable()) postgres.start();
    }
}
