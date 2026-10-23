package dev.qalab.library.book;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import dev.qalab.library.common.ConflictException;
import dev.qalab.library.common.InvalidFieldException;
import dev.qalab.library.common.NotFoundException;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Level: unit test with mocks. The repository is replaced by a Mockito mock,
 * the clock is fixed to 2026-06-15 so "current year" is deterministic.
 * Jest analogy: jest.mock() + jest.useFakeTimers().
 */
@Epic("Catalog")
@Feature("Books service")
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC);

	@Mock
	private BookRepository repository;

	@Captor
	private ArgumentCaptor<Book> savedBook;

	private BookService service;

	@BeforeEach
	void setUp() {
		service = new BookService(repository, FIXED_CLOCK);
	}

	@Nested
	@Story("Create book")
	@DisplayName("create()")
	class Create {

		@Test
		@DisplayName("normalizes the ISBN and saves the book")
		void savesNormalizedBook() {
			when(repository.findByIsbn("9783161484100")).thenReturn(Optional.empty());
			when(repository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

			service.create(request("978-3-16-148410-0", 2020));

			verify(repository).save(savedBook.capture());
			assertThat(savedBook.getValue())
				.extracting(Book::getIsbn, Book::getTitle, Book::getPublishedYear)
				.containsExactly("9783161484100", "Clean Tests", 2020);
		}

		@Test
		@DisplayName("accepts the current year (upper boundary)")
		void acceptsCurrentYear() {
			when(repository.findByIsbn(any())).thenReturn(Optional.empty());
			when(repository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

			Book book = service.create(request("9783161484100", 2026));

			assertThat(book.getPublishedYear()).isEqualTo(2026);
		}

		@Test
		@DisplayName("rejects next year (upper boundary + 1)")
		void rejectsNextYear() {
			assertThatThrownBy(() -> service.create(request("9783161484100", 2027)))
				.isInstanceOf(InvalidFieldException.class)
				.hasMessageContaining("max 2026")
				.extracting("field")
				.isEqualTo("publishedYear");
			verify(repository, never()).save(any());
		}

		@Test
		@DisplayName("rejects a duplicate ISBN, written differently")
		void rejectsDuplicateIsbn() {
			Allure.step("Given a book with ISBN 9783161484100 exists", () -> when(repository.findByIsbn("9783161484100"))
				.thenReturn(Optional.of(book(1L, "9783161484100"))));

			Allure.step("When the same ISBN is created with hyphens, then a conflict is raised",
					() -> assertThatThrownBy(() -> service.create(request("978-3-16-148410-0", 2020)))
						.isInstanceOf(ConflictException.class)
						.hasMessageContaining("9783161484100"));

			Allure.step("And nothing is saved", () -> verify(repository, never()).save(any()));
		}

	}

	@Nested
	@Story("Update book")
	@DisplayName("update()")
	class Update {

		@Test
		@DisplayName("changes all fields of an existing book")
		void updatesFields() {
			Book existing = book(7L, "9783161484100");
			when(repository.findById(7L)).thenReturn(Optional.of(existing));
			when(repository.findByIsbn("9783161484100")).thenReturn(Optional.of(existing));
			when(repository.save(existing)).thenReturn(existing);

			Book updated = service.update(7L, new BookRequest("9783161484100", "New title", "New author", 2001, 9));

			assertThat(updated)
				.extracting(Book::getTitle, Book::getAuthor, Book::getPublishedYear, Book::getCopiesTotal)
				.containsExactly("New title", "New author", 2001, 9);
		}

		@Test
		@DisplayName("rejects taking over the ISBN of another book")
		void rejectsIsbnOfOtherBook() {
			when(repository.findById(7L)).thenReturn(Optional.of(book(7L, "9783161484100")));
			when(repository.findByIsbn("9780132350884")).thenReturn(Optional.of(book(1L, "9780132350884")));

			assertThatThrownBy(() -> service.update(7L, request("9780132350884", 2020)))
				.isInstanceOf(ConflictException.class);
		}

		@Test
		@DisplayName("throws NotFound for an unknown id")
		void unknownId() {
			when(repository.findById(42L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.update(42L, request("9783161484100", 2020)))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Book with id 42 not found");
		}

	}

	@Nested
	@Story("Delete book")
	@DisplayName("delete()")
	class Delete {

		@Test
		void deletesExistingBook() {
			when(repository.existsById(7L)).thenReturn(true);

			service.delete(7L);

			verify(repository).deleteById(7L);
		}

		@Test
		void unknownIdIsNotDeleted() {
			when(repository.existsById(42L)).thenReturn(false);

			assertThatThrownBy(() -> service.delete(42L)).isInstanceOf(NotFoundException.class);
			verify(repository, never()).deleteById(any());
		}

	}

	private static BookRequest request(String isbn, int publishedYear) {
		return new BookRequest(isbn, "Clean Tests", "Jane Tester", publishedYear, 1);
	}

	/** Book is a JPA entity whose id is set by the database, so tests set it via reflection. */
	private static Book book(long id, String isbn) {
		Book book = new Book(isbn, "Existing", "Someone", 2000, 1);
		ReflectionTestUtils.setField(book, "id", id);
		return book;
	}

}
