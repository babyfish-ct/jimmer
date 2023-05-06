package org.babyfish.jimmer.sql.meta;

public class IdentityIdGenerator implements IdGenerator {

    public static final IdentityIdGenerator INSTANCE = new IdentityIdGenerator();

    private IdentityIdGenerator() {}
}
