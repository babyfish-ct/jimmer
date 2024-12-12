package org.babyfish.jimmer.meta;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.model.Immutables;
import org.babyfish.jimmer.model.User;
import org.babyfish.jimmer.model.UserProps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

public class KeyMatcherTest {

    @Test
    public void test() {
        KeyMatcher keyMatcher = KeyMatcher.of(
                ImmutableType.get(User.class),
                mapOf(
                        "g1",
                        setOf(
                                UserProps.LOGIN_NAME.unwrap()
                        ),
                        "g2",
                        setOf(
                                UserProps.EMAIL.unwrap()
                        ),
                        "g3",
                        setOf(
                                UserProps.AREA.unwrap(),
                                UserProps.NICK_NAME.unwrap()
                        )
                )
        );

        Assertions.assertEquals(
                "[org.babyfish.jimmer.model.User.loginName, " +
                        "org.babyfish.jimmer.model.User.email, " +
                        "org.babyfish.jimmer.model.User.area, " +
                        "org.babyfish.jimmer.model.User.nickName]",
                keyMatcher.getAllProps().toString()
        );

        User user1 = Immutables.createUser(draft -> {
            draft.setLoginName("MadFrog-02323");
        });
        User user2 = Immutables.createUser(draft -> {
            draft.setEmail("madfrog@gmail.com");
        });
        User user3 = Immutables.createUser(draft -> {
            draft.setArea("North");
            draft.setNickName("MadFrog");
        });
        User user4 = ImmutableObjects.merge(user1, user2, user3);
        User user5 = Immutables.createUser(draft -> {
            draft.setPassword(new byte[] {1, 3, 5});
        });
        Assertions.assertEquals(
                "KeyMatcher.Group{name='g1', props=[org.babyfish.jimmer.model.User.loginName]}",
                keyMatcher.match(user1).toString()
        );
        Assertions.assertEquals(
                "KeyMatcher.Group{name='g2', props=[org.babyfish.jimmer.model.User.email]}",
                keyMatcher.match(user2).toString()
        );
        Assertions.assertEquals(
                "KeyMatcher.Group{name='g3', props=[" +
                        "org.babyfish.jimmer.model.User.area, " +
                        "org.babyfish.jimmer.model.User.nickName]}",
                keyMatcher.match(user3).toString()
        );
        Assertions.assertEquals(
                "KeyMatcher.Group{name='g3', props=[" +
                        "org.babyfish.jimmer.model.User.area, " +
                        "org.babyfish.jimmer.model.User.nickName]}",
                keyMatcher.match(user4).toString()
        );
        Assertions.assertNull(
                keyMatcher.match(user5)
        );
        Assertions.assertEquals(
                "[]",
                keyMatcher.missedProps(
                        Arrays.asList(
                                UserProps.NICK_NAME.unwrap(),
                                UserProps.AREA.unwrap()
                        )
                ).toString()
        );
        Assertions.assertEquals(
                "[org.babyfish.jimmer.model.User.area]",
                keyMatcher.missedProps(
                        Collections.singleton(UserProps.NICK_NAME.unwrap())
                ).toString()
        );
    }

    private static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

    private static <E> Set<E> setOf(E... elements) {
        return new LinkedHashSet<>(Arrays.asList(elements));
    }
}
