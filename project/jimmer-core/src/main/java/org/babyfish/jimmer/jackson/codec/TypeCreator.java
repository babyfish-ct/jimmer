package org.babyfish.jimmer.jackson.codec;

@FunctionalInterface
public interface TypeCreator<JT> {
    JT createType(JsonTypeFactory<JT> tf);
}
