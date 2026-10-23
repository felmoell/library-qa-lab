package dev.qalab.library.book;

import java.util.List;

import dev.qalab.library.common.ConflictException;
import dev.qalab.library.common.NotFoundException;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Level: web slice test. Starts only the MVC layer (controller, JSON, validation, exception handler),
 * no database, no real HTTP port. The service is a Mockito mock inside the Spring context (@MockitoBean).
 * Focus: HTTP contract - status codes, headers, JSON shape, validation messages.
 */
@Epic("Catalog")
@Feature("Books API (web layer)")
@WebMvcTest(BookController.class)
class BookControllerTest {

	private static final String VALID_BODY = """
			{"isbn": "978-3-16-148410-0", "title": "Clean Tests", "author": "Jane Tester",
			 "publishedYear": 2020, "copiesTotal": 2}
			""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private BookService service;

	@Test
	@Story("Create book")
	@DisplayName("POST returns 201 with Location header and the created book")
	void createReturns201() throws Exception {
		when(service.create(any())).thenReturn(book(42L));

		mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", endsWith("/api/books/42")))
			.andExpect(jsonPath("$.id").value(42))
			.andExpect(jsonPath("$.isbn").value("9783161484100"));
	}

	@Test
	@Story("Create book")
	@DisplayName("POST with several invalid fields lists every field error")
	void createWithInvalidFieldsReturns400() throws Exception {
		String body = """
				{"isbn": "9783161484101", "title": "", "author": "Jane Tester",
				 "publishedYear": 1449, "copiesTotal": -1}
				""";

		mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Bad Request"))
			.andExpect(jsonPath("$.errors[*].field")
				.value(containsInAnyOrder("isbn", "title", "publishedYear", "copiesTotal")));

		verifyNoInteractions(service);
	}

	@Test
	@Story("Create book")
	@DisplayName("POST with malformed JSON returns 400")
	void malformedJsonReturns400() throws Exception {
		mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content("{\"isbn\":"))
			.andExpect(status().isBadRequest());
	}

	@Test
	@Story("Create book")
	@DisplayName("POST with a duplicate ISBN returns 409")
	void duplicateReturns409() throws Exception {
		when(service.create(any())).thenThrow(new ConflictException("A book with ISBN 9783161484100 already exists"));

		mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("A book with ISBN 9783161484100 already exists"));
	}

	@Test
	@Story("Get book")
	@DisplayName("GET of an unknown id returns 404 problem details")
	void unknownIdReturns404() throws Exception {
		when(service.get(anyLong())).thenThrow(new NotFoundException("Book", 999));

		mvc.perform(get("/api/books/999"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.instance").value("/api/books/999"));
	}

	/** Boundary value analysis on the "size" query parameter: valid range is 1..100. */
	@ParameterizedTest(name = "size={0} -> {1}")
	@CsvSource({ "0, 400", "1, 200", "100, 200", "101, 400" })
	@Story("Search books")
	void pageSizeBoundaries(int size, int expectedStatus) throws Exception {
		when(service.search(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(book(1L))));

		mvc.perform(get("/api/books").param("size", String.valueOf(size)))
			.andExpect(status().is(expectedStatus));
	}

	private static Book book(long id) {
		Book book = new Book("9783161484100", "Clean Tests", "Jane Tester", 2020, 2);
		ReflectionTestUtils.setField(book, "id", id);
		return book;
	}

}
