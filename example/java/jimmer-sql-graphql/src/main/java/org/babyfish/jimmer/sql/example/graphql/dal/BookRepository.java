package org.babyfish.jimmer.sql.example.graphql.dal;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.Predicate;
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
                                            Predicate.or(
                                                author.firstName().ilike(authorName, LikeMode.START),
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

    public Map<Long, BigDecimal> findAvgPricesByStoreIds(
            Collection<Long> storeIds
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
