package org.babyfish.jimmer.client.java.ts;

import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.ts.TypeScriptContext;
import org.babyfish.jimmer.client.common.OperationParserImpl;
import org.babyfish.jimmer.client.common.ParameterParserImpl;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Arrays;

public class IndexTest {

    private static final Metadata METADATA =
            Metadata
                    .newBuilder()
                    .setOperationParser(new OperationParserImpl())
                    .setParameterParameter(new ParameterParserImpl())
                    .setGroups(Arrays.asList("bookService", "treeService"))
                    .setGenericSupported(true)
                    .build();

    @Test
    public void testRoot() {
        Context ctx = new TypeScriptContext(METADATA);
        StringWriter writer = new StringWriter();
        ctx.renderIndex("", writer);
        Assertions.assertEquals(
                "export {Api} from './Api';\n" +
                        "export type {AllErrors, ApiErrors} from './ApiErrors';\n" +
                        "export type {ElementOf} from './ElementOf';\n" +
                        "export type {Executor} from './Executor';\n" +
                        "export type {RequestOf} from './RequestOf';\n" +
                        "export type {ResponseOf} from './ResponseOf';\n",
                writer.toString()
        );
    }

    @Test
    public void testServices() {
        Context ctx = new TypeScriptContext(METADATA);
        StringWriter writer = new StringWriter();
        ctx.renderIndex("services", writer);
        Assertions.assertEquals(
                "export {BookService} from './BookService';\n" +
                        "export {TreeService} from './TreeService';\n",
                writer.toString()
        );
    }

    @Test
    public void testModelDto() {
        Context ctx = new TypeScriptContext(METADATA);
        StringWriter writer = new StringWriter();
        ctx.renderIndex("model/dto", writer);
        Assertions.assertEquals(
                "export type {AuthorDto} from './AuthorDto';\n" +
                        "export type {BookDto} from './BookDto';\n" +
                        "export type {TreeNodeDto} from './TreeNodeDto';\n",
                writer.toString()
        );
    }

    @Test
    public void testModelDynamic() {
        Context ctx = new TypeScriptContext(METADATA);
        StringWriter writer = new StringWriter();
        ctx.renderIndex("model/dynamic", writer);
        Assertions.assertEquals(
                "export type {Dynamic_Author} from './Dynamic_Author';\n" +
                        "export type {Dynamic_Book} from './Dynamic_Book';\n" +
                        "export type {Dynamic_BookStore} from './Dynamic_BookStore';\n",
                writer.toString()
        );
    }

    @Test
    public void testModelStatic() {
        Context ctx = new TypeScriptContext(METADATA);
        StringWriter writer = new StringWriter();
        ctx.renderIndex("model/static", writer);
        Assertions.assertEquals(
                "export type {BookInput} from './BookInput';\n" +
                        "export type {ExportedSavePath} from './ExportedSavePath';\n" +
                        "export type {ExportedSavePath_Node} from './ExportedSavePath_Node';\n" +
                        "export type {FindBookArguments} from './FindBookArguments';\n" +
                        "export type {Page} from './Page';\n" +
                        "export type {SimpleTreeNodeView} from './SimpleTreeNodeView';\n" +
                        "export type {Tree} from './Tree';\n" +
                        "export type {Tuple2} from './Tuple2';\n",
                writer.toString()
        );
    }
}
