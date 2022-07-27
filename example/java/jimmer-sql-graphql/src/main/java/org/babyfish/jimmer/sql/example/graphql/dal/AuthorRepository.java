package org.babyfish.jimmer.sql.example.graphql.dal;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.example.graphql.entities.Author;
import org.babyfish.jimmer.sql.example.graphql.entities.AuthorTable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
public class AuthorRepository {

    private final SqlClient sqlClient;

    public AuthorRepository(SqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    public List<Author> find(@Nullable String name) {
        return sqlClient.createQuery(AuthorTable.class, (q, author) -> {
            if (StringUtils.hasText(name)) {
                q.where(
                        Predicate.or(
                                author.firstName().ilike(name, LikeMode.START),
                                author.lastName().ilike(name, LikeMode.START)
                        )
                );
            }
            q.orderBy(author.firstName());
            return q.select(author);
        }).execute();
    }
}
