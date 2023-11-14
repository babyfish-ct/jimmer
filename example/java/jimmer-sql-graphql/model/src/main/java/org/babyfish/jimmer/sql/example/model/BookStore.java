package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.example.model.common.BaseEntity;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

@Entity
public interface BookStore extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key // ❶
    String name();

    @Nullable // ❷
    String website();

    @OneToMany(mappedBy = "store", orderedProps = { // ❸
            @OrderedProp("name"),
            @OrderedProp(value = "edition", desc = true)
    })
    List<Book> books();

    // -----------------------------
    //
    // Everything below this line are calculated properties.
    //
    // The complex calculated properties are shown here.
    // As for the simple calculated properties, you can view `Author.fullName`
    // -----------------------------

    @Transient(ref = "bookStoreAvgPriceResolver") // ❹
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
    @Transient(ref = "bookStoreNewestBooksResolver") // ❺
    List<Book> newestBooks();
}

/*----------------Documentation Links----------------
❶ https://babyfish-ct.github.io/jimmer/docs/mapping/advanced/key
❷ https://babyfish-ct.github.io/jimmer/docs/mapping/base/nullity
❸ https://babyfish-ct.github.io/jimmer/docs/mapping/base/association/one-to-many
❹ ❺ https://babyfish-ct.github.io/jimmer/docs/mapping/advanced/calculated/transient
---------------------------------------------------*/
