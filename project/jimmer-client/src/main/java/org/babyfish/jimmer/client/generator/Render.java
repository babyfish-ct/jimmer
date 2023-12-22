package org.babyfish.jimmer.client.generator;

public interface Render {

    default void export(SourceWriter writer) {}

    void render(SourceWriter writer);
}
