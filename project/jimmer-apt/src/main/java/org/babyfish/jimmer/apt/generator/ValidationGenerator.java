package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.MetaException;

import javax.lang.model.element.*;
import javax.validation.ValidationException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Supplier;

public class ValidationGenerator {

    private static final Object[] EMPTY_ARGS = new Object[0];

    private final ImmutableProp prop;

    private final String valueName;

    private final Map<String, List<AnnotationMirror>> mirrorMultiMap;

    private final MethodSpec.Builder methodBuilder;

    public ValidationGenerator(
            ImmutableProp prop,
            String valueName,
            MethodSpec.Builder methodBuilder
    ) {

        this.prop = prop;
        this.valueName = valueName;
        this.mirrorMultiMap = Annotations.validateAnnotationMirrorMultiMap(prop);
        this.methodBuilder = methodBuilder;
    }

    public void generate() {
        if (!prop.isNullable() && !prop.getTypeName().isPrimitive()) {
            methodBuilder
                    .beginControlFlow("if ($L == null)", valueName)
                    .addCode("throw new IllegalArgumentException(\n")
                    .addCode(
                            "    \"'$L' cannot be null, please specify non-null value or use nullable annotation to decorate this property\"\n",
                            prop.getName()
                    )
                    .addCode(");\n")
                    .endControlFlow();
        }
        generateNotEmpty();
        generateNotBlank();
        generateSize();
        generateBound();
        generateEmail();
        generatePattern();
        generateConstraints();
    }

    private void generateNotEmpty() {
        List<AnnotationMirror> mirrors = mirrorMultiMap.get("NotEmpty");
        if (mirrors == null) {
            return;
        }
        if (!isSimpleClass(String.class) && !isSimpleClass(List.class)) {
            throw new MetaException(
                    prop.toElement(),
                    "it's decorated by the annotation @" +
                            Annotations.qualifiedName(mirrors.get(0)) +
                            " but its type is neither string nor list"
            );
        }
        validate(
                "$L.isEmpty()",
                new Object[]{ valueName },
                Annotations.annotationValue(mirrors.get(0), "message", ""),
                () -> "it cannot be empty"
        );
    }

    private void generateNotBlank() {
        List<AnnotationMirror> mirrors = mirrorMultiMap.get("NotBlank");
        if (mirrors == null) {
            return;
        }
        if (!isSimpleClass(String.class)) {
            throw new MetaException(
                    prop.toElement(),
                    "it's decorated by the annotation @" +
                            Annotations.qualifiedName(mirrors.get(0)) +
                            " but its type is not string"
            );
        }
        validate(
                "$L.trim().isEmpty()",
                new Object[]{ valueName },
                Annotations.annotationValue(mirrors.get(0), "message", ""),
                () -> "it cannot be empty"
        );
    }

    private void generateSize() {
        List<AnnotationMirror> mirrors = mirrorMultiMap.get("Size");
        if (mirrors == null) {
            return;
        }
        if (!isSimpleClass(String.class) && !isSimpleClass(List.class)) {
            throw new MetaException(
                    prop.toElement(),
                    "it's decorated by the annotation @" +
                            Annotations.qualifiedName(mirrors.get(0)) +
                            " but its type is neither string nor list"
            );
        }
        int min = 0;
        int max = Integer.MAX_VALUE;
        String minMessage = null;
        String maxMessage = null;
        for (AnnotationMirror mirror : mirrors) {
            int mirrorMin = Annotations.annotationValue(mirror, "min", 0);
            int mirrorMax = Annotations.annotationValue(mirror, "max", Integer.MAX_VALUE);
            if (mirrorMin > min) {
                min = mirrorMin;
                minMessage = Annotations.annotationValue(mirror, "message", "");
            }
            if (mirrorMax < max) {
                max = mirrorMax;
                maxMessage = Annotations.annotationValue(mirror, "message", "");
            }
        }
        if (min > max) {
            throw new MetaException(
                    prop.toElement(),
                    "its size validation rules is illegal " +
                            "so that there is not valid length"
            );
        }
        if (min == 0 && max == Integer.MAX_VALUE) {
            return;
        }
        String sizeFun = isSimpleClass(String.class) ? "length" : "size";
        if (min > 0) {
            final int finalValue = min;
            validate(
                    "$L.$L() < $L",
                    new Object[]{ valueName, sizeFun, finalValue },
                    minMessage,
                    () -> "it cannot be less than " + finalValue
            );
        }
        if (max < Integer.MAX_VALUE) {
            final int finalValue = max;
            validate(
                    "$L.$L() > $L",
                    new Object[]{ valueName, sizeFun, finalValue },
                    maxMessage,
                    () -> "it cannot be greater than " + finalValue
            );
        }
    }

    @SuppressWarnings({"unchecked", "rawtype"})
    private void generateBound() {

        List<AnnotationMirror> minList = mirrorMultiMap.get("Min");
        List<AnnotationMirror> maxList = mirrorMultiMap.get("Max");
        List<AnnotationMirror> positives = mirrorMultiMap.get("Positive");
        List<AnnotationMirror> positiveOrZeros = mirrorMultiMap.get("PositiveOrZero");
        List<AnnotationMirror> negatives = mirrorMultiMap.get("Negative");
        List<AnnotationMirror> negativeOrZeros = mirrorMultiMap.get("NegativeOrZero");
        List<AnnotationMirror>[] allMirrors = new List[] { minList, maxList, positives, positiveOrZeros, negatives, negativeOrZeros};
        AnnotationMirror mirror = Arrays.stream(allMirrors)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .findFirst()
                .orElse(null);
        if (mirror == null) {
            return;
        }
        if (!prop.getTypeName().isPrimitive() &&
                !prop.getTypeName().isBoxedPrimitive() &&
                !isSimpleClass(BigInteger.class) &&
                !isSimpleClass(BigDecimal.class)) {
            throw new MetaException(
                    prop.toElement(),
                    "it's decorated by the annotation @" +
                            Annotations.qualifiedName(mirror) +
                            " but its type is numeric"
            );
        }

        Long minValue = null;
        Long maxValue = null;
        String message = null;
        for (AnnotationMirror min : Annotations.nonNullList(minList)) {
            long mirrorMinValue = Annotations.annotationValue(min, "value", 0L);
            if (minValue == null || mirrorMinValue > minValue) {
                minValue = mirrorMinValue;
                message = Annotations.annotationValue(min, "message", "");
            }
        }
        for (AnnotationMirror positive : Annotations.nonNullList(positives)) {
            if (minValue == null || 1L > minValue) {
                minValue = 1L;
                message = Annotations.annotationValue(positive, "message", "");
            }
        }
        for (AnnotationMirror positiveOrZero : Annotations.nonNullList(positiveOrZeros)) {
            if (minValue == null || 0L > minValue) {
                minValue = 0L;
                message = Annotations.annotationValue(positiveOrZero, "message", "");
            }
        }
        for (AnnotationMirror max : Annotations.nonNullList(maxList)) {
            long mirrorMaxValue = Annotations.annotationValue(max, "value", 0L);
            if (maxValue == null || mirrorMaxValue < maxValue) {
                maxValue = mirrorMaxValue;
                message = Annotations.annotationValue(max, "message", "");
            }
        }
        for (AnnotationMirror negative : Annotations.nonNullList(negatives)) {
            if (maxValue == null || -1L < maxValue) {
                maxValue = -1L;
                message = Annotations.annotationValue(negative, "message", "");
            }
        }
        for (AnnotationMirror negativeOrZero : Annotations.nonNullList(negativeOrZeros)) {
            if (maxValue == null || 0L < maxValue) {
                maxValue = 0L;
                message = Annotations.annotationValue(negativeOrZero, "message", "");
            }
        }
        if (minValue != null && maxValue != null && minValue > maxValue) {
            throw new MetaException(
                    prop.toElement(),
                    "its numeric range validation rules is illegal " +
                            "so that there is not valid number"
            );
        }
        if (minValue != null) {
            validateBound(minValue, "<", message);
        }
        if (maxValue != null) {
            validateBound(maxValue, ">", message);
        }
    }

    private void generateEmail() {
        List<AnnotationMirror> mirrors = mirrorMultiMap.get("Email");
        if (mirrors == null) {
            return;
        }
        if (!isSimpleClass(String.class)) {
            throw new MetaException(
                    prop.toElement(),
                    "it's decorated by the annotation @" +
                            Annotations.qualifiedName(mirrors.get(0)) +
                            " but its type is not string"
            );
        }
        validate(
                "!$L.matcher($L).matches()",
                new Object[]{ Constants.DRAFT_FIELD_EMAIL_PATTERN, valueName },
                Annotations.annotationValue(mirrors.get(0), "message", ""),
                () -> "it is not email address"
        );
    }

    private void generatePattern() {
        List<AnnotationMirror> mirrors = mirrorMultiMap.get("Pattern");
        if (mirrors == null) {
            return;
        }
        if (!isSimpleClass(String.class)) {
            throw new MetaException(
                    prop.toElement(),
                    "it's decorated by the annotation @" +
                            Annotations.qualifiedName(mirrors.get(0)) +
                            " but its type is not string"
            );
        }
        for (int i = 0; i < mirrors.size(); i++) {
            final int index = i;
            validate(
                    "!$L.matcher($L).matches()",
                    new Object[]{ Constants.regexpPatternFieldName(prop, i), valueName },
                    Annotations.annotationValue(mirrors.get(i), "message", ""),
                    () -> "it does not match the regexp '" +
                            Annotations.annotationValue(mirrors.get(index), "regexp", "")
                                    .replace("\\", "\\\\") +
                            "'"
            );
        }
    }

    private void generateConstraints() {
        for (Map.Entry<ClassName, String> e : prop.getValidationMessageMap().entrySet()) {
            methodBuilder.addStatement(
                    "$L.validate($L)",
                    Constants.validatorFieldName(prop, e.getKey()),
                    prop.getName()
            );
        }
    }

    private void validate(
            String condition,
            Object[] args,
            String errorMessage,
            Supplier<String> defaultMessageSupplier
    ) {
        if (!prop.isNullable() || prop.getTypeName().isPrimitive()) {
            methodBuilder.beginControlFlow(
                    "if (" + condition + ")",
                    args != null ? args : EMPTY_ARGS
            );
        } else {
            methodBuilder.beginControlFlow(
                    "if (" + valueName + " != null && " + condition + ")",
                    args != null ? args : EMPTY_ARGS
            );
        }
        if (errorMessage == null ||
                errorMessage.isEmpty() ||
                errorMessage.startsWith("{javax.validation.constraints.")
        ) {
            errorMessage = "Illegal value '\" + " +
                    valueName +
                    " + \"' for property '" +
                    prop +
                    "', " +
                    defaultMessageSupplier.get();
            methodBuilder.addStatement(
                    "throw new $T(\"$L\")",
                    ValidationException.class,
                    errorMessage
            );
        }
        methodBuilder.endControlFlow();
    }

    private boolean isSimpleClass(Class<?> type) {
        TypeName typeName = prop.getTypeName();
        ClassName className;
        if (typeName instanceof ClassName) {
            className = ((ClassName)typeName);
        } else if (typeName instanceof ParameterizedTypeName) {
            className = ((ParameterizedTypeName)typeName).rawType;
        } else {
            return false;
        }
        return className.packageName().equals(type.getPackage().getName()) &&
                className.simpleNames().size() == 1 &&
                className.simpleName().equals(type.getSimpleName());
    }

    private void validateBound(long bound, String cmp, String message) {
        String bigNumLiteral;
        if (prop.getTypeName().equals(ClassName.get(BigDecimal.class))) {
            if (bound == 0) {
                bigNumLiteral = "$T.ZERO";
            } else if (bound == 1) {
                bigNumLiteral = "$T.ONE";
            } else if (bound == 2) {
                bigNumLiteral = "$T.TWO";
            } else if (bound == 10) {
                bigNumLiteral = "$T.TEN";
            } else {
                bigNumLiteral = "$T.valueOf(" + bound + ")";
            }
        } else if (prop.getTypeName().equals(ClassName.get(BigInteger.class))) {
            if (bound == -1) {
                bigNumLiteral = "$T.NEGATIVE_ONE";
            } else if (bound == 0) {
                bigNumLiteral = "$T.ZERO";
            } else if (bound == 1) {
                bigNumLiteral = "$T.ONE";
            } else if (bound == 2) {
                bigNumLiteral = "$T.TWO";
            } else if (bound == 10) {
                bigNumLiteral = "$T.TEN";
            } else {
                bigNumLiteral = "$T.valueOf(" + bound + ", 0)";
            }
        } else {
            bigNumLiteral = null;
        }
        validate(
                bigNumLiteral != null ?
                        "$L.compareTo(" + bigNumLiteral + ") $L 0" :
                        "$L $L $L",
                bigNumLiteral != null ?
                        new Object[] { valueName, prop.getElementType(), cmp } :
                        new Object[] { valueName, cmp, bound },
                message,
                () -> "it cannot be " +
                        (cmp.equals("<") ? "less than" : "greater than") +
                        " " +
                        bound
        );
    }
}
