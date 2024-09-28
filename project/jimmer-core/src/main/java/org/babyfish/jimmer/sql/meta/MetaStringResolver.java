package org.babyfish.jimmer.sql.meta;

@FunctionalInterface
public interface MetaStringResolver {
    MetaStringResolver NO_OP = str -> str;

    String resolve(String value);
}
