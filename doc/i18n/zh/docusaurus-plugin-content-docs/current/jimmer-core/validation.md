---
sidebar_position: 5
title: 验证
---

验证是jimmer对象一个附带的功能，保证对象数据符合业务规则。

:::info
jimmer对象的验证在Annotation Processor生成的源代码中直接硬编码实现，Annotation Procssor一旦完成，就拥有了完整的验证功能，无需任何框架层面的拦截和性能开销。
:::

## Nullity

jimmer使用如下手段判断字段是否允许为null。

对jimmer不可变数据接口中定义的任何一个属性，Annotation processor会先进行两个子判定

1. 这些规则任何一条满足，则判断为非null

    - 是否被`@javax.validation.constraints.NotNull`修饰。
    - 是否被`@org.jetbrains.annotations.NotNull`修饰。
    - 是否被`@org.springframework.lang.NonNull`修饰。
    - 是否被`@org.babyfish.jimmer.sql.Id`修饰（jimmer-sql部分的内容）。
    - 是否是集合类型属性。
    - 是否是原生类型`boolean`、`char`、`byte`、`short`、`int`、`long`、`float`和`double`之一。

2. 这些规则任何一条满足，则判断为可null

    - 是否被`@javax.validation.constraints.Null`修饰。
    - 是否被`@org.jetbrains.annotations.Nullable`修饰。
    - 是否被`@org.springframework.lang.Nullable`修饰。
    - 是否是基于中间表映射的多对一关联（jimmer-sql部分的内容）。
    - 是否是原生类型的装箱类型`java.lang.Boolean`、`java.lang.Character`、`java.lang.Byte`、`java.lang.Short`、`java.lang.Integer`、`java.lang.Long`、`java.lang.Float`和`java.lang.Double`之一。

最后，合并判定。

- 如果既被判定成非null，又被判定为可null，AnnotationProcessor会报错，告诉用户在不可变接口声明中包含矛盾的配置。
- 如果仅被判定成非null，而没被判定为可null，则该属性非null。
- 如果仅被判定成可null，而没被判定为非null，则该属性可null。
- 如果既没被判定成非null，又没被判定为可null，则参考属性所在的接口的注解`@org.babyfish.jimmer.Immutable`的`value`值。
    - 如果接口被注解`@Immutable`修饰且其`value`为`NULLABLE`，则该属性可null。
    - 否则，该属性非null。

## 其它验证

除了Nullity外，jimmer实现了部分JSR380的验证。截止到目前为止，支持的验证规则有

- 任意的自定义验证注解，自定义注解本身需要被`@javax.validation.constraints.Constraint`修饰
- `@javax.validation.constraints.NotEmpty`
- `@javax.validation.constraints.NotBlank`
- `@javax.validation.constraints.Size`
- `@javax.validation.constraints.Min`
- `@javax.validation.constraints.Max`
- `@javax.validation.constraints.Positive`
- `@javax.validation.constraints.MositiveOrZero`
- `@javax.validation.constraints.Negative`
- `@javax.validation.constraints.NegativeOrZero`
- `@javax.validation.constraints.Email`
- `@javax.validation.constraints.Pattern`

:::note
后续版本会不断完善此功能，支持的规则会逐渐变多。
:::