package org.babyfish.jimmer.client.meta;

public interface MapType extends Type {

    Type getKeyType();

    Type getValueType();
}
