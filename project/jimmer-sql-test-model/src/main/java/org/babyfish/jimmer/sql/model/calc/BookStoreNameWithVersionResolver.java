package org.babyfish.jimmer.sql.model.calc;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.TypedTransientResolver;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.model.BookStore;
import org.babyfish.jimmer.sql.model.BookStoreTable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BookStoreNameWithVersionResolver implements TypedTransientResolver<BookStore, UUID, String> {

    private static final BookStoreTable table = BookStoreTable.$;

    private final JSqlClient sqlClient;

    public BookStoreNameWithVersionResolver(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Override
    public Map<UUID, String> resolve(Collection<UUID> ids) {
        List<Tuple3<UUID, String, Integer>> tuples = sqlClient
                .createQuery(table)
                .where(table.id().in(ids))
                .select(
                        table.id(),
                        table.name(),
                        table.version()
                )
                .execute(TransientResolver.currentConnection());
        Map<UUID, String> map = new LinkedHashMap<>((tuples.size() * 4 + 2) / 3);
        for (Tuple3<UUID, String, Integer> tuple : tuples) {
            map.put(tuple.get_1(), tuple.get_2() + "#" + tuple.get_3());
        }
        return map;
    }

    @Override
    public Map<UUID, String> resolve(Collection<UUID> ids, Context<BookStore> context) {
        Collection<? extends BookStore> stores = context.getContent();
        Map<UUID, String> map = new LinkedHashMap<>((stores.size() * 4 + 2) / 3);
        for (BookStore store : stores) {
            map.put(store.id(), store.name() + "#" + store.version());
        }
        return map;
    }
}
