package dev.qalab.library.common;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Level: pure unit test. No Spring, no mocks, milliseconds to run.
 * Technique: equivalence classes + boundary values, expressed as parameterized tests.
 */
@Epic("Catalog")
@Feature("ISBN validation")
@DisplayName("ISBN-13 validator")
class IsbnValidatorTest {

	@Nested
	@DisplayName("accepts")
	class Accepts {

		@ParameterizedTest(name = "{1}: \"{0}\"")
		@CsvSource({
				"9783161484100,         plain digits",
				"978-3-16-148410-0,     hyphenated",
				"'978 3 16 148410 0',   separated by spaces",
				"9791090636071,         979 prefix",
				"9780141439471,         check digit 1",
				"9780201633610,         check digit 0"
		})
		@Tag("smoke")
		@Severity(SeverityLevel.CRITICAL)
		void validIsbn(String isbn, String description) {
			assertThat(IsbnValidator.isValidIsbn13(isbn)).isTrue();
		}

		@Test
		@DisplayName("null as 'not my job' (@NotBlank handles missing values)")
		void nullIsLeftToNotBlank() {
			assertThat(new IsbnValidator().isValid(null, null)).isTrue();
		}

	}

	@Nested
	@DisplayName("rejects")
	class Rejects {

		@ParameterizedTest(name = "{1}: \"{0}\"")
		@CsvSource({
				"9783161484101,    wrong check digit",
				"978316148410,     12 digits (lower length boundary - 1)",
				"97831614841000,   14 digits (upper length boundary + 1)",
				"978316148410X,    letter instead of check digit",
				"9773161484101,    prefix 977 with valid checksum",
				"9803161484105,    prefix 980 with valid checksum",
				"3161484100,       ISBN-10 (not supported)",
				"978_3_16_148410_0, unsupported separator"
		})
		@Severity(SeverityLevel.CRITICAL)
		void invalidIsbn(String isbn, String description) {
			assertThat(IsbnValidator.isValidIsbn13(isbn)).isFalse();
		}

		@ParameterizedTest(name = "\"{0}\"")
		@NullAndEmptySource
		@ValueSource(strings = { "   ", "---" })
		void missingValue(String isbn) {
			assertThat(IsbnValidator.isValidIsbn13(isbn)).isFalse();
		}

	}

	@Test
	@DisplayName("normalize() strips hyphens and spaces")
	void normalizeStripsSeparators() {
		assertThat(IsbnValidator.normalize(" 978-3-16 148410-0 ")).isEqualTo("9783161484100");
	}

}
