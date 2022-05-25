package org.babyfish.jimmer.sql.example.graphql.dal;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStore;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStoreTable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
public class BookStoreRepository {

    private final SqlClient sqlClient;

    public BookStoreRepository(SqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    public List<BookStore> find(@Nullable String name) {
        return sqlClient.createQuery(BookStoreTable.class, (q, store) -> {
            if (StringUtils.hasText(name)) {
                q.where(store.name().ilike(name, LikeMode.START));
            }
            return q.select(store);
        }).execute();
    }
}
