package dev.qalab.library.book;

public record BookResponse(long id, String isbn, String title, String author, int publishedYear, int copiesTotal) {

	static BookResponse from(Book book) {
		return new BookResponse(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
				book.getPublishedYear(), book.getCopiesTotal());
	}

}
