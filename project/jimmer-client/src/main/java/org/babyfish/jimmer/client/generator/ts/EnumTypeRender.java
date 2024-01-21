package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.runtime.EnumType;

import java.util.regex.Pattern;

public class EnumTypeRender implements Render {

    private static final Pattern UNDER_LINE_PATTERN = Pattern.compile("_");

    private final String name;

    private final EnumType enumType;

    private final boolean isEnumTsStyle;

    public EnumTypeRender(String name, EnumType enumType, boolean isEnumTsStyle) {
        this.name = name;
        this.enumType = enumType;
        this.isEnumTsStyle = isEnumTsStyle;
    }

    public EnumType getEnumType() {
        return enumType;
    }

    @Override
    public void export(SourceWriter writer) {
        writer.code("export type {").code(name).code("} from './").code(name).code("';\n");
        writer.code("export {").code(name).code("_CONSTANTS} from './").code(name).code("';\n");
        if (isEnumTsStyle) {
            writer.code("export {").code(name).code("_CONSTANT_MAP} from './").code(name).code("';\n");
        }
    }

    @Override
    public void render(SourceWriter writer) {
        writer.code("export const ").code(name).code("_CONSTANTS = ");
        writer.scope(SourceWriter.ScopeType.LIST, ", ", true, () -> {
            for (EnumType.Constant constant : enumType.getConstants()) {
                writer.separator();
                DocUtils.doc(constant.getDoc(), constant.getName(), enumType.getDoc(), writer);
                writer.code('\'').code(constant.getName()).code('\'');
            }
        });
        writer.code(" as const;\n");
        writer.doc(enumType.getDoc());
        if (isEnumTsStyle) {
            writer.code("export const ").code(name).code("_CONSTANT_MAP = ");
            writer.scope(SourceWriter.ScopeType.OBJECT, ", ", true, () -> {
                for (EnumType.Constant constant : enumType.getConstants()) {
                    writer.separator();
                    DocUtils.doc(constant.getDoc(), constant.getName(), enumType.getDoc(), writer);
                    writer
                            .code('"')
                            .code(tsEnumConstant(constant.getName()))
                            .code('"')
                            .code(": ")
                            .code('\'')
                            .code(constant.getName())
                            .code('\'');
                }
            });
            writer.code(" as const;\n");
            writer.code("export enum ").code(name).code(' ');
            writer.scope(SourceWriter.ScopeType.OBJECT, ", ", true, () -> {
                for (EnumType.Constant constant : enumType.getConstants()) {
                    writer.separator();
                    DocUtils.doc(constant.getDoc(), constant.getName(), enumType.getDoc(), writer);
                    writer
                            .code(tsEnumConstant(constant.getName()))
                            .code(" = ")
                            .code('\'')
                            .code(constant.getName())
                            .code('\'');
                }
            });
        } else {
            writer.code("export type ").code(name).code(" = typeof ").code(name).code("_CONSTANTS[number];\n");
        }
    }

    private static String tsEnumConstant(String javaEnumConstant) {
        StringBuilder builder = new StringBuilder();
        for (String part : UNDER_LINE_PATTERN.split(javaEnumConstant)) {
            if (!part.isEmpty()) {
                builder
                        .append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase());
            }
        }
        return builder.toString();
    }
}
