package org.babyfish.jimmer.sql.example.graphql.dal;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.example.graphql.entities.Author;
import org.babyfish.jimmer.sql.example.graphql.entities.AuthorTable;
import org.babyfish.jimmer.sql.fluent.Fluent;
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
        Fluent fluent = sqlClient.createFluent();
        AuthorTable author = new AuthorTable();

        return fluent
                .query(author)
                .whereIf(
                        StringUtils.hasText(name),
                        () -> Predicate.or(
                                author.firstName().ilike(name, LikeMode.START),
                                author.lastName().ilike(name, LikeMode.START)
                        )
                )
                .orderBy(author.firstName(), author.lastName())
                .select(author)
                .execute();
    }
}
