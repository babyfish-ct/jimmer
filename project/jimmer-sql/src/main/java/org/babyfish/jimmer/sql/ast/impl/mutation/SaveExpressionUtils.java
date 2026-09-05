package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.InheritanceInfo;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.table.spi.UntypedJoinDisabledTableProxy;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class SaveExpressionUtils {

    private SaveExpressionUtils() {
    }

    static Table<?> table(JSqlClientImplementor sqlClient, ImmutableType type, String disabledJoinReason) {
        MutableRootQueryImpl<?> query = new MutableRootQueryImpl<>(sqlClient, type, ExecutionPurpose.MUTATE, FilterLevel.DEFAULT);
        Table<?> table = query.getTable();
        if (table instanceof TableImplementor<?>) {
            return new UntypedJoinDisabledTableProxy<>((TableImplementor<?>) table, disabledJoinReason);
        }
        return ((TableProxy<?>) table).__disableJoin(disabledJoinReason);
    }

    static ImmutableType physicalTableType(ImmutableType type) {
        InheritanceInfo info = type.getInheritanceInfo();
        return info != null && info.getStrategy() == InheritanceType.SINGLE_TABLE ? info.getRootType() : type;
    }

    static boolean belongsToTable(ImmutableProp prop, ImmutableType tableType) {
        ImmutableType declaringType = prop.getDeclaringType();
        return declaringType == tableType || physicalTableType(declaringType) == tableType;
    }

    static @Nullable PropertyGetter singleColumnGetter(JSqlClientImplementor sqlClient, ImmutableProp prop) {
        List<PropertyGetter> getters = PropertyGetter.propertyGetters(sqlClient, prop);
        return getters.size() == 1 && getters.get(0).metadata().getColumnName() != null ? getters.get(0) : null;
    }

    static List<PropertyGetter> validate(
            Ast ast,
            JSqlClientImplementor sqlClient,
            ImmutableType tableType,
            Shape shape,
            String expressionDescription,
            String referenceDescription
    ) {
        List<PropertyGetter> inputGetters = new ArrayList<>();
        ast.accept(new AstVisitor(new AstContext(sqlClient)) {

            @Override
            public void visitTableReference(RealTable table, @Nullable ImmutableProp prop, boolean rawId) {
                if (prop == null || !belongsToTable(prop, tableType) || !prop.isColumnDefinition()) {
                    throw new IllegalArgumentException(expressionDescription + " can only read local physical target properties");
                }
            }

            @Override
            public void visitSaveInputValue(ImmutableProp prop) {
                PropertyGetter getter = singleColumnGetter(sqlClient, prop);
                if (getter == null || !belongsToTable(prop, tableType)) {
                    throw new IllegalArgumentException(expressionDescription + " can only read local physical scalar input properties");
                }
                if (!shape.contains(getter)) {
                    throw new IllegalArgumentException(
                            "The input property \"" + prop + "\" referenced by " + referenceDescription + " is unloaded"
                    );
                }
                if (!inputGetters.contains(getter)) {
                    inputGetters.add(getter);
                }
            }
        });
        return inputGetters;
    }
}
