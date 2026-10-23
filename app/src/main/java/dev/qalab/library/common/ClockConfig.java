package dev.qalab.library.common;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * All "now" lookups go through this bean instead of {@code LocalDate.now()},
 * so tests can replace it with {@code Clock.fixed(...)} and control time.
 */
@Configuration
class ClockConfig {

	@Bean
	Clock clock() {
		return Clock.systemDefaultZone();
	}

}
