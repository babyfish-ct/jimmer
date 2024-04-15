package org.babyfish.jimmer.sql.embedded;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.Objects;
import org.babyfish.jimmer.sql.model.embedded.Machine;
import org.babyfish.jimmer.sql.model.embedded.Rect;
import org.babyfish.jimmer.sql.model.embedded.Transform;
import org.babyfish.jimmer.sql.model.embedded.TransformDraft;
import org.babyfish.jimmer.sql.model.embedded.dto.DynamicRectInput;
import org.babyfish.jimmer.sql.runtime.DbLiteral;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public class EmbeddedMutationTest extends AbstractMutationTest {

    @Test
    public void testNestedEmbedded() {
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

    @Test
    public void updatePartial() {
        Transform transform = Objects.createTransform(draft -> {
            draft.setId(1L);
            draft.applySource(source -> {
                source.applyLeftTop(leftTop -> {
                    leftTop.setX(1).setY(2);
                });
            });
            draft.applyTarget(target -> {
                target.applyRightBottom(rightBottom -> {
                    rightBottom.setX(3).setY(4);
                });
            });
        });
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(transform).setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update TRANSFORM " +
                                        "set `LEFT` = ?, TOP = ?, " +
                                        "TARGET_RIGHT = ?, TARGET_BOTTOM = ? " +
                                        "where ID = ?"
                        );
                        it.variables(1L, 2L, 3L, 4L, 1L);
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{" +
                                        "--->\"id\":1," +
                                        "--->\"source\":{\"leftTop\":{\"x\":1,\"y\":2}}," +
                                        "--->\"target\":{\"rightBottom\":{\"x\":3,\"y\":4}}" +
                                        "}"
                        );
                        it.modified(
                                "{" +
                                        "--->\"id\":1," +
                                        "--->\"source\":{\"leftTop\":{\"x\":1,\"y\":2}}," +
                                        "--->\"target\":{\"rightBottom\":{\"x\":3,\"y\":4}}" +
                                        "}"
                        );
                    });
                }
        );
    }

    @Test
    public void testIssue527() {
        String sourceJson = "{" +
                "    \"leftTop\": {\"x\": 1}, " +
                "    \"rightBottom\": {\"y\": 2} " +
                "}";
        String targetJson = "{" +
                "    \"leftTop\": {\"y\": 3}, " +
                "    \"rightBottom\": {\"x\": 4} " +
                "}";
        Transform transform = TransformDraft.$.produce(draft -> {
            draft.setId(1L);
            draft.setSource(
                    new ObjectMapper()
                            .readValue(sourceJson, DynamicRectInput.class)
                            .toImmutable()
            );
            draft.setTarget(
                    new ObjectMapper()
                            .readValue(targetJson, DynamicRectInput.class)
                            .toImmutable()
            );
        });
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(transform).setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update TRANSFORM " +
                                        "set `LEFT` = ?, BOTTOM = ?, TARGET_TOP = ?, TARGET_RIGHT = ? " +
                                        "where ID = ?"
                        );
                        it.variables(1L, 2L, 3L, 4L, 1L);
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "--->\"id\":1," +
                                        "--->\"source\":{\"leftTop\":{\"x\":1},\"rightBottom\":{\"y\":2}}," +
                                        "--->\"target\":{\"leftTop\":{\"y\":3},\"rightBottom\":{\"x\":4}}" +
                                        "}"
                        );
                    });
                }
        );
    }
}
