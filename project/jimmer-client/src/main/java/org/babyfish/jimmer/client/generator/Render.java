package org.babyfish.jimmer.client.generator;

public interface Render {

    default void export(CodeWriter writer) {}

    void render(CodeWriter writer);
}
