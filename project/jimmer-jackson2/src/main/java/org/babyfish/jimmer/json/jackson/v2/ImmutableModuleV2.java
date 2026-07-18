package org.babyfish.jimmer.json.jackson.v2;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.DocDeserializerV2;
import org.babyfish.jimmer.client.meta.DocSerializerV2;
import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.meta.TypeNameDeserializerV2;
import org.babyfish.jimmer.client.meta.TypeNameSerializerV2;
import org.babyfish.jimmer.client.meta.impl.ApiOperationImpl;
import org.babyfish.jimmer.client.meta.impl.ApiOperationImplDeserializerV2;
import org.babyfish.jimmer.client.meta.impl.ApiOperationImplSerializerV2;
import org.babyfish.jimmer.client.meta.impl.ApiParameterImpl;
import org.babyfish.jimmer.client.meta.impl.ApiParameterImplDeserializerV2;
import org.babyfish.jimmer.client.meta.impl.ApiParameterImplSerializerV2;
import org.babyfish.jimmer.client.meta.impl.ApiServiceImpl;
import org.babyfish.jimmer.client.meta.impl.ApiServiceImplDeserializerV2;
import org.babyfish.jimmer.client.meta.impl.ApiServiceImplSerializerV2;
import org.babyfish.jimmer.client.meta.impl.EnumConstantImpl;
import org.babyfish.jimmer.client.meta.impl.EnumConstantImplDeserializerV2;
import org.babyfish.jimmer.client.meta.impl.EnumConstantImplSerializerV2;
import org.babyfish.jimmer.client.meta.impl.PropImpl;
import org.babyfish.jimmer.client.meta.impl.PropImplDeserializerV2;
import org.babyfish.jimmer.client.meta.impl.PropImplSerializerV2;
import org.babyfish.jimmer.client.meta.impl.SchemaImpl;
import org.babyfish.jimmer.client.meta.impl.SchemaImplDeserializerV2;
import org.babyfish.jimmer.client.meta.impl.SchemaImplSerializerV2;
import org.babyfish.jimmer.client.meta.impl.TypeDefinitionImpl;
import org.babyfish.jimmer.client.meta.impl.TypeDefinitionImplDeserializerV2;
import org.babyfish.jimmer.client.meta.impl.TypeDefinitionImplSerializerV2;
import org.babyfish.jimmer.client.meta.impl.TypeRefImpl;
import org.babyfish.jimmer.client.meta.impl.TypeRefImplDeserializerV2;
import org.babyfish.jimmer.client.meta.impl.TypeRefImplSerializerV2;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class ImmutableModuleV2 extends SimpleModule {

    public static final String MODULE_ID = ImmutableModuleV2.class.getName();

    public ImmutableModuleV2() {
        registerClientMetadata();
    }

    @Override
    public Object getTypeId() {
        return MODULE_ID;
    }

    @Override
    public String getModuleName() {
        return MODULE_ID;
    }

    @Override
    public void setupModule(SetupContext ctx) {
        super.setupModule(ctx);
        ctx.addBeanSerializerModifier(new ImmutableSerializerModifierV2());
        ctx.insertAnnotationIntrospector(new ImmutableAnnotationIntrospectorV2());
        ctx.setMixInAnnotations(Page.class, PageMixinV2.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerClientMetadata() {
        addSerializer((Class) TypeName.class, new TypeNameSerializerV2());
        addDeserializer((Class) TypeName.class, new TypeNameDeserializerV2());
        addSerializer((Class) Doc.class, new DocSerializerV2());
        addDeserializer((Class) Doc.class, new DocDeserializerV2());
        addSerializer((Class) SchemaImpl.class, new SchemaImplSerializerV2());
        addDeserializer((Class) SchemaImpl.class, new SchemaImplDeserializerV2());
        addSerializer((Class) ApiServiceImpl.class, new ApiServiceImplSerializerV2());
        addDeserializer((Class) ApiServiceImpl.class, new ApiServiceImplDeserializerV2());
        addSerializer((Class) ApiOperationImpl.class, new ApiOperationImplSerializerV2());
        addDeserializer((Class) ApiOperationImpl.class, new ApiOperationImplDeserializerV2());
        addSerializer((Class) ApiParameterImpl.class, new ApiParameterImplSerializerV2());
        addDeserializer((Class) ApiParameterImpl.class, new ApiParameterImplDeserializerV2());
        addSerializer((Class) TypeRefImpl.class, new TypeRefImplSerializerV2());
        addDeserializer((Class) TypeRefImpl.class, new TypeRefImplDeserializerV2());
        addSerializer((Class) TypeDefinitionImpl.class, new TypeDefinitionImplSerializerV2());
        addDeserializer((Class) TypeDefinitionImpl.class, new TypeDefinitionImplDeserializerV2());
        addSerializer((Class) PropImpl.class, new PropImplSerializerV2());
        addDeserializer((Class) PropImpl.class, new PropImplDeserializerV2());
        addSerializer((Class) EnumConstantImpl.class, new EnumConstantImplSerializerV2());
        addDeserializer((Class) EnumConstantImpl.class, new EnumConstantImplDeserializerV2());
    }
}
