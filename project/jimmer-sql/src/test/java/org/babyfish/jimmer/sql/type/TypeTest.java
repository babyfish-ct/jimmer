package org.babyfish.jimmer.sql.type;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.type.*;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TypeTest extends AbstractQueryTest {

    @Test
    public void addScalarProviderForSuperProp() {
        ScalarProvider<?, ?> provider = new AnnotationsProvider();
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient(cfg -> {
            cfg.addScalarProvider(AnnotatedProps.ANNOTATIONS, provider);
        });
        Assertions.assertSame(
                provider,
                sqlClient.getScalarProvider(AnnotatedProps.ANNOTATIONS)
        );
        Assertions.assertSame(
                provider,
                sqlClient.getScalarProvider(InterfaceNodeProps.ANNOTATIONS)
        );
        Assertions.assertSame(
                provider,
                sqlClient.getScalarProvider(ClassNodeProps.ANNOTATIONS)
        );
    }

    @Test
    public void addScalarProviderFroDerivedProps() {
        ScalarProvider<?, ?> provider = new AnnotationsProvider();
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            getSqlClient(cfg -> {
                cfg.addScalarProvider(InterfaceNodeProps.ANNOTATIONS, provider);
            });
        });
        Assertions.assertEquals(
                "\"org.babyfish.jimmer.sql.model.type.InterfaceNode.annotations\" " +
                        "hides \"org.babyfish.jimmer.sql.model.type.Annotated.annotations\", " +
                        "please add scalar provider for that hidden property",
                ex.getMessage()
        );
    }

    private static class AnnotationsProvider extends ScalarProvider<List<AnnotationNode>, String> {

        @Override
        public @NotNull List<AnnotationNode> toScalar(@NotNull String sqlValue) throws Exception {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull String toSql(@NotNull List<AnnotationNode> scalarValue) throws Exception {
            throw new UnsupportedOperationException();
        }
    }
}
