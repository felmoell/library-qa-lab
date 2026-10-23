package dev.qalab.library.book;

import dev.qalab.library.common.Isbn;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Body of POST and PUT /api/books. The upper bound of publishedYear (not in the future)
 * depends on the clock, so {@link BookService} checks it.
 */
public record BookRequest(
		@NotBlank @Isbn String isbn,
		@NotBlank @Size(max = 200) String title,
		@NotBlank @Size(max = 120) String author,
		@NotNull @Min(1450) Integer publishedYear,
		@NotNull @PositiveOrZero Integer copiesTotal) {
}
