package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.MetaException;

import javax.lang.model.element.AnnotationMirror;
import javax.validation.ValidationException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        generateAssert();
        generateDigits();
        generateTime();
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
                new Object[]{valueName},
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
                new Object[]{valueName},
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
                    new Object[]{valueName, sizeFun, finalValue},
                    minMessage,
                    () -> "it cannot be less than " + finalValue
            );
        }
        if (max < Integer.MAX_VALUE) {
            final int finalValue = max;
            validate(
                    "$L.$L() > $L",
                    new Object[]{valueName, sizeFun, finalValue},
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
        List<AnnotationMirror> decimalMinList = mirrorMultiMap.get("DecimalMin");
        List<AnnotationMirror> decimalMaxList = mirrorMultiMap.get("DecimalMax");
        List<AnnotationMirror>[] allMirrors = new List[]{minList, maxList, positives, positiveOrZeros, negatives, negativeOrZeros, decimalMinList, decimalMaxList};
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

        BigDecimal minValue = null;
        BigDecimal maxValue = null;
        String message = null;
        for (AnnotationMirror min : Annotations.nonNullList(minList)) {
            BigDecimal mirrorMinValue = new BigDecimal(Annotations.annotationValue(min, "value", 0L));
            if (minValue == null || mirrorMinValue.compareTo(minValue) > 0) {
                minValue = mirrorMinValue;
                message = Annotations.annotationValue(min, "message", "");
            }
        }
        for (AnnotationMirror decimalMin : Annotations.nonNullList(decimalMinList)) {
            BigDecimal mirrorDecimalMinValue = new BigDecimal(Annotations.annotationValue(decimalMin, "value", "0"));
            if (minValue == null || mirrorDecimalMinValue.compareTo(minValue) > 0) {
                minValue = mirrorDecimalMinValue;
                message = Annotations.annotationValue(decimalMin, "message", "");
            }
        }
        for (AnnotationMirror positive : Annotations.nonNullList(positives)) {
            if (minValue == null || new BigDecimal(1L).compareTo(minValue) > 0) {
                minValue = new BigDecimal(1L);
                message = Annotations.annotationValue(positive, "message", "");
            }
        }
        for (AnnotationMirror positiveOrZero : Annotations.nonNullList(positiveOrZeros)) {
            if (minValue == null || new BigDecimal(0L).compareTo(minValue) > 0) {
                minValue = new BigDecimal(0L);
                message = Annotations.annotationValue(positiveOrZero, "message", "");
            }
        }
        for (AnnotationMirror max : Annotations.nonNullList(maxList)) {
            BigDecimal mirrorMaxValue = new BigDecimal(Annotations.annotationValue(max, "value", 0L));
            if (maxValue == null || mirrorMaxValue.compareTo(maxValue) < 0) {
                maxValue = mirrorMaxValue;
                message = Annotations.annotationValue(max, "message", "");
            }
        }
        for (AnnotationMirror decimalMax : Annotations.nonNullList(decimalMaxList)) {
            BigDecimal mirrorDecimalMaxValue = new BigDecimal(Annotations.annotationValue(decimalMax, "value", "0"));
            if (maxValue == null || mirrorDecimalMaxValue.compareTo(maxValue) < 0) {
                maxValue = mirrorDecimalMaxValue;
                message = Annotations.annotationValue(decimalMax, "message", "");
            }
        }
        for (AnnotationMirror negative : Annotations.nonNullList(negatives)) {
            if (maxValue == null || new BigDecimal(-1L).compareTo(maxValue) < 0) {
                maxValue = new BigDecimal(-1L);
                message = Annotations.annotationValue(negative, "message", "");
            }
        }
        for (AnnotationMirror negativeOrZero : Annotations.nonNullList(negativeOrZeros)) {
            if (maxValue == null || new BigDecimal(0L).compareTo(maxValue) < 0) {
                maxValue = new BigDecimal(0L);
                message = Annotations.annotationValue(negativeOrZero, "message", "");
            }
        }
        if (minValue != null && maxValue != null && minValue.compareTo(maxValue) > 0) {
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
                new Object[]{Constants.DRAFT_FIELD_EMAIL_PATTERN, valueName},
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
                    new Object[]{Constants.regexpPatternFieldName(prop, i), valueName},
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

    private void generateAssert() {
        List<AnnotationMirror> assertFalseList = mirrorMultiMap.get("AssertFalse");
        List<AnnotationMirror> assertTrueList = mirrorMultiMap.get("AssertTrue");

        List<AnnotationMirror>[] allMirrors = new List[]{
                assertFalseList,
                assertTrueList
        };

        AnnotationMirror mirror = Arrays.stream(allMirrors)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .findFirst()
                .orElse(null);

        if (mirror == null) {
            return;
        }

        if (!prop.getTypeName().equals(TypeName.BOOLEAN)
                && !prop.getTypeName().equals(TypeName.BOOLEAN.box())) {
            throw new MetaException(
                    prop.toElement(),
                    "it's decorated by the annotation @" +
                            Annotations.qualifiedName(mirror) +
                            " but its type is not boolean"
            );
        }

        for (AnnotationMirror assertFalse : Annotations.nonNullList(assertFalseList)) {
            validate(
                    valueName + " != false",
                    null,
                    Annotations.annotationValue(assertFalse, "message", ""),
                    () -> "it is not false"
            );
        }

        for (AnnotationMirror assertTrue : Annotations.nonNullList(assertTrueList)) {
            validate(
                    valueName + " != true",
                    null,
                    Annotations.annotationValue(assertTrue, "message", ""),
                    () -> "it is not true"
            );
        }
    }

    private void generateDigits() {
        List<AnnotationMirror> digitsList = mirrorMultiMap.get("Digits");

        if (digitsList == null) {
            return;
        }

        if (!prop.getTypeName().isPrimitive()
                && !prop.getTypeName().isBoxedPrimitive()
                && !isSimpleClass(BigDecimal.class)
                && !isSimpleClass(BigInteger.class)
                && !isSimpleClass(CharSequence.class)) {
            throw new MetaException(
                    prop.toElement(),
                    "it's decorated by the annotation @Digits " +
                            "but its type is not primitive, boxed primitive, BigDecimal, BigInteger or CharSequence");
        }

        for (AnnotationMirror digits : digitsList) {
            int integer = Annotations.annotationValue(digits, "integer", 0);
            int fraction = Annotations.annotationValue(digits, "fraction", 0);
            if (integer < 0 || fraction < 0) {
                throw new MetaException(
                        prop.toElement(),
                        "its numeric range validation rules is illegal " +
                                "so that there is not valid number"
                );
            }
            if (integer == 0 && fraction == 0) {
                throw new MetaException(
                        prop.toElement(),
                        "its numeric range validation rules is illegal " +
                                "so that there is not valid number"
                );
            }

            if (prop.getTypeName().equals(TypeName.get(BigDecimal.class))) {
                validate(
                        "$L.precision() > $L",
                        new Object[]{valueName, integer},
                        Annotations.annotationValue(digits, "message", ""),
                        () -> "its integer digits is greater than " + integer
                );
                validate(
                        "$L.scale() > $L",
                        new Object[]{valueName, fraction},
                        Annotations.annotationValue(digits, "message", ""),
                        () -> "its fraction digits is greater than " + fraction
                );
            } else if (prop.getTypeName().equals(TypeName.get(BigInteger.class))) {
                validate(
                        "$L.bitLength() > $L",
                        new Object[]{valueName, integer},
                        Annotations.annotationValue(digits, "message", ""),
                        () -> "its integer digits is greater than " + integer
                );
            } else if (prop.getTypeName().equals(TypeName.get(CharSequence.class))) {
                validate(
                        "$L.length() > $L",
                        new Object[]{valueName, integer},
                        Annotations.annotationValue(digits, "message", ""),
                        () -> "its integer digits is greater than " + integer
                );
            } else if (prop.getTypeName().isPrimitive() || prop.getTypeName().isBoxedPrimitive()) {
                validate(
                        "new $T($L).precision() > $L",
                        new Object[]{BigDecimal.class, valueName, integer},
                        Annotations.annotationValue(digits, "message", ""),
                        () -> "its integer digits is greater than " + integer
                );
            }
        }
    }

    private void generateTime() {
        List<AnnotationMirror> pastOrPresents = mirrorMultiMap.get("PastOrPresent");
        List<AnnotationMirror> pasts = mirrorMultiMap.get("Past");
        List<AnnotationMirror> futureOrPresents = mirrorMultiMap.get("FutureOrPresent");
        List<AnnotationMirror> futures = mirrorMultiMap.get("Future");

        List<AnnotationMirror>[] allMirrors = new List[]{
                pastOrPresents,
                pasts,
                futureOrPresents,
                futures,
        };

        AnnotationMirror mirror = Arrays.stream(allMirrors)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .findFirst()
                .orElse(null);


        if (mirror == null) {
            return;
        }

        if (!isSimpleClass(LocalDate.class)
                && isSimpleClass(LocalDateTime.class)
                && isSimpleClass(LocalTime.class)) {
            throw new MetaException(
                    prop.toElement(),
                    "it's decorated by the annotation @" +
                            Annotations.qualifiedName(mirror) +
                            " but its type is not LocalDate, LocalDateTime or LocalTime"
            );
        }

        for (AnnotationMirror pastOrPresent : Annotations.nonNullList(pastOrPresents)) {
            if (prop.getTypeName().equals(TypeName.get(LocalDate.class))) {
                validate(
                        "$L.isAfter($T.now())",
                        new Object[]{valueName, LocalDate.class},
                        Annotations.annotationValue(pastOrPresent, "message", ""),
                        () -> "it is not before or equal to now"
                );
            } else if (prop.getTypeName().equals(TypeName.get(LocalDateTime.class))) {
                validate(
                        "$L.isAfter($T.now())",
                        new Object[]{valueName, LocalDateTime.class},
                        Annotations.annotationValue(pastOrPresent, "message", ""),
                        () -> "it is not before or equal to now"
                );
            } else if (prop.getTypeName().equals(TypeName.get(LocalTime.class))) {
                validate(
                        "$L.isAfter($T.now())",
                        new Object[]{valueName, LocalTime.class},
                        Annotations.annotationValue(pastOrPresent, "message", ""),
                        () -> "it is not before or equal to now"
                );
            }
        }


        for (AnnotationMirror past : Annotations.nonNullList(pasts)) {
            if (prop.getTypeName().equals(TypeName.get(LocalDate.class))) {
                validate(
                        "$L.isAfter($T.now()) || $L.isEqual($T.now())",
                        new Object[]{valueName, LocalDate.class, valueName, LocalDate.class},
                        Annotations.annotationValue(past, "message", ""),
                        () -> "it is not before now"
                );
            } else if (prop.getTypeName().equals(TypeName.get(LocalDateTime.class))) {
                validate(
                        "$L.isAfter($T.now()) || $L.isEqual($T.now())",
                        new Object[]{valueName, LocalDateTime.class, valueName, LocalDateTime.class},
                        Annotations.annotationValue(past, "message", ""),
                        () -> "it is not before now"
                );
            } else if (prop.getTypeName().equals(TypeName.get(LocalTime.class))) {
                validate(
                        "$L.isAfter($T.now()) || $L.isEqual($T.now())",
                        new Object[]{valueName, LocalTime.class, valueName, LocalTime.class},
                        Annotations.annotationValue(past, "message", ""),
                        () -> "it is not before now"
                );
            }
        }

        for (AnnotationMirror futureOrPresent : Annotations.nonNullList(futureOrPresents)) {
            if (prop.getTypeName().equals(TypeName.get(LocalDate.class))) {
                validate(
                        "$L.isBefore($T.now())",
                        new Object[]{valueName, LocalDate.class},
                        Annotations.annotationValue(futureOrPresent, "message", ""),
                        () -> "it is not after or equal to now"
                );
            } else if (prop.getTypeName().equals(TypeName.get(LocalDateTime.class))) {
                validate(
                        "$L.isBefore($T.now())",
                        new Object[]{valueName, LocalDateTime.class},
                        Annotations.annotationValue(futureOrPresent, "message", ""),
                        () -> "it is not after or equal to now"
                );
            } else if (prop.getTypeName().equals(TypeName.get(LocalTime.class))) {
                validate(
                        "$L.isBefore($T.now())",
                        new Object[]{valueName, LocalTime.class},
                        Annotations.annotationValue(futureOrPresent, "message", ""),
                        () -> "it is not after or equal to now"
                );
            }
        }

        for (AnnotationMirror future : Annotations.nonNullList(futures)) {
            if (prop.getTypeName().equals(TypeName.get(LocalDate.class))) {
                validate(
                        "$L.isBefore($T.now()) || $L.isEqual($T.now())",
                        new Object[]{valueName, LocalDate.class, valueName, LocalDate.class},
                        Annotations.annotationValue(future, "message", ""),
                        () -> "it is not after now"
                );
            } else if (prop.getTypeName().equals(TypeName.get(LocalDateTime.class))) {
                validate(
                        "$L.isBefore($T.now()) || $L.isEqual($T.now())",
                        new Object[]{valueName, LocalDateTime.class, valueName, LocalDateTime.class},
                        Annotations.annotationValue(future, "message", ""),
                        () -> "it is not after now"
                );
            } else if (prop.getTypeName().equals(TypeName.get(LocalTime.class))) {
                validate(
                        "$L.isBefore($T.now()) || $L.isEqual($T.now())",
                        new Object[]{valueName, LocalTime.class, valueName, LocalTime.class},
                        Annotations.annotationValue(future, "message", ""),
                        () -> "it is not after now"
                );
            }
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
            className = ((ClassName) typeName);
        } else if (typeName instanceof ParameterizedTypeName) {
            className = ((ParameterizedTypeName) typeName).rawType;
        } else {
            return false;
        }
        return className.packageName().equals(type.getPackage().getName()) &&
                className.simpleNames().size() == 1 &&
                className.simpleName().equals(type.getSimpleName());
    }

    private void validateBound(BigDecimal bound, String cmp, String message) {
        String bigNumLiteral;
        if (prop.getTypeName().equals(ClassName.get(BigDecimal.class))) {
            if (bound.compareTo(BigDecimal.ZERO) == 0) {
                bigNumLiteral = "$T.ZERO";
            } else if (bound.compareTo(BigDecimal.ONE) == 0) {
                bigNumLiteral = "$T.ONE";
            } else if (bound.compareTo(BigDecimal.TEN) == 0) {
                bigNumLiteral = "$T.TEN";
            } else {
                bigNumLiteral = "$T.valueOf(" + bound + ", 0)";
            }
        } else if (prop.getTypeName().equals(ClassName.get(BigInteger.class))) {
            if (bound.compareTo(new BigDecimal(-1L)) == 0) {
                bigNumLiteral = "$T.NEGATIVE_ONE";
            } else if (bound.compareTo(BigDecimal.ZERO) == 0) {
                bigNumLiteral = "$T.ZERO";
            } else if (bound.compareTo(BigDecimal.ONE) == 0) {
                bigNumLiteral = "$T.ONE";
            } else if (bound.compareTo(BigDecimal.TEN) == 0) {
                bigNumLiteral = "$T.TEN";
            } else {
                bigNumLiteral = "$T.valueOf(" + bound + ")";
            }
        } else {
            bigNumLiteral = null;
        }
        validate(
                bigNumLiteral != null ?
                        "$L.compareTo(" + bigNumLiteral + ") $L 0" :
                        "$L $L $L",
                bigNumLiteral != null ?
                        new Object[]{valueName, prop.getElementType(), cmp} :
                        new Object[]{valueName, cmp, bound},
                message,
                () -> "it cannot be " +
                        (cmp.equals("<") ? "less than" : "greater than") +
                        " " +
                        bound
        );
    }
}
