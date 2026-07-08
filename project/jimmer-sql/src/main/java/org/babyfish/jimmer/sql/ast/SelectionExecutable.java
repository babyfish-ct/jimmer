package org.babyfish.jimmer.sql.ast;

import java.sql.Connection;
import java.util.List;
import java.util.stream.Stream;

public interface SelectionExecutable<R> extends Executable<List<R>> {

    default Stream<R> stream() {
        return stream(null);
    }

    Stream<R> stream(Connection con);
}
