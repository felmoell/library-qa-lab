package dev.qalab.library.support;

import java.util.concurrent.ThreadLocalRandom;

/** Generates random but valid ISBN-13s, so tests never collide with seed data or each other. */
public final class TestIsbn {

	private TestIsbn() {
	}

	public static String random() {
		StringBuilder digits = new StringBuilder("978");
		for (int i = 0; i < 9; i++) {
			digits.append(ThreadLocalRandom.current().nextInt(10));
		}
		return digits.append(checkDigit(digits)).toString();
	}

	static int checkDigit(CharSequence first12) {
		int sum = 0;
		for (int i = 0; i < 12; i++) {
			int digit = first12.charAt(i) - '0';
			sum += (i % 2 == 0) ? digit : digit * 3;
		}
		return (10 - sum % 10) % 10;
	}

}
