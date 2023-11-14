package org.babyfish.jimmer.sql.example.service;

import org.babyfish.jimmer.spring.model.SortUtils;
import org.babyfish.jimmer.spring.repository.SpringOrders;
import org.babyfish.jimmer.sql.example.model.Book;
import org.babyfish.jimmer.sql.example.repository.BookRepository;
import org.babyfish.jimmer.sql.example.service.dto.BookInput;
import org.babyfish.jimmer.sql.example.service.dto.BookSpecification;
import org.babyfish.jimmer.sql.example.service.dto.CompositeBookInput;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * A real project should be a three-tier architecture consisting
 * of repository, service, and controller.
 *
 * This demo has no business logic, its purpose is only to tell users
 * how to use jimmer with the <b>least</b> code. Therefore, this demo
 * does not follow this convention, and let services be directly
 * decorated by `@Controller`, not `@Service`.
 */
@Controller
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // --- Query ---

    @QueryMapping
    public List<Book> books(
            @Argument @Nullable String name,
            @Argument @Nullable BigDecimal minPrice,
            @Argument @Nullable BigDecimal maxPrice,
            @Argument @Nullable String storeName,
            @Argument @Nullable String authorName,
            @Argument @Nullable String sortCode
    ) {
        return bookRepository.findBooks(name, minPrice, maxPrice, storeName, authorName, sortCode);
    }

    @QueryMapping
    public List<Book> booksBySuperQBE(
            @Argument @Nullable String name,
            @Argument @Nullable BigDecimal minPrice,
            @Argument @Nullable BigDecimal maxPrice,
            @Argument @Nullable String storeName,
            @Argument @Nullable String authorName,
            @Argument @Nullable String sortCode
    ) {
        BookSpecification specification = new BookSpecification();
        specification.setName(name);
        specification.setMinPrice(minPrice);
        specification.setMaxPrice(maxPrice);
        specification.setStoreName(storeName);
        specification.setAuthorName(authorName);
        return bookRepository.find(
                specification,
                SortUtils.toSort(sortCode != null ? sortCode : "name asc")
        );
    }

    // --- Mutation ---

    @MutationMapping
    @Transactional
    public Book saveBook(@Argument BookInput input) {
        return bookRepository.save(input);
    }

    @MutationMapping
    @Transactional
    public Book saveCompositeBook(@Argument CompositeBookInput input) {
        return bookRepository.save(input);
    }

    @MutationMapping
    @Transactional
    public int deleteBook(@Argument long id) {
        bookRepository.deleteById(id);
        // GraphQL requires return value,
        // but `deleteById` of spring-data does not support value.
        // Is there a better design?
        return 1;
    }
}
