package org.babyfish.jimmer.apt.error;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.MetaException;
import org.babyfish.jimmer.apt.immutable.generator.Annotations;
import org.babyfish.jimmer.apt.immutable.generator.Constants;
import org.babyfish.jimmer.error.*;
import org.babyfish.jimmer.impl.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Filer;
import javax.lang.model.element.*;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ErrorGenerator {

    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

    private static final String ERROR_FIELDS_NAME = ErrorFields.class.getName();

    private static final String ERROR_FIELD_NAME = ErrorField.class.getName();

    private static final String[] EMPTY_STR_ARR = new String[0];

    private static final Map<String, TypeName> PRIMITIVE_TYPE_MAP;

    private final Context context;

    private final TypeElement typeElement;

    private final boolean checkedException;

    private final Filer filer;

    private final String packageName;

    private final ClassName className;

    private final String family;

    private final String exceptionName;

    private final ClassName exceptionClassName;

    private TypeSpec.Builder typeBuilder;

    private Map<Element, List<Field>> declaredFieldsCache = new HashMap<>();

    private Map<Element, List<Field>> fieldsCache = new HashMap<>();

    public ErrorGenerator(Context context, TypeElement typeElement, boolean checkedException, Filer filer) {
        this.context = context;
        this.typeElement = typeElement;
        this.checkedException = checkedException;
        this.filer = filer;
        this.packageName = packageName();
        String[] simpleNames = simpleNames();
        this.className = ClassName.get(
                packageName,
                simpleNames[0],
                Arrays.copyOfRange(simpleNames, 1, simpleNames.length)
        );
        String name = String.join("_", simpleNames);
        if (name.endsWith("_ErrorCode")) {
            name = name.substring(0, name.length() - 10);
        } else if (name.endsWith("ErrorCode")) {
            name = name.substring(0, name.length() - 9);
        } else if (name.endsWith("_Error")) {
            name = name.substring(0, name.length() - 6);
        } else if (name.endsWith("Error")) {
            name = name.substring(0, name.length() - 5);
        }
        String family = typeElement.getAnnotation(ErrorFamily.class).value();
        if (family.isEmpty()) {
            family = StringUtil.snake(name, StringUtil.SnakeCase.UPPER);
        }
        this.family = family;
        this.exceptionName = name + "Exception";
        this.exceptionClassName = ClassName.get(packageName, exceptionName);
    }

    public void generate() {
        typeBuilder = TypeSpec
                .classBuilder(exceptionName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .superclass(checkedException ? CodeBasedException.class : CodeBasedRuntimeException.class)
                .addAnnotation(
                        AnnotationSpec
                                .builder(Constants.GENERATED_BY_CLASS_NAME)
                                .addMember("type", "$T.class", className)
                                .build()
                )
                .addAnnotation(clientException(typeElement));
        String doc = context.getElements().getDocComment(typeElement);
        if (doc != null && !doc.isEmpty()) {
            typeBuilder.addJavadoc(doc);
        }
        addMembers();
        try {
            JavaFile
                    .builder(
                            packageName,
                            typeBuilder.build()
                    )
                    .indent("    ")
                    .build()
                    .writeTo(filer);
        } catch (IOException ex) {
            throw new GeneratorException(
                    String.format(
                            "Cannot generate code based exception for enum type '%s'",
                            typeElement.getQualifiedName().toString()
                    ),
                    ex
            );
        }
    }

    private String packageName() {
        for (Element element = typeElement.getEnclosingElement();
             element != null;
             element = element.getEnclosingElement()) {
            if (element instanceof PackageElement) {
                return ((PackageElement) element).getQualifiedName().toString();
            }
        }
        return "";
    }

    private String[] simpleNames() {
        String qualifiedName = typeElement.getQualifiedName().toString();
        if (packageName.isEmpty()) {
            return DOT_PATTERN.split(qualifiedName);
        }
        return DOT_PATTERN.split(qualifiedName.substring(packageName.length() + 1));
    }

    private void addMembers() {

        addCommonMembers(typeElement, typeBuilder);
        addGetEnum();

        for (Element element : typeElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.ENUM_CONSTANT) {
                addCreator(element, false);
                addCreator(element, true);
            }
        }

        for (Element element : typeElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.ENUM_CONSTANT) {
                addType(element);
            }
        }
    }

    private void addGetEnum() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("get" + typeElement.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(JsonIgnore.class)
                .returns(className);
        typeBuilder.addMethod(builder.build());
    }

    @SuppressWarnings("unchecked")
    private void addCreator(Element element, boolean withCause) {

        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(javaName(element, false))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(exceptionClassName.nestedClass(javaName(element, true)))
                .addParameter(
                        ParameterSpec
                                .builder(Constants.STRING_CLASS_NAME, "message")
                                .addAnnotation(NotNull.class)
                                .build()
                );
        if (withCause) {
            builder.addParameter(
                    ParameterSpec
                            .builder(Throwable.class, "cause")
                            .addAnnotation(Nullable.class)
                            .build()
            );
        }

        List<Field> fields = fieldsOf(element);

        for (Field field : fields) {
            builder.addParameter(
                    ParameterSpec
                            .builder(field.type, field.name)
                            .addAnnotation(field.isNullable ? Nullable.class : NotNull.class)
                            .build()
            );
        }
        builder.addCode("return new $L(\n$>", javaName(element, true));
        builder.addCode("message,\n").addCode(withCause ? "cause" : "null");
        for (Field field : fields) {
            builder.addCode(",\n$L", field.name);
        }
        builder.addCode("\n$<);\n");

        typeBuilder.addMethod(builder.build());
    }

    private void addType(Element element) {
        TypeSpec.Builder builder = TypeSpec
                .classBuilder(javaName(element, true))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .superclass(exceptionClassName)
                .addAnnotation(clientException(element));

        String doc = context.getElements().getDocComment(element);
        if (doc != null && !doc.isEmpty()) {
            builder.addJavadoc(doc);
        }

        addCommonMembers(element, builder);

        builder.addMethod(
                MethodSpec
                        .methodBuilder("get" + typeElement.getSimpleName().toString())
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(JsonIgnore.class)
                        .addAnnotation(Override.class)
                        .returns(className)
                        .addStatement("return $T.$L", className, element.getSimpleName().toString())
                        .build()
        );

        MethodSpec.Builder getFieldsBuilder = MethodSpec
                .methodBuilder("getFields")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(
                        ParameterizedTypeName.get(
                                Constants.MAP_CLASS_NAME,
                                Constants.STRING_CLASS_NAME,
                                TypeName.OBJECT
                        )
                );
        List<Field> fields = fieldsOf(element);
        if (fields.isEmpty()) {
            getFieldsBuilder.addStatement("return $T.emptyMap()", Constants.COLLECTIONS_CLASS_NAME);
        } else if (fields.size() == 1) {
            getFieldsBuilder.addStatement(
                    "return $T.singletonMap($S, $L)",
                    Constants.COLLECTIONS_CLASS_NAME,
                    fields.get(0).name,
                    fields.get(0).name
            );
        } else {
            getFieldsBuilder.addStatement(
                    "$T __fields = new $T<>()",
                    Constants.MAP_CLASS_NAME,
                    Constants.LINKED_HASH_MAP_CLASS_NAME
            );
            for (Field field : fields) {
                getFieldsBuilder.addStatement("__fields.put($S, $L)", field.name, field.name);
            }
            getFieldsBuilder.addStatement("return __fields");
        }
        builder.addMethod(getFieldsBuilder.build());

        typeBuilder.addType(builder.build());
    }

    private void addCommonMembers(Element element, TypeSpec.Builder builder) {

        for (Field field : declaredFieldsOf(element)) {
            FieldSpec.Builder fieldBuilder = FieldSpec
                    .builder(field.type, field.name)
                    .addModifiers(Modifier.FINAL)
                    .addAnnotation(field.isNullable ? Nullable.class : NotNull.class);
            builder.addField(fieldBuilder.build());
        }

        MethodSpec.Builder constructorBuilder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Constants.STRING_CLASS_NAME, "message")
                .addParameter(Constants.THROWABLE_CLASS_NAME, "cause");
        for (Field field : fieldsOf(element)) {
            ParameterSpec.Builder parameterBuilder =
                    ParameterSpec
                            .builder(field.type, field.name)
                            .addAnnotation(field.isNullable ? Nullable.class : NotNull.class);
            constructorBuilder.addParameter(parameterBuilder.build());
        }
        if (element.getKind() == ElementKind.ENUM) {
            constructorBuilder.addStatement("super(message, cause)");
        } else {
            CodeBlock.Builder cb = CodeBlock.builder();
            cb.add("super(message, cause");
            for (Field parentField : declaredFieldsOf(element.getEnclosingElement())) {
                cb.add(", ").add(parentField.name);
            }
            cb.add(")");
            constructorBuilder.addStatement(cb.build());
        }
        for (Field field : declaredFieldsOf(element)) {
            constructorBuilder.addStatement("this.$L = $L", field.name, field.name);
        }
        builder.addMethod(constructorBuilder.build());

        for (Field field : declaredFieldsOf(element)) {
            MethodSpec.Builder mb = MethodSpec
                    .methodBuilder(
                            (field.type.equals(TypeName.BOOLEAN) ? "is" : "get") +
                                    Character.toUpperCase(field.name.charAt(0)) +
                                    field.name.substring(1)
                    )
                    .addModifiers(Modifier.PUBLIC)
                    .returns(field.type)
                    .addAnnotation(field.isNullable ? Nullable.class : NotNull.class);
            if (!field.doc.isEmpty()) {
                mb.addJavadoc(field.doc);
            }
            mb.addStatement("return $L", field.name);
            builder.addMethod(mb.build());
        }
    }

    private static String javaName(Element element, boolean upperHead) {
        String simpleName = element.getSimpleName().toString();
        int size = simpleName.length();
        boolean toUpper = upperHead;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) {
            char c = simpleName.charAt(i);
            if (c == '_') {
                toUpper = true;
            } else {
                if (toUpper) {
                    builder.append(Character.toUpperCase(c));
                } else {
                    builder.append(Character.toLowerCase(c));
                }
                toUpper = false;
            }
        }
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    private List<Field> declaredFieldsOf(Element element) {
        List<Field> fields = declaredFieldsCache.get(element);
        if (fields != null) {
            return fields;
        }
        Map<String, Field> map = new LinkedHashMap<>();
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            String qualifiedName = ((TypeElement) annotationMirror.getAnnotationType().asElement())
                    .getQualifiedName()
                    .toString();
            if (qualifiedName.equals(ERROR_FIELDS_NAME)) {
                Iterator<? extends AnnotationValue> itr = annotationMirror.getElementValues().values().iterator();
                if (itr.hasNext()) {
                    AnnotationValue annotationValue = itr.next();
                    for (AnnotationMirror childMirror : (List<AnnotationMirror>)annotationValue.getValue()) {
                        Field field = Field.of(childMirror, element);
                        if (map.put(field.name, field) != null) {
                            throw new MetaException(
                                    element,
                                    "Duplicate field \"" +
                                            field.name +
                                            "\""
                            );
                        }
                    }
                }
                break;
            } else if (qualifiedName.equals(ERROR_FIELD_NAME)) {
                Field field = Field.of(annotationMirror, element);
                map.put(field.name, field);
                break;
            }
        }
        fields = Collections.unmodifiableList(new ArrayList<>(map.values()));
        declaredFieldsCache.put(element, fields);
        return fields;
    }

    private List<Field> fieldsOf(Element element) {
        List<Field> fields = fieldsCache.get(element);
        if (fields != null) {
            return fields;
        }
        List<Field> declaredFields = declaredFieldsOf(element);
        if (element.getKind() == ElementKind.ENUM_CONSTANT) {
            List<Field> sharedFields = declaredFieldsOf(element.getEnclosingElement());
            if (!sharedFields.isEmpty()) {
                Set<String> sharedFieldNames = sharedFields.stream().map(it -> it.name).collect(Collectors.toSet());
                for (Field declaredField : declaredFields) {
                    if (sharedFieldNames.contains(declaredField.name)) {
                        throw new MetaException(
                                element,
                                "The field \"" +
                                        declaredField.name +
                                        "\" has been defined in enum"
                        );
                    }
                }
                List<Field> mergedFields = new ArrayList<>(sharedFields.size() + declaredFields.size());
                mergedFields.addAll(sharedFields);
                mergedFields.addAll(declaredFields);
                mergedFields = Collections.unmodifiableList(mergedFields);
                fields = mergedFields;
            }
        }
        if (fields == null) {
            fields = declaredFieldsOf(element);
        }
        fieldsCache.put(element, fields);
        return fields;
    }

    private AnnotationSpec clientException(Element element) {
        AnnotationSpec.Builder builder = AnnotationSpec
                .builder(Constants.CLIENT_EXCEPTION_CLASS_NAME)
                .addMember("family", "$S", family);
        if (element.getKind() == ElementKind.ENUM) {
            CodeBlock.Builder cb = CodeBlock.builder();
            cb.add("{");
            boolean addComma = false;
            for (Element subElement : element.getEnclosedElements()) {
                if (subElement.getKind() != ElementKind.ENUM_CONSTANT) {
                    continue;
                }
                if (addComma) {
                    cb.add(", ");
                } else {
                    addComma = true;
                }
                ClassName className = exceptionClassName.nestedClass(
                        javaName(subElement, true)
                );
                cb.add("$T.class", className);
            }
            cb.add("}");
            builder.addMember("subTypes", cb.build());
        } else {
            builder.addMember("code", "$S", element.getSimpleName().toString());
        }
        return builder.build();
    }

    private static class Field {

        final String name;

        final TypeName type;

        final boolean isNullable;

        final boolean isList;

        final String doc;

        private Field(String name, TypeName type, boolean isNullable, boolean isList, String doc) {
            this.name = name;
            this.type = type;
            this.isNullable = isNullable;
            this.isList = isList;
            this.doc = doc;
        }

        public static Field of(AnnotationMirror annotationMirror, Element constantElement) {
            String name = null;
            TypeName typeName = null;
            boolean isNullable = false;
            boolean isList = false;
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e :
                    annotationMirror.getElementValues().entrySet()) {
                String key = e.getKey().getSimpleName().toString();
                Object value = e.getValue().getValue();
                if (key.equals("name")) {
                    String str = (String)value;
                    if (str.equals("family") || str.equals("code")) {
                        throw new MetaException(
                                constantElement,
                                "The enum constant \"" +
                                        constantElement.getEnclosingElement().getSimpleName().toString() +
                                        '.' +
                                        constantElement.getSimpleName().toString() +
                                        "\" is illegal, it cannot be decorated by \"@" +
                                        ErrorFamily.class.getName() +
                                        "\" with the name \"family\" or \"code\""
                        );
                    }
                    name = str;
                } else if (key.equals("type")) {
                    typeName = typeName(value.toString());
                } else if (key.equals("nullable")) {
                    isNullable = (boolean) value;
                } else if (key.equals("list")){
                    isList = (boolean) value;
                }
            }
            assert typeName != null;
            if (isList) {
                if (typeName.isPrimitive()) {
                    throw new MetaException(
                            constantElement,
                            "The enum constant \"" +
                                    constantElement.getEnclosingElement().getSimpleName().toString() +
                                    '.' +
                                    constantElement.getSimpleName().toString() +
                                    "\" is decorated by @" +
                                    ErrorField.class.getName() +
                                    ", this annotation is illegal because its `type` is primitive but " +
                                    "its `list` is true"
                    );
                }
                typeName = ParameterizedTypeName.get(
                        Constants.LIST_CLASS_NAME,
                        typeName
                );
            }
            String doc = Annotations.annotationValue(annotationMirror, "doc", "").trim();
            return new Field(name, typeName, isNullable, isList, doc);
        }
    }

    private static TypeName typeName(String value) {
        TypeName primitiveTypeName = PRIMITIVE_TYPE_MAP.get(value);
        if (primitiveTypeName != null) {
            return primitiveTypeName;
        }
        StringBuilder packageBuilder = new StringBuilder();
        String simpleName = null;
        List<String> nestNames = new ArrayList<>();
        for (String part : DOT_PATTERN.split(value)) {
            if (Character.isUpperCase(part.charAt(0))) {
                if (simpleName == null) {
                    simpleName = part;
                } else {
                    nestNames.add(part);
                }
            } else {
                packageBuilder.append(part).append('.');
            }
        }
        return ClassName.get(
                packageBuilder.length() == 0 ?
                        "" :
                        packageBuilder.substring(0, packageBuilder.length() - 1),
                simpleName,
                nestNames.toArray(EMPTY_STR_ARR)
        );
    }

    static {
        Map<String, TypeName> map = new HashMap<>();
        map.put("boolean", TypeName.BOOLEAN);
        map.put("char", TypeName.CHAR);
        map.put("byte", TypeName.BYTE);
        map.put("short", TypeName.SHORT);
        map.put("int", TypeName.INT);
        map.put("long", TypeName.LONG);
        map.put("float", TypeName.FLOAT);
        map.put("double", TypeName.DOUBLE);
        PRIMITIVE_TYPE_MAP = map;
    }
}
