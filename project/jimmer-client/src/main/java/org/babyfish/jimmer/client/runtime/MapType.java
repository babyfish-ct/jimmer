package org.babyfish.jimmer.client.runtime;

public interface MapType extends Type {

    Type getKeyType();

    Type getValueType();
}
