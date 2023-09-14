package org.babyfish.jimmer.sql.meta.impl;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.GenerationType;
import org.babyfish.jimmer.sql.meta.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Map;

public class IdGenerators {

    private IdGenerators() {}

    public static IdGenerator of(ImmutableType type, SqlContext sqlContext) {
        ImmutableProp idProp = type.getIdProp();

        GeneratedValue generatedValue = idProp.getAnnotation(GeneratedValue.class);
        if (generatedValue == null) {
            return null;
        }

        Class<? extends UserIdGenerator<?>> generatorType = generatedValue.generatorType();
        String generatorRef = generatedValue.generatorRef();

        GenerationType strategy = generatedValue.strategy();
        GenerationType strategyFromGeneratorType = GenerationType.AUTO;
        GenerationType strategyFromSequenceName = GenerationType.AUTO;

        if (generatorType != UserIdGenerator.None.class && !generatorRef.isEmpty()) {
            throw new ModelException(
                    "Illegal property \"" + idProp + "\", the " +
                            "`generatorType` and `generatorRef` cannot be specified at same time"
            );
        }
        if (generatorType != UserIdGenerator.None.class || !generatorRef.isEmpty()) {
            strategyFromGeneratorType = GenerationType.USER;
        }

        if (!generatedValue.sequenceName().isEmpty()) {
            strategyFromSequenceName = GenerationType.SEQUENCE;
        }

        if (strategy == GenerationType.USER && strategyFromGeneratorType != GenerationType.USER) {
            throw new ModelException(
                    "Illegal property \"" +
                            idProp +
                            "\", its generator strategy is explicitly specified to \"USER\"," +
                            "but its generator type does not implement " +
                            UserIdGenerator.class.getName()
            );
        }
        if (strategy != GenerationType.AUTO &&
                strategyFromGeneratorType != GenerationType.AUTO &&
                strategy != strategyFromGeneratorType) {
            throw new ModelException(
                    "Illegal property \"" +
                            idProp +
                            "\", it's decorated by the annotation @" +
                            GeneratedValue.class.getName() +
                            " but that annotation has conflict attributes 'strategy' and 'generatorType'"
            );
        }
        if (strategy != GenerationType.AUTO &&
                strategyFromSequenceName != GenerationType.AUTO &&
                strategy != strategyFromSequenceName) {
            throw new ModelException(
                    "Illegal property \"" +
                            idProp +
                            "\", it's decorated by the annotation @" +
                            GeneratedValue.class.getName() +
                            " but that annotation has conflict attributes 'strategy' and 'sequenceName'"
            );
        }
        if (strategyFromGeneratorType != GenerationType.AUTO &&
                strategyFromSequenceName != GenerationType.AUTO) {
            throw new ModelException(
                    "Illegal property \"" +
                            idProp +
                            "\", it's decorated by the annotation @" +
                            GeneratedValue.class.getName() +
                            " but that annotation has conflict attributes 'generatorType' and 'sequenceName'"
            );
        }

        if (strategy == GenerationType.AUTO) {
            strategy = strategyFromGeneratorType;
        }
        if (strategy == GenerationType.AUTO) {
            strategy = strategyFromSequenceName;
        }
        if (strategy == GenerationType.AUTO) {
            throw new ModelException(
                    "Illegal property \"" +
                            idProp +
                            "\", it's decorated by the annotation @" +
                            GeneratedValue.class.getName() +
                            " but that annotation does not have any attributes"
            );
        }

        if ((strategy == GenerationType.IDENTITY || strategy == GenerationType.SEQUENCE)) {
            Class<?> returnType = idProp.getElementClass();
            if (!returnType.isPrimitive() && !Number.class.isAssignableFrom(returnType)) {
                throw new ModelException(
                        "Illegal property \"" +
                                idProp +
                                "\", it's id generation strategy is \"" +
                                strategy +
                                "\", but that the type of id is not numeric"
                );
            }
        } else if (generatorType != UserIdGenerator.None.class) {
            Class<?> returnType = idProp.getElementClass();
            Map<?, Type> typeArguments = TypeUtils.getTypeArguments(generatorType, UserIdGenerator.class);
            Class<?> parsedType = null;
            if (!typeArguments.isEmpty()) {
                Type typeArgument = typeArguments.values().iterator().next();
                if (typeArgument instanceof Class<?>) {
                    parsedType = (Class<?>) typeArgument;
                }
            }
            if (parsedType == null) {
                throw new ModelException(
                        "Illegal property \"" +
                                idProp +
                                "\", the generator type is \"" +
                                generatorType.getName() +
                                "\" does support type argument for \"" +
                                UserIdGenerator.class +
                                "\""
                );
            }
            if (!Classes.matches(parsedType, returnType)) {
                throw new ModelException(
                        "Illegal property \"" +
                                idProp +
                                "\", the generator type is \"" +
                                generatorType.getName() +
                                "\" generates id whose type is \"" +
                                parsedType.getName() +
                                "\" but the property returns \"" +
                                returnType.getName() +
                                "\""
                );
            }
        }

        IdGenerator idGenerator = null;
        if (strategy == GenerationType.USER) {
            String error = null;
            Throwable errorCause = null;
            if (!generatorRef.isEmpty()) {
                try {
                    idGenerator = sqlContext.getUserIdGenerator(generatorRef);
                } catch (Exception ex) {
                    error = "cannot get id generator named \"" + generatorRef + "\" from IOC framework";
                    errorCause = ex instanceof InvocationTargetException ?
                            ((InvocationTargetException) ex).getTargetException() :
                            ex;
                }
            } else {
                try {
                    idGenerator = sqlContext.getUserIdGenerator(generatorType);
                } catch (Exception ex) {
                    error = "cannot create the instance of \"" + generatorType.getName() + "\"";
                    errorCause = ex instanceof InvocationTargetException ?
                            ((InvocationTargetException) ex).getTargetException() :
                            ex;
                }
            }
            if (error != null) {
                throw new ModelException(
                        "Illegal property \"" + idProp + "\" with the annotation @GeneratedValue, " + error,
                        errorCause
                );
            }
        } else if (strategy == GenerationType.IDENTITY) {
            idGenerator = IdentityIdGenerator.INSTANCE;
        } else if (strategy == GenerationType.SEQUENCE) {
            String sequenceName = generatedValue.sequenceName();
            if (sequenceName.isEmpty()) {
                sequenceName = sqlContext.getMetadataStrategy().getNamingStrategy().sequenceName(idProp.getDeclaringType());
            }
            idGenerator = new SequenceIdGenerator(sequenceName);
        }
        return idGenerator;
    }
}
