package dev.qalab.library.testsupport;

import org.flywaydb.core.Flyway;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Testability hook, only active with the "test-support" profile (never in production):
 * POST /api/test/reset drops all data and re-runs the Flyway migrations, so the seed data is back
 * and ids start at 1 again. Useful before a Postman or JMeter run that needs a known state.
 */
@RestController
@RequestMapping("/api/test")
@Profile("test-support")
class TestSupportController {

	private final Flyway flyway;

	TestSupportController(Flyway flyway) {
		this.flyway = flyway;
	}

	@PostMapping("/reset")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void reset() {
		flyway.clean();
		flyway.migrate();
	}

}
