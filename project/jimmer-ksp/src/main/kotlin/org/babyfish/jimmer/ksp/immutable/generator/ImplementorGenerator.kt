package org.babyfish.jimmer.ksp.immutable.generator

import com.squareup.kotlinpoet.*
import org.babyfish.jimmer.impl.util.StringUtil
import org.babyfish.jimmer.jackson.ImmutableModuleRequiredException
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableProp
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableType
import org.babyfish.jimmer.ksp.util.generatedAnnotation
import org.babyfish.jimmer.meta.PropId
import kotlin.reflect.KClass

class ImplementorGenerator(
    private val ctx: Context,
    private val type: ImmutableType,
    private val parent: TypeSpec.Builder
) {

    fun generate() {
        parent.addType(
            TypeSpec
                .interfaceBuilder(IMPLEMENTOR)
                .addAnnotation(generatedAnnotation(type))
                .addModifiers(KModifier.PRIVATE, KModifier.ABSTRACT)
                .addSuperinterface(type.className)
                .addSuperinterface(IMMUTABLE_SPI_CLASS_NAME)
                .apply {
                    addPropertyOrderAnnotation()
                    addGetFun(PropId::class)
                    addGetFun(String::class)
                    addTypeFun()
                    addDummyPropForNoImmutableModuleError()
                    addCompanionObject()
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addPropertyOrderAnnotation() {
        addAnnotation(
            AnnotationSpec
                .builder(ctx.jacksonTypes.jsonPropertyOrder)
                .addMember(
                    CodeBlock
                        .builder()
                        .add("%S", "dummyPropForJacksonError__")
                        .apply {
                            for (prop in type.propsOrderById) {
                                add(", %S", prop.name)
                            }
                        }
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addGetFun(
        argType: KClass<*>
    ) {
        addFunction(
            FunSpec
                .builder("__get")
                .addParameter("prop", argType)
                .addModifiers(KModifier.OVERRIDE)
                .returns(ANY.copy(nullable = true))
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            val appender = CaseAppender(this, type, argType)
                            if (argType == PropId::class) {
                                beginControlFlow("return when (prop.asIndex())")
                                appender.addIllegalCase()
                                addStatement("__get(prop.asName())")
                            } else {
                                beginControlFlow("return when (prop)")
                            }
                            for (prop in type.propsOrderById) {
                                appender.addCase(prop)
                                addStatement(prop.name)
                            }
                            addElseForNonExistingProp(type, argType)
                            endControlFlow()
                        }
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addTypeFun() {
        addFunction(
            FunSpec
                .builder("__type")
                .addModifiers(KModifier.OVERRIDE)
                .returns(IMMUTABLE_TYPE_CLASS_NAME)
                .addCode("return %T.type", type.draftClassName(PRODUCER))
                .build()
        )
    }

    private fun TypeSpec.Builder.addDummyPropForNoImmutableModuleError() {
        addProperty(
            PropertySpec
                .builder("dummyPropForJacksonError__", INT)
                .getter(
                    FunSpec
                        .getterBuilder()
                        .addStatement("throw %T()", ImmutableModuleRequiredException::class)
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addCompanionObject() {
        val deeperPropIdPropMap = mutableMapOf<String, ImmutableProp>()
        for (prop in type.properties.values) {
            val deeperPropIdName = deeperPropIdPropName(prop)
            if (deeperPropIdName !== null) {
                deeperPropIdPropMap[deeperPropIdName] = prop
            }
        }
        if (deeperPropIdPropMap.isNotEmpty()) {
            addType(
                TypeSpec.companionObjectBuilder()
                    .apply {
                        for ((deeperPropIdPropName, prop) in deeperPropIdPropMap) {
                            addProperty(
                                PropertySpec
                                    .builder(
                                        deeperPropIdPropName,
                                        PROP_ID_CLASS_NAME
                                    )
                                    .initializer(
                                        "%T.type.getProp(%S).getManyToManyViewBaseDeeperProp().getId()",
                                        type.draftClassName("$"),
                                        prop.name
                                    )
                                    .build()
                            )
                        }
                    }
                    .build()
            )
        }
    }

    companion object {
        internal fun deeperPropIdPropName(prop: ImmutableProp): String? =
            prop.manyToManyViewBaseDeeperProp?.let {
                "DEEP_PROP_ID_" + StringUtil.snake(prop.name, StringUtil.SnakeCase.UPPER)
            }
    }
}