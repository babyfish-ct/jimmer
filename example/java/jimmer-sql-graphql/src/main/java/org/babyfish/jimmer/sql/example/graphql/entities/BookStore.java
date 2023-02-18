package org.babyfish.jimmer.sql.example.graphql.entities;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.example.graphql.bll.resolver.BookStoreAvgPriceResolver;
import org.babyfish.jimmer.sql.example.graphql.bll.resolver.BookStoreNewestBooksResolver;
import org.babyfish.jimmer.sql.example.graphql.entities.common.BaseEntity;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

@Entity
public interface BookStore extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    @NotBlank
    String name();

    @Nullable
    @NotBlank
    String website();

    @OneToMany(mappedBy = "store", orderedProps = @OrderedProp("name"))
    List<Book> books();

    // -----------------------------
    //
    // Everything below this line are calculated properties.
    //
    // The complex calculated properties are shown here.
    // As for the simple calculated properties, you can view `Author.fullName`
    // -----------------------------

    @Transient(BookStoreAvgPriceResolver.class)
    BigDecimal avgPrice();

    /*
     * For example, if `BookStore.books` returns `[
     *     {name: A, edition: 1}, {name: A, edition: 2}, {name: A, edition: 3},
     *     {name: B, edition: 1}, {name: B, edition: 2}
     * ]`, `BookStore.newestBooks` returns `[
     *     {name: A, edition: 3}, {name: B, edition: 2}
     * ]`
     *
     * It is worth noting that if the calculated property returns entity object
     * or entity list, the shape can be controlled by the deeper child fetcher
     */
    @Transient(BookStoreNewestBooksResolver.class)
    List<Book> newestBooks();
}