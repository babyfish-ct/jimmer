package org.babyfish.jimmer.example.core.model;

import org.babyfish.jimmer.Immutable;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Immutable
public interface BookStore {

    @Pattern(regexp = "[^\\d]+.*")
    @Size(max = 50)
    String name();

    List<Book> books();
}
