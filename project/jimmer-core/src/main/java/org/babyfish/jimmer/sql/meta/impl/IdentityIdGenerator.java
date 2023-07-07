package org.babyfish.jimmer.sql.meta.impl;

import org.babyfish.jimmer.sql.meta.IdGenerator;

public final class IdentityIdGenerator implements IdGenerator {

    public static final IdentityIdGenerator INSTANCE = new IdentityIdGenerator();

    private IdentityIdGenerator() {}
}
