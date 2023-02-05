package org.babyfish.jimmer.sql.example.bll;

import org.babyfish.jimmer.spring.model.SortUtils;
import org.babyfish.jimmer.sql.example.dal.BookRepository;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.example.model.dto.*;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/book")
@Transactional
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @GetMapping("/simpleList")
    public List<SimpleBook> findSimpleBooks() {
        return bookRepository.findAllStaticObjects(SimpleBook.class, BookProps.NAME, BookProps.EDITION.desc());
    }

    @GetMapping("/list")
    public Page<DefaultBook> findBooks(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "5") int pageSize,
            // The `sortCode` also support implicit join, like `store.name asc`
            @RequestParam(defaultValue = "name asc, edition desc") String sortCode,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String storeName,
            @RequestParam(required = false) String authorName
    ) {
        return bookRepository.findBooks(
                PageRequest.of(pageIndex, pageSize, SortUtils.toSort(sortCode)),
                name,
                storeName,
                authorName,
                DefaultBook.class
        );
    }

    @GetMapping("/{id}")
    @Nullable
    public ComplexBook findComplexBook(@PathVariable("id") long id) {
        return bookRepository.findNullableStaticObject(ComplexBook.class, id);
    }

    @PutMapping
    public Book saveBook(@RequestBody BookInput input) {
        return bookRepository.save(input);
    }

    @PutMapping("/withChapters")
    public Book saveCompositeBook(@RequestBody CompositeBookInput input) {
        return bookRepository.save(input);
    }

    @DeleteMapping("/{id}")
    public void deleteBook(@PathVariable("id") long id) {
        bookRepository.deleteById(id);
    }
}
