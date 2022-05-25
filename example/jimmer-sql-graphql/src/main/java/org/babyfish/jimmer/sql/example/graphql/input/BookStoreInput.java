package org.babyfish.jimmer.sql.example.graphql.input;

import org.babyfish.jimmer.sql.example.graphql.entities.BookStore;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStoreDraft;
import org.springframework.lang.Nullable;

import java.util.UUID;

public class BookStoreInput {

    @Nullable
    private final UUID id;

    private final String name;

    @Nullable
    public final String website;

    public BookStoreInput(@Nullable UUID id, String name, @Nullable String website) {
        this.id = id;
        this.name = name;
        this.website = website;
    }

    @Override
    public String toString() {
        return "BookStoreInput{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", website='" + website + '\'' +
                '}';
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
