package org.babyfish.jimmer.sql.example.graphql.input;

import org.babyfish.jimmer.sql.example.graphql.entities.Book;
import org.babyfish.jimmer.sql.example.graphql.entities.BookDraft;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.List;

public class BookInput {

    @Nullable
    private final Long id;

    private final String name;

    private final int edition;

    private final BigDecimal price;

    @Nullable
    private final Long storeId;

    private final List<Long> authorIds;

    public BookInput(
            @Nullable Long id,
            String name,
            int edition,
            BigDecimal price,
            @Nullable Long storeId,
            List<Long> authorIds
    ) {
        this.id = id;
        this.name = name;
        this.edition = edition;
        this.price = price;
        this.storeId = storeId;
        this.authorIds = authorIds;
    }

    public Book toBook() {
        return BookDraft.$.produce(book -> {
            if (id != null) {
                book.setId(id);
            }
            if (storeId != null) {
                book.setStore(store -> store.setId(storeId));
            }
            book
                    .setName(name)
                    .setEdition(edition)
                    .setPrice(price);
            for (Long authorId : authorIds) {
                book.addIntoAuthors(author -> author.setId(authorId));
            }
        });
    }
}
