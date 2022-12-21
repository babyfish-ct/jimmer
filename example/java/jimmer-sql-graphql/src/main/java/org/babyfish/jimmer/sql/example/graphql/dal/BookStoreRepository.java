package org.babyfish.jimmer.sql.example.graphql.dal;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStore;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStoreTable;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.List;

public interface BookStoreRepository extends JRepository<BookStore, Long> {

    BookStoreTable table = BookStoreTable.$;

    default List<BookStore> find(@Nullable String name) {
        return sql()
                .createQuery(table)
                .whereIf(
                        StringUtils.hasText(name),
                        table.name().ilike(name)
                )
                .orderBy(table.name())
                .select(table)
                .execute();
    }
}
