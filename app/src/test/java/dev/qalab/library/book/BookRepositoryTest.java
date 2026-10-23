package dev.qalab.library.book;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Level: persistence slice test. Starts JPA + Flyway against an in-memory H2, so the real migrations
 * and seed data (V2__seed_books.sql) are used. Each test runs in a transaction that is rolled back.
 */
@Epic("Catalog")
@Feature("Books persistence")
@DataJpaTest
class BookRepositoryTest {

	@Autowired
	private BookRepository repository;

	@Test
	@DisplayName("author search is case-insensitive and matches parts of the name")
	void searchByAuthor() {
		Page<Book> result = search("", "MARTIN");

		assertThat(result.getContent()).extracting(Book::getAuthor)
			.containsExactly("Robert C. Martin", "Martin Fowler");
	}

	@Test
	@DisplayName("title search finds 'Testing' and 'Tests' for 'test'")
	void searchByTitle() {
		assertThat(search("test", "").getTotalElements()).isEqualTo(6);
	}

	@Test
	@DisplayName("empty filters return all seeded books")
	void emptyFiltersReturnAll() {
		assertThat(search("", "").getTotalElements()).isEqualTo(21);
	}

	@Test
	@DisplayName("the database rejects a duplicate ISBN (unique constraint)")
	void uniqueIsbnConstraint() {
		Book copy = new Book("9780132350884", "Copy of Clean Code", "Someone", 2020, 1);

		// saveAndFlush forces the INSERT now. (With IDENTITY ids even save() inserts immediately, but
		// relying on that is fragile: with a sequence strategy the error would only surface on commit.)
		assertThatThrownBy(() -> repository.saveAndFlush(copy)).isInstanceOf(DataIntegrityViolationException.class);
	}

	private Page<Book> search(String title, String author) {
		return repository.findByTitleContainingIgnoreCaseAndAuthorContainingIgnoreCase(title, author,
				PageRequest.of(0, 50, Sort.by("id")));
	}

}
