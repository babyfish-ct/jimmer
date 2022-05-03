package org.babyfish.jimmer.example.core.model;

import org.babyfish.jimmer.Immutable;

import javax.validation.constraints.Size;
import java.util.List;

@Immutable
public interface Author {

    @Size(max = 50)
    String name();

    List<Book> books();
}
