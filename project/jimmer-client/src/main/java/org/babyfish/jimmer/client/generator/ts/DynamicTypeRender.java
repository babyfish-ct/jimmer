package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.CodeWriter;
import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.runtime.ObjectType;

public class DynamicTypeRender implements Render {

    private final String name;

    private final ObjectType type;

    public DynamicTypeRender(String name, ObjectType type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public void render(CodeWriter writer) {

    }
}
