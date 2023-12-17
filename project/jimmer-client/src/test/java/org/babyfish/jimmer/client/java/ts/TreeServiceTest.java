package org.babyfish.jimmer.client.java.ts;

import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.ts.TypeScriptContext;
import org.babyfish.jimmer.client.java.common.OperationParserImpl;
import org.babyfish.jimmer.client.java.common.ParameterParserImpl;
import org.babyfish.jimmer.client.java.model.Tree;
import org.babyfish.jimmer.client.java.service.TreeService;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.runtime.ObjectType;
import org.babyfish.jimmer.client.runtime.Service;
import org.babyfish.jimmer.client.source.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Collections;
import java.util.stream.Collectors;

public class TreeServiceTest {

    private static final Metadata METADATA =
            Metadata
                    .newBuilder()
                    .setOperationParser(new OperationParserImpl())
                    .setParameterParameter(new ParameterParserImpl())
                    .setGroups(Collections.singleton("treeService"))
                    .setGenericSupported(true)
                    .build();

    @Test
    public void testApi() {
        Context ctx = new TypeScriptContext(METADATA);
        Source serviceSource = ctx.getRootSource("Api");
        StringWriter writer = new StringWriter();
        ctx.render(serviceSource, writer);
        Assertions.assertEquals(
                "",
                writer.toString()
        );
    }

    @Test
    public void testService() {
        Context ctx = new TypeScriptContext(METADATA);
        Source serviceSource = ctx.getRootSource("services/" + TreeService.class.getSimpleName());
        StringWriter writer = new StringWriter();
        ctx.render(serviceSource, writer);
        Assertions.assertEquals(
                "",
                writer.toString()
        );
    }

    @Test
    public void testTree() {
        Context ctx = new TypeScriptContext(METADATA);
        Source treeSource = ctx.getRootSource("model/static/Tree");
        StringWriter writer = new StringWriter();
        ctx.render(treeSource, writer);
        Assertions.assertEquals(
                "",
                writer.toString()
        );
    }

    @Test
    public void testTreeNode() {
        Context ctx = new TypeScriptContext(METADATA);
        Source treeNodeDtoSource = ctx.getRootSource("model/dto/TreeNodeDto");
        StringWriter writer = new StringWriter();
        ctx.render(treeNodeDtoSource, writer);
        Assertions.assertEquals(
                "",
                writer.toString()
        );
    }
}
