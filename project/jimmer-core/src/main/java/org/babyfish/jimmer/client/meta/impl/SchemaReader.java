package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.json.Node;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.TypeDefinition;
import org.babyfish.jimmer.client.meta.TypeName;

import java.util.*;
import java.util.function.Consumer;

final class SchemaReader {

    private final Set<String> groups;

    SchemaReader(Set<String> groups) {
        this.groups = groups;
    }

    SchemaImpl<Object> schema(Node json, boolean servicesOnly) {
        require(json, Node.Kind.OBJECT);
        SchemaImpl<Object> schema = new SchemaImpl<>();
        forEachObjectArray(json.get("services"), service -> {
            List<String> groups = groups(service);
            if (isAllowed(groups)) {
                schema.addApiService(service(service, groups));
            }
        });
        if (!servicesOnly) {
            forEachObjectArray(json.get("definitions"), definition -> {
                List<String> groups = groups(definition);
                if (isAllowed(groups)) {
                    schema.addTypeDefinition(definition(definition, groups));
                }
            });
        }
        return schema;
    }

    private ApiServiceImpl<Object> service(Node json, List<String> serviceGroups) {
        ApiServiceImpl<Object> service = new ApiServiceImpl<>(null, typeName(json));
        service.setGroups(serviceGroups);
        service.setDoc(doc(json.get("doc")));
        forEachObject(json, "operations", node -> {
            List<String> groups = groups(node);
            if (isAllowed(groups)) {
                service.addOperation(operation(node, groups));
            }
        });
        return service;
    }

    private static ApiOperationImpl<Object> operation(Node json, List<String> groups) {
        ApiOperationImpl<Object> operation = new ApiOperationImpl<>(null, string(json.get("name")));
        operation.setKey(string(json.get("key")));
        operation.setGroups(groups);
        operation.setDoc(doc(json.get("doc")));
        forEachObject(json, "parameters", node -> {
            ApiParameterImpl<Object> parameter = new ApiParameterImpl<>(null, string(node.get("name")));
            parameter.setType(typeRef(require(node.get("type"), Node.Kind.OBJECT)));
            parameter.setOriginalIndex(Integer.parseInt(require(node.get("index"), Node.Kind.NUMBER).toJavaValue().toString()));
            operation.addParameter(parameter);
        });
        if (json.get("returnType") != null) {
            operation.setReturnType(typeRef(require(json.get("returnType"), Node.Kind.OBJECT)));
        }
        Node exceptions = json.get("exceptions");
        if (exceptions != null) {
            List<TypeName> typeNames = new ArrayList<>();
            require(exceptions, Node.Kind.ARRAY);
            for (int i = 0; i < exceptions.size(); i++) {
                typeNames.add(TypeName.parse(string(exceptions.get(i))));
            }
            operation.setExceptionTypeNames(typeNames);
        }
        return operation;
    }

    private static TypeDefinitionImpl<Object> definition(Node json, List<String> groups) {
        TypeDefinitionImpl<Object> definition = new TypeDefinitionImpl<>(null, typeName(json));
        definition.mergeGroups(groups);
        definition.setKind(TypeDefinition.Kind.valueOf(string(json, "kind", "OBJECT")));
        definition.setDoc(doc(json.get("doc")));
        Node error = json.get("error");
        if (error != null) {
            Node node = require(error, Node.Kind.OBJECT);
            definition.setError(new TypeDefinition.Error(string(node.get("family")), string(node.get("code"))));
        }
        definition.setApiIgnore(flag(json, "apiIgnore"));
        forEachObject(json, "props", node -> {
            PropImpl<Object> prop = new PropImpl<>(null, string(node.get("name")));
            prop.setType(typeRef(require(node.get("type"), Node.Kind.OBJECT)));
            prop.setDoc(doc(node.get("doc")));
            definition.addProp(prop);
        });
        forEachObject(json, "superTypes", node -> definition.addSuperType(typeRef(node)));
        forEachObject(json, "branches", node -> definition.addPolymorphicBranch(typeRef(node)));
        forEachObject(json, "constants", node -> {
            EnumConstantImpl<Object> constant = new EnumConstantImpl<>(null, string(node.get("name")));
            constant.setDoc(doc(node.get("doc")));
            definition.addEnumConstant(constant);
        });
        return definition;
    }

    private static TypeRefImpl<Object> typeRef(Node json) {
        TypeRefImpl<Object> ref = new TypeRefImpl<>();
        ref.setTypeName(typeName(json));
        ref.setNullable(flag(json, "nullable"));
        forEachObject(json, "arguments", node -> ref.addArgument(typeRef(node)));
        if (json.get("fetchBy") != null) {
            ref.setFetchBy(string(json.get("fetchBy")));
            ref.setFetcherOwner(TypeName.parse(string(json.get("fetcherOwner"))));
            ref.setFetcherDoc(doc(json.get("fetcherDoc")));
        }
        return ref;
    }

    private static TypeName typeName(Node json) {
        return TypeName.parse(string(json.get("typeName")));
    }

    private static Doc doc(Node value) {
        if (value == null || value.isNull()) {
            return null;
        }
        Node json = require(value, Node.Kind.OBJECT);
        return new Doc(
                string(json, "value", null),
                stringMap(json.get("parameters")),
                string(json, "return", null),
                stringMap(json.get("properties"))
        );
    }

    private static Map<String, String> stringMap(Node value) {
        if (value == null) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new LinkedHashMap<>();
        require(value, Node.Kind.OBJECT).fieldsIterator().forEachRemaining(entry -> {
            Node text = entry.getValue();
            map.put(entry.getKey(), text.isNull() ? null : string(text));
        });
        return map;
    }

    private static List<String> groups(Node json) {
        Node value = json.get("groups");
        if (value == null) {
            return null;
        }
        List<String> groups = new ArrayList<>();
        require(value, Node.Kind.ARRAY);
        for (int i = 0; i < value.size(); i++) {
            groups.add(string(value.get(i)));
        }
        return groups;
    }

    private boolean isAllowed(List<String> elementGroups) {
        return groups == null || groups.isEmpty() ||
                elementGroups == null || elementGroups.isEmpty() ||
                !Collections.disjoint(elementGroups, groups);
    }

    private static void forEachObject(Node json, String name, Consumer<Node> consumer) {
        Node array = json.get(name);
        if (array != null) {
            forEachObjectArray(array, consumer);
        }
    }

    private static void forEachObjectArray(Node array, Consumer<Node> consumer) {
        require(array, Node.Kind.ARRAY);
        for (int i = 0; i < array.size(); i++) {
            consumer.accept(require(array.get(i), Node.Kind.OBJECT));
        }
    }

    private static Node require(Node value, Node.Kind kind) {
        if (value == null || value.kind() != kind) {
            throw new IllegalArgumentException("Expected a JSON " + kind + " in client schema");
        }
        return value;
    }

    private static String string(Node value) {
        return (String) require(value, Node.Kind.STRING).toJavaValue();
    }

    private static String string(Node object, String name, String defaultValue) {
        Node value = object.get(name);
        return value != null ? string(value) : defaultValue;
    }

    private static boolean flag(Node object, String name) {
        Node value = object.get(name);
        return value != null && (Boolean) require(value, Node.Kind.BOOLEAN).toJavaValue();
    }
}
