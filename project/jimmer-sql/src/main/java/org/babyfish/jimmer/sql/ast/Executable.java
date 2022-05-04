package org.babyfish.jimmer.sql.ast;

import java.sql.Connection;

public interface Executable<R> {

    public R execute(Connection con);
}
