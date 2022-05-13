package org.babyfish.jimmer.meta.sql;

public class IdentityIdGenerator implements IdGenerator {

    public static final IdentityIdGenerator INSTANCE = new IdentityIdGenerator();

    private IdentityIdGenerator() {}
}
