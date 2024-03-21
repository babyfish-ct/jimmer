package org.babyfish.jimmer.sql.embedded;

import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.Objects;
import org.babyfish.jimmer.sql.model.embedded.Transform;
import org.babyfish.jimmer.sql.runtime.DbLiteral;
import org.junit.jupiter.api.Test;

public class EmbeddedMutationTest extends AbstractMutationTest {

    @Test
    public void test() {
        Transform transform = Objects.createTransform(draft -> {
            draft.setId(3L);
            draft.applySource(source -> {
                source.applyLeftTop(leftTop -> {
                    leftTop.setX(1).setY(2);
                });
                source.applyRightBottom(rightBottom -> {
                    rightBottom.setX(3).setY(4);
                });
            });
            draft.setTarget(null);
        });
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(transform).setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into TRANSFORM(" +
                                        "--->ID, " +
                                        "--->`LEFT`, TOP, `RIGHT`, BOTTOM, " +
                                        "--->TARGET_LEFT, TARGET_TOP, TARGET_RIGHT, TARGET_BOTTOM" +
                                        ") values(?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                        it.variables(
                                3L,
                                1L,
                                2L,
                                3L,
                                4L,
                                new DbLiteral.DbNull(long.class),
                                new DbLiteral.DbNull(long.class),
                                new DbLiteral.DbNull(long.class),
                                new DbLiteral.DbNull(long.class)
                        );
                    });
                    ctx.totalRowCount(1);
                    ctx.entity(it -> {});
                }
        );
    }
}
