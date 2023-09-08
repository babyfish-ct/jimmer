# 使用jimmer颠覆你的业务开发体验

## 写在开头

> There are two hard things in computer science: cache invalidation, naming things, and off-by-one errors.--Leon Bambrick

如果你是一个有不少开发经验的程序员，那么你看到上面这句名言时，大概会会心一笑。这句话一点也不夸张，是日常开发中的真实写照，有无数的程序员每天都被命名和缓存困扰着。

那么简单的介绍一下我自己，一个有7年开发经验的普普通通的程序员，主要使用的编程语言是java，Golang、Kotlin、Python也都会一点点，就职过的公司说得上号的也就是中行以及现在的中电科了。

## 使用过的ORM框架体验感受

我相信作为一名java web后端的业务开发程序员，最经常打交道的一个环节就是对数据库表的操作了吧，所以ORM框架可以说是业务开发中必不可少的一环。市面上常见的ORM框架，我基本可以说是都有过长时间的使用体验了：JPA、QueryDSL、Jooq、JdbcTemplate、MyBatis、MyBatis-plus、Fluent-MyBatis等等。

这些框架基本上可以大致分为两类，一类是以`JPA`或者`Hibernate`为代表的面向对象的ORM，另一类是以`MyBatis`为代表的写原生sql的ORM。

这两类各有优劣，以`JPA`为代表的ORM框架，符合`Java`面向对象的思维，以操作对象来替代操作数据库表，具有强类型、隔离SQL方言的优势，但缺点是在动态条件查询很弱，书写起来很复杂，而且如果要只查询某些列而不是整个对象时，也比较复杂。以`MyBatis`为代表的ORM框架，优点是足够灵活，直接写SQL的方式使得其可以应对非常复杂的业务，但缺点也很明显，SQL是字符串，及其容易出错，即使是MyBatis老手了也很经常栽在这里。

那么有没有一种方式兼具JPA的强类型和MyBatis的灵活呢？有，那就是JPA+QueryDSL，QueryDSL补足了JPA在动态条件查询、只查询某些列这两个弱项，让我可以以强类型的方式书写SQL，但这接下来就遇到了另一个问题：`VO`爆炸式增长带来的命名问题。

## 命名问题

随着项目的演变，各类`POJO:VO, DTO, BO...`（为了表述方便，后文只称其为`VO`）将会充斥着整个项目，这些`VO`大部分是同一类对象的不同属性的各种组合，需要为这些`VO`取不同的名字！

但是**合适**名字也不是那么好取的，不然也不会有各种命名规范了。我们来看一个很常见的示例：

一个`User`对象，对应数据库表`user`
```java
@Data
@Entity
@Table(name = "user")
public class User {
    private int id;
    private String name;
    private String password;
    private int age;
    private Gender gender;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```
现有需求：根据`id`查询`User`的`name`, `age`, `gender`，那么此时你需要创建一个`UserVo`如下：
```java
@Data
public class UserVo {
    private String name;
    private int age;
    private Gender gender;
}
```
那么还有一个需求：查询所有`User`列表，同时需要根据创建时间排序，为了安全性考虑，`password`这个属性是不应该对前端暴漏的。所以你需要在创建一个`UserVo2`如下：
```java
@Data
public class UserVo2 {
    private int id;
    private String name;
    private int age;
    private Gender gender;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```
再来一个需求：`Gender`性别这一个属性，不要默认的`男,女,未知`，而是翻译成`帅哥，美女，秘密`。此时又需要再次创建一个`UserVo3`：
```java
@Data
public class UserVo {
    private String name;
    private int age;
    private String gender;
}
```
如果还有其他的需求呢？每一个需求要查询的属性都有部分相似，但都有一些不同，你需要为每一个这样的需求创建一个`VO`。长期以往，项目中就会充斥着大量相似但不同的同类型对象，随着这种对象的爆炸式增长，命名就成了一个大难题。

这里给一个我实际项目的中的`VO`列表：
```
+--- AlarmTerminalBean.java
+--- AppBean.java
+--- AttributeBean.java
+--- AttributeDataBean.java
+--- AttributesBean.java
+--- CnAttributelBean.java
+--- CnAttribute_GA.java
+--- FtStVo.java
+--- FtTplBean.java
+--- JobLogBean.java
+--- Lx_DeviceBean.java
+--- Lx_ResultJSONBean.java
+--- PropertyBean.java
+--- PushCountBean.java
+--- PushLogBean.java
+--- req
|   +--- ApplyResourceParameter.java
|   +--- SubcribeParameter.java
|   +--- UnitParameter.java
+--- res
|   +--- AlarmDataBean.java
|   +--- DeviceBean.java
|   +--- DeviceBeanres.java
|   +--- DeviceDataBean.java
|   +--- EquipmentBean.java
|   +--- MapBean.java
|   +--- PlatformBean.java
|   +--- ResultBean.java
|   +--- SP_IndustryBean.java
|   +--- SubPlatformDevice.java
|   +--- UnitBean.java
+--- ResponseBean.java
+--- ResultBean.java
+--- ResultJSONBean.java
+--- Result_Bean.java
+--- RoleBean.java
+--- RolePermission.java
+--- ServerBean.java
+--- SpInfoBean.java
+--- SpPlatBean.java
+--- SP_Apply_ResourcesBean.java
+--- SP_Apply_Resources_CentreBean.java
+--- SP_JobBean.java
+--- SP_JobDetailBean.java
+--- SP_UserBean.java
+--- TransvalueBean.java

...省略其他的几百个，不要吐槽这命名了，这项目经手了N多人，这样的命名我自己看着都烦，但是不能改，牵一发而动全身呀        
```

而且还带来了另一个问题：维护成本的上升！

维护成本的上升主要体现在以下几点：
1. 接手这个项目的人，他不会对所有的VO都了如指掌，当实现新的需求时，会想当然的在随手增加一个`VO`，进一步让`VO`的数量上升。
2. 同一个属性结果有不同的名称，比如一个`UserVo3`对象，将`name`改成了`username`，而另一个`UserVo4`将`name`改成了`nickname`，与其他服务对接时，就会很困惑到底应该要什么名字。
3. 随着业务演变之后，一些`VO`已不再使用，但是维护人员是不敢随意删除这些`VO`的，毕竟“代码能跑就行”（偷笑
4. 无聊且重复的体力劳动，很多`VO`大部分属性是相同的，只有少部分属性不同，而维护人员避免不了复制已有属性，再手动敲剩下的不同的属性，这个过程是纯粹的无聊且重复的体力劳动。

## 解决`VO`爆炸带来的命名难题

所以当我厌烦了这种`VO`爆炸的问题之后，我开始寻找有没有一种可能，让Java能用一个对象表达多种可能的`VO`。所幸我找到了，那就是接下来要说的Jimmer！

Jimmer采用动态对象的设计，让其具备了复杂的表达能力，还是以上面的`User`的几个需求为例。

注：以下仅为示例，并非从头搭建，完整搭建Jimmer项目，请去官网查看相关文档。

需求1: 只查询`User`的的`name`, `age`, `gender`
```java
// 真实情况应该从dao层查询数据库后返回该对象，这里为了方便就直接先手动创建一个对象了
User user = UserDraft.$.produce(draft -> draft.setName("张三").setAge(20).setGender(Gender.MAN));
```
返回给前端的json为
```json
{
	"name": "张三",
	"age": 20,
	"gender": "MAN"
}
```

需求2: 查询所有`User`列表，去掉`password`属性
```java
// 真实情况应该从dao层查询数据库后返回该对象，这里为了方便就直接先手动创建一个对象了
User user = UserDraft.$.produce(draft -> draft.setId(1).setName("张三").setAge(20).setGender(Gender.MAN).setCreateTime(LocalDateTime.now()).setUpdateTime(LocalDateTime.now()));
```
返回给前端的json为
```json
{
	"id": 1,
	"name": "张三",
	"age": 20,
	"gender": "MAN",
	"createTime": "2023-04-12T18:12:46.273",
	"updateTime": "2023-04-12T18:12:46.273"
}
```
可以看到Jimmer框架提供的这个动态对象，具有无穷的表达能力，无论`User`的属性怎么组合，始终属于`User`这个范畴内，那就可以只用这一个`User`类型，而不必再创建无穷多的`VO`，一举解决了`VO`爆炸的管理问题以及命名问题！

使用了Jimmer的项目中，将不再有爆炸数量的`VO`，我终于不再为`VO`烦恼了，可以把精力和脑力用来更为专注的写业务代码了。这个功能是最吸引我的，是我决定尝试Jimmer的最主要原因！

## 发现问题越早，修复问题所付出的成本就越低

工程质量管理中有个早鸟原则：发现越早，损失越小。

Jimmer采用与QueryDSL、Jooq类似的提前编译技术，根据实体接口生成代理对象，由此提供强类型约束，让你在写代码时直接操作对象的API，而不是书写SQL字符串，让潜在的错误在编译时就不通过。将发现错误的时间提前到编译期而不是运行时，带来的好处就是可以以极低的成本进行修复，相反，如果是项目上线了，在运行时才暴露出问题，此时造成的生产环境的损失就不可估量了。

以上面的两个需求为例，dao层代码如下：

```java
// 再次感慨，不用针对每种不同的属性组合而书写对应的`VO`是真的很爽啊！
// 这两个方法都返回的User，而不是特定的UserVo1，UserVo2
public User findUserById(int id){
    UserTable user = UserTable.$;
    // select id, name, age, gender from user where id = :id
    return sqlClient.createQuery(user)
					.where(user.id().eq(id))
					.select(user.fetch(UserFetcher.name().age().gender()))
					.execute();
}

public User findUserWithoutPassword(int id){
    UserTable user = UserTable.$;
    // select id, name, age, gender, create_time, update_time from user where id = :id
    return sqlClient.createQuery(user)
					.where(user.id().eq(id))
					.select(user.fetch(UserFetcher.allScalarFields().password(false)))
					.execute();
}
```

可以看到，该查询是强类型的，中间没有任何字符串的影子，因此绝不会有手写SQL时的最经常出现的语法错误等问题。任何时候只要你使用DSL写的代码不符合规范时，编译器就会直接告诉你错误，这可以让你在开发时就注意到问题，而此时所付出的修复成本，就远比在运行时才发现问题后的修复成本要低得多！

这种提前编译的风格是我一向的偏爱，可以享受到强类型的各种好处：书写代码时，ide会给你提示api，拥有极其流畅的体验，书写错误时，ide会给出错误警告。如果说`Jimmer`动态对象无穷的表达能力，是让我感兴趣尝试的原因，那么这提前编译，将错误提前到编译期而不是运行时，以及流畅的api书写体验，就是我决定深度使用的原因了！

## 完全透明的缓存机制

还记得本文开头的那句名言么，除了命名，还有一个缓存也是一个让无数程序员头疼的问题。

在日常开发中，经常需要先查看是否命中缓存，如果命中缓存了则直接返回缓存中的数据，这也是很无聊的重复劳动，需要在每个地方都手写一遍该过程。而这还是最简单的情况，如果情况稍微复杂一点，需要返回一个带有关联关系的数据，比如需要返回`User`对象关联的`Role`对象，那么这就得从多个地方去重复上述步骤，得到缓存之后再进行组装，最后再进行返回。

而在`Jimmer`中，提供了完全透明的缓存机制，它不与任何缓存技术耦合，你可以选择任意你所喜爱的缓存技术，`Jimmer`将自动保证缓存的一致性。

以下代码截取自官方：
```java
@Configuration
public class CacheConfig {

    @Bean
    public CacheFactory cacheFactory(
            RedisTemplate<String, byte[]> redisTemplate,
            ObjectMapper objectMapper
    ) {
        RedisTemplate<String, byte[]> redisTemplate = RedisCaches.cacheRedisTemplate(connectionFactory);
        
        return new CacheFactory() {

            // Id -> Object
            @Override
            public Cache<?, ?> createObjectCache(ImmutableType type) {
                return new ChainCacheBuilder<>()
                        .add(new CaffeineBinder<>(512, Duration.ofSeconds(1)))
                        .add(new RedisValueBinder<>(redisTemplate, objectMapper, type, Duration.ofMinutes(10)))
                        .build();
            }

            // Id -> TargetId, for one-to-one/many-to-one
            @Override
            public Cache<?, ?> createAssociatedIdCache(ImmutableProp prop) {
                return createPropCache(
                        TenantAware.class.isAssignableFrom(prop.getTargetType().getJavaClass()),
                        prop,
                        redisTemplate,
                        objectMapper,
                        Duration.ofMinutes(5)
                );
            }

            // Id -> TargetId list, for one-to-many/many-to-many
            @Override
            public Cache<?, List<?>> createAssociatedIdListCache(ImmutableProp prop) {
                return createPropCache(
                        TenantAware.class.isAssignableFrom(prop.getTargetType().getJavaClass()),
                        prop,
                        redisTemplate,
                        objectMapper,
                        Duration.ofMinutes(5)
                );
            }

            // Id -> calculated value, for transient properties with resolver
            @Override
            public Cache<?, ?> createResolverCache(ImmutableProp prop) {
                return createPropCache(
                        prop.equals(BookStoreProps.AVG_PRICE.unwrap()) ||
                                prop.equals(BookStoreProps.NEWEST_BOOKS.unwrap()),
                        prop,
                        redisTemplate,
                        objectMapper,
                        Duration.ofHours(1)
                );
            }
        };
    }

    private static <K, V> Cache<K, V> createPropCache(
            boolean isMultiView,
            ImmutableProp prop,
            RedisTemplate<String, byte[]> redisTemplate,
            ObjectMapper objectMapper,
            Duration redisDuration
    ) {
        /*
         * If multi-view cache is required, only redis can be used, because redis support hash structure.
         * The value of redis hash is a nested map, so that different users can see different data.
         *
         * Other simple key value caches can be divided into two levels.
         * The first level is caffeine, the second level is redis.
         *
         * Note: Once the multi-view cache takes affect, it will consume
         * a lot of cache space, please only use it for important data.
         */
        if (isMultiView) {
            return new ChainCacheBuilder<K, V>()
                    .add(new RedisHashBinder<>(redisTemplate, objectMapper, prop, redisDuration))
                    .build();
        }

        return new ChainCacheBuilder<K, V>()
                .add(new CaffeineBinder<>(512, Duration.ofSeconds(1)))
                .add(new RedisValueBinder<>(redisTemplate, objectMapper, prop, redisDuration))
                .build();
    }
}

```
只要定义一个`CacheFactory`的Bean，实现`CacheFactory`里的几个方法即可，而具体实现可以由你自己定制。以上代码使用了两个缓存：一级缓存`Caffeine`和二级缓存`Redis`。你甚至可以直接copy该代码，不用自己实现（偷懒）。

而有了以上配置之后，Jimmer将自动负责所有经过Jimmer自身API的增删改查的缓存一致性，从此再也不用从各处代码中组装缓存了！抛弃原来的方式吧，在各处拼接查询缓存、组装缓存的操作，对代码的侵入性太强了。

## 总结

Jimmer采用的提前编译、动态对象、流畅的API设计、透明的缓存一致性，让我经过半年多的深度使用后，已经彻底迷上了这款ORM，极大程度的将我的精力从无聊重复的体力劳动中解脱了出来，代码开发效率有了质的提升！只要设计好对象之间的关联关系，写起业务来真的太简单了，得心应手，流畅至极！