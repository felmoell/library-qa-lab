package dev.qalab.library.common;

/**
 * A business rule rejected a single field (e.g. a publication year in the future).
 * Mapped to HTTP 400 with the same "errors" format as Bean Validation failures.
 */
public class InvalidFieldException extends RuntimeException {

	private final String field;

	public InvalidFieldException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}

}
