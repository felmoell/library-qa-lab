package dev.qalab.library.book;

import dev.qalab.library.support.BookApiSteps;
import dev.qalab.library.support.TestIsbn;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;

/**
 * Level: integration / API test. The whole application starts on a random port (with the real database layer),
 * RestAssured sends real HTTP requests. Runs in the Failsafe "integration-test" phase because the class ends in IT.
 * Playwright analogy: request.newContext({ baseURL }) + expect(response).
 */
@Epic("Catalog")
@Feature("Books API")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class BookApiIT {

	@LocalServerPort
	private int port;

	private RequestSpecification spec;

	private BookApiSteps api;

	@BeforeEach
	void setUp() {
		spec = new RequestSpecBuilder().setBaseUri("http://localhost")
			.setPort(port)
			// Attaches every request/response to the current Allure step
			.addFilter(new AllureRestAssured())
			.log(LogDetail.URI)
			.build();
		api = new BookApiSteps(spec);
	}

	@Test
	@Tag("smoke")
	@Severity(SeverityLevel.BLOCKER)
	@DisplayName("health endpoint reports UP")
	void healthIsUp() {
		given(spec).get("/actuator/health").then().statusCode(200).body("status", equalTo("UP"));
	}

	@Test
	@Story("Book lifecycle")
	@Severity(SeverityLevel.CRITICAL)
	@DisplayName("create, read, update and delete a book")
	void bookLifecycle() {
		String isbn = TestIsbn.random();

		long id = api.createBook(isbn, "Lifecycle")
			.statusCode(201)
			.header("Location", containsString("/api/books/"))
			.body(matchesJsonSchemaInClasspath("schemas/book.json"))
			.extract()
			.jsonPath()
			.getLong("id");

		api.getBook(id).statusCode(200).body("isbn", equalTo(isbn)).body("title", equalTo("Lifecycle"));

		api.updateBook(id, isbn, "Lifecycle, 2nd edition", 5)
			.statusCode(200)
			.body("title", equalTo("Lifecycle, 2nd edition"))
			.body("copiesTotal", equalTo(5));

		api.deleteBook(id).statusCode(204);

		api.getBook(id).statusCode(404).body("title", equalTo("Not Found"));
	}

	@Test
	@Story("Create book")
	@DisplayName("the same ISBN with and without hyphens counts as a duplicate")
	void duplicateIsbnInDifferentNotation() {
		String isbn = TestIsbn.random();
		String hyphenated = isbn.substring(0, 3) + "-" + isbn.substring(3, 12) + "-" + isbn.substring(12);

		api.createBook(isbn, "Original").statusCode(201);

		api.createBook(hyphenated, "Copy").statusCode(409).body("detail", containsString(isbn));
	}

	@Test
	@Story("Search books")
	@DisplayName("searching by author finds the seeded books")
	void searchByAuthor() {
		api.searchByAuthor("humble")
			.statusCode(200)
			.body("totalElements", greaterThanOrEqualTo(2))
			.body("content.title", hasItems("Continuous Delivery", "Accelerate"))
			.body("content.author", everyItem(containsString("Humble")));
	}

}
