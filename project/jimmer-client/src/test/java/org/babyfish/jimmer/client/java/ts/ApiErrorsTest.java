package org.babyfish.jimmer.client.java.ts;

import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.ts.TypeScriptContext;
import org.babyfish.jimmer.client.common.OperationParserImpl;
import org.babyfish.jimmer.client.common.ParameterParserImpl;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.source.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Arrays;

public class ApiErrorsTest {

    private static final Metadata METADATA =
            Metadata
                    .newBuilder()
                    .setOperationParser(new OperationParserImpl())
                    .setParameterParameter(new ParameterParserImpl())
                    .setGroups(Arrays.asList("bookService", "treeService"))
                    .setGenericSupported(true)
                    .build();

    @Test
    public void testApiErrors() {
        Context ctx = new TypeScriptContext(METADATA);
        Source apiErrorsSources = ctx.getRootSource("ApiErrors");
        StringWriter writer = new StringWriter();
        ctx.render(apiErrorsSources, writer);
        Assertions.assertEquals(
                "import type {ExportedSavePath} from './model/static/';\n" +
                        "\n" +
                        "export type AllErrors = {\n" +
                        "        family: 'SAVE_COMMAND', \n" +
                        "        code: 'NULL_TARGET', \n" +
                        "        exportedPath: ExportedSavePath\n" +
                        "    } | {\n" +
                        "        family: 'SAVE_COMMAND', \n" +
                        "        code: 'ILLEGAL_TARGET_ID', \n" +
                        "        exportedPath: ExportedSavePath\n" +
                        "    } | {\n" +
                        "        family: 'SAVE_COMMAND', \n" +
                        "        code: 'CANNOT_DISSOCIATE_TARGETS', \n" +
                        "        exportedPath: ExportedSavePath\n" +
                        "    } | {\n" +
                        "        family: 'SAVE_COMMAND', \n" +
                        "        code: 'NO_ID_GENERATOR', \n" +
                        "        exportedPath: ExportedSavePath\n" +
                        "    } | {\n" +
                        "        family: 'SAVE_COMMAND', \n" +
                        "        code: 'ILLEGAL_ID_GENERATOR', \n" +
                        "        exportedPath: ExportedSavePath\n" +
                        "    } | {\n" +
                        "        family: 'SAVE_COMMAND', \n" +
                        "        code: 'ILLEGAL_GENERATED_ID', \n" +
                        "        exportedPath: ExportedSavePath\n" +
                        "    } | {\n" +
                        "        family: 'SAVE_COMMAND', \n" +
                        "        code: 'EMPTY_OBJECT', \n" +
                        "        exportedPath: ExportedSavePath\n" +
                        "    } | {\n" +
                        "        family: 'SAVE_COMMAND', \n" +
                        "        code: 'NO_KEY_PROPS', \n" +
                        "        exportedPath: ExportedSavePath\n" +
                        "    } | {\n" +
                        "        family: 'SAVE_COMMAND', \n" +
                        "        code: 'NO_NON_ID_PROPS', \n" +
                        "        exportedPath: ExportedSavePath\n" +
                        "    } | {\n" +
                        "        family: 'SAVE_COMMAND', \n" +
                        "        code: 'NO_VERSION', \n" +
                        "        exportedPath: ExportedSavePath\n" +
                        "    } | {\n" +
                        "        family: 'SAVE_COMMAND', \n" +
                        "        code: 'OPTIMISTIC_LOCK_ERROR', \n" +
                        "        exportedPath: ExportedSavePath\n" +
                        "    } | {\n" +
                        "        family: 'SAVE_COMMAND', \n" +
                        "        code: 'KEY_NOT_UNIQUE', \n" +
                        "        exportedPath: ExportedSavePath\n" +
                        "    } | {\n" +
                        "        family: 'SAVE_COMMAND', \n" +
                        "        code: 'NEITHER_ID_NOR_KEY', \n" +
                        "        exportedPath: ExportedSavePath\n" +
                        "    } | {\n" +
                        "        family: 'SAVE_COMMAND', \n" +
                        "        code: 'REVERSED_REMOTE_ASSOCIATION', \n" +
                        "        exportedPath: ExportedSavePath\n" +
                        "    } | {\n" +
                        "        family: 'SAVE_COMMAND', \n" +
                        "        code: 'LONG_REMOTE_ASSOCIATION', \n" +
                        "        exportedPath: ExportedSavePath\n" +
                        "    } | {\n" +
                        "        family: 'SAVE_COMMAND', \n" +
                        "        code: 'FAILED_REMOTE_VALIDATION', \n" +
                        "        exportedPath: ExportedSavePath\n" +
                        "    } | {\n" +
                        "        family: 'SAVE_COMMAND', \n" +
                        "        code: 'UNSTRUCTURED_ASSOCIATION', \n" +
                        "        exportedPath: ExportedSavePath\n" +
                        "    };\n" +
                        "export type ApiErrors = {\n" +
                        "    'bookService': {\n" +
                        "        'saveBook': AllErrors & ({\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'NULL_TARGET', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'ILLEGAL_TARGET_ID', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'CANNOT_DISSOCIATE_TARGETS', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'NO_ID_GENERATOR', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'ILLEGAL_ID_GENERATOR', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'ILLEGAL_GENERATED_ID', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'EMPTY_OBJECT', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'NO_KEY_PROPS', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'NO_NON_ID_PROPS', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'NO_VERSION', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'OPTIMISTIC_LOCK_ERROR', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'KEY_NOT_UNIQUE', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'NEITHER_ID_NOR_KEY', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'REVERSED_REMOTE_ASSOCIATION', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'LONG_REMOTE_ASSOCIATION', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'FAILED_REMOTE_VALIDATION', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'UNSTRUCTURED_ASSOCIATION', \n" +
                        "                readonly [key:string]: any\n" +
                        "            }), \n" +
                        "        'updateBook': AllErrors & ({\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'NULL_TARGET', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'ILLEGAL_TARGET_ID', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'CANNOT_DISSOCIATE_TARGETS', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'NO_ID_GENERATOR', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'ILLEGAL_ID_GENERATOR', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'ILLEGAL_GENERATED_ID', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'EMPTY_OBJECT', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'NO_KEY_PROPS', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'NO_NON_ID_PROPS', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'NO_VERSION', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'OPTIMISTIC_LOCK_ERROR', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'KEY_NOT_UNIQUE', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'NEITHER_ID_NOR_KEY', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'REVERSED_REMOTE_ASSOCIATION', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'LONG_REMOTE_ASSOCIATION', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'FAILED_REMOTE_VALIDATION', \n" +
                        "                readonly [key:string]: any\n" +
                        "            } | {\n" +
                        "                family: 'SAVE_COMMAND', \n" +
                        "                code: 'UNSTRUCTURED_ASSOCIATION', \n" +
                        "                readonly [key:string]: any\n" +
                        "            })\n" +
                        "    }, \n" +
                        "    'treeService': {\n" +
                        "    }\n" +
                        "};\n",
                writer.toString()
        );
    }

    @Test
    public void testExportedSavePath() {
        Context ctx = new TypeScriptContext(METADATA);
        Source apiErrorsSources = ctx.getRootSource("model/static/ExportedSavePath");
        StringWriter writer = new StringWriter();
        ctx.render(apiErrorsSources, writer);
        Assertions.assertEquals(
                "import type {ExportedSavePath_Node} from './';\n" +
                        "\n" +
                        "export interface ExportedSavePath {\n" +
                        "    readonly rootTypeName: string;\n" +
                        "    readonly nodes: ReadonlyArray<ExportedSavePath_Node>;\n" +
                        "}\n",
                writer.toString()
        );
    }

    @Test
    public void testExportedSavePathNode() {
        Context ctx = new TypeScriptContext(METADATA);
        Source apiErrorsSources = ctx.getRootSource("model/static/ExportedSavePath_Node");
        StringWriter writer = new StringWriter();
        ctx.render(apiErrorsSources, writer);
        Assertions.assertEquals(
                "export interface ExportedSavePath_Node {\n" +
                        "    readonly prop: string;\n" +
                        "    readonly targetTypeName: string;\n" +
                        "}\n",
                writer.toString()
        );
    }
}
