package org.babyfish.jimmer.sql.example.dal;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.example.model.Author;
import org.babyfish.jimmer.sql.example.model.AuthorTable;
import org.babyfish.jimmer.sql.example.model.AuthorTableEx;
import org.babyfish.jimmer.sql.example.model.Gender;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.List;

public interface AuthorRepository extends JRepository<Author, Long> {

    AuthorTable table = AuthorTable.$;

    default List<Author> findAuthors(
            @Nullable String firstName,
            @Nullable String lastName,
            @Nullable Gender gender,
            Fetcher<Author> fetcher
    ) {
        return sql()
                .createQuery(table)
                .whereIf(StringUtils.hasText(firstName), table.firstName().eq(firstName))
                .whereIf(StringUtils.hasText(lastName), table.lastName().eq(lastName))
                .whereIf(gender != null, table.gender().eq(gender))
                .orderBy(table.firstName(), table.lastName())
                .select(table.fetch(fetcher))
                .execute();
    }
}
