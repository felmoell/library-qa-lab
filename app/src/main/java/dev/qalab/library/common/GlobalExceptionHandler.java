package dev.qalab.library.common;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 9457 "problem details" responses (Content-Type application/problem+json).
 * Validation errors additionally carry an "errors" array: [{"field": "...", "message": "..."}].
 */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail handleNotFound(NotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail handleConflict(ConflictException ex) {
		return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
	}

	/** Safety net: two concurrent requests passed the service check, the DB unique constraint caught it. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
		return problem(HttpStatus.CONFLICT, "Conflict", "Request conflicts with existing data");
	}

	@ExceptionHandler(InvalidFieldException.class)
	ProblemDetail handleInvalidField(InvalidFieldException ex) {
		ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Bad Request", "Validation failed");
		body.setProperty("errors", List.of(fieldError(ex.getField(), ex.getMessage())));
		return body;
	}

	/** Request body failed Bean Validation (@Valid). */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail body = ex.getBody();
		body.setDetail("Validation failed");
		body.setProperty("errors", ex.getFieldErrors()
			.stream()
			.map(error -> fieldError(error.getField(), error.getDefaultMessage()))
			.toList());
		return handleExceptionInternal(ex, body, headers, status, request);
	}

	/** Request parameter failed validation (e.g. @Max on a query parameter). */
	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail body = ex.getBody();
		body.setDetail("Validation failed");
		body.setProperty("errors", ex.getParameterValidationResults()
			.stream()
			.flatMap(result -> result.getResolvableErrors()
				.stream()
				.map(error -> fieldError(result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
			.toList());
		return handleExceptionInternal(ex, body, headers, status, request);
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
		body.setTitle(title);
		return body;
	}

	private static Map<String, String> fieldError(String field, String message) {
		return Map.of("field", field, "message", message);
	}

}
