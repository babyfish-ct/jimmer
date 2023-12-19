package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.CodeWriter;
import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.runtime.EnumType;

public class EnumTypeRender implements Render {

    private final String name;

    private final EnumType enumType;

    public EnumTypeRender(String name, EnumType enumType) {
        this.name = name;
        this.enumType = enumType;
    }

    @Override
    public void export(CodeWriter writer) {
        writer.code("export type {").code(name).code("} from './").code(name).code("';\n");
        writer.code("export {").code(name).code("_CONSTANTS} from './").code(name).code("';\n");
    }

    @Override
    public void render(CodeWriter writer) {
        writer.code("export const ").code(name).code("_CONSTANTS = ");
        writer.scope(CodeWriter.ScopeType.LIST, ", ", true, () -> {
            for (EnumType.Constant constant : enumType.getConstants()) {
                writer.separator();
                DocUtils.doc(constant.getDoc(), constant.getName(), enumType.getDoc(), writer);
                writer.code('\'').code(constant.getName()).code('\'');
            }
        });
        writer.code(" as const;\n");
        writer.doc(enumType.getDoc());
        writer.code("export type ").code(name).code(" = typeof ").code(name).code("_CONSTANTS;\n");
    }
}
