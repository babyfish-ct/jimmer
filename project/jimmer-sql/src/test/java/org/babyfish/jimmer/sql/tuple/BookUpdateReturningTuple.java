package org.babyfish.jimmer.sql.tuple;

import lombok.Data;
import org.babyfish.jimmer.sql.TypedTuple;

import java.util.UUID;

@TypedTuple
@Data
public class BookUpdateReturningTuple {

    private final UUID id;

    private final String name;
}
