package org.babyfish.jimmer.example.cloud.author;

import org.babyfish.jimmer.example.cloud.model.Author;
import org.babyfish.jimmer.spring.repository.JRepository;

public interface AuthorRepository extends JRepository<Author, Long> {
}
