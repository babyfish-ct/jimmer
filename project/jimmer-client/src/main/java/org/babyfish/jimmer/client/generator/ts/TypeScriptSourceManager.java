package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.Namespace;
import org.babyfish.jimmer.client.runtime.*;
import org.babyfish.jimmer.client.source.Source;
import org.babyfish.jimmer.client.source.SourceManager;

public class TypeScriptSourceManager extends SourceManager {

    private final Namespace staticNamespace = new Namespace();

    private final Namespace fetchedNamespace = new Namespace();

    private final Namespace dynamicNamespace = new Namespace();

    private final Namespace enumNamespace = new Namespace();

    protected TypeScriptSourceManager(Metadata metadata, boolean isNewestTypeSupported) {
        super(metadata, isNewestTypeSupported);
    }

    @Override
    protected Source createStaticTypeSource(ObjectType objectType) {
        String name = staticNamespace.allocate(String.join("_", objectType.getSimpleNames()));
        return createRootSource("model/static", name, () -> new StaticTypeRender(name, objectType));
    }

    @Override
    protected Source createFetchedTypeSource(ObjectType objectType) {
        String name = fetchedNamespace.allocate(String.join("_", objectType.getSimpleNames()) + "Dto");
        FetchByInfo info = objectType.getFetchByInfo();
        assert info != null;
        String subName = fetchedNamespace
                .subNamespace(name)
                .allocate(info.getOwnerType().getSimpleName() + '/' + info.getConstant());
        return createRootSource("model/dto", name, () -> new DtoWrapperRender(name))
                .subSource(subName, () -> new FetchedTypeRender(subName, objectType));
    }

    @Override
    protected Source createDynamicTypeSource(ObjectType objectType) {
        String name = dynamicNamespace.allocate(String.join("_", objectType.getSimpleNames()));
        return createRootSource("model/entities", name, () -> new DynamicTypeRender(name, objectType));
    }

    @Override
    protected Source createEnumTypeSource(EnumType enumType) {
        String name = enumNamespace.allocate(String.join("_", enumType.getJavaType().getSimpleName()));
        return createRootSource("model/enums", name, () -> new EnumTypeRender(name));
    }

    @Override
    protected Source createErrorTypeSource(ObjectType objectType) {
        return null;
    }
}
