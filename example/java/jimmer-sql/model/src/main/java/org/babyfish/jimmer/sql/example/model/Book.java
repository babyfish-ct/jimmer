package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.example.model.common.BaseEntity;
import org.babyfish.jimmer.sql.example.model.common.TenantAware;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

@Entity
public interface Book extends BaseEntity, TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key // ❶
    String name();

    @Key // ❷
    int edition();

    BigDecimal price();

    @Nullable // ❸ Null property, Java API requires this annotation, but kotlin API does not
    @ManyToOne // ❹
    BookStore store();

    @ManyToMany(orderedProps = { // ❺
            @OrderedProp("firstName"),
            @OrderedProp("lastName")
    })
    @JoinTable(
            name = "BOOK_AUTHOR_MAPPING",
            joinColumnName = "BOOK_ID",
            inverseJoinColumnName = "AUTHOR_ID"
    )
    List<Author> authors();

    // -----------------------------
    // Optional properties
    // -----------------------------

    // Optional property `storeId`
    // If this property is deleted, please add `BookInput.Mapper.toBookStore(Long)`
    @IdView  // ❻
    Long storeId();

    // Optional property `authorIds`
    // If this property is deleted, please add `BookInputMapper.toAuthor(Long)`
    @IdView("authors") // ❼
    List<Long> authorIds();
}

/*----------------Documentation Links----------------
❶ ❷ https://babyfish-ct.github.io/jimmer/docs/mapping/advanced/key
❸ https://babyfish-ct.github.io/jimmer/docs/mapping/base/nullity
❹ https://babyfish-ct.github.io/jimmer/docs/mapping/base/association/many-to-one
❺ https://babyfish-ct.github.io/jimmer/docs/mapping/base/association/many-to-many
❻ ❼https://babyfish-ct.github.io/jimmer/docs/mapping/advanced/view/id-view
---------------------------------------------------*/
