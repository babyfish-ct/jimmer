package org.babyfish.jimmer.client.generator.java;

import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.runtime.ObjectType;

public class ObjectTypeRender implements Render {

    private final ObjectType type;

    private final String name;

    private final boolean dynamic;

    public ObjectTypeRender(ObjectType type, String name, boolean dynamic) {
        this.type = type;
        this.name = name;
        this.dynamic = dynamic;
    }

    @Override
    public void render(SourceWriter writer) {

    }
}
