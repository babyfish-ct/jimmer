package org.babyfish.jimmer.jackson.v3;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.DocDeserializerV3;
import org.babyfish.jimmer.client.meta.DocSerializerV3;
import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.meta.TypeNameDeserializerV3;
import org.babyfish.jimmer.client.meta.TypeNameSerializerV3;
import org.babyfish.jimmer.client.meta.impl.ApiOperationImpl;
import org.babyfish.jimmer.client.meta.impl.ApiOperationImplDeserializerV3;
import org.babyfish.jimmer.client.meta.impl.ApiOperationImplSerializerV3;
import org.babyfish.jimmer.client.meta.impl.ApiParameterImpl;
import org.babyfish.jimmer.client.meta.impl.ApiParameterImplDeserializerV3;
import org.babyfish.jimmer.client.meta.impl.ApiParameterImplSerializerV3;
import org.babyfish.jimmer.client.meta.impl.ApiServiceImpl;
import org.babyfish.jimmer.client.meta.impl.ApiServiceImplDeserializerV3;
import org.babyfish.jimmer.client.meta.impl.ApiServiceImplSerializerV3;
import org.babyfish.jimmer.client.meta.impl.EnumConstantImpl;
import org.babyfish.jimmer.client.meta.impl.EnumConstantImplDeserializerV3;
import org.babyfish.jimmer.client.meta.impl.EnumConstantImplSerializerV3;
import org.babyfish.jimmer.client.meta.impl.PropImpl;
import org.babyfish.jimmer.client.meta.impl.PropImplDeserializerV3;
import org.babyfish.jimmer.client.meta.impl.PropImplSerializerV3;
import org.babyfish.jimmer.client.meta.impl.SchemaImpl;
import org.babyfish.jimmer.client.meta.impl.SchemaImplDeserializerV3;
import org.babyfish.jimmer.client.meta.impl.SchemaImplSerializerV3;
import org.babyfish.jimmer.client.meta.impl.TypeDefinitionImpl;
import org.babyfish.jimmer.client.meta.impl.TypeDefinitionImplDeserializerV3;
import org.babyfish.jimmer.client.meta.impl.TypeDefinitionImplSerializerV3;
import org.babyfish.jimmer.client.meta.impl.TypeRefImpl;
import org.babyfish.jimmer.client.meta.impl.TypeRefImplDeserializerV3;
import org.babyfish.jimmer.client.meta.impl.TypeRefImplSerializerV3;
import tools.jackson.databind.module.SimpleModule;

public class ImmutableModuleV3 extends SimpleModule {

    public static final String MODULE_ID = ImmutableModuleV3.class.getName();

    public ImmutableModuleV3() {
        registerClientMetadata();
    }

    @Override
    public Object getRegistrationId() {
        return MODULE_ID;
    }

    @Override
    public String getModuleName() {
        return MODULE_ID;
    }

    @Override
    public void setupModule(SetupContext ctx) {
        super.setupModule(ctx);
        ctx.addSerializerModifier(new ImmutableSerializerModifierV3());
        ctx.insertAnnotationIntrospector(new ImmutableAnnotationIntrospectorV3());
        ctx.setMixIn(Page.class, PageMixinV3.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerClientMetadata() {
        addSerializer((Class) TypeName.class, new TypeNameSerializerV3());
        addDeserializer((Class) TypeName.class, new TypeNameDeserializerV3());
        addSerializer((Class) Doc.class, new DocSerializerV3());
        addDeserializer((Class) Doc.class, new DocDeserializerV3());
        addSerializer((Class) SchemaImpl.class, new SchemaImplSerializerV3());
        addDeserializer((Class) SchemaImpl.class, new SchemaImplDeserializerV3());
        addSerializer((Class) ApiServiceImpl.class, new ApiServiceImplSerializerV3());
        addDeserializer((Class) ApiServiceImpl.class, new ApiServiceImplDeserializerV3());
        addSerializer((Class) ApiOperationImpl.class, new ApiOperationImplSerializerV3());
        addDeserializer((Class) ApiOperationImpl.class, new ApiOperationImplDeserializerV3());
        addSerializer((Class) ApiParameterImpl.class, new ApiParameterImplSerializerV3());
        addDeserializer((Class) ApiParameterImpl.class, new ApiParameterImplDeserializerV3());
        addSerializer((Class) TypeRefImpl.class, new TypeRefImplSerializerV3());
        addDeserializer((Class) TypeRefImpl.class, new TypeRefImplDeserializerV3());
        addSerializer((Class) TypeDefinitionImpl.class, new TypeDefinitionImplSerializerV3());
        addDeserializer((Class) TypeDefinitionImpl.class, new TypeDefinitionImplDeserializerV3());
        addSerializer((Class) PropImpl.class, new PropImplSerializerV3());
        addDeserializer((Class) PropImpl.class, new PropImplDeserializerV3());
        addSerializer((Class) EnumConstantImpl.class, new EnumConstantImplSerializerV3());
        addDeserializer((Class) EnumConstantImpl.class, new EnumConstantImplDeserializerV3());
    }
}
