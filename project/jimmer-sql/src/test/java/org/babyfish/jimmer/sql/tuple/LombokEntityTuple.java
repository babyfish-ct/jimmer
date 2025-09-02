package org.babyfish.jimmer.sql.tuple;

import lombok.Data;
import org.babyfish.jimmer.sql.TypedTuple;
import org.babyfish.jimmer.sql.model.Book;

@Data
@TypedTuple
public class LombokEntityTuple {

    private final Book book;

    private final long authorCount;
}
