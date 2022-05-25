package org.babyfish.jimmer.sql.ast;

import java.sql.Connection;

public interface Executable<R> {

    R execute();

    R execute(Connection con);
}
