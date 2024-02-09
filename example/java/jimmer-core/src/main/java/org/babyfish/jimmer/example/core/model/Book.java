package org.babyfish.jimmer.example.core.model;

import org.babyfish.jimmer.Immutable;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.Null;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Immutable
public interface Book {

    @Size(max = 50)
    String name();

    @Null // Nullable property, Java-API needs it, but kotlin-API does not.
    BookStore store();

    int price();

    LocalDateTime lastModifiedTime();

    List<Author> authors();
}
