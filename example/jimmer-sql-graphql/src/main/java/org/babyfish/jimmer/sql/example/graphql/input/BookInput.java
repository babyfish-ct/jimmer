package org.babyfish.jimmer.sql.example.graphql.input;

import org.babyfish.jimmer.sql.example.graphql.entities.Book;
import org.babyfish.jimmer.sql.example.graphql.entities.BookDraft;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class BookInput {

    @Nullable
    private UUID id;

    private String name;

    private int edition;

    private BigDecimal price;

    @Nullable
    private UUID storeId;

    private List<UUID> authorIds;

    public BookInput(
            @Nullable UUID id,
            String name,
            int edition,
            BigDecimal price,
            @Nullable UUID storeId,
            List<UUID> authorIds
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
            for (UUID authorId : authorIds) {
                book.addIntoAuthors(author -> author.setId(authorId));
            }
        });
    }
}
