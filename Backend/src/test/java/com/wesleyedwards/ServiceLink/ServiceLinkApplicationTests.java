package com.wesleyedwards.ServiceLink;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Verifies the Spring context (incl. JPA repositories and @Query validation)
 * boots against a real Postgres. Container lives in {@link AbstractPostgresIT};
 * the Docker gate must be declared here (@EnabledIf is not inherited).
 */
@EnabledIf("com.wesleyedwards.ServiceLink.AbstractPostgresIT#dockerAvailable")
class ServiceLinkApplicationTests extends AbstractPostgresIT {

	@Test
	void contextLoads() {
	}

}
