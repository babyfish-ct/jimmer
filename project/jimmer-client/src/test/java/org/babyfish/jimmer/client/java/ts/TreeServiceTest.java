package org.babyfish.jimmer.client.java.ts;

import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.ts.TypeScriptContext;
import org.babyfish.jimmer.client.java.common.OperationParserImpl;
import org.babyfish.jimmer.client.java.common.ParameterParserImpl;
import org.babyfish.jimmer.client.java.model.Tree;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.runtime.ObjectType;
import org.babyfish.jimmer.client.source.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Collections;

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
    public void testTree() {
        ObjectType treeType = METADATA
                .getStaticTypes()
                .stream()
                .filter(it -> it.getJavaType() == Tree.class)
                .findFirst()
                .orElse(null);
        Context ctx = new TypeScriptContext(METADATA, "    ", false);
        Source treeSource = ctx.getRootSources().stream().filter(it -> it.getName().equals("Tree")).findFirst().get();
        StringWriter writer = new StringWriter();
        ctx.render(treeSource, writer);
        Assertions.assertEquals(
                "",
                writer.toString()
        );
    }

    @Test
    public void testTreeNode() {
        ObjectType treeType = METADATA
                .getStaticTypes()
                .stream()
                .filter(it -> it.getJavaType() == Tree.class)
                .findFirst()
                .orElse(null);
        Context ctx = new TypeScriptContext(METADATA, "    ", false);
        Source treeNodeDtoSource = ctx.getRootSources().stream().filter(it -> it.getName().equals("TreeNodeDto")).findFirst().get();
        StringWriter writer = new StringWriter();
        ctx.render(treeNodeDtoSource, writer);
        Assertions.assertEquals(
                "",
                writer.toString()
        );
    }
}
