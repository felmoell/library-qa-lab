package dev.qalab.library.common;

/** Mapped to HTTP 404 by {@link GlobalExceptionHandler}. */
public class NotFoundException extends RuntimeException {

	public NotFoundException(String resource, Object id) {
		super(resource + " with id " + id + " not found");
	}

}
