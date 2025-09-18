package org.babyfish.jimmer.apt.tuple;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.MetaException;
import org.babyfish.jimmer.apt.immutable.generator.Constants;
import org.babyfish.jimmer.impl.util.StringUtil;
import org.babyfish.jimmer.sql.TypedTuple;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TypedTupleGenerator {

    private static final TypeName SELECTION_ARR_TYPE_NAME =
            ArrayTypeName.of(
                    ParameterizedTypeName.get(
                            Constants.SELECTION_CLASS_NAME,
                            WildcardTypeName.subtypeOf(TypeName.OBJECT)
                    )
            );

    private final Context context;

    private final TypeElement typeElement;

    private final List<VariableElement> fieldElements;

    private final ClassName className;

    private final int[] ctorPropIndices;

    private TypeSpec.Builder typeBuilder;

    TypedTupleGenerator(
            Context context,
            TypeElement typeElement
    ) {
        this.context = context;
        this.typeElement = typeElement;
        List<VariableElement> fieldElements = new ArrayList<>();
        for (Element element : typeElement.getEnclosedElements()) {
            if (element instanceof VariableElement) {
                if (!element.getModifiers().contains(Modifier.STATIC)) {
                    fieldElements.add((VariableElement) element);
                }
            }
        }
        if (fieldElements.isEmpty()) {
            throw new MetaException(
                    typeElement,
                    "There is no non-state field"
            );
        }
        this.fieldElements = Collections.unmodifiableList(fieldElements);
        this.className = ClassName.get(
                ((PackageElement) typeElement.getEnclosingElement()).getQualifiedName().toString(),
                typeElement.getSimpleName().toString() + "Mapper"
        );
        this.ctorPropIndices = determineCtorPropIndices();
    }

    private int[] determineCtorPropIndices() {
        int lombokCtorKind = findLombokCtorKind();
        if (lombokCtorKind == 2) {
            int[] indices = new int[fieldElements.size()];
            for (int i = indices.length - 1; i >= 0; --i) {
                indices[i] = i;
            }
            return indices;
        } else if (lombokCtorKind == 1) {
            return null;
        }
        if (hasDefaultConstructor()) {
            return null;
        }
        int[] indices = findUserAllArgsConstructorPropIndices();
        if (indices != null) {
            return indices;
        }
        throw new MetaException(
                typeElement,
                "it is decorated by @" +
                        TypedTuple.class.getName() +
                        ", " +
                        "but there is neither default constructor " +
                        "nor constructor with full arguments"
        );
    }

    private int findLombokCtorKind() { // 0: No ctors, 1. NoArgs ctor, 2. AllArgsCtor
        for (AnnotationMirror annotationMirror : typeElement.getAnnotationMirrors()) {
            TypeElement annotationElement = (TypeElement) annotationMirror.getAnnotationType().asElement();
            if (annotationElement.getQualifiedName().toString().equals("lombok.Builder")) {
                throw new MetaException(
                        typeElement,
                        "it is decorated by @" +
                                TypedTuple.class.getName() +
                                ", so it cannot be decorated by @lombok.Builder"
                );
            }
        }
        for (AnnotationMirror annotationMirror : typeElement.getAnnotationMirrors()) {
            TypeElement annotationElement = (TypeElement) annotationMirror.getAnnotationType().asElement();
            if (annotationElement.getQualifiedName().toString().equals("lombok.AllArgsConstructor")) {
                return 2;
            }
        }
        for (AnnotationMirror annotationMirror : typeElement.getAnnotationMirrors()) {
            TypeElement annotationElement = (TypeElement) annotationMirror.getAnnotationType().asElement();
            if (annotationElement.getQualifiedName().toString().equals("lombok.NoArgsConstructor")) {
                return 1;
            }
        }
        for (AnnotationMirror annotationMirror : typeElement.getAnnotationMirrors()) {
            TypeElement annotationElement = (TypeElement) annotationMirror.getAnnotationType().asElement();
            if (annotationElement.getQualifiedName().toString().equals("lombok.Data")) {
                Boolean isFinal = null;
                for (VariableElement field : fieldElements) {
                    boolean _final = field.getModifiers().contains(Modifier.FINAL);
                    if (isFinal != null && isFinal != _final) {
                        throw new MetaException(
                                typeElement,
                                "it is decorated by both @" +
                                        TypedTuple.class.getName() +
                                        " and @lombok.Data, so it cannot mix final fields and non-final fields"
                        );
                    }
                    isFinal = _final;
                }
                return isFinal != null && isFinal ? 2 : 1;
            }
        }
        return 0;
    }

    private boolean hasDefaultConstructor() {
        boolean hasConstructor = false;
        for (Element element : typeElement.getEnclosedElements()) {
            if (element.getKind() != ElementKind.CONSTRUCTOR) {
                continue;
            }
            hasConstructor = true;
            if (element.getModifiers().contains(Modifier.PRIVATE)) {
                continue;
            }
            ExecutableElement executableElement = (ExecutableElement) element;
            if (executableElement.getParameters().isEmpty()) {
                return true;
            }
        }
        return !hasConstructor;
    }

    private int[] findUserAllArgsConstructorPropIndices() {
        for (Element element : typeElement.getEnclosedElements()) {
            if (element.getKind() != ElementKind.CONSTRUCTOR) {
                continue;
            }
            if (element.getModifiers().contains(Modifier.PRIVATE)) {
                continue;
            }
            ExecutableElement executableElement = (ExecutableElement) element;
            int count = executableElement.getParameters().size();
            if (fieldElements.size() != count) {
                continue;
            }
            int[] indices = new int[count];
            for (int paramIndex = 0; paramIndex < count; paramIndex++) {
                VariableElement param = executableElement.getParameters().get(paramIndex);
                int matchedFieldIndex = -1;
                for (int fieldIndex = 0; fieldIndex < fieldElements.size(); fieldIndex++) {
                    VariableElement field = fieldElements.get(fieldIndex);
                    if (field.getSimpleName().toString().equals(param.getSimpleName().toString())) {
                        if (field.asType().equals(param.asType())) {
                            matchedFieldIndex = fieldIndex;
                            indices[paramIndex] = fieldIndex;
                            break;
                        }
                    }
                }
                if (matchedFieldIndex == -1) {
                    indices = null;
                    break;
                }
            }
            if (indices != null) {
                return indices;
            }
        }
        return null;
    }

    public void generate() {
        typeBuilder = TypeSpec
                .classBuilder(className.simpleName())
                .addSuperinterface(
                        ParameterizedTypeName.get(
                                Constants.TUPLE_MAPPER_CLASS_NAME,
                                ClassName.get(typeElement)
                        )
                )
                .addModifiers(Modifier.PUBLIC);
        generateFields();
        generateConstructor();
        generateGetSelections();
        generateCreateTuple();
        generateFirstMethod();
        int size = fieldElements.size();
        for (int i = 1; i < size; i++) {
            generateBuilderClass(
                    i,
                    fieldElements.get(i),
                    i + 1 < size ? fieldElements.get(i + 1) : null
            );
        }
        TypeSpec typeSpec = typeBuilder.build();
        try {
            JavaFile
                    .builder(
                            className.packageName(),
                            typeSpec
                    )
                    .indent("    ")
                    .build()
                    .writeTo(context.getFiler());
        } catch (IOException ex) {
            throw new GeneratorException(
                    String.format(
                            "Cannot generate draft interface for '%s'",
                            typeElement.getQualifiedName()
                    ),
                    ex
            );
        }
    }

    private void generateFields() {
        typeBuilder.addField(
                FieldSpec
                        .builder(SELECTION_ARR_TYPE_NAME, "selections")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build()
        );
    }

    private void generateConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addParameter(
                        ArrayTypeName.of(
                                ParameterizedTypeName.get(
                                        Constants.SELECTION_CLASS_NAME,
                                        WildcardTypeName.subtypeOf(TypeName.OBJECT)
                                )
                        ),
                        "selections"
                )
                .addStatement("this.selections = selections");
        typeBuilder.addMethod(builder.build());
    }

    private void generateGetSelections() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("getSelections")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(
                        ParameterizedTypeName.get(
                                Constants.LIST_CLASS_NAME,
                                ParameterizedTypeName.get(
                                        Constants.SELECTION_CLASS_NAME,
                                        WildcardTypeName.subtypeOf(TypeName.OBJECT)
                                )
                        )
                )
                .addStatement(
                        "return $T.unmodifiableList($T.asList(selections))",
                        Constants.COLLECTIONS_CLASS_NAME,
                        Constants.ARRAYS_CLASS_NAME
                );
        typeBuilder.addMethod(builder.build());
    }

    private void generateCreateTuple() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("createTuple")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(
                        ArrayTypeName.of(TypeName.OBJECT),
                        "args"
                )
                .returns(TypeName.get(typeElement.asType()));
        if (ctorPropIndices != null) {
            builder.addCode("return new $T($>\n", typeElement);
            for (int paramIndex = 0; paramIndex < ctorPropIndices.length; paramIndex++) {
                int fieldIndex = ctorPropIndices[paramIndex];
                if (paramIndex != 0) {
                    builder.addCode(",\n");
                }
                builder.addCode(
                        "($T)args[$L]",
                        TypeName.get(fieldElements.get(fieldIndex).asType()).box(),
                        fieldIndex
                );
            }
            builder.addCode("$<\n);");
        } else {
            builder.addStatement("$T __tuple = new $T()", typeElement, typeElement);
            for (int i = 0; i < fieldElements.size(); i++) {
                VariableElement field = fieldElements.get(i);
                builder.addStatement(
                        "__tuple.$L(($T)args[$L])",
                        StringUtil.identifier("set", field.getSimpleName().toString()),
                        TypeName.get(field.asType()).box(),
                        i
                );
            }
            builder.addStatement("return __tuple");
        }
        typeBuilder.addMethod(builder.build());
    }

    private void generateFirstMethod() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(fieldElements.get(0).getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(
                        ParameterizedTypeName.get(
                                Constants.SELECTION_CLASS_NAME,
                                TypeName.get(fieldElements.get(0).asType()).box()
                        ),
                        "selection"
                );
        VariableElement firstFieldElement = fieldElements.get(0);
        ClassName className = this.className
                .nestedClass(StringUtil.typeName(firstFieldElement.getSimpleName().toString()) + "Builder");
        builder.returns(className);
        builder.addStatement(
                "$T<?>[] selections = new $T<?>[$L]",
                Constants.SELECTION_CLASS_NAME,
                Constants.SELECTION_CLASS_NAME,
                fieldElements.size()
        );
        addMapCode(builder, 0, fieldElements.size() < 2 ? null : fieldElements.get(1));
        typeBuilder.addMethod(builder.build());
    }

    private void generateBuilderClass(
            int index,
            VariableElement fieldElement,
            @Nullable VariableElement nextFieldElement
    ) {
        TypeSpec.Builder builder = TypeSpec
                .classBuilder(StringUtil.typeName(fieldElement.getSimpleName().toString()) + "Builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addField(
                        FieldSpec
                                .builder(SELECTION_ARR_TYPE_NAME, "selections")
                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                .build()
                )
                .addMethod(
                        MethodSpec
                                .constructorBuilder()
                                .addParameter(
                                        SELECTION_ARR_TYPE_NAME,
                                        "selections"
                                )
                                .addStatement("this.selections = selections")
                                .build()
                )
                .addMethod(
                        newBuildMethod(
                                index,
                                fieldElement,
                                nextFieldElement
                        )
                );
        typeBuilder.addType(builder.build());
    }

    private MethodSpec newBuildMethod(
            int index,
            VariableElement fieldElement,
            @Nullable VariableElement nextFieldElement
    ) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(fieldElement.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(
                        ParameterizedTypeName.get(
                                Constants.SELECTION_CLASS_NAME,
                                TypeName.get(fieldElement.asType()).box()
                        ),
                        "selection"
                );
        addMapCode(builder, index, nextFieldElement);
        return builder.build();
    }

    private void addMapCode(MethodSpec.Builder builder, int index, VariableElement nextFieldElement) {
        builder.addStatement("selections[$L] = selection", index);
        if (nextFieldElement != null) {
            ClassName className = this.className
                    .nestedClass(StringUtil.typeName(nextFieldElement.getSimpleName().toString()) + "Builder");
            builder.returns(className);
            builder.addStatement("return new $T(selections)", className);
        } else {
            builder.returns(className);
            builder.addStatement("return new $T(selections)", className);
        }
    }
}
