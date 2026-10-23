package dev.qalab.library.support;

import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;

/**
 * A throwaway PostgreSQL in Docker for tests that need the database: {@code @Import(PostgresTestcontainer.class)}.
 * {@code @ServiceConnection} points the DataSource at the container, so no URL or password has to be configured.
 * Spring starts the container with the test context and stops it when the context closes; tests sharing the
 * same context configuration share one container. Same major version as the local and CI database.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainer {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgres() {
		return new PostgreSQLContainer("postgres:18-alpine");
	}

}
