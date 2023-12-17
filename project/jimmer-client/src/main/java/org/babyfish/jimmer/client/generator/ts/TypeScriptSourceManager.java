package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.Namespace;
import org.babyfish.jimmer.client.runtime.*;
import org.babyfish.jimmer.client.source.Source;
import org.babyfish.jimmer.client.source.SourceManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TypeScriptSourceManager extends SourceManager {

    private final Namespace namespace = new Namespace();

    private final Map<ObjectType, Source> dtoWrapperSourceMap = new HashMap<>();

    protected TypeScriptSourceManager(Context ctx) {
        super(ctx);
    }

    @Override
    protected Source createServiceSource(Service service) {
        String name = namespace.allocate(service.getJavaType().getSimpleName());
        return createRootSource("services", name, () -> new ServiceRender(name, service));
    }

    @Override
    protected Source createOperationSource(Operation operation) {
        Source source = getSource(operation.getDeclaringService());
        String name = namespace.subNamespace(source.getName()).allocate(operation.getName());
        return source.subSource(name, () -> new OperationRender(name, operation));
    }

    @Override
    protected Source createStaticTypeSource(ObjectType objectType) {
        String name = namespace.allocate(String.join("_", objectType.getSimpleNames()));
        return createRootSource("model/static", name, () -> new StaticTypeRender(name, objectType));
    }

    @Override
    protected Source createFetchedTypeSource(ObjectType objectType) {
        Source dtoWrapperSource = dtoWrapperSourceMap.get(objectType);
        if (dtoWrapperSource == null) {
            String name = namespace.allocate(String.join("_", objectType.getSimpleNames()) + "Dto");
            dtoWrapperSource = createRootSource("model/dto", name, () -> new DtoWrapperRender(name));
            dtoWrapperSourceMap.put(objectType, dtoWrapperSource);
        }
        FetchByInfo info = objectType.getFetchByInfo();
        assert info != null;
        String subName = info.getOwnerType().getSimpleName() + '/' + info.getConstant();
        Map<Type, String> recursiveTypeNames = ((DtoWrapperRender)dtoWrapperSource.getRender()).recursiveTypeNames;
        return dtoWrapperSource.subSource(
                subName,
                () -> new FetchedTypeRender(subName, objectType, recursiveTypeNames)
        );
    }

    @Override
    protected Source createDynamicTypeSource(ObjectType objectType) {
        String name = namespace.allocate("Dynamic_" + String.join("_", objectType.getSimpleNames()));
        return createRootSource("model/dynamic", name, () -> new DynamicTypeRender(name, objectType));
    }

    @Override
    protected Source createEnumTypeSource(EnumType enumType) {
        String name = namespace.allocate(String.join("_", enumType.getJavaType().getSimpleName()));
        return createRootSource("model/enums", name, () -> new EnumTypeRender(name));
    }

    @Override
    protected Source createErrorTypeSource(ObjectType objectType) {
        return null;
    }

    @Override
    public void createAdditionalSources() {
        TypeScriptContext ctx = getContext();
        createRootSource("", "Executor", ExecutorRender::new);
        createRootSource("", ctx.getApiName(), () -> new ApiRender(ctx.getApiName(), ctx.getMetadata().getServices()));
        createRootSource(
                "",
                ctx.getApiName() + "Errors",
                () -> new ApiErrorsRender(ctx.getApiName() + "Errors", ctx.getMetadata().getServices())
        );
    }
}
