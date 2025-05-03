package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.*;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Element;
import java.security.Principal;
import java.util.*;
import java.util.function.Consumer;

public abstract class SchemaBuilder<S> {

    private static final Set<String> IGNORED_PARAMETER_TYPES;

    private LinkedList<AstNode<S>> stack = new LinkedList<>();

    private List<TypeName> thisModuleTypeNameList;

    public List<TypeName> getThisModuleTypeNameList() {
        return thisModuleTypeNameList;
    }

    public void setThisModuleTypeNameList(List<TypeName> thisModuleTypeNameList) {
        this.thisModuleTypeNameList = thisModuleTypeNameList;
    }

    @SuppressWarnings("unchecked")
    public SchemaBuilder(Schema original) {
        Map<TypeName, ApiServiceImpl<S>> serviceMap = null;
        if (original != null) {
            serviceMap = (Map<TypeName, ApiServiceImpl<S>>) (Map<?, ?>) original.getApiServiceMap();
            serviceMap = new TreeMap<>(serviceMap);
            serviceMap.keySet().removeIf(s -> loadSource(s.toString()) == null);
        }
        SchemaImpl<S> schema = new SchemaImpl<>(serviceMap);
        stack.push(schema);
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
                for (Class<?> otherType : types) {
                    if (node.getClass() == otherType) {
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

    @SuppressWarnings("unchecked")
    public <X extends AstNode<?>> X parent(Class<X> parentType) {
        if (stack.size() < 2) {
            return null;
        }
        AstNode<S> parent = stack.get(1);
        if (parentType.isAssignableFrom(parent.getClass())) {
            return (X) parent;
        }
        return null;
    }

    public void api(S source, TypeName typeName, Consumer<ApiServiceImpl<S>> block) {
        run(new ApiServiceImpl<>(source, typeName), block);
    }

    public void operation(S source, String name, Consumer<ApiOperationImpl<S>> block) {
        run(new ApiOperationImpl<>(source, name), block);
    }

    public void parameter(S source, String name, Consumer<ApiParameterImpl<S>> block) {
        run(new ApiParameterImpl<>(source, name), block);
    }

    public void typeRef(Consumer<TypeRefImpl<S>> block) {
        run(new TypeRefImpl<>(), block);
    }

    public void definition(S source, TypeName typeName, Consumer<TypeDefinitionImpl<S>> block) {
        run(new TypeDefinitionImpl<>(source, typeName), block);
    }

    public void prop(S source, String name, Consumer<PropImpl<S>> block) {
        run(new PropImpl<>(source, name), block);
    }

    public void constant(S source, String name, Consumer<EnumConstantImpl<S>> block) {
        run(new EnumConstantImpl<>(source, name), block);
    }

    private <X extends AstNode<S>> void run(X child, Consumer<X> block) {
        stack.push(child);
        try {
            block.accept(child);
        } finally {
            stack.pop();
        }
    }

    public void push(AstNode<S> child) {
        stack.push(child);
    }

    public void pop() {
        stack.pop();
    }

    public Schema build() {
        resolve();
        unnecessaryPart();
        return (Schema) stack.peek();
    }

    @Nullable
    protected abstract S loadSource(String typeName);

    protected abstract void throwException(S source, String message);

    protected abstract void fillDefinition(S source);

    private void resolve() {
        SchemaImpl<S> schema = current();
        schema.accept(new TypeDefinitionVisitor<>(this));
    }

    @SuppressWarnings("unchecked")
    private void unnecessaryPart() {
        SchemaImpl<S> schema = current();
        for (ApiService service : schema.getApiServiceMap().values()) {
            for (ApiOperation operation : service.getOperations()) {
                Collection<ApiParameterImpl<S>> parameters =
                        (List<ApiParameterImpl<S>>) (List<?>) operation.getParameters();
                parameters.removeIf(parameter -> {
                    if (IGNORED_PARAMETER_TYPES.contains(parameter.getType().getTypeName().toString())) {
                        return true;
                    }
                    TypeDefinition typeDefinition = schema.getTypeDefinitionMap().get(parameter.getType().getTypeName());
                    return typeDefinition != null && typeDefinition.isApiIgnore();
                });
            }
        }
    }

    static {

        Set<String> ignoredParameterTypes = new HashSet<>();
        ignoredParameterTypes.add("javax.servlet.http.HttpServletRequest");
        ignoredParameterTypes.add("javax.servlet.http.ServletRequest");
        ignoredParameterTypes.add("javax.servlet.http.HttpServletResponse");
        ignoredParameterTypes.add("javax.servlet.http.ServletResponse");
        ignoredParameterTypes.add("jakarta.servlet.http.HttpServletRequest");
        ignoredParameterTypes.add("jakarta.servlet.http.ServletRequest");
        ignoredParameterTypes.add("jakarta.servlet.http.HttpServletResponse");
        ignoredParameterTypes.add("jakarta.servlet.http.ServletResponse");
        ignoredParameterTypes.add(Principal.class.getName());
        IGNORED_PARAMETER_TYPES = ignoredParameterTypes;
    }
}
