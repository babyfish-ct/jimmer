package org.babyfish.jimmer.client.meta;

public interface ApiParameter {

    String getName();

    TypeRef getType();

    int getOriginalIndex();

    boolean isDefaultValueSpecified();
}
