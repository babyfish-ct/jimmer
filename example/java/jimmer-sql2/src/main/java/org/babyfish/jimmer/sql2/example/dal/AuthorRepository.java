package org.babyfish.jimmer.sql2.example.dal;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql2.example.model.Author;
import org.babyfish.jimmer.sql2.example.model.Gender;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface AuthorRepository extends JRepository<Author, Long> {

    List<Author> findByFirstNameAndLastNameAndGender(
            Sort sort,
            @Nullable String firstName,
            @Nullable String lastName,
            @Nullable Gender gender,
            Fetcher<Author> fetcher
    );
}
