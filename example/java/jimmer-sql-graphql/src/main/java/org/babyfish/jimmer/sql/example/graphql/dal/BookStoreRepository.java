package org.babyfish.jimmer.sql.example.graphql.dal;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStore;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStoreTable;
import org.babyfish.jimmer.sql.example.graphql.entities.BookTable;
import org.babyfish.jimmer.sql.fluent.Fluent;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
public class BookStoreRepository {

    private final JSqlClient sqlClient;

    public BookStoreRepository(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    public List<BookStore> find(@Nullable String name) {
        Fluent fluent = sqlClient.createFluent();
        BookStoreTable store = new BookStoreTable();

        return fluent
                .query(store)
                .whereIf(
                        StringUtils.hasText(name),
                        () -> store.name().ilike(name)
                )
                .orderBy(store.name())
                .select(store)
                .execute();
    }
}
