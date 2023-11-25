package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Schema;

import java.util.LinkedList;
import java.util.function.Consumer;

public abstract class SchemaBuilder<S> {

    private LinkedList<AstNode<S>> stack = new LinkedList<>();

    public SchemaBuilder() {
        stack.push(new SchemaImpl<>());
    }

    @SuppressWarnings("unchecked")
    public <X extends AstNode<S>> X ancestor(Class<?> type) {
        for (AstNode<S> node : stack) {
            if (node.getClass() == type) {
                return (X) node;
            }
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    @SafeVarargs
    public final S ancestorSource(Class<? extends AstNode>... types) {
        for (AstNode<S> node : stack) {
            if (node.getSource() != null) {
                if (types.length == 0) {
                    return node.getSource();
                }
                for (Class<?> type : types) {
                    if (node.getClass() == type) {
                        return node.getSource();
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <X extends AstNode<S>> X current() {
        return (X) stack.peek();
    }

    public void api(S source, String className, Consumer<ApiServiceImpl<S>> block) {
        run(new ApiServiceImpl<>(source, className), block);
    }

    public void operation(S source, String name, Consumer<ApiOperationImpl<S>> block) {
        run(new ApiOperationImpl<>(source, name), block);
    }

    public void parameter(S source, String name, Consumer<ParameterImpl<S>> block) {
        run(new ParameterImpl<>(source, name), block);
    }

    public void typeRef(Consumer<TypeRefImpl<S>> block) {
        run(new TypeRefImpl<>(), block);
    }

    public void definition(S source, String className, Consumer<TypeDefinitionImpl<S>> block) {
        run(new TypeDefinitionImpl<>(source, className), block);
    }

    public void prop(S source, String name, Consumer<PropImpl<S>> block) {
        run(new PropImpl<>(source, name), block);
    }

    private <X extends AstNode<S>> void run(X child, Consumer<X> block) {
        stack.push(child);
        try {
            block.accept(child);
        } finally {
            stack.pop();
        }
    }

    public Schema build() {
        resolve();
        return (Schema) stack.peek();
    }

    protected abstract S loadSource(String typeName);

    protected abstract void handleDefinition(S source);

    private void resolve() {
        AstNode<S> current = stack.peek();
        assert current != null;
        TypeDefinitionVisitor<S> visitor = new TypeDefinitionVisitor<>(this);
        current.accept(visitor);
        visitor.clearUnusedDefinitions();
    }
}
