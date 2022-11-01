package org.babyfish.jimmer.sql.example.graphql.dal;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.example.graphql.entities.Author;
import org.babyfish.jimmer.sql.example.graphql.entities.AuthorTable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
public class AuthorRepository {

    private final JSqlClient sqlClient;

    public AuthorRepository(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    public List<Author> find(@Nullable String name) {

        AuthorTable author = AuthorTable.$;

        return sqlClient
                .createQuery(author)
                .whereIf(
                        StringUtils.hasText(name),
                        Predicate.or(
                                author.firstName().ilike(name),
                                author.lastName().ilike(name)
                        )
                )
                .orderBy(author.firstName(), author.lastName())
                .select(author)
                .execute();
    }
}
