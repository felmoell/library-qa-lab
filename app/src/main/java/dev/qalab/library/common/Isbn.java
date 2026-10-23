package dev.qalab.library.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Bean Validation constraint: the value must be a valid ISBN-13 (hyphens and spaces allowed).
 * {@code null} is considered valid, combine with {@code @NotBlank} to make it mandatory.
 */
@Documented
@Constraint(validatedBy = IsbnValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface Isbn {

	String message() default "must be a valid ISBN-13";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
