package org.babyfish.jimmer.client.java.ts;

import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.runtime.Operation;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;

public class TreeServiceTest {

    private static final Metadata.OperationParser OPERATION_PARSER = new Metadata.OperationParser() {
        @Override
        public String uri(AnnotatedElement element) {
            return null;
        }

        @Override
        public Operation.HttpMethod http(Method method) {
            return null;
        }
    };

    private static final Metadata.ParameterParser PARAMETER_PARSER = new Metadata.ParameterParser() {
        @Nullable
        @Override
        public String requestParam(Parameter javaParameter) {
            return null;
        }

        @Override
        public boolean isDefault(Parameter javaParameter) {
            return false;
        }

        @Nullable
        @Override
        public String pathVariable(Parameter javaParameter) {
            return null;
        }

        @Override
        public boolean isRequestBody(Parameter javaParameter) {
            return false;
        }
    };

    @Test
    public void testWithoutGenericSupporting() {
        Metadata metadata = Metadata
                .newBuilder()
                .setOperationParser(OPERATION_PARSER)
                .setParameterParameter(PARAMETER_PARSER)
                .setGroups(Collections.singleton("treeService"))
                .build();
        assertContent(
                "[" +
                        "--->org.babyfish.jimmer.client.java.model.TreeNode{" +
                        "--->--->id: long, " +
                        "--->--->name: java.lang.String, " +
                        "--->--->childNodes: list<" +
                        "--->--->--->org.babyfish.jimmer.client.java.model.TreeNode{" +
                        "--->--->--->--->id: long, " +
                        "--->--->--->--->name: java.lang.String, " +
                        "--->--->--->--->childNodes: ..." +
                        "--->--->--->}" +
                        "--->--->>" +
                        "--->}" +
                        "]",
                metadata.getFetchedTypes()
        );
        assertContent(
                "[" +
                        "--->org.babyfish.jimmer.client.java.model.Tree<int> {" +
                        "--->--->data: int, " +
                        "--->--->children: list<...>" +
                        "--->}, " +
                        "--->org.babyfish.jimmer.client.java.model.Tree<java.lang.String> {" +
                        "--->--->data: java.lang.String, " +
                        "--->--->children: list<...>" +
                        "--->}" +
                        "]",
                metadata.getStaticTypes()
        );
    }

    @Test
    public void testWithGenericSupporting() {
        Metadata metadata = Metadata
                .newBuilder()
                .setOperationParser(OPERATION_PARSER)
                .setParameterParameter(PARAMETER_PARSER)
                .setGenericSupported(true)
                .setGroups(Collections.singleton("treeService"))
                .build();
        assertContent(
                "[" +
                        "--->org.babyfish.jimmer.client.java.model.TreeNode{" +
                        "--->--->id: long, " +
                        "--->--->name: java.lang.String, " +
                        "--->--->childNodes: list<" +
                        "--->--->--->org.babyfish.jimmer.client.java.model.TreeNode{" +
                        "--->--->--->--->id: long, " +
                        "--->--->--->--->name: java.lang.String, " +
                        "--->--->--->--->childNodes: ..." +
                        "--->--->--->}" +
                        "--->--->>" +
                        "--->}" +
                        "]",
                metadata.getFetchedTypes()
        );
        assertContent(
                "[org.babyfish.jimmer.client.java.model.Tree{" +
                        "--->data: <org.babyfish.jimmer.client.java.model.Tree::T>, " +
                        "--->children: list<...>}" +
                        "]",
                metadata.getStaticTypes()
        );
    }

    private static void assertContent(String expect, Object actual) {
        Assertions.assertEquals(
                expect.replace("--->", ""),
                actual.toString()
        );
    }
}
