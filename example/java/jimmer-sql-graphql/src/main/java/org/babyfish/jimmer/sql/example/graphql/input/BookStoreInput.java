package org.babyfish.jimmer.sql.example.graphql.entities.input;

import org.babyfish.jimmer.sql.example.graphql.entities.BookStore;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStoreDraft;
import org.springframework.lang.Nullable;

public class BookStoreInput {

    @Nullable
    private final Long id;

    private final String name;

    @Nullable
    public final String website;

    public BookStoreInput(
            @Nullable Long id,
            String name,
            @Nullable String website
    ) {
        this.id = id;
        this.name = name;
        this.website = website;
    }

    public BookStore toBookStore() {
        return BookStoreDraft.$.produce(store -> {
            if (id != null) {
                store.setId(id);
            }
            store.setName(name).setWebsite(website);
        });
    }
}
