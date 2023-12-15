package org.babyfish.jimmer.client.java.service;
//
//import org.babyfish.jimmer.client.FetchBy;
//import org.babyfish.jimmer.client.ThrowsAll;
//import org.babyfish.jimmer.client.java.model.Author;
//import org.babyfish.jimmer.client.java.model.Book;
//import org.babyfish.jimmer.client.java.model.BookInput;
//import org.babyfish.jimmer.client.java.model.Page;
//import org.babyfish.jimmer.client.runtime.Operation;
//import org.babyfish.jimmer.client.meta.common.*;
//import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
//import org.babyfish.jimmer.sql.fetcher.Fetcher;
//import org.babyfish.jimmer.sql.runtime.SaveErrorCode;
//import org.jetbrains.annotations.Nullable;
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.Optional;
//
//@RequestMapping(value = "/java", method = Operation.HttpMethod.GET)
//public interface BookService {
//
//    Fetcher<Book> SIMPLE_FETCHER = BookFetcher.$.name().storeId();
//
//    Fetcher<Book> COMPLEX_FETCHER = BookFetcher.$
//            .allScalarFields()
//            .store(BookStoreFetcher.$.name())
//            .authors(
//                    AuthorFetcher.$.allScalarFields().gender(false)
//            );
//
//    Fetcher<Author> AUTHOR_FETCHER = AuthorFetcher.$
//            .allScalarFields()
//            .books(
//                    BookFetcher.$
//                            .name()
//                            .store(BookStoreFetcher.$.name())
//            );
//
//    @GetMapping("/books/simple")
//    List<@FetchBy("SIMPLE_FETCHER") Book> findSimpleBooks();
//
//    @GetMapping("/books/complex")
//    List<@FetchBy("COMPLEX_FETCHER") Book> findComplexBooks(
//            @RequestParam("name") String name,
//            @RequestParam("storeName") @Nullable String storeName,
//            @RequestParam("authorName") @Nullable String authorName,
//            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
//            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice
//    );
//
//    @GetMapping("/books/complex2")
//    ResponseEntity<List<@FetchBy("COMPLEX_FETCHER") Book>> findComplexBooksByArguments(
//            FindBookArguments arguments
//    );
//
//    @GetMapping("/tuples")
//    Page<
//            Tuple2<
//                    ? extends @FetchBy("COMPLEX_FETCHER") Book,
//                    ? extends @FetchBy(value = "AUTHOR_FETCHER", nullable = true) Author
//                    >
//            > findTuples(
//            @RequestParam("name") @Nullable String name,
//            @RequestParam("pageIndex") int pageIndex,
//            @RequestParam("pageSize") int pageSize
//    );
//
//    @GetMapping("/book/{id}")
//    Optional<@FetchBy("COMPLEX_FETCHER") Book> findBook(@PathVariable("id") long id);
//
//    @PutMapping("/book")
//    @ThrowsAll(SaveErrorCode.class)
//    Book saveBooks(@RequestBody BookInput input);
//
//    @PatchMapping("/book")
//    @ThrowsAll(SaveErrorCode.class)
//    Book updateBooks(@RequestBody BookInput input);
//
//    @DeleteMapping("/book/{id}")
//    int deleteBook(@PathVariable("id") long id);
//
//    @RequestMapping("version")
//    int version();
//}
