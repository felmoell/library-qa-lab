package dev.qalab.library.book;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {

	Optional<Book> findByIsbn(String isbn);

	/** Both filters are "contains, ignore case"; pass "" to not filter. */
	Page<Book> findByTitleContainingIgnoreCaseAndAuthorContainingIgnoreCase(String title, String author,
			Pageable pageable);

}
