package dev.qalab.library.book;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "book")
public class Book {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Normalized ISBN-13: digits only. */
	@Column(nullable = false, unique = true, length = 13)
	private String isbn;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, length = 120)
	private String author;

	@Column(name = "published_year", nullable = false)
	private int publishedYear;

	@Column(name = "copies_total", nullable = false)
	private int copiesTotal;

	protected Book() {
		// for JPA
	}

	public Book(String isbn, String title, String author, int publishedYear, int copiesTotal) {
		this.isbn = isbn;
		this.title = title;
		this.author = author;
		this.publishedYear = publishedYear;
		this.copiesTotal = copiesTotal;
	}

	public Long getId() {
		return id;
	}

	public String getIsbn() {
		return isbn;
	}

	public String getTitle() {
		return title;
	}

	public String getAuthor() {
		return author;
	}

	public int getPublishedYear() {
		return publishedYear;
	}

	public int getCopiesTotal() {
		return copiesTotal;
	}

	void update(String isbn, String title, String author, int publishedYear, int copiesTotal) {
		this.isbn = isbn;
		this.title = title;
		this.author = author;
		this.publishedYear = publishedYear;
		this.copiesTotal = copiesTotal;
	}

}
