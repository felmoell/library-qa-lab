package dev.qalab.library.support;

import java.util.Map;

import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/**
 * Reusable API steps (similar to a Page Object, but for an API).
 * Every @Step method appears as a named step in Allure, with its parameters filled into the name.
 * This needs the AspectJ weaver as -javaagent, which the pom configures for Surefire and Failsafe.
 */
public class BookApiSteps {

	private final RequestSpecification spec;

	public BookApiSteps(RequestSpecification spec) {
		this.spec = spec;
	}

	@Step("Create book \"{title}\" with ISBN {isbn}")
	public ValidatableResponse createBook(String isbn, String title) {
		return given(spec).contentType(ContentType.JSON)
			.body(Map.of("isbn", isbn, "title", title, "author", "Jane Tester", "publishedYear", 2020,
					"copiesTotal", 2))
			.post("/api/books")
			.then();
	}

	@Step("Get book {id}")
	public ValidatableResponse getBook(long id) {
		return given(spec).get("/api/books/{id}", id).then();
	}

	@Step("Update book {id}: title \"{title}\", {copiesTotal} copies")
	public ValidatableResponse updateBook(long id, String isbn, String title, int copiesTotal) {
		return given(spec).contentType(ContentType.JSON)
			.body(Map.of("isbn", isbn, "title", title, "author", "Jane Tester", "publishedYear", 2020,
					"copiesTotal", copiesTotal))
			.put("/api/books/{id}", id)
			.then();
	}

	@Step("Delete book {id}")
	public ValidatableResponse deleteBook(long id) {
		return given(spec).delete("/api/books/{id}", id).then();
	}

	@Step("Search books by author \"{author}\"")
	public ValidatableResponse searchByAuthor(String author) {
		return given(spec).queryParam("author", author).get("/api/books").then();
	}

}
