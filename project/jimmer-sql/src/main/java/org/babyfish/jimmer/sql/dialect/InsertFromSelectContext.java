package org.babyfish.jimmer.sql.dialect;

import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.Nullable;

/**
 * Semantic rendering context for a native insert/upsert-from-select statement.
 *
 * <p>The dialect owns the SQL grammar. This context owns mutation metadata and
 * AST rendering, so dialect implementations do not depend on mutation internals.</p>
 */
public interface InsertFromSelectContext {

    InsertFromSelectMode getMode();

    boolean hasReturning();

    boolean hasUpdateAssignments();

    boolean hasUpdatePredicates();

    /**
     * Whether key-based conflict detection is restricted to active rows by a
     * logical-delete predicate.
     */
    boolean hasConflictPredicate();

    /**
     * Whether every source expression used by the update branch can be
     * represented by the corresponding inserted column.
     */
    boolean isUpdateExpressionAliasingSupported();

    /**
     * Whether the update branch consists only of direct assignments from the
     * inserted values and has no update predicate.
     */
    boolean isSimpleInsertedValueUpdate();

    /**
     * Whether reacting to any unique-constraint conflict is equivalent to
     * reacting to the explicitly selected conflict target.
     */
    boolean isConflictTargetUnambiguous();

    /**
     * Whether the selected conflict target is safe for native conflict
     * detection when one or more of its properties are nullable.
     *
     * <p>This is always {@code true} for a non-null conflict target. For a
     * nullable key it is true only when the model declares nulls to be not
     * distinct and the current dialect supports that conflict semantics.</p>
     */
    boolean isNullableConflictTargetSupported();

    InsertFromSelectContext sql(String sql);

    InsertFromSelectContext enter(AbstractSqlBuilder.ScopeType type);

    InsertFromSelectContext separator();

    InsertFromSelectContext leave();

    InsertFromSelectContext appendTableName();

    InsertFromSelectContext appendTargetAlias();

    InsertFromSelectContext appendInsertColumns();

    InsertFromSelectContext appendConflictColumns();

    /**
     * Append the active-row predicate for a conditional logical-delete key.
     * When {@code targetAlias} is true, the target-table alias is rendered for
     * merge-style SQL; it is omitted in an {@code ON CONFLICT} target predicate.
     */
    InsertFromSelectContext appendConflictPredicate(boolean targetAlias);

    InsertFromSelectContext appendSourceSelect();

    InsertFromSelectContext appendSourceTable();

    InsertFromSelectContext appendConflictCondition();

    InsertFromSelectContext appendInsertValues();

    /**
     * Append update assignments. When {@code insertedValuePrefix} is non-null,
     * source expressions are replaced by the corresponding inserted column,
     * wrapped by the supplied prefix and suffix.
     */
    InsertFromSelectContext appendUpdateAssignments(
            boolean targetAlias,
            @Nullable String insertedValuePrefix,
            @Nullable String insertedValueSuffix
    );

    InsertFromSelectContext appendFakeUpdateAssignment(
            boolean leftTargetAlias,
            boolean rightTargetAlias
    );

    InsertFromSelectContext appendUpdatePredicates(
            @Nullable String insertedValuePrefix,
            @Nullable String insertedValueSuffix
    );

    InsertFromSelectContext appendReturning();
}
