package com.wesleyedwards.ServiceLink;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
// Skips this test when Docker isn't usable (e.g. locally without a working
// Docker daemon). CI runners have Docker, so it runs there. Evaluated before the
// Testcontainers extension, so it skips cleanly instead of erroring.
@EnabledIf("dockerAvailable")
class ServiceLinkApplicationTests {

	static boolean dockerAvailable() {
		return DockerClientFactory.instance().isDockerAvailable();
	}

	// Started once for this class and stopped afterward. @ServiceConnection hands
	// the container's JDBC URL/username/password to Spring Boot automatically, so
	// no spring.datasource.* config is needed for tests.
	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

	@Test
	void contextLoads() {
	}

}
