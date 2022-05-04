package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.sql.model.AuthorTable;
import org.babyfish.jimmer.sql.model.BookTable;
import org.junit.jupiter.api.Test;

public class JimmerSqlTest {

    @Test
    public void test() {

    }

    private void impl(
            String name,
            String storeName,
            String authorFirstName,
            String authorLastName
    ) {
        SqlClient sqlClient = sqlClient();
        BookTable.createQuery(sqlClient, (query, book) -> {
            if (name != null) {
                query.where(book.name().ilike(name));
            }
            if (storeName != null) {
                query.where(book.store().name().ilike(name));
            }
            if (authorFirstName != null || authorLastName != null) {
                query.where(
                    book.id().in(
                        AuthorTable.createSubQuery(query, (subQuery, author) ->
                            subQuery.where(
                                author.firstName().ilike(authorFirstName).or(
                                        author.lastName().ilike(authorLastName)
                                )
                            ).select(author.books().id())
                        )
                    )
                );
            }
            return query.select(book);
        });
    }

    private SqlClient sqlClient() {
        throw new RuntimeException();
    }
}
