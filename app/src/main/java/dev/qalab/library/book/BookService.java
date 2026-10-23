package dev.qalab.library.book;

import java.time.Clock;
import java.time.Year;

import dev.qalab.library.common.ConflictException;
import dev.qalab.library.common.InvalidFieldException;
import dev.qalab.library.common.IsbnValidator;
import dev.qalab.library.common.NotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookService {

	private final BookRepository repository;

	private final Clock clock;

	public BookService(BookRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public Page<Book> search(String title, String author, Pageable pageable) {
		return repository.findByTitleContainingIgnoreCaseAndAuthorContainingIgnoreCase(nullToEmpty(title),
				nullToEmpty(author), pageable);
	}

	@Transactional(readOnly = true)
	public Book get(long id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Book", id));
	}

	public Book create(BookRequest request) {
		String isbn = IsbnValidator.normalize(request.isbn());
		checkPublishedYear(request.publishedYear());
		if (repository.findByIsbn(isbn).isPresent()) {
			throw duplicateIsbn(isbn);
		}
		return repository.save(new Book(isbn, request.title(), request.author(), request.publishedYear(),
				request.copiesTotal()));
	}

	public Book update(long id, BookRequest request) {
		Book book = get(id);
		String isbn = IsbnValidator.normalize(request.isbn());
		checkPublishedYear(request.publishedYear());
		repository.findByIsbn(isbn).filter(other -> !other.getId().equals(book.getId())).ifPresent(other -> {
			throw duplicateIsbn(isbn);
		});
		book.update(isbn, request.title(), request.author(), request.publishedYear(), request.copiesTotal());
		return repository.save(book);
	}

	public void delete(long id) {
		if (!repository.existsById(id)) {
			throw new NotFoundException("Book", id);
		}
		repository.deleteById(id);
	}

	private void checkPublishedYear(int publishedYear) {
		int currentYear = Year.now(clock).getValue();
		if (publishedYear > currentYear) {
			throw new InvalidFieldException("publishedYear", "must not be in the future (max " + currentYear + ")");
		}
	}

	private static ConflictException duplicateIsbn(String isbn) {
		return new ConflictException("A book with ISBN " + isbn + " already exists");
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value.strip();
	}

}
