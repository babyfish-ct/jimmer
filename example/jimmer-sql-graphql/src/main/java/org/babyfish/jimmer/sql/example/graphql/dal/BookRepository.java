package org.babyfish.jimmer.sql.example.graphql.dal;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.example.graphql.entities.AuthorTableEx;
import org.babyfish.jimmer.sql.example.graphql.entities.Book;
import org.babyfish.jimmer.sql.example.graphql.entities.BookTable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class BookRepository {

    private final SqlClient sqlClient;

    public BookRepository(SqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    public List<Book> find(
            @Nullable String name,
            @Nullable String storeName,
            @Nullable String authorName
    ) {
        return sqlClient.createQuery(BookTable.class, (q, book) -> {
            if (StringUtils.hasText(name)) {
                q.where(book.name().ilike(name, LikeMode.START));
            }
            if (StringUtils.hasText(storeName)) {
                q.where(book.store().name().ilike(storeName, LikeMode.START));
            }
            if (StringUtils.hasText(authorName)) {
                q.where(
                        book.id().in(
                                q.createSubQuery(AuthorTableEx.class, (sq, author) -> {
                                    sq.where(
                                            author.firstName().ilike(authorName, LikeMode.START).or(
                                                    author.lastName().ilike(authorName, LikeMode.START)
                                            )
                                    );
                                    return sq.select(author.books().id());
                                })
                        )
                );
            }
            return q
                    .orderBy(book.name())
                    .select(book);
        }).execute();
    }

    public Map<UUID, BigDecimal> findAvgPricesByStoreIds(
            Collection<UUID> storeIds
    ) {
        return Tuple2.toMap(
                sqlClient.createQuery(BookTable.class, (q, book) -> {
                    q
                            .where(book.store().id().in(storeIds))
                            .groupBy(book.store().id());
                    return q.select(
                            book.store().id(),
                            book.price().avg()
                    );
                }).execute()
        );
    }
}
