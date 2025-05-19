package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.mapper.TypedTupleMapper;
import org.jetbrains.annotations.NotNull;

public class MapperSelectionImpl<T> implements MapperSelection<T>, Ast {

    private final TypedTupleMapper<T> mapper;

    public MapperSelectionImpl(TypedTupleMapper<T> mapper) {
        this.mapper = mapper;
    }

    @Override
    public TypedTupleMapper<T> getMapper() {
        return mapper;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        TypedTupleMapper<?> mapper = this.mapper;
        int size = mapper.size();
        for (int i = 0; i < size; i++) {
            Ast.from(mapper.get(i), visitor.getAstContext()).accept(visitor);
        }
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.enter(AbstractSqlBuilder.ScopeType.COMMA);
        TypedTupleMapper<?> mapper = this.mapper;
        int size = mapper.size();
        for (int i = 0; i < size; i++) {
            builder.separator();
            Ast.from(mapper.get(i), builder.assertSimple().getAstContext()).renderTo(builder);
        }
        builder.leave();
    }

    @Override
    public boolean hasVirtualPredicate() {
        return false;
    }

    @Override
    public Ast resolveVirtualPredicate(AstContext ctx) {
        return this;
    }
}
