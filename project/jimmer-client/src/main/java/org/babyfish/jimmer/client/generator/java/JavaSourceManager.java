package org.babyfish.jimmer.client.generator.java;

import org.babyfish.jimmer.client.runtime.EnumType;
import org.babyfish.jimmer.client.runtime.ObjectType;
import org.babyfish.jimmer.client.runtime.Operation;
import org.babyfish.jimmer.client.runtime.Service;
import org.babyfish.jimmer.client.source.Source;
import org.babyfish.jimmer.client.source.SourceManager;

import java.util.UUID;

public class JavaSourceManager extends SourceManager {

    private final String rootDir;

    protected JavaSourceManager(JavaContext ctx) {
        super(ctx);
        rootDir = ctx.getPackageName().replace('.', '/');
    }

    @Override
    protected Source createServiceSource(Service service) {
        return createRootSource(
                rootDir + "/service",
                service.getJavaType().getSimpleName(),
                () -> new ServiceRender(service)
        );
    }

    @Override
    protected Source createServiceImplSource(Service service) {
        return createRootSource(
                rootDir + "/service/impl",
                service.getJavaType().getSimpleName() + "Impl",
                () -> new ServiceRender(service)
        );
    }

    @Override
    protected Source createOperationSource(Operation operation) {
        return getSource(operation.getDeclaringService())
                .subSource(
                        operation.getName() + '_' + UUID.randomUUID(),
                        () -> new OperationRender(operation)
                );
    }

    @Override
    protected Source createStaticTypeSource(ObjectType objectType) {
        return createRootSource(
                rootDir + "/model/static",
                objectType.getJavaType().getSimpleName(),
                () -> new ObjectTypeRender(objectType, objectType.getJavaType().getSimpleName(), false)
        );
    }

    @Override
    protected Source createFetchedTypeSource(ObjectType objectType) {
        return createRootSource(
                rootDir + "/model/dto",
                objectType.getJavaType().getSimpleName() +
                        '_' +
                        objectType.getFetchByInfo().getOwnerType().getSimpleName() +
                        '_' +
                        objectType.getFetchByInfo().getConstant(),
                () -> new ObjectTypeRender(objectType, objectType.getJavaType().getSimpleName(), false)
        );
    }

    @Override
    protected Source createDynamicTypeSource(ObjectType objectType) {
        return createRootSource(
                rootDir + "/model/dynamic",
                "Dynamic" + objectType.getJavaType().getSimpleName(),
                () -> new ObjectTypeRender(objectType, objectType.getJavaType().getSimpleName(), true)
        );
    }

    @Override
    protected Source createEmbeddableTypeSource(ObjectType objectType) {
        return createRootSource(
                rootDir + "/model/embeddable",
                objectType.getJavaType().getSimpleName(),
                () -> new ObjectTypeRender(objectType, objectType.getJavaType().getSimpleName(), false)
        );
    }

    @Override
    protected Source createEnumTypeSource(EnumType enumType) {
        return createRootSource(
                rootDir + "/model/constant",
                enumType.getJavaType().getSimpleName(),
                () -> new EnumTypeRender(enumType)
        );
    }
}
