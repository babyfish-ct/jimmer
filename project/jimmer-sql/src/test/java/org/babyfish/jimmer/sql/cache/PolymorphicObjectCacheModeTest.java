package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.CacheImpl;
import org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator.ClientType;
import org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator.EnumOrganization;
import org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator.EnumOrganizationDraft;
import org.babyfish.jimmer.sql.model.inheritance.singletable.*;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.ClientImplicitCatchAllView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PolymorphicObjectCacheModeTest extends AbstractQueryTest {

    @Test
    public void testDiscriminatorCanBeDerivedWhenPropertyIsUnloaded() {
        Organization organization = OrganizationDraft.$.produce(draft -> draft.setId(1L));
        assertFalse(ImmutableObjects.isLoaded(organization, "type"));
        assertEquals("ORG", ImmutableObjects.getDiscriminator(organization));

        EnumOrganization enumOrganization = EnumOrganizationDraft.$.produce(draft -> draft.setId(2L));
        assertFalse(ImmutableObjects.isLoaded(enumOrganization, "type"));
        assertEquals(ClientType.ORG, ImmutableObjects.getDiscriminator(enumOrganization));
    }

    @Test
    public void testLocalOnlyCache() {
        JSqlClient sqlClient = createSqlClient(true);
        jdbc(con -> assertReadMatrix(sqlClient, con));
    }

    @Test
    public void testRemoteOnlyCache() {
        JSqlClient sqlClient = createSqlClient(false);
        jdbc(con -> assertReadMatrix(sqlClient, con));
    }

    private JSqlClient createSqlClient(boolean local) {
        return getSqlClient(builder -> builder.setCaches(cfg -> cfg.setCacheFactory(new CacheFactory() {
            @Override
            public Cache<?, ?> createObjectCache(ImmutableType type) {
                if (type.getJavaClass() != Client.class) {
                    return null;
                }
                if (local) {
                    return new RetainingCache<>(type);
                }
                return new CacheImpl<>(type);
            }
        })));
    }

    private static void assertReadMatrix(JSqlClient sqlClient, Connection con) {
        for (int i = 0; i < 2; i++) {
            Client client = sqlClient
                    .getEntities()
                    .forConnection(con)
                    .findById(Client.class, 100L);
            assertInstanceOf(Organization.class, client);
            assertFalse(ImmutableObjects.isLoaded(client, "type"));
            assertEquals("ORG", ImmutableObjects.getDiscriminator(client));
            assertLoadState(client, "id", "name");

            List<Client> clients = sqlClient
                    .getEntities()
                    .forConnection(con)
                    .findByIds(Client.class, Arrays.asList(100L, 101L));
            assertInstanceOf(Organization.class, clients.get(0));
            assertFalse(ImmutableObjects.isLoaded(clients.get(0), "type"));
            assertEquals("ORG", ImmutableObjects.getDiscriminator(clients.get(0)));
            assertLoadState(clients.get(0), "id", "name");
            assertInstanceOf(Person.class, clients.get(1));
            assertFalse(ImmutableObjects.isLoaded(clients.get(1), "type"));
            assertEquals("Person", ImmutableObjects.getDiscriminator(clients.get(1)));
            assertLoadState(clients.get(1), "id", "name");

            clients = sqlClient
                    .getEntities()
                    .forConnection(con)
                    .findByIds(
                            ClientFetcher.$
                                    .type()
                                    .name()
                                    .forType(OrganizationFetcher.$.taxCode())
                                    .forType(PersonFetcher.$.firstName()),
                            Arrays.asList(100L, 101L)
                    );
            Organization organization = assertInstanceOf(Organization.class, clients.get(0));
            assertEquals("ORG", organization.type());
            assertEquals("ACME-001", organization.taxCode());
            assertLoadState(organization, "id", "type", "name", "taxCode");
            Person person = assertInstanceOf(Person.class, clients.get(1));
            assertEquals("Person", person.type());
            assertEquals("Bob", person.firstName());
            assertLoadState(person, "id", "type", "name", "firstName");

            List<ClientImplicitCatchAllView> views = sqlClient
                    .getEntities()
                    .forConnection(con)
                    .findByIds(ClientImplicitCatchAllView.class, Arrays.asList(100L, 101L));
            ClientImplicitCatchAllView.Organization organizationView = assertInstanceOf(
                    ClientImplicitCatchAllView.Organization.class,
                    views.get(0)
            );
            assertEquals("ACME-001", organizationView.getTaxCode());
            assertInstanceOf(ClientImplicitCatchAllView.Default.class, views.get(1));

            ClientImplicitCatchAllView view = sqlClient
                    .getEntities()
                    .forConnection(con)
                    .findById(ClientImplicitCatchAllView.class, 100L);
            organizationView = assertInstanceOf(ClientImplicitCatchAllView.Organization.class, view);
            assertEquals("ACME-001", organizationView.getTaxCode());
        }
    }

    private static class RetainingCache<T> implements Cache<Object, T> {

        private final ImmutableType type;

        private final Map<Object, T> valueMap = new HashMap<>();

        private RetainingCache(ImmutableType type) {
            this.type = type;
        }

        @Override
        public @NotNull ImmutableType type() {
            return type;
        }

        @Override
        public @Nullable ImmutableProp prop() {
            return null;
        }

        @Override
        public @NotNull Map<Object, T> getAll(
                @NotNull Collection<Object> keys,
                @NotNull CacheEnvironment<Object, T> env
        ) {
            Map<Object, T> resultMap = new LinkedHashMap<>();
            Set<Object> missedKeys = new LinkedHashSet<>();
            for (Object key : keys) {
                if (valueMap.containsKey(key)) {
                    resultMap.put(key, valueMap.get(key));
                } else {
                    missedKeys.add(key);
                }
            }
            if (!missedKeys.isEmpty()) {
                Map<Object, T> loadedMap = env.getLoader().loadAll(missedKeys);
                for (Object key : missedKeys) {
                    T value = loadedMap.get(key);
                    valueMap.put(key, value);
                    resultMap.put(key, value);
                }
            }
            return resultMap;
        }

        @Override
        public void deleteAll(@NotNull Collection<Object> keys, @Nullable Object reason) {
            valueMap.keySet().removeAll(keys);
        }
    }
}
