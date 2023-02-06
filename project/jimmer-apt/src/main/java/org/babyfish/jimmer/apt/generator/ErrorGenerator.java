package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.error.CodeBasedException;
import org.babyfish.jimmer.error.ErrorField;
import org.babyfish.jimmer.error.ErrorFields;
import org.babyfish.jimmer.impl.asm.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Filer;
import javax.lang.model.element.*;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class ErrorGenerator {

    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

    private static final String ERROR_FIELDS_NAME = ErrorFields.class.getName();

    private static final String ERROR_FIELD_NAME = ErrorField.class.getName();

    private static final String[] EMPTY_STR_ARR = new String[0];

    private static final Map<String, TypeName> PRIMITIVE_TYPE_MAP;

    private final TypeElement typeElement;

    private final Filer filer;

    private final String packageName;

    private final ClassName className;

    private final String exceptionName;

    private final ClassName exceptionClassName;

    private TypeSpec.Builder typeBuilder;

    public ErrorGenerator(TypeElement typeElement, Filer filer) {
        this.typeElement = typeElement;
        this.filer = filer;
        this.packageName = packageName();
        String[] simpleNames = simpleNames();
        this.className = ClassName.get(
                packageName,
                simpleNames[0],
                Arrays.copyOfRange(simpleNames, 1, simpleNames.length)
        );
        String exceptionName = String.join("_", simpleNames);
        if (exceptionName.endsWith("_Error")) {
            exceptionName = exceptionName.substring(0, exceptionName.length() - 6) + "Exception";
        } else if (exceptionName.endsWith("Error")) {
            exceptionName = exceptionName.substring(0, exceptionName.length() - 5) + "Exception";
        } else {
            exceptionName = exceptionName + "Exception";
        }
        this.exceptionName = exceptionName;
        this.exceptionClassName = ClassName.get(packageName, exceptionName);
    }

    public void generate() {
        typeBuilder = TypeSpec
                .classBuilder(exceptionName)
                .addModifiers(Modifier.PUBLIC)
                .superclass(CodeBasedException.class);
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
        addClassFields();
        addConstructor();
        addGetCode();
        addGetFields();

        for (Element element : typeElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.ENUM_CONSTANT) {
                addCreator(element, false);
                addCreator(element, true);
            }
        }
    }

    private void addClassFields() {
        typeBuilder.addField(
                FieldSpec
                        .builder(className, "code")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build()
        );
        typeBuilder.addField(
                FieldSpec
                        .builder(
                                ParameterizedTypeName.get(
                                        Constants.MAP_CLASS_NAME,
                                        Constants.STRING_CLASS_NAME,
                                        Constants.OBJECT_CLASS_NAME
                                ),
                                "fields"
                        )
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build()
        );
    }

    private void addConstructor() {
        typeBuilder.addMethod(
                MethodSpec
                        .constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .addParameter(String.class, "message")
                        .addParameter(Throwable.class, "cause")
                        .addParameter(className, "code")
                        .addParameter(
                                ParameterizedTypeName.get(
                                        Constants.MAP_CLASS_NAME,
                                        Constants.STRING_CLASS_NAME,
                                        Constants.OBJECT_CLASS_NAME
                                ),
                                "fields"
                        )
                        .addStatement("super(message, cause)")
                        .addStatement("this.code = code")
                        .addStatement("this.fields = fields")
                        .build()
        );
    }

    private void addGetCode() {
        typeBuilder.addMethod(
                MethodSpec
                        .methodBuilder("getCode")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .returns(className)
                        .addStatement("return code")
                        .build()
        );
    }

    private void addGetFields() {
        typeBuilder.addMethod(
                MethodSpec
                        .methodBuilder("getFields")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .returns(
                                ParameterizedTypeName.get(
                                        Constants.MAP_CLASS_NAME,
                                        Constants.STRING_CLASS_NAME,
                                        Constants.OBJECT_CLASS_NAME
                                )
                        )
                        .addStatement("return fields")
                        .build()
        );
    }

    @SuppressWarnings("unchecked")
    private void addCreator(Element element, boolean withCause) {

        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(creatorName(element))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(exceptionClassName)
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

        List<Field> fields = new ArrayList<>();
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            String qualifiedName = ((TypeElement) annotationMirror.getAnnotationType().asElement())
                    .getQualifiedName()
                    .toString();
            if (qualifiedName.equals(ERROR_FIELDS_NAME)) {
                Iterator<? extends AnnotationValue> itr = annotationMirror.getElementValues().values().iterator();
                if (itr.hasNext()) {
                    AnnotationValue annotationValue = itr.next();
                    for (AnnotationMirror childMirror : (List<AnnotationMirror>)annotationValue.getValue()) {
                        fields.add(Field.of(childMirror));
                    }
                }
                break;
            } else if (qualifiedName.equals(ERROR_FIELD_NAME)) {
                fields = Collections.singletonList(Field.of(annotationMirror));
                break;
            }
        }

        for (Field field : fields) {
            builder.addParameter(
                    ParameterSpec
                            .builder(
                                    field.isList ?
                                            ParameterizedTypeName.get(Constants.LIST_CLASS_NAME, field.type) :
                                            field.type,
                                    field.name
                            )
                            .addAnnotation(field.isNullable ? Nullable.class : NotNull.class)
                            .build()
            );
        }
        if (fields.isEmpty()) {
            builder.addStatement(
                    "return new $T(message, $L, $T.$L, $T.emptyMap())",
                    exceptionClassName,
                    withCause ? "cause" : "null",
                    className,
                    element.getSimpleName().toString(),
                    Constants.COLLECTIONS_CLASS_NAME
            );
        } else if (fields.size() == 1) {
            builder.addStatement(
                    "return new $T(message, $L, $T.$L, $T.singletonMap($S, $L))",
                    exceptionClassName,
                    withCause ? "cause" : "null",
                    className,
                    element.getSimpleName().toString(),
                    Constants.COLLECTIONS_CLASS_NAME,
                    fields.get(0).name,
                    fields.get(0).name
            );
        } else {
            builder.addStatement(
                    "$T<String, Object> __map = new $T()",
                    Constants.MAP_CLASS_NAME,
                    Constants.LINKED_HASH_MAP_CLASS_NAME
            );
            for (Field field : fields) {
                builder.addStatement("__map.put($S, $L)", field.name, field.name);
            }
            builder.addStatement(
                    "return new $T(message, $L, $T.$L, __map)",
                    exceptionClassName,
                    withCause ? "cause" : "null",
                    className,
                    element.getSimpleName().toString()
            );
        }
        typeBuilder.addMethod(builder.build());
    }

    private static String creatorName(Element element) {
        String simpleName = element.getSimpleName().toString();
        int size = simpleName.length();
        boolean toUpper = false;
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

    private static class Field {

        final String name;

        final TypeName type;

        final boolean isNullable;

        final boolean isList;

        private Field(String name, TypeName type, boolean isNullable, boolean isList) {
            this.name = name;
            this.type = type;
            this.isNullable = isNullable;
            this.isList = isList;
        }

        public static Field of(AnnotationMirror annotationMirror) {
            String name = null;
            TypeName typeName = null;
            boolean isNullable = false;
            boolean isList = false;
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e :
                    annotationMirror.getElementValues().entrySet()) {
                String key = e.getKey().getSimpleName().toString();
                Object value = e.getValue().getValue();
                if (key.equals("name")) {
                    name = (String)value;
                } else if (key.equals("type")) {
                    typeName = typeName(value.toString());
                } else if (key.equals("nullable")) {
                    isNullable = (boolean) value;
                } else if (key.equals("list")){
                    isList = (boolean) value;
                }
            }
            return new Field(name, typeName, isNullable, isList);
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
