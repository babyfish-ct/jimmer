package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class SchemaWriter {

    private SchemaWriter() {}

    static Map<String, Object> schema(Schema schema) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("services", array(schema.getApiServiceMap().values(), SchemaWriter::service));
        json.put("definitions", array(schema.getTypeDefinitionMap().values(), SchemaWriter::definition));
        return json;
    }

    private static Map<String, Object> service(ApiService service) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("typeName", service.getTypeName().toString(true));
        addGroups(json, service.getGroups());
        addDoc(json, service.getDoc());
        addArray(json, "operations", service.getOperations(), SchemaWriter::operation);
        return json;
    }

    private static Map<String, Object> operation(ApiOperation operation) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("name", operation.getName());
        json.put("key", ((ApiOperationImpl<?>) operation).key());
        addGroups(json, operation.getGroups());
        addDoc(json, operation.getDoc());
        addArray(json, "parameters", operation.getParameters(), SchemaWriter::parameter);
        if (operation.getReturnType() != null) {
            json.put("returnType", typeRef(operation.getReturnType()));
        }
        addArray(json, "exceptions", operation.getExceptionTypes(), ref -> ref.getTypeName().toString(true));
        return json;
    }

    private static Map<String, Object> parameter(ApiParameter parameter) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("name", parameter.getName());
        json.put("type", typeRef(parameter.getType()));
        json.put("index", parameter.getOriginalIndex());
        return json;
    }

    private static Map<String, Object> definition(TypeDefinition definition) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("typeName", definition.getTypeName().toString(true));
        if (definition.getKind() != TypeDefinition.Kind.OBJECT) {
            json.put("kind", definition.getKind().name());
        }
        addDoc(json, definition.getDoc());
        TypeDefinition.Error error = definition.getError();
        if (error != null) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("family", error.getFamily());
            details.put("code", error.getCode());
            json.put("error", details);
        }
        if (definition.isApiIgnore()) {
            json.put("apiIgnore", true);
        }
        addGroups(json, definition.getGroups());
        addArray(json, "props", definition.getPropMap().values(), SchemaWriter::prop);
        addArray(json, "superTypes", definition.getSuperTypes(), SchemaWriter::typeRef);
        addArray(json, "branches", definition.getPolymorphicBranches(), SchemaWriter::typeRef);
        addArray(json, "constants", definition.getEnumConstantMap().values(), SchemaWriter::constant);
        return json;
    }

    private static Map<String, Object> prop(Prop prop) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("name", prop.getName());
        json.put("type", typeRef(prop.getType()));
        addDoc(json, prop.getDoc());
        return json;
    }

    private static Map<String, Object> constant(EnumConstant constant) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("name", constant.getName());
        addDoc(json, constant.getDoc());
        return json;
    }

    private static Map<String, Object> typeRef(TypeRef ref) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("typeName", ref.getTypeName().toString(true));
        if (ref.isNullable()) {
            json.put("nullable", true);
        }
        addArray(json, "arguments", ref.getArguments(), SchemaWriter::typeRef);
        if (ref.getFetchBy() != null) {
            json.put("fetchBy", ref.getFetchBy());
            json.put("fetcherOwner", ref.getFetcherOwner().toString());
            json.put("fetcherDoc", ref.getFetcherDoc() != null ? doc(ref.getFetcherDoc()) : null);
        }
        return json;
    }

    private static void addDoc(Map<String, Object> json, Doc doc) {
        if (doc != null) {
            json.put("doc", doc(doc));
        }
    }

    private static Map<String, Object> doc(Doc doc) {
        Map<String, Object> json = new LinkedHashMap<>();
        if (doc.getValue() != null) {
            json.put("value", doc.getValue());
        }
        if (!doc.getParameterValueMap().isEmpty()) {
            json.put("parameters", doc.getParameterValueMap());
        }
        if (doc.getReturnValue() != null) {
            json.put("return", doc.getReturnValue());
        }
        if (!doc.getPropertyValueMap().isEmpty()) {
            json.put("properties", doc.getPropertyValueMap());
        }
        return json;
    }

    private static void addGroups(Map<String, Object> json, Collection<String> groups) {
        if (groups != null) {
            json.put("groups", new ArrayList<>(groups));
        }
    }

    private static <T> void addArray(
            Map<String, Object> json, String name, Collection<T> values, Function<T, ?> mapper
    ) {
        if (!values.isEmpty()) {
            json.put(name, array(values, mapper));
        }
    }

    private static <T> List<Object> array(Collection<T> values, Function<T, ?> mapper) {
        List<Object> json = new ArrayList<>(values.size());
        for (T value : values) {
            json.add(mapper.apply(value));
        }
        return json;
    }
}
