package org.babyfish.jimmer.client.generator.java;

import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.runtime.EnumType;

public class EnumTypeRender implements Render {

    private final EnumType enumType;

    public EnumTypeRender(EnumType enumType) {
        this.enumType = enumType;
    }

    @Override
    public void render(SourceWriter writer) {

    }
}
