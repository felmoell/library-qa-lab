package dev.qalab.library.book;

import java.net.URI;

import dev.qalab.library.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/books")
class BookController {

	private final BookService service;

	BookController(BookService service) {
		this.service = service;
	}

	@GetMapping
	PageResponse<BookResponse> list(@RequestParam(required = false) String title,
			@RequestParam(required = false) String author,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		PageRequest pageable = PageRequest.of(page, size, Sort.by("id"));
		return PageResponse.from(service.search(title, author, pageable), BookResponse::from);
	}

	@GetMapping("/{id}")
	BookResponse get(@PathVariable long id) {
		return BookResponse.from(service.get(id));
	}

	@PostMapping
	ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request) {
		Book book = service.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(book.getId())
			.toUri();
		return ResponseEntity.created(location).body(BookResponse.from(book));
	}

	@PutMapping("/{id}")
	BookResponse update(@PathVariable long id, @Valid @RequestBody BookRequest request) {
		return BookResponse.from(service.update(id, request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable long id) {
		service.delete(id);
	}

}
