package dev.qalab.library.common;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * ISBN-13 rules:
 * <ul>
 * <li>13 digits after removing hyphens and spaces</li>
 * <li>prefix 978 or 979</li>
 * <li>checksum: digits weighted 1,3,1,3,... must sum to a multiple of 10</li>
 * </ul>
 */
public class IsbnValidator implements ConstraintValidator<Isbn, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return value == null || isValidIsbn13(value);
	}

	public static boolean isValidIsbn13(String value) {
		if (value == null) {
			return false;
		}
		String digits = normalize(value);
		if (!digits.matches("\\d{13}")) {
			return false;
		}
		if (!digits.startsWith("978") && !digits.startsWith("979")) {
			return false;
		}
		int sum = 0;
		for (int i = 0; i < 13; i++) {
			int digit = digits.charAt(i) - '0';
			sum += (i % 2 == 0) ? digit : digit * 3;
		}
		return sum % 10 == 0;
	}

	/** Removes hyphens and spaces, e.g. "978-3-16-148410-0" becomes "9783161484100". */
	public static String normalize(String value) {
		return value.replaceAll("[\\s-]", "");
	}

}
