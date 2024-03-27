package org.babyfish.jimmer.client.java.ts;

import org.babyfish.jimmer.client.common.OperationParserImpl;
import org.babyfish.jimmer.client.common.ParameterParserImpl;
import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.ts.TypeScriptContext;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.source.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Collections;

public class TsEnumTest {

    private static final Metadata METADATA =
            Metadata
                    .newBuilder()
                    .setOperationParser(new OperationParserImpl())
                    .setParameterParser(new ParameterParserImpl())
                    .setGroups(Collections.singleton("bookService"))
                    .setGenericSupported(true)
                    .build();

    @Test
    public void testGender() {
        Context ctx = TypeScriptContext.newBuilder(METADATA).setEnumTsStyle(true).build();
        Source source = ctx.getRootSource("model/enums/Gender");
        StringWriter writer = new StringWriter();
        ctx.render(source, writer);
        Assertions.assertEquals(
                "export const Gender_CONSTANTS = [\n" +
                        "    /**\n" +
                        "     * BOYS\n" +
                        "     */\n" +
                        "    'MALE', \n" +
                        "    /**\n" +
                        "     * GIRLS\n" +
                        "     */\n" +
                        "    'FEMALE'\n" +
                        "] as const;\n" +
                        "/**\n" +
                        " * The gender, which can only be `MALE` or `FEMALE`\n" +
                        " */\n" +
                        "export const Gender_CONSTANT_MAP = {\n" +
                        "    /**\n" +
                        "     * BOYS\n" +
                        "     */\n" +
                        "    \"Male\": 'MALE', \n" +
                        "    /**\n" +
                        "     * GIRLS\n" +
                        "     */\n" +
                        "    \"Female\": 'FEMALE'\n" +
                        "} as const;\n" +
                        "export enum Gender {\n" +
                        "    /**\n" +
                        "     * BOYS\n" +
                        "     */\n" +
                        "    Male = 'MALE', \n" +
                        "    /**\n" +
                        "     * GIRLS\n" +
                        "     */\n" +
                        "    Female = 'FEMALE'\n" +
                        "}",
                writer.toString()
        );
    }

    @Test
    public void testEnumIndex() {
        Context ctx = TypeScriptContext.newBuilder(METADATA).setEnumTsStyle(true).build();
        StringWriter writer = new StringWriter();
        ctx.renderIndex("model/enums", writer);
        Assertions.assertEquals(
                "export type {Gender} from './Gender';\n" +
                        "export {Gender_CONSTANTS} from './Gender';\n" +
                        "export {Gender_CONSTANT_MAP} from './Gender';\n" +
                        "import { Gender_CONSTANTS, Gender_CONSTANT_MAP } from './Gender';\n" +
                        "export const ALL_ENUM_CONSTANTS = {\n" +
                        "    \"Gender\": Gender_CONSTANTS\n" +
                        "}\n" +
                        "export const ALL_ENUM_CONSTANT_MAPS = {\n" +
                        "    \"Gender\": Gender_CONSTANT_MAP\n" +
                        "}\n",
                writer.toString()
        );
    }

    @Test
    public void testClassicGender() {
        Context ctx = TypeScriptContext.newBuilder(METADATA).build();
        Source source = ctx.getRootSource("model/enums/Gender");
        StringWriter writer = new StringWriter();
        ctx.render(source, writer);
        Assertions.assertEquals(
                "export const Gender_CONSTANTS = [\n" +
                        "    /**\n" +
                        "     * BOYS\n" +
                        "     */\n" +
                        "    'MALE', \n" +
                        "    /**\n" +
                        "     * GIRLS\n" +
                        "     */\n" +
                        "    'FEMALE'\n" +
                        "] as const;\n" +
                        "/**\n" +
                        " * The gender, which can only be `MALE` or `FEMALE`\n" +
                        " */\n" +
                        "export type Gender = typeof Gender_CONSTANTS[number];\n",
                writer.toString()
        );
    }

    @Test
    public void testClassicEnumIndex() {
        Context ctx = TypeScriptContext.newBuilder(METADATA).build();
        StringWriter writer = new StringWriter();
        ctx.renderIndex("model/enums", writer);
        Assertions.assertEquals(
                "export type {Gender} from './Gender';\n" +
                        "export {Gender_CONSTANTS} from './Gender';\n" +
                        "import { Gender_CONSTANTS } from './Gender';\n" +
                        "export const ALL_ENUM_CONSTANTS = {\n" +
                        "    \"Gender\": Gender_CONSTANTS\n" +
                        "}\n",
                writer.toString()
        );
    }
}
