# Using jimmer to subvert your business development experience

## Preface

> There are two hard things in computer science: cache invalidation, naming things, and off-by-one errors.--Leon Bambrick

If you are a programmer with considerable development experience, you will probably smile knowingly when you see the famous quote above. This quote is not exaggerated at all, it is a true reflection of daily development, and countless programmers are troubled by naming and caching every day.

Let me briefly introduce myself. I am an ordinary programmer with 7 years of development experience. The main programming language I use is Java. I also know a little Golang, Kotlin, and Python. The well-known companies I have worked for are only ICBC and currently CETC.

## Experience with ORM frameworks 

I believe that as a Java web back-end business developer, one of the most frequent parts of daily work is operating on database tables, so ORM frameworks are an indispensable part of business development. The common ORM frameworks on the market, I can say that I have had long-term use experience with almost all of them: JPA, QueryDSL, Jooq, JdbcTemplate, MyBatis, MyBatis-plus, Fluent-MyBatis, etc.

These frameworks can be roughly divided into two categories. One represented by `JPA` or `Hibernate` is object-oriented ORM, and the other represented by `MyBatis` is ORM that writes native SQL.

Each has its own pros and cons. ORM frameworks represented by `JPA` conform to the object-oriented thinking of `Java`, replacing operating database tables with operating objects, with the advantages of strong typing and isolating SQL dialects, but the disadvantages are that dynamic conditional queries are very weak, writing is complicated, and it is also complicated to query only some columns instead of the entire object. ORM frameworks represented by `MyBatis` have the advantage of being flexible enough. Writing SQL directly allows it to handle very complex business logic, but the disadvantages are also obvious. SQL is a string and it is easy to make mistakes. Even MyBatis veterans often stumble here.

So is there a way to combine the strong typing of JPA and the flexibility of MyBatis? Yes, that's JPA+QueryDSL. QueryDSL makes up for JPA's two weaknesses in dynamic conditional queries and querying only some columns, allowing me to write SQL in a strongly typed way, but then I encountered another problem: the explosive growth of `VO` brought naming issues.

## Naming issues

As the project evolves, various `POJO: VO, DTO, BO...` (for ease of expression, they are collectively referred to as `VO` below) will be filled with the entire project. Most of these `VOs` are different combinations of properties of the same type of object, and different names need to be given to these `VOs`!

However, suitable names are not so easy to come up with, otherwise there would not be various naming conventions. Let's look at a very common example:

A `User` object corresponds to the `user` table in the database
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
Existing requirements: Query `User`'s `name`, `age`, and `gender` according to `id`, then you need to create a `UserVo` as follows:
```java
@Data
public class UserVo {
    private String name;
    private int age;
    private Gender gender;
}
```
Then there is another requirement: Query all `User` lists, and sort by creation time. For security reasons, the `password` property should not be exposed to the front end. So you need to create another `UserVo2` as follows:
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
Another requirement: The `Gender` gender property should not use the default `male, female, unknown`, but translate it to `handsome guy, beauty, secret`. At this time, `UserVo3` needs to be created again:
```java
@Data
public class UserVo {
    private String name;
    private int age;
    private String gender; 
}
```
What if there are other requirements? The properties that need to be queried for each requirement are partially similar but somewhat different. You need to create a `VO` for each such requirement. In the long run, the project will be filled with a large number of similar but different objects of the same type. As these objects explode, naming becomes a big problem.

Here is a list of `VOs` in my actual project:
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

...Omit hundreds of others. Don't complain about this naming. This project has been handled by many people. I find such naming annoying myself, but I can't change it because it affects everything...
```

And it brings another problem: the increase of maintenance costs!

The increase in maintenance costs is mainly reflected in the following points:
1. People taking over this project will not be proficient in all VOs. When implementing new requirements, they will inadvertently increase a `VO`, further increasing the number of `VOs`.
2. The same property has different names in different places. For example, a `UserVo3` object changes `name` to `username`, while another `UserVo4` changes `name` to `nickname`. It will be very confusing which name to use when interfacing with other services.
3. As business evolves, some `VOs` are no longer used, but maintenance personnel dare not delete these `VOs` at will. After all, "the code can run" (sneer).
4. Boring and repetitive manual work. Most `VOs` have mostly the same properties, with only a few different properties. Maintenance personnel inevitably have to copy existing properties and then manually type the remaining different properties. This process is purely boring and repetitive manual work.

## Solving the naming problem caused by VO explosion

So after I got tired of this VO explosion problem, I started looking for a possibility that would allow Java to use one object to express many possible `VOs`. Fortunately I found it, that's Jimmer I'm going to talk about next!

Jimmer uses dynamic object design, giving it complex expressive capabilities. Still use the above User requirements as examples.

Note: The following are only examples, not a complete setup from scratch. For complete setup of a Jimmer project, please check the related documents on the official website.

Requirement 1: Only query User's `name`, `age` and `gender`
```java
// In real cases it should be returned from dao query, here for convenience manually create an object
User user = UserDraft.$.produce(draft -> draft.setName("Zhang San").setAge(20).setGender(Gender.MAN));
```
The json returned to the front end is: 
```json
{
    "name": "Zhang San",
    "age": 20,
    "gender": "MAN"
}
```

Requirement 2: Query all `User` lists, remove `password` property
```java 
// In real cases it should be returned from dao query, here for convenience manually create an object
User user = UserDraft.$.produce(draft -> draft.setId(1).setName("Zhang San").setAge(20).setGender(Gender.MAN).setCreateTime(LocalDateTime.now()).setUpdateTime(LocalDateTime.now()));
```
The json returned to the front end is:
```json
{
    "id": 1,
    "name": "Zhang San", 
    "age": 20,
    "gender": "MAN",
    "createTime": "2023-04-12T18:12:46.273",
    "updateTime": "2023-04-12T18:12:46.273"
}
```
It can be seen that the dynamic object provided by the Jimmer framework has infinite expressive power. No matter how the properties of `User` are combined, it always belongs to the category of `User`, then only this one `User` type can be used, instead of creating infinitely many `VOs`, which solves the management problem and naming problem caused by VO explosion!

In projects using Jimmer, there will no longer be an explosive number of `VOs`. I no longer have to worry about `VOs`, and can devote my energy and brainpower to writing business code more focused! This feature is the most appealing to me and the main reason I decided to try Jimmer!

## The earlier problems are discovered, the lower the cost to fix them

There is an early bird principle in engineering quality management: The earlier discovered, the smaller the loss.

Jimmer adopts pre-compilation technology similar to QueryDSL and Jooq to generate proxy objects based on entity interfaces, thus providing strong type constraints, allowing you to directly operate object APIs when writing code, rather than writing SQL strings, so that potential errors do not pass at compile time instead of runtime. The benefit of bringing forward error discovery to compile time instead of runtime is that fixes can be made at extremely low cost. On the contrary, if problems are only exposed at runtime after the project goes online, the resulting losses in the production environment would be inestimable.

Take the two requirements above as examples, the dao layer code is as follows:

```java
// Once again feel that not having to write corresponding `VO` for different property combinations is really cool!
// These two methods both return User, not specific UserVo1, UserVo2  
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

It can be seen that this query is strongly typed without any string shadows, so there will never be syntax errors and other common problems when writing SQL by hand. Whenever the DSL code you write does not conform to the specification, the compiler will tell you the error directly. This allows you to notice problems during development, and the cost of fixing at this time is much lower than the cost of fixing after the problem is discovered at runtime! 

I have always favored this precompilation style. You can enjoy all the benefits of strong typing: when writing code, IDE will give you API prompts and provide an extremely fluent experience. When writing errors, IDE will give error warnings. If I say the infinite expressiveness of Jimmer's dynamic objects is what made me interested in trying it out, then this precompilation that brings errors forward to compile time rather than runtime, along with the fluent API writing experience, is the reason I decided to use it in depth!

## Fully transparent cache mechanism 

Do you still remember the famous quote at the beginning of this article? In addition to naming, caching is also a problem that gives headaches to countless programmers.

In daily development, we often need to check if the cache is hit first, and directly return the data in the cache if hit, which is also a boring repetition that needs to be done manually everywhere. And this is the simplest case. If the situation is slightly more complicated, a data with associations needs to be returned, such as returning the `Role` object associated with the `User` object, then this process needs to be repeated from multiple places to get the cache and then assemble it before finally returning it.

In `Jimmer`, a completely transparent cache mechanism is provided. It does not couple with any caching technology. You can choose any caching technology you like, and `Jimmer` will automatically ensure cache consistency.

The following code is excerpted from the official website:
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

            // Id -> computed value, for transient properties with resolver
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
Just define a `CacheFactory` Bean and implement the methods in `CacheFactory` . Specific implementation can be customized by yourself. The above code uses two caches: first level cache `Caffeine` and second level cache `Redis`. You can even directly copy this code without implementing it yourself (lazy).

With the above configuration, Jimmer will automatically be responsible for cache consistency of all CRUD operations through Jimmer's own APIs. From now on, you don't need to assemble caches from various places in the code! Abandon the old way of splicing query caches and assembling caches from various places, which is too invasive to the code.

## Summary

Jimmer's precompilation, dynamic objects, fluent API design, and transparent cache consistency allow me to thoroughly fall in love with this ORM after more than half a year of intensive use, greatly freeing me from boring repetitive manual work and significantly improving code development efficiency! As long as the associations between objects are well designed, It's really easy to write business, it's handy, and it's extremely smooth!
