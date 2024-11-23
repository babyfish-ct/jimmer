package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.exclude.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ComplexKeyTest extends Tests {

    @Test
    public void test() {
        ImmutableType type = ImmutableType.get(User.class);
        assertContentEquals(
                "{" +
                        "=[" +
                        "--->org.babyfish.jimmer.sql.model.exclude.User.name, " +
                        "--->org.babyfish.jimmer.sql.model.exclude.User.nickName" +
                        "], " +
                        "2=[" +
                        "--->org.babyfish.jimmer.sql.model.exclude.User.nickName, " +
                        "--->org.babyfish.jimmer.sql.model.exclude.User.password" +
                        "]}",
                type.getKeyMatcher().toMap()
        );
    }
}
